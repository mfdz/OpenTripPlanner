package org.opentripplanner.routing.core;


import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
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
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class CarParkRoutingTest {

    private static final Logger LOG = LoggerFactory.getLogger(CarParkRoutingTest.class);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 07, 9, 15, 0, 0);

    Graph graph = getDefaultGraph();

    static GeometryFactory gf = new GeometryFactory();

    public static Graph getDefaultGraph() {
        var graph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_OSM);
        return addCarParksToGraph(graph);
    }

    private static Graph addCarParksToGraph(Graph graph) {
        var carParks = ImmutableSet.of(
                makeCarPark("1", "Goethestr.", 100, 100, 48.59077, 8.86707),
                makeCarPark("2", "Affstädter Tal", 0, 100, 48.59978, 8.87140)

        );

        var service = new CarParkService();
        graph.putService(CarParkService.class, service);

        carParks.forEach((CarPark carPark) -> {

            var linker = new SimpleStreetSplitter(graph);
            service.addCarPark(carPark);

            var carParkVertex = new ParkAndRideVertex(graph, carPark);
            new ParkAndRideEdge(carParkVertex);
            var envelope = carPark.geometry.getEnvelopeInternal();
            graph.streetIndex
                    .getVerticesForEnvelope(envelope)
                    .stream()
                    .filter(vertex -> vertex instanceof StreetVertex)
                    .filter(vertex -> gf.createPoint(vertex.getCoordinate()).within(carPark.geometry))
                    .peek(vertex -> new ParkAndRideLinkEdge(vertex, carParkVertex))
                    .forEach(vertex -> new ParkAndRideLinkEdge(carParkVertex, vertex));

            if (!(linker.link(carParkVertex, TraverseMode.CAR, null) &&
                    linker.link(carParkVertex, TraverseMode.WALK, null))) {
                LOG.error("{} not near any streets; it will not be usable.", carPark);
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

    private static String calculatePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        var plan = getTripPlan(graph, from, to);

        return firstTripToPolyline(plan);
    }

    private static String firstTripToPolyline(TripPlan plan) {
        Stream<List<Coordinate>> points = plan.itinerary.get(0).legs.stream().map(l -> PolylineEncoder.decode(l.legGeometry));
        return PolylineEncoder.createEncodings(points.flatMap(List::stream).collect(Collectors.toList())).getPoints();
    }

    private static TripPlan getTripPlan(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

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

        var tripPlan = getTripPlan(graph, zwickauerStr, walterKnollStr);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "adrgHez~t@gAW_AO]KKEDcCDkCD{ABw@@_@FwC@S?[F}EK{DMoCEiB@_CLmEBaBFiDCuBGiAK_BYyEQoCEs@GuA?gA?]?_@FAbA]DElA_AJKRs@DQ?O\\ENDDJJ^DNPMTQDWC@AGCGl@e@LIXSVS@A?ONw@DI?CEg@LQ~@o@FKBGLNPv@");
    }

    @Test
    public void driveToDynamicallyAddedCarPark() {
        var zwickauerStr = new GenericLocation(48.59473, 8.84661);
        var hölderlinStr = new GenericLocation(48.59140, 8.86790);

        var tripPlan = getTripPlan(graph, zwickauerStr, hölderlinStr);
        var polyline = firstTripToPolyline(tripPlan);

        assertThatPolylinesAreEqual(polyline, "adrgHez~t@gAW_AO]KKEDcCDkCD{ABw@@_@FwC@S?[F}EK{DMoCEiB@_CLmEBaBFiDCuBGiAK_BYyEQoCEs@GuA?gA?]?_@BuABaAb@uGBe@D_A?_AAaBAyA@m@?s@@cF@g@?k@@M@MBIBIHAF?D@FDB@HDFJHJLPHHJJj@n@PLLJNLPNp@p@NNNPJN`Au@^Sn@W`@Kj@Q`Cq@dBg@j@Il@Ed@?z@?A?{@?e@?[kD");
        assertNull(tripPlan.itinerary.get(0).legs.get(0).alerts);
    }

    @Test
    public void driveToDynamicallyAddedCarParkEvenIfItHasZeroFreeSpaces() {
        var nufringen = new GenericLocation(48.6225, 8.8884);
        var benzStr = new GenericLocation(48.59878, 8.87175);

        var tripPlan = getTripPlan(graph, nufringen, benzStr);
        var polyline = firstTripToPolyline(tripPlan);
        assertThatPolylinesAreEqual(polyline, "arwgHg_gu@Hl@NRPL\\Rf@Lf@T`@Ln@NdBVd@VRHjAh@BBtE~BXLjClAj@XjAz@LLPR`BpCrF~Hv@bAd@n@f@r@l@hA`@bAJLJLFFHBN@JAJAAe@AY?U?S?a@@]@U@[BYBMFi@Fc@PgABSJo@DYrAf@hAb@j@^bBbArAxAjCrDhCpDDJvAbB~CvCTRNNVV`HfHxD|DrDtEx@bAzBpBh@b@NLzAp@`A\\n@RhA\\dAXLDzFrALDn@RVJtAf@~Av@hAz@Pv@l@n@K\\M\\KKEEAC??@BDDJJ`BpBVi@Tg@RQXg@b@{@WYYZA@");

        assertEquals(tripPlan.itinerary.get(0).legs.get(0).alerts.get(0).getAlertUrl(), "alert:carpark:few-spaces-available");
    }
}