package org.opentripplanner.graph_builder;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.GraphBuilderOSMAnnotation;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class generates a set of <a href="https://github.com/joto/osmoscope">Osmoscope</a> graph
 * annotations reports for OSM issues.
 * 
 * @author hbruch
 */
public class AnnotationsToOsmoscope implements GraphBuilderModule {
    
    private static Logger LOG = LoggerFactory.getLogger(AnnotationsToOsmoscope.class);

    // Path to output folder
    private File outPath;

    // Key is classname, value is annotation message
    // Multimap because there are multiple annotations for each classname
    private Multimap<String, OsmoscopeIssue> annotations;

    private Map<String, OsmoscopeLayer> layerInfos = new HashMap<>();

    private String updatePolicy = "unknown";

    public AnnotationsToOsmoscope(File outpath) {
        this.outPath = outpath;
        annotations = ArrayListMultimap.create();
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        // Groups annotations in multimap according to annotation class
        for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
            addAnnotation(annotation);
        }

        LOG.info("Creating Annotations log");

        // Write json file for every annotation type and update stats
        Date now = new Date();
        for (Map.Entry<String, Collection<OsmoscopeIssue>> entry : annotations.asMap().entrySet()) {
            List<OsmoscopeIssue> annotationsList;
            if (entry.getValue() instanceof List) {
                annotationsList = (List<OsmoscopeIssue>) entry.getValue();
            } else {
                annotationsList = new ArrayList<>(entry.getValue());
            }
            writeAnnotationsFile(entry.getKey(), annotationsList);
            appendStatsFile(entry.getKey(), now, annotationsList.size());
            writeLayerFile(layerInfos.get(entry.getKey()));
        }

        writeLayersFile();

