package calendar.view;

import calendar.model.Event;
import java.util.List;

/**
 * View interface responsible for presenting output to the user.
 * The view can implemented by different types like console, GUI.
 */
public interface CalendarView {

  /**
   * Shows a normal message to the user.
   *
   * @param message message text
   */
  void showMessage(String message);

  /**
   * Shows an error message to the user.
   *
   * @param error error text
   */
  void showError(String error);

  /**
   * Show the interactive prompt.
   */
  void showPrompt();

  /**
   * Print events formatted for the print events on DATE command.
   *
   * @param events events that occur on the requested date
   */
  void printEventsOnDate(List<Event> events);

  /**
   * Print events formatted for the print events from start to end command.
   *
   * @param events events whose times fall within start to end.
   */
  void printEventsInRange(List<Event> events);
}