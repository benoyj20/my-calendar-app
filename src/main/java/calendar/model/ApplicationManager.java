package calendar.model;

import java.time.ZoneId;
import java.util.Set;

/**
 * Top-level model for managing multiple calendars.
 * The controller talks to this interface to create, edit, and look up calendars.
 */
public interface ApplicationManager {

  /**
   * Creates a new, empty calendar with a unique name and timezone.
   *
   * @param name   the unique name for the calendar
   * @param zoneId the timezone for the calendar
   * @throws ValidationException if the name is not unique or timezone is invalid
   */
  void createCalendar(String name, ZoneId zoneId) throws ValidationException;

  /**
   * Edits the name of an existing calendar.
   *
   * @param oldName the current name of the calendar
   * @param newName the new unique name
   * @throws ValidationException if oldName is not found or newName is not unique
   */
  void editCalendarName(String oldName, String newName) throws ValidationException;

  /**
   * Edits the timezone of an existing calendar.
   *
   * @param calendarName the name of the calendar to edit
   * @param newZoneId    the new timezone
   * @throws ValidationException if the calendar is not found
   */
  void editCalendarTimezone(String calendarName, ZoneId newZoneId) throws ValidationException;

  /**
   * Sets which calendar is currently "active" for later commands
   * (like creating or printing events).
   *
   * @param name the name of the calendar to set as active
   * @throws ValidationException if no calendar with that name exists
   */
  void setActiveCalendar(String name) throws ValidationException;

  /**
   * Returns the currently active calendar.
   *
   * @return the active Calendar object
   * @throws ValidationException if no calendar is currently active
   */
  Calendar getActiveCalendar() throws ValidationException;

  /**
   * Looks up a calendar by name.
   *
   * @param name the name of the calendar to retrieve
   * @return the Calendar object
   * @throws ValidationException if no calendar with that name exists
   */
  Calendar getCalendar(String name) throws ValidationException;

  /**
   * Returns a set of all calendar names.
   *
   * @return set of calendar names
   */
  Set<String> getCalendarNames();

  /**
   * Deletes a calendar.
   */
  void deleteCalendar(String name) throws ValidationException;
}