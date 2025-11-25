package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.commands.Command;
import calendar.controller.commands.UnknownCommand;
import calendar.model.ApplicationManagerImpl;
import calendar.model.ValidationException;
import org.junit.Test;

/**
 * Verifies that the UnknownCommand class correctly handles unrecognized input by halting execution
 * and reporting the specific invalid command string.
 */
public class UnknownCommandTest {

  @Test
  public void testReportsErrorForUnrecognizedInput() {
    Command cmd = new UnknownCommand("make coffee");
    try {
      cmd.execute(new ApplicationManagerImpl(), new TestView());
      fail("UnknownCommand should always throw ValidationException");
    } catch (Exception e) {
      assertTrue(e instanceof ValidationException);
      assertEquals("Invalid or unexpected command: make coffee", e.getMessage());
    }
  }
}