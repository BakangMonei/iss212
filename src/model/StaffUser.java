package model;

/**
 * UB staff identity for the logged-in session (non-database demo).
 */
public class StaffUser {
    private final String staffId;
    private final String name;
    private final String department;
    private final Role role;

    public StaffUser(String staffId, String name, String department, Role role) {
        this.staffId = staffId;
        this.name = name;
        this.department = department;
        this.role = role;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public Role getRole() {
        return role;
    }
}
