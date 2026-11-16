package view;

import model.AttendanceRecord;
import model.AttendanceReport;
import service.AttendanceService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class AttendanceOfficerFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AttendanceService service;
    private final AttendanceReport reportService;

    private DefaultTableModel tableModel;
    private JTable attendanceTable;

    private JTextField attendanceIdField;
    private JTextField studentIdField;
    private JTextField classIdField;
    private JTextField dateField;

    private JComboBox<String> statusBox;

    public AttendanceOfficerFrame() {
        service = new AttendanceService();
        reportService = new AttendanceReport();

        setTitle("SDAMS - Attendance Officer");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadAttendance();
    }

    private void createInterface() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Attendance Officer Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(title, BorderLayout.NORTH);

        String[] columns = {
                "Attendance ID", "Student ID", "Class ID", "Date", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        attendanceTable = new JTable(tableModel);
        attendanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mainPanel.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));

        attendanceIdField = new JTextField();
        studentIdField = new JTextField();
        classIdField = new JTextField();
        dateField = new JTextField(LocalDate.now().toString());
        statusBox = new JComboBox<>(new String[]{"PRESENT", "ABSENT", "LATE"});

        formPanel.add(new JLabel("Attendance ID:"));
        formPanel.add(attendanceIdField);
        formPanel.add(new JLabel("Student ID:"));
        formPanel.add(studentIdField);
        formPanel.add(new JLabel("Class ID:"));
        formPanel.add(classIdField);
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        formPanel.add(dateField);
        formPanel.add(new JLabel("Attendance Status:"));
        formPanel.add(statusBox);

        JButton addButton = new JButton("Add Attendance");
        JButton updateButton = new JButton("Update Status");
        JButton studentSearchButton = new JButton("Search Student");
        JButton classSearchButton = new JButton("Search Class");
        JButton dateSearchButton = new JButton("Search Date");
        JButton refreshButton = new JButton("Show All");
        JButton studentReportButton = new JButton("Student Report");
        JButton classReportButton = new JButton("Class Report");
        JButton clearButton = new JButton("Clear Fields");

        JPanel buttonPanel = new JPanel(new GridLayout(3, 3, 8, 8));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(studentSearchButton);
        buttonPanel.add(classSearchButton);
        buttonPanel.add(dateSearchButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(studentReportButton);
        buttonPanel.add(classReportButton);
        buttonPanel.add(clearButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addAttendance());
        updateButton.addActionListener(e -> updateAttendance());
        studentSearchButton.addActionListener(e -> searchStudent());
        classSearchButton.addActionListener(e -> searchClass());
        dateSearchButton.addActionListener(e -> searchDate());
        refreshButton.addActionListener(e -> loadAttendance());
        studentReportButton.addActionListener(e -> showStudentReport());
        classReportButton.addActionListener(e -> showClassReport());
        clearButton.addActionListener(e -> clearFields());

        attendanceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFieldsFromSelectedRow();
            }
        });

        add(mainPanel);
    }

    private void addAttendance() {
        String attendanceId = attendanceIdField.getText().trim();
        String studentId = studentIdField.getText().trim();
        String classId = classIdField.getText().trim();
        String date = dateField.getText().trim();
        String status = (String) statusBox.getSelectedItem();

        if (attendanceId.isEmpty() || studentId.isEmpty()
                || classId.isEmpty() || date.isEmpty()) {
            showMessage("Please fill in all fields.");
            return;
        }

        if (!isValidDate(date)) {
            showMessage("Enter a valid date in YYYY-MM-DD format.");
            return;
        }

        try {
            ArrayList<AttendanceRecord> records = service.getAllAttendance();

            for (AttendanceRecord record : records) {
                if (record.getAttendanceId().equalsIgnoreCase(attendanceId)) {
                    showMessage("Attendance ID already exists.");
                    return;
                }

                if (record.getStudentId().equalsIgnoreCase(studentId)
                        && record.getClassId().equalsIgnoreCase(classId)
                        && record.getDate().equals(date)) {
                    showMessage(
                            "Attendance has already been recorded for this student, class and date.");
                    return;
                }
            }

            AttendanceRecord record = new AttendanceRecord(
                    attendanceId, studentId, classId, date, status);

            service.addAttendance(record);
            loadAttendance();
            clearFields();
            showMessage("Attendance added successfully.");

        } catch (IOException | IllegalArgumentException ex) {
            showError(ex);
        }
    }

    private void updateAttendance() {
        int selectedRow = attendanceTable.getSelectedRow();

        if (selectedRow == -1) {
            showMessage("Select a record from the table first.");
            return;
        }

        String attendanceId = tableModel.getValueAt(selectedRow, 0).toString();
        String newStatus = (String) statusBox.getSelectedItem();

        try {
            boolean updated =
                    service.updateAttendanceStatus(attendanceId, newStatus);

            if (updated) {
                loadAttendance();
                showMessage("Attendance status updated successfully.");
            } else {
                showMessage("Attendance record was not found.");
            }
        } catch (IOException | IllegalArgumentException ex) {
            showError(ex);
        }
    }

    private void searchStudent() {
        String studentId = studentIdField.getText().trim();

        if (studentId.isEmpty()) {
            showMessage("Enter a Student ID to search.");
            return;
        }

        try {
            ArrayList<AttendanceRecord> records =
                    service.searchByStudentId(studentId);
            displayRecords(records);

            if (records.isEmpty()) {
                showMessage("No attendance records found for this student.");
            }
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void searchClass() {
        String classId = classIdField.getText().trim();

        if (classId.isEmpty()) {
            showMessage("Enter a Class ID to search.");
            return;
        }

        try {
            ArrayList<AttendanceRecord> records =
                    service.searchByClassId(classId);
            displayRecords(records);

            if (records.isEmpty()) {
                showMessage("No attendance records found for this class.");
            }
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void searchDate() {
        String date = dateField.getText().trim();

        if (!isValidDate(date)) {
            showMessage("Enter a valid date in YYYY-MM-DD format.");
            return;
        }

        try {
            ArrayList<AttendanceRecord> records =
                    service.searchByDate(date);
            displayRecords(records);

            if (records.isEmpty()) {
                showMessage("No attendance records found for this date.");
            }
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void loadAttendance() {
        try {
            displayRecords(service.getAllAttendance());
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void displayRecords(ArrayList<AttendanceRecord> records) {
        tableModel.setRowCount(0);

        for (AttendanceRecord record : records) {
            tableModel.addRow(new Object[]{
                    record.getAttendanceId(),
                    record.getStudentId(),
                    record.getClassId(),
                    record.getDate(),
                    record.getStatus()
            });
        }
    }

    private void fillFieldsFromSelectedRow() {
        int row = attendanceTable.getSelectedRow();

        if (row == -1) {
            return;
        }

        attendanceIdField.setText(tableModel.getValueAt(row, 0).toString());
        studentIdField.setText(tableModel.getValueAt(row, 1).toString());
        classIdField.setText(tableModel.getValueAt(row, 2).toString());
        dateField.setText(tableModel.getValueAt(row, 3).toString());
        statusBox.setSelectedItem(tableModel.getValueAt(row, 4).toString());
    }

    private void showStudentReport() {
        String studentId = studentIdField.getText().trim();

        if (studentId.isEmpty()) {
            showMessage("Enter a Student ID first.");
            return;
        }

        try {
            ArrayList<AttendanceRecord> records =
                    service.searchByStudentId(studentId);

            if (records.isEmpty()) {
                showMessage("No attendance records found for this student.");
                return;
            }

            String report = reportService.generateStudentReport(studentId);

            JOptionPane.showMessageDialog(
                    this, report, "Student Attendance Report",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void showClassReport() {
        String classId = classIdField.getText().trim();

        if (classId.isEmpty()) {
            showMessage("Enter a Class ID first.");
            return;
        }

        try {
            ArrayList<AttendanceRecord> records =
                    service.searchByClassId(classId);

            if (records.isEmpty()) {
                showMessage("No attendance records found for this class.");
                return;
            }

            String report = reportService.generateClassReport(classId);

            JOptionPane.showMessageDialog(
                    this, report, "Class Attendance Report",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            showError(ex);
        }
    }

    private boolean isValidDate(String date) {
        try {
            LocalDate.parse(date);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private void clearFields() {
        attendanceIdField.setText("");
        studentIdField.setText("");
        classIdField.setText("");
        dateField.setText(LocalDate.now().toString());
        statusBox.setSelectedIndex(0);
        attendanceTable.clearSelection();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                this, message, "SDAMS", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "An error occurred: " + ex.getMessage(),
                "System Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AttendanceOfficerFrame frame = new AttendanceOfficerFrame();
            frame.setVisible(true);
        });
    }
}
