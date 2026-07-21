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
package org.fenixedu.academic.domain.studentCurriculum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.ConclusionRule;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.CurricularRule;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleType;
import org.fenixedu.academic.domain.curricularRules.EnrolmentModel;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnroledCurriculumModuleWrapper;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.predicates.AndPredicate;
import org.fenixedu.academic.util.predicates.ResultCollection;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurriculumGroup extends CurriculumGroup_Base {

    private static final Logger logger = LoggerFactory.getLogger(CurriculumGroup.class);

    static final public Comparator<CurriculumGroup> COMPARATOR_BY_CHILD_ORDER_AND_ID = new Comparator<CurriculumGroup>() {
        @Override
        public int compare(CurriculumGroup o1, CurriculumGroup o2) {
            int result = o1.getChildOrder().compareTo(o2.getChildOrder());
            return (result != 0) ? result : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected CurriculumGroup() {
        super();
    }

    public CurriculumGroup(final CurriculumGroup curriculumGroup, final CourseGroup courseGroup) {
        this();
        init(curriculumGroup, courseGroup);
    }

    protected void init(final CurriculumGroup curriculumGroup, final CourseGroup courseGroup) {
        if (courseGroup == null || curriculumGroup == null) {
            throw new DomainException("error.studentCurriculum.curriculumGroup.courseGroup.cannot.be.null");
        }
        checkInitConstraints(curriculumGroup, courseGroup);
        setDegreeModule(courseGroup);
        setCurriculumGroup(curriculumGroup);
    }

    protected void checkInitConstraints(final CurriculumGroup parent, final CourseGroup courseGroup) {
        if (parent.getRootCurriculumGroup().hasCourseGroup(courseGroup)) {
            throw new DomainException("error.studentCurriculum.CurriculumGroup.duplicate.courseGroup", courseGroup.getName());
        }

        if (courseGroup.isBranchCourseGroup()) {
            final CycleCurriculumGroup cycle = parent.getParentCycleCurriculumGroup();
            if (cycle != null && cycle.getBranchCurriculumGroups().stream()
                    .anyMatch(cg -> cg.getDegreeModule().getBranchType() == courseGroup.getBranchType())) {
                throw new DomainException("error.BranchCurriculumGroup.parent.cycle.cannot.have.another.branch.with.same.type");
            }
        }
    }

    protected void checkParameters(CourseGroup courseGroup, ExecutionInterval executionInterval) {
        if (courseGroup == null) {
            throw new DomainException("error.studentCurriculum.curriculumGroup.courseGroup.cannot.be.null");
        }
        if (executionInterval == null) {
            throw new DomainException("error.studentCurriculum.curriculumGroup.executionPeriod.cannot.be.null");
        }
    }

    public CurriculumGroup(CurriculumGroup parentCurriculumGroup, CourseGroup courseGroup, ExecutionInterval executionSemester) {
        super();
        init(parentCurriculumGroup, courseGroup, executionSemester);
    }

    protected void init(final CurriculumGroup curriculumGroup, final CourseGroup courseGroup,
            final ExecutionInterval executionInterval) {

        checkInitConstraints(curriculumGroup, courseGroup);
        checkParameters(curriculumGroup, courseGroup, executionInterval);

        setDegreeModule(courseGroup);
        setCurriculumGroup(curriculumGroup);
        addChildCurriculumGroups(courseGroup, executionInterval);
    }

    private void checkParameters(CurriculumGroup parentCurriculumGroup, CourseGroup courseGroup,
            ExecutionInterval executionInterval) {

        if (parentCurriculumGroup == null) {
            throw new DomainException("error.studentCurriculum.curriculumGroup.parentCurriculumGroup.cannot.be.null");
        }

        checkParameters(courseGroup, executionInterval);
    }

    protected void addChildCurriculumGroups(final CourseGroup courseGroup, final ExecutionInterval executionInterval) {
        if (!canCreateGroupOrChilds(courseGroup, executionInterval)) {
            return;
        }

        for (final CourseGroup iter : courseGroup.getNotOptionalChildCourseGroups(executionInterval)) {
            if (!canCreateGroupOrChilds(iter, executionInterval)) {
                continue;
            }

            CurriculumGroupFactory.createGroup(this, iter, executionInterval);
        }
    }

    private boolean canCreateGroupOrChilds(final CourseGroup courseGroup, final ExecutionInterval executionInterval) {
        return courseGroup.getCurricularRules(executionInterval).stream()
                .noneMatch(CurricularRule::isRulePreventingAutomaticEnrolment);
    }

    @Override
    final public boolean isLeaf() {
        return false;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getCurriculumModulesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                    "error.studentCurriculum.CurriculumGroup.notEmptyCurriculumGroupModules", getName().getContent()));
        }
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    @Override
    public void deleteRecursive() {
        for (final CurriculumModule child : getCurriculumModulesSet()) {
            child.deleteRecursive();
        }

        delete();
    }

    /**
     * Before trying to delete, try to delete only empty child groups, leaving leafs untouched
     */
    protected void deleteRecursiveEmptyChildGroups() {

        for (final Iterator<CurriculumGroup> iterator = getChildCurriculumGroups().iterator(); iterator.hasNext();) {
            iterator.next().deleteRecursiveEmptyChildGroups();
        }

        deleteRecursive();
    }

    @Override
    public final StringBuilder print(String tabs) {
        final StringBuilder builder = new StringBuilder();
        builder.append(tabs);
        builder.append("[CG ").append(getName().getContent()).append(" - ").append(getAprovedEctsCredits()).append(" ects")
                .append(" ]\n");
        final String tab = tabs + "\t";
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            builder.append(curriculumModule.print(tab));
        }
        return builder;
    }

    @Override
    public CourseGroup getDegreeModule() {
        return (CourseGroup) super.getDegreeModule();
    }

    @Override
    final public List<Enrolment> getEnrolments() {
        return getCurriculumModulesSet().stream().flatMap(cm -> cm.getEnrolments().stream()).collect(Collectors.toList());
    }

    final public Set<Enrolment> getEnrolmentsSet() {
        return getCurriculumModulesSet().stream().flatMap(cm -> cm.getEnrolments().stream()).collect(Collectors.toSet());
    }

    @Override
    final public boolean hasAnyEnrolments() {
        return hasAnyCurriculumModules(new CurriculumModulePredicateByType(Enrolment.class));
    }

    @Override
    public boolean hasAnyCurriculumModules(final Predicate<CurriculumModule> predicate) {
        return super.hasAnyCurriculumModules(predicate) || getCurriculumModulesSet().stream()
                .anyMatch(cm -> cm.hasAnyCurriculumModules(predicate));
    }

    @Override
    final public void collectDismissals(final List<Dismissal> result) {
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            curriculumModule.collectDismissals(result);
        }
    }

    public List<Dismissal> getChildDismissals() {
        return getCurriculumModulesSet().stream().filter(CurriculumModule::isDismissal).map(Dismissal.class::cast)
                .collect(Collectors.toList());
    }

    public List<Enrolment> getChildEnrolments() {
        final List<Enrolment> result = new ArrayList<Enrolment>();
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            if (curriculumModule.isEnrolment()) {
                result.add((Enrolment) curriculumModule);
            }
        }

        return result;
    }

    public List<CurriculumLine> getChildCurriculumLines() {
        return getCurriculumModulesSet().stream().filter(CurriculumModule::isLeaf).map(CurriculumLine.class::cast)
                .collect(Collectors.toList());
    }

    public List<CurriculumGroup> getChildCurriculumGroups() {
        return getCurriculumModulesSet().stream().filter(cm -> !cm.isLeaf()).map(CurriculumGroup.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public StudentCurricularPlan getStudentCurricularPlan() {
        return getCurriculumGroup().getStudentCurricularPlan();
    }

    private Collection<Context> getDegreeModulesFor(ExecutionInterval executionInterval) {
        return this.getDegreeModule().getValidChildContexts(executionInterval);
    }

    public List<Context> getCurricularCourseContextsToEnrol(ExecutionInterval executionInterval) {
        List<Context> result = new ArrayList<Context>();
        for (Context context : this.getDegreeModulesFor(executionInterval)) {
            if (context.getChildDegreeModule().isLeaf()) {

                final CurricularCourse curricularCourse = (CurricularCourse) context.getChildDegreeModule();

                if (getDegreeCurricularPlanOfStudent().getCurricularRuleValidationType() == EnrolmentModel.YEAR) {

                    if (!getStudentCurricularPlan().isApproved(curricularCourse, executionInterval)
                            && !getStudentCurricularPlan().getRoot().hasEnrolmentWithEnroledState(curricularCourse,
                                    executionInterval.getExecutionYear())
                            && !hasEnrolmentForInterval(curricularCourse, executionInterval)
                            && matchesIntervalCurricularPeriod(executionInterval, context)) {
                        result.add(context);
                    }

                } else {

                    if (!this.getStudentCurricularPlan().isApproved(curricularCourse, executionInterval)
                            && !this.getStudentCurricularPlan().isEnroledInExecutionPeriod(curricularCourse, executionInterval)
                            && matchesIntervalCurricularPeriod(executionInterval, context)) {
                        result.add(context);
                    }
                }

            }
        }
        return result;
    }

    private boolean matchesIntervalCurricularPeriod(ExecutionInterval executionInterval, Context context) {
        final CurricularCourse curricularCourse = (CurricularCourse) context.getChildDegreeModule();
        return !curricularCourse.isAnual(executionInterval.getExecutionYear())
                || (context.getCurricularPeriod().getChildOrder().intValue() == executionInterval.getChildOrder().intValue()
                        && context.getCurricularPeriod().getAcademicPeriod().equals(executionInterval.getAcademicPeriod()));
    }

    private boolean hasEnrolmentForInterval(CurricularCourse curricularCourse, ExecutionInterval executionInterval) {
        return getStudentCurricularPlan().getEnrolments(curricularCourse).stream().anyMatch(e -> e.isValid(executionInterval));
    }

    public List<Context> getCourseGroupContextsToEnrol(ExecutionInterval executionInterval) {
        return getDegreeModulesFor(executionInterval).stream().filter(context -> !context.getChildDegreeModule().isLeaf())
                .filter(context -> !getStudentCurricularPlan().getRoot().hasDegreeModule(context.getChildDegreeModule()))
                .collect(Collectors.toList());
    }

    public Collection<CurricularCourse> getCurricularCoursesToDismissal(final ExecutionInterval executionInterval) {
        return getDegreeModule().getOpenChildContexts(CurricularCourse.class, executionInterval).stream()
                .map(context -> (CurricularCourse) context.getChildDegreeModule())
                .filter(curricularCourse -> !getStudentCurricularPlan().getRoot().isApproved(curricularCourse, null))
                .collect(Collectors.toSet());
    }

    @Override
    final public boolean isApproved(CurricularCourse curricularCourse, ExecutionInterval executionInterval) {
        return getCurriculumModulesSet().stream().anyMatch(cm -> cm.isApproved(curricularCourse, executionInterval));
    }

    @Override
    final public boolean isEnroledInExecutionPeriod(CurricularCourse curricularCourse, ExecutionInterval executionInterval) {
        return getCurriculumModulesSet().stream()
                .anyMatch(cm -> cm.isEnroledInExecutionPeriod(curricularCourse, executionInterval));
    }

    @Override
    final public boolean hasEnrolmentWithEnroledState(final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval) {
        return getCurriculumModulesSet().stream()
                .anyMatch(cm -> cm.hasEnrolmentWithEnroledState(curricularCourse, executionInterval));
    }

    @Override
    final public ExecutionYear getIEnrolmentsLastExecutionYear() {
        ExecutionYear result = null;

        for (final CurriculumModule curriculumModule : this.getCurriculumModulesSet()) {
            final ExecutionYear lastExecutionYear = curriculumModule.getIEnrolmentsLastExecutionYear();
            if (result == null || result.isBefore(lastExecutionYear)) {
                result = lastExecutionYear;
            }
        }

        return result;
    }

    @Override
    public boolean hasDegreeModule(DegreeModule degreeModule) {
        return super.hasDegreeModule(degreeModule) || getCurriculumModulesSet().stream()
                .anyMatch(cm -> cm.hasDegreeModule(degreeModule));
    }

    @Override
    final public boolean hasCurriculumModule(CurriculumModule curriculumModule) {
        return super.hasCurriculumModule(curriculumModule) || getCurriculumModulesSet().stream()
                .anyMatch(cm -> cm.hasCurriculumModule(curriculumModule));
    }

    @Override
    final public Enrolment findEnrolmentFor(final CurricularCourse curricularCourse, final ExecutionInterval executionInterval) {
        return getCurriculumModulesSet().stream().map(cm -> cm.findEnrolmentFor(curricularCourse, executionInterval))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    final public Enrolment getApprovedEnrolment(final CurricularCourse curricularCourse) {
        return getCurriculumModulesSet().stream().map(cm -> cm.getApprovedEnrolment(curricularCourse)).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Override
    final public Dismissal getDismissal(final CurricularCourse curricularCourse) {
        return getCurriculumModulesSet().stream().map(cm -> cm.getDismissal(curricularCourse)).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Override
    final public CurriculumLine getApprovedCurriculumLine(final CurricularCourse curricularCourse) {
        return getCurriculumModulesSet().stream().map(cm -> cm.getApprovedCurriculumLine(curricularCourse))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    public CurriculumGroup findCurriculumGroupFor(final CourseGroup courseGroup) {
        if (getDegreeModule() == courseGroup) {
            return this;
        }

        return getCurriculumModulesSet().stream().filter(cm -> !cm.isLeaf()).map(CurriculumGroup.class::cast)
                .map(group -> group.findCurriculumGroupFor(courseGroup)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public void getCurriculumModules(final ResultCollection<CurriculumModule> collection) {
        collection.condicionalAdd(this);
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            curriculumModule.getCurriculumModules(collection);
        }
    }

    final public Set<CurriculumLine> getCurriculumLines() {
        return getCurriculumModulesSet().stream().filter(CurriculumModule::isLeaf).map(CurriculumLine.class::cast)
                .collect(Collectors.toCollection(() -> new TreeSet<CurriculumLine>(CurriculumModule.COMPARATOR_BY_NAME_AND_ID)));
    }

    final public boolean hasCurriculumLines() {
        return getCurriculumModulesSet().stream().anyMatch(CurriculumModule::isLeaf);
    }

    @Override
    final public void addApprovedCurriculumLines(final Collection<CurriculumLine> result) {
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            curriculumModule.addApprovedCurriculumLines(result);
        }
    }

    @Override
    final public boolean hasAnyApprovedCurriculumLines() {
        final AndPredicate<CurriculumModule> andPredicate = new AndPredicate<CurriculumModule>();
        andPredicate.add(new CurriculumModulePredicateByType(CurriculumLine.class));
        andPredicate.add(new CurriculumModulePredicateByApproval());

        return hasAnyCurriculumModules(andPredicate);
    }

    final public Set<CurriculumGroup> getCurriculumGroups() {
        return getCurriculumModulesSet().stream().filter(cm -> !cm.isLeaf()).map(CurriculumGroup.class::cast)
                .collect(Collectors.toCollection(() -> new TreeSet<CurriculumGroup>(CurriculumModule.COMPARATOR_BY_NAME_AND_ID)));
    }

    public Set<CurriculumGroup> getCurriculumGroupsToEnrolmentProcess() {
        return getCurriculumModulesSet().stream().filter(cm -> !cm.isLeaf() && !cm.isNoCourseGroupCurriculumGroup())
                .map(CurriculumGroup.class::cast)
                .collect(Collectors.toCollection(() -> new TreeSet<CurriculumGroup>(CurriculumModule.COMPARATOR_BY_NAME_AND_ID)));
    }

    public Set<CurriculumGroup> getBranchCurriculumGroups() {
        if (isBranchCurriculumGroup()) {
            return Set.of(this);
        }
        return getCurriculumModulesSet().stream().filter(CurriculumGroup.class::isInstance)
                .flatMap(cm -> ((CurriculumGroup) cm).getBranchCurriculumGroups().stream()).collect(Collectors.toSet());
    }

    @Override
    public Set<CurriculumGroup> getAllCurriculumGroups() {
        return Stream.concat(Stream.of(this),
                        getCurriculumModulesSet().stream().flatMap(cm -> cm.getAllCurriculumGroups().stream()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<CurriculumGroup> getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups() {
        return Stream.concat(Stream.of(this), getCurriculumModulesSet().stream()
                        .flatMap(cm -> cm.getAllCurriculumGroupsWithoutNoCourseGroupCurriculumGroups().stream()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<CurriculumLine> getAllCurriculumLines() {
        return getCurriculumModulesSet().stream().flatMap(cm -> cm.getAllCurriculumLines().stream()).collect(Collectors.toSet());
    }

    public Integer getChildOrder() {
        return getChildOrder(null);
    }

    public Integer getChildOrder(final ExecutionInterval executionInterval) {
        final Integer childOrder = getParentCurriculumGroup().searchChildOrderForChild(this, executionInterval);
        return childOrder != null ? childOrder : Integer.MAX_VALUE;
    }

    private CurriculumGroup getParentCurriculumGroup() {
        return getCurriculumGroup();
    }

    protected Integer searchChildOrderForChild(final CurriculumGroup child, final ExecutionInterval executionInterval) {
        for (final Context context : getDegreeModule().getValidChildContexts(executionInterval)) {
            if (context.getChildDegreeModule() == child.getDegreeModule()) {
                return context.getChildOrder();
            }
        }
        return null;
    }

    public boolean hasCourseGroup(final CourseGroup courseGroup) {
        return getDegreeModule() == courseGroup || getCurriculumModulesSet().stream().filter(cm -> !cm.isLeaf())
                .map(CurriculumGroup.class::cast).anyMatch(cm -> cm.hasCourseGroup(courseGroup));
    }

    final public NoCourseGroupCurriculumGroup getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType groupType) {
        return getCurriculumGroups().stream().filter(CurriculumGroup::isNoCourseGroupCurriculumGroup)
                .map(NoCourseGroupCurriculumGroup.class::cast).filter(ng -> ng.getNoCourseGroupCurriculumGroupType() == groupType)
                .findFirst().orElse(null);
    }

    @Override
    final public Double getEctsCredits() {
        BigDecimal bigDecimal = BigDecimal.ZERO;
        for (CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            bigDecimal = bigDecimal.add(new BigDecimal(curriculumModule.getEctsCredits()));
        }
        return Double.valueOf(bigDecimal.doubleValue());
    }

    @Override
    public Double getAprovedEctsCredits() {
        BigDecimal bigDecimal = BigDecimal.ZERO;
        for (CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            bigDecimal = bigDecimal.add(new BigDecimal(curriculumModule.getAprovedEctsCredits()));
        }
        return Double.valueOf(bigDecimal.doubleValue());
    }

    @Override
    final public Double getEnroledEctsCredits(final ExecutionInterval executionInterval) {
        BigDecimal bigDecimal = BigDecimal.ZERO;
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            bigDecimal = bigDecimal.add(new BigDecimal(curriculumModule.getEnroledEctsCredits(executionInterval)));
        }
        return Double.valueOf(bigDecimal.doubleValue());
    }

    @Override
    final public Double getEnroledEctsCredits(final ExecutionYear executionYear) {
        //NOTE: this method cannot be implemented iterating over semesters, because annual curricular courses would be accounted twice (they are valid on both semesters)
        BigDecimal bigDecimal = BigDecimal.ZERO;
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            bigDecimal = bigDecimal.add(new BigDecimal(curriculumModule.getEnroledEctsCredits(executionYear)));
        }
        return Double.valueOf(bigDecimal.doubleValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Double getCreditsConcluded(ExecutionYear executionYear) {
        final CreditsLimit creditsLimit =
                (CreditsLimit) getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, executionYear);

        Double creditsConcluded = 0d;
        for (CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            creditsConcluded += curriculumModule.getCreditsConcluded(executionYear);
        }

        if (creditsLimit == null) {
            return creditsConcluded;
        } else {
            return Math.min(creditsLimit.getMaximumCredits(), creditsConcluded);
        }
    }

    final public int getNumberOfChildCurriculumGroupsWithCourseGroup() {
        return (int) getCurriculumModulesSet().stream().filter(cm -> !cm.isLeaf() && !cm.isNoCourseGroupCurriculumGroup())
                .count();
    }

    /**
     * This method returns the number of approved child CurriculumLines
     */
    final public int getNumberOfApprovedChildCurriculumLines() {
        return (int) getCurriculumModulesSet().stream().filter(CurriculumModule::isCurriculumLine).map(CurriculumLine.class::cast)
                .filter(CurriculumGroup::isApprovedCurriculumLine).count();
    }

    /**
     * This method makes a deep search to count number of all enrolled
     * CurriculumLines (except NoCourseGroupCurriculumGroups)
     */
    public int getNumberOfAllEnroledCurriculumLines() {
        int result = 0;
        for (CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            if (curriculumModule.isCurriculumLine()) {
                final CurriculumLine curriculumLine = (CurriculumLine) curriculumModule;
                if (curriculumLine.isEnrolment()) {
                    result++;
                }
            } else {
                result += ((CurriculumGroup) curriculumModule).getNumberOfAllEnroledCurriculumLines();
            }
        }
        return result;
    }

    /**
     * This method makes a deep search to count number of all approved
     * CurriculumLines (except NoCourseGroupCurriculumGroups)
     */
    public int getNumberOfAllApprovedCurriculumLines() {
        return getCurriculumModulesSet().stream().mapToInt(cm -> {
            if (cm.isCurriculumLine()) {
                return isApprovedCurriculumLine((CurriculumLine) cm) ? 1 : 0;
            }
            return ((CurriculumGroup) cm).getNumberOfAllApprovedCurriculumLines();
        }).sum();
    }

    private static boolean isApprovedCurriculumLine(final CurriculumLine curriculumLine) {
        return curriculumLine.isDismissal() && curriculumLine.hasCurricularCourse()
                || curriculumLine.isEnrolment() && curriculumLine.isApproved();
    }

    final public int getNumberOfChildEnrolments(final ExecutionInterval executionInterval) {
        return (int) getCurriculumModulesSet().stream().filter(Enrolment.class::isInstance).map(Enrolment.class::cast)
                .filter(e -> e.isValid(executionInterval) && e.isEnroled()).count();
    }

    final public int getNumberOfChildEnrolments(final ExecutionYear executionYear) {
        //NOTE: this method cannot be implemented iterating over semesters, because annual curricular courses would be
        //accounted twice (they are valid on both semesters)
        return (int) getCurriculumModulesSet().stream().filter(Enrolment.class::isInstance).map(Enrolment.class::cast)
                .filter(e -> e.isValid(executionYear) && e.isEnroled()).count();
    }

    @Override
    public int getNumberOfAllApprovedEnrolments(final ExecutionInterval executionInterval) {
        return getCurriculumModulesSet().stream().mapToInt(cm -> cm.getNumberOfAllApprovedEnrolments(executionInterval)).sum();
    }

    @Override
    public Set<IDegreeModuleToEvaluate> getDegreeModulesToEvaluate(final ExecutionInterval executionInterval) {
        return Stream.concat(Stream.of(new EnroledCurriculumModuleWrapper(this, executionInterval)),
                        getCurriculumModulesSet().stream().flatMap(cm -> cm.getDegreeModulesToEvaluate(executionInterval).stream()))
                .collect(Collectors.toSet());
    }

    @Override
    final public void getAllDegreeModules(final Collection<DegreeModule> degreeModules) {
        degreeModules.add(getDegreeModule());
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            curriculumModule.getAllDegreeModules(degreeModules);
        }
    }

    @Override
    public ConclusionValue isConcluded(final ExecutionYear executionYear) {
        final Collection<ConclusionRule> rules = getConclusionRulesToEvaluate(executionYear);
        return rules.isEmpty() ? ConclusionValue.UNKNOWN : ConclusionValue
                .create(rules.stream().allMatch(r -> r.isConcluded(this, executionYear)));
    }

    @Override
    public boolean canConclude(ExecutionYear executionYear) {
        final Collection<ConclusionRule> rules = getConclusionRulesToEvaluate(executionYear);
        return !rules.isEmpty() && rules.stream().allMatch(r -> r.canConclude(this, executionYear));
    }

    public Collection<ConclusionRule> getConclusionRulesToEvaluate(ExecutionYear executionYear) {
        return getDegreeModule().getCurricularRules(executionYear).stream().filter(ConclusionRule.class::isInstance)
                .map(ConclusionRule.class::cast).filter(r -> r.canBeEvaluatedForConclusion(this, executionYear))
                .collect(Collectors.toSet());
    }

    public void assertCorrectStructure(final Collection<CurriculumGroup> result, ExecutionYear lastApprovedYear) {

        if (isSkipConcluded()) {
            return;
        }

        for (final CurriculumGroup curriculumGroup : getCurriculumGroups()) {
            if (curriculumGroup.getCurriculumGroups().isEmpty() && curriculumGroup.hasUnexpectedCredits(lastApprovedYear)) {
                result.add(curriculumGroup);
            } else {
                curriculumGroup.assertCorrectStructure(result, lastApprovedYear);
            }
        }
    }

    public boolean hasUnexpectedCredits(ExecutionYear lastApprovedYear) {
        return getAprovedEctsCredits().doubleValue() != getCreditsConcluded(lastApprovedYear).doubleValue();
    }

    @Override
    public boolean hasConcluded(final DegreeModule degreeModule, final ExecutionYear executionYear) {
        return getDegreeModule() == degreeModule ? isConcluded(executionYear).value() : getCurriculumModulesSet().stream()
                .anyMatch(cm -> cm.hasConcluded(degreeModule, executionYear));
    }

    @Override
    public YearMonthDay calculateConclusionDate() {
        final Collection<CurriculumModule> curriculumModules = new HashSet<CurriculumModule>(getCurriculumModulesSet());
        YearMonthDay result = null;
        for (final CurriculumModule curriculumModule : curriculumModules) {
            if (curriculumModule.isConcluded(getApprovedCurriculumLinesLastExecutionYear()).isValid()
                    && curriculumModule.hasAnyApprovedCurriculumLines()) {
                final YearMonthDay curriculumModuleConclusionDate = curriculumModule.calculateConclusionDate();
                if (curriculumModuleConclusionDate != null
                        && (result == null || curriculumModuleConclusionDate.isAfter(result))) {
                    result = curriculumModuleConclusionDate;
                }
            }
        }

        return result;
    }

    public void assertConclusionDate(final Collection<CurriculumModule> result) {
        for (final CurriculumModule curriculumModule : getCurriculumModulesSet()) {
            if (curriculumModule.isConcluded(getApprovedCurriculumLinesLastExecutionYear()).isValid()
                    && curriculumModule.hasAnyApprovedCurriculumLines()) {
                final YearMonthDay curriculumModuleConclusionDate = curriculumModule.calculateConclusionDate();
                if (curriculumModuleConclusionDate == null) {
                    result.add(curriculumModule);
                }
            }
        }
    }

    @Override
    public Curriculum getCurriculum(final DateTime when, final ExecutionYear executionYear) {
        return getCurriculumSupplier().get(this, when, executionYear);
    }

    static public interface CurriculumSupplier {

        public Curriculum get(final CurriculumGroup curriculumGroup, final DateTime when, final ExecutionYear executionYear);
    }

    static private Supplier<CurriculumSupplier> CURRICULUM_SUPPLIER = () -> new CurriculumSupplier() {

        @Override
        public Curriculum get(final CurriculumGroup curriculumGroup, final DateTime when, final ExecutionYear executionYear) {

            final Curriculum curriculum = Curriculum.createEmpty(curriculumGroup, executionYear);
            if (!curriculumGroup.wasCreated(when)) {
                return curriculum;
            }

            for (final CurriculumModule curriculumModule : curriculumGroup.getCurriculumModulesSet()) {
                curriculum.add(curriculumModule.getCurriculum(when, executionYear));
            }

            return curriculum;
        }
    };

    static public CurriculumSupplier getCurriculumSupplier() {
        return CURRICULUM_SUPPLIER.get();
    }

    static public void setCurriculumSupplier(final Supplier<CurriculumSupplier> input) {
        if (input != null && input.get() != null) {
            CURRICULUM_SUPPLIER = input;
        } else {
            logger.error("Could not set CURRICULUM_SUPPLIER to null");
        }
    }

    @Override
    public boolean isPropaedeutic() {
        return getCurriculumGroup() != null && getCurriculumGroup().isPropaedeutic();
    }

    public boolean isExtraCurriculum() {
        return false;
    }

    public boolean isStandalone() {
        return false;
    }

    public boolean isInternalCreditsSourceGroup() {
        return false;
    }

    public boolean isExternal() {
        return false;
    }

    public boolean canAdd(final CurriculumLine curriculumLine) {
        final CurricularCourse curricularCourse =
                curriculumLine instanceof OptionalEnrolment ? ((OptionalEnrolment) curriculumLine)
                        .getOptionalCurricularCourse() : curriculumLine.getCurricularCourse();

        if (curricularCourse == null) {
            return true;
        }

        final CourseGroup courseGroup = getDegreeModule();
        final Collection<CurricularCourse> curricularCourses =
                curricularCourse.getCompetenceCourse() == null ? Set.of(curricularCourse) : curricularCourse.getCompetenceCourse()
                        .getAssociatedCurricularCoursesSet();

        return curricularCourses.stream().anyMatch(c -> courseGroup.hasDegreeModuleOnChilds(c));
    }

    public Collection<CurriculumGroup> getCurricularCoursePossibleGroups(final CurricularCourse curricularCourse) {
        Collection<CurriculumGroup> result = new HashSet<CurriculumGroup>();
        if (getDegreeModule().hasDegreeModuleOnChilds(curricularCourse)) {
            result.add(this);
        }

        for (CurriculumGroup curriculumGroup : this.getCurriculumGroups()) {
            result.addAll(curriculumGroup.getCurricularCoursePossibleGroups(curricularCourse));
        }

        return result;
    }

    public Collection<CurriculumGroup> getCurricularCoursePossibleGroupsWithoutNoCourseGroupCurriculumGroups(
            final CurricularCourse curricularCourse) {
        Collection<CurriculumGroup> result = new HashSet<CurriculumGroup>();
        if (getDegreeModule().hasDegreeModuleOnChilds(curricularCourse)) {
            result.add(this);
        }

        for (CurriculumGroup curriculumGroup : this.getCurriculumGroups()) {
            result.addAll(
                    curriculumGroup.getCurricularCoursePossibleGroupsWithoutNoCourseGroupCurriculumGroups(curricularCourse));
        }

        return result;
    }

    public Collection<NoCourseGroupCurriculumGroup> getNoCourseGroupCurriculumGroups() {
        return getCurriculumGroups().stream().flatMap(cg -> cg.getNoCourseGroupCurriculumGroups().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasEnrolment(ExecutionYear executionYear) {
        final AndPredicate<CurriculumModule> andPredicate = new AndPredicate<CurriculumModule>();
        andPredicate.add(new CurriculumModulePredicateByType(Enrolment.class));
        andPredicate.add(new CurriculumModulePredicateByExecutionYear(executionYear));

        return hasAnyCurriculumModules(andPredicate);
    }

    @Override
    public boolean hasEnrolment(ExecutionInterval executionInterval) {
        final AndPredicate<CurriculumModule> andPredicate = new AndPredicate<CurriculumModule>();
        andPredicate.add(new CurriculumModulePredicateByType(Enrolment.class));
        andPredicate.add(new CurriculumModulePredicateByExecutionInterval(executionInterval));

        return hasAnyCurriculumModules(andPredicate);
    }

    public Set<Enrolment> getEnrolmentsBy(final ExecutionYear executionYear) {
        return getEnrolmentsSet().stream().filter(e -> e.getExecutionYear() == executionYear).collect(Collectors.toSet());
    }

    public Set<Enrolment> getEnrolmentsBy(final ExecutionInterval executionInterval) {
        return getEnrolmentsSet().stream().filter(e -> e.getExecutionInterval() == executionInterval).collect(Collectors.toSet());
    }

    @Override
    public ConclusionProcess getConclusionProcess() {
        final ConclusionProcess conclusionProcess = super.getConclusionProcess();
        if (conclusionProcess != null && conclusionProcess.isActive()) {
            return conclusionProcess;
        }
        return null;
    }

    @Override
    public void setConclusionProcess(ConclusionProcess process) {
        if (process != null && !process.isActive()) {
            throw new DomainException("error.conclusion.process.not.valid");
        }
        super.setConclusionProcess(process);
    }

    public ConclusionProcess readConclusionProcessEvenIfInactive() {
        return super.getConclusionProcess();
    }

    public boolean isSkipConcluded() {
        return getDegreeModule() != null && getDegreeModule().getProgramConclusion() != null
                && getDegreeModule().getProgramConclusion().isSkipValidation();
    }

    @Override
    public boolean isConcluded() {
        return isConclusionProcessed() || isSkipConcluded() || super.isConcluded();
    }

    public boolean isConclusionProcessed() {
        return getConclusionProcess() != null;
    }

    final public ExecutionYear getIngressionYear() {
        return isConclusionProcessed() ? getConclusionProcess().getIngressionYear() : getRegistration().calculateIngressionYear();
    }

    final public Grade getRawGrade() {
        return isConclusionProcessed() ? getConclusionProcess().getRawGrade() : null;
    }

    final public Grade getFinalGrade() {
        return isConclusionProcessed() ? getConclusionProcess().getFinalGrade() : null;
    }

    final public Grade getDescriptiveGrade() {
        return isConclusionProcessed() ? getConclusionProcess().getDescriptiveGrade() : null;
    }

    final public ExecutionYear getConclusionYear() {
        return isConclusionProcessed() ? getConclusionProcess().getConclusionYear() : null;
    }

    final public Person getConclusionProcessResponsible() {
        return isConclusionProcessed() ? getConclusionProcess().getResponsible() : null;
    }

    final public Person getConclusionProcessLastResponsible() {
        return isConclusionProcessed() ? getConclusionProcess().getLastResponsible() : null;
    }

    final public String getConclusionProcessNotes() {
        return isConclusionProcessed() ? getConclusionProcess().getNotes() : null;
    }

    final public DateTime getConclusionProcessCreationDateTime() {
        return isConclusionProcessed() ? getConclusionProcess().getCreationDateTime() : null;
    }

    final public DateTime getConclusionProcessLastModificationDateTime() {
        return isConclusionProcessed() ? getConclusionProcess().getLastModificationDateTime() : null;
    }

    final public YearMonthDay getConclusionDate() {
        return isConclusionProcessed() ? getConclusionProcess().getConclusionYearMonthDay() : null;
    }

    @Override
    public Double getCreditsConcluded() {
        return isConclusionProcessed() ? getConclusionProcess().getCredits().doubleValue() : calculateCreditsConcluded();
    }

    final public Double calculateCreditsConcluded() {
        return super.getCreditsConcluded();
    }

    @Override
    public Stream<CurriculumLine> getCurriculumLineStream() {
        return getCurriculumModulesSet().stream().flatMap(m -> m.getCurriculumLineStream());
    }

    @Override
    public boolean isBranchCurriculumGroup() {
        return getDegreeModule() != null && getDegreeModule().isBranchCourseGroup();
    }
}
