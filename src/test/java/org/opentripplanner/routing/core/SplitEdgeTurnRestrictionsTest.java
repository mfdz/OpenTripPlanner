package org.opentripplanner.routing.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SplitEdgeTurnRestrictionsTest {


    // Deufringen
    static GenericLocation hardtheimerWeg = new GenericLocation(48.67765, 8.87212);
    static GenericLocation steinhaldenWeg = new GenericLocation(48.67815, 8.87305);
    static GenericLocation k1022 = new GenericLocation(48.67846, 8.87021);

    // Böblingen
    static GenericLocation paulGerhardtWeg = new GenericLocation(48.67765, 8.87212);
    static GenericLocation parkStrasse = new GenericLocation(48.68357, 9.00827);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 03, 3, 7, 0, 0);

    public static Graph buildGraph(String osmFile, String gtfsFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        GraphBuilder graphBuilder = new GraphBuilder(Files.createTempDirectory("otp-").toFile(), new GraphBuilderParameters(node));

        List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
        OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File(osmFile));
        osmProviders.add(osmProvider);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
        osmModule.edgeFactory = new DefaultStreetEdgeFactory();
        osmModule.skipVisibility = true;
        graphBuilder.addModule(osmModule);
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(gtfsFile));
        gtfsBundles.add(gtfsBundle);
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
        graphBuilder.addModule(gtfsModule);
        graphBuilder.addModule(new StreetLinkerModule());
        graphBuilder.serializeGraph = false;
        graphBuilder.run();
        Graph output = graphBuilder.getGraph();
        output.index(new DefaultStreetVertexIndexFactory());
        return output;
    }

    private static String computeCarPolyline(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.CAR);

        request.setNumItineraries(1);
        request.setRoutingContext(graph);

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
        return plan.itinerary.get(0).legs.get(0).legGeometry.getPoints();
    }

    @Test
    public void shouldTakeDeufringenTurnRestrictionsIntoAccount() throws IOException {
        Graph graph = buildGraph(ConstantsForTests.DEUFRINGEN_OSM, ConstantsForTests.DEUFRINGEN_GTFS);
        // https://www.openstreetmap.org/relation/10264251 has a turn restriction so when leaving Hardtheimer Weg
        // you must turn right and take the long way to Steinhaldenweg.
        // on top of this, it has a bus stop so this test also makes sure that the turn restrictions work
        // even when the streets are split.
        String noRightTurnPermitted = computeCarPolyline(graph, hardtheimerWeg, steinhaldenWeg);
        assertThat(noRightTurnPermitted, is("ijbhHuycu@g@Uq@[VeAj@iCTsANoAJiAHsAFuDLoG@_@?YBeGCaAO@C?KBKBKFIJKREf@?d@?h@\\TNb@Ff@?bAMnEKjEOxDWbCc@vCIDMDCB"));

        // when to drive in reverse direction it's fine to go this way
        String leftTurnOk = computeCarPolyline(graph, steinhaldenWeg, hardtheimerWeg);
        assertThat(leftTurnOk, is("kmbhHo_du@BCLEAd@Q`Ak@~CC\\@HBFFWDOd@}Bp@Zf@T"));

        // make sure that going straight on a straight-only turn direction also works
        String straightAhead = computeCarPolyline(graph, hardtheimerWeg, k1022);
        assertThat(straightAhead, is("ijbhHuycu@g@Uq@[e@|BENGVYxA]xAXn@Hd@"));

        // make sure that turning left onto the minor road works even when the opposite direction has a straight-only
        // restriction
        String leftTurnAllowed = computeCarPolyline(graph, k1022, steinhaldenWeg);
        assertThat(leftTurnAllowed, is("kobhHwmcu@Ie@Yo@\\yAXyACGAIB]j@_DPaA@e@MDCB"));
    }

    @Test
    public void shouldTakeBoeblingenTurnRestrictionsIntoAccount() throws IOException {
        Graph graph = buildGraph(ConstantsForTests.BOEBLINGEN_OSM, ConstantsForTests.BOEBLINGEN_GTFS);

        String noRightTurnPermitted = computeCarPolyline(graph, paulGerhardtWeg, parkStrasse);
        assertThat(noRightTurnPermitted, is("ijbhHuycu@g@Uq@[VeAj@iCTsANoAJiAHsAFuDLoG@_@?YBeGCaAO@C?KBKBKFIJKREf@?d@?h@\\TNb@Ff@?bAMnEKjEOxDWbCc@vCIDMDCB"));

    }
}
