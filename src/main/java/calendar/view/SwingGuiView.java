package calendar.view;

import calendar.controller.ControllerFeatures;
import calendar.model.Event;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * The main window for the Virtual Calendar application.
 * It organizes the screen into three main areas:
 * 1. Top Panel: Calendar management (Create/Edit/Delete/Switch) and Export.
 * 2. Center Panel: The main display that switches between Month, Week, and Day views.
 * 3. Bottom Panel: Quick access to create new events.
 * It delegates the rendering of the calendar grids to specialized panels
 * (MonthViewPanel, WeekViewPanel, DayViewPanel).
 */
public class SwingGuiView extends JFrame implements GuiView {

  private final ControllerFeatures features;
  private JComboBox<String> calendarSelector;
  private ActionListener calendarSelectorListener;
  private JLabel dateRangeLabel;
  private CardLayout cardLayout;
  private JPanel centerContainer;
  private MonthViewPanel monthView;
  private WeekViewPanel weekView;
  private DayViewPanel dayView;
  private JToggleButton btnMonthView;
  private JToggleButton btnWeekView;
  private JToggleButton btnDayView;

  private static final Color EVENT_BG_COLOR = new Color(220, 240, 255);
  private static final Color EVENT_BORDER_COLOR = new Color(100, 150, 200);

  private static final DateTimeFormatter DETAILS_DATE_FMT =
      DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
  private static final DateTimeFormatter DETAILS_TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

  /**
   * Constructs the main application window.
   *
   * @param features The controller interface for handling user actions.
   */
  public SwingGuiView(ControllerFeatures features) {
    this.features = features;
    setTitle("Virtual Calendar");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1100, 800);
    setLayout(new BorderLayout());

