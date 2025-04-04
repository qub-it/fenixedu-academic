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
package org.fenixedu.academic.domain.enrolment;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.EnrolmentModel;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroupType;
import org.fenixedu.academic.dto.administrativeOffice.studentEnrolment.NoCourseGroupEnrolmentBean;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;

public class EnrolmentContext {

    private StudentCurricularPlan studentCurricularPlan;

    private ExecutionInterval executionInterval;

    private final Set<IDegreeModuleToEvaluate> degreeModulesToEvaluate;

    private final List<CurriculumModule> curriculumModulesToRemove;

    private CurricularRuleLevel curricularRuleLevel;

    private EvaluationSeason evaluationSeason;

    private final User userView;

    private Map<String, Object> extraParameters = new HashMap<>();

    public EnrolmentContext(final StudentCurricularPlan studentCurricularPlan, final ExecutionInterval executionInterval,
            final Set<IDegreeModuleToEvaluate> degreeModulesToEnrol, final List<CurriculumModule> curriculumModulesToRemove,
            final CurricularRuleLevel curricularRuleLevel) {

        this.userView = Authenticate.getUser();
        this.studentCurricularPlan = studentCurricularPlan;

        this.degreeModulesToEvaluate = new HashSet<IDegreeModuleToEvaluate>();
        for (final IDegreeModuleToEvaluate moduleToEnrol : degreeModulesToEnrol) {
            if (curriculumModulesToRemove.contains(moduleToEnrol.getCurriculumGroup())) {
                throw new DomainException(
                        "error.StudentCurricularPlan.cannot.remove.enrollment.on.curriculum.group.because.other.enrollments.depend.on.it",
                        moduleToEnrol.getCurriculumGroup().getName().getContent());
            }

            this.addDegreeModuleToEvaluate(moduleToEnrol);
        }

        this.executionInterval = executionInterval;
        this.curriculumModulesToRemove = curriculumModulesToRemove;
        this.curricularRuleLevel = curricularRuleLevel;
    }

    public EnrolmentContext(final StudentCurricularPlan studentCurricularPlan, final ExecutionInterval executionInterval,
            final Set<IDegreeModuleToEvaluate> degreeModulesToEnrol, final List<CurriculumModule> curriculumModulesToRemove,
            final CurricularRuleLevel curricularRuleLevel, final EvaluationSeason evaluationSeason) {
        this(studentCurricularPlan, executionInterval, degreeModulesToEnrol, curriculumModulesToRemove, curricularRuleLevel);
        setEvaluationSeason(evaluationSeason);
    }

    public Set<IDegreeModuleToEvaluate> getDegreeModulesToEvaluate() {
        return degreeModulesToEvaluate;
    }

    public Set<IDegreeModuleToEvaluate> getAllChildDegreeModulesToEvaluateFor(final DegreeModule degreeModule) {
        final Set<IDegreeModuleToEvaluate> result = new HashSet<IDegreeModuleToEvaluate>();
        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : this.degreeModulesToEvaluate) {
            if (degreeModuleToEvaluate.isEqualOrHasParent(degreeModule)) {
                result.add(degreeModuleToEvaluate);
            }
        }

        return result;
    }

    public void addDegreeModuleToEvaluate(final IDegreeModuleToEvaluate degreeModuleToEvaluate) {
        getDegreeModulesToEvaluate().add(degreeModuleToEvaluate);
    }

    public boolean hasDegreeModulesToEvaluate() {
        return degreeModulesToEvaluate != null && !degreeModulesToEvaluate.isEmpty();
    }

    public ExecutionInterval getExecutionPeriod() {
        return executionInterval;
    }

    public void setExecutionPeriod(ExecutionInterval executionInterval) {
        this.executionInterval = executionInterval;
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        return studentCurricularPlan;
    }

    public Registration getRegistration() {
        return studentCurricularPlan.getRegistration();
    }

    public void setStudentCurricularPlan(StudentCurricularPlan studentCurricularPlan) {
        this.studentCurricularPlan = studentCurricularPlan;
    }

    public List<CurriculumModule> getToRemove() {
        return curriculumModulesToRemove;
    }

    public CurricularRuleLevel getCurricularRuleLevel() {
        return curricularRuleLevel;
    }

    public void setCurricularRuleLevel(CurricularRuleLevel curricularRuleLevel) {
        this.curricularRuleLevel = curricularRuleLevel;
    }

    public EvaluationSeason getEvaluationSeason() {
        return evaluationSeason;
    }

    public void setEvaluationSeason(final EvaluationSeason input) {
        this.evaluationSeason = input;
    }

    public Person getResponsiblePerson() {
        return userView.getPerson();
    }

    public boolean hasResponsiblePerson() {
        return getResponsiblePerson() != null;
    }

    public boolean isResponsiblePersonStudent() {
        return RoleType.STUDENT.isMember(userView.getPerson().getUser());
    }

    public boolean isRegistrationFromResponsiblePerson() {
        return getResponsiblePerson() == getRegistration().getPerson();
    }

    public boolean isNormal() {
        return getCurricularRuleLevel().isNormal();
    }

    public boolean isExtra() {
        return getCurricularRuleLevel() == CurricularRuleLevel.EXTRA_ENROLMENT;
    }

    public boolean isStandalone() {
        return getCurricularRuleLevel() == CurricularRuleLevel.STANDALONE_ENROLMENT
                || getCurricularRuleLevel() == CurricularRuleLevel.STANDALONE_ENROLMENT_NO_RULES;
    }

    public boolean isEnrolmentWithoutRules() {
        return getCurricularRuleLevel() == CurricularRuleLevel.ENROLMENT_NO_RULES;
    }

    public boolean isPhdDegree() {
        return studentCurricularPlan.getDegreeType().isAdvancedSpecializationDiploma();
    }

    public boolean isToEvaluateRulesByYear() {
        return getStudentCurricularPlan().getDegreeCurricularPlan().getCurricularRuleValidationType() == EnrolmentModel.YEAR;
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionPeriod().getExecutionYear();
    }

    public SortedSet<ExecutionInterval> getExecutionIntervalsToEvaluate() {
        final SortedSet<ExecutionInterval> result = new TreeSet<>(ExecutionInterval.COMPARATOR_BY_BEGIN_DATE);
        result.addAll(
                isToEvaluateRulesByYear() ? getExecutionYear().getChildIntervals() : Collections.singleton(getExecutionPeriod()));

        return result;
    }

    public void addExtraParameter(String key, Object value) {
        extraParameters.put(key, value);
    }

    public <T> T getExtraParameter(String key) {
        return (T) extraParameters.get(key);
    }

    @SuppressWarnings("unchecked")
    static public EnrolmentContext createForVerifyWithRules(final StudentCurricularPlan studentCurricularPlan,
            final ExecutionInterval executionInterval) {
        return createForVerifyWithRules(studentCurricularPlan, executionInterval, Collections.EMPTY_SET);
    }

    @SuppressWarnings("unchecked")
    static public EnrolmentContext createForVerifyWithRules(final StudentCurricularPlan studentCurricularPlan,
            final ExecutionInterval executionInterval, final Set<IDegreeModuleToEvaluate> degreeModulesToEvaluate) {
        return new EnrolmentContext(studentCurricularPlan, executionInterval, degreeModulesToEvaluate, Collections.EMPTY_LIST,
                CurricularRuleLevel.ENROLMENT_WITH_RULES);
    }

    @SuppressWarnings("unchecked")
    static public EnrolmentContext createForNoCourseGroupCurriculumGroupEnrolment(
            final StudentCurricularPlan studentCurricularPlan, final NoCourseGroupEnrolmentBean bean) {

        final IDegreeModuleToEvaluate moduleToEvaluate = new ExternalCurricularCourseToEnrol(
                readOrCreateNoCourseGroupCurriculumGroup(studentCurricularPlan, bean.getGroupType()),
                bean.getSelectedCurricularCourse(), bean.getExecutionPeriod());

        return new EnrolmentContext(studentCurricularPlan, bean.getExecutionPeriod(), Collections.singleton(moduleToEvaluate),
                Collections.EMPTY_LIST, bean.getCurricularRuleLevel());
    }

    static private NoCourseGroupCurriculumGroup readOrCreateNoCourseGroupCurriculumGroup(
            final StudentCurricularPlan studentCurricularPlan, final NoCourseGroupCurriculumGroupType groupType) {
        NoCourseGroupCurriculumGroup group = studentCurricularPlan.getNoCourseGroupCurriculumGroup(groupType);
        if (group == null) {
            group = studentCurricularPlan.createNoCourseGroupCurriculumGroup(groupType);
        }
        return group;
    }

}
