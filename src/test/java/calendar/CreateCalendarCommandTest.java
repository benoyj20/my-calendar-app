package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import calendar.controller.commands.CreateCalendarCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.ValidationException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link CreateCalendarCommand} class to ensure it
 * correctly parses tokens, creates new calendars, and handles
 * errors like duplicate names or invalid timezones.
 */
public class CreateCalendarCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Sets up a fresh model and view before each test.
   */
  @Before
  public void setUp() {
    model = new ApplicationManagerImpl();
    view = new TestView();
  }

  /**
   * Tests the standard "happy path" for creating a new calendar.
   * We check that the model is updated and the user gets a success message.
   */
  @Test
  public void testCreateCalendarSuccess() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Work",
        "--timezone", "America/New_York");
    new CreateCalendarCommand(tokens).execute(model, view);

    assertEquals("Calendar 'Work' created successfully.", view.getLastMessage());
    assertNotNull(model.getCalendar("Work"));
    assertEquals(ZoneId.of("America/New_York"), model.getCalendar("Work").getZoneId());
  }

  /**
   * Verifies that the command fails if a calendar with the
   * same name already exists in the model.
   */
  @Test
  public void testCreateCalendarNameConflict() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Test",
        "--timezone", "UTC");

    new CreateCalendarCommand(tokens).execute(model, view);

    try {
      new CreateCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for duplicate calendar name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Test' already exists.", e.getMessage());
    }
  }

  /**
   * Ensures the command fails if the user provides a string
   * that isn't a valid IANA timezone ID.
   */
  @Test
  public void testCreateCalendarBadTimezone() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Test",
        "--timezone", "Mars/Gale_Crater");
    try {
      new CreateCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for bad timezone string.");
    } catch (ValidationException e) {
      assertEquals("Invalid timezone format: Mars/Gale_Crater", e.getMessage());
    } catch (DateTimeException e) {
      fail("Command threw the wrong exception. Expected ValidationException, got " + e);
    }
  }

  /**
   * Checks that the command fails gracefully if not enough
   * arguments are provided.
   */
  @Test(expected = ValidationException.class)
  public void testCreateCalendarBadSyntaxShort() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Test");
    new CreateCalendarCommand(tokens).execute(model, view);
  }

  /**
   * Checks that the command fails if the keywords
   * '--name' and '--timezone' are missing.
   */
  @Test(expected = ValidationException.class)
  public void testCreateCalendarBadSyntaxKeywords() throws Exception {
    List<String> tokens = List.of("create", "calendar", "name", "Test",
        "timezone", "UTC");
    new CreateCalendarCommand(tokens).execute(model, view);
  }

  /**
   * Checks that the command fails if the '--timezone'
   * keyword is misspelled (e.g., --zone).
   */
  @Test(expected = ValidationException.class)
  public void testCreateCalendarBadTimezoneKeyword() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Test",
        "--zone", "UTC");
    new CreateCalendarCommand(tokens).execute(model, view);
  }
}