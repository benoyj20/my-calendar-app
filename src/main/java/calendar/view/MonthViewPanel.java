package calendar.view;

import calendar.controller.ControllerFeatures;
import calendar.model.Event;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Renders the Month View calendar grid.
 * This panel organizes days into a 7-column grid for the weeks.
 */
public class MonthViewPanel extends JPanel {
  private final ControllerFeatures features;
  private final SwingGuiView parentView;

  /**
   * Initializes the month view grid.
   *
   * @param parentView The main application window.
   * @param features   The controller to notify when a specific date is clicked.
   */
  public MonthViewPanel(SwingGuiView parentView, ControllerFeatures features) {
    this.parentView = parentView;
    this.features = features;
    setLayout(new GridLayout(0, 7));
  }

  /**
   * Updates the calendar grid for the specified month.
   *
   * @param currentMonth A date representing the month to display
   * @param events       The full list of events to filter and display on the grid.
   */
  protected void update(LocalDate currentMonth, List<Event> events) {
    removeAll();
    addHeaders();
    addEmptySlots(currentMonth);
    addDayCells(currentMonth, events);
    revalidate();
    repaint();
  }

  private void addHeaders() {
    String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    for (String d : days) {
      JLabel lbl = new JLabel(d, SwingConstants.CENTER);
      lbl.setBorder(BorderFactory.createEtchedBorder());
      add(lbl);
    }
  }

  private void addEmptySlots(LocalDate currentMonth) {
    YearMonth ym = YearMonth.from(currentMonth);
    LocalDate firstOfMonth = ym.atDay(1);
    int startDayIndex = firstOfMonth.getDayOfWeek().getValue();
    for (int i = 1; i < startDayIndex; i++) {
      add(new JLabel(""));
    }
  }

  private void addDayCells(LocalDate currentMonth, List<Event> events) {
    YearMonth ym = YearMonth.from(currentMonth);
    for (int day = 1; day <= ym.lengthOfMonth(); day++) {
      add(createDayCell(ym.atDay(day), events));
    }
  }

  private JPanel createDayCell(LocalDate date, List<Event> allEvents) {
    JPanel cell = new JPanel(new BorderLayout());
    cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    cell.setBackground(Color.WHITE);
    cell.add(createDayButton(date), BorderLayout.NORTH);
    cell.add(createEventsPreview(date, allEvents), BorderLayout.CENTER);
    return cell;
  }

  private JButton createDayButton(LocalDate date) {
    JButton btn = new JButton(String.valueOf(date.getDayOfMonth()));
    btn.setHorizontalAlignment(SwingConstants.LEFT);
    btn.setBorderPainted(false);
    btn.setOpaque(false);
    btn.addActionListener(e -> {
      if (parentView.ensureCalendarSelected()) {
        features.selectDate(date);
        features.setViewMode(ViewMode.DAY);
      }
    });
    return btn;
  }

  private JPanel createEventsPreview(LocalDate date, List<Event> allEvents) {
    JPanel preview = new JPanel();
    preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
    preview.setOpaque(false);
    preview.setBorder(new EmptyBorder(2, 2, 2, 2));

    List<Event> todays = filterEventsForDate(date, allEvents);
    if (!todays.isEmpty()) {
      addEventLabels(preview, todays);
    }
    return preview;
  }

  private List<Event> filterEventsForDate(LocalDate date, List<Event> events) {
    return events.stream()
        .filter(e -> !e.getStart().toLocalDate().isAfter(date)
            && !e.getEnd().toLocalDate().isBefore(date))
        .collect(Collectors.toList());
  }

  private void addEventLabels(JPanel preview, List<Event> todays) {
    for (int k = 0; k < Math.min(todays.size(), 3); k++) {
      Event evt = todays.get(k);
      JLabel box = parentView.createEventBox(evt.getSubject());
      box.addMouseListener(new EventClickHandler(evt));
      preview.add(box);
      preview.add(Box.createVerticalStrut(2));
    }
    if (todays.size() > 3) {
      preview.add(new JLabel("..."));
    }
  }

  private class EventClickHandler extends MouseAdapter {
    private final Event event;

    EventClickHandler(Event event) {
      this.event = event;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      parentView.showEventDetailsDialog(event);
    }
  }
}