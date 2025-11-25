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
 * Verifies that the CalendarManager correctly handles the events,
 * ensuring they can be added, removed, and searched for without issues.
 */
public class CalendarManagerImplTest {

  private static final String SUBJECT_DOCTOR = "Doctor Appointment";
  private static final String SUBJECT_LUNCH = "Team Lunch";
  private static final String ERROR_DUPLICATE =
      "An event with the same subject, start, and end time already exists.";
  private static final String ERROR_EXPECTED =
      "Expected ValidationException for duplicate event.";
  private static final Predicate<Event> FILTER_ALL = e -> true;

  private CalendarManager model;
  private Event doctorAppt;
  private Event teamLunch;

  /**
   * Prepares a calendar instance.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new CalendarManagerImpl();
    doctorAppt = Event.builder()
        .setSubject(SUBJECT_DOCTOR)
        .setStart(LocalDateTime.of(2025, 11, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 1, 11, 0))
        .build();
    teamLunch = Event.builder()
        .setSubject(SUBJECT_LUNCH)
        .setStart(LocalDateTime.of(2025, 11, 1, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 1, 15, 0))
        .build();
  }

  @Test
  public void testCanAddMultipleEventsAtOnce() throws ValidationException {
    model.addEvents(List.of(doctorAppt, teamLunch));
    List<Event> events = model.findEvents(FILTER_ALL);
    assertEquals(2, events.size());
    assertTrue("List should contain the doctor appointment", events.contains(doctorAppt));
    assertTrue("List should contain the team lunch", events.contains(teamLunch));
  }

  @Test
  public void testFindEventsFiltersAndSortsCorrectly() throws ValidationException {
    model.addEvents(List.of(teamLunch, doctorAppt));

    List<Event> events = model.findEvents(
        e -> e.getSubject().equals(SUBJECT_DOCTOR)
    );
    assertEquals(1, events.size());
    assertEquals(doctorAppt, events.get(0));

    List<Event> allEvents = model.findEvents(FILTER_ALL);
    assertEquals(2, allEvents.size());
    assertEquals("Doctor appointment (10am) should be first", doctorAppt, allEvents.get(0));
    assertEquals("Team Lunch (2pm) should be second", teamLunch, allEvents.get(1));
  }

  @Test
  public void testAddingDuplicateEventThrowsException() throws ValidationException {
    model.addEvent(doctorAppt);

    Event duplicate = Event.builder()
        .setSubject(SUBJECT_DOCTOR)
        .setStart(LocalDateTime.of(2025, 11, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 1, 11, 0))
        .build();

    try {
      model.addEvent(duplicate);
      fail(ERROR_EXPECTED);
    } catch (ValidationException e) {
      assertEquals(
          ERROR_DUPLICATE,
          e.getMessage()
      );
    }
  }

  @Test
  public void testCanRemoveEventsIndividuallyAndInBatch() throws ValidationException {
    model.addEvents(List.of(doctorAppt, teamLunch));
    assertEquals(2, model.findEvents(FILTER_ALL).size());

    model.removeEvent(doctorAppt);
    List<Event> events = model.findEvents(FILTER_ALL);
    assertEquals(1, events.size());
    assertEquals(teamLunch, events.get(0));

    model.removeEvents(List.of(teamLunch));
    assertEquals(0, model.findEvents(FILTER_ALL).size());
  }

  @Test
  public void testCanAddSingleEventSuccessfully() throws ValidationException {
    model.addEvent(doctorAppt);
    List<Event> events = model.findEvents(FILTER_ALL);
    assertEquals(1, events.size());
    assertEquals(doctorAppt, events.get(0));
  }
}