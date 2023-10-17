package org.fenixedu.academic.domain.student;

import static org.junit.Assert.assertTrue;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationStateTest {

    private static Registration registration;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initRegistrationStates();
            return null;
        });
    }

    private static void initRegistrationStates() {
        StudentTest.initStudentAndRegistration();
        registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
    }

    @Test
    public void testRegistration_states() {
        /*
         +----------+----------+----------+----------------+----------+
         |          |  state1  |          | state2, state3 |          |
         +----------+----------+----------+----------------+----------+
         | 19/20 S2 | 20/21 S1 | 20/21 S2 |    21/22 S1    | 21/22 S2 |
         +----------+----------+----------+----------------+----------+
        */

        final ExecutionYear executionYear1920 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear executionYear2021 = ExecutionYear.readExecutionYearByName("2020/2021");
        final ExecutionYear executionYear2122 = ExecutionYear.readExecutionYearByName("2021/2022");
        final ExecutionInterval semester2of1920 = executionYear1920.getChildInterval(2, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester1of2021 = executionYear2021.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester2of2021 = executionYear2021.getChildInterval(2, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester1of2122 = executionYear2122.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionInterval semester2of2122 = executionYear2122.getChildInterval(2, AcademicPeriod.SEMESTER);

        assertTrue(registration.getRegistrationStatesSet().size() == 1);
        final RegistrationState state1 = registration.getRegistrationStatesSet().iterator().next();
        assertTrue(state1.getExecutionInterval() == semester1of2021);

        assertTrue(registration.getRegistrationStates(semester2of1920).isEmpty());
        assertTrue(registration.getRegistrationStates(semester1of2021).size() == 1);
        assertTrue(registration.getRegistrationStates(semester1of2021).contains(state1));
        assertTrue(registration.getRegistrationStates(semester2of2021).size() == 1);
        assertTrue(registration.getRegistrationStates(semester2of2021).contains(state1));

        final RegistrationState state2 =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), semester1of2122);

        assertTrue(registration.getRegistrationStatesSet().size() == 2);
        assertTrue(registration.getRegistrationStates(semester2of1920).isEmpty());
        assertTrue(registration.getRegistrationStates(semester1of2021).size() == 1);
        assertTrue(registration.getRegistrationStates(semester1of2021).contains(state1));
        assertTrue(registration.getRegistrationStates(semester2of2021).size() == 1);
        assertTrue(registration.getRegistrationStates(semester2of2021).contains(state1));
        assertTrue(registration.getRegistrationStates(semester1of2122).size() == 1);
        assertTrue(registration.getRegistrationStates(semester1of2122).contains(state2));
        assertTrue(registration.getRegistrationStates(semester2of2122).size() == 1);
        assertTrue(registration.getRegistrationStates(semester2of2122).contains(state2));

        final RegistrationState state3 = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), semester1of2122);

        assertTrue(registration.getRegistrationStatesSet().size() == 3);
        assertTrue(registration.getRegistrationStates(semester1of2122).size() == 2);
        assertTrue(registration.getRegistrationStates(semester1of2122).contains(state2));
        assertTrue(registration.getRegistrationStates(semester1of2122).contains(state3));
        assertTrue(registration.getRegistrationStates(semester2of2122).size() == 1);
        assertTrue(registration.getRegistrationStates(semester2of2122).contains(state3));
    }
}
