package calendar.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implements the ApplicationManager interface.
 * Manages a collection of Calendars mapped by their unique names.
 */
public class ApplicationManagerImpl implements ApplicationManager {

  private final Map<String, Calendar> calendars = new HashMap<>();
  private String activeCalendarName = null;

  @Override
  public void createCalendar(final String name, final ZoneId zoneId) throws ValidationException {
    Objects.requireNonNull(name);
    Objects.requireNonNull(zoneId);
    if (calendars.containsKey(name)) {
      throw new ValidationException("A calendar with the name '" + name + "' already exists.");
    }
    calendars.put(name, new Calendar(name, zoneId));
    if (activeCalendarName == null) {
      activeCalendarName = name;
    }
  }

  @Override
  public void editCalendarName(final String oldName, final String newName)
      throws ValidationException {
    if (!calendars.containsKey(oldName)) {
      throw new ValidationException("Calendar '" + oldName + "' not found.");
    }
    if (calendars.containsKey(newName)) {
      throw new ValidationException("A calendar with the name '" + newName + "' already exists.");
    }
    Calendar calendar = calendars.remove(oldName);
    calendar.setName(newName);
    calendars.put(newName, calendar);

    if (Objects.equals(activeCalendarName, oldName)) {
      activeCalendarName = newName;
    }
  }

  @Override
  public void editCalendarTimezone(final String calendarName, final ZoneId newZoneId)
      throws ValidationException {
    Calendar cal = getCalendar(calendarName);
    ZoneId oldZone = cal.getZoneId();

    if (!oldZone.equals(newZoneId)) {
      List<Event> allEvents = cal.findEvents(e -> true);
      List<Event> convertedEvents = new ArrayList<>();

      for (Event e : allEvents) {
        ZonedDateTime startZoned = e.getStart().atZone(oldZone);
        LocalDateTime newStart = startZoned.withZoneSameInstant(newZoneId).toLocalDateTime();

        ZonedDateTime endZoned = e.getEnd().atZone(oldZone);
        LocalDateTime newEnd = endZoned.withZoneSameInstant(newZoneId).toLocalDateTime();

        Event.EventBuilder builder = Event.builder().fromEvent(e);
        builder.setStart(newStart);
        builder.setEnd(newEnd);
        convertedEvents.add(builder.build());
      }

      cal.removeEvents(allEvents);
      cal.addEvents(convertedEvents);
      cal.setZoneId(newZoneId);
    }
  }

  @Override
  public void deleteCalendar(String name) throws ValidationException {
    if (!calendars.containsKey(name)) {
      throw new ValidationException("Calendar '" + name + "' not found.");
    }
    calendars.remove(name);
    if (activeCalendarName.equals(name)) {
      activeCalendarName = calendars.isEmpty() ? null : calendars.keySet().iterator().next();
    }
  }

  @Override
  public void setActiveCalendar(final String name) throws ValidationException {
    if (!calendars.containsKey(name)) {
      throw new ValidationException("Calendar '" + name + "' not found.");
    }
    this.activeCalendarName = name;
  }

  @Override
  public Calendar getActiveCalendar() throws ValidationException {
    if (activeCalendarName == null) {
      throw new ValidationException("No active calendar selected.");
    }
    return calendars.get(activeCalendarName);
  }

  @Override
  public Calendar getCalendar(final String name) throws ValidationException {
    if (!calendars.containsKey(name)) {
      throw new ValidationException("Calendar '" + name + "' not found.");
    }
    return calendars.get(name);
  }

  @Override
  public Set<String> getCalendarNames() {
    return Collections.unmodifiableSet(calendars.keySet());
  }
}