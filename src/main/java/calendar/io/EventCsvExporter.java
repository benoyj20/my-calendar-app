package calendar.io;

import calendar.model.Event;
import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports events to a CSV file in a format Google Calendar understands.
 */
public class EventCsvExporter {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("hh:mm a");

  private static final String[] HEADERS = {
      "Subject", "Start Date", "Start Time", "End Date", "End Time",
      "All Day Event", "Description", "Location", "Private"
  };

  /**
   * Writes the provided events as CSV to the given writer.
   *
   * @param events list of events
   * @param writer destination writer
   * @throws IOException if writing fails
   */
  public void export(final List<Event> events, final Writer writer)
      throws IOException {
    writer.write(String.join(",", HEADERS));
    writer.write("\n");

    for (Event event : events) {
      String[] row = new String[HEADERS.length];
      row[0] = quote(event.getSubject());
      row[1] = event.getStart().toLocalDate().format(DATE_FORMAT);
      row[2] = event.getStart().toLocalTime().format(TIME_FORMAT);
      row[3] = event.getEnd().toLocalDate().format(DATE_FORMAT);
      row[4] = event.getEnd().toLocalTime().format(TIME_FORMAT);
      row[5] = event.isAllDay() ? "True" : "False";
      row[6] = quote(event.getDescription());
      row[7] = quote(event.getLocation());
      row[8] = event.isPrivate() ? "True" : "False";

      writer.write(String.join(",", row));
      writer.write("\n");
    }
  }

  /**
   * Quotes a field if it contains commas, quotes, or newlines.
   *
   * @param text value to quote
   * @return possibly-quoted field
   */
  private String quote(final String text) {
    if (text.isEmpty()) {
      return "";
    }

    boolean needsQuotes =
        text.contains(",") || text.contains("\"") || text.contains("\n");
    if (!needsQuotes) {
      return text;
    }

    String escaped = text.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
  }
}