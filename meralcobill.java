import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;
import java.util.TreeSet;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class MeralcoBill extends JFrame {
    // UI Components
    private JTextField kwhField, rateField, searchField, dateField, idField;
    private JTextArea memoArea;
    private JLabel totalLabel, statusLabel;
    private JButton computeButton, saveButton, editButton, deleteButton,
            viewAllButton, searchButton, clearButton;
    private JTable recordsTable;
    private DefaultTableModel tableModel;
    private JScrollPane tableScrollPane, memoScrollPane;
    private JComboBox<String> searchTypeComboBox;
    private JComboBox<String> sortComboBox;


    // Database
    private static final String DB_URL = "jdbc:sqlite:meralco_bills.db";
    private Connection dbConnection;

    // Current calculation
    private double currentTotal = 0.0;
    private boolean isCalculated = false;
    private TreeSet<Integer> availableIds = new TreeSet<>();
    private int selectedRecordId = -1;
    private boolean isEditingExistingRecord = false;

    public MeralcoBill() {
        setupUI();
        setupEventHandlers();
        initializeDatabase();
        loadAllRecords();
        updateAvailableIds();
    }

    private void setupUI() {
        setTitle("Meralco Bill Calculator");
        setSize(750, 700);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Create main panels
        add(createInputPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            // Use default look and feel
        }
    }

    private JPanel createInputPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Bill Calculator"));
        mainPanel.setFont(new Font("Arial", Font.BOLD, 14));

        // Input fields panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Font for labels
        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font inputFont = new Font("Arial", Font.PLAIN, 16);

        // ID input
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel idLabel = new JLabel("ID:");
        idLabel.setFont(labelFont);
        inputPanel.add(idLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        idField = new JTextField(15);
        idField.setFont(inputFont);
        idField.setToolTipText("Leave empty for auto-assignment of next available ID");
        inputPanel.add(idField, gbc);

        // kWh input
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        JLabel kwhLabel = new JLabel("kWh Used:");
        kwhLabel.setFont(labelFont);
        inputPanel.add(kwhLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        kwhField = new JTextField(15);
        kwhField.setFont(inputFont);
        inputPanel.add(kwhField, gbc);

        // Rate input
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        JLabel rateLabel = new JLabel("Rate per kWh (₱):");
        rateLabel.setFont(labelFont);
        inputPanel.add(rateLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        rateField = new JTextField(15);
        rateField.setFont(inputFont);
        inputPanel.add(rateField, gbc);

        // Date input
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel dateLabel = new JLabel("Date (YYYY-MM-DD):");
        dateLabel.setFont(labelFont);
        inputPanel.add(dateLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dateField = new JTextField(15);
        dateField.setFont(inputFont);
        dateField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        inputPanel.add(dateField, gbc);

        // Total display
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        JLabel totalTextLabel = new JLabel("Total Bill:");
        totalTextLabel.setFont(labelFont);
        inputPanel.add(totalTextLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        totalLabel = new JLabel("₱0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 22)); // Larger font for total
        totalLabel.setForeground(new Color(0, 128, 0));
        totalLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        totalLabel.setOpaque(true);
        totalLabel.setBackground(Color.WHITE);
        totalLabel.setHorizontalAlignment(SwingConstants.LEFT);
        totalLabel.setPreferredSize(new Dimension(150, 30));
        inputPanel.add(totalLabel, gbc);

        // Memo area
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE;
        JLabel memoLabel = new JLabel("Memo:");
        memoLabel.setFont(labelFont);
        inputPanel.add(memoLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 5; gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        memoArea = new JTextArea(3, 15);
        memoArea.setFont(inputFont);
        memoArea.setLineWrap(true);
        memoArea.setWrapStyleWord(true);
        memoScrollPane = new JScrollPane(memoArea);
        inputPanel.add(memoScrollPane, gbc);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        Font buttonFont = new Font("Arial", Font.BOLD, 14);

        computeButton = createStyledButton("Calculate", new Color(70, 130, 180), buttonFont);
        saveButton = createStyledButton("Save Record", new Color(34, 139, 34), buttonFont);
        saveButton.setEnabled(false);
        editButton = createStyledButton("Edit Selected", new Color(255, 140, 0), buttonFont);
        deleteButton = createStyledButton("Delete Selected", new Color(220, 20, 60), buttonFont);
        clearButton = createStyledButton("Clear Fields", new Color(128, 128, 128), buttonFont);

        buttonPanel.add(computeButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JButton createStyledButton(String text, Color bgColor, Font font) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFont(font);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }

    private JPanel createTablePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Bill Records"));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(new JLabel("Search by:"));

        JButton estimatedBillingButton = createStyledButton("Estimated Billing", new Color(75, 0, 130), new Font("Arial", Font.BOLD, 12));
        searchPanel.add(estimatedBillingButton);
        estimatedBillingButton.addActionListener(e -> showEstimatedBilling());


        String[] searchTypes = {"ID", "Date", "Rate", "Memo"};
        searchTypeComboBox = new JComboBox<>(searchTypes);
        searchTypeComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        searchPanel.add(searchTypeComboBox);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchPanel.add(searchField);

        searchButton = createStyledButton("Search", new Color(70, 130, 180), new Font("Arial", Font.BOLD, 12));
        viewAllButton = createStyledButton("View All", new Color(128, 128, 128), new Font("Arial", Font.BOLD, 12));
        searchPanel.add(searchButton);
        searchPanel.add(viewAllButton);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Table setup
        String[] columnNames = {"ID", "Date & Time", "kWh Used", "Rate (₱)", "Total (₱)", "Memo"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recordsTable = new JTable(tableModel);
        recordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordsTable.getTableHeader().setReorderingAllowed(false);
        recordsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        recordsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        recordsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        recordsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        recordsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        recordsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        recordsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        recordsTable.getColumnModel().getColumn(5).setPreferredWidth(250);

        tableScrollPane = new JScrollPane(recordsTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 300));
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Sort panel (NEW)
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        sortPanel.add(new JLabel("Sort by:"));

        String[] sortOptions = {"Date (Newest First)", "Date (Oldest First)", "ID (Ascending)", "ID (Descending)"};
        sortComboBox = new JComboBox<>(sortOptions);
        sortComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        sortPanel.add(sortComboBox);

        sortComboBox.addActionListener(e -> {
            String selected = (String) sortComboBox.getSelectedItem();
            switch (selected) {
                case "Date (Newest First)":
                    loadAllRecords("timestamp DESC");
                    break;
                case "Date (Oldest First)":
                    loadAllRecords("timestamp ASC");
                    break;
                case "ID (Ascending)":
                    loadAllRecords("id ASC");
                    break;
                case "ID (Descending)":
                    loadAllRecords("id DESC");
                    break;
            }
        });

        mainPanel.add(sortPanel, BorderLayout.SOUTH);

        // Row selection logic
        recordsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = recordsTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        idField.setText(tableModel.getValueAt(selectedRow, 0).toString());
                        kwhField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                        rateField.setText(tableModel.getValueAt(selectedRow, 3).toString());
                        totalLabel.setText("₱" + tableModel.getValueAt(selectedRow, 4).toString());
                        dateField.setText(tableModel.getValueAt(selectedRow, 1).toString().split(" ")[0]);
                        memoArea.setText(tableModel.getValueAt(selectedRow, 5).toString());
                        saveButton.setEnabled(false);
                        isCalculated = true;
                        currentTotal = Double.parseDouble(tableModel.getValueAt(selectedRow, 4).toString());
                        selectedRecordId = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
                        isEditingExistingRecord = true;
                    }
                }
            }
        });

        return mainPanel;
    }

    private void loadAllRecords(String orderByClause) {
        String sql = "SELECT * FROM bills ORDER BY " + orderByClause;
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            populateTable(rs);
            updateStatus("Records sorted by: " + orderByClause);
        } catch (SQLException e) {
            showMessage("Error loading records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready | 🔔 Note: Readings due 12–13th monthly");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        statusPanel.add(statusLabel);
        return statusPanel;
    }

    private void setupEventHandlers() {
        computeButton.addActionListener(e -> calculateBill());
        saveButton.addActionListener(e -> saveRecord());
        editButton.addActionListener(e -> editSelectedRecord());
        deleteButton.addActionListener(e -> deleteSelectedRecord());
        clearButton.addActionListener(e -> clearFields());
        searchButton.addActionListener(e -> searchRecords());
        viewAllButton.addActionListener(e -> loadAllRecords());

        // Enter key support for calculation and search
        kwhField.addActionListener(e -> calculateBill());
        rateField.addActionListener(e -> calculateBill());
        dateField.addActionListener(e -> calculateBill());
        searchField.addActionListener(e -> searchRecords());
    }

    private void calculateBill() {
        try {
            String kwhText = kwhField.getText().trim();
            String rateText = rateField.getText().trim();

            if (kwhText.isEmpty() || rateText.isEmpty()) {
                showMessage("Please enter both kWh usage and rate.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double kwh = Double.parseDouble(kwhText);
            double rate = Double.parseDouble(rateText);

            if (kwh < 0 || rate < 0) {
                showMessage("Please enter positive numbers only.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            currentTotal = kwh * rate;
            totalLabel.setText(String.format("₱%.4f", currentTotal));
            saveButton.setEnabled(true);
            isCalculated = true;

            updateStatus("Bill calculated successfully. Click 'Save Record' to store in database.");

        } catch (NumberFormatException e) {
            showMessage("Please enter valid numbers for kWh and rate.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            clearCalculation();
        }
    }

    private void saveRecord() {
        if (!isCalculated) {
            showMessage("Please calculate the bill first.", "No Calculation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double kwh = Double.parseDouble(kwhField.getText().trim());
            double rate = Double.parseDouble(rateField.getText().trim());
            String date = dateField.getText().trim();
            String memo = memoArea.getText().trim();
            String idText = idField.getText().trim();

            if (date.isEmpty()) {
                showMessage("Please enter a date.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                showMessage("Please enter date in YYYY-MM-DD format.", "Invalid Date", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int recordId;
            if (idText.isEmpty()) {
                // Auto-assign next available ID
                recordId = getNextAvailableId();
            } else {
                // Use specified ID
                recordId = Integer.parseInt(idText);
                if (recordId <= 0) {
                    showMessage("ID must be a positive number.", "Invalid ID", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check if ID already exists (unless we're editing the same record)
                if (!isEditingExistingRecord || recordId != selectedRecordId) {
                    if (idExists(recordId)) {
                        showMessage("ID " + recordId + " already exists. Please choose a different ID.", "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            String sql = "INSERT INTO bills (id, kwh, rate, total, timestamp, memo) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
                pstmt.setInt(1, recordId);
                pstmt.setDouble(2, kwh);
                pstmt.setDouble(3, rate);
                pstmt.setDouble(4, currentTotal);
                pstmt.setString(5, date + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                pstmt.setString(6, memo);

                pstmt.executeUpdate();
                updateStatus("Record saved successfully with ID: " + recordId);

                updateAvailableIds();
                loadAllRecords();
                clearFields();
            }
        } catch (NumberFormatException e) {
            showMessage("Please enter valid numbers for ID, kWh and rate.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            showMessage("Error saving record: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedRecord() {
        int selectedRow = recordsTable.getSelectedRow();
        if (selectedRow == -1) {
            showMessage("Please select a record to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String kwhText = kwhField.getText().trim();
            String rateText = rateField.getText().trim();
            String date = dateField.getText().trim();
            String memo = memoArea.getText().trim();
            String idText = idField.getText().trim();

            if (kwhText.isEmpty() || rateText.isEmpty() || date.isEmpty() || idText.isEmpty()) {
                showMessage("Please enter ID, kWh usage, rate, and date.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                showMessage("Please enter date in YYYY-MM-DD format.", "Invalid Date", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double kwh = Double.parseDouble(kwhText);
            double rate = Double.parseDouble(rateText);
            int newId = Integer.parseInt(idText);

            if (kwh < 0 || rate < 0) {
                showMessage("Please enter positive numbers only.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newId <= 0) {
                showMessage("ID must be a positive number.", "Invalid ID", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if new ID already exists (unless it's the same record)
            if (newId != selectedRecordId && idExists(newId)) {
                showMessage("ID " + newId + " already exists. Please choose a different ID.", "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double total = kwh * rate;

            // If ID is changing, we need to delete the old record and insert a new one
            if (newId != selectedRecordId) {
                // Delete old record
                String deleteSql = "DELETE FROM bills WHERE id = ?";
                try (PreparedStatement deleteStmt = dbConnection.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, selectedRecordId);
                    deleteStmt.executeUpdate();
                }

                // Insert with new ID
                String insertSql = "INSERT INTO bills (id, kwh, rate, total, timestamp, memo) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, newId);
                    insertStmt.setDouble(2, kwh);
                    insertStmt.setDouble(3, rate);
                    insertStmt.setDouble(4, total);
                    insertStmt.setString(5, date + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    insertStmt.setString(6, memo);
                    insertStmt.executeUpdate();
                }

                // Add the old ID back to available IDs
                availableIds.add(selectedRecordId);
            } else {
                // ID is not changing, just update the record
                String sql = "UPDATE bills SET kwh = ?, rate = ?, total = ?, timestamp = ?, memo = ? WHERE id = ?";
                try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
                    pstmt.setDouble(1, kwh);
                    pstmt.setDouble(2, rate);
                    pstmt.setDouble(3, total);
                    pstmt.setString(4, date + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    pstmt.setString(5, memo);
                    pstmt.setInt(6, selectedRecordId);
                    pstmt.executeUpdate();
                }
            }

            updateStatus("Record updated successfully!");
            updateAvailableIds();
            loadAllRecords();
            clearFields();

        } catch (NumberFormatException e) {
            showMessage("Please enter valid numbers for ID, kWh and rate.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            showMessage("Error updating record: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedRecord() {
        int selectedRow = recordsTable.getSelectedRow();
        if (selectedRow == -1) {
            showMessage("Please select a record to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Object idValue = tableModel.getValueAt(selectedRow, 0);
                int recordId = (idValue instanceof Integer) ? (Integer)idValue : Integer.parseInt(idValue.toString());

                String sql = "DELETE FROM bills WHERE id = ?";

                try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
                    pstmt.setInt(1, recordId);
                    pstmt.executeUpdate();

                    // Add this ID to available IDs
                    availableIds.add(recordId);
                    updateStatus("Record deleted successfully! ID " + recordId + " is now available for reuse.");

                    loadAllRecords();
                    clearFields();
                }
            } catch (SQLException e) {
                showMessage("Error deleting record: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        idField.setText("");
        kwhField.setText("");
        rateField.setText("");
        dateField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        memoArea.setText("");
        totalLabel.setText("₱0.00");
        saveButton.setEnabled(false);
        isCalculated = false;
        currentTotal = 0.0;
        selectedRecordId = -1;
        isEditingExistingRecord = false;
        recordsTable.clearSelection();
        updateStatus("Fields cleared.");
    }

    private void searchRecords() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            showMessage("Please enter a search term.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String searchType = (String) searchTypeComboBox.getSelectedItem();
        String column;

        switch (searchType) {
            case "ID":
                column = "id";
                break;
            case "Date":
                column = "timestamp";
                break;
            case "Rate":
                column = "rate";
                break;
            case "Memo":
                column = "memo";
                break;
            default:
                showMessage("Invalid search type selected.", "Search Error", JOptionPane.ERROR_MESSAGE);
                return;
        }

        String sql = "SELECT * FROM bills WHERE CAST(" + column + " AS TEXT) LIKE ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            ResultSet rs = pstmt.executeQuery();

            // Fill the table
            populateTable(rs);

            // Show message if no records
            if (tableModel.getRowCount() == 0) {
                updateStatus("No records found for: " + searchType + " = " + searchTerm);
                showMessage("No matching records found.", "Search Result", JOptionPane.INFORMATION_MESSAGE);
            } else {
                updateStatus("Search completed for: " + searchType + " = " + searchTerm);
            }

        } catch (SQLException e) {
            showMessage("Error searching records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAllRecords() {
        String sql = "SELECT * FROM bills ORDER BY timestamp DESC";
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            populateTable(rs);
            updateStatus("All records loaded.");
        } catch (SQLException e) {
            showMessage("Error loading records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEstimatedBilling() {
        String sql = "SELECT * FROM bills ORDER BY timestamp DESC LIMIT 2";
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            double latestKwh = -1;
            double previousKwh = -1;
            double latestRate = -1;
            String latestDate = "";

            if (rs.next()) {
                latestKwh = rs.getDouble("kwh");
                latestRate = rs.getDouble("rate");
                latestDate = rs.getString("timestamp").split(" ")[0];
            }

            if (rs.next()) {
                previousKwh = rs.getDouble("kwh");
            }

            if (latestKwh == -1 || previousKwh == -1) {
                showMessage("Not enough data to estimate billing. At least 2 readings are required.", "Estimation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double usedKwh = latestKwh - previousKwh;
            if (usedKwh < 0) {
                showMessage("Warning: Latest kWh is less than previous. Please check input data.", "Data Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double estimatedBill = usedKwh * latestRate;

            String message = String.format(
                    "<html><b>Estimated Billing for %s</b><br><br>" +
                            "Latest kWh: %.4f<br>" +
                            "Previous kWh: %.4f<br>" +
                            "Used kWh: %.4f<br>" +
                            "Rate: ₱%.4f<br><br>" +
                            "<b>Estimated Bill: ₱%.4f</b></html>",
                    latestDate, latestKwh, previousKwh, usedKwh, latestRate, estimatedBill
            );

            JOptionPane.showMessageDialog(this, message, "Estimated Billing", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            showMessage("Error fetching billing data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateTable(ResultSet rs) throws SQLException {
        tableModel.setRowCount(0);

        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            row.add(rs.getInt("id"));
            row.add(rs.getString("timestamp"));
            row.add(String.format("%.4f", rs.getDouble("kwh")));
            row.add(String.format("%.4f", rs.getDouble("rate")));
            row.add(String.format("%.4f", rs.getDouble("total")));
            row.add(rs.getString("memo"));
            tableModel.addRow(row);
        }
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection(DB_URL);

            // Create table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS bills (" +
                    "id INTEGER PRIMARY KEY, " +
                    "kwh REAL NOT NULL, " +
                    "rate REAL NOT NULL, " +
                    "total REAL NOT NULL, " +
                    "timestamp TEXT NOT NULL, " +
                    "memo TEXT)";

            try (Statement stmt = dbConnection.createStatement()) {
                stmt.execute(createTableSQL);
            }

            updateStatus("Database connected successfully.");

        } catch (ClassNotFoundException e) {
            String message = "SQLite JDBC driver not found. Please add sqlite-jdbc.jar to classpath.";
            if (this.isDisplayable()) {
                showMessage(message, "Driver Error", JOptionPane.ERROR_MESSAGE);
            } else {
                System.err.println("Driver Error: " + message);
            }
            System.exit(1);
        } catch (SQLException e) {
            String message = "Database connection error: " + e.getMessage();
            if (this.isDisplayable()) {
                showMessage(message, "Database Error", JOptionPane.ERROR_MESSAGE);
            } else {
                System.err.println("Database Error: " + message);
            }
            System.exit(1);
        }
    }

    private void updateAvailableIds() {
        availableIds.clear();
        try {
            // Get all existing IDs
            String sql = "SELECT id FROM bills ORDER BY id";
            try (Statement stmt = dbConnection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                TreeSet<Integer> existingIds = new TreeSet<>();
                while (rs.next()) {
                    existingIds.add(rs.getInt("id"));
                }

                // Find gaps in the sequence
                if (!existingIds.isEmpty()) {
                    // Check for gaps from 1 to max ID
                    int maxId = existingIds.last();
                    for (int i = 1; i <= maxId; i++) {
                        if (!existingIds.contains(i)) {
                            availableIds.add(i);
                        }
                    }
                }

                // If no gaps or table is empty, next ID is max + 1 (or 1 if empty)
                if (availableIds.isEmpty()) {
                    if (existingIds.isEmpty()) {
                        availableIds.add(1);
                    } else {
                        availableIds.add(existingIds.last() + 1);
                    }
                }
            }
        } catch (SQLException e) {
            // Fallback: start with ID 1
            availableIds.clear();
            availableIds.add(1);
        }
    }

    private int getNextAvailableId() {
        if (availableIds.isEmpty()) {
            updateAvailableIds();
        }
        return availableIds.isEmpty() ? 1 : availableIds.first();
    }

    private boolean idExists(int id) {
        try {
            String sql = "SELECT COUNT(*) FROM bills WHERE id = ?";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void showMessage(String message, String title, int messageType) {
        if (this.isDisplayable()) {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        } else {
            System.err.println(title + ": " + message);
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void clearCalculation() {
        totalLabel.setText("₱0.00");
        saveButton.setEnabled(false);
        isCalculated = false;
        currentTotal = 0.0;
    }

    @Override
    public void dispose() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MeralcoBill().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}