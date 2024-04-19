package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V2;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V3;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StudentTest {

    public static final String INGRESSION_CODE = "I";
    public static final String PROTOCOL_CODE = "P";
    public static final String REGISTRATION_STATE_INTERRUPTED = "INTERRUPTED";

    public static final String STUDENT_A_USERNAME = "student.a";

    private static Student student;
    private static Registration registration;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initStudentAndRegistration();
            return null;
        });
    }

    public static void initStudentAndRegistration() {
        initRegistrationConfigEntities();

        student = createStudent("Student A", STUDENT_A_USERNAME);

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        ExecutionsAndSchedulesTest.initExecutions();

        final Degree degree = Degree.find(DEGREE_A_CODE);
        final DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().stream()
                .filter(dcp -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(dcp.getName())).findAny().orElseThrow();

        registration = createRegistration(student, degreeCurricularPlan,
                ExecutionYear.findCurrent(degreeCurricularPlan.getDegree().getCalendar()));
    }

    public static Student createStudent(String name, String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        final Person person = new Person(userProfile);

        return new Student(person);
    }

    public static Registration createRegistration(final Student student, final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return Registration.create(student, degreeCurricularPlan, executionYear, RegistrationProtocol.findByCode(PROTOCOL_CODE),
                IngressionType.findIngressionTypeByCode(INGRESSION_CODE).orElseThrow());
    }

    public static void initRegistrationConfigEntities() {
        RegistrationProtocol.create(PROTOCOL_CODE, new LocalizedString.Builder().with(Locale.getDefault(), "Protocol").build());

        IngressionType.createIngressionType(INGRESSION_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Ingression").build());

        RegistrationStateType.create(RegistrationStateType.REGISTERED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Registered").build(), true, null);

        RegistrationStateType.create(REGISTRATION_STATE_INTERRUPTED,
                new LocalizedString.Builder().with(Locale.getDefault(), "Interrupted").build(), false, null);

        RegistrationStateType.create(RegistrationStateType.CONCLUDED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Concluded").build(), false, null);
    }

    @Test
    public void testStudent_find() {
        assertEquals(Student.readStudentByNumber(1), student);
    }

    @Test
    public void testRegistration_find() {
        assertEquals(student.getRegistrationsSet().size(), 1);
        assertTrue(student.getRegistrationsSet().contains(registration));
    }

    @Test
    public void testRegistration_studentCurricularPlans() {
        Map<String, DegreeCurricularPlan> dcpsByName = registration.getDegree().getDegreeCurricularPlansSet().stream()
                .collect(Collectors.toMap(dcp -> dcp.getName(), dcp -> dcp));

        DegreeCurricularPlan dcpV1 = dcpsByName.get(DCP_NAME_V1);
        DegreeCurricularPlan dcpV2 = dcpsByName.get(DCP_NAME_V2);
        DegreeCurricularPlan dcpV3 = dcpsByName.get(DCP_NAME_V3);

        final ExecutionYear currentYear = ExecutionYear.findCurrent(registration.getDegree().getCalendar());
        final ExecutionYear nextYear = (ExecutionYear) currentYear.getNext();
        final ExecutionYear nextNextYear = (ExecutionYear) nextYear.getNext();

        assertEquals(registration.getStudentCurricularPlansSet().size(), 1);

        StudentCurricularPlan firstSCP = registration.getStudentCurricularPlansSet().iterator().next();
        StudentCurricularPlan secondSCP = registration.createStudentCurricularPlan(dcpV2, nextYear);
        StudentCurricularPlan thirdSCP = StudentCurricularPlan.createBolonhaStudentCurricularPlan(registration, dcpV3,
                nextYear.getBeginDateYearMonthDay().plusDays(1), nextYear.getFirstExecutionPeriod(), null);

        assertEquals(firstSCP.getDegreeCurricularPlan(), dcpV1);
        assertEquals(secondSCP.getDegreeCurricularPlan(), dcpV2);
        assertEquals(thirdSCP.getDegreeCurricularPlan(), dcpV3);
        assertEquals(firstSCP.getStartExecutionInterval(), currentYear.getFirstExecutionPeriod());
        assertEquals(secondSCP.getStartExecutionInterval(), thirdSCP.getStartExecutionInterval());

        assertEquals(registration.getFirstStudentCurricularPlan(), firstSCP);
        assertEquals(registration.getLastStudentCurricularPlan(), thirdSCP);

        assertTrue(registration.findStudentCurricularPlan(currentYear.getPrevious()).isEmpty());
        assertEquals(registration.findStudentCurricularPlan(currentYear).get(), firstSCP);
        assertEquals(registration.findStudentCurricularPlan(currentYear.getLastExecutionPeriod()).get(), firstSCP);
        assertEquals(registration.findStudentCurricularPlan(nextYear).get(), thirdSCP);
        assertEquals(registration.findStudentCurricularPlan(nextNextYear).get(), thirdSCP);
    }
}
