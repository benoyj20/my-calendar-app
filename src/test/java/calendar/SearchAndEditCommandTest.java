package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.commands.SearchAndEditCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * These tests verify the command's behavior, ensuring that
 * search criteria correctly identify events and modifications are applied as expected.
 */
public class SearchAndEditCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Sets up a model and view before each test.
   * Creates a calendar "Work" and adds two sample events.
   */
  @Before
  public void setUp() throws Exception {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("Work", ZoneId.of("America/New_York"));
    model.setActiveCalendar("Work");

    Calendar cal = model.getActiveCalendar();

    LocalDateTime start1 = LocalDateTime.of(2025, 11, 23, 10, 0);
    LocalDateTime end1 = start1.plusHours(1);
    Event e1 = Event.builder()
        .setSubject("meeting")
        .setLocation("RoomA")
        .setStart(start1)
        .setEnd(end1)
        .build();

    LocalDateTime start2 = LocalDateTime.of(2025, 11, 24, 9, 0);
    LocalDateTime end2 = start2.plusHours(1);
    Event e2 = Event.builder()
        .setSubject("other")
        .setLocation("RoomB")
        .setStart(start2)
        .setEnd(end2)
        .build();

    cal.addEvent(e1);
    cal.addEvent(e2);
  }

  @Test
  public void testSearchBySubjectAndChangeLocation() throws Exception {
    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "location", "RoomC"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);

    List<Event> meetings = model.getActiveCalendar().findEvents(
        e -> e.getSubject().equalsIgnoreCase("meeting"));
    assertFalse(meetings.isEmpty());
    for (Event e : meetings) {
      assertEquals("RoomC", e.getLocation());
    }
  }

  @Test
  public void testNoMatchesJustShowsMessageAndLeavesEventsAlone() throws Exception {
    int originalCount = model.getActiveCalendar().findEvents(e -> true).size();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "does-not-exist",
        "set", "location", "Nowhere"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);

    int afterCount = model.getActiveCalendar().findEvents(e -> true).size();
    assertEquals("Event count should be unchanged when no matches", originalCount, afterCount);
  }

  @Test(expected = ValidationException.class)
  public void testUnknownCriteriaTypeThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search-edit", "by", "something-weird", "meeting",
        "set", "location", "RoomZ"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testUnknownPropertyThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "not-a-property", "value"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test
  public void testBadDateFormatThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search-edit", "events", "between",
        "not-a-date", "and", "2025-11-23T11:00",
        "set", "subject", "whatever"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    try {
      cmd.execute(model, view);
      fail("Expected ValidationException for invalid date format");
    } catch (ValidationException e) {
      assertEquals("Invalid format. Expected YYYY-MM-DDThh:mm.", e.getMessage());
    }
  }

  @Test(expected = ValidationException.class)
  public void testNoActiveCalendarThrows() throws Exception {
    ApplicationManager emptyModel = new ApplicationManagerImpl();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "location", "RoomX"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(emptyModel, view);
  }

  @Test(expected = ValidationException.class)
  public void testMissingSetKeywordThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "location", "RoomC"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testMissingModificationValueThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "location"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test
  public void testSearchBetweenRangeAndModifySubject() throws Exception {
    Event inside = Event.builder()
        .setSubject("InsideRange")
        .setStart(LocalDateTime.of(2025, 11, 23, 10, 30))
        .setEnd(LocalDateTime.of(2025, 11, 23, 11, 30))
        .build();
    model.getActiveCalendar().addEvent(inside);

    List<String> tokens = Arrays.asList(
        "search-edit", "events", "between",
        "2025-11-23T10:00", "and", "2025-11-23T12:00",
        "set", "subject", "Updated"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);

    List<Event> updatedEvents = model.getActiveCalendar().findEvents(
        e -> e.getSubject().equals("Updated"));
    assertFalse("Event inside range should have been updated", updatedEvents.isEmpty());

    List<Event> oldMeetings = model.getActiveCalendar().findEvents(
        e -> e.getSubject().equalsIgnoreCase("meeting"));
    assertTrue("Old meeting subject should be gone", oldMeetings.isEmpty());

    assertEquals("Should have 2 events updated (meeting + InsideRange)", 2, updatedEvents.size());
  }
}