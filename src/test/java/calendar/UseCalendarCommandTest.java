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
 * Verifies that the UseCalendarCommand correctly switches the active calendar
 * within the application, handling both successful switches and errors for missing calendars.
 */
public class UseCalendarCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Prepares a model with a "Personal" calendar before each test.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();
    model.createCalendar("Personal", ZoneId.of("UTC"));
  }

  @Test(expected = ValidationException.class)
  public void testFailsWhenCommandIsIncomplete() throws Exception {
    List<String> tokens = List.of("use", "calendar", "--name");
    new UseCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanSwitchToExistingCalendar() throws Exception {
    List<String> tokens = List.of("use", "calendar", "--name", "Personal");
    new UseCalendarCommand(tokens).execute(model, view);

    assertEquals("Now using calendar 'Personal'.", view.getLastMessage());
    assertEquals("Personal", model.getActiveCalendar().getName());
  }

  @Test(expected = ValidationException.class)
  public void testFailsWhenNameFlagIsMissing() throws Exception {
    List<String> tokens = List.of("use", "calendar", "name", "Personal");
    new UseCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testFailsWhenCalendarDoesNotExist() throws Exception {
    List<String> tokens = List.of("use", "calendar", "--name", "Secret Project");
    try {
      new UseCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Secret Project' not found.", e.getMessage());
    }
  }
}