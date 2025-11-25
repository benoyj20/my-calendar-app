package calendar;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.commands.ExportCommand;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Event;
import calendar.model.ValidationException;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures the ExportCommand correctly writes calendar data to files,
 * verifying both CSV and iCal formats, handling special characters properly,
 * and rejecting invalid file types.
 */
public class ExportCommandTest {

  private ApplicationManager model;
  private TestView view;
  private File tempCsvFile = null;
  private File tempIcalFile = null;

  /**
   * Sets up a "Work" calendar with a mix of vents
   * (quotes, commas, all-day) before each test.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("Work", ZoneId.of("America/New_York"));
    model.setActiveCalendar("Work");

    LocalDateTime baseTime = LocalDateTime.of(2025, 11, 20, 14, 30);

    Event standard = Event.builder()
        .setSubject("Weekly Sync")
        .setStart(baseTime)
        .setEnd(baseTime.plusMinutes(30))
        .setLocation("Room 101")
        .setDescription("Regular team updates")
        .build();

    Event complex = Event.builder()
        .setSubject("Review: Q4, Strategy & Goals")
        .setStart(baseTime.plusDays(1))
        .setEnd(baseTime.plusDays(1).plusMinutes(30))
        .setLocation("Main Hall, West Wing")
        .setDescription("Notes: \"Important\"\nDon't be late.")
        .setPrivate(true)
        .build();

    Event allDay = Event.builder()
        .setSubject("Company Holiday")
        .setStart(baseTime.plusDays(2))
        .setEnd(baseTime.plusDays(2).plusMinutes(30))
        .setAllDay(true)
        .build();

    model.getActiveCalendar().addEvents(List.of(standard, complex, allDay));
  }

  /**
   * Cleanup.
   */
  @After
  public void tearDown() {
    if (tempCsvFile != null) {
      tempCsvFile.delete();
      tempCsvFile = null;
    }
    if (tempIcalFile != null) {
      tempIcalFile.delete();
      tempIcalFile = null;
    }
  }

  @Test
  public void testCanExportSpecialCharactersToIcal() throws Exception {
    tempIcalFile = File.createTempFile("complex_export", ".ical");
    tempIcalFile.deleteOnExit();
    String path = tempIcalFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempIcalFile.toPath());

    assertTrue(content.contains("SUMMARY:Review: Q4\\, Strategy & Goals"));
    assertTrue(content.contains("LOCATION:Main Hall\\, West Wing"));
    assertTrue(content.contains("DESCRIPTION:Notes: \"Important\"\\nDon't be late."));
  }

  @Test(expected = ValidationException.class)
  public void testRejectsExportWithoutFilename() throws Exception {
    List<String> tokens = List.of("export", "cal");
    new ExportCommand(tokens).execute(model, view);
  }

  @Test
  public void testCanExportStandardCsv() throws Exception {
    tempCsvFile = File.createTempFile("simple_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    assertTrue(view.getLastMessage().contains("Calendar exported successfully"));

    String content = Files.readString(tempCsvFile.toPath());
    assertTrue(content.contains("Subject,Start Date,Start Time,End Date,End Time"));
    assertTrue(content.contains(
        "Weekly Sync,11/20/2025,02:30 PM,11/20/2025,03:00 PM,False,Regular "
           + "team updates,Room 101,False"
    ));
  }

  @Test
  public void testCanExportEmptyCalendarToIcal() throws Exception {
    model.createCalendar("Empty", ZoneId.of("UTC"));
    model.setActiveCalendar("Empty");

    tempIcalFile = File.createTempFile("empty_export", ".ical");
    tempIcalFile.deleteOnExit();
    String path = tempIcalFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempIcalFile.toPath());

    assertTrue(content.contains("BEGIN:VCALENDAR"));
    assertTrue(content.contains("PRODID:-//ChillCoders//VirtualCalendar v1.0//EN"));
    assertTrue(content.endsWith("END:VCALENDAR\n"));
  }

  @Test
  public void testCanExportComplexCsvWithQuotes() throws Exception {
    tempCsvFile = File.createTempFile("complex_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());

    assertTrue(content.contains(
        "\"Review: Q4, Strategy & Goals\",11/21/2025,02:30 PM,11/21/2025,03:00 PM,False,\""
            + "Notes: \"\"Important\"\"\nDon't be late.\",\"Main Hall, West Wing\",True"
    ));
  }

  @Test
  public void testCanExportStandardIcal() throws Exception {
    tempIcalFile = File.createTempFile("simple_export", ".ical");
    tempIcalFile.deleteOnExit();
    String path = tempIcalFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    assertTrue(view.getLastMessage().contains("Calendar exported successfully"));
    String content = Files.readString(tempIcalFile.toPath());

    assertTrue(content.startsWith("BEGIN:VCALENDAR"));
    assertTrue(content.contains("VERSION:1.0"));
    assertTrue(content.contains("BEGIN:VEVENT"));

    assertTrue(Pattern.compile("UID:[a-f0-9\\-]+\n").matcher(content).find());
    assertTrue(Pattern.compile("DTSTAMP:\\d{8}T\\d{6}Z\n").matcher(content).find());

    assertTrue(content.contains("SUMMARY:Weekly Sync"));
    assertTrue(content.contains("LOCATION:Room 101"));
    assertTrue(content.contains("DESCRIPTION:Regular team updates"));
    assertTrue(content.contains("CLASS:PRIVATE"));
    assertTrue(content.contains("END:VEVENT"));
    assertTrue(content.endsWith("END:VCALENDAR\n"));
  }

  @Test
  public void testFailsForInvalidFileExtension() throws Exception {
    List<String> tokens = List.of("export", "cal", "schedule.pdf");
    try {
      new ExportCommand(tokens).execute(model, view);
      fail("Expected ValidationException for unsupported file type.");
    } catch (ValidationException e) {
      assertTrue(e.getMessage().contains("Unsupported file type"));
    }
  }

  @Test
  public void testCanExportAllDayEventsCsv() throws Exception {
    tempCsvFile = File.createTempFile("allday_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());

    assertTrue(content.contains(
        "Company Holiday,11/22/2025,02:30 PM,11/22/2025,03:00 PM,True,,,False"
    ));
  }
}