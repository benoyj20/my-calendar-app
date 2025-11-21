package calendar.controller.commands;

import calendar.model.ApplicationManager;
import calendar.model.EditScope;
import calendar.model.Event;
import calendar.model.ValidationException;
import calendar.view.CalendarView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Command for editing an event or series of events in the active calendar.
 * Can update a single event, all future events in a series, or the whole series.
 */
public class EditEventCommand implements Command {

  private static final DateTimeFormatter DATETIME_FORMAT =
      DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final List<String> tokens;
  private final EditScope scope;

  /**
   * Constructs a new EditEventCommand.
   *
   * @param tokens tokenized command input representing the edit event
   *               command and its arguments
   */
  public EditEventCommand(List<String> tokens) {
    this.tokens = tokens;

    switch (tokens.get(1).toLowerCase()) {
      case "events":
        this.scope = EditScope.FUTURE;
        break;
      case "series":
        this.scope = EditScope.ALL;
        break;
      default:
        this.scope = EditScope.SINGLE;
    }
  }

  /**
   * Executes the edit event command.
   *
   * @param model application model
   * @param view  view to show feedback
   * @throws Exception if validation or conflict errors occur
   */
  @Override
  public void execute(ApplicationManager model, CalendarView view) throws Exception {
    CommandParams params = parseTokens();
    LocalDateTime startTime = parseDateTime(params.startString);

    Event targetEvent = findTargetEvent(model, params.subject, startTime);
    List<Event> eventsToModify = determineEventsToModify(model, targetEvent);

    Function<Event.EventBuilder, Event.EventBuilder> modifier;
    modifier = createModifier(params.property, params.newValue);

    UUID newSeriesId = computeNewSeriesId(params.property, targetEvent);

    List<Event> newEvents;
    newEvents = buildModifiedEvents(eventsToModify, modifier, newSeriesId, model);

    applyEventChanges(model, eventsToModify, newEvents);

    view.showMessage(
        String.format("Successfully updated %d event(s).", newEvents.size())
    );
  }

  private Event findTargetEvent(ApplicationManager model, String subject,
                                LocalDateTime startTime) throws ValidationException {
    List<Event> foundEvents = model.getActiveCalendar().findEvents(
        e -> e.getSubject().equals(subject) && e.getStart().equals(startTime)
    );

    if (foundEvents.isEmpty()) {
      throw new ValidationException(
          "No event found with that subject and start time."
      );
    }
    if (foundEvents.size() > 1 && scope == EditScope.SINGLE) {
      throw new ValidationException(
          "Multiple events match. Cannot edit single event."
      );
    }
    return foundEvents.get(0);
  }

  private List<Event> determineEventsToModify(ApplicationManager model, Event targetEvent)
      throws ValidationException {
    UUID seriesId = targetEvent.getSeriesId();
    return filterEvents(List.of(targetEvent), model, targetEvent, seriesId);
  }

  private UUID computeNewSeriesId(String property, Event targetEvent) {
    boolean breaksSeries = property.equalsIgnoreCase("start")
        && scope != EditScope.SINGLE;
    return breaksSeries ? UUID.randomUUID() : null;
  }

  private void applyEventChanges(ApplicationManager model, List<Event> oldEvents,
                                 List<Event> newEvents) throws ValidationException {
    model.getActiveCalendar().removeEvents(oldEvents);
    model.getActiveCalendar().addEvents(newEvents);
  }

  private CommandParams parseTokens() throws ValidationException {
    if ((scope == EditScope.SINGLE && tokens.size() != 10)
        || (scope != EditScope.SINGLE && tokens.size() != 8)) {
      throw new ValidationException(
          scope == EditScope.SINGLE
              ? "Invalid 'edit event' syntax. Expected: edit event <prop> <subject> "
              + "from <start> to <end> with <newValue>"
              : "Invalid 'edit events/series' syntax. Expected: edit events/series <prop> "
              + "<subject> from <start> with <newValue>"
      );
    }

    String property = tokens.get(2);
    String subject = tokens.get(3);
    String startString = tokens.get(5);
    String newValue = tokens.get(scope == EditScope.SINGLE ? 9 : 7);

    return new CommandParams(property, subject, startString, newValue);
  }


