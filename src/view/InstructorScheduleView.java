
package view;

import model.TrainingClass;
import service.ClassService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

public class InstructorScheduleView extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTable classTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private final ClassService classService = new ClassService();

    public InstructorScheduleView() {
        setTitle("SDAMS - Instructor Class Schedule");
        setSize(1200, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel =
                new JLabel("Instructor - Manage Class Schedule");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        String[] columns = {
            "Class ID", "Class Name", "Discipline", "Date",
            "Start Time", "End Time", "Room", "Capacity",
            "Fee", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        classTable = new JTable(tableModel);
        classTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        classTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        classTable.setRowHeight(25);

        int[] widths = {
            100, 150, 130, 110, 100,
            100, 100, 90, 90, 110
        };

        for (int i = 0; i < widths.length; i++) {
            classTable.getColumnModel()
                    .getColumn(i).setPreferredWidth(widths[i]);
        }

        mainPanel.add(
                new JScrollPane(classTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 15));
        JPanel searchPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton viewButton = new JButton("View All");

        searchPanel.add(new JLabel("Search by ID or name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(viewButton);

        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton addButton = new JButton("Add Class");
        JButton updateButton = new JButton("Update Selected");
        JButton cancelButton = new JButton("Cancel Selected");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);

        viewButton.addActionListener(e -> loadClasses());
        searchButton.addActionListener(e -> searchClasses());
        searchField.addActionListener(e -> searchClasses());
        addButton.addActionListener(e -> addClass());
        updateButton.addActionListener(e -> updateClass());
        cancelButton.addActionListener(e -> cancelClass());

        loadClasses();
    }

    private void loadClasses() {
        try {
            tableModel.setRowCount(0);

            for (TrainingClass c : classService.getAllClasses()) {
                addClassRow(c);
            }
        } catch (IOException ex) {
            showError("Could not load classes: " + ex.getMessage());
        }
    }

    private void addClassRow(TrainingClass c) {
        tableModel.addRow(new Object[]{
            c.getClassId(),
            c.getClassName(),
            c.getDiscipline(),
            c.getDate(),
            c.getStartTime(),
            c.getEndTime(),
            c.getRoom(),
            c.getCapacity(),
            c.getFee(),
            c.getStatus()
        });
    }

    private void searchClasses() {
        String keyword = searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            loadClasses();
            return;
        }

        try {
            tableModel.setRowCount(0);

            for (TrainingClass c : classService.getAllClasses()) {
                boolean matchesId = c.getClassId() != null
                        && c.getClassId().toLowerCase().contains(keyword);

                boolean matchesName = c.getClassName() != null
                        && c.getClassName().toLowerCase().contains(keyword);

                if (matchesId || matchesName) {
                    addClassRow(c);
                }
            }
        } catch (IOException ex) {
            showError("Search failed: " + ex.getMessage());
        }
    }

    private void addClass() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField discipline = new JTextField();
        JTextField instructorId = new JTextField();
        JTextField date = new JTextField();
        JTextField start = new JTextField();
        JTextField end = new JTextField();
        JTextField room = new JTextField();
        JTextField capacity = new JTextField();
        JTextField fee = new JTextField();

        Object[] fields = {
            "Class ID:", id,
            "Class Name:", name,
            "Discipline:", discipline,
            "Instructor ID:", instructorId,
            "Date (YYYY-MM-DD):", date,
            "Start Time:", start,
            "End Time:", end,
            "Room:", room,
            "Capacity:", capacity,
            "Fee:", fee
        };

        int result = JOptionPane.showConfirmDialog(
                this, fields, "Add Class",
                JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            if (id.getText().trim().isEmpty()
                    || name.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Class ID and name are required.");
                return;
            }

            int cap = Integer.parseInt(capacity.getText().trim());
            double price = Double.parseDouble(fee.getText().trim());

            if (cap <= 0 || price < 0) {
                JOptionPane.showMessageDialog(
                        this, "Capacity must be positive and fee cannot be negative.");
                return;
            }

            TrainingClass c = new TrainingClass(
                    id.getText().trim(),
                    name.getText().trim(),
                    discipline.getText().trim(),
                    instructorId.getText().trim(),
                    date.getText().trim(),
                    start.getText().trim(),
                    end.getText().trim(),
                    room.getText().trim(),
                    cap,
                    price
            );

            c.setStatus("ACTIVE");

            if (!classService.addClass(c)) {
                JOptionPane.showMessageDialog(
                        this, "That Class ID already exists.");
                return;
            }

            loadClasses();
            JOptionPane.showMessageDialog(
                    this, "Class added successfully.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this, "Capacity must be a whole number and fee must be numeric.");
        } catch (IOException ex) {
            showError("Could not add class: " + ex.getMessage());
        }
    }

    private void updateClass() {
        int row = classTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this, "Select a class first.");
            return;
        }

        String id = tableModel.getValueAt(row, 0).toString();

        try {
            TrainingClass c = classService.findClass(id);

            if (c == null) {
                JOptionPane.showMessageDialog(
                        this, "Class not found.");
                loadClasses();
                return;
            }

            JTextField name = new JTextField(c.getClassName());
            JTextField discipline = new JTextField(c.getDiscipline());
            JTextField instructor = new JTextField(c.getInstructorId());
            JTextField date = new JTextField(c.getDate());
            JTextField start = new JTextField(c.getStartTime());
            JTextField end = new JTextField(c.getEndTime());
            JTextField room = new JTextField(c.getRoom());
            JTextField capacity =
                    new JTextField(String.valueOf(c.getCapacity()));
            JTextField fee = new JTextField(String.valueOf(c.getFee()));

            Object[] fields = {
                "Class ID (cannot change):", new JLabel(id),
                "Class Name:", name,
                "Discipline:", discipline,
                "Instructor ID:", instructor,
                "Date:", date,
                "Start Time:", start,
                "End Time:", end,
                "Room:", room,
                "Capacity:", capacity,
                "Fee:", fee
            };

            int result = JOptionPane.showConfirmDialog(
                    this, fields, "Update Class",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            int cap = Integer.parseInt(capacity.getText().trim());
            double price = Double.parseDouble(fee.getText().trim());

            if (cap <= 0 || price < 0) {
                JOptionPane.showMessageDialog(
                        this, "Capacity must be positive and fee cannot be negative.");
                return;
            }

            c.setClassName(name.getText().trim());
            c.setDiscipline(discipline.getText().trim());
            c.setInstructorId(instructor.getText().trim());
            c.setDate(date.getText().trim());
            c.setStartTime(start.getText().trim());
            c.setEndTime(end.getText().trim());
            c.setRoom(room.getText().trim());
            c.setCapacity(cap);
            c.setFee(price);

            if (classService.updateClass(c)) {
                loadClasses();
                JOptionPane.showMessageDialog(
                        this, "Class updated successfully.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this, "Capacity must be a whole number and fee must be numeric.");
        } catch (IOException ex) {
            showError("Could not update class: " + ex.getMessage());
        }
    }

    private void cancelClass() {
        int row = classTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this, "Select a class first.");
            return;
        }

        String id = tableModel.getValueAt(row, 0).toString();

        int answer = JOptionPane.showConfirmDialog(
                this,
                "Cancel class " + id + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION);

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (classService.cancelClass(id)) {
                loadClasses();
                JOptionPane.showMessageDialog(
                        this, "Class cancelled successfully.");
            }
        } catch (IOException ex) {
            showError("Could not cancel class: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InstructorScheduleView view =
                    new InstructorScheduleView();
            view.setVisible(true);
        });
    }
}
