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
 * Validates the CreateEventCommand by checking all the different ways events can be created,
 * including single events, recurring series, and ensuring invalid inputs are caught.
 */
public class CreateEventCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Prepares an environment with a "Personal" calendar before each test run.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("Personal", ZoneId.of("UTC"));
    model.setActiveCalendar("Personal");
  }

  @Test
  public void testCanCreateRecurringEventUntilDate() throws Exception {
    List<String> tokens = List.of("create", "event", "Morning Jog", "from",
        "2025-11-10T07:00", "to", "2025-11-10T08:00",
        "repeats", "TR", "until", "2025-11-14");
    new CreateEventCommand(tokens).execute(model, view);

    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(2, events.size());
    assertEquals(LocalDate.of(2025, 11, 11), events.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 13), events.get(1).getStart().toLocalDate());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsEmptyWeekdays() throws Exception {
    List<String> tokens = List.of("create", "event", "Lecture", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsUnknownCommandType() throws Exception {
    List<String> tokens = List.of("create", "event", "Oops", "at", "2025-01-01");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanCreateAllDayRecurringEvent() throws Exception {
    List<String> tokens = List.of("create", "event", "Annual Festival", "on", "2025-12-25",
        "repeats", "R", "for", "2", "times");
    new CreateEventCommand(tokens).execute(model, view);

    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(2, events.size());
    assertTrue(events.get(0).isAllDay());
    assertTrue(events.get(1).isAllDay());
    assertEquals(LocalDate.of(2025, 12, 25), events.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.of(2026, 1, 1), events.get(1).getStart().toLocalDate());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsInvalidWeekdays() throws Exception {
    List<String> tokens = List.of("create", "event", "Lecture", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWFX", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsExtraTokenInFromSyntax() throws Exception {
    List<String> tokens = List.of("create", "event", "Sync", "from",
        "2025-11-01T10:00", "to", "2025-11-01T11:30", "extra");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsInvalidDate() throws Exception {
    List<String> tokens = List.of("create", "event", "Party", "on", "2025-13-32");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanCreateSimpleTimedEvent() throws Exception {
    List<String> tokens = List.of("create", "event", "Coffee Chat", "from",
        "2025-11-01T10:00", "to", "2025-11-01T11:30");
    new CreateEventCommand(tokens).execute(model, view);

    assertEquals("Event(s) created successfully.", view.getLastMessage());
    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, events.size());
    assertEquals("Coffee Chat", events.get(0).getSubject());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsInvalidRecurrenceCondition() throws Exception {
    List<String> tokens = List.of("create", "event", "Lecture", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWF", "every", "3", "days");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanCreateRecurringEventWithCount() throws Exception {
    List<String> tokens = List.of("create", "event", "CS101", "from",
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

  @Test(expected = ValidationException.class)
  public void testRejectsExtraTokenInOnSyntax() throws Exception {
    List<String> tokens = List.of("create", "event", "Holiday", "on", "2025-12-25", "extra");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsIncompleteCommand() throws Exception {
    List<String> tokens = List.of("create", "event", "Incomplete");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsMultiDayRecurringEvent() throws Exception {
    List<String> tokens = List.of("create", "event", "Trip", "from",
        "2025-11-03T09:00", "to", "2025-11-04T10:00",
        "repeats", "MWF", "for", "3", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsNonNumericRecurrenceCount() throws Exception {
    List<String> tokens = List.of("create", "event", "Lecture", "from",
        "2025-11-03T09:00", "to", "2025-11-03T10:00",
        "repeats", "MWF", "for", "two", "times");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanCreateRecurringEventOverWeekend() throws Exception {
    List<String> tokens = List.of("create", "event", "Weekend Trip", "from",
        "2025-11-29T08:00", "to", "2025-11-29T11:00",
        "repeats", "SU", "for", "4", "times");
    new CreateEventCommand(tokens).execute(model, view);

    List<Event> events = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Weekend Trip"));
    assertEquals(4, events.size());

    assertEquals(LocalDate.of(2025, 11, 29), events.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 30), events.get(1).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 12, 6), events.get(2).getStart().toLocalDate());
    assertEquals(LocalDate.of(2025, 12, 7), events.get(3).getStart().toLocalDate());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsInvalidDateTime() throws Exception {
    List<String> tokens = List.of("create", "event", "Sync", "from",
        "2025-11-01T25:00", "to", "2025-11-01T11:30");
    new CreateEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanCreateAllDayEvent() throws Exception {
    List<String> tokens = List.of("create", "event", "Christmas", "on", "2025-12-25");
    new CreateEventCommand(tokens).execute(model, view);

    assertEquals("Event(s) created successfully.", view.getLastMessage());
    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, events.size());
    assertTrue(events.get(0).isAllDay());
    assertEquals(LocalDateTime.of(2025, 12, 25, 8, 0), events.get(0).getStart());
  }
}