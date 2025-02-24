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
package org.fenixedu.academic.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.enrolment.EnroledOptionalEnrolment;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.log.EnrolmentActionType;
import org.fenixedu.academic.domain.log.OptionalEnrolmentLog;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;

public class OptionalEnrolment extends OptionalEnrolment_Base {

    protected OptionalEnrolment() {
        super();
    }

    public OptionalEnrolment(StudentCurricularPlan studentCurricularPlan, CurriculumGroup curriculumGroup,
            CurricularCourse curricularCourse, ExecutionInterval executionInterval, EnrollmentCondition enrolmentCondition,
            String createdBy, OptionalCurricularCourse optionalCurricularCourse) {

        if (studentCurricularPlan == null || curriculumGroup == null || curricularCourse == null || executionInterval == null
                || enrolmentCondition == null || createdBy == null || optionalCurricularCourse == null) {
            throw new DomainException("invalid arguments");
        }
        checkInitConstraints(studentCurricularPlan, curricularCourse, executionInterval, optionalCurricularCourse);
        // TODO: check this
        // validateDegreeModuleLink(curriculumGroup, curricularCourse);
        initializeAsNew(studentCurricularPlan, curriculumGroup, curricularCourse, executionInterval, enrolmentCondition,
                createdBy);
        setOptionalCurricularCourse(optionalCurricularCourse);
        createCurriculumLineLog(EnrolmentActionType.ENROL);
    }

    protected void checkInitConstraints(StudentCurricularPlan studentCurricularPlan, CurricularCourse curricularCourse,
            ExecutionInterval executionInterval, OptionalCurricularCourse optionalCurricularCourse) {
        super.checkInitConstraints(studentCurricularPlan, curricularCourse, executionInterval);

        final OptionalEnrolment optionalEnrolment =
                (OptionalEnrolment) studentCurricularPlan.findEnrolmentFor(optionalCurricularCourse, executionInterval);
        if (optionalEnrolment != null && optionalEnrolment.isValid(executionInterval)) {
            throw new DomainException("error.OptionalEnrolment.duplicate.enrolment", optionalCurricularCourse.getName());

        }

        if (optionalCurricularCourse.getParentContexts(executionInterval).isEmpty()) {
            throw new DomainException("error.Enrolment.no.valid.context.found");
        }
    }

    @Override
    protected void createCurriculumLineLog(final EnrolmentActionType type) {
        new OptionalEnrolmentLog(type, getRegistration(), getCurricularCourse(), getOptionalCurricularCourse(),
                getExecutionInterval(), getCurrentUser());
    }

    @Override
    final public boolean isApproved(final CurricularCourse curricularCourse, final ExecutionInterval executionInterval) {
        if (executionInterval == null || getExecutionInterval().isBeforeOrEquals(executionInterval)) {
            return isApproved() && hasCurricularCourseOrOptionalCurricularCourse(curricularCourse, executionInterval);
        } else {
            return false;
        }
    }

    private boolean hasCurricularCourseOrOptionalCurricularCourse(final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval) {
        return hasCurricularCourse(getCurricularCourse(), curricularCourse, executionInterval)
                || hasCurricularCourse(getOptionalCurricularCourse(), curricularCourse, executionInterval);
    }

    @Override
    final public boolean isEnroledInExecutionPeriod(CurricularCourse curricularCourse, ExecutionInterval executionInterval) {
        return isValid(executionInterval) && (this.getCurricularCourse()
                .isEquivalent(curricularCourse) || this.getOptionalCurricularCourse().equals(curricularCourse));
    }

    @Override
    public boolean isOptional() {
        return true;
    }

    @Override
    public LocalizedString getName() {
        final ExecutionInterval executionInterval = getExecutionInterval();
        return new LocalizedString()
                .with(org.fenixedu.academic.util.LocaleUtils.PT, this.getOptionalCurricularCourse().getName(executionInterval))
                .with(org.fenixedu.academic.util.LocaleUtils.EN, this.getOptionalCurricularCourse().getNameEn(executionInterval));
    }

    @Override
    public LocalizedString getPresentationName() {

        final String namePt = String.format("%s (%s)", getOptionalCurricularCourse().getName(getExecutionInterval()),
                getCurricularCourse().getName(getExecutionInterval()));

        final String nameEn = String.format("%s (%s)", getOptionalCurricularCourse().getNameEn(getExecutionInterval()),
                getCurricularCourse().getNameEn(getExecutionInterval()));

        return new LocalizedString().with(org.fenixedu.academic.util.LocaleUtils.PT, namePt)
                .with(org.fenixedu.academic.util.LocaleUtils.EN, nameEn);
    }

