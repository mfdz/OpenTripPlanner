package org.opentripplanner.routing.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.GermanyWayPropertySetSource;
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
import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class SplitEdgeTurnRestrictionsTest {

    // Deufringen
    static GenericLocation hardtheimerWeg = new GenericLocation(48.67765, 8.87212);
    static GenericLocation steinhaldenWeg = new GenericLocation(48.67815, 8.87305);
    static GenericLocation k1022 = new GenericLocation(48.67846, 8.87021);

    // Böblingen
    static GenericLocation paulGerhardtWegEast = new GenericLocation(48.68363, 9.00728);
    static GenericLocation paulGerhardtWegWest = new GenericLocation(48.68297, 9.00520);
    static GenericLocation parkStrasse = new GenericLocation(48.68358, 9.00826);
    static GenericLocation herrenbergerStrasse = new GenericLocation(48.68497, 9.00909);
    static GenericLocation steinbeissWeg = new GenericLocation(48.68172, 9.00599);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 03, 3, 7, 0, 0);

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
        List<Leg> legs = plan.itinerary.get(0).legs;


        assertThat(legs.size(), is(1));
        Leg leg = legs.get(0);
        assertThat(leg.mode, is("CAR"));
        return leg.legGeometry.getPoints();
    }

    @Test
    public void shouldTakeDeufringenTurnRestrictionsIntoAccount() throws IOException {
        Graph graph = TestGraphBuilder.buildGtfsGraph(ConstantsForTests.DEUFRINGEN_OSM, ConstantsForTests.DEUFRINGEN_GTFS);
        // https://www.openstreetmap.org/relation/10264251 has a turn restriction so when leaving Hardtheimer Weg
        // you must turn right and take the long way to Steinhaldenweg.
        // on top of this, it has a bus stop so this test also makes sure that the turn restrictions work
        // even when the streets are split.
        String noRightTurnPermitted = computeCarPolyline(graph, hardtheimerWeg, steinhaldenWeg);
        assertThatPolylinesAreEqual(noRightTurnPermitted, "ijbhHuycu@g@Uq@[e@|BENGVYxA]xAYz@Yp@Yj@^n@JDN_@?Wa@i@Xq@X{@\\yAXyACGAIB]j@_DPaA@e@MDCB");

        // when to drive in reverse direction it's fine to go this way
        String leftTurnOk = computeCarPolyline(graph, steinhaldenWeg, hardtheimerWeg);
        assertThat(leftTurnOk, is("kmbhHo_du@BCLEAd@Q`Ak@~CC\\@HBFFWDOd@}Bp@Zf@T"));

        // make sure that going straight on a straight-only turn direction also works
        String straightAhead = computeCarPolyline(graph, hardtheimerWeg, k1022);
        assertThat(straightAhead, is("ijbhHuycu@g@Uq@[e@|BENGVYxA]xAXn@Hd@"));

        String straightAheadBack = computeCarPolyline(graph, k1022, hardtheimerWeg);
        assertThat(straightAheadBack, is("kobhHwmcu@Ie@Yo@\\yAXyAFWDOd@}Bp@Zf@T"));

        // make sure that turning left onto the minor road works even when the opposite direction has a straight-only
        // restriction
        String leftTurnAllowed = computeCarPolyline(graph, k1022, steinhaldenWeg);
        assertThat(leftTurnAllowed, is("kobhHwmcu@Ie@Yo@\\yAXyACGAIB]j@_DPaA@e@MDCB"));

        String rightTurnAllowed = computeCarPolyline(graph, steinhaldenWeg, k1022);
        assertThat(rightTurnAllowed, is("kmbhHo_du@BCLEAd@Q`Ak@~CC\\@HBFYxA]xAXn@Hd@"));
    }

    @Test
    public void shouldTakeBoeblingenTurnRestrictionsIntoAccount() throws IOException {
        // this tests that the following turn restriction is transferred correctly to the split edges
        // https://www.openstreetmap.org/relation/299171
        Graph graph = TestGraphBuilder.buildGtfsGraph(ConstantsForTests.BOEBLINGEN_OSM, ConstantsForTests.BOEBLINGEN_GTFS);

        // turning left from the main road onto a residential one
        String turnLeft = computeCarPolyline(graph, parkStrasse, paulGerhardtWegEast);
        assertThatPolylinesAreEqual(turnLeft, "kochHsl~u@HQL]N_@v@mBDKN]KKM\\{@~BKXWj@KRKPCFYj@DP^lAJX");

        // right hand turn out of the the residential road onto the main road, only right turn allowed plus there
        // is a bus station along the way, splitting the edge
        String noLeftTurnPermitted = computeCarPolyline(graph, paulGerhardtWegEast, parkStrasse);
        assertThat(noLeftTurnPermitted, is("sochHof~u@KY_@mAVi@Te@DK"));

        // right hand turn out of the the residential road onto the main road, only right turn allowed plus there
        // is a bus station along the way, splitting the edge
        String longWay = computeCarPolyline(graph, paulGerhardtWegEast, herrenbergerStrasse);
        assertThatPolylinesAreEqual(longWay, "sochHof~u@KY_@mAVi@Te@N]L]N_@v@mBDKN]KKM\\{@~BKXWj@KRKPCFa@`@_@XWPSHQDMCEAQMKKSgAa@qCMe@");

        String longWayBack = computeCarPolyline(graph, herrenbergerStrasse, paulGerhardtWegEast);
        assertThatPolylinesAreEqual(longWayBack, "axchHwq~u@G_@Qc@CGGKGIQKKEKCWC]Am@EWC[CYGYGg@QSQQKKGMCEIGCG@GBEFCH?H?H@HDHFDD?F?DEDGPBTFVLNBPDNDRDr@Hz@FF@l@JVFLFNPFLFLDNDJDZHb@Lx@d@dDBTBRTLNBTARKpAiA^lAJX");

        // test that you can correctly turn right here https://www.openstreetmap.org/relation/415123 when approaching
        // from south
        String fromSouth = computeCarPolyline(graph, steinbeissWeg, paulGerhardtWegWest);
        assertThat(fromSouth, is("wcchHk~}u@Fd@Hj@o@\\{@b@KFyBlAWmA"));
        String toSouth = computeCarPolyline(graph, paulGerhardtWegWest, steinbeissWeg);
        assertThat(toSouth, is("okchHoy}u@VlAxBmAJGz@c@n@]Ik@Ge@"));

        // test that you cannot turn left here https://www.openstreetmap.org/relation/415123 when approaching
        // from north
        String fromNorth = computeCarPolyline(graph, paulGerhardtWegWest, herrenbergerStrasse);
        assertThat(fromNorth, is("okchHoy}u@VlA{BlAIBOLCBIDc@{AYiAM_@Kc@K_@I_@Ia@Ga@Gc@Gc@Ei@EYAIKaAEe@CQCSIm@SgAa@qCMe@"));

        // this doesn't actually work!
        // when you approach you cannot turn left so you have to take a long way but it seems that OTP gives up beforehand!
        //String toNorth = computeCarPolyline(graph, herrenbergerStrasse, paulGerhardtWegWest);
        //assertThat(toNorth, is("???"));
    }

    @Test
    public void shouldBeAbleToRouteReinholdSchickPlatz() throws IOException {
        Graph graph = TestGraphBuilder.buildGtfsGraph(ConstantsForTests.HERRENBERG_OSM, ConstantsForTests.HERRENBERG_ONLY_BRONNTOR_BUS_STOP);

        var hindenburgStr = new GenericLocation(48.59532, 8.86777);
        var seeStr = new GenericLocation(48.59640, 8.86744);
        var horberStr = new GenericLocation(48.59491, 8.86676);
        var gisiloStrGueltstein = new GenericLocation(48.5748987, 8.8788304);

        var polyline1 = computeCarPolyline(graph, hindenburgStr, seeStr);
        assertThatPolylinesAreEqual(polyline1, "ugrgHo~bu@EHGLIFGHGDIHGFGDIFI@I@MBO@K?IAKAKAQCMEKGA?");

        var polyline2 = computeCarPolyline(graph, seeStr, horberStr);
        assertThatPolylinesAreEqual(polyline2, "onrgHm|bu@@?JFLHNJLDLBB@T?NAN?FANCFAB?JAHAF?D@FDB@HDFJHJLPHJJJTV");

        var polyline3 = computeCarPolyline(graph, gisiloStrGueltstein, seeStr);
        assertThatPolylinesAreEqual(polyline3, "ahngHuceu@OoAKe@]n@MTWd@i@x@m@j@m@Xi@Ja@Ds@E]MkA_AeA}AMQMUU]MYM[IOSa@gBcDo@gASYc@k@Y][]sBiAu@a@[][a@]u@S_@]y@a@s@[_@i@m@[M]OsA]gCi@}BW_H]}DE]Gk@e@W]OECFk@pAWj@[l@S^U`@U`@W^W`@Y`@Y^WZWZc@j@i@n@g@r@q@x@s@`AW^Y^g@t@[f@S\\SX[h@Yf@Yh@c@v@a@v@a@x@U`@MVWf@S`@e@|@Yl@Wd@OXw@|As@tAk@bAU`@wBfEQ\\[f@GJ[b@}A|B]d@U\\U\\IPUh@KXAHCn@Bz@Dt@Dh@@TDd@@l@Et@UrDE~@Ez@Eh@Cn@AZKv@CPEREPMVKVMXMVS^GLIFGHGDIHGFGDIFI@I@MBO@K?IAKAKAQCMEKGA?");
    }
}
