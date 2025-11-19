package org.fenixedu.academic.domain.student;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationTest {

    private static Registration registration;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initStudentAndRegistration();

            registration = Student.readStudentByNumber(1)
                    .getRegistrationStream()
                    .findAny()
                    .orElseThrow();

            return null;
        });
    }

    @Test
    public void getActiveState_shouldReturnAStateFromTheRegistration() {
        RegistrationState activeState = registration.getActiveState();

        assertNotNull("Active state should not be null", activeState);
        assertTrue("Active state should belong to this registration",
                registration.getRegistrationStatesSet().contains(activeState));
    }

    @Test
    public void getState_sameExecutionIntervalWithTwoStates_shouldReturnLastState() {
        new HashSet<>(registration.getRegistrationStatesSet())
                .forEach(RegistrationState::delete);

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final ExecutionInterval semester1 = currentYear.getChildInterval(1, AcademicPeriod.SEMESTER);

        DateTime october2023 = new DateTime(2023, 10, 31, 0, 0);
        DateTime november2023 = new DateTime(2023, 11, 1, 0, 0);

        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, october2023,
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), semester1);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, november2023,
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), semester1);

        assertEquals(firstState.getExecutionYear(), lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals(
                "For the same execution interval with two states, getActiveState should return the last one",
                lastState,
                activeState
        );
    }

    @Test
    public void getActiveState_sameYearWithTwoStates_shouldReturnCurrentState() {
        new HashSet<>(registration.getRegistrationStatesSet())
                .forEach(RegistrationState::delete);

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final ExecutionInterval semester1 = currentYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester2 = currentYear.getChildInterval(2, AcademicPeriod.SEMESTER);

        DateTime october2023 = new DateTime(2023, 10, 31, 0, 0);
        DateTime march2024 = new DateTime(2024, 3, 5, 0, 0);

        final RegistrationState currentState = RegistrationState.createRegistrationState(registration, null, october2023,
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), semester1);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, march2024,
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), semester2);

        assertEquals(currentState.getExecutionYear(), lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals(
                "For the same year with two states, getActiveState should return the current state, not the last one",
                currentState,
                activeState
        );
    }

    @Test
    public void getState_differentExecutionYear() {
        new HashSet<>(registration.getRegistrationStatesSet())
                .forEach(RegistrationState::delete);

        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final ExecutionYear futureYear  = currentYear.getNextExecutionYear();

        final ExecutionInterval sem1_2020_2021 = currentYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval sem1_2021_2022 = futureYear.getChildInterval(1, AcademicPeriod.SEMESTER);

        DateTime now = DateTime.now();
        DateTime stateInNewestYearDate = now.minusMonths(2);
        DateTime stateInOldYearDate    = now.minusMonths(1);

        final RegistrationState stateInNewestYear = RegistrationState.createRegistrationState(
                registration, null, stateInNewestYearDate,
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(),
                sem1_2021_2022
        );

        final RegistrationState stateInOldYear = RegistrationState.createRegistrationState(
                registration, null, stateInOldYearDate,
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(),
                sem1_2020_2021
        );

        assertEquals(2, registration.getRegistrationStatesSet().size());
        assertEquals(currentYear, stateInOldYear.getExecutionYear());
        assertEquals(futureYear, stateInNewestYear.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals(
                "For different execution years, getActiveState should return the state from the most recent execution year",
                stateInNewestYear,
                activeState
        );
    }

    @Test
    public void getActiveState_currentYearAndFutureYear_shouldReturnCurrentYearState() {
        new HashSet<>(registration.getRegistrationStatesSet())
                .forEach(RegistrationState::delete);

        assertEquals(0, registration.getRegistrationStatesSet().size());

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final ExecutionYear futureYear  = currentYear.getNextExecutionYear();
        assertNotNull("There must be a next execution year in the test data", futureYear);

        final ExecutionInterval currentSemester1 = currentYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval futureSemester1  = futureYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        assertNotNull(currentSemester1);
        assertNotNull(futureSemester1);

        final DateTime simulatedNow = futureYear.getAcademicInterval().getStart().minusDays(1);
        DateTimeUtils.setCurrentMillisFixed(simulatedNow.getMillis());
        try {
            DateTime currentStateDate = simulatedNow.minusDays(10);
            DateTime futureStateDate = simulatedNow.plusDays(10);

            final RegistrationState currentYearState = RegistrationState.createRegistrationState(
                    registration, null, currentStateDate,
                    RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(),
                    currentSemester1
            );

            final RegistrationState futureYearState = RegistrationState.createRegistrationState(
                    registration, null, futureStateDate,
                    RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(),
                    futureSemester1
            );

            assertEquals(2, registration.getRegistrationStatesSet().size());

            final RegistrationState activeState = registration.getActiveState();

            assertEquals(
                    "With one state in the current execution year and another in a future one " +
                            "whose academic interval has not started, getActiveState should return the current-year state",
                    currentYearState,
                    activeState
            );
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }
}

