package com.project.coursework2.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the SQLite schema and loads demo data the first time the application runs.
 *
 * The database file is not kept in version control. When the application starts against
 * an empty database, this class applies {@code /db/schema.sql} followed by
 * {@code /db/seed.sql} inside a single transaction. An existing database is never modified.
 */
final class DatabaseInitializer {

    private static final String SCHEMA = "/db/schema.sql";
    private static final String SEED = "/db/seed.sql";

    private DatabaseInitializer() {}

    static void initialiseIfEmpty(Connection conn) throws SQLException {
        if (hasSchema(conn)) {
            return;
        }
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            runScript(st, SCHEMA);
            runScript(st, SEED);
            conn.commit();
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private static boolean hasSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'User'")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Scripts contain one statement per line; blank lines and "--" comments are skipped. */
    private static void runScript(Statement st, String resource) throws SQLException {
        for (String line : readResource(resource).split("\\R")) {
            String sql = line.strip();
            if (sql.isEmpty() || sql.startsWith("--")) {
                continue;
            }
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }
            st.execute(sql);
        }
    }

    private static String readResource(String resource) {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + resource, e);
        }
    }
}
