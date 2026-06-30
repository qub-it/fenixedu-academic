package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
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
    private static ExecutionDegree executionDegree;

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

            final CurricularCourse cc = executionCourse.getAssociatedCurricularCoursesSet().iterator().next();
            final DegreeCurricularPlan dcp = cc.getDegreeCurricularPlan();
            executionDegree = dcp.findExecutionDegree(executionCourse.getExecutionInterval()).orElse(null);

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
    public void testGetCompetenceCourses() {
        final Set<CompetenceCourse> competenceCourses = executionCourse.getCompetenceCourses();
        assertFalse(competenceCourses.isEmpty());
        assertEquals(1, competenceCourses.size());
        final CompetenceCourse competenceCourse = competenceCourses.iterator().next();
        assertEquals(CompetenceCourseTest.COURSE_A_CODE, competenceCourse.getCode());
    }

    @Test
    public void testGetCompetenceCoursesInformation() {
        final Set<CompetenceCourseInformation> informations = executionCourse.getCompetenceCoursesInformations();
        assertFalse(informations.isEmpty());
        assertEquals(1, informations.size());

        final CompetenceCourseInformation info = informations.iterator().next();
        assertEquals("Course A", info.getName());
        assertEquals(new BigDecimal("6.0"), info.getCredits());
        assertEquals(AcademicPeriod.SEMESTER, info.getAcademicPeriod());

        final CompetenceCourse cc = executionCourse.getCompetenceCourses().iterator().next();
        assertTrue(informations.contains(cc.findInformationMostRecentUntil(executionCourse.getExecutionInterval())));
    }

    @Test
    public void testGetExecutionDegrees() {
        final Set<ExecutionDegree> executionDegrees = executionCourse.getExecutionDegrees();
        assertFalse(executionDegrees.isEmpty());
        assertEquals(1, executionDegrees.size());
        assertEquals(Degree.find(DEGREE_A_CODE), executionDegrees.iterator().next().getDegree());
        assertTrue(executionDegrees.contains(executionDegree));
    }

    @Test
    public void testGetAssociatedDegreeCurricularPlans() {
        final Collection<DegreeCurricularPlan> plans = executionCourse.getAssociatedDegreeCurricularPlans();
        assertFalse(plans.isEmpty());
        assertEquals(1, plans.size());
        assertTrue(plans.contains(executionDegree.getDegreeCurricularPlan()));
    }

    @Test
    public void executionCourseWithoutAssociations_returnsEmptyCollections() {
        final ExecutionInterval interval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
        final ExecutionCourse emptyCourse = new ExecutionCourse("TestCourse", "TC", interval);

        assertTrue(emptyCourse.getCompetenceCourses().isEmpty());
        assertTrue(emptyCourse.getCompetenceCoursesInformations().isEmpty());
        assertTrue(emptyCourse.getExecutionDegrees().isEmpty());
        assertTrue(emptyCourse.getAssociatedDegreeCurricularPlans().isEmpty());
    }
}
