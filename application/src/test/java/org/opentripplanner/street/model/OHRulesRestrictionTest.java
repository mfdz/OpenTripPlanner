package org.opentripplanner.street.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner._support.time.ZoneIds;

public class OHRulesRestrictionTest {

  private final ZoneId zoneId = ZoneIds.PARIS;

  @Test
  public void testRepeatingTimePeriod() throws OpeningHoursParseException {
    // Note: day_on/day_off is currently required, relations like e.g.
    // https://www.openstreetmap.org/relation/54249 would not work, as day_on/day_off is missing
    TimeRestriction timePeriod = OHRulesRestriction.parseFromOsmTurnRestriction(
      "monday",
      "sunday",
      "06",
      "23",
      () -> {
        return zoneId;
      }
    );
    Instant dateTime = Instant.parse("2024-07-26T10:30:00Z");
    long time = dateTime.getEpochSecond();
    assertTrue(timePeriod.active(time));
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "mo-su 06:00-23:00", "fri", "none @ sat", "2024 Jul 26", "2024 Jul 25 - 2024 Jul 27",
    }
  ) // six numbers
  // Note: hours only restrictions (e.g. "06:00-23:00") are not yet supported
  public void testOHCalendarRestrictionOpen(String condition) throws OpeningHoursParseException {
    TimeRestriction tr = OHRulesRestriction.parseFromCondition(
      condition,
      () -> {
        return zoneId;
      }
    );
    Instant dateTime = Instant.parse("2024-07-26T10:30:00Z");
    long time = dateTime.getEpochSecond();
    assertTrue(tr.active(time));
  }
}
