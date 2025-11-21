package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Command to check the availability of the user at a specific date and time.
 * Determines whether the active calendar has any events overlapping
 * with the specified instant.
 */
public class StatusCommand implements Command {

  private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final List<String> tokens;

  /**
   * Constructs a new StatusCommand.
   *
   * @param tokens tokenized command input representing the status check
   *               and its arguments
   */
  public StatusCommand(final List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(final ApplicationManager model, final CalendarView view) throws Exception {
    Calendar cal = getActiveCalendar(model);
    validateTokens();

    LocalDateTime when = parseDateTime(tokens.get(3));
    List<Event> matches = cal.findEvents(isBusyAt(when));

    view.showMessage(matches.isEmpty() ? "Available" : "Busy");
  }

  private Calendar getActiveCalendar(ApplicationManager model) throws ValidationException {
    try {
      return model.getActiveCalendar();
    } catch (ValidationException e) {
      throw new ValidationException(
          "Error: No calendar is active. Use 'use calendar --name <calName>' first."
      );
    }
  }

  private void validateTokens() throws ValidationException {
    if (tokens.size() != 4 || !tokens.get(2).equalsIgnoreCase("on")) {
      throw new ValidationException(
          "Invalid 'show status' command. Expected: show status on YYYY-MM-DDThh:mm"
      );
    }
  }

  private Predicate<Event> isBusyAt(LocalDateTime when) {
    return e -> !e.getStart().isAfter(when) && e.getEnd().isAfter(when);
  }

  private LocalDateTime parseDateTime(final String text) throws ValidationException {
    try {
      String fixed = text.replace("::", ":");
      return LocalDateTime.parse(fixed, DATETIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException(
          "Invalid date/time format. Expected YYYY-MM-DDThh:mm."
      );
    }
  }
}
