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
 * Tests the {@link ConsoleView} class by capturing and verifying
 * System.out and System.err output.
 */
public class ConsoleViewTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  private ConsoleView view;
  private Event event1;

  /**
   * Redirects System.out and System.err to an in-memory stream before each test.
   *
   * @throws ValidationException if event creation fails
   */
  @Before
  public void setUp() throws ValidationException {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
    view = new ConsoleView();

    event1 = Event.builder()
        .setSubject("Test Event")
        .setStart(LocalDateTime.of(2025, 11, 20, 14, 30))
        .setEnd(LocalDateTime.of(2025, 11, 20, 15, 0))
        .setLocation("Here")
        .build();
  }

  /**
   * Restores the original System.out and System.err streams after each test.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  /**
   * Tests the showMessage method.
   */
  @Test
  public void testShowMessage() {
    view.showMessage("Hello World");
    assertEquals("Hello World" + System.lineSeparator(), outContent.toString());
  }

  /**
   * Tests the showError method.
   */
  @Test
  public void testShowError() {
    view.showError("Oops");
    assertEquals("ERROR: Oops" + System.lineSeparator(), errContent.toString());
  }

  /**
   * Tests the showPrompt method.
   */
  @Test
  public void testShowPrompt() {
    view.showPrompt();
    assertEquals("> ", outContent.toString());
  }

  /**
   * Tests printEventsOnDate with a non-empty list.
   */
  @Test
  public void testPrintEventsOnDate() {
    view.printEventsOnDate(List.of(event1));
    String output = outContent.toString();
    assertTrue(output.contains("* Test Event from 02:30 PM to 03:00 PM at Here"));
  }

  /**
   * Tests printEventsOnDate with an empty list.
   */
  @Test
  public void testPrintEventsOnDateEmpty() {
    view.printEventsOnDate(List.of());
    String output = outContent.toString();
    assertTrue(output.contains("No events scheduled."));
  }

  /**
   * Tests printEventsInRange with a non-empty list.
   */
  @Test
  public void testPrintEventsInRange() {
    view.printEventsInRange(List.of(event1));
    String output = outContent.toString();
    assertTrue(output.contains(
        "* Test Event starting on 2025-11-20 at 02:30 PM, ending on 2025-11-20 at 03:00 PM at Here"
    ));
  }

  /**
   * Tests printEventsInRange with an empty list.
   */
  @Test
  public void testPrintEventsInRangeEmpty() {
    view.printEventsInRange(List.of());
    String output = outContent.toString();
    assertTrue(output.contains("No events found in this range."));
  }
}