package calendar.view;

import calendar.controller.ControllerFeatures;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This is the search and edit dialog window to search multiple
 * events and edit them together.
 *
 */
public class SearchAndEditDialog extends JDialog {

  private final ControllerFeatures features;
  private JComboBox<String> searchTypeBox;
  private JPanel inputPanel;
  private CardLayout inputLayout;

  private JTextField subjectField;

  private JTextField startDateField;
  private JTextField startTimeField;
  private JTextField endDateField;
  private JTextField endTimeField;

  private JComboBox<String> propertyBox;

  private JPanel valuePanel;
  private CardLayout valueLayout;
  private JTextField textValueField;
  private JTextField dateValueField;
  private JTextField timeValueField;
  private JComboBox<String> statusValueBox;

  private static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 14);
  private static final Dimension BIG_FIELD_DIM = new Dimension(250, 30);
  private static final Dimension DATE_FIELD_DIM = new Dimension(140, 30);
  private static final Dimension TIME_FIELD_DIM = new Dimension(80, 30);

  /**
   * Initializes the search and edit dialog window with all form components.
   *
   * @param parent   The parent frame to center this dialog over.
   * @param features The controller interface for performing the bulk update.
   */
  public SearchAndEditDialog(JFrame parent, ControllerFeatures features) {
    super(parent, "Search and Edit", true);
    this.features = features;
    setSize(500, 550);
    setLayout(new BorderLayout());
    setLocationRelativeTo(parent);

    add(createFormPanel(), BorderLayout.CENTER);
    add(createButtonPanel(), BorderLayout.SOUTH);
  }

  private JPanel createFormPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    mainPanel.add(createSearchSection());
    mainPanel.add(createModificationSection());

    return mainPanel;
  }

  private JPanel createSearchSection() {
    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    container.add(createHeader("1. Search Criteria"));
    container.add(createSearchTypeRow());
    container.add(Box.createVerticalStrut(10));
    container.add(createSearchParametersCard());
    container.add(Box.createVerticalStrut(20));
    return container;
  }

  private JPanel createSearchTypeRow() {
    JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    typePanel.add(new JLabel("Search By:  "));
    searchTypeBox = new JComboBox<>(new String[] {"Subject", "Time Range"});
    searchTypeBox.setFont(INPUT_FONT);
    typePanel.add(searchTypeBox);

    searchTypeBox.addActionListener(e ->
        inputLayout.show(inputPanel, (String) searchTypeBox.getSelectedItem()));
    return typePanel;
  }

  private JPanel createSearchParametersCard() {
    inputLayout = new CardLayout();
    inputPanel = new JPanel(inputLayout);
    inputPanel.setBorder(BorderFactory.createTitledBorder("Search Parameters"));

    inputPanel.add(createSubjectPanel(), "Subject");
    inputPanel.add(createTimePanel(), "Time Range");
    return inputPanel;
  }

  private JPanel createModificationSection() {
    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    container.add(createHeader("2. Modification"));
    container.add(createModificationGrid());
    return container;
  }

  private JPanel createModificationGrid() {
    JPanel modPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = createGbc();

    addPropertyRow(modPanel, gbc);
    addValueRow(modPanel, gbc);

    return modPanel;
  }

  private GridBagConstraints createGbc() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    return gbc;
  }

  private void addPropertyRow(JPanel panel, GridBagConstraints gbc) {
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(new JLabel("Property to Set:"), gbc);

    gbc.gridx = 1;
    String[] props = {
        "Subject", "Description", "Location", "Status",
        "Start Date", "Start Time", "End Date", "End Time"
    };
    propertyBox = new JComboBox<>(props);
    propertyBox.setFont(INPUT_FONT);
    propertyBox.setPreferredSize(BIG_FIELD_DIM);
    propertyBox.addActionListener(e -> updateValueInputType());
    panel.add(propertyBox, gbc);
  }

  private void addValueRow(JPanel panel, GridBagConstraints gbc) {
    gbc.gridx = 0;
    gbc.gridy = 1;
    panel.add(new JLabel("New Value:"), gbc);

    gbc.gridx = 1;
    createValueInputPanel();
    panel.add(valuePanel, gbc);
  }

  private void createValueInputPanel() {
    valueLayout = new CardLayout();
    valuePanel = new JPanel(valueLayout);

    addTextFieldToPanel();
    addDateFieldToPanel();
    addTimeFieldToPanel();
    addStatusFieldToPanel();
  }

  private void addTextFieldToPanel() {
    textValueField = new JTextField();
    textValueField.setFont(INPUT_FONT);
    textValueField.setPreferredSize(BIG_FIELD_DIM);
    valuePanel.add(textValueField, "TEXT");
  }

  private void addDateFieldToPanel() {
    dateValueField = new JTextField(LocalDate.now().toString());
    dateValueField.setFont(INPUT_FONT);
    dateValueField.setPreferredSize(BIG_FIELD_DIM);
    dateValueField.setToolTipText("YYYY-MM-DD");
    valuePanel.add(dateValueField, "DATE");
  }

  private void addTimeFieldToPanel() {
    timeValueField = new JTextField("09:00");
    timeValueField.setFont(INPUT_FONT);
    timeValueField.setPreferredSize(BIG_FIELD_DIM);
    timeValueField.setToolTipText("HH:mm");
    valuePanel.add(timeValueField, "TIME");
  }

  private void addStatusFieldToPanel() {
    statusValueBox = new JComboBox<>(new String[] {"Public", "Private"});
    statusValueBox.setFont(INPUT_FONT);
    statusValueBox.setPreferredSize(BIG_FIELD_DIM);
    valuePanel.add(statusValueBox, "STATUS");
  }

  private void updateValueInputType() {
    String p = (String) propertyBox.getSelectedItem();
    if (p == null) {
      return;
    }
    if (p.contains("Date")) {
      valueLayout.show(valuePanel, "DATE");
    } else if (p.contains("Time")) {
      valueLayout.show(valuePanel, "TIME");
    } else if (p.equals("Status")) {
      valueLayout.show(valuePanel, "STATUS");
    } else {
      valueLayout.show(valuePanel, "TEXT");
    }
  }

  private JLabel createHeader(String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("SansSerif", Font.BOLD, 16));
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    return label;
  }

  private JPanel createSubjectPanel() {
    subjectField = new JTextField(20);
    subjectField.setFont(INPUT_FONT);
    subjectField.setPreferredSize(BIG_FIELD_DIM);

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.add(new JLabel("Subject Contains: "));
    panel.add(subjectField);
    return panel;
  }

  private JPanel createTimePanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    addTimeRow(panel, gbc, 0, "From:",
        startDateField = createDateInput(), startTimeField = createTimeInput("09:00"));
    addTimeRow(panel, gbc, 1, "To:",
        endDateField = createDateInput(), endTimeField = createTimeInput("17:00"));

    return panel;
  }

  private JTextField createDateInput() {
    JTextField field = new JTextField(LocalDate.now().toString());
    field.setFont(INPUT_FONT);
    field.setPreferredSize(DATE_FIELD_DIM);
    return field;
  }

  private JTextField createTimeInput(String defaultTime) {
    JTextField field = new JTextField(defaultTime);
    field.setFont(INPUT_FONT);
    field.setPreferredSize(TIME_FIELD_DIM);
    return field;
  }

  private void addTimeRow(JPanel p, GridBagConstraints gbc, int y, String lbl,
                          JTextField date, JTextField time) {
    gbc.gridy = y;
    gbc.gridx = 0;
    p.add(new JLabel(lbl), gbc);
    gbc.gridx = 1;
    p.add(date, gbc);
    gbc.gridx = 2;
    p.add(time, gbc);
  }

  private JPanel createButtonPanel() {
    JButton btnRun = new JButton("Apply Changes");
    btnRun.setFont(INPUT_FONT);
    JButton btnCancel = new JButton("Cancel");
    btnCancel.setFont(INPUT_FONT);

    btnCancel.addActionListener(e -> dispose());
    btnRun.addActionListener(e -> onRun());

    JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    p.add(btnCancel);
    p.add(btnRun);
    return p;
  }

  private void onRun() {
    try {
      String searchType = getSearchType();
      String[] searchArgs = getSearchArgs(searchType);
      String prop = (String) propertyBox.getSelectedItem();
      assert prop != null;
      String val = getSelectedValue(prop);

      features.searchAndBulkEdit(searchType, searchArgs[0], searchArgs[1], prop, val);
      dispose();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
          "Failed", JOptionPane.ERROR_MESSAGE);
    }
  }

  private String getSearchType() {
    return ((String) Objects.requireNonNull(searchTypeBox.getSelectedItem())).equalsIgnoreCase(
        "Subject")
        ? "subject" : "time";
  }

  private String[] getSearchArgs(String type) {
    if (type.equals("subject")) {
      String sub = subjectField.getText().trim();
      if (sub.isEmpty()) {
        throw new IllegalArgumentException("Subject cannot be empty.");
      }
      return new String[] {sub, null};
    }
    String start = startDateField.getText().trim() + "T" + startTimeField.getText().trim();
    String end = endDateField.getText().trim() + "T" + endTimeField.getText().trim();
    return new String[] {start, end};
  }

  private String getSelectedValue(String prop) {
    if (prop.contains("Date")) {
      return dateValueField.getText().trim();
    }
    if (prop.contains("Time")) {
      return timeValueField.getText().trim();
    }
    if (prop.equals("Status")) {
      return (String) statusValueBox.getSelectedItem();
    }
    return textValueField.getText().trim();
  }
}