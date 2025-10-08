package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import gnu.trove.set.TIntSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class CarpoolRouter {

  public static final double MIN_CARPOOLING_SCORE = 0.05;
  private final OtpServerRequestContext serverContext;
  private final RouteRequest request;
  private final ZoneId timeZone;

  public CarpoolRouter(OtpServerRequestContext serverContext, RouteRequest request) {
    this.serverContext = serverContext;
    this.request = request;
    this.timeZone = ZoneIdFallback.zoneId(serverContext.transitService().getTimeZone());
  }

  public static List<Itinerary> route(OtpServerRequestContext serverContext, RouteRequest request) {
    if (!isCarpoolOnlyRequest(request)) {
      return Collections.emptyList();
    }

    return new CarpoolRouter(serverContext, request).route();
  }

  public static boolean isCarpoolOnlyRequest(RouteRequest request) {
    if (
      request.journey().transit().filters().size() == 1 &&
      request.journey().transit().filters().getFirst() instanceof TransitFilterRequest filterRequest
    ) {
      List<MainAndSubMode> transportModes = filterRequest.select().getFirst().transportModes();
      if (
        transportModes.size() != 1 ||
        !TransitMode.CARPOOL.equals(transportModes.getFirst().mainMode())
      ) {
        return false;
      }
    } else {
      return false;
    }
    return true;
  }

  protected List<Itinerary> route() {
    OTPRequestTimeoutException.checkForTimeout();

    // Find all appropriate tripPatterns --------------------------

    final GenericLocation origin = request.from();
    final GenericLocation destination = request.to();
    // TODO verify if we better should use ServceDateUtils.asServiceDay (+12h...)
    final LocalDate serviceDate = ServiceDateUtils.asStartOfService(
      request.dateTime(),
      timeZone
    ).toLocalDate();

    return getItineraries(origin, destination, serviceDate)
      .stream()
      .filter(it -> it.getCarpoolingScore() > MIN_CARPOOLING_SCORE)
      .toList();
  }

  protected List<Itinerary> getItineraries(
    GenericLocation origin,
    GenericLocation destination,
    LocalDate serviceDate
  ) {
    // distance should not be larger than half the distance of origin/dest.
    // and not larger than a max distance for now
    final int distanceInMeters = distance(origin, destination) / 2;
    final Collection<StopLocation> stopsAroundOrigin = findStopsAround(
      serverContext,
      origin,
      distanceInMeters
    );
    final Collection<StopLocation> stopsAroundDestination = findStopsAround(
      serverContext,
      destination,
      distanceInMeters
    );
    // find trippatterns which have a pick around start and dropoff at endstops
    final Stream<TripPattern> originTripPatternStream = stopsAroundOrigin
      .stream()
      .map(stop -> serverContext.transitService().findPatterns(stop, true))
      .flatMap(Collection::stream);
    final Stream<TripPattern> destinationTripPatternStream = stopsAroundDestination
      .stream()
      .map(stop -> serverContext.transitService().findPatterns(stop, true))
      .flatMap(Collection::stream);
    final Set<TripPattern> originTripPatterns = originTripPatternStream.collect(Collectors.toSet());
    final Set<TripPattern> destinationTripPatterns = destinationTripPatternStream.collect(
      Collectors.toSet()
    );
    originTripPatterns.retainAll(destinationTripPatterns);

    final Stream<TripPattern> tripPatternStream = originTripPatterns
      .stream()
      .filter(tripPattern -> isValidForServiceDate(tripPattern, serviceDate))
      .filter(tripPattern ->
        hasBoardingAlightingStopsAroundOriginDestination(
          tripPattern,
          stopsAroundOrigin,
          stopsAroundDestination
        )
      );

    return createItinerariesFromTripPatterns(origin, destination, serviceDate, tripPatternStream);
  }

  private boolean isValidForServiceDate(TripPattern tripPattern, LocalDate serviceDate) {
    final Timetable timetable = serverContext
      .transitService()
      .findTimetable(tripPattern, serviceDate);
    if (timetable.isCreatedByRealTimeUpdater()) {
      return timetable.isValidFor(serviceDate);
    } else {
      final TIntSet serviceCodesRunningForDate = serverContext
        .transitService()
        .getServiceCodesRunningForDate(serviceDate);
      return tripPattern
        .scheduledTripsAsStream()
        .anyMatch(trip ->
          serviceCodesRunningForDate.contains(
            serverContext.transitService().getServiceCode(trip.getServiceId())
          )
        );
    }
  }

  protected List<Itinerary> createItinerariesFromTripPatterns(
    GenericLocation origin,
    GenericLocation destination,
    LocalDate serviceDate,
    Stream<TripPattern> tripPatternStream
  ) {
    final List<Itinerary> sortedItineraries = tripPatternStream
      .map(tripPattern -> getItineraryForTripPattern(tripPattern, origin, destination, serviceDate))
      .filter(Objects::nonNull)
      .sorted(Comparator.comparingDouble(it -> -it.getCarpoolingScore()))
      .toList();

    return sortedItineraries;
  }

  private static Collection<StopLocation> findStopsAround(
    OtpServerRequestContext serverContext,
    GenericLocation location,
    int distanceInMeters
  ) {
    // build envelopes around start and end
    Envelope envelope = createEnvelope(location, distanceInMeters);
    // search stops in envelopes and filter by distance
    return serverContext
      .transitService()
      .findRegularStopsByBoundingBox(envelope)
      .stream()
      .filter(s -> distance(s, location) < distanceInMeters)
      .map(s -> (StopLocation) s)
      .toList();
  }

  private static int distance(GenericLocation a, GenericLocation b) {
    // TODO Would be nice, if GenericLocation and StopLocation would share a common interface to access the coordinate...
    return (int) SphericalDistanceLibrary.distance(a.getCoordinate(), b.getCoordinate());
  }

  private static int distance(StopLocation a, GenericLocation b) {
    return (int) SphericalDistanceLibrary.distance(
      a.getLat(),
      a.getLon(),
      b.getCoordinate().y,
      b.getCoordinate().x
    );
  }

  private static int distance(StopLocation a, StopLocation b) {
    return (int) SphericalDistanceLibrary.distance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
  }

  /**
   * Returns TRUE, if any of stopsAroundOrigin is a boarding stop of tripPattern and
   * any tripPattern stop with stopIndex greater than the first boardable stop is contained in
   * stopsAroundDestination
   * @param tripPattern
   * @param stopsAroundOrigin
   * @param stopsAroundDestination
   * @return
   */
  protected static boolean hasBoardingAlightingStopsAroundOriginDestination(
    TripPattern tripPattern,
    Collection<StopLocation> stopsAroundOrigin,
    Collection<StopLocation> stopsAroundDestination
  ) {
    boolean canBoardCloseToOrigin = false;

    for (int stopIndex = 0; stopIndex < tripPattern.numberOfStops(); stopIndex++) {
      if (
        !canBoardCloseToOrigin &&
        tripPattern.canBoard(stopIndex) &&
        stopsAroundOrigin.contains(tripPattern.getStop(stopIndex))
      ) {
        canBoardCloseToOrigin = true;
        continue;
      }
      if (
        canBoardCloseToOrigin &&
        tripPattern.canAlight(stopIndex) &&
        stopsAroundDestination.contains(tripPattern.getStop(stopIndex))
      ) {
        return true;
      }
    }
    return false;
  }

  protected Itinerary getItineraryForTripPattern(
    TripPattern tripPattern,
    GenericLocation from,
    GenericLocation to,
    LocalDate serviceDate
  ) {
    final Timetable timetable = serverContext
      .transitService()
      .findTimetable(tripPattern, serviceDate);
    final List<TripTimes> tripTable = timetable.getTripTimes();
    if (tripTable.isEmpty()) {
      // in case no trip is running at current service day, we return null (which is filtered afterwards)
      return null;
    }
    TripTimes tripTimes = tripTable.getFirst();
    int[] boardingStop = findClosestStop(from, tripPattern, true);
    int[] alightingStop = findClosestStop(to, tripPattern, false);
    if (boardingStop[0] >= alightingStop[0]) {
      // trip pattern is in reverse direction, boarding has stopIndex greeator or equal to alighting stop
      return null;
    }
    int accessLegCost = boardingStop[1];
    int egressLegCost = alightingStop[1];

    final int alightingTime = tripTimes.getArrivalTime(alightingStop[0]);
    final int boardingTime = tripTimes.getDepartureTime(boardingStop[0]);
    // duration or distance as Cost?
    //int carpoolLegCostTime = alightingTime - boardingTime;
    ZoneId timeZone = tripPattern.getRoute().getAgency().getTimezone();

    // Rating
    int directCost = distance(from, to);
    int carpoolLegCostDistance = distance(
      tripPattern.getStop(boardingStop[0]),
      tripPattern.getStop(alightingStop[0])
    );
    int carpoolLegCost = IntStream.range(boardingStop[0], alightingStop[0])
      .map(i -> (int) SphericalDistanceLibrary.length(tripPattern.getHopGeometry(i)))
      .sum();
    int carpoolPassengerCost = accessLegCost + carpoolLegCostDistance + egressLegCost;

    int prePickupCost = IntStream.range(0, boardingStop[0])
      .map(i -> (int) SphericalDistanceLibrary.length(tripPattern.getHopGeometry(i)))
      .sum();
    int postDropoffCost = IntStream.range(alightingStop[0], tripPattern.numberOfStops() - 1)
      .map(i -> (int) SphericalDistanceLibrary.length(tripPattern.getHopGeometry(i)))
      .sum();
    int carpoolDriverCost = prePickupCost + carpoolLegCost + postDropoffCost;

    // Rating aus Fahrerrsighted: inverser Anteil zu gemeinsamer Fahrt (mindestens 50% der Strecke, je mehr umso besser)
    // oder TODO 200km...
    double carpoolDriverRating = Math.max((2.0 * carpoolLegCost) / carpoolDriverCost - 1.0, 0);
    // Kosten aus mitfahrersicht: Anreise zu Zustieg, gemeinsame Fahrt, Weiterfahrt nach Ausstieg
    double carpoolPassengerRating = Math.max((4.0 * directCost) / carpoolPassengerCost - 3.0, 0);
    double carpoolPassengerSharingRating = Math.max(
      (2.0 * carpoolLegCostDistance) / carpoolPassengerCost - 1.0,
      0
    );

    // System.out.println("Trip: "+ tripPattern.getName()+" SharingRatio "+carpoolPassengerSharingRating+ " Rating: "+ carpoolPassengerRating + " carpoolPassengerCost: "+carpoolPassengerCost+ " carpoolDriverRating "+carpoolDriverRating);

    var leg = new ScheduledTransitLegBuilder()
      .withTripTimes(tripTimes)
      .withTripPattern(tripPattern)
      .withBoardStopIndexInPattern(boardingStop[0])
      .withAlightStopIndexInPattern(alightingStop[0])
      .withServiceDate(serviceDate)
      .withStartTime(ServiceDateUtils.toZonedDateTime(serviceDate, timeZone, boardingTime))
      .withEndTime(ServiceDateUtils.toZonedDateTime(serviceDate, timeZone, alightingTime))
      .withZoneId(timeZone)
      .withGeneralizedCost(carpoolLegCost)
      .withDistanceMeters(carpoolLegCostDistance) // TODO
      .build();
    final ItineraryBuilder itineraryBuilder = Itinerary.ofScheduledTransit(List.of(leg));
    itineraryBuilder
      .withGeneralizedCost(Cost.costOfSeconds(accessLegCost + carpoolLegCost + egressLegCost))
      .withCarpoolingScore(
        (float) (Math.pow(
            Math.pow(carpoolPassengerSharingRating, 3) * carpoolPassengerRating,
            0.25
          ))
      );
    return itineraryBuilder.build();
  }

  private static int[] findClosestStop(
    GenericLocation location,
    TripPattern tripPattern,
    boolean boarding
  ) {
    int closestDistanceInMeters = Integer.MAX_VALUE, closestIndex = 0;
    Coordinate c = location.getCoordinate();
    for (int stopIndex = 0; stopIndex < tripPattern.numberOfStops(); stopIndex++) {
      if (
        (boarding && tripPattern.canBoard(stopIndex)) ||
        (!boarding && tripPattern.canAlight(stopIndex))
      ) {
        int distance = distance(tripPattern.getStop(stopIndex), location);
        if (distance < closestDistanceInMeters) {
          closestDistanceInMeters = distance;
          closestIndex = stopIndex;
        }
      }
    }
    return new int[] { closestIndex, closestDistanceInMeters };
  }

  @Nullable
  private static Envelope createEnvelope(GenericLocation location, final int distanceInMeters) {
    if (location == null) {
      return null;
    }

    Coordinate coordinate = location.getCoordinate();
    if (coordinate == null) {
      return null;
    }

    double latDelta = SphericalDistanceLibrary.metersToDegrees(distanceInMeters);
    double lonDelta = SphericalDistanceLibrary.metersToLonDegrees(distanceInMeters, coordinate.y);

    Envelope env = new Envelope(coordinate);
    env.expandBy(lonDelta, latDelta);

    return env;
  }
}
