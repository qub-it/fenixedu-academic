package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

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

    private static final String INGRESSION_CODE = "I";
    private static final String PROTOCOL_CODE = "P";

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

    static void initStudentAndRegistration() {
        initConfigEntities();

        final UserProfile userProfile =
                new UserProfile("Student", "A", "Student A", "student.a@fenixedu.com", Locale.getDefault());
        new User(STUDENT_A_USERNAME, userProfile);
        final Person person = new Person(userProfile);
        student = new Student(person);

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        ExecutionsAndSchedulesTest.initExecutions();

        final Degree degree = Degree.find(DEGREE_A_CODE);
        final DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().iterator().next();

        registration = Registration.create(student, degreeCurricularPlan, ExecutionYear.findCurrent(degree.getCalendar()),
                RegistrationProtocol.findByCode(PROTOCOL_CODE),
                IngressionType.findIngressionTypeByCode(INGRESSION_CODE).orElseThrow());
    }

    private static void initConfigEntities() {
        RegistrationProtocol.create(PROTOCOL_CODE, new LocalizedString.Builder().with(Locale.getDefault(), "Protocol").build());

        IngressionType.createIngressionType(INGRESSION_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Ingression").build());

        RegistrationStateType.create(RegistrationStateType.REGISTERED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Registered").build(), true, null);
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
    public void testRegistration_studentCurricularPlan() {
        assertEquals(registration.getStudentCurricularPlansSet().size(), 1);
        assertNotNull(registration.getLastStudentCurricularPlan());
    }
}
