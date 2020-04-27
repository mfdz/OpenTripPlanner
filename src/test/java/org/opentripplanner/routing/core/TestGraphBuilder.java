package org.opentripplanner.routing.core;

import com.google.common.collect.Lists;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.GermanyWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import java.io.File;
import java.util.List;

public class TestGraphBuilder {
    public static Graph buildGraph(String osmFile) {
        GraphBuilder graphBuilder = new GraphBuilder();

        List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
        OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File(osmFile));
        osmProviders.add(osmProvider);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
        osmModule.skipVisibility = true;
        osmModule.setDefaultWayPropertySetSource(new GermanyWayPropertySetSource());
        graphBuilder.addModule(osmModule);
        graphBuilder.addModule(new StreetLinkerModule());
        graphBuilder.serializeGraph = false;
        graphBuilder.run();

        Graph graph = graphBuilder.getGraph();
        graph.index(new DefaultStreetVertexIndexFactory());
        return graph;
    }
}
