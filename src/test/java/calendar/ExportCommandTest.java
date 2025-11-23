package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * Tests the {@link ExportCommand} class.
 * Creates and reads temporary files to verify export content
 * for both CSV and iCal formats.
 */
public class ExportCommandTest {

  private ApplicationManager model;
  private TestView view;

  /**
   * Set up a model with a few different events before each test. We need one
   * simple event, one with weird characters to check escaping, and one that's
   * all-day with empty fields.
   */
  @Before
  public void setUp() throws ValidationException {
    model = new ApplicationManagerImpl();
    view = new TestView();

    model.createCalendar("TestCal", ZoneId.of("America/New_York"));
    model.setActiveCalendar("TestCal");

    LocalDateTime start = LocalDateTime.of(2025, 11, 20, 14, 30);
    LocalDateTime end = LocalDateTime.of(2025, 11, 20, 15, 0);

    Event simpleEvent = Event.builder()
        .setSubject("Test Event")
        .setStart(start)
        .setEnd(end)
        .setLocation("Here")
        .setDescription("A test")
        .build();

    Event quoteEvent = Event.builder()
        .setSubject("Comma, Quote and Newline")
        .setStart(start.plusDays(1))
        .setEnd(end.plusDays(1))
        .setLocation("Main Office, 2nd Floor")
        .setDescription("A quote: \"Hello\"\nAnd a newline.")
        .setPrivate(true)
        .build();

    Event allDayEmptyEvent = Event.builder()
        .setSubject("All Day Event")
        .setStart(start.plusDays(2))
        .setEnd(end.plusDays(2))
        .setAllDay(true)
        .build();

    Event commaEvent = Event.builder()
        .setSubject("Comma Event")
        .setStart(start.plusDays(3))
        .setEnd(end.plusDays(3))
        .setLocation("A, B")
        .build();

    Event quoteOnlyEvent = Event.builder()
        .setSubject("Quote Event")
        .setStart(start.plusDays(4))
        .setEnd(end.plusDays(4))
        .setDescription("A \"quote\"")
        .build();

    Event newlineEvent = Event.builder()
        .setSubject("Newline Event")
        .setStart(start.plusDays(5))
        .setEnd(end.plusDays(5))
        .setDescription("Line 1\nLine 2")
        .build();

    model.getActiveCalendar().addEvents(List.of(
        simpleEvent, quoteEvent, allDayEmptyEvent,
        commaEvent, quoteOnlyEvent, newlineEvent
    ));
  }

  private File tempCsvFile = null;
  private File tempIcalFile = null;

  /**
   * Clean up any temporary files we created so we don't pollute the file system.
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

  /**
   * Checks a standard, simple CSV export. This makes sure the happy path
   * works and the headers are written correctly.
   */
  @Test
  public void testExportSuccessCsv() throws Exception {
    tempCsvFile = File.createTempFile("test_cal_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    assertTrue(view.getLastMessage().contains("Calendar exported successfully"));

    String content = Files.readString(tempCsvFile.toPath());
    assertTrue(content.contains("Subject,Start Date,Start Time,End Date,End Time"));
    assertTrue(content.contains(
        "Test Event,11/20/2025,02:30 PM,11/20/2025,03:00 PM,False,A test,Here,False"
    ));
  }

  /**
   * Tests a CSV export where the event subject and location have commas,
   * quotes, and newlines. This ensures our `quote()` method is working right.
   */
  @Test
  public void testExportWithQuotedFieldsCsv() throws Exception {
    tempCsvFile = File.createTempFile("test_quote_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());

    assertTrue(content.contains(
        "\"Comma, Quote and Newline\",11/21/2025,02:30 PM,11/21/2025,03:00 PM,False,\""
            + "A quote: \"\"Hello\"\"\nAnd a newline.\",\"Main Office, 2nd Floor\",True"
    ));
  }

  /**
   * This test is now stronger and checks for all required iCal lines
   * to satisfy mutation testing coverage.
   */
  @Test
  public void testExportSuccessIcal() throws Exception {
    tempIcalFile = File.createTempFile("test_cal_export", ".ical");
    tempIcalFile.deleteOnExit();
    String path = tempIcalFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    assertTrue(view.getLastMessage().contains("Calendar exported successfully"));
    String content = Files.readString(tempIcalFile.toPath());

    assertTrue(content.startsWith("BEGIN:VCALENDAR"));
    assertTrue(content.contains("VERSION:1.0"));

    assertTrue(content.contains("PRODID:-//ChillCoders//VirtualCalendar v1.0//EN"));

    assertTrue(content.contains("BEGIN:VEVENT"));

    assertTrue(Pattern.compile("UID:[a-f0-9\\-]+\n").matcher(content).find());
    assertTrue(Pattern.compile("DTSTAMP:\\d{8}T\\d{6}Z\n").matcher(content).find());
    assertTrue(Pattern.compile("DTSTART:\\d{8}T\\d{6}Z\n").matcher(content).find());
    assertTrue(Pattern.compile("DTEND:\\d{8}T\\d{6}Z\n").matcher(content).find());

    assertTrue(content.contains("SUMMARY:Test Event"));
    assertTrue(content.contains("LOCATION:Here"));
    assertTrue(content.contains("DESCRIPTION:A test"));
    assertTrue(content.contains("CLASS:PRIVATE"));
    assertTrue(content.contains("END:VEVENT"));
    assertTrue(content.endsWith("END:VCALENDAR\n"));
  }

  /**
   * Tests an iCal export with special characters. This checks that commas
   * and newlines are properly escaped with backslashes.
   */
  @Test
  public void testExportWithEscapedFieldsIcal() throws Exception {
    tempIcalFile = File.createTempFile("test_quote_export", ".ical");
    tempIcalFile.deleteOnExit();
    String path = tempIcalFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempIcalFile.toPath());

    assertTrue(content.contains("SUMMARY:Comma\\, Quote and Newline"));
    assertTrue(content.contains("LOCATION:Main Office\\, 2nd Floor"));
    assertTrue(content.contains("DESCRIPTION:A quote: \"Hello\"\\nAnd a newline."));
  }

  /**
   * Makes sure the command fails if the user just types `export cal`
   * without a filename.
   */
  @Test(expected = ValidationException.class)
  public void testExportSyntaxError() throws Exception {
    List<String> tokens = List.of("export", "cal");
    new ExportCommand(tokens).execute(model, view);
  }

  /**
   * Makes sure the command fails if the user tries to export
   * to a weird file type like `.txt`.
   */
  @Test
  public void testExportUnsupportedExtension() throws Exception {
    List<String> tokens = List.of("export", "cal", "myfile.txt");
    try {
      new ExportCommand(tokens).execute(model, view);
      fail("Expected ValidationException for unsupported file type.");
    } catch (ValidationException e) {
      assertEquals("Unsupported file type. Please use .csv or .ical", e.getMessage());
    }
  }

  /**
   * Checks that the command throws an error if the user tries to export
   * before selecting a calendar with `use calendar`.
   */
  @Test(expected = ValidationException.class)
  public void testErrorNoActiveCalendar() throws Exception {
    ApplicationManager freshModel = new ApplicationManagerImpl();
    freshModel.createCalendar("TestCal", ZoneId.of("UTC"));

    List<String> tokens = List.of("export", "cal", "file.csv");
    new ExportCommand(tokens).execute(freshModel, view);
  }

  /**
   * Tests the CSV export for an all-day event. This also checks that `null` or
   * empty fields (like description) are exported as just empty commas,
   * not the string "null". This covers the `isAllDay() == true` branch
   * and the `text.isEmpty()` branch.
   */
  @Test
  public void testExportCoversAllDayAndEmptyFields() throws Exception {
    tempCsvFile = File.createTempFile("test_allday_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());

    assertTrue(content.contains(
        "All Day Event,11/22/2025,02:30 PM,11/22/2025,03:00 PM,True,,,False"
    ));
  }

  /**
   * Verifies that a field containing only a comma is correctly quoted.
   */
  @Test
  public void testExportQuotesFieldWithComma() throws Exception {
    tempCsvFile = File.createTempFile("test_comma_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());
    assertTrue(content.contains("Comma Event"));
    assertTrue(content.contains("\"A, B\""));
  }

  /**
   * Verifies that a field containing only a double-quote is correctly quoted.
   */
  @Test
  public void testExportQuotesFieldWithQuote() throws Exception {
    tempCsvFile = File.createTempFile("test_quote_only_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());
    assertTrue(content.contains("Quote Event"));
    assertTrue(content.contains("\"A \"\"quote\"\"\""));
  }

  /**
   * Verifies that a field containing only a newline is correctly quoted.
   */
  @Test
  public void testExportQuotesFieldWithNewline() throws Exception {
    tempCsvFile = File.createTempFile("test_newline_export", ".csv");
    tempCsvFile.deleteOnExit();
    String path = tempCsvFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempCsvFile.toPath());
    assertTrue(content.contains("Newline Event"));
    assertTrue(content.contains("\"Line 1\nLine 2\""));
  }

  /**
   * Tests an iCal export on an empty calendar. This ensures the for-loop
   * is skipped but the header/footer are still written correctly.
   */
  @Test
  public void testExportIcalOnEmptyCalendar() throws Exception {
    model.createCalendar("EmptyCal", ZoneId.of("UTC"));
    model.setActiveCalendar("EmptyCal");

    tempIcalFile = File.createTempFile("test_empty_export", ".ical");
    tempIcalFile.deleteOnExit();
    String path = tempIcalFile.getAbsolutePath();

    List<String> tokens = List.of("export", "cal", path);
    new ExportCommand(tokens).execute(model, view);

    String content = Files.readString(tempIcalFile.toPath());

    assertTrue(content.contains("BEGIN:VCALENDAR"));
    assertTrue(content.contains("PRODID:-//ChillCoders//VirtualCalendar v1.0//EN"));
    assertFalse("Should not contain any VEVENT blocks", content.contains("BEGIN:VEVENT"));
    assertTrue(content.endsWith("END:VCALENDAR\n"));
  }
}