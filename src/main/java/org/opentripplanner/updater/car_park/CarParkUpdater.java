package org.opentripplanner.updater.car_park;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.car_park.CarParkService;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic bike-rental station updater which updates the Graph with bike rental stations from one
 * BikeRentalDataSource.
 */
public class CarParkUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(CarParkUpdater.class);
    private final CarParkDataSource source;
    private final Map<CarPark, ParkAndRideVertex> verticesByPark = new HashMap<>();
    private final Map<CarPark, DisposableEdgeCollection> tempEdgesByStation = new HashMap<>();

    private GraphUpdaterManager updaterManager;
    private VertexLinker linker;

    private CarParkService service;

    private StreetVertexIndex streetIndex;

    private final GeometryFactory gf = new GeometryFactory();

    public CarParkUpdater(CarParkUpdaterParameters parameters) throws IllegalArgumentException {
        super(parameters);
        // Configure updater
        LOG.info("Setting up car park updater.");

        this.source = parameters.sourceParameters();

        if (pollingPeriodSeconds <= 0) {
            LOG.info("Creating car park updater running once only (non-polling): {}", source);
        }
        else {
            LOG.info(
                    "Creating car park updater running every {} seconds: {}", pollingPeriodSeconds,
                    source
            );
        }
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Creation of network linker library will not modify the graph
        linker = graph.getLinker();
        // Adding a car park station service needs a graph writer runnable
        service = graph.getService(CarParkService.class, true);
    }

    @Override
    public void teardown() {
    }

    @Override
    protected void runPolling() {
        LOG.debug("Updating car parks from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        var carParks = source.getCarParks();

        // Create graph writer runnable to apply these stations to the graph
        CarParkGraphWriterRunnable graphWriterRunnable = new CarParkGraphWriterRunnable(carParks);
        updaterManager.execute(graphWriterRunnable);
    }

    private class CarParkGraphWriterRunnable implements GraphWriterRunnable {

        private final List<CarPark> carParks;

        private CarParkGraphWriterRunnable(List<CarPark> carParks) {
            this.carParks = carParks;
        }

        public void run(Graph graph) {
            // Apply stations to graph
            var stationSet = new HashSet<>();

            /* add any new stations and update bike counts for existing stations */
            for (CarPark station : carParks) {
                service.addCarPark(station);
                stationSet.add(station);
                var carParkVertex = verticesByPark.get(station);
                if (carParkVertex == null) {
                    carParkVertex = new ParkAndRideVertex(graph, station);
                    DisposableEdgeCollection tempEdges = linker.linkVertexForRealTime(
                            carParkVertex,
                            TraverseMode.WALK,
                            LinkingDirection.BOTH_WAYS,
                            (vertex, streetVertex) -> List.of(
                                    new StreetBikeRentalLink((BikeRentalStationVertex) vertex, streetVertex),
                                    new StreetBikeRentalLink(streetVertex, (BikeRentalStationVertex) vertex)
                            )
                    );
                    if (carParkVertex.getOutgoing().isEmpty()) {
                        // the toString includes the text "Bike rental station"
                        LOG.info("BikeRentalStation {} is unlinked", carParkVertex);
                    }
                    tempEdges.addEdge(new ParkAndRideEdge(carParkVertex));
                    verticesByPark.put(station, carParkVertex);
                    tempEdgesByStation.put(station, tempEdges);
                } else {
                    carParkVertex.updateCapacity(station.maxCapacity, station.spacesAvailable);
                }
            }
            /* remove existing stations that were not present in the update */
            var toRemove = new ArrayList<CarPark>();
            for (Entry<CarPark, ParkAndRideVertex> entry : verticesByPark.entrySet()) {
                var station = entry.getKey();
                if (stationSet.contains(station))
                    continue;
                toRemove.add(station);
                service.removeCarPark(station);
            }
            for (CarPark station : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByPark.remove(station);
                tempEdgesByStation.get(station).disposeEdges();
                tempEdgesByStation.remove(station);
            }
        }
    }

}
