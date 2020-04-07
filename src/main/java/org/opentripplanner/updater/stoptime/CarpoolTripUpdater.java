package org.opentripplanner.updater.stoptime;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
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
public class CarpoolTripUpdater extends PollingGraphUpdater {
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
    public void runPolling() throws Exception {
        LOG.info("Starting carpool trip updater");

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        updaterManager.execute(graph -> {
            GtfsRealtime.TripDescriptor tripDescriptor = GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("carpool-update-123")
                    .setStartDate(today)
                    .setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
                    .build();

            Stop start = new Stop();
            start.setId(FeedScopedId.convertFromString("1:de:08115:4512:1:2"));
            start.setName("Herrenberg Bahnhof");
            start.setLat(48.5938);
            start.setLon(8.8627);

            Stop end = new Stop();
            end.setId(FeedScopedId.convertFromString("1:de:08115:5773:1:1"));
            end.setName("Ehningen Bahnhof");
            end.setLat(48.66203);
            end.setLon(8.94332);

            ZonedDateTime threeOclock = LocalDate.now().atStartOfDay().plusHours(15).atZone(ZoneId.of("Europe/Berlin"));

            List<TripUpdate.StopTimeUpdate> stopTimeUpdates = Arrays.asList(
                    buildStopTimeUpdate(start, threeOclock),
                    buildStopTimeUpdate(end, threeOclock.plusMinutes(30))
            );

            TripUpdate update = TripUpdate.newBuilder().setTrip(tripDescriptor).addAllStopTimeUpdate(stopTimeUpdates).build();

            boolean result = graph.timetableSnapshotSource
                    .validateAndHandleAddedTrip(graph, update, "1", ServiceDate.parseString(today));

            if(result) {
                LOG.info("Trip {} added to graph", tripDescriptor.getTripId());
            } else {
                LOG.error("Trip {} failed to be added to graph", tripDescriptor.getTripId());
            }
        });
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {

    }

    private TripUpdate.StopTimeUpdate buildStopTimeUpdate(Stop stop, ZonedDateTime time) {
        TripUpdate.StopTimeEvent timeEvent = TripUpdate.StopTimeEvent.newBuilder().setTime(time.toEpochSecond()).build();
        return TripUpdate.StopTimeUpdate.newBuilder()
                .setDeparture(timeEvent)
                .setArrival(timeEvent)
                .setStopId(stop.getId().getId())
                .build();
    }

    @Override
    public void teardown() {
    }

}
