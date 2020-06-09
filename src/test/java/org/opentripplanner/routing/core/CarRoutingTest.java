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

import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class CarRoutingTest {

    static Graph ordinaryHerrenbergGraph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_OSM);
    static Graph hindenburgStrUnderConstruction = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_HINDENBURG_UNDER_CONSTRUCTION_OSM);
    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 03, 2, 7, 0, 0);

    @BeforeClass
    public static void setUp() {
    }

    private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);

        request.setNumItineraries(5);
        request.setRoutingContext(graph);

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
        return plan.itinerary.get(0).legs.get(0).legGeometry.getPoints();
    }

    @Test
    public void routeToHighwayTrack() {
        GenericLocation seeStrasse = new GenericLocation(48.59724504108028,8.868606090545656);
        GenericLocation offTuebingerStr = new GenericLocation(48.58529481682537, 8.888196945190431);

        var polyline = computePolyline(ordinaryHerrenbergGraph, seeStrasse, offTuebingerStr);

        assertThatPolylinesAreEqual(polyline, "usrgHyccu@d@bAl@jAT\\JLFHNNLJJHJFLHNJLDPDT?NAN?FANCFAB?JADGDIFKDINULSHONa@FUJ_@Jo@DSBQJw@DkADi@D{@D_ATsDDu@Am@Ee@AUEi@Eu@C{@Bo@@IJYTi@HQT]T]\\e@|A}BZc@FKZg@P]vBgETa@j@cAr@uAv@}ANYVe@Xm@d@}@Ra@Vg@LWTa@`@y@`@w@b@w@Xi@Xg@Zi@RYR]Zg@f@u@X_@V_@r@aAp@y@f@s@h@o@b@k@V[V[X_@Xa@Va@V_@Ta@Ta@R_@Zm@Vk@j@qABGHUN_@Na@Xw@Vu@b@wANk@Pm@z@_DRu@");
    }

    @Test
    public void shouldNotRouteAcrossParkingLot() {
        var nagolderStr = new GenericLocation(48.59559, 8.86472);
        var horberstr = new GenericLocation(48.59459, 8.86647);

        var polyline = computePolyline(ordinaryHerrenbergGraph, nagolderStr, horberstr);

        assertThatPolylinesAreEqual(polyline, "mirgHmkbu@Au@@m@?s@@cF@g@?k@@M@MBKBIHAF?D@FDB@HDFJHJLPHJJJj@n@PLLJHF");
    }

    @Test
    public void shouldBeAbleToTurnIntoAufDemGraben() {
        var gueltsteinerStr = new GenericLocation(48.59240, 8.87024);
        var aufDemGraben = new GenericLocation(48.59487, 8.87133);

        var polyline = computePolyline(hindenburgStrUnderConstruction, gueltsteinerStr, aufDemGraben);

        assertThatPolylinesAreEqual(polyline, "ouqgH}mcu@gAE]U}BaA]Q}@]uAs@[SAm@Ee@AUEi@XEQkBQ?Bz@Dt@Dh@@TGBC@KBSHGx@");
    }

    @Test
    public void shouldTakeDetoursInAlzentalIntoAccount() {
        var nagoldStr = new GenericLocation(48.59559, 8.86472);
        var aufDemGraben = new GenericLocation(48.59487, 8.87133);

        var polyline1 = computePolyline(hindenburgStrUnderConstruction, nagoldStr, aufDemGraben);
        assertThatPolylinesAreEqual(polyline1, "mirgHmkbu@Au@@m@?s@@cF@g@?k@@M@MBKBIHAF?D@FDB@HDFJHJLPHJJJj@n@PLLJNLPNp@p@NNNPJNDHFLHTTdA^zADNDLFH@BBDJLDBDBDBH@B@F?X@X@p@BfB@v@?lA@D?xA@b@?Z@h@?d@?[}D[cD[uD]qDYcDEWg@{FkAEmAIKAiCK]U}BaA]Q}@]uAs@[SAm@Ee@AUEi@XEQkBQ?Bz@Dt@Dh@@TGBC@KBSHGx@");

        var polyline2 = computePolyline(hindenburgStrUnderConstruction, aufDemGraben, nagoldStr);
        assertThatPolylinesAreEqual(polyline2, "}drgHytcu@Fy@RIJCBAFCDd@@l@ZRtAr@|@\\\\P|B`A\\T?dDHn@l@hE?BZlCVjCV`CFv@BZBXHr@@PBTBP@N?N?J@L?bAw@?gBAq@CYAYAG?CAIAECECECKMCEACGIEMEO_@{AUeAIUGMEIKOOQOOq@q@QOOMMKQMk@o@KKIKMQIKGKKMIGCEGCGAA?EAG@EJA@CHALAJ?R?T?VA^?h@A`E?zB@XBnCInB]jFGrAGdAC|APIBuABaAb@uGBe@D_A?_AAaB?c@");
    }
}