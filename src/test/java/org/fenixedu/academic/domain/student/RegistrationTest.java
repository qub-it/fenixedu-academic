package org.fenixedu.academic.domain.student;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationTest {

    private static Registration registration;

    private static final String PRESENT_ACADEMIC_YEAR_NAME = "PRESENT_YEAR";
    private static final String FUTURE_ACADEMIC_YEAR_NAME = "FUTURE_YEAR";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initStudentAndRegistration();

            registration = Student.readStudentByNumber(1)
                    .getRegistrationStream()
                    .findAny()
                    .orElseThrow();

            final int presentYear = LocalDate.now().getYear();
            AcademicYearCE presentAcademicYearEntry =
                    createStandardYearInterval(Bennu.getInstance().getDefaultAcademicCalendar(), PRESENT_ACADEMIC_YEAR_NAME,
                            presentYear);
            AcademicYearCE futureAcademicYearEntry =
                    createStandardYearInterval(Bennu.getInstance().getDefaultAcademicCalendar(), FUTURE_ACADEMIC_YEAR_NAME,
                            presentYear + 1);

            createFirstSemesterInterval(presentAcademicYearEntry);
            createFirstSemesterInterval(futureAcademicYearEntry);

            return null;
        });
    }

    private static AcademicYearCE createStandardYearInterval(final AcademicCalendarRootEntry calendar, final String name,
            final int year) {
        return createYearInterval(calendar, name, new LocalDate(year, 9, 1), new LocalDate(year + 1, 8, 30));
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

    @Before
    public void setupTests() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            registration.getRegistrationStatesSet().forEach(RegistrationState::delete);

            final ExecutionYear executionYear2021 = ExecutionYear.readExecutionYearByName("2020/2021");
            final ExecutionYear executionYear2122 = ExecutionYear.readExecutionYearByName("2021/2022");
            executionYear2021.setState(PeriodState.CURRENT);
            executionYear2122.setState(PeriodState.OPEN);

            final ExecutionInterval semester1_2021 = executionYear2021.getChildInterval(1, AcademicPeriod.SEMESTER);
            final ExecutionInterval semester2_2021 = executionYear2021.getChildInterval(2, AcademicPeriod.SEMESTER);
            semester1_2021.setState(PeriodState.CURRENT);
            semester2_2021.setState(PeriodState.OPEN);

            final ExecutionInterval semester1_2122 = executionYear2122.getChildInterval(1, AcademicPeriod.SEMESTER);
            final ExecutionInterval semester2_2122 = executionYear2122.getChildInterval(2, AcademicPeriod.SEMESTER);
            semester1_2122.setState(PeriodState.OPEN);
            semester2_2122.setState(PeriodState.OPEN);

            final ExecutionYear presentExecutionYear = ExecutionYear.readExecutionYearByName(PRESENT_ACADEMIC_YEAR_NAME);
            final ExecutionYear futureExecutionYear = ExecutionYear.readExecutionYearByName(FUTURE_ACADEMIC_YEAR_NAME);
            presentExecutionYear.setState(PeriodState.OPEN);
            futureExecutionYear.setState(PeriodState.OPEN);

            final ExecutionInterval presentFirstSemester = presentExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            final ExecutionInterval futureFirstSemester = futureExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            presentFirstSemester.setState(PeriodState.OPEN);
            futureFirstSemester.setState(PeriodState.OPEN);

            return null;
        });
    }

    @Test
    public void testActiveState_shouldReturnStateFromRegistration() {
        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(),
                ExecutionYear.readCurrentExecutionYear().getFirstExecutionPeriod());

        RegistrationState activeState = registration.getActiveState();

        assertNotNull("Active state should not be null", activeState);
        assertEquals("Active state should be the first state created", activeState, firstState);
        assertTrue("Active state should belong to this registration",
                registration.getRegistrationStatesSet().contains(activeState));
    }

    @Test
    public void testActiveState_sameExecutionInterval_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear executionYear2021 = ExecutionYear.readExecutionYearByName("2020/2021");
        assertTrue(executionYear2021.isCurrent());

        final ExecutionInterval semester1 = executionYear2021.getChildInterval(1, AcademicPeriod.SEMESTER);

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), semester1);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), semester1);

        assertEquals(firstState.getExecutionYear(), lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For the same current execution interval with two states, the active state should be the last one",
                lastState,
                activeState
        );
    }

    @Test
    public void testActiveState_sameYearDifferentExecutionIntervals_shouldReturnLastCreatedState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear executionYear2021 = ExecutionYear.readExecutionYearByName("2020/2021");
        assertTrue(executionYear2021.isCurrent());

        final ExecutionInterval semester1 = executionYear2021.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester2 = executionYear2021.getChildInterval(2, AcademicPeriod.SEMESTER);
        assertTrue(semester1.isCurrent());
        assertFalse(semester2.isCurrent());

        final RegistrationState semester1State =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), semester1);

        final RegistrationState semester2State = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), semester2);

        assertEquals(semester1State.getExecutionYear(), semester2State.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals(
                "For the same year with two states in different execution intervals, the active state should be the last state created",
                semester2State,
                activeState
        );
    }

    @Test
    public void testActiveState_differentExecutionYear_shouldReturnFirstState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear presentExecutionYear = ExecutionYear.readExecutionYearByName(PRESENT_ACADEMIC_YEAR_NAME);
        final ExecutionYear futureExecutionYear = ExecutionYear.readExecutionYearByName(FUTURE_ACADEMIC_YEAR_NAME);
        presentExecutionYear.setState(PeriodState.CURRENT);
        assertTrue(presentExecutionYear.isCurrent());
        assertFalse(futureExecutionYear.isCurrent());

        final ExecutionInterval presentFirstSemester = presentExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval futureFirstSemester = futureExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        presentFirstSemester.setState(PeriodState.CURRENT);
        assertTrue(presentFirstSemester.isCurrent());
        assertFalse(futureFirstSemester.isCurrent());

        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), presentFirstSemester);

        final RegistrationState lastState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().plusDays(1),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), futureFirstSemester);

        assertEquals(presentExecutionYear, firstState.getExecutionYear());
        assertEquals(futureExecutionYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different execution years, the active state should be the state from the current execution year",
                firstState,
                activeState
        );
    }

    @Test
    public void testActiveState_differentExecutionYear_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear presentExecutionYear = ExecutionYear.readExecutionYearByName(PRESENT_ACADEMIC_YEAR_NAME);
        final ExecutionYear futureExecutionYear = ExecutionYear.readExecutionYearByName(FUTURE_ACADEMIC_YEAR_NAME);
        futureExecutionYear.setState(PeriodState.CURRENT);
        assertFalse(presentExecutionYear.isCurrent());
        assertTrue(futureExecutionYear.isCurrent());

        final ExecutionInterval presentFirstSemester = presentExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval futureFirstSemester = futureExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        futureFirstSemester.setState(PeriodState.CURRENT);
        assertFalse(presentFirstSemester.isCurrent());
        assertTrue(futureFirstSemester.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), presentFirstSemester);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), futureFirstSemester);

        assertEquals(presentExecutionYear, firstState.getExecutionYear());
        assertEquals(futureExecutionYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different execution years, the active state should be the state from the current execution year",
                lastState, activeState);
    }

    @Test
    public void testActiveState_pastExecutionYears_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear executionYear2021 = ExecutionYear.readExecutionYearByName("2020/2021");
        final ExecutionYear executionYear2122 = ExecutionYear.readExecutionYearByName("2021/2022");
        executionYear2021.setState(PeriodState.CURRENT);
        assertTrue(executionYear2021.isCurrent());
        assertFalse(executionYear2122.isCurrent());

        final ExecutionInterval semester1_2021 = executionYear2021.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester1_2122 = executionYear2122.getChildInterval(1, AcademicPeriod.SEMESTER);
        semester1_2021.setState(PeriodState.CURRENT);
        assertTrue(semester1_2021.isCurrent());
        assertFalse(semester1_2122.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusMonths(6),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), semester1_2021);

        final RegistrationState lastState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusMonths(5),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), semester1_2122);

        assertEquals(executionYear2021, firstState.getExecutionYear());
        assertEquals(executionYear2122, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different past execution years, the active state should be the last state created",
                lastState, activeState);
    }

}

