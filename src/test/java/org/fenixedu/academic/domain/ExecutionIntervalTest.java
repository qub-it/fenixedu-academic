package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionIntervalTest {

    private static AcademicCalendarRootEntry rootEntry;
    private static AcademicYearCE academicYearEntryFirst;
    private static AcademicYearCE academicYearEntrySecond;
    private static AcademicYearCE academicYearEntryThird;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initRootCalendarAndExecutionYears();
            return null;
        });
    }

    static void initRootCalendarAndExecutionYears() {

        if (rootEntry != null) { // if initialization was already executed
            return;
        }

        rootEntry = new AcademicCalendarRootEntry(new LocalizedString().with(Locale.getDefault(), "Root entry"), null);
        Bennu.getInstance().setDefaultAcademicCalendar(rootEntry);

        final int year = new LocalDate().getYear();

        academicYearEntryFirst = createYearInterval(year - 1);
        academicYearEntrySecond = createYearInterval(year);
        academicYearEntryThird = createYearInterval(year + 1);

//        final ExecutionYear currentExecutionYear = (ExecutionYear) academicYearEntrySecond.getExecutionInterval();
//        currentExecutionYear.setState(PeriodState.CURRENT);
        academicYearEntrySecond.getExecutionInterval().setState(PeriodState.CURRENT);

        createFirstSemesterInterval(academicYearEntryFirst);
        createSecondSemesterInterval(academicYearEntryFirst);

        createFirstSemesterInterval(academicYearEntrySecond).getExecutionInterval().setState(PeriodState.CURRENT);
        createSecondSemesterInterval(academicYearEntrySecond);

        createFirstSemesterInterval(academicYearEntryThird);
        createSecondSemesterInterval(academicYearEntryThird);

    }

    private static AcademicYearCE createYearInterval(final int year) {
        return new AcademicYearCE(rootEntry, new LocalizedString().with(Locale.getDefault(), year + "/" + (year + 1)), null,
                new LocalDate(year, 9, 1).toDateTimeAtStartOfDay(), new LocalDate(year + 1, 8, 30).toDateTimeAtStartOfDay(),
                rootEntry);
    }

    private static AcademicIntervalCE createFirstSemesterInterval(AcademicYearCE academicYearEntry) {
        final int year = academicYearEntry.getBegin().getYear();
        final AcademicIntervalCE firstSemesterEntry = new AcademicIntervalCE(AcademicPeriod.SEMESTER, academicYearEntry,
                new LocalizedString().with(Locale.getDefault(), "1st Semester"), null, new DateTime(year, 9, 1, 0, 0, 0),
                new DateTime(year + 1, 1, 31, 23, 59, 59), rootEntry);

        firstSemesterEntry.getExecutionInterval().setState(PeriodState.OPEN);
        return firstSemesterEntry;
    }

    private static AcademicIntervalCE createSecondSemesterInterval(AcademicYearCE academicYearEntry) {
        final int year = academicYearEntry.getBegin().getYear();
        final AcademicIntervalCE secondSemesterEntry = new AcademicIntervalCE(AcademicPeriod.SEMESTER, academicYearEntry,
                new LocalizedString().with(Locale.getDefault(), "2nd Semester"), null, new DateTime(year + 1, 2, 1, 0, 0, 0),
                new DateTime(year + 1, 8, 31, 23, 59, 59), rootEntry);
        secondSemesterEntry.getExecutionInterval().setState(PeriodState.OPEN);
        return secondSemesterEntry;
    }

    @Test
    public void testExecutionInterval() {
        assertTrue("Root entry".equals(rootEntry.getTitle().getContent()));
        assertTrue(academicYearEntryFirst.getExecutionInterval() != null);
        assertTrue(academicYearEntryFirst.getExecutionInterval().isBefore(academicYearEntrySecond.getExecutionInterval()));
        assertTrue(ExecutionYear.findAllAggregators().size() == 3);
        assertEquals(ExecutionYear.findCurrent(null), academicYearEntrySecond.getExecutionInterval());
    }

}
