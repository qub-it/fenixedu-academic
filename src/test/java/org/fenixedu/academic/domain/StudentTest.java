package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V2;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V3;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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
        final Person person = createPerson(name, username);
        return new Student(person);
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
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
                new LocalizedString.Builder().with(Locale.getDefault(), "Registered").build(), true);

        RegistrationStateType.create(REGISTRATION_STATE_INTERRUPTED,
                new LocalizedString.Builder().with(Locale.getDefault(), "Interrupted").build(), false);

        RegistrationStateType.create(RegistrationStateType.CONCLUDED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Concluded").build(), false);
    }

    @Test
    public void testStudent_createAndFind() {
        final Student studentA = new Student(createPerson("test_createAndFind.A", "test_createAndFind.a"));
        final Student studentB = new Student(createPerson("test_createAndFind.B", "test_createAndFind.b"));
        final Student studentC = new Student(createPerson("test_createAndFind.C", "test_createAndFind.c"));

        assertEquals(Student.readStudentByNumber(studentA.getNumber()), studentA);
        assertEquals(Student.readStudentByNumber(studentB.getNumber()), studentB);
        assertEquals(Student.readStudentByNumber(studentC.getNumber()), studentC);
    }

    @Test
    public void testStudent_createWithExistingNumber() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Student.number.already.exists");

        final Student studentA = new Student(createPerson("test_withExistingNumber.A", "test_withExistingNumber.a"));
        new Student(createPerson("test_withExistingNumber.B", "test_withExistingNumber.b"), studentA.getNumber());
    }

    @Test
    public void testStudent_generateNumber() {
        final Student studentA = new Student(createPerson("test_generateNumber.A", "test_generateNumber.a"));
        assertEquals(Student.generateStudentNumber(), Integer.valueOf(studentA.getNumber() + 1));

        final Student studentB = new Student(createPerson("test_generateNumber.B", "test_generateNumber.b"));
        assertEquals(Student.generateStudentNumber(), Integer.valueOf(studentB.getNumber() + 1));

        final Student studentC =
                new Student(createPerson("test_generateNumber.C", "test_generateNumber.c"), studentB.getNumber() + 10);
        assertEquals(Student.generateStudentNumber(), Integer.valueOf(studentC.getNumber() + 1));
        assertEquals(studentC.getNumber(), Integer.valueOf(studentB.getNumber() + 10));
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
