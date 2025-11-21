package calendar.view;

import calendar.controller.ControllerFeatures;
import calendar.model.EditScope;
import calendar.model.Event;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A popup form that handles both creating new events and editing existing ones.
 * This dialog manages the form fields for event details like subject, time, location.
 */
public class EventDialog extends JDialog {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private final ControllerFeatures features;
  private final Event eventToEdit;
  private final LocalDate defaultDate;
  private final EditScope editScope; // New field

  private JTextField startDateField;
  private JTextField endDateField;
  private JTextField subjectField;
  private JTextField locationField;
  private JTextField timeStartField;
  private JTextField timeEndField;
  private JTextField repeatValueField;
  private JTextArea descArea;
  private JComboBox<String> privacyBox;
  private JComboBox<String> repeatTypeBox;
  private JCheckBox cbM;
  private JCheckBox cbT;
  private JCheckBox cbW;
  private JCheckBox cbR;
  private JCheckBox cbF;
  private JCheckBox cbS;
  private JCheckBox cbU;

  /**
   * Opens the dialog in "Create Mode".
   * Use this constructor when the user wants to make a new event.
   *
   * @param parent      The main application window.
   * @param features    The controller to call when the user hits "Save".
   * @param defaultDate The date to pre-fill in the date input fields.
   */
  public EventDialog(JFrame parent, ControllerFeatures features, LocalDate defaultDate) {
    this(parent, features, null, defaultDate, null);
  }

  /**
   * Opens the dialog in "Edit Mode" for a specific event.
   * Use this when modifying an existing event.
   *
   * @param parent      The main application window.
   * @param features    The controller to call when the user hits "Save".
   * @param eventToEdit The existing event object to modify.
   * @param scope       How the edit should be applied (Single, Future, or All).
   */
  public EventDialog(JFrame parent, ControllerFeatures features, Event eventToEdit,
                     EditScope scope) {
    this(parent, features, eventToEdit, null, scope);
  }

  private EventDialog(JFrame parent, ControllerFeatures features, Event eventToEdit,
                      LocalDate defaultDate, EditScope scope) {
    super(parent, eventToEdit == null ? "Create Event" : "Edit Event", true);
    this.features = features;
    this.eventToEdit = eventToEdit;
    this.defaultDate = defaultDate;
    this.editScope = scope;

    initializeui();
    if (eventToEdit != null) {
      prefillData();
    }
    pack();
    setLocationRelativeTo(parent);
  }

  private void initializeui() {
    setLayout(new BorderLayout());
    JPanel form = createFormPanel();
    add(form, BorderLayout.CENTER);
    add(createButtonPanel(), BorderLayout.SOUTH);
  }

  private JPanel createFormPanel() {
    JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
    form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    initBasicFields();
    initTimeFields();
    initRecurrenceFields();
    addFieldsToForm(form);
    return form;
  }

  private void initBasicFields() {
    String dateStr = defaultDate != null ? defaultDate.toString() : "";
    startDateField = new JTextField(dateStr);
    endDateField = new JTextField(dateStr);
    subjectField = new JTextField();
    descArea = new JTextArea(3, 20);
    locationField = new JTextField();
    privacyBox = new JComboBox<>(new String[] {"", "Public", "Private"});
  }

  private void initTimeFields() {
    timeStartField = new JTextField("");
    timeEndField = new JTextField("");
  }

  private void initRecurrenceFields() {
    cbM = new JCheckBox("M");
    cbT = new JCheckBox("T");
    cbW = new JCheckBox("W");
    cbR = new JCheckBox("Th");
    cbF = new JCheckBox("F");
    cbS = new JCheckBox("Sa");
    cbU = new JCheckBox("Su");
    repeatTypeBox = new JComboBox<>(new String[] {"None", "Count", "Until"});
    repeatValueField = new JTextField();
  }

  private void addFieldsToForm(JPanel form) {
    form.add(new JLabel("Start Date (YYYY-MM-DD):"));
    form.add(startDateField);
    form.add(new JLabel("End Date (YYYY-MM-DD):"));
    form.add(endDateField);
    form.add(new JLabel("Subject:"));
    form.add(subjectField);
    form.add(new JLabel("Description:"));
    form.add(new JScrollPane(descArea));
    form.add(new JLabel("Status:"));
    form.add(privacyBox);
    form.add(new JLabel("Location:"));
    form.add(locationField);
    form.add(new JLabel("Start Time (HH:mm):"));
    form.add(timeStartField);
    form.add(new JLabel("End Time (HH:mm):"));
    form.add(timeEndField);

    if (eventToEdit == null) {
      form.add(new JLabel("Repeat Days:"));
      form.add(createDaysPanel());
      form.add(new JLabel("Repeat Type:"));
      form.add(repeatTypeBox);
      form.add(new JLabel("Value:"));
      form.add(repeatValueField);
    }
  }

  private JPanel createDaysPanel() {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    p.add(cbM);
    p.add(cbT);
    p.add(cbW);
    p.add(cbR);
    p.add(cbF);
    p.add(cbS);
    p.add(cbU);
    return p;
  }

  private JPanel createButtonPanel() {
    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton btnSave = new JButton("Save");
    JButton btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(e -> dispose());
    btnSave.addActionListener(e -> onSave());
    btnPanel.add(btnCancel);
    btnPanel.add(btnSave);
    return btnPanel;
  }

  private void prefillData() {
    startDateField.setText(eventToEdit.getStart().toLocalDate().toString());
    endDateField.setText(eventToEdit.getEnd().toLocalDate().toString());
    subjectField.setText(eventToEdit.getSubject());
    descArea.setText(eventToEdit.getDescription());
    locationField.setText(eventToEdit.getLocation());
    privacyBox.setSelectedItem(eventToEdit.isPrivate() ? "Private" : "Public");

    if (!eventToEdit.isAllDay()) {
      timeStartField.setText(eventToEdit.getStart().toLocalTime().format(TIME_FORMAT));
      timeEndField.setText(eventToEdit.getEnd().toLocalTime().format(TIME_FORMAT));
    } else {
      timeStartField.setText("");
      timeEndField.setText("");
    }
  }

  private void onSave() {
    try {
      validateRecurrence();
      Event.EventBuilder builder = buildEventFromInput();

      if (eventToEdit == null) {
        String repeatTypeBoxSelectedItem = (String) repeatTypeBox.getSelectedItem();
        features.createEvent(builder, getRecurrenceString(),
            "None".equals(repeatTypeBoxSelectedItem) ? null : repeatTypeBoxSelectedItem,
            repeatValueField.getText());
      } else {
        features.editEvent(eventToEdit, builder, editScope != null ? editScope : EditScope.SINGLE);
      }
      dispose();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void validateRecurrence() throws Exception {
    if (!getRecurrenceString().isEmpty() && eventToEdit == null) {
      String repeatTypeBoxSelectedItem = (String) repeatTypeBox.getSelectedItem();
      String repeatValueFieldText = repeatValueField.getText();
      if ("None".equals(repeatTypeBoxSelectedItem) || repeatValueFieldText == null
          || repeatValueFieldText.trim().isEmpty()) {
        throw new Exception("Please specify Repeat Type and Value for repeating events.");
      }
      if ("Count".equals(repeatTypeBoxSelectedItem)) {
        try {
          Integer.parseInt(repeatValueFieldText);
        } catch (NumberFormatException e) {
          throw new Exception("Repeat Value must be a valid number for 'Count' type.");
        }
      }
    }
  }

  private Event.EventBuilder buildEventFromInput() {
    Event.EventBuilder builder = Event.builder();
    if (eventToEdit != null) {
      builder.fromEvent(eventToEdit);
    }

    String sub = subjectField.getText().trim();
    if (sub.isEmpty()) {
      throw new RuntimeException("Subject cannot be empty.");
    }

    builder.setSubject(sub);
    builder.setDescription(descArea.getText());
    builder.setLocation(locationField.getText());
    builder.setPrivate("Private".equals(privacyBox.getSelectedItem()));

    setDates(builder);
    return builder;
  }

  private void setDates(Event.EventBuilder builder) {
    String sdatetext = startDateField.getText().trim();
    if (sdatetext.isEmpty()) {
      throw new RuntimeException("Start Date cannot be empty.");
    }

    LocalDate sdate;
    try {
      sdate = LocalDate.parse(sdatetext, DATE_FORMAT);
    } catch (DateTimeParseException e) {
      throw new RuntimeException("Invalid Start Date format. Use YYYY-MM-DD.");
    }

    String stime = timeStartField.getText().trim();
    String etime = timeEndField.getText().trim();
    boolean isAllDay = stime.isEmpty() && etime.isEmpty();
    builder.setAllDay(isAllDay);

    if (isAllDay) {
      builder.setStart(sdate.atTime(8, 0));
      builder.setEnd(sdate.atTime(17, 0));
    } else {
      if (stime.isEmpty() || etime.isEmpty()) {
        throw new RuntimeException(
            "For a timed event, both Start Time and End Time must be provided.");
      }
      String edatetext = endDateField.getText().trim();
      LocalDate edate;
      if (edatetext.isEmpty()) {
        edate = sdate;
      } else {
        try {
          edate = LocalDate.parse(edatetext, DATE_FORMAT);
        } catch (DateTimeParseException e) {
          throw new RuntimeException("Invalid End Date format. Use YYYY-MM-DD.");
        }
      }
      try {
        builder.setStart(LocalDateTime.of(sdate, LocalTime.parse(stime, TIME_FORMAT)));
        builder.setEnd(LocalDateTime.of(edate, LocalTime.parse(etime, TIME_FORMAT)));
      } catch (DateTimeParseException e) {
        throw new RuntimeException("Invalid Time format. Use HH:mm.");
      }
    }
  }

  private String getRecurrenceString() {
    Map<JCheckBox, String> dayMap = new LinkedHashMap<>();
    dayMap.put(cbM, "M");
    dayMap.put(cbT, "T");
    dayMap.put(cbW, "W");
    dayMap.put(cbR, "R");
    dayMap.put(cbF, "F");
    dayMap.put(cbS, "S");
    dayMap.put(cbU, "U");

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<JCheckBox, String> entry : dayMap.entrySet()) {
      if (entry.getKey().isSelected()) {
        sb.append(entry.getValue());
      }
    }
    return sb.toString();
  }
}