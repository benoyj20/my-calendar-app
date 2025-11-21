package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.util.List;

/**
 * Sets the active calendar context.
 */
public class UseCalendarCommand implements Command {

  private final List<String> tokens;

  /**
   * Constructor for setting an active calendar.
   */
  public UseCalendarCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    if (tokens.size() != 4 || !tokens.get(2).equalsIgnoreCase("--name")) {
      throw new ValidationException(
          "Invalid command. Expected: use calendar --name <name-of-calendar>");
    }
    String name = tokens.get(3);
    model.setActiveCalendar(name);
    view.showMessage("Now using calendar '" + name + "'.");
  }
}