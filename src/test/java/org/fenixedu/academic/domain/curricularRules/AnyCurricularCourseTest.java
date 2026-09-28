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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AnyCurricularCourseTest {

    private static final String COMPETENCE_COURSES_UNIT = "QS>Courses>CC";

    private static final String OTHER_UNIT = "QS>Degrees";

    private static final Double COURSE_CREDITS = 6d;

    private static ExecutionYear executionYear;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CourseGroup mandatoryGroup;
    private static CourseGroup optionalGroup;
    private static OptionalCurricularCourse optionalCourse;
    private static AnyCurricularCourse rule;
    private static StudentCurricularPlan curricularPlan;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
            return null;
        });
    }

    @Before
    public void setup() {
        degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        optionalCourse = createOptionalCurricularCourse("Optional 1", degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER),
                executionYear.getFirstExecutionPeriod(), mandatoryGroup);
    }

    @Test
    public void enrolmentAllowed_withNoRestrictions() {
        createScenario(null, null);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCreditsBelowMinimum() {
        createScenario(7d, null);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void enrolmentAllowed_whenMinimumEqualToCredits() {
        createScenario(COURSE_CREDITS, null);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCreditsAboveMaximum() {
        createScenario(null, 5d);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenMaximumEqualToCredits() {
        createScenario(null, COURSE_CREDITS);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentAllowed_whenCreditsWithinBounds() {
        createScenario(4d, 8d);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentAllowed_whenCourseGroupMatches() {
        createScenario(null, null);
        rule.getCourseGroupsSet().add(optionalGroup);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCourseGroupDoesNotMatch() {
        createScenario(null, null);
        rule.getCourseGroupsSet().add(optionalGroup);

        final RuleResult result = evaluateEnrolling("C3", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_whenDegreeCurricularPlanDoesNotMatch() {
        createScenario(null, null);
        final DegreeCurricularPlan otherDegreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        rule.getDegreeCurricularPlansSet().add(otherDegreeCurricularPlan);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_whenDegreeDoesNotMatch() {
        createScenario(null, null);
        final DegreeCurricularPlan otherDegreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        rule.getDegreesSet().add(otherDegreeCurricularPlan.getDegree());

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_whenDegreeTypeDoesNotMatch() {
        createScenario(null, null);
        rule.getDegreeTypesSet().add(new DegreeType(new LocalizedString(Locale.getDefault(), "Other Degree Type")));

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenCompetenceCourseMatches() {
        createScenario(null, null);
        rule.getCompetenceCoursesSet().add(getCurricularCourse("C4").getCompetenceCourse());

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenCompetenceCourseDoesNotMatch() {
        createScenario(null, null);
        rule.getCompetenceCoursesSet().add(getCurricularCourse("C1").getCompetenceCourse());

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenLevelTypeMatches() {
        createScenario(null, null);
        final CompetenceCourseLevelType levelType = createLevelType();
        rule.getCompetenceCourseLevelTypesSet().add(levelType);
        final ExecutionInterval interval = getExecutionInterval(getCurricularCourse("C4"));
        final CompetenceCourseInformation information =
                getCurricularCourse("C4").getCompetenceCourse().findInformationMostRecentUntil(interval);
        information.setLevelType(levelType);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenLevelTypeDoesNotMatch() {
        createScenario(null, null);
        rule.getCompetenceCourseLevelTypesSet().add(createLevelType());

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenUnitMatches() {
        createScenario(null, null);
        rule.getUnitsSet().add(Unit.findInternalUnitByAcronymPath(COMPETENCE_COURSES_UNIT).orElseThrow());

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenUnitDoesNotMatch() {
        createScenario(null, null);
        rule.getUnitsSet().add(Unit.findInternalUnitByAcronymPath(OTHER_UNIT).orElseThrow());

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentBlocked_withNegation_whenRuleMatches() {
        createScenario(null, null);
        rule.setNegation(true);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_withNegation_whenRuleDoesNotMatch() {
        createScenario(7d, null);
        rule.setNegation(true);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenAlreadyApproved() {
        createScenario(null, null);
        enrol(curricularPlan, executionYear, "C1");
        approve(curricularPlan, executionYear, "C1");

        final RuleResult result = evaluateEnrolling("C1", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void enrolmentBlocked_whenAlreadyEnrolled() {
        createScenario(null, null);
        final Context optionalContext = optionalCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(optionalCourse);
        EnrolmentTest.createOptionalEnrolment(curricularPlan, interval, optionalContext, getCurricularCourse("C4"),
                ADMIN_USERNAME);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentImpossible_whenEnrolledCourseStopsMatchingTheRule() {
        createScenario(null, null);
        final Context optionalContext = optionalCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(optionalCourse);
        EnrolmentTest.createOptionalEnrolment(curricularPlan, interval, optionalContext, getCurricularCourse("C4"),
                ADMIN_USERNAME);
        final OptionalEnrolment optionalEnrolment =
                (OptionalEnrolment) curricularPlan.getEnrolments(getCurricularCourse("C4")).iterator().next();

        rule.getCompetenceCoursesSet().add(getCurricularCourse("C1").getCompetenceCourse());

        final EnroledOptionalEnrolment enrolled = new EnroledOptionalEnrolment(optionalEnrolment, optionalCourse, interval);
        final RuleResult result = evaluate(enrolled, rule, interval, CurricularRuleLevel.ENROLMENT_VERIFICATION_WITH_RULES);

        assertTrue(result.isTrue());
        assertTrue(result.hasAnyImpossibleEnrolment());
    }

    @Test
    public void enrolmentNA_whenRuleDoesNotApply() {
        createScenario(null, null);
        final AnyCurricularCourse otherRule =
                new AnyCurricularCourse(optionalCourse, mandatoryGroup, executionYear, null, null, null);

        final RuleResult result = evaluateEnrolling("C4", otherRule, CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isNA());
        assertFalse(result.isTrue());
    }

    @Test
    public void prefilterEnrolmentBlocked_whenRuleFails() {
        createScenario(7d, null);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_PREFILTER);

        assertTrue(result.isFalse());
    }

    @Test
    public void prefilterEnrolmentNA_whenNonOptionalModule() {
        createScenario(null, null);
        final CurricularCourse c4 = getCurricularCourse("C4");
        final Context context = c4.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(c4);
        final CurriculumGroup curriculumGroup = EnrolmentTest.findOrCreateCurriculumGroupFor(curricularPlan, mandatoryGroup);
        final DegreeModuleToEnrol module = new DegreeModuleToEnrol(curriculumGroup, context, interval);

        final RuleResult result = evaluate(module, rule, interval, CurricularRuleLevel.ENROLMENT_PREFILTER);

        assertTrue(result.isNA());
    }

    @Test
    public void enrolmentBlocked_whenFilteredException_onOtherDegreeCurricularPlan() {
        createScenario(null, null);
        setupExceptionsConfiguration();

        final DegreeCurricularPlan otherDegreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CurricularCourse c4FromOtherDegreeCurricularPlan = otherDegreeCurricularPlan.getCurricularCourseByCode("C4");
        final Context context = c4FromOtherDegreeCurricularPlan.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(c4FromOtherDegreeCurricularPlan);
        final CurriculumGroup curriculumGroup = EnrolmentTest.findOrCreateCurriculumGroupFor(curricularPlan, mandatoryGroup);
        final OptionalDegreeModuleToEnrol module =
                new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, c4FromOtherDegreeCurricularPlan);

        final RuleResult result = evaluate(module, rule, interval, CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    @Test
    public void enrolmentAllowed_whenFilteredException_onSameDegreeCurricularPlan() {
        createScenario(null, null);
        setupExceptionsConfiguration();

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isTrue());
    }

    @Test
    public void enrolmentBlocked_whenFilteredException_onSameDegreeCurricularPlan_withFilterStudentDegree() {
        createScenario(null, null);
        setupExceptionsConfiguration();
        rule.setFilterStudentDegree(true);

        final RuleResult result = evaluateEnrolling("C4", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
    }

    private static CompetenceCourseLevelType createLevelType() {
        return CompetenceCourseLevelType.create("LVL" + System.currentTimeMillis(),
                new LocalizedString(Locale.getDefault(), "Level"));
    }

    private static void createScenario(final Double minimumCredits, final Double maximumCredits) {
        rule = new AnyCurricularCourse(optionalCourse, null, executionYear, null, minimumCredits, maximumCredits);
        curricularPlan = createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
    }

    private static CurricularCourse getCurricularCourse(final String code) {
        return degreeCurricularPlan.getCurricularCourseByCode(code);
    }

    private static ExecutionInterval getExecutionInterval(final CurricularCourse course) {
        final Context context = course.getParentContextsSet().iterator().next();
        return executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(),
                context.getCurricularPeriod().getAcademicPeriod());
    }

    private static RuleResult evaluateEnrolling(final String courseCode, final CurricularRuleLevel curricularRuleLevel) {
        return evaluateEnrolling(courseCode, rule, curricularRuleLevel);
    }

    private static RuleResult evaluateEnrolling(final String courseCode, final AnyCurricularCourse rule,
            final CurricularRuleLevel curricularRuleLevel) {
        final CurricularCourse targetCourse = getCurricularCourse(courseCode);
        final Context context = targetCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(targetCourse);
        final CurriculumGroup curriculumGroup = EnrolmentTest.findOrCreateCurriculumGroupFor(curricularPlan, mandatoryGroup);
        final OptionalDegreeModuleToEnrol module =
                new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, targetCourse);
        return evaluate(module, rule, interval, curricularRuleLevel);
    }

    private static RuleResult evaluate(final IDegreeModuleToEvaluate module, final AnyCurricularCourse rule,
            final ExecutionInterval interval, final CurricularRuleLevel curricularRuleLevel) {
        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");
            final EnrolmentContext enrolmentContext =
                    new EnrolmentContext(curricularPlan, interval, Set.of(module), List.of(), curricularRuleLevel);
            return rule.evaluate(module, enrolmentContext);
        } finally {
            Authenticate.unmock();
        }
    }

    private static void setupExceptionsConfiguration() {
        AnyCurricularCourseExceptionsConfiguration.init();
        AnyCurricularCourseExceptionsConfiguration.getInstance().clearCompetenceCourses();
        AnyCurricularCourseExceptionsConfiguration.getInstance()
                .addCompetenceCourse(getCurricularCourse("C4").getCompetenceCourse());
        rule.setFilterExceptions(true);
    }
}