package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.fenixedu.academic.domain.StudentTest.STUDENT_A_USERNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CurricularCourseTest {

    private static ExecutionYear executionYear, nextExecutionYear;
    private static ExecutionInterval executionInterval;
    private static CurricularPeriod firstSemester, secondSemester;

    private static DegreeCurricularPlan dcpV1;
    private static CompetenceCourse competenceCourseA, competenceCourseB;
    private static CurricularCourse curricularCourseA, curricularCourseB;
    private static Context curricularCourseAContext, curricularCourseBContext;

    private static StudentCurricularPlan scpV1;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initSetup();
            return null;
        });
    }

    private static void initSetup() {
        StudentTest.initRegistrationConfigEntities();
        DegreeCurricularPlanTest.initDegreeCurricularPlan();
        EvaluationSeasonTest.initEvaluationSeasons();

        Degree degree = Degree.find(DEGREE_A_CODE);
        dcpV1 = degree.getDegreeCurricularPlansSet().stream()
                .filter(p -> DCP_NAME_V1.equals(p.getName())).findAny().orElseThrow();

        executionYear = ExecutionYear.findCurrent(dcpV1.getDegree().getCalendar());
        executionInterval = executionYear.getFirstExecutionPeriod();

        nextExecutionYear = executionYear.getNext().getExecutionYear();

        degree.getDegreeCurricularPlansSet().forEach(dcp -> dcp.createExecutionDegree(executionYear));

        Student student = StudentTest.createStudent("Student A", STUDENT_A_USERNAME);
        Registration registration = StudentTest.createRegistration(student, dcpV1, executionYear);
        scpV1 = registration.getStudentCurricularPlansSet().iterator().next();

        competenceCourseA = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);
        curricularCourseA = competenceCourseA.getCurricularCourse(dcpV1);
        curricularCourseAContext = curricularCourseA.getParentContextsSet().stream()
                .filter(ctx -> ctx.isValid(executionInterval)).findAny().orElseThrow();

        competenceCourseB = CompetenceCourse.find(CompetenceCourseTest.COURSE_B_CODE);

        CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcpV1.getDegreeStructure());
        firstSemester = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);
        secondSemester = new CurricularPeriod(AcademicPeriod.SEMESTER, 2, yearPeriod);

        curricularCourseB = new CurricularCourse();
        curricularCourseB.setCompetenceCourse(competenceCourseB);
        curricularCourseBContext = createContext(dcpV1, curricularCourseB, firstSemester, executionInterval);
    }

    @After
    public void cleanup() {
        scpV1.getEnrolmentsSet().forEach(e -> e.getEvaluationsSet().forEach(ee -> e.delete()));

        dcpV1.getRoot().getChildContextsSet().stream().filter(ctx -> ctx != curricularCourseAContext && ctx != curricularCourseBContext)
                .forEach(Context::delete);

        dcpV1.getCurricularCoursesSet().stream().filter(cc -> cc != curricularCourseA && cc != curricularCourseB)
                .forEach(CurricularCourse::delete);

        curricularCourseA.getAssociatedExecutionCoursesSet().clear();
        curricularCourseB.getAssociatedExecutionCoursesSet().clear();
    }

    @Test
    public void testCurricularCourse_create() {
        CurricularCourse curricularCourse = new CurricularCourse();

        assertNotNull(curricularCourse);
        assertEquals(0, curricularCourse.getBaseWeight(), 0); // setWeight(0) by default
    }

    @Test
    public void testCurricularCourse_createWithParams() {
        CurricularCourse curricularCourse = new CurricularCourse(7.5, competenceCourseA, dcpV1.getRoot(), firstSemester,
                executionInterval, null);

        assertNotNull(curricularCourse);
        assertEquals(7.5, curricularCourse.getBaseWeight(), 0);
        assertEquals(curricularCourse.getCompetenceCourse(), competenceCourseA);
        assertFalse(curricularCourse.getParentContextsSet().isEmpty());
        assertTrue(dcpV1.getCurricularCoursesSet().contains(curricularCourse));
    }

    @Test
    public void testCurricularCourse_setWeight() {
        CurricularCourse curricularCourse = new CurricularCourse();
        curricularCourse.setWeight(new BigDecimal("6"));

        assertEquals(6, curricularCourse.getBaseWeight(), 0);

        curricularCourse.setWeight(null);

        assertNull(curricularCourse.getBaseWeight());
    }

    @Test
    public void testCurricularCourse_delete() {
        CurricularCourse curricularCourse = new CurricularCourse(5.0, competenceCourseA, dcpV1.getRoot(), firstSemester,
                executionInterval, null);

        curricularCourse.delete();

        assertFalse(dcpV1.getCurricularCoursesSet().contains(curricularCourse));
        assertNull(curricularCourse.getCompetenceCourse());
        assertNull(curricularCourse.getDegreeCurricularPlan());
    }

    @Test
    public void testCurricularCourse_comparatorByDegreeAndName() {
        // New DegreeCurricularPlan with different DegreeType
        DegreeType masterDegreeType = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).orElseThrow();
        Degree masterDegree =
                DegreeTest.createDegree(masterDegreeType, DegreeTest.MASTER_DEGREE_TYPE_CODE, "Master", executionYear);
        DegreeCurricularPlan masterDcp = new DegreeCurricularPlan(masterDegree, "Master_DCP", AcademicPeriod.TWO_YEAR);
        masterDcp.createExecutionDegree(executionYear);

        CurricularCourse masterCourse = new CurricularCourse();
        masterCourse.setCompetenceCourse(CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE));
        createContext(masterDcp, masterCourse, firstSemester, executionInterval);

        // New DegreeCurricularPlan with same DegreeType and different name
        DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        Degree newDegree = DegreeTest.createDegree(degreeType, "NEW_DEGREE_CODE", "New Degree", executionYear);
        DegreeCurricularPlan newDcp = new DegreeCurricularPlan(newDegree, "New_DCP", AcademicPeriod.THREE_YEAR);
        newDcp.createExecutionDegree(executionYear);

        CurricularCourse newCourse = new CurricularCourse();
        newCourse.setCompetenceCourse(CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE));
        createContext(newDcp, newCourse, firstSemester, executionInterval);

        List<CurricularCourse> courses = Arrays.asList(masterCourse, newCourse, curricularCourseA, curricularCourseB);
        courses.sort(CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME);

        assertEquals(curricularCourseA, courses.get(0));
        assertEquals(curricularCourseB, courses.get(1)); // Same Degree and different CurricularCourse name (proceeds to .thenComparing(CurricularCourse.COMPARATOR_BY_NAME))
        assertEquals(newCourse, courses.get(2)); // Different Degree with same DegreeType
        assertEquals(masterCourse, courses.get(3)); // Different Degree with different DegreeType

        assertEquals(0, CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME
                .compare(curricularCourseA, curricularCourseA));
        assertTrue(CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME
                .compare(curricularCourseA, curricularCourseB) < 0);
        assertTrue(CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME
                .compare(curricularCourseA, newCourse) < 0);
        assertTrue(CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME
                .compare(masterCourse, newCourse) > 0);
    }

    @Test
    public void testCurricularCourse_getExecutionCoursesByExecutionYear() {
        assertTrue(curricularCourseA.getExecutionCoursesByExecutionYear(executionYear).isEmpty());

        ExecutionCourse executionCourse = createExecutionCourse(curricularCourseA, executionInterval);
        curricularCourseA.addAssociatedExecutionCourses(executionCourse);

        assertEquals(1, curricularCourseA.getExecutionCoursesByExecutionYear(executionYear).size());
        assertTrue(curricularCourseA.getExecutionCoursesByExecutionYear(executionYear).contains(executionCourse));
        assertTrue(curricularCourseA.getExecutionCoursesByExecutionYear(nextExecutionYear).isEmpty());

        ExecutionCourse newExecutionCourse = createExecutionCourse(curricularCourseA, nextExecutionYear.getFirstExecutionPeriod());
        curricularCourseA.addAssociatedExecutionCourses(newExecutionCourse);

        assertEquals(1, curricularCourseA.getExecutionCoursesByExecutionYear(executionYear).size());
        assertEquals(1, curricularCourseA.getExecutionCoursesByExecutionYear(nextExecutionYear).size());
        assertTrue(curricularCourseA.getExecutionCoursesByExecutionYear(executionYear).contains(executionCourse));
        assertTrue(curricularCourseA.getExecutionCoursesByExecutionYear(nextExecutionYear).contains(newExecutionCourse));
    }

    @Test
    public void testCurricularCourse_getEnrolmentsByExecutionPeriod() {
        Enrolment enrolment = createEnrolmentInCourse(curricularCourseA, executionInterval);

        assertEquals(1, curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval).size());
        assertTrue(curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval).contains(enrolment));
        assertTrue(curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval.getNext()).isEmpty());

        createContext(dcpV1, curricularCourseA, secondSemester, executionInterval.getNext());
        Enrolment newEnrolment = createEnrolmentInCourse(curricularCourseA, executionInterval.getNext());

        assertEquals(1, curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval).size());
        assertEquals(1, curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval.getNext()).size());
        assertTrue(curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval).contains(enrolment));
        assertTrue(curricularCourseA.getEnrolmentsByExecutionPeriod(executionInterval.getNext()).contains(newEnrolment));
    }

    // Helpers

    private static Enrolment createEnrolmentInCourse(CurricularCourse curricularCourse, ExecutionInterval interval) {
        return new Enrolment(scpV1, scpV1.getRoot(), curricularCourse, interval, EnrollmentCondition.FINAL, STUDENT_A_USERNAME);
    }

    private static ExecutionCourse createExecutionCourse(CurricularCourse curricularCourse, ExecutionInterval executionInterval) {
        return new ExecutionCourse(curricularCourse.getName(), curricularCourse.getCode(), executionInterval);
    }

    private static Context createContext(DegreeCurricularPlan degreeCurricularPlan, CurricularCourse curricularCourse, CurricularPeriod period, ExecutionInterval begin) {
        return new Context(degreeCurricularPlan.getRoot(), curricularCourse, period, begin, null);
    }
}
