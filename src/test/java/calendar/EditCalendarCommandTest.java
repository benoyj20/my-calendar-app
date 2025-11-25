package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import calendar.controller.commands.EditCalendarCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.ValidationException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the EditCalendarCommand properly handles requests to rename calendars
 * or change their timezones, including error handling for conflicts and bad input.
 */
public class EditCalendarCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Prepares the model with two calendars ("Personal" and "Work") and activates one before testing.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("Personal", ZoneId.of("America/New_York"));
    model.createCalendar("Work", ZoneId.of("America/Los_Angeles"));
    model.setActiveCalendar("Personal");
  }

  @Test(expected = ValidationException.class)
  public void testRejectsMissingNameFlag() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "name", "Personal",
        "--property", "name", "NewName");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanChangeTimezone() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "timezone", "Europe/Paris");
    new EditCalendarCommand(tokens).execute(model, view);

    assertEquals("Calendar 'Personal' timezone updated to 'Europe/Paris'.", view.getLastMessage());
    assertEquals(ZoneId.of("Europe/Paris"), model.getCalendar("Personal").getZoneId());
  }

  @Test(expected = ValidationException.class)
  public void testRejectsCommandWithTooManyTokens() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "name", "NewName", "extra-token");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testFailsWhenRenamingToExistingName() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "name", "Work");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for duplicate name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Work' already exists.", e.getMessage());
    }
  }

  @Test
  public void testFailsForUnknownProperty() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "color", "Blue");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for unknown property.");
    } catch (ValidationException e) {
      assertEquals("Unknown property 'color'. Can only edit 'name' or 'timezone'.",
          e.getMessage());
    }
  }

  @Test
  public void testCanRenameActiveCalendar() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "name", "Personal (Archived)");
    new EditCalendarCommand(tokens).execute(model, view);

    assertEquals("Personal (Archived)", model.getActiveCalendar().getName());
  }

  @Test
  public void testCanRenameCalendar() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "name", "My Life");
    new EditCalendarCommand(tokens).execute(model, view);

    assertEquals("Calendar 'Personal' renamed to 'My Life'.", view.getLastMessage());

    assertNotNull(model.getCalendar("My Life"));
    try {
      model.getCalendar("Personal");
      fail("Old calendar name 'Personal' should no longer exist.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Personal' not found.", e.getMessage());
    }
  }

  @Test(expected = ValidationException.class)
  public void testRejectsMissingPropertyFlag() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "property", "name", "NewName");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testFailsIfCalendarDoesNotExist() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Ghost",
        "--property", "name", "Real");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Ghost' not found.", e.getMessage());
    }
  }

  @Test(expected = ValidationException.class)
  public void testRejectsIncompleteCommand() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testFailsWithInvalidTimezone() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Personal",
        "--property", "timezone", "Mars/Olympus_Mons");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for invalid timezone.");
    } catch (ValidationException e) {
      assertEquals("Invalid timezone format: Mars/Olympus_Mons", e.getMessage());
    } catch (DateTimeException e) {
      fail("Command threw the wrong exception. Expected ValidationException, got " + e);
    }
  }
}