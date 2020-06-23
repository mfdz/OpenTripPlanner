package org.opentripplanner.routing.core;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.routing.core.PolylineAssert.assertThatPolylinesAreEqual;

public class BikeRentalRoutingTest {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalUpdater.class);

    static long dateTime = TestUtils.dateInSeconds("Europe/Berlin", 2020, 06, 23, 15, 0, 0);

    public static Graph getDefaultGraph() {
        var graph = TestGraphBuilder.buildGraph(ConstantsForTests.HERRENBERG_OSM);

        var defaultNetworks = ImmutableSet.of("default");
        // Apply stations to graph
        var rentalStation = new BikeRentalStation();
        rentalStation.bikesAvailable = 10;
        rentalStation.y = 48.59438;
        rentalStation.x = 8.86210;
        rentalStation.networks = defaultNetworks;
        rentalStation.id = "1";
        rentalStation.name = new NonLocalizedString("Herrenberg Bahnhof");

        Set<BikeRentalStation> stations = ImmutableSet.of(rentalStation);

        var service = new BikeRentalStationService();
        graph.putService(BikeRentalStationService.class, service);

        stations.forEach((BikeRentalStation station) -> {

            service.addBikeRentalStation(station);

            var vertex = new BikeRentalStationVertex(graph, station);
            var linker = new SimpleStreetSplitter(graph);

            if (!linker.link(vertex)) {
                // the toString includes the text "Bike rental station"
                LOG.warn("{} not near any streets; it will not be usable.", station);
            } else {
                LOG.warn("Added {} to street network.", station);
            }
            new RentABikeOnEdge(vertex, vertex, station.networks);
            new RentABikeOffEdge(vertex, vertex, station.networks);
        });

        return graph;
    }

    private static String calculatePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.BICYCLE);
        new QualifiedMode(TraverseMode.BICYCLE, QualifiedMode.Qualifier.RENT).applyToRoutingRequest(request, false);

        request.setNumItineraries(5);

        request.setRoutingContext(graph);

        request.setOptimize(OptimizeType.QUICK);
        request.walkSpeed = 1.2;
        request.bikeSpeed = 5;
        request.bikeSwitchCost = 20;
        request.walkReluctance = 20;
        request.maxWalkDistance = 100;
        request.walkBoardCost = 60;
        request.setWalkReluctance(2);
        request.wheelchairAccessible = false;


        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);

        Assert.assertTrue(
                "Should contain a bike rental leg, but doesn't.",
                plan.itinerary.get(0).legs.stream().anyMatch(leg -> leg.rentedBike)
        );

        Stream<List<Coordinate>> points = plan.itinerary.get(0).legs.stream().map(l -> PolylineEncoder.decode(l.legGeometry));
        return PolylineEncoder.createEncodings(points.flatMap(List::stream).collect(Collectors.toList())).getPoints();
    }

    @Test
    public void useBikeNetworkRoutesFromNebringenToHerrenberg() {
        var graph = getDefaultGraph();

        var bahnhof = new GenericLocation(48.59385, 8.86399);
        var herrenbergWilhelmstr = new GenericLocation(48.59586, 8.87710);

        var polyline = calculatePolyline(graph, bahnhof, herrenbergWilhelmstr);
        assertThatPolylinesAreEqual(polyline, "q~qgH{fbu@Jf@b@`BMRDf@?BGHMv@?NA@WRYRMHm@d@?ECC??EGm@TEDOE]DAQESSq@?CSo@GSI]O]]aAUe@A?GE?_AAaBAyA@m@?u@@cF@e@?m@@K@MBKBIDGDIFKDINULSHONa@FUJ_@Jo@DSBQJw@DkADi@D{@D_ATsDDu@Am@Ee@AUEi@Eu@C{@YBu@NEOS}@Y}ESsCMcBUyCi@oEAE");
    }
}