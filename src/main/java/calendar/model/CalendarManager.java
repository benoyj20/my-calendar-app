package calendar.model;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Model interface that manages all event insertion, conflict and removal logic.
 */
public interface CalendarManager {

  /**
   * Adds a single event to the calendar.
   * If an event with the same subject, start, and end exists, this is a conflict.
   *
   * @param event event to add
   * @throws ValidationException if the event creates a conflict or fails validation.
   */
  void addEvent(Event event) throws ValidationException;

  /**
   * Adds a collection of events to the calendar.
   *
   * @param events events to add
   * @throws ValidationException if any event creates a conflict or fails validation
   */
  void addEvents(Collection<Event> events) throws ValidationException;

  /**
   * Removes a single event.
   *
   * @param event event to remove
   */
  void removeEvent(Event event);

  /**
   * Removes a collection of events.
   *
   * @param events events to remove
   */
  void removeEvents(Collection<Event> events);

  /**
   * Finds events that match a predicate filter.
   *
   * @param filter predicate applied to each event
   * @return matching events
   */
  List<Event> findEvents(Predicate<Event> filter);

  /**
   * Checks whether the provided event would conflict with an existing one.
   * Conflicts are found if all (subject, start, end) are same.
   *
   * @param event event to test
   * @return {@code true} if a conflict exists; {@code false} otherwise
   */
  boolean hasConflict(Event event);
}