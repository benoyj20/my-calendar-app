package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.view.CalendarView;

/**
 * Command that signals the controller to exit.
 */
public class ExitCommand implements Command {

  /**
   * Prints an exit message; controller stops after seeing this type.
   */
  @Override
  public void execute(final ApplicationManager model, final CalendarView view) {
    view.showMessage("Exiting calendar...");
  }
}