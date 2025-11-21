package calendar.model;

/**
 * Custom exception for validation failures.
 */
public class ValidationException extends Exception {

  /**
   * Constructs a new validation exception with a message.
   *
   * @param message explanation of the error.
   */
  public ValidationException(final String message) {
    super(message);
  }
}