    @Override
    public boolean hasDegreeModule(final DegreeModule degreeModule) {
        return super.hasDegreeModule(degreeModule) || hasOptionalCurricularCourse(degreeModule);
    }

    private boolean hasOptionalCurricularCourse(final DegreeModule degreeModule) {
        return getOptionalCurricularCourse() == degreeModule;
    }

    @Override
    protected void deleteInformation() {
        super.deleteInformation();
        setOptionalCurricularCourse(null);
    }

    @Override
    public Set<IDegreeModuleToEvaluate> getDegreeModulesToEvaluate(ExecutionInterval executionInterval) {
        if (isValid(executionInterval) && isEnroled()) {
            final Set<IDegreeModuleToEvaluate> result = new HashSet<IDegreeModuleToEvaluate>(1);
            result.add(new EnroledOptionalEnrolment(this, getOptionalCurricularCourse(), executionInterval));
            return result;
        }
        return Collections.emptySet();

    }

    public StringBuilder print(String tabs) {
        final StringBuilder builder = new StringBuilder();
        builder.append(tabs);
        builder.append("[OE ").append(getCode()).append(" - ").append(getOptionalCurricularCourse().getName()).append("(")
                .append(getDegreeModule().getName()).append(")").append(" ").append(isApproved()).append(" - ")
                .append(getEctsCreditsForCurriculum()).append(" ects - ").append(" - ")
                .append(getExecutionInterval().getQualifiedName()).append(" ]\n");
        return builder;
    }

    /**
     * 
     * After create new OptionalEnrolment, must delete Enrolment (to delete
     * Enrolment disconnect at least: ProgramCertificateRequests,
     * CourseLoadRequests, ExamDateCertificateRequests)
     * 
     * @param enrolment
     * @param curriculumGroup
     *            : new CurriculumGroup for OptionalEnrolment
     * @param optionalCurricularCourse
     *            : choosed OptionalCurricularCourse
     * @return OptionalEnrolment
     */
    static OptionalEnrolment createBasedOn(final Enrolment enrolment, final CurriculumGroup curriculumGroup,
            final OptionalCurricularCourse optionalCurricularCourse) {
        checkParameters(enrolment, curriculumGroup, optionalCurricularCourse);

        final OptionalEnrolment optionalEnrolment = new OptionalEnrolment();
        optionalEnrolment.setCurricularCourse(enrolment.getCurricularCourse());
        optionalEnrolment.setWeigth(enrolment.getWeigth());
        optionalEnrolment.setEnrollmentState(enrolment.getEnrollmentState());
        optionalEnrolment.setExecutionPeriod(enrolment.getExecutionPeriod());
        optionalEnrolment.setEvaluationSeason(enrolment.getEvaluationSeason());
        optionalEnrolment.setCreatedBy(Authenticate.getUser().getUsername());
        optionalEnrolment.setCreationDateDateTime(enrolment.getCreationDateDateTime());
        optionalEnrolment.setEnrolmentCondition(enrolment.getEnrolmentCondition());
        optionalEnrolment.setCurriculumGroup(curriculumGroup);
        optionalEnrolment.setOptionalCurricularCourse(optionalCurricularCourse);

        optionalEnrolment.getEvaluationsSet().addAll(enrolment.getEvaluationsSet());
        optionalEnrolment.getEnrolmentWrappersSet().addAll(enrolment.getEnrolmentWrappersSet());
        changeAttends(enrolment, optionalEnrolment);
        optionalEnrolment.createCurriculumLineLog(EnrolmentActionType.ENROL);

        return optionalEnrolment;
    }

    private static void checkParameters(final Enrolment enrolment, final CurriculumGroup curriculumGroup,
            final OptionalCurricularCourse optionalCurricularCourse) {
        if (enrolment == null || enrolment.isOptional()) {
            throw new DomainException("error.OptionalEnrolment.invalid.enrolment");
        }
        if (curriculumGroup == null) {
            throw new DomainException("error.OptionalEnrolment.invalid.curriculumGroup");
        }
        if (optionalCurricularCourse == null) {
            throw new DomainException("error.OptionalEnrolment.invalid.optional.curricularCourse");
        }
    }

}
