package org.opentripplanner.routing.core;

import org.apache.commons.lang3.ArrayUtils;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.GermanyWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TestGraphBuilder {

    private static Map<String, Graph> cache = new ConcurrentHashMap<>();

    static Graph buildGraph(String... osmFile) {
        return buildGraph(osmFile, new String[]{});
    }

    static Graph buildGtfsGraph(String osmFile, String gtfsFile) {
        return buildGraph(new String[]{osmFile}, new String[]{gtfsFile});
    }

    private static Graph buildGraph(String[] osmFiles, String[] gtfsFiles) {
        var cacheKey = cacheKey(osmFiles, gtfsFiles);
        if(cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        } else {
            GraphBuilder graphBuilder = new GraphBuilder();

            List<OpenStreetMapProvider> osmProviders = Arrays.stream(osmFiles)
                    .map(f -> new AnyFileBasedOpenStreetMapProviderImpl(new File(f)))
                    .collect(Collectors.toList());

            var gtfsBundles = Arrays.stream(gtfsFiles)
                    .map(f -> new GtfsBundle(new File(f)))
                    .collect(Collectors.toList());
            graphBuilder.addModule(new GtfsModule(gtfsBundles));

            OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
            osmModule.skipVisibility = true;
            osmModule.setDefaultWayPropertySetSource(new GermanyWayPropertySetSource());
            graphBuilder.addModule(osmModule);
            graphBuilder.addModule(new StreetLinkerModule());
            graphBuilder.serializeGraph = false;
            graphBuilder.run();

            Graph graph = graphBuilder.getGraph();
            graph.index(new DefaultStreetVertexIndexFactory());
            cache.put(cacheKey, graph);
            return graph;
        }
    }

    private static String cacheKey(String[] osmFiles, String[] gtfsFiles) {
        var names = Arrays.asList(ArrayUtils.addAll(osmFiles, gtfsFiles));
        names.sort(Comparator.naturalOrder());
        return String.join("|", names);
    }
}
