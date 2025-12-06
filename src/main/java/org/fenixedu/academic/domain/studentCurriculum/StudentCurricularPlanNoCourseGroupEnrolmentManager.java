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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.qubit.terra.framework.services.ServiceProvider;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.EnrolmentResultType;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;

import com.google.common.collect.Sets;

public class StudentCurricularPlanNoCourseGroupEnrolmentManager extends StudentCurricularPlanEnrolment {

    public static String STANDALONE_ENROLMENT = "STANDALONE_ENROLMENT";
    public static String EXTRACURRICULAR_ENROLMENT = "EXTRACURRICULAR_ENROLMENT";

    public StudentCurricularPlanNoCourseGroupEnrolmentManager(final EnrolmentContext enrolmentContext) {
        super(enrolmentContext);
    }

    @Override
    protected void assertEnrolmentPreConditions() {
        super.assertEnrolmentPreConditions();

        checkEnrolingDegreeModules();
    }

    private void checkEnrolingDegreeModules() {
        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : enrolmentContext.getDegreeModulesToEvaluate()) {
            if (degreeModuleToEvaluate.isEnroling()) {
                if (!degreeModuleToEvaluate.getDegreeModule().isCurricularCourse()) {
                    throw new DomainException(
                            "error.StudentCurricularPlanPropaeudeuticsEnrolmentManager.can.only.enrol.in.curricularCourses");
                }
                checkIDegreeModuleToEvaluate((CurricularCourse) degreeModuleToEvaluate.getDegreeModule());
            }
        }
    }

    private void checkIDegreeModuleToEvaluate(final CurricularCourse curricularCourse) {
        if (getStudentCurricularPlan().isApproved(curricularCourse, getExecutionSemester())) {
            throw new DomainException("error.already.aproved", curricularCourse.getName());
        }

        if (getStudentCurricularPlan().isEnroledInExecutionPeriod(curricularCourse, getExecutionSemester())) {
            throw new DomainException("error.already.enroled.in.executionPeriod", curricularCourse.getName(),
                    getExecutionSemester().getQualifiedName());
        }
    }

    @Override
    protected void addEnroled() {
        addEnroledFromStudentCurricularPlan();
        addEnroledFromNoCourseGroups();
    }

    private void addEnroledFromStudentCurricularPlan() {
        for (final ExecutionInterval interval : enrolmentContext.getExecutionIntervalsToEvaluate()) {
            for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : getStudentCurricularPlan().getDegreeModulesToEvaluate(
                    interval)) {
                enrolmentContext.addDegreeModuleToEvaluate(degreeModuleToEvaluate);
            }
        }
    }

    private void addEnroledFromNoCourseGroups() {
        final Set<NoCourseGroupCurriculumGroup> groupsToEvaluate =
                getStudentCurricularPlan().getNoCourseGroupCurriculumGroups().stream()
                        .filter(cg -> cg.isStandalone() || cg.isExtraCurriculum()).collect(Collectors.toSet());
        for (final CurriculumGroup group : groupsToEvaluate) {
            for (final CurriculumLine curriculumLine : group.getChildCurriculumLines()) {
                for (final ExecutionInterval interval : enrolmentContext.getExecutionIntervalsToEvaluate()) {
                    for (final IDegreeModuleToEvaluate module : curriculumLine.getDegreeModulesToEvaluate(interval)) {
                        enrolmentContext.addDegreeModuleToEvaluate(module);
                    }
                }
            }
        }
    }

    @Override
    protected Map<IDegreeModuleToEvaluate, Set<ICurricularRule>> getRulesToEvaluate() {
        final Map<IDegreeModuleToEvaluate, Set<ICurricularRule>> result =
                new HashMap<IDegreeModuleToEvaluate, Set<ICurricularRule>>();
        for (final IDegreeModuleToEvaluate toEvaluate : enrolmentContext.getDegreeModulesToEvaluate()) {
            if (toEvaluate.canCollectRules() || (toEvaluate.getCurriculumGroup() != null && (
                    toEvaluate.getCurriculumGroup().isStandalone() || toEvaluate.getCurriculumGroup().isExtraCurriculum()))) {
                result.put(toEvaluate, toEvaluate.getCurriculumGroup().getRootCurriculumGroup()
                        .getCurricularRules(toEvaluate.getExecutionInterval()));
            }
        }

        return result;
    }

    @Override
    protected void performEnrolments(Map<EnrolmentResultType, List<IDegreeModuleToEvaluate>> degreeModulesToEnrolMap) {
        final Set<Enrolment> enrolmentsToNotify = Sets.newHashSet();

        for (final Entry<EnrolmentResultType, List<IDegreeModuleToEvaluate>> entry : degreeModulesToEnrolMap.entrySet()) {
            for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : entry.getValue()) {
                if (degreeModuleToEvaluate.isEnroling() && degreeModuleToEvaluate.getDegreeModule().isCurricularCourse()) {
                    final CurricularCourse curricularCourse = (CurricularCourse) degreeModuleToEvaluate.getDegreeModule();

                    checkIDegreeModuleToEvaluate(curricularCourse);
                    final Enrolment enrolment =
                            new Enrolment(getStudentCurricularPlan(), degreeModuleToEvaluate.getCurriculumGroup(),
                                    curricularCourse, getExecutionSemester(), EnrollmentCondition.VALIDATED,
                                    getResponsiblePerson().getUsername());

                    enrolmentsToNotify.add(enrolment);
                }
            }
        }

        for (final Enrolment enrolment : enrolmentsToNotify) {
            if (enrolment.isStandalone()) {
                Signal.emit(STANDALONE_ENROLMENT, new DomainObjectEvent<>(enrolment));
            } else if (enrolment.isExtraCurricular()) {
                Signal.emit(EXTRACURRICULAR_ENROLMENT, new DomainObjectEvent<>(enrolment));
            }
        }

        getRegistration().updateEnrolmentDate(getExecutionYear());
    }

    @Override
    protected void unEnrol() {

        // First remove Enrolments
        for (final CurriculumModule curriculumModule : enrolmentContext.getToRemove()) {
            if (curriculumModule.isLeaf()) {
                if (curriculumModule.getCurriculumGroup().isStandalone()) {
                    ServiceProvider.getService(ITreasuryBridgeAPI.class).standaloneUnenrolment((Enrolment) curriculumModule);
                } else if (curriculumModule.getCurriculumGroup().isExtraCurriculum()) {
                    ServiceProvider.getService(ITreasuryBridgeAPI.class).extracurricularUnenrolment((Enrolment) curriculumModule);
                }

                curriculumModule.delete();
            }
        }

        // After, remove CurriculumGroups
        for (final CurriculumModule curriculumModule : enrolmentContext.getToRemove()) {
            if (!curriculumModule.isLeaf()) {
                curriculumModule.delete();
            }
        }
    }

}
