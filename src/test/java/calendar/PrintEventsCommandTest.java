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
 * Ensures that the PrintEventsCommand correctly retrieves events from the model
 * and formats them for display in the console, handling date filtering and empty states properly.
 */
public class PrintEventsCommandTest {

  private ApplicationManager model;
  private CalendarView view;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  private String getLastOutputLine() {
    String output = outContent.toString().trim();
    if (output.isEmpty()) {
      return null;
    }
    String[] lines = output.split(System.lineSeparator());
    return lines[lines.length - 1];
  }

  /**
   * Setup streams.
   */
  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
  }

  /**
   * Restore.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  /**
   * Prepares a calendar with a variety of events (meetings, lunches, conferences)
   * to test printing logic across different dates and times.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new ConsoleView();

    model.createCalendar("Work", ZoneId.of("UTC"));
    model.setActiveCalendar("Work");

    Event sync = Event.builder()
        .setSubject("Morning Standup")
        .setStart(LocalDateTime.of(2025, 11, 20, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 10, 0))
        .setLocation("Zoom Room 1")
        .build();

    Event lunch = Event.builder()
        .setSubject("Team Lunch")
        .setStart(LocalDateTime.of(2025, 11, 20, 12, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 13, 0))
        .setLocation("Cafeteria")
        .build();

    Event checkIn = Event.builder()
        .setSubject("Project Sync")
        .setStart(LocalDateTime.of(2025, 11, 20, 15, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 15, 30))
        .setLocation("Huddle Room")
        .build();

    Event futureEvent = Event.builder()
        .setSubject("Q4 Planning")
        .setStart(LocalDateTime.of(2025, 11, 21, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 21, 11, 30))
        .setLocation("Boardroom")
        .build();

    Event remoteWork = Event.builder()
        .setSubject("Focus Time")
        .setStart(LocalDateTime.of(2025, 11, 20, 10, 30))
        .setEnd(LocalDateTime.of(2025, 11, 20, 11, 0))
        .setLocation("")
        .build();

    Event workshop = Event.builder()
        .setSubject("Workshop: Agile")
        .setStart(LocalDateTime.of(2025, 12, 1, 11, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 13, 0))
        .build();

    Event demo = Event.builder()
        .setSubject("Product Demo")
        .setStart(LocalDateTime.of(2025, 12, 1, 13, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 15, 0))
        .build();

    Event conference = Event.builder()
        .setSubject("Tech Conference")
        .setStart(LocalDateTime.of(2025, 12, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 16, 0))
        .build();

    Event early = Event.builder()
        .setSubject("Early Bird Coffee")
        .setStart(LocalDateTime.of(2025, 12, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 11, 0))
        .build();

    Event late = Event.builder()
        .setSubject("Late Wrap-up")
        .setStart(LocalDateTime.of(2025, 12, 1, 15, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 16, 0))
        .build();

    model.getActiveCalendar().addEvents(List.of(
        sync, lunch, checkIn, futureEvent, remoteWork,
        workshop, demo, conference, early, late
    ));
  }

  @Test(expected = ValidationException.class)
  public void testFailsWhenRangeMissingEnd() throws Exception {
    List<String> tokens = List.of("print", "events", "from", "2025-11-20T08:00", "to");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testFailsWithInvalidDate() throws Exception {
    List<String> tokens = List.of("print", "events", "on", "2025-99-99");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanPrintEventsOnSpecificDate() throws Exception {
    List<String> tokens = List.of("print", "events", "on", "2025-11-20");
    new PrintEventsCommand(tokens).execute(model, view);

    String output = outContent.toString();
    assertTrue(output.contains("* Morning Standup from 09:00 AM to 10:00 AM at Zoom Room 1"));
    assertTrue(output.contains("* Focus Time from 10:30 AM to 11:00 AM"));
    assertTrue(output.contains("* Team Lunch from 12:00 PM to 01:00 PM at Cafeteria"));
    assertTrue(output.contains("* Project Sync from 03:00 PM to 03:30 PM at Huddle Room"));
    assertFalse(output.contains("Q4 Planning"));
  }

  @Test(expected = ValidationException.class)
  public void testFailsWhenEndIsBeforeStart() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-20T12:00", "to", "2025-11-20T08:00");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test
  public void testShowsNoEventsMessageForEmptyRange() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-22T08:00", "to", "2025-11-22T12:00");
    new PrintEventsCommand(tokens).execute(model, view);

    assertEquals("No events found in this range.", getLastOutputLine());
  }

  @Test(expected = ValidationException.class)
  public void testFailsWithInvalidDateTime() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-20T99:00", "to", "2025-11-20T12:00");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanPrintEventsInTimeRange() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-11-20T08:00", "to", "2025-11-20T12:30");
    new PrintEventsCommand(tokens).execute(model, view);

    String output = outContent.toString();
    String expected1 = "* Morning Standup starting on 2025-11-20 at 09:00 AM, "
        + "ending on 2025-11-20 at 10:00 AM at Zoom Room 1";
    String expected2 = "* Focus Time starting on 2025-11-20 at 10:30 AM, "
        + "ending on 2025-11-20 at 11:00 AM";
    String expected3 = "* Team Lunch starting on 2025-11-20 at 12:00 PM, "
        + "ending on 2025-11-20 at 01:00 PM at Cafeteria";

    assertTrue(output.contains(expected1));
    assertTrue(output.contains(expected2));
    assertTrue(output.contains(expected3));
    assertFalse(output.contains("Project Sync"));
  }

  @Test
  public void testShowsNoEventsMessageForEmptyDate() throws Exception {
    List<String> tokens = List.of("print", "events", "on", "2025-11-22");
    new PrintEventsCommand(tokens).execute(model, view);

    assertEquals("No events scheduled.", getLastOutputLine());
  }

  @Test
  public void testHandlesOverlappingEventsInRange() throws Exception {
    List<String> tokens = List.of("print", "events", "from",
        "2025-12-01T12:00", "to", "2025-12-01T14:00");
    new PrintEventsCommand(tokens).execute(model, view);

    String output = outContent.toString();

    String expectedLine1 = "* Tech Conference starting on 2025-12-01 "
        + "at 10:00 AM, ending on 2025-12-01 at 04:00 PM";
    String expectedLine2 = "* Workshop: Agile starting on 2025-12-01 "
        + "at 11:00 AM, ending on 2025-12-01 at 01:00 PM";
    String expectedLine3 = "* Product Demo starting on 2025-12-01 "
        + "at 01:00 PM, ending on 2025-12-01 at 03:00 PM";

    assertTrue("Should include the event that envelops the range",
        output.contains(expectedLine1));
    assertTrue("Should include the event that starts before and ends during",
        output.contains(expectedLine2));
    assertTrue("Should include the event that starts during and ends after",
        output.contains(expectedLine3));

    assertFalse("Should not include event ending before the start",
        output.contains("Early Bird Coffee"));
    assertFalse("Should not include event starting after the end",
        output.contains("Late Wrap-up"));

    assertFalse("Should not include event from 11-20",
        output.contains("Morning Standup"));
  }

  @Test(expected = ValidationException.class)
  public void testFailsWhenCommandIsTooShort() throws Exception {
    List<String> tokens = List.of("print", "events", "on");
    new PrintEventsCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testFailsWithInvalidPreposition() throws Exception {
    List<String> tokens = List.of("print", "events", "at", "2025-11-20");
    new PrintEventsCommand(tokens).execute(model, view);
  }
}