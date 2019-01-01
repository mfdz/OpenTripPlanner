package org.opentripplanner.graph_builder;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.graph_builder.annotation.GraphBuilderOSMAnnotation;
import org.opentripplanner.routing.graph.Graph;

public class AnnotationsToOsmoscopeTest {

    private static final String ANNOTION_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[{\"id\":0,\"type\":\"Feature\",\"properties\":{\"way_id\":4711},\"geometry\":{\"type\":\"Point\",\"coordinates\":[0.0,0.0]}}]}";

    private static final String ANNOTION_WITH_NOTES_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[{\"id\":0,\"type\":\"Feature\",\"properties\":{\"way_id\":4711,\"notes\":\"Some Notes\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[0.0,0.0]}}]}";

    private static final String LAYERS_JSON = "{\"name\":\"OpenTripPlanner OSM related Graph Annotations\",\"layers\":[\"layer_TestAnnotation.json\"]}";

    private static final String LAYER_JSON = "{\"id\":\"TestAnnotation\",\"name\":\"Test annotation\",\"doc\":{\"description\":\"Description\",\"why_problem\":\"WhyProblem\",\"how_to_fix\":\"HowToFix\"},\"updates\":\"unknown\",\"geojson_url\":\"TestAnnotation.json\",\"stats_data_url\":\"stats_TestAnnotation.csv\"}";

    private Graph graph;

    private String tempDir;

    private class TestAnnotation extends GraphBuilderOSMAnnotation {

        private static final long serialVersionUID = 1L;

        private String notes;

        public TestAnnotation(OsmType osmType, long osmId) {
            super(osmType, osmId);
        }

        public TestAnnotation(OsmType osmType, long osmId, String notes) {
            super(osmType, osmId);
            this.notes = notes;
        }

        @Override
        public String getDescription() {
            return "Description";
        }

        @Override
        public String getWhyProblem() {
            return "WhyProblem";
        }

        @Override
        public String getHowToFix() {
            return "HowToFix";
        }

        @Override
        public String getMessage() {
            return "Message";
        }

        @Override
        public Map<String, Object> getAdditionalInfo() {
            HashMap<String, Object> map = new HashMap<>();
            if (notes != null) {
                map.put("notes", notes);
            }
            return map;
        }
    }

    @Before
    public void setUp() throws Exception {
        graph = new Graph();
        tempDir = Files.createTempDirectory(this.getClass().getName()).toString();
    }

    @Test
    public void testAnnotationFileGeneration() throws Exception {
        graph.addBuilderAnnotation(new TestAnnotation(GraphBuilderOSMAnnotation.OsmType.WAY, 4711));
        AnnotationsToOsmoscope annotationsExporter = new AnnotationsToOsmoscope(new File(tempDir));

        annotationsExporter.checkInputs();
        annotationsExporter.buildGraph(graph, null);

        assertFileContentEquals(tempDir + "/report_osmoscope/TestAnnotation.json",
                ANNOTION_GEOJSON);
    }

    @Test
    public void testStatsFileGeneration() throws Exception {
        graph.addBuilderAnnotation(new TestAnnotation(GraphBuilderOSMAnnotation.OsmType.WAY, 4711));
        AnnotationsToOsmoscope annotationsExporter = new AnnotationsToOsmoscope(new File(tempDir));

        annotationsExporter.checkInputs();
        annotationsExporter.buildGraph(graph, null);

        Date now = new Date();
        assertFileContentEquals(tempDir + "/report_osmoscope/stats_TestAnnotation.csv",
                "Date,Count\n" + new SimpleDateFormat("yyyy-MM-dd").format(now) + ",1\n");
    }

    @Test
    public void testLayersFileGeneration() throws Exception {
        graph.addBuilderAnnotation(new TestAnnotation(GraphBuilderOSMAnnotation.OsmType.WAY, 4711));
        AnnotationsToOsmoscope annotationsExporter = new AnnotationsToOsmoscope(new File(tempDir));

        annotationsExporter.checkInputs();
        annotationsExporter.buildGraph(graph, null);

        assertFileContentEquals(tempDir + "/report_osmoscope/layers.json", LAYERS_JSON);
    }

    @Test
    public void testLayerFileGeneration() throws Exception {
        graph.addBuilderAnnotation(new TestAnnotation(GraphBuilderOSMAnnotation.OsmType.WAY, 4711));
        AnnotationsToOsmoscope annotationsExporter = new AnnotationsToOsmoscope(new File(tempDir));

        annotationsExporter.checkInputs();
        annotationsExporter.buildGraph(graph, null);

        assertFileContentEquals(tempDir + "/report_osmoscope/layer_TestAnnotation.json",
                LAYER_JSON);
    }

    @Test
    public void testAdditionalInfo() throws Exception {
        graph.addBuilderAnnotation(
                new TestAnnotation(GraphBuilderOSMAnnotation.OsmType.WAY, 4711, "Some Notes"));
        AnnotationsToOsmoscope annotationsExporter = new AnnotationsToOsmoscope(new File(tempDir));

        annotationsExporter.checkInputs();
        annotationsExporter.buildGraph(graph, null);

        assertFileContentEquals(tempDir + "/report_osmoscope/TestAnnotation.json",
                ANNOTION_WITH_NOTES_GEOJSON);
    }

    private void assertFileContentEquals(String fileName, String annotationGeojson)
            throws IOException {
        File file = new File(fileName);
        Assert.assertTrue("File " + fileName + " does not exist", file.exists());
        String content = new String(Files.readAllBytes(file.toPath()));
        Assert.assertEquals("Content of " + fileName + " does not match expected value",
                annotationGeojson, content);
    }

    public void testSentenceCase() {
        assertEquals(AnnotationsToOsmoscope.sentenceCase("ParkAndRideUnlinked"),
                "Park and ride unlinked");
    }
}
