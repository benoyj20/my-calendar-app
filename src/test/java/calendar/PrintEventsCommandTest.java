package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.commands.PrintEventsCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import calendar.view.ConsoleView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link PrintEventsCommand} class.
 * This test integrates with a real ConsoleView to capture
 * and verify the actual formatted string output.
 */
public class PrintEventsCommandTest {

  private ApplicationManager model;
  private CalendarView view;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  /**
   * Helper to get the last non-empty line from the captured output.
   *
   * @return The last non-empty line of System.out, or null.
   */
  private String getLastOutputLine() {
    String output = outContent.toString().trim();
    if (output.isEmpty()) {
      return null;
    }
    String[] lines = output.split(System.lineSeparator());
    return lines[lines.length - 1];
  }

  /**
   * Sets up stream redirection before each test.
   */
  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
  }

  /**
   * Restores original streams after each test.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  /**
   * Sets up a model with one event in an active calendar.
   *
   * @throws ValidationException if event creation fails
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new ConsoleView();

    model.createCalendar("TestCal", ZoneId.of("UTC"));
    model.setActiveCalendar("TestCal");

    Event event1 = Event.builder()
        .setSubject("Morning Sync")
        .setStart(LocalDateTime.of(2025, 11, 20, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 10, 0))
        .setLocation("Office")
        .build();

    Event event2 = Event.builder()
        .setSubject("Lunch")
        .setStart(LocalDateTime.of(2025, 11, 20, 12, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 13, 0))
        .setLocation("Cafe")
        .build();
    Event event3 = Event.builder()
        .setSubject("Afternoon Check-in")
        .setStart(LocalDateTime.of(2025, 11, 20, 15, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 15, 30))
        .setLocation("Room 3")
        .build();
    Event event4 = Event.builder()
        .setSubject("Next Day Project")
        .setStart(LocalDateTime.of(2025, 11, 21, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 21, 11, 30))
        .setLocation("Lab")
        .build();

    Event eventNoLoc = Event.builder()
        .setSubject("Event No Location")
        .setStart(LocalDateTime.of(2025, 11, 20, 10, 30))
        .setEnd(LocalDateTime.of(2025, 11, 20, 11, 0))
        .setLocation("")
        .build();

    Event planningSession = Event.builder()
        .setSubject("Planning Session")
        .setStart(LocalDateTime.of(2025, 12, 1, 11, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 13, 0))
        .build();
    Event designReview = Event.builder()
        .setSubject("Design Review")
        .setStart(LocalDateTime.of(2025, 12, 1, 13, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 15, 0))
        .build();
    Event allDayConference = Event.builder()
        .setSubject("All-Day Conference")
        .setStart(LocalDateTime.of(2025, 12, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 16, 0))
        .build();
    Event coffeeRun = Event.builder()
        .setSubject("Coffee Run")
        .setStart(LocalDateTime.of(2025, 12, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 11, 0))
        .build();
    Event execBriefing = Event.builder()
        .setSubject("Exec Briefing")
        .setStart(LocalDateTime.of(2025, 12, 1, 15, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 16, 0))
        .build();

    model.getActiveCalendar().addEvents(List.of(
        event1, event2, event3, event4, eventNoLoc,
        planningSession, designReview, allDayConference, coffeeRun, execBriefing
    ));
  }

  @Test
  public void testPrintEventsOnDate() throws Exception {
    List<String> tokens = List.of("print", "events", "on", "2025-11-20");
    new PrintEventsCommand(tokens).execute(model, view);

    String output = outContent.toString();
    assertTrue(output.contains("* Morning Sync from 09:00 AM to 10:00 AM at Office"));
    assertTrue(output.contains("* Event No Location from 10:30 AM to 11:00 AM"));
    assertTrue(output.contains("* Lunch from 12:00 PM to 01:00 PM at Cafe"));
    assertTrue(output.contains("* Afternoon Check-in from 03:00 PM to 03:30 PM at Room 3"));
    assertFalse(output.contains("Next Day Project"));
  }

  @Test
  public void testPrintEventsInRange() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-20T08:00", "to", "2025-11-20T12:30");
    new PrintEventsCommand(tokens).execute(model, view);

    String output = outContent.toString();
    String expected1 = "* Morning Sync starting on 2025-11-20 at 09:00 AM, "
        + "ending on 2025-11-20 at 10:00 AM at Office";
    String expected2 = "* Event No Location starting on 2025-11-20 at 10:30 AM, "
        + "ending on 2025-11-20 at 11:00 AM";
    String expected3 = "* Lunch starting on 2025-11-20 at 12:00 PM, "
        + "ending on 2025-11-20 at 01:00 PM at Cafe";

    assertTrue(output.contains(expected1));
    assertTrue(output.contains(expected2));
    assertTrue(output.contains(expected3));
    assertFalse(output.contains("Afternoon Check-in"));
  }

  @Test
  public void testPrintEventsOnDateNoEvents() throws Exception {
    List<String> tokens = List.of("print", "events", "on", "2025-11-22");
    new PrintEventsCommand(tokens).execute(model, view);

    assertEquals("No events scheduled.", getLastOutputLine());
  }

  @Test
  public void testPrintEventsInRangeNoEvents() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-22T08:00", "to", "2025-11-22T12:00");
    new PrintEventsCommand(tokens).execute(model, view);

    assertEquals("No events found in this range.", getLastOutputLine());
  }

  @Test(expected = ValidationException.class)
  public void testPrintSyntaxErrorShort() throws Exception {
    List<String> tokens = List.of("print", "events", "on");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testPrintSyntaxErrorBadType() throws Exception {
    List<String> tokens = List.of("print", "events", "at", "2025-11-20");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testPrintSyntaxErrorRangeShort() throws Exception {
    List<String> tokens = List.of("print", "events", "from", "2025-11-20T08:00", "to");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testPrintSyntaxErrorEndBeforeStart() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-20T12:00", "to", "2025-11-20T08:00");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testPrintSyntaxErrorBadDate() throws Exception {
    List<String> tokens = List.of("print", "events", "on", "2025-99-99");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testPrintSyntaxErrorBadDateTime() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-20T99:00", "to", "2025-11-20T12:00");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test
  public void testPrintEventsInRangePartialOverlaps() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-12-01T12:00", "to", "2025-12-01T14:00");
    new PrintEventsCommand(tokens).execute(model, view);

    String output = outContent.toString();

    String expectedLine1 = "* All-Day Conference starting on 2025-12-01 "
        + "at 10:00 AM, ending on 2025-12-01 at 04:00 PM";
    String expectedLine2 = "* Planning Session starting on 2025-12-01 "
        + "at 11:00 AM, ending on 2025-12-01 at 01:00 PM";
    String expectedLine3 = "* Design Review starting on 2025-12-01 "
        + "at 01:00 PM, ending on 2025-12-01 at 03:00 PM";

    assertTrue("Should include the event that envelops the range",
        output.contains(expectedLine1));
    assertTrue("Should include the event that starts before and ends during",
        output.contains(expectedLine2));
    assertTrue("Should include the event that starts during and ends after",
        output.contains(expectedLine3));

    assertFalse("Should not include event ending before the start",
        output.contains("Coffee Run"));
    assertFalse("Should not include event starting after the end",
        output.contains("Exec Briefing"));

    assertFalse("Should not include event from 11-20",
        output.contains("Morning Sync"));
  }
}