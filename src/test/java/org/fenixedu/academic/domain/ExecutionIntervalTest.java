package org.fenixedu.academic.domain;

import java.time.Year;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Locale;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;
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

import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionIntervalTest {

    /*
     * Child execution intervals created:
     *  > 1st Semester 2019/2020
     *  > 2nd Semester 2019/2020
     *  > 1st Semester 2020/2021 (current)
     *  > 2nd Semester 2020/2021
     *  > 1st Semester 2021/2022
     *  > 2nd Semester 2021/2022
     *  > 1st Semester 2022/2023
     *  > 2nd Semester 2022/2023
     *  > 1st Semester 2023/2024
     *  > 2nd Semester 2023/2024
     */

    private static final int CURRENT_YEAR = 2020;
    private static AcademicYearCE academicYearEntryFirst;
    private static AcademicYearCE academicYearEntrySecond;
    private static AcademicYearCE academicYearEntryThird;
    private static AcademicYearCE academicYearEntryFourth;
    private static AcademicYearCE academicYearEntryFifth;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initRootCalendarAndExecutionYears();
            return null;
        });
    }

    public static void initRootCalendarAndExecutionYears() {
        if (Bennu.getInstance().getDefaultAcademicCalendar() != null) {// if initialization was already executed
            return;
        }

        AcademicCalendarRootEntry rootEntry =
                new AcademicCalendarRootEntry(new LocalizedString().with(Locale.getDefault(), "Root entry"), null);
        Bennu.getInstance().setDefaultAcademicCalendar(rootEntry);

        final int year = CURRENT_YEAR;

        academicYearEntryFirst = createStandardYearInterval(rootEntry, year - 1);
        academicYearEntrySecond = createStandardYearInterval(rootEntry, year);
        academicYearEntryThird = createStandardYearInterval(rootEntry, year + 1);
        academicYearEntryFourth = createStandardYearInterval(rootEntry, year + 2);
        academicYearEntryFifth = createStandardYearInterval(rootEntry, year + 3);

//        final ExecutionYear currentExecutionYear = (ExecutionYear) academicYearEntrySecond.getExecutionInterval();
//        currentExecutionYear.setState(PeriodState.CURRENT);
//        academicYearEntrySecond.getExecutionInterval().changeState(PeriodState.CURRENT);

        createFirstSemesterInterval(academicYearEntryFirst);
        createSecondSemesterInterval(academicYearEntryFirst);

        createFirstSemesterInterval(academicYearEntrySecond).getExecutionInterval().changeState(PeriodState.CURRENT);
        createSecondSemesterInterval(academicYearEntrySecond);

        createFirstSemesterInterval(academicYearEntryThird);
        createSecondSemesterInterval(academicYearEntryThird);

        createFirstSemesterInterval(academicYearEntryFourth);
        createSecondSemesterInterval(academicYearEntryFourth);

        createFirstSemesterInterval(academicYearEntryFifth);
        createSecondSemesterInterval(academicYearEntryFifth);

        AcademicCalendarRootEntry civilCalendar =
                new AcademicCalendarRootEntry(new LocalizedString().with(Locale.getDefault(), "Civil Calendar"), null);

        createCivilYearIntervalAndMonths(civilCalendar, year - 1);
        createCivilYearIntervalAndMonths(civilCalendar, year);
        createCivilYearIntervalAndMonths(civilCalendar, year + 1);
        createCivilYearIntervalAndMonths(civilCalendar, year + 2);
        createCivilYearIntervalAndMonths(civilCalendar, year + 3);

        AcademicPeriodOrder.initialize();
    }

    private static AcademicYearCE createStandardYearInterval(final AcademicCalendarRootEntry calendar, final int year) {
        return createYearInterval(calendar, year + "/" + (year + 1), new LocalDate(year, 9, 1), new LocalDate(year + 1, 8, 30));
    }

    private static AcademicYearCE createYearInterval(AcademicCalendarRootEntry calendar, String name, LocalDate startDate,
            LocalDate endDate) {
        return new AcademicYearCE(calendar, new LocalizedString().with(Locale.getDefault(), name), null,
                startDate.toDateTimeAtStartOfDay(), endDate.toDateTimeAtStartOfDay(), calendar);
    }
    private static AcademicIntervalCE createFirstSemesterInterval(AcademicYearCE academicYearEntry) {
        final int year = academicYearEntry.getBegin().getYear();
        final AcademicIntervalCE firstSemesterEntry = new AcademicIntervalCE(AcademicPeriod.SEMESTER, academicYearEntry,
                new LocalizedString().with(Locale.getDefault(), "1st Semester"), null, new DateTime(year, 9, 1, 0, 0, 0),
                new DateTime(year + 1, 1, 31, 23, 59, 59), academicYearEntry.getRootEntry());

        firstSemesterEntry.getExecutionInterval().setState(PeriodState.OPEN);
        return firstSemesterEntry;
    }

    private static AcademicIntervalCE createSecondSemesterInterval(AcademicYearCE calendar) {
        final int year = calendar.getBegin().getYear();
        final AcademicIntervalCE secondSemesterEntry = new AcademicIntervalCE(AcademicPeriod.SEMESTER, calendar,
                new LocalizedString().with(Locale.getDefault(), "2nd Semester"), null, new DateTime(year + 1, 2, 1, 0, 0, 0),
                new DateTime(year + 1, 8, 31, 23, 59, 59), calendar.getRootEntry());
        secondSemesterEntry.getExecutionInterval().setState(PeriodState.OPEN);
        return secondSemesterEntry;
    }

    private static AcademicYearCE createCivilYearIntervalAndMonths(AcademicCalendarRootEntry calendar, final int year) {
        final AcademicYearCE yearEntry = createCivilYearInterval(calendar, year);
        for (int i = 1; i <= 12; i++) {
            createMonthInterval(yearEntry, i);
        }

        return yearEntry;
    }

    private static AcademicYearCE createCivilYearInterval(AcademicCalendarRootEntry calendar, final int year) {
        return createYearInterval(calendar, String.valueOf(year), new LocalDate(year, 1, 1), new LocalDate(year, 12, 31));
    }

    private static AcademicIntervalCE createMonthInterval(AcademicYearCE academicYearEntry, int month) {
        final int year = academicYearEntry.getBegin().getYear();
        final AcademicIntervalCE monthEntry = new AcademicIntervalCE(AcademicPeriod.MONTH, academicYearEntry,
                new LocalizedString().with(Locale.getDefault(), "Month " + month), null, new DateTime(year, month, 1, 0, 0, 0),
                new DateTime(year, month, YearMonth.of(year, month).lengthOfMonth(), 0, 0, 0), academicYearEntry.getRootEntry());
        monthEntry.getExecutionInterval().setState(PeriodState.OPEN);
        return monthEntry;
    }

    @Test
    public void testExecutionInterval() {
        ExecutionYear academicYearSecond = (ExecutionYear) academicYearEntrySecond.getExecutionInterval();

        assertTrue("Root entry".equals(Bennu.getInstance().getDefaultAcademicCalendar().getTitle().getContent()));
        assertTrue(academicYearEntryFirst.getExecutionInterval() != null);
        assertTrue(academicYearEntryFirst.getExecutionInterval().isBefore(academicYearSecond));
        assertTrue(ExecutionYear.findAllAggregators().size() == 10);

        ExecutionInterval firstSemSecondYear = academicYearSecond.getChildInterval(1, AcademicPeriod.SEMESTER);
        firstSemSecondYear.changeState(PeriodState.CURRENT);
        assertEquals(ExecutionYear.findCurrent(null), academicYearSecond);

        final Collection<ExecutionInterval> currentsChilds = ExecutionInterval.findCurrentsChilds();
        assertEquals(currentsChilds.size(), 1);

        final ExecutionInterval currentInterval = currentsChilds.iterator().next();
        assertEquals(currentInterval, firstSemSecondYear);
        assertEquals(currentInterval.getExecutionYear(), academicYearSecond);
    }

    @Test
    public void testShortNameFormat() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        assertEquals("A", executionYear.getShortName());
        assertEquals("S1", executionYear.getChildInterval(1, AcademicPeriod.SEMESTER).getShortName());
        assertEquals("S2", executionYear.getChildInterval(2, AcademicPeriod.SEMESTER).getShortName());
    }

    @Test
    public void testQualifiedNameWithLocale() {
        final AcademicCalendarRootEntry rootEntry = Bennu.getInstance().getDefaultAcademicCalendar();
        final AcademicYearCE yearEntry = createStandardYearInterval(rootEntry, 2000);
        final ExecutionInterval executionYear = yearEntry.getExecutionInterval();

        final AcademicIntervalCE semesterEntry = createFirstSemesterInterval(yearEntry);
        final ExecutionInterval executionSemester = semesterEntry.getExecutionInterval();

        final Locale defaultLocale = Locale.getDefault();
        final Locale pt = new Locale("pt");

        assertEquals("2000/2001", executionYear.getQualifiedName());
        assertEquals("1st Semester 2000/2001", executionSemester.getQualifiedName());

        final LocalizedString yearNewTitle =
                new LocalizedString.Builder().with(pt, "2000/2001 pt").with(defaultLocale, "2000/2001 default").build();
        yearEntry.setTitle(yearNewTitle);
        assertEquals(yearNewTitle, executionYear.getQualifiedNameI18N());
        assertEquals("2000/2001", executionYear.getQualifiedName()); // ExecutionYear implementation falls back to getName()
        assertEquals("2000/2001 default", executionYear.getQualifiedNameI18N().getContent());
        assertEquals("2000/2001 default", executionYear.getQualifiedNameI18N().getContent(defaultLocale));
        assertEquals("2000/2001 pt", executionYear.getQualifiedNameI18N().getContent(pt));
        assertEquals("2000/2001 default",
                executionYear.getQualifiedNameI18N().getContent(Locale.FRANCE)); // falls back to default locale

        final LocalizedString semesterNewTitle =
                new LocalizedString.Builder().with(pt, "1o Semestre pt").with(defaultLocale, "1st Semester default").build();
        semesterEntry.setTitle(semesterNewTitle);
        assertEquals("1st Semester default 2000/2001",
                executionSemester.getQualifiedName()); // ExecutionYear implementation falls back to getName()

        final LocalizedString semesterNewQualifiedName = new LocalizedString.Builder().with(pt, "1o Semestre pt 2000/2001 pt")
                .with(defaultLocale, "1st Semester default 2000/2001 default").build();
        assertEquals(semesterNewQualifiedName, executionSemester.getQualifiedNameI18N());
        assertEquals("1st Semester default 2000/2001 default", executionSemester.getQualifiedNameI18N().getContent());
        assertEquals("1st Semester default 2000/2001 default",
                executionSemester.getQualifiedNameI18N().getContent(defaultLocale));
        assertEquals("1o Semestre pt 2000/2001 pt", executionSemester.getQualifiedNameI18N().getContent(pt));
        assertEquals("1st Semester default 2000/2001 default",
                executionSemester.getQualifiedNameI18N().getContent(Locale.FRANCE));

        semesterEntry.delete(rootEntry);
        yearEntry.delete(rootEntry);
    }

    @Test
    public void testChangeStateToCurrentSwitchesPrevious() {
        final AcademicCalendarRootEntry rootEntry = Bennu.getInstance().getDefaultAcademicCalendar();
        AcademicYearCE year1 = createStandardYearInterval(rootEntry, 1950);
        AcademicYearCE year2 = createStandardYearInterval(rootEntry, 1951);
        final ExecutionInterval semYear1 = createFirstSemesterInterval(year1).getExecutionInterval();
        final ExecutionInterval semYear2 = createFirstSemesterInterval(year2).getExecutionInterval();

        // make first semester of year1 current
        semYear1.changeState(PeriodState.CURRENT);
        assertTrue(semYear1.isCurrent());
        assertEquals(PeriodState.CURRENT, semYear1.getExecutionYear().getState());

        // now set first semester of year2 current -> should deactivate (open) the previous
        semYear2.changeState(PeriodState.CURRENT);
        assertTrue(semYear2.isCurrent());
        assertEquals(PeriodState.CURRENT, semYear2.getExecutionYear().getState());

        // previous semester and its year must be opened
        assertEquals(PeriodState.OPEN, semYear1.getState());
        assertEquals(PeriodState.OPEN, semYear1.getExecutionYear().getState());

        // tear down
        semYear1.getAcademicCalendarEntry().delete(rootEntry);
        semYear2.getAcademicCalendarEntry().delete(rootEntry);
        year1.delete(rootEntry);
        year2.delete(rootEntry);
    }

}
