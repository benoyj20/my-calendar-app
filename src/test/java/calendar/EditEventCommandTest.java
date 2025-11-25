package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.commands.EditEventCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link EditEventCommand} class.
 * Validates all edit scopes (SINGLE, FUTURE, ALL) and property modifications,
 * including conflict and series-breaking logic.
 */
public class EditEventCommandTest {

  private ApplicationManager model;
  private TestView view;
  private UUID seriesId;
  private Event event1;
  private Event event2;
  private Event event3;

  /**
   * Sets up a model with a 3-event series in an active calendar.
   *
   * @throws ValidationException if event creation fails
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();
    seriesId = UUID.randomUUID();

    model.createCalendar("TestCal", ZoneId.of("UTC"));
    model.setActiveCalendar("TestCal");

    event1 = Event.builder().setSubject("Weekly Sync").setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 3, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 3, 11, 0)).build();
    event2 = Event.builder().setSubject("Weekly Sync").setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 5, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 5, 11, 0)).build();
    event3 = Event.builder().setSubject("Weekly Sync").setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 7, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 7, 11, 0)).build();

    model.getActiveCalendar().addEvents(List.of(event1, event2, event3));
  }

  /**
   * Tests editing the subject of a single event instance,
   * which should break it from the series.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditSingleEventSubject() throws Exception {
    List<String> tokens = List.of("edit", "event", "subject", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Kick-off");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());

    Event oldEvent = model.getActiveCalendar()
        .findEvents(e -> e.getStart().equals(event1.getStart())
            && e.getSubject().equals("Weekly Sync")).stream().findFirst().orElse(null);
    assertNull(oldEvent);

    List<Event> newEvents = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Kick-off"));
    assertEquals(1, newEvents.size());
    assertNotEquals("SeriesId should be broken", seriesId, newEvents.get(0).getSeriesId());

    List<Event> oldSeries = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Weekly Sync"));
    assertEquals(2, oldSeries.size());
  }

  /**
   * Tests editing the start time for 'this and all future' events.
   * This should break event 2 and 3 into a new series.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditFutureEventsStart() throws Exception {
    List<String> tokens = List.of("edit", "events", "start", "Weekly Sync", "from",
        "2025-11-05T10:00", "with", "2025-11-05T10:30");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 2 event(s).", view.getLastMessage());

    Event e1 = model.getActiveCalendar()
        .findEvents(e -> e.getStart().equals(event1.getStart())).get(0);
    assertEquals(event1.getEnd(), e1.getEnd());
    assertEquals(seriesId, e1.getSeriesId());

    Event e2 = model.getActiveCalendar().findEvents(e -> e.getStart().toLocalDate()
        .equals(event2.getStart().toLocalDate())).get(0);
    assertEquals(LocalDateTime.of(2025, 11, 5, 10, 30), e2.getStart());
    assertEquals(LocalDateTime.of(2025, 11, 5, 11, 30), e2.getEnd());
    assertNotEquals("New series should be created", seriesId, e2.getSeriesId());

    Event e3 = model.getActiveCalendar().findEvents(e -> e.getStart().toLocalDate()
        .equals(event3.getStart().toLocalDate())).get(0);
    assertEquals(LocalDateTime.of(2025, 11, 7, 10, 30), e3.getStart());
    assertEquals("Should share new seriesId", e2.getSeriesId(), e3.getSeriesId());
  }

  /**
   * Tests editing a non-time property (location) for an entire series.
   * This should not break the series.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditAllSeriesLocation() throws Exception {
    List<String> tokens = List.of("edit", "series", "location", "Weekly Sync", "from",
        "2025-11-05T10:00", "with", "Room 101");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 3 event(s).", view.getLastMessage());

    List<Event> allEvents = model.getActiveCalendar().findEvents(
        e -> e.getSeriesId().equals(seriesId));
    assertEquals(3, allEvents.size());
    for (Event e : allEvents) {
      assertEquals("Room 101", e.getLocation());
      assertEquals("SeriesId should be preserved", seriesId, e.getSeriesId());
    }
  }

  /**
   * Tests editing the 'status' property to 'private'.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditStatus() throws Exception {
    List<String> tokens = List.of("edit", "event", "status", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "private");
    new EditEventCommand(tokens).execute(model, view);

    Event e = model.getActiveCalendar()
        .findEvents(evt -> evt.getStart().equals(event1.getStart())).get(0);
    assertTrue(e.isPrivate());
  }

  /**
   * Tests editing the 'description' property.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditDescription() throws Exception {
    List<String> tokens = List.of("edit", "event", "description", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Team Stand-up");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());
    Event e = model.getActiveCalendar()
        .findEvents(evt -> evt.getStart().equals(event1.getStart())).get(0);
    assertEquals("Team Stand-up", e.getDescription());
  }

  /**
   * Tests validation for editing an event that cannot be found.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testEditEventNotFound() throws Exception {
    List<String> tokens = List.of("edit", "event", "subject", "Missing Event", "from",
        "2025-01-01T10:00", "to", "2025-01-01T11:00", "with", "New");
    new EditEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation for an edit that would create a conflict with an existing event.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditCreatesConflict() throws Exception {
    Event conflict = Event.builder().setSubject("Weekly Sync")
        .setStart(LocalDateTime.of(2025, 11, 3, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 3, 15, 0)).build();
    model.getActiveCalendar().addEvent(conflict);

    List<String> tokens = List.of("edit", "event", "start", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "2025-11-03T14:00");

    try {
      new EditEventCommand(tokens).execute(model, view);
      fail("Expected ValidationException for conflict.");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().startsWith("Edit creates a conflict"));
    }
  }

  /**
   * Tests validation for an unknown property name.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testEditUnknownProperty() throws Exception {
    List<String> tokens = List.of("edit", "event", "organizer", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Me");
    new EditEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax validation for an 'edit event' command that is too short.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testEditSyntaxErrorSingle() throws Exception {
    List<String> tokens = List.of("edit", "event", "subject", "Weekly Sync");
    new EditEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax validation for an 'edit series' command that is too short.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testEditSyntaxErrorSeries() throws Exception {
    List<String> tokens = List.of("edit", "series", "subject", "Weekly Sync");
    new EditEventCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation for a malformed date-time string in the 'with' clause
   * for the 'start' property.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testEditStartWithBadDateTime() throws Exception {
    List<String> tokens = List.of("edit", "event", "start", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "not-a-date");
    new EditEventCommand(tokens).execute(model, view);
  }

  /**
   * Creates an event and uses reflection to set its seriesId to null.
   *
   * @return An event with a null seriesId
   * @throws Exception if reflection fails
   */
  private Event createEventWithNullSeriesId() throws Exception {
    Event event = Event.builder()
        .setSubject("Null Series Event")
        .setStart(LocalDateTime.of(2025, 10, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 10, 1, 11, 0))
        .build();

    Field seriesIdField = Event.class.getDeclaredField("seriesId");
    seriesIdField.setAccessible(true);
    seriesIdField.set(event, null);

    return event;
  }

