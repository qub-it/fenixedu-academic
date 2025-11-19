/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.AnyCurricularCourse;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.CurricularRuleLabelFormatter;
import org.fenixedu.bennu.core.domain.Bennu;

import com.google.common.collect.Sets;

public class AnyCurricularCourseExecutor extends CurricularRuleExecutor {

    @Override
    protected RuleResult executeEnrolmentVerificationWithRules(final ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final EnrolmentContext enrolmentContext) {

        final AnyCurricularCourse rule = (AnyCurricularCourse) curricularRule;

        if (!rule.appliesToContext(sourceDegreeModuleToEvaluate.getContext())) {
            return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        final CurricularCourse curricularCourseToEnrol;
        if (sourceDegreeModuleToEvaluate.isEnroling()) {
            final OptionalDegreeModuleToEnrol optionalDegreeModuleToEnrol =
                    (OptionalDegreeModuleToEnrol) sourceDegreeModuleToEvaluate;
            curricularCourseToEnrol = optionalDegreeModuleToEnrol.getCurricularCourse();

            if (isApproved(enrolmentContext, curricularCourseToEnrol) || isEnroled(enrolmentContext, curricularCourseToEnrol)
                    || isApproved(enrolmentContext, optionalDegreeModuleToEnrol.getCurricularCourse())
                    || isEnroled(enrolmentContext, optionalDegreeModuleToEnrol.getCurricularCourse())) {

                return RuleResult.createFalse(sourceDegreeModuleToEvaluate.getDegreeModule(),
                        "curricularRules.ruleExecutors.AnyCurricularCourseExecutor.already.approved.or.enroled",
                        curricularCourseToEnrol.getName(), rule.getDegreeModuleToApplyRule().getName());
            }

        } else if (sourceDegreeModuleToEvaluate.isEnroled()) {
            curricularCourseToEnrol = (CurricularCourse) ((EnroledOptionalEnrolment) sourceDegreeModuleToEvaluate)
                    .getCurriculumModule().getDegreeModule();
        } else {
            throw new DomainException(
                    "error.curricularRules.executors.ruleExecutors.AnyCurricularCourseExecutor.unexpected.degree.module.to.evaluate");
        }

        final ExecutionInterval executionInterval = enrolmentContext.getExecutionPeriod();
        final Degree degree = curricularCourseToEnrol.getDegree();
        final DegreeCurricularPlan degreeCurricularPlan = curricularCourseToEnrol.getDegreeCurricularPlan();
        final Set<CourseGroup> allParentCourseGroups =
                curricularCourseToEnrol.getParentContexts(executionInterval).stream().map(ctx -> ctx.getParentCourseGroup())
                        .collect(Collectors.toSet());

        boolean result = true;

        result &= matchesMinCredits(rule, curricularCourseToEnrol, executionInterval);
        result &= matchesMaxCredits(rule, curricularCourseToEnrol, executionInterval);
        result &= matchesCourseGroupDcpDegreesAndDegreeTypes(rule, degree, degreeCurricularPlan, allParentCourseGroups);
        result &= matchesCompetenceCoursesAndLevels(rule, curricularCourseToEnrol, executionInterval);
        result &= matchesUnits(rule, curricularCourseToEnrol, executionInterval);
        result &= matchesExceptionConfigurations(rule, sourceDegreeModuleToEvaluate, enrolmentContext);

        if (Boolean.TRUE.equals(rule.getNegation())) {
            result = !result;
        }

        if (result) {
            return RuleResult.createTrue(curricularCourseToEnrol);
        } else {
            if (sourceDegreeModuleToEvaluate.isEnroled()) {
                return RuleResult.createImpossibleWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(),
                        CurricularRuleLabelFormatter.getLabel(rule));
            } else {
                return RuleResult.createFalseWithLiteralMessage(sourceDegreeModuleToEvaluate.getDegreeModule(),
                        CurricularRuleLabelFormatter.getLabel(rule));
            }
        }

    }

    @Override
    protected RuleResult executeEnrolmentPrefilter(ICurricularRule curricularRule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {

        if (sourceDegreeModuleToEvaluate instanceof OptionalDegreeModuleToEnrol) {
            final RuleResult result =
                    executeEnrolmentVerificationWithRules(curricularRule, sourceDegreeModuleToEvaluate, enrolmentContext);
            return result.isTrue() ? result : RuleResult.createFalse(sourceDegreeModuleToEvaluate.getDegreeModule());
        }

        return RuleResult.createNA(sourceDegreeModuleToEvaluate.getDegreeModule());
    }

    private boolean matchesMinCredits(final AnyCurricularCourse rule, final CurricularCourse curricularCourseToEnrol,
            final ExecutionInterval executionInterval) {
        return rule.hasMinimumCredits() ? rule.getMinimumCredits() <= curricularCourseToEnrol
                .getEctsCredits(executionInterval) : true;
    }

    private boolean matchesMaxCredits(final AnyCurricularCourse rule, final CurricularCourse curricularCourseToEnrol,
            final ExecutionInterval executionInterval) {
        return rule.hasMaximumCredits() ? rule.getMaximumCredits() >= curricularCourseToEnrol
                .getEctsCredits(executionInterval) : true;
    }

    private boolean matchesCompetenceCoursesAndLevels(AnyCurricularCourse rule, CurricularCourse courseToEnrol,
            ExecutionInterval executionInterval) {

        final CompetenceCourse competenceCourse = courseToEnrol.getCompetenceCourse();
        if (!rule.getCompetenceCoursesSet().isEmpty()) {
            return rule.getCompetenceCoursesSet().contains(competenceCourse);
        }

        final CompetenceCourseLevelType levelType =
                competenceCourse.findInformationMostRecentUntil(executionInterval).getLevelType();
        return rule.getCompetenceCourseLevelTypesSet().isEmpty()
                || (levelType != null && rule.getCompetenceCourseLevelTypesSet().contains(levelType));
    }

    private boolean matchesCourseGroupDcpDegreesAndDegreeTypes(AnyCurricularCourse rule, Degree degree,
            DegreeCurricularPlan degreeCurricularPlan, Set<CourseGroup> courseGroups) {
        if (!rule.getCourseGroupsSet().isEmpty()) {

            final Set<CourseGroup> ancestorGroups = Stream.concat(courseGroups.stream(),
                    courseGroups.stream().flatMap(cg -> cg.getAllParentCourseGroups().stream())).collect(Collectors.toSet());

            return !Sets.intersection(rule.getCourseGroupsSet(), ancestorGroups).isEmpty();
        }
        if (!rule.getDegreeCurricularPlansSet().isEmpty()) {
            return rule.getDegreeCurricularPlansSet().contains(degreeCurricularPlan);
        }
        if (!rule.getDegreesSet().isEmpty()) {
            return rule.getDegreesSet().contains(degree);
        }

        return rule.getDegreeTypesSet().isEmpty() || rule.getDegreeTypesSet().contains(degree.getDegreeType());
    }

    private boolean matchesUnits(AnyCurricularCourse rule, CurricularCourse courseToEnrol, ExecutionInterval executionInterval) {
        if (rule.getUnitsSet().isEmpty()) {
            return true;
        }

        return courseToEnrol.getCompetenceCourse().getParentUnits(u -> rule.getUnitsSet().contains(u), executionInterval)
                .findAny().isPresent();
    }

    static private CurricularCourse getCurricularCourseFromOptional(final IDegreeModuleToEvaluate input) {
        CurricularCourse result = null;

        if (input.isEnroling()) {
            final OptionalDegreeModuleToEnrol toEnrol = (OptionalDegreeModuleToEnrol) input;
            result = toEnrol.getCurricularCourse();

        } else if (input.isEnroled()) {
            final EnroledOptionalEnrolment enroled = (EnroledOptionalEnrolment) input;
            result = (CurricularCourse) enroled.getCurriculumModule().getDegreeModule();
        }

        return result;
    }

    private boolean verifyOptionalsConfiguration(final AnyCurricularCourse rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol) {

        final Boolean optionalsConfiguration = rule.getOptionalsConfiguration();

        if (optionalsConfiguration != null) {

            boolean useOptionals = optionalsConfiguration && hasOneOptionalParentCourseGroup(curricularCourseToEnrol,
                    sourceDegreeModuleToEvaluate.getExecutionInterval());

            boolean useMandatory = !optionalsConfiguration && !hasOneOptionalParentCourseGroup(curricularCourseToEnrol,
                    sourceDegreeModuleToEvaluate.getExecutionInterval());

            return (useOptionals || useMandatory);
        }
        return true;

    }

    private static boolean hasOneOptionalParentCourseGroup(final CurricularCourse curricularCourseToEnrol,
            ExecutionInterval executionInterval) {
        return curricularCourseToEnrol.getParentContextsByExecutionYear(executionInterval.getExecutionYear()).stream()
                .anyMatch(ctx -> ctx.getParentCourseGroup().getIsOptional());
    }

    private boolean verifyCompetenceCourses(final AnyCurricularCourse rule,
            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol,
            final EnrolmentContext enrolmentContext) {

        final CompetenceCourse competenceCourse = curricularCourseToEnrol.getCompetenceCourse();
        final DegreeCurricularPlan chosenDegreeCurricularPlan = curricularCourseToEnrol.getDegreeCurricularPlan();
        final StudentCurricularPlan studentCurricularPlan = enrolmentContext.getStudentCurricularPlan();

        return !isException(competenceCourse, chosenDegreeCurricularPlan, studentCurricularPlan);
    }

    public static boolean isException(final CompetenceCourse competenceCourse,
            final DegreeCurricularPlan chosenDegreeCurricularPlan, final StudentCurricularPlan studentCurricularPlan) {

        // can only be considered an exception if the user chose other DCP than his own
        return chosenDegreeCurricularPlan != studentCurricularPlan.getDegreeCurricularPlan() && Bennu.getInstance()
                .getAnyCurricularCourseExceptionsConfiguration().getCompetenceCoursesSet().contains(competenceCourse);
    }

    protected boolean matchesExceptionConfigurations(AnyCurricularCourse rule,
            IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        if (rule.getFilterExceptions() != null && rule.getFilterExceptions()) {
            final CurricularCourse curricularCourseToEnrolFromOptional =
                    getCurricularCourseFromOptional(sourceDegreeModuleToEvaluate);
            if (curricularCourseToEnrolFromOptional != null) {
                return verifyOptionalsConfiguration(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrolFromOptional)
                        && verifyCompetenceCourses(rule, sourceDegreeModuleToEvaluate, curricularCourseToEnrolFromOptional,
                        enrolmentContext);
            }

            return false;
        }

        return true;
    }

    //    //Para sair se entretanto mover a classe AbstractCurricularRuleExecutorLogic para o academic
    //    static private RuleResult createResultFalse(final AnyCurricularCourse rule,
    //            final IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, final CurricularCourse curricularCourseToEnrol,
    //            final String messageKey) {
    //
    //        final String message = BundleUtil.getString(Bundle.APPLICATION, messageKey, curricularCourseToEnrol.getName(),
    //                rule.getDegreeModuleToApplyRule().getName());
    //
    //        return sourceDegreeModuleToEvaluate.isEnroled() ? RuleResult.createImpossibleWithLiteralMessage(
    //                sourceDegreeModuleToEvaluate.getDegreeModule(), message) : RuleResult.createFalseWithLiteralMessage(
    //                sourceDegreeModuleToEvaluate.getDegreeModule(), message);
    //    }
    //
    //    public RuleResult createFalseConfiguration(final DegreeModule degreeModule) {
    //        return createFalseConfiguration(degreeModule, StringUtils.EMPTY, getCurricularRuleLabelKey());
    //    }

    //    static public RuleResult createFalseConfiguration(final DegreeModule degreeModule, final String prefix,
    //            final String curricularRuleLabelKey) {
    //
    //        final String literalMessage =
    //                prefix + BundleUtil.getString(Bundle.APPLICATION, "curricularRules.ruleExecutors.logic.unavailable",
    //                        BundleUtil.getString(Bundle.BOLONHA, curricularRuleLabelKey));
    //        return RuleResult.createFalseWithLiteralMessage(degreeModule, literalMessage);
    //    }

    //    protected String getCurricularRuleLabelKey() {
    //        return "label.anyCurricularCourse";
    //    }

}
