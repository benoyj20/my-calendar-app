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
 * Ensures that the EditEventCommand allows users to modify existing events correctly,
 * handling single instances, future events, and entire series while enforcing valid input.
 */
public class EditEventCommandTest {

  private ApplicationManager model;
  private TestView view;
  private UUID seriesId;
  private Event event1;
  private Event event2;
  private Event event3;

  /**
   * Sets up a calendar with a recurring "Stand-up" meeting series before each test.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();
    seriesId = UUID.randomUUID();

    model.createCalendar("Work", ZoneId.of("UTC"));
    model.setActiveCalendar("Work");

    event1 = Event.builder().setSubject("Daily Stand-up").setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 3, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 3, 11, 0)).build();
    event2 = Event.builder().setSubject("Daily Stand-up").setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 5, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 5, 11, 0)).build();
    event3 = Event.builder().setSubject("Daily Stand-up").setSeriesId(seriesId)
        .setStart(LocalDateTime.of(2025, 11, 7, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 7, 11, 0)).build();

    model.getActiveCalendar().addEvents(List.of(event1, event2, event3));
  }

  @Test(expected = ValidationException.class)
  public void testEditFailsForNonExistentProperty() throws Exception {
    List<String> tokens = List.of("edit", "event", "organizer", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Me");
    new EditEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanChangeEndTimeOfSingleEvent() throws Exception {
    List<String> tokens = List.of("edit", "event", "end", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "2025-11-03T11:30");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());

    Event e = model.getActiveCalendar()
        .findEvents(evt -> evt.getStart().equals(event1.getStart())).get(0);

    assertEquals(LocalDateTime.of(2025, 11, 3, 11, 30), e.getEnd());
    assertEquals(event1.getStart(), e.getStart());
  }

  @Test(expected = ValidationException.class)
  public void testEditFailsWithInvalidDate() throws Exception {
    List<String> tokens = List.of("edit", "event", "start", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "not-a-date");
    new EditEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanEditFutureEventsInSeries() throws Exception {
    List<String> tokens = List.of("edit", "events", "start", "Daily Stand-up", "from",
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

  @Test
  public void testEditFailsIfEndTimeBeforeStartTime() throws Exception {
    List<String> tokens = List.of("edit", "event", "end", "Daily Stand-up", "from",
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

  @Test
  public void testCanUpdateDescription() throws Exception {
    List<String> tokens = List.of("edit", "event", "description", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Discuss blocking issues");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());
    Event e = model.getActiveCalendar()
        .findEvents(evt -> evt.getStart().equals(event1.getStart())).get(0);
    assertEquals("Discuss blocking issues", e.getDescription());
  }

  @Test(expected = ValidationException.class)
  public void testEditFailsForMissingEvent() throws Exception {
    List<String> tokens = List.of("edit", "event", "subject", "Non-Existent Meeting", "from",
        "2025-01-01T10:00", "to", "2025-01-01T11:00", "with", "New");
    new EditEventCommand(tokens).execute(model, view);
  }

  private Event createEventWithNullSeriesId() throws Exception {
    Event event = Event.builder()
        .setSubject("Ad-hoc Event")
        .setStart(LocalDateTime.of(2025, 10, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 10, 1, 11, 0))
        .build();

    Field seriesIdField = Event.class.getDeclaredField("seriesId");
    seriesIdField.setAccessible(true);
    seriesIdField.set(event, null);

    return event;
  }

  @Test
  public void testEditingSeriesOnSingleEventBehavesLikeSingleEdit() throws Exception {
    Event nullSeriesEvent = createEventWithNullSeriesId();
    model.getActiveCalendar().addEvent(nullSeriesEvent);

    List<String> tokens = List.of("edit", "events", "subject", "Ad-hoc Event", "from",
        "2025-10-01T10:00", "with", "Renamed Event");

    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());

    List<Event> newEvents = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Renamed Event"));
    assertEquals(1, newEvents.size());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsIncompleteSeriesCommand() throws Exception {
    List<String> tokens = List.of("edit", "series", "subject", "Daily Stand-up");
    new EditEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanChangeLocationForWholeSeries() throws Exception {
    List<String> tokens = List.of("edit", "series", "location", "Daily Stand-up", "from",
        "2025-11-05T10:00", "with", "Zoom Room 5");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 3 event(s).", view.getLastMessage());

    List<Event> allEvents = model.getActiveCalendar().findEvents(
        e -> e.getSeriesId().equals(seriesId));
    assertEquals(3, allEvents.size());
    for (Event e : allEvents) {
      assertEquals("Zoom Room 5", e.getLocation());
      assertEquals("SeriesId should be preserved", seriesId, e.getSeriesId());
    }
  }

  @Test(expected = ValidationException.class)
  public void testRejectsIncompleteSingleCommand() throws Exception {
    List<String> tokens = List.of("edit", "event", "subject", "Daily Stand-up");
    new EditEventCommand(tokens).execute(model, view);
  }

  @Test
  public void testEditFailsIfConflictCreated() throws Exception {
    Event conflict = Event.builder().setSubject("Daily Stand-up")
        .setStart(LocalDateTime.of(2025, 11, 3, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 3, 15, 0)).build();
    model.getActiveCalendar().addEvent(conflict);

    List<String> tokens = List.of("edit", "event", "start", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "2025-11-03T14:00");

    try {
      new EditEventCommand(tokens).execute(model, view);
      fail("Expected ValidationException for conflict.");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().startsWith("Edit creates a conflict"));
    }
  }

  @Test
  public void testCanChangeStatusToPrivate() throws Exception {
    List<String> tokens = List.of("edit", "event", "status", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "private");
    new EditEventCommand(tokens).execute(model, view);

    Event e = model.getActiveCalendar()
        .findEvents(evt -> evt.getStart().equals(event1.getStart())).get(0);
    assertTrue(e.isPrivate());
  }

  @Test
  public void testEditSingleInstanceBreaksSeries() throws Exception {
    List<String> tokens = List.of("edit", "event", "subject", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "Client Demo");
    new EditEventCommand(tokens).execute(model, view);

    assertEquals("Successfully updated 1 event(s).", view.getLastMessage());

    Event oldEvent = model.getActiveCalendar()
        .findEvents(e -> e.getStart().equals(event1.getStart())
            && e.getSubject().equals("Daily Stand-up")).stream().findFirst().orElse(null);
    assertNull(oldEvent);

    List<Event> newEvents = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Client Demo"));
    assertEquals(1, newEvents.size());
    assertNotEquals("SeriesId should be broken", seriesId, newEvents.get(0).getSeriesId());

    List<Event> oldSeries = model.getActiveCalendar()
        .findEvents(e -> e.getSubject().equals("Daily Stand-up"));
    assertEquals(2, oldSeries.size());
  }

  @Test
  public void testEditFailsWhenMultipleEventsMatchSingleLocator() throws Exception {
    Event duplicate = Event.builder()
        .setSubject("Daily Stand-up")
        .setStart(LocalDateTime.of(2025, 11, 3, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 3, 12, 0))
        .build();
    model.getActiveCalendar().addEvent(duplicate);

    List<String> tokens = List.of("edit", "event", "subject", "Daily Stand-up", "from",
        "2025-11-03T10:00", "to", "2025-11-03T11:00", "with", "New Subject");

    try {
      new EditEventCommand(tokens).execute(model, view);
      fail("Expected ValidationException for multiple matches.");
    } catch (ValidationException e) {
      assertEquals("Multiple events match. Cannot edit single event.", e.getMessage());
    }
  }
}