package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.view.CalendarView;

/**
 * A command that performs an action on the calendar.
 */
public interface Command {

  /**
   * Executes the command.
   *
   * @param model the application model (now ApplicationManager)
   * @param view  the output view
   * @throws Exception if parsing or execution fails
   */
  void execute(ApplicationManager model, CalendarView view) throws Exception;
}