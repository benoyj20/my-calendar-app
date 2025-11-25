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
 * Integration tests for the {@link CalendarController} class.
 * Uses a real model, parser, and a {@link TestView} stub.
 */
public class CalendarControllerTest {

  private ApplicationManager model;
  private TestView view;
  private CommandParser parser;
  private CalendarController controller;

  /**
   * Sets up the MVC components before each test.
   */
  @Before
  public void setUp() {
    model = new ApplicationManagerImpl();
    view = new TestView();
    parser = new CommandParser();
    controller = new CalendarController(model, view, parser);
  }

  /**
   * Creates a mock command supplier from a list of strings.
   *
   * @param commands The list of commands to supply.
   * @return A {@link Supplier} that provides one command at a time.
   */
  private Supplier<String> createCommandSupplier(List<String> commands) {
    Queue<String> commandQueue = new LinkedList<>(commands);
    return commandQueue::poll;
  }

  /**
   * Tests a run loop with valid commands.
   * This test also covers calendar creation and usage.
   */
  @Test
  public void testControllerRunLoop() throws Exception {
    List<String> commands = List.of(
        "create calendar --name \"TestCal\" --timezone \"America/New_York\"",
        "use calendar --name \"TestCal\"",
        "create event \"Final Exam\" on 2025-12-15",
        "show status on 2025-12-15T10:00",
        "exit"
    );
    Supplier<String> supplier = createCommandSupplier(commands);
    controller.run(supplier);

    List<Event> events = model.getActiveCalendar().findEvents(e -> true);
    assertEquals(1, events.size());
    assertEquals("Final Exam", events.get(0).getSubject());

    assertEquals("Exiting calendar...", view.getLastMessage());
    assertTrue(controller.isExitCommandReceived());
  }

  /**
   * Tests that the controller catches and reports exceptions from invalid commands.
   */
  @Test
  public void testControllerHandlesException() {
    List<String> commands = List.of(
        "create calendar --name \"TestCal\" --timezone \"America/New_York\"",
        "use calendar --name \"TestCal\"",
        "create event \"Bad Command\"",
        "exit"
    );
    Supplier<String> supplier = createCommandSupplier(commands);
    controller.run(supplier);

    assertEquals("Invalid 'create event' command syntax.", view.getLastError());

    assertTrue(controller.isExitCommandReceived());
    assertEquals("Exiting calendar...", view.getLastMessage());
  }

  /**
   * Tests that the controller correctly skips empty and whitespace-only lines.
   */
  @Test
  public void testControllerRunLoopHandlesEmptyLines() throws Exception {
    List<String> commands = List.of(
        "create calendar --name \"TestCal\" --timezone \"America/New_York\"",
        "use calendar --name \"TestCal\"",
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

  /**
   * Tests that the controller loop correctly terminates when the supplier
   * returns null.
   */
  @Test
  public void testControllerRunLoopHandlesNullInput() throws Exception {
    List<String> commands = List.of(
        "create calendar --name \"TestCal\" --timezone \"America/New_York\"",
        "use calendar --name \"TestCal\"",
        "create event \"First\" on 2025-01-01"
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