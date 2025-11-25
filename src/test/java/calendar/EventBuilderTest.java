package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.Test;

/**
 * Verifies that the Event.Builder class correctly validates input and constructs Event objects,
 * preventing invalid states like missing subjects or inverted start/end times.
 */
public class EventBuilderTest {

  private final LocalDateTime start = LocalDateTime.of(2025, 11, 1, 10, 0);
  private final LocalDateTime end = LocalDateTime.of(2025, 11, 1, 11, 0);

  @Test
  public void testRejectsInvertedTimeRange() {
    try {
      Event.builder()
          .setSubject("Time Machine Error")
          .setStart(end)
          .setEnd(start)
          .build();
      fail("Expected ValidationException for end before start.");
    } catch (ValidationException e) {
      assertEquals("Event end time cannot be before start time.", e.getMessage());
    }
  }

  @Test
  public void testDefaultValuesForOptionalFields() throws ValidationException {
    Event event = Event.builder()
        .setSubject("Bare Minimum Event")
        .setStart(start)
        .setEnd(end)
        .setDescription(null)
        .setLocation(null)
        .build();

    assertEquals("", event.getDescription());
    assertEquals("", event.getLocation());
  }

  @Test
  public void testCanBuildValidEvent() throws ValidationException {
    Event event = Event.builder()
        .setSubject("Doctor Appointment")
        .setStart(start)
        .setEnd(end)
        .build();
    assertEquals("Doctor Appointment", event.getSubject());
    assertEquals(start, event.getStart());
    assertEquals(end, event.getEnd());
  }

  @Test
  public void testRejectsMultiDaySeriesEvent() {
    try {
      Event.builder()
          .setSubject("Week-long Workshop")
          .setStart(start)
          .setEnd(end.plusDays(1))
          .setSeriesId(UUID.randomUUID())
          .build();
      fail("Expected ValidationException for multi-day series event.");
    } catch (ValidationException e) {
      assertEquals("Events in a series must start and end on the same day.", e.getMessage());
    }
  }

  @Test
  public void testRejectsMissingStartOrEnd() {
    try {
      Event.builder().setSubject("Startless Event").setEnd(end).build();
      fail("Expected ValidationException for missing start.");
    } catch (ValidationException e) {
      assertEquals("Event must have a start date/time.", e.getMessage());
    }

    try {
      Event.builder().setSubject("Endless Event").setStart(start).build();
      fail("Expected ValidationException for missing end.");
    } catch (ValidationException e) {
      assertEquals("Event must have an end date/time.", e.getMessage());
    }
  }

  @Test
  public void testToStringFormat() throws ValidationException {
    UUID seriesId = UUID.randomUUID();
    Event event = Event.builder()
        .setSubject("Debug Me")
        .setStart(start)
        .setEnd(end)
        .setSeriesId(seriesId)
        .build();

    String expected = "Event{subject='Debug Me', start=2025-11-01T10:00, "
        + "end=2025-11-01T11:00, seriesId=" + seriesId + "}";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testAllowsMultiDayEventWithoutSeriesId() throws ValidationException {
    Event event = Event.builder()
        .setSubject("Weekend Retreat")
        .setStart(start)
        .setEnd(end.plusDays(1))
        .setSeriesId(null)
        .build();

    assertEquals("Weekend Retreat", event.getSubject());
    assertEquals(start, event.getStart());
    assertEquals(end.plusDays(1), event.getEnd());
  }

  @Test
  public void testEqualityAndHashCodeConsistency() throws ValidationException {
    Event event1 = Event.builder()
        .setSubject("Team Lunch")
        .setStart(start)
        .setEnd(end)
        .setLocation("Pizza Place")
        .build();

    Event event2 = Event.builder()
        .setSubject("Team Lunch")
        .setStart(start)
        .setEnd(end)
        .setLocation("Burger Joint")
        .build();

    Event event3 = Event.builder()
        .setSubject("Client Dinner")
        .setStart(start)
        .setEnd(end)
        .build();

    assertEquals("Events with same subject/start/end should be equal", event1, event2);
    assertNotEquals("Events with different subject should not be equal", event1, event3);
    assertNotEquals(null, event1);
    assertNotEquals("Event should not equal a different object type", new Object(), event1);

    assertEquals("Hash codes must be equal for equal objects", event1.hashCode(),
        event2.hashCode());
  }

  @Test
  public void testRejectsMissingSubject() {
    try {
      Event.builder().setStart(start).setEnd(end).build();
      fail("Expected ValidationException for missing subject.");
    } catch (ValidationException e) {
      assertEquals("Event must have a subject.", e.getMessage());
    }

    try {
      Event.builder().setSubject("  ").setStart(start).setEnd(end).build();
      fail("Expected ValidationException for blank subject.");
    } catch (ValidationException e) {
      assertEquals("Event must have a subject.", e.getMessage());
    }
  }
}