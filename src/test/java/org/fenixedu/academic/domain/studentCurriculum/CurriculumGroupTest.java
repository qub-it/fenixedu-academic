package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
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
        enrolmentCc1 = studentCurricularPlan.getEnrolments(cc1).iterator().next();
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
}
