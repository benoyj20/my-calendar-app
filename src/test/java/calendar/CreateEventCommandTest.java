package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.controller.commands.CreateEventCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link CreateEventCommand} class, covering all creation types,
 * recurrence rules, and error paths within the context of an ApplicationManager.
 */
public class CreateEventCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Sets up a fresh model, a test view, and creates/activates
   * a default calendar for event commands to operate on.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("TestCal", ZoneId.of("UTC"));
    model.setActiveCalendar("TestCal");
  }

  /**
   * Tests creating a simple, non-recurring timed event.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCreateSimpleEvent() throws Exception {
    List<String> tokens = List.of("create", "event", "Meeting", "from",
        "2025-11-01T10:00", "to", "2025-11-01T11:30");
    new CreateEventCommand(tokens).execute(model, view);

    assertEquals("Event(s) created successfully.", view.getLastMessage());
    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  /**
   * Tests creating a non-recurring, all-day event using the "on" syntax.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCreateAllDayEvent() throws Exception {
    List<String> tokens = List.of("create", "event", "Holiday", "on", "2025-12-25");
    new CreateEventCommand(tokens).execute(model, view);

    assertEquals("Event(s) created successfully.", view.getLastMessage());
    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, events.size());
    assertTrue(events.get(0).isAllDay());
    assertEquals(LocalDateTime.of(2025, 12, 25, 8, 0), events.get(0).getStart());
  }

  /**
   * Tests repeating event generation that spans across a weekend.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCreateRepeatingForWeekend() throws Exception {
    List<String> tokens = List.of("create", "event", "Weekend Hike", "from",
        "2025-11-29T08:00", "to", "2025-11-29T11:00",
        "repeats", "SU", "for", "4", "times");
    new CreateEventCommand(tokens).execute(model, view);

    List<Event> events = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Weekend Hike"));
    assertEquals(4, events.size());

    assertEquals(LocalDate.of(2025, 11, 29), events.get(0).getStart()
        .toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 30), events.get(1).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 12, 6), events.get(2).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 12, 7), events.get(3).getStart().toLocalDate());
  }

  /**
   * Tests creating a timed event that repeats "for N times".
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCreateRepeatingFor() throws Exception {
    List<String> tokens = List.of("create", "event", "Class", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWF", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);

    assertEquals("Event(s) created successfully.", view.getLastMessage());
    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(3, events.size());
    assertEquals(LocalDate.of(2025, 11, 3), events.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 5), events.get(1).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 7), events.get(2).getStart().toLocalDate());

    assertNotNull(events.get(0).getSeriesId());
    assertEquals(events.get(0).getSeriesId(), events.get(1).getSeriesId());
    assertEquals(events.get(1).getSeriesId(), events.get(2).getSeriesId());
  }

  /**
   * Tests creating a timed event that repeats "until DATE".
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCreateRepeatingUntil() throws Exception {
    List<String> tokens = List.of("create", "event", "Gym", "from",
        "2025-11-10T07:00", "to", "2025-11-10T08:00",
        "repeats", "TR", "until", "2025-11-14");
    new CreateEventCommand(tokens).execute(model, view);

    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(2, events.size());
    assertEquals(LocalDate.of(2025, 11, 11), events.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 13), events.get(1).getStart().toLocalDate());
  }

  /**
   * Tests creating an all-day event that repeats.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testCreateAllDayRepeating() throws Exception {
    List<String> tokens = List.of("create", "event", "Annual Party", "on", "2025-12-25",
        "repeats", "R", "for", "2", "times");
    new CreateEventCommand(tokens).execute(model, view);

    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(2, events.size());
    assertTrue(events.get(0).isAllDay());
    assertTrue(events.get(1).isAllDay());
    assertEquals(LocalDate.of(2025, 12, 25), events.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.of(2026, 1, 1), events.get(1).getStart().toLocalDate());
  }

  /**
   * Tests syntax error for too few tokens.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testSyntaxErrorShort() throws Exception {
    List<String> tokens = List.of("create", "event", "Oops");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax error for invalid type (not "on" or "from").
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testSyntaxErrorBadType() throws Exception {
    List<String> tokens = List.of("create", "event", "Oops", "at", "2025-01-01");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax error for extra tokens on an "on" command.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testSyntaxErrorOnExtraToken() throws Exception {
    List<String> tokens = List.of("create", "event", "Holiday", "on", "2025-12-25", "extra");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax error for extra tokens on a "from" command.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testSyntaxErrorFromExtraToken() throws Exception {
    List<String> tokens = List.of("create", "event", "Meeting", "from",
        "2025-11-01T10:00", "to", "2025-11-01T11:30", "extra");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation error for recurring multi-day events.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testRepeatingMultiDayFail() throws Exception {
    List<String> tokens = List.of("create", "event", "Trip", "from",
        "2025-11-03T09:00", "to", "2025-11-04T10:00",
        "repeats", "MWF", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax error for invalid recurrence condition.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testRepeatingBadCondition() throws Exception {
    List<String> tokens = List.of("create", "event", "Class", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWF", "every", "3", "days");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation error for unparseable "for N" number.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testRepeatingBadNumber() throws Exception {
    List<String> tokens = List.of("create", "event", "Class", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWF", "for", "two", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation error for unparseable date.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testParseBadDate() throws Exception {
    List<String> tokens = List.of("create", "event", "Holiday", "on", "2025-13-32");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation error for unparseable date-time.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testParseBadDateTime() throws Exception {
    List<String> tokens = List.of("create", "event", "Meeting", "from",
        "2025-11-01T25:00", "to", "2025-11-01T11:30");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests that the command fails if the weekdays string contains
   * invalid characters.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testRepeatingInvalidWeekdays() throws Exception {
    List<String> tokens = List.of("create", "event", "Class", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWFX", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests that the command fails if the weekdays string is empty.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testRepeatingEmptyWeekdays() throws Exception {
    List<String> tokens = List.of("create", "event", "Class", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

}