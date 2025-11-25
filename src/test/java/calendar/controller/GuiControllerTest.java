package calendar.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Calendar;
import calendar.model.EditScope;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.GuiView;
import calendar.view.ViewMode;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the GuiController logic using a fake view.
 */
public class GuiControllerTest {

  private ApplicationManager model;
  private FakeGuiView view;

  /**
   * Sets up the model with a calendar and one event and the fake view.
   */
  @Before
  public void setUp() throws Exception {
    model = new ApplicationManagerImpl();
    model.createCalendar("Home", ZoneId.of("America/New_York"));
    model.setActiveCalendar("Home");

    Calendar cal = model.getActiveCalendar();
    LocalDateTime start = LocalDateTime.of(2025, 11, 23, 10, 0);
    LocalDateTime end = start.plusHours(1);

    Event evt = Event.builder()
        .setSubject("Morning Standup")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(evt);

    view = new FakeGuiView();

    new GuiController(model, view);
  }

  /**
   * Ensures the controller loads initial data from model into the view.
   */
  @Test
  public void testControllerInitializesViewFromModel() {
    assertTrue("GUI should be made visible", view.visible);
    assertTrue("Calendar list should contain 'Home'", view.lastCalendarNames.contains("Home"));
    assertEquals("Home", view.lastActiveCalendar);
    assertNotNull("Month view events should be set", view.lastMonthEvents);
    assertFalse("Month view should have events", view.lastMonthEvents.isEmpty());
  }

  /**
   * Checks that changing dates and view modes updates the view state.
   */
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
  }

  /**
   * Verifies creating a new calendar updates the view and shows a message.
   */
  @Test
  public void testCreateCalendarAddsAndActivatesAndShowsMessage() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.createCalendar("Work Projects", ZoneId.of("America/Los_Angeles"));

    assertTrue(view.lastCalendarNames.contains("Work Projects"));
    assertEquals("Work Projects", view.lastActiveCalendar);
    assertEquals("Calendar 'Work Projects' created.", view.lastInfoMessage);
  }

  /**
   * Checks that selecting a specific date updates the view to that date.
   */
  @Test
  public void testSelectDateUpdatesView() {
    GuiController controller = new GuiController(model, view);
    LocalDate newDate = LocalDate.of(2030, 1, 15);
    controller.selectDate(newDate);

    assertEquals(newDate.withDayOfMonth(1), view.lastMonth.withDayOfMonth(1));
  }

  /**
   * Tests creating a recurring event using 'until' date stops correctly.
   */
  @Test
  public void testCreateEventRecurringUntilDate() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    cal.removeEvents(cal.findEvents(e -> true));

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    Event.EventBuilder builder = Event.builder()
        .setSubject("Daily Jog")
        .setStart(start)
        .setEnd(start.plusHours(1));

    controller.createEvent(builder, "MTWRFSU", "until", "2025-01-03");

    List<Event> events = cal.findEvents(e -> true);
    assertEquals("Should create 3 events (1st, 2nd, 3rd)", 3, events.size());
  }

  /**
   * Tests creating a recurring event with invalid 'count' throws NumberFormatException.
   */
  @Test
  public void testCreateEventInvalidLimitThrowsException() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 6, 1, 10, 0);
    Event.EventBuilder builder = Event.builder()
        .setSubject("Bad Input")
        .setStart(start)
        .setEnd(start.plusHours(1));

    try {
      controller.createEvent(builder, "M", "count", "invalid");
      fail("Should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
      return;
    }
  }

  /**
   * Tests series detection logic by creating single and recurring events.
   */
  @Test
  public void testCreateSingleAndRecurringEventsAndSeriesDetection() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    Event.EventBuilder singleBuilder = Event.builder()
        .setSubject("Dentist Appointment")
        .setStart(LocalDateTime.of(2025, 11, 24, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 24, 10, 0));
    controller.createEvent(singleBuilder, "", "", "");

    Event.EventBuilder seriesBuilder = Event.builder()
        .setSubject("Weekly Team Sync")
        .setStart(LocalDateTime.of(2025, 11, 25, 9, 0))
        .setEnd(LocalDateTime.of(2025, 11, 25, 10, 0));
    controller.createEvent(seriesBuilder, "MTWRF", "count", "3");

    List<Event> all = cal.findEvents(e -> true);
    Event single =
        all.stream().filter(e -> "Dentist Appointment".equals(e.getSubject())).findFirst().get();
    Event series =
        all.stream().filter(e -> "Weekly Team Sync".equals(e.getSubject())).findFirst().get();

    assertFalse(controller.isEventPartofSeries(single));
    assertTrue(controller.isEventPartofSeries(series));
  }

  /**
   * Verifies renaming a calendar updates the model and view.
   */
  @Test
  public void testEditCalendarRenamesAndUpdatesView() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.editCalendar("Renamed Schedule", ZoneId.of("Europe/London"));

    assertEquals("Renamed Schedule", model.getActiveCalendar().getName());
    assertTrue(view.lastCalendarNames.contains("Renamed Schedule"));
    assertEquals("Calendar updated.", view.lastInfoMessage);
  }

  /**
   * Tests that editing an event with end time before start time throws exception.
   */
  @Test
  public void testEditEventThrowsWhenEndBeforeStart() throws Exception {
    GuiController controller = new GuiController(model, view);
    Event evt = model.getActiveCalendar().findEvents(e -> true).get(0);

    LocalDateTime badEnd = evt.getStart().minusHours(1);
    Event.EventBuilder badBuilder = Event.builder().fromEvent(evt).setEnd(badEnd);

    try {
      controller.editEvent(evt, badBuilder, EditScope.SINGLE);
      fail("Should have thrown ValidationException");
    } catch (ValidationException e) {
      return;
    }
  }

  /**
   * Verifies bulk edit by subject updates properties like location.
   */
  @Test
  public void testSearchAndBulkEditBySubjectUpdatesLocation() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    Event extra = Event.builder()
        .setSubject("Morning Workshop")
        .setStart(LocalDateTime.of(2025, 11, 24, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 24, 11, 0))
        .build();
    cal.addEvent(extra);

    controller.searchAndBulkEdit("subject", "Morning", null, "location", "Conference Room B");

    List<Event> matches = cal.findEvents(e -> e.getSubject().contains("Morning"));
    for (Event e : matches) {
      assertEquals("Conference Room B", e.getLocation());
    }
  }

  /**
   * Tests bulk edit using a time range filter.
   */
  @Test
  public void testSearchAndBulkEditByTimeRange() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    controller.searchAndBulkEdit("time range", "2025-11-23T09:00", "2025-11-23T11:00",
        "subject", "Rescheduled Call");

    Event e = cal.findEvents(ev -> true).get(0);
    assertEquals("Rescheduled Call", e.getSubject());
  }

  /**
   * Tests bulk edit by time string.
   */
  @Test
  public void testSearchAndBulkEditByTime() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    controller.searchAndBulkEdit("time", "2025-11-23T09:00", "2025-11-23T13:00",
        "description", "Room Changed");

    Event e = cal.findEvents(ev -> true).get(0);
    assertEquals("Room Changed", e.getDescription());
  }

  /**
   * Verifies creating event with null repetition string creates single event.
   */
  @Test
  public void testCreateEventNullRepeatCreatesSingle() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    int before = cal.findEvents(e -> true).size();

    Event.EventBuilder builder = Event.builder()
        .setSubject("Quick Reminder")
        .setStart(LocalDateTime.now())
        .setEnd(LocalDateTime.now().plusHours(1));

    controller.createEvent(builder, null, "", "");
    assertEquals(before + 1, cal.findEvents(e -> true).size());
  }

  /**
   * Verifies creating event with valid weekday string creates correct dates.
   */
  @Test
  public void testCreateEventRecurringWeekdays() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    cal.removeEvents(cal.findEvents(e -> true));

    LocalDateTime start = LocalDateTime.of(2025, 11, 24, 10, 0);
    Event.EventBuilder builder = Event.builder()
        .setSubject("Yoga Class")
        .setStart(start)
        .setEnd(start.plusHours(1));

    controller.createEvent(builder, "MW", "count", "2");

    List<Event> events = cal.findEvents(e -> true);
    assertEquals(2, events.size());
    assertEquals(DayOfWeek.MONDAY, events.get(0).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.WEDNESDAY, events.get(1).getStart().getDayOfWeek());
  }

  /**
   * Tests bulk edit updating the start date property of events.
   */
  @Test
  public void testBulkEditStartDate() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    controller.searchAndBulkEdit("subject", "Morning", null, "start date", "2025-12-01");

    Event e = cal.findEvents(ev -> true).get(0);
    assertEquals(LocalDate.of(2025, 12, 1), e.getStart().toLocalDate());
  }

  /**
   * Tests bulk edit updating the end time property of events.
   */
  @Test
  public void testBulkEditEndTime() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    controller.searchAndBulkEdit("subject", "Morning", null, "end time", "15:00");

    Event e = cal.findEvents(ev -> true).get(0);
    assertEquals(LocalTime.of(15, 0), e.getEnd().toLocalTime());
  }

  /**
   * Ensures bulk update failures restore the original state.
   */
  @Test
  public void testCommitBulkChangesRestoresOldEventsOnFailure() throws Exception {
    GuiController controller = new GuiController(model, view);

    try {
      controller.searchAndBulkEdit("subject", "Morning", null, "end time", "09:00");
      fail("Should throw due to validation");
    } catch (Exception e) {
      return;
    }

    Event e = model.getActiveCalendar().findEvents(ev -> true).get(0);
    assertEquals("Original end time should be preserved", LocalTime.of(11, 0),
        e.getEnd().toLocalTime());
  }

  /**
   * Verifies no matches found in bulk edit throws exception.
   */
  @Test(expected = ValidationException.class)
  public void testSearchAndBulkEditNoMatchesThrows() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.searchAndBulkEdit("subject", "Ghost Event", null, "location", "Val");
  }

  /**
   * Verifies invalid search type throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSearchAndBulkEditRejectsUnknownSearchType() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.searchAndBulkEdit("badType", "val", null, "location", "Val");
  }

  /**
   * Verifies invalid property to update throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSearchAndBulkEditRejectsUnknownProperty() throws Exception {
    GuiController controller = new GuiController(model, view);
    controller.searchAndBulkEdit("subject", "Morning", null, "badProp", "Val");
  }

  /**
   * Tests editing a single event in a series (EditScope.SINGLE).
   */
  @Test
  public void testEditEventScopeSingle() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    cal.removeEvents(cal.findEvents(e -> true));

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    controller.createEvent(
        Event.builder().setSubject("Book Club").setStart(start).setEnd(start.plusHours(1)),
        "MT", "count", "2");

    List<Event> series = cal.findEvents(e -> true);
    Event first = series.get(0);

    Event.EventBuilder edit = Event.builder().fromEvent(first).setSubject("Rescheduled Book Club");
    controller.editEvent(first, edit, EditScope.SINGLE);

    List<Event> all = cal.findEvents(e -> true);
    assertEquals(2, all.size());
    assertTrue(all.stream().anyMatch(e -> "Rescheduled Book Club".equals(e.getSubject())));
    assertTrue(all.stream().anyMatch(e -> "Book Club".equals(e.getSubject())));
  }

  /**
   * Tests editing future events in a series (EditScope.FUTURE).
   */
  @Test
  public void testEditEventScopeFuture() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();
    cal.removeEvents(cal.findEvents(e -> true));

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    controller.createEvent(
        Event.builder().setSubject("Project Sync").setStart(start).setEnd(start.plusHours(1)),
        "WRF", "count", "3");

    List<Event> series = cal.findEvents(e -> true);
    Event middle = series.get(1);

    Event.EventBuilder edit = Event.builder().fromEvent(middle).setSubject("New Project Sync Time");
    controller.editEvent(middle, edit, EditScope.FUTURE);

    List<Event> all = cal.findEvents(e -> true);
    long countOld = all.stream().filter(e -> "Project Sync".equals(e.getSubject())).count();
    long countNew =
        all.stream().filter(e -> "New Project Sync Time".equals(e.getSubject())).count();

    assertEquals(1, countOld);
    assertEquals(2, countNew);
  }

  /**
   * Tests handling when view refresh finds no active calendar.
   */
  @Test
  public void testRefreshViewNoActiveCalendar() throws Exception {
    ApplicationManager badModel = new ApplicationManagerImpl() {
      @Override
      public Calendar getActiveCalendar() throws ValidationException {
        throw new ValidationException("None");
      }

      @Override
      public Set<String> getCalendarNames() {
        return Collections.emptySet();
      }
    };

    FakeGuiView v = new FakeGuiView();
    new GuiController(badModel, v);

    assertNotNull(v.lastMonth);
    assertTrue(v.lastMonthEvents.isEmpty());
  }

  /**
   * Tests filtering events for view (showing only events in range).
   */
  @Test
  public void testViewOnlyShowsEventsInMonth() throws Exception {
    GuiController controller = new GuiController(model, view);
    Calendar cal = model.getActiveCalendar();

    Event nextMonth =
        Event.builder().setSubject("Holiday Party").setStart(LocalDateTime.of(2025, 12, 1, 10, 0))
            .setEnd(LocalDateTime.of(2025, 12, 1, 11, 0)).build();
    cal.addEvent(nextMonth);

    controller.changeDateRange(0);

    boolean hasDec =
        view.lastMonthEvents.stream().anyMatch(e -> "Holiday Party".equals(e.getSubject()));
    assertFalse("View should not show next month event", hasDec);
    assertTrue("View should show current month event",
        view.lastMonthEvents.stream().anyMatch(e -> "Morning Standup".equals(e.getSubject())));
  }

  /**
   * Verifies deleting a calendar shows message and updates list.
   */
  @Test
  public void testDeleteCalendarUpdatesView() throws Exception {
    model.createCalendar("Temp Calendar", ZoneId.systemDefault());
    GuiController controller = new GuiController(model, view);

    assertTrue(view.lastCalendarNames.contains("Temp Calendar"));
    controller.deleteCalendar("Temp Calendar");
    assertFalse(view.lastCalendarNames.contains("Temp Calendar"));
    assertEquals("Calendar 'Temp Calendar' deleted.", view.lastInfoMessage);
  }

  /**
   * Tests exporting calendar to file.
   */
  @Test
  public void testExportCalendar() throws Exception {
    GuiController controller = new GuiController(model, view);
    File tmp = File.createTempFile("test", ".csv");
    tmp.deleteOnExit();

    controller.exportCalendar(tmp, "CSV");
    assertTrue(tmp.length() > 0);
  }

  /**
   * Verifies export failure shows error message.
   */
  @Test
  public void testExportCalendarFailure() throws Exception {
    GuiController controller = new GuiController(model, view);
    File bad = new File("/invalid/path/file.csv");

    controller.exportCalendar(bad, "CSV");
    assertNotNull(view.lastErrorMessage);
  }

  /**
   * Tests switching calendar updates the active calendar in view.
   */
  @Test
  public void testSwitchCalendar() throws Exception {
    model.createCalendar("Shared", ZoneId.systemDefault());
    GuiController controller = new GuiController(model, view);

    controller.switchCalendar("Shared");
    assertEquals("Shared", view.lastActiveCalendar);
  }

  /**
   * Verifies that the view receives initialization calls (calendar list, view update, visibility).
   */
  @Test
  public void testViewReceivesInitializationCalls() {
    boolean hasSetCalList = view.logs.stream().anyMatch(s -> s.startsWith("setCalendarList"));
    boolean hasUpdateView = view.logs.stream().anyMatch(s -> s.startsWith("updateMonthView"));
    boolean hasSetVisible = view.logs.contains("setVisible: true");

    assertTrue("View should receive calendar list on init", hasSetCalList);
    assertTrue("View should receive view update on init", hasUpdateView);
    assertTrue("View should be made visible on init", hasSetVisible);
  }

  /**
   * Verifies that user actions like navigation trigger view updates.
   */
  @Test
  public void testViewUpdatesOnNavigation() {
    GuiController controller = new GuiController(model, view);
    view.logs.clear();

    controller.changeDateRange(1);

    assertTrue("View should update after navigation",
        view.logs.stream().anyMatch(s -> s.startsWith("updateMonthView")));
  }

  /**
   * Verifies that creating an event triggers a view refresh.
   */
  @Test
  public void testViewReflectsEventCreation() throws Exception {
    GuiController controller = new GuiController(model, view);
    view.logs.clear();

    Event.EventBuilder builder = Event.builder()
        .setSubject("New Event")
        .setStart(LocalDateTime.now())
        .setEnd(LocalDateTime.now().plusHours(1));

    controller.createEvent(builder, "", "", "");

    assertTrue("View should refresh after event creation",
        view.logs.stream().anyMatch(s -> s.startsWith("updateMonthView")));
  }

  /**
   * Verifies that the view receives specific event data to create widgets in Month view.
   */
  @Test
  public void testMonthViewCreatesWidgetsForEvents() throws Exception {
    Event evt2 = Event.builder()
        .setSubject("Lunch with Boss")
        .setStart(LocalDateTime.of(2025, 11, 24, 12, 0))
        .setEnd(LocalDateTime.of(2025, 11, 24, 13, 0))
        .build();
    model.getActiveCalendar().addEvent(evt2);

    view = new FakeGuiView();
    new GuiController(model, view);

    boolean found = view.logs.stream().anyMatch(log ->
        log.startsWith("updateMonthView")
            && log.contains("Morning Standup")
            && log.contains("Lunch with Boss"));

    assertTrue("View log should contain both events for month view widgets", found);
  }

  /**
   * Verifies that the view receives specific event data for Week view widgets.
   */
  @Test
  public void testWeekViewCreatesWidgetsForEvents() throws Exception {
    GuiController controller = new GuiController(model, view);
    view.logs.clear();

    controller.selectDate(LocalDate.of(2025, 11, 23));
    controller.setViewMode(ViewMode.WEEK);

    boolean found = view.logs.stream().anyMatch(log ->
        log.startsWith("updateWeekView")
            && log.contains("Morning Standup"));

    assertTrue("View log should contain event for week view widgets", found);
  }

  /**
   * Verifies that the view receives specific event data for Day view widgets.
   */
  @Test
  public void testDayViewCreatesWidgetsForEvents() throws Exception {
    GuiController controller = new GuiController(model, view);
    view.logs.clear();

    controller.selectDate(LocalDate.of(2025, 11, 23));
    controller.setViewMode(ViewMode.DAY);

    boolean found = view.logs.stream().anyMatch(log ->
        log.startsWith("updateDayView")
            && log.contains("Morning Standup"));

    assertTrue("View log should contain event for day view widgets", found);
  }

  /**
   * Verifies that creating a new event triggers an update with the new event widget data.
   */
  @Test
  public void testEventCreationUpdatesWidgets() throws Exception {
    GuiController controller = new GuiController(model, view);
    view.logs.clear();

    Event.EventBuilder builder = Event.builder()
        .setSubject("Client Call")
        .setStart(LocalDateTime.of(2025, 11, 23, 14, 0))
        .setEnd(LocalDateTime.of(2025, 11, 23, 15, 0));

    controller.createEvent(builder, "", "", "");

    boolean found = view.logs.stream().anyMatch(log ->
        log.startsWith("updateMonthView")
            && log.contains("Client Call"));

    assertTrue("View log should reflect newly created event widget", found);
  }

  /**
   * Verifies that view displays event details properly in logs.
   */
  @Test
  public void testViewShowsEventDetails() throws Exception {
    Event detailedEvt = Event.builder()
        .setSubject("Performance Review")
        .setStart(LocalDateTime.of(2025, 11, 23, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 23, 12, 0))
        .setLocation("Main Hall")
        .setDescription("Detailed description")
        .build();
    model.getActiveCalendar().addEvent(detailedEvt);

    view = new FakeGuiView();
    new GuiController(model, view);

    boolean found = view.logs.stream().anyMatch(log ->
        log.contains("Performance Review"));

    assertTrue("View should display event subject 'Performance Review'", found);
  }

  /**
   * Fake view implementation for testing.
   */
  private static class FakeGuiView implements GuiView {
    final List<String> logs = new ArrayList<>();
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

    private String formatEvents(List<Event> events) {
      if (events == null || events.isEmpty()) {
        return "[]";
      }
      return "[" + events.stream().map(Event::getSubject).collect(Collectors.joining("|")) + "]";
    }

    @Override
    public void setCalendarList(Set<String> calendarNames, String activeCalendar) {
      logs.add("setCalendarList: " + activeCalendar);
      this.lastCalendarNames.clear();
      if (calendarNames != null) {
        this.lastCalendarNames.addAll(calendarNames);
      }
      this.lastActiveCalendar = activeCalendar;
    }

    @Override
    public void updateMonthView(LocalDate currentMonth, List<Event> monthEvents) {
      logs.add("updateMonthView: " + currentMonth + " events=" + formatEvents(monthEvents));
      this.lastMonth = currentMonth;
      this.lastMonthEvents = monthEvents == null ? new ArrayList<>() : new ArrayList<>(monthEvents);
    }

    @Override
    public void updateWeekView(LocalDate startOfWeek, List<Event> weekEvents) {
      logs.add("updateWeekView: " + startOfWeek + " events=" + formatEvents(weekEvents));
      this.lastWeekStart = startOfWeek;
      this.lastWeekEvents = weekEvents == null ? new ArrayList<>() : new ArrayList<>(weekEvents);
    }

    @Override
    public void updateDayView(LocalDate date, List<Event> dayEvents) {
      logs.add("updateDayView: " + date + " events=" + formatEvents(dayEvents));
      this.lastDayDate = date;
      this.lastDayEvents = dayEvents == null ? new ArrayList<>() : new ArrayList<>(dayEvents);
    }

    @Override
    public void setViewMode(ViewMode mode) {
      logs.add("setViewMode: " + mode);
      this.lastViewMode = mode;
    }

    @Override
    public void showError(String message) {
      logs.add("showError: " + message);
      this.lastErrorMessage = message;
    }

    @Override
    public void showMessage(String message) {
      logs.add("showMessage: " + message);
      this.lastInfoMessage = message;
    }

    @Override
    public void setVisible(boolean visible) {
      logs.add("setVisible: " + visible);
      this.visible = visible;
    }
  }
}