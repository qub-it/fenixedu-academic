package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.CurricularRule;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleType;
import org.fenixedu.academic.domain.curricularRules.DegreeModulesSelectionLimit;
import org.fenixedu.academic.domain.curricularRules.EnrolmentPeriodRestrictions;
import org.fenixedu.academic.domain.curricularRules.Exclusiveness;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.RestrictionDoneDegreeModule;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeModuleTest {

    private static DegreeCurricularPlan degreeCurricularPlan;
    private static RootCourseGroup root;

    private static CourseGroup groupA, groupA1, groupB;
    private static CurricularCourse courseA1, courseA2;
    private static CurricularPeriod yearPeriod, semesterPeriod;

    private static ExecutionYear currentYear, pastYear;
    private static ExecutionInterval semester1, semester2, pastSemester1, pastSemester2;

    private static EnrolmentPeriodRestrictions enrolmentPeriodRestriction;
    private static CreditsLimit creditsLimitCurrent, creditsLimitPast;
    private static Exclusiveness exclusivenessRule;
    private static DegreeModulesSelectionLimit selectionLimit;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            DegreeTest.initDegree();
            UserUtil.initAdminUser();

            final Degree degree = Degree.find(DEGREE_A_CODE);

            currentYear = ExecutionYear.findCurrent(null);
            semester1 = currentYear.getFirstExecutionPeriod();
            semester2 = currentYear.getLastExecutionPeriod();

            degreeCurricularPlan = new DegreeCurricularPlan(degree, DCP_NAME_V1, AcademicPeriod.THREE_YEAR, semester1);
            root = degreeCurricularPlan.getRoot();

            yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
            semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

            pastYear = (ExecutionYear) currentYear.getPrevious();
            pastSemester1 = pastYear.getFirstExecutionPeriod();
            pastSemester2 = pastYear.getLastExecutionPeriod();

            // root
            //   ├── groupA
            //   │   ├── groupA1
            //   │   │   └── courseA1
            //   │   └── courseA2
            //   └── groupB

            groupA = new CourseGroup(root, "Group A", "Group A", semester1, null);
            groupA1 = new CourseGroup(groupA, "Group A1", "Group A1", semester1, null);

            courseA1 = new CurricularCourse();
            new Context(groupA1, courseA1, semesterPeriod, semester1, null);

            courseA2 = new CurricularCourse();
            new Context(groupA, courseA2, semesterPeriod, semester2, null);

            groupB = new CourseGroup(root, "Group B", "Group B", semester1, null);

            enrolmentPeriodRestriction = new EnrolmentPeriodRestrictions(root, semester1, null);

            // CurricularRules on groupA1: valid from current year onward
            creditsLimitCurrent = new CreditsLimit(groupA1, groupA, semester1, null, 30.0, 60.0);
            selectionLimit = new DegreeModulesSelectionLimit(groupA1, groupA, semester1, null, 5, 10);

            // CurricularRule on groupA: valid only in past year
            creditsLimitPast = new CreditsLimit(groupA, root, pastSemester1, pastSemester2, 10.0, 20.0);

            // Exclusiveness on courseA1
            exclusivenessRule = new Exclusiveness(courseA1, courseA2, groupA, semester1, null);

            return null;
        });
    }

    public void comparatorByMinEcts() {
        List<DegreeModule> modules = new ArrayList<>(List.of(groupA1, groupB));
        modules.sort(DegreeModule.comparatorByMinEcts(semester1));

        assertEquals(groupB, modules.get(0));   // 0.0
        assertEquals(groupA1, modules.get(1));  // 30.0

        // tie-breaker, compare by externalId
        CourseGroup a = new CourseGroup(root, "AA", "AA", semester1, null);
        CourseGroup b = new CourseGroup(root, "BB", "BB", semester1, null);

        modules = new ArrayList<>(List.of(b, a));
        modules.sort(DegreeModule.comparatorByMinEcts(semester1));
        assertTrue(modules.get(0).getExternalId().compareTo(modules.get(1).getExternalId()) < 0);

        a.delete();
        b.delete();
    }

    @Test
    public void getAllParentCourseGroups_rootLevelHasNoParents() {
        assertTrue(groupA.getAllParentCourseGroups().isEmpty());
        assertTrue(groupB.getAllParentCourseGroups().isEmpty());
        assertTrue(root.getAllParentCourseGroups().isEmpty());
    }

    @Test
    public void getAllParentCourseGroups_directChildReturnsImmediateParent() {
        assertEquals(Set.of(groupA), groupA1.getAllParentCourseGroups());
        assertEquals(Set.of(groupA), courseA2.getAllParentCourseGroups());
    }

    @Test
    public void getAllParentCourseGroups_deepChildReturnsFullHierarchy() {
        assertEquals(Set.of(groupA1, groupA), courseA1.getAllParentCourseGroups());
    }

    @Test
    public void getParentContextsByExecutionYear_currentYear() {
        List<Context> result = courseA1.getParentContextsByExecutionYear(currentYear);
        assertEquals(1, result.size());
        assertEquals(groupA1, result.get(0).getParentCourseGroup());
    }

    @Test
    public void getParentContextsByExecutionYear_returnsEmpty() {
        // courseA1's context begins at semester1 (current year); not open for pastYear
        assertTrue(courseA1.getParentContextsByExecutionYear(pastYear).isEmpty());
        assertTrue(root.getParentContextsByExecutionYear(currentYear).isEmpty());
    }

    @Test
    public void getParentContextsByExecutionSemester_validInterval() {
        List<Context> result = courseA1.getParentContextsByExecutionSemester(semester1);
        assertEquals(1, result.size());
        assertEquals(groupA1, result.get(0).getParentCourseGroup());
    }

    @Test
    public void getParentContextsByExecutionSemester_nullInterval() {
        // null interval returns all parent contexts
        List<Context> result = courseA1.getParentContextsByExecutionSemester(null);
        assertEquals(1, result.size());
        assertEquals(groupA1, result.get(0).getParentCourseGroup());
    }

    @Test
    public void getParentContextsByExecutionSemester_returnsEmpty() {
        // pastSemester1 is before courseA1's context validity
        assertTrue(courseA1.getParentContextsByExecutionSemester(pastSemester1).isEmpty());

        // curricular period mismatch (courseA1's curricular period is semester 1)
        assertTrue(courseA1.getParentContextsByExecutionSemester(semester2).isEmpty());
    }

    @Test
    public void getParentContextsBy_matchingParentContext() {
        List<Context> result = courseA1.getParentContextsBy(semester1, groupA1);
        assertEquals(1, result.size());
        assertEquals(groupA1, result.get(0).getParentCourseGroup());
    }

    @Test
    public void getParentContextsBy_returnsEmpty() {
        // groupA is not courseA1's direct parent (groupA1 is)
        assertTrue(courseA1.getParentContextsBy(semester1, groupA).isEmpty());

        // pastSemester1 is before courseA1's context validity
        assertTrue(courseA1.getParentContextsBy(pastSemester1, groupA1).isEmpty());
    }

    @Test
    public void hasAnyParentContexts_validInterval() {
        assertTrue(courseA1.hasAnyParentContexts(semester1));
    }

    @Test
    public void hasAnyParentContexts_returnsEmpty() {
        // curricular period mismatch (courseA1's curricular period is semester 1)
        assertFalse(courseA1.hasAnyParentContexts(semester2));

        // pastSemester1 is before courseA1's context validity
        assertFalse(courseA1.hasAnyParentContexts(pastSemester1));
    }

    @Test
    public void hasAnyOpenParentContexts_contextOpenInInterval() {
        assertTrue(courseA1.hasAnyOpenParentContexts(semester1));
    }

    @Test
    public void hasAnyOpenParentContexts_contextNotOpenInInterval() {
        assertFalse(courseA1.hasAnyOpenParentContexts(pastSemester1));
    }

    @Test
    public void getCurricularRules_executionYearReturnsRules() {
        // groupA1 has creditsLimitCurrent and selectionLimit (both valid for current year)
        List<CurricularRule> rules = groupA1.getCurricularRules(currentYear);
        assertEquals(2, rules.size());
        assertTrue(rules.contains(creditsLimitCurrent));
        assertTrue(rules.contains(selectionLimit));

        // groupA has creditsLimitPast (valid only for past year)
        List<CurricularRule> pastRules = groupA.getCurricularRules(pastYear);
        assertEquals(1, pastRules.size());
        assertTrue(pastRules.contains(creditsLimitPast));
    }

    @Test
    public void getCurricularRules_executionYearReturnsEmpty() {
        assertTrue(groupA1.getCurricularRules(pastYear).isEmpty());
        assertTrue(groupA.getCurricularRules(currentYear).isEmpty());
    }

    @Test
    public void getCurricularRules_executionIntervalReturnsRules() {
        List<CurricularRule> rules = groupA1.getCurricularRules(semester1);
        assertEquals(2, rules.size());
        assertTrue(rules.contains(creditsLimitCurrent));
        assertTrue(rules.contains(selectionLimit));

        List<CurricularRule> pastRules = groupA.getCurricularRules(pastSemester1);
        assertEquals(1, pastRules.size());
        assertTrue(pastRules.contains(creditsLimitPast));
    }

    @Test
    public void getCurricularRules_executionIntervalReturnsEmpty() {
        assertTrue(groupA1.getCurricularRules(pastSemester1).isEmpty());
        assertTrue(groupA.getCurricularRules(semester1).isEmpty());
    }

    @Test
    public void getVisibleCurricularRules_excludesNonVisibleRules() {
        assertFalse(enrolmentPeriodRestriction.isVisible());

        assertEquals(1, root.getCurricularRules(currentYear).size());
        assertTrue(root.getVisibleCurricularRules(currentYear).isEmpty());

        assertEquals(1, root.getCurricularRules(semester1).size());
        assertTrue(root.getVisibleCurricularRules(semester1).isEmpty());
    }

    @Test
    public void getVisibleCurricularRules_excludesInactiveRules() {
        assertTrue(creditsLimitPast.isVisible());

        assertTrue(groupA.getCurricularRules(currentYear).isEmpty());
        assertTrue(groupA.getVisibleCurricularRules(currentYear).isEmpty());

        assertTrue(groupA.getCurricularRules(semester1).isEmpty());
        assertTrue(groupA.getVisibleCurricularRules(semester1).isEmpty());
    }

    @Test
    public void getVisibleCurricularRules_returnsVisibleRules() {
        List<CurricularRule> yearVisibleRules = groupA.getVisibleCurricularRules(pastYear);
        assertEquals(1, yearVisibleRules.size());
        assertTrue(yearVisibleRules.contains(creditsLimitPast));

        List<CurricularRule> intervalVisibleRules = groupA1.getVisibleCurricularRules(semester1);
        assertEquals(2, intervalVisibleRules.size());
        assertTrue(intervalVisibleRules.contains(creditsLimitCurrent));
        assertTrue(intervalVisibleRules.contains(selectionLimit));
    }

    @Test
    public void getCurricularRules_byContextAndInterval_matchingContext() {
        Context ctx = groupA.getChildContexts(CourseGroup.class).iterator().next();
        assertEquals(groupA, ctx.getParentCourseGroup());

        List<CurricularRule> rules = groupA1.getCurricularRules(ctx, semester1);
        assertEquals(2, rules.size());
        assertTrue(rules.contains(creditsLimitCurrent));
        assertTrue(rules.contains(selectionLimit));
    }

    @Test
    public void getCurricularRules_byContextAndInterval_returnsEmpty() {
        Context ctx = groupA.getChildContexts(CourseGroup.class).iterator().next();
        assertTrue(groupA1.getCurricularRules(ctx, pastSemester1).isEmpty());
    }

    @Test
    public void getCurricularRules_byType_returnsMatchingRules() {
        List<? extends ICurricularRule> intervalRules = groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, semester1);
        assertEquals(1, intervalRules.size());
        assertEquals(creditsLimitCurrent, intervalRules.get(0));

        List<? extends ICurricularRule> yearRules = groupA.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, pastYear);
        assertEquals(1, yearRules.size());
        assertEquals(creditsLimitPast, yearRules.get(0));
    }

    @Test
    public void getCurricularRules_byType_returnsEmpty() {
        assertTrue(groupA1.getCurricularRules(CurricularRuleType.EXCLUSIVENESS, semester1).isEmpty());
        assertTrue(groupA.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, currentYear).isEmpty());
    }

    @Test
    public void getCurricularRules_byTypeAndParent_returnsMatchingRules() {
        List<? extends ICurricularRule> yearRules =
                groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, groupA, currentYear);
        assertEquals(1, yearRules.size());
        assertEquals(creditsLimitCurrent, yearRules.get(0));

        List<? extends ICurricularRule> intervalRules =
                groupA1.getCurricularRules(CurricularRuleType.DEGREE_MODULES_SELECTION_LIMIT, groupA, semester1);
        assertEquals(1, intervalRules.size());
        assertEquals(selectionLimit, intervalRules.get(0));
    }

    @Test
    public void getCurricularRules_byTypeAndParent_returnsEmpty() {
        // invalid interval
        assertTrue(groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, groupA, pastYear).isEmpty());
        // no match for rule type
        assertTrue(groupA1.getCurricularRules(CurricularRuleType.EXCLUSIVENESS, groupA, semester1).isEmpty());
        // does not apply to course group
        assertTrue(groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, groupB, currentYear).isEmpty());
    }

    @Test
    public void getMostRecentActiveCurricularRule_returnsMostRecentRule() {
        assertEquals(creditsLimitCurrent,
                groupA1.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, groupA, null));
        assertEquals(creditsLimitPast,
                groupA.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, root, pastYear));
    }

    @Test
    public void getMostRecentActiveCurricularRule_returnsNullWhenNoRuleMatches() {
        assertNull(groupB.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, groupB, null));
        assertNull(groupA.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, root, currentYear));
    }

    @Test
    public void getMostRecentActiveCurricularRule_throwsWhenMultipleRulesMatch() {
        CreditsLimit newActiveRule = new CreditsLimit(groupA1, groupA, semester2, null, 40.0, 50.0);
        assertThrows(DomainException.class,
                () -> groupA1.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, groupA, currentYear));
        newActiveRule.delete();
    }

    @Test
    public void getDegreeModulesSelectionLimitRule_returnsRule() {
        assertEquals(selectionLimit, groupA1.getDegreeModulesSelectionLimitRule(semester1));
    }

    @Test
    public void getDegreeModulesSelectionLimitRule_notFound() {
        assertNull(groupB.getDegreeModulesSelectionLimitRule(semester1));
    }

    @Test
    public void getCreditsLimitRule_returnsRule() {
        assertEquals(creditsLimitCurrent, groupA1.getCreditsLimitRule(semester1));
    }

    @Test
    public void getCreditsLimitRule_notFound() {
        assertNull(groupB.getCreditsLimitRule(semester1));
    }

    @Test
    public void getExclusivenessRules_returnsRule() {
        List<Exclusiveness> rules = courseA1.getExclusivenessRules(semester1);
        assertEquals(1, rules.size());
        assertEquals(exclusivenessRule, rules.get(0));
    }

    @Test
    public void getExclusivenessRules_notFound() {
        assertTrue(groupB.getExclusivenessRules(semester1).isEmpty());
    }

    @Test
    public void getParentCycleCourseGroups_withCycleParent_returnsCycle() {
        CycleCourseGroup cycleGroup =
                new CycleCourseGroup(root, "Test Cycle", "Test Cycle", CycleType.FIRST_CYCLE, semester1, null);
        CourseGroup intermediate = new CourseGroup(cycleGroup, "Intermediate", "Intermediate", semester1, null);
        CurricularCourse course = new CurricularCourse();
        new Context(intermediate, course, semesterPeriod, semester1, null);

        Collection<CycleCourseGroup> result = course.getParentCycleCourseGroups();
        assertEquals(1, result.size());
        assertTrue(result.contains(cycleGroup));

        course.delete();
        intermediate.delete();
        cycleGroup.delete();
    }

    @Test
    public void getParentCycleCourseGroups_withoutCycleParent_returnsEmpty() {
        assertTrue(courseA1.getParentCycleCourseGroups().isEmpty());
    }

    @Test
    public void getParentCourseGroups_returnsDirectParents() {
        // single parent
        assertEquals(Set.of(groupA), groupA1.getParentCourseGroups());

        // two parents
        CurricularCourse temp = new CurricularCourse();
        new Context(groupA, temp, semesterPeriod, semester1, null);
        new Context(groupB, temp, semesterPeriod, semester1, null);
        assertEquals(Set.of(groupA, groupB), temp.getParentCourseGroups());

        temp.delete();
    }

    @Test
    public void delete_deletableModule() {
        CourseGroup parentGroup = new CourseGroup(root, "DeleteTestParent", "DeleteTestParent", semester1, null);
        CurricularCourse temp = new CurricularCourse();
        CurricularCourse tempOther = new CurricularCourse();

        Context ctx = new Context(parentGroup, temp, semesterPeriod, semester1, null);

        Exclusiveness ruleOnTemp = new Exclusiveness(temp, tempOther, null, semester1, null);

        RestrictionDoneDegreeModule precedence = new RestrictionDoneDegreeModule(tempOther, temp, null, null, semester1, null);

        Exclusiveness exclusiveness = new Exclusiveness(tempOther, temp, null, semester1, null);

        assertTrue(temp.getCanBeDeleted());
        assertEquals(1, temp.getParentContextsSet().size());
        assertEquals(1, temp.getCurricularRulesSet().size());
        assertEquals(1, temp.getParticipatingPrecedenceCurricularRulesSet().size());
        assertEquals(1, temp.getParticipatingExclusivenessCurricularRulesSet().size());

        temp.delete();
        assertNull(temp.getRootDomainObject());

        assertNull(ctx.getRootDomainObject());
        assertNull(ruleOnTemp.getRootDomainObject());
        assertNull(precedence.getRootDomainObject());
        assertNull(exclusiveness.getRootDomainObject());

        parentGroup.delete();
        tempOther.delete();
        assertNull(parentGroup.getRootDomainObject());
        assertNull(tempOther.getRootDomainObject());
    }
}