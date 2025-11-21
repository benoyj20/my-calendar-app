package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

/**
 * Handles editing of an existing calendar.
 * This command allows updating properties of the active calendar,
 * such as its name or other metadata.
 */
public class EditCalendarCommand implements Command {

  private final List<String> tokens;

  /**
   * Constructs a new EditCalendarCommand.
   *
   * @param tokens tokenized input representing the edit calendar command
   *               and its arguments
   */
  public EditCalendarCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    validateTokens();
    String name = tokens.get(3);
    String property = tokens.get(5).toLowerCase();
    String value = tokens.get(6);
    editProperty(model, view, name, property, value);
  }

  private void validateTokens() throws ValidationException {
    if (tokens.size() != 7
        || !"--name".equalsIgnoreCase(tokens.get(2))
        || !"--property".equalsIgnoreCase(tokens.get(4))) {
      throw new ValidationException(
          "Invalid command. Expected: edit calendar --name <name> --property <prop> <value>");
    }
  }

  private void editProperty(ApplicationManager model, CalendarView view,
                            String name, String property, String value) throws ValidationException {
    switch (property) {
      case "name":
        editName(model, view, name, value);
        break;
      case "timezone":
        editTimezone(model, view, name, value);
        break;
      default:
        throw new ValidationException(
            "Unknown property '" + property + "'. Can only edit 'name' or 'timezone'.");
    }
  }

  private void editName(ApplicationManager model, CalendarView view,
                        String name, String newName) throws ValidationException {
    model.editCalendarName(name, newName);
    view.showMessage("Calendar '" + name + "' renamed to '" + newName + "'.");
  }

  private void editTimezone(ApplicationManager model, CalendarView view,
                            String name, String value) throws ValidationException {
    try {
      ZoneId zoneId = ZoneId.of(value);
      model.editCalendarTimezone(name, zoneId);
      view.showMessage("Calendar '" + name + "' timezone updated to '" + value + "'.");
    } catch (DateTimeException e) {
      throw new ValidationException("Invalid timezone format: " + value);
    }
  }
}
