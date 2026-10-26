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
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.ExternalCurricularCourse;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.RegimeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

public class ExternalEnrolment extends ExternalEnrolment_Base implements IEnrolment {

    public static final Comparator<ExternalEnrolment> COMPARATOR_BY_NAME =
            Comparator.comparing(ExternalEnrolment::getName).thenComparing(ExternalEnrolment::getExternalId);

    public static final Comparator<ExternalEnrolment> COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE =
            ((Comparator<ExternalEnrolment>) COMPARATOR_BY_EXECUTION_PERIOD::compare).thenComparing(
                    COMPARATOR_BY_APPROVEMENT_DATE);

    protected ExternalEnrolment() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setWhenCreated(new DateTime());
        if (AccessControl.getPerson() != null) {
            setCreatedBy(AccessControl.getPerson().getUsername());
        }
    }

    public ExternalEnrolment(final Registration registration, final ExternalCurricularCourse externalCurricularCourse,
            final Grade grade, final ExecutionInterval executionInterval, final YearMonthDay evaluationDate,
            final Double ectsCredits) {
        this();

        checkConstraints(registration, externalCurricularCourse, executionInterval, grade, ectsCredits);
        checkIfCanCreateExternalEnrolment(registration, externalCurricularCourse);

        setExternalCurricularCourse(externalCurricularCourse);
        setFields(registration, grade, executionInterval, evaluationDate, ectsCredits);
    }

    private void checkIfCanCreateExternalEnrolment(final Registration registration,
            final ExternalCurricularCourse externalCurricularCourse) {
        final boolean alreadyExists = registration.getExternalEnrolmentsSet().stream()
                .anyMatch(e -> e.getExternalCurricularCourse() == externalCurricularCourse);

        if (alreadyExists) {
            throw new DomainException(
                    "error.studentCurriculum.ExternalEnrolment.already.exists.externalEnrolment.for.externalCurricularCourse",
                    externalCurricularCourse.getName());
        }
    }

    private void checkConstraints(final Registration registration, final ExternalCurricularCourse externalCurricularCourse,
            final ExecutionInterval executionInterval, final Grade grade, final Double ectsCredits) {
        if (registration == null) {
            throw new DomainException("error.externalEnrolment.student.cannot.be.null");
        }
        if (externalCurricularCourse == null) {
            throw new DomainException("error.externalEnrolment.externalCurricularCourse.cannot.be.null");
        }
        if (executionInterval == null) {
            throw new DomainException("error.externalEnrolment.executionPeriod.cannot.be.null");
        }
        if (grade == null || grade.isEmpty()) {
            throw new DomainException("error.externalEnrolment.invalid.grade");
        }
        if (ectsCredits == null) {
            throw new DomainException("error.externalEnrolment.ectsCredits.cannot.be.null");
        }
    }

    public void edit(final Registration registration, final Grade grade, final ExecutionInterval executionInterval,
            final YearMonthDay evaluationDate, final Double ectsCredits) {

        checkConstraints(registration, getExternalCurricularCourse(), executionInterval, grade, ectsCredits);
        if (registration != getRegistration()) {
            checkIfCanCreateExternalEnrolment(registration, getExternalCurricularCourse());
        }

        setFields(registration, grade, executionInterval, evaluationDate, ectsCredits);
    }

    private void setFields(final Registration registration, final Grade grade, final ExecutionInterval executionInterval,
            final YearMonthDay evaluationDate, final Double ectsCredits) {
        setRegistration(registration);
        setGrade(grade);
        setExecutionPeriod(executionInterval);
        setEvaluationDate(evaluationDate);
        setEctsCredits(ectsCredits);
    }

    @Override
    public LocalizedString getName() {
        //External enrolments only have one name
        final String name = getExternalCurricularCourse().getName();
        final LocalizedString.Builder builder = new LocalizedString.Builder();
        CoreConfiguration.supportedLocales().forEach(locale -> builder.with(locale, name));
        return builder.build();
    }

    @Override
    public String getCode() {
        return getExternalCurricularCourse().getCode();
    }

    public String getFullPathName() {
        return getExternalCurricularCourse().getFullPathName();
    }

    @Override
    public String getDescription() {
        return getFullPathName();
    }

    @Override
    public void delete() {
        checkRulesToDelete();

        setExecutionPeriod(null);
        setExternalCurricularCourse(null);
        setRegistration(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    private void checkRulesToDelete() {
        if (!getEnrolmentWrappersSet().isEmpty()) {
            throw new DomainException("error.Enrolment.is.origin.in.some.Equivalence");
        }
    }

    @Override
    final public boolean isApproved() {
        return true;
    }

    @Override
    final public boolean isEnroled() {
        return true;
    }

    @Override
    final public boolean isExternalEnrolment() {
        return true;
    }

    @Override
    final public boolean isEnrolment() {
        return false;
    }

    @Override
    final public ExecutionYear getExecutionYear() {
        return getExecutionInterval() != null ? getExecutionInterval().getExecutionYear() : null;
    }

    @Override
    final public YearMonthDay getApprovementDate() {
        return getEvaluationDate() == null && getExecutionInterval() != null ? getExecutionInterval()
                .getEndDateYearMonthDay() : getEvaluationDate();
    }

    @Override
    public Unit getAcademicUnit() {
        return getExternalCurricularCourse().getAcademicUnit();
    }

    @Override
    public String getGradeValue() {
        return getGrade().getValue();
    }

    @Override
    final public BigDecimal getEctsCreditsForCurriculum() {
        return BigDecimal.valueOf(getEctsCredits());
    }

    @Override
    public Set<CurriculumLine> getCurriculumLinesForCurriculum(final StudentCurricularPlan studentCurricularPlan) {
        return studentCurricularPlan.getCreditsSet().stream()
                .filter(c -> c.getEnrolmentsSet().stream().anyMatch(ew -> ew.getIEnrolment() == this))
                .flatMap(c -> c.getDismissalsSet().stream()).collect(Collectors.toSet());
    }

    @Override
    public Double getWeigth() {
        return getEctsCredits();
    }

    @Override
    final public BigDecimal getWeigthForCurriculum() {
        return BigDecimal.valueOf(getWeigth());
    }

    @Override
    public boolean isAnual() {
        return getRegime() == RegimeType.ANUAL;
    }

    @Override
    public String getEnrolmentTypeName() {
        return "COMPULSORY_ENROLMENT";
    }

    @Override
    public LocalizedString getPresentationName() {
        return getName();
    }

    @Override
    public boolean hasExecutionPeriod() {
        return getExecutionPeriod() != null;
    }

    /**
     * @deprecated use {@link #getExecutionInterval()} instead.
     */
    @Deprecated
    @Override
    public ExecutionInterval getExecutionPeriod() {
        return getExecutionInterval();
    }

    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionPeriod();
    }

    @Override
    public DateTime getCreationDateDateTime() {
        return getWhenCreated();
    }

}
