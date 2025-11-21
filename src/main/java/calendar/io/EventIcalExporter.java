package calendar.io;

import calendar.model.Calendar;
import calendar.model.Event;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Exports a calendar's events to iCalendar (.ical) using UTC timestamps.
 */
public class EventIcalExporter {

  private static final DateTimeFormatter ICAL_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  /**
   * Writes the provided calendar's events as iCal to the given writer.
   *
   * @param calendar calendar to export
   * @param writer   destination writer
   * @throws IOException if writing fails
   */
  public void export(final Calendar calendar, final Writer writer) throws IOException {
    final ZoneId zoneId = calendar.getZoneId();

    writer.write("BEGIN:VCALENDAR\n");
    writer.write("VERSION:1.0\n");
    writer.write("PRODID:-//ChillCoders//VirtualCalendar v1.0//EN\n");

    for (Event event : calendar.findEvents(e -> true)) {
      writeEvent(writer, event, zoneId);
    }

    writer.write("END:VCALENDAR\n");
  }

  private void writeEvent(Writer writer, Event event, ZoneId zoneId) throws IOException {
    writer.write("BEGIN:VEVENT\n");
    ZonedDateTime startZoned = event.getStart().atZone(zoneId);
    ZonedDateTime endZoned = event.getEnd().atZone(zoneId);

    writer.write("UID:" + UUID.randomUUID().toString() + "\n");
    writer.write("DTSTAMP:" + ICAL_FORMAT.format(Instant.now()) + "\n");
    writer.write("DTSTART:" + ICAL_FORMAT.format(startZoned.toInstant()) + "\n");
    writer.write("DTEND:" + ICAL_FORMAT.format(endZoned.toInstant()) + "\n");
    writer.write("SUMMARY:" + escape(event.getSubject()) + "\n");
    writer.write("DESCRIPTION:" + escape(event.getDescription()) + "\n");
    writer.write("LOCATION:" + escape(event.getLocation()) + "\n");

    if (event.isPrivate()) {
      writer.write("CLASS:PRIVATE\n");
    }
    writer.write("END:VEVENT\n");
  }

  private String escape(final String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\\", "\\\\")
        .replace(",", "\\,")
        .replace(";", "\\;")
        .replace("\n", "\\n");
  }
}
