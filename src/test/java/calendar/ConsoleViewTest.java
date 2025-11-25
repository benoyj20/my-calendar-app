package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.ConsoleView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks that the ConsoleView correctly writes messages, errors, and event details
 * to the standard output and error streams.
 */
public class ConsoleViewTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  private ConsoleView view;
  private Event sampleEvent;

  /**
   * Redirects System.out and System.err to capture output for verification,
   * and initializes a sample event for testing.
   *
   * @throws ValidationException if event creation fails
   */
  @Before
  public void setUp() throws ValidationException {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
    view = new ConsoleView();

    sampleEvent = Event.builder()
        .setSubject("Weekly Sync")
        .setStart(LocalDateTime.of(2025, 11, 20, 14, 30))
        .setEnd(LocalDateTime.of(2025, 11, 20, 15, 0))
        .setLocation("Conference Room A")
        .build();
  }

  /**
   * Restores streams.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testDisplayingErrorMessage() {
    view.showError("Something went wrong");
    assertEquals("ERROR: Something went wrong" + System.lineSeparator(), errContent.toString());
  }

  @Test
  public void testPrintingEventsOnSpecificDate() {
    view.printEventsOnDate(List.of(sampleEvent));
    String output = outContent.toString();
    assertTrue(output.contains("* Weekly Sync from 02:30 PM to 03:00 PM at Conference Room A"));
  }

  @Test
  public void testPrintingEmptyDateRangeList() {
    view.printEventsInRange(List.of());
    String output = outContent.toString();
    assertTrue(output.contains("No events found in this range."));
  }

  @Test
  public void testDisplayingUserPrompt() {
    view.showPrompt();
    assertEquals("> ", outContent.toString());
  }

  @Test
  public void testPrintingEventsWithinRange() {
    view.printEventsInRange(List.of(sampleEvent));
    String output = outContent.toString();
    assertTrue(output.contains(
        "* Weekly Sync starting on 2025-11-20 at 02:30 PM, ending on 2025-11-20 at 03:00 PM "
            + "at Conference Room A"
    ));
  }

  @Test
  public void testDisplayingStandardMessage() {
    view.showMessage("Welcome to Calendar");
    assertEquals("Welcome to Calendar" + System.lineSeparator(), outContent.toString());
  }

  @Test
  public void testPrintingEmptyDateList() {
    view.printEventsOnDate(List.of());
    String output = outContent.toString();
    assertTrue(output.contains("No events scheduled."));
  }
}