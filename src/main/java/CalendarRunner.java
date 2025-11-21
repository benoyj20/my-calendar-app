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

    if (args.length == 0) {
      SwingUtilities.invokeLater(() -> {
        GuiController controller = new GuiController(model);
        SwingGuiView view = new SwingGuiView(controller);
        controller.setView(view);
        view.setVisible(true);
      });
      return;
    }

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
    view.showMessage("Welcome to the Virtual Calendar\n"
        + "\n"
        + "You are in interactive mode. The application is ready for your commands.\n"
        + "* Type your command after the > prompt and press Enter.\n"
        + "* Type 'exit' at any time to close the application.\n"
        + "-----------------------------------------------------------------\n"
        + "--- How to Write Commands ---\n"
        + "\n"
        + "* Subjects/Names with Spaces: If a calendar name or event subject has spaces\n"
        + "    (e.g., \"Team Meeting\"), you MUST enclose it in double quotes.\n"
        + "    > Correct:   create calendar --name \"Work Calendar\" --timezone America/New_York\n"
        + "    > Incorrect: create calendar --name Work Calendar ...\n"
        + "\n"
        + "* Keywords: Command keywords (e.g., 'create', 'from') are case-insensitive.\n"
        + "* Values: Calendar names and event details ARE case-sensitive.\n"
        + "\n"
        + "--- Command Format Key ---\n"
        + "  <calName>    A string, in quotes if it has spaces (e.g., \"Personal\")\n"
        + "  <timezone>   area/location format (e.g., America/New_York, Europe/Paris)\n"
        + "  <subject>    A string, in quotes if it has spaces (e.g., \"Final Exam\")\n"
        + "  <date>       YYYY-MM-DD (e.g., 2025-12-01)\n"
        + "  <time>       hh:mm (24-hour) (e.g., 09:00 or 14:30)\n"
        + "  <datetime>   YYYY-MM-DDThh:mm (e.g., 2025-12-01T09:00)\n"
        + "  <weekdays>   Sequence of chars (M,T,W,R,F,S,U) (e.g., MWF)\n"
        + "  <property>   A keyword (e.g., subject, start, location, status, name, timezone)\n"
        + "  <newValue>   A string or datetime, based on the property\n"
        + "\n"
        + "--- Command Reference ---\n"
        + "\n"
        + "  1. Manage Calendars\n"
        + "  > create calendar --name <calName> --timezone <timezone>\n"
        + "  > edit calendar --name <calName> --property <name|timezone> <newValue>\n"
        + "  > use calendar --name <calName>\n"
        + "\n"
        + "  2. Manage Events (Requires an active calendar)\n"
        + "  > create event <subject> from <datetime> to <datetime>\n"
        + "  > create event <subject> on <date>\n"
        + "  > create event ... [repeats <weekdays> for <N> times | until <date>]\n"
        + "  > edit event <property> <subject> from <datetime> to <datetime> with <newValue>\n"
        + "  > edit events <property> <subject> from <datetime> with <newValue>\n"
        + "  > edit series <property> <subject> from <datetime> with <newValue>\n"
        + "\n"
        + "  3. Query & Copy (Requires an active calendar)\n"
        + "  > print events on <date>\n"
        + "  > print events from <datetime> to <datetime>\n"
        + "  > show status on <datetime>\n"
        + "  > copy event <subject> on <datetime> --target <calName> to <datetime>\n"
        + "  > copy events on <date> --target <calName> to <date>\n"
        + "  > copy events between <date> and <date> --target <calName> to <date>\n"
        + "\n"
        + "  4. Miscellaneous\n"
        + "  > export cal <fileName.csv | fileName.ical>\n"
        + "  > exit\n"
        + "-----------------------------------------------------------------\n"
        + "Ready for your first command.");
  }
}