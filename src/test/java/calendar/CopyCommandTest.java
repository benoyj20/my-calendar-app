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
 * Tests the {@link CopyCommand} class, verifying all three copy syntaxes.
 * This includes validation of timezone conversion, date shifting,
 * series ID retention, and error handling.
 */
public class CopyCommandTest {

  private ApplicationManager model;
  private TestView view;
  private Event e1;
  private Event s1;
  private Event s2;
  private UUID seriesId;
  private ZoneId zoneEst;
  private ZoneId zonePst;


  /**
   * Sets up a model with two calendars ("Work" in EST, "Home" in PST)
   * and populates "Work" with a single event and a two-event series.
   *
   * @throws ValidationException if setup fails
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

    e1 = Event.builder()
        .setSubject("Meeting")
        .setStart(LocalDateTime.of(2025, 11, 10, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 10, 10, 0))
        .build();

    s1 = Event.builder()
        .setSubject("Sync")
        .setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 17, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 17, 15, 0))
        .build();

    s2 = Event.builder()
        .setSubject("Sync")
        .setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 19, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 19, 15, 0))
        .build();

    model.getActiveCalendar().addEvents(List.of(e1, s1, s2));
  }

  /**
   * Tests the 'copy event' syntax.
   * This should copy the event to the exact target time and break its series ID.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCopyEvent() throws Exception {
    List<String> tokens = List.of("copy", "event", "Meeting", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Event copied to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Meeting"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 1, 15, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 1, 16, 0), copiedEvent.getEnd());
    assertNotEquals("Series ID should be broken", e1.getSeriesId(), copiedEvent.getSeriesId());
  }

  /**
   * Tests the 'copy events on' syntax.
   * This must test the timezone conversion logic (9am EST -> 6am PST)
   * and that the series ID is retained.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCopyEventsOnWithTimezoneConversion() throws Exception {
    List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
        "--target", "Home", "to", "2025-12-02");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Copied 1 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Meeting"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 2, 6, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 2, 7, 0), copiedEvent.getEnd());
    assertEquals("Series ID should be retained", e1.getSeriesId(), copiedEvent.getSeriesId());
  }

  /**
   * Tests the 'copy events between' syntax.
   * This tests date shifting, timezone conversion, and series ID retention.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCopyEventsBetweenWithDateShift() throws Exception {
    List<String> tokens = List.of("copy", "events", "between", "2025-11-17", "and", "2025-11-20",
        "--target", "Home", "to", "2026-01-10");

    new CopyCommand(tokens).execute(model, view);
    assertEquals("Copied 2 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Sync"));
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

  /**
   * Tests that 'copy event' fails when no matching event is found.
   *
   * @throws Exception expected ValidationException
   */
  @Test(expected = ValidationException.class)
  public void testCopyEventNotFound() throws Exception {
    List<String> tokens = List.of("copy", "event", "Missing Event", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests that any copy fails if the target calendar does not exist.
   *
   * @throws Exception expected ValidationException
   */
  @Test(expected = ValidationException.class)
  public void testCopyTargetCalendarNotFound() throws Exception {
    List<String> tokens = List.of("copy", "event", "Meeting", "on", "2025-11-10T09:00",
        "--target", "MissingCal", "to", "2025-12-01T15:00");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests that any copy fails if it would create a conflict in the target.
   *
   * @throws Exception expected ValidationException
   */
  @Test
  public void testCopyCreatesConflict() throws Exception {
    Event conflict = Event.builder()
        .setSubject("Meeting")
        .setStart(LocalDateTime.of(2025, 12, 1, 15, 0))
        .setEnd(LocalDateTime.of(2025, 12, 1, 16, 0))
        .build();
    model.getCalendar("Home").addEvent(conflict);

    List<String> tokens = List.of("copy", "event", "Meeting", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");

    try {
      new CopyCommand(tokens).execute(model, view);
      fail("Expected ValidationException for conflict.");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("An event with the "
          + "same subject, start, and end time already exists."));
    }
  }

  /**
   * Tests bad syntax for 'copy event'.
   *
   * @throws Exception expected ValidationException
   */
  @Test(expected = ValidationException.class)
  public void testCopyEventBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "event", "Meeting", "on", "2025-11-10T09:00");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests bad syntax for 'copy events on'.
   *
   * @throws Exception expected ValidationException
   */
  @Test(expected = ValidationException.class)
  public void testCopyEventsOnBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
        "to", "2025-12-02");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests bad syntax for 'copy events between'.
   *
   * @throws Exception expected ValidationException
   */
  @Test(expected = ValidationException.class)
  public void testCopyEventsBetweenBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "events", "between", "2025-11-17",
        "and", "2025-11-20", "to", "2026-01-10");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests that a command fails if no calendar is active.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testErrorNoActiveCalendar() throws Exception {
    ApplicationManager freshModel = new ApplicationManagerImpl();
    freshModel.createCalendar("Work", zoneEst);
    freshModel.createCalendar("Home", zonePst);

    List<String> tokens = List.of("copy", "event", "Meeting", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");
    new CopyCommand(tokens).execute(freshModel, view);
  }

  /**
   * Tests all branches of the 'copy event' syntax validation.
   */
  @Test
  public void testCopyEventBadSyntaxBranches() throws Exception {
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

  /**
   * Tests all branches of the 'copy events between' syntax validation.
   */
  @Test
  public void testCopyEventsBetweenBadSyntaxBranches() throws Exception {
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

  /**
   * Tests validation for all unparseable date/time strings.
   */
  @Test
  public void testCopyAllBadDateTimes() throws Exception {
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

  /**
   * Tests the 'copy events between' logic when the start and end
   * dates are provided in reverse order.
   */
  @Test
  public void testCopyEventsBetweenReversedDates() throws Exception {
    List<String> tokens = List.of("copy", "events", "between", "2025-11-20", "and", "2025-11-17",
        "--target", "Home", "to", "2026-01-10");
    new CopyCommand(tokens).execute(model, view);

    assertEquals("Copied 2 events to 'Home'.", view.getLastMessage());

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Sync"));
    assertEquals(2, events.size());

    Event copiedS1 = events.get(0);
    Event copiedS2 = events.get(1);

    assertEquals(LocalDateTime.of(2026, 1, 10, 11, 0), copiedS1.getStart());
    assertEquals(LocalDateTime.of(2026, 1, 12, 11, 0), copiedS2.getStart());
  }

  /**
   * Tests the logic for an event that does not cross midnight in the source,
   * but *does* cross midnight after timezone conversion.
   * (e.g., 8 PM PST -> 11 PM EST, 10 PM PST -> 1 AM EST)
   */
  @Test
  public void testCopyEventsOnCrossesMidnightInTarget() throws Exception {
    model.setActiveCalendar("Home");
    Event lateEvent = Event.builder()
        .setSubject("Late Event")
        .setStart(LocalDateTime.of(2025, 11, 20, 20, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 22, 0))
        .build();
    model.getActiveCalendar().addEvent(lateEvent);

    List<String> tokens = List.of("copy", "events", "on", "2025-11-20",
        "--target", "Work", "to", "2025-12-10");
    new CopyCommand(tokens).execute(model, view);

    Calendar workCal = model.getCalendar("Work");
    List<Event> events = workCal.findEvents(e -> e.getSubject().equals("Late Event"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 10, 23, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 11, 1, 0), copiedEvent.getEnd());

    assertNotEquals(lateEvent.getSeriesId(), copiedEvent.getSeriesId());
  }

  /**
   * Tests the 'else' branch of the main execute method.
   * This provides a command other than "event" or "events".
   */
  @Test(expected = ValidationException.class)
  public void testCopyUnknownCommand() throws Exception {
    List<String> tokens = List.of("copy", "calendar", "Work", "to", "Home");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests that the 'copy event' predicate correctly finds one event
   * based on both subject and start time.
   */
  @Test
  public void testCopyEventFindsCorrectEvent() throws Exception {
    Event e2 = Event.builder()
        .setSubject("Meeting")
        .setStart(LocalDateTime.of(2025, 11, 10, 11, 0))
        .setEnd(LocalDateTime.of(2025, 11, 10, 12, 0))
        .build();
    model.getActiveCalendar().addEvent(e2);

    List<String> tokens = List.of("copy", "event", "Meeting", "on", "2025-11-10T09:00",
        "--target", "Home", "to", "2025-12-01T15:00");

    new CopyCommand(tokens).execute(model, view);

    Calendar homeCal = model.getCalendar("Home");
    List<Event> events = homeCal.findEvents(e -> e.getSubject().equals("Meeting"));
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 12, 1, 15, 0), events.get(0).getStart());
  }

  /**
   * Tests the 'else' branch of the 'executeCopyEvents' method.
   * This provides a command other than "on" or "between".
   */
  @Test(expected = ValidationException.class)
  public void testCopyEventsBadSyntax() throws Exception {
    List<String> tokens = List.of("copy", "events", "from", "2025-11-10",
        "--target", "Home", "to", "2025-12-02");
    new CopyCommand(tokens).execute(model, view);
  }

  /**
   * Tests all branches of the 'copy events on' syntax validation.
   */
  @Test
  public void testCopyEventsOnBadSyntaxBranches() throws Exception {
    try {
      List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
          "target", "Home", "to", "2025-12-02");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing '--target'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }

    try {
      List<String> tokens = List.of("copy", "events", "on", "2025-11-10",
          "--target", "Home", "from", "2025-12-02");
      new CopyCommand(tokens).execute(model, view);
      fail("Expected validation error for missing 'to'");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  /**
   * Tests the overlap predicate for 'copy events between' to ensure
   * events outside the range are not copied.
   */
  @Test
  public void testCopyEventsBetweenPartialOverlaps() throws Exception {
    Event outside = Event.builder()
        .setSubject("Outside")
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
    assertFalse(copied.stream().anyMatch(e -> e.getSubject().equals("Outside")));
  }

  /**
   * Tests that 'copy events on' fails if it creates a conflict
   * and that the exception is correctly wrapped.
   */
  @Test
  public void testCopyEventsOnCreatesConflict() throws Exception {
    Event conflict = Event.builder()
        .setSubject("Meeting")
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
          + "One or more events in the series create a conflict: Meeting";
      assertEquals(expectedMsg, e.getMessage());
    }
  }

  /**
   * Tests the 'copy events on' filter to ensure it ignores events
   * that end before the start of the day.
   */
  @Test
  public void testCopyEventsOnIgnoresEventEndingBeforeStartOfDay() throws Exception {
    Event before = Event.builder()
        .setSubject("Event From Yesterday")
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
    assertEquals("Meeting", events.get(0).getSubject());
  }

  /**
   * Tests the 'copy events between' logic for an event that crosses
   * midnight after timezone conversion.
   */
  @Test
  public void testCopyEventsBetweenCrossesMidnightInTarget() throws Exception {
    model.setActiveCalendar("Home");
    Event lateEvent = Event.builder()
        .setSubject("Late PST Event")
        .setStart(LocalDateTime.of(2025, 11, 20, 20, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 22, 0))
        .build();
    model.getActiveCalendar().addEvent(lateEvent);

    List<String> tokens = List.of("copy", "events", "between", "2025-11-20", "and", "2025-11-20",
        "--target", "Work", "to", "2025-12-10");
    new CopyCommand(tokens).execute(model, view);

    Calendar workCal = model.getCalendar("Work");
    List<Event> events = workCal.findEvents(e -> e.getSubject().equals("Late PST Event"));
    assertEquals(1, events.size());
    Event copiedEvent = events.get(0);

    assertEquals(LocalDateTime.of(2025, 12, 10, 23, 0), copiedEvent.getStart());
    assertEquals(LocalDateTime.of(2025, 12, 11, 1, 0), copiedEvent.getEnd());
    assertNotEquals("Series ID should be broken", lateEvent.getSeriesId(),
        copiedEvent.getSeriesId());
  }

  /**
   * Tests that 'copy events between' fails if it creates a conflict
   * and that the exception is correctly wrapped.
   */
  @Test
  public void testCopyEventsBetweenCreatesConflict() throws Exception {
    Event conflict = Event.builder()
        .setSubject("Sync")
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
          + "or more events in the series create a conflict: Sync";
      assertEquals(expectedMsg, e.getMessage());
    }
  }
}