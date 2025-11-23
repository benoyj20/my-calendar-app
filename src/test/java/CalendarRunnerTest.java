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
 * Tests the {@link CalendarRunner} class, covering all branches of the
 * main() method, argument parsing, and modes (interactive/headless).
 *
 */
public class CalendarRunnerTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final InputStream originalIn = System.in;

  /**
   * Redirects all standard I/O streams before each test.
   */
  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  /**
   * Restores all standard I/O streams after each test.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);
  }

  /**
   * Simulates user input for interactive mode tests.
   *
   * @param input The string to be fed into System.in
   */
  private void simulateInput(String input) {
    ByteArrayInputStream inStream = new ByteArrayInputStream(input.getBytes());
    System.setIn(inStream);
  }

  /**
   * Creates a temporary command file for headless mode tests.
   *
   * @param content The commands to write to the file
   * @return The created temporary file
   * @throws Exception if file creation fails
   */
  private File createTempCommandFile(String content) throws Exception {
    File tempFile = File.createTempFile("commands", ".txt");
    tempFile.deleteOnExit();
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write(content);
    }
    return tempFile;
  }

  /**
   * Tests the main method with no arguments.
   * This path must call printUsage().
   */
  @Test
  public void testMainNoArgs() {
    CalendarRunner.main(new String[0]);
    assertTrue(errContent.toString().contains("Usage:"));
  }

  /**
   * Tests the main method with an invalid flag.
   * This path must call printUsage().
   */
  @Test
  public void testMainBadFlag() {
    CalendarRunner.main(new String[] {"--invalid", "interactive"});
    assertTrue(errContent.toString().contains("Usage:"));
  }

  /**
   * Tests the main method with an unknown mode.
   * This path must call printUsage().
   */
  @Test
  public void testMainUnknownMode() {
    CalendarRunner.main(new String[] {"--mode", "invalid"});
    assertTrue(errContent.toString().contains("Error: Unknown mode 'invalid'"));
    assertTrue(errContent.toString().contains("Usage:"));
  }

  /**
   * Tests interactive mode with extra arguments, which should fail.
   * This path must call printUsage().
   */
  @Test
  public void testMainInteractiveWithFile() {
    CalendarRunner.main(new String[] {"--mode", "interactive", "somefile.txt"});
    assertTrue(errContent.toString().contains("Error: Interactive mode does not "
        + "accept a filename."));
    assertTrue(errContent.toString().contains("Usage:"));
  }

  /**
   * Tests headless mode with too few arguments, which should fail.
   * This path must call printUsage().
   */
  @Test
  public void testMainHeadlessNoFile() {
    CalendarRunner.main(new String[] {"--mode", "headless"});
    assertTrue(errContent.toString().contains("Error: Headless mode requires a filename."));
    assertTrue(errContent.toString().contains("Usage:"));
  }

  /**
   * Tests headless mode with a non-existent file path.
   */
  @Test
  public void testMainHeadlessBadFile() {
    String badPath = "non_existent_directory" + File.separator + "badfile.txt";
    CalendarRunner.main(new String[] {"--mode", "headless", badPath});
    assertTrue(errContent.toString().contains("Failed to open command file"));
  }

  /**
   * Tests a successful run in interactive mode.
   */
  @Test
  public void testMainInteractiveSuccess() {
    simulateInput("exit" + System.lineSeparator());
    CalendarRunner.main(new String[] {"--mode", "interactive"});

    String output = outContent.toString();
    assertTrue(output.contains("Welcome to the Virtual Calendar"));
    assertTrue(output.contains("Exiting calendar..."));
  }

  /**
   * Tests a successful run in headless mode.
   *
   * @throws Exception if file creation fails
   */
  @Test
  public void testMainHeadlessSuccess() throws Exception {
    String commands = String.join(System.lineSeparator(),
        "create calendar --name \"TestCal\" --timezone \"America/New_York\"",
        "use calendar --name \"TestCal\"",
        "create event \"Test\" on 2025-01-01",
        "exit"
    );
    File cmdFile = createTempCommandFile(commands);
    CalendarRunner.main(new String[] {"--mode", "headless", cmdFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.contains("Virtual Calendar (Headless Mode)"));
    assertTrue(output.contains("Calendar 'TestCal' created successfully."));
    assertTrue(output.contains("Now using calendar 'TestCal'."));
    assertTrue(output.contains("Event(s) created successfully."));
    assertTrue(output.contains("Exiting calendar..."));
    // Ensure no errors were printed
    assertEquals("", errContent.toString());
  }

  /**
   * Tests headless mode where the command file ends without an 'exit' command.
   *
   * @throws Exception if file creation fails
   */
  @Test
  public void testMainHeadlessNoExit() throws Exception {
    File cmdFile = createTempCommandFile("create event Test on 2025-01-01");
    CalendarRunner.main(new String[] {"--mode", "headless", cmdFile.getAbsolutePath()});

    assertFalse(outContent.toString().contains("Event(s) created successfully."));

    String errorOutput = errContent.toString();
    assertTrue(errorOutput.contains("No calendar is active."));
    assertTrue(errorOutput.contains("Error: Command file "
        + "ended without an 'exit' command."));
  }

  /**
   * Tests the private constructor for 100% code coverage.
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testPrivateConstructor() throws Exception {
    Constructor<CalendarRunner> constructor = CalendarRunner.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    try {
      constructor.newInstance();
    } catch (InvocationTargetException e) {
      fail("Constructor should not throw an exception.");
    }
    assertNotNull("Constructor exists", constructor);
  }
}