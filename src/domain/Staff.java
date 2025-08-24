package domain;

import contracts.IUser;

public class Staff implements IUser {
    private String staffId;
    private String name;
    private String password;
    private boolean isAdmin; // true = admin staff, false = normal staff

    public Staff(String staffId, String name, String password, boolean isAdmin) {
        this.staffId = staffId;
        this.name = name;
        this.password = password;
        this.isAdmin = isAdmin;
    }

    @Override
    public String getId() { return staffId; }
    @Override
    public String getName() { return name; }
    @Override
    public String getPassword() { return password; }

    public boolean isAdmin() { return isAdmin; }
}

