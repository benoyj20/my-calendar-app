package calendar.view;

import calendar.controller.ControllerFeatures;
import calendar.model.Event;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Renders the calendar in a Week View.
 * Each column represents a single day, starting from Monday.
 */
public class WeekViewPanel extends JPanel {
  private final ControllerFeatures features;
  private final SwingGuiView parentView;

  /**
   * Sets up the panel structure for showing the weekly view.
   *
   * @param parentView The main window.
   * @param features   The controller to handle actions like adding new events.
   */
  public WeekViewPanel(SwingGuiView parentView, ControllerFeatures features) {
    this.parentView = parentView;
    this.features = features;
    setLayout(new GridLayout(1, 7));
  }

  /**
   * Refreshes the current display and rebuilds the 7 daily columns for the specified week.
   *
   * @param startOfWeek The date of the first day to display.
   * @param events      The list of all events in the active calendar.
   */
  protected void update(LocalDate startOfWeek, List<Event> events) {
    removeAll();
    for (int i = 0; i < 7; i++) {
      add(createDayColumn(startOfWeek.plusDays(i), events));
    }
    revalidate();
    repaint();
  }

  private JPanel createDayColumn(LocalDate date, List<Event> events) {
    JPanel col = new JPanel(new BorderLayout());
    col.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    col.add(createHeader(date), BorderLayout.NORTH);
    col.add(createEventListPanel(date, events), BorderLayout.CENTER);
    col.add(createAddButton(date), BorderLayout.SOUTH);
    return col;
  }

  private JLabel createHeader(LocalDate date) {
    JLabel header =
        new JLabel(date.format(DateTimeFormatter.ofPattern("EEE dd")), SwingConstants.CENTER);
    header.setOpaque(true);
    header.setBackground(new Color(230, 230, 230));
    return header;
  }

  private JButton createAddButton(LocalDate date) {
    JButton addBtn = new JButton("+");
    addBtn.addActionListener(e -> {
      if (parentView.ensureCalendarSelected()) {
        new EventDialog(parentView, features, date).setVisible(true);
      }
    });
    return addBtn;
  }

  private JScrollPane createEventListPanel(LocalDate date, List<Event> events) {
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    listPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

    List<Event> todays = filterEvents(date, events);
    for (Event e : todays) {
      listPanel.add(createEventLabel(e));
      listPanel.add(Box.createVerticalStrut(5));
    }
    return new JScrollPane(listPanel);
  }

  private List<Event> filterEvents(LocalDate date, List<Event> events) {
    return events.stream()
        .filter(e -> !e.getStart().toLocalDate().isAfter(date)
            && !e.getEnd().toLocalDate().isBefore(date))
        .sorted(Comparator.comparing(Event::getStart))
        .collect(Collectors.toList());
  }

  private JLabel createEventLabel(Event e) {
    JLabel el = parentView.createEventBox(e.getSubject());
    el.setToolTipText(e.getDescription());
    el.addMouseListener(new EventClickHandler(e));
    return el;
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