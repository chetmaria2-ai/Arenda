import com.realestate.config.SessionManager;
import org.junit.Test;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {
    @BeforeEach
    void setUp() {
        SessionManager.clear();
    }
    @Test
    public void testUserRoleAfterLogin() {
        SessionManager.setUser("uid1", "Рустам", "+79001234567", "user", false);
        assertTrue(SessionManager.isLoggedIn());
        assertFalse(SessionManager.isAdmin());
        assertFalse(SessionManager.isGuest());
    }
    @Test
    public void testAdminRoleAfterLogin() {
        SessionManager.setUser("uid2", "Админ", null, "admin", false);
        assertTrue(SessionManager.isAdmin());
        assertTrue(SessionManager.isLoggedIn());
    }
    @Test
    public void testClearSession() {
        SessionManager.setUser("uid1", "Рустам", null, "user", false);
        SessionManager.clear();
        assertTrue(SessionManager.isGuest());
        assertFalse(SessionManager.isLoggedIn());
    }
    @Test
    public void testBlockedUser() {
        SessionManager.setUser("uid3", "Тест", null, "user", true);
        assertTrue(SessionManager.isBlocked());
    }
    @Test
    public void testUserData() {
        SessionManager.setUser("uid4", "Рустам", "+79001234567", "user", false);
        assertEquals("uid4", SessionManager.getUserId());
        assertEquals("Рустам", SessionManager.getFullName());
        assertEquals("+79001234567", SessionManager.getPhone());
    }
}
