package org.opentripplanner.routing.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class BikeRentalRoutingTest {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalRoutingTest.class);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 06, 23, 15, 0, 0);

    Graph graph = getDefaultGraph();

    public static Graph getDefaultGraph() {
        var graph = TestGraphBuilder.buildGraph(
                new String[] { ConstantsForTests.HERRENBERG_AND_AROUND_OSM, ConstantsForTests.BOEBLINGEN_OSM },
                new String[] { ConstantsForTests.HERRENBERG_S1_TRAIN_ONLY }
        );

        return addBikesToGraph(graph);
    }

    private static Graph addBikesToGraph(Graph graph) {
        Set<BikeRentalStation> stations = ImmutableSet.of(
                makeBikeStation("1", "Herrenberg Bahnhof", 48.59438, 8.86210),
                makeBikeStation("2", "Kuppingen", 48.61115, 8.84013),
                makeBikeStation("3", "Herrenberg Meisenweg", 48.5870, 8.8566),
                makeBikeStation("4", "Böblingen Schwabstr.", 48.6866, 9.0285)
        );

        var service = new BikeRentalStationService();
        graph.putService(BikeRentalStationService.class, service);

        stations.forEach((BikeRentalStation station) -> {

            service.addBikeRentalStation(station);

            var vertex = new BikeRentalStationVertex(graph, station);
            var linker = new SimpleStreetSplitter(graph);

            if (!linker.link(vertex)) {
                LOG.warn("Bike station not near any street.");
            } else {
                LOG.info("Added {} to street network.", station);
            }
            new RentABikeOnEdge(vertex, vertex, station.networks);
            new RentABikeOffEdge(vertex, vertex, station.networks);
        });

        graph.getService(BikeRentalStationService.class).setNetworkType("default", BikeRentalStationService.RentalType.STATION_BASED_WITH_TEMPORARY_DROP_OFF);

        return graph;
    }

    private static BikeRentalStation makeBikeStation(String id, String name, double lat, double lon) {
        var defaultNetworks = ImmutableSet.of("default");
        var rentalStation = new BikeRentalStation();
        rentalStation.bikesAvailable = 10;
        rentalStation.y = lat;
        rentalStation.x = lon;
        rentalStation.allowDropoff = true;
        rentalStation.spacesAvailable = 100;
        rentalStation.networks = defaultNetworks;
        rentalStation.id = id;
        rentalStation.name = new NonLocalizedString(name);
        return rentalStation;
    }

    private static String calculatePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        var plan = getTripPlan(graph, from, to);

        //plan.itinerary.get(0).legs.forEach(l -> System.out.println(PolylineAssert.makeUrl(l.legGeometry.getPoints())));
        return firstTripToPolyline(plan);
    }

    private static String firstTripToPolyline(TripPlan plan) {
        Stream<List<Coordinate>> points = plan.itinerary.get(0).legs.stream().map(l -> PolylineEncoder.decode(l.legGeometry));
        return PolylineEncoder.createEncodings(points.flatMap(List::stream).collect(Collectors.toList())).getPoints();
    }

    private static TripPlan getTripPlan(Graph graph, GenericLocation from, GenericLocation to) {
        return getTripPlan(graph, from, to, Optional.empty(), new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.TRANSIT, TraverseMode.WALK));
    }

    private static TripPlan getTripPlan(Graph graph, GenericLocation from, GenericLocation to, Optional<GenericLocation> via, TraverseModeSet modes) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;
        request.intermediatePlaces = via.stream().collect(Collectors.toList());

        request.modes = modes;
        new QualifiedMode(TraverseMode.BICYCLE, QualifiedMode.Qualifier.RENT).applyToRoutingRequest(request, false);

        request.setNumItineraries(5);

        request.setRoutingContext(graph);

        request.setOptimize(OptimizeType.QUICK);
        request.walkSpeed = 1.2;
        request.bikeSpeed = 5;
        request.bikeSwitchCost = 1;
        request.walkReluctance = 20;
        request.maxWalkDistance = 15000;
        request.setWalkReluctance(2);
        request.wheelchairAccessible = false;
        request.setNumItineraries(3);


        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);

        assertTrue(
                "Should contain a bike rental leg, but doesn't.",
                plan.itinerary.get(0).legs.stream().anyMatch(leg -> leg.rentedBike)
        );
        return plan;
    }

    @Test
    public void rentBikeAndRideToDestination() {
        var bahnhof = new GenericLocation(48.59385, 8.86399);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var polyline = calculatePolyline(graph, bahnhof, herrenbergWilhelmstr);
        assertThatPolylinesAreEqual(polyline, "q~qgH{fbu@Jf@b@`BMRDf@?BGHMv@?NA@WRYRMHUPGDOL?ECC????EGm@TEDOE]DAQESSq@?CSo@GSI]O]]aAUe@A?GE?_AAaBAyA@m@?u@@cF@e@?m@@K@MBKBIDGDIFKDINULSHONa@FUJ_@Jo@DSBQJw@@[Bo@Di@D{@D_ATsDDu@Am@Ee@AUEi@Eu@C{@YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");
    }

    @Test
    public void rentBikeCycleToStationAndTakeTrain() {
        var herrenbergImVogelsang = new GenericLocation(48.5867, 8.8549);
        var gärtringen = new GenericLocation(48.64100, 8.90832);

        var polyline = calculatePolyline(graph, herrenbergImVogelsang, gärtringen);
        assertThatPolylinesAreEqual(polyline, "yqpgHan`u@BGTu@XmAg@]Yc@a@iBCIEU??Kg@Oo@s@oDqAyGqAz@GE}@eDo@qAmAsAcDcAIKEBgAZWF}@b@MA]OW]Uc@c@w@S_@Sg@u@cAgA}@iBqAU]KWYqAMOCFGJ_An@MRDf@?B@DR|@C@WYCDLf@MAKEA@WRYRMHUPGDOL?ECC??BB?DNMFETQLIXS@DSqA@CgB{Gg@uAcCmEwAaB}@m@y@c@eBi@yAOuA?mBTyDrA}JdE}DtAwA^y@LuCNyACiCYgCw@wBcAqAy@iBcBgCeD_AaBkBoEo@{BeAiFe@yDuCwYi@mEc@cCm@aC{@cC_AyBkB_DkByBuBeBuHyE}C{BkBcBuBcCsAoBsBoDsO{ZsCmFgDyFoFaIwCsDwByBsh@qe@_I}FgHgEaEoBkOqGkAq@oDiCeB}AyCgDkCiD@EBBBGZ^ABSd@?AY[E@GNA@CB]`@LPBF");
    }

    @Test
    public void rentBikeAfterGettingOffTrain() {
        var gärtringen = new GenericLocation(48.64100, 8.90832);
        var nebringen = new GenericLocation(48.5627, 8.8483);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var polyline1 = calculatePolyline(graph, gärtringen, herrenbergWilhelmstr);
        assertThatPolylinesAreEqual(polyline1, "ee{gH_|ju@CGMQ\\a@BC@AFODA@IECBGfFfGbAdArFtEdEtBtL~E~DnBhHhE~H|Ffb@r_@bIvHpB`CfD~EtDnGfFvJbQl\\pB~CpAbBjBrBbCtBrK~GbBrAvAzAv@`AhBzC~@xBz@dCj@zBd@fCh@fEzC|Zx@vFj@hCn@zBlBrEnB`DvAdBjBdBnAx@xB`AfCx@jCXxABvAE~AQ|DeAlKoEvGaCpBSrAAvAPfBf@rBtAt@t@nBnC`AjCrBnI??b@hAAEYRMHUPGDOL?ECC????EGm@TEDOE]DAQESSq@?CSo@GSI]O]]aAUe@A?GE?_AAaBAyA@m@?u@@cF@e@?m@@K@MBKBIDGDIFKDINULSHONa@FUJ_@Jo@DSBQJw@@[Bo@Di@D{@D_ATsDDu@Am@Ee@AUEi@Eu@C{@YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");

        var polyline2 = calculatePolyline(graph, gärtringen, nebringen);
        assertThatPolylinesAreEqual(polyline2, "ee{gH_|ju@CGMQ\\a@BC@AFODA@IECBGfFfGbAdArFtEdEtBtL~E~DnBhHhE~H|Ffb@r_@bIvHpB`CfD~EtDnGfFvJbQl\\pB~CpAbBjBrBbCtBrK~GbBrAvAzAv@`AhBzC~@xBz@dCj@zBd@fCh@fEzC|Zx@vFj@hCn@zBlBrEnB`DvAdBjBdBnAx@xB`AfCx@jCXxABvAE~AQ|DeAlKoEvGaCpBSrAAvAPfBf@rBtAt@t@nBnC`AjCrBnI??b@hAAEYRMHUPGDOL?ECC??BB?DNMFETQLIXSVS@AJDL@Mg@@EXXB?S_AAE?CEg@LQ~@o@FKBG@MCKMg@GWCOAEAIAIAK?M?ODBDBD@H@B@F?X@X@p@BfB@v@?lA@D?xA@b@?Z@h@?d@?XCTCVCXA`AKdAKbCStBSxAIv@Gz@Ax@Fd@Nx@^dAbAr@pAR`@h@rAl@xBTl@HXf@n@b@Vj@PjAHj@@L?bA@`@?`@EXG`@OjAi@t@UhAYnBa@lB_@tBSZCtGk@xAO|BUrCUl@?h@Bh@FrAPNFPFLDHDF@DBBF@FBBBDBBB@D@F?HLJNHPJTHNBHJZL\\p@jBp@hCrAnF^nAZfAXt@FGLMPXZh@`@h@^b@XVh@^^R`Bv@dAb@h@^tCbANFP@XLzAl@bA\\dA^jA`@p@Rr@TRHLDdErAtAh@b@Hh@T\\HVDd@BT@FJDL@J[`CApA?rB?nD?dA@dJD@j@Pj@Pz@V|@V\\J\\L^L\\Ll@Tr@VJFHDHFFBF@J?DNFJDHHJHLIR");
    }

    @Test
    public void reverseOptimizeBikeRentalTrips() {
        var gärtringen = new GenericLocation(48.64100, 8.90832);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var itinerary = getTripPlan(graph, gärtringen, herrenbergWilhelmstr).itinerary;

        var firstTrip = itinerary.get(0);
        var secondTrip = itinerary.get(1);

        var firstStart = calendarToOffsetDateTime(firstTrip.startTime);
        var secondStart = calendarToOffsetDateTime(secondTrip.startTime);

        assertTrue(firstStart.isBefore(secondStart));

        assertTrue(firstStart.isEqual(OffsetDateTime.parse("2020-07-23T13:05:59Z")));
        assertEquals(secondStart.toString(), "2020-07-23T13:20:59Z");

        var firstArrival = calendarToOffsetDateTime(firstTrip.endTime);
        var secondArrival = calendarToOffsetDateTime(secondTrip.endTime);

        assertTrue(firstArrival.isBefore(secondArrival));
    }

    private OffsetDateTime calendarToOffsetDateTime(Calendar time) {
        return time.toInstant().atOffset(ZoneOffset.UTC);
    }

    @Test
    public void addAlertWhenLastLegIsFreeFloatingDropOff() {
        var gärtringen = new GenericLocation(48.64100, 8.90832);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var itinerary = getTripPlan(graph, gärtringen, herrenbergWilhelmstr).itinerary;

        var firstTrip = itinerary.get(0).legs;
        assertThatBikeLegHasDropOffAlert(firstTrip.get(firstTrip.size() - 1));
    }

    @Test
    public void dontAddAlertForProperFreeFloatingNetwork() {
        graph.getService(BikeRentalStationService.class).setNetworkType("default", BikeRentalStationService.RentalType.FREE_FLOATING);
        var gärtringen = new GenericLocation(48.64100, 8.90832);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var itinerary = getTripPlan(graph, gärtringen, herrenbergWilhelmstr).itinerary;

        var firstTrip = itinerary.get(0).legs;
        var lastLeg = firstTrip.get(firstTrip.size() - 1);
        assertNull(lastLeg.alerts);
    }

    @Test
    public void dropOffNearStation() {
        var böblingenBeethovenstr = new GenericLocation(48.6865, 9.0345);
        var herrenberg = new GenericLocation(48.5934, 8.8629);

        var plan = getTripPlan(graph, böblingenBeethovenstr, herrenberg);

        var polyline = firstTripToPolyline(plan);
        assertThatPolylinesAreEqual(polyline, "uadhHspcv@iAhS@JL`Cx@|IEZOV??GHv@bBNZTT^BlACD~@LfBJrADrACfC@~CWpDMzCIrCEx@QhIGx@Gx@Sp@QVMNNpAVrC?J@pA@bA?@Bf@Df@DX\\hDB\\?P?n@Af@e@xB[nAM`@Wv@Qh@CNCX?p@AXD~AFhAJjAFb@Ht@DXTnAVv@h@xA@?BJED?JCD@N@R@RJbA}@bCKVm@~AOj@Sj@u@vBGPa@lA[|@_@fAKXSf@ADKTELIJA@Yk@Y^EKo@uAKNBD?ACEOPt@`BABUZSXFNNZF@?AfOtZrGzLnBhE~ArEx@fDv@zDlRfgAdKtk@dBvHrCtJpE|Lxy@vwBnPrd@vEhMjAjE`ApF|CnVfAzGjBjGhCdHhBfE|AbCnBzB~PfNhIlGrCdC~DhF~CjGtP`c@pAlCtAbClBxCrAdBvMdPtLrNfFfGbAdArFtEdEtBtL~E~DnBhHhE~H|Ffb@r_@bIvHnB`ChD~ErDnGfFvJdQl\\nB~CpAbBjBrBbCtBtK~GbBrAvAzAt@`AhBzC~@xB|@dCh@zBf@fCh@fEzC|Zx@vFj@hCn@zBjBrEnB`DvAdBjBdBnAx@xB`AfCx@lCXxABtAE`BQ|DeAlKoEvGaCpBSrAAvAPfBf@rBtAt@t@nBnC`AjCrBnI??OgA?Eb@]TOLKRv@LS\\xA");
    }

    private void assertThatBikeLegHasDropOffAlert(Leg leg) {
        assertEquals(leg.mode, "BICYCLE");
        assertTrue(leg.rentedBike);
        assertFalse(leg.alerts.isEmpty());
        assertEquals("Destination is not a designated drop-off area.", leg.alerts.get(0).getAlertHeaderText());
        assertEquals("Rental cannot be completed here. Please check terms & conditions for additional fees.", leg.alerts.get(0).getAlertDescriptionText());
        assertEquals(Alert.AlertId.BIKE_RENTAL_FREE_FLOATING_DROP_OFF, leg.alerts.get(0).alert.alertId);
    }

    @Test
    public void dontAddAlertWhenLeavingInStation() {
        var herrenbergImVogelsang = new GenericLocation(48.5867, 8.8549);
        var gärtringen = new GenericLocation(48.64100, 8.90832);

        var itinerary = getTripPlan(graph, herrenbergImVogelsang, gärtringen).itinerary;

        var firstTrip = itinerary.get(0);
        var secondLeg = firstTrip.legs.get(1);
        assertEquals(secondLeg.mode, "BICYCLE");
        assertNull(secondLeg.alerts);
    }

    @Test
    public void allowViaPoints() {
        var herrenbergImVogelsang = new GenericLocation(48.5867, 8.8549);
        var gärtringen = new GenericLocation(48.64100, 8.90832);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var tripPlan = getTripPlan(graph, herrenbergImVogelsang, gärtringen, Optional.of(herrenbergWilhelmstr), new TraverseModeSet(TraverseMode.WALK));

        var polyline = firstTripToPolyline(tripPlan);

        assertThatPolylinesAreEqual(polyline, "yqpgHan`u@BGTu@XmAg@]Yc@a@iBCIEUKg@Oo@s@oDqAyGq@qCq@kCEQq@eCQc@Qq@G]Gc@E]UgCIeAIiAkAJq@Fe@D_@BQBOa@Ec@K_A[}D[aD[uD]qDYcDEWg@{FkAEmAIKAiCK]U}BaA]Q}@]uAs@[SAm@Ee@AUEi@Eu@C{@YBu@NEOS}@Y}ESsCMcBUyCi@oEAE?A@Dh@nETxCLbBTrCX|ER~@DNt@OXCBz@Dt@Dh@@RDd@@l@Ev@UpDE`AEx@Eh@Cn@AZKv@CPERKn@K^GTO`@INMROTEHGJEHEFCHCJALAL?j@Af@AbF?r@Cl@BxA@`B?~@FD@?Td@\\`AN\\H\\FRRp@?BRp@DR@P\\ENDDEl@UDF??A???EGm@TEDOE]DAQESSq@?CSo@GSI]O]]aAUe@A?GE?_AAaBAyA@m@?u@@cF@e@?m@@K?GAI?ICGAAACCECCG?I@I@M@O@UAK?KAQCMEKGKIMKOOGIKMU]m@kA{@gBPY]q@y@_B?[KYIUU\\Sa@o@mAw@kAWUIUACGKGSGI?ASP??EGkA}AQUSS}@s@iA{@_Bw@uAg@WIo@SMG{FsAMEeAYiA]o@SaA]{Ao@OMi@c@{BqBy@cAsDuEyD}D_HgHYYILmAmAq@o@s@o@_@c@_@e@OO]OMKMWOc@e@m@QSY[cAw@qAeAgBcBq@u@kAmAISW[OOMIUKUGKAKAG?OIWKEBKKKGEAQK}@g@sAc@i@Qe@YUKSY{@]g@SiBu@{@_@c@UKIoAeCEI{@cBS[_CmEeCsFMWgA_CKSSe@_BaDm@gAGSU]i@_A]a@EI{AcCoAoBu@q@m@q@IIc@T_Bv@GSMe@IOa@eA@A?C?CACAAAAC@A@?B?@?BU?QCg@YsAo@u@_@YIAUKIYIm@u@q@w@{@mBo@eB_@}AQYKIi@_@[EMIMWGS}A{Ac@McAAC}@ASKO}@aAoDeDoD{CwBgBCCk@e@KMOAIK{@u@i@_@QMMBEWaA{@aAs@wA_A}@m@KE_Ai@mAk@eBi@[I??a@S]_@QKOKGCSIcAB]GKWII_DgBiAi@qAa@g@W[G?MkAeAo@k@sAeA_B{Ag@m@QMYUQSU[Y[{@aASTIIEEI?]?Ch@?AQk@CI");
    }

    @Test
    public void dontAllowFreeFloatingDropOffWhenDisabled() {
        var herrenbergImVogelsang = new GenericLocation(48.5867, 8.8549);
        var horberStr = new GenericLocation(48.59520, 8.86716);

        graph.getService(BikeRentalStationService.class).setNetworkType("default", BikeRentalStationService.RentalType.STATION_BASED);

        var tripPlan = getTripPlan(graph, herrenbergImVogelsang, horberStr);
        var modes = tripPlan.itinerary.get(0).legs.stream().map(l -> l.mode).collect(Collectors.toList());

        assertEquals(modes, ImmutableList.of("WALK", "BICYCLE", "WALK"));

        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "yqpgHan`u@BGTu@XmAg@]Yc@a@iBCIEU??Kg@Oo@s@oDqAyGqAz@GE}@eDo@qAmAsAcDcAIKEBgAZWF}@b@MA]OW]Uc@c@w@S_@Sg@u@cAgA}@iBqAU]KWYqAMOCFGJ_An@MRDf@?B@DR|@C@WYCDLf@MAKEA@WRYRMHUPGDOL?ECC??BB?DNMFETQLIXSVS@A?OLw@FI?CEg@Sw@Ok@AKCSWeAS{@i@{BCMWmAAe@A[Gu@K]IKSQDOQMk@o@KKIKMQCE");
    }

}