package calendar.view;

import calendar.controller.ControllerFeatures;
import calendar.model.Event;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

/**
 * Renders the detailed Day View of the calendar.
 * It creates a focused view for managing a day.
 */
public class DayViewPanel extends JPanel {
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  private final ControllerFeatures features;
  private final SwingGuiView parentView;

  /**
   * Sets up the panel layout.
   *
   * @param parentView The main window.
   * @param features   The controller for handling user actions.
   */
  public DayViewPanel(SwingGuiView parentView, ControllerFeatures features) {
    this.parentView = parentView;
    this.features = features;
    setLayout(new BorderLayout());
  }

  /**
   * Refreshes the panel to show the schedule for the given date.
   * This clears the current list and rebuilds it with the provided events.
   *
   * @param date   The date being displayed.
   * @param events The list of events occurring on this date.
   */
  protected void update(LocalDate date, List<Event> events) {
    removeAll();
    add(createEventList(events), BorderLayout.CENTER);
    add(createAddButton(date), BorderLayout.SOUTH);
    revalidate();
    repaint();
  }

  private JScrollPane createEventList(List<Event> events) {
    DefaultListModel<Event> model = new DefaultListModel<>();
    events.forEach(model::addElement);
    JList<Event> list = new JList<>(model);
    list.setCellRenderer(new DetailedEventRenderer());
    list.addMouseListener(new ListClickHandler(list));
    return new JScrollPane(list);
  }

  private class ListClickHandler extends MouseAdapter {
    private final JList<Event> list;

    ListClickHandler(JList<Event> list) {
      this.list = list;
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
      if (evt.getClickCount() == 2) {
        Event selected = list.getSelectedValue();
        if (selected != null) {
          parentView.showEventDetailsDialog(selected);
        }
      }
    }
  }

  private JButton createAddButton(LocalDate date) {
    JButton addBtn = new JButton("Add Event on " + date.toString());
    addBtn.addActionListener(e -> {
      if (parentView.ensureCalendarSelected()) {
        new EventDialog(parentView, features, date).setVisible(true);
      }
    });
    return addBtn;
  }

  private static class DetailedEventRenderer extends JPanel implements ListCellRenderer<Event> {
    private final JLabel subj = new JLabel();
    private final JLabel time = new JLabel();
    private final JLabel desc = new JLabel();

    public DetailedEventRenderer() {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
      JPanel top = new JPanel(new BorderLayout());
      subj.setFont(subj.getFont().deriveFont(Font.BOLD));
      top.add(subj, BorderLayout.CENTER);
      top.add(time, BorderLayout.EAST);
      top.setOpaque(false);
      add(top, BorderLayout.NORTH);
      desc.setFont(desc.getFont().deriveFont(Font.ITALIC));
      desc.setForeground(Color.DARK_GRAY);
      add(desc, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Event> list, Event value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      subj.setText(value.getSubject() + (value.isPrivate() ? " [Private]" : ""));
      String startStr = value.getStart().toLocalTime().format(TIME_FORMAT);
      String endStr = value.getEnd().toLocalTime().format(TIME_FORMAT);
      time.setText(value.isAllDay() ? "All Day" : startStr + " - " + endStr);
      desc.setText("  " + value.getDescription()
          + (value.getLocation().isEmpty() ? "" : " @ " + value.getLocation()));
      setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
      setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
      return this;
    }
  }
}