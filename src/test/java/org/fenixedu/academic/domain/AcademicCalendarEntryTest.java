package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AcademicCalendarEntryTest {

    private static AcademicCalendarRootEntry rootEntry, mixedRootEntry;
    private static AcademicYearCE year2024, year2025, year2026, mixedYear;
    private static AcademicIntervalCE semester2_2024, semester1_2025, semester2_2025, semester1_2026, mixedSemester1, month,
            nextMonth, mixedSemester2, firstOtherInterval, secondOtherInterval;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initSetup();
            return null;
        });
    }

    private static void initSetup() {
        AcademicPeriodOrder.initialize();

        rootEntry = new AcademicCalendarRootEntry(buildLocalizedString("Test Calendar"), buildLocalizedString("Test Calendar Description"));

        year2024 = new AcademicYearCE(rootEntry, buildLocalizedString("2024/2025"), buildLocalizedString("Year 2024/2025"),
                new DateTime(2024, 9, 1, 0, 0), new DateTime(2025, 8, 31, 23, 59), rootEntry);

        year2025 = new AcademicYearCE(rootEntry, buildLocalizedString("2025/2026"), buildLocalizedString("Year 2025/2026"),
                new DateTime(2025, 9, 1, 0, 0), new DateTime(2026, 8, 31, 23, 59), rootEntry);

        year2026 = new AcademicYearCE(rootEntry, buildLocalizedString("2026/2027"), buildLocalizedString("Year 2026/2027"),
                new DateTime(2026, 9, 1, 0, 0), new DateTime(2027, 8, 31, 23, 59), rootEntry);

        semester2_2024 = new AcademicIntervalCE(AcademicPeriod.SEMESTER,
                AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).orElseThrow(), year2024,
                buildLocalizedString("2nd Semester"), buildLocalizedString("2nd Semester 2024/2025"),
                new DateTime(2024, 9, 1, 0, 0), new DateTime(2025, 1, 31, 23, 59), rootEntry);

        semester1_2025 = new AcademicIntervalCE(AcademicPeriod.SEMESTER,
                AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).orElseThrow(), year2025,
                buildLocalizedString("1st Semester"), buildLocalizedString("1st Semester 2025/2026"),
                new DateTime(2025, 9, 1, 0, 0), new DateTime(2026, 1, 31, 23, 59), rootEntry);

        semester2_2025 = new AcademicIntervalCE(AcademicPeriod.SEMESTER,
                AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).orElseThrow(), year2025,
                buildLocalizedString("2nd Semester"), buildLocalizedString("2nd Semester 2025/2026"),
                new DateTime(2026, 2, 1, 0, 0), new DateTime(2026, 8, 31, 23, 59), rootEntry);

        semester1_2026 = new AcademicIntervalCE(AcademicPeriod.SEMESTER,
                AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).orElseThrow(), year2026,
                buildLocalizedString("1st Semester"), buildLocalizedString("1st Semester 2026/2027"),
                new DateTime(2026, 9, 1, 0, 0), new DateTime(2027, 1, 31, 23, 59), rootEntry);

        firstOtherInterval = new AcademicIntervalCE(AcademicPeriod.OTHER, null, year2025, buildLocalizedString("Other 1"),
                buildLocalizedString("Other 1"), new DateTime(2025, 10, 1, 0, 0), new DateTime(2025, 10, 31, 23, 59), rootEntry);

        secondOtherInterval = new AcademicIntervalCE(AcademicPeriod.OTHER, null, year2025, buildLocalizedString("Other 2"),
                buildLocalizedString("Other 2"), new DateTime(2025, 11, 1, 0, 0), new DateTime(2025, 11, 30, 23, 59), rootEntry);

        mixedRootEntry = new AcademicCalendarRootEntry(buildLocalizedString("Mixed Calendar"),
                buildLocalizedString("Mixed Calendar Description"));
        mixedYear = new AcademicYearCE(mixedRootEntry, buildLocalizedString("2030/2031"),
                buildLocalizedString("Year 2030/2031"), new DateTime(2030, 9, 1, 0, 0), new DateTime(2031, 8, 31, 23, 59),
                mixedRootEntry);

        mixedSemester1 = new AcademicIntervalCE(AcademicPeriod.SEMESTER,
                AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).orElseThrow(), mixedYear,
                buildLocalizedString("1st Semester"), buildLocalizedString("1st Semester 2030/2031"),
                new DateTime(2030, 9, 1, 0, 0), new DateTime(2030, 12, 31, 23, 59), mixedRootEntry);

        month = new AcademicIntervalCE(AcademicPeriod.MONTH,
                AcademicPeriodOrder.findBy(AcademicPeriod.MONTH, 1).orElseThrow(), mixedYear,
                buildLocalizedString("January"), buildLocalizedString("January 2031"), new DateTime(2031, 1, 1, 0, 0),
                new DateTime(2031, 1, 31, 23, 59), mixedRootEntry);

        nextMonth = new AcademicIntervalCE(AcademicPeriod.MONTH,
                AcademicPeriodOrder.findBy(AcademicPeriod.MONTH, 2).orElseThrow(), mixedYear,
                buildLocalizedString("February"), buildLocalizedString("February 2031"),
                new DateTime(2031, 2, 1, 0, 0), new DateTime(2031, 2, 28, 23, 59), mixedRootEntry);

        mixedSemester2 = new AcademicIntervalCE(AcademicPeriod.SEMESTER,
                AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).orElseThrow(), mixedYear,
                buildLocalizedString("2nd Semester"), buildLocalizedString("2nd Semester 2030/2031"),
                new DateTime(2031, 2, 1, 0, 0), new DateTime(2031, 5, 31, 23, 59), mixedRootEntry);
    }

    private static LocalizedString buildLocalizedString(String content) {
        return new LocalizedString(Locale.getDefault(), content);
    }

    // ---------------------------
    // AcademicCalendarEntry tests
    // ---------------------------

    @Test
    public void testAcademicCalendarEntry_comparatorByBeginDate() {
        assertTrue(AcademicCalendarEntry.COMPARATOR_BY_BEGIN_DATE.compare(year2025, year2026) < 0);
        assertTrue(AcademicCalendarEntry.COMPARATOR_BY_BEGIN_DATE.compare(year2026, year2025) > 0);
        assertEquals(0, AcademicCalendarEntry.COMPARATOR_BY_BEGIN_DATE.compare(year2025, year2025));
    }

    @Test
    public void testAcademicCalendarEntry_getAllChildEntries() {
        List<AcademicCalendarEntry> childEntries = year2025.getAllChildEntries(AcademicPeriod.SEMESTER);

        assertEquals(2, childEntries.size());
        assertTrue(childEntries.contains(semester1_2025));
        assertTrue(childEntries.contains(semester2_2025));

        childEntries = rootEntry.getAllChildEntries(AcademicPeriod.YEAR);

        assertEquals(3, childEntries.size());
        assertTrue(childEntries.contains(year2024));
        assertTrue(childEntries.contains(year2025));
        assertTrue(childEntries.contains(year2026));

        // Now test getAllChildEntries recursiveness

        childEntries = rootEntry.getAllChildEntries(AcademicPeriod.SEMESTER);
        assertEquals(4, childEntries.size());
        assertTrue(childEntries.contains(semester2_2024));
        assertTrue(childEntries.contains(semester1_2025));
        assertTrue(childEntries.contains(semester2_2025));
        assertTrue(childEntries.contains(semester1_2026));

        // getAllChildEntries returns null for null AcademicPeriod
        assertTrue(rootEntry.getAllChildEntries(null).isEmpty());
    }

    @Test
    public void testAcademicCalendarEntry_getNextAcademicCalendarEntry() {
        assertNull(rootEntry.getNextAcademicCalendarEntry());

        // Ordered by begin date
        assertEquals(year2025, year2024.getNextAcademicCalendarEntry());
        assertEquals(year2026, year2025.getNextAcademicCalendarEntry());
        assertNull(year2026.getNextAcademicCalendarEntry());

        // Ordered by Parent begin date, then AcademicPeriodOrder
        assertEquals(semester1_2025, semester2_2024.getNextAcademicCalendarEntry());
        assertEquals(semester2_2025, semester1_2025.getNextAcademicCalendarEntry());
        assertEquals(semester1_2026, semester2_2025.getNextAcademicCalendarEntry());
        assertNull(semester1_2026.getNextAcademicCalendarEntry());
        assertEquals(nextMonth, month.getNextAcademicCalendarEntry());
        assertEquals(mixedSemester2, mixedSemester1.getNextAcademicCalendarEntry());

        // AcademicPeriod.OTHER entries have no AcademicPeriodOrder and are ordered by begin date
        assertEquals(secondOtherInterval, firstOtherInterval.getNextAcademicCalendarEntry());
        assertNull(secondOtherInterval.getNextAcademicCalendarEntry());
    }

    @Test
    public void testAcademicCalendarEntry_getPreviousAcademicCalendarEntry() {
        assertNull(rootEntry.getPreviousAcademicCalendarEntry());

        // Ordered by begin date
        assertEquals(year2025, year2026.getPreviousAcademicCalendarEntry());
        assertEquals(year2024, year2025.getPreviousAcademicCalendarEntry());
        assertNull(year2024.getPreviousAcademicCalendarEntry());

        // Ordered by Parent begin date, then AcademicPeriodOrder
        assertEquals(semester2_2025, semester1_2026.getPreviousAcademicCalendarEntry());
        assertEquals(semester1_2025, semester2_2025.getPreviousAcademicCalendarEntry());
        assertEquals(semester2_2024, semester1_2025.getPreviousAcademicCalendarEntry());
        assertNull(semester2_2024.getPreviousAcademicCalendarEntry());
        assertEquals(month, nextMonth.getPreviousAcademicCalendarEntry());
        assertEquals(mixedSemester1, mixedSemester2.getPreviousAcademicCalendarEntry());

        // AcademicPeriod.OTHER entries have no AcademicPeriodOrder and are ordered by begin date
        assertEquals(firstOtherInterval, secondOtherInterval.getPreviousAcademicCalendarEntry());
        assertNull(firstOtherInterval.getPreviousAcademicCalendarEntry());
    }

    @Test
    public void testAcademicCalendarEntry_getCardinality() {
        assertEquals(1, semester1_2025.getCardinality());
        assertEquals(2, semester2_2025.getCardinality());
        assertEquals(1, mixedSemester1.getCardinality());
        assertEquals(1, month.getCardinality());
        assertEquals(2, nextMonth.getCardinality());
        assertEquals(2, mixedSemester2.getCardinality());
        assertEquals(0, year2024.getCardinality());
        assertEquals(0, mixedYear.getCardinality());
        assertEquals(0, rootEntry.getCardinality());
        assertEquals(0, firstOtherInterval.getCardinality());
        assertEquals(0, secondOtherInterval.getCardinality());
    }

    // -------------------------------
    // AcademicCalendarRootEntry tests
    // -------------------------------

    @Test
    public void testAcademicCalendarRootEntry_getBegin() {
        assertEquals(year2024.getBegin(), rootEntry.getBegin());
        assertNotEquals(year2025.getBegin(), rootEntry.getBegin());
        assertNotEquals(year2026.getBegin(), rootEntry.getBegin());
    }

    @Test
    public void testAcademicCalendarRootEntry_getEntryByInstant() {
        long instant = new DateTime(2026, 2, 15, 12, 0).getMillis();

        assertEquals(year2025, rootEntry.getEntryByInstant(instant, AcademicPeriod.YEAR));
        assertEquals(semester2_2025, rootEntry.getEntryByInstant(instant, AcademicPeriod.SEMESTER));

        instant = new DateTime(2026, 10, 15, 12, 0).getMillis();

        assertEquals(year2026, rootEntry.getEntryByInstant(instant, AcademicPeriod.YEAR));
        assertEquals(semester1_2026, rootEntry.getEntryByInstant(instant, AcademicPeriod.SEMESTER));

        // Test for invalid instants
        instant = new DateTime(2020, 1, 1, 0, 0).getMillis();

        assertNull(rootEntry.getEntryByInstant(instant, AcademicPeriod.YEAR));
        assertNull(rootEntry.getEntryByInstant(instant, AcademicPeriod.SEMESTER));

        instant = new DateTime(2028, 1, 1, 0, 0).getMillis();

        assertNull(rootEntry.getEntryByInstant(instant, AcademicPeriod.YEAR));
        assertNull(rootEntry.getEntryByInstant(instant, AcademicPeriod.SEMESTER));
    }

    @Test
    public void testAcademicCalendarRootEntry_getEntryIndexByInstant() {
        long instant = new DateTime(2026, 2, 15, 12, 0).getMillis();

        assertEquals(Integer.valueOf(2), rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.YEAR)); // year2025
        assertEquals(Integer.valueOf(3),
                rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.SEMESTER)); // semester2_year2025

        instant = new DateTime(2026, 10, 15, 12, 0).getMillis();

        assertEquals(Integer.valueOf(3), rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.YEAR)); // year2026
        assertEquals(Integer.valueOf(4),
                rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.SEMESTER)); // semester1_year2026

        // Test for invalid instant
        instant = new DateTime(2020, 1, 1, 0, 0).getMillis();

        assertNull(rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.YEAR));
        assertNull(rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.SEMESTER));

        // Scenario in the future: check branch entry.getEnd().isBefore(instant) of getEntryIndexByInstant
        instant = new DateTime(2028, 1, 1, 0, 0).getMillis();

        assertEquals(Integer.valueOf(3), rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.YEAR)); // year2026
        assertEquals(Integer.valueOf(4),
                rootEntry.getEntryIndexByInstant(instant, AcademicPeriod.SEMESTER)); // semester1_year2026
    }

    @Test
    public void testAcademicCalendarRootEntry_getEntryByIndex() {
        assertEquals(year2024, rootEntry.getEntryByIndex(1, AcademicPeriod.YEAR));
        assertEquals(year2025, rootEntry.getEntryByIndex(2, AcademicPeriod.YEAR));
        assertEquals(year2026, rootEntry.getEntryByIndex(3, AcademicPeriod.YEAR));

        assertEquals(semester2_2024, rootEntry.getEntryByIndex(1, AcademicPeriod.SEMESTER));
        assertEquals(semester1_2025, rootEntry.getEntryByIndex(2, AcademicPeriod.SEMESTER));
        assertEquals(semester2_2025, rootEntry.getEntryByIndex(3, AcademicPeriod.SEMESTER));
        assertEquals(semester1_2026, rootEntry.getEntryByIndex(4, AcademicPeriod.SEMESTER));

        assertNull(rootEntry.getEntryByIndex(0, AcademicPeriod.YEAR));
        assertNull(rootEntry.getEntryByIndex(5, AcademicPeriod.SEMESTER));
    }

    @Test
    public void testAcademicCalendarRootEntry_getAcademicCalendarByTitle() {
        assertEquals(rootEntry, AcademicCalendarRootEntry.getAcademicCalendarByTitle("Test Calendar"));
        assertNull(AcademicCalendarRootEntry.getAcademicCalendarByTitle("Non Existent Calendar"));
    }
}