  private List<Event> filterEvents(List<Event> foundEvents, ApplicationManager model,
                                   Event targetEvent, UUID seriesId)
      throws ValidationException {
    if (scope == EditScope.SINGLE || seriesId == null) {
      return List.of(targetEvent);
    }

    Predicate<Event> seriesFilter = e -> e.getSeriesId().equals(seriesId);
    Predicate<Event> timeFilter = (scope == EditScope.ALL)
        ? e -> true
        : e -> !e.getStart().isBefore(targetEvent.getStart());

    return model.getActiveCalendar().findEvents(seriesFilter.and(timeFilter));
  }

  private List<Event> buildModifiedEvents(List<Event> oldEvents,
                                          Function<Event.EventBuilder, Event.EventBuilder> modifier,
                                          UUID newSeriesId,
                                          ApplicationManager model) throws ValidationException {
    List<Event> newEvents = new ArrayList<>();
    for (Event oldEvent : oldEvents) {
      Event.EventBuilder builder = modifier.apply(Event.builder().fromEvent(oldEvent));
      builder.setSeriesId(scope == EditScope.SINGLE ? UUID.randomUUID() :
          newSeriesId != null ? newSeriesId : oldEvent.getSeriesId());

      Event newEvent = builder.build();
      if (!newEvent.equals(oldEvent) && model.getActiveCalendar().hasConflict(newEvent)) {
        throw new ValidationException("Edit creates a conflict "
            + "with an existing event: " + newEvent.getSubject());
      }
      newEvents.add(newEvent);
    }
    return newEvents;
  }


  private Function<Event.EventBuilder, Event.EventBuilder> createModifier(
      String property, String newValue) throws ValidationException {
    switch (property.toLowerCase()) {
      case "subject":
        return b -> b.setSubject(newValue);
      case "start":
        return modStart(newValue);
      case "end":
        return b -> {
          try {
            return b.setEnd(parseDateTime(newValue));
          } catch (ValidationException ex) {
            throw new RuntimeException(ex);
          }
        };
      case "description":
        return b -> b.setDescription(newValue);
      case "location":
        return b -> b.setLocation(newValue);
      case "status":
        return modStatus(newValue);
      default:
        throw new ValidationException("Unknown event property: " + property);
    }
  }

  private Function<Event.EventBuilder, Event.EventBuilder> modStart(String newValue)
      throws ValidationException {
    java.time.LocalTime newTime = parseDateTime(newValue).toLocalTime();
    return b -> {
      Event old;
      try {
        old = b.build();
      } catch (ValidationException ex) {
        throw new RuntimeException(ex);
      }
      java.time.LocalDate originalDate = old.getStart().toLocalDate();
      java.time.LocalDateTime newStart = LocalDateTime.of(originalDate, newTime);
      long mins = java.time.Duration.between(old.getStart(), old.getEnd()).toMinutes();
      return b.setStart(newStart).setEnd(newStart.plusMinutes(mins));
    };
  }

  private Function<Event.EventBuilder, Event.EventBuilder> modStatus(String newValue) {
    boolean isPrivate = "private".equalsIgnoreCase(newValue);
    return b -> b.setPrivate(isPrivate);
  }

  private LocalDateTime parseDateTime(String text) throws ValidationException {
    try {
      String fixed = text.replace("::", ":");
      return LocalDateTime.parse(fixed, DATETIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new ValidationException(
          "Invalid date/time format. Expected YYYY-MM-DDThh:mm."
      );
    }
  }

  private static class CommandParams {
    String property;
    String subject;
    String startString;
    String newValue;

    CommandParams(String property, String subject, String startString, String newValue) {
      this.property = property;
      this.subject = subject;
      this.startString = startString;
      this.newValue = newValue;
    }
  }
}