        LOG.info("Annotated logs are in {}", outPath);

    }

    private void writeLayersFile() {
        File layersFile = new File(outPath, "layers.json");
        try {
            HashMap<String, Object> content = new LinkedHashMap<>();
            content.put("name", "OpenTripPlanner OSM related Graph Annotations");
            List<String> layers = new ArrayList<>();
            for (String annotationType : annotations.keySet()) {
                layers.add("layer_" + annotationType + ".json");
            }
            content.put("layers", layers);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(layersFile, content);
        } catch (IOException ex) {
            LOG.error("Failed to write layers file: " + layersFile.toString(), ex);
        }
    }

    private void appendStatsFile(String key, Date now, int size) {
        File statsFile = new File(outPath, "stats_" + key + ".csv");
        boolean fileExists = statsFile.exists();
        try (FileWriter fileWriter = new FileWriter(statsFile, true)) {
            if (!fileExists) {
                fileWriter.write("Date,Count\n");
            }
            fileWriter.write(new SimpleDateFormat("yyyy-MM-dd").format(now) + "," + size + "\n");
        } catch (IOException e) {
            LOG.error("Failed to append stats to stats file: " + statsFile.toString() + ".", e);
        }
    }

    private void writeLayerFile(OsmoscopeLayer layer) {
        File layersFile = new File(outPath, "layer_" + layer.id + ".json");
        try {
            HashMap<String, Object> content = new LinkedHashMap<>();
            HashMap<String, Object> doc = new LinkedHashMap<>();
            content.put("id", layer.id);
            content.put("name", sentenceCase(layer.name));
            content.put("doc", doc);
            content.put("updates", layer.updates);
            content.put("geojson_url", layer.id + ".json");
            content.put("stats_data_url", "stats_" + layer.id + ".csv");
            doc.put("description", layer.description);
            doc.put("why_problem", layer.whyProblem);
            doc.put("how_to_fix", layer.howToFix);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(layersFile, content);
        } catch (IOException ex) {
            LOG.error("Failed to write layers file: " + layersFile.toString(), ex);
        }
    }

    private void writeAnnotationsFile(String key, List<OsmoscopeIssue> annotationsList) {
        File annotationsFile = new File(outPath, key + ".json");
        try {
            Collection<HashMap<String, Object>> features = new ArrayList<>();
            int counter = 0;
            for (OsmoscopeIssue issue : annotationsList) {
                HashMap<String, Object> feature = new LinkedHashMap<>();
                HashMap<String, Object> properties = new LinkedHashMap<>();
                HashMap<String, Object> geometry = new LinkedHashMap<>();

                feature.put("id", counter++);
                feature.put("type", "Feature");
                feature.put("properties", properties);
                feature.put("geometry", geometry);
                properties.put(idAttributeFor(issue), issue.osmId);
                properties.putAll(issue.properties);
                geometry.put("type", "Point");
                geometry.put("coordinates", new double[] { issue.lon, issue.lat });

                features.add(feature);
            }

            HashMap<String, Object> featureCollection = new LinkedHashMap<>();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", features);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(annotationsFile, featureCollection);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AnnotationsToOsmoscope.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    private String idAttributeFor(OsmoscopeIssue issue) {
        switch (issue.osmType) {
        case NODE:
            return "node_id";
        case WAY:
            return "way_id";
        case RELATION:
            return "relation_id";
        default:
            return "node_id";
        }
    }

    /**
     * Groups OSM annotations according to annotation class name
     *
     * All OSM annotations are saved together in multimap where key is annotation classname and
     * values are list of annotations with that class
     * 
     * @param annotation
     */
    private void addAnnotation(GraphBuilderAnnotation annotation) {
        if (annotation instanceof GraphBuilderOSMAnnotation) {
            GraphBuilderOSMAnnotation osmAnnotation = (GraphBuilderOSMAnnotation) annotation;
            String annotationTypeId = annotation.getClass().getSimpleName();
            if (!layerInfos.containsKey(annotationTypeId)) {
                layerInfos.put(annotationTypeId, asOsmoscopeLayer(osmAnnotation));
            }
            annotations.put(annotationTypeId, asToOsmoscopeIssue(osmAnnotation));
        }
    }

    private OsmoscopeLayer asOsmoscopeLayer(GraphBuilderOSMAnnotation annotation) {
        String annotationTypeId = annotation.getClass().getSimpleName();

        return new OsmoscopeLayer(annotationTypeId, asName(annotationTypeId), updatePolicy,
                annotation.getDescription(), annotation.getWhyProblem(), annotation.getHowToFix());
    }

    private String asName(String annotationTypeId) {
        return annotationTypeId;
    }

    private OsmoscopeIssue asToOsmoscopeIssue(GraphBuilderOSMAnnotation annotation) {
        return new OsmoscopeIssue(annotation.getOsmType(), annotation.getOsmId(),
                annotation.getLon(), annotation.getLat(), annotation.getAdditionalInfo());
    }

    @Override
    public void checkInputs() {
        if (outPath == null) {
            LOG.error("Saving folder is empty!");
            return;
        }

        outPath = new File(outPath, "report_osmoscope");
        if (outPath.exists()) {
            // Removes all json files from report directory (we want to keep csv
            // files to append statistics)
            final File[] files = outPath.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name.matches("\\.json$");
                }
            });
            for (final File file : files) {
                if (!file.delete()) {
                    LOG.error("Failed to remove file " + outPath.toString()
                            + ". Osmoscope report won't be generated!");
                    return;
                }
            }
        } else {
            // Creates report directory if it doesn't exist yet
            try {
                FileUtils.forceMkdir(outPath);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Failed to create Osmoscope report directory: " + outPath.toString()
                        + ". Osmoscope report won't be generated!", e);
                return;
            }
        }
    }

    static String sentenceCase(String s) {
        try {
            String split = s.replaceAll("([^_A-Z])([A-Z])", "$1 $2");
            return s.substring(0, 1) + split.substring(1).toLowerCase();
        } catch (Exception e) {
            LOG.error(s, e);
            return s;
        }
    }

    private static class OsmoscopeIssue {
        private double lat;
        private double lon;
        private GraphBuilderOSMAnnotation.OsmType osmType;
        private long osmId;
        private Map<String, Object> properties;  

        public OsmoscopeIssue(GraphBuilderOSMAnnotation.OsmType osmType, long osmId, double lon,
                double lat, Map<String, Object> properties) {
            this.osmType = osmType;
            this.osmId = osmId;
            this.lon = lon;
            this.lat = lat;
            this.properties = properties;
        }
    }

    public class OsmoscopeLayer {
        private String id;
        private String name;
        private String updates;
        private String description;
        private Object whyProblem;
        private String howToFix;

        public OsmoscopeLayer(String id, String name, String updates, String description,
                String whyProblem, String howToFix) {
            this.id = id;
            this.name = name;
            this.updates = updates;
            this.description = description;
            this.whyProblem = whyProblem;
            this.howToFix = howToFix;
        }
    }
}
