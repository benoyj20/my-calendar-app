package calendar.view;

import calendar.model.Event;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * The main contract for the Graphical User Interface.
 * This interface lists all the things the GUI window needs to be able to do like
 * updating the calendar grid, showing a list of events, or popping up an error message.
 */
public interface GuiView {

  /**
   * Updates the dropdown menu with the list of available calendars.
   * It also makes sure the currently active calendar is selected in the list.
   *
   * @param calendarNames  A set of all calendar names to show in the dropdown.
   * @param activeCalendar The name of the calendar that should be currently selected.
   */
  void setCalendarList(Set<String> calendarNames, String activeCalendar);

  /**
   * Refreshes the Month View grid.
   * The view should clear whatever month it's showing and redraw the grid for
   * the specified {@code currentMonth}.
   *
   * @param currentMonth The month to display.
   * @param monthEvents  The list of events falling within this month to draw on the grid.
   */
  void updateMonthView(LocalDate currentMonth, List<Event> monthEvents);

  /**
   * Refreshes the Week View columns.
   * The view should fill each day's column with the events that happen on that day.
   *
   * @param startOfWeek The date of the first day of the week to display.
   * @param weekEvents  The list of events falling within this week.
   */
  void updateWeekView(LocalDate startOfWeek, List<Event> weekEvents);

  /**
   * Refreshes the Day View list.
   * The view should show a detailed list of all events for the specific {@code date},
   * including times, descriptions, and other details..
   *
   * @param date      The specific date being viewed.
   * @param dayEvents The list of events scheduled for this day.
   */
  void updateDayView(LocalDate date, List<Event> dayEvents);

  /**
   * Switches the main display area to show the requested view mode.
   * This is called when the user clicks one of the view toggle buttons.
   *
   * @param mode The view mode to switch to.
   */
  void setViewMode(ViewMode mode);

  /**
   * Pops up a modal dialog to alert the user about an error.
   *
   * @param message The error text to display.
   */
  void showError(String message);

  /**
   * Pops up a modal dialog to give the user information.
   *
   * @param message The info text to display.
   */
  void showMessage(String message);

  /**
   * Controls whether the main window is visible on screen.
   *
   * @param visible True to show the window, false to hide it.
   */
  void setVisible(boolean visible);
}