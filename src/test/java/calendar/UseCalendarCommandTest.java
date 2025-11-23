package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import calendar.controller.commands.UseCalendarCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.ValidationException;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link UseCalendarCommand} class to ensure it
 * correctly sets the active calendar and handles all error cases.
 */
public class UseCalendarCommandTest {

  private ApplicationManager model;
  private TestView view;


  /**
   * Sets up a fresh model and view, and create a
   * calendar that we can try to "use".
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();
    model.createCalendar("Work", ZoneId.of("UTC"));
  }

  /**
   * Tests the standard success path where the calendar
   * exists and is correctly set as active.
   */
  @Test
  public void testUseCalendarSuccess() throws Exception {
    List<String> tokens = List.of("use", "calendar", "--name", "Work");
    new UseCalendarCommand(tokens).execute(model, view);

    assertEquals("Now using calendar 'Work'.", view.getLastMessage());
    assertEquals("Work", model.getActiveCalendar().getName());
  }

  /**
   * Ensures the command fails if the specified calendar
   * name does not exist in the model.
   */
  @Test
  public void testUseCalendarNotFound() throws Exception {
    List<String> tokens = List.of("use", "calendar", "--name", "Missing");
    try {
      new UseCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Missing' not found.", e.getMessage());
    }
  }

  /**
   * Checks that the command fails when too few arguments
   * are provided (e.g., missing the calendar name).
   */
  @Test(expected = ValidationException.class)
  public void testUseCalendarSyntaxErrorShort() throws Exception {
    List<String> tokens = List.of("use", "calendar", "--name");
    new UseCalendarCommand(tokens).execute(model, view);
  }

  /**
   * Checks that the command fails if the '--name' keyword
   * is missing or incorrect.
   */
  @Test(expected = ValidationException.class)
  public void testUseCalendarSyntaxErrorBadKeyword() throws Exception {
    List<String> tokens = List.of("use", "calendar", "name", "Work");
    new UseCalendarCommand(tokens).execute(model, view);
  }
}