package calendar.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implements CalendarManager interface for managing events and their conflicts.
 * Uses hashSet for storage and conflict detection.
 */
public class CalendarManagerImpl implements CalendarManager {

  private final Set<Event> events = new HashSet<>();

  @Override
  public void addEvent(final Event event) throws ValidationException {
    if (!events.add(event)) {
      throw new ValidationException(
          "An event with the same subject, start, and end time already exists.");
    }
  }

  @Override
  public void addEvents(final Collection<Event> newEvents) throws ValidationException {
    for (Event e : newEvents) {
      if (events.contains(e)) {
        throw new ValidationException(
            "One or more events in the series create a conflict: " + e.getSubject());
      }
    }
    events.addAll(newEvents);
  }

  @Override
  public void removeEvent(final Event event) {
    events.remove(event);
  }

  @Override
  public void removeEvents(final Collection<Event> eventsToRemove) {
    events.removeAll(eventsToRemove);
  }

  @Override
  public List<Event> findEvents(final Predicate<Event> filter) {
    return events.stream()
        .filter(filter)
        .sorted(java.util.Comparator.comparing(Event::getStart))
        .collect(Collectors.toList());
  }

  @Override
  public boolean hasConflict(final Event event) {
    return events.contains(event);
  }
}