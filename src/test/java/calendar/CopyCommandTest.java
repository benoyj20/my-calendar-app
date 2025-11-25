package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.commands.CopyCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the CopyCommand properly duplicates events across calendars,
 * handling timezone conversions and recurring series logic correctly.
 */
public class CopyCommandTest {

  private ApplicationManager model;
  private TestView view;
  private Event meeting;
  private Event sync1;
  private Event sync2;
  private UUID seriesId;
  private ZoneId zoneEst;
  private ZoneId zonePst;


  /**
   * Prepares a test environment with "Work" (EST) and "Home" (PST) calendars,
   * and "Work" with a single meeting and a recurring sync series.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    zoneEst = ZoneId.of("America/New_York");
    zonePst = ZoneId.of("America/Los_Angeles");

    model.createCalendar("Work", zoneEst);
    model.createCalendar("Home", zonePst);
    model.setActiveCalendar("Work");

    seriesId = UUID.randomUUID();

    meeting = Event.builder()
        .setSubject("Client Call")
        .setStart(LocalDateTime.of(2025, 11, 10, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 10, 10, 0))
        .build();

    sync1 = Event.builder()
        .setSubject("Team Sync")
        .setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 17, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 17, 15, 0))
        .build();

    sync2 = Event.builder()
        .setSubject("Team Sync")
        .setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 19, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 19, 15, 0))
        .build();

    model.getActiveCalendar().addEvents(List.of(meeting, sync1, sync2));
  }

  @Test
  public void testCopySingleEventSuccessfully() throws Exception {
    List<String> tokens = List.of("copy", "event", "Client Call", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Event copied to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Client Call"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 1, 15, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 1, 16, 0), copiedEvent.getEnd());
    assertNotEquals("Series ID should be broken", meeting.getSeriesId(), copiedEvent.getSeriesId());
  }

  @Test
  public void testCopyEventsOnDateHandlesTimezones() throws Exception {
    List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
        "--target", "Home", "to", "2025-12-02");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Copied 1 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Client Call"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    // 9 AM EST is 6 AM PST
    assertEquals(LocalDateTime.of(2025, 12, 2, 6, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 2, 7, 0), copiedEvent.getEnd());
    assertEquals("Series ID should be retained", meeting.getSeriesId(), copiedEvent.getSeriesId());
  }

  @Test
  public void testCopyRangeHandlesReversedDates() throws Exception {
    List<String> tokens = List.of("copy", "events", "between", "2025-11-20", "and", "2025-11-17",
        "--target", "Home", "to", "2026-01-10");
    new CopyCommand(tokens).execute(model, view);

    assertEquals("Copied 2 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Team Sync"));
    assertEquals(2, events.size());

    Event copiedS1 = events.get(0);
    Event copiedS2 = events.get(1);

    assertEquals(LocalDateTime.of(2026, 1, 10, 11, 0), copiedS1.getStart());
    assertEquals(LocalDateTime.of(2026, 1, 12, 11, 0), copiedS2.getStart());
  }

  @Test(expected = ValidationException.class)
  public void testCopyFailsIfEventNotFound() throws Exception {
    List<String> tokens = List.of("copy", "event", "NonExistent", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testCopyFailsIfTargetCalendarMissing() throws Exception {
    List<String> tokens = List.of("copy", "event", "Client Call", "on", "2025-11-10T09:00",
        "--target", "Vacation", "to", "2025-12-01T15:00");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test
  public void testCopyFailsOnConflict() throws Exception {
    Event conflict = Event.builder()
        .setSubject("Client Call")
        .setStart(LocalDateTime.of(2025, 12, 1, 15, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 16, 0))
        .build();
    model.getCalendar("Home").addEvent(conflict);

    List<String> tokens = List.of("copy", "event", "Client Call", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");

    try {
      new CopyCommand(tokens).execute(model, view);
      fail("Expected ValidationException for conflict.");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("An event with the "
          + "same subject, start, and end time already exists."));
    }
  }

  @Test(expected = ValidationException.class)
  public void testCopyEventRejectsBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "event", "Client Call", "on", "2025-11-10T09:00");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testCopyEventsOnRejectsBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
        "to", "2025-12-02");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testCopyEventsBetweenRejectsBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "events", "between", "2025-11-17",
        "and", "2025-11-20", "to", "2026-01-10");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testCopyFailsWithoutActiveCalendar() throws Exception {
    ApplicationManager freshModel = new ApplicationManagerImpl();
    freshModel.createCalendar("Work", zoneEst);
    freshModel.createCalendar("Home", zonePst);

    List<String> tokens = List.of("copy", "event", "Client Call", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");
    new CopyCommand(tokens).execute(freshModel, view);
  }

  @Test
  public void testCopyEventSyntaxValidation() throws Exception {
    try {
      List<String> tokens = List.of("copy", "event", "S", "at", "2025-11-10T09:00",
          "--target", "H", "to", "2025-12-01T15:00");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing 'on'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }

    try {
      List<String> tokens = List.of("copy", "event", "S", "on", "2025-11-10T09:00",
          "target", "H", "to", "2025-12-01T15:00");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing '--target'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }

    try {
      List<String> tokens = List.of("copy", "event", "S", "on", "2025-11-10T09:00",
          "--target", "H", "from", "2025-12-01T15:00");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing 'to'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test
  public void testCopyEventsBetweenSyntaxValidation() throws Exception {
    try {
      List<String> tokens = List.of("copy", "events", "between", "D1", "or", "D2",
          "--target", "H", "to", "D3");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing 'and'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }

    try {
      List<String> tokens = List.of("copy", "events", "between", "D1", "and", "D2",
          "target", "H", "to", "D3");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing '--target'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }

    try {
      List<String> tokens = List.of("copy", "events", "between", "D1", "and", "D2",
          "--target", "H", "from", "D3");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing 'to'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test
  public void testCopyRejectsInvalidDates() throws Exception {
    try {
      List<String> tokens = List.of("copy", "event", "S", "on", "BAD-DATE",
          "--target", "H", "to", "2025-12-01T15:00");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad source datetime");
    } catch (ValidationException e) {
      assertEquals("Invalid date/time format. Expected YYYY-MM-DDThh:mm.", e.getMessage());
    }

    try {
      List<String> tokens = List.of("copy", "event", "S", "on", "2025-11-10T09:00",
          "--target", "H", "to", "BAD-DATE");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad target datetime");
    } catch (ValidationException e) {
      assertEquals("Invalid date/time format. Expected YYYY-MM-DDThh:mm.", e.getMessage());
    }

    try {
      List<String> tokens = List.of("copy", "events", "on", "BAD-DATE",
          "--target", "H", "to", "2025-12-02");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad source date");
    } catch (ValidationException e) {
      assertEquals("Invalid date format. Expected YYYY-MM-DD.", e.getMessage());
    }

    try {
      List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
          "--target", "H", "to", "BAD-DATE");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad target date");
    } catch (ValidationException e) {
      assertEquals("Invalid date format. Expected YYYY-MM-DD.", e.getMessage());
    }

    try {
      List<String> tokens = List.of("copy", "events", "between", "BAD-DATE", "and", "2025-11-20",
          "--target", "H", "to", "2026-01-10");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad source date 1");
    } catch (ValidationException e) {
      assertEquals("Invalid date format. Expected YYYY-MM-DD.", e.getMessage());
    }

    try {
      List<String> tokens = List.of("copy", "events", "between", "2025-11-17", "and", "BAD-DATE",
          "--target", "H", "to", "2026-01-10");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad source date 2");
    } catch (ValidationException e) {
      assertEquals("Invalid date format. Expected YYYY-MM-DD.", e.getMessage());
    }

    try {
      List<String> tokens = List.of("copy", "events", "between", "2025-11-17", "and", "2025-11-20",
          "--target", "H", "to", "BAD-DATE");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for bad target date");
    } catch (ValidationException e) {
      assertEquals("Invalid date format. Expected YYYY-MM-DD.", e.getMessage());
    }
  }

  @Test
  public void testCopyBetweenHandlesDateShiftAndSeriesId() throws Exception {
    List<String> tokens = List.of("copy", "events", "between", "2025-11-17", "and", "2025-11-20",
        "--target", "Home", "to", "2026-01-10");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Copied 2 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Team Sync"));
    assertEquals(2, events.size());
    Event copiedS1 = events.get(0);
    Event copiedS2 = events.get(1);

    assertEquals(LocalDate.of(2026, 1, 10), copiedS1.getStart().toLocalDate());
    assertEquals(LocalDateTime.of(2026, 1, 10, 11, 0), copiedS1.getStart());

    assertEquals(LocalDate.of(2026, 1, 12), copiedS2.getStart().toLocalDate());
    assertEquals(LocalDateTime.of(2026, 1, 12, 11, 0), copiedS2.getStart());

    assertEquals("Series ID should be retained", seriesId, copiedS1.getSeriesId());
    assertEquals("Series ID should be retained", seriesId, copiedS2.getSeriesId());
  }

  @Test
  public void testCopyEventsOnHandlesMidnightCrossing() throws Exception {
    model.setActiveCalendar("Home");
    Event lateEvent = Event.builder()
        .setSubject("Late Night Gaming")
        .setStart(LocalDateTime.of(2025, 11, 20, 20, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 22, 0))
        .build();
    model.getActiveCalendar().addEvent(lateEvent);

    List<String> tokens = List.of("copy", "events", "on", "2025-11-20",
        "--target", "Work", "to", "2025-12-10");
    new CopyCommand(tokens).execute(model, view);

    Calendar workCal = model.getCalendar("Work");
    List<Event> events = workCal.findEvents(e -> e.getSubject().equals("Late Night Gaming"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 10, 23, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 11, 1, 0), copiedEvent.getEnd());

    assertNotEquals(lateEvent.getSeriesId(), copiedEvent.getSeriesId());
  }

  @Test(expected = ValidationException.class)
  public void testCopyRejectsUnknownEntityType() throws Exception {
    List<String> tokens = List.of("copy", "calendar", "Work", "to", "Home");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test
  public void testCopyEventLocatesCorrectInstance() throws Exception {
    Event e2 = Event.builder()
        .setSubject("Client Call")
        .setStart(LocalDateTime.of(2025, 11, 10, 11, 0))
        .setEnd(LocalDateTime.of(2025, 11, 10, 12, 0))
        .build();
    model.getActiveCalendar().addEvent(e2);

    List<String> tokens = List.of("copy", "event", "Client Call", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");

    new CopyCommand(tokens).execute(model, view);

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Client Call"));
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 12, 1, 15, 0), events.get(0).getStart());
  }

  @Test(expected = ValidationException.class)
  public void testCopyEventsRejectsUnknownSubCommand() throws Exception {
    List<String> tokens = List.of("copy", "events", "from", "2025-11-10",
        "--target", "Home", "to", "2025-12-02");
    new CopyCommand(tokens).execute(model, view);
  }

  @Test
  public void testCopyEventsOnIgnoresNonOverlapping() throws Exception {
    Event before = Event.builder()
        .setSubject("Yesterday's News")
        .setStart(LocalDateTime.of(2025, 11, 9, 23, 0))
        .setEnd(LocalDateTime.of(2025, 11, 9, 23, 59))
        .build();
    model.getActiveCalendar().addEvent(before);

    List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
        "--target", "Home", "to", "2025-12-02");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Copied 1 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> true);
    assertEquals(1, events.size());
    assertEquals("Client Call", events.get(0).getSubject());
  }

  @Test
  public void testCopyBetweenHandlesTimezoneMidnightCrossing() throws Exception {
    model.setActiveCalendar("Home");
    Event lateEvent = Event.builder()
        .setSubject("Late PST Call")
        .setStart(LocalDateTime.of(2025, 11, 20, 20, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 22, 0))
        .build();
    model.getActiveCalendar().addEvent(lateEvent);

    List<String> tokens = List.of("copy", "events", "between", "2025-11-20", "and", "2025-11-20",
        "--target", "Work", "to", "2025-12-10");
    new CopyCommand(tokens).execute(model, view);

    Calendar workCal = model.getCalendar("Work");
    List<Event> events = workCal.findEvents(e -> e.getSubject().equals("Late PST Call"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 10, 23, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 11, 1, 0), copiedEvent.getEnd());
    assertNotEquals("Series ID should be broken", lateEvent.getSeriesId(),
        copiedEvent.getSeriesId());
  }

  @Test
  public void testCopyEventsBetweenSkipsNonMatchingEvents() throws Exception {
    Event outside = Event.builder()
        .setSubject("Out of Scope")
        .setStart(LocalDateTime.of(2025, 11, 21, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 21, 11, 0))
        .build();
    model.getActiveCalendar().addEvent(outside);

    List<String> tokens = List.of("copy", "events", "between", "2025-11-17", "and", "2025-11-20",
        "--target", "Home", "to", "2026-01-10");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Copied 2 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> copied = homeCal.findEvents(e -> true);
    assertEquals(2, copied.size());
    assertFalse(copied.stream().anyMatch(e -> e.getSubject().equals("Out of Scope")));
  }

  @Test
  public void testCopyEventsOnReportsConflict() throws Exception {
    Event conflict = Event.builder()
        .setSubject("Client Call")
        .setStart(LocalDateTime.of(2025, 12, 2, 6, 0))
        .setEnd(LocalDateTime.of(2025, 12, 2, 7, 0))
        .build();
    model.getCalendar("Home").addEvent(conflict);

    List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
        "--target", "Home", "to", "2025-12-02");

    try {
      new CopyCommand(tokens).execute(model, view);
      fail("Expected ValidationException for conflict.");
    } catch (ValidationException e) {
      String expectedMsg = "Copy creates a conflict: "
          + "One or more events in the series create a conflict: Client Call";
      assertEquals(expectedMsg, e.getMessage());
    }
  }

  @Test
  public void testCopyBetweenReportsConflict() throws Exception {
    Event conflict = Event.builder()
        .setSubject("Team Sync")
        .setStart(LocalDateTime.of(2026, 1, 10, 11, 0))
        .setEnd(LocalDateTime.of(2026, 1, 10, 12, 0))
        .build();
    model.getCalendar("Home").addEvent(conflict);

    List<String> tokens = List.of("copy", "events", "between", "2025-11-17", "and", "2025-11-20",
        "--target", "Home", "to", "2026-01-10");

    try {
      new CopyCommand(tokens).execute(model, view);
      fail("Expected ValidationException for conflict.");
    } catch (ValidationException e) {
      String expectedMsg = "Copy creates a conflict: One "
          + "or more events in the series create a conflict: Team Sync";
      assertEquals(expectedMsg, e.getMessage());
    }
  }
}