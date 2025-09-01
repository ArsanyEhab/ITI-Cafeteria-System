package infrastructure;

import contracts.IStaffRepository;
import domain.Staff;
import utils.PasswordEncryption;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseStaffRepository implements IStaffRepository {
    public DatabaseStaffRepository() { }

    @Override
    public Staff findById(String staffId) {
        String sql = "SELECT staff_id, name, password, role FROM staff WHERE staff_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return null;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, staffId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean isAdmin = "admin".equalsIgnoreCase(rs.getString("role"));
                        return new Staff(
                                rs.getString("staff_id"),
                                rs.getString("name"),
                                rs.getString("password"),
                                isAdmin
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to find staff: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return null;
    }

    @Override
    public boolean save(Staff staff) {
        // Try update first
        if (updateStaff(staff)) {
            return true;
        }
        return addStaff(staff);
    }

    @Override
    public boolean registerStaff(String staffId, String name, String password, boolean isAdmin) {
        if (findById(staffId) != null) {
            System.out.println("⚠️ Staff ID already exists");
            return false;
        }
        return addStaff(new Staff(staffId, name, password, isAdmin));
    }

    @Override
    public Staff login(String staffId, String password) {
        Staff s = findById(staffId);
        if (s != null && verifyPassword(staffId, password)) {
            System.out.println("✅ Staff login successful: " + s.getName());
            return s;
        }
        System.out.println("❌ Invalid staff credentials");
        return null;
    }
    
    /**
     * Verifies a staff member's password during login
     * @param staffId The staff ID
     * @param password The plain text password to verify
     * @return true if password is correct, false otherwise
     */
    public boolean verifyPassword(String staffId, String password) {
        String sql = "SELECT password FROM staff WHERE staff_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, staffId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        return PasswordEncryption.verifyPassword(password, storedPassword);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to verify staff password: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }
    
    public List<Staff> getAllStaff() {
        List<Staff> allStaff = new ArrayList<>();
        String sql = "SELECT staff_id, name, password, role FROM staff";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return allStaff;
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    boolean isAdmin = "admin".equalsIgnoreCase(rs.getString("role"));
                    Staff staff = new Staff(
                        rs.getString("staff_id"),
                        rs.getString("name"),
                        rs.getString("password"),
                        isAdmin
                    );
                    allStaff.add(staff);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get all staff: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return allStaff;
    }

    private boolean addStaff(Staff staff) {
        String sql = "INSERT INTO staff (staff_id, name, password, role) VALUES (?, ?, ?, ?)";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            // Encrypt the password before storing
            String encryptedPassword = PasswordEncryption.encryptPassword(staff.getPassword());
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, staff.getId());
                ps.setString(2, staff.getName());
                ps.setString(3, encryptedPassword);
                ps.setString(4, staff.isAdmin() ? "admin" : "staff");
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Staff added successfully: " + staff.getName());
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add staff: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }

    public boolean updateStaff(Staff staff) {
        String sql = "UPDATE staff SET name = ?, password = ?, role = ? WHERE staff_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            
            // Encrypt the password before storing
            String encryptedPassword = PasswordEncryption.encryptPassword(staff.getPassword());
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, staff.getName());
                ps.setString(2, encryptedPassword);
                ps.setString(3, staff.isAdmin() ? "admin" : "staff");
                ps.setString(4, staff.getId());
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update staff: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }
    
    public boolean deleteStaff(String staffId) {
        String sql = "DELETE FROM staff WHERE staff_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, staffId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Staff deleted successfully: " + staffId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete staff: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }
}


