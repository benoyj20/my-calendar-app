package calendar.model;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a single calendar, which has a name, a timezone,
 * and an internal CalendarManager to manage its events.
 * This class wraps the original CalendarManager to add new state.
 */
public class Calendar {

  private String name;
  private ZoneId zoneId;
  private final CalendarManager eventManager;

  /**
   * Creates a new calendar.
   *
   * @param name   The unique name
   * @param zoneId The timezone
   */
  public Calendar(final String name, final ZoneId zoneId) {
    this.name = Objects.requireNonNull(name);
    this.zoneId = Objects.requireNonNull(zoneId);
    this.eventManager = new CalendarManagerImpl();
  }


  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = Objects.requireNonNull(name);
  }

  public ZoneId getZoneId() {
    return zoneId;
  }

  public void setZoneId(final ZoneId zoneId) {
    this.zoneId = Objects.requireNonNull(zoneId);
  }

  /**
   * Delegates to the internal event manager.
   */
  public void addEvent(final Event event) throws ValidationException {
    eventManager.addEvent(event);
  }

  /**
   * Delegates to the internal event manager.
   */
  public void addEvents(final Collection<Event> events) throws ValidationException {
    eventManager.addEvents(events);
  }

  /**
   * Delegates to the internal event manager.
   */
  public void removeEvent(final Event event) {
    eventManager.removeEvent(event);
  }

  /**
   * Delegates to the internal event manager.
   */
  public void removeEvents(final Collection<Event> events) {
    eventManager.removeEvents(events);
  }

  /**
   * Delegates to the internal event manager.
   */
  public List<Event> findEvents(final Predicate<Event> filter) {
    return eventManager.findEvents(filter);
  }

  /**
   * Delegates to the internal event manager.
   */
  public boolean hasConflict(final Event event) {
    return eventManager.hasConflict(event);
  }
}