package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
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
import org.fenixedu.academic.domain.degreeStructure.BranchType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.enrolment.DismissalCurriculumModuleWrapper;
import org.fenixedu.academic.domain.enrolment.EnroledCurriculumModuleWrapper;
import org.fenixedu.academic.domain.enrolment.EnroledEnrolmentWrapper;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CurriculumGroupTest {

    private static final String STUDENT_USERNAME = "curriculum.group.test.student";
    private static StudentCurricularPlan studentCurricularPlan;
    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static RootCurriculumGroup rootCurriculumGroup;
    private static CurriculumGroup cycleCurriculumGroup;
    private static CurriculumGroup mandatoryCurriculumGroup;
    private static CurriculumGroup optionalCurriculumGroup;
    private static CurricularCourse cc1, cc2, cc3, cc4, cc5;
    private static Enrolment enrolmentCc1, enrolmentCc2, enrolmentCc3, enrolmentCc4;
    private static Dismissal dismissalCc4;

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
         */

        ConclusionRulesTestUtil.initData();
        executionYear = ExecutionYear.findCurrent(null);
        firstSemester = executionYear.getFirstExecutionPeriod();
        secondSemester = executionYear.getLastExecutionPeriod();

        DegreeCurricularPlan dcp = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        cc1 = dcp.getCurricularCourseByCode("C1");
        cc2 = dcp.getCurricularCourseByCode("C2");
        cc3 = dcp.getCurricularCourseByCode("C3");
        cc4 = dcp.getCurricularCourseByCode("C4");
        cc5 = dcp.getCurricularCourseByCode("C5");

        final Student student = StudentTest.createStudent("Curriculum Group Test Student", STUDENT_USERNAME);
        final Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
        studentCurricularPlan = registration.getLastStudentCurricularPlan();

        rootCurriculumGroup = studentCurricularPlan.getRoot();
        CourseGroup cycleCourseGroup = ConclusionRulesTestUtil.getChildGroup(dcp.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        cycleCurriculumGroup = rootCurriculumGroup.findCurriculumGroupFor(cycleCourseGroup);
        mandatoryCurriculumGroup = cycleCurriculumGroup.findCurriculumGroupFor(
                ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.MANDATORY_GROUP));
        optionalCurriculumGroup = cycleCurriculumGroup.findCurriculumGroupFor(
                ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.OPTIONAL_GROUP));

        ConclusionRulesTestUtil.enrol(studentCurricularPlan, executionYear, "C1", "C2", "C3");
        ConclusionRulesTestUtil.approve(studentCurricularPlan, "C1");
        ConclusionRulesTestUtil.createEquivalence(studentCurricularPlan, executionYear, "C4");

        enrolmentCc1 = studentCurricularPlan.getEnrolments(cc1).get(0);
        enrolmentCc2 = studentCurricularPlan.getEnrolments(cc2).get(0);
        enrolmentCc3 = studentCurricularPlan.getEnrolments(cc3).get(0);
        dismissalCc4 = studentCurricularPlan.getDismissal(cc4);
    }

    @Test
    public void testCurriculumGroup_getChildDismissals() {
        // mandatory has 3 enrolments (C1–C3), no dismissals -> 0; optional has 1 dismissal (C4) -> 1
        assertEquals(0, mandatoryCurriculumGroup.getChildDismissals().size());
        assertEquals(1, optionalCurriculumGroup.getChildDismissals().size());
        assertEquals(cc4, optionalCurriculumGroup.getChildDismissals().get(0).getCurricularCourse());
    }

    @Test
    public void testCurriculumGroup_getChildCurriculumLines() {
        // root/cycleGroup have no direct CurriculumLine children
        assertTrue(rootCurriculumGroup.getChildCurriculumLines().isEmpty());
        assertTrue(cycleCurriculumGroup.getChildCurriculumLines().isEmpty());

        // mandatory has 3 direct curriculumLines (C1–C3 enrolments); optional has 1 (C4 dismissal)
        final List<CurriculumLine> mandatoryCurriculumLines = mandatoryCurriculumGroup.getChildCurriculumLines();
        assertEquals(3, mandatoryCurriculumLines.size());

        final List<CurriculumLine> optionalCurriculumLines = optionalCurriculumGroup.getChildCurriculumLines();
        assertEquals(1, optionalCurriculumLines.size());
    }

    @Test
    public void testCurriculumGroup_getCurriculumLines() {
        // Same as getChildCurriculumLines but returns Set instead of List
        assertTrue(rootCurriculumGroup.getCurriculumLines().isEmpty());
        assertTrue(cycleCurriculumGroup.getCurriculumLines().isEmpty());

        final Set<CurriculumLine> mandatoryCurriculumLines = mandatoryCurriculumGroup.getCurriculumLines();
        assertEquals(3, mandatoryCurriculumLines.size());

        final Set<CurriculumLine> optionalCurriculumLines = optionalCurriculumGroup.getCurriculumLines();
        assertEquals(1, optionalCurriculumLines.size());
    }

    @Test
    public void testCurriculumGroup_hasCurriculumLines_onlyGroupsWithDirectChildLines() {
        // mandatory/optional have direct child curriculumLines; root/cycle only have curriculumGroup children (no direct lines)
        assertTrue(mandatoryCurriculumGroup.hasCurriculumLines());
        assertTrue(optionalCurriculumGroup.hasCurriculumLines());
        assertFalse(rootCurriculumGroup.hasCurriculumLines());
        assertFalse(cycleCurriculumGroup.hasCurriculumLines());
    }

    @Test
    public void testCurriculumGroup_getCurriculumGroups() {
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
    public void testCurriculumGroup_getChildCurriculumGroups() {
        // Same as getChildCurriculumGroups but returns List instead of Set
        assertTrue(mandatoryCurriculumGroup.getChildCurriculumGroups().isEmpty());
        assertTrue(optionalCurriculumGroup.getChildCurriculumGroups().isEmpty());

        final List<CurriculumGroup> children = cycleCurriculumGroup.getChildCurriculumGroups();
        assertEquals(2, children.size());
        assertTrue(children.contains(mandatoryCurriculumGroup));
        assertTrue(children.contains(optionalCurriculumGroup));
    }

    @Test
    public void testCurriculumGroup_getCurriculumGroupsToEnrolmentProcess_filtersNoCourseGroup() {
        // Same as getCurriculumGroups but should EXCLUDE NoCourseGroup groups
        Set<CurriculumGroup> enrolGroups = rootCurriculumGroup.getCurriculumGroupsToEnrolmentProcess();
        assertEquals(1, enrolGroups.size());
        assertTrue(enrolGroups.contains(cycleCurriculumGroup));

        final NoCourseGroupCurriculumGroup noCourseGroup =
                NoCourseGroupCurriculumGroup.create(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR, rootCurriculumGroup);
        try {
            enrolGroups = rootCurriculumGroup.getCurriculumGroupsToEnrolmentProcess();
            assertEquals(1, enrolGroups.size());
            assertTrue(enrolGroups.contains(cycleCurriculumGroup));
            assertFalse(enrolGroups.contains(noCourseGroup));
        } finally {
            noCourseGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_isApproved_returnsTrueForApprovedCourse() {
        // C1 is approved; C2/C3 are enrolled-only; C4 has a dismissal
        assertTrue(mandatoryCurriculumGroup.isApproved(cc1, null));
        assertFalse(mandatoryCurriculumGroup.isApproved(cc2, null));
        assertFalse(mandatoryCurriculumGroup.isApproved(cc3, null));
        assertFalse(mandatoryCurriculumGroup.isApproved(cc4, null));

        // root.isApproved recursively delegates to children
        assertTrue(rootCurriculumGroup.isApproved(cc1, null));
        assertFalse(rootCurriculumGroup.isApproved(cc2, null));

        // firstSemester -> true; secondSemester: true because C1 (firstSemester) is beforeOrEquals
        assertTrue(mandatoryCurriculumGroup.isApproved(cc1, firstSemester));
        assertTrue(mandatoryCurriculumGroup.isApproved(cc1, secondSemester));
    }

    @Test
    public void testCurriculumGroup_isEnroledInExecutionPeriod() {
        // C1 -> first semester
        assertTrue(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc1, firstSemester));
        assertFalse(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc1, secondSemester));

        // C2 -> second semester
        assertTrue(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc2, secondSemester));
        assertFalse(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc2, firstSemester));

        // C4 has no enrolment
        assertFalse(mandatoryCurriculumGroup.isEnroledInExecutionPeriod(cc4, firstSemester));
    }

    @Test
    public void testCurriculumGroup_hasEnrolmentWithEnroledState_excludesApprovedAndMissing() {
        // C1 is approved (enrollment state is not ENROLLED) -> false; C2 is enroled in secondSemester -> true; C4 has no enrolment -> false
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc1, firstSemester));
        assertTrue(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc2, secondSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc2, firstSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentWithEnroledState(cc4, firstSemester));
    }

    @Test
    public void testCurriculumGroup_hasCurriculumModule() {
        // self
        assertTrue(mandatoryCurriculumGroup.hasCurriculumModule(mandatoryCurriculumGroup));
        // direct child
        assertTrue(mandatoryCurriculumGroup.hasCurriculumModule(enrolmentCc1));
        // not child
        assertFalse(optionalCurriculumGroup.hasCurriculumModule(enrolmentCc1));
        // all children
        assertTrue(rootCurriculumGroup.hasCurriculumModule(mandatoryCurriculumGroup));
        assertTrue(rootCurriculumGroup.hasCurriculumModule(enrolmentCc1));

        assertFalse(mandatoryCurriculumGroup.hasCurriculumModule(optionalCurriculumGroup));
        assertFalse(rootCurriculumGroup.hasCurriculumModule(null));
    }

    @Test
    public void testCurriculumGroup_hasDegreeModule() {
        // test curricularCourses
        assertTrue(mandatoryCurriculumGroup.hasDegreeModule(cc1));
        assertFalse(mandatoryCurriculumGroup.hasDegreeModule(cc4));
        assertTrue(optionalCurriculumGroup.hasDegreeModule(cc4));
        assertFalse(optionalCurriculumGroup.hasDegreeModule(cc2));

        // not enrolled in cc5, returns false
        assertFalse(optionalCurriculumGroup.hasDegreeModule(cc5));

        // all children
        assertTrue(rootCurriculumGroup.hasDegreeModule(cc1));
        assertTrue(rootCurriculumGroup.hasDegreeModule(cc4));

        // test courseGroups
        assertTrue(cycleCurriculumGroup.hasDegreeModule(mandatoryCurriculumGroup.getDegreeModule()));
        assertFalse(mandatoryCurriculumGroup.hasDegreeModule(cycleCurriculumGroup.getDegreeModule()));

        // test null case
        assertFalse(rootCurriculumGroup.hasDegreeModule(null));
    }

    @Test
    public void testCurriculumGroup_hasCourseGroup() {
        // cycle has mandatory + optional as children; mandatory does NOT have cycle (parent); optional does NOT have mandatory (sibling)
        assertTrue(cycleCurriculumGroup.hasCourseGroup(mandatoryCurriculumGroup.getDegreeModule()));
        assertTrue(cycleCurriculumGroup.hasCourseGroup(optionalCurriculumGroup.getDegreeModule()));
        assertFalse(mandatoryCurriculumGroup.hasCourseGroup(cycleCurriculumGroup.getDegreeModule()));
        assertFalse(optionalCurriculumGroup.hasCourseGroup(mandatoryCurriculumGroup.getDegreeModule()));

        // all children
        assertTrue(rootCurriculumGroup.hasCourseGroup(mandatoryCurriculumGroup.getDegreeModule()));

        // null
        assertFalse(rootCurriculumGroup.hasCourseGroup(null));
    }

    @Test
    public void testCurriculumGroup_hasAnyCurriculumModules_withPredicate() {
        // Predicate 1: always true/false predicates
        assertTrue(rootCurriculumGroup.hasAnyCurriculumModules(cm -> true));
        assertFalse(rootCurriculumGroup.hasAnyCurriculumModules(cm -> false));

        // Predicate 2: approved enrolment -> root has (C1), mandatory has (C1), optional doesn't
        Predicate<CurriculumModule> isApprovedEnrolment = cm -> cm.isEnrolment() && ((Enrolment) cm).isApproved();
        assertTrue(rootCurriculumGroup.hasAnyCurriculumModules(isApprovedEnrolment));
        assertTrue(mandatoryCurriculumGroup.hasAnyCurriculumModules(isApprovedEnrolment));
        assertFalse(optionalCurriculumGroup.hasAnyCurriculumModules(isApprovedEnrolment));

        // Predicate 3: isDismissal -> root has (C4), optional has (C4), mandatory doesn't
        Predicate<CurriculumModule> isDismissal = CurriculumModule::isDismissal;
        assertTrue(rootCurriculumGroup.hasAnyCurriculumModules(isDismissal));
        assertTrue(optionalCurriculumGroup.hasAnyCurriculumModules(isDismissal));
        assertFalse(mandatoryCurriculumGroup.hasAnyCurriculumModules(isDismissal));
    }

    @Test
    public void testCurriculumGroup_findEnrolmentFor() {
        // C1 (1Y1S) found in firstSemester in both mandatory and root; not found in secondSemester; C4 has no enrolment
        final Enrolment found = mandatoryCurriculumGroup.findEnrolmentFor(cc1, firstSemester);
        assertNotNull(found);
        assertEquals(cc1, found.getCurricularCourse());

        final Enrolment fromRoot = rootCurriculumGroup.findEnrolmentFor(cc1, firstSemester);
        assertNotNull(fromRoot);
        assertEquals(cc1, fromRoot.getCurricularCourse());

        assertNull(mandatoryCurriculumGroup.findEnrolmentFor(cc1, secondSemester));
        assertNull(optionalCurriculumGroup.findEnrolmentFor(cc4, firstSemester));
    }

    @Test
    public void testCurriculumGroup_getApprovedEnrolment() {
        Enrolment approved = mandatoryCurriculumGroup.getApprovedEnrolment(cc1);
        assertTrue(approved.isApproved(cc1));

        Enrolment fromRoot = rootCurriculumGroup.getApprovedEnrolment(cc1);
        assertTrue(fromRoot.isApproved(cc1));

        assertNull(mandatoryCurriculumGroup.getApprovedEnrolment(cc2));
        assertNull(optionalCurriculumGroup.getApprovedEnrolment(cc1));
    }

    @Test
    public void testCurriculumGroup_getDismissal() {
        Dismissal dismissalCc4 = studentCurricularPlan.getDismissal(cc4);

        Dismissal dismissal = optionalCurriculumGroup.getDismissal(cc4);
        assertNotNull(dismissal);
        assertTrue(dismissal.isDismissal());
        assertEquals(dismissal, dismissalCc4);

        Dismissal fromRoot = rootCurriculumGroup.getDismissal(cc4);
        assertNotNull(fromRoot);
        assertTrue(fromRoot.isDismissal());
        assertEquals(fromRoot, dismissalCc4);

        assertNull(mandatoryCurriculumGroup.getDismissal(cc4));
        assertNull(mandatoryCurriculumGroup.getDismissal(cc1));
        assertNull(rootCurriculumGroup.getDismissal(cc1));
    }

    @Test
    public void testCurriculumGroup_getApprovedCurriculumLine() {
        CurriculumLine line = mandatoryCurriculumGroup.getApprovedCurriculumLine(cc1);
        assertTrue(line.isApproved(cc1));

        CurriculumLine fromRoot = rootCurriculumGroup.getApprovedCurriculumLine(cc1);
        assertTrue(fromRoot.isApproved(cc1));

        CurriculumLine dismissalCurriculumLine = optionalCurriculumGroup.getApprovedCurriculumLine(cc4);
        assertTrue(dismissalCurriculumLine.isApproved(cc4));

        assertNull(mandatoryCurriculumGroup.getApprovedCurriculumLine(cc2));
        assertNull(mandatoryCurriculumGroup.getApprovedCurriculumLine(cc5));
    }

    @Test
    public void testCurriculumGroup_findCurriculumGroupFor() {
        assertEquals(mandatoryCurriculumGroup,
                rootCurriculumGroup.findCurriculumGroupFor(mandatoryCurriculumGroup.getDegreeModule()));
        assertEquals(optionalCurriculumGroup,
                rootCurriculumGroup.findCurriculumGroupFor(optionalCurriculumGroup.getDegreeModule()));
        assertEquals(optionalCurriculumGroup,
                optionalCurriculumGroup.findCurriculumGroupFor(optionalCurriculumGroup.getDegreeModule()));

        assertNull(optionalCurriculumGroup.findCurriculumGroupFor(cycleCurriculumGroup.getDegreeModule()));

        // A CourseGroup not present in the student's plan returns null
        CourseGroup courseGroup =
                new CourseGroup(mandatoryCurriculumGroup.getDegreeModule(), "New", "New", executionYear, null, null);
        assertNull(rootCurriculumGroup.findCurriculumGroupFor(courseGroup));
    }

    @Test
    public void testCurriculumGroup_getNoCourseGroupCurriculumGroup_findsByType() {
        NoCourseGroupCurriculumGroupType type = NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR;
        NoCourseGroupCurriculumGroup extra = NoCourseGroupCurriculumGroup.create(type, rootCurriculumGroup);
        try {
            NoCourseGroupCurriculumGroup found = rootCurriculumGroup.getNoCourseGroupCurriculumGroup(type);
            assertNotNull(found);
            assertEquals(type, found.getNoCourseGroupCurriculumGroupType());
        } finally {
            extra.delete();
        }
    }

    @Test
    public void testCurriculumGroup_hasConcluded() {
        CourseGroup mandatoryCourseGroup = mandatoryCurriculumGroup.getDegreeModule();

        assertTrue(mandatoryCurriculumGroup.hasConcluded(cc1, executionYear));
        assertFalse(mandatoryCurriculumGroup.hasConcluded(cc2, executionYear));

        assertTrue(optionalCurriculumGroup.hasConcluded(cc4, executionYear));
        assertFalse(optionalCurriculumGroup.hasConcluded(cc5, executionYear));
        assertFalse(optionalCurriculumGroup.hasConcluded(cc1, executionYear));

        assertTrue(rootCurriculumGroup.hasConcluded(cc1, executionYear));

        // test hasConcluded on courseGroup
        assertFalse(mandatoryCurriculumGroup.hasConcluded(mandatoryCourseGroup, executionYear));

        // Add a CreditsLimit(min=6) to mandatory CourseGroup; C1 = 6 ECTS approved -> concluded
        CreditsLimit creditsLimit = new CreditsLimit(mandatoryCourseGroup, mandatoryCourseGroup, firstSemester, null, 6.0d, 6.0d);
        try {
            assertTrue(mandatoryCurriculumGroup.hasConcluded(mandatoryCourseGroup, executionYear));
        } finally {
            creditsLimit.delete();
        }
    }

    @Test
    public void testCurriculumGroup_hasEnrolmentWithEnroledState_delegatesFromRoot() {
        assertTrue(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc2, secondSemester));
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc2, firstSemester));

        // C1 is approved (state != ENROLLED) -> false; C4 has a dismissal (not an enrolment) -> false;
        // C5 has no student interaction at all -> false
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc1, firstSemester));
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc4, firstSemester));
        assertFalse(rootCurriculumGroup.hasEnrolmentWithEnroledState(cc5, firstSemester));
    }

    @Test
    public void testCurriculumGroup_getAllCurriculumGroups() {
        // Recursively collects all groups in cycle's subtree: cycle + mandatory + optional = 3
        final Set<CurriculumGroup> allInCycle = cycleCurriculumGroup.getAllCurriculumGroups();
        assertEquals(3, allInCycle.size());
        assertTrue(allInCycle.contains(cycleCurriculumGroup));
        assertTrue(allInCycle.contains(mandatoryCurriculumGroup));
        assertTrue(allInCycle.contains(optionalCurriculumGroup));
    }

    @Test
    public void testCurriculumGroup_getAllCurriculumLines() {
        Set<CurriculumLine> rootLines = rootCurriculumGroup.getAllCurriculumLines();
        assertEquals(4, rootLines.size());
        assertTrue(rootLines.contains(enrolmentCc1));
        assertTrue(rootLines.contains(enrolmentCc2));
        assertTrue(rootLines.contains(enrolmentCc3));
        assertTrue(rootLines.contains(dismissalCc4));

        Set<CurriculumLine> mandatoryLines = mandatoryCurriculumGroup.getAllCurriculumLines();
        assertEquals(3, mandatoryLines.size());
        assertTrue(mandatoryLines.contains(enrolmentCc1));
        assertTrue(mandatoryLines.contains(enrolmentCc2));
        assertTrue(mandatoryLines.contains(enrolmentCc3));

        Set<CurriculumLine> optionalLines = optionalCurriculumGroup.getAllCurriculumLines();
        assertEquals(1, optionalLines.size());
        assertTrue(optionalLines.contains(dismissalCc4));
    }

    @Test
    public void testCurriculumGroup_getBranchCurriculumGroups() {
        assertFalse(optionalCurriculumGroup.isBranchCurriculumGroup());
        assertTrue(cycleCurriculumGroup.getBranchCurriculumGroups().isEmpty());

        // Temporarily set BranchType.MAJOR on the optionalCourseGroup; cycleCourseGroup then detects it as a branch
        optionalCurriculumGroup.getDegreeModule().setBranchType(BranchType.MAJOR);
        try {
            assertTrue(optionalCurriculumGroup.isBranchCurriculumGroup());
            assertTrue(cycleCurriculumGroup.getBranchCurriculumGroups().contains(optionalCurriculumGroup));
        } finally {
            optionalCurriculumGroup.getDegreeModule().setBranchType(null);
        }
    }

    @Test
    public void testCurriculumGroup_getNumberOfChildCurriculumGroupsWithCourseGroup() {
        // root: 1 (cycle); cycle: 2 (mandatory + optional); mandatory: 0 (leaf, no child groups)
        assertEquals(1, rootCurriculumGroup.getNumberOfChildCurriculumGroupsWithCourseGroup());
        assertEquals(2, cycleCurriculumGroup.getNumberOfChildCurriculumGroupsWithCourseGroup());
        assertEquals(0, mandatoryCurriculumGroup.getNumberOfChildCurriculumGroupsWithCourseGroup());
    }

    @Test
    public void testCurriculumGroup_getNumberOfApprovedChildCurriculumLines() {
        // cycle has no direct child lines (only groups); mandatory has 1 (C1 approved); optional has 1 (C4 dismissal isApproved=true)
        assertEquals(0, cycleCurriculumGroup.getNumberOfApprovedChildCurriculumLines());
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfApprovedChildCurriculumLines());
        assertEquals(1, optionalCurriculumGroup.getNumberOfApprovedChildCurriculumLines());
    }

    @Test
    public void testCurriculumGroup_getNumberOfAllApprovedCurriculumLines() {
        // Recursively counts approved lines: cycle -> C1 + C4; mandatory -> C1; optional -> C4
        assertEquals(2, cycleCurriculumGroup.getNumberOfAllApprovedCurriculumLines());
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfAllApprovedCurriculumLines());
        assertEquals(1, optionalCurriculumGroup.getNumberOfAllApprovedCurriculumLines());
    }

    @Test
    public void testCurriculumGroup_getNumberOfChildEnrolments_byInterval() {
        // Filters direct children by isEnroled()+isValid(interval):
        // C1: isAproved (not enroled); C2: 2nd semester, C3: 1st semester; C4: isDismissal (not enroled)
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfChildEnrolments(firstSemester));
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfChildEnrolments(secondSemester));
        assertEquals(0, optionalCurriculumGroup.getNumberOfChildEnrolments(firstSemester));
        assertEquals(0, rootCurriculumGroup.getNumberOfChildEnrolments(firstSemester));
    }

    @Test
    public void testCurriculumGroup_getNumberOfChildEnrolments_byExecutionYear() {
        assertEquals(2, mandatoryCurriculumGroup.getNumberOfChildEnrolments(executionYear));
        assertEquals(0, optionalCurriculumGroup.getNumberOfChildEnrolments(executionYear));
        assertEquals(0, rootCurriculumGroup.getNumberOfChildEnrolments(executionYear));
    }

    @Test
    public void testCurriculumGroup_getNumberOfAllApprovedEnrolments() {
        // Only C1 approved in firstSemester; none approved in secondSemester
        assertEquals(1, mandatoryCurriculumGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(0, mandatoryCurriculumGroup.getNumberOfAllApprovedEnrolments(secondSemester));
        assertEquals(0, optionalCurriculumGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(1, rootCurriculumGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(0, rootCurriculumGroup.getNumberOfAllApprovedEnrolments(secondSemester));
    }

    @Test
    public void testCurriculumGroup_getEnrolmentsBy_executionYear() {
        // C1–C3 all in executionYear -> 3 for root/mandatory (recursive), 0 for optional

        Set<Enrolment> rootEnrolments = rootCurriculumGroup.getEnrolmentsBy(executionYear);
        assertEquals(3, rootEnrolments.size());
        assertTrue(rootEnrolments.contains(enrolmentCc1));
        assertTrue(rootEnrolments.contains(enrolmentCc2));
        assertTrue(rootEnrolments.contains(enrolmentCc3));

        Set<Enrolment> mandatoryEnrolments = mandatoryCurriculumGroup.getEnrolmentsBy(executionYear);
        assertEquals(3, mandatoryEnrolments.size());
        assertTrue(mandatoryEnrolments.contains(enrolmentCc1));
        assertTrue(mandatoryEnrolments.contains(enrolmentCc2));
        assertTrue(mandatoryEnrolments.contains(enrolmentCc3));

        assertEquals(0, optionalCurriculumGroup.getEnrolmentsBy(executionYear).size());
    }

    @Test
    public void testCurriculumGroup_getEnrolmentsBy_executionInterval() {
        // C1+C3 in firstSemester -> 2; C2 in secondSemester -> 1
        Set<Enrolment> rootFirstSemester = rootCurriculumGroup.getEnrolmentsBy(firstSemester);
        assertEquals(2, rootFirstSemester.size());
        assertTrue(rootFirstSemester.contains(enrolmentCc1));
        assertTrue(rootFirstSemester.contains(enrolmentCc3));

        Set<Enrolment> mandatoryFirstSemester = mandatoryCurriculumGroup.getEnrolmentsBy(firstSemester);
        assertEquals(2, mandatoryFirstSemester.size());
        assertTrue(mandatoryFirstSemester.contains(enrolmentCc1));
        assertTrue(mandatoryFirstSemester.contains(enrolmentCc3));

        Set<Enrolment> mandatorySecondSemester = mandatoryCurriculumGroup.getEnrolmentsBy(secondSemester);
        assertEquals(1, mandatorySecondSemester.size());
        assertTrue(mandatorySecondSemester.contains(enrolmentCc2));
    }

    @Test
    public void testCurriculumGroup_getDegreeModulesToEvaluate() {
        Set<IDegreeModuleToEvaluate> mandatorySemester1 = mandatoryCurriculumGroup.getDegreeModulesToEvaluate(firstSemester);
        assertEquals(2, mandatorySemester1.size());
        assertTrue(mandatorySemester1.contains(new EnroledCurriculumModuleWrapper(mandatoryCurriculumGroup, firstSemester)));
        assertTrue(mandatorySemester1.contains(new EnroledEnrolmentWrapper(enrolmentCc3, firstSemester)));

        Set<IDegreeModuleToEvaluate> mandatorySemester2 = mandatoryCurriculumGroup.getDegreeModulesToEvaluate(secondSemester);
        assertEquals(2, mandatorySemester2.size());
        assertTrue(mandatorySemester2.contains(new EnroledCurriculumModuleWrapper(mandatoryCurriculumGroup, secondSemester)));
        assertTrue(mandatorySemester2.contains(new EnroledEnrolmentWrapper(enrolmentCc2, secondSemester)));

        Set<IDegreeModuleToEvaluate> optionalSemester1 = optionalCurriculumGroup.getDegreeModulesToEvaluate(firstSemester);
        assertEquals(2, optionalSemester1.size());
        assertTrue(optionalSemester1.contains(new EnroledCurriculumModuleWrapper(optionalCurriculumGroup, firstSemester)));
        assertTrue(optionalSemester1.contains(new DismissalCurriculumModuleWrapper(dismissalCc4, firstSemester)));

        Set<IDegreeModuleToEvaluate> cycleSemester1 = cycleCurriculumGroup.getDegreeModulesToEvaluate(firstSemester);
        assertEquals(5, cycleSemester1.size());
        assertTrue(cycleSemester1.contains(new EnroledCurriculumModuleWrapper(cycleCurriculumGroup, firstSemester)));
        assertTrue(cycleSemester1.contains(new EnroledCurriculumModuleWrapper(mandatoryCurriculumGroup, firstSemester)));
        assertTrue(cycleSemester1.contains(new EnroledEnrolmentWrapper(enrolmentCc3, firstSemester)));
        assertTrue(cycleSemester1.contains(new EnroledCurriculumModuleWrapper(optionalCurriculumGroup, firstSemester)));
        assertTrue(cycleSemester1.contains(new DismissalCurriculumModuleWrapper(dismissalCc4, firstSemester)));
    }

    @Test
    public void testCurriculumGroup_getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups() {
        // Create an EXTRA_CURRICULAR NoCourseGroup under root to test exclusion
        NoCourseGroupCurriculumGroupType type = NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR;
        NoCourseGroupCurriculumGroup extraGroup = NoCourseGroupCurriculumGroup.create(type, rootCurriculumGroup);

        try {
            // Root excludes itself (RootCurriculumGroup overrides) and NoCourseGroup -> 3: cycle, mandatory, optional
            final Set<CurriculumGroup> rootGroups =
                    rootCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups();
            assertEquals(3, rootGroups.size());
            assertTrue(rootGroups.contains(cycleCurriculumGroup));
            assertTrue(rootGroups.contains(mandatoryCurriculumGroup));
            assertTrue(rootGroups.contains(optionalCurriculumGroup));
            assertFalse(rootGroups.contains(extraGroup));

            // Cycle includes self + children, excluding NoCourseGroup -> 3: cycle, mandatory, optional
            final Set<CurriculumGroup> cycleGroups =
                    cycleCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups();
            assertEquals(3, cycleGroups.size());
            assertTrue(cycleGroups.contains(cycleCurriculumGroup));
            assertTrue(cycleGroups.contains(mandatoryCurriculumGroup));
            assertTrue(cycleGroups.contains(optionalCurriculumGroup));
            assertFalse(cycleGroups.contains(extraGroup));

            // Leaf groups (mandatory/optional) have no group children -> just themselves
            Set<CurriculumGroup> mandatoryGroups =
                    mandatoryCurriculumGroup.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups();
            assertEquals(1, mandatoryGroups.size());
            assertTrue(mandatoryGroups.contains(mandatoryCurriculumGroup));

            Set<CurriculumGroup> optionalGroups =
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
    public void testCurriculumGroup_getNoCourseGroupCurriculumGroups() {
        // getNoCourseGroupCurriculumGroups on a CurriculumGroup collects from children only;
        // on NoCourseGroupCurriculumGroup it includes itself + children
        NoCourseGroupCurriculumGroupType type = NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR;
        NoCourseGroupCurriculumGroup extraGroup = NoCourseGroupCurriculumGroup.create(type, rootCurriculumGroup);

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
    public void testCurriculumGroup_getEnrolments() {
        // mandatory has 3 enrolments (C1–C3); optional 0 (C4 is a dismissal, C5 never taken); root aggregates all 3 recursively
        List<Enrolment> mandatoryEnrolments = mandatoryCurriculumGroup.getEnrolments();
        assertEquals(3, mandatoryEnrolments.size());
        assertTrue(mandatoryEnrolments.contains(enrolmentCc1));
        assertTrue(mandatoryEnrolments.contains(enrolmentCc2));
        assertTrue(mandatoryEnrolments.contains(enrolmentCc3));

        assertEquals(0, optionalCurriculumGroup.getEnrolments().size());

        List<Enrolment> rootEnrolments = rootCurriculumGroup.getEnrolments();
        assertEquals(3, rootEnrolments.size());
        assertTrue(rootEnrolments.contains(enrolmentCc1));
        assertTrue(rootEnrolments.contains(enrolmentCc2));
        assertTrue(rootEnrolments.contains(enrolmentCc3));
    }

    @Test
    public void testCurriculumGroup_getEnrolmentsSet() {
        Set<Enrolment> mandatoryEnrolments = mandatoryCurriculumGroup.getEnrolmentsSet();
        assertEquals(3, mandatoryEnrolments.size());
        assertTrue(mandatoryEnrolments.contains(enrolmentCc1));
        assertTrue(mandatoryEnrolments.contains(enrolmentCc2));
        assertTrue(mandatoryEnrolments.contains(enrolmentCc3));

        assertEquals(0, optionalCurriculumGroup.getEnrolmentsSet().size());

        Set<Enrolment> rootEnrolments = rootCurriculumGroup.getEnrolmentsSet();
        assertEquals(3, rootEnrolments.size());
        assertTrue(rootEnrolments.contains(enrolmentCc1));
        assertTrue(rootEnrolments.contains(enrolmentCc2));
        assertTrue(rootEnrolments.contains(enrolmentCc3));
    }

    @Test
    public void testCurriculumGroup_getCourseGroupContextsToEnrol() {
        // Without an unenroled CourseGroup child, cycle returns empty; create one and verify it appears
        assertTrue(cycleCurriculumGroup.getCourseGroupContextsToEnrol(firstSemester).isEmpty());

        CourseGroup cycleCourseGroup = cycleCurriculumGroup.getDegreeModule();
        CourseGroup newGroup = new CourseGroup(cycleCourseGroup, "TEST_ENROL", "Test Enrol Group", executionYear, null, null);

        try {
            List<Context> contexts = cycleCurriculumGroup.getCourseGroupContextsToEnrol(firstSemester);
            assertEquals(1, contexts.size());
            assertTrue(contexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == newGroup));
        } finally {
            newGroup.delete();
        }
    }

    @Test
    public void testCurriculumGroup_getCurricularCoursesToDismissal() {
        // cycle/root have no direct CurricularCourse children -> empty
        assertTrue(cycleCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).isEmpty());
        assertTrue(rootCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).isEmpty());

        // mandatory: C1 approved -> filtered out;
        assertEquals(2, mandatoryCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).size());
        assertTrue(mandatoryCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).contains(cc2));
        assertTrue(mandatoryCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).contains(cc3));

        // optional: C4 (dismissal) approved -> filtered out;
        assertEquals(1, optionalCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).size());
        assertTrue(optionalCurriculumGroup.getCurricularCoursesToDismissal(firstSemester).contains(cc5));
    }

    @Test
    public void testCurriculumGroup_isEnroledInSpecialSeason() {
        // Create a special-season EnrolmentEvaluation on C1 (enroled in mandatory);
        // groups containing C1 detect it recursively; optional has no C1 -> false
        EvaluationSeason specialSeason = EvaluationSeason.findByCode(EvaluationSeasonTest.SPECIAL_SEASON_CODE).orElseThrow();
        EnrolmentEvaluation evaluation = new EnrolmentEvaluation(enrolmentCc1, specialSeason);

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

    /* Tests for private utility methods commented out because methods are private

    @Test
    public void testCurriculumGroup_hasEnrolmentForInterval() {
        assertTrue(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc1, firstSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc1, secondSemester));

        assertTrue(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc2, secondSemester));

        // C4 has a dismissal (not an enrolment) -> false; C5 has no enrolment -> false
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc4, firstSemester));
        assertFalse(mandatoryCurriculumGroup.hasEnrolmentForInterval(cc5, firstSemester));
    }

    @Test
    public void testCurriculumGroup_canCreateGroupOrChilds() {
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
