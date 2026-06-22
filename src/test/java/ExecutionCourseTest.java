import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
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
    private static ExecutionDegree executionDegree;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();

            registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
            student = registration.getStudent();
            executionCourse = registration.getAssociatedAttendsSet().iterator().next().getExecutionCourse();

            final CurricularCourse cc = executionCourse.getAssociatedCurricularCoursesSet().iterator().next();
            final DegreeCurricularPlan dcp = cc.getDegreeCurricularPlan();
            executionDegree = dcp.findExecutionDegree(executionCourse.getExecutionInterval()).orElse(null);

            return null;
        });
    }

    @Test
    public void testGetCompetenceCourses() {
        final Set<CompetenceCourse> competenceCourses = executionCourse.getCompetenceCourses();
        assertFalse(competenceCourses.isEmpty());
        final CompetenceCourse competenceCourse = competenceCourses.iterator().next();
        assertEquals(CompetenceCourseTest.COURSE_A_CODE, competenceCourse.getCode());
    }

    @Test
    public void testGetCompetenceCoursesInformation() {
        final Set<CompetenceCourseInformation> informations = executionCourse.getCompetenceCoursesInformations();
        assertFalse(informations.isEmpty());
    }

    @Test
    public void testGetExecutionDegrees() {
        final Set<ExecutionDegree> executionDegrees = executionCourse.getExecutionDegrees();
        assertFalse(executionDegrees.isEmpty());
        assertTrue(executionDegrees.contains(executionDegree));
    }

    @Test
    public void testGetAssociatedDegreeCurricularPlans() {
        final Collection<DegreeCurricularPlan> plans = executionCourse.getAssociatedDegreeCurricularPlans();
        assertFalse(plans.isEmpty());
    }

    @Test
    public void testAllMethods_emptyWhenNoAssociations() {
        final ExecutionInterval interval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
        final ExecutionCourse emptyCourse = new ExecutionCourse("TestCourse", "TC", interval);

        assertTrue(emptyCourse.getCompetenceCourses().isEmpty());
        assertTrue(emptyCourse.getCompetenceCoursesInformations().isEmpty());
        assertTrue(emptyCourse.getExecutionDegrees().isEmpty());
        assertTrue(emptyCourse.getAssociatedDegreeCurricularPlans().isEmpty());
    }
}