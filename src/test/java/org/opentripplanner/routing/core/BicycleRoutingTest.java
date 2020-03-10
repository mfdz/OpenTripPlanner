package org.opentripplanner.routing.core;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.TestUtils;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BicycleRoutingTest {

    static Graph graph;

    static GenericLocation nebringen = new GenericLocation(48.5624, 8.8480);
    static GenericLocation herrenbergBahnhof = new GenericLocation(48.59345, 8.86245);
    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 03, 10, 11, 0, 0);

    @BeforeClass
    public static void setUp() {
        graph = TestGraphBuilder.buildGraph(ConstantsForTests.NEBRINGEN_HERRENBERG_OSM);
    }

    private static RoutingRequest buildRoutingRequest(Graph graph) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = nebringen;
        request.to = herrenbergBahnhof;

        request.modes = new TraverseModeSet(TraverseMode.BICYCLE);

        request.setNumItineraries(1);
        request.setRoutingContext(graph);
        request.setOptimize(OptimizeType.TRIANGLE);
        request.setTriangleSafetyFactor(0.5);
        request.setTriangleSlopeFactor(0.5);
        request.setTriangleTimeFactor(0);

        return request;
    }

    @Test
    public void useBikeNetworkRoutesFromNebringenToHerrenberg() {
        RoutingRequest req = buildRoutingRequest(graph);
        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(req);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, req);
        String polyline = plan.itinerary.get(0).legs.get(0).legGeometry.getPoints();
        assertThat(polyline, is("uykgHoc_u@QSOQOOQ^Qb@Q`@IPIPGNIRKf@AJGXEVCZCTCNGXGXGTCLALG@KBKBIHIHEJENETKn@Kn@W~AIh@Ih@If@EXGPIGMISMWMWMMEOG[Mg@Qg@Qi@SSIICIAM?MAY?k@?s@Ao@?}@CeFMI@iBAyCXmANi@La@NcChBKDq@Xw@JoACQ?yAX[J[LYVWZaA`BW^wAbCoBdBu@`@wAx@gCnAwAx@s@`@e@b@iBjBQVYr@_@hAm@lAKTITGh@Cd@D~@C`@MXIF_@CIGW[wAaE{@mBw@yAi@gAk@aA_AsA[MQIEgAEs@Gk@AQAGAUKcBCg@Em@GCCWMsBQ_DIy@S{A[wAq@kCOs@e@_CQiAImAICOYkAqCm@cAe@u@s@m@q@c@a@Sc@G_@UYKBQM_@Ow@K[OUo@e@]a@_@q@]iA]sA[gAYcA[i@c@_@UIc@CW?MAo@Aq@C_@MOWEQCQEUq@aBONSV]ZWBWO_Aw@o@S_@Eq@I{APo@BYeBEOKq@KuAIqB@mB?m@?G@Q@cAA}AKiCQsBE]Eg@?GC}@?e@?{@AaE?OgAZWF}@b@MA]OW]Uc@c@w@S_@Sg@u@cAgA}@iBqAU]KWYqAMOCFGJ_An@MRDf@?B@D"));
    }

}