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
 * Tests the {@link EditCalendarCommand} class.
 * Validates changing calendar name and timezone, and handles
 * errors like conflicts and unknown properties.
 */
public class EditCalendarCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Sets up a model with two calendars and activates one.
   *
   * @throws ValidationException if setup fails
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("Work", ZoneId.of("America/New_York"));
    model.createCalendar("Home", ZoneId.of("America/Los_Angeles"));
    model.setActiveCalendar("Work");
  }

  /**
   * Checks the "happy path" for renaming a calendar.
   */
  @Test
  public void testEditCalendarNameSuccess() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "name", "Work-Renamed");
    new EditCalendarCommand(tokens).execute(model, view);

    assertEquals("Calendar 'Work' renamed to 'Work-Renamed'.", view.getLastMessage());

    assertNotNull(model.getCalendar("Work-Renamed"));
    try {
      model.getCalendar("Work");
      fail("Old calendar name 'Work' should no longer exist.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Work' not found.", e.getMessage());
    }
  }

  /**
   * Checks the "happy path" for changing a calendar's timezone.
   */
  @Test
  public void testEditCalendarTimezoneSuccess() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "timezone", "Europe/Paris");
    new EditCalendarCommand(tokens).execute(model, view);

    assertEquals("Calendar 'Work' timezone updated to 'Europe/Paris'.", view.getLastMessage());
    assertEquals(ZoneId.of("Europe/Paris"), model.getCalendar("Work").getZoneId());
  }

  /**
   * Verifies that renaming a calendar to an already existing name fails.
   */
  @Test
  public void testEditCalendarNameConflict() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "name", "Home");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for duplicate name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Home' already exists.", e.getMessage());
    }
  }

  /**
   * Ensures that trying to edit a calendar that doesn't exist throws an error.
   */
  @Test
  public void testEditCalendarNotFound() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Missing",
        "--property", "name", "New");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Missing' not found.", e.getMessage());
    }
  }

  /**
   * Makes sure the command fails if the user tries to edit an invalid property like 'owner'.
   */
  @Test
  public void testEditUnknownProperty() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "owner", "Me");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for unknown property.");
    } catch (ValidationException e) {
      assertEquals("Unknown property 'owner'. Can only edit 'name' or 'timezone'.",
          e.getMessage());
    }
  }

  /**
   * Ensures that providing a garbage timezone string (not a valid IANA ID) fails.
   */
  @Test
  public void testEditInvalidTimezoneValue() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "timezone", "Mars/Gale_Crater");
    try {
      new EditCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for invalid timezone.");
    } catch (ValidationException e) {
      assertEquals("Invalid timezone format: Mars/Gale_Crater", e.getMessage());
    } catch (DateTimeException e) {
      fail("Command threw the wrong exception. Expected ValidationException, got " + e);
    }
  }

  /**
   * Tests the command's syntax validation for a command that is too short.
   */
  @Test(expected = ValidationException.class)
  public void testEditSyntaxErrorShort() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  /**
   * Tests the syntax validation for a missing or incorrect '--name' keyword.
   */
  @Test(expected = ValidationException.class)
  public void testEditSyntaxErrorBadNameKeyword() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "name", "Work",
        "--property", "name", "New");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  /**
   * Tests the syntax validation for a missing or incorrect '--property' keyword.
   */
  @Test(expected = ValidationException.class)
  public void testEditSyntaxErrorBadPropertyKeyword() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "property", "name", "New");
    new EditCalendarCommand(tokens).execute(model, view);
  }

  /**
   * Checks the important edge case of renaming the *active* calendar
   * to ensure the model's state remains valid.
   */
  @Test
  public void testEditActiveCalendarName() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "name", "Work-Renamed");
    new EditCalendarCommand(tokens).execute(model, view);

    assertEquals("Work-Renamed", model.getActiveCalendar().getName());
  }

  /**
   * Tests the command's syntax validation for a command that has too many tokens.
   */
  @Test(expected = ValidationException.class)
  public void testEditSyntaxErrorLong() throws Exception {
    List<String> tokens = List.of("edit", "calendar", "--name", "Work",
        "--property", "name", "NewName", "extra-token");
    new EditCalendarCommand(tokens).execute(model, view);
  }
}