package calendar.controller;

import calendar.model.EditScope;
import calendar.model.Event;
import calendar.view.GuiView;
import calendar.view.ViewMode;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The contract between the View and the Controller.
 * This interface lists every action a user can take in the GUI that requires
 * the application to do some work or update the data model.
 */
public interface ControllerFeatures {

  /**
   * Tells the controller to create a brand new calendar with the given settings.
   * If it works, the app will automatically switch to using this new calendar.
   *
   * @param name   The name the user typed in.
   * @param zoneId The timezone selected by the user.
   * @throws Exception If the name is already taken or invalid.
   */
  void createCalendar(String name, ZoneId zoneId) throws Exception;

  /**
   * Updates the details of the currently active calendar.
   * Note: Changing the timezone here will shift all existing events to match the new zone.
   *
   * @param newName   The new name to assign (can be the same as the old one).
   * @param newZoneId The new timezone setting.
   * @throws Exception If the new name conflicts with another existing calendar.
   */
  void editCalendar(String newName, ZoneId newZoneId) throws Exception;

  /**
   * Permanently removes a calendar and all its events.
   *
   * @param name The name of the calendar to delete.
   * @throws Exception If the calendar cannot be found.
   */
  void deleteCalendar(String name) throws Exception;

  /**
   * Changes which calendar the application is currently looking at.
   * The view will refresh immediately to show the events from this calendar.
   *
   * @param name The name of the calendar to load.
   * @throws Exception If something goes wrong while loading.
   */
  void switchCalendar(String name) throws Exception;

  /**
   * Moves the calendar view forward or backward in time.
   * The actual jump depends on the current view mode (e.g., next month vs. next week).
   *
   * @param amount Use -1 to go back and +1 to go forward.
   */
  void changeDateRange(int amount);

  /**
   * Swaps the main display between Month, Week, and Day views.
   *
   * @param mode The new view mode the user selected.
   */
  void setViewMode(ViewMode mode);

  /**
   * Updates the system's "focus" date.
   * This is usually called when a user clicks a specific day cell in the Month view,
   * preparing the app to show detailed events for that specific date.
   *
   * @param date The date the user clicked on.
   */
  void selectDate(LocalDate date);

  /**
   * Takes the data from the "Create Event" form and saves it.
   * If the user filled out the recurrence fields, this will generate the whole series of events.
   *
   * @param eventBuilder The basic event details (subject, time, etc.).
   * @param repeatDays   A string like "MWF" for repeating days, or empty for a single event.
   * @param repeatType   Either "Count" (for N times) or "Until" (for a specific end date).
   * @param repeatValue  The number of times or the date string to stop repeating.
   * @throws Exception If the data is invalid (e.g., end time before start time).
   */
  void createEvent(Event.EventBuilder eventBuilder, String repeatDays, String repeatType,
                   String repeatValue) throws Exception;

  /**
   * Saves changes to an existing event.
   * This handles complex logic like splitting a recurring series or updating just one instance.
   *
   * @param originalEvent The event as it existed before editing.
   * @param newBuilder    The new details the user entered.
   * @param scope         Whether to apply changes to just this event, future ones, or all of them.
   * @throws Exception If the edit causes a conflict or validation error.
   */
  void editEvent(Event originalEvent, Event.EventBuilder newBuilder, EditScope scope)
      throws Exception;

  /**
   * Dumps the current calendar's events into a file on the user's computer.
   *
   * @param file   The destination file selected by the user.
   * @param format The format to write (e.g., "CSV" or "iCal").
   */
  void exportCalendar(File file, String format);

  /**
   * A helper check to see if an event belongs to a repeating series.
   * The view uses this to decide whether to ask the "Edit Series?" question.
   *
   * @param event The event to check.
   * @return true if this event is linked to others via a series ID.
   */
  boolean isEventPartofSeries(Event event);

  /**
   * Looks up the timezone for a specific calendar.
   * The view needs this to pre-fill the "Edit Calendar" dialog with the current setting.
   *
   * @param calendarName The name of the calendar.
   * @return The ZoneId associated with that calendar.
   */
  ZoneId getZoneId(String calendarName);

  /**
   * Connects the Swing window (View) to this controller.
   * Once connected, we immediately trigger a refresh so the window isn't empty.
   *
   * @param view The main GUI window interface.
   */
  void setView(GuiView view);
}