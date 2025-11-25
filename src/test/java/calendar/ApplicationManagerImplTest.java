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
 * Ensures the ApplicationManager correctly handles calendar creation, deletion,
 * and updates.
 */
public class ApplicationManagerImplTest {

  private ApplicationManager model;
  private final ZoneId zoneNewYork = ZoneId.of("America/New_York");
  private final ZoneId zoneLondon = ZoneId.of("Europe/London");

  /**
   * Resets the application state before each test, creating a clean environment
   * with two default calendars: "Family Schedule" and "Project Alpha".
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    model.createCalendar("Project Alpha", zoneLondon);
    model.createCalendar("Family Schedule", zoneNewYork);
  }

  @Test
  public void testShouldFailWhenCreatingCalendarWithDuplicateName() throws Exception {
    try {
      model.createCalendar("Project Alpha", zoneNewYork);
      fail("Expected ValidationException for duplicate calendar name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Project Alpha' already exists.", e.getMessage());
    }
  }

  @Test
  public void testShouldFailToUpdateMissingCalendarTimezone() throws Exception {
    try {
      model.editCalendarTimezone("Ghost Calendar", zoneLondon);
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Ghost Calendar' not found.", e.getMessage());
    }
  }

  @Test
  public void testShouldCreateNewCalendarSuccessfully() throws Exception {
    model.createCalendar("Gym Routine", ZoneId.of("Asia/Tokyo"));
    Calendar cal = model.getCalendar("Gym Routine");
    assertNotNull(cal);
    assertEquals("Gym Routine", cal.getName());
    assertEquals(ZoneId.of("Asia/Tokyo"), cal.getZoneId());
  }

  @Test
  public void testShouldRenameCalendarSuccessfully() throws Exception {
    model.editCalendarName("Project Alpha", "Project Beta");
    assertNotNull(model.getCalendar("Project Beta"));

    try {
      model.getCalendar("Project Alpha");
      fail("Old calendar name 'Project Alpha' should no longer exist.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Project Alpha' not found.", e.getMessage());
    }
  }

  @Test
  public void testShouldUpdateTimezoneSuccessfully() throws Exception {
    model.editCalendarTimezone("Family Schedule", ZoneId.of("Asia/Tokyo"));
    assertEquals(ZoneId.of("Asia/Tokyo"), model.getCalendar("Family Schedule").getZoneId());
  }

  @Test
  public void testShouldFailToActivateMissingCalendar() throws Exception {
    try {
      model.setActiveCalendar("Secret Plan");
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Secret Plan' not found.", e.getMessage());
    }
  }

  @Test
  public void testShouldSwitchActiveCalendarWhenCurrentIsDeleted() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();

    mgr.createCalendar("Primary", ZoneId.systemDefault());
    mgr.createCalendar("Backup", ZoneId.systemDefault());

    assertEquals("Primary", mgr.getActiveCalendar().getName());

    mgr.deleteCalendar("Primary");

    assertEquals("Backup", mgr.getActiveCalendar().getName());
  }

  @Test
  public void testShouldRejectNullParametersForCreation() {
    try {
      model.createCalendar(null, zoneLondon);
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

  @Test
  public void testShouldUpdateEventTimesWhenTimezoneChanges() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();
    mgr.createCalendar("Trip", ZoneId.of("America/New_York"));
    mgr.setActiveCalendar("Trip");
    Calendar cal = mgr.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 5, 1, 10, 0);
    LocalDateTime end = start.plusHours(2);
    Event evt = Event.builder()
        .setSubject("Flight to LHR")
        .setStart(start)
        .setEnd(end)
        .build();
    cal.addEvent(evt);

    mgr.editCalendarTimezone("Trip", ZoneId.of("Europe/London"));

    Calendar after = mgr.getActiveCalendar();
    assertEquals(ZoneId.of("Europe/London"), after.getZoneId());
    Event afterEvt = after.findEvents(e -> true).get(0);

    assertNotEquals(start, afterEvt.getStart());
    assertNotEquals(end, afterEvt.getEnd());
  }

  @Test
  public void testShouldFailToDeleteMissingCalendar() {
    ApplicationManagerImpl model = new ApplicationManagerImpl();

    String missing = "Old Calendar";

    try {
      model.deleteCalendar(missing);
      fail("Expected ValidationException for missing calendar");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Calendar 'Old Calendar' not found."));
    }
  }

  @Test
  public void testShouldUpdateActiveReferenceOnRename() throws Exception {
    model.setActiveCalendar("Project Alpha");
    assertEquals("Project Alpha", model.getActiveCalendar().getName());

    model.editCalendarName("Project Alpha", "Project Alpha V2");

    assertEquals("Project Alpha V2", model.getActiveCalendar().getName());
  }

  @Test
  public void testShouldClearActiveWhenLastCalendarIsDeleted() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();

    mgr.createCalendar("OnlyOne", ZoneId.systemDefault());
    assertEquals("OnlyOne", mgr.getActiveCalendar().getName());

    mgr.deleteCalendar("OnlyOne");

    try {
      mgr.getActiveCalendar();
      fail("Should throw ValidationException when no active calendar exists");
    } catch (ValidationException expected) {
    }
  }

  @Test
  public void testShouldFailToRenameToExistingName() throws Exception {
    try {
      model.editCalendarName("Project Alpha", "Family Schedule");
      fail("Expected ValidationException for duplicate name.");
    } catch (ValidationException e) {
      assertEquals("A calendar with the name 'Family Schedule' already exists.", e.getMessage());
    }
  }

  @Test
  public void testShouldNotChangeEventTimesIfTimezoneIsSame() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();
    mgr.createCalendar("Home", ZoneId.of("America/New_York"));
    mgr.setActiveCalendar("Home");
    Calendar cal = mgr.getActiveCalendar();

    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = start.plusHours(2);
    Event evt = Event.builder()
        .setSubject("Breakfast")
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
  public void testShouldMaintainActiveReferenceIfDifferentCalendarRenamed() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();

    mgr.createCalendar("Current", ZoneId.systemDefault());
    mgr.createCalendar("Old", ZoneId.systemDefault());

    assertEquals("Current", mgr.getActiveCalendar().getName());

    mgr.editCalendarName("Old", "Archived");

    assertEquals("Current", mgr.getActiveCalendar().getName());
  }

  @Test
  public void testShouldFailToRenameMissingCalendar() throws Exception {
    try {
      model.editCalendarName("Mystery", "New Name");
      fail("Expected ValidationException for missing calendar.");
    } catch (ValidationException e) {
      assertEquals("Calendar 'Mystery' not found.", e.getMessage());
    }
  }

  @Test
  public void testShouldMaintainActiveReferenceIfDifferentCalendarDeleted() throws Exception {
    ApplicationManagerImpl mgr = new ApplicationManagerImpl();

    mgr.createCalendar("KeepMe", ZoneId.systemDefault());
    mgr.createCalendar("DeleteMe", ZoneId.systemDefault());

    assertEquals("KeepMe", mgr.getActiveCalendar().getName());

    mgr.deleteCalendar("DeleteMe");

    assertEquals("KeepMe", mgr.getActiveCalendar().getName());
  }

  @Test
  public void testShouldAllowSwitchingActiveCalendar() throws Exception {
    model.setActiveCalendar("Family Schedule");
    Calendar activeCal = model.getActiveCalendar();
    assertEquals("Family Schedule", activeCal.getName());
    assertEquals(zoneNewYork, activeCal.getZoneId());
  }
}