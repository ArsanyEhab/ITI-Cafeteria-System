package infrastructure;

import contracts.IUserRepository;
import contracts.IStudentManager;
import domain.Student;
import domain.IStudent;

import java.sql.*;
import java.util.*;

public class DatabaseUserRepository implements IUserRepository, IStudentManager {
    private Connection con;

    public DatabaseUserRepository() {
        con = DatabaseRepository.getConnection();
    }

    @Override
    public Student findById(String studentId) {
        // Handle offline mode
        if (con == null || DatabaseRepository.getConnection() == null) {
            // Return null in offline mode - no students available
            return null;
        }
        
        String sql = "SELECT * FROM students WHERE student_id = ?";
        
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
        } catch (SQLException e) {
            System.err.println("❌ Failed to find student: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Student student) {
        // Handle offline mode
        if (con == null || DatabaseRepository.getConnection() == null) {
            // Silent operation in offline mode
            return;
        }
        
        // First try to update existing student
        if (updateStudent(student)) {
            return;
        }
        // If update failed, add new student
        addStudent(student);
    }

    private boolean addStudent(Student student) {
        String sql = "INSERT INTO students (student_id, name, password, loyalty_points) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, student.getStudentID());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getPassword());
            pstmt.setInt(4, student.getLoyaltyPoints());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Student added successfully: " + student.getName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add student: " + e.getMessage());
        }
        return false;
    }

    private boolean updateStudent(Student student) {
        String sql = "UPDATE students SET name = ?, password = ?, loyalty_points = ? WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getPassword());
            pstmt.setInt(3, student.getLoyaltyPoints());
            pstmt.setString(4, student.getStudentID());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to update student: " + e.getMessage());
            return false;
        }
    }

    // IStudentManager implementation
    @Override
    public IStudent registerStudent(String name, String studentID, String password) {
        // Handle offline mode
        if (con == null || DatabaseRepository.getConnection() == null) {
            // Create student in memory for offline mode
            Student student = new Student(name, studentID, password);
            System.out.println("⚠️ Offline mode: Student created in memory only: " + name);
            return student;
        }
        
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
        // Handle offline mode
        if (con == null || DatabaseRepository.getConnection() == null) {
            System.out.println("⚠️ Offline mode: Cannot verify login credentials");
            return null;
        }
        
        Student student = findById(studentID);
        if (student != null && student.getName().equals(name) && student.getPassword().equals(password)) {
            System.out.println("✅ Login successful for: " + student.getName());
            return student;
        }
        System.out.println("❌ Invalid studentID, name, or password!");
        return null;
    }

    // Additional helper methods for student management
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";
        
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
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch students: " + e.getMessage());
        }
        return students;
    }

    public boolean deleteStudent(String studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Student deleted successfully");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete student: " + e.getMessage());
        }
        return false;
    }
}
