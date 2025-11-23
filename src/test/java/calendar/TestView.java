package calendar;

import calendar.model.Event;
import calendar.view.CalendarView;
import java.util.ArrayList;
import java.util.List;

/**
 * A stub implementation of CalendarView for testing.
 *
 * <p>It captures all outputs in fields instead of printing to the console,
 * allowing tests to assert the results.
 */
public class TestView implements CalendarView {

  private String lastMessage;
  private String lastError;
  private List<Event> printedEvents;
  private String printMode;

  /**
   * Captures a normal message.
   *
   * @param message message text
   */
  @Override
  public void showMessage(String message) {
    this.lastMessage = message;
  }

  /**
   * Captures an error message.
   *
   * @param error error text
   */
  @Override
  public void showError(String error) {
    this.lastError = error;
  }

  /**
   * No-op for tests.
   */
  @Override
  public void showPrompt() {
    // No-op for tests
  }

  /**
   * Captures events printed for a specific date.
   *
   * @param events events that occur on the requested date
   */
  @Override
  public void printEventsOnDate(List<Event> events) {
    this.printMode = "onDate";
    this.printedEvents = new ArrayList<>(events);
    if (events.isEmpty()) {
      showMessage("No events scheduled.");
    }
  }

  /**
   * Captures events printed for a time range.
   *
   * @param events events whose times fall within start to end.
   */
  @Override
  public void printEventsInRange(List<Event> events) {
    this.printMode = "inRange";
    this.printedEvents = new ArrayList<>(events);
    if (events.isEmpty()) {
      showMessage("No events found in this range.");
    }
  }

  /**
   * Gets the last normal message that was captured.
   *
   * @return the last message, or null if none
   */
  public String getLastMessage() {
    return lastMessage;
  }

  /**
   * Gets the last error message that was captured.
   *
   * @return the last error, or null if none
   */
  public String getLastError() {
    return lastError;
  }

  /**
   * Gets the list of events that were "printed".
   *
   * @return the list of printed events, or null if none
   */
  public List<Event> getPrintedEvents() {
    return printedEvents;
  }

  /**
   * Gets the print mode used ("onDate" or "inRange").
   *
   * @return the print mode, or null if none
   */
  public String getPrintMode() {
    return printMode;
  }

  /**
   * Clears all captured values.
   */
  public void clear() {
    lastMessage = null;
    lastError = null;
    printedEvents = null;
    printMode = null;
  }
}

