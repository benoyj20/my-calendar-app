package calendar.model;

/**
 * Finds the scope of an edit operation on an event series.
 */
public enum EditScope {
  /**
   * Modify only this single event instance.
   */
  SINGLE,

  /**
   * Modify this event and all future events in the series.
   */
  FUTURE,

  /**
   * Modify all events in the entire series (past, present, and future).
   */
  ALL
}