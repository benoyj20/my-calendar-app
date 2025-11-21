package calendar.controller.commands;

import calendar.io.EventCsvExporter;
import calendar.io.EventIcalExporter;
import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command to export all events from the active calendar to an external file.
 * Supports exporting in formats compatible with Google Calendar, such as CSV
 * or iCalendar (.ical).
 */
public class ExportCommand implements Command {

  private final List<String> tokens;

  /**
   * Constructs a new ExportCommand.
   *
   * @param tokens tokenized command input representing the export command
   *               and its arguments
   */
  public ExportCommand(final List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(final ApplicationManager model, final CalendarView view) throws Exception {
    Calendar activeCalendar = getActiveCalendar(model);
    String fileName = parseFileName();

    File file = Paths.get(fileName).toFile();
    try (FileWriter writer = new FileWriter(file)) {
      exportByFileType(activeCalendar, fileName, writer);
    }

    view.showMessage("Calendar exported successfully to: " + file.getAbsolutePath());
  }

  private Calendar getActiveCalendar(ApplicationManager model) throws ValidationException {
    try {
      return model.getActiveCalendar();
    } catch (ValidationException e) {
      throw new ValidationException(
          "Error: No calendar is active. Use 'use calendar --name <calName>' first."
      );
    }
  }

  private String parseFileName() throws ValidationException {
    if (tokens.size() != 3) {
      throw new ValidationException(
          "Invalid 'export cal' command. Expected: export cal <fileName.csv|fileName.ical>"
      );
    }
    return tokens.get(2);
  }

  private void exportByFileType(Calendar calendar, String fileName, FileWriter writer)
      throws Exception {
    if (fileName.toLowerCase().endsWith(".csv")) {
      List<Event> allEvents = calendar.findEvents(e -> true);
      new EventCsvExporter().export(allEvents, writer);

    } else if (fileName.toLowerCase().endsWith(".ical")) {
      new EventIcalExporter().export(calendar, writer);

    } else {
      throw new ValidationException("Unsupported file type. Please use .csv or .ical");
    }
  }
}