    initPanels();
    add(createTopBar(), BorderLayout.NORTH);
    add(createCenterWrapper(), BorderLayout.CENTER);
    add(createBottomPanel(), BorderLayout.SOUTH);
  }

  private void initPanels() {
    monthView = new MonthViewPanel(this, features);
    weekView = new WeekViewPanel(this, features);
    dayView = new DayViewPanel(this, features);
    btnMonthView = new JToggleButton("Month", true);
    btnWeekView = new JToggleButton("Week");
    btnDayView = new JToggleButton("Day");
    dateRangeLabel = new JLabel("Date Range");
    dateRangeLabel.setFont(dateRangeLabel.getFont().deriveFont(18f));
  }

  private JPanel createTopBar() {
    JPanel topBar = new JPanel(new BorderLayout());
    topBar.add(createCalendarManagementPanel(), BorderLayout.WEST);
    topBar.add(createActionPanel(), BorderLayout.EAST);
    return topBar;
  }

  private JPanel createCenterWrapper() {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(createViewControlPanel(), BorderLayout.NORTH);
    wrapper.add(createCardsPanel(), BorderLayout.CENTER);
    return wrapper;
  }

  private JPanel createCalendarManagementPanel() {
    calendarSelector = new JComboBox<>();

    calendarSelectorListener = e -> {
      String sel = (String) calendarSelector.getSelectedItem();
      if (sel != null) {
        try {
          features.switchCalendar(sel);
        } catch (Exception ex) {
          showError(ex.getMessage());
        }
      }
    };
    calendarSelector.addActionListener(calendarSelectorListener);

    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
    p.add(new JLabel("Calendar:"));
    p.add(calendarSelector);
    p.add(createButton("New Cal", this::showCreateCalendarDialog));
    p.add(createButton("Edit Cal", () -> {
      if (ensureCalendarSelected()) {
        showEditCalendarDialog();
      }
    }));
    p.add(createButton("Delete Cal", () -> {
      if (ensureCalendarSelected()) {
        deleteCalendarAction();
      }
    }));
    return p;
  }

  private JPanel createActionPanel() {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    p.add(createButton("Search and Edit", () -> {
      if (ensureCalendarSelected()) {
        new SearchAndEditDialog(this, features).setVisible(true);
      }
    }));

    p.add(createButton("Export Calendar", () -> {
      if (ensureCalendarSelected()) {
        showExportDialog();
      }
    }));
    return p;
  }

  private JButton createButton(String text, Runnable action) {
    JButton b = new JButton(text);
    b.addActionListener(e -> action.run());
    return b;
  }

  private JPanel createViewControlPanel() {
    JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER));
    nav.add(createButton("<", () -> {
      if (ensureCalendarSelected()) {
        features.changeDateRange(-1);
      }
    }));
    nav.add(dateRangeLabel);
    nav.add(createButton(">", () -> {
      if (ensureCalendarSelected()) {
        features.changeDateRange(1);
      }
    }));

    JPanel mode = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    ButtonGroup bg = new ButtonGroup();
    configureToggle(btnMonthView, ViewMode.MONTH, bg, mode);
    configureToggle(btnWeekView, ViewMode.WEEK, bg, mode);
    configureToggle(btnDayView, ViewMode.DAY, bg, mode);

    JPanel p = new JPanel(new BorderLayout());
    p.add(nav, BorderLayout.CENTER);
    p.add(mode, BorderLayout.EAST);
    return p;
  }

  private void configureToggle(JToggleButton btn, ViewMode m, ButtonGroup bg, JPanel p) {
    btn.setPreferredSize(new Dimension(100, 30));
    bg.add(btn);
    p.add(btn);
    btn.addActionListener(e -> {
      if (ensureCalendarSelected()) {
        features.setViewMode(m);
      }
    });
  }

  private JPanel createCardsPanel() {
    cardLayout = new CardLayout();
    centerContainer = new JPanel(cardLayout);
    centerContainer.add(monthView, ViewMode.MONTH.name());
    centerContainer.add(weekView, ViewMode.WEEK.name());
    centerContainer.add(dayView, ViewMode.DAY.name());
    return centerContainer;
  }

  private JPanel createBottomPanel() {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JButton btn = new JButton("Create New Event");
    btn.setFont(btn.getFont().deriveFont(16f));
    btn.addActionListener(e -> {
      if (ensureCalendarSelected()) {
        new EventDialog(this, features, LocalDate.now()).setVisible(true);
      }
    });
    p.add(btn);
    return p;
  }

  /**
   * Checks if a calendar is currently selected in the dropdown.
   * If not, shows an error message to the user.
   *
   * @return true if a calendar is selected, false otherwise.
   */
  protected boolean ensureCalendarSelected() {
    if (calendarSelector.getSelectedItem() == null) {
      showError("No active calendar selected.");
      return false;
    }
    return true;
  }

  @Override
  public void setCalendarList(Set<String> names, String active) {
    calendarSelector.removeActionListener(calendarSelectorListener);
    calendarSelector.removeAllItems();
    for (String n : names) {
      calendarSelector.addItem(n);
    }
    calendarSelector.setSelectedItem(active);
    calendarSelector.addActionListener(calendarSelectorListener);
  }

  @Override
  public void setViewMode(ViewMode mode) {
    cardLayout.show(centerContainer, mode.name());
    if (mode == ViewMode.MONTH) {
      btnMonthView.setSelected(true);
    } else if (mode == ViewMode.WEEK) {
      btnWeekView.setSelected(true);
    } else {
      btnDayView.setSelected(true);
    }
  }

  @Override
  public void updateMonthView(LocalDate date, List<Event> events) {
    dateRangeLabel.setText(date.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    monthView.update(date, events);
  }

  @Override
  public void updateWeekView(LocalDate date, List<Event> events) {
    dateRangeLabel.setText("Week of " + date.format(DateTimeFormatter.ofPattern("MMM dd")));
    weekView.update(date, events);
  }

  @Override
  public void updateDayView(LocalDate date, List<Event> events) {
    dateRangeLabel.setText(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
    dayView.update(date, events);
  }

  /**
   * Helper method to create a label for an event.
   *
   * @param text The text to display on the event label.
   * @return The JLabel to be added to the UI.
   */
  protected JLabel createEventBox(String text) {
    JLabel lbl = new JLabel(text);
    lbl.setOpaque(true);
    lbl.setBackground(EVENT_BG_COLOR);
    lbl.setForeground(Color.BLACK);
    lbl.setFont(lbl.getFont().deriveFont(11f));
    lbl.setBorder(
        new CompoundBorder(new LineBorder(EVENT_BORDER_COLOR, 1), new EmptyBorder(3, 5, 3, 5)));
    lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    return lbl;
  }

  /**
   * Opens a popup dialog showing the full details of an event.
   *
   * @param event The event to display.
   */
  protected void showEventDetailsDialog(Event event) {
    JDialog d = new JDialog(this, "Event Details", true);
    d.setLayout(new BorderLayout());
    d.setSize(400, 300);
    d.add(createDetailsContent(event), BorderLayout.CENTER);
    d.add(createDetailsButtons(d, event), BorderLayout.SOUTH);
    d.setLocationRelativeTo(this);
    d.setVisible(true);
  }

  private JScrollPane createDetailsContent(Event event) {
    JPanel content = new JPanel();
    final String startStr = event.getStart().format(DETAILS_DATE_FMT)
        + " at "
        + event.getStart().format(DETAILS_TIME_FMT);
    final String endStr = event.getEnd().format(DETAILS_DATE_FMT)
        + " at "
        + event.getEnd().format(DETAILS_TIME_FMT);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    addDetail(content, "Subject: ", event.getSubject());
    addDetail(content, "Start: ", startStr);
    addDetail(content, "End: ", endStr);
    addDetail(content, "Location: ", event.getLocation());
    addDetail(content, "Status: ", event.isPrivate() ? "Private" : "Public");
    addDetail(content, "Description: ", event.getDescription());
    return new JScrollPane(content);
  }

  private void addDetail(JPanel p, String label, String val) {
    JPanel row = new JPanel(new BorderLayout());
    row.setOpaque(false);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    JLabel l = new JLabel(label);
    l.setFont(l.getFont().deriveFont(Font.BOLD));
    row.add(l, BorderLayout.WEST);
    row.add(new JLabel(val), BorderLayout.CENTER);
    p.add(row);
    p.add(Box.createVerticalStrut(5));
  }

  private JPanel createDetailsButtons(JDialog d, Event event) {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    p.add(createButton("Edit", () -> {
      d.dispose();
      promptAndEdit(event);
    }));
    p.add(createButton("Close", d::dispose));
    return p;
  }

  private void promptAndEdit(Event event) {
    calendar.model.EditScope scope = calendar.model.EditScope.SINGLE;
    if (features.isEventPartofSeries(event)) {
      String[] options = {"Single Event", "Future Events", "All Events"};
      int choice =
          JOptionPane.showOptionDialog(this, "This is a repeating event. Edit:", "Edit Series",
              JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
      if (choice == 0) {
        scope = calendar.model.EditScope.SINGLE;
      } else if (choice == 1) {
        scope = calendar.model.EditScope.FUTURE;
      } else if (choice == 2) {
        scope = calendar.model.EditScope.ALL;
      } else {
        return;
      }
    }
    new EventDialog(this, features, event, scope).setVisible(true);
  }

  private void showExportDialog() {
    JFileChooser fc = new JFileChooser();
    if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      String[] ops = {"CSV", "iCal"};
      int fmt =
          JOptionPane.showOptionDialog(this, "Select Format", "Export", JOptionPane.YES_NO_OPTION,
              JOptionPane.INFORMATION_MESSAGE, null, ops, ops[0]);
      if (fmt != -1) {
        features.exportCalendar(fc.getSelectedFile(), ops[fmt]);
      }
    }
  }

  private void deleteCalendarAction() {
    String cur = (String) calendarSelector.getSelectedItem();
    if (cur != null
        && JOptionPane.showConfirmDialog(this, "Delete " + cur + "?", "Confirm",
        JOptionPane.YES_NO_OPTION) == 0) {
      try {
        features.deleteCalendar(cur);
      } catch (Exception e) {
        showError(e.getMessage());
      }
    }
  }

  private void showEditCalendarDialog() {
    String currentName = (String) calendarSelector.getSelectedItem();
    if (currentName == null) {
      return;
    }
    ZoneId currentZone = features.getZoneId(currentName);
    promptCalendar("Edit Calendar", currentName, currentZone.toString(), (n, z) -> {
      try {
        features.editCalendar(n, ZoneId.of(z));
      } catch (Exception e) {
        showError(e.getMessage());
      }
    });
  }

  private void showCreateCalendarDialog() {
    promptCalendar("New Calendar", "", ZoneId.systemDefault().toString(), (n, z) -> {
      try {
        features.createCalendar(n, ZoneId.of(z));
      } catch (Exception e) {
        showError(e.getMessage());
      }
    });
  }

  private void promptCalendar(String title, String defaultName, String defaultZone,
                              java.util.function.BiConsumer<String, String> action) {
    JTextField n = new JTextField(defaultName);
    JTextField z = new JTextField(defaultZone);
    Object[] content = {"Name:", n, "Timezone:", z};
    if (JOptionPane.showConfirmDialog(this, content, title, JOptionPane.OK_CANCEL_OPTION)
        == JOptionPane.OK_OPTION) {
      action.accept(n.getText(), z.getText());
    }
  }

  @Override
  public void showError(String m) {
    JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void showMessage(String m) {
    JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
  }
}