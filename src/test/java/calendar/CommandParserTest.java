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
 * Tests the {@link CommandParser} class to ensure it maps
 * command strings to the correct {@link Command} objects.
 */
public class CommandParserTest {

  private CommandParser parser;

  /**
   * Sets up a new parser before each test.
   */
  @Before
  public void setUp() {
    parser = new CommandParser();
  }

  /**
   * Tests parsing of the "create calendar" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseCreateCalendar() throws ValidationException {
    Command cmd = parser.parse("create calendar --name \"Test\" --timezone \"UTC\"");
    assertTrue(cmd instanceof CreateCalendarCommand);
  }

  /**
   * Tests parsing of the "edit calendar" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseEditCalendar() throws ValidationException {
    Command cmd = parser.parse("edit calendar --name \"Test\" --property name \"New\"");
    assertTrue(cmd instanceof EditCalendarCommand);
  }

  /**
   * Tests parsing of the "use calendar" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseUseCalendar() throws ValidationException {
    Command cmd = parser.parse("use calendar --name \"Test\"");
    assertTrue(cmd instanceof UseCalendarCommand);
  }

  /**
   * Tests parsing of both "copy" command variants.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseCopyCommands() throws ValidationException {
    Command cmdEvent = parser.parse("copy event \"Meeting\" on ...");
    assertTrue(cmdEvent instanceof CopyCommand);

    Command cmdEvents = parser.parse("copy events on ...");
    assertTrue(cmdEvents instanceof CopyCommand);
  }

  /**
   * Tests parsing of the "create event" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseCreateEvent() throws ValidationException {
    Command cmd = parser.parse("create event \"Test\" on 2025-01-01");
    assertTrue(cmd instanceof CreateEventCommand);
  }

  /**
   * Tests parsing of all three "edit" command variants.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseEditCommands() throws ValidationException {
    assertTrue(parser.parse("edit event ...") instanceof EditEventCommand);
    assertTrue(parser.parse("edit events ...") instanceof EditEventCommand);
    assertTrue(parser.parse("edit series ...") instanceof EditEventCommand);
  }

  /**
   * Tests parsing of the "print events" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParsePrintEvents() throws ValidationException {
    Command cmd = parser.parse("print events on 2025-01-01");
    assertTrue(cmd instanceof PrintEventsCommand);
  }

  /**
   * Tests parsing of the "export cal" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseExportCal() throws ValidationException {
    Command cmd = parser.parse("export cal my_file.csv");
    assertTrue(cmd instanceof ExportCommand);
  }

  /**
   * Tests parsing of the "show status" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseShowStatus() throws ValidationException {
    Command cmd = parser.parse("show status on 2025-01-01T10:00");
    assertTrue(cmd instanceof StatusCommand);
  }

  /**
   * Tests parsing of the "exit" command.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseExit() throws ValidationException {
    Command cmd = parser.parse("exit");
    assertTrue(cmd instanceof ExitCommand);
  }

  /**
   * Tests that an unrecognized command maps to UnknownCommand.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseUnknown() throws ValidationException {
    Command cmd = parser.parse("delete everything");
    assertTrue(cmd instanceof UnknownCommand);
  }

  /**
   * Tests that an empty or whitespace string maps to UnknownCommand.
   *
   * @throws ValidationException if parse fails
   */
  @Test
  public void testParseEmptyCommand() throws ValidationException {
    Command cmd = parser.parse("");
    assertTrue(cmd instanceof UnknownCommand);
    Command cmdWhitespace = parser.parse("   ");
    assertTrue(cmdWhitespace instanceof UnknownCommand);
  }

}