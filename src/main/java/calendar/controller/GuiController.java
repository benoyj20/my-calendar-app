package calendar.controller;

import calendar.io.EventCsvExporter;
import calendar.io.EventIcalExporter;
import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.EditScope;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.GuiView;
import calendar.view.ViewMode;
import java.io.File;
import java.io.FileWriter;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The main controller for the Swing GUI.
 * It processes inputs like creating events or switching views
 * and tells the UI what to display next.
 */
public class GuiController implements ControllerFeatures {

  private final ApplicationManager model;
  private GuiView view;
  private LocalDate currentDateMarker;
  private ViewMode currentMode;

  private Map<String, BiFunction<Event.EventBuilder, String, Event.EventBuilder>> modifierMap;

  /**
   * Sets up the controller with the given data model.
   *
   * @param model The model for the application.
   */
  public GuiController(ApplicationManager model) {
    this.model = model;
    this.currentDateMarker = LocalDate.now();
    this.currentMode = ViewMode.MONTH;
    initializeModifiers();
    initializeDefaultCalendar();
  }

  /**
   * Convenience constructor so tests (or the GUI launcher) can inject a view
   * directly and have the controller wire itself up.
   *
   * @param model the application model
   * @param view  the GUI view implementation
   */
  public GuiController(ApplicationManager model, GuiView view) {
    this(model);    // do normal setup
    setView(view);  // attach the view and refresh it
  }

  private void initializeModifiers() {
    modifierMap = new HashMap<>();
    modifierMap.put("subject", Event.EventBuilder::setSubject);
    modifierMap.put("description", Event.EventBuilder::setDescription);
    modifierMap.put("location", Event.EventBuilder::setLocation);
    modifierMap.put("status", (b, v) -> b.setPrivate("private".equalsIgnoreCase(v)));

    modifierMap.put("start date", (b, v) -> shiftEventStart(b,
        LocalDateTime.of(LocalDate.parse(v), getStart(b).toLocalTime())));

    modifierMap.put("start time", (b, v) -> shiftEventStart(b,
        LocalDateTime.of(getStart(b).toLocalDate(), LocalTime.parse(v))));

    modifierMap.put("end date", (b, v) -> setEventEnd(b,
        LocalDateTime.of(LocalDate.parse(v), getEnd(b).toLocalTime())));

    modifierMap.put("end time", (b, v) -> setEventEnd(b,
        LocalDateTime.of(getEnd(b).toLocalDate(), LocalTime.parse(v))));
  }

  private void initializeDefaultCalendar() {
    if (model.getCalendarNames().isEmpty()) {
      try {
        model.createCalendar("Personal", ZoneId.systemDefault());
      } catch (ValidationException e) {
        view.showError(e.getMessage());
      }
    }
    ensureActiveCalendar();
  }

  @Override
  public void setView(GuiView view) {
    this.view = view;
    // build the initial GUI state from the model
    refreshView();
    // finally, show the window (needed for the FakeGuiView test)
    this.view.setVisible(true);
  }

  private void ensureActiveCalendar() {
    if (!model.getCalendarNames().isEmpty()) {
      try {
        model.getActiveCalendar();
      } catch (ValidationException e) {
        try {
          model.setActiveCalendar(model.getCalendarNames().iterator().next());
        } catch (ValidationException ex) {
          view.showError(ex.getMessage());
        }
      }
    }
  }

  private void refreshView() {
    if (view == null) {
      return;
    }

    Calendar active = null;
    String activeName = null;

    try {
      active = model.getActiveCalendar();
      activeName = active.getName();
    } catch (ValidationException e) {
      view.showError(e.getMessage());
    }

    view.setCalendarList(model.getCalendarNames(), activeName);

    if (active != null) {
      view.setViewMode(currentMode);
      updateCurrentViewPanel(active);
    } else {
      view.updateMonthView(currentDateMarker, Collections.emptyList());
    }
  }

  private void updateCurrentViewPanel(Calendar active) {
    List<Event> allEvents = active.findEvents(e -> true);
    if (currentMode == ViewMode.MONTH) {
      updateMonthView(allEvents);
    } else if (currentMode == ViewMode.WEEK) {
      updateWeekView(allEvents);
    } else {
      updateDayView(allEvents);
    }
  }

  private void updateMonthView(List<Event> all) {
    LocalDate start = currentDateMarker.withDayOfMonth(1);
    LocalDate end = start.plusMonths(1);
    view.updateMonthView(start, filterEvents(all, start, end));
  }

  private void updateWeekView(List<Event> all) {
    LocalDate start = currentDateMarker.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate end = start.plusDays(7);
    view.updateWeekView(start, filterEvents(all, start, end));
  }

  private void updateDayView(List<Event> all) {
    LocalDate end = currentDateMarker.plusDays(1);
    view.updateDayView(currentDateMarker, filterEvents(all, currentDateMarker, end));
  }

  private List<Event> filterEvents(List<Event> all, LocalDate start, LocalDate end) {
    LocalDateTime startDt = start.atStartOfDay();
    LocalDateTime endDt = end.atStartOfDay();
    return all.stream()
        .filter(e -> e.getStart().isBefore(endDt) && e.getEnd().isAfter(startDt))
        .collect(Collectors.toList());
  }

  @Override
  public void createCalendar(String name, ZoneId zoneId) throws Exception {
    model.createCalendar(name, zoneId);
    model.setActiveCalendar(name);
    refreshView();
    view.showMessage("Calendar '" + name + "' created.");
  }

  @Override
  public void editCalendar(String newName, ZoneId newZoneId) throws Exception {
    String currentName = model.getActiveCalendar().getName();
    if (!currentName.equals(newName)) {
      model.editCalendarName(currentName, newName);
    }
    model.editCalendarTimezone(newName, newZoneId);
    refreshView();
    view.showMessage("Calendar updated.");
  }

  @Override
  public void deleteCalendar(String name) throws Exception {
    model.deleteCalendar(name);
    refreshView();
    view.showMessage("Calendar '" + name + "' deleted.");
  }

  @Override
  public void switchCalendar(String name) throws Exception {
    model.setActiveCalendar(name);
    refreshView();
  }

  @Override
  public void changeDateRange(int amount) {
    if (currentMode == ViewMode.MONTH) {
      currentDateMarker = currentDateMarker.plusMonths(amount);
    } else if (currentMode == ViewMode.WEEK) {
      currentDateMarker = currentDateMarker.plusWeeks(amount);
    } else {
      currentDateMarker = currentDateMarker.plusDays(amount);
    }
    refreshView();
  }

  @Override
  public void setViewMode(ViewMode mode) {
    this.currentMode = mode;
    refreshView();
  }

  @Override
  public void selectDate(LocalDate date) {
    this.currentDateMarker = date;
    refreshView();
  }

  @Override
  public void createEvent(Event.EventBuilder builder, String repeatDays,
                          String repeatType, String repeatValue)
      throws Exception {
    Calendar cal = model.getActiveCalendar();
    if (repeatDays == null || repeatDays.isEmpty()) {
      cal.addEvent(builder.build());
    } else {
      builder.setSeriesId(UUID.randomUUID());
      cal.addEvents(generateSeries(builder, repeatDays, repeatType, repeatValue));
    }
    refreshView();
  }

  private List<Event> generateSeries(Event.EventBuilder tmpl, String days, String type, String val)
      throws ValidationException {
    List<Event> events = new ArrayList<>();
    Event base = tmpl.build();
    LocalDate current = base.getStart().toLocalDate();
    int count = 0;
    int limit = parseLimit(type, val);
    LocalDate endDate = parseEndDate(type, val);

    while (shouldContinue(count, limit, current, endDate)) {
      if (isWeekdayMatch(current.getDayOfWeek(), days)) {
        events.add(createInstance(tmpl, base, current));
        count++;
      }
      current = current.plusDays(1);
    }
    return events;
  }

  private int parseLimit(String type, String val) {
    if ("count".equalsIgnoreCase(type) && val != null && !val.isBlank()) {
      return Integer.parseInt(val);
    }
    return -1;
  }

  private LocalDate parseEndDate(String type, String val) {
    if ("until".equalsIgnoreCase(type) && val != null && !val.isBlank()) {
      return LocalDate.parse(val);
    }
    return null;
  }

  private boolean shouldContinue(int count, int limit, LocalDate current, LocalDate endDate) {
    if (limit != -1 && count >= limit) {
      return false;
    }
    if (endDate != null && current.isAfter(endDate)) {
      return false;
    }
    return count <= 3650;
  }

  private Event createInstance(Event.EventBuilder tmpl, Event base, LocalDate date)
      throws ValidationException {
    Event.EventBuilder b = Event.builder().fromEvent(base);
    LocalDateTime newStart = LocalDateTime.of(date, base.getStart().toLocalTime());
    LocalDateTime newEnd = LocalDateTime.of(date, base.getEnd().toLocalTime());
    return b.setStart(newStart).setEnd(newEnd).build();
  }

  private boolean isWeekdayMatch(DayOfWeek dow, String weekdays) {
    Map<DayOfWeek, Character> map = Map.of(
        DayOfWeek.MONDAY, 'M', DayOfWeek.TUESDAY, 'T', DayOfWeek.WEDNESDAY, 'W',
        DayOfWeek.THURSDAY, 'R', DayOfWeek.FRIDAY, 'F', DayOfWeek.SATURDAY, 'S');
    return weekdays.toUpperCase().indexOf(map.getOrDefault(dow, 'U')) >= 0;
  }

  @Override
  public boolean isEventPartofSeries(Event event) {
    try {
      List<Event> series = model.getActiveCalendar().findEvents(e ->
          e.getSeriesId() != null && e.getSeriesId().equals(event.getSeriesId()));
      return series.size() > 1;
    } catch (ValidationException e) {
      return false;
    }
  }

  @Override
  public void editEvent(Event original, Event.EventBuilder newBuilder, EditScope scope)
      throws Exception {
    Calendar cal = model.getActiveCalendar();
    List<Event> toRemove = findEventsToEdit(cal, original, scope);

    if (scope == EditScope.SINGLE) {
      newBuilder.setSeriesId(null);
    } else if (scope == EditScope.FUTURE) {
      newBuilder.setSeriesId(UUID.randomUUID());
    }

    cal.removeEvents(toRemove);
    cal.addEvents(buildModifiedEvents(toRemove, newBuilder, scope));
    refreshView();
    view.showMessage("Events updated successfully.");
  }

  private List<Event> findEventsToEdit(Calendar cal, Event original, EditScope scope) {
    UUID sid = original.getSeriesId();
    if (scope == EditScope.SINGLE) {
      return List.of(original);
    }
    if (scope == EditScope.FUTURE) {
      return cal.findEvents(
          e -> e.getSeriesId().equals(sid) && !e.getStart().isBefore(original.getStart()));
    }
    return cal.findEvents(e -> e.getSeriesId().equals(sid));
  }

  private List<Event> buildModifiedEvents(List<Event> oldEvents, Event.EventBuilder tmplBuilder,
                                          EditScope scope) throws ValidationException {
    List<Event> newEvents = new ArrayList<>();
    Event tmpl = tmplBuilder.build();
    long duration = Duration.between(tmpl.getStart(), tmpl.getEnd()).toNanos();

    for (Event old : oldEvents) {
      Event.EventBuilder b = Event.builder().fromEvent(old);
      updateCommonFields(b, tmpl);

      if (scope == EditScope.SINGLE) {
        b.setStart(tmpl.getStart()).setEnd(tmpl.getEnd()).setSeriesId(null);
      } else {
        updateSeriesTime(b, old, tmpl, duration);
        b.setSeriesId(tmpl.getSeriesId());
      }
      newEvents.add(b.build());
    }
    return newEvents;
  }

  private void updateCommonFields(Event.EventBuilder b, Event tmpl) {
    b.setSubject(tmpl.getSubject());
    b.setDescription(tmpl.getDescription());
    b.setLocation(tmpl.getLocation());
    b.setPrivate(tmpl.isPrivate());
    b.setAllDay(tmpl.isAllDay());
  }

  private void updateSeriesTime(Event.EventBuilder b, Event old, Event tmpl, long duration) {
    LocalDateTime s = LocalDateTime.of(old.getStart().toLocalDate(), tmpl.getStart().toLocalTime());
    b.setStart(s).setEnd(s.plusNanos(duration));
  }

  @Override
  public void searchAndBulkEdit(String searchType, String searchArg1, String searchArg2,
                                String property, String newValue) throws Exception {
    Calendar cal = model.getActiveCalendar();
    Predicate<Event> filter = createSearchFilter(searchType, searchArg1, searchArg2);
    List<Event> matches = cal.findEvents(filter);

    if (matches.isEmpty()) {
      throw new ValidationException("No events found matching criteria.");
    }

    Function<Event.EventBuilder, Event.EventBuilder> modifier = getModifier(property, newValue);
    boolean shouldBreakSeries = isTimeProperty(property);

    List<Event> newEvents = applyBulkModifications(matches, modifier, shouldBreakSeries);
    commitBulkChanges(cal, matches, newEvents);

    refreshView();
    view.showMessage("Updated " + matches.size() + " event(s).");
  }

  private boolean isTimeProperty(String property) {
    String p = property.toLowerCase();
    return p.contains("date") || p.contains("time");
  }

  private Predicate<Event> createSearchFilter(String type, String arg1, String arg2) {
    if ("subject".equalsIgnoreCase(type)) {
      return e -> e.getSubject().toLowerCase().contains(arg1.toLowerCase());
    } else if ("time range".equalsIgnoreCase(type) || "time".equalsIgnoreCase(type)) {
      LocalDateTime start = LocalDateTime.parse(arg1);
      LocalDateTime end = LocalDateTime.parse(arg2);
      return e -> !e.getStart().isBefore(start) && !e.getEnd().isAfter(end);
    }
    throw new IllegalArgumentException("Invalid search type");
  }

  private List<Event> applyBulkModifications(List<Event> matches,
                                             Function<Event.EventBuilder,
                                                 Event.EventBuilder> modifier,
                                             boolean breakSeries) {
    List<Event> newEvents = new ArrayList<>();
    for (Event oldEvent : matches) {
      Event.EventBuilder b = Event.builder().fromEvent(oldEvent);
      modifier.apply(b);

      updateSeriesId(b, oldEvent, breakSeries);
      addBuiltEvent(newEvents, b);
    }
    return newEvents;
  }

  private void updateSeriesId(Event.EventBuilder b, Event old, boolean breakSeries) {
    if (breakSeries) {
      b.setSeriesId(null);
    } else {
      b.setSeriesId(old.getSeriesId());
    }
  }

  private void addBuiltEvent(List<Event> events, Event.EventBuilder b) {
    try {
      events.add(b.build());
    } catch (ValidationException e) {
      throw new RuntimeException("Error building modified event: " + e.getMessage());
    }
  }

  private void commitBulkChanges(Calendar cal, List<Event> oldEvents, List<Event> newEvents)
      throws ValidationException {
    cal.removeEvents(oldEvents);
    try {
      cal.addEvents(newEvents);
    } catch (ValidationException e) {
      cal.addEvents(oldEvents);
      throw new ValidationException("Bulk update failed: " + e.getMessage());
    }
  }

  private Function<Event.EventBuilder, Event.EventBuilder> getModifier(String prop, String val) {
    BiFunction<Event.EventBuilder, String, Event.EventBuilder> strategy =
        modifierMap.get(prop.toLowerCase());

    if (strategy == null) {
      throw new IllegalArgumentException("Cannot bulk edit property: " + prop);
    }
    return b -> strategy.apply(b, val);
  }

  private LocalDateTime getStart(Event.EventBuilder b) {
    try {
      return b.build().getStart();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private LocalDateTime getEnd(Event.EventBuilder b) {
    try {
      return b.build().getEnd();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Event.EventBuilder shiftEventStart(Event.EventBuilder b, LocalDateTime newStart) {
    try {
      Event old = b.build();
      long durationSeconds = Duration.between(old.getStart(), old.getEnd()).toSeconds();
      b.setStart(newStart);
      b.setEnd(newStart.plusSeconds(durationSeconds));
      return b;
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
  }

  private Event.EventBuilder setEventEnd(Event.EventBuilder b, LocalDateTime newEnd) {
    try {
      if (newEnd.isBefore(b.build().getStart())) {
        throw new RuntimeException("New End cannot be before Start.");
      }
      b.setEnd(newEnd);
      return b;
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void exportCalendar(File file, String format) {
    try (FileWriter writer = new FileWriter(file)) {
      Calendar cal = model.getActiveCalendar();
      if ("CSV".equalsIgnoreCase(format)) {
        new EventCsvExporter().export(cal.findEvents(e -> true), writer);
      } else {
        new EventIcalExporter().export(cal, writer);
      }
      view.showMessage("Successfully exported to " + file.getAbsolutePath());
    } catch (Exception e) {
      view.showError("Export failed: " + e.getMessage());
    }
  }

  @Override
  public ZoneId getZoneId(String calendarName) {
    try {
      return model.getCalendar(calendarName).getZoneId();
    } catch (ValidationException e) {
      return ZoneId.systemDefault();
    }
  }
}