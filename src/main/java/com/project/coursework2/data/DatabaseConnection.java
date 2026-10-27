package com.project.coursework2.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class opens the database connection.
 * Other database classes use this class when they need to talk with SQLite.
 *
 * <p>The JDBC URL defaults to {@code jdbc:sqlite:university_booking.db} and can be overridden
 * with the {@code crbas.db.url} system property (the tests use this to run against a
 * temporary database). The first connection to each URL creates the schema and demo data
 * if the database is empty.</p>
 */
public class DatabaseConnection {

    public static final String URL_PROPERTY = "crbas.db.url";
    private static final String DEFAULT_URL = "jdbc:sqlite:university_booking.db";

    private static final Set<String> initialisedUrls = ConcurrentHashMap.newKeySet();

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty(URL_PROPERTY, DEFAULT_URL);
        Connection conn = DriverManager.getConnection(url);
        try (java.sql.Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        if (!initialisedUrls.contains(url)) {
            synchronized (DatabaseConnection.class) {
                if (!initialisedUrls.contains(url)) {
                    DatabaseInitializer.initialiseIfEmpty(conn);
                    initialisedUrls.add(url);
                }
            }
        }
        return conn;
    }
}
