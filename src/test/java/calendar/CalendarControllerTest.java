package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarController;
import calendar.controller.CommandParser;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.model.Event;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the CalendarController to ensure it properly does the model
 * and view based on user commands.
 */
public class CalendarControllerTest {

  private ApplicationManager model;
  private TestView view;
  private CommandParser parser;
  private CalendarController controller;

  /**
   * Initializes the model, view, and parser before each test..
   */
  @Before
  public void setUp() {
    model = new ApplicationManagerImpl();
    view = new TestView();
    parser = new CommandParser();
    controller = new CalendarController(model, view, parser);
  }

  private Supplier<String> createCommandSupplier(List<String> commands) {
    Queue<String> commandQueue = new LinkedList<>(commands);
    return commandQueue::poll;
  }

  @Test
  public void testHandlesEmptyLinesGracefully() throws Exception {
    List<String> commands = List.of(
        "create calendar --name \"School Schedule\" --timezone \"Europe/London\"",
        "use calendar --name \"School Schedule\"",
        "",
        "   ",
        "exit"
    );
    Supplier<String> supplier = createCommandSupplier(commands);
    controller.run(supplier);

    assertNull(view.getLastError());
    assertEquals("Exiting calendar...", view.getLastMessage());
    assertTrue(controller.isExitCommandReceived());
    assertEquals(0, model.getActiveCalendar().findEvents(e -> true).size());
  }

  @Test
  public void testInvalidCommandDoesNotCrashApp() {
    List<String> commands = List.of(
        "create calendar --name \"Hobby Calendar\" --timezone \"UTC\"",
        "use calendar --name \"Hobby Calendar\"",
        "create event \"Garbage Input\"",
        "exit"
    );
    Supplier<String> supplier = createCommandSupplier(commands);
    controller.run(supplier);

    assertEquals("Invalid 'create event' command syntax.", view.getLastError());

    assertTrue(controller.isExitCommandReceived());
    assertEquals("Exiting calendar...", view.getLastMessage());
  }

  @Test
  public void testCompleteUserSessionWithEvents() throws Exception {
    List<String> commands = List.of(
        "create calendar --name \"Family Trip\" --timezone \"America/Los_Angeles\"",
        "use calendar --name \"Family Trip\"",
        "create event \"Flight to Hawaii\" on 2025-07-15",
        "show status on 2025-07-15T10:00",
        "exit"
    );
    Supplier<String> supplier = createCommandSupplier(commands);
    controller.run(supplier);

    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, events.size());
    assertEquals("Flight to Hawaii", events.get(0).getSubject());

    assertEquals("Exiting calendar...", view.getLastMessage());
    assertTrue(controller.isExitCommandReceived());
  }

  @Test
  public void testControllerEndsWhenInputEnds() throws Exception {
    List<String> commands = List.of(
        "create calendar --name \"Gym Routine\" --timezone \"America/New_York\"",
        "use calendar --name \"Gym Routine\"",
        "create event \"Morning Yoga\" on 2025-06-01"
    );
    Supplier<String> supplier = createCommandSupplier(commands);
    controller.run(supplier);

    assertFalse("Controller should not think an exit command was received",
        controller.isExitCommandReceived());

    assertEquals("Event(s) created successfully.", view.getLastMessage());
    assertNull(view.getLastError());

    assertEquals(1, model.getActiveCalendar().findEvents(e -> true).size());
  }
}