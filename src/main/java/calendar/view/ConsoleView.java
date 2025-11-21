package calendar.view;

import calendar.model.Event;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Console-based implementation of the CalendarView. In this
 * the user interacts with the console by inputting commands.
 */
public class ConsoleView implements CalendarView {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd");

  @Override
  public void showMessage(String message) {
    System.out.println(message);
  }

  @Override
  public void showError(String error) {
    System.err.println("ERROR: " + error);
  }

  @Override
  public void showPrompt() {
    System.out.print("> ");
  }

  @Override
  public void printEventsOnDate(List<Event> events) {
    if (events.isEmpty()) {
      showMessage("No events scheduled.");
      return;
    }
    for (Event event : events) {
      String location = event.getLocation().isEmpty() ? "" : " at " + event.getLocation();
      showMessage(String.format("* %s from %s to %s%s",
          event.getSubject(),
          event.getStart().toLocalTime().format(TIME_FORMAT),
          event.getEnd().toLocalTime().format(TIME_FORMAT),
          location
      ));
    }
  }

  @Override
  public void printEventsInRange(List<Event> events) {
    if (events.isEmpty()) {
      showMessage("No events found in this range.");
      return;
    }
    for (Event event : events) {
      String location = event.getLocation().isEmpty() ? "" : " at " + event.getLocation();
      showMessage(String.format("* %s starting on %s at %s, ending on %s at %s%s",
          event.getSubject(),
          event.getStart().toLocalDate().format(DATE_FORMAT),
          event.getStart().toLocalTime().format(TIME_FORMAT),
          event.getEnd().toLocalDate().format(DATE_FORMAT),
          event.getEnd().toLocalTime().format(TIME_FORMAT),
          location
      ));
    }
  }
}