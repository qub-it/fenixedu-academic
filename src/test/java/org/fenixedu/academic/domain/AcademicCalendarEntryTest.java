package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
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

    private static AcademicCalendarRootEntry rootEntry;
    private static AcademicYearCE year2025, year2026;
    private static AcademicIntervalCE semester1_2025, semester2_2025, semester1_2026;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initSetup();
            return null;
        });
    }

    private static void initSetup() {
        rootEntry = new AcademicCalendarRootEntry(buildLocalizedString("Test Calendar"), buildLocalizedString("Test Calendar Description"));

        year2025 = new AcademicYearCE(rootEntry, buildLocalizedString("2025/2026"), buildLocalizedString("Year 2025/2026"),
                new DateTime(2025, 9, 1, 0, 0), new DateTime(2026, 8, 31, 23, 59), rootEntry);

        year2026 = new AcademicYearCE(rootEntry, buildLocalizedString("2026/2027"), buildLocalizedString("Year 2026/2027"),
                new DateTime(2026, 9, 1, 0, 0), new DateTime(2027, 8, 31, 23, 59), rootEntry);

        semester1_2025 = new AcademicIntervalCE(AcademicPeriod.SEMESTER, year2025, buildLocalizedString("1st Semester"),
                buildLocalizedString("1st Semester 2025/2026"), new DateTime(2025, 9, 1, 0, 0), new DateTime(2026, 1, 31, 23, 59),
                rootEntry);

        semester2_2025 = new AcademicIntervalCE(AcademicPeriod.SEMESTER, year2025, buildLocalizedString("2nd Semester"),
                buildLocalizedString("2nd Semester 2025/2026"), new DateTime(2026, 2, 1, 0, 0), new DateTime(2026, 8, 31, 23, 59),
                rootEntry);

        semester1_2026 = new AcademicIntervalCE(AcademicPeriod.SEMESTER, year2026, buildLocalizedString("1st Semester"),
                buildLocalizedString("1st Semester 2026/2027"), new DateTime(2026, 9, 1, 0, 0), new DateTime(2027, 1, 31, 23, 59),
                rootEntry);
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

        assertEquals(2, childEntries.size());
        assertTrue(childEntries.contains(year2025));
        assertTrue(childEntries.contains(year2026));

        // Now test getAllChildEntries recursiveness

        childEntries = rootEntry.getAllChildEntries(AcademicPeriod.SEMESTER);
        assertEquals(3, childEntries.size());
        assertTrue(childEntries.contains(semester1_2025));
        assertTrue(childEntries.contains(semester2_2025));
        assertTrue(childEntries.contains(semester1_2026));

        // getAllChildEntries returns null for null AcademicPeriod
        assertTrue(rootEntry.getAllChildEntries(null).isEmpty());
    }

    @Test
    public void testAcademicCalendarEntry_getNextAcademicCalendarEntry() {
        assertEquals(year2026, year2025.getNextAcademicCalendarEntry());

        // lastEntry.getNext returns null
        assertNull(year2026.getNextAcademicCalendarEntry());

        assertEquals(semester1_2026, semester2_2025.getNextAcademicCalendarEntry());

        // lastEntry.getNext returns null
        assertNull(semester1_2026.getNextAcademicCalendarEntry());
    }

    @Test
    public void testAcademicCalendarEntry_getPreviousAcademicCalendarEntry() {
        assertEquals(year2025, year2026.getPreviousAcademicCalendarEntry());

        // firstEntry.getPrevious returns null
        assertNull(year2025.getPreviousAcademicCalendarEntry());

        assertEquals(semester2_2025, semester1_2026.getPreviousAcademicCalendarEntry());

        // firstEntry.getPrevious returns null
        assertNull(semester1_2025.getPreviousAcademicCalendarEntry());
    }
}
