package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.UUID;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionCourseTest {

    private static Registration registration;
    private static Student student;
    private static ExecutionCourse executionCourse;

    private static Registration nonAttendingRegistration;
    private static Student studentWithoutRegistrations;
    private static Registration regA;
    private static Registration regB;
    private ExecutionCourse emptyExecutionCourse;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();

            student = Student.readStudentByNumber(1);
            registration = student.getRegistrationStream().findAny().orElseThrow();
            executionCourse = registration.getAssociatedAttendsSet().iterator().next().getExecutionCourse();

            nonAttendingRegistration = createNewRegistration("Different", "different." + UUID.randomUUID());
            studentWithoutRegistrations = StudentTest.createStudent("Other", "other." + UUID.randomUUID());

            regA = createNewRegistration("RegA", "reg.a." + UUID.randomUUID());
            regB = createNewRegistration("RegB", "reg.b." + UUID.randomUUID());

            return null;
        });
    }

    @Before
    public void setUp() {
        emptyExecutionCourse = createEmptyExecutionCourse();
    }

    private static ExecutionCourse createEmptyExecutionCourse() {
        final String uuid = UUID.randomUUID().toString();
        final ExecutionInterval interval = ExecutionInterval.findFirstCurrentChild(null);
        return new ExecutionCourse(uuid, uuid, interval);
    }

    private static Registration createNewRegistration(final String name, final String username) {
        final Student s = StudentTest.createStudent(name, username);
        final Degree degree = Degree.find(DEGREE_A_CODE);
        assertNotNull(degree);
        final DegreeCurricularPlan dcp = degree.getDegreeCurricularPlansSet().stream()
                .filter(p -> DCP_NAME_V1.equals(p.getName())).findAny().orElseThrow();
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        return StudentTest.createRegistration(s, dcp, executionYear);
    }

    @Test
    public void getAttendsByStudent_withRegistration_matchFound() {
        final Attends result = executionCourse.getAttendsByStudent(registration);
        assertNotNull(result);
        assertSame(registration, result.getRegistration());
    }

    @Test
    public void getAttendsByStudent_withRegistration_filtersAmongMultipleAttends() {
        final Attends a = new Attends(regA, emptyExecutionCourse);
        new Attends(regB, emptyExecutionCourse);
        assertSame(a, emptyExecutionCourse.getAttendsByStudent(regA));
    }

    @Test
    public void getAttendsByStudent_withStudent_matchFound() {
        final Attends result = executionCourse.getAttendsByStudent(student);
        assertNotNull(result);
        assertTrue(result.isFor(student));
    }

    @Test
    public void getAttendsByStudent_withNull() {
        assertNull(executionCourse.getAttendsByStudent((Registration) null));
        assertNull(executionCourse.getAttendsByStudent((Student) null));
    }

    @Test
    public void getAttendsByStudent_overloadCoherence() {
        final Attends attends = new Attends(regA, emptyExecutionCourse);
        final Student student = regA.getStudent();
        assertSame(attends, emptyExecutionCourse.getAttendsByStudent(regA));
        assertSame(attends, emptyExecutionCourse.getAttendsByStudent(student));
    }

    @Test
    public void getAttendsByStudent_withStudentWithoutRegistrations_returnsNull() {
        new Attends(regA, emptyExecutionCourse);
        assertNull(emptyExecutionCourse.getAttendsByStudent(studentWithoutRegistrations));
    }

    @Test
    public void delete_emptyExecutionCourse() {
        final ExecutionCourse ec = createEmptyExecutionCourse();
        ec.delete();
        assertNull(ec.getExecutionPeriod());
    }

    public void delete_blockedWhenHasAttends() {
        new Attends(regA, emptyExecutionCourse);
        assertThrows(DomainException.class, () -> emptyExecutionCourse.delete());
    }

    @Test
    public void delete_blockedWhenHasShifts() {
        final ExecutionCourse ec = createEmptyExecutionCourse();
        new Shift(ec, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        assertThrows(DomainException.class, () -> ec.delete());
    }

    @Test
    public void delete_removesLessonPlannings() {
        final ExecutionCourse ec = createEmptyExecutionCourse();
        final LessonPlanning planning =
                new LessonPlanning(new LocalizedString(Locale.ENGLISH, "title"), new LocalizedString(Locale.ENGLISH, "planning"),
                        CourseLoadType.of(CourseLoadType.THEORETICAL), ec);
        ec.delete();
        assertNull(planning.getExecutionCourse());
    }

    @Test
    public void delete_removesExecutionCourseLogs() {
        final ExecutionCourse ec = createEmptyExecutionCourse();
        final ExecutionCourseLog log = new ContentManagementLog(ec, "test log");
        ec.delete();
        assertNull(log.getExecutionCourse());
    }
}
