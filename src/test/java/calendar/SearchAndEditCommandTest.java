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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;

/**
 * Tests for {@link calendar.controller.commands.SearchAndEditCommand}.
 *
 * <p>These tests build the token lists directly instead of going through
 * the parser/lexer, so that we can hit all of the branches in
 * {@link calendar.controller.commands.SearchAndEditCommand}.</p>
 */
public class SearchAndEditCommandTest {

  // ---------- helpers ----------

  /**
   * Build a model with one calendar "Work" and two events.
   *
   * <ul>
   *   <li>"meeting" from 2025-11-23T10:00 to 11:00</li>
   *   <li>"other" from 2025-11-24T09:00 to 10:00</li>
   * </ul>
   */
  private ApplicationManager buildModelWithSampleEvents() throws Exception {
    ApplicationManager model = new ApplicationManagerImpl();
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

    return model;
  }

  private Calendar getWorkCalendar(ApplicationManager model) throws ValidationException {
    return model.getActiveCalendar();
  }

  @Test
  public void testSearchBySubjectAndChangeLocation() throws Exception {
    ApplicationManager model = buildModelWithSampleEvents();
    Calendar cal = getWorkCalendar(model);
    TestView view = new TestView();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "location", "RoomC"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);

    List<Event> meetings = cal.findEvents(
        e -> e.getSubject().equalsIgnoreCase("meeting"));
    assertFalse(meetings.isEmpty());
    for (Event e : meetings) {
      assertEquals("RoomC", e.getLocation());
    }
  }

  @Test
  public void testNoMatchesJustShowsMessageAndLeavesEventsAlone() throws Exception {
    ApplicationManager model = buildModelWithSampleEvents();
    Calendar cal = getWorkCalendar(model);
    int originalCount = cal.findEvents(e -> true).size();
    TestView view = new TestView();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "does-not-exist",
        "set", "location", "Nowhere"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);

    int afterCount = cal.findEvents(e -> true).size();
    assertEquals("Event count should be unchanged when no matches", originalCount, afterCount);
  }

  @Test(expected = ValidationException.class)
  public void testUnknownCriteriaTypeThrows() throws Exception {
    ApplicationManager model = buildModelWithSampleEvents();
    TestView view = new TestView();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "something-weird", "meeting",
        "set", "location", "RoomZ"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testUnknownPropertyThrows() throws Exception {
    ApplicationManager model = buildModelWithSampleEvents();
    TestView view = new TestView();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "not-a-property", "value"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test(expected = ValidationException.class)
  public void testBadDateFormatThrows() throws Exception {
    ApplicationManager model = buildModelWithSampleEvents();
    TestView view = new TestView();


    List<String> tokens = Arrays.asList(
        "search-edit", "between",
        "not-a-date", "and", "2025-11-23T11:00",
        "set", "subject", "whatever"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test
  public void testParseSearchCriteriaBetweenRange() throws Exception {

    List<String> tokens = Arrays.asList(
        "search", "events", "between",
        "2025-11-23T10:00",
        "and",
        "2025-11-23T12:00",
        "set", "subject", "Updated"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);

    Method m = SearchAndEditCommand.class.getDeclaredMethod("parseSearchCriteria");
    m.setAccessible(true);

    @SuppressWarnings("unchecked")
    Predicate<Event> pred = (Predicate<Event>) m.invoke(cmd);

    Event inside = Event.builder()
        .setSubject("test")
        .setStart(LocalDateTime.of(2025, 11, 23, 10, 30))
        .setEnd(LocalDateTime.of(2025, 11, 23, 11, 30))
        .build();

    Event before = Event.builder()
        .setSubject("test")
        .setStart(LocalDateTime.of(2025, 11, 23, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 23, 9, 30))
        .build();

    Event after = Event.builder()
        .setSubject("test")
        .setStart(LocalDateTime.of(2025, 11, 23, 12, 30))
        .setEnd(LocalDateTime.of(2025, 11, 23, 13, 30))
        .build();

    assertTrue("Inside event must match range", pred.test(inside));
    assertFalse("Before event must NOT match", pred.test(before));
    assertFalse("After event must NOT match", pred.test(after));
  }

  @Test(expected = ValidationException.class)
  public void testNoActiveCalendarThrows() throws Exception {
    ApplicationManager model = new ApplicationManagerImpl();
    TestView view = new TestView();

    List<String> tokens = Arrays.asList(
        "search-edit", "by", "subject", "meeting",
        "set", "location", "RoomX"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);
    cmd.execute(model, view);
  }

  @Test
  public void testParseDateTimeInvalidFormatThrows() throws Exception {
    List<String> tokens = Arrays.asList("search", "events", "subject", "x");
    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);

    Method m = SearchAndEditCommand.class.getDeclaredMethod("parseDateTime", String.class);
    m.setAccessible(true);

    try {
      m.invoke(cmd, "INVALID-DATE");
      fail("Expected ValidationException");
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      assertTrue(cause instanceof ValidationException);
      assertEquals("Invalid format. Expected YYYY-MM-DDThh:mm.", cause.getMessage());
    }
  }

  @Test(expected = ValidationException.class)
  public void testParseModificationParamsNoSetTokenThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search", "events",
        "subject", "Meeting"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);

    Method m = SearchAndEditCommand.class.getDeclaredMethod("parseModificationParams");
    m.setAccessible(true);

    try {
      m.invoke(cmd);
    } catch (InvocationTargetException ite) {
      throw (ValidationException) ite.getCause();
    }
  }

  @Test(expected = ValidationException.class)
  public void testParseModificationParamsMissingPropertyAndValueThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search", "events",
        "subject", "Meeting",
        "set"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);

    Method m = SearchAndEditCommand.class.getDeclaredMethod("parseModificationParams");
    m.setAccessible(true);

    try {
      m.invoke(cmd);
    } catch (InvocationTargetException ite) {
      throw (ValidationException) ite.getCause();
    }
  }

  @Test(expected = ValidationException.class)
  public void testParseSearchCriteriaMissingAndThrows() throws Exception {
    List<String> tokens = Arrays.asList(
        "search", "events", "between",
        "2025-11-23T10:00",
        "WRONG",
        "2025-11-23T12:00",
        "set", "subject", "Updated"
    );

    SearchAndEditCommand cmd = new SearchAndEditCommand(tokens);

    Method m = SearchAndEditCommand.class.getDeclaredMethod("parseSearchCriteria");
    m.setAccessible(true);

    try {
      m.invoke(cmd);
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof ValidationException) {
        throw (ValidationException) cause;
      }
      fail("Expected ValidationException but got: " + cause);
    }
  }

}
