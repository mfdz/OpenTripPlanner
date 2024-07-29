package io.leonard;

import ch.poole.openinghoursparser.TimeSpan;
import java.time.LocalTime;
import java.util.Comparator;

public class TimeRange {

  public final LocalTime start;
  public final LocalTime end;

  public TimeRange(TimeSpan span) {
    this.start = LocalTime.ofSecondOfDay(span.getStart() * 60L);
    this.end =
      LocalTime.ofSecondOfDay(Math.min(span.getEnd() * 60L, LocalTime.MAX.toSecondOfDay()));
  }

  public boolean surrounds(LocalTime time) {
    return time.isAfter(start) && time.isBefore(end);
  }

  public static Comparator<TimeRange> startComparator = Comparator.comparing(timeRange ->
    timeRange.start
  );

  public static Comparator<TimeRange> endComparator = Comparator.comparing(timeRange ->
    timeRange.end
  );
}
