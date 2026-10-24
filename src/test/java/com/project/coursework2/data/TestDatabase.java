package com.project.coursework2.data;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Points {@link DatabaseConnection} at a fresh SQLite file for one test.
 * The first connection creates the schema and loads the demo data from src/main/resources/db.
 */
public final class TestDatabase {

    private TestDatabase() {}

    public static void useFreshDatabase(Path dir) {
        System.setProperty(DatabaseConnection.URL_PROPERTY,
                "jdbc:sqlite:" + dir.resolve("crbas-test.db").toAbsolutePath());
    }

    public static void reset() {
        System.clearProperty(DatabaseConnection.URL_PROPERTY);
    }

    public static void execute(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }
}
