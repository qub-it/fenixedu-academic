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
package org.fenixedu.academic.domain.degreeStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.CurricularRule;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleType;
import org.fenixedu.academic.domain.curricularRules.DegreeModulesSelectionLimit;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.util.StringFormatter;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.I18N;

import com.google.common.base.Strings;

public class CourseGroup extends CourseGroup_Base {

    static public List<CourseGroup> readCourseGroups() {
        final List<CourseGroup> result = new ArrayList<CourseGroup>();
        for (final DegreeModule degreeModule : Bennu.getInstance().getDegreeModulesSet()) {
            if (degreeModule instanceof CourseGroup) {
                result.add((CourseGroup) degreeModule);
            }
        }
        return result;
    }

    public CourseGroup() {
        super();
    }

    protected CourseGroup(final String name, final String nameEn) {
        this();
        init(name, nameEn);
    }

    protected void init(final String name, final String nameEn) {
        super.setName(name);
        super.setNameEn(nameEn);
    }

    public CourseGroup(final CourseGroup parentCourseGroup, final String name, final String nameEn, final ExecutionInterval begin,
            final ExecutionInterval end) {
        this(parentCourseGroup, name, nameEn, begin, end, null);
    }

    public CourseGroup(final CourseGroup parentCourseGroup, final String name, final String nameEn, final ExecutionInterval begin,
            final ExecutionInterval end, final ProgramConclusion programConclusion) {
        init(parentCourseGroup, name, nameEn, begin, end, programConclusion);
    }

    protected void init(CourseGroup parentCourseGroup, String name, String nameEn, ExecutionInterval begin,
            ExecutionInterval end) {
        init(parentCourseGroup, name, nameEn, begin, end, null);
    }

    protected void init(CourseGroup parentCourseGroup, String name, String nameEn, ExecutionInterval begin, ExecutionInterval end,
            final ProgramConclusion programConclusion) {
        init(name, nameEn);
        if (parentCourseGroup == null) {
            throw new DomainException("error.degreeStructure.CourseGroup.parentCourseGroup.cannot.be.null");
        }
        parentCourseGroup.checkDuplicateChildNames(name, nameEn);
        new Context(parentCourseGroup, this, null, begin, end);
        setProgramConclusion(programConclusion);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public void edit(String name, String nameEn, Context context, ExecutionInterval beginExecutionPeriod,
            ExecutionInterval endExecutionPeriod, Boolean isOptional, ProgramConclusion programConclusion) {
        // override, assure that root's name equals degree curricular plan name
        if (this.isRoot()) {
            setName(getParentDegreeCurricularPlan().getName());
            setNameEn(getParentDegreeCurricularPlan().getName());
        } else {
            setName(name);
            setNameEn(nameEn);
        }

        this.checkDuplicateBrotherNames(name, nameEn);

        if (!this.isRoot() && context != null) {
            context.edit(beginExecutionPeriod, endExecutionPeriod);
        }
        setIsOptional(isOptional);
        setProgramConclusion(programConclusion);
    }

    @Override
    public Boolean getCanBeDeleted() {
        return super.getCanBeDeleted() && getChildContextsSet().isEmpty();
    }

    @Override
    public void delete() {
        if (getCanBeDeleted()) {
            super.delete();
            for (; !getParticipatingContextCurricularRulesSet().isEmpty(); getParticipatingContextCurricularRulesSet().iterator()
                    .next().delete()) {
                ;
            }
            setRootDomainObject(null);
            super.deleteDomainObject();
        } else {
            throw new DomainException("courseGroup.notEmptyCourseGroupContexts");
        }
    }

    @Override
    public void print(StringBuilder dcp, String tabs, Context previousContext) {
        String tab = tabs + "\t";
        dcp.append(tab);
        dcp.append("[CG ").append(this.getExternalId()).append("] ").append(this.getName()).append("\n");

        for (Context context : this.getChildContexts(CurricularCourse.class).stream().sorted().collect(Collectors.toList())) {
            context.getChildDegreeModule().print(dcp, tab, context);
        }
        for (Context context : this.getChildContexts(CourseGroup.class).stream().sorted().collect(Collectors.toList())) {
            context.getChildDegreeModule().print(dcp, tab, context);
        }
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public DegreeCurricularPlan getParentDegreeCurricularPlan() {
        return !getParentContextsSet().isEmpty() ? getParentContextsSet().iterator().next().getParentCourseGroup()
                .getParentDegreeCurricularPlan() : null;
    }

    public List<Context> getChildContexts(Class<? extends DegreeModule> clazz) {
        return getValidChildContextsForExecutionAggregation(clazz, (ExecutionYear) null);
    }

    public List<Context> getValidChildContextsForExecutionAggregation(final ExecutionYear executionYear) {
        return getValidChildContextsForExecutionAggregation(null, executionYear);
    }

    public List<Context> getValidChildContexts(final ExecutionInterval executionInterval) {
        return getValidChildContexts(null, executionInterval);
    }

    // Valid means that is open to execution year, and if is
    // CurricularCourse
    // the context must have same semester of any ExecutionPeriod of
    // ExecutionYear
    public List<Context> getValidChildContextsForExecutionAggregation(final Class<? extends DegreeModule> clazz,
            final ExecutionYear executionYear) {
        final List<Context> result = new ArrayList<Context>();
        for (final Context context : this.getChildContextsSet()) {
            if (hasClass(clazz, context.getChildDegreeModule())
                    && ((executionYear == null || context.isValidForExecutionAggregation(executionYear)))) {
                result.add(context);
            }
        }
        return result;
    }

    // Valid means that is open to execution period, and if is
    // CurricularCourse
    // the context must have same semester than executionPeriod
    public List<Context> getValidChildContexts(final Class<? extends DegreeModule> clazz,
            final ExecutionInterval executionInterval) {
        final List<Context> result = new ArrayList<Context>();
        for (Context context : this.getChildContextsSet()) {
            if (hasClass(clazz, context.getChildDegreeModule())
                    && ((executionInterval == null || context.isValid(executionInterval)))) {
                result.add(context);
            }
        }

        return result;
    }

//    public List<Context> getSortedOpenChildContextsWithCurricularCourses(final ExecutionYear executionYear) {
//        final List<Context> result = getOpenChildContextsForExecutionAggregation(CurricularCourse.class, executionYear);
//        Collections.sort(result);
//        return result;
//    }

//    public List<Context> getSortedOpenChildContextsWithCourseGroups(final ExecutionYear executionYear) {
//        final List<Context> result = this.getOpenChildContextsForExecutionAggregation(CourseGroup.class, executionYear);
//        Collections.sort(result);
//        return result;
//    }

//    public List<Context> getSortedOpenChildContextsWithCourseGroups(final ExecutionInterval executionInterval) {
//        final List<Context> result = this.getOpenChildContexts(CourseGroup.class, executionInterval);
//        Collections.sort(result);
//        return result;
//    }

    public List<Context> getOpenChildContexts(final Class<? extends DegreeModule> clazz,
            final ExecutionInterval executionInterval) {
        final List<Context> result = new ArrayList<Context>();
        for (final Context context : getChildContextsSet()) {
            if (hasClass(clazz, context.getChildDegreeModule())
                    && ((executionInterval == null || context.isOpen(executionInterval)))) {
                result.add(context);
            }
        }
        return result;
    }

    public List<Context> getOpenChildContextsForExecutionAggregation(final Class<? extends DegreeModule> clazz,
            final ExecutionYear executionYear) {
        final List<Context> result = new ArrayList<Context>();
        for (final Context context : getChildContextsSet()) {
            if (hasClass(clazz, context.getChildDegreeModule()) && ((executionYear == null || context.isOpen(executionYear)))) {
                result.add(context);
            }
        }
        return result;
    }

    private boolean hasClass(final Class<? extends DegreeModule> clazz, final DegreeModule degreeModule) {
        return clazz == null || clazz.isAssignableFrom(degreeModule.getClass());
    }

//    public List<Context> getSortedChildContextsWithCurricularCourses() {
//        List<Context> result = this.getChildContexts(CurricularCourse.class);
//        Collections.sort(result);
//        return result;
//    }

//    public List<Context> getSortedChildContextsWithCurricularCoursesByExecutionYear(ExecutionYear executionYear) {
//        List<Context> result = this.getValidChildContextsForExecutionAggregation(CurricularCourse.class, executionYear);
//        Collections.sort(result);
//        return result;
//    }

//    public List<Context> getSortedChildContextsWithCourseGroups() {
//        List<Context> result = new ArrayList<Context>(this.getChildContexts(CourseGroup.class));
//        Collections.sort(result);
//        return result;
//    }

//    public List<Context> getSortedChildContextsWithCourseGroupsByExecutionYear(ExecutionYear executionYear) {
//        List<Context> result = this.getValidChildContexts(CourseGroup.class, executionYear);
//        Collections.sort(result);
//        return result;
//    }

    @Override
    public List<CurricularRule> getParticipatingCurricularRules() {
        final List<CurricularRule> result = new ArrayList<CurricularRule>();
        result.addAll(super.getParticipatingCurricularRules());
        result.addAll(getParticipatingContextCurricularRulesSet());
        return result;
    }

    @Override
    public void setProgramConclusion(ProgramConclusion programConclusion) {
        checkIfAlreadyHasConclusionProcesses(programConclusion);
        checkDuplicateProgramConclusion(programConclusion);
        super.setProgramConclusion(programConclusion);
    }

    private void checkIfAlreadyHasConclusionProcesses(ProgramConclusion programConclusion) {
        if (getProgramConclusion() == null || getProgramConclusion() == programConclusion) {
            return;
        }

        if (getCurriculumModulesSet().stream().map(CurriculumGroup.class::cast)
                .anyMatch(cg -> cg.getConclusionProcess() != null)) {
            throw new DomainException("error.CourseGroup.cannot.change.program.conclusion.with.existing.conclusions");
        }
    }

    private void checkDuplicateProgramConclusion(ProgramConclusion programConclusion) {

        if (programConclusion == null) {
            return;
        }

        if (getParentDegreeCurricularPlan().getAllCoursesGroups().stream().filter(cg -> !cg.equals(this))
                .map(CourseGroup::getProgramConclusion).filter(Objects::nonNull).anyMatch(pc -> pc.equals(programConclusion))) {
            throw new DomainException("error.program.conclusion.already.exists", programConclusion.getName().getContent());
        }
    }

    public void checkDuplicateChildNames(final String name, final String nameEn) {
        String normalizedName = StringFormatter.normalize(name);
        String normalizedNameEn = StringFormatter.normalize(nameEn);
        if (!verifyNames(normalizedName, normalizedNameEn)) {
            throw new DomainException("error.existingCourseGroupWithSameName");
        }
    }

    public void checkDuplicateBrotherNames(final String name, final String nameEn) {
        String normalizedName = StringFormatter.normalize(name);
        String normalizedNameEn = StringFormatter.normalize(nameEn);
        for (Context parentContext : getParentContextsSet()) {
            CourseGroup parentCourseGroup = parentContext.getParentCourseGroup();
            if (!parentCourseGroup.verifyNames(normalizedName, normalizedNameEn, this)) {
                throw new DomainException("error.existingCourseGroupWithSameName");
            }
        }
    }

    private boolean verifyNames(String normalizedName, String normalizedNameEn) {
        return verifyNames(normalizedName, normalizedNameEn, this);
    }

    private boolean verifyNames(String normalizedName, String normalizedNameEn, DegreeModule excludedModule) {
        for (Context context : getChildContextsSet()) {
            DegreeModule degreeModule = context.getChildDegreeModule();
            if (degreeModule != excludedModule) {
                if (!Strings.isNullOrEmpty(degreeModule.getName())
                        && StringFormatter.normalize(degreeModule.getName()).equals(normalizedName)) {
                    return false;
                }
                if (!Strings.isNullOrEmpty(degreeModule.getNameEn())
                        && StringFormatter.normalize(degreeModule.getNameEn()).equals(normalizedNameEn)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void orderChild(Context contextToOrder, int position) {
        List<Context> newSort = null;
        if (contextToOrder.getChildDegreeModule() instanceof CurricularCourse) {
            newSort = this.getChildContexts(CurricularCourse.class).stream().sorted().collect(Collectors.toList());
        } else {
            newSort = this.getChildContexts(CourseGroup.class).stream().sorted().collect(Collectors.toList());
        }

        if (newSort.size() <= 1 || position < 0 || position > newSort.size()) {
            return;
        }

        newSort.remove(contextToOrder);
        newSort.add(position, contextToOrder);

        for (int newOrder = 0; newOrder < newSort.size(); newOrder++) {
            Context context = newSort.get(newOrder);

            if (context == contextToOrder && newOrder != position) {
                throw new DomainException("wrong.order.algorithm");
            }
            context.setChildOrder(newOrder);
        }
    }

    public Set<DegreeModule> collectAllChildDegreeModules(final Class<? extends DegreeModule> clazz,
            final ExecutionYear executionYear) {
        final Set<DegreeModule> result = new HashSet<DegreeModule>();
        for (final Context context : this.getValidChildContextsForExecutionAggregation(executionYear)) {
            final DegreeModule degreeModule = context.getChildDegreeModule();
            if (clazz.isAssignableFrom(degreeModule.getClass())) {
                result.add(degreeModule);
            }
            if (!degreeModule.isLeaf()) {
                final CourseGroup courseGroup = (CourseGroup) degreeModule;
                result.addAll(courseGroup.collectAllChildDegreeModules(clazz, executionYear));
            }
        }
        return result;
    }

//    public Set<DegreeModule> collectAllChildDegreeModules(final Class<? extends DegreeModule> clazz,
//            final ExecutionInterval executionInterval) {
//        final Set<DegreeModule> result = new HashSet<DegreeModule>();
//        for (final Context context : getValidChildContexts(executionInterval)) {
//            final DegreeModule degreeModule = context.getChildDegreeModule();
//            if (clazz.isAssignableFrom(degreeModule.getClass())) {
//                result.add(degreeModule);
//            }
//            if (!degreeModule.isLeaf()) {
//                final CourseGroup courseGroup = (CourseGroup) degreeModule;
//                result.addAll(courseGroup.collectAllChildDegreeModules(clazz, executionInterval));
//            }
//        }
//        return result;
//    }

    public void collectChildDegreeModulesIncludingFullPath(Class<? extends DegreeModule> clazz, List<List<DegreeModule>> result,
            List<DegreeModule> previousDegreeModulesPath, ExecutionYear executionYear) {
        final List<DegreeModule> currentDegreeModulesPath = previousDegreeModulesPath;
        for (final Context context : this.getValidChildContextsForExecutionAggregation(executionYear)) {
            List<DegreeModule> newDegreeModulesPath = null;
            if (clazz.isAssignableFrom(context.getChildDegreeModule().getClass())) {
                newDegreeModulesPath =
                        initNewDegreeModulesPath(newDegreeModulesPath, currentDegreeModulesPath, context.getChildDegreeModule());
                result.add(newDegreeModulesPath);
            }
            if (!context.getChildDegreeModule().isLeaf()) {
                newDegreeModulesPath =
                        initNewDegreeModulesPath(newDegreeModulesPath, currentDegreeModulesPath, context.getChildDegreeModule());
                ((CourseGroup) context.getChildDegreeModule()).collectChildDegreeModulesIncludingFullPath(clazz, result,
                        newDegreeModulesPath, executionYear);
            }
        }
    }

    private List<DegreeModule> initNewDegreeModulesPath(List<DegreeModule> newDegreeModulesPath,
            final List<DegreeModule> currentDegreeModulesPath, final DegreeModule degreeModule) {
        if (newDegreeModulesPath == null) {
            newDegreeModulesPath = new ArrayList<DegreeModule>(currentDegreeModulesPath);
            newDegreeModulesPath.add(degreeModule);
        }
        return newDegreeModulesPath;
    }

    public Collection<CourseGroup> getNotOptionalChildCourseGroups(final ExecutionInterval executionSemester) {

        final Collection<DegreeModule> degreeModules = getDegreeModulesByExecutionInterval(executionSemester);
        final Collection<CurricularRule> curricularRules = getCurricularRulesByExecutionInterval(executionSemester);
        final DegreeModulesSelectionLimit degreeModulesSelectionLimit = getDegreeModulesSelectionLimitRule(curricularRules);

        if (degreeModulesSelectionLimit != null) {

            if (degreeModulesSelectionLimit.getMinimumLimit().equals(degreeModulesSelectionLimit.getMaximumLimit())
                    && degreeModulesSelectionLimit.getMaximumLimit().equals(degreeModules.size())) {

                return filterCourseGroups(degreeModules);

            } else {
                return Collections.EMPTY_LIST;
            }
        }
        return filterCourseGroups(degreeModules);
    }

    private Collection<CourseGroup> filterCourseGroups(final Collection<DegreeModule> degreeModules) {
        final Collection<CourseGroup> result = new HashSet<CourseGroup>();
        for (final DegreeModule degreeModule : degreeModules) {
            if (!degreeModule.isLeaf()) {
                result.add((CourseGroup) degreeModule);
            }
        }
        return result;
    }

    private DegreeModulesSelectionLimit getDegreeModulesSelectionLimitRule(final Collection<CurricularRule> curricularRules) {
        for (final CurricularRule curricularRule : curricularRules) {
            if (curricularRule.getCurricularRuleType() == CurricularRuleType.DEGREE_MODULES_SELECTION_LIMIT) {
                return (DegreeModulesSelectionLimit) curricularRule;
            }
        }
        return null;
    }

    private Collection<CurricularRule> getCurricularRulesByExecutionInterval(final ExecutionInterval executionInterval) {
        final Collection<CurricularRule> result = new HashSet<CurricularRule>();
        for (final CurricularRule curricularRule : this.getCurricularRulesSet()) {
            if (curricularRule.isValid(executionInterval)) {
                result.add(curricularRule);
            }
        }
        return result;
    }

    private Collection<DegreeModule> getDegreeModulesByExecutionInterval(final ExecutionInterval executionInterval) {
        final Collection<DegreeModule> result = new HashSet<DegreeModule>();
        for (final Context context : this.getChildContextsSet()) {
            if (context.isValid(executionInterval)) {
                result.add(context.getChildDegreeModule());
            }
        }
        return result;
    }

    public boolean validate(CurricularCourse curricularCourse) {
        for (final Context context : this.getChildContextsSet()) {
            if (context.getChildDegreeModule() instanceof CurricularCourse) {
                CurricularCourse childCurricularCourse = (CurricularCourse) context.getChildDegreeModule();
                if (childCurricularCourse.isEquivalent(curricularCourse)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<Context> getContextsWithCurricularCourseByCurricularPeriod(final CurricularPeriod curricularPeriod,
            final ExecutionInterval executionSemester) {

        final Collection<Context> result = new HashSet<Context>();

        for (final Context context : this.getChildContextsSet()) {

            if (context.getChildDegreeModule().isLeaf() && context.getCurricularPeriod() != null
                    && context.getCurricularPeriod().equals(curricularPeriod) && context.isValid(executionSemester)) {

                result.add(context);
            }
        }
        return result;
    }

    public Set<DegreeModule> getOpenChildDegreeModulesByExecutionPeriod(final ExecutionInterval executionInterval) {
        final Set<DegreeModule> result = new HashSet<DegreeModule>();
        for (final Context context : getChildContextsSet()) {
            if (context.isOpen(executionInterval)) {
                result.add(context.getChildDegreeModule());
            }
        }
        return result;
    }

    /**
     * @deprecated use getParentContextsSet directly instead
     */
    @Override
    @Deprecated
    public Set<CourseGroup> getParentCourseGroups() {
        final Set<CourseGroup> result = new HashSet<CourseGroup>();
        for (final Context context : getParentContextsSet()) {
            result.add(context.getParentCourseGroup());
        }
        return result;
    }

    public Stream<CourseGroup> getParentCourseGroupStream() {
        return getParentContextsSet().stream().map(c -> c.getParentCourseGroup());
    }

    @Override
    public Double getMaxEctsCredits(final ExecutionInterval executionInterval) {
        final List<CreditsLimit> creditsLimitRules =
                (List<CreditsLimit>) getCurricularRules(CurricularRuleType.CREDITS_LIMIT, executionInterval);
        if (!creditsLimitRules.isEmpty()) {
            for (final CreditsLimit creditsLimit : creditsLimitRules) {
                if (getParentCourseGroupStream().anyMatch(g -> g == creditsLimit.getContextCourseGroup())) {
                    return creditsLimit.getMaximumCredits();
                }
            }
            return creditsLimitRules.iterator().next().getMaximumCredits();
        }

        final Collection<DegreeModule> modulesByExecutionPeriod = getOpenChildDegreeModulesByExecutionPeriod(executionInterval);
        final DegreeModulesSelectionLimit modulesSelectionLimit = getDegreeModulesSelectionLimitRule(executionInterval);
        if (modulesSelectionLimit != null) {
            return countMaxEctsCredits(modulesByExecutionPeriod, executionInterval, modulesSelectionLimit.getMaximumLimit());
        }

        return countMaxEctsCredits(modulesByExecutionPeriod, executionInterval, modulesByExecutionPeriod.size());
    }

    private Double countMaxEctsCredits(final Collection<DegreeModule> modulesByExecutionPeriod,
            final ExecutionInterval executionInterval, final Integer maximumLimit) {

        final List<Double> ectsCredits = new ArrayList<Double>();
        for (final DegreeModule degreeModule : modulesByExecutionPeriod) {
            ectsCredits.add(degreeModule.getMaxEctsCredits(executionInterval));
        }
        Collections.sort(ectsCredits, new ReverseComparator());
        return sumEctsCredits(ectsCredits, maximumLimit.intValue());
    }

    @Override
    public Double getMinEctsCredits(final ExecutionInterval executionInterval) {
        final List<CreditsLimit> creditsLimitRules =
                (List<CreditsLimit>) getCurricularRules(CurricularRuleType.CREDITS_LIMIT, executionInterval);
        if (!creditsLimitRules.isEmpty()) {
            for (final CreditsLimit creditsLimit : creditsLimitRules) {
                if (getParentCourseGroupStream().anyMatch(g -> g == creditsLimit.getContextCourseGroup())) {
                    return creditsLimit.getMinimumCredits();
                }
            }
            return creditsLimitRules.iterator().next().getMinimumCredits();
        }

        final Collection<DegreeModule> modulesByExecutionPeriod = getOpenChildDegreeModulesByExecutionPeriod(executionInterval);
        final DegreeModulesSelectionLimit modulesSelectionLimit = getDegreeModulesSelectionLimitRule(executionInterval);
        if (modulesSelectionLimit != null) {
            return countMinEctsCredits(modulesByExecutionPeriod, executionInterval, modulesSelectionLimit.getMinimumLimit());
        }

        return countMinEctsCredits(modulesByExecutionPeriod, executionInterval, modulesByExecutionPeriod.size());
    }

    private Double countMinEctsCredits(final Collection<DegreeModule> modulesByExecutionPeriod,
            final ExecutionInterval executionInterval, final Integer minimumLimit) {

        final List<Double> ectsCredits = new ArrayList<Double>();
        for (final DegreeModule degreeModule : modulesByExecutionPeriod) {
            ectsCredits.add(degreeModule.getMinEctsCredits(executionInterval));
        }
        Collections.sort(ectsCredits);
        return sumEctsCredits(ectsCredits, minimumLimit.intValue());
    }

    private Double sumEctsCredits(final List<Double> ectsCredits, int limit) {
        double result = 0d;
        final Iterator<Double> ectsCreditsIter = ectsCredits.iterator();
        for (; ectsCreditsIter.hasNext() && limit > 0; limit--) {
            result += ectsCreditsIter.next().doubleValue();
        }
        return Double.valueOf(result);
    }

    @Override
    public boolean hasDegreeModule(final DegreeModule degreeModule) {
        if (super.hasDegreeModule(degreeModule)) {
            return true;
        }
        for (final Context context : getChildContextsSet()) {
            if (context.getChildDegreeModule().hasDegreeModule(degreeModule)) {
                return true;
            }
        }
        return false;
    }

    public Context addCurricularCourse(final CurricularCourse curricularCourse, final CurricularPeriod curricularPeriod,
            final ExecutionInterval begin, final ExecutionInterval end) {
        return addContext(curricularCourse, curricularPeriod, begin, end);
    }

    public Context addContext(final DegreeModule degreeModule, final CurricularPeriod curricularPeriod,
            final ExecutionInterval begin, final ExecutionInterval end) {

        if (!allowChildWith(begin)) {
            throw new DomainException("degreeModule.cannot.add.context.with.begin.execution.period", getName(), begin.getName(),
                    begin.getExecutionYear().getYear());
        }
        return new Context(this, degreeModule, curricularPeriod, begin, end);
    }

    @Override
    public void getAllDegreeModules(final Collection<DegreeModule> degreeModules) {
        degreeModules.add(this);
        for (Context context : getChildContextsSet()) {
            context.getAllDegreeModules(degreeModules);
        }
    }

    public void getAllCoursesGroupse(final Set<CourseGroup> courseGroups) {
        for (final Context context : getChildContextsSet()) {
            context.addAllCourseGroups(courseGroups);
        }
    }

    public boolean allowChildWith(final ExecutionInterval executionSemester) {
        return getMinimumExecutionPeriod().isBeforeOrEquals(executionSemester);
    }

    public Set<Context> getChildContextsSortedByDegreeModuleName() {
        final Set<Context> contexts = new TreeSet<Context>(Context.COMPARATOR_BY_DEGREE_MODULE_NAME);
        contexts.addAll(getChildContextsSet());
        return contexts;
    }

    public Set<DegreeModule> getChildDegreeModules() {
        final Set<DegreeModule> result = new HashSet<DegreeModule>();
        for (final Context context : getChildContextsSet()) {
            result.add(context.getChildDegreeModule());
        }
        return result;
    }

    public Set<DegreeModule> getChildDegreeModulesValidOn(final ExecutionInterval executionSemester) {
        final Set<DegreeModule> result = new HashSet<DegreeModule>();
        for (final Context context : getValidChildContexts(executionSemester)) {
            result.add(context.getChildDegreeModule());
        }

        return result;
    }

    public Set<DegreeModule> getChildDegreeModulesValidOnExecutionAggregation(final ExecutionYear executionYear) {
        final Set<DegreeModule> result = new HashSet<DegreeModule>();
        for (final Context context : getValidChildContextsForExecutionAggregation(executionYear)) {
            result.add(context.getChildDegreeModule());
        }

        return result;
    }

    public Set<CurricularCourse> getChildCurricularCoursesValidOn(final ExecutionInterval executionInterval) {
        final Set<CurricularCourse> result = new HashSet<CurricularCourse>();

        for (final Context context : getValidChildContexts(executionInterval)) {
            if (context.getChildDegreeModule().isCurricularCourse()) {
                result.add((CurricularCourse) context.getChildDegreeModule());
            }
        }

        return result;
    }

    public boolean hasDegreeModuleOnChilds(final DegreeModule degreeModuleToSearch) {
        for (final Context context : getChildContextsSet()) {
            if (context.getChildDegreeModule() == degreeModuleToSearch) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyChildContextWithCurricularCourse() {
        for (final Context context : getChildContextsSet()) {
            if (context.getChildDegreeModule().isCurricularCourse()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCourseGroup() {
        return true;
    }

    @Override
    public Set<CurricularCourse> getAllCurricularCourses(final ExecutionInterval executionInterval) {
        final Set<CurricularCourse> result = new HashSet<CurricularCourse>();
        for (final Context context : getChildContextsSet()) {
            if (executionInterval == null || context.isOpen(executionInterval)) {
                result.addAll(context.getChildDegreeModule().getAllCurricularCourses(executionInterval));
            }
        }
        return result;
    }

    @Override
    public Set<CurricularCourse> getAllCurricularCourses() {
        return getAllCurricularCourses(null);
    }

    public Set<ExecutionYear> getBeginContextExecutionYears() {
        final Set<ExecutionYear> result = new HashSet<ExecutionYear>();
        for (final Context context : getChildContexts(CourseGroup.class)) {
            result.add(context.getBeginExecutionInterval().getExecutionYear());
            result.addAll(((CourseGroup) context.getChildDegreeModule()).getBeginContextExecutionYears());
        }
        return result;
    }

    public boolean hasAnyParentBranchCourseGroup() {

        if (isBranchCourseGroup()) {
            return true;
        } else {
            for (Context context : getParentContextsSet()) {
                if (context.getParentCourseGroup().hasAnyParentBranchCourseGroup()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void applyToCurricularCourses(final ExecutionYear executionYear, final Predicate predicate) {
        for (final Context context : getChildContextsSet()) {
            if (executionYear == null || context.isValidForExecutionAggregation(executionYear)) {
                final DegreeModule childDegreeModule = context.getChildDegreeModule();
                childDegreeModule.applyToCurricularCourses(executionYear, predicate);
            }
        }
    }

    public Context createContext(final ExecutionInterval begin, final ExecutionInterval end, final DegreeModule degreeModule,
            final CurricularPeriod curricularPeriod) {

        final Context context = new Context(this, degreeModule, curricularPeriod, begin, end);

        /**
         * Degree module requires a context first to answer about
         */
        if (degreeModule != null) {
            for (final Context parentContext : degreeModule.getParentContextsSet()) {
                if (parentContext.getParentCourseGroup().getParentDegreeCurricularPlan() != getParentDegreeCurricularPlan()) {
                    throw new DomainException("error.CourseGroup.mismatch.ParentDegreeCurricularPlan");
                }
            }
        }

        return context;
    }

    @Deprecated
    public boolean isOptionalCourseGroup() {
        return super.getIsOptional();
    }

    public Double getDefaultEcts(final ExecutionYear executionYear) {
        final CreditsLimit creditsLimit =
                (CreditsLimit) getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, null, executionYear);
        if (creditsLimit != null) {
            return creditsLimit.getMinimumCredits();
        }

        if (getDegreeType().hasExactlyOneCycleType()) {
            return getDegree().getEctsCredits();
        }

        throw new DomainException("error.CycleCourseGroup.cannot.calculate.default.ects.credits");
    }

    public String getDegreeNameWithTitleSuffix(final ExecutionYear executionYear, final Locale locale) {
        return getDegree().getNameFor(executionYear).getContent(locale);
    }

    final public String getGraduateTitle() {
        return getGraduateTitle(ExecutionYear.findCurrent(getDegree().getCalendar()), I18N.getLocale());
    }

    public String getGraduateTitleSuffix(final ExecutionYear executionYear, final Locale locale) {
        return null;
    }

    final public String getGraduateTitle(final ExecutionYear executionYear, final Locale locale) {
        return getProgramConclusion() == null ? null : getProgramConclusion().getGraduationTitle(locale,
                getDegreeNameWithTitleSuffix(executionYear, locale));
    }

    @Override
    public boolean isBranchCourseGroup() {
        return getBranchType() != null;
    }

}
