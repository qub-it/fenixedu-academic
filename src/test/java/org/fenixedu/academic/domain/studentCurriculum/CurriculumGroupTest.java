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
    public void testCurriculumGroup_getEnrolments_countPerGroup() {
        // mandatory has 3 enrolments (C1–C3); optional 0 (C4 is a dismissal, C5 never taken); root aggregates all 3 recursively
        assertEquals(3, mandatoryCurriculumGroup.getEnrolments().size());
        assertEquals(0, optionalCurriculumGroup.getEnrolments().size());
        assertEquals(3, rootCurriculumGroup.getEnrolments().size());
    }

    @Test
    public void testCurriculumGroup_getEnrolmentsSet_countPerGroup() {
        // Same as getEnrolments() but returns the raw Fenix relation set
        assertEquals(3, mandatoryCurriculumGroup.getEnrolmentsSet().size());
        assertEquals(0, optionalCurriculumGroup.getEnrolmentsSet().size());
        assertEquals(3, rootCurriculumGroup.getEnrolmentsSet().size());
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
    public void testCurriculumGroup_getAllCurriculumGroups_includesSelfAndDescendants() {
        // Recursively collects all groups in cycle's subtree: cycle + mandatory + optional = 3
        final Set<CurriculumGroup> allInCycle = cycleCurriculumGroup.getAllCurriculumGroups();
        assertEquals(3, allInCycle.size());
        assertTrue(allInCycle.contains(cycleCurriculumGroup));
        assertTrue(allInCycle.contains(mandatoryCurriculumGroup));
        assertTrue(allInCycle.contains(optionalCurriculumGroup));
    }

    @Test
    public void testCurriculumGroup_getAllCurriculumLines_collectsRecursively() {
        // Recursively collects all lines: root has 4 (C1–C3 enrolments + C4 dismissal), mandatory 3, optional 1
        assertEquals(4, rootCurriculumGroup.getAllCurriculumLines().size());
        assertEquals(3, mandatoryCurriculumGroup.getAllCurriculumLines().size());
        assertEquals(1, optionalCurriculumGroup.getAllCurriculumLines().size());
    }

    @Test
    public void testCurriculumGroup_getBranchCurriculumGroups_whenNotBranch_returnsEmpty() {
        // mandatory has no branch type set -> isBranchCurriculumGroup returns false, getBranchCurriculumGroups returns empty
        assertFalse(mandatoryCurriculumGroup.isBranchCurriculumGroup());
        assertTrue(mandatoryCurriculumGroup.getBranchCurriculumGroups().isEmpty());
    }

    @Test
    public void testCurriculumGroup_getBranchCurriculumGroups_withBranch_returnsBranchGroup() {
        // Temporarily set BranchType.MAJOR on the optional CourseGroup; cycle then detects it as a branch
        final DegreeCurricularPlan dcp = studentCurricularPlan.getDegreeCurricularPlan();
        final CourseGroup cycleCourseGroup =
                ConclusionRulesTestUtil.getChildGroup(dcp.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        final CourseGroup optionalCourseGroup =
                ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.OPTIONAL_GROUP);

        optionalCourseGroup.setBranchType(BranchType.MAJOR);
        try {
            assertTrue(optionalCurriculumGroup.isBranchCurriculumGroup());
            assertTrue(cycleCurriculumGroup.getBranchCurriculumGroups().contains(optionalCurriculumGroup));
        } finally {
            optionalCourseGroup.setBranchType(null);
        }
    }

    @Test
    public void testCurriculumGroup_isApproved_returnsTrueForApprovedCourse() {
        // C1 is approved via ConclusionRulesTestUtil.approve; C2/C3 are enroled-only; C4 has a dismissal (not a matching enrolment)
        assertTrue(mandatoryCurriculumGroup.isApproved(cc1, null));
        assertFalse(mandatoryCurriculumGroup.isApproved(cc2, null));
        assertFalse(mandatoryCurriculumGroup.isApproved(cc3, null));
        assertFalse(mandatoryCurriculumGroup.isApproved(cc4, null));
    }

    @Test
    public void testCurriculumGroup_isApproved_delegatesToChildren() {
        // root.isApproved recursively delegates to descendants -> finds C1 approved; C2 not approved
        assertTrue(rootCurriculumGroup.isApproved(cc1, null));
        assertFalse(rootCurriculumGroup.isApproved(cc2, null));
    }

    @Test
    public void testCurriculumGroup_isEnroledInExecutionPeriod_matchesCourseContextInterval() {
        // C1 context=1Y1S -> enroled in firstSemester; C2 context=1Y2S -> enroled in secondSemester; C4 has no enrolment
        assertTrue(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc1, firstSemester));
        assertTrue(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc2, secondSemester));
        assertFalse(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc4, firstSemester));
    }

    @Test
    public void testCurriculumGroup_hasEnrolmentWithEnroledState_excludesApprovedAndMissing() {
        // C1 is approved (enrollment state is not ENROLLED) -> false; C2 is enroled in secondSemester -> true; C4 has no enrolment -> false
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc1, firstSemester));
        assertTrue(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc2, secondSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc4, firstSemester));
    }

    @Test
    public void testCurriculumGroup_hasDegreeModule_findsRecursivelyExcludesMissing() {
        // root finds C1–C4 recursively; cycle finds C1/C2 as descendants; NONEXISTENT code returns false
        DegreeCurricularPlan dcp = rootCurriculumGroup.getStudentCurricularPlan().getDegreeCurricularPlan();
        assertTrue(rootCurriculumGroup.hasDegreeModule(cc1));
        assertTrue(rootCurriculumGroup.hasDegreeModule(cc2));
        assertTrue(rootCurriculumGroup.hasDegreeModule(cc3));
        assertTrue(rootCurriculumGroup.hasDegreeModule(cc4));
        assertFalse(rootCurriculumGroup.hasDegreeModule(dcp.getCurricularCourseByCode("NONEXISTENT")));

        assertTrue(cycleCurriculumGroup.hasDegreeModule(cc1));
        assertTrue(cycleCurriculumGroup.hasDegreeModule(cc2));
    }

    @Test
    public void testCurriculumGroup_hasCurriculumModule_identifiesSelfAndDescendantsExcludesSiblings() {
        // Self, direct child, and recursive descendant -> true; non-child group, non-child dismissal, null -> false
        assertTrue(rootCurriculumGroup.hasCurriculumModule(rootCurriculumGroup));
        assertTrue(cycleCurriculumGroup.hasCurriculumModule(mandatoryCurriculumGroup));
        assertTrue(rootCurriculumGroup.hasCurriculumModule(enrolmentCc1));

        assertFalse(mandatoryCurriculumGroup.hasCurriculumModule(optionalCurriculumGroup));
        assertFalse(mandatoryCurriculumGroup.hasCurriculumModule(optionalCurriculumGroup.getChildDismissals().get(0)));
        assertFalse(rootCurriculumGroup.hasCurriculumModule(null));
    }

    @Test
    public void testCurriculumGroup_hasCurriculumLines_onlyGroupsWithDirectChildLines() {
        // mandatory/optional have direct child lines; root/cycle only have group children (no direct lines)
        assertTrue(mandatoryCurriculumGroup.hasCurriculumLines());
        assertTrue(optionalCurriculumGroup.hasCurriculumLines());
        assertFalse(rootCurriculumGroup.hasCurriculumLines());
        assertFalse(cycleCurriculumGroup.hasCurriculumLines());
    }

    @Test
    public void testCurriculumGroup_hasCourseGroup_parentFindsChildrenExcludesSiblings() {
        // cycle has mandatory + optional as children; mandatory does NOT have cycle (parent); optional does NOT have mandatory (sibling)
        assertTrue(cycleCurriculumGroup.hasCourseGroup(mandatoryCurriculumGroup.getDegreeModule()));
        assertTrue(cycleCurriculumGroup.hasCourseGroup(optionalCurriculumGroup.getDegreeModule()));
        assertFalse(mandatoryCurriculumGroup.hasCourseGroup(cycleCurriculumGroup.getDegreeModule()));
        assertFalse(optionalCurriculumGroup.hasCourseGroup(mandatoryCurriculumGroup.getDegreeModule()));
    }

    @Test
    public void testCurriculumGroup_hasAnyCurriculumModules_withPredicate() {
        // Predicate 1: approved enrolment -> root has (C1), optional doesn't
        final Predicate<CurriculumModule> isApprovedEnrolment = cm -> cm.isEnrolment() && ((Enrolment) cm).isApproved();
        assertTrue(rootCurriculumGroup.hasAnyCurriculumModules(isApprovedEnrolment));
        assertFalse(optionalCurriculumGroup.hasAnyCurriculumModules(isApprovedEnrolment));

        // Predicate 2: isDismissal -> root has (C4), optional has (C4), mandatory doesn't
        final Predicate<CurriculumModule> isDismissal = CurriculumModule::isDismissal;
        assertTrue(rootCurriculumGroup.hasAnyCurriculumModules(isDismissal));
        assertTrue(optionalCurriculumGroup.hasAnyCurriculumModules(isDismissal));
        assertFalse(mandatoryCurriculumGroup.hasAnyCurriculumModules(isDismissal));

        // Predicate 3: always false -> no match
        assertFalse(rootCurriculumGroup.hasAnyCurriculumModules(cm -> false));
    }

    @Test
    public void testCurriculumGroup_findEnrolmentFor_findsByCourseAndInterval() {
        // C1 (1Y1S) found in firstSemester in both mandatory and root; not found in secondSemester; C4 has no enrolment
        final Enrolment found = mandatoryCurriculumGroup.findEnrolmentFor(cc1, firstSemester);
        assertNotNull(found);
        assertEquals(cc1, found.getCurricularCourse());

        final Enrolment fromRoot = rootCurriculumGroup.findEnrolmentFor(cc1, firstSemester);
        assertNotNull(fromRoot);
        assertEquals(cc1, fromRoot.getCurricularCourse());

        assertNull(mandatoryCurriculumGroup.findEnrolmentFor(cc1, secondSemester));
        assertNull(mandatoryCurriculumGroup.findEnrolmentFor(cc4, firstSemester));
    }

    @Test
    public void testCurriculumGroup_getApprovedEnrolment_returnsApprovedOnly() {
        // C1 is approved -> found in mandatory and root; C2 not approved -> null; cc1 not in optional -> null
        Enrolment approved = mandatoryCurriculumGroup.getApprovedEnrolment(cc1);
        assertNotNull(approved);
        assertEquals(cc1, approved.getCurricularCourse());

        Enrolment fromRoot = rootCurriculumGroup.getApprovedEnrolment(cc1);
        assertNotNull(fromRoot);
        assertEquals(cc1, fromRoot.getCurricularCourse());

        assertNull(mandatoryCurriculumGroup.getApprovedEnrolment(cc2));
        assertNull(optionalCurriculumGroup.getApprovedEnrolment(cc1));
    }

    @Test
    public void testCurriculumGroup_getDismissal_findsRecursively() {
        // C4 has a dismissal -> found in optional and root recursively; C1/C2 have no dismissal -> null
        assertNotNull(optionalCurriculumGroup.getDismissal(cc4));
        assertNotNull(rootCurriculumGroup.getDismissal(cc4));

        assertNull(mandatoryCurriculumGroup.getDismissal(cc4));
        assertNull(mandatoryCurriculumGroup.getDismissal(cc1));
        assertNull(rootCurriculumGroup.getDismissal(cc1));
    }

    @Test
    public void testCurriculumGroup_getApprovedCurriculumLine_returnsApprovedOnly() {
        // C1 approved -> found as CurriculumLine in mandatory and root; C2 not approved -> null
        final CurriculumLine line = mandatoryCurriculumGroup.getApprovedCurriculumLine(cc1);
        assertNotNull(line);
        assertTrue(line.isApproved());
        assertEquals(cc1, line.getCurricularCourse());

        final CurriculumLine fromRoot = rootCurriculumGroup.getApprovedCurriculumLine(cc1);
        assertNotNull(fromRoot);
        assertTrue(fromRoot.isApproved());
        assertEquals(cc1, fromRoot.getCurricularCourse());

        assertNull(mandatoryCurriculumGroup.getApprovedCurriculumLine(cc2));
    }

    @Test
    public void testCurriculumGroup_findCurriculumGroupFor_findsByDegreeModuleIncludingSelf() {
        // Each CurriculumGroup can be found by its CourseGroup degree module, including self-matching
        assertEquals(mandatoryCurriculumGroup,
                rootCurriculumGroup.findCurriculumGroupFor(mandatoryCurriculumGroup.getDegreeModule()));
        assertEquals(optionalCurriculumGroup,
                rootCurriculumGroup.findCurriculumGroupFor(optionalCurriculumGroup.getDegreeModule()));
        assertEquals(optionalCurriculumGroup,
                optionalCurriculumGroup.findCurriculumGroupFor(optionalCurriculumGroup.getDegreeModule()));
    }

    @Test
    public void testCurriculumGroup_findCurriculumGroupFor_returnsNullWhenNotFound() {
        // A CourseGroup not present in the student's plan returns null
        final CourseGroup courseGroup =
                new CourseGroup(mandatoryCurriculumGroup.getDegreeModule(), "New", "New", executionYear, null, null);
        assertNull(rootCurriculumGroup.findCurriculumGroupFor(courseGroup));
    }

    @Test
    public void testCurriculumGroup_getNoCourseGroupCurriculumGroup_findsByType() {
        // Create an EXTRA_CURRICULAR NoCourseGroup; look it up by type -> found
        final NoCourseGroupCurriculumGroupType type = NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR;
        final NoCourseGroupCurriculumGroup extra = NoCourseGroupCurriculumGroup.create(type, rootCurriculumGroup);
        try {
            final NoCourseGroupCurriculumGroup found = rootCurriculumGroup.getNoCourseGroupCurriculumGroup(type);
            assertNotNull(found);
            assertEquals(type, found.getNoCourseGroupCurriculumGroupType());
        } finally {
            extra.delete();
        }
    }

    @Test
    public void testCurriculumGroup_getNumberOfChildCurriculumGroupsWithCourseGroup_perGroupLevel() {
        // root: 1 (cycle); cycle: 2 (mandatory + optional); mandatory: 0 (leaf, no child groups)
        assertEquals(1, rootCurriculumGroup.getNumberOfChildCurriculumGroupsWithCourseGroup());
        assertEquals(2, cycleCurriculumGroup.getNumberOfChildCurriculumGroupsWithCourseGroup());
        assertEquals(0, mandatoryCurriculumGroup.getNumberOfChildCurriculumGroupsWithCourseGroup());
    }

    @Test
    public void testCurriculumGroup_getNumberOfApprovedChildCurriculumLines_perGroup() {
        // cycle has no direct child lines (only groups); mandatory has 1 (C1 approved); optional has 1 (C4 dismissal isApproved=true)
        assertEquals(0, cycleCurriculumGroup.getNumberOfApprovedChildCurriculumLines());
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfApprovedChildCurriculumLines());
        assertEquals(1, optionalCurriculumGroup.getNumberOfApprovedChildCurriculumLines());
    }

    @Test
    public void testCurriculumGroup_getNumberOfAllApprovedCurriculumLines_recursive() {
        // Recursively counts approved lines: cycle -> C1 + C4 = 2; mandatory -> C1 = 1; optional -> C4 = 1
        assertEquals(2, cycleCurriculumGroup.getNumberOfAllApprovedCurriculumLines());
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfAllApprovedCurriculumLines());
        assertEquals(1, optionalCurriculumGroup.getNumberOfAllApprovedCurriculumLines());
    }

    @Test
    public void testCurriculumGroup_getNumberOfChildEnrolments_perInterval() {
        // Filters direct children by isEnroled()+isValid(interval):
        // C3 (firstSemester, enroled) -> 1; C2 (secondSemester, enroled) -> 1
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfChildEnrolments(firstSemester));
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfChildEnrolments(secondSemester));
        assertEquals(0, optionalCurriculumGroup.getNumberOfChildEnrolments(firstSemester));
        assertEquals(0, rootCurriculumGroup.getNumberOfChildEnrolments(firstSemester));
    }

    @Test
    public void testCurriculumGroup_getNumberOfChildEnrolments_byExecutionYear() {
        // Filters by isEnroled()+isValid(year): C2 + C3 = 2 for executionYear
        assertEquals(2, mandatoryCurriculumGroup.getNumberOfChildEnrolments(executionYear));
        assertEquals(0, optionalCurriculumGroup.getNumberOfChildEnrolments(executionYear));
        assertEquals(0, rootCurriculumGroup.getNumberOfChildEnrolments(executionYear));
    }

    @Test
    public void testCurriculumGroup_getNumberOfAllApprovedEnrolments_recursivePerInterval() {
        // C1 approved in firstSemester = 1; none approved in secondSemester; root aggregates recursively (C1 = 1)
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(0, mandatoryCurriculumGroup.getNumberOfAllApprovedEnrolments(secondSemester));
        assertEquals(0, optionalCurriculumGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(1, rootCurriculumGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(0, rootCurriculumGroup.getNumberOfAllApprovedEnrolments(secondSemester));
    }

    @Test
    public void testCurriculumGroup_getEnrolmentsBy_executionYear() {
        // C1–C3 all in executionYear -> 3 for root/mandatory (recursive), 0 for optional
        assertEquals(3, rootCurriculumGroup.getEnrolmentsBy(executionYear).size());
        assertEquals(3, mandatoryCurriculumGroup.getEnrolmentsBy(executionYear).size());
        assertEquals(0, optionalCurriculumGroup.getEnrolmentsBy(executionYear).size());
    }

    @Test
    public void testCurriculumGroup_getEnrolmentsBy_executionInterval() {
        // C1+C3 in firstSemester -> 2; C2 in secondSemester -> 1
        assertEquals(2, rootCurriculumGroup.getEnrolmentsBy(firstSemester).size());
        assertEquals(2, mandatoryCurriculumGroup.getEnrolmentsBy(firstSemester).size());
        assertEquals(1, mandatoryCurriculumGroup.getEnrolmentsBy(secondSemester).size());
    }

    @Test
    public void testCurriculumGroup_getDegreeModulesToEvaluate_aggregatesAcrossLevels() {
        // mandatory has 2 (open+not-approved) per interval; cycle aggregates children -> 5; root aggregates all -> 6
        assertEquals(2, mandatoryCurriculumGroup.getDegreeModulesToEvaluate(firstSemester).size());
        assertEquals(2, mandatoryCurriculumGroup.getDegreeModulesToEvaluate(secondSemester).size());
        assertEquals(2, optionalCurriculumGroup.getDegreeModulesToEvaluate(firstSemester).size());
        assertEquals(5, cycleCurriculumGroup.getDegreeModulesToEvaluate(firstSemester).size());
        assertEquals(6, rootCurriculumGroup.getDegreeModulesToEvaluate(firstSemester).size());
    }

    @Test
    public void testCurriculumGroup_getCourseGroupContextsToEnrol_includesUnenroledGroups() {
        // Without an unenroled CourseGroup child, cycle returns empty; create one and verify it appears
        assertTrue(cycleCurriculumGroup.getCourseGroupContextsToEnrol(firstSemester).isEmpty());

        final CourseGroup cycleCourseGroup =
                ConclusionRulesTestUtil.getChildGroup(studentCurricularPlan.getDegreeCurricularPlan().getRoot(),
                        ConclusionRulesTestUtil.CYCLE_GROUP);
        final CourseGroup newGroup =
                new CourseGroup(cycleCourseGroup, "TEST_ENROL", "Test Enrol Group", executionYear, null, null);

        try {
            final List<Context> contexts = cycleCurriculumGroup.getCourseGroupContextsToEnrol(firstSemester);
            assertFalse(contexts.isEmpty());
            assertTrue(contexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == newGroup));
        } finally {
            newGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_getCurricularCoursesToDismissal_excludesApprovedCourses() {
        // cycle/root have no direct CurricularCourse children -> empty
        assertTrue(cycleCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).isEmpty());
        assertTrue(rootCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).isEmpty());

        // mandatory: C1 approved -> filtered out; C2, C3 open + not approved -> 2
        assertEquals(2, mandatoryCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).size());
        assertTrue(mandatoryCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).contains(cc2));
        assertTrue(mandatoryCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).contains(cc3));

        // optional: C4 dismissal -> isApproved=true -> filtered out; C5 open + not approved -> 1
        assertEquals(1, optionalCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).size());
        assertFalse(optionalCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).contains(cc4));
    }

    @Test
    public void testCurriculumGroup_isEnroledInSpecialSeason_delegatesRecursivelyFromMandatory() {
        // Create a special-season EnrolmentEvaluation on C1 (enroled in mandatory);
        // groups containing C1 detect it recursively; optional has no C1 -> false
        final EvaluationSeason specialSeason =
                EvaluationSeason.findByCode(EvaluationSeasonTest.SPECIAL_SEASON_CODE).orElseThrow();
        final EnrolmentEvaluation evaluation = new EnrolmentEvaluation(enrolmentCc1, specialSeason);
        try {
            assertTrue(mandatoryCurriculumGroup.isEnroledInSpecialSeason(firstSemester));
            assertTrue(cycleCurriculumGroup.isEnroledInSpecialSeason(firstSemester));
            assertTrue(rootCurriculumGroup.isEnroledInSpecialSeason(firstSemester));
            assertTrue(mandatoryCurriculumGroup.isEnroledInSpecialSeason(executionYear));
            assertTrue(rootCurriculumGroup.isEnroledInSpecialSeason(executionYear));
            assertFalse(optionalCurriculumGroup.isEnroledInSpecialSeason(firstSemester));
            assertFalse(optionalCurriculumGroup.isEnroledInSpecialSeason(executionYear));
        } finally {
            evaluation.delete();
        }
    }

    @Test
    public void testCurriculumGroup_hasConcluded_selfFalseCourseTrueCreditLimitEnablesGroup() {
        final CourseGroup mandatoryCourseGroup = mandatoryCurriculumGroup.getDegreeModule();
        final CourseGroup optionalCourseGroup = optionalCurriculumGroup.getDegreeModule();
        final CourseGroup cycleCourseGroup = cycleCurriculumGroup.getDegreeModule();
        final CourseGroup rootCourseGroup = rootCurriculumGroup.getDegreeModule();

        // Self-check on each group's own CourseGroup: no CreditsLimit rule -> hasConcluded delegates to
        // isConcluded on child lines, which returns false because no enrolment has all credits approved for the group
        assertFalse(mandatoryCurriculumGroup.hasConcluded(mandatoryCourseGroup, executionYear));
        assertFalse(optionalCurriculumGroup.hasConcluded(optionalCourseGroup, executionYear));
        assertFalse(cycleCurriculumGroup.hasConcluded(cycleCourseGroup, executionYear));
        assertFalse(rootCurriculumGroup.hasConcluded(rootCourseGroup, executionYear));

        // Course-level: C1 is approved -> concluded; C4 has equivalence Dismissal (isConcluded always true) -> concluded
        assertTrue(mandatoryCurriculumGroup.hasConcluded(cc1, executionYear));
        assertTrue(optionalCurriculumGroup.hasConcluded(cc4, executionYear));
        assertTrue(rootCurriculumGroup.hasConcluded(cc1, executionYear));
        assertTrue(cycleCurriculumGroup.hasConcluded(cc1, executionYear));

        // C2 is enroled but not approved -> not concluded; C4 is not in mandatory -> not concluded
        assertFalse(mandatoryCurriculumGroup.hasConcluded(cc2, executionYear));
        assertFalse(mandatoryCurriculumGroup.hasConcluded(cc4, executionYear));

        // Add a CreditsLimit(min=6) to mandatory's CourseGroup; C1 = 6 ECTS approved -> now concluded at group level
        final CreditsLimit creditsLimit =
                new CreditsLimit(mandatoryCourseGroup, mandatoryCourseGroup, firstSemester, null, 6.0d, 6.0d);
        try {
            assertTrue(mandatoryCurriculumGroup.hasConcluded(mandatoryCourseGroup, executionYear));
        } finally {
            creditsLimit.delete();
        }
    }

    @Test
    public void testCurriculumGroup_getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups_returnsFilteredGroups() {
        // Create an EXTRA_CURRICULAR NoCourseGroup under root to test exclusion
        final NoCourseGroupCurriculumGroupType type = NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR;
        final NoCourseGroupCurriculumGroup extraGroup = NoCourseGroupCurriculumGroup.create(type, rootCurriculumGroup);
        try {
            // Root excludes itself (RootCurriculumGroup overrides) and NoCourseGroup -> 3: cycle, mandatory, optional
            assertEquals(3, rootCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups().size());
            assertTrue(rootCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups()
                    .contains(cycleCurriculumGroup));
            assertTrue(rootCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups()
                    .contains(mandatoryCurriculumGroup));
            assertTrue(rootCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups()
                    .contains(optionalCurriculumGroup));
            assertFalse(rootCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups().contains(extraGroup));

            // Cycle includes self + children, excluding NoCourseGroup -> 3: cycle, mandatory, optional
            assertEquals(3, cycleCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups().size());
            assertTrue(cycleCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups()
                    .contains(cycleCurriculumGroup));
            assertTrue(cycleCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups()
                    .contains(mandatoryCurriculumGroup));
            assertTrue(cycleCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups()
                    .contains(optionalCurriculumGroup));
            assertFalse(cycleCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups().contains(extraGroup));

            // Leaf groups (mandatory/optional) have no group children -> just themselves
            final Set<CurriculumGroup> mandatoryGroups =
                    mandatoryCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups();
            assertEquals(1, mandatoryGroups.size());
            assertTrue(mandatoryGroups.contains(mandatoryCurriculumGroup));

            final Set<CurriculumGroup> optionalGroups =
                    optionalCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups();
            assertEquals(1, optionalGroups.size());
            assertTrue(optionalGroups.contains(optionalCurriculumGroup));

            // NoCourseGroup itself returns empty set (its override excludes self and all descendants)
            assertTrue(extraGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups().isEmpty());
        } finally {
            extraGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_getNoCourseGroupCurriculumGroups_collectsRecursivelyIncludesSelf() {
        // getNoCourseGroupCurriculumGroups on a CurriculumGroup collects from children only;
        // on NoCourseGroupCurriculumGroup it includes itself + children
        final NoCourseGroupCurriculumGroupType type = NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR;
        final NoCourseGroupCurriculumGroup extraGroup = NoCourseGroupCurriculumGroup.create(type, rootCurriculumGroup);
        try {
            // root has extraGroup as a child -> finds it; mandatory has no NoCourseGroup children -> empty
            assertTrue(rootCurriculumGroup.getNoCourseGroupCurriculumGroups().contains(extraGroup));
            assertTrue(mandatoryCurriculumGroup.getNoCourseGroupCurriculumGroups().isEmpty());

            // NoCourseGroup includes itself and delegates to children (root's other groups -> none)
            assertTrue(extraGroup.getNoCourseGroupCurriculumGroups().contains(extraGroup));
        } finally {
            extraGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_hasEnrolmentWithEnroledState_delegatesFromRoot() {
        // root has no direct lines; delegates through cycle -> mandatory -> finds C2 enroled in secondSemester
        assertTrue(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc2, secondSemester));
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc2, firstSemester));

        // C1 is approved (state != ENROLLED) -> false; C4 has a dismissal (not an enrolment) -> false;
        // C5 has no student interaction at all -> false
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc1, firstSemester));
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc4, firstSemester));
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc5, firstSemester));
    }


    /* Tests for private utility methods commented out because methods are private

    @Test
    public void testCurriculumGroup_hasEnrolmentForInterval_checksEnrolmentAcrossGroups() {
        assertTrue(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc1, firstSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc1, secondSemester));

        assertTrue(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc2, secondSemester));

        // C4 has a dismissal (not an enrolment) -> false; C5 has no enrolment -> false
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc4, firstSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc5, firstSemester));
    }

    @Test
    public void testCurriculumGroup_canCreateGroupOrChilds_allowsWhenNoPreventingRule() {
        final CourseGroup mandatoryCourseGroup = mandatoryCurriculumGroup.getDegreeModule();

        // No rules -> true
        assertTrue(cycleCurriculumGroup.canCreateGroupOrChilds(cycleCurriculumGroup.getDegreeModule(), firstSemester));

        // Non-preventing rule (CreditsLimit) -> true
        CreditsLimit creditsLimit = new CreditsLimit(mandatoryCourseGroup, mandatoryCourseGroup, firstSemester, null, 6.0d, 6.0d);
        try {
            assertTrue(mandatoryCurriculumGroup.canCreateGroupOrChilds(mandatoryCourseGroup, firstSemester));

            // Preventing rule (Exclusiveness) -> false
            CourseGroup optionalCourseGroup = optionalCurriculumGroup.getDegreeModule();
            Exclusiveness exclusiveness = new Exclusiveness(mandatoryCourseGroup, optionalCourseGroup, null, firstSemester, null);
            try {
                assertFalse(mandatoryCurriculumGroup.canCreateGroupOrChilds(mandatoryCourseGroup, firstSemester));
            } finally {
                exclusiveness.delete();
            }
        } finally {
            creditsLimit.delete();
        }
    }
    */
}
