package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V2;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StudentCurricularPlanTest {

    public static final String STUDENT_USERNAME = "student.a";
    public static final String CURRICULAR_COURSE_B_NAME = "Course B";
    public static final String NEW_CC_NAME = "NEW_CC";

    private static Registration registration;
    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;
    private static CurricularPeriod semesterPeriod;

    private static StudentCurricularPlan scpV1;
    private static DegreeCurricularPlan dcpV1;
    private static DegreeCurricularPlan dcpV2;
    private static CurricularCourse curricularCourseA;
    private static Context curricularCourseAContext;
    private static CurricularCourse curricularCourseB;
    private static Context curricularCourseBContext;

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
        initDissertationCompetenceCourseType();

        Student student = StudentTest.createStudent("Student A", STUDENT_USERNAME);

        Degree degree = Degree.find(DEGREE_A_CODE);
        dcpV1 = degree.getDegreeCurricularPlansSet().stream()
                .filter(p -> DCP_NAME_V1.equals(p.getName())).findAny().orElseThrow();
        dcpV2 = degree.getDegreeCurricularPlansSet().stream()
                .filter(p -> DCP_NAME_V2.equals(p.getName())).findAny().orElseThrow();

        executionYear = ExecutionYear.findCurrent(dcpV1.getDegree().getCalendar());
        executionInterval = executionYear.getFirstExecutionPeriod();

        degree.getDegreeCurricularPlansSet()
                .forEach(dcp -> dcp.createExecutionDegree(executionYear));

        // StudentCurricularPlan is created when we create the registration
        registration = StudentTest.createRegistration(student, dcpV1, executionYear);
        scpV1 = registration.getStudentCurricularPlansSet().iterator().next();

        // The Context for this CurricularCourse is created in DegreeCurricularPlanTest.initDegreeCurricularPlan()
        curricularCourseA = dcpV1.getCurricularCoursesSet().stream().findAny().orElseThrow();

        curricularCourseAContext = curricularCourseA.getParentContextsSet().stream()
                .filter(ctx -> ctx.isValid(executionInterval))
                .findAny().orElseThrow();

        // Create annual CurricularCourseB for Enrolment tests
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_B_CODE);

        curricularCourseB = new CurricularCourse();
        curricularCourseB.setCompetenceCourse(competenceCourse);
        curricularCourseB.setName(CURRICULAR_COURSE_B_NAME);

        CurricularPeriod yearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 1, dcpV1.getDegreeStructure());
        semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

        curricularCourseBContext = new Context(dcpV1.getRoot(), curricularCourseB, semesterPeriod, executionInterval, null);
    }

    private static void initDissertationCompetenceCourseType() {
        String code = CompetenceCourseType.DISSERTATION;
        LocalizedString name = new LocalizedString(Locale.getDefault(), code);

        CompetenceCourseType.findByCode(code).orElseGet(() -> CompetenceCourseType.create(code, name, true));
    }

    @After
    public void cleanup() {
        dcpV1.getStudentCurricularPlansSet().stream()
                .flatMap(scp -> scp.getEnrolmentsSet().stream())
                .forEach(e -> {
                    e.getEvaluationsSet().stream()
                            .filter(ee -> !ee.isTemporary())
                            .forEach(EnrolmentEvaluation::removeGrade);
                    e.delete();
                });

        dcpV2.getStudentCurricularPlansSet().forEach(StudentCurricularPlan::delete);

        dcpV1.getRoot().getChildContextsSet().stream()
                .filter(ctx -> ctx.getChildDegreeModule() != curricularCourseA && ctx.getChildDegreeModule() != curricularCourseB)
                .forEach(Context::delete);

        dcpV1.getCurricularCoursesSet().stream()
                .filter(cc -> cc != curricularCourseA && cc != curricularCourseB)
                .forEach(CurricularCourse::delete);
    }

    private StudentCurricularPlan create(Registration registration, DegreeCurricularPlan dcp, ExecutionInterval interval) {
        return StudentCurricularPlan.createBolonhaStudentCurricularPlan(registration, dcp,
                interval.getBeginDateYearMonthDay(), interval);
    }

    @Test
    public void testStudentCurricularPlan_create() {
        StudentCurricularPlan scp = create(registration, dcpV2, executionInterval);

        assertNotNull(scp);
        assertEquals(scp.getRegistration(), registration);
        assertEquals(scp.getDegreeCurricularPlan(), dcpV2);
        assertEquals(scp.getStartExecutionInterval(), executionInterval);
    }

    @Test
    public void testStudentCurricularPlan_createDuplicateThrows() {
        assertThrows(DomainException.class, () -> create(registration, dcpV1, executionInterval),
                "Student Curricular Plan already exists for this registration and degree");
    }

    @Test
    public void testStudentCurricularPlan_delete() {
        StudentCurricularPlan scp = create(registration, dcpV2, executionInterval);

        assertNotNull(scp);
        assertNotNull(scp.getRootDomainObject());
        assertEquals(registration, scp.getRegistration());
        assertEquals(dcpV2, scp.getDegreeCurricularPlan());
        assertEquals(executionInterval, scp.getStartExecutionInterval());

        scp.delete();

        assertNull(scp.getRootDomainObject());
        assertNull(scp.getRegistration());
        assertNull(scp.getDegreeCurricularPlan());
        assertNull(scp.getStartExecutionInterval());
    }

    @Test
    public void testStudentCurricularPlan_deleteRemovesEnrolments() {
        StudentCurricularPlan scp = create(registration, dcpV2, executionInterval);

        assertNotNull(scp);
        assertTrue(scp.getEnrolmentsSet().isEmpty());

        EnrolmentTest.createEnrolment(scp, executionInterval,
                new Context(dcpV2.getRoot(), curricularCourseA, semesterPeriod, executionInterval, null), STUDENT_USERNAME);
        Enrolment newEnrolment = scp.getEnrolments(curricularCourseA).stream().findAny().orElseThrow();

        assertFalse(scp.getEnrolmentsSet().isEmpty());
        assertTrue(scp.getEnrolmentsSet().contains(newEnrolment));
        assertEquals(scp, newEnrolment.getStudentCurricularPlan());

        scp.delete();

        assertNull(scp.getRootDomainObject());
        assertNull(newEnrolment.getStudentCurricularPlan());
        assertThrows(NullPointerException.class, scp::getEnrolmentsSet); // Because scp.getRoot() is null (scp.getEnrolmentsSet() == scp.getRoot().getEnrolmentsSet())
    }

    @Test
    public void testStudentCurricularPlan_getEnrolments() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.getEnrolments(curricularCourseA).size());
        assertTrue(scpV1.getEnrolments(curricularCourseA).contains(enrolmentInCourseA));
        assertTrue(scpV1.getEnrolments(curricularCourseB).isEmpty());

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(1, scpV1.getEnrolments(curricularCourseB).size());
        assertTrue(scpV1.getEnrolments(curricularCourseB).contains(enrolmentInCourseB));
    }

    @Test
    public void testStudentCurricularPlan_countEnrolmentsByCurricularCourse() {
        createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.countEnrolmentsByCurricularCourse(curricularCourseA));
        assertEquals(0, scpV1.countEnrolmentsByCurricularCourse(curricularCourseB));

        createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(1, scpV1.countEnrolmentsByCurricularCourse(curricularCourseB));
    }

    @Test
    public void testStudentCurricularPlan_countEnrolmentsByCurricularCourseWithInterval() {
        createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.countEnrolmentsByCurricularCourse(curricularCourseA, executionInterval));
        assertEquals(0, scpV1.countEnrolmentsByCurricularCourse(curricularCourseB, executionInterval));

        createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(1, scpV1.countEnrolmentsByCurricularCourse(curricularCourseB, executionInterval));
    }

    @Test
    public void testStudentCurricularPlan_getEnrolmentsByExecutionYear() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.getEnrolmentsByExecutionYear(executionYear).size());
        assertTrue(scpV1.getEnrolmentsByExecutionYear(executionYear).contains(enrolmentInCourseA));

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(2, scpV1.getEnrolmentsByExecutionYear(executionYear).size());
        assertEquals(0, scpV1.getEnrolmentsByExecutionYear((ExecutionYear) executionYear.getPrevious()).size());
        assertTrue(scpV1.getEnrolmentsByExecutionYear(executionYear).contains(enrolmentInCourseA));
        assertTrue(scpV1.getEnrolmentsByExecutionYear(executionYear).contains(enrolmentInCourseB));
    }

    @Test
    public void testStudentCurricularPlan_getEnrolmentsByExecutionPeriod() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.getEnrolmentsByExecutionPeriod(executionInterval).size());
        assertTrue(scpV1.getEnrolmentsByExecutionPeriod(executionInterval).contains(enrolmentInCourseA));

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(2, scpV1.getEnrolmentsByExecutionPeriod(executionInterval).size());
        assertTrue(scpV1.getEnrolmentsByExecutionPeriod(executionInterval).contains(enrolmentInCourseB));

        // Change enrolmentInCourseB's ExecutionPeriod
        ExecutionInterval newExecutionInterval = executionYear.getLastExecutionPeriod();
        enrolmentInCourseB.setExecutionPeriod(newExecutionInterval);

        assertEquals(1, scpV1.getEnrolmentsByExecutionPeriod(executionInterval).size());
        assertEquals(1, scpV1.getEnrolmentsByExecutionPeriod(newExecutionInterval).size());
        assertFalse(scpV1.getEnrolmentsByExecutionPeriod(executionInterval).contains(enrolmentInCourseB));
        assertTrue(scpV1.getEnrolmentsByExecutionPeriod(newExecutionInterval).contains(enrolmentInCourseB));
    }

    @Test
    public void testStudentCurricularPlan_getLatestCurricularCoursesEnrolments() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        Collection<Enrolment> latestEnrolments = scpV1.getLatestCurricularCoursesEnrolments(executionYear);
        assertEquals(1, latestEnrolments.size());
        assertTrue(latestEnrolments.contains(enrolmentInCourseA));

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        latestEnrolments = scpV1.getLatestCurricularCoursesEnrolments(executionYear);
        assertEquals(2, latestEnrolments.size());
        assertTrue(latestEnrolments.contains(enrolmentInCourseA));
        assertTrue(latestEnrolments.contains(enrolmentInCourseB));

        ExecutionInterval laterInterval = executionYear.getLastExecutionPeriod();
        CurricularPeriod secondSemester = new CurricularPeriod(AcademicPeriod.SEMESTER, 2, semesterPeriod.getParent());
        new Context(dcpV1.getRoot(), curricularCourseA, secondSemester, laterInterval, null);
        Enrolment newEnrolmentA = new Enrolment(scpV1, scpV1.getRoot(), curricularCourseA, laterInterval,
                EnrollmentCondition.FINAL, STUDENT_USERNAME);

        latestEnrolments = scpV1.getLatestCurricularCoursesEnrolments(executionYear);
        assertEquals(2, latestEnrolments.size());
        assertFalse(latestEnrolments.contains(enrolmentInCourseA));
        assertTrue(latestEnrolments.contains(newEnrolmentA));
        assertTrue(latestEnrolments.contains(enrolmentInCourseB));
    }

    @Test
    public void testStudentCurricularPlan_getDismissalApprovedEnrolments() {
        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertTrue(scpV1.getDismissalApprovedEnrolments().isEmpty());

        approveEnrolment(enrolmentInCourseB);

        assertEquals(1, scpV1.getDismissalApprovedEnrolments().size());
        assertTrue(scpV1.getDismissalApprovedEnrolments().contains(enrolmentInCourseB));

        enrolmentInCourseB.setEnrolmentCondition(EnrollmentCondition.INVISIBLE);
        assertFalse(scpV1.getDismissalApprovedEnrolments().contains(enrolmentInCourseB));
        assertTrue(scpV1.getDismissalApprovedEnrolments().isEmpty());
    }

    // Helpers

    private Enrolment createEnrolmentInCourse(CurricularCourse course, Context context, ExecutionInterval interval) {
        EnrolmentTest.createEnrolment(scpV1, interval, context, STUDENT_USERNAME);
        return scpV1.getEnrolmentsSet().stream()
                .filter(e -> e.getCurricularCourse() == course).findAny().orElseThrow();
    }

    private void approveEnrolment(Enrolment enrolment) {
        GradeScale gradeScale = enrolment.getStudentCurricularPlan().getDegree().getNumericGradeScale();
        gradeScale.setMinimumApprovedGrade(new BigDecimal("10"));
        gradeScale.setMaximumApprovedGrade(new BigDecimal("20"));

        EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
        Grade grade = Grade.createGrade("14", gradeScale);
        Person admin = User.findByUsername("admin").getPerson();

        evaluation.edit(admin, grade, new Date(), new Date());
        evaluation.confirmSubmission(admin, "Testing Enrolment Approval");
    }
}
