package infrastructure;

import contracts.IUserRepository;
import contracts.IStudentManager;
import domain.Student;
import domain.IStudent;
import utils.PasswordEncryption;

import java.sql.*;
import java.util.*;

public class DatabaseUserRepository implements IUserRepository, IStudentManager {
    
    public DatabaseUserRepository() { }

    @Override
    public Student findById(String studentId) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return null;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Student student = new Student(
                            rs.getString("name"),
                            rs.getString("student_id"),
                            rs.getString("password")
                        );
                        student.setLoyaltyPoints(rs.getInt("loyalty_points"));
                        return student;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to find student: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return null;
    }

    @Override
    public void save(Student student) {
        // First try to update existing student
        if (updateStudent(student)) {
            return;
        }
        // If update failed, add new student
        addStudent(student);
    }
    
    /**
     * Verifies a student's password during login
     * @param studentId The student ID
     * @param password The plain text password to verify
     * @return true if password is correct, false otherwise
     */
    public boolean verifyPassword(String studentId, String password) {
        String sql = "SELECT password FROM students WHERE student_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        return PasswordEncryption.verifyPassword(password, storedPassword);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to verify password: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }

    private boolean addStudent(Student student) {
        String sql = "INSERT INTO students (student_id, name, password, loyalty_points) VALUES (?, ?, ?, ?)";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            // Encrypt the password before storing
            String encryptedPassword = PasswordEncryption.encryptPassword(student.getPassword());
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, student.getStudentID());
                pstmt.setString(2, student.getName());
                pstmt.setString(3, encryptedPassword);
                pstmt.setInt(4, student.getLoyaltyPoints());
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✅ Student added successfully: " + student.getName());
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add student: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }

    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET name = ?, password = ?, loyalty_points = ? WHERE student_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            // Encrypt the password before storing
            String encryptedPassword = PasswordEncryption.encryptPassword(student.getPassword());
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, student.getName());
                pstmt.setString(2, encryptedPassword);
                pstmt.setInt(3, student.getLoyaltyPoints());
                pstmt.setString(4, student.getStudentID());
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update student: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }

    // IStudentManager implementation
    @Override
    public IStudent registerStudent(String name, String studentID, String password) {
        // Check if student already exists
        Student existingStudent = findById(studentID);
        if (existingStudent != null) {
            System.out.println("⚠️ Student ID already exists");
            return null;
        }

        Student student = new Student(name, studentID, password);
        if (addStudent(student)) {
            System.out.println("✅ Student registered: " + name);
            return student;
        }
        return null;
    }

    @Override
    public IStudent login(String studentID, String name, String password) {
        Student student = findById(studentID);
        if (student != null && student.getName().equals(name) && student.getPassword().equals(password)) {
            // Login successful - silent operation
            return student;
        }
        System.out.println("❌ Invalid studentID, name, or password!");
        return null;
    }

    // Additional helper methods for student management
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return students;
            
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Student student = new Student(
                        rs.getString("name"),
                        rs.getString("student_id"),
                        rs.getString("password")
                    );
                    student.setLoyaltyPoints(rs.getInt("loyalty_points"));
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch students: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return students;
    }

    public boolean deleteStudent(String studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✅ Student deleted successfully");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete student: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }
}
