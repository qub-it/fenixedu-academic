package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeCurricularPlanTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.BranchType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CurriculumGroupTest {

    private static final String STUDENT_USERNAME = "curriculum.group.test.student";
    private static final String GRADE_SCALE_NUMERIC = "TYPE20";
    private static StudentCurricularPlan studentCurricularPlan;
    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static RootCurriculumGroup rootCurriculumGroup;
    private static CurriculumGroup cycleCurriculumGroup;
    private static CurriculumGroup mandatoryCurriculumGroup;
    private static CurriculumGroup optionalCurriculumGroup;
    private static CurricularCourse cc1, cc2, cc3, cc4, cc5;
    private static Enrolment enrolmentCc1;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initData();
            return null;
        });
    }

    public static void initData() {
        /**
         * Data setup (DegreeCurricularPlan):
         *
         *   Root -> Cycle -> Mandatory (C1: 1Y1S, C2: 1Y2S, C3: 2Y1S)
         *                -> Optional  (C4: 2Y1S, C5: 2Y1S)
         *
         *   C1: enrolled + approved (6 ECTS, firstSemester)
         *   C2: enrolled, not approved (secondSemester)
         *   C3: enrolled, not approved (firstSemester)
         *   C4: equivalence dismissal (not enrolled, isApproved=true)
         *   C5: unenroled
         *
         *   Groups: root (RootCurriculumGroup), cycle, mandatory, optional
         */

        DegreeCurricularPlanTest.initDegreeCurricularPlan();
        EvaluationSeasonTest.initEvaluationSeasons();
        StudentTest.initRegistrationConfigEntities();

        GradeScale.create(GRADE_SCALE_NUMERIC, new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true);

        executionYear = ExecutionYear.findCurrent(null);
        firstSemester = executionYear.getFirstExecutionPeriod();
        secondSemester = executionYear.getLastExecutionPeriod();

        final DegreeCurricularPlan dcp = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        cc1 = dcp.getCurricularCourseByCode("C1");
        cc2 = dcp.getCurricularCourseByCode("C2");
        cc3 = dcp.getCurricularCourseByCode("C3");
        cc4 = dcp.getCurricularCourseByCode("C4");
        cc5 = dcp.getCurricularCourseByCode("C5");

        final Student student = StudentTest.createStudent("Curriculum Group Test Student", STUDENT_USERNAME);
        final Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
        studentCurricularPlan = registration.getLastStudentCurricularPlan();

        rootCurriculumGroup = studentCurricularPlan.getRoot();
        final CourseGroup cycleCourseGroup =
                ConclusionRulesTestUtil.getChildGroup(dcp.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        cycleCurriculumGroup = rootCurriculumGroup.findCurriculumGroupFor(cycleCourseGroup);
        mandatoryCurriculumGroup = cycleCurriculumGroup.findCurriculumGroupFor(
                ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.MANDATORY_GROUP));
        optionalCurriculumGroup = cycleCurriculumGroup.findCurriculumGroupFor(
                ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.OPTIONAL_GROUP));

        ConclusionRulesTestUtil.enrol(studentCurricularPlan, executionYear, "C1", "C2", "C3");
        ConclusionRulesTestUtil.approve(studentCurricularPlan, "C1");
        ConclusionRulesTestUtil.createEquivalence(studentCurricularPlan, executionYear, "C4");
        enrolmentCc1 = findEnrolmentFor(cc1);
    }

    private static Enrolment findEnrolmentFor(final CurricularCourse course) {
        return studentCurricularPlan.getEnrolmentsSet().stream().filter(e -> e.getCurricularCourse() == course).findFirst()
                .orElseThrow();
    }

    @Test
    public void testCurriculumGroup_getChildDismissals_countPerGroup() {
        // mandatory has 3 enrolments (C1–C3), no dismissals -> 0; optional has 1 dismissal (C4) -> 1
        assertEquals(0, mandatoryCurriculumGroup.getChildDismissals().size());
        assertEquals(1, optionalCurriculumGroup.getChildDismissals().size());
        assertEquals(cc4, optionalCurriculumGroup.getChildDismissals().get(0).getCurricularCourse());
    }

    @Test
    public void testCurriculumGroup_getCurriculumLines_allReturnedLinesAreLeaf() {
        // root/cycle have no direct CurriculumLine children
        assertTrue(rootCurriculumGroup.getCurriculumLines().isEmpty());
        assertTrue(cycleCurriculumGroup.getCurriculumLines().isEmpty());

        // mandatory has 3 direct lines (C1–C3 enrolments); optional has 1 (C4 dismissal); all lines are isLeaf
        final Set<CurriculumLine> mandatoryCurriculumLines = mandatoryCurriculumGroup.getCurriculumLines();
        assertEquals(3, mandatoryCurriculumLines.size());
        assertTrue(mandatoryCurriculumLines.stream().allMatch(CurriculumModule::isLeaf));

        final Set<CurriculumLine> optionalCurriculumLines = optionalCurriculumGroup.getCurriculumLines();
        assertEquals(1, optionalCurriculumLines.size());
        assertTrue(optionalCurriculumLines.stream().allMatch(CurriculumModule::isLeaf));
    }

    @Test
    public void testCurriculumGroup_getChildCurriculumLines_allReturnedChildLinesAreLeaf() {
        // Same as getCurriculumLines but returns List<CurriculumLine> instead of Set
        assertTrue(rootCurriculumGroup.getChildCurriculumLines().isEmpty());
        assertTrue(cycleCurriculumGroup.getChildCurriculumLines().isEmpty());

        final List<CurriculumLine> mandatoryCurriculumLines = mandatoryCurriculumGroup.getChildCurriculumLines();
        assertEquals(3, mandatoryCurriculumLines.size());
        assertTrue(mandatoryCurriculumLines.stream().allMatch(CurriculumModule::isLeaf));

        final List<CurriculumLine> optionalCurriculumLines = optionalCurriculumGroup.getChildCurriculumLines();
        assertEquals(1, optionalCurriculumLines.size());
        assertTrue(optionalCurriculumLines.stream().allMatch(CurriculumModule::isLeaf));
    }

    @Test
    public void testCurriculumGroup_getCurriculumGroups_returnedGroupsAreNotLeaf() {
        // mandatory/optional are leaf-level groups with no child groups
        assertTrue(mandatoryCurriculumGroup.getCurriculumGroups().isEmpty());
        assertTrue(optionalCurriculumGroup.getCurriculumGroups().isEmpty());

        // cycle has 2 child groups: mandatory + optional; none are leaves
        final Set<CurriculumGroup> children = cycleCurriculumGroup.getCurriculumGroups();
        assertEquals(2, children.size());
        assertTrue(children.stream().noneMatch(CurriculumModule::isLeaf));
        assertTrue(children.contains(mandatoryCurriculumGroup));
        assertTrue(children.contains(optionalCurriculumGroup));
    }

    @Test
    public void testCurriculumGroup_getChildCurriculumGroups_returnedChildGroupsAreNotLeaf() {
        // Same as getCurriculumGroups but returns List<CurriculumGroup> instead of Set
        assertTrue(mandatoryCurriculumGroup.getChildCurriculumGroups().isEmpty());
        assertTrue(optionalCurriculumGroup.getChildCurriculumGroups().isEmpty());

        final List<CurriculumGroup> children = cycleCurriculumGroup.getChildCurriculumGroups();
        assertEquals(2, children.size());
        assertTrue(children.stream().noneMatch(CurriculumModule::isLeaf));
        assertTrue(children.contains(mandatoryCurriculumGroup));
        assertTrue(children.contains(optionalCurriculumGroup));
    }

    @Test
    public void testCurriculumGroup_getCurriculumGroups_includesNoCourseGroup() {
        // getCurriculumGroups returns ALL child CurriculumGroups, including NoCourseGroup subtypes
        final NoCourseGroupCurriculumGroup noCourseGroup =
                NoCourseGroupCurriculumGroup.create(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR, rootCurriculumGroup);
        try {
            assertTrue(rootCurriculumGroup.getCurriculumGroups().contains(noCourseGroup));
        } finally {
            noCourseGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_getCurriculumGroupsToEnrolmentProcess_filtersNoCourseGroup() {
        // getCurriculumGroupsToEnrolmentProcess should EXCLUDE NoCourseGroup groups (unlike getCurriculumGroups)
        final NoCourseGroupCurriculumGroup noCourseGroup =
                NoCourseGroupCurriculumGroup.create(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR, rootCurriculumGroup);
        try {
            final Set<CurriculumGroup> enrolGroups = rootCurriculumGroup.getCurriculumGroupsToEnrolmentProcess();
            assertFalse(enrolGroups.contains(noCourseGroup));
            assertTrue(enrolGroups.contains(cycleCurriculumGroup));
        } finally {
            noCourseGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_hasCurriculumLines_onlyGroupsWithDirectChildLines() {
        // mandatory/optional have direct child lines; root/cycle only have group children (no direct lines)
        assertTrue(mandatoryCurriculumGroup.hasCurriculumLines());
        assertTrue(optionalCurriculumGroup.hasCurriculumLines());
        assertFalse(rootCurriculumGroup.hasCurriculumLines());
        assertFalse(cycleCurriculumGroup.hasCurriculumLines());
    }
}
