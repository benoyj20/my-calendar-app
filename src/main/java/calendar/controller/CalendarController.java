package calendar.controller;

import calendar.controller.commands.Command;
import calendar.controller.commands.ExitCommand;
import calendar.model.ApplicationManager;
import calendar.view.CalendarView;
import java.util.function.Supplier;

/**
 * Main application controller: runs the command loop and delegates to commands
 * according to the format to see which operation should be run.
 */
public class CalendarController {

  private final ApplicationManager model;
  private final CalendarView view;
  private final CommandParser parser;
  private boolean exitCommandReceived = false;

  /**
   * Creates a controller with model, view and parser.
   *
   * @param model  calendar model
   * @param view   output view
   * @param parser command parser
   */
  public CalendarController(final ApplicationManager model,
                            final CalendarView view,
                            final CommandParser parser) {
    this.model = model;
    this.view = view;
    this.parser = parser;
  }

  /**
   * Runs the main loop until an exitCommand is received.
   *
   * @param commandSupplier produces the next command string
   */
  public void run(final Supplier<String> commandSupplier) {
    String commandLine;
    while (!exitCommandReceived) {
      commandLine = commandSupplier.get();

      if (commandLine == null) {
        break;
      }

      commandLine = commandLine.trim();
      if (commandLine.isEmpty()) {
        continue;
      }

      try {
        Command command = parser.parse(commandLine);

        if (command instanceof ExitCommand) {
          this.exitCommandReceived = true;
        }

        command.execute(model, view);
      } catch (Exception e) {
        view.showError(e.getMessage());
      }
    }
  }

  /**
   * Returns whether an exit command was received.
   *
   * @return true if exit was requested
   */
  public boolean isExitCommandReceived() {
    return exitCommandReceived;
  }
}