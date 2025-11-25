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
 * Checks that the StatusCommand correctly reports a user's availability (Busy/Available)
 * at a specific point in time, handling date parsing and command syntax errors.
 */
public class StatusCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Sets up a calendar with a scheduled "Doctor Appointment" from 10:00 to 11:00
   * to test busy/available logic.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("Personal", ZoneId.of("UTC"));
    model.setActiveCalendar("Personal");

    Event event = Event.builder()
        .setSubject("Doctor Appointment")
        .setStart(LocalDateTime.of(2025, 11, 20, 10, 0))
        .setEnd(LocalDateTime.of(2025, 11, 20, 11, 0))
        .build();
    model.getActiveCalendar().addEvent(event);
  }

  @Test(expected = ValidationException.class)
  public void testRejectsInvalidDateFormat() throws Exception {
    List<String> tokens = List.of("show", "status", "on", "2025-99-99T10:00");
    new StatusCommand(tokens).execute(model, view);
  }

  @Test
  public void testReportsBusyDuringEvent() throws Exception {
    List<String> tokens = List.of("show", "status", "on", "2025-11-20T10:00");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Busy", view.getLastMessage());

    tokens = List.of("show", "status", "on", "2025-11-20T10:30");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Busy", view.getLastMessage());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsShortCommand() throws Exception {
    List<String> tokens = List.of("show", "status", "on");
    new StatusCommand(tokens).execute(model, view);
  }

  @Test
  public void testReportsAvailableOutsideEvent() throws Exception {
    List<String> tokens = List.of("show", "status", "on", "2025-11-20T09:59");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Available", view.getLastMessage());

    tokens = List.of("show", "status", "on", "2025-11-20T11:00");
    new StatusCommand(tokens).execute(model, view);
    assertEquals("Available", view.getLastMessage());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsWrongKeyword() throws Exception {
    List<String> tokens = List.of("show", "status", "at", "2025-11-20T10:00");
    new StatusCommand(tokens).execute(model, view);
  }
}