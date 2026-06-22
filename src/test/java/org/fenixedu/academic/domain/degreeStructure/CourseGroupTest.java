package org.fenixedu.academic.domain.degreeStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CourseGroupTest {

    private static DegreeCurricularPlan dcp;
    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static CourseGroup rootCourseGroup;
    private static CourseGroup cycleCourseGroup;
    private static CourseGroup mandatoryCourseGroup;
    private static CourseGroup optionalCourseGroup;
    private static CurricularCourse cc1, cc2, cc3, cc4, cc5, cc6;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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
         *   Root -> Cycle -> Mandatory (C1: 1Y1S, C2: 1Y2S, C3: 2Y1S, C6: 1Y3S)
         *                -> Optional  (C4: 2Y1S, C5: 2Y1S)
         *
         * C6 is in a 3rd-semester period (1Y3S). The execution year has only 2 semesters
         * (child orders 1 and 2), so C6's context:
         *   - isOpen(ExecutionYear)         -> true  (year-range matches, no semester check)
         *   - isValidForExecutionAggregation -> false (no semester with child order 3 in the year)
         */

        ConclusionRulesTestUtil.initData();
        executionYear = ExecutionYear.findCurrent(null);
        firstSemester = executionYear.getFirstExecutionPeriod();
        secondSemester = executionYear.getLastExecutionPeriod();

        dcp = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        cc1 = dcp.getCurricularCourseByCode("C1");
        cc2 = dcp.getCurricularCourseByCode("C2");
        cc3 = dcp.getCurricularCourseByCode("C3");
        cc4 = dcp.getCurricularCourseByCode("C4");
        cc5 = dcp.getCurricularCourseByCode("C5");

        rootCourseGroup = dcp.getRoot();
        cycleCourseGroup = ConclusionRulesTestUtil.getChildGroup(rootCourseGroup, ConclusionRulesTestUtil.CYCLE_GROUP);
        mandatoryCourseGroup = ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.MANDATORY_GROUP);
        optionalCourseGroup = ConclusionRulesTestUtil.getChildGroup(cycleCourseGroup, ConclusionRulesTestUtil.OPTIONAL_GROUP);

        // Add a 3rd semester period (1Y3S) and C6 to demonstrate difference between isOpen(ExecutionYear) and isValid(ExecutionYear)
        CurricularPeriod period1Y1S = dcp.getCurricularPeriodFor(1, 1, AcademicPeriod.SEMESTER);
        CurricularPeriod period1Y3S = new CurricularPeriod(AcademicPeriod.SEMESTER, 3, period1Y1S.getParent());
        cc6 = ConclusionRulesTestUtil.createCurricularCourse("C6", "Course 6", new BigDecimal(6), period1Y3S, firstSemester,
                mandatoryCourseGroup);
    }

    @Test
    public void testCourseGroup_getValidChildContexts_filtersByClassAndInterval() {
        final List<Context> allInCycle = cycleCourseGroup.getValidChildContexts(null, firstSemester);
        final List<Context> ccInCycle = cycleCourseGroup.getValidChildContexts(CurricularCourse.class, firstSemester);
        final List<Context> cgInCycle = cycleCourseGroup.getValidChildContexts(CourseGroup.class, firstSemester);

        // cycle has 2 CourseGroup children (mandatory + optional) and 0 direct CurricularCourse children
        assertEquals(2, cgInCycle.size());
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == mandatoryCourseGroup));
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == optionalCourseGroup));
        assertTrue(ccInCycle.isEmpty());
        assertEquals(2, allInCycle.size());

        // mandatory has 4 CC children
        // C1 (1Y1S) and C3 (2Y1S) match firstSemester, C2 (1Y2S) and C6 (1Y3S) don't -> 2
        final List<Context> mandatoryCCs = mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, firstSemester);
        assertEquals(2, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCourseGroup.getValidChildContexts(CourseGroup.class, firstSemester).isEmpty());

        // secondSemester matches only C2 (1Y2S)
        final List<Context> mandatoryCCsSecond =
                mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, secondSemester);
        assertEquals(1, mandatoryCCsSecond.size());
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));

        // null interval returns all 4 (C6 included)
        final List<Context> mandatoryCCsNull = mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, null);
        assertEquals(4, mandatoryCCsNull.size());
        assertTrue(mandatoryCCsNull.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));

        // previous execution interval returns empty
        final ExecutionInterval previousInterval = (ExecutionInterval) executionYear.getPrevious();
        assertTrue(cycleCourseGroup.getValidChildContexts(null, previousInterval).isEmpty());
        assertTrue(mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, previousInterval).isEmpty());
    }

    @Test
    public void testCourseGroup_getValidChildContextsForExecutionAggregation_filtersByClassAndExecutionYear() {
        final List<Context> allInCycle = cycleCourseGroup.getValidChildContextsForExecutionAggregation(null, null);
        final List<Context> ccInCycle =
                cycleCourseGroup.getValidChildContextsForExecutionAggregation(CurricularCourse.class, null);
        final List<Context> cgInCycle = cycleCourseGroup.getValidChildContextsForExecutionAggregation(CourseGroup.class, null);

        // cycle has 2 CourseGroup children (mandatory + optional) and 0 direct CurricularCourse children
        assertEquals(2, cgInCycle.size());
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == mandatoryCourseGroup));
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == optionalCourseGroup));
        assertTrue(ccInCycle.isEmpty());
        assertEquals(2, allInCycle.size());

        // mandatory has 4 CurricularCourse children (C1–C3, C6) and 0 CourseGroup children
        // null executionYear returns all 4 (C6 included)
        final List<Context> mandatoryCCs =
                mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(CurricularCourse.class, null);
        assertEquals(4, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(CourseGroup.class, null).isEmpty());

        // Filtering by current executionYear: C6 (1Y3S) is NOT valid because there is no 3rd-semester
        assertEquals(3, mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(null, executionYear).size());
        assertEquals(2, cycleCourseGroup.getValidChildContextsForExecutionAggregation(null, executionYear).size());

        // Filtering by previous executionYear returns empty set
        final ExecutionYear previousYear = (ExecutionYear) executionYear.getPrevious();
        assertTrue(cycleCourseGroup.getValidChildContextsForExecutionAggregation(null, previousYear).isEmpty());
        assertTrue(mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(null, previousYear).isEmpty());
    }

    @Test
    public void testCourseGroup_getOpenChildContexts_filtersByClassAndInterval() {
        // getOpenChildContexts uses context.isOpen() which only checks year-range, not semester order.
        // All contexts are open for the current year regardless of semester.
        final List<Context> allInCycle = cycleCourseGroup.getOpenChildContexts(null, firstSemester);
        final List<Context> ccInCycle = cycleCourseGroup.getOpenChildContexts(CurricularCourse.class, firstSemester);
        final List<Context> cgInCycle = cycleCourseGroup.getOpenChildContexts(CourseGroup.class, firstSemester);

        // cycle has 2 CourseGroup children (mandatory + optional) and 0 direct CurricularCourse children
        assertEquals(2, cgInCycle.size());
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == mandatoryCourseGroup));
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == optionalCourseGroup));
        assertTrue(ccInCycle.isEmpty());
        assertEquals(2, allInCycle.size());

        // mandatory has 4 CC children, all open in firstSemester (isOpen ignores semester order) and 0 CourseGroup children.
        // Note: isValid returns only 2 for firstSemester (C1, C3) but isOpen returns all 4
        final List<Context> mandatoryCCs = mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, firstSemester);
        assertEquals(4, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCourseGroup.getOpenChildContexts(CourseGroup.class, firstSemester).isEmpty());

        // second semester returns same matches because isOpen ignores semester order
        final List<Context> mandatoryCCsSecond =
                mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, secondSemester);
        assertEquals(4, mandatoryCCsSecond.size());
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCourseGroup.getOpenChildContexts(CourseGroup.class, secondSemester).isEmpty());

        // null interval also returns all 4
        assertEquals(4, mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, null).size());

        // previous execution interval returns empty
        final ExecutionInterval previousInterval = (ExecutionInterval) executionYear.getPrevious();
        assertTrue(cycleCourseGroup.getOpenChildContexts(null, previousInterval).isEmpty());
        assertTrue(mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, previousInterval).isEmpty());
    }

    @Test
    public void testCourseGroup_getOpenChildContextsForExecutionAggregation_filtersByClassAndYear() {
        final List<Context> allInCycle = cycleCourseGroup.getOpenChildContextsForExecutionAggregation(null, executionYear);
        final List<Context> ccInCycle =
                cycleCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, executionYear);
        final List<Context> cgInCycle =
                cycleCourseGroup.getOpenChildContextsForExecutionAggregation(CourseGroup.class, executionYear);

        // cycle has 2 CourseGroup children (mandatory + optional) and 0 direct CurricularCourse children
        assertEquals(2, cgInCycle.size());
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == mandatoryCourseGroup));
        assertTrue(cgInCycle.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == optionalCourseGroup));
        assertTrue(ccInCycle.isEmpty());
        assertEquals(2, allInCycle.size());

        // mandatory has 4 CurricularCourse children (C1–C3, C6) and 0 CourseGroup children.
        // isOpen(ExecutionYear) returns true for C6 (year-range matches, no semester check),
        // isValid(ExecutionYear) returns only 3 (C6 filtered out)
        final List<Context> mandatoryCCs =
                mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, executionYear);
        assertEquals(4, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CourseGroup.class, executionYear).isEmpty());

        // null year also returns all 4
        assertEquals(4, mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, null).size());

        // previous execution year returns empty
        final ExecutionYear previousYear = (ExecutionYear) executionYear.getPrevious();
        assertTrue(cycleCourseGroup.getOpenChildContextsForExecutionAggregation(null, previousYear).isEmpty());
        assertTrue(
                mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, previousYear).isEmpty());
    }

    @Test
    public void testCourseGroup_getContextsWithCurricularCourseByCurricularPeriod_filtersByPeriodAndInterval() {
        final CurricularPeriod period1Y1S = dcp.getCurricularPeriodFor(1, 1, AcademicPeriod.SEMESTER);
        final CurricularPeriod period1Y2S = dcp.getCurricularPeriodFor(1, 2, AcademicPeriod.SEMESTER);
        final CurricularPeriod period2Y1S = dcp.getCurricularPeriodFor(2, 1, AcademicPeriod.SEMESTER);
        final CurricularPeriod period1Y3S = dcp.getCurricularPeriodFor(1, 3, AcademicPeriod.SEMESTER);

        // C1 (1Y1S) matches 1Y1S period in firstSemester
        assertEquals(cc1,
                mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y1S, firstSemester).iterator()
                        .next().getChildDegreeModule());

        // C2 (1Y2S) matches 1Y2S period in secondSemester
        assertEquals(cc2,
                mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y2S, secondSemester).iterator()
                        .next().getChildDegreeModule());

        // C3 (2Y1S) matches 2Y1S period in firstSemester
        assertEquals(cc3,
                mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period2Y1S, firstSemester).iterator()
                        .next().getChildDegreeModule());

        // test wrong interval
        assertTrue(mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y2S, firstSemester).isEmpty());
        assertTrue(mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y3S, firstSemester).isEmpty());
    }

    @Test
    public void testCourseGroup_getOpenChildDegreeModulesByExecutionPeriod_allOpenInCurrentInterval() {
        assertEquals(4, mandatoryCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(firstSemester).size());
        assertEquals(4, mandatoryCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(secondSemester).size());

        final Set<DegreeModule> cycleOpen = cycleCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(firstSemester);
        assertEquals(2, cycleOpen.size());
        assertTrue(cycleOpen.contains(mandatoryCourseGroup));
        assertTrue(cycleOpen.contains(optionalCourseGroup));

        final Set<DegreeModule> optionalOpen = optionalCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(firstSemester);
        assertEquals(2, optionalOpen.size());
        assertTrue(optionalOpen.contains(cc4));
        assertTrue(optionalOpen.contains(cc5));

        // previous interval returns empty for all groups
        final ExecutionInterval previousInterval = (ExecutionInterval) executionYear.getPrevious();
        assertTrue(mandatoryCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(previousInterval).isEmpty());
        assertTrue(cycleCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(previousInterval).isEmpty());
        assertTrue(optionalCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(previousInterval).isEmpty());
    }

    /* Tests for private utility methods commented out because methods are private

    @Test
    public void testCourseGroup_getDegreeModulesByExecutionInterval_filtersByInterval() {
        // mandatory: C1 (1Y1S) + C3 (2Y1S) match firstSemester; C2 (1Y2S) matches secondSemester
        // C6 (1Y3S) does not match either (no 3rd semester in the year)
        final Collection<DegreeModule> firstSemesterModules =
                mandatoryCourseGroup.getDegreeModulesByExecutionInterval(firstSemester);
        assertEquals(2, firstSemesterModules.size());
        assertTrue(firstSemesterModules.contains(cc1));
        assertTrue(firstSemesterModules.contains(cc3));

        final Collection<DegreeModule> secondSemesterModules =
                mandatoryCourseGroup.getDegreeModulesByExecutionInterval(secondSemester);
        assertEquals(1, secondSemesterModules.size());
        assertTrue(secondSemesterModules.contains(cc2));

        // cycle has 2 CourseGroup children, always valid when open
        Collection<DegreeModule> cycleModules =
                cycleCourseGroup.getDegreeModulesByExecutionInterval(firstSemester);
        assertEquals(2, cycleModules.size());
        assertTrue(cycleModules.contains(mandatoryCourseGroup));
        assertTrue(cycleModules.contains(optionalCourseGroup));
        assertEquals(2, cycleCourseGroup.getDegreeModulesByExecutionInterval(secondSemester).size());

        // previous interval -> empty
        final ExecutionInterval previousInterval = executionYear.getPrevious();
        assertTrue(mandatoryCourseGroup.getDegreeModulesByExecutionInterval(previousInterval).isEmpty());
        assertTrue(cycleCourseGroup.getDegreeModulesByExecutionInterval(previousInterval).isEmpty());
    }
    */
}