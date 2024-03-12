package org.fenixedu.academic.domain.curricularRules;

import static org.fenixedu.academic.util.Bundle.APPLICATION;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class DegreeModuleSetApprovalRule extends DegreeModuleSetApprovalRule_Base implements ConclusionRule {

    private static CurricularRuleExecutor RULE_EXECUTOR = new CurricularRuleExecutor() {

        @Override
        protected RuleResult executeEnrolmentVerificationWithRules(ICurricularRule curricularRule,
                IDegreeModuleToEvaluate toEvaluate, EnrolmentContext enrolmentContext) {
            final DegreeModuleSetApprovalRule rule = (DegreeModuleSetApprovalRule) curricularRule;
            final DegreeModule moduleToEvaluate = toEvaluate.getDegreeModule();

            if (!rule.getShowWarningOnEnrolment()) {
                return RuleResult.createNA(moduleToEvaluate);
            }

            final StudentCurricularPlan curricularPlan = enrolmentContext.getStudentCurricularPlan();
            final ExecutionYear executionYear = enrolmentContext.getExecutionYear();
            final CourseGroup groupForApprovals = rule.getCourseGroupForApprovals();

            final Set<DegreeModule> modulesMissing = rule.getDegreeModulesToApproveFor(curricularPlan).stream()
                    .filter(dm -> !isConcluded(curricularPlan, dm, groupForApprovals, executionYear))
                    .filter(dm -> !canConclude(curricularPlan, dm, groupForApprovals, executionYear))
                    .filter(dm -> !isEnrolling(enrolmentContext, dm)).collect(Collectors.toSet());

            if (modulesMissing.isEmpty()) {
                return RuleResult.createTrue(moduleToEvaluate);
            }

            final DegreeModule moduleToApplyRule = rule.getDegreeModuleToApplyRule();

            if (groupForApprovals == null || moduleToApplyRule == groupForApprovals) {
                return RuleResult.createWarning(moduleToEvaluate, "label.DegreeModuleSetApprovalRule.conclusion.warning",
                        moduleToApplyRule.getName(), buildModulesToApproveText(modulesMissing));
            }

            return RuleResult.createWarning(moduleToEvaluate, "label.DegreeModuleSetApprovalRule.conclusion.warning.with.group",
                    moduleToApplyRule.getName(), buildModulesToApproveText(modulesMissing), groupForApprovals.getName());
        }
    };

    protected DegreeModuleSetApprovalRule() {
        super();
        setCurricularRuleType(CurricularRuleType.CUSTOM);
    }

    public DegreeModuleSetApprovalRule(DegreeModule degreeModule, CourseGroup courseGroup, ExecutionInterval begin,
            ExecutionInterval end, boolean showWarningOnEnrolment, boolean validateEnroledModulesOnly, CourseGroup approvalsGroup,
            Collection<DegreeModule> degreeModulesToApprove) {
        this();
        super.init(degreeModule, courseGroup, begin, end);
        edit(courseGroup, begin, end, showWarningOnEnrolment, validateEnroledModulesOnly, approvalsGroup, degreeModulesToApprove);
    }

    public void edit(CourseGroup courseGroup, ExecutionInterval begin, ExecutionInterval end, boolean showWarningOnEnrolment,
            boolean validateEnroledModulesOnly, CourseGroup approvalsGroup, Collection<DegreeModule> degreeModulesToApprove) {
        super.edit(begin, end);
        super.setContextCourseGroup(courseGroup);
        super.setShowWarningOnEnrolment(showWarningOnEnrolment);
        super.setValidateEnroledModulesOnly(validateEnroledModulesOnly);
        super.setCourseGroupForApprovals(approvalsGroup);
        super.getDegreeModulesToApproveSet().clear();
        super.getDegreeModulesToApproveSet().addAll(degreeModulesToApprove);

        checkRules();
    }

    private void checkRules() {
        if (getDegreeModulesToApproveSet().isEmpty()) {
            throw new DomainException("error.curricularRules.DegreeModuleSetApprovalRule.degreeModulesToApprove.is.required");
        }

        if (getCourseGroupForApprovals() != null) {
            final Stream<DegreeModule> parentGroups = getDegreeModulesToApproveSet().stream()
                    .flatMap(dm -> dm.getParentContextsSet().stream().map(ctx -> ctx.getParentCourseGroup()));
            if (parentGroups.noneMatch(cg -> cg == getCourseGroupForApprovals())) {
                throw new DomainException(
                        "error.curricularRules.DegreeModuleSetApprovalRule.degreeModulesToApprove.must.belong.to.same.group");
            }
        }

    }

    public List<GenericPair<Object, Boolean>> getLabel() {
        final String key = "label.DegreeModuleSetApprovalRule.approval.in" + (getValidateEnroledModulesOnly() ? ".enroled" : "");
        return List.of(new GenericPair<Object, Boolean>(
                BundleUtil.getString(APPLICATION, key, buildModulesToApproveText(getDegreeModulesToApproveSet())), false));
    }

    private static String buildModulesToApproveText(final Collection<DegreeModule> degreeModules) {
        return degreeModules.stream()
                .map(dm -> (StringUtils.isNotBlank(dm.getCode()) ? dm.getCode() + " - " : "") + dm.getNameI18N().getContent())
                .sorted().collect(Collectors.joining("; "));
    }

    @Override
    protected void removeOwnParameters() {
        super.setCourseGroupForApprovals(null);
        super.getDegreeModulesToApproveSet().clear();
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate toEvaluate, EnrolmentContext enrolmentContext) {
        return RULE_EXECUTOR.execute(this, toEvaluate, enrolmentContext);
    }

    @Override
    public boolean isConcluded(CurriculumGroup group, ExecutionYear executionYear) {
        return getDegreeModulesToApproveFor(group.getStudentCurricularPlan()).stream()
                .allMatch(dm -> isConcluded(group.getStudentCurricularPlan(), dm, getCourseGroupForApprovals(), executionYear));
    }

    @Override
    public boolean canConclude(CurriculumGroup group, ExecutionYear executionYear) {
        return getDegreeModulesToApproveFor(group.getStudentCurricularPlan()).stream()
                .filter(dm -> !isConcluded(group.getStudentCurricularPlan(), dm, getCourseGroupForApprovals(), executionYear))
                .allMatch(dm -> canConclude(group.getStudentCurricularPlan(), dm, getCourseGroupForApprovals(), executionYear));
    }

    public Collection<DegreeModule> getDegreeModulesToApproveFor(final StudentCurricularPlan curricularPlan) {
        if (!getValidateEnroledModulesOnly()) {
            return getDegreeModulesToApproveSet();
        }

        final Set<DegreeModule> result = new HashSet<>();

        getDegreeModulesToApproveSet().stream().filter(d -> d.isCourseGroup()).map(CourseGroup.class::cast)
                .filter(cg -> curricularPlan.findCurriculumGroupFor(cg) != null).forEach(cg -> result.add(cg));

        getDegreeModulesToApproveSet().stream().filter(d -> d.isLeaf()).map(CurricularCourse.class::cast).forEach(cc -> {
            curricularPlan.getEnrolmentStream().filter(e -> e.hasDegreeModule(cc))
                    .filter(e -> !e.getCurriculumGroup().isNoCourseGroupCurriculumGroup())
                    .filter(e -> getCourseGroupForApprovals() == null
                            || e.getCurriculumGroup().getDegreeModule() == getCourseGroupForApprovals())
                    .forEach(e -> result.add(cc));
        });

        return result;
    }

    private static boolean isConcluded(StudentCurricularPlan curricularPlan, DegreeModule degreeModule, CourseGroup parentGroup,
            ExecutionYear executionYear) {
        if (degreeModule.isCourseGroup()) {
            final CurriculumGroup curriculumGroup = curricularPlan.findCurriculumGroupFor((CourseGroup) degreeModule);
            return curriculumGroup != null && curriculumGroup.isConcluded(executionYear).value();
        }

        final CurricularCourse curricularCourse = (CurricularCourse) degreeModule;
        final CurriculumLine line = curricularPlan.getApprovedCurriculumLines().stream()
                .filter(l -> matchesCourse(l, curricularCourse)).findAny().orElse(null);
        if (line == null) {
            //credits dismissal with no enrol courses
            return curricularPlan.isApproved(curricularCourse, null);
        }

        if (line.getCurriculumGroup().isNoCourseGroupCurriculumGroup()) {
            return false;
        }

        return (parentGroup == null || line.getCurriculumGroup().getDegreeModule() == parentGroup)
                && line.isConcluded(executionYear).value();
    }

    private static boolean canConclude(StudentCurricularPlan curricularPlan, DegreeModule degreeModule, CourseGroup parentGroup,
            ExecutionYear executionYear) {
        if (degreeModule.isCourseGroup()) {
            final CurriculumGroup curriculumGroup = curricularPlan.findCurriculumGroupFor((CourseGroup) degreeModule);
            return curriculumGroup != null && curriculumGroup.canConclude(executionYear);
        }

        final CurricularCourse curricularCourse = (CurricularCourse) degreeModule;
        return curricularPlan.getEnrolmentsByExecutionYear(executionYear).stream().filter(e -> matchesCourse(e, curricularCourse))
                .filter(e -> !e.getCurriculumGroup().isNoCourseGroupCurriculumGroup())
                .filter(e -> parentGroup == null || e.getCurriculumGroup().getDegreeModule() == parentGroup)
                .anyMatch(e -> e.canConclude(executionYear));
    }

    private static boolean matchesCourse(CurriculumLine line, CurricularCourse course) {
        if (line.getDegreeModule() == null) {
            return false;
        }

        if (line.hasDegreeModule(course)) {
            return true;
        }

        final CompetenceCourse competenceCourse = course.getCompetenceCourse();
        return competenceCourse != null && line.getCurricularCourse().getCompetenceCourse() == competenceCourse;
    }

}
