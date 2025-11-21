package calendar.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for an event. Takes care of start time, end time
 * location and other details of an event.
 */
public interface Ievent {

  /**
   * Returns the event subject.
   *
   * @return event subject
   */
  String getSubject();

  /**
   * Returns the start date/time.
   *
   * @return start date/time
   */
  LocalDateTime getStart();

  /**
   * Returns the end date/time.
   *
   * @return end date/time
   */
  LocalDateTime getEnd();

  /**
   * Returns the description, never {@code null}.
   *
   * @return description
   */
  String getDescription();

  /**
   * Returns the location, never {@code null}.
   *
   * @return location
   */
  String getLocation();

  /**
   * Indicates whether the event is private.
   *
   * @return {@code true} if private
   */
  boolean isPrivate();

  /**
   * Indicates whether the event spans all day.
   *
   * @return {@code true} if all-day
   */
  boolean isAllDay();

  /**
   * Returns the series identifier.
   *
   * @return series id (non-null)
   */
  UUID getSeriesId();

  /**
   * Compares events by subject, start, and end.
   *
   * @param o other object
   * @return equality by (subject, start, end)
   */
  @Override
  boolean equals(final Object o);

  /**
   * Returns a hash code based on subject, start, and end.
   *
   * @return hash code
   */
  @Override
  int hashCode();

  /**
   * Returns a string representation of the event.
   *
   * @return string form
   */
  @Override
  String toString();
}