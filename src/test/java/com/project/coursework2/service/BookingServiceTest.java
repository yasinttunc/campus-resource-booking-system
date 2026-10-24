package com.project.coursework2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.coursework2.data.BookingsDatabaseManager;
import com.project.coursework2.data.ResourcesDatabaseManager.ResourceRow;
import com.project.coursework2.data.TestDatabase;
import com.project.coursework2.data.UserDatabaseManager;
import com.project.coursework2.model.Booking;
import com.project.coursework2.model.User;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises {@link BookingService#createBooking} end to end against a real SQLite database
 * built from schema.sql and seed.sql.
 */
class BookingServiceTest {

    private static final String STUDY_ROOM_B = "RES002";      // Student, active
    private static final String STUDY_ROOM_A = "RES001";      // Student, inactive
    private static final String BIOLOGY_LAB = "RES009";       // Staff only

    @TempDir
    Path tempDir;

    private User student;
    private String nextWeek;

    @BeforeEach
    void setUp() throws SQLException {
        TestDatabase.useFreshDatabase(tempDir);
        student = UserDatabaseManager.getUser("emily.carter@student.university.ac.uk", "emily123");
        assertNotNull(student, "seed user must be able to log in");
        nextWeek = LocalDate.now().plusDays(7).toString();
    }

    @AfterEach
    void tearDown() {
        TestDatabase.reset();
    }

    @Test
    void createsPendingBookingForValidRequest() throws SQLException {
        String id = BookingService.createBooking(student, resource(STUDY_ROOM_B), nextWeek, "10:00", "11:00");

        Booking saved = BookingsDatabaseManager.getBookingsByUser(student.getUserID()).stream()
                .filter(b -> b.getBookingID().equals(id))
                .findFirst()
                .orElseThrow();
        assertEquals("pending", saved.getStatus().toLowerCase());
        assertEquals(nextWeek, saved.getDate());
    }

    @Test
    void rejectsDateInThePast() throws SQLException {
        String yesterday = LocalDate.now().minusDays(1).toString();

        assertRejected("past", () -> BookingService.createBooking(
                student, resource(STUDY_ROOM_B), yesterday, "10:00", "11:00"));
    }

    @Test
    void rejectsInactiveResource() throws SQLException {
        assertRejected("not available", () -> BookingService.createBooking(
                student, resource(STUDY_ROOM_A), nextWeek, "10:00", "11:00"));
    }

    @Test
    void rejectsResourceThatRequiresAHigherRole() throws SQLException {
        assertRejected("required role", () -> BookingService.createBooking(
                student, resource(BIOLOGY_LAB), nextWeek, "10:00", "11:00"));
    }

    @Test
    void rejectsOverlappingBookingButAllowsAdjacentSlot() throws SQLException {
        ResourceRow room = resource(STUDY_ROOM_B);
        BookingService.createBooking(student, room, nextWeek, "10:00", "11:00");

        assertRejected("already booked", () -> BookingService.createBooking(
                student, room, nextWeek, "10:30", "11:30"));
        assertNotNull(BookingService.createBooking(student, room, nextWeek, "11:00", "12:00"),
                "a slot that starts when the previous one ends is not a conflict");
    }

    @Test
    void cancelledBookingFreesTheSlot() throws SQLException {
        ResourceRow room = resource(STUDY_ROOM_B);
        String id = BookingService.createBooking(student, room, nextWeek, "14:00", "15:00");

        BookingService.cancelBooking(id);

        assertNotNull(BookingService.createBooking(student, room, nextWeek, "14:00", "15:00"));
    }

    @Test
    void rejectsSlotDuringMaintenance() throws SQLException {
        TestDatabase.execute("INSERT INTO MaintenanceWindow (resourceID, reason, startDate, startTime, endDate, endTime) "
                + "VALUES ('" + STUDY_ROOM_B + "', 'Deep clean', '" + nextWeek + "', '09:00', '" + nextWeek + "', '12:00')");

        assertRejected("maintenance", () -> BookingService.createBooking(
                student, resource(STUDY_ROOM_B), nextWeek, "10:00", "11:00"));
    }

    @Test
    void rejectsBookingOnceActiveQuotaIsReached() throws SQLException {
        ResourceRow room = resource(STUDY_ROOM_B);
        int quota = student.getMaxActiveBookings();
        for (int i = 0; i < quota; i++) {
            String day = LocalDate.now().plusDays(7 + i).toString();
            BookingService.createBooking(student, room, day, "09:00", "10:00");
        }

        String extraDay = LocalDate.now().plusDays(7 + quota).toString();
        assertRejected("limit", () -> BookingService.createBooking(student, room, extraDay, "09:00", "10:00"));
    }

    private static ResourceRow resource(String id) throws SQLException {
        return ResourceService.getAllResources().stream()
                .filter(r -> r.getResourceId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed resource " + id + " missing"));
    }

    private static void assertRejected(String expectedMessagePart, BookingAttempt attempt) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, attempt::run);
        assertTrue(e.getMessage().toLowerCase().contains(expectedMessagePart),
                "unexpected message: " + e.getMessage());
    }

    @FunctionalInterface
    private interface BookingAttempt {
        void run() throws Exception;
    }
}
