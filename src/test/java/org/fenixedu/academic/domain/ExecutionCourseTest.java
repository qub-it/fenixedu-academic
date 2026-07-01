package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
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

    private static SchoolClass createSchoolClassFor(final ExecutionCourse ec, final DegreeCurricularPlan dcp, final String name) {
        final ExecutionInterval interval = ec.getExecutionInterval();
        final ExecutionDegree executionDegree = dcp.findExecutionDegree(interval).orElseThrow();
        final SchoolClass schoolClass = new SchoolClass(executionDegree, interval, name, 1);
        final Shift shift = new Shift(ec, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        shift.addAssociatedClasses(schoolClass);
        return schoolClass;
    }

    @Test
    public void testSetSigla_sameValue() {
        final String s = "SAME_" + UUID.randomUUID();
        emptyExecutionCourse.setSigla(s);
        emptyExecutionCourse.setSigla(s);
        assertEquals(s, emptyExecutionCourse.getSigla());
    }

    @Test
    public void testSetSigla_conflicts() {
        final ExecutionInterval interval = emptyExecutionCourse.getExecutionInterval();
        final String target = "CONFLICT_" + UUID.randomUUID();

        // existing course holds the sigla in uppercase
        final ExecutionCourse other = new ExecutionCourse("Other", UUID.randomUUID().toString(), interval);
        other.setSigla(target.toUpperCase());

        // first conflict is detected case-insensitively -> target-0
        emptyExecutionCourse.setSigla(target.toLowerCase());
        assertEquals(target.toLowerCase() + "-0", emptyExecutionCourse.getSigla());

        // second conflict -> target-1
        final ExecutionCourse anotherCourse = new ExecutionCourse("Another", UUID.randomUUID().toString(), interval);
        anotherCourse.setSigla(target.toLowerCase());
        assertEquals(target.toLowerCase() + "-1", anotherCourse.getSigla());
    }

    @Test
    public void testSetSigla_normalizesSpecialCharacters() {
        emptyExecutionCourse.setSigla("B C");
        assertEquals("B_C", emptyExecutionCourse.getSigla());

        emptyExecutionCourse.setSigla("A/B");
        assertEquals("A-B", emptyExecutionCourse.getSigla());

        emptyExecutionCourse.setSigla("A/B C");
        assertEquals("A-B_C", emptyExecutionCourse.getSigla());
    }

    @Test
    public void testGetSchoolClasses() {
        final DegreeCurricularPlan dcp = registration.getLastDegreeCurricularPlan();
        final SchoolClass schoolClass = createSchoolClassFor(emptyExecutionCourse, dcp, "TestClass");
        assertEquals(Set.of(schoolClass), emptyExecutionCourse.getSchoolClasses());
    }

    @Test
    public void testGetSchoolClasses_multiple() {
        final DegreeCurricularPlan dcp = registration.getLastDegreeCurricularPlan();
        final SchoolClass a = createSchoolClassFor(emptyExecutionCourse, dcp, "Multi_A");
        final SchoolClass b = createSchoolClassFor(emptyExecutionCourse, dcp, "Multi_B");
        assertEquals(Set.of(a, b), emptyExecutionCourse.getSchoolClasses());
    }

    @Test
    public void testGetSchoolClassesBy() {
        final DegreeCurricularPlan dcp = registration.getLastDegreeCurricularPlan();
        final SchoolClass schoolClass = createSchoolClassFor(emptyExecutionCourse, dcp, "TestClass_By");

        // matches the DCP the school class belongs to
        assertEquals(Set.of(schoolClass), emptyExecutionCourse.getSchoolClassesBy(dcp));

        // does not match a different DCP
        final DegreeCurricularPlan otherDcp =
                dcp.getDegree().getDegreeCurricularPlansSet().stream().filter(d -> !d.equals(dcp)).findAny().orElseThrow();
        assertTrue(emptyExecutionCourse.getSchoolClassesBy(otherDcp).isEmpty());
    }
}
