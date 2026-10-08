package org.fenixedu.academic.domain.curricularRules;

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

    private static final String TARGET_COURSE = "C4";

    private static ExecutionYear executionYear;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CourseGroup mandatoryGroup;
    private static CourseGroup optionalGroup;
    private static OptionalCurricularCourse optionalCourse;
    private static AnyCurricularCourse rule;
    private static StudentCurricularPlan studentCurricularPlan;

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
        degreeCurricularPlan = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup =
                ConclusionRulesTestUtil.getChildGroup(degreeCurricularPlan.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        mandatoryGroup = ConclusionRulesTestUtil.getChildGroup(cycleGroup, ConclusionRulesTestUtil.MANDATORY_GROUP);
        optionalGroup = ConclusionRulesTestUtil.getChildGroup(cycleGroup, ConclusionRulesTestUtil.OPTIONAL_GROUP);
        optionalCourse = ConclusionRulesTestUtil.createOptionalCurricularCourse("Optional 1",
                degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryGroup);
        studentCurricularPlan =
                ConclusionRulesTestUtil.createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        setupRule(null, null);
    }

    @Test
    public void succeeds_withNoRestrictions() {
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byCreditsBelowMinimum() {
        setupRule(7d, null);

        final RuleResult result = evaluateEnrolment();

        assertTrue(result.isFalse());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void succeeds_withMinimumEqualToCredits() {
        setupRule(COURSE_CREDITS, null);
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byCreditsAboveMaximum() {
        setupRule(null, 5d);
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void succeeds_withMaximumEqualToCredits() {
        setupRule(null, COURSE_CREDITS);
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void succeeds_withCreditsWithinBounds() {
        setupRule(4d, 8d);
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void succeeds_byCourseGroupMatch() {
        rule.getCourseGroupsSet().add(optionalGroup);
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byCourseGroupMismatch() {
        rule.getCourseGroupsSet().add(optionalGroup);
        assertTrue(evaluateEnrolment("C3", CurricularRuleLevel.ENROLMENT_WITH_RULES).isFalse());
    }

    @Test
    public void fails_byDegreeCurricularPlanMismatch() {
        final DegreeCurricularPlan otherDegreeCurricularPlan = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        rule.getDegreeCurricularPlansSet().add(otherDegreeCurricularPlan);
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void fails_byDegreeMismatch() {
        final DegreeCurricularPlan otherDegreeCurricularPlan = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        rule.getDegreesSet().add(otherDegreeCurricularPlan.getDegree());
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void fails_byDegreeTypeMismatch() {
        rule.getDegreeTypesSet().add(new DegreeType(new LocalizedString(Locale.getDefault(), "Other Degree Type")));
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void succeeds_byCompetenceCourseMatch() {
        rule.getCompetenceCoursesSet().add(getCurricularCourse(TARGET_COURSE).getCompetenceCourse());
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byCompetenceCourseMismatch() {
        rule.getCompetenceCoursesSet().add(getCurricularCourse("C1").getCompetenceCourse());
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void succeeds_byLevelTypeMatch() {
        final CompetenceCourseLevelType levelType = createLevelType();
        rule.getCompetenceCourseLevelTypesSet().add(levelType);
        final ExecutionInterval interval = getExecutionInterval(getCurricularCourse(TARGET_COURSE));
        final CompetenceCourseInformation information =
                getCurricularCourse(TARGET_COURSE).getCompetenceCourse().findInformationMostRecentUntil(interval);
        information.setLevelType(levelType);
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byLevelTypeMismatch() {
        rule.getCompetenceCourseLevelTypesSet().add(createLevelType());
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void succeeds_byUnitMatch() {
        rule.getUnitsSet().add(Unit.findInternalUnitByAcronymPath(COMPETENCE_COURSES_UNIT).orElseThrow());
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byUnitMismatch() {
        rule.getUnitsSet().add(Unit.findInternalUnitByAcronymPath(OTHER_UNIT).orElseThrow());
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void fails_byNegatedRuleMatch() {
        rule.setNegation(true);
        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void succeeds_byNegatedRuleMismatch() {
        setupRule(7d, null);
        rule.setNegation(true);
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byCourseAlreadyApproved() {
        ConclusionRulesTestUtil.enrol(studentCurricularPlan, executionYear, "C1");
        ConclusionRulesTestUtil.approve(studentCurricularPlan, executionYear, "C1");

        final RuleResult result = evaluateEnrolment("C1", CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isFalse());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    public void fails_byCourseAlreadyEnrolled() {
        final Context optionalContext = optionalCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(optionalCourse);
        EnrolmentTest.createOptionalEnrolment(studentCurricularPlan, interval, optionalContext,
                getCurricularCourse(TARGET_COURSE), ConclusionRulesTestUtil.ADMIN_USERNAME);

        assertTrue(evaluateEnrolment().isFalse());
    }

    @Test
    public void succeeds_whenEnrolledCourseStopsMatchingTheRule() {
        final Context optionalContext = optionalCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(optionalCourse);
        EnrolmentTest.createOptionalEnrolment(studentCurricularPlan, interval, optionalContext,
                getCurricularCourse(TARGET_COURSE), ConclusionRulesTestUtil.ADMIN_USERNAME);
        final OptionalEnrolment optionalEnrolment =
                (OptionalEnrolment) studentCurricularPlan.getEnrolments(getCurricularCourse(TARGET_COURSE)).iterator().next();

        rule.getCompetenceCoursesSet().add(getCurricularCourse("C1").getCompetenceCourse());

        final EnroledOptionalEnrolment enrolled = new EnroledOptionalEnrolment(optionalEnrolment, optionalCourse, interval);
        final RuleResult result = evaluate(enrolled, rule, interval, CurricularRuleLevel.ENROLMENT_VERIFICATION_WITH_RULES);

        assertTrue(result.isTrue());
        assertTrue(result.hasAnyImpossibleEnrolment());
    }

    @Test
    public void fails_whenRuleScopedToOtherCourse() {
        final AnyCurricularCourse otherRule =
                new AnyCurricularCourse(optionalCourse, mandatoryGroup, executionYear, null, null, null);

        final RuleResult result = evaluateEnrolment("C4", otherRule, CurricularRuleLevel.ENROLMENT_WITH_RULES);

        assertTrue(result.isNA());
        assertFalse(result.isTrue());
    }

    @Test
    public void fails_byCreditsBelowMinimum_inPrefilter() {
        setupRule(7d, null);
        assertTrue(evaluateEnrolment("C4", CurricularRuleLevel.ENROLMENT_PREFILTER).isFalse());
    }

    @Test
    public void fails_whenModuleIsMandatory_inPrefilter() {
        final CurricularCourse c4 = getCurricularCourse(TARGET_COURSE);
        final Context context = c4.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(c4);
        final CurriculumGroup curriculumGroup =
                EnrolmentTest.findOrCreateCurriculumGroupFor(studentCurricularPlan, mandatoryGroup);
        final DegreeModuleToEnrol module = new DegreeModuleToEnrol(curriculumGroup, context, interval);

        assertTrue(evaluate(module, rule, interval, CurricularRuleLevel.ENROLMENT_PREFILTER).isNA());
    }

    @Test
    public void fails_byFilteredExceptionOnOtherDegreeCurricularPlan() {
        setupExceptionsConfiguration();

        final DegreeCurricularPlan otherDegreeCurricularPlan = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        final CurricularCourse c4FromOtherDegreeCurricularPlan = otherDegreeCurricularPlan.getCurricularCourseByCode("C4");
        final Context context = c4FromOtherDegreeCurricularPlan.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(c4FromOtherDegreeCurricularPlan);
        final CurriculumGroup curriculumGroup =
                EnrolmentTest.findOrCreateCurriculumGroupFor(studentCurricularPlan, mandatoryGroup);
        final OptionalDegreeModuleToEnrol module =
                new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, c4FromOtherDegreeCurricularPlan);

        assertTrue(evaluate(module, rule, interval, CurricularRuleLevel.ENROLMENT_WITH_RULES).isFalse());
    }

    @Test
    public void succeeds_byFilteredExceptionOnSameDegreeCurricularPlan() {
        setupExceptionsConfiguration();
        assertTrue(evaluateEnrolment().isTrue());
    }

    @Test
    public void fails_byFilteredExceptionWithStudentDegreeFilter() {
        setupExceptionsConfiguration();
        rule.setFilterStudentDegree(true);
        assertTrue(evaluateEnrolment().isFalse());
    }

    private static CompetenceCourseLevelType createLevelType() {
        return CompetenceCourseLevelType.create("LVL" + System.currentTimeMillis(),
                new LocalizedString(Locale.getDefault(), "Level"));
    }

    private static void setupRule(final Double minimumCredits, final Double maximumCredits) {
        rule = new AnyCurricularCourse(optionalCourse, null, executionYear, null, minimumCredits, maximumCredits);
    }

    private static CurricularCourse getCurricularCourse(final String code) {
        return degreeCurricularPlan.getCurricularCourseByCode(code);
    }

    private static ExecutionInterval getExecutionInterval(final CurricularCourse course) {
        final Context context = course.getParentContextsSet().iterator().next();
        return executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(),
                context.getCurricularPeriod().getAcademicPeriod());
    }

    private static RuleResult evaluateEnrolment() {
        return evaluateEnrolment(TARGET_COURSE, CurricularRuleLevel.ENROLMENT_WITH_RULES);
    }

    private static RuleResult evaluateEnrolment(final String courseCode, final CurricularRuleLevel curricularRuleLevel) {
        return evaluateEnrolment(courseCode, rule, curricularRuleLevel);
    }

    private static RuleResult evaluateEnrolment(final String courseCode, final AnyCurricularCourse rule,
            final CurricularRuleLevel curricularRuleLevel) {
        final CurricularCourse targetCourse = getCurricularCourse(courseCode);
        final Context context = targetCourse.getParentContextsSet().iterator().next();
        final ExecutionInterval interval = getExecutionInterval(targetCourse);
        final CurriculumGroup curriculumGroup =
                EnrolmentTest.findOrCreateCurriculumGroupFor(studentCurricularPlan, mandatoryGroup);
        final OptionalDegreeModuleToEnrol module =
                new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, targetCourse);
        return evaluate(module, rule, interval, curricularRuleLevel);
    }

    private static RuleResult evaluate(final IDegreeModuleToEvaluate module, final AnyCurricularCourse rule,
            final ExecutionInterval interval, final CurricularRuleLevel curricularRuleLevel) {
        try {
            Authenticate.mock(User.findByUsername(ConclusionRulesTestUtil.ADMIN_USERNAME), "none");
            final EnrolmentContext enrolmentContext =
                    new EnrolmentContext(studentCurricularPlan, interval, Set.of(module), List.of(), curricularRuleLevel);
            return rule.evaluate(module, enrolmentContext);
        } finally {
            Authenticate.unmock();
        }
    }

    private static void setupExceptionsConfiguration() {
        AnyCurricularCourseExceptionsConfiguration.init();
        AnyCurricularCourseExceptionsConfiguration.getInstance().clearCompetenceCourses();
        AnyCurricularCourseExceptionsConfiguration.getInstance()
                .addCompetenceCourse(getCurricularCourse(TARGET_COURSE).getCompetenceCourse());
        rule.setFilterExceptions(true);
    }
}