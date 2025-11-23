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
 * Verifies the validation and construction logic of the Event.Builder.
 * This ensures that events cannot be created in an invalid state,
 * such as missing a subject or having an end time before the start time.
 */
public class EventBuilderTest {

  private final LocalDateTime start = LocalDateTime.of(2025, 11, 1, 10, 0);
  private final LocalDateTime end = LocalDateTime.of(2025, 11, 1, 11, 0);

  @Test
  public void testBuildSuccess() throws ValidationException {
    Event event = Event.builder()
        .setSubject("Test Event")
        .setStart(start)
        .setEnd(end)
        .build();
    assertEquals("Test Event", event.getSubject());
    assertEquals(start, event.getStart());
    assertEquals(end, event.getEnd());
  }

  @Test
  public void testBuildFailNoSubject() {
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

  @Test
  public void testBuildFailNoStartOrEnd() {
    try {
      Event.builder().setSubject("Test").setEnd(end).build();
      fail("Expected ValidationException for missing start.");
    } catch (ValidationException e) {
      assertEquals("Event must have a start date/time.", e.getMessage());
    }

    try {
      Event.builder().setSubject("Test").setStart(start).build();
      fail("Expected ValidationException for missing end.");
    } catch (ValidationException e) {
      assertEquals("Event must have an end date/time.", e.getMessage());
    }
  }

  /**
   * Checks the format of the event's toString method for completeness.
   *
   * @throws ValidationException if build fails
   */
  @Test
  public void testToString() throws ValidationException {
    UUID seriesId = UUID.randomUUID();
    Event event = Event.builder()
        .setSubject("My Event")
        .setStart(start)
        .setEnd(end)
        .setSeriesId(seriesId)
        .build();

    String expected = "Event{subject='My Event', start=2025-11-01T10:00, "
        + "end=2025-11-01T11:00, seriesId=" + seriesId + "}";
    assertEquals(expected, event.toString());
  }

  /**
   * Ensures that null values for optional fields, like description
   * or location, are correctly defaulted to empty strings.
   *
   * @throws ValidationException if build fails
   */
  @Test
  public void testBuildWithNulls() throws ValidationException {
    Event event = Event.builder()
        .setSubject("Test")
        .setStart(start)
        .setEnd(end)
        .setDescription(null)
        .setLocation(null)
        .build();

    assertEquals("", event.getDescription());
    assertEquals("", event.getLocation());
  }

  @Test
  public void testBuildFailEndBeforeStart() {
    try {
      Event.builder()
          .setSubject("Test")
          .setStart(end)
          .setEnd(start)
          .build();
      fail("Expected ValidationException for end before start.");
    } catch (ValidationException e) {
      assertEquals("Event end time cannot be before start time.", e.getMessage());
    }
  }

  @Test
  public void testEqualsAndHashCodeContract() throws ValidationException {
    Event event1 = Event.builder()
        .setSubject("Meeting")
        .setStart(start)
        .setEnd(end)
        .setLocation("Conf Room A")
        .build();

    Event event2 = Event.builder()
        .setSubject("Meeting")
        .setStart(start)
        .setEnd(end)
        .setLocation("Conf Room B")
        .build();

    Event event3 = Event.builder()
        .setSubject("Different Meeting")
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
  public void testBuildFailSeriesEventSpansMultipleDays() {
    try {
      Event.builder()
          .setSubject("Multi-day Series Event")
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
  public void testBuildSuccessMultiDayEventNoSeriesId() throws ValidationException {
    Event event = Event.builder()
        .setSubject("Multi-day Event")
        .setStart(start)
        .setEnd(end.plusDays(1))
        .setSeriesId(null)
        .build();

    assertEquals("Multi-day Event", event.getSubject());
    assertEquals(start, event.getStart());
    assertEquals(end.plusDays(1), event.getEnd());
  }

}