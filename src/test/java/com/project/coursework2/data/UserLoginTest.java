package com.project.coursework2.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.coursework2.model.User;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Login against a real SQLite database built from schema.sql and seed.sql. */
class UserLoginTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TestDatabase.useFreshDatabase(tempDir);
    }

    @AfterEach
    void tearDown() {
        TestDatabase.reset();
    }

    @Test
    void logsInWithEmailAndCorrectPassword() throws SQLException {
        User user = UserDatabaseManager.getUser("emily.carter@student.university.ac.uk", "emily123");

        assertNotNull(user);
        assertEquals("USR011", user.getUserID());
        assertEquals("Student", user.getRole());
    }

    @Test
    void rejectsWrongPasswordAndUnknownUser() throws SQLException {
        assertNull(UserDatabaseManager.getUser("emily.carter@student.university.ac.uk", "wrong"));
        assertNull(UserDatabaseManager.getUser("nobody@university.ac.uk", "emily123"));
    }

    @Test
    void logsInWithStudentId() throws SQLException {
        User user = UserDatabaseManager.getUser("STU2021001", "emily123");

        assertNotNull(user);
        assertEquals("USR011", user.getUserID());
    }

    @Test
    void upgradesLegacySha256HashOnSuccessfulLogin() throws SQLException {
        // SHA-256("admin"), as stored by the original version of the app
        TestDatabase.execute("UPDATE User SET password = "
                + "'8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918' WHERE userID = 'ADM003'");

        assertNotNull(UserDatabaseManager.getUser("admin", "admin"));

        String stored = storedHash("ADM003");
        assertTrue(stored.startsWith("pbkdf2$"), "legacy hash should be replaced after login");
        assertNotNull(UserDatabaseManager.getUser("admin", "admin"), "upgraded hash must still verify");
    }

    private static String storedHash(String userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password FROM User WHERE userID = ?")) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
