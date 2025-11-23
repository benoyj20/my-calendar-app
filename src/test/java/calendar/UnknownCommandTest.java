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
 * Tests the {@link UnknownCommand} class.
 */
public class UnknownCommandTest {

  /**
   * Tests that executing an UnknownCommand always throws a ValidationException.
   */
  @Test
  public void testExecuteAlwaysThrows() {
    Command cmd = new UnknownCommand("some bad command");
    try {
      cmd.execute(new ApplicationManagerImpl(), new TestView());
      fail("UnknownCommand should always throw ValidationException");
    } catch (Exception e) {
      assertTrue(e instanceof ValidationException);
      assertEquals("Invalid or unexpected command: some bad command", e.getMessage());
    }
  }
}