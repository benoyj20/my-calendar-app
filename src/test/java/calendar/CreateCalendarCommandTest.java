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
 * Verifies that the CreateCalendarCommand correctly interprets user input to create new calendars,
 * ensuring valid names and timezones are enforced while rejecting invalid commands.
 */
public class CreateCalendarCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Resets the model and view before each test.
   */
  @Before
  public void setUp() {
    model = new ApplicationManagerImpl();
    view = new TestView();
  }

  @Test
  public void testCommandFailsWithBadTimezoneFormat() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Conference",
        "--timezone", "Moon/Armstrong_Base");
    try {
      new CreateCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for bad timezone string.");
    } catch (ValidationException e) {
      assertEquals("Invalid timezone format: Moon/Armstrong_Base", e.getMessage());
    } catch (DateTimeException e) {
      fail("Command threw the wrong exception. Expected ValidationException, got " + e);
    }
  }

  @Test(expected = ValidationException.class)
  public void testCommandRejectsTypoInKeyword() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Errand List",
        "--zone", "UTC");
    new CreateCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanCreateNewCalendarSuccessfully() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Project Zeta",
        "--timezone", "America/Chicago");
    new CreateCalendarCommand(tokens).execute(model, view);

    assertEquals("Calendar 'Project Zeta' created successfully.", view.getLastMessage());
    assertNotNull(model.getCalendar("Project Zeta"));
    assertEquals(ZoneId.of("America/Chicago"), model.getCalendar("Project Zeta").getZoneId());
  }

  @Test(expected = ValidationException.class)
  public void testCommandRejectsMissingArguments() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Incomplete");
    new CreateCalendarCommand(tokens).execute(model, view);
  }

  @Test
  public void testCommandFailsIfCalendarExists() throws Exception {
    List<String> tokens = List.of("create", "calendar", "--name", "Duplicate",
        "--timezone", "UTC");

    new CreateCalendarCommand(tokens).execute(model, view);

    try {
      new CreateCalendarCommand(tokens).execute(model, view);
      fail("Expected ValidationException for duplicate calendar name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Duplicate' already exists.", e.getMessage());
    }
  }

  @Test(expected = ValidationException.class)
  public void testCommandRejectsMissingFlags() throws Exception {
    List<String> tokens = List.of("create", "calendar", "name", "Bad Syntax",
        "timezone", "UTC");
    new CreateCalendarCommand(tokens).execute(model, view);
  }
}