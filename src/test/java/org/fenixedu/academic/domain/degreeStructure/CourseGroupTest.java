package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.degreeStructure.CompetenceCourseTypeTest.initCompetenceCourseType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
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
    private static CurricularCourse cc1, cc2, cc3, cc4, cc5, cc6, cc7, cc8;
    private static CurricularPeriod period1Y1S, period1Y3S;

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
         *   Root -> Cycle -> Mandatory (C1: 1Y1S, C2: 1Y2S, C3: 2Y1S, C6: 1Y3S, C7: 1Y1S, C8: 1Y)
         *                -> Optional  (C4: 2Y1S, C5: 2Y1S)
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
        period1Y1S = dcp.getCurricularPeriodFor(1, 1, AcademicPeriod.SEMESTER);
        period1Y3S = new CurricularPeriod(AcademicPeriod.SEMESTER, 3, period1Y1S.getParent());
        cc6 = ConclusionRulesTestUtil.createCurricularCourse("C6", "Course 6", new BigDecimal(6), period1Y3S, firstSemester,
                mandatoryCourseGroup);

        // C7: 1Y1S with context begin=firstSemester, end=firstSemester.
        // isOpen checks at year granularity, so it returns true for any semester of current year,
        // but false for future/past years. isValid checks semester order so only firstSemester matches
        cc7 = ConclusionRulesTestUtil.createCurricularCourse("C7", "Course 7", new BigDecimal(6), period1Y1S, firstSemester,
                mandatoryCourseGroup);
        cc7.getParentContextsSet().iterator().next().setEndExecutionInterval(firstSemester);

        // C8: annual course. The context uses the first leaf period (1Y1S) because execution
        // courses are connected to leaf execution intervals, not execution years.
        CompetenceCourse cc8competence =
                new CompetenceCourse("C8", new LocalizedString(Locale.getDefault(), "Course 8"), null, new BigDecimal(12),
                        Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow(),
                        AcademicPeriod.YEAR, initCompetenceCourseType(), CompetenceCourseLevelType.UNKNOWN().orElse(null),
                        firstSemester, new GradeScale());

        cc8 = new CurricularCourse(12d, cc8competence, mandatoryCourseGroup, period1Y1S, firstSemester, null);
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

        // mandatory has 6 CC children
        // C8 (annual) is valid for all semesters; C2 (1Y2S) and C6 (1Y3S) don't match firstSemester
        final List<Context> mandatoryCCs = mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, firstSemester);
        assertEquals(4, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc7));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));
        assertTrue(mandatoryCourseGroup.getValidChildContexts(CourseGroup.class, firstSemester).isEmpty());

        // secondSemester matches C2 (1Y2S) and C8 (annual).
        final List<Context> mandatoryCCsSecond =
                mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, secondSemester);
        assertEquals(2, mandatoryCCsSecond.size());
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));

        // null interval returns all 6
        final List<Context> mandatoryCCsNull = mandatoryCourseGroup.getValidChildContexts(CurricularCourse.class, null);
        assertEquals(6, mandatoryCCsNull.size());

        // previous execution interval returns empty
        final ExecutionInterval previousInterval = executionYear.getPrevious();
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

        // mandatory has 6 CurricularCourse children (C1–C3, C6–C8) and 0 CourseGroup children
        // null executionYear returns all 6 (C6, C7, C8 included)
        final List<Context> mandatoryCCs =
                mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(CurricularCourse.class, null);
        assertEquals(6, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc7));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));
        assertTrue(mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(CourseGroup.class, null).isEmpty());

        // Filtering by current executionYear: C6 (1Y3S) is NOT valid because there is no 3rd-semester;
        // C7 (1Y1S end=1S), C8 (annual) ARE valid
        assertEquals(5, mandatoryCourseGroup.getValidChildContextsForExecutionAggregation(null, executionYear).size());
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

        // mandatory has 6 CC children, all open in firstSemester (isOpen ignores semester order) and 0 CourseGroup children.
        // Note: isValid returns only 4 for firstSemester (C1, C3, C7, C8) but isOpen returns all 6
        final List<Context> mandatoryCCs = mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, firstSemester);
        assertEquals(6, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc7));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));
        assertTrue(mandatoryCourseGroup.getOpenChildContexts(CourseGroup.class, firstSemester).isEmpty());

        // second semester returns same matches because isOpen ignores semester order
        final List<Context> mandatoryCCsSecond =
                mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, secondSemester);
        assertEquals(6, mandatoryCCsSecond.size());
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc7));
        assertTrue(mandatoryCCsSecond.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));
        assertTrue(mandatoryCourseGroup.getOpenChildContexts(CourseGroup.class, secondSemester).isEmpty());

        // null interval also returns all 6
        assertEquals(6, mandatoryCourseGroup.getOpenChildContexts(CurricularCourse.class, null).size());

        // previous execution interval returns empty
        final ExecutionInterval previousInterval = executionYear.getPrevious();
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

        // mandatory has 6 CurricularCourse children (C1–C3, C6–C8) and 0 CourseGroup children.
        // isOpen(ExecutionYear) returns true for all 6 (year-range matches, no semester check),
        // isValid(ExecutionYear) returns only 5 (C6 filtered out)
        final List<Context> mandatoryCCs =
                mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, executionYear);
        assertEquals(6, mandatoryCCs.size());
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc6));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc7));
        assertTrue(mandatoryCCs.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));
        assertTrue(mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CourseGroup.class, executionYear).isEmpty());

        // null year also returns all 6
        assertEquals(6, mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, null).size());

        // previous execution year returns empty
        final ExecutionYear previousYear = (ExecutionYear) executionYear.getPrevious();
        assertTrue(cycleCourseGroup.getOpenChildContextsForExecutionAggregation(null, previousYear).isEmpty());
        assertTrue(
                mandatoryCourseGroup.getOpenChildContextsForExecutionAggregation(CurricularCourse.class, previousYear).isEmpty());
    }

    @Test
    public void testCourseGroup_getContextsWithCurricularCourseByCurricularPeriod_filtersByPeriodAndInterval() {
        final CurricularPeriod period1Y2S = dcp.getCurricularPeriodFor(1, 2, AcademicPeriod.SEMESTER);
        final CurricularPeriod period2Y1S = dcp.getCurricularPeriodFor(2, 1, AcademicPeriod.SEMESTER);

        // C1, C7 and C8 match 1Y1S period in firstSemester
        Collection<Context> period1Y1SContexts =
                mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y1S, firstSemester);
        assertEquals(3, period1Y1SContexts.size());
        assertTrue(period1Y1SContexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc1));
        assertTrue(period1Y1SContexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc7));
        assertTrue(period1Y1SContexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc8));

        // C2 (1Y2S)
        Collection<Context> period1Y2SContexts =
                mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y2S, secondSemester);
        assertEquals(1, period1Y2SContexts.size());
        assertTrue(period1Y2SContexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc2));

        // C3 (2Y1S)
        Collection<Context> period2Y1SContexts =
                mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period2Y1S, firstSemester);
        assertEquals(1, period2Y1SContexts.size());
        assertTrue(period2Y1SContexts.stream().anyMatch(ctx -> ctx.getChildDegreeModule() == cc3));

        // test wrong interval
        assertTrue(mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y2S, firstSemester).isEmpty());
        assertTrue(mandatoryCourseGroup.getContextsWithCurricularCourseByCurricularPeriod(period1Y3S, firstSemester).isEmpty());
    }

    @Test
    public void testCourseGroup_getOpenChildDegreeModulesByExecutionPeriod_allOpenInCurrentInterval() {
        assertEquals(6, mandatoryCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(firstSemester).size());
        assertEquals(6, mandatoryCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(secondSemester).size());

        final Set<DegreeModule> cycleOpen = cycleCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(firstSemester);
        assertEquals(2, cycleOpen.size());
        assertTrue(cycleOpen.contains(mandatoryCourseGroup));
        assertTrue(cycleOpen.contains(optionalCourseGroup));

        final Set<DegreeModule> optionalOpen = optionalCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(firstSemester);
        assertEquals(2, optionalOpen.size());
        assertTrue(optionalOpen.contains(cc4));
        assertTrue(optionalOpen.contains(cc5));

        // previous interval returns empty for all groups
        final ExecutionInterval previousInterval = executionYear.getPrevious();
        assertTrue(mandatoryCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(previousInterval).isEmpty());
        assertTrue(cycleCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(previousInterval).isEmpty());
        assertTrue(optionalCourseGroup.getOpenChildDegreeModulesByExecutionPeriod(previousInterval).isEmpty());
    }

    @Test
    public void testCourseGroup_getChildDegreeModulesValidOn_filtersByInterval() {
        // mandatory: C1 (1Y1S) + C3 (2Y1S) + C7 (1Y1S) + C8 (1Y) match firstSemester;
        // C2 (1Y2S) matches secondSemester; C6 (1Y3S) does not match either (no 3rd semester in the year)
        final Set<DegreeModule> firstSemesterModules = mandatoryCourseGroup.getChildDegreeModulesValidOn(firstSemester);
        assertEquals(4, firstSemesterModules.size());
        assertTrue(firstSemesterModules.contains(cc1));
        assertTrue(firstSemesterModules.contains(cc3));
        assertTrue(firstSemesterModules.contains(cc7));
        assertTrue(firstSemesterModules.contains(cc8));

        final Set<DegreeModule> secondSemesterModules = mandatoryCourseGroup.getChildDegreeModulesValidOn(secondSemester);
        assertEquals(2, secondSemesterModules.size());
        assertTrue(secondSemesterModules.contains(cc2));
        assertTrue(secondSemesterModules.contains(cc8));
    }

    @Test
    public void testCourseGroup_getChildDegreeModulesValidOnExecutionAggregation_filtersByYear() {
        // C6 (1Y3S) is NOT valid for executionYear aggregation (no 3rd-semester period in the year),
        final Set<DegreeModule> mandatoryValid =
                mandatoryCourseGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear);
        assertEquals(5, mandatoryValid.size());
        assertTrue(mandatoryValid.contains(cc1));
        assertTrue(mandatoryValid.contains(cc2));
        assertTrue(mandatoryValid.contains(cc3));
        assertTrue(mandatoryValid.contains(cc7));
        assertTrue(mandatoryValid.contains(cc8));

        final Set<DegreeModule> rootValid = rootCourseGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear);
        assertEquals(1, rootValid.size());
        assertTrue(rootValid.contains(cycleCourseGroup));

        // cycle: mandatory + optional
        final Set<DegreeModule> cycleValid = cycleCourseGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear);
        assertEquals(2, cycleValid.size());
        assertTrue(cycleValid.contains(mandatoryCourseGroup));
        assertTrue(cycleValid.contains(optionalCourseGroup));
    }

    @Test
    public void testCourseGroup_getChildCurricularCoursesValidOn_returnsOnlyCurricularCourses() {
        // mandatory: C1 + C3 + C7 + C8 match firstSemester, C2+C8 matches secondSemester, C6 (1Y3S) matches neither
        final Set<CurricularCourse> firstSemesterModules = mandatoryCourseGroup.getChildCurricularCoursesValidOn(firstSemester);
        assertEquals(4, firstSemesterModules.size());
        assertTrue(firstSemesterModules.contains(cc1));
        assertTrue(firstSemesterModules.contains(cc3));
        assertTrue(firstSemesterModules.contains(cc7));
        assertTrue(firstSemesterModules.contains(cc8));

        // secondSemester: C2 + C8 (annual) match
        final Set<CurricularCourse> secondSemesterModules = mandatoryCourseGroup.getChildCurricularCoursesValidOn(secondSemester);
        assertEquals(2, secondSemesterModules.size());
        assertTrue(secondSemesterModules.contains(cc2));
        assertTrue(secondSemesterModules.contains(cc8));

        // cycle has no direct CurricularCourse children -> empty
        assertTrue(cycleCourseGroup.getChildCurricularCoursesValidOn(firstSemester).isEmpty());
    }

    @Test
    public void testCourseGroup_getChildDegreeModules_returnsDirectChildren() {
        // getChildDegreeModules returns only direct children (not recursive)
        final Set<DegreeModule> rootChildren = rootCourseGroup.getChildDegreeModules();
        assertEquals(1, rootChildren.size());
        assertTrue(rootChildren.contains(cycleCourseGroup));

        // cycle's direct children: mandatory + optional
        final Set<DegreeModule> cycleChildren = cycleCourseGroup.getChildDegreeModules();
        assertEquals(2, cycleChildren.size());
        assertTrue(cycleChildren.contains(mandatoryCourseGroup));
        assertTrue(cycleChildren.contains(optionalCourseGroup));

        // mandatory's direct children: C1, C2, C3, C6, C7, C8
        final Set<DegreeModule> mandatoryChildren = mandatoryCourseGroup.getChildDegreeModules();
        assertEquals(6, mandatoryChildren.size());
        assertTrue(mandatoryChildren.contains(cc1));
        assertTrue(mandatoryChildren.contains(cc2));
        assertTrue(mandatoryChildren.contains(cc3));
        assertTrue(mandatoryChildren.contains(cc6));
        assertTrue(mandatoryChildren.contains(cc7));
        assertTrue(mandatoryChildren.contains(cc8));

        // optional's direct children: C4, C5
        final Set<DegreeModule> optionalChildren = optionalCourseGroup.getChildDegreeModules();
        assertEquals(2, optionalChildren.size());
        assertTrue(optionalChildren.contains(cc4));
        assertTrue(optionalChildren.contains(cc5));
    }

    @Test
    public void testCourseGroup_getParentCourseGroups_returnsParentGroups() {
        assertTrue(rootCourseGroup.getParentCourseGroups().isEmpty());

        Set<CourseGroup> cycleParents = cycleCourseGroup.getParentCourseGroups();
        assertEquals(1, cycleParents.size());
        assertTrue(cycleParents.contains(rootCourseGroup));

        Set<CourseGroup> mandatoryParents = mandatoryCourseGroup.getParentCourseGroups();
        assertEquals(1, mandatoryParents.size());
        assertTrue(mandatoryParents.contains(cycleCourseGroup));
    }

    @Test
    public void testCourseGroup_hasDegreeModule_findsRecursivelyExcludesSiblings() {
        // root finds all modules recursively
        assertTrue(rootCourseGroup.hasDegreeModule(cycleCourseGroup));
        assertTrue(rootCourseGroup.hasDegreeModule(mandatoryCourseGroup));
        assertTrue(rootCourseGroup.hasDegreeModule(optionalCourseGroup));
        assertTrue(rootCourseGroup.hasDegreeModule(cc1));
        assertTrue(rootCourseGroup.hasDegreeModule(cc4));

        // mandatory finds itself and its children (C1–C3, C6)
        assertTrue(mandatoryCourseGroup.hasDegreeModule(mandatoryCourseGroup));
        assertTrue(mandatoryCourseGroup.hasDegreeModule(cc1));
        assertTrue(mandatoryCourseGroup.hasDegreeModule(cc2));
        assertTrue(mandatoryCourseGroup.hasDegreeModule(cc3));
        assertTrue(mandatoryCourseGroup.hasDegreeModule(cc6));

        // mandatory does NOT find a sibling group (optional) or sibling courses (C4)
        assertFalse(mandatoryCourseGroup.hasDegreeModule(optionalCourseGroup));
        assertFalse(mandatoryCourseGroup.hasDegreeModule(cc4));
    }

    @Test
    public void testCourseGroup_hasDegreeModuleOnChilds_detectsDirectChildren() {
        assertTrue(cycleCourseGroup.hasDegreeModuleOnChilds(mandatoryCourseGroup));
        assertTrue(cycleCourseGroup.hasDegreeModuleOnChilds(optionalCourseGroup));
        assertFalse(cycleCourseGroup.hasDegreeModuleOnChilds(cc1)); // C1 is under mandatory, not direct

        assertTrue(mandatoryCourseGroup.hasDegreeModuleOnChilds(cc1));
        assertTrue(mandatoryCourseGroup.hasDegreeModuleOnChilds(cc2));
        assertTrue(mandatoryCourseGroup.hasDegreeModuleOnChilds(cc6));
        assertFalse(mandatoryCourseGroup.hasDegreeModuleOnChilds(cc4)); // C4 is in optional
    }

    @Test
    public void testCourseGroup_hasAnyChildContextWithCurricularCourse_trueForGroupsWithCourses() {
        assertTrue(mandatoryCourseGroup.hasAnyChildContextWithCurricularCourse());
        assertTrue(optionalCourseGroup.hasAnyChildContextWithCurricularCourse());

        // cycle has only CourseGroup children (mandatory, optional), not direct CurricularCourse children -> false
        assertFalse(cycleCourseGroup.hasAnyChildContextWithCurricularCourse());
    }

    @Test
    public void testCourseGroup_hasAnyParentBranchCourseGroup() {
        assertFalse(rootCourseGroup.hasAnyParentBranchCourseGroup());
        assertFalse(cycleCourseGroup.hasAnyParentBranchCourseGroup());
        assertFalse(mandatoryCourseGroup.hasAnyParentBranchCourseGroup());
        assertFalse(optionalCourseGroup.hasAnyParentBranchCourseGroup());

        // When a group is itself a branch, it returns true
        cycleCourseGroup.setBranchType(BranchType.MAJOR);
        try {
            assertFalse(rootCourseGroup.hasAnyParentBranchCourseGroup());
            assertTrue(cycleCourseGroup.hasAnyParentBranchCourseGroup());
            assertTrue(mandatoryCourseGroup.hasAnyParentBranchCourseGroup());
            assertTrue(optionalCourseGroup.hasAnyParentBranchCourseGroup());
        } finally {
            cycleCourseGroup.setBranchType(null);
        }
    }

    @Test
    public void testCourseGroup_getAllCurricularCourses_collectsRecursively() {
        final Set<CurricularCourse> rootCCs = rootCourseGroup.getAllCurricularCourses(firstSemester);
        assertEquals(8, rootCCs.size());
        assertTrue(rootCCs.contains(cc1));
        assertTrue(rootCCs.contains(cc2));
        assertTrue(rootCCs.contains(cc3));
        assertTrue(rootCCs.contains(cc4));
        assertTrue(rootCCs.contains(cc5));
        assertTrue(rootCCs.contains(cc6));
        assertTrue(rootCCs.contains(cc7));
        assertTrue(rootCCs.contains(cc8));

        final Set<CurricularCourse> cycleCCs = cycleCourseGroup.getAllCurricularCourses(secondSemester);
        assertEquals(8, cycleCCs.size());

        // mandatory collects C1–C3, C6-C8
        final Set<CurricularCourse> mandatoryCCs = mandatoryCourseGroup.getAllCurricularCourses(secondSemester);
        assertEquals(6, mandatoryCCs.size());
        assertTrue(mandatoryCCs.contains(cc1));
        assertTrue(mandatoryCCs.contains(cc2));
        assertTrue(mandatoryCCs.contains(cc3));
        assertTrue(mandatoryCCs.contains(cc6));
        assertTrue(mandatoryCCs.contains(cc7));
        assertTrue(mandatoryCCs.contains(cc8));

        // optional collects C4, C5
        final Set<CurricularCourse> optionalCCs = optionalCourseGroup.getAllCurricularCourses(firstSemester);
        assertEquals(2, optionalCCs.size());
        assertTrue(optionalCCs.contains(cc4));
        assertTrue(optionalCCs.contains(cc5));

        // Null interval returns all CCs regardless of interval
        final Set<CurricularCourse> nullIntervalCCs = rootCourseGroup.getAllCurricularCourses(null);
        assertEquals(8, nullIntervalCCs.size());
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