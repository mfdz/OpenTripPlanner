package io.leonard;

import ch.poole.openinghoursparser.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

import static ch.poole.openinghoursparser.RuleModifier.Modifier.*;

public class OpeningHoursEvaluator {

  private static final Set<RuleModifier.Modifier> CLOSED_MODIFIERS = Set.of(CLOSED, OFF);
  private static final Set<RuleModifier.Modifier> OPEN_MODIFIERS = Set.of(OPEN, UNKNOWN);
  private static final Map<WeekDay, DayOfWeek> weekDayToDayOfWeek =
    Map.of(
      WeekDay.MO, DayOfWeek.MONDAY,
      WeekDay.TU, DayOfWeek.TUESDAY,
      WeekDay.WE, DayOfWeek.WEDNESDAY,
      WeekDay.TH, DayOfWeek.THURSDAY,
      WeekDay.FR, DayOfWeek.FRIDAY,
      WeekDay.SA, DayOfWeek.SATURDAY,
      WeekDay.SU, DayOfWeek.SUNDAY);

  // when calculating the next time the hours are open, how many days should you go into the future
  // this protects against stack overflows when the place is never going to open again
  private static final int MAX_SEARCH_DAYS = 365 * 10;

  public static boolean isOpenAt(LocalDateTime time, List<Rule> rules) {
    var closed = getClosedRules(rules);
    var open = getOpenRules(rules);
    return closed.noneMatch(rule -> timeMatchesRule(time, rule))
      && open.anyMatch(rule -> rule.isTwentyfourseven() || timeMatchesRule(time, rule));
  }

  /**
   * @return LocalDateTime in Optional, representing next closing time ; or empty Optional if place
   * is either closed at time or never closed at all.
   */
  public static Optional<LocalDateTime> isOpenUntil(LocalDateTime time, List<Rule> rules) {
    var closed = getClosedRules(rules);
    var open = getOpenRules(rules);
    if (closed.anyMatch(rule -> timeMatchesRule(time, rule))) return Optional.empty();
    return getTimeRangesOnThatDay(time, open)
      .filter(r -> r.surrounds(time.toLocalTime()))
      .findFirst()
      .map(r -> time.toLocalDate().atTime(r.end));
  }

  public static Optional<LocalDateTime> wasLastOpen(LocalDateTime time, List<Rule> rules) {
    return isOpenIterative(time, rules, false, MAX_SEARCH_DAYS);
  }

  public static Optional<LocalDateTime> wasLastOpen(
    LocalDateTime time, List<Rule> rules, int searchDays) {
    return isOpenIterative(time, rules, false, searchDays);
  }

  public static Optional<LocalDateTime> isOpenNext(LocalDateTime time, List<Rule> rules) {
    return isOpenIterative(time, rules, true, MAX_SEARCH_DAYS);
  }

  public static Optional<LocalDateTime> isOpenNext(
    LocalDateTime time, List<Rule> rules, int searchDays) {
    return isOpenIterative(time, rules, true, searchDays);
  }

  /**
   * This is private function, this doc-string means only help onboard new devs.
   *
   * @param initialTime Starting point in time to search from.
   * @param rules       From parser
   * @param forward     Whether to search in future (true)? or in the past(false)?
   * @param searchDays  Limit search scope in days.
   * @return an Optional LocalDateTime
   */
  private static Optional<LocalDateTime> isOpenIterative(
    final LocalDateTime initialTime,
    final List<Rule> rules,
    boolean forward,
    final int searchDays) {

    var nextTime = initialTime;
    for (var iterations = 0; iterations <= searchDays; ++iterations) {
      var open = getOpenRules(rules);
      var closed = getClosedRules(rules);

      var time = nextTime;
      if (isOpenAt(time, rules)) return Optional.of(time);
      else {

        var openRangesOnThatDay = getTimeRangesOnThatDay(time, open);
        var closedRangesThatDay = getTimeRangesOnThatDay(time, closed);

        var endOfExclusion =
          closedRangesThatDay
            .filter(r -> r.surrounds(time.toLocalTime()))
            .findFirst()
            .map(r -> time.toLocalDate().atTime(forward ? r.end : r.start));

        var startOfNextOpening =
          forward
            ? openRangesOnThatDay
            .filter(range -> range.start.isAfter(time.toLocalTime()))
            .min(TimeRange.startComparator)
            .map(timeRange -> time.toLocalDate().atTime(timeRange.start))
            : openRangesOnThatDay
            .filter(range -> range.end.isBefore(time.toLocalTime()))
            .max(TimeRange.endComparator)
            .map(timeRange -> time.toLocalDate().atTime(timeRange.end));

        var opensNextThatDay = endOfExclusion.or(() -> startOfNextOpening);
        if (opensNextThatDay.isPresent()) {
          return opensNextThatDay;
        }

        // if we cannot find time on the same day when the POI is open, we skip forward to the start
        // of the following day and try again
        nextTime =
          forward
            ? time.toLocalDate().plusDays(1).atStartOfDay()
            : time.toLocalDate().minusDays(1).atTime(LocalTime.MAX);
      }
    }

    return Optional.empty();
  }

  private static Stream<TimeRange> getTimeRangesOnThatDay(
    LocalDateTime time, Stream<Rule> ruleStream) {
    return ruleStream
      .filter(rule -> timeMatchesDayRanges(time, rule.getDays()))
      .filter(r -> !Objects.isNull(r.getTimes()))
      .flatMap(r -> r.getTimes().stream().map(TimeRange::new));
  }

  private static Stream<Rule> getOpenRules(List<Rule> rules) {
    return rules.stream()
      .filter(
        r -> {
          var modifier = r.getModifier();
          return modifier == null || OPEN_MODIFIERS.contains(modifier.getModifier());
        });
  }

  private static Stream<Rule> getClosedRules(List<Rule> rules) {
    return rules.stream()
      .filter(
        r -> {
          var modifier = r.getModifier();
          return modifier != null && CLOSED_MODIFIERS.contains(modifier.getModifier());
        });
  }

  private static boolean timeMatchesRule(LocalDateTime time, Rule rule) {
    return (timeMatchesDayRanges(time, rule.getDays())
      || rule.getDays() == null
      && dateMatchesDateRanges(time, rule.getDates()))
      && nullToEntireDay(rule.getTimes()).stream()
      .anyMatch(timeSpan -> timeMatchesHours(time, timeSpan));
  }

  private static boolean timeMatchesDayRanges(LocalDateTime time, List<WeekDayRange> ranges) {
    return nullToEmptyList(ranges).stream().anyMatch(dayRange -> timeMatchesDay(time, dayRange));
  }

  private static boolean timeMatchesDay(LocalDateTime time, WeekDayRange range) {
    // if the end day is null it means that it's just a single day like in "Th
    // 10:00-18:00"
    if (range.getEndDay() == null) {
      return time.getDayOfWeek().equals(weekDayToDayOfWeek.getOrDefault(range.getStartDay(), null));
    }
    int ordinal = time.getDayOfWeek().ordinal();
    return range.getStartDay().ordinal() <= ordinal && range.getEndDay().ordinal() >= ordinal;
  }

  private static boolean dateMatchesDateRanges(LocalDateTime time, List<DateRange> ranges) {
    return nullToEmptyList(ranges).stream().anyMatch(dateRange -> dateMatchesDateRange(time, dateRange));
  }

  private static boolean dateMatchesDateRange(LocalDateTime time, DateRange range) {
    // if the end date is null it means that it's just a single date like in "2020 Aug 11"
    DateWithOffset startDate = range.getStartDate();
    boolean afterStartDate = time.getYear() >= startDate.getYear() && time.getMonth().ordinal() >= startDate.getMonth().ordinal() && time.getDayOfMonth() >= startDate.getDay();
    if (range.getEndDate() == null) {
      return afterStartDate;
    }
    DateWithOffset endDate = range.getEndDate();
    boolean beforeEndDate = time.getYear() <= endDate.getYear() && time.getMonth().ordinal() <= endDate.getMonth().ordinal() && time.getDayOfMonth() <= endDate.getDay();
    return afterStartDate && beforeEndDate;
  }

  private static boolean timeMatchesHours(LocalDateTime time, TimeSpan timeSpan) {
    var minutesAfterMidnight = minutesAfterMidnight(time.toLocalTime());
    return timeSpan.getStart() <= minutesAfterMidnight && timeSpan.getEnd() >= minutesAfterMidnight;
  }

  private static int minutesAfterMidnight(LocalTime time) {
    return time.getHour() * 60 + time.getMinute();
  }

  private static <T> List<T> nullToEmptyList(List<T> list) {
    if (list == null) return Collections.emptyList();
    else return list;
  }

  private static List<TimeSpan> nullToEntireDay(List<TimeSpan> span) {
    if (span == null) {
      var allDay = new TimeSpan();
      allDay.setStart(TimeSpan.MIN_TIME);
      allDay.setEnd(TimeSpan.MAX_TIME);
      return List.of(allDay);
    } else return span;
  }
}