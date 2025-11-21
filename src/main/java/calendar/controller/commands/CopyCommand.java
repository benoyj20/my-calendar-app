package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Handles all copy command such as copying a single event or multiple events
 * from one date to another in the active calendar.
 * The command parses the provided tokens to find the source event(s),
 * the target date.
 */
public class CopyCommand implements Command {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final List<String> tokens;

  /**
   * Constructs a new CopyCommand.
   *
   * @param tokens the tokens that describe what to copy and where
   */
  public CopyCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    String commandType = tokens.get(1).toLowerCase();
    if ("event".equals(commandType)) {
      executeCopyEvent(model, view);
    } else if ("events".equals(commandType)) {
      executeCopyEvents(model, view);
    } else {
      throw new ValidationException("Unknown copy command.");
    }
  }


  private void executeCopyEvent(ApplicationManager model, CalendarView view) throws Exception {
    validateCopyEventTokens();
    String eventName = tokens.get(2);
    LocalDateTime sourceDt = parseDateTime(tokens.get(4));
    String targetCalName = tokens.get(6);
    LocalDateTime targetDt = parseDateTime(tokens.get(8));

    Calendar sourceCal = model.getActiveCalendar();
    Calendar targetCal = model.getCalendar(targetCalName);

    Event sourceEvent = findSingleEvent(sourceCal, eventName, sourceDt);
    Event newEvent = buildCopiedEvent(sourceEvent, targetDt);

    addEventWithConflictCheck(targetCal, newEvent);
    view.showMessage("Event copied to '" + targetCalName + "'.");
  }

  private void validateCopyEventTokens() throws ValidationException {
    if (tokens.size() != 9
        || !"on".equalsIgnoreCase(tokens.get(3))
        || !"--target".equalsIgnoreCase(tokens.get(5))
        || !"to".equalsIgnoreCase(tokens.get(7))) {
      throw new ValidationException(
          "Invalid command. Expected: copy event <eventName> on "
              + "<datetime> --target <calName> to <datetime>");
    }
  }

  private Event findSingleEvent(Calendar cal, String name, LocalDateTime dt)
      throws ValidationException {
    Predicate<Event> filter = e -> e.getSubject().equals(name) && e.getStart().equals(dt);
    List<Event> events = cal.findEvents(filter);
    if (events.isEmpty()) {
      throw new ValidationException("No event found matching '" + name + "' at " + dt);
    }
    return events.get(0);
  }

  private Event buildCopiedEvent(Event source, LocalDateTime targetStart)
      throws ValidationException {
    long durationNanos = Duration.between(source.getStart(), source.getEnd()).toNanos();
    LocalDateTime targetEnd = targetStart.plusNanos(durationNanos);
    return Event.builder()
        .fromEvent(source)
        .setStart(targetStart)
        .setEnd(targetEnd)
        .setSeriesId(null)
        .build();
  }

  private void addEventWithConflictCheck(Calendar cal, Event event) throws ValidationException {
    try {
      cal.addEvent(event);
    } catch (ValidationException e) {
      throw new ValidationException("Copy creates a conflict: " + e.getMessage());
    }
  }


  private void executeCopyEvents(ApplicationManager model, CalendarView view) throws Exception {
    String type = tokens.get(2).toLowerCase();
    if ("on".equals(type)) {
      copyEventsOn(model, view);
    } else if ("between".equals(type)) {
      copyEventsBetween(model, view);
    } else {
      throw new ValidationException("Expected 'on' or 'between' after 'copy events'.");
    }
  }

  private void copyEventsOn(ApplicationManager model, CalendarView view) throws Exception {
    validateCopyEventsOnTokens();
    LocalDate sourceDate = parseDate(tokens.get(3));
    String targetCalName = tokens.get(5);
    LocalDate targetDate = parseDate(tokens.get(7));

    Calendar sourceCal = model.getActiveCalendar();
    Calendar targetCal = model.getCalendar(targetCalName);

    List<Event> eventsToCopy = findEventsOnDate(sourceCal, sourceDate);
    List<Event> newEvents =
        buildCopiedEventsForDate(eventsToCopy, sourceCal, targetCal, targetDate);

    addEventsWithConflictCheck(targetCal, newEvents);
    view.showMessage(String.format("Copied %d events to '%s'.", newEvents.size(), targetCalName));
  }

  private void validateCopyEventsOnTokens() throws ValidationException {
    if (tokens.size() != 8
        || !"--target".equalsIgnoreCase(tokens.get(4))
        || !"to".equalsIgnoreCase(tokens.get(6))) {
      throw new ValidationException(
          "Invalid command. Expected: copy events on <date> --target <calName> to <date>");
    }
  }

  private List<Event> findEventsOnDate(Calendar cal, LocalDate date) {
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();
    Predicate<Event> filter = e -> e.getStart().isBefore(end) && e.getEnd().isAfter(start);
    return cal.findEvents(filter);
  }

  private List<Event> buildCopiedEventsForDate(List<Event> events, Calendar sourceCal,
                                               Calendar targetCal, LocalDate targetDate)
      throws ValidationException {
    List<Event> newEvents = new ArrayList<>();
    for (Event sourceEvent : events) {
      newEvents.add(buildSingleCopiedEvent(sourceEvent, sourceCal, targetCal, targetDate));
    }
    return newEvents;
  }

  private Event buildSingleCopiedEvent(Event sourceEvent, Calendar sourceCal, Calendar targetCal,
                                       LocalDate targetDate)
      throws ValidationException {
    ZonedDateTime sourceStartZ = sourceEvent.getStart().atZone(sourceCal.getZoneId());
    ZonedDateTime targetStartZ = sourceStartZ.withZoneSameInstant(targetCal.getZoneId());
    ZonedDateTime sourceEndZ = sourceEvent.getEnd().atZone(sourceCal.getZoneId());
    ZonedDateTime targetEndZ = sourceEndZ.withZoneSameInstant(targetCal.getZoneId());

    LocalDateTime newStart = LocalDateTime.of(targetDate, targetStartZ.toLocalTime());
    LocalDateTime newEnd = LocalDateTime.of(targetDate, targetEndZ.toLocalTime());

    boolean crossesMidnight = false;
    if (newEnd.isBefore(newStart)) {
      newEnd = newEnd.plusDays(1);
      crossesMidnight = true;
    }

    Event.EventBuilder builder = Event.builder()
        .fromEvent(sourceEvent)
        .setStart(newStart)
        .setEnd(newEnd);
    if (crossesMidnight) {
      builder.setSeriesId(null);
    }
    return builder.build();
  }

  private void copyEventsBetween(ApplicationManager model, CalendarView view) throws Exception {
    validateCopyEventsBetweenTokens();

    LocalDate sourceDate1 = parseDate(tokens.get(3));
    LocalDate sourceDate2 = parseDate(tokens.get(5));
    String targetCalName = tokens.get(7);
    LocalDate targetStartDate = parseDate(tokens.get(9));

    Calendar sourceCal = model.getActiveCalendar();
    Calendar targetCal = model.getCalendar(targetCalName);

    if (sourceDate1.isAfter(sourceDate2)) {
      LocalDate temp = sourceDate1;
      sourceDate1 = sourceDate2;
      sourceDate2 = temp;
    }

    List<Event> eventsToCopy = findEventsBetween(sourceCal, sourceDate1, sourceDate2);
    List<Event> newEvents =
        buildCopiedEventsForInterval(eventsToCopy, sourceCal, targetCal, sourceDate1,
            targetStartDate);

    addEventsWithConflictCheck(targetCal, newEvents);
    view.showMessage(String.format("Copied %d events to '%s'.", newEvents.size(), targetCalName));
  }

  private void validateCopyEventsBetweenTokens() throws ValidationException {
    if (tokens.size() != 10
        || !"and".equalsIgnoreCase(tokens.get(4))
        || !"--target".equalsIgnoreCase(tokens.get(6))
        || !"to".equalsIgnoreCase(tokens.get(8))) {
      throw new ValidationException(
          "Invalid command. Expected: copy events between <date> "
              + "and <date> --target <calName> to <date>");
    }
  }

  private List<Event> findEventsBetween(Calendar cal, LocalDate startDate, LocalDate endDate) {
    LocalDateTime intervalStart = startDate.atStartOfDay();
    LocalDateTime intervalEnd = endDate.plusDays(1).atStartOfDay();
    Predicate<Event> filter =
        e -> e.getStart().isBefore(intervalEnd) && e.getEnd().isAfter(intervalStart);
    return cal.findEvents(filter);
  }

  private List<Event> buildCopiedEventsForInterval(List<Event> events, Calendar sourceCal,
                                                   Calendar targetCal,
                                                   LocalDate sourceStartDate,
                                                   LocalDate targetStartDate)
      throws ValidationException {
    List<Event> newEvents = new ArrayList<>();
    LocalDateTime intervalStart = sourceStartDate.atStartOfDay();

    for (Event sourceEvent : events) {
      long dayOffset = Duration.between(intervalStart.toLocalDate().atStartOfDay(),
          sourceEvent.getStart().toLocalDate().atStartOfDay()).toDays();
      LocalDate newDate = targetStartDate.plusDays(dayOffset);
      newEvents.add(buildSingleCopiedEvent(sourceEvent, sourceCal, targetCal, newDate));
    }
    return newEvents;
  }

  private void addEventsWithConflictCheck(Calendar cal, List<Event> events)
      throws ValidationException {
    try {
      cal.addEvents(events);
    } catch (ValidationException e) {
      throw new ValidationException("Copy creates a conflict: " + e.getMessage());
    }
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
      String fixed = text.replace("::", ":");
      return LocalDateTime.parse(fixed, DATETIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException(
          "Invalid date/time format. Expected YYYY-MM-DDThh:mm.");
    }
  }
}
