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

public class CarRoutingTest {

    static Graph graph;

    static GenericLocation seeStrasse = new GenericLocation(48.59724504108028,8.868606090545656);
    static GenericLocation offTuebingerStr = new GenericLocation(48.58529481682537, 8.888196945190431);
    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 03, 2, 7, 0, 0);

    @BeforeClass
    public static void setUp() {
        graph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_OSM);
    }

    private static RoutingRequest buildRoutingRequest(Graph graph) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = seeStrasse;
        request.to = offTuebingerStr;

        request.modes = new TraverseModeSet(TraverseMode.CAR);

        request.setNumItineraries(1);
        request.setRoutingContext(graph);

        return request;
    }

    @Test
    public void routeToHighwayTrack() {
        RoutingRequest req = buildRoutingRequest(graph);
        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(req);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, req);
        String polyline = plan.itinerary.get(0).legs.get(0).legGeometry.getPoints();
        assertThat(polyline, is("usrgHyccu@d@bAl@jAT\\JLFHNNLJJHJFLHNJLDPDT?NAN?FANCFAB?JADGDIFKDINULSHONa@FUJ_@Jo@DSBQJw@DkADi@D{@D_ATsDDu@Am@Ee@AUEi@Eu@C{@Bo@@IJYTi@HQT]T]\\e@|A}BZc@FKZg@P]vBgETa@j@cAr@uAv@}ANYVe@Xm@d@}@Ra@Vg@LWTa@`@y@`@w@b@w@Xi@Xg@Zi@RYR]Zg@f@u@X_@V_@r@aAp@y@f@s@h@o@b@k@V[V[X_@Xa@Va@V_@Ta@Ta@R_@Zm@Vk@j@qABGHUN_@Na@Xw@Vu@b@wANk@Pm@z@_DRu@"));
    }

}