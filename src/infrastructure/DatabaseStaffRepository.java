package infrastructure;

import contracts.IStaffRepository;
import domain.Staff;

import java.sql.*;

public class DatabaseStaffRepository implements IStaffRepository {
    public DatabaseStaffRepository() { }

    @Override
    public Staff findById(String staffId) {
        String sql = "SELECT staff_id, name, password, role FROM staff WHERE staff_id = ?";
        Connection con = DatabaseRepository.createNewConnection();
        try {
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
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
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
        if (s != null && s.getPassword().equals(password)) {
            System.out.println("✅ Staff login successful: " + s.getName());
            return s;
        }
        System.out.println("❌ Invalid staff credentials");
        return null;
    }

    private boolean addStaff(Staff staff) {
        String sql = "INSERT INTO staff (staff_id, name, password, role) VALUES (?, ?, ?, ?)";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return false;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, staff.getId());
                ps.setString(2, staff.getName());
                ps.setString(3, staff.getPassword());
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
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    private boolean updateStaff(Staff staff) {
        String sql = "UPDATE staff SET name = ?, password = ?, role = ? WHERE staff_id = ?";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return false;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, staff.getName());
                ps.setString(2, staff.getPassword());
                ps.setString(3, staff.isAdmin() ? "admin" : "staff");
                ps.setString(4, staff.getId());
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update staff: " + e.getMessage());
            return false;
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException ignored) {}
            }
        }
    }
}


