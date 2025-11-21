package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.ValidationException;
import calendar.view.CalendarView;

/**
 * Command to handle unrecognized input.
 */
public class UnknownCommand implements Command {

  private final String commandLine;

  /**
   * Creates an unknown command wrapper.
   *
   * @param commandLine original text
   */
  public UnknownCommand(final String commandLine) {
    this.commandLine = commandLine;
  }

  /**
   * Always throws a ValidationException.
   */
  @Override
  public void execute(final ApplicationManager model, final CalendarView view)
      throws ValidationException {
    throw new ValidationException("Invalid or unexpected command: " + commandLine);
  }
}