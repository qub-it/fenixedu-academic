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

import static org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel.ENROLMENT_WITH_RULES;
import static org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel.EXTRA_ENROLMENT;
import static org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel.STANDALONE_ENROLMENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.EnrolmentResultType;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.exceptions.EnrollmentDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class StudentCurricularPlanEnrolment {

    private static Logger logger = LoggerFactory.getLogger(StudentCurricularPlanEnrolment.class);

    protected EnrolmentContext enrolmentContext;

    protected StudentCurricularPlanEnrolment(final EnrolmentContext enrolmentContext) {
        checkParameters(enrolmentContext);
        this.enrolmentContext = enrolmentContext;
    }

    private void checkParameters(final EnrolmentContext enrolmentContext) {

        if (enrolmentContext == null) {
            throw new DomainException("error.StudentCurricularPlanEnrolment.invalid.enrolmentContext");
        }

        if (enrolmentContext.getStudentCurricularPlan() == null) {
            throw new DomainException("error.StudentCurricularPlanEnrolment.invalid.studentCurricularPlan");
        }

        if (!enrolmentContext.hasResponsiblePerson()) {
            throw new DomainException("error.StudentCurricularPlanEnrolment.enrolmentContext.invalid.person");
        }

    }

    final public RuleResult manage() {

        assertEnrolmentPreConditions();

        unEnrol();
        addEnroled();

        final Map<EnrolmentResultType, List<IDegreeModuleToEvaluate>> degreeModulesToEnrolMap =
                new HashMap<EnrolmentResultType, List<IDegreeModuleToEvaluate>>();
        final RuleResult result = evaluateDegreeModules(degreeModulesToEnrolMap);
        performEnrolments(degreeModulesToEnrolMap);

        return result;
    }

    protected void assertEnrolmentPreConditions() {
        if (isResponsiblePersonStudent()) {
            assertStudentEnrolmentPreConditions();
        }
    }

    protected Person getPerson() {
        return getStudent().getPerson();
    }

    protected void assertStudentEnrolmentPreConditions() {
        final CurricularRuleLevel ruleLevel = getCurricularRuleLevel();
        if (ruleLevel != ENROLMENT_WITH_RULES && ruleLevel != EXTRA_ENROLMENT && ruleLevel != STANDALONE_ENROLMENT) {
            throw new DomainException("error.StudentCurricularPlan.invalid.curricular.rule.level");
        }

        if (!getRegistration().hasActiveLastState(getExecutionInterval())) {
            throw new DomainException("error.StudentCurricularPlan.student.is.not.allowed.to.perform.enrol");
        }
    }

    private RuleResult evaluateDegreeModules(
            final Map<EnrolmentResultType, List<IDegreeModuleToEvaluate>> degreeModulesEnrolMap) {

        RuleResult finalResult = RuleResult.createInitialTrue();
        final Map<IDegreeModuleToEvaluate, Set<ICurricularRule>> rulesToEvaluate = getRulesToEvaluate();
        for (final Entry<IDegreeModuleToEvaluate, Set<ICurricularRule>> entry : rulesToEvaluate.entrySet()) {
            RuleResult result = evaluateRules(entry.getKey(), entry.getValue());
            finalResult = finalResult.and(result);
        }

        finalResult = evaluateExtraRules(finalResult);

        if (!finalResult.isFalse()) {
            for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : rulesToEvaluate.keySet()) {
                addDegreeModuleToEvaluateToMap(degreeModulesEnrolMap,
                        finalResult.getEnrolmentResultTypeFor(degreeModuleToEvaluate.getDegreeModule()), degreeModuleToEvaluate);
            }

        }

        if (finalResult.isFalse()) {
            throw new EnrollmentDomainException(finalResult);
        }

        return finalResult;
    }

    protected RuleResult evaluateExtraRules(final RuleResult actualResult) {
        // no extra rules to be executed
        return actualResult;

    }

    private RuleResult evaluateRules(final IDegreeModuleToEvaluate degreeModuleToEvaluate,
            final Set<ICurricularRule> curricularRules) {
        RuleResult ruleResult = RuleResult.createTrue(degreeModuleToEvaluate.getDegreeModule());

        for (final ICurricularRule rule : curricularRules) {
            ruleResult = ruleResult.and(rule.evaluate(degreeModuleToEvaluate, enrolmentContext));
        }

        return ruleResult;
    }

    private void addDegreeModuleToEvaluateToMap(final Map<EnrolmentResultType, List<IDegreeModuleToEvaluate>> result,
            final EnrolmentResultType enrolmentResultType, final IDegreeModuleToEvaluate degreeModuleToEnrol) {

        List<IDegreeModuleToEvaluate> information = result.get(enrolmentResultType);
        if (information == null) {
            result.put(enrolmentResultType, information = new ArrayList<IDegreeModuleToEvaluate>());
        }
        information.add(degreeModuleToEnrol);
    }

    /**
     * @deprecated use {@link #getExecutionInterval()}
     */
    @Deprecated
    protected ExecutionInterval getExecutionSemester() {
        return enrolmentContext.getExecutionPeriod();
    }

    protected ExecutionInterval getExecutionInterval() {
        return enrolmentContext.getExecutionPeriod();
    }

    protected ExecutionYear getExecutionYear() {
        return getExecutionSemester().getExecutionYear();
    }

    protected StudentCurricularPlan getStudentCurricularPlan() {
        return enrolmentContext.getStudentCurricularPlan();
    }

    protected Registration getRegistration() {
        return getStudentCurricularPlan().getRegistration();
    }

    protected RootCurriculumGroup getRoot() {
        return getStudentCurricularPlan().getRoot();
    }

    protected Student getStudent() {
        return getRegistration().getStudent();
    }

    protected DegreeCurricularPlan getDegreeCurricularPlan() {
        return getStudentCurricularPlan().getDegreeCurricularPlan();
    }

    protected CurricularRuleLevel getCurricularRuleLevel() {
        return enrolmentContext.getCurricularRuleLevel();
    }

    protected Person getResponsiblePerson() {
        return enrolmentContext.getResponsiblePerson();
    }

    protected boolean isResponsiblePersonStudent() {
        return getRegistration().getStudent().getPerson() == getResponsiblePerson();
    }

    abstract protected void unEnrol();

    abstract protected void addEnroled();

    abstract protected Map<IDegreeModuleToEvaluate, Set<ICurricularRule>> getRulesToEvaluate();

    abstract protected void performEnrolments(Map<EnrolmentResultType, List<IDegreeModuleToEvaluate>> degreeModulesToEnrolMap);

    // -------------------
    // static information
    // -------------------

    static public StudentCurricularPlanEnrolment createManager(final EnrolmentContext enrolmentContext) {
        return ENROLMENT_MANAGER_FACTORY.get().createManager(enrolmentContext);
    }

    public static void setEnrolmentManagerFactory(final Supplier<EnrolmentManagerFactory> input) {
        if (input != null && input.get() != null) {
            ENROLMENT_MANAGER_FACTORY = input;
        } else {
            logger.error("Could not set factory to null");
        }
    }

    static private Supplier<EnrolmentManagerFactory> ENROLMENT_MANAGER_FACTORY = () -> new EnrolmentManagerFactory() {

        @Override
        public StudentCurricularPlanEnrolment createManager(final EnrolmentContext enrolmentContext) {

            if (enrolmentContext.isNormal()) {
                return new StudentCurricularPlanEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isImprovement()) {
                return new StudentCurricularPlanImprovementOfApprovedEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isSpecialSeason()) {
                return new StudentCurricularPlanEnrolmentInSpecialSeasonEvaluationManager(enrolmentContext);

            } else if (enrolmentContext.isExtra()) {
                return new StudentCurricularPlanExtraEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isPropaeudeutics()) {
                return new StudentCurricularPlanPropaeudeuticsEnrolmentManager(enrolmentContext);

            } else if (enrolmentContext.isStandalone()) {
                return new StudentCurricularPlanStandaloneEnrolmentManager(enrolmentContext);
            }

            throw new DomainException("StudentCurricularPlanEnrolment");
        }

    };

    public static interface EnrolmentManagerFactory {

        public StudentCurricularPlanEnrolment createManager(final EnrolmentContext enrolmentContext);
    }

}
