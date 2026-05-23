package com.realestate.config;

public class SessionManager {

    public enum Role {GUEST, USER, ADMIN}

    private static String userId;
    private static String fullName;
    private static String phone;
    private static Role role = Role.GUEST;
    private static boolean blocked;

    public static void setUser(String id, String name, String ph, String roleStr, boolean isBlocked) {
        userId = id;
        fullName = name;
        phone = ph;
        blocked = isBlocked;
        role = "admin".equalsIgnoreCase(roleStr) ? Role.ADMIN : Role.USER;
    }

    public static void clear() {
        userId = fullName = phone = null;
        role = Role.GUEST;
        blocked = false;
        SupabaseConfig.clearSession();
    }

    public static String getUserId() {
        return userId;
    }

    public static String getFullName() {
        return fullName;
    }

    public static String getPhone() {
        return phone;
    }

    public static Role getRole() {
        return role;
    }

    public static boolean isBlocked() {
        return blocked;
    }

    public static boolean isGuest() {
        return role == Role.GUEST;
    }

    public static boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public static boolean isLoggedIn() {
        return role != Role.GUEST;
    }
}
