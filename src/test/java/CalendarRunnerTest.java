import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the CalendarRunner acts as the correct entry point for the application,
 * handling command-line arguments properly and launching either interactive or headless mode.
 */
public class CalendarRunnerTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final InputStream originalIn = System.in;

  /**
   * Redirects system I/O streams to internal buffers adn inspect the output
   * and inject input during tests.
   */
  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  /**
   * Restore.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);
  }

  private void simulateInput(String input) {
    ByteArrayInputStream inStream = new ByteArrayInputStream(input.getBytes());
    System.setIn(inStream);
  }

  private File createTempCommandFile(String content) throws Exception {
    File tempFile = File.createTempFile("commands", ".txt");
    tempFile.deleteOnExit();
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write(content);
    }
    return tempFile;
  }

  @Test
  public void testFailsIfHeadlessFileIsMissing() {
    String badPath = "folder_that_does_not_exist" + File.separator + "missing.txt";
    CalendarRunner.main(new String[] {"--mode", "headless", badPath});
    assertTrue(errContent.toString().contains("Failed to open command file"));
  }

  @Test
  public void testRunsSuccessfullyInInteractiveMode() {
    simulateInput("exit" + System.lineSeparator());
    CalendarRunner.main(new String[] {"--mode", "interactive"});

    String output = outContent.toString();
    assertTrue(output.contains("Welcome to the Virtual Calendar"));
    assertTrue(output.contains("Exiting calendar..."));
  }

  @Test
  public void testFailsIfArgumentIsUnknown() {
    CalendarRunner.main(new String[] {"--pizza", "interactive"});
    assertTrue(errContent.toString().contains("Usage:"));
  }

  @Test
  public void testFailsIfModeIsInvalid() {
    CalendarRunner.main(new String[] {"--mode", "virtual-reality"});
    assertTrue(errContent.toString().contains("Error: Unknown mode 'virtual-reality'"));
    assertTrue(errContent.toString().contains("Usage:"));
  }

  @Test
  public void testConstructorIsPrivate() throws Exception {
    Constructor<CalendarRunner> constructor = CalendarRunner.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    try {
      constructor.newInstance();
    } catch (InvocationTargetException e) {
      fail("Constructor should not throw an exception.");
    }
    assertNotNull("Constructor exists", constructor);
  }

  @Test
  public void testRunsSuccessfullyInHeadlessMode() throws Exception {
    String commands = String.join(System.lineSeparator(),
        "create calendar --name \"Project Alpha\" --timezone \"Europe/London\"",
        "use calendar --name \"Project Alpha\"",
        "create event \"Kickoff\" on 2025-05-01",
        "exit"
    );
    File cmdFile = createTempCommandFile(commands);
    CalendarRunner.main(new String[] {"--mode", "headless", cmdFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.contains("Virtual Calendar (Headless Mode)"));
    assertTrue(output.contains("Calendar 'Project Alpha' created successfully."));
    assertTrue(output.contains("Now using calendar 'Project Alpha'."));
    assertTrue(output.contains("Event(s) created successfully."));
    assertTrue(output.contains("Exiting calendar..."));
    assertEquals("", errContent.toString());
  }

  @Test
  public void testFailsIfHeadlessMissingFilename() {
    CalendarRunner.main(new String[] {"--mode", "headless"});
    assertTrue(errContent.toString().contains("Error: Headless mode requires a filename."));
    assertTrue(errContent.toString().contains("Usage:"));
  }

  @Test
  public void testFailsIfHeadlessScriptHasNoExit() throws Exception {
    File cmdFile = createTempCommandFile("create event 'Forever Loop' on 2025-01-01");
    CalendarRunner.main(new String[] {"--mode", "headless", cmdFile.getAbsolutePath()});

    assertFalse(outContent.toString().contains("Event(s) created successfully."));

    String errorOutput = errContent.toString();
    assertTrue(errorOutput.contains("No calendar is active."));
    assertTrue(errorOutput.contains("Error: Command file "
        + "ended without an 'exit' command."));
  }
}