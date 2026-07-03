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
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
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
        Enrolment enrolmentCc1 = studentCurricularPlan.getEnrolments(cc1).iterator().next();

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
}
