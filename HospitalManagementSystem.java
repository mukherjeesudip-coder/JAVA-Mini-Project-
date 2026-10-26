import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// ==========================================
// UNIT III: CUSTOM EXCEPTIONS
// ==========================================
class InvalidPatientException extends Exception {
    public InvalidPatientException(String message) { super(message); }
}

class AppointmentConflictException extends Exception {
    public AppointmentConflictException(String message) { super(message); }
}

// ==========================================
// UNIT II: INTERFACE & ABSTRACT ARCHITECTURE
// ==========================================
interface Billable {
    double calculateFinalBill(double consultationFee, double taxPercent);
}

abstract class Person {
    protected String name;
    protected int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }

    public abstract void displayDetails();
}

// ==========================================
// UNIT I & II: DOMAIN ENTITIES (POJOs)
// ==========================================
class Patient extends Person implements Billable {
    private static int idCounter = 1000;
    private final int patientId;
    private LocalDate dateOfBirth;

    public Patient(String name, int age, LocalDate dateOfBirth) throws InvalidPatientException {
        super(name, age);
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidPatientException("Patient name cannot be empty!");
        }
        this.patientId = ++idCounter;
        this.dateOfBirth = dateOfBirth;
    }

    public int getPatientId() { return patientId; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    @Override
    public void displayDetails() {
        System.out.println("[Patient ID: " + patientId + "] Name: " + name + " | Age: " + age);
    }

    @Override
    public double calculateFinalBill(double consultationFee, double taxPercent) {
        return consultationFee + (consultationFee * (taxPercent / 100.0));
    }
}

class Doctor extends Person {
    private static int docIdCounter = 500;
    private final int doctorId;
    private String specialization;
    private double consultationFee;

    public Doctor(String name, int age, String specialization, double fee) {
        super(name, age);
        this.doctorId = ++docIdCounter;
        this.specialization = specialization;
        this.consultationFee = fee;
    }

    public int getDoctorId() { return doctorId; }
    public String getSpecialization() { return specialization; }
    public double getConsultationFee() { return consultationFee; }

    @Override
    public void displayDetails() {
        System.out.println("Dr. " + name + " (" + specialization + ") | Fee: $" + consultationFee);
    }
}

// ==========================================
// UNIT III: THREAD SAFETY & MULTITHREADING
// ==========================================
class AppointmentManager {
    private List<LocalDateTime> bookedSlots = new ArrayList<>();
    private List<String> confirmedBookings = new ArrayList<>();

    public synchronized boolean bookAppointment(Patient patient, Doctor doctor, LocalDateTime slot) throws AppointmentConflictException {
        if (bookedSlots.contains(slot)) {
            throw new AppointmentConflictException("Slot " + slot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " is already booked!");
        }
        bookedSlots.add(slot);
        confirmedBookings.add(patient.getPatientId() + "-" + doctor.getDoctorId());
        return true;
    }

    public boolean hasAppointment(int patientId, int doctorId) {
        return confirmedBookings.contains(patientId + "-" + doctorId);
    }
}

class BillingTask implements Runnable {
    private Patient patient;
    private Doctor doctor;
    private javax.swing.JTextArea logArea;

    public BillingTask(Patient patient, Doctor doctor, javax.swing.JTextArea logArea) {
        this.patient = patient;
        this.doctor = doctor;
        this.logArea = logArea;
    }

    @Override
    public void run() {
        logArea.append("\n[Billing Thread] Processing background invoice for Patient ID: " + patient.getPatientId() + "...");
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        double total = patient.calculateFinalBill(doctor.getConsultationFee(), 18.0);
        Double finalAmountWrapper = Double.valueOf(total);
        logArea.append("\n[Billing Complete] Patient: " + patient.getName() + " | Doctor: " + doctor.getName() + " | Final Invoiced Amount: $" + String.format("%.2f", finalAmountWrapper) + "\n");
    }
}

// ==========================================
// DATABASE PERSISTENCE LAYER (JDBC)
// ==========================================
class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:hospital_system.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS PATIENTS (" +
                    "patient_id INT PRIMARY KEY, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "age INT, " +
                    "date_of_birth TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS DOCTORS (" +
                    "doctor_id INT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "specialization VARCHAR(100), " +
                    "fee DECIMAL(10,2))");

            stmt.execute("CREATE TABLE IF NOT EXISTS APPOINTMENTS (" +
                    "appointment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "appointment_time TEXT NOT NULL, " +
                    "patient_id INT, " +
                    "doctor_id INT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS BILLING (" +
                    "bill_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "patient_id INT, " +
                    "doctor_id INT, " +
                    "total_amount DECIMAL(10,2) DEFAULT 0.00)");

            System.out.println("Hospital Management Database Tables Initialized Successfully.");
        } catch (SQLException e) {
            System.err.println("Database Initialization Error: " + e.getMessage());
        }
    }

    public static void savePatient(Patient p) {
        String query = "INSERT INTO PATIENTS(patient_id, full_name, age, date_of_birth) VALUES(?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, p.getPatientId());
            pstmt.setString(2, p.getName());
            pstmt.setInt(3, p.getAge());
            pstmt.setString(4, p.getDateOfBirth().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database Error (Patient Save): " + e.getMessage());
        }
    }

    public static void saveDoctor(Doctor d) {
        String query = "INSERT INTO DOCTORS(doctor_id, name, specialization, fee) VALUES(?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, d.getDoctorId());
            pstmt.setString(2, d.getName());
            pstmt.setString(3, d.getSpecialization());
            pstmt.setDouble(4, d.getConsultationFee());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database Error (Doctor Save): " + e.getMessage());
        }
    }
    
    public static void saveAppointment(String time, int patientId, int doctorId) {
        String query = "INSERT INTO APPOINTMENTS(appointment_time, patient_id, doctor_id) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, time);
            pstmt.setInt(2, patientId);
            pstmt.setInt(3, doctorId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database Error (Appointment Save): " + e.getMessage());
        }
    }

    public static void saveBillingRecord(int patientId, int doctorId, double amount) {
        String query = "INSERT INTO BILLING(patient_id, doctor_id, total_amount) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patientId);
            pstmt.setInt(2, doctorId);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database Error (Billing Save): " + e.getMessage());
        }
    }
}

// ==========================================
// PORTFOLIO-GRADE MODERN SWING GUI
// ==========================================
class HospitalManagementGUI extends JFrame {
    private JTabbedPane tabbedPane;
    
    private List<Patient> patientList = new ArrayList<>();
    private List<Doctor> doctorList = new ArrayList<>();
    private AppointmentManager appointmentManager = new AppointmentManager();
    private Doctor defaultDoc;

    private JTextField pNameField, pAgeField, dNameField, dSpecField, dFeeField;
    private JTable patientTable, doctorTable, appointmentTable;
    private DefaultTableModel patientTableModel, doctorTableModel, appointmentTableModel;
    private JComboBox<String> patientCombo, doctorCombo, billPatientCombo, billDoctorCombo;
    private JTextArea logArea;

    // Modern Color Palette
    private final Color PRIMARY_COLOR = new Color(15, 23, 42);   // Slate Dark
    private final Color ACCENT_COLOR = new Color(2, 132, 199);   // High-Visibility Vivid Blue
    private final Color BG_LIGHT = new Color(248, 250, 252);     // Off-white Background
    private final Color CARD_BG = Color.WHITE;                   // Card Background

    public HospitalManagementGUI() {
        setTitle("Enterprise Smart Healthcare & Patient Management System");
        setSize(1100, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        DatabaseManager.initializeDatabase();

        defaultDoc = new Doctor("Dr. Sarah Smith", 45, "Cardiology", 150.0);
        doctorList.add(defaultDoc);
        DatabaseManager.saveDoctor(defaultDoc);

        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(BG_LIGHT);
        
        tabbedPane.add("  Patient Management  ", createPatientPanel());
        tabbedPane.add("  Doctor Directory  ", createDoctorPanel());
        tabbedPane.add("  Appointment Scheduler  ", createAppointmentPanel());
        tabbedPane.add("  Billing & Transactions  ", createBillingPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(new EmptyBorder(15, 25, 15, 25));

        JLabel titleLabel = new JLabel("SMART HEALTHCARE PORTAL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Object-Oriented & Thread-Safe Hospital Administration Suite");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        return header;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(ACCENT_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(3, 105, 161), 2),
            new EmptyBorder(8, 15, 8, 15)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(25);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(186, 230, 253));
    }

    // --- TAB 1: PATIENT MANAGEMENT ---
    private JPanel createPatientPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)), 
            " Register New Patient ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; 
        pNameField = new JTextField(15);
        form.add(pNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Age:"), gbc);
        gbc.gridx = 1; 
        pAgeField = new JTextField(15);
        form.add(pAgeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton regBtn = createStyledButton("Register & Persist Patient");
        form.add(regBtn, gbc);

        panel.add(form, BorderLayout.WEST);

        patientTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Age", "DOB"}, 0);
        patientTable = new JTable(patientTableModel);
        styleTable(patientTable);
        panel.add(new JScrollPane(patientTable), BorderLayout.CENTER);

        regBtn.addActionListener(e -> {
            try {
                String name = pNameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Patient name cannot be empty!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int age = Integer.parseInt(pAgeField.getText());

                // Prevent Duplicate Patient Registration
                boolean alreadyExists = patientList.stream()
                        .anyMatch(p -> p.getName().equalsIgnoreCase(name) && p.getAge() == age);
                if (alreadyExists) {
                    JOptionPane.showMessageDialog(this, "A patient with the name '" + name + "' and age " + age + " is already registered!", "Duplicate Patient Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                int birthYear = LocalDate.now().getYear() - age;
                Random random = new Random();
                LocalDate randomDob = LocalDate.of(birthYear, random.nextInt(12) + 1, random.nextInt(25) + 1);

                Patient p = new Patient(name, age, randomDob);
                patientList.add(p);
                DatabaseManager.savePatient(p);
                patientTableModel.addRow(new Object[]{p.getPatientId(), p.getName(), p.getAge(), p.getDateOfBirth()});
                
                refreshDropdowns();
                JOptionPane.showMessageDialog(this, "Patient Registered Successfully!");
                pNameField.setText(""); pAgeField.setText("");
            } catch (InvalidPatientException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Validation Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // --- TAB 2: DOCTOR DIRECTORY ---
    private JPanel createDoctorPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)), 
            " Add Medical Staff ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Doctor Name:"), gbc);
        gbc.gridx = 1; dNameField = new JTextField(15);
        form.add(dNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Specialization:"), gbc);
        gbc.gridx = 1; dSpecField = new JTextField(15);
        form.add(dSpecField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Fee ($):"), gbc);
        gbc.gridx = 1; dFeeField = new JTextField(15);
        form.add(dFeeField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton addDocBtn = createStyledButton("Save Doctor Profile");
        form.add(addDocBtn, gbc);

        panel.add(form, BorderLayout.WEST);

        doctorTableModel = new DefaultTableModel(new String[]{"Doc ID", "Name", "Specialization", "Fee ($)"}, 0);
        doctorTableModel.addRow(new Object[]{defaultDoc.getDoctorId(), defaultDoc.getName(), defaultDoc.getSpecialization(), defaultDoc.getConsultationFee()});
        doctorTable = new JTable(doctorTableModel);
        styleTable(doctorTable);
        panel.add(new JScrollPane(doctorTable), BorderLayout.CENTER);

        addDocBtn.addActionListener(e -> {
            try {
                String name = dNameField.getText().trim();
                String spec = dSpecField.getText().trim();
                if (name.isEmpty() || spec.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name and Specialization cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                double fee = Double.parseDouble(dFeeField.getText());

                // Prevent Duplicate Doctor Registration
                boolean docExists = doctorList.stream()
                        .anyMatch(d -> d.getName().equalsIgnoreCase(name) && d.getSpecialization().equalsIgnoreCase(spec));
                if (docExists) {
                    JOptionPane.showMessageDialog(this, "Doctor '" + name + "' with specialization '" + spec + "' is already registered!", "Duplicate Doctor Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Doctor d = new Doctor(name, 40, spec, fee);
                doctorList.add(d);
                DatabaseManager.saveDoctor(d);
                doctorTableModel.addRow(new Object[]{d.getDoctorId(), d.getName(), d.getSpecialization(), d.getConsultationFee()});
                refreshDropdowns();
                
                JOptionPane.showMessageDialog(this, "Doctor Profile Added Successfully!");
                dNameField.setText(""); dSpecField.setText(""); dFeeField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding doctor: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // --- TAB 3: APPOINTMENT SCHEDULER ---
    private JPanel createAppointmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)), 
            " Schedule Slot (Synchronized) ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Select Patient:"), gbc);
        gbc.gridx = 1; patientCombo = new JComboBox<>();
        form.add(patientCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Select Doctor:"), gbc);
        gbc.gridx = 1; doctorCombo = new JComboBox<>();
        doctorCombo.addItem("Dr. Sarah Smith (Cardiology) - Fee: $150.0");
        form.add(doctorCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton bookBtn = createStyledButton("Confirm Appointment Slot");
        form.add(bookBtn, gbc);

        panel.add(form, BorderLayout.WEST);

        appointmentTableModel = new DefaultTableModel(new String[]{"Patient Name", "Assigned Doctor", "Timestamp"}, 0);
        appointmentTable = new JTable(appointmentTableModel);
        styleTable(appointmentTable);
        panel.add(new JScrollPane(appointmentTable), BorderLayout.CENTER);

        bookBtn.addActionListener(e -> {
            if (patientList.isEmpty() || doctorList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please register at least one patient and doctor first!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                int patientIndex = patientCombo.getSelectedIndex();
                int doctorIndex = doctorCombo.getSelectedIndex();
                
                if (patientIndex < 0 || doctorIndex < 0) return;

                Patient p = patientList.get(patientIndex);
                Doctor d = doctorList.get(doctorIndex);
                LocalDateTime slot = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

                if (appointmentManager.bookAppointment(p, d, slot)) {
                    DatabaseManager.saveAppointment(slot.toString(), p.getPatientId(), d.getDoctorId());
                    appointmentTableModel.addRow(new Object[]{p.getName(), d.getName(), slot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))});
                    JOptionPane.showMessageDialog(this, "Appointment successfully locked and confirmed!");
                }
            } catch (AppointmentConflictException ex) {
                JOptionPane.showMessageDialog(this, "Conflict Error: " + ex.getMessage(), "Double-Booking Prevention", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // --- TAB 4: BILLING & ASYNCHRONOUS MULTITHREADING + FILE EXPORT ---
    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(CARD_BG);
        controlPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)), 
            " Generate Invoice & Bill Export ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("Select Patient:"), gbc);
        gbc.gridx = 1; billPatientCombo = new JComboBox<>();
        controlPanel.add(billPatientCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        controlPanel.add(new JLabel("Select Attending Doctor:"), gbc);
        gbc.gridx = 1; billDoctorCombo = new JComboBox<>();
        billDoctorCombo.addItem("Dr. Sarah Smith (Cardiology) - Fee: $150.0");
        controlPanel.add(billDoctorCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setBackground(CARD_BG);
        JButton generateBillBtn = createStyledButton("Process Async Invoice");
        JButton exportPdfBtn = createStyledButton("Export Bill Text File");
        buttonRow.add(generateBillBtn);
        buttonRow.add(exportPdfBtn);
        controlPanel.add(buttonRow, gbc);

        panel.add(controlPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(PRIMARY_COLOR);
        logArea.setForeground(new Color(56, 189, 248));
        logArea.setText(">>> System Ready. Multithreaded Billing Console Initialized.\n");
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)), 
            " Live Transaction Log ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR
        ));
        panel.add(scrollPane, BorderLayout.CENTER);

        generateBillBtn.addActionListener(e -> {
            if (patientList.isEmpty() || doctorList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No active patient or doctor records available.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int pIndex = billPatientCombo.getSelectedIndex();
            int dIndex = billDoctorCombo.getSelectedIndex();
            if (pIndex < 0 || dIndex < 0) return;

            Patient p = patientList.get(pIndex);
            Doctor d = doctorList.get(dIndex);

            if (!appointmentManager.hasAppointment(p.getPatientId(), d.getDoctorId())) {
                JOptionPane.showMessageDialog(this, 
                    "Billing Blocked: Patient '" + p.getName() + "' does not have a confirmed appointment with " + d.getName() + "!", 
                    "Appointment Required", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Thread billingThread = new Thread(new BillingTask(p, d, logArea));
            billingThread.start();
        });

        exportPdfBtn.addActionListener(e -> {
            if (patientList.isEmpty() || doctorList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No records to export.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int pIndex = billPatientCombo.getSelectedIndex();
            int dIndex = billDoctorCombo.getSelectedIndex();
            if (pIndex < 0 || dIndex < 0) return;

            Patient p = patientList.get(pIndex);
            Doctor d = doctorList.get(dIndex);
            
            if (!appointmentManager.hasAppointment(p.getPatientId(), d.getDoctorId())) {
                JOptionPane.showMessageDialog(this, 
                    "Invoice Export Blocked: Patient '" + p.getName() + "' does not have a confirmed appointment with " + d.getName() + "!", 
                    "Appointment Required", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double subtotal = d.getConsultationFee();
            double tax = subtotal * 0.18;
            double total = p.calculateFinalBill(subtotal, 18.0);

            String fileName = "Patient_Bill_" + p.getPatientId() + ".txt";
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.println("==================================================");
                writer.println("         SMART HEALTHCARE PORTAL - INVOICE        ");
                writer.println("==================================================");
                writer.println("Date & Time  : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("Patient ID   : " + p.getPatientId());
                writer.println("Patient Name : " + p.getName());
                writer.println("Patient Age  : " + p.getAge());
                writer.println("--------------------------------------------------");
                writer.println("Attending Dr.: " + d.getName());
                writer.println("Specialization: " + d.getSpecialization());
                writer.println("Consultation : $" + String.format("%.2f", subtotal));
                writer.println("Tax (18%)    : $" + String.format("%.2f", tax));
                writer.println("--------------------------------------------------");
                writer.println("TOTAL AMOUNT : $" + String.format("%.2f", total));
                writer.println("==================================================");
                writer.println("        Thank you for choosing our hospital!      ");
                writer.println("==================================================");
                
                DatabaseManager.saveBillingRecord(p.getPatientId(), d.getDoctorId(), total);
                
                logArea.append("\n[File Export Success] Bill invoice saved to: " + new java.io.File(fileName).getAbsolutePath() + "\n");
                JOptionPane.showMessageDialog(this, "Bill successfully exported as a text invoice file:\n" + fileName, "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "File Export Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void refreshDropdowns() {
        patientCombo.removeAllItems();
        billPatientCombo.removeAllItems();
        for (Patient p : patientList) {
            String display = p.getName() + " (ID: " + p.getPatientId() + ")";
            patientCombo.addItem(display);
            billPatientCombo.addItem(display);
        }

        doctorCombo.removeAllItems();
        billDoctorCombo.removeAllItems();
        for (Doctor d : doctorList) {
            String display = "Dr. " + d.getName() + " (" + d.getSpecialization() + ") - Fee: $" + d.getConsultationFee();
            doctorCombo.addItem(display);
            billDoctorCombo.addItem(display);
        }
    }
}

// ==========================================
// MAIN CLASS PROGRAM EXECUTION
// ==========================================
public class HospitalManagementSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HospitalManagementGUI().setVisible(true);
        });
    }
}
