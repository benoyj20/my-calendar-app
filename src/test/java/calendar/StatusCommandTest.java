package calendar;

import static org.junit.Assert.assertEquals;

import calendar.controller.commands.StatusCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the {@link StatusCommand} class to verify availability logic.
 */
public class StatusCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Sets up a model with one event from 10:00 to 11:00 in an active calendar.
   *
   * @throws ValidationException if event creation fails
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("TestCal", ZoneId.of("UTC"));
    model.setActiveCalendar("TestCal");

    Event event = Event.builder()
        .setSubject("Meeting")
        .setStart(LocalDateTime.of(2025, 11, 20, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 11, 0))
        .build();
    model.getActiveCalendar().addEvent(event);
  }

  /**
   * Tests that status is "Available" before and exactly at the end time.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testStatusAvailable() throws Exception {
    List<String> tokens = List.of("show", "status", "on", "2025-11-20T09:59");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Available", view.getLastMessage());

    tokens = List.of("show", "status", "on", "2025-11-20T11:00");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Available", view.getLastMessage());
  }

  /**
   * Tests that status is "Busy" at start time and during the event.
   *
   * @throws Exception if execute fails
   */
  @Test
  public void testStatusBusy() throws Exception {
    List<String> tokens = List.of("show", "status", "on", "2025-11-20T10:00");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Busy", view.getLastMessage());

    tokens = List.of("show", "status", "on", "2025-11-20T10:30");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Busy", view.getLastMessage());
  }

  /**
   * Tests syntax error for too few tokens.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testStatusSyntaxErrorShort() throws Exception {
    List<String> tokens = List.of("show", "status", "on");
    new StatusCommand(tokens).execute(model, view);
  }

  /**
   * Tests syntax error for wrong keyword.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testStatusSyntaxErrorBadWord() throws Exception {
    List<String> tokens = List.of("show", "status", "at", "2025-11-20T10:00");
    new StatusCommand(tokens).execute(model, view);
  }

  /**
   * Tests validation error for unparseable date-time.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testStatusSyntaxErrorBadDate() throws Exception {
    List<String> tokens = List.of("show", "status", "on", "2025-99-99T10:00");
    new StatusCommand(tokens).execute(model, view);
  }

  /**
   * Tests that a command fails if no calendar is active.
   *
   * @throws Exception if execute fails
   */
  @Test(expected = ValidationException.class)
  public void testErrorNoActiveCalendar() throws Exception {

    ApplicationManager freshModel = new ApplicationManagerImpl();
    freshModel.createCalendar("TestCal", ZoneId.of("UTC"));

    List<String> tokens = List.of("show", "status", "on", "2025-11-20T10:00");

    new StatusCommand(tokens).execute(freshModel, view);
  }
}