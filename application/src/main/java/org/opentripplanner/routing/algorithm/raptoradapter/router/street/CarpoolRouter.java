package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class CarpoolRouter {

  public static boolean isCarpoolOnlyRequest(RouteRequest request) {
    if (
      request.journey().transit().filters().size() == 1 &&
      request.journey().transit().filters().get(0) instanceof TransitFilterRequest filterRequest
    ) {
      List<MainAndSubMode> transportModes = filterRequest.select().get(0).transportModes();
      if (
        transportModes.size() != 1 || !TransitMode.CARPOOL.equals(transportModes.get(0).mainMode())
      ) {
        return false;
      }
    } else {
      return false;
    }
    return true;
  }

  public static List<Itinerary> route(OtpServerRequestContext serverContext, RouteRequest request) {
    if (!isCarpoolOnlyRequest(request)) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();

    // Find all appropriate tripPatterns --------------------------

    final GenericLocation origin = request.from();
    final GenericLocation destination = request.to();

    // distance should not be larger than half the distance of origin/dest.
    // and not larger than a max distance for now
    final int distanceInMeters = distance(origin, destination) / 2;
    final Collection<RegularStop> stopsAroundOrigin = findStopsAround(
      serverContext,
      origin,
      distanceInMeters
    );
    final Collection<RegularStop> stopsAroundDestination = findStopsAround(
      serverContext,
      destination,
      distanceInMeters
    );
    final Collection<NearbyStop> stopsAroundOriginAccess = findAccessStops(request, serverContext);

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

    return originTripPatterns
      .stream()
      .filter(tripPattern ->
        hasBoardingAlightingStopsAroundOriginDestination(
          tripPattern,
          stopsAroundOrigin,
          stopsAroundDestination
        )
      )
      .map(tripPattern -> getItineraryForTripPattern(tripPattern, origin, destination))
      .toList();
  }

  private static Collection<RegularStop> findStopsAround(
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
      .toList();
  }

  private static Collection<NearbyStop> findAccessStops(RouteRequest request, OtpServerRequestContext serverContext) {
    // TODO debug here
    var temporaryVertices = new TemporaryVerticesContainer(
      serverContext.graph(),
      request.from(),
      request.to(),
      request.journey().direct().mode(),
      request.journey().direct().mode()
    );
    return AccessEgressRouter.findAccessEgresses(
      request,
      temporaryVertices,
      request.journey().direct(),
      serverContext.dataOverlayContext(request),
      AccessEgressType.ACCESS,
      serverContext.flexParameters().maxAccessWalkDuration(),
      0
    );
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

  private static boolean hasBoardingAlightingStopsAroundOriginDestination(
    TripPattern tripPattern,
    Collection<RegularStop> stopsAroundOrigin,
    Collection<RegularStop> stopsAroundDestination
  ) {
    boolean canBoardCloseToOrigin = false;
    boolean canAlightCloseToOrigin = false;

    for (int stopIndex = 0; stopIndex < tripPattern.numberOfStops(); stopIndex++) {
      if (
        tripPattern.canBoard(stopIndex) &&
        stopsAroundOrigin.contains(tripPattern.getStop(stopIndex))
      ) {
        canBoardCloseToOrigin = true;
      }
      if (
        tripPattern.canAlight(stopIndex) &&
        stopsAroundDestination.contains(tripPattern.getStop(stopIndex))
      ) {
        canAlightCloseToOrigin = true;
      }
      if (canBoardCloseToOrigin && canAlightCloseToOrigin) {
        return true;
      }
    }
    return false;
  }

  private static Itinerary getItineraryForTripPattern(
    TripPattern tripPattern,
    GenericLocation from,
    GenericLocation to
  ) {
    TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes().get(0);

    int[] boardingStop = findClosestStop(from, tripPattern, true);
    int[] alightingStop = findClosestStop(to, tripPattern, false);
    int accessLegCost = boardingStop[1];
    int egressLegCost = alightingStop[1];

    // duration or distance as Cost?
    final int alightingTime = tripTimes.getArrivalTime(alightingStop[0]);
    final int boardingTime = tripTimes.getDepartureTime(boardingStop[0]);
    int carpoolLegCost = alightingTime - boardingTime;
    ZoneId timeZone = tripPattern.getRoute().getAgency().getTimezone();
    System.out.println("Cost: " + accessLegCost + " " + carpoolLegCost + " " + egressLegCost);
    // TODO: kosten aus mitfahrersicht: Anreise zu Zustieg, gemeinsame Fahrt, Weiterfahrt nach Ausstieg
    // TODO: kosten aus fahrersicht: inverser Anteil zu gemeinsamer Fahrt (mindestens 50% der Strecke, je mehr umso besser)

    final LocalDate serviceDate = LocalDate.now(); // TODO should use the first possible service day for this tripPattern
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
      .build();
    final ItineraryBuilder itineraryBuilder = Itinerary.ofScheduledTransit(List.of(leg));
    itineraryBuilder.withGeneralizedCost(Cost.costOfSeconds(accessLegCost + carpoolLegCost + egressLegCost));
    Itinerary itinerary = itineraryBuilder.build();

    return itinerary;
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
        boarding && tripPattern.canBoard(stopIndex) || !boarding && tripPattern.canAlight(stopIndex)
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
