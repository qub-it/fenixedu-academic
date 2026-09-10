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

    private static ExecutionYear executionYear, nextExecutionYear, presentYear, futureYear;
    private static ExecutionInterval firstSemester, secondSemester, nextYearFirstSemester, nextYearSecondSemester,
            presentFirstSemester, futureFirstSemester;
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

            executionYear = ExecutionYear.findCurrent(null);
            nextExecutionYear = executionYear.getNext().getExecutionYear();
            executionYear.setState(PeriodState.CURRENT);
            nextExecutionYear.setState(PeriodState.OPEN);

            firstSemester = executionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            secondSemester = executionYear.getChildInterval(2, AcademicPeriod.SEMESTER);
            firstSemester.setState(PeriodState.CURRENT);
            secondSemester.setState(PeriodState.OPEN);

            nextYearFirstSemester = nextExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            nextYearSecondSemester = nextExecutionYear.getChildInterval(2, AcademicPeriod.SEMESTER);
            nextYearFirstSemester.setState(PeriodState.OPEN);
            nextYearSecondSemester.setState(PeriodState.OPEN);

            presentYear = ExecutionYear.readExecutionYearByName(PRESENT_ACADEMIC_YEAR_NAME);
            futureYear = ExecutionYear.readExecutionYearByName(FUTURE_ACADEMIC_YEAR_NAME);
            presentYear.setState(PeriodState.OPEN);
            futureYear.setState(PeriodState.OPEN);

            presentFirstSemester = presentYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            futureFirstSemester = futureYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            presentFirstSemester.setState(PeriodState.OPEN);
            futureFirstSemester.setState(PeriodState.OPEN);

            return null;
        });
    }

    @Test
    public void testRegistration_activeState_shouldReturnStateFromRegistration() {
        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        RegistrationState activeState = registration.getActiveState();

        assertNotNull("Active state should not be null", activeState);
        assertEquals("Active state should be the first state created", activeState, firstState);
        assertTrue("Active state should belong to this registration",
                registration.getRegistrationStatesSet().contains(activeState));
    }

    @Test
    public void testRegistration_activeState_sameExecutionInterval_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        assertTrue(executionYear.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), firstSemester);

        assertEquals(firstState.getExecutionYear(), lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For the same current execution interval with two states, the active state should be the last one",
                lastState,
                activeState
        );
    }

    @Test
    public void testRegistration_activeState_sameYearDifferentExecutionIntervals_shouldReturnLastCreatedState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        assertTrue(executionYear.isCurrent());
        assertTrue(firstSemester.isCurrent());
        assertFalse(secondSemester.isCurrent());

        final RegistrationState semester1State =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        final RegistrationState semester2State = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), secondSemester);

        assertEquals(semester1State.getExecutionYear(), semester2State.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals(
                "For the same year with two states in different execution intervals, the active state should be the last state created",
                semester2State,
                activeState
        );
    }

    @Test
    public void testRegistration_activeState_differentExecutionYear_shouldReturnFirstState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        presentYear.setState(PeriodState.CURRENT);
        assertTrue(presentYear.isCurrent());
        assertFalse(futureYear.isCurrent());

        presentFirstSemester.setState(PeriodState.CURRENT);
        assertTrue(presentFirstSemester.isCurrent());
        assertFalse(futureFirstSemester.isCurrent());

        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), presentFirstSemester);

        final RegistrationState lastState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().plusDays(1),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), futureFirstSemester);

        assertEquals(presentYear, firstState.getExecutionYear());
        assertEquals(futureYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different execution years, the active state should be the state from the current execution year",
                firstState,
                activeState
        );
    }

    @Test
    public void testRegistration_activeState_differentExecutionYear_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        futureYear.setState(PeriodState.CURRENT);
        assertFalse(presentYear.isCurrent());
        assertTrue(futureYear.isCurrent());

        futureFirstSemester.setState(PeriodState.CURRENT);
        assertFalse(presentFirstSemester.isCurrent());
        assertTrue(futureFirstSemester.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), presentFirstSemester);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), futureFirstSemester);

        assertEquals(presentYear, firstState.getExecutionYear());
        assertEquals(futureYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different execution years, the active state should be the state from the current execution year",
                lastState, activeState);
    }

    @Test
    public void testRegistration_activeState_pastExecutionYears_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        assertTrue(executionYear.isCurrent());
        assertFalse(nextExecutionYear.isCurrent());

        firstSemester.setState(PeriodState.CURRENT);
        assertTrue(firstSemester.isCurrent());
        assertFalse(nextYearFirstSemester.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusMonths(6),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        final RegistrationState lastState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusMonths(5),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(),
                        nextYearFirstSemester);

        assertEquals(executionYear, firstState.getExecutionYear());
        assertEquals(nextExecutionYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different past execution years, the active state should be the last state created",
                lastState, activeState);
    }

}

