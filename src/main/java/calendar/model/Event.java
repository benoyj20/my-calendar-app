package calendar.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable data object representing a single calendar event.
 * Uses the Builder object for construction.
 */
public final class Event implements Ievent {

  private final String subject;
  private final LocalDateTime start;
  private final LocalDateTime end;
  private final String description;
  private final String location;
  private final boolean isPrivate;
  private final boolean isAllDay;
  private final UUID seriesId;

  /**
   * Creates an event from the provided builder.
   *
   * @param builder builder carrying values
   */
  private Event(final EventBuilder builder) {
    this.subject = builder.subject;
    this.start = builder.start;
    this.end = builder.end;
    this.description = builder.description;
    this.location = builder.location;
    this.isPrivate = builder.isPrivate;
    this.isAllDay = builder.isAllDay;
    this.seriesId = (builder.seriesId == null)
        ? UUID.randomUUID()
        : builder.seriesId;
  }

  @Override
  public String getSubject() {
    return subject;
  }

  @Override
  public LocalDateTime getStart() {
    return start;
  }

  @Override
  public LocalDateTime getEnd() {
    return end;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getLocation() {
    return location;
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  @Override
  public boolean isAllDay() {
    return isAllDay;
  }

  @Override
  public UUID getSeriesId() {
    return seriesId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Event event = (Event) o;
    return Objects.equals(subject, event.subject)
        && Objects.equals(start, event.start)
        && Objects.equals(end, event.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, start, end);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Event{");
    sb.append("subject='")
        .append(subject)
        .append('\'')
        .append(", start=")
        .append(start)
        .append(", end=")
        .append(end)
        .append(", seriesId=")
        .append(seriesId)
        .append('}');
    return sb.toString();
  }

  /**
   * Creates a new builder for event.
   *
   * @return new builder
   */
  public static EventBuilder builder() {
    return new EventBuilder();
  }

  /**
   * Builder for creating immutable event instances.
   */
  public static class EventBuilder {
    private String subject;
    private LocalDateTime start;
    private LocalDateTime end;
    private String description = "";
    private String location = "";
    private boolean isPrivate = false;
    private boolean isAllDay = false;
    private UUID seriesId;

    /**
     * Creates an empty builder.
     */
    public EventBuilder() {
    }

    /**
     * Copies all fields from an existing event.
     *
     * @param event existing event
     * @return this builder
     */
    public EventBuilder fromEvent(final Event event) {
      this.subject = event.subject;
      this.start = event.start;
      this.end = event.end;
      this.description = event.description;
      this.location = event.location;
      this.isPrivate = event.isPrivate;
      this.isAllDay = event.isAllDay;
      this.seriesId = event.seriesId;
      return this;
    }

    /**
     * Sets the subject.
     *
     * @param subject subject
     * @return this builder
     */
    public EventBuilder setSubject(final String subject) {
      this.subject = subject;
      return this;
    }

    /**
     * Sets the start date/time.
     *
     * @param start start date/time
     * @return this builder
     */
    public EventBuilder setStart(final LocalDateTime start) {
      this.start = start;
      return this;
    }

    /**
     * Sets the end date/time.
     *
     * @param end end date/time
     * @return this builder
     */
    public EventBuilder setEnd(final LocalDateTime end) {
      this.end = end;
      return this;
    }

    /**
     * Sets the description.
     *
     * @param description description
     * @return this builder
     */
    public EventBuilder setDescription(final String description) {
      this.description = (description == null) ? "" : description;
      return this;
    }

    /**
     * Sets the location.
     *
     * @param location location
     * @return this builder
     */
    public EventBuilder setLocation(final String location) {
      this.location = (location == null) ? "" : location;
      return this;
    }

    /**
     * Sets whether the event is private.
     *
     * @param privateFlag private flag
     * @return this builder
     */
    public EventBuilder setPrivate(final boolean privateFlag) {
      this.isPrivate = privateFlag;
      return this;
    }

    /**
     * Sets whether the event is all-day.
     *
     * @param allDay all-day flag
     * @return this builder
     */
    public EventBuilder setAllDay(final boolean allDay) {
      this.isAllDay = allDay;
      return this;
    }

    /**
     * Sets the series identifier.
     *
     * @param seriesId series id
     * @return this builder
     */
    public EventBuilder setSeriesId(final UUID seriesId) {
      this.seriesId = seriesId;
      return this;
    }

    /**
     * Validates fields and builds the event.
     *
     * @return built event
     * @throws ValidationException if validation fails
     */
    public Event build() throws ValidationException {
      if (subject == null || subject.trim().isEmpty()) {
        throw new ValidationException("Event must have a subject.");
      }
      if (start == null) {
        throw new ValidationException("Event must have a start date/time.");
      }
      if (end == null) {
        throw new ValidationException("Event must have an end date/time.");
      }
      if (end.isBefore(start)) {
        throw new ValidationException(
            "Event end time cannot be before start time.");
      }
      if (seriesId != null
          && !start.toLocalDate().equals(end.toLocalDate())) {
        throw new ValidationException(
            "Events in a series must start and end on the same day.");
      }
      return new Event(this);
    }
  }
}