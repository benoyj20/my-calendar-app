package calendar.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Calendar;
import calendar.model.EditScope;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import calendar.view.GuiView;
import calendar.view.ViewMode;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the GUI controller with a fake GuiView (no Swing).
 * Ensures the controller initializes the view correctly and makes it visible.
 */
public class GuiControllerTest {

  private ApplicationManager model;
  private FakeGuiView view;

  /**
   * Sets up a simple model and fake GUI view before each test.
   *
   * @throws Exception if calendar creation or setup fails
   */
  @Before
  public void setUp() throws Exception {
    model = new ApplicationManagerImpl();
    model.createCalendar("Personal", ZoneId.of("America/New_York"));
    model.setActiveCalendar("Personal");

    Calendar cal = model.getActiveCalendar();
    LocalDateTime start = LocalDateTime.of(2025, 11, 23, 10, 0);
    LocalDateTime end = start.plusHours(1);

    Event evt = Event.builder()
        .setSubject("GUI test event")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(evt);

    view = new FakeGuiView();

    new GuiController(model, view);
  }

  private static class FakeAppManager extends ApplicationManagerImpl {
    @Override
    public java.util.Set<String> getCalendarNames() {
      return java.util.Collections.emptySet();
    }
  }

  private static class FakeView implements CalendarView {

    StringBuilder log = new StringBuilder();

    @Override
    public void showMessage(String message) {
      log.append("MSG:").append(message).append("\n");
    }

    @Override
    public void showError(String error) {
      log.append("ERR:").append(error).append("\n");
    }

    @Override
    public void showPrompt() {
      log.append("PROMPT\n");
    }

    @Override
    public void printEventsOnDate(List<Event> events) {
      log.append("PRINT_ON_DATE:").append(events.size()).append("\n");
    }

    @Override
    public void printEventsInRange(List<Event> events) {
      log.append("PRINT_IN_RANGE:").append(events.size()).append("\n");
    }
  }


  @Test
  public void testControllerInitializesViewFromModel() {
    assertTrue("GUI should be made visible", view.visible);

    assertNotNull("Calendar list should be initialized", view.lastCalendarNames);
    assertTrue("Calendar list should contain 'Personal'",
        view.lastCalendarNames.contains("Personal"));

    assertEquals("Personal", view.lastActiveCalendar);

    assertNotNull("Month view date should be set", view.lastMonth);
    assertNotNull("Month view events should be set", view.lastMonthEvents);
    assertTrue("Month view should contain at least one event",
        !view.lastMonthEvents.isEmpty());

    assertEquals("Default view mode should be MONTH",
        ViewMode.MONTH, view.lastViewMode);
  }

  @Test
  public void testChangeDateRangeAndViewModes() {
    GuiController controller = new GuiController(model, view);

    LocalDate originalMonth = view.lastMonth;
    controller.changeDateRange(1);
    assertTrue(view.lastMonth.isAfter(originalMonth));

    controller.setViewMode(ViewMode.WEEK);
    LocalDate originalWeek = view.lastWeekStart;
    controller.changeDateRange(1);
    assertTrue(view.lastWeekStart.isAfter(originalWeek));

    controller.setViewMode(ViewMode.DAY);
    LocalDate originalDay = view.lastDayDate;
    controller.changeDateRange(1);
    assertTrue(view.lastDayDate.isAfter(originalDay));
  }

  @Test
  public void testCreateCalendarAddsAndActivatesAndShowsMessage() throws Exception {
    GuiController controller = new GuiController(model, view);

    String newName = "Work";
    ZoneId zone = ZoneId.of("America/Los_Angeles");

    controller.createCalendar(newName, zone);

    assertTrue("View calendar list should contain the new calendar",
        view.lastCalendarNames.contains(newName));
    assertEquals("View should show the new calendar as active",
        newName, view.lastActiveCalendar);

    assertEquals("Calendar '" + newName + "' created.", view.lastInfoMessage);
  }


  @Test
  public void testSelectDateUpdatesView() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDate newDate = LocalDate.of(2030, 1, 15);

    LocalDate oldMonth = view.lastMonth;

    controller.selectDate(newDate);

    assertNotNull(view.lastMonth);
    assertNotNull(view.lastMonthEvents);

    assertEquals("Month in view should match selected date's month",
        newDate.withDayOfMonth(1), view.lastMonth.withDayOfMonth(1));
  }

  @Test
  public void testParseEndDateCoversParseBranch() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "parseEndDate", String.class, String.class);
    m.setAccessible(true);

    LocalDate result = (LocalDate) m.invoke(controller, "until", "2025-12-31");

    assertEquals(LocalDate.of(2025, 12, 31), result);
  }

  @Test
  public void testShouldContinueEndDateBranchReturnsFalse() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "shouldContinue", int.class, int.class, LocalDate.class, LocalDate.class);
    m.setAccessible(true);

    LocalDate end = LocalDate.of(2025, 1, 1);
    LocalDate current = LocalDate.of(2025, 1, 5);

    boolean result = (boolean) m.invoke(controller,
        0,
        -1,
        current,
        end
    );

    assertFalse(result);
  }

  @Test
  public void testParseLimitReturnsMinusOneForInvalidInput() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "parseLimit", String.class, String.class);
    m.setAccessible(true);

    int result = (int) m.invoke(controller, "count", "   ");

    assertEquals(-1, result);
  }

  @Test
  public void testCreateSingleAndRecurringEventsAndSeriesDetection() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    int originalCount = cal.findEvents(e -> true).size();

    LocalDateTime singleStart = LocalDateTime.of(2025, 11, 24, 9, 0);
    LocalDateTime singleEnd = singleStart.plusHours(1);
    Event.EventBuilder singleBuilder = Event.builder()
        .setSubject("Single event")
        .setLocation("Room 1")
        .setStart(singleStart)
        .setEnd(singleEnd);
    controller.createEvent(singleBuilder, "", "", "");

    LocalDateTime seriesStart = LocalDateTime.of(2025, 11, 25, 9, 0);
    LocalDateTime seriesEnd = seriesStart.plusHours(1);
    Event.EventBuilder seriesBuilder = Event.builder()
        .setSubject("Series event")
        .setLocation("Room 2")
        .setStart(seriesStart)
        .setEnd(seriesEnd);
    controller.createEvent(seriesBuilder, "MTWRF", "count", "3");

    List<Event> all = cal.findEvents(e -> true);
    assertEquals(originalCount + 4, all.size());

    Event single = all.stream()
        .filter(e -> "Single event".equals(e.getSubject()))
        .findFirst()
        .orElseThrow();

    Event series = all.stream()
        .filter(e -> "Series event".equals(e.getSubject()))
        .findFirst()
        .orElseThrow();

    assertFalse(controller.isEventPartofSeries(single));
    assertTrue(controller.isEventPartofSeries(series));
  }

  @Test
  public void testEditCalendarCoversRenameBranch() throws Exception {
    GuiController controller = new GuiController(model, view);

    assertEquals("Personal", model.getActiveCalendar().getName());

    controller.editCalendar("RenamedCal", ZoneId.of("Europe/London"));

    assertEquals("RenamedCal", model.getActiveCalendar().getName());

    assertEquals("Calendar updated.", view.lastInfoMessage);

    assertTrue(view.lastCalendarNames.contains("RenamedCal"));
  }

  @Test
  public void testSetEventEndThrowsWhenEndBeforeStart() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 11, 23, 10, 0);
    LocalDateTime end = start.plusHours(1);

    Event.EventBuilder builder = Event.builder()
        .setSubject("BadEndEvent")
        .setStart(start)
        .setEnd(end);

    LocalDateTime invalidNewEnd = start.minusHours(2);

    Method m = GuiController.class.getDeclaredMethod(
        "setEventEnd",
        Event.EventBuilder.class,
        LocalDateTime.class
    );
    m.setAccessible(true);

    try {
      m.invoke(controller, builder, invalidNewEnd);
      fail("Expected RuntimeException for end < start");
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      assertTrue(cause instanceof RuntimeException);
      assertEquals("New End cannot be before Start.", cause.getMessage());
    }
  }

  @Test
  public void testSearchAndBulkEditBySubjectUpdatesLocation() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 11, 24, 14, 0);
    LocalDateTime end = start.plusHours(1);
    Event extra = Event.builder()
        .setSubject("team meeting")
        .setLocation("Old location")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(extra);

    controller.searchAndBulkEdit(
        "subject", "meeting", null,
        "location", "NewPlace");

    List<Event> meetings = cal.findEvents(
        e -> e.getSubject().toLowerCase().contains("meeting"));
    assertFalse(meetings.isEmpty());
    for (Event e : meetings) {
      assertEquals("NewPlace", e.getLocation());
    }

    assertNotNull(view.lastMonth);
    assertNotNull(view.lastMonthEvents);
  }

  @Test
  public void testCreateSearchFilterTimeRange() throws Exception {

    FakeAppManager model = new FakeAppManager();
    FakeGuiView view = new FakeGuiView();

    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "createSearchFilter", String.class, String.class, String.class);
    m.setAccessible(true);

    @SuppressWarnings("unchecked")
    Predicate<Event> filter = (Predicate<Event>)
        m.invoke(controller, "time range",
            "2025-11-23T10:00", "2025-11-23T12:00");

    Event e = Event.builder()
        .setSubject("X")
        .setStart(LocalDateTime.parse("2025-11-23T10:30"))
        .setEnd(LocalDateTime.parse("2025-11-23T11:30"))
        .build();

    assertTrue(filter.test(e));
  }

  @Test
  public void testCreateSearchFilterTime() throws Exception {

    GuiController controller =
        new GuiController(new FakeAppManager(), new FakeGuiView());

    // Access private method via reflection
    Method m = GuiController.class.getDeclaredMethod(
        "createSearchFilter", String.class, String.class, String.class);
    m.setAccessible(true);

    @SuppressWarnings("unchecked")
    Predicate<Event> filter = (Predicate<Event>)
        m.invoke(controller, "time",
            "2025-11-23T09:00", "2025-11-23T13:00");

    Event e = Event.builder()
        .setSubject("X")
        .setStart(LocalDateTime.parse("2025-11-23T10:30"))
        .setEnd(LocalDateTime.parse("2025-11-23T11:30"))
        .build();

    assertTrue(filter.test(e));
  }

  @Test
  public void testCreateSearchFilterTimeRangeCoversPredicate() throws Exception {
    GuiController controller =
        new GuiController(new FakeAppManager(), new FakeGuiView());

    Method m = GuiController.class.getDeclaredMethod(
        "createSearchFilter", String.class, String.class, String.class);
    m.setAccessible(true);

    LocalDateTime start = LocalDateTime.parse("2025-11-23T10:00");
    LocalDateTime end = LocalDateTime.parse("2025-11-23T12:00");

    @SuppressWarnings("unchecked")
    Predicate<Event> filter = (Predicate<Event>)
        m.invoke(controller, "time range", start.toString(), end.toString());

    Event inside = Event.builder()
        .setSubject("InsideRange")
        .setStart(LocalDateTime.parse("2025-11-23T10:30"))
        .setEnd(LocalDateTime.parse("2025-11-23T11:30"))
        .build();

    assertTrue("Predicate must match event fully inside range", filter.test(inside));
  }

  @Test
  public void testCreateSearchFilterTimeRangePredicateFullCoverage() throws Exception {
    GuiController controller =
        new GuiController(new ApplicationManagerImpl(), new FakeGuiView());

    Method m = GuiController.class.getDeclaredMethod(
        "createSearchFilter", String.class, String.class, String.class);
    m.setAccessible(true);

    @SuppressWarnings("unchecked")
    Predicate<Event> filter = (Predicate<Event>)
        m.invoke(controller, "time range",
            "2025-11-23T10:00", "2025-11-23T12:00");

    Event inside = Event.builder()
        .setSubject("in")
        .setStart(LocalDateTime.parse("2025-11-23T10:30"))
        .setEnd(LocalDateTime.parse("2025-11-23T11:30"))
        .build();
    assertTrue(filter.test(inside));

    Event rightFail = Event.builder()
        .setSubject("rfail")
        .setStart(LocalDateTime.parse("2025-11-23T10:30"))
        .setEnd(LocalDateTime.parse("2025-11-23T12:30"))
        .build();
    assertFalse(filter.test(rightFail));

    Event leftFail = Event.builder()
        .setSubject("lfail")
        .setStart(LocalDateTime.parse("2025-11-23T09:30"))
        .setEnd(LocalDateTime.parse("2025-11-23T11:00"))
        .build();
    assertFalse(filter.test(leftFail));

    Event bothFail = Event.builder()
        .setSubject("bfail")
        .setStart(LocalDateTime.parse("2025-11-23T09:00"))
        .setEnd(LocalDateTime.parse("2025-11-23T13:00"))
        .build();
    assertFalse(filter.test(bothFail));
  }

  @Test
  public void testCreateEventNullRepeatDaysCoversBranch() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    int before = cal.findEvents(e -> true).size();

    LocalDateTime start = LocalDateTime.of(2025, 11, 28, 10, 0);
    LocalDateTime end = start.plusHours(1);
    Event.EventBuilder builder = Event.builder()
        .setSubject("NullRepeat")
        .setStart(start)
        .setEnd(end);

    controller.createEvent(builder, null, "", "");

    int after = cal.findEvents(e -> true).size();
    assertEquals(before + 1, after);
  }


  @Test
  public void testCreateEventEmptyRepeatDaysCoversBranch() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    int before = cal.findEvents(e -> true).size();

    LocalDateTime start = LocalDateTime.of(2025, 11, 28, 11, 0);
    LocalDateTime end = start.plusHours(1);
    Event.EventBuilder builder = Event.builder()
        .setSubject("EmptyRepeat")
        .setStart(start)
        .setEnd(end);

    controller.createEvent(builder, "", "", "");

    int after = cal.findEvents(e -> true).size();
    assertEquals(before + 1, after);
  }

  @Test
  public void testIsWeekdayMatchCoversReturnStatement() throws Exception {
    GuiController controller = new GuiController(
        new ApplicationManagerImpl(), new FakeGuiView()
    );

    Method m = GuiController.class.getDeclaredMethod(
        "isWeekdayMatch", DayOfWeek.class, String.class
    );
    m.setAccessible(true);

    boolean resultTrue = (boolean)
        m.invoke(controller, DayOfWeek.MONDAY, "MTWRF");
    assertTrue("Expected Monday (M) to match MTWRF", resultTrue);

    boolean resultFalse = (boolean)
        m.invoke(controller, DayOfWeek.SUNDAY, "MTWRF");
    assertFalse("Expected Sunday to NOT match MTWRF", resultFalse);
  }


  @Test
  public void testShouldContinueEndDateConditionReturnsFalse() throws Exception {
    GuiController controller = new GuiController(
        new ApplicationManagerImpl(), new FakeGuiView()
    );

    Method m = GuiController.class.getDeclaredMethod(
        "shouldContinue", int.class, int.class,
        LocalDate.class, LocalDate.class
    );
    m.setAccessible(true);

    int count = 0;
    int limit = -1;
    LocalDate endDate = LocalDate.of(2025, 1, 1);
    LocalDate current = LocalDate.of(2025, 1, 5);

    boolean result = (boolean) m.invoke(
        controller, count, limit, current, endDate
    );

    assertFalse("shouldContinue must return false when current > endDate", result);
  }

  @Test
  public void testAddBuiltEventWrapsValidationException() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 11, 23, 10, 0);
    LocalDateTime badEnd = start.minusHours(1);

    Event.EventBuilder badBuilder = Event.builder()
        .setSubject("Bad event")
        .setStart(start)
        .setEnd(badEnd);

    List<Event> target = new ArrayList<>();

    Method m = GuiController.class.getDeclaredMethod(
        "addBuiltEvent", List.class, Event.EventBuilder.class);
    m.setAccessible(true);

    try {
      m.invoke(controller, target, badBuilder);
      fail("Expected RuntimeException from addBuiltEvent");
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      assertTrue(cause instanceof RuntimeException);
      assertTrue("Message should mention error building modified event",
          cause.getMessage().startsWith("Error building modified event: "));
    }

    assertTrue("Target list should remain empty when build fails", target.isEmpty());
  }

  @Test
  public void testSearchAndBulkEditByTimeRange() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    controller.searchAndBulkEdit(
        "time range",
        "2025-11-23T10:00",
        "2025-11-23T11:00",
        "subject",
        "Updated by time");

    List<Event> updated = cal.findEvents(
        e -> "Updated by time".equals(e.getSubject()));
    assertFalse(updated.isEmpty());
  }

  @Test
  public void testBulkEditTimePropertyUpdatesEvents() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 11, 26, 9, 0);
    LocalDateTime end = start.plusHours(1);
    Event.EventBuilder builder = Event.builder()
        .setSubject("SeriesEditTime")
        .setStart(start)
        .setEnd(end);
    controller.createEvent(builder, "MTWRF", "count", "2");

    controller.searchAndBulkEdit(
        "subject", "SeriesEditTime", null,
        "start date", "2025-11-30");

    List<Event> edited = cal.findEvents(
        e -> "SeriesEditTime".equals(e.getSubject()));
    assertFalse("Expected at least one edited event", edited.isEmpty());

    for (Event e : edited) {
      assertEquals(LocalDate.of(2025, 11, 30), e.getStart().toLocalDate());
    }
  }

  @Test
  public void testCommitBulkChangesRestoresOldEventsOnFailure() throws Exception {
    final GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    List<Event> existing = cal.findEvents(e -> true);
    cal.removeEvents(existing);

    LocalDateTime baseDay = LocalDateTime.of(2025, 11, 23, 9, 0);
    Event a = Event.builder()
        .setSubject("A")
        .setStart(baseDay)
        .setEnd(baseDay.plusHours(1))
        .build();

    Event b = Event.builder()
        .setSubject("B")
        .setStart(baseDay.plusHours(2))
        .setEnd(baseDay.plusHours(3))
        .build();

    cal.addEvent(a);
    cal.addEvent(b);

    List<Event> oldEvents = Collections.singletonList(b);

    Event conflictEvent = Event.builder()
        .setSubject("B-conflict")
        .setStart(baseDay.plusMinutes(30))
        .setEnd(baseDay.plusHours(1).plusMinutes(30))
        .build();
    List<Event> newEvents = Collections.singletonList(conflictEvent);

    Method m = GuiController.class.getDeclaredMethod(
        "commitBulkChanges", Calendar.class, List.class, List.class);
    m.setAccessible(true);

    m.invoke(controller, cal, oldEvents, newEvents);

    List<Event> finalEvents = cal.findEvents(e -> true);
    assertEquals("Calendar should have two events after bulk change",
        2, finalEvents.size());
    assertTrue(finalEvents.stream().anyMatch(e -> "A".equals(e.getSubject())));
    assertTrue(finalEvents.stream().anyMatch(e -> "B-conflict".equals(e.getSubject())));
    assertFalse(finalEvents.stream().anyMatch(e -> "B".equals(e.getSubject())));
  }


  @Test(expected = calendar.model.ValidationException.class)
  public void testSearchAndBulkEditNoMatchesThrows() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.searchAndBulkEdit(
        "subject", "this-does-not-exist", null,
        "location", "Somewhere");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchAndBulkEditRejectsUnknownSearchType() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.searchAndBulkEdit(
        "unknown-type", "x", null,
        "location", "New");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchAndBulkEditRejectsUnknownProperty() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.searchAndBulkEdit(
        "subject", "GUI", null,
        "not-a-property", "value");
  }

  @Test
  public void testEditEventForDifferentScopes() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime base = LocalDateTime.of(2025, 11, 26, 9, 0);
    Event.EventBuilder b1 = Event.builder()
        .setSubject("SeriesSingle")
        .setStart(base)
        .setEnd(base.plusHours(1));
    controller.createEvent(b1, "MTWRF", "count", "2");
    List<Event> s1 = cal.findEvents(e -> "SeriesSingle".equals(e.getSubject()));
    Event firstSingle = s1.get(0);
    Event.EventBuilder singleEdit = Event.builder()
        .fromEvent(firstSingle)
        .setSubject("SeriesSingleEdited");
    controller.editEvent(firstSingle, singleEdit,
        calendar.model.EditScope.SINGLE);

    Event.EventBuilder b2 = Event.builder()
        .setSubject("SeriesFuture")
        .setStart(base)
        .setEnd(base.plusHours(1));
    controller.createEvent(b2, "MTWRF", "count", "2");
    List<Event> s2 = cal.findEvents(e -> "SeriesFuture".equals(e.getSubject()));
    Event firstFuture = s2.get(0);
    Event.EventBuilder futureEdit = Event.builder()
        .fromEvent(firstFuture)
        .setSubject("SeriesFutureEdited");
    controller.editEvent(firstFuture, futureEdit,
        calendar.model.EditScope.FUTURE);

    Event.EventBuilder b3 = Event.builder()
        .setSubject("SeriesAll")
        .setStart(base)
        .setEnd(base.plusHours(1));
    controller.createEvent(b3, "MTWRF", "count", "2");
    List<Event> s3 = cal.findEvents(e -> "SeriesAll".equals(e.getSubject()));
    Event firstAll = s3.get(0);
    Event.EventBuilder allEdit = Event.builder()
        .fromEvent(firstAll)
        .setSubject("SeriesAllEdited");
    controller.editEvent(firstAll, allEdit,
        calendar.model.EditScope.ALL);

    assertNotNull(view.lastMonth);
  }

  @Test
  public void testRefreshViewNoActiveCalendarTriggersEmptyMonthView() throws Exception {
    class NoActiveCalendarModel extends ApplicationManagerImpl {
      @Override
      public Calendar getActiveCalendar() throws ValidationException {
        throw new ValidationException("no-active");
      }

      @Override
      public Set<String> getCalendarNames() {
        return Collections.emptySet();
      }
    }

    FakeGuiView view = new FakeGuiView();
    NoActiveCalendarModel model = new NoActiveCalendarModel();

    GuiController controller = new GuiController(model, view);

    assertNotNull(view.lastMonth);
    assertTrue(view.lastMonthEvents.isEmpty());
  }

  @Test
  public void testFilterEventsPredicateIsCovered() throws Exception {
    ApplicationManagerImpl model = new ApplicationManagerImpl();
    FakeGuiView view = new FakeGuiView();
    final GuiController controller = new GuiController(model, view);

    model.createCalendar("TestCal", ZoneId.systemDefault());
    model.setActiveCalendar("TestCal");
    calendar.model.Calendar cal = model.getActiveCalendar();

    LocalDate rangeStart = LocalDate.of(2025, 1, 10);
    LocalDate rangeEnd   = LocalDate.of(2025, 1, 11);

    LocalDateTime startDt = rangeStart.atStartOfDay();
    LocalDateTime endDt   = rangeEnd.atStartOfDay();

    Event inRange = Event.builder()
        .setSubject("In")
        .setStart(startDt.plusHours(1))
        .setEnd(endDt.plusHours(1))
        .build();

    Event outRange = Event.builder()
        .setSubject("Out")
        .setStart(endDt.plusHours(5))
        .setEnd(endDt.plusHours(7))
        .build();

    cal.addEvent(inRange);
    cal.addEvent(outRange);

    Method m = GuiController.class.getDeclaredMethod("filterEvents",
        List.class, LocalDate.class, LocalDate.class);
    m.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<Event> result = (List<Event>) m.invoke(controller,
        List.of(inRange, outRange), rangeStart, rangeEnd);

    assertEquals(1, result.size());
    assertTrue(result.contains(inRange));
    assertFalse(result.contains(outRange));
  }

  @Test
  public void testDeleteCalendarShowMessageCovered() throws Exception {
    FakeAppManager model = new FakeAppManager();
    model.createCalendar("Work", ZoneId.systemDefault());
    model.setActiveCalendar("Work");

    FakeGuiView view = new FakeGuiView();
    GuiController controller = new GuiController(model, view);

    controller.deleteCalendar("Work");

    assertEquals("Calendar 'Work' deleted.", view.lastInfoMessage);
  }

  @Test
  public void testShiftEventStartReturnsUpdatedBuilder() throws Exception {
    FakeAppManager model = new FakeAppManager();
    model.createCalendar("Test", ZoneId.systemDefault());
    model.setActiveCalendar("Test");

    FakeGuiView view = new FakeGuiView();
    GuiController controller = new GuiController(model, view);

    Event.EventBuilder builder = Event.builder()
        .setSubject("TestEvent")
        .setStart(LocalDateTime.of(2025, 1, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 1, 1, 11, 0));

    LocalDateTime newStart = LocalDateTime.of(2025, 1, 2, 8, 0);

    Method m = GuiController.class.getDeclaredMethod(
        "shiftEventStart",
        Event.EventBuilder.class,
        LocalDateTime.class
    );
    m.setAccessible(true);

    Event.EventBuilder result = (Event.EventBuilder) m.invoke(controller, builder, newStart);
    Event shifted = result.build();

    assertEquals(newStart, shifted.getStart());
    assertEquals(newStart.plusHours(1), shifted.getEnd());
  }

  @Test
  public void testExportCalendarCsvAndIcal() throws Exception {
    GuiController controller = new GuiController(model, view);

    java.io.File csv = java.io.File.createTempFile("cal", ".csv");
    try {
      controller.exportCalendar(csv, "CSV");
      assertTrue(csv.length() > 0);
      assertNotNull(view.lastInfoMessage);
    } finally {
      csv.delete();
    }

    view.lastInfoMessage = null;

    java.io.File ical = java.io.File.createTempFile("cal", ".ics");
    try {
      controller.exportCalendar(ical, "ICAL");
      assertTrue(ical.length() > 0);
      assertNotNull(view.lastInfoMessage);
    } finally {
      ical.delete();
    }
  }

  @Test
  public void testIsTimePropertyCoversAllPit() throws Exception {
    GuiController controller = new GuiController(new FakeAppManager(), new FakeGuiView());

    Method m = GuiController.class.getDeclaredMethod("isTimeProperty", String.class);
    m.setAccessible(true);

    assertTrue((Boolean) m.invoke(controller, "start date"));

    assertTrue((Boolean) m.invoke(controller, "end time"));

    assertTrue((Boolean) m.invoke(controller, "datetime"));

    assertFalse((Boolean) m.invoke(controller, "subject"));
  }

  @Test
  public void testSetEventEndReturnsBuilder() throws Exception {
    FakeAppManager model = new FakeAppManager();
    model.createCalendar("Test", ZoneId.systemDefault());
    model.setActiveCalendar("Test");

    FakeGuiView view = new FakeGuiView();
    GuiController controller = new GuiController(model, view);

    Event.EventBuilder builder = Event.builder()
        .setSubject("TestEvent")
        .setStart(LocalDateTime.of(2025, 1, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 1, 1, 11, 0));

    LocalDateTime newEnd = LocalDateTime.of(2025, 1, 1, 12, 0);

    Method m = GuiController.class.getDeclaredMethod(
        "setEventEnd",
        Event.EventBuilder.class,
        LocalDateTime.class
    );
    m.setAccessible(true);

    Event.EventBuilder result =
        (Event.EventBuilder) m.invoke(controller, builder, newEnd);

    Event updated = result.build();

    assertEquals(newEnd, updated.getEnd());

    assertSame(builder, result);
  }


  @Test
  public void testSetEventEndCatchBlock() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.minusHours(1);

    Event.EventBuilder badBuilder = Event.builder()
        .setSubject("Bad")
        .setStart(start)
        .setEnd(end);

    Method m = GuiController.class.getDeclaredMethod(
        "setEventEnd", Event.EventBuilder.class, LocalDateTime.class);
    m.setAccessible(true);

    try {
      m.invoke(controller, badBuilder, start.plusHours(2));
      fail("Expected RuntimeException");
    } catch (InvocationTargetException ite) {
      assertTrue(ite.getCause() instanceof RuntimeException);
    }
  }

  @Test
  public void testExportCalendarCatchBlock() throws Exception {
    GuiController controller = new GuiController(model, view);

    File badFile = new File("/root/forbidden.txt");

    controller.exportCalendar(badFile, "CSV");

    assertNotNull("Expected error message", view.lastErrorMessage);
    assertTrue(view.lastErrorMessage.startsWith("Export failed"));
  }

  @Test
  public void testGetZoneIdCatchBlock() throws Exception {
    ApplicationManager failingModel = new ApplicationManagerImpl() {
      @Override
      public Calendar getCalendar(String name) throws ValidationException {
        throw new ValidationException("forced zone failure");
      }
    };

    FakeGuiView failingView = new FakeGuiView();

    GuiController controller = new GuiController(failingModel, failingView);

    ZoneId z = controller.getZoneId("Anything");

    assertEquals(ZoneId.systemDefault(), z);
  }


  @Test
  public void testGetZoneIdForExistingCalendar() throws Exception {
    GuiController controller = new GuiController(model, view);
    assertEquals(ZoneId.of("America/New_York"),
        controller.getZoneId("Personal"));
  }

  @Test
  public void testChangeDateRangeUpdatesMonthView() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDate firstMonth = view.lastMonth;
    controller.changeDateRange(1);

    assertNotNull(view.lastMonth);
    assertTrue("Month should change when range is advanced",
        !view.lastMonth.equals(firstMonth));
  }

  @Test
  public void testControllerCreatesDefaultCalendarWhenModelIsEmpty() throws Exception {
    ApplicationManager emptyModel = new ApplicationManagerImpl();
    FakeGuiView emptyView = new FakeGuiView();

    new GuiController(emptyModel, emptyView);

    assertNotNull("Active calendar should be created", emptyModel.getActiveCalendar());
    assertNotNull("View calendar list should not be null", emptyView.lastCalendarNames);
    assertTrue("View should have at least one calendar in the list",
        !emptyView.lastCalendarNames.isEmpty());
  }


  @Test
  public void testParseEndDateTypeNotUntil() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "parseEndDate", String.class, String.class);
    m.setAccessible(true);

    LocalDate result = (LocalDate) m.invoke(controller, "COUNT", "2025-12-31");
    assertNull(result);
  }

  @Test
  public void testParseEndDateNullVal() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "parseEndDate", String.class, String.class);
    m.setAccessible(true);

    LocalDate result = (LocalDate) m.invoke(controller, "until", null);
    assertNull(result);
  }

  @Test
  public void testParseEndDateBlankVal() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "parseEndDate", String.class, String.class);
    m.setAccessible(true);

    LocalDate result = (LocalDate) m.invoke(controller, "until", "   ");
    assertNull(result);
  }

  @Test
  public void testParseEndDateAllConditionsTrue() throws Exception {
    GuiController controller = new GuiController(model, view);

    Method m = GuiController.class.getDeclaredMethod(
        "parseEndDate", String.class, String.class);
    m.setAccessible(true);

    LocalDate result = (LocalDate) m.invoke(controller, "until", "2025-12-31");
    assertEquals(LocalDate.of(2025, 12, 31), result);
  }


  @Test
  public void testDeleteCalendarRemovesCalendarAndRefreshesView() throws Exception {
    model.createCalendar("Work", ZoneId.of("America/New_York"));
    model.setActiveCalendar("Work");
    new GuiController(model, view);

    assertTrue(view.lastCalendarNames.contains("Personal"));
    assertTrue(view.lastCalendarNames.contains("Work"));

    GuiController controller = new GuiController(model, view);
    controller.deleteCalendar("Work");

    assertTrue(view.lastCalendarNames.contains("Personal"));
    assertTrue("Work calendar should be removed",
        !view.lastCalendarNames.contains("Work"));
  }

  @Test
  public void testSetViewModeSwitchesToWeekAndDay() throws Exception {
    GuiController controller = new GuiController(model, view);

    controller.setViewMode(ViewMode.WEEK);
    assertEquals(ViewMode.WEEK, view.lastViewMode);
    assertNotNull("Week start should be set", view.lastWeekStart);

    controller.setViewMode(ViewMode.DAY);
    assertEquals(ViewMode.DAY, view.lastViewMode);
    assertNotNull("Day date should be set", view.lastDayDate);
  }

  @Test
  public void testEditCalendarUpdatesTimeZone() throws Exception {
    GuiController controller = new GuiController(model, view);

    ZoneId original = controller.getZoneId("Personal");
    ZoneId newZone = ZoneId.of("Europe/London");

    controller.editCalendar("Personal", newZone);

    ZoneId updated = controller.getZoneId("Personal");
    assertEquals("Calendar time zone should be updated", newZone, updated);

    assertTrue("View should still list 'Personal' calendar",
        view.lastCalendarNames.contains("Personal"));
  }

  @Test
  public void testSetEventEndUpdatesBuilderEndTime() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 11, 23, 10, 0);
    LocalDateTime initialEnd = start.plusHours(1);
    LocalDateTime newEnd = start.plusHours(2);

    Event.EventBuilder builder = Event.builder()
        .setSubject("End-time test")
        .setStart(start)
        .setEnd(initialEnd);

    Method m = GuiController.class.getDeclaredMethod(
        "setEventEnd", Event.EventBuilder.class, LocalDateTime.class);
    m.setAccessible(true);
    m.invoke(controller, builder, newEnd);

    Event e = builder.build();
    assertEquals("setEventEnd should set the builder's end time",
        newEnd, e.getEnd());
  }

  @Test
  public void testSearchAndBulkEditUpdatesStartTime() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 11, 29, 9, 0);
    LocalDateTime end = start.plusHours(1);

    Event e = Event.builder()
        .setSubject("StartTimeTest")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(e);

    controller.searchAndBulkEdit(
        "subject", "StartTimeTest", null,
        "start time", "11:30");

    Event updated = cal.findEvents(ev -> "StartTimeTest".equals(ev.getSubject()))
        .get(0);

    assertEquals("Date should not change",
        start.toLocalDate(), updated.getStart().toLocalDate());
    assertEquals("Start time should be updated",
        LocalTime.parse("11:30"), updated.getStart().toLocalTime());
  }

  @Test
  public void testSearchAndBulkEditUpdatesEndDate() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 11, 29, 9, 0);
    LocalDateTime end = start.plusHours(1);

    Event e = Event.builder()
        .setSubject("EndDateTest")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(e);

    controller.searchAndBulkEdit(
        "subject", "EndDateTest", null,
        "end date", "2025-12-05");

    Event updated = cal.findEvents(ev -> "EndDateTest".equals(ev.getSubject()))
        .get(0);

    assertEquals("End date should be updated",
        LocalDate.parse("2025-12-05"), updated.getEnd().toLocalDate());
    assertEquals("End time should stay the same",
        end.toLocalTime(), updated.getEnd().toLocalTime());
  }

  @Test
  public void testGetStartCatchesException() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.minusHours(1);

    Event.EventBuilder badBuilder = Event.builder()
        .setSubject("Bad")
        .setStart(start)
        .setEnd(end);

    Method m = GuiController.class.getDeclaredMethod("getStart", Event.EventBuilder.class);
    m.setAccessible(true);

    try {
      m.invoke(controller, badBuilder);
      fail("Expected RuntimeException");
    } catch (InvocationTargetException ite) {
      assertTrue(ite.getCause() instanceof RuntimeException);
    }
  }

  @Test
  public void testGetEndCatchesException() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.minusHours(1);

    Event.EventBuilder badBuilder = Event.builder()
        .setSubject("Bad")
        .setStart(start)
        .setEnd(end);

    Method m = GuiController.class.getDeclaredMethod("getEnd", Event.EventBuilder.class);
    m.setAccessible(true);

    try {
      m.invoke(controller, badBuilder);
      fail("Expected RuntimeException");
    } catch (InvocationTargetException ite) {
      assertTrue(ite.getCause() instanceof RuntimeException);
    }
  }

  @Test
  public void testShiftEventStartCatchesException() throws Exception {
    GuiController controller = new GuiController(model, view);

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.minusHours(1);

    Event.EventBuilder badBuilder = Event.builder()
        .setSubject("Bad")
        .setStart(start)
        .setEnd(end);

    Method m = GuiController.class.getDeclaredMethod(
        "shiftEventStart", Event.EventBuilder.class, LocalDateTime.class);
    m.setAccessible(true);

    try {
      m.invoke(controller, badBuilder, start.plusHours(2));
      fail("Expected RuntimeException");
    } catch (InvocationTargetException ite) {
      assertTrue(ite.getCause() instanceof RuntimeException);
    }
  }

  @Test
  public void testIsEventPartOfSeriesCatchBlock() throws Exception {

    ApplicationManager badModel = new ApplicationManagerImpl() {
      @Override
      public calendar.model.Calendar getActiveCalendar() throws ValidationException {
        throw new ValidationException("forced failure for testing");
      }
    };

    FakeGuiView badView = new FakeGuiView();
    GuiController controller = new GuiController(badModel, badView);

    Event evt = Event.builder()
        .setSubject("X")
        .setStart(LocalDateTime.of(2025, 1, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 1, 1, 11, 0))
        .build();

    boolean result = controller.isEventPartofSeries(evt);

    assertFalse(result);
  }

  @Test
  public void testSwitchCalendarUpdatesActiveCalendarAndRefreshesView() throws Exception {
    model.createCalendar("Work", ZoneId.of("America/Los_Angeles"));

    GuiController controller = new GuiController(model, view);

    assertEquals("Personal", view.lastActiveCalendar);

    controller.switchCalendar("Work");

    assertEquals("Work", model.getActiveCalendar().getName());

    assertEquals("Work", view.lastActiveCalendar);
    assertNotNull(view.lastMonth);
    assertNotNull(view.lastMonthEvents);
  }

  @Test
  public void testCommitBulkChangesCatchBlockIsCovered() throws Exception {
    GuiController controller = new GuiController(model, view);

    FailingCalendarForBulk fakeCal = new FailingCalendarForBulk();

    Event oldEvt = Event.builder()
        .setSubject("old")
        .setStart(LocalDateTime.of(2025, 11, 23, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 23, 10, 0))
        .build();

    Event newEvt = Event.builder()
        .setSubject("new")
        .setStart(LocalDateTime.of(2025, 11, 23, 11, 0))
        .setEnd(LocalDateTime.of(2025, 11, 23, 12, 0))
        .build();

    List<Event> oldEvents = Collections.singletonList(oldEvt);
    List<Event> newEvents = Collections.singletonList(newEvt);

    Method m = GuiController.class.getDeclaredMethod(
        "commitBulkChanges",
        calendar.model.Calendar.class,
        List.class,
        List.class
    );
    m.setAccessible(true);

    try {
      m.invoke(controller, fakeCal, oldEvents, newEvents);
      fail("Expected ValidationException");
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      assertTrue(cause instanceof ValidationException);
      assertTrue(cause.getMessage().startsWith("Bulk update failed"));
    }

    assertTrue(fakeCal.removeCalled);
    assertTrue(fakeCal.oldEventsRestored);
  }

  @Test
  public void testSearchAndBulkEditUpdatesEndTime() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 11, 29, 9, 0);
    LocalDateTime end = start.plusHours(1);

    Event e = Event.builder()
        .setSubject("EndTimeTest")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(e);

    controller.searchAndBulkEdit(
        "subject", "EndTimeTest", null,
        "end time", "15:45");

    Event updated = cal.findEvents(ev -> "EndTimeTest".equals(ev.getSubject()))
        .get(0);

    assertEquals("End date should stay the same",
        end.toLocalDate(), updated.getEnd().toLocalDate());
    assertEquals("End time should be updated",
        LocalTime.parse("15:45"), updated.getEnd().toLocalTime());
  }

  @Test
  public void testEditEventUsesSingleBranch() throws Exception {
    ApplicationManagerImpl model = new ApplicationManagerImpl();
    model.createCalendar("A", ZoneId.systemDefault());
    GuiController controller = new GuiController(model, new FakeGuiView());

    Event original = Event.builder()
        .setSubject("x")
        .setStart(LocalDateTime.of(2025, 1, 1, 10, 0))
        .setEnd(LocalDateTime.of(2025, 1, 1, 11, 0))
        .setSeriesId(UUID.randomUUID())
        .build();

    model.getActiveCalendar().addEvent(original);

    Event.EventBuilder newB = Event.builder().fromEvent(original).setSubject("updated");

    controller.editEvent(original, newB, EditScope.SINGLE);

    List<Event> all = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, all.size());
    assertEquals("updated", all.get(0).getSubject());
  }

  /**
   * A fake ApplicationManager that always throws on createCalendar
   * and reports no calendars. Used to trigger the error path in
   * initializeDefaultCalendar().
   */
  private static class FailingCreateCalendarManager extends ApplicationManagerImpl {

    @Override
    public java.util.Set<String> getCalendarNames() {
      return java.util.Collections.emptySet();
    }

    @Override
    public void createCalendar(String name, ZoneId zoneId) throws ValidationException {
      throw new ValidationException("forced failure for test");
    }
  }


  /**
   * ApplicationManager that always fails to provide an active calendar.
   * Used to drive the else-branch in refreshView (no active calendar).
   */
  private static class NoActiveCalendarManager extends ApplicationManagerImpl {

    @Override
    public Calendar getActiveCalendar() throws ValidationException {
      throw new ValidationException("no active calendar for test");
    }

    @Override
    public void createCalendar(String name, ZoneId zoneId) throws ValidationException {
      throw new ValidationException("forced failure for test");
    }

    @Override
    public java.util.Set<String> getCalendarNames() {
      return Collections.emptySet();
    }
  }

  private static class FailingCalendarForBulk extends calendar.model.Calendar {

    boolean removeCalled = false;
    boolean oldEventsRestored = false;
    boolean firstAdd = true;

    FailingCalendarForBulk() {
      super("FailingCal", java.time.ZoneId.systemDefault());
    }

    @Override
    public void removeEvents(Collection<Event> events) {
      removeCalled = true;
    }

    @Override
    public void addEvents(Collection<Event> events) throws ValidationException {
      if (firstAdd) {
        firstAdd = false;
        throw new ValidationException("forced failure");
      } else {
        oldEventsRestored = true;
      }
    }
  }

  /**
   * Simple fake implementation of {@link GuiView} that just records
   * what the controller tells it to do. No Swing, no dialogs.
   */
  private static class FakeGuiView implements GuiView {
    Set<String> lastCalendarNames = new HashSet<>();
    String lastActiveCalendar;

    LocalDate lastMonth;
    List<Event> lastMonthEvents = new ArrayList<>();

    LocalDate lastWeekStart;
    List<Event> lastWeekEvents = new ArrayList<>();

    LocalDate lastDayDate;
    List<Event> lastDayEvents = new ArrayList<>();

    ViewMode lastViewMode;

    String lastErrorMessage;
    String lastInfoMessage;

    boolean visible = false;

    @Override
    public void setCalendarList(Set<String> calendarNames, String activeCalendar) {
      this.lastCalendarNames.clear();
      if (calendarNames != null) {
        this.lastCalendarNames.addAll(calendarNames);
      }
      this.lastActiveCalendar = activeCalendar;
    }

    @Override
    public void updateMonthView(LocalDate currentMonth, List<Event> monthEvents) {
      this.lastMonth = currentMonth;
      this.lastMonthEvents =
          monthEvents == null ? new ArrayList<>() : new ArrayList<>(monthEvents);
    }

    @Override
    public void updateWeekView(LocalDate startOfWeek, List<Event> weekEvents) {
      this.lastWeekStart = startOfWeek;
      this.lastWeekEvents =
          weekEvents == null ? new ArrayList<>() : new ArrayList<>(weekEvents);
    }

    @Override
    public void updateDayView(LocalDate date, List<Event> dayEvents) {
      this.lastDayDate = date;
      this.lastDayEvents =
          dayEvents == null ? new ArrayList<>() : new ArrayList<>(dayEvents);
    }

    @Override
    public void setViewMode(ViewMode mode) {
      this.lastViewMode = mode;
    }

    @Override
    public void showError(String message) {
      this.lastErrorMessage = message;
    }

    @Override
    public void showMessage(String message) {
      this.lastInfoMessage = message;
    }

    @Override
    public void setVisible(boolean visible) {
      this.visible = visible;
    }
  }
}
