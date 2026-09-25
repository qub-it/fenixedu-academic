package org.fenixedu.academic.domain.curricularRules;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.ADMIN_USERNAME;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.CYCLE_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.MANDATORY_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.OPTIONAL_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.approve;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createOptionalCurricularCourse;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.getChildGroup;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AnyCurricularCourseTest {

    private static final String COMPETENCE_COURSES_UNIT = "QS>Courses>CC";

    private static final String OTHER_UNIT = "QS>Degrees";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            return null;
        });
    }

    @Test
    public void enrolmentAllowed_withNoRestrictions() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCreditsBelowMinimum() {
        final AnyCurricularCourseScenario scenario = createScenario(7d, null);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void enrolmentAllowed_whenMinimumEqualToCredits() {
        final AnyCurricularCourseScenario scenario = createScenario(COURSE_CREDITS(), null);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCreditsAboveMaximum() {
        final AnyCurricularCourseScenario scenario = createScenario(null, 5d);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenMaximumEqualToCredits() {
        final AnyCurricularCourseScenario scenario = createScenario(null, COURSE_CREDITS());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentAllowed_whenCreditsWithinBounds() {
        final AnyCurricularCourseScenario scenario = createScenario(4d, 8d);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentAllowed_whenCourseGroupMatches() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getCourseGroupsSet().add(scenario.optionalGroup());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCourseGroupDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getCourseGroupsSet().add(scenario.optionalGroup());

        final RuleResult result = evaluateEnrolling(scenario, "C3", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_whenDegreeCurricularPlanDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final DegreeCurricularPlan otherDegreeCurricularPlan = createDegreeCurricularPlan(scenario.executionYear());
        scenario.rule().getDegreeCurricularPlansSet().add(otherDegreeCurricularPlan);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_whenDegreeDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final DegreeCurricularPlan otherDegreeCurricularPlan = createDegreeCurricularPlan(scenario.executionYear());
        scenario.rule().getDegreesSet().add(otherDegreeCurricularPlan.getDegree());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_whenDegreeTypeDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getDegreeTypesSet().add(new DegreeType(new LocalizedString(Locale.getDefault(), "Other Degree Type")));

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenCompetenceCourseMatches() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getCompetenceCoursesSet().add(course(scenario, "C4").getCompetenceCourse());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCompetenceCourseDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getCompetenceCoursesSet().add(course(scenario, "C1").getCompetenceCourse());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenLevelTypeMatches() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final CompetenceCourseLevelType levelType = createLevelType();
        scenario.rule().getCompetenceCourseLevelTypesSet().add(levelType);
        final ExecutionInterval interval = interval(scenario);
        final CompetenceCourseInformation information =
                course(scenario, "C4").getCompetenceCourse().findInformationMostRecentUntil(interval);
        information.setLevelType(levelType);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenLevelTypeDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getCompetenceCourseLevelTypesSet().add(createLevelType());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenUnitMatches() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getUnitsSet().add(Unit.findInternalUnitByAcronymPath(COMPETENCE_COURSES_UNIT).orElseThrow());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenUnitDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().getUnitsSet().add(Unit.findInternalUnitByAcronymPath(OTHER_UNIT).orElseThrow());

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_withNegation_whenRuleMatches() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        scenario.rule().setNegation(true);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_withNegation_whenRuleDoesNotMatch() {
        final AnyCurricularCourseScenario scenario = createScenario(7d, null);
        scenario.rule().setNegation(true);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenAlreadyApproved() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        enrol(scenario.getStudentCurricularPlan(), scenario.executionYear(), "C1");
        approve(scenario.getStudentCurricularPlan(), scenario.executionYear(), "C1");

        final RuleResult result = evaluateEnrolling(scenario, "C1", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void enrolmentBlocked_whenAlreadyEnrolled() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final Context optionalContext = scenario.optionalCourse().getParentContextsSet().iterator().next();
        final ExecutionInterval interval = scenario.executionYear()
                .getChildInterval(optionalContext.getCurricularPeriod().getChildOrder(),
                        optionalContext.getCurricularPeriod().getAcademicPeriod());
        EnrolmentTest.createOptionalEnrolment(scenario.getStudentCurricularPlan(), interval, optionalContext,
                course(scenario, "C4"), ADMIN_USERNAME);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentImpossible_whenEnrolledCourseStopsMatchingTheRule() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final Context optionalContext = scenario.optionalCourse().getParentContextsSet().iterator().next();
        final ExecutionInterval interval = scenario.executionYear()
                .getChildInterval(optionalContext.getCurricularPeriod().getChildOrder(),
                        optionalContext.getCurricularPeriod().getAcademicPeriod());
        EnrolmentTest.createOptionalEnrolment(scenario.getStudentCurricularPlan(), interval, optionalContext,
                course(scenario, "C4"), ADMIN_USERNAME);
        final OptionalEnrolment optionalEnrolment =
                (OptionalEnrolment) scenario.getStudentCurricularPlan().getEnrolments(course(scenario, "C4")).iterator().next();

        scenario.rule().getCompetenceCoursesSet().add(course(scenario, "C1").getCompetenceCourse());

        final EnroledOptionalEnrolment enroled =
                new EnroledOptionalEnrolment(optionalEnrolment, scenario.optionalCourse(), interval);
        final RuleResult result = evaluate(enroled, scenario.rule(), scenario.getStudentCurricularPlan(), interval,
                CurricularRuleLevel.ENROLMENT_VERIFICATION_WITH_RULES);

        assertTrue(result.isTrue());
        assertTrue(result.hasAnyImpossibleEnrolment());
    }

    @Test
    public void enrolmentNA_whenRuleDoesNotApply() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final AnyCurricularCourse rule =
                new AnyCurricularCourse(scenario.optionalCourse(), scenario.mandatoryGroup(), scenario.executionYear(), null,
                        null, null);

        final RuleResult result = evaluateEnrolling(scenario, "C4", rule, CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isNA());
        assertFalse(result.isTrue());
    }

    @Test
    public void prefilterEnrolmentBlocked_whenRuleFails() {
        final AnyCurricularCourseScenario scenario = createScenario(7d, null);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_PREFILTER);

        assertTrue(result.isFalse());
    }

    @Test
    public void prefilterEnrolmentNA_whenNonOptionalModule() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        final CurricularCourse c4 = course(scenario, "C4");
        final Context context = c4.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = interval(scenario);
        final CurriculumGroup curriculumGroup =
                EnrolmentTest.findOrCreateCurriculumGroupFor(scenario.getStudentCurricularPlan(), scenario.mandatoryGroup());
        final DegreeModuleToEnrol module = new DegreeModuleToEnrol(curriculumGroup, context, interval);

        final RuleResult result = evaluate(module, scenario.rule(), scenario.getStudentCurricularPlan(), interval,
                CurricularRuleLevel.ENROLMENT_PREFILTER);

        assertTrue(result.isNA());
    }

    @Test
    public void enrolmentBlocked_whenFilteredException_onOtherDegreeCurricularPlan() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        configureExceptionFor(scenario);
        scenario.rule().setFilterExceptions(true);

        final DegreeCurricularPlan otherDegreeCurricularPlan = createDegreeCurricularPlan(scenario.executionYear());
        final CurricularCourse c4FromOtherDegreeCurricularPlan = otherDegreeCurricularPlan.getCurricularCourseByCode("C4");
        final Context context = c4FromOtherDegreeCurricularPlan.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = scenario.executionYear()
                .getChildInterval(context.getCurricularPeriod().getChildOrder(),
                        context.getCurricularPeriod().getAcademicPeriod());
        final CurriculumGroup curriculumGroup =
                EnrolmentTest.findOrCreateCurriculumGroupFor(scenario.getStudentCurricularPlan(), scenario.mandatoryGroup());
        final OptionalDegreeModuleToEnrol module =
                new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, c4FromOtherDegreeCurricularPlan);

        final RuleResult result = evaluate(module, scenario.rule(), scenario.getStudentCurricularPlan(), interval,
                CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenFilteredException_onSameDegreeCurricularPlan() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        configureExceptionFor(scenario);
        scenario.rule().setFilterExceptions(true);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenFilteredException_onSameDegreeCurricularPlan_withFilterStudentDegree() {
        final AnyCurricularCourseScenario scenario = createScenario(null, null);
        configureExceptionFor(scenario);
        scenario.rule().setFilterExceptions(true);
        scenario.rule().setFilterStudentDegree(true);

        final RuleResult result = evaluateEnrolling(scenario, "C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    // ==================== Helpers ====================

    private static Double COURSE_CREDITS() {
        return 6d;
    }

    private static CompetenceCourseLevelType createLevelType() {
        return CompetenceCourseLevelType.create("LVL" + System.currentTimeMillis(),
                new LocalizedString(Locale.getDefault(), "Level"));
    }

    private static AnyCurricularCourseScenario createScenario(final Double minimumCredits, final Double maximumCredits) {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CurricularPeriod period1Y1S = degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER);
        final OptionalCurricularCourse optionalCourse =
                createOptionalCurricularCourse("Optional 1", period1Y1S,
                        executionYear != null ? executionYear.getFirstExecutionPeriod() : null, mandatoryGroup);
        final AnyCurricularCourse rule =
                new AnyCurricularCourse(optionalCourse, null, executionYear, null, minimumCredits, maximumCredits);
        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        return new AnyCurricularCourseScenario(executionYear, degreeCurricularPlan, mandatoryGroup, optionalGroup, optionalCourse,
                rule, curricularPlan);
    }

    private static CurricularCourse course(final AnyCurricularCourseScenario scenario, final String code) {
        return scenario.degreeCurricularPlan().getCurricularCourseByCode(code);
    }

    private static ExecutionInterval interval(final AnyCurricularCourseScenario scenario) {
        final Context context = course(scenario, "C4").getParentContextsSet().iterator().next();
        return scenario.executionYear().getChildInterval(context.getCurricularPeriod().getChildOrder(),
                context.getCurricularPeriod().getAcademicPeriod());
    }

    private static RuleResult evaluateEnrolling(final AnyCurricularCourseScenario scenario, final String courseCode,
            final CurricularRuleLevel curricularRuleLevel) {
        return evaluateEnrolling(scenario, courseCode, scenario.rule(), curricularRuleLevel);
    }

    private static RuleResult evaluateEnrolling(final AnyCurricularCourseScenario scenario, final String courseCode,
            final AnyCurricularCourse rule, final CurricularRuleLevel curricularRuleLevel) {
        final CurricularCourse targetCourse = course(scenario, courseCode);
        final Context context = targetCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = scenario.executionYear()
                .getChildInterval(context.getCurricularPeriod().getChildOrder(),
                        context.getCurricularPeriod().getAcademicPeriod());
        final CurriculumGroup curriculumGroup =
                EnrolmentTest.findOrCreateCurriculumGroupFor(scenario.getStudentCurricularPlan(), scenario.mandatoryGroup());
        final OptionalDegreeModuleToEnrol module =
                new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, targetCourse);
        return evaluate(module, rule, scenario.getStudentCurricularPlan(), interval, curricularRuleLevel);
    }

    private static RuleResult evaluate(final IDegreeModuleToEvaluate module, final AnyCurricularCourse rule,
            final StudentCurricularPlan curricularPlan, final ExecutionInterval interval,
            final CurricularRuleLevel curricularRuleLevel) {
        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");
            final EnrolmentContext enrolmentContext =
                    new EnrolmentContext(curricularPlan, interval, Set.of(module), List.of(), curricularRuleLevel);
            return rule.evaluate(module, enrolmentContext);
        } finally {
            Authenticate.unmock();
        }
    }

    private static void configureExceptionFor(final AnyCurricularCourseScenario scenario) {
        AnyCurricularCourseExceptionsConfiguration.init();
        AnyCurricularCourseExceptionsConfiguration.getInstance().clearCompetenceCourses();
        AnyCurricularCourseExceptionsConfiguration.getInstance()
                .addCompetenceCourse(course(scenario, "C4").getCompetenceCourse());
    }

    private record AnyCurricularCourseScenario(ExecutionYear executionYear, DegreeCurricularPlan degreeCurricularPlan,
                                               CourseGroup mandatoryGroup, CourseGroup optionalGroup,
                                               OptionalCurricularCourse optionalCourse, AnyCurricularCourse rule,
                                               StudentCurricularPlan curricularPlan) {

        private StudentCurricularPlan getStudentCurricularPlan() {
            return curricularPlan;
        }
    }
}