package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StudentTest {

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
        final Person person =
                new Person(new UserProfile("Student", "A", "Student A", "student.a@fenixedu.com", Locale.getDefault()));
        student = new Student(person);

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
//        DegreeCurricularPlanTest.initDegreeCurricularPlan();
        ExecutionsAndSchedulesTest.initExecutions();

        final Degree degree = Degree.find("CS");
        final DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().iterator().next();

        final RegistrationProtocol protocol =
                RegistrationProtocol.create("P", new LocalizedString.Builder().with(Locale.getDefault(), "Protocol").build());

        final IngressionType ingression = IngressionType.createIngressionType("I",
                new LocalizedString.Builder().with(Locale.getDefault(), "Ingression").build());

        RegistrationStateType.create(RegistrationStateType.REGISTERED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Registered").build(), true, null);

        registration = Registration.create(student, degreeCurricularPlan, ExecutionYear.findCurrent(degree.getCalendar()),
                protocol, ingression);
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
