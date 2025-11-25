package calendar;

import static org.junit.Assert.assertTrue;

import calendar.controller.CommandParser;
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
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the CommandParser correctly translates user input strings into
 * the appropriate executable Command objects.
 */
public class CommandParserTest {

  private CommandParser parser;

  /**
   * Initializes a parser instance before each test run.
   */
  @Before
  public void setUp() {
    parser = new CommandParser();
  }

  @Test
  public void testCanParseEditCalendarCommands() throws ValidationException {
    Command cmd = parser.parse("edit calendar --name \"Work\" "
        + "--property timezone \"Europe/London\"");
    assertTrue(cmd instanceof EditCalendarCommand);
  }

  @Test
  public void testCanParseCreateEventCommands() throws ValidationException {
    Command cmd = parser.parse("create event \"Team Meeting\" on 2025-11-15");
    assertTrue(cmd instanceof CreateEventCommand);
  }

  @Test
  public void testCanParseExportCommands() throws ValidationException {
    Command cmd = parser.parse("export cal backup_2025.csv");
    assertTrue(cmd instanceof ExportCommand);
  }

  @Test
  public void testCanParseUnknownCommands() throws ValidationException {
    Command cmd = parser.parse("make me a sandwich");
    assertTrue(cmd instanceof UnknownCommand);
  }

  @Test
  public void testCanParseUseCalendarCommands() throws ValidationException {
    Command cmd = parser.parse("use calendar --name \"Family\"");
    assertTrue(cmd instanceof UseCalendarCommand);
  }

  @Test
  public void testCanParseCreateCalendarCommands() throws ValidationException {
    Command cmd = parser.parse("create calendar --name \"Personal\" --timezone "
        + "\"America/New_York\"");
    assertTrue(cmd instanceof CreateCalendarCommand);
  }

  @Test
  public void testCanParsePrintEventsCommands() throws ValidationException {
    Command cmd = parser.parse("print events on 2025-12-25");
    assertTrue(cmd instanceof PrintEventsCommand);
  }

  @Test
  public void testCanParseStatusCommands() throws ValidationException {
    Command cmd = parser.parse("show status on 2025-01-01T09:00");
    assertTrue(cmd instanceof StatusCommand);
  }

  @Test
  public void testCanParseCopyCommands() throws ValidationException {
    Command cmdEvent = parser.parse("copy event \"Sync\" on 2025-01-01T10:00 --target "
        + "\"Work\" to 2025-01-02T10:00");
    assertTrue(cmdEvent instanceof CopyCommand);

    Command cmdEvents = parser.parse("copy events on 2025-01-01 --target \"Home\" "
        + "to 2025-01-02");
    assertTrue(cmdEvents instanceof CopyCommand);
  }

  @Test
  public void testCanParseExitCommand() throws ValidationException {
    Command cmd = parser.parse("exit");
    assertTrue(cmd instanceof ExitCommand);
  }

  @Test
  public void testCanParseEditEventCommands() throws ValidationException {
    assertTrue(parser.parse("edit event subject \"Old\" from 2025-01-01T10:00 with "
        + "\"New\"") instanceof EditEventCommand);
    assertTrue(parser.parse("edit events location \"Meeting\" from 2025-01-01T10:00 "
        + "with \"Room 2\"") instanceof EditEventCommand);
    assertTrue(parser.parse("edit series description \"Weekly\" from 2025-01-01T10:00 "
        + "with \"Updated\"") instanceof EditEventCommand);
  }

  @Test
  public void testHandlesEmptyAndWhitespaceCommands() throws ValidationException {
    Command cmd = parser.parse("");
    assertTrue(cmd instanceof UnknownCommand);
    Command cmdWhitespace = parser.parse("   ");
    assertTrue(cmdWhitespace instanceof UnknownCommand);
  }
}