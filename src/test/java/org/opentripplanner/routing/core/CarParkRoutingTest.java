package org.opentripplanner.routing.core;


import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.car_park.CarParkService;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class CarParkRoutingTest {

    private static final Logger LOG = LoggerFactory.getLogger(CarParkRoutingTest.class);

    static Instant now = Instant.now();

    Graph graph = getDefaultGraph();

    static GeometryFactory gf = new GeometryFactory();

    public static Graph getDefaultGraph() {
        var graph = TestGraphBuilder.buildGtfsGraph(ConstantsForTests.HERRENBERG_OSM, ConstantsForTests.HERRENBERG_ONLY_BRONNTOR_BUS_STOP);
        return addCarParksToGraph(graph);
    }

    private static Graph addCarParksToGraph(Graph graph) {
        var danzigerStr  = makeCarPark("danziger-str", "Danziger Str.", 30, 100, 48.59382, 8.85469);
        danzigerStr.openingHours = "Mo-Fr 10:00-12:00";

        var carParks = ImmutableSet.of(
                makeCarPark("1", "Goethestr.", 100, 100, 48.59077, 8.86707),
                makeCarPark("2", "Affstädter Tal", 0, 100, 48.60091, 8.87195),
                danzigerStr
        );

        var service = new CarParkService();
        graph.putService(CarParkService.class, service);

        var linker = new SimpleStreetSplitter(graph);

        carParks.forEach((CarPark carPark) -> {
            ParkAndRideVertex carParkVertex = new ParkAndRideVertex(graph, carPark);
            new ParkAndRideEdge(carParkVertex);
            Envelope envelope = carPark.geometry.getEnvelopeInternal();
            long numberOfVertices = graph.streetIndex
                    .getVerticesForEnvelope(envelope)
                    .stream()
                    .filter(vertex -> vertex instanceof StreetVertex)
                    .filter(vertex -> gf.createPoint(vertex.getCoordinate()).within(carPark.geometry))
                    .peek(vertex -> new ParkAndRideLinkEdge(vertex, carParkVertex))
                    .peek(vertex -> new ParkAndRideLinkEdge(carParkVertex, vertex))
                    .count();
            if (numberOfVertices == 0) {
                if (!(linker.link(carParkVertex, TraverseMode.CAR, null) &&
                        linker.link(carParkVertex, TraverseMode.WALK, null))) {
                    LOG.warn("{} not near any streets; it will not be usable.", carPark);
                }
            }
        });

        return graph;
    }

    private static CarPark makeCarPark(String id, String name, int freeSpaces, int maxCapacity, double lat, double lon) {
        var carPark = new CarPark();
        carPark.y = lat;
        carPark.x = lon;
        carPark.geometry = gf.createPoint(new Coordinate(carPark.x, carPark.y));
        carPark.spacesAvailable = freeSpaces;
        carPark.maxCapacity = maxCapacity;
        carPark.realTimeData = true;
        carPark.id = id;
        carPark.name = new NonLocalizedString(name);
        return carPark;
    }

    private static String firstTripToPolyline(TripPlan plan) {
        Stream<List<Coordinate>> points = plan.itinerary.get(0).legs.stream().map(l -> PolylineEncoder.decode(l.legGeometry));
        return PolylineEncoder.createEncodings(points.flatMap(List::stream).collect(Collectors.toList())).getPoints();
    }

    private static TripPlan getTripPlan(Graph graph, Instant startTime, GenericLocation from, GenericLocation to) {
        return getTripPlan(graph, startTime, false, from, to);
    }

    private static TripPlan getTripPlan(Graph graph, Instant startTime, boolean carParkAvailability, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = startTime.getEpochSecond();
        request.from = from;
        request.to = to;
        request.useCarParkAvailabilityInformation = carParkAvailability;

        request.modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);
        new QualifiedMode(TraverseMode.CAR, QualifiedMode.Qualifier.PARK).applyToRoutingRequest(request, false);
        request.setRoutingContext(graph);
        request.parkAndRide = true;

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);

        var legs = plan.itinerary.get(0).legs;
        var firstLeg = legs.get(0);
        assertEquals(firstLeg.mode, "CAR");
        var lastLeg = legs.get(legs.size() - 1);
        assertEquals(lastLeg.mode, "WALK");

        return plan;
    }

    @Test
    public void driveToStaticParkRide() {
        var zwickauerStr = new GenericLocation(48.59473, 8.84661);
        var walterKnollStr = new GenericLocation(48.59308, 8.86327);

        var tripPlan = getTripPlan(graph, now, zwickauerStr, walterKnollStr);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "adrgHez~t@gAW_AO]KKEDcCDkCD{ABw@@_@FwC@S?[F}EK{DMoCEiB@_CLmEBaBFiDCuBGiAK_BYyEQoCEs@GuA?gA?]?_@FAbA]DElA_AJKRs@DQ?O\\ENDDJJ^DNPMTQDWC@AGCGNMFETQLIXSVS@A?OLw@FI?CEg@LQ~@o@FKBGLNPv@");
    }

    @Test
    public void driveToDynamicallyAddedCarPark() {
        var zwickauerStr = new GenericLocation(48.59473, 8.84661);
        var hölderlinStr = new GenericLocation(48.59140, 8.86790);

        var tripPlan = getTripPlan(graph, now, zwickauerStr, hölderlinStr);
        var polyline = firstTripToPolyline(tripPlan);

        assertThatPolylinesAreEqual(polyline, "adrgHez~t@gAW_AO]KKEDcCDkCD{ABw@@_@FwC@S?[F}EK{DMoCEiB@_CLmEBaBFiDCuBGiAK_BYyEQoCEs@GuA?gA?]?_@BuABaAb@uGBe@D_A?_AAaBAyA@m@?s@@cF@g@?k@@M@MBIBIHAF?D@FDB@HDFJHJLPHHJJj@n@PLLJNLPNp@p@NNNPJN`Au@^Sn@W`@Kj@Q`Cq@dBg@j@Il@Ed@?z@?A?{@?e@?[kD");
        assertNull(tripPlan.itinerary.get(0).legs.get(0).alerts);
    }

    @Test
    public void driveToDynamicallyAddedCarParkEvenIfItHasZeroFreeSpaces() {
        var nufringen = new GenericLocation(48.6225, 8.8884);
        var affstädterTal = new GenericLocation(48.60143, 8.87084);

        var tripPlan = getTripPlan(graph, now, nufringen, affstädterTal);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "arwgHg_gu@Hl@NRPL\\Rf@Lf@T`@Ln@NdBVd@VRHjAh@BBtE~BXLjClAj@XjAz@LLPR`BpCrF~Hv@bAd@n@f@r@l@hA`@bAJLJLFFHBN@JAJAAe@AY?U?S?a@@]@U@[BYBMFi@Fc@PgABSJo@DYrAf@hAb@j@^bBbArAxAjCrDhCpDDJvAbB~CvCTRNNVV`HfHxD|DrDtEx@bAzBpBh@b@NLzAp@`A\\n@RhA\\dAXLDzFrALDn@RVJtAf@~Av@hAz@Pv@l@n@K\\M\\KKEEq@q@US]W}@m@w@YWv@UnAc@YQtAQxA");

        var alert = tripPlan.itinerary.get(0).legs.get(0).alerts.get(0);
        assertEquals("Few parking spaces available", alert.getAlertHeaderText());
        assertEquals("The selected car park has only few spaces available. Please add extra time to your trip.", alert.getAlertDescriptionText());
        assertEquals(Alert.AlertId.CAR_PARK_FULL, alert.alert.alertId);
    }

    @Test
    public void shouldNotAddAlertIfRequestInTheFuture() {
        var nufringen = new GenericLocation(48.6225, 8.8884);
        var affstädterTal = new GenericLocation(48.60143, 8.87084);

        var tomorrow = OffsetDateTime.now().plusDays(1).toInstant();

        var tripPlan = getTripPlan(graph, tomorrow, nufringen, affstädterTal);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "arwgHg_gu@Hl@NRPL\\Rf@Lf@T`@Ln@NdBVd@VRHjAh@BBtE~BXLjClAj@XjAz@LLPR`BpCrF~Hv@bAd@n@f@r@l@hA`@bAJLJLFFHBN@JAJAAe@AY?U?S?a@@]@U@[BYBMFi@Fc@PgABSJo@DYrAf@hAb@j@^bBbArAxAjCrDhCpDDJvAbB~CvCTRNNVV`HfHxD|DrDtEx@bAzBpBh@b@NLzAp@`A\\n@RhA\\dAXLDzFrALDn@RVJtAf@~Av@hAz@Pv@l@n@K\\M\\KKEEq@q@US]W}@m@w@YWv@UnAc@YQtAQxA");

        assertNull(tripPlan.itinerary.get(0).legs.get(0).alerts);
    }

    @Test
    public void shouldExcludeFullCarParksWhenParamSet() {
        var nufringen = new GenericLocation(48.6225, 8.8884);
        var affstädterTal = new GenericLocation(48.60143, 8.87084);

        var tripPlan = getTripPlan(graph, now, true, nufringen, affstädterTal);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "arwgHg_gu@Hl@NRPL\\Rf@Lf@T`@Ln@NdBVd@VRHjAh@BBtE~BXLjClAj@XjAz@LLPR`BpCrF~Hv@bAd@n@f@r@l@hA`@bAJLJLFFHBN@JAJABv@HxBB`@Bn@@l@?^?ZAp@An@Cn@Ej@E`@In@G^E^GXMh@Mh@M`@IXKVKZMXMVUb@U^OTQTORa@f@YZYZu@v@YZWZSTMPKNOTQVMVOZMTKXK\\IVIZI\\I^EZE\\Ed@Gv@Cf@?r@?d@@`@@b@D`@B`@B\\LzAH~@H~@Bp@Dn@D|@Bz@DlABnABlABjA@t@@v@@t@?t@@h@?h@?`AA~@?`AAh@@h@?h@?d@@~@B|@B`AD~@F`AH~AH~AHlAFlAFrADpAH|ABx@Dt@Dv@Dr@Fv@Ft@Db@DXD\\Hd@FXBNNn@J`@H\\^vAPl@Pn@Lj@Nl@Pn@Nn@Ll@Ll@Np@Nt@BLBZDZB\\@HEFCHCH?D?F?JBLDJDHHDF?@?HADCBC@ABGBG@I@IHEFCLGXOf@IXGXI~@Qd@KXGf@MRGVIPGRKRITKNKVOd@WRMTMRIRITINGVGRERCTATCV?T@T@P@TBRDTFPFTHNHNHLFNPNPDDDFBF?F?H@F@F@FBDDFDBD@D@F?DCDCHDHDNLRPJRHLHPJRJVRh@Vp@Rh@Rb@LXNVPZNRNPPRRPRPRLNJTJXLRFTFRBVBTBV?RARAPAXGREXKRGNIXORQPONMRSRWb@i@PU\\e@f@u@^m@`@g@NSPSVWNKLKTQVMVMRIPGVGd@Kn@KVGRG\\MRKXQZUPQRQRURSNQNORKJGHGFCDBFBH?B?BAFEDK@ABI@M?OCMEKEE?M?K?O?S@[BUDQDQDOHUHSLUTa@V]LUJQJSHQFQHUFUHUFWFYDWB]Dq@Bi@@m@B}B?cA?_D?o@h@?n@Cd@G\\ILAj@WdAo@v@e@^Ql@Y\\GXEX?F@r@B|@Jf@BdAGT?PIFAbA]DElA_AJKRs@DQ?Q\\ENDDJJ^DNPMTQA?UPQLEOK_@EKOE]DAQESSq@?CSo@GSI]O]]aAUe@A?GE?_AAaBAyA@m@?u@O@@aE?i@@_@?WIQGMGGGASGSIC?MCMEOKMIKGKIMKOOGIKMU]m@kA{@gBiAyBe@y@MSOWEGYe@S[GMi@{@Wc@OWCAQYKQUf@Wh@EJ]f@SZ[d@EFa@d@QPWZeBiBm@YUK]KUMa@Y_@Y");

        assertNull(tripPlan.itinerary.get(0).legs.get(0).alerts);
    }

    @Test
    public void shouldExcludeCarParkWhenClosed() {
        var berlinerStr = new GenericLocation(48.59418, 8.85091);
        var schwerinerStr = new GenericLocation(48.59539, 8.85296);

        var elevenOclock= OffsetDateTime.parse("2020-08-10T11:00:00+02:00"); // before car park closes
        var oneOclock= elevenOclock.plusHours(2); // after car park closes

        var tripPlan = getTripPlan(graph, elevenOclock.toInstant(), true, berlinerStr, schwerinerStr);
        var polyline1 = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline1, "s`rgHeu_u@FQj@wA\\uAJo@Hs@FkA?aBCe@Gm@Iw@Oo@Sk@OYPOPM??QLQNOWGIKP[_@EZKPMHICCQGHO?}BIO?CdCEZCvBFn@");

        assertNull(tripPlan.itinerary.get(0).legs.get(0).alerts);

        var tripPlan2 = getTripPlan(graph, oneOclock.toInstant(), true, berlinerStr, schwerinerStr);
        var polyline2 = firstTripToPolyline(tripPlan2);
        assertThatPolylinesAreEqual(polyline2, "s`rgHeu_u@FQj@wA\\uAJo@Hs@FkA?aBCe@Gm@Iw@Oo@Sk@OYOWGIGIKIQQMIc@]i@Y_@ScAc@k@SFiDCuBGiAK_BYyEQoCEs@GuA?gA?]?_@FAbA]DEnA_AHKTs@DQ?O\\ENDDJJ^DNPMTQA?UPQLEOK_@EKKFKDCFa@xAAHGHKFO?IHi@\\e@Ra@XGb@@p@Bz@D~@DdAFz@Dn@JCd@jHFHDvC@~@CvALv@?^?T?LCx@Al@Ad@O@CdCEZAvBFn@");

    }

    @Test
    public void shouldAddAlertWhenCarParkClosesSoon() {
        var berlinerStr = new GenericLocation(48.59418, 8.85091);
        var schwerinerStr = new GenericLocation(48.59539, 8.85296);

        var quarterToTwelve = OffsetDateTime.parse("2020-08-10T11:45:00+02:00");

        var tripPlan = getTripPlan(graph, quarterToTwelve.toInstant(), true, berlinerStr, schwerinerStr);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "s`rgHeu_u@FQj@wA\\uAJo@Hs@FkA?aBCe@Gm@Iw@Oo@Sk@OYPOPM??QLQNOWGIKP[_@EZKPMHICCQGHO?}BIO?CdCEZCvBFn@");

        var alert = tripPlan.itinerary.get(0).legs.get(0).alerts.get(0);
        assertEquals(alert.alert.alertId, Alert.AlertId.CAR_PARK_CLOSING_SOON);
    }
}