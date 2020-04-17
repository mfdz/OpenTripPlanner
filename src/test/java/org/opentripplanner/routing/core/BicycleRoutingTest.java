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

    static GenericLocation nebringen = new GenericLocation(48.56494, 8.85318);
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
        assertThat(polyline, is("{ilgHgc`u@IGC[y@?@qAZaCAKEMGKSCe@CWEOe@GGgCoNa@mEWqCo@eHI_Ay@wIYeDo@kHe@yEu@gGm@aFWmBkHgEKCeC\\gFn@M_@NoAWAOGSWRi@zAsFYMIOc@aAEIo@yAGIG\\IZ]p@e@v@u@|@m@f@cCn@?pBGt@Kx@[fB]bBYx@c@`Ag@x@g@b@S@YEa@Ge@OeBw@eAa@a@Cc@JiBv@_@P]FS@o@KsAg@qAe@e@Co@HyBh@MFMHKLEPCLi@Po@PqAT_@N_Ad@c@Nk@LYJ]JU@i@?w@Cg@?G@K?G??[COGOkAwBs@cBK]]oAa@mAKu@]{ByAtAULg@Hq@Aq@EsCQa@CC?qB@]BkAJ_C^eCCqBGP|AkA\\IVw@Ry@L_CZDVcB`@wA`@iBh@?ByBv@oCt@q@PQFY`@Wp@CJKZ]dANLERJ\\Ft@@Z@d@VlABLh@zBRz@VdABR@LNj@Rv@Df@?B@D"));
    }

}