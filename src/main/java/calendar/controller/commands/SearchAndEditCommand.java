package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Command to search for events by name or date range and modify a specific property
 * for all matches simultaneously.
 */
public class SearchAndEditCommand implements Command {

  private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private final List<String> tokens;

  /**
   * Constructor to initialise command.
   *
   * @param tokens List of tokens to parse
   */
  public SearchAndEditCommand(List<String> tokens) {
    this.tokens = tokens;
  }

  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    Calendar activeCalendar = getActiveCalendar(model);

    Predicate<Event> filter = parseSearchCriteria();

    List<Event> matches = activeCalendar.findEvents(filter);
    if (matches.isEmpty()) {
      view.showMessage("No events found matching your criteria.");
      return;
    }

    ModificationParams modParams = parseModificationParams();
    Function<Event.EventBuilder, Event.EventBuilder> modifier = createModifier(modParams);

    List<Event> newEvents = new ArrayList<>();
    for (Event oldEvent : matches) {
      Event.EventBuilder builder = Event.builder().fromEvent(oldEvent);

      modifier.apply(builder);

      builder.setSeriesId(oldEvent.getSeriesId());

      newEvents.add(builder.build());
    }

    try {
      activeCalendar.removeEvents(matches);
      activeCalendar.addEvents(newEvents);
      view.showMessage(String.format("Successfully modified %d event(s).", matches.size()));
    } catch (ValidationException e) {
      activeCalendar.addEvents(matches);
      throw new ValidationException("Bulk edit failed due to conflict: " + e.getMessage());
    }
  }

  private Calendar getActiveCalendar(ApplicationManager model) throws ValidationException {
    try {
      return model.getActiveCalendar();
    } catch (ValidationException e) {
      throw new ValidationException("Error: No active calendar.");
    }
  }

  private Predicate<Event> parseSearchCriteria() throws ValidationException {
    String criteriaType = tokens.get(2).toLowerCase();

    if ("subject".equals(criteriaType)) {
      String subject = tokens.get(3);
      return e -> e.getSubject().equalsIgnoreCase(subject);

    } else if ("between".equals(criteriaType)) {
      LocalDateTime start = parseDateTime(tokens.get(3));
      if (!"and".equalsIgnoreCase(tokens.get(4))) {
        throw new ValidationException("Expected 'and' in date range.");
      }
      LocalDateTime end = parseDateTime(tokens.get(5));
      return e -> !e.getStart().isBefore(start) && !e.getEnd().isAfter(end);

    } else {
      throw new ValidationException("Unknown search criteria. Use 'subject' or 'between'.");
    }
  }

  private ModificationParams parseModificationParams() throws ValidationException {
    int setIndex = -1;
    for (int i = 0; i < tokens.size(); i++) {
      if ("set".equalsIgnoreCase(tokens.get(i))) {
        setIndex = i;
        break;
      }
    }

    if (setIndex == -1 || setIndex + 2 >= tokens.size()) {
      throw new ValidationException("Syntax error. Expected '... set <property> <value>'");
    }

    String property = tokens.get(setIndex + 1);
    String value = tokens.get(setIndex + 2);
    return new ModificationParams(property, value);
  }

  private Function<Event.EventBuilder, Event.EventBuilder> createModifier(ModificationParams params)
      throws ValidationException {
    String val = params.value;
    switch (params.property.toLowerCase()) {
      case "subject":
        return b -> b.setSubject(val);
      case "description":
        return b -> b.setDescription(val);
      case "location":
        return b -> b.setLocation(val);
      case "status":
        boolean isPrivate = "private".equalsIgnoreCase(val);
        return b -> b.setPrivate(isPrivate);
      default:
        throw new ValidationException("Property '" + params.property + "' cannot be bulk edited.");
    }
  }

  private LocalDateTime parseDateTime(String text) throws ValidationException {
    try {
      return LocalDateTime.parse(text.replace("::", ":"), DATETIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException("Invalid format. Expected YYYY-MM-DDThh:mm.");
    }
  }

  private static class ModificationParams {
    String property;
    String value;

    ModificationParams(String p, String v) {
      property = p;
      value = v;
    }
  }
}