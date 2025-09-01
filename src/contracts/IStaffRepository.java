package contracts;

import domain.Staff;

public interface IStaffRepository {
    Staff findById(String staffId);
    boolean save(Staff staff);
    boolean registerStaff(String staffId, String name, String password, boolean isAdmin);
    Staff login(String staffId, String password);
    boolean verifyPassword(String staffId, String password);
}