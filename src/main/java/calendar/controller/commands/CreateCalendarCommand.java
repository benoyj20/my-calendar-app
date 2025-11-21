package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

/**
 * Creates a new calendar in the application.
 * This command parses the provided tokens to extract the calendar name
 * and other parameters, then instructs the model to create it.
 */
public class CreateCalendarCommand implements Command {

  private final List<String> tokens;

  /**
   * Constructs a new CreateCalendarCommand.
   *
   * @param tokens tokenized input representing the command and its arguments
   */
  public CreateCalendarCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    validateTokens();
    String name = tokens.get(3);
    ZoneId zoneId = parseZoneId(tokens.get(5));
    model.createCalendar(name, zoneId);
    view.showMessage("Calendar '" + name + "' created successfully.");
  }

  private void validateTokens() throws ValidationException {
    if (tokens.size() != 6
        || !"--name".equalsIgnoreCase(tokens.get(2))
        || !"--timezone".equalsIgnoreCase(tokens.get(4))) {
      throw new ValidationException(
          "Invalid command. Expected: create calendar --name <calName> --timezone area/location");
    }
  }

  private ZoneId parseZoneId(String zoneStr) throws ValidationException {
    try {
      return ZoneId.of(zoneStr);
    } catch (DateTimeException e) {
      throw new ValidationException("Invalid timezone format: " + zoneStr);
    }
  }
}
