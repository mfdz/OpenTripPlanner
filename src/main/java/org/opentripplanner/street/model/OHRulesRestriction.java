package org.opentripplanner.street.model;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import io.leonard.OpeningHoursEvaluator;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * <code>OHRulesRestriction</code> represents timezone aware OSM opening hours rules
 * to represent temporal restrictions.
 */
public class OHRulesRestriction implements Serializable, TimeRestriction {

  /**
   * The timezone this is represented in.
   */
  private final ZoneId timeZone;
  /**
   * The opening hours rules as parsed by OpeningHoursParser.
   */
  private final List<Rule> rules;
  /**
   * Should the time restriction be reversed (in case the condition's restriction value is "none")
   */
  private final boolean inverse;

  private OHRulesRestriction(ZoneId timeZone, List<Rule> rules, boolean inverse) {
    this.timeZone = timeZone;
    this.rules = rules;
    this.inverse = inverse;
  }

  /**
   * Parse the time specification from an OSM turn restriction
   */
  public static TimeRestriction parseFromOsmTurnRestriction(
    String day_on,
    String day_off,
    String hour_on,
    String hour_off,
    Supplier<ZoneId> timeZoneSupplier
  ) throws OpeningHoursParseException {
    return parseFromCondition(
      day_on.substring(0, 3) + " - " + day_off.substring(0, 3) + " " + hour_on + "-" + hour_off,
      timeZoneSupplier
    );
  }

  /**
   *
   * @param tagValueWithCondition
   * @param timeZoneSupplier
   * @return
   * @throws OpeningHoursParseException
   */
  public static TimeRestriction parseFromCondition(
    String tagValueWithCondition,
    Supplier<ZoneId> timeZoneSupplier
  ) throws OpeningHoursParseException {
    ZoneId timeZone = timeZoneSupplier.get();
    if (timeZone == null) {
      return null;
    }
    int indexOfAt = tagValueWithCondition.indexOf('@');
    String temporalCondition = indexOfAt > 0
      ? tagValueWithCondition.substring(indexOfAt + 1).trim()
      : tagValueWithCondition;
    OpeningHoursParser openingHoursParser = new OpeningHoursParser(
      new ByteArrayInputStream(temporalCondition.getBytes())
    );
    boolean inverse =
      indexOfAt > 0 && "none".equals(tagValueWithCondition.substring(0, indexOfAt).trim());

    return new OHRulesRestriction(timeZone, openingHoursParser.rules(false), inverse);
  }

  @Override
  public boolean active(long time) {
    return (
      OpeningHoursEvaluator.isOpenAt(
        LocalDateTime.ofInstant(Instant.ofEpochSecond(time), timeZone),
        rules
      ) ^
      inverse
    );
  }
}
