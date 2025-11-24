package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the core logic of the ApplicationManagerImpl class, which
 * is responsible for managing the collection of all calendars.
 */
public class ApplicationManagerImplTest {

  private ApplicationManager model;
  private final ZoneId zonePst = ZoneId.of("America/Los_Angeles");
  private final ZoneId zoneUtc = ZoneId.of("UTC");

  /**
   * Sets up the model and pre-populates it with two calendars,
   * "Work" and "Home", for testing.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    model.createCalendar("Work", zoneUtc);
    model.createCalendar("Home", zonePst);
  }

  /**
   * Makes sure we can add a new, valid calendar to the manager.
   */
  @Test
  public void testCreateCalendarSuccess() throws Exception {
    model.createCalendar("Personal", ZoneId.of("Europe/Paris"));
    Calendar cal = model.getCalendar("Personal");
    assertNotNull(cal);
    assertEquals("Personal", cal.getName());
    assertEquals(ZoneId.of("Europe/Paris"), cal.getZoneId());
  }

  /**
   * Checks that trying to add a calendar with a duplicate name fails.
   */
  @Test
  public void testCreateCalendarNameConflict() throws Exception {
    try {
      model.createCalendar("Work", zonePst);
      fail("Expected ValidationException for duplicate calendar name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Work' already exists.", e.getMessage());
    }
  }

  /**
   * Verifies that the underlying Objects.requireNonNull checks for null inputs.
   */
  @Test
  public void testCreateCalendarNullInputs() {
    try {
      model.createCalendar(null, zoneUtc);
      fail("Expected NullPointerException for null name.");
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }

    try {
      model.createCalendar("Test", null);
      fail("Expected NullPointerException for null zoneId.");
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

  /**
   * Tests that a calendar's name can be changed successfully.
   */
  @Test
  public void testEditCalendarNameSuccess() throws Exception {
    model.editCalendarName("Work", "Work-Office");
    assertNotNull(model.getCalendar("Work-Office"));

    try {
      model.getCalendar("Work");
      fail("Old calendar name 'Work' should no longer exist.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Work' not found.", e.getMessage());
    }
  }

  /**
   * Makes sure renaming a calendar to a name that already
   * exists is not allowed.
   */
  @Test
  public void testEditCalendarNameConflict() throws Exception {
    try {
      model.editCalendarName("Work", "Home");
      fail("Expected ValidationException for duplicate name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Home' already exists.", e.getMessage());
    }
  }

  /**
   * Checks that attempting to edit a calendar that doesn't
   * exist throws an error.
   */
  @Test
  public void testEditCalendarNameNotFound() throws Exception {
    try {
      model.editCalendarName("Missing", "NewName");
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Missing' not found.", e.getMessage());
    }
  }

  /**
   * Tests that a calendar's timezone can be changed successfully.
   */
  @Test
  public void testEditCalendarTimezoneSuccess() throws Exception {
    model.editCalendarTimezone("Home", ZoneId.of("Europe/Paris"));
    assertEquals(ZoneId.of("Europe/Paris"), model.getCalendar("Home").getZoneId());
  }

  /**
   * Checks that attempting to edit the timezone of a calendar
   * that doesn't exist throws an error.
   */
  @Test
  public void testEditCalendarTimezoneNotFound() throws Exception {
    try {
      model.editCalendarTimezone("Missing", zoneUtc);
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Missing' not found.", e.getMessage());
    }
  }

  /**
   * Verifies that we can set a calendar as active and then
   * retrieve it successfully.
   */
  @Test
  public void testSetAndGetActiveCalendar() throws Exception {
    model.setActiveCalendar("Home");
    Calendar activeCal = model.getActiveCalendar();
    assertEquals("Home", activeCal.getName());
    assertEquals(zonePst, activeCal.getZoneId());
  }

  /**
   * Checks that setting a non-existent calendar as active
   * throws an error.
   */
  @Test
  public void testSetActiveCalendarNotFound() throws Exception {
    try {
      model.setActiveCalendar("Missing");
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Missing' not found.", e.getMessage());
    }
  }

  /**
   * Verifies the edge case of renaming the currently active calendar.
   * The manager should update its internal pointer and
   * getActiveCalendar should return the renamed calendar.
   */
  @Test
  public void testEditNameOfActiveCalendar() throws Exception {
    model.setActiveCalendar("Work");
    assertEquals("Work", model.getActiveCalendar().getName());

    model.editCalendarName("Work", "Work-Renamed");

    assertEquals("Work-Renamed", model.getActiveCalendar().getName());
  }

  @Test
  public void testEditCalendarTimezoneSameZoneNoChange() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();
    mgr.createCalendar("Home", ZoneId.of("America/New_York"));
    mgr.setActiveCalendar("Home");
    Calendar cal = mgr.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.plusHours(2);
    Event evt = Event.builder()
        .setSubject("Meeting")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(evt);

    mgr.editCalendarTimezone("Home", ZoneId.of("America/New_York"));

    Calendar after = mgr.getActiveCalendar();
    assertEquals(ZoneId.of("America/New_York"), after.getZoneId());
    Event afterEvt = after.findEvents(e -> true).get(0);
    assertEquals(start, afterEvt.getStart());
    assertEquals(end, afterEvt.getEnd());
  }

  @Test
  public void testEditCalendarTimezoneConvertsEventsWhenZoneChanges() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();
    mgr.createCalendar("Home", ZoneId.of("America/New_York"));
    mgr.setActiveCalendar("Home");
    Calendar cal = mgr.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.plusHours(2);
    Event evt = Event.builder()
        .setSubject("Meeting")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(evt);

    mgr.editCalendarTimezone("Home", ZoneId.of("Europe/London"));

    Calendar after = mgr.getActiveCalendar();
    assertEquals(ZoneId.of("Europe/London"), after.getZoneId());
    Event afterEvt = after.findEvents(e -> true).get(0);

    assertNotEquals(start, afterEvt.getStart());
    assertNotEquals(end, afterEvt.getEnd());
  }

  @Test
  public void testDeleteCalendarThrowsWhenNotFound() {
    ApplicationManagerImpl model = new ApplicationManagerImpl();

    String missing = "DoesNotExist";

    try {
      model.deleteCalendar(missing);
      fail("Expected ValidationException for missing calendar");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Calendar 'DoesNotExist' not found."));
    }
  }

}