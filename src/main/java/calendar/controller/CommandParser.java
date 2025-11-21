package calendar.controller;

import calendar.controller.commands.Command;
import calendar.controller.commands.CopyCommand;
import calendar.controller.commands.CreateCalendarCommand;
import calendar.controller.commands.CreateEventCommand;
import calendar.controller.commands.EditCalendarCommand;
import calendar.controller.commands.EditEventCommand;
import calendar.controller.commands.ExitCommand;
import calendar.controller.commands.ExportCommand;
import calendar.controller.commands.PrintEventsCommand;
import calendar.controller.commands.StatusCommand;
import calendar.controller.commands.UnknownCommand;
import calendar.controller.commands.UseCalendarCommand;
import calendar.model.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Parses a command string into an executable command object.
 */
public class CommandParser {

  private final Map<String, Function<List<String>, Command>> commandMap = new HashMap<>();

  /**
   * Creates a new executable command object.
   */
  public CommandParser() {
    commandMap.put("create calendar", CreateCalendarCommand::new);
    commandMap.put("edit calendar", EditCalendarCommand::new);
    commandMap.put("use calendar", UseCalendarCommand::new);
    commandMap.put("copy event", CopyCommand::new);
    commandMap.put("copy events", CopyCommand::new);
    commandMap.put("create event", CreateEventCommand::new);
    commandMap.put("edit event", EditEventCommand::new);
    commandMap.put("edit events", EditEventCommand::new);
    commandMap.put("edit series", EditEventCommand::new);
    commandMap.put("print events", PrintEventsCommand::new);
    commandMap.put("export cal", ExportCommand::new);
    commandMap.put("show status", StatusCommand::new);
    commandMap.put("exit", tokens -> new ExitCommand());
  }

  /**
   * Parses the given command line into a Command.
   *
   * @param commandLine raw command
   * @return executable command
   * @throws ValidationException if the command is incorrect
   */
  public Command parse(final String commandLine) throws ValidationException {
    List<String> tokens = CommandTokenizer.tokenize(commandLine);
    if (tokens.isEmpty()) {
      return new UnknownCommand("Empty command");
    }

    String commandWord = tokens.get(0).toLowerCase();
    if (tokens.size() > 1) {
      commandWord += " " + tokens.get(1).toLowerCase();
    }

    return commandMap.getOrDefault(commandWord, t -> new UnknownCommand(commandLine))
        .apply(tokens);
  }
}
