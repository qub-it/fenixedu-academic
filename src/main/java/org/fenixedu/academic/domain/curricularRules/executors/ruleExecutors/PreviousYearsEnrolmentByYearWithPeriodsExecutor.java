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

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.EnrolmentModelConfigEntry;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class PreviousYearsEnrolmentByYearWithPeriodsExecutor extends PreviousYearsEnrolmentByYearExecutor {

    @Override
    protected RuleResult hasAnyCurricularCoursesToEnrolInPreviousYears(final EnrolmentContext enrolmentContext,
            final Map<Integer, Set<CurricularCourse>> coursesToEnrolByYear, final IDegreeModuleToEvaluate toEvaluate) {
        final Collection<ExecutionInterval> intervalsToProcess = getIntervalsToProcess(enrolmentContext);
        final Set<IDegreeModuleToEvaluate> modulesForIntervals =
                enrolmentContext.getAllChildDegreeModulesToEvaluateFor(toEvaluate.getDegreeModule()).stream()
                        .filter(dme -> dme.isLeaf()).filter(dme -> intervalsToProcess.contains(dme.getExecutionInterval()))
                        .collect(Collectors.toSet());

        RuleResult result = RuleResult.createTrue(toEvaluate.getDegreeModule());
        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : modulesForIntervals) {
            if (degreeModuleToEvaluate.getContext() == null) {
                throw new DomainException("error.degreeModuleToEvaluate.has.invalid.context", degreeModuleToEvaluate.getName(),
                        degreeModuleToEvaluate.getExecutionInterval().getQualifiedName());
            }

            if (hasCurricularCoursesToEnrolInPreviousYears(coursesToEnrolByYear,
                    degreeModuleToEvaluate.getContext().getCurricularYear())) {
                if (degreeModuleToEvaluate.isEnroled()) {
                    result = result.and(createImpossibleRuleResult(toEvaluate, degreeModuleToEvaluate));
                } else {
                    result = result.and(createFalseRuleResult(toEvaluate, degreeModuleToEvaluate));
                }
            }
        }

        return result;
    }

    @Override
    protected void collectCurricularCoursesToEnrol(final Map<Integer, Set<CurricularCourse>> result,
            final CourseGroup courseGroup, final EnrolmentContext enrolmentContext,
            final IDegreeModuleToEvaluate sourceToEvaluate) {
        final SortedSet<Context> contexts = getChildCurricularCoursesContextsToEvaluate(courseGroup, enrolmentContext);

        removeApprovedOrEnrolledOrEnrollingOrNotSatifyingCurricularRules(contexts, enrolmentContext, sourceToEvaluate);

        final Collection<ExecutionInterval> executionIntervals = getIntervalsToProcess(enrolmentContext);

        removeAllThatCanBeApprovedInOtherPeriods(contexts, courseGroup, enrolmentContext, executionIntervals);

        addValidCurricularCourses(result, contexts, executionIntervals);
    }

    private void removeAllThatCanBeApprovedInOtherPeriods(final SortedSet<Context> contexts, CourseGroup courseGroup,
            final EnrolmentContext enrolmentContext, Collection<ExecutionInterval> executionIntervals) {
        final double missingCredits = getMissingCreditsToConcludeGroup(courseGroup, enrolmentContext);

        if (canObtainApprovalInOtherCurricularPeriod(missingCredits, contexts, enrolmentContext, executionIntervals)) {
            contexts.removeIf(ctx -> executionIntervals.stream().anyMatch(ei -> ctx.isValid(ei)));
        }
    }

    private static double getMissingCreditsToConcludeGroup(CourseGroup courseGroup, EnrolmentContext enrolmentContext) {
        final CurriculumGroup curriculumGroup = enrolmentContext.getStudentCurricularPlan().findCurriculumGroupFor(courseGroup);

        if (curriculumGroup == null) {
            return courseGroup.getMinEctsCredits(enrolmentContext.getExecutionYear());
        }

        final ExecutionYear executionYear = enrolmentContext.getExecutionYear();
        final double minCredits = curriculumGroup.getDegreeModule().getMinEctsCredits(executionYear);
        final double creditsConcluded = curriculumGroup.getCreditsConcluded(executionYear);
        final double enrolledCredits = curriculumGroup.getEnroledEctsCredits(executionYear);
        final double enrollingCredits = enrolmentContext.getDegreeModulesToEvaluate().stream()
                .filter(dme -> dme.isLeaf() && dme.isEnroling() && dme.getCurriculumGroup().getDegreeModule() == courseGroup)
                .mapToDouble(dme -> dme.getDegreeModule().getMinEctsCredits(executionYear)).sum();

        return minCredits - creditsConcluded - enrolledCredits - enrollingCredits;
    }

    private boolean canObtainApprovalInOtherCurricularPeriod(final double missingCredits, final SortedSet<Context> contexts,
            EnrolmentContext enrolmentContext, Collection<ExecutionInterval> intervals) {
        final double creditsToApproveInOtherPeriods =
                contexts.stream().filter(ctx -> intervals.stream().allMatch(ei -> isContextAfterInterval(ctx, ei)))
                        .map(ctx -> (CurricularCourse) ctx.getChildDegreeModule()).distinct()
                        .mapToDouble(cc -> cc.getMinEctsCredits(enrolmentContext.getExecutionYear())).sum();

        return creditsToApproveInOtherPeriods >= missingCredits;
    }

    private boolean isContextAfterInterval(Context context, ExecutionInterval interval) {
        final ExecutionInterval contextInterval = interval.getExecutionYear()
                .getChildInterval(context.getCurricularPeriod().getChildOrder(),
                        context.getCurricularPeriod().getAcademicPeriod());
        return contextInterval != null && contextInterval.isAfter(interval);
    }

    private void addValidCurricularCourses(final Map<Integer, Set<CurricularCourse>> result, final Set<Context> contexts,
            final Collection<ExecutionInterval> executionIntervals) {
        contexts.stream().filter(ctx -> executionIntervals.stream().anyMatch(ei -> ctx.isValid(ei))).forEach(
                ctx -> result.computeIfAbsent(ctx.getCurricularYear(), (k) -> new HashSet<>())
                        .add((CurricularCourse) ctx.getChildDegreeModule()));
    }

    private static Collection<ExecutionInterval> getIntervalsToProcess(EnrolmentContext enrolmentContext) {
        final AcademicPeriodOrder periodOrder =
                AcademicPeriodOrder.findBy(enrolmentContext.getExecutionPeriod().getAcademicPeriod(),
                        enrolmentContext.getExecutionPeriod().getChildOrder()).orElseThrow();
        final EnrolmentModelConfigEntry modelConfig =
                EnrolmentModelConfigEntry.findFor(enrolmentContext.getStudentCurricularPlan().getDegreeCurricularPlan(),
                        periodOrder).orElseThrow(() -> new IllegalStateException(
                        "Unable to find enrolment model config for academic period: " + periodOrder.getCode()));

        return modelConfig.getIntervalsFrom(enrolmentContext.getExecutionYear());
    }
}