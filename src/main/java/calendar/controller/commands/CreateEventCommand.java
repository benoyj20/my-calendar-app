package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Handles creation of new events in the active calendar.
 * Supports both single events and simple recurring events on given weekdays.
 */

public class CreateEventCommand implements Command {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final Pattern VALID_WEEKDAYS =
      Pattern.compile("^[MTWRFSU]+$", Pattern.CASE_INSENSITIVE);

  private final List<String> tokens;

  /**
   * Constructs a new CreateEventCommand.
   *
   * @param tokens tokenized input representing the create event command
   *               and its arguments
   */
  public CreateEventCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    Calendar activeCalendar = getActiveCalendar(model);
    validateTokensLength();

    String subject = tokens.get(2);
    String type = tokens.get(3);
    UUID seriesId = UUID.randomUUID();

    List<Event> eventsToAdd = type.equalsIgnoreCase("on")
        ? handleCreateOn(subject, seriesId)
        : handleCreateFrom(subject, seriesId);

    activeCalendar.addEvents(eventsToAdd);
    view.showMessage("Event(s) created successfully.");
  }

  private Calendar getActiveCalendar(ApplicationManager model) throws ValidationException {
    try {
      return model.getActiveCalendar();
    } catch (ValidationException e) {
      throw new ValidationException(
          "Error: No calendar is active. Use 'use calendar --name <calName>' first.");
    }
  }

  private void validateTokensLength() throws ValidationException {
    if (tokens.size() < 4) {
      throw new ValidationException("Invalid 'create event' command syntax.");
    }
  }

  private List<Event> handleCreateOn(String subject, UUID seriesId) throws ValidationException {
    LocalDate date = parseDate(tokens.get(4));
    Event.EventBuilder builder = buildAllDayEvent(subject, date, seriesId);

    if (tokens.size() == 5) {
      return List.of(builder.build());
    } else if ("repeats".equalsIgnoreCase(tokens.get(5))) {
      return generateSeries(builder, 6);
    } else {
      throw new ValidationException("Invalid 'create event on' syntax.");
    }
  }

  private Event.EventBuilder buildAllDayEvent(String subject, LocalDate date, UUID seriesId) {
    LocalDateTime start = date.atTime(8, 0);
    LocalDateTime end = date.atTime(17, 0);
    return Event.builder()
        .setSubject(subject)
        .setStart(start)
        .setEnd(end)
        .setAllDay(true)
        .setSeriesId(seriesId);
  }

  private List<Event> handleCreateFrom(String subject, UUID seriesId) throws ValidationException {
    LocalDateTime start = parseDateTime(tokens.get(4));
    LocalDateTime end = parseDateTime(tokens.get(6));

    if (!start.toLocalDate().equals(end.toLocalDate()) && tokens.size() > 7) {
      throw new ValidationException("Recurring events must start and end on the same day.");
    }

    Event.EventBuilder builder = buildTimedEvent(subject, start, end, seriesId);

    if (tokens.size() == 7) {
      return List.of(builder.build());
    } else if ("repeats".equalsIgnoreCase(tokens.get(7))) {
      return generateSeries(builder, 8);
    } else {
      throw new ValidationException("Invalid 'create event from' syntax.");
    }
  }

  private Event.EventBuilder buildTimedEvent(String subject, LocalDateTime start, LocalDateTime end,
                                             UUID seriesId) {
    return Event.builder()
        .setSubject(subject)
        .setStart(start)
        .setEnd(end)
        .setAllDay(false)
        .setSeriesId(seriesId);
  }

  private List<Event> generateSeries(Event.EventBuilder templateBuilder, int repeatsTokenIndex)
      throws ValidationException {
    String weekdays = tokens.get(repeatsTokenIndex);
    String condition = tokens.get(repeatsTokenIndex + 1);
    validateWeekdays(weekdays);

    return condition.equalsIgnoreCase("for")
        ? generateSeriesFor(templateBuilder, weekdays, repeatsTokenIndex + 2)
        : generateSeriesUntil(templateBuilder, weekdays, repeatsTokenIndex + 2);
  }

  private void validateWeekdays(String weekdays) throws ValidationException {
    if (weekdays.isEmpty() || !VALID_WEEKDAYS.matcher(weekdays).matches()) {
      throw new ValidationException(
          "Invalid weekdays string. Only M, T, W, R, F, S, U are allowed.");
    }
  }

  private List<Event> generateSeriesFor(Event.EventBuilder templateBuilder, String weekdays,
                                        int countTokenIndex)
      throws ValidationException {
    int n;
    try {
      n = Integer.parseInt(tokens.get(countTokenIndex));
    } catch (NumberFormatException e) {
      throw new ValidationException("Invalid number of occurrences.");
    }

    List<Event> seriesEvents = new ArrayList<>();
    LocalDate currentDate = templateBuilder.build().getStart().toLocalDate();
    int count = 0;
    while (count < n) {
      if (isWeekdayMatch(currentDate.getDayOfWeek(), weekdays)) {
        seriesEvents.add(buildEventForDate(templateBuilder, currentDate));
        count++;
      }
      currentDate = currentDate.plusDays(1);
    }
    return seriesEvents;
  }

  private List<Event> generateSeriesUntil(Event.EventBuilder templateBuilder, String weekdays,
                                          int untilDateIndex)
      throws ValidationException {
    LocalDate untilDate = parseDate(tokens.get(untilDateIndex));
    List<Event> seriesEvents = new ArrayList<>();
    LocalDate currentDate = templateBuilder.build().getStart().toLocalDate();

    while (!currentDate.isAfter(untilDate)) {
      if (isWeekdayMatch(currentDate.getDayOfWeek(), weekdays)) {
        seriesEvents.add(buildEventForDate(templateBuilder, currentDate));
      }
      currentDate = currentDate.plusDays(1);
    }
    return seriesEvents;
  }

  private boolean isWeekdayMatch(DayOfWeek dow, String weekdays) {
    final Map<DayOfWeek, Character> dayMap = Map.of(
        DayOfWeek.MONDAY, 'M',
        DayOfWeek.TUESDAY, 'T',
        DayOfWeek.WEDNESDAY, 'W',
        DayOfWeek.THURSDAY, 'R',
        DayOfWeek.FRIDAY, 'F',
        DayOfWeek.SATURDAY, 'S'
    );
    char dayChar = dayMap.getOrDefault(dow, 'U');
    return weekdays.toUpperCase().indexOf(dayChar) >= 0;
  }

  private Event buildEventForDate(Event.EventBuilder template, LocalDate date)
      throws ValidationException {
    Event built = template.build();
    LocalTime startTime = built.getStart().toLocalTime();
    LocalTime endTime = built.getEnd().toLocalTime();
    return Event.builder()
        .fromEvent(built)
        .setStart(LocalDateTime.of(date, startTime))
        .setEnd(LocalDateTime.of(date, endTime))
        .build();
  }

  private LocalDate parseDate(String text) throws ValidationException {
    try {
      return LocalDate.parse(text, DATE_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException("Invalid date format. Expected YYYY-MM-DD.");
    }
  }

  private LocalDateTime parseDateTime(String text) throws ValidationException {
    try {
      return LocalDateTime.parse(text.replace("::", ":"), DATETIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException("Invalid date/time format. Expected YYYY-MM-DDThh:mm.");
    }
  }
}
