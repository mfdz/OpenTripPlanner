package org.opentripplanner.model.plan.paging.cursor;

import java.time.Instant;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This class contains all the information needed to dedupe itineraries when
 * paging - the exact same information as the {@link ItinerarySortKey}.
 * <p>
 * It implements the ItinerarySortKey interface so that it can be sorted with itineraries which
 * potentially contain duplicates.
 */
record DeduplicationPageCut(
  Instant departureTime,
  Instant arrivalTime,
  Cost generalizedCost,
  int numOfTransfers,
  boolean onStreet,
  Float carpoolingScore
)
  implements ItinerarySortKey {
  @Override
  public Instant startTimeAsInstant() {
    return departureTime;
  }

  @Override
  public Instant endTimeAsInstant() {
    return arrivalTime;
  }

  @Override
  public Cost generalizedCostIncludingPenalty() {
    return generalizedCost;
  }

  @Override
  public int numberOfTransfers() {
    return numOfTransfers;
  }

  @Override
  public boolean isStreetOnly() {
    return onStreet;
  }

  @Override
  public Float getCarpoolingScore() {
    return carpoolingScore;
  }

  @Override
  public String toString() {
    return keyAsString();
  }
}
