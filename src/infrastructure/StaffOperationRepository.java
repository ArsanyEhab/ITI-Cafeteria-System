package infrastructure;

import java.sql.*;
import java.util.*;

public class StaffOperationRepository {

    Connection con;

    public StaffOperationRepository() {
        con = DatabaseRepository.getConnection();
    }

    // STAFF OPERATIONS
    // =================================================================
    
    public Map<String, Object> getStaffById(String staffId) {
        String sql = "SELECT * FROM staff WHERE staff_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> staff = new HashMap<>();
                    staff.put("staff_id", rs.getString("staff_id"));
                    staff.put("name", rs.getString("name"));
                    staff.put("role", rs.getString("role"));
                    return staff;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch staff member: " + e.getMessage());
        }
        return null;
    }
    
    public boolean validateStaffCredentials(String staffId, String password) {
        String sql = "SELECT * FROM staff WHERE staff_id = ? AND password = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Staff login validation failed: " + e.getMessage());
            return false;
        }
    }

    public String getStaffRole(String staffId) {
        String sql = "SELECT role FROM staff WHERE staff_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get staff role: " + e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> getAllStaff() {
        List<Map<String, Object>> staffList = new ArrayList<>();
        String sql = "SELECT staff_id, name, role FROM staff";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> staff = new HashMap<>();
                staff.put("staff_id", rs.getString("staff_id"));
                staff.put("name", rs.getString("name"));
                staff.put("role", rs.getString("role"));
                staffList.add(staff);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch staff list: " + e.getMessage());
        }
        return staffList;
    }

    public boolean addStaffMember(String staffId, String name, String password, String role) {
        String sql = "INSERT INTO staff (staff_id, name, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            pstmt.setString(2, name);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add staff member: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateStaffMember(String staffId, String name, String password, String role) {
        StringBuilder sqlBuilder = new StringBuilder("UPDATE staff SET ");
        List<Object> params = new ArrayList<>();
        boolean hasUpdates = false;
        
        if (name != null && !name.isEmpty()) {
            sqlBuilder.append("name = ?");
            params.add(name);
            hasUpdates = true;
        }
        
        if (password != null && !password.isEmpty()) {
            if (hasUpdates) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("password = ?");
            params.add(password);
            hasUpdates = true;
        }
        
        if (role != null && !role.isEmpty()) {
            if (hasUpdates) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("role = ?");
            params.add(role);
            hasUpdates = true;
        }
        
        if (!hasUpdates) {
            return false; // Nothing to update
        }
        
        sqlBuilder.append(" WHERE staff_id = ?");
        params.add(staffId);
        
        try (PreparedStatement pstmt = con.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to update staff member: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteStaffMember(String staffId) {
        String sql = "DELETE FROM staff WHERE staff_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete staff member: " + e.getMessage());
            return false;
        }
    }
    
}
