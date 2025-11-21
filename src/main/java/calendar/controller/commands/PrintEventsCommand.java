package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Command to print events from the active calendar.
 * The command parses the input tokens to determine which events to retrieve
 * and passes them to the view for display.
 */
public class PrintEventsCommand implements Command {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ISO_LOCAL_DATE;

  private static final DateTimeFormatter DATETIME_FORMAT =
      DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final List<String> tokens;

  /**
   * Constructs a new PrintEventsCommand.
   *
   * @param tokens tokenized command input representing the print command
   *               and its arguments
   */
  public PrintEventsCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view)
      throws Exception {

    Calendar activeCalendar = getActiveCalendar(model);
    validateTokenCount();

    String type = tokens.get(2).toLowerCase();

    if (type.equals("on")) {
      handlePrintOn(activeCalendar, view);
    } else if (type.equals("from")) {
      handlePrintFrom(activeCalendar, view);
    } else {
      throw new ValidationException(
          "Invalid 'print events' command. Expected 'on' or 'from'.");
    }
  }

  private Calendar getActiveCalendar(ApplicationManager model)
      throws ValidationException {
    try {
      return model.getActiveCalendar();
    } catch (ValidationException e) {
      throw new ValidationException(
          "Error: No calendar is active. Use 'use calendar --name <calName>' first.");
    }
  }

  private void validateTokenCount() throws ValidationException {
    if (tokens.size() < 4) {
      throw new ValidationException("Invalid 'print events' command.");
    }
  }

  private void handlePrintOn(Calendar cal, CalendarView view)
      throws ValidationException {

    LocalDate date = parseDate(tokens.get(3));
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    Predicate<Event> filter =
        e -> e.getStart().isBefore(end) && e.getEnd().isAfter(start);

    List<Event> events = cal.findEvents(filter);
    view.printEventsOnDate(events);
  }

  private void handlePrintFrom(Calendar cal, CalendarView view)
      throws ValidationException {

    if (tokens.size() < 6) {
      throw new ValidationException("Invalid 'print events from' command.");
    }
    LocalDateTime start = parseDateTime(tokens.get(3));
    LocalDateTime end = parseDateTime(tokens.get(5));

    if (end.isBefore(start)) {
      throw new ValidationException(
          "End of range cannot be before start of range.");
    }

    Predicate<Event> filter =
        e -> e.getStart().isBefore(end) && e.getEnd().isAfter(start);
    List<Event> events = cal.findEvents(filter);
    view.printEventsInRange(events);
  }

  private LocalDate parseDate(String text) throws ValidationException {
    try {
      return LocalDate.parse(text, DATE_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException("Invalid date format. Expected YYYY-MM-DD.");
    }
  }

  private LocalDateTime parseDateTime(String text)
      throws ValidationException {
    try {
      String cleaned = text.replace("::", ":");
      return LocalDateTime.parse(cleaned, DATETIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException(
          "Invalid date/time format. Expected YYYY-MM-DDThh:mm.");
    }
  }
}
