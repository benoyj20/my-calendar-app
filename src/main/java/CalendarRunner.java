import calendar.controller.CalendarController;
import calendar.controller.CommandParser;
import calendar.controller.GuiController;
import calendar.model.ApplicationManager;
import calendar.model.ApplicationManagerImpl;
import calendar.view.CalendarView;
import calendar.view.ConsoleView;
import calendar.view.SwingGuiView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/**
 * Main entry point for the Virtual Calendar app.
 * Parses args to run in interactive, headless, or GUI mode.
 */
public final class CalendarRunner {

  private CalendarRunner() {
  }

  /**
   * Application entry point.
   *
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    ApplicationManager model = new ApplicationManagerImpl();

    // GUI Mode (No arguments)
    if (args.length == 0) {
      SwingUtilities.invokeLater(() -> {
        GuiController controller = new GuiController(model);
        SwingGuiView view = new SwingGuiView(controller);
        controller.setView(view);
        view.setVisible(true);
      });
      return;
    }

    // Text/Headless Modes
    String modeFlag = args[0].toLowerCase();

    if (!modeFlag.equals("--mode") || args.length < 2) {
      printUsage();
      return;
    }

    String modeType = args[1].toLowerCase();
    CalendarView view = new ConsoleView();
    CommandParser parser = new CommandParser();
    CalendarController controller = new CalendarController(model, view, parser);

    if (modeType.equals("interactive")) {
      runInteractive(controller, view);
    } else if (modeType.equals("headless")) {
      if (args.length != 3) {
        view.showError("Error: Headless mode requires a filename.");
        printUsage();
        return;
      }
      runHeadless(controller, view, args[2]);
    } else {
      view.showError("Error: Unknown mode '" + args[1] + "'.");
      printUsage();
    }
  }

  private static void runInteractive(final CalendarController controller,
                                     final CalendarView view) {
    printWelcomeMenu(view);
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    Supplier<String> supplier = () -> {
      try {
        view.showPrompt();
        return reader.readLine();
      } catch (IOException e) {
        view.showError("Error reading input: " + e.getMessage());
        return "exit";
      }
    };
    controller.run(supplier);
  }

  private static void runHeadless(final CalendarController controller,
                                  final CalendarView view,
                                  final String filePath) {
    view.showMessage("Virtual Calendar (Headless Mode). Reading from: " + filePath);
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      Supplier<String> supplier = () -> {
        try {
          return reader.readLine();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      };
      controller.run(supplier);
      if (!controller.isExitCommandReceived()) {
        view.showError("Error: Command file ended without an 'exit' command.");
      }
    } catch (IOException e) {
      view.showError("Failed to open command file: " + e.getMessage());
    }
  }

  private static void printUsage() {
    System.err.println("Usage:");
    System.err.println("  java -jar calendar.jar (GUI Mode)");
    System.err.println("  java -jar calendar.jar --mode interactive");
    System.err.println("  java -jar calendar.jar --mode headless <commands.txt>");
  }

  private static void printWelcomeMenu(CalendarView view) {
    view.showMessage("Welcome to the Virtual Calendar (Text Mode).");
  }
}