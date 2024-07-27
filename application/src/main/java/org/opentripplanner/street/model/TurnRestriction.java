package org.opentripplanner.street.model;

import java.io.Serializable;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.TraverseModeSet;

public class TurnRestriction implements Serializable {

  public final TurnRestrictionType type;
  public final StreetEdge from;
  public final StreetEdge to;
  public final TimeRestriction time;
  public final TraverseModeSet modes;

  public TurnRestriction(
    StreetEdge from,
    StreetEdge to,
    TurnRestrictionType type,
    TraverseModeSet modes,
    TimeRestriction time
  ) {
    this.from = from;
    this.to = to;
    this.type = type;
    this.modes = modes;
    this.time = time;
  }

  public TurnRestriction(
    StreetEdge from,
    StreetEdge to,
    TurnRestrictionType type,
    TraverseModeSet modes
  ) {
    this(from, to, type, modes, null);
  }

  @Override
  public String toString() {
    // TODO print timeRestriction
    return type.name() + " from " + from + " to " + to + " (modes: " + modes + ")";
  }
}
