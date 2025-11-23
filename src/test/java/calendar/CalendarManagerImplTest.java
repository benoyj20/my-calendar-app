package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.model.CalendarManager;
import calendar.model.CalendarManagerImpl;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link CalendarManagerImpl} class.
 * Verifies the core logic of adding, removing, and finding events,
 * as well as conflict detection and sorting.
 */
public class CalendarManagerImplTest {

  private static final String EVENT1_SUBJECT = "Event 1";
  private static final String EVENT2_SUBJECT = "Event 2";
  private static final String DUPLICATE_EVENT_MSG =
      "An event with the same subject, start, and end time already exists.";
  private static final String EXPECTED_VALIDATION_FAIL_MSG =
      "Expected ValidationException for duplicate event.";
  private static final Predicate<Event> PREDICATE_MATCH_ALL = e -> true;

  private CalendarManager model;
  private Event event1;
  private Event event2;

  /**
   * Sets up the model and creates common {@link Event} objects
   * used in multiple tests.
   *
   * @throws ValidationException if event creation fails (should not happen in setup)
   */
  @Before
  public void setUp() throws ValidationException {
    model = new CalendarManagerImpl();
    event1 = Event.builder()
        .setSubject(EVENT1_SUBJECT)
        .setStart(LocalDateTime.of(2025, 11, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 1, 11, 0))
        .build();
    event2 = Event.builder()
        .setSubject(EVENT2_SUBJECT)
        .setStart(LocalDateTime.of(2025, 11, 1, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 1, 15, 0))
        .build();
  }

  /**
   * Tests that a single event can be added successfully.
   */
  @Test
  public void testAddEvent() throws ValidationException {
    model.addEvent(event1);
    List<Event> events = model.findEvents(PREDICATE_MATCH_ALL);
    assertEquals(1, events.size());
    assertEquals(event1, events.get(0));
  }

  /**
   * Tests that adding a duplicate event throws a ValidationException
   * with the correct error message.
   */
  @Test
  public void testAddEventConflict() throws ValidationException {
    model.addEvent(event1);

    Event duplicate = Event.builder()
        .setSubject(EVENT1_SUBJECT)
        .setStart(LocalDateTime.of(2025, 11, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 1, 11, 0))
        .build();

    try {
      model.addEvent(duplicate);
      fail(EXPECTED_VALIDATION_FAIL_MSG);
    } catch (ValidationException e) {
      assertEquals(
          DUPLICATE_EVENT_MSG,
          e.getMessage()
      );
    }
  }

  /**
   * Tests that a list of events can be added successfully.
   */
  @Test
  public void testAddEvents() throws ValidationException {
    model.addEvents(List.of(event1, event2));
    List<Event> events = model.findEvents(PREDICATE_MATCH_ALL);
    assertEquals(2, events.size());
    assertTrue("List should contain event 1", events.contains(event1));
    assertTrue("List should contain event 2", events.contains(event2));
  }

  /**
   * Tests removing a single event and a list of events.
   */
  @Test
  public void testRemoveEvents() throws ValidationException {
    model.addEvents(List.of(event1, event2));
    assertEquals(2, model.findEvents(PREDICATE_MATCH_ALL).size());

    model.removeEvent(event1);
    List<Event> events = model.findEvents(PREDICATE_MATCH_ALL);
    assertEquals(1, events.size());
    assertEquals(event2, events.get(0));

    model.removeEvents(List.of(event2));
    assertEquals(0, model.findEvents(PREDICATE_MATCH_ALL).size());
  }

  /**
   * Tests finding events with a specific predicate and verifies
   * that the returned list is sorted by start time, even if added out of order.
   */
  @Test
  public void testFindEvents() throws ValidationException {
    model.addEvents(List.of(event2, event1));

    List<Event> events = model.findEvents(
        e -> e.getSubject().equals(EVENT1_SUBJECT)
    );
    assertEquals(1, events.size());
    assertEquals(event1, events.get(0));

    List<Event> allEvents = model.findEvents(PREDICATE_MATCH_ALL);
    assertEquals(2, allEvents.size());
    assertEquals("Event 1 (10am) should be first", event1, allEvents.get(0));
    assertEquals("Event 2 (2pm) should be second", event2, allEvents.get(1));
  }


}