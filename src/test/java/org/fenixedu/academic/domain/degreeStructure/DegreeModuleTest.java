package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

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

    private static CourseGroup groupA;
    private static CourseGroup groupA1;
    private static CurricularCourse courseA1;
    private static CurricularCourse courseA2;
    private static CurricularPeriod semesterPeriod;
    private static CourseGroup groupB;

    private static ExecutionYear currentYear;
    private static ExecutionInterval semester1;
    private static ExecutionInterval semester2;
    private static ExecutionYear pastYear;
    private static ExecutionInterval pastSemester1;
    private static ExecutionInterval pastSemester2;

    private static EnrolmentPeriodRestrictions enrolmentPeriodRestriction;
    private static CreditsLimit creditsLimitCurrent;
    private static CreditsLimit creditsLimitPast;
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

            final CurricularPeriod yearPeriod =
                    new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
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

    @Test
    public void getCurricularRules_byExecutionYear_currentYear() {
        List<CurricularRule> rules = groupA1.getCurricularRules(currentYear);
        assertEquals(2, rules.size());
        assertTrue(rules.contains(creditsLimitCurrent));
        assertTrue(rules.contains(selectionLimit));
    }

    @Test
    public void getCurricularRules_byExecutionYear_noMatch() {
        assertTrue(groupA1.getCurricularRules(pastYear).isEmpty());
    }

    @Test
    public void getCurricularRules_byExecutionYear_pastYear() {
        assertTrue(groupA.getCurricularRules(currentYear).isEmpty());

        List<CurricularRule> pastRules = groupA.getCurricularRules(pastYear);
        assertEquals(1, pastRules.size());
        assertTrue(pastRules.contains(creditsLimitPast));
    }

    @Test
    public void getCurricularRules_byExecutionInterval_currentInterval() {
        List<CurricularRule> rules = groupA1.getCurricularRules(semester1);
        assertEquals(2, rules.size());
        assertTrue(rules.contains(creditsLimitCurrent));
        assertTrue(rules.contains(selectionLimit));
    }

    @Test
    public void getCurricularRules_byExecutionInterval_noMatch() {
        assertTrue(groupA1.getCurricularRules(pastSemester1).isEmpty());
    }

    @Test
    public void getCurricularRules_byExecutionInterval_pastInterval() {
        // groupA has creditsLimitPast (valid only for past interval)
        assertTrue(groupA.getCurricularRules(semester1).isEmpty());

        List<CurricularRule> pastRules = groupA.getCurricularRules(pastSemester1);
        assertEquals(1, pastRules.size());
        assertTrue(pastRules.contains(creditsLimitPast));
    }

    @Test
    public void getVisibleCurricularRules_byExecutionYear_excludesNonVisible() {
        assertFalse(enrolmentPeriodRestriction.isVisible());
        assertTrue(root.getCurricularRules(currentYear).contains(enrolmentPeriodRestriction));
        assertFalse(root.getVisibleCurricularRules(currentYear).contains(enrolmentPeriodRestriction));
    }

    @Test
    public void getVisibleCurricularRules_byExecutionInterval_excludesNonVisible() {
        assertFalse(enrolmentPeriodRestriction.isVisible());
        assertTrue(root.getCurricularRules(semester1).contains(enrolmentPeriodRestriction));
        assertFalse(root.getVisibleCurricularRules(semester1).contains(enrolmentPeriodRestriction));
    }

    @Test
    public void getVisibleCurricularRules_byExecutionYear_excludesInactive() {
        assertTrue(creditsLimitPast.isVisible());
        assertTrue(groupA.getCurricularRules(currentYear).isEmpty());
        assertTrue(groupA.getVisibleCurricularRules(currentYear).isEmpty());
    }

    @Test
    public void getVisibleCurricularRules_byExecutionInterval_excludesInactive() {
        assertTrue(creditsLimitPast.isVisible());
        assertTrue(groupA.getCurricularRules(semester1).isEmpty());
        assertTrue(groupA.getVisibleCurricularRules(semester1).isEmpty());
    }

    @Test
    public void getVisibleCurricularRules_byExecutionYear_validMatch() {
        assertTrue(groupA.getVisibleCurricularRules(pastYear).contains(creditsLimitPast));
    }

    @Test
    public void getCurricularRules_byContextAndInterval_matchingContext() {
        Context ctx = groupA.getChildContexts(CurricularCourse.class).iterator().next();
        assertEquals(groupA, ctx.getParentCourseGroup());

        List<CurricularRule> rules = groupA1.getCurricularRules(ctx, semester1);
        assertEquals(2, rules.size());
        assertTrue(rules.contains(creditsLimitCurrent));
        assertTrue(rules.contains(selectionLimit));
    }

    @Test
    public void getCurricularRules_byContextAndInterval_invalid() {
        Context ctx = groupA.getChildContexts(CurricularCourse.class).iterator().next();
        assertTrue(groupA1.getCurricularRules(ctx, pastSemester1).isEmpty());
    }

    @Test
    public void getCurricularRules_byTypeAndInterval() {
        List<? extends ICurricularRule> rules = groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, semester1);
        assertEquals(1, rules.size());
        assertEquals(creditsLimitCurrent, rules.get(0));
    }

    @Test
    public void getCurricularRules_byTypeAndInterval_noMatch() {
        List<? extends ICurricularRule> rules = groupA1.getCurricularRules(CurricularRuleType.EXCLUSIVENESS, semester1);
        assertTrue(rules.isEmpty());
    }

    @Test
    public void getCurricularRules_byTypeAndYear() {
        List<? extends ICurricularRule> rules = groupA.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, pastYear);
        assertEquals(1, rules.size());
        assertEquals(creditsLimitPast, rules.get(0));
    }

    @Test
    public void getCurricularRules_byTypeAndYear_noMatch() {
        assertTrue(groupA.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, currentYear).isEmpty());
    }

    @Test
    public void getCurricularRules_byTypeParentAndYear() {
        List<? extends ICurricularRule> rules = groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, groupA, currentYear);
        assertEquals(1, rules.size());
    }

    @Test
    public void getCurricularRules_byTypeParentAndYear_noMatch() {
        assertTrue(groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, groupB, currentYear).isEmpty());
    }

    @Test
    public void getCurricularRules_byTypeParentAndInterval() {
        List<? extends ICurricularRule> rules =
                groupA1.getCurricularRules(CurricularRuleType.DEGREE_MODULES_SELECTION_LIMIT, groupA, semester1);
        assertEquals(1, rules.size());
        assertEquals(selectionLimit, rules.get(0));
    }

    @Test
    public void getCurricularRules_byTypeParentAndInterval_noMatch() {
        assertTrue(groupA1.getCurricularRules(CurricularRuleType.CREDITS_LIMIT, groupB, semester1).isEmpty());
    }

    @Test
    public void getMostRecentActiveCurricularRule_noRules() {
        assertNull(groupB.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, groupB, (ExecutionYear) null));
    }

    @Test
    public void getMostRecentActiveCurricularRule_nullYear() {
        ICurricularRule result =
                groupA1.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, groupA, (ExecutionYear) null);
        assertEquals(creditsLimitCurrent, result);
    }

    @Test
    public void getMostRecentActiveCurricularRule_withYear() {
        ICurricularRule result = groupA.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, root, pastYear);
        assertEquals(creditsLimitPast, result);
    }

    @Test
    public void getMostRecentActiveCurricularRule_withYear_noValid() {
        assertNull(groupA.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, root, currentYear));
    }

    @Test
    public void getMostRecentActiveCurricularRule_multipleValid_throws() {
        CreditsLimit newActiveRule = new CreditsLimit(groupA1, groupA, semester2, null, 40.0, 50.0);
        assertThrows(DomainException.class,
                () -> groupA1.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, groupA, currentYear));
        newActiveRule.delete();
    }

    @Test
    public void getDegreeModulesSelectionLimitRule_found() {
        assertEquals(selectionLimit, groupA1.getDegreeModulesSelectionLimitRule(semester1));
    }

    @Test
    public void getDegreeModulesSelectionLimitRule_notFound() {
        assertNull(groupB.getDegreeModulesSelectionLimitRule(semester1));
    }

    @Test
    public void getCreditsLimitRule_found() {
        assertEquals(creditsLimitCurrent, groupA1.getCreditsLimitRule(semester1));
    }

    @Test
    public void getCreditsLimitRule_notFound() {
        assertNull(groupB.getCreditsLimitRule(semester1));
    }

    @Test
    public void getExclusivenessRules_found() {
        List<Exclusiveness> rules = courseA1.getExclusivenessRules(semester1);
        assertEquals(1, rules.size());
        assertEquals(exclusivenessRule, rules.get(0));
    }

    @Test
    public void getExclusivenessRules_notFound() {
        assertTrue(groupB.getExclusivenessRules(semester1).isEmpty());
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
    }
}