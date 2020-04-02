package org.opentripplanner.updater.stoptime;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * rt.type = stop-time-updater
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 */
public class CarpoolTripUpdater implements GraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(CarpoolTripUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private GraphUpdaterManager updaterManager;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph _graph) {
        updaterManager.execute(graph -> {
            // Only create a realtime data snapshot source if none exists already
            TimetableSnapshotSource snapshotSource = graph.timetableSnapshotSource;
            if (snapshotSource == null) {
                snapshotSource = new TimetableSnapshotSource(graph);
                // Add snapshot source to graph
                graph.timetableSnapshotSource = (snapshotSource);
            }
        });
    }

    @Override
    public void run() throws Exception {
        LOG.info("Starting carpool trip updater");
        updaterManager.execute(graph -> {
            GtfsRealtime.TripDescriptor tripDescriptor = GtfsRealtime.TripDescriptor.newBuilder().setTripId("carpool-update-" + OffsetDateTime.now()).build();

            Stop start = new Stop();
            start.setId(FeedScopedId.convertFromString("1:herrenberg"));
            start.setName("Herrenberg Bahnhof");
            start.setLat(48.5938);
            start.setLon(8.8627);

            Stop end = new Stop();
            end.setId(FeedScopedId.convertFromString("1:ehningen"));
            end.setName("Ehningen Bahnhof");
            end.setLat(48.66203);
            end.setLon(8.94332);
            new TransitStop(graph, end);
            Arrays.asList(start, end).forEach(stop ->{
                TransitStop transitStop = new TransitStop(graph, stop);
                graph.index.stopVertexForStop.put(stop, transitStop);
            });

            ZonedDateTime threeOclock = LocalDate.now().atStartOfDay().plusHours(15).atZone(ZoneId.of("Europe/Berlin"));

            List<TripUpdate.StopTimeUpdate> stopTimeUpdates = Arrays.asList(buildStopTime(threeOclock), buildStopTime(threeOclock.plusMinutes(30)));

            TripUpdate update = TripUpdate.newBuilder().setTrip(tripDescriptor).addAllStopTimeUpdate(stopTimeUpdates).build();

            boolean result = graph.timetableSnapshotSource
                    .validateAndHandleAddedTrip(graph, update, "1", ServiceDate.parseString(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)));


            if(result) {
                LOG.info("Trip added to graph");
            } else {
                LOG.info("Trip failed to be added to graph");
            }
        });
    }

    private TripUpdate.StopTimeUpdate buildStopTime(ZonedDateTime time) {
        return TripUpdate.StopTimeUpdate.newBuilder().setDeparture(TripUpdate.StopTimeEvent.newBuilder().setTime(time.toEpochSecond()).build()).build();
    }

    @Override
    public void teardown() {
    }

    @Override
    public void configure(Graph graph, JsonNode jsonNode) throws Exception {

    }
}