  /**
   * Tests that editing a 'series' on an event with no seriesId
   * correctly defaults to editing a single event.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditSeriesOnEventWithNullSeriesId() throws Exception {
    Event nullSeriesEvent = createEventWithNullSeriesId();
    model.getActiveCalendar().addEvent(nullSeriesEvent);

    List<String> tokens = List.of("edit", "events", "subject", "Null Series Event", "from",
        "2025-10-01T10:00", "with", "New Subject");

    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());

    List<Event> newEvents = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("New Subject"));
    assertEquals(1, newEvents.size());
  }

  /**
   * Tests that the 'end' property modifier correctly wraps a
   * parse error in a RuntimeException.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditEndPropertyWithBadDateTime() throws Exception {
    List<String> tokens = List.of("edit", "event", "end", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "not-a-date");

    try {
      new EditEventCommand(tokens).execute(model, view);
      fail("Expected RuntimeException from modifier.");
    } catch (RuntimeException e) {

      assertTrue(e.getCause() instanceof ValidationException);

      assertEquals("Invalid date/time format. Expected YYYY-MM-DDThh:mm.",
          e.getCause().getMessage());
    }
  }

  /**
   * Tests that an 'edit event' (single scope) command fails when
   * multiple events match the locator.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditSingleEventFailsWithMultipleMatches() throws Exception {
    Event duplicate = Event.builder()
        .setSubject("Weekly Sync")
        .setStart(LocalDateTime.of(2025, 11, 3, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 3, 12, 0))
        .build();
    model.getActiveCalendar().addEvent(duplicate);

    List<String> tokens = List.of("edit", "event", "subject", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Kick-off");

    try {
      new EditEventCommand(tokens).execute(model, view);
      fail("Expected ValidationException for multiple matches.");
    } catch (ValidationException e) {
      assertEquals("Multiple events match. Cannot edit single event.", e.getMessage());
    }
  }


  /**
   * Tests successfully editing the 'end' property of a single event.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testEditEndPropertySuccess() throws Exception {
    List<String> tokens = List.of("edit", "event", "end", "Weekly Sync", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "2025-11-03T11:30");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());

    Event e = model.getActiveCalendar()
        .findEvents(evt -> evt.getStart().equals(event1.getStart())).get(0);

    assertEquals(LocalDateTime.of(2025, 11, 3, 11, 30), e.getEnd());
    assertEquals(event1.getStart(), e.getStart());
  }

}