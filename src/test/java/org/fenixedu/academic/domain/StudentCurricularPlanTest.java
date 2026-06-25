package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V2;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroupType;
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

    private static Student student;
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

        student = StudentTest.createStudent("Student A", STUDENT_USERNAME);

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

        student.getRegistrationsSet().stream().filter(r -> r != registration).forEach(Registration::delete);
    }

    private StudentCurricularPlan create(Registration registration, DegreeCurricularPlan dcp, ExecutionInterval interval) {
        return StudentCurricularPlan.createBolonhaStudentCurricularPlan(registration, dcp,
                interval.getBeginDateYearMonthDay(), interval);
    }

    @Test
    public void testStudentCurricularPlan_create() {
        StudentCurricularPlan scp = create(registration, dcpV2, executionInterval);

        assertNotNull(scp);
        assertEquals(registration, scp.getRegistration());
        assertEquals(scp.getRegistration(), scp.getStudent());
        assertEquals(dcpV2, scp.getDegreeCurricularPlan());
        assertEquals(executionInterval, scp.getStartExecutionInterval());
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
        assertEquals(scp.getRegistration(), scp.getStudent());
        assertEquals(dcpV2, scp.getDegreeCurricularPlan());
        assertEquals(executionInterval, scp.getStartExecutionInterval());

        scp.delete();

        assertNull(scp.getRootDomainObject());
        assertNull(scp.getRegistration());
        assertNull(scp.getStudent());
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
    public void testStudentCurricularPlan_comparatorByDegreeTypeAndDegreeName() {
        // New DegreeCurricularPlan with different DegreeType
        DegreeType masterDegreeType = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).orElseThrow();
        Degree masterDegree =
                DegreeTest.createDegree(masterDegreeType, DegreeTest.MASTER_DEGREE_TYPE_CODE, "Master", executionYear);
        DegreeCurricularPlan masterDcp = new DegreeCurricularPlan(masterDegree, "Master_Degree", AcademicPeriod.TWO_YEAR);
        masterDcp.createExecutionDegree(executionYear);

        Registration mastersRegistration = StudentTest.createRegistration(student, masterDcp, executionYear);
        StudentCurricularPlan mastersScp = mastersRegistration.getStudentCurricularPlansSet().iterator().next();

        // New DegreeCurricularPlan with same DegreeType and different Degree name
        DegreeType newDegreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        Degree newDegree = DegreeTest.createDegree(newDegreeType, "NEW_DEGREE", "New Degree", executionYear);
        DegreeCurricularPlan newDcp = new DegreeCurricularPlan(newDegree, "New_Degree", AcademicPeriod.THREE_YEAR);
        newDcp.createExecutionDegree(executionYear);

        Registration newRegistration = StudentTest.createRegistration(student, newDcp, executionYear);
        StudentCurricularPlan newScp = newRegistration.getStudentCurricularPlansSet().iterator().next();

        List<StudentCurricularPlan> plans = Arrays.asList(newScp, mastersScp, scpV1);
        plans.sort(StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME);

        assertEquals(scpV1, plans.get(0));
        assertEquals(newScp, plans.get(1));
        assertEquals(mastersScp, plans.get(2));

        assertEquals(0,
                StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME.compare(scpV1, scpV1));
        assertEquals(1,
                StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME.compare(mastersScp,
                        scpV1));
        assertEquals(10, // Same DegreeType but different Degree name
                StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME.compare(newScp, scpV1));
        assertEquals(-1, StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME.compare(newScp,
                mastersScp));
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

        ExecutionInterval earlierInterval = executionInterval.getPrevious();
        CurricularPeriod secondSemester = new CurricularPeriod(AcademicPeriod.SEMESTER, 2, semesterPeriod.getParent());
        new Context(dcpV1.getRoot(), curricularCourseA, secondSemester, earlierInterval, null);
        new Enrolment(scpV1, scpV1.getRoot(), curricularCourseA, earlierInterval, EnrollmentCondition.FINAL, STUDENT_USERNAME);

        assertEquals(2, scpV1.countEnrolmentsByCurricularCourse(curricularCourseA, executionInterval));

        // We won't ever have 3 semesters...
        // This is just for easier demonstration and test case of e.getExecutionInterval().isBeforeOrEquals(untilExecutionInterval)
        ExecutionInterval laterInterval = executionInterval.getNext();
        CurricularPeriod laterSemester = new CurricularPeriod(AcademicPeriod.SEMESTER, 3, semesterPeriod.getParent());
        new Context(dcpV1.getRoot(), curricularCourseA, laterSemester, laterInterval, null);
        new Enrolment(scpV1, scpV1.getRoot(), curricularCourseA, laterInterval, EnrollmentCondition.FINAL, STUDENT_USERNAME);

        assertEquals(2, scpV1.countEnrolmentsByCurricularCourse(curricularCourseA, executionInterval));
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

    @Test
    public void testStudentCurricularPlan_getLastApprovement() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertNull(scpV1.getLastApprovement());

        approveEnrolment(enrolmentInCourseA);

        assertEquals(enrolmentInCourseA, scpV1.getLastApprovement());

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);
        approveEnrolment(enrolmentInCourseB);

        assertEquals(enrolmentInCourseB, scpV1.getLastApprovement());
    }

    @Test
    public void testStudentCurricularPlan_getAprovedEnrolments() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertTrue(scpV1.getAprovedEnrolments().isEmpty());

        approveEnrolment(enrolmentInCourseA);

        assertEquals(1, scpV1.getAprovedEnrolments().size());
        assertTrue(scpV1.getAprovedEnrolments().contains(enrolmentInCourseA));

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(1, scpV1.getAprovedEnrolments().size());
        assertFalse(scpV1.getAprovedEnrolments().contains(enrolmentInCourseB));

        approveEnrolment(enrolmentInCourseB);

        assertEquals(2, scpV1.getAprovedEnrolments().size());
        assertTrue(scpV1.getAprovedEnrolments().contains(enrolmentInCourseB));
    }

    @Test
    public void testStudentCurricularPlan_hasAnyApprovedEnrolment() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertFalse(scpV1.hasAnyApprovedEnrolment());

        approveEnrolment(enrolmentInCourseA);

        assertTrue(scpV1.hasAnyApprovedEnrolment());
    }

    @Test
    public void testStudentCurricularPlan_getDissertationEnrolments() {
        createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.getEnrolmentsSet().size()); // enrolmentInCourseA
        assertTrue(scpV1.getDissertationEnrolments().isEmpty());

        Enrolment dissertationEnrolment = createDissertationEnrolment();

        assertEquals(2, scpV1.getEnrolmentsSet().size()); // enrolmentInCourseA + dissertationEnrolment
        assertEquals(1, scpV1.getDissertationEnrolments().size());
        assertTrue(scpV1.getDissertationEnrolments().contains(dissertationEnrolment));
    }

    @Test
    public void testStudentCurricularPlan_getLatestDissertationEnrolment() {
        assertTrue(scpV1.getDissertationEnrolments().isEmpty());
        assertNull(scpV1.getLatestDissertationEnrolment());

        Enrolment dissertationEnrolment = createDissertationEnrolment();

        assertTrue(scpV1.getDissertationEnrolments().contains(dissertationEnrolment));
        assertEquals(dissertationEnrolment, scpV1.getLatestDissertationEnrolment());
    }

    @Test
    public void testStudentCurricularPlan_getEnrolmentByCurricularCourseAndExecutionPeriod() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(enrolmentInCourseA,
                scpV1.getEnrolmentByCurricularCourseAndExecutionPeriod(curricularCourseA, executionInterval));

        assertNull(scpV1.getEnrolmentByCurricularCourseAndExecutionPeriod(curricularCourseB, executionInterval));

        ExecutionInterval laterInterval = executionYear.getLastExecutionPeriod();
        assertNull(scpV1.getEnrolmentByCurricularCourseAndExecutionPeriod(curricularCourseA, laterInterval));
    }

    @Test
    public void testStudentCurricularPlan_getEnrolmentsExecutionPeriods() {
        createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.getEnrolmentsExecutionPeriods().size());
        assertTrue(scpV1.getEnrolmentsExecutionPeriods().contains(executionInterval));

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(1, scpV1.getEnrolmentsExecutionPeriods().size());

        ExecutionInterval newExecutionInterval = executionYear.getLastExecutionPeriod();
        enrolmentInCourseB.setExecutionPeriod(newExecutionInterval);

        assertEquals(2, scpV1.getEnrolmentsExecutionPeriods().size());
        assertTrue(scpV1.getEnrolmentsExecutionPeriods().contains(newExecutionInterval));
    }

    @Test
    public void testStudentCurricularPlan_getLastExecutionYear() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(executionYear, scpV1.getLastExecutionYear());

        ExecutionYear previousYear = (ExecutionYear) executionYear.getPrevious();
        enrolmentInCourseA.setExecutionPeriod(previousYear);

        assertNotEquals(executionYear, scpV1.getLastExecutionYear());
        assertEquals(previousYear, scpV1.getLastExecutionYear());
    }

    @Test
    public void testStudentCurricularPlan_getAllEnrollments() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertEquals(1, scpV1.getAllEnrollments().size());
        assertTrue(scpV1.getAllEnrollments().contains(enrolmentInCourseA));

        Enrolment enrolmentInCourseB = createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);

        assertEquals(2, scpV1.getAllEnrollments().size());
        assertTrue(scpV1.getAllEnrollments().contains(enrolmentInCourseB));
    }

    @Test
    public void testStudentCurricularPlan_getStudentEnrollmentsWithApprovedState() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertTrue(scpV1.getStudentEnrollmentsWithApprovedState().isEmpty());

        approveEnrolment(enrolmentInCourseA);

        assertEquals(1, scpV1.getStudentEnrollmentsWithApprovedState().size());
        assertTrue(scpV1.getStudentEnrollmentsWithApprovedState().contains(enrolmentInCourseA));
    }

    @Test
    public void testStudentCurricularPlan_isCurricularCourseApproved() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertFalse(scpV1.isCurricularCourseApproved(curricularCourseA));

        approveEnrolment(enrolmentInCourseA);

        assertTrue(scpV1.isCurricularCourseApproved(curricularCourseA));

        CurricularCourse newCurricularCourse = createCurricularCourseWithSameCompetenceAsCurricularCourseA();

        assertTrue(scpV1.isCurricularCourseApproved(
                newCurricularCourse)); // because curricularCourseA and newCurricularCourse are derived from the same CompetenceCourse
        assertFalse(scpV1.isCurricularCourseApproved(curricularCourseB));
    }

    @Test
    public void testStudentCurricularPlan_getNumberOfApprovedCurricularCourses() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertFalse(enrolmentInCourseA.isApproved());
        assertEquals(0, scpV1.getNumberOfApprovedCurricularCourses());
        assertEquals(2,
                scpV1.getDegreeCurricularPlan().getCurricularCoursesSet().size()); // curricularCourseA + curricularCourseB

        approveEnrolment(enrolmentInCourseA);

        assertTrue(enrolmentInCourseA.isApproved());
        assertTrue(scpV1.hasAnyApprovedEnrolment());
        assertEquals(1, scpV1.getAprovedEnrolments().size());
        assertEquals(1, scpV1.getNumberOfApprovedCurricularCourses());

        createCurricularCourseWithSameCompetenceAsCurricularCourseA();

        // If the student is approved to curricularCourseA, then he is also approved to newCurricularCourse
        // because both are derived from the same CompetenceCourse
        assertEquals(3, scpV1.getDegreeCurricularPlan().getCurricularCoursesSet().size());
        assertEquals(2, scpV1.getNumberOfApprovedCurricularCourses());
    }

    @Test
    public void testStudentCurricularPlan_hasEquivalenceIn() {
        CurricularCourse newCurricularCourse = createCurricularCourseWithSameCompetenceAsCurricularCourseA();

        assertFalse(scpV1.hasEquivalenceIn(curricularCourseA, List.of()));
        assertFalse(scpV1.hasEquivalenceIn(curricularCourseA, List.of(curricularCourseB)));
        assertTrue(scpV1.hasEquivalenceIn(curricularCourseA, List.of(curricularCourseA)));
        // curricularCourseA and newCurricularCourse share the same CompetenceCourse
        assertTrue(scpV1.hasEquivalenceIn(curricularCourseA, List.of(newCurricularCourse, curricularCourseB)));
    }

    @Test
    public void testStudentCurricularPlan_getEnrolmentsEctsCredits() {
        createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        double creditsBeforeEnrolmentInCourseB = scpV1.getEnrolmentsEctsCredits(executionYear);

        assertEquals(6d, creditsBeforeEnrolmentInCourseB, 0d); // 6 credits from curricularCourseA

        createEnrolmentInCourse(curricularCourseB, curricularCourseBContext,
                executionInterval); // 15 + 15 credits from curricularCourseB because it is annual (see CompetenceCourseTest:82)

        assertEquals(36d, scpV1.getEnrolmentsEctsCredits(executionYear), 0d);
    }

    @Test
    public void testStudentCurricularPlan_getEnroledImprovements() {
        Enrolment enrolmentInCourseA = createEnrolmentInCourse(curricularCourseA, curricularCourseAContext, executionInterval);

        assertTrue(scpV1.getEnroledImprovements(executionInterval).isEmpty());

        EvaluationSeason improvementSeason =
                EvaluationSeason.findByCode(EvaluationSeasonTest.IMPROVEMENT_SEASON_CODE).orElseThrow();
        EnrolmentEvaluation improvementEvaluation = new EnrolmentEvaluation(enrolmentInCourseA, improvementSeason);
        improvementEvaluation.setExecutionPeriod(executionInterval);

        assertEquals(1, scpV1.getEnroledImprovements(executionInterval).size());
        assertTrue(scpV1.getEnroledImprovements(executionInterval).contains(improvementEvaluation));

        improvementEvaluation.delete();
    }

    @Test
    public void testStudentCurricularPlan_getExtraCurricularCurriculumLines() {
        assertTrue(scpV1.getExtraCurricularCurriculumLines().isEmpty());

        Enrolment extraEnrolment =
                createNoCourseGroupEnrolment(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR, curricularCourseB,
                        executionInterval);

        assertEquals(1, scpV1.getExtraCurricularCurriculumLines().size());
        assertTrue(scpV1.getExtraCurricularCurriculumLines().contains(extraEnrolment));
    }

    @Test
    public void testStudentCurricularPlan_getStandaloneCurriculumLines() {
        assertTrue(scpV1.getStandaloneCurriculumLines().isEmpty());

        Enrolment standaloneEnrolment =
                createNoCourseGroupEnrolment(NoCourseGroupCurriculumGroupType.STANDALONE, curricularCourseB, executionInterval);

        assertEquals(1, scpV1.getStandaloneCurriculumLines().size());
        assertTrue(scpV1.getStandaloneCurriculumLines().contains(standaloneEnrolment));
    }

    @Test
    public void testStudentCurricularPlan_getPropaedeuticCurriculumLines() {
        assertTrue(scpV1.getPropaedeuticCurriculumLines().isEmpty());

        Enrolment propaedeuticEnrolment =
                createNoCourseGroupEnrolment(NoCourseGroupCurriculumGroupType.PROPAEDEUTICS, curricularCourseB,
                        executionInterval);

        assertEquals(1, scpV1.getPropaedeuticCurriculumLines().size());
        assertTrue(scpV1.getPropaedeuticCurriculumLines().contains(propaedeuticEnrolment));
    }

    @Test
    public void testStudentCurricularPlan_getPropaedeuticEnrolments() {
        assertTrue(scpV1.getPropaedeuticEnrolments().isEmpty());

        Enrolment propaedeuticEnrolment =
                createNoCourseGroupEnrolment(NoCourseGroupCurriculumGroupType.PROPAEDEUTICS, curricularCourseB,
                        executionInterval);

        assertEquals(1, scpV1.getPropaedeuticEnrolments().size());
        assertTrue(scpV1.getPropaedeuticEnrolments().contains(propaedeuticEnrolment));
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

    private CurricularCourse createCurricularCourseWithSameCompetenceAsCurricularCourseA() {
        CurricularCourse newCurricularCourse = new CurricularCourse();
        newCurricularCourse.setCompetenceCourse(CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE));
        newCurricularCourse.setName(NEW_CC_NAME);
        new Context(dcpV1.getRoot(), newCurricularCourse, semesterPeriod, executionInterval, null);
        return newCurricularCourse;
    }

    private Enrolment createDissertationEnrolment() {
        CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_B_CODE);
        assertNotNull(competenceCourse);
        competenceCourse.setCompetenceCourseType(
                CompetenceCourseType.findByCode(CompetenceCourseType.DISSERTATION).orElseThrow());

        return createEnrolmentInCourse(curricularCourseB, curricularCourseBContext, executionInterval);
    }

    private Enrolment createNoCourseGroupEnrolment(NoCourseGroupCurriculumGroupType type, CurricularCourse course,
            ExecutionInterval interval) {
        NoCourseGroupCurriculumGroup group = scpV1.getNoCourseGroupCurriculumGroup(type);
        if (group == null) {
            group = scpV1.createNoCourseGroupCurriculumGroup(type);
        }
        return new Enrolment(scpV1, group, course, interval, EnrollmentCondition.FINAL, STUDENT_USERNAME);
    }
}
