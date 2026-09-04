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

import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.EnrolmentEvaluationContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.FenixDigestUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

import static org.fenixedu.academic.util.Bundle.APPLICATION;

public class EnrolmentEvaluation extends EnrolmentEvaluation_Base {

    public static final Comparator<EnrolmentEvaluation> COMPARATORY_BY_WHEN =
            Comparator.comparing(EnrolmentEvaluation::getWhenDateTime, Comparator.nullsFirst(Comparator.naturalOrder()));

    static final Comparator<EnrolmentEvaluation> COMPARATOR_BY_EXAM_DATE =
            Comparator.comparing(EnrolmentEvaluation::getExamDateYearMonthDay, Comparator.nullsFirst(Comparator.naturalOrder()));

    public EnrolmentEvaluation() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
        setGrade(Grade.createEmptyGrade());
        setContext(EnrolmentEvaluationContext.MARK_SHEET_EVALUATION);
    }

    public EnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season) {
        this();
        if (enrolment == null || season == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }

        if (enrolment.getEnrolmentEvaluationBySeason(season).count() > 0) {
            throw new DomainException("error.enrolmentEvaluation.duplicate.season");
        }

        setEnrolment(enrolment);
        setEvaluationSeason(season);
    }

    protected EnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season, EnrolmentEvaluationState evaluationState) {
        this(enrolment, season);
        if (evaluationState == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }
        setEnrolmentEvaluationState(evaluationState);
        setWhenDateTime(new DateTime());
    }

    public EnrollmentState getEnrollmentStateByGrade() {
        return getGrade().getEnrolmentState();
    }

    public boolean isFlunked() {
        return isFinal() && !isApproved();
    }

    public boolean isApproved() {
        return isFinal() && getEnrollmentStateByGrade() == EnrollmentState.APROVED;
    }

    public void edit(Person responsibleFor, Grade grade, Date availableDate, Date examDate) {
        if (responsibleFor == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }

        if (examDate != null) {
            setExamDateYearMonthDay(YearMonthDay.fromDateFields(examDate));

        } else if (grade.isEmpty()) {
            setExamDateYearMonthDay(null);

        } else {
            setExamDateYearMonthDay(YearMonthDay.fromDateFields(availableDate));
        }

        setGrade(grade);
        setGradeAvailableDateYearMonthDay(YearMonthDay.fromDateFields(availableDate));
        setPersonResponsibleForGrade(responsibleFor);

        generateCheckSum();
    }

    public void editImprovementExecutionInterval(final ExecutionInterval newInterval) {
        if (!getEvaluationSeason().isImprovement()) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(APPLICATION, "error.enrolmentEvaluation.improvement.invalidSeason"));
        }

        if (newInterval == null) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(APPLICATION, "error.enrolmentEvaluation.improvement.intervalIsRequired"));
        }

        final Enrolment enrolment = getEnrolment();

        if (newInterval.isBefore(enrolment.getExecutionInterval())) {
            throw new IllegalArgumentException(
                    BundleUtil.getString(APPLICATION, "error.enrolmentEvaluation.improvement.intervalIsBeforeEnrolment"));
        }

        final ExecutionInterval oldInterval = getExecutionInterval();

        if (oldInterval != null && oldInterval != newInterval && oldInterval != enrolment.getExecutionInterval()) {
            enrolment.findAttends(oldInterval).ifPresent(Attends::delete);
        }

        setExecutionPeriod(newInterval);

        if (enrolment.getExecutionInterval() != newInterval) {
            enrolment.getCurricularCourse().findExecutionCourses(newInterval).findAny().ifPresent(enrolment::findOrCreateAttends);
        }
    }

    public void removeGrade() {
        setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
        setGrade(Grade.createEmptyGrade());
        setExamDateYearMonthDay(null);
        setGradeAvailableDateYearMonthDay(null);
        setPersonResponsibleForGrade(null);
    }

    public void confirmSubmission(Person person, String observation) {
        if (!isTemporary()) {
            throw new DomainException("EnrolmentEvaluation.cannot.submit.not.temporary",
                    getEnrolment().getStudent().getPerson().getUsername());
        }

        if (!hasGrade()) {
            throw new DomainException("EnrolmentEvaluation.cannot.submit.with.empty.grade");
        }

        setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        setPerson(person);
        setObservation(observation);
        setWhenDateTime(new DateTime());

        this.getEnrolment().setEnrollmentState(getEnrolment().getGrade().getEnrolmentState());
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!isTemporary()) {
            blockers.add(
                    BundleUtil.getString(APPLICATION, "error.enrolmentEvaluation.isTemporary.or.hasConfirmedMarksheet"));
        }
    }

    public boolean isTemporary() {
        return EnrolmentEvaluationState.TEMPORARY_OBJ.equals(getEnrolmentEvaluationState());
    }

    public boolean isFinal() {
        return EnrolmentEvaluationState.FINAL_OBJ.equals(getEnrolmentEvaluationState());
    }

    public boolean isAnnuled() {
        return EnrolmentEvaluationState.ANNULED_OBJ.equals(getEnrolmentEvaluationState());
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (getEnrolment() != null && getExecutionInterval() != null && getExecutionInterval() != getEnrolment().getExecutionInterval() && getEnrolment().findEnrolmentEvaluations(
                getExecutionInterval()).count() == 1) {
            getEnrolment().findAttends(getExecutionInterval()).filter(Attends::isDeletable).ifPresent(Attends::delete);
        }

        setPersonResponsibleForGrade(null);
        setPerson(null);
        setEnrolment(null);
        setRectification(null);
        setRectified(null);

        setExecutionPeriod(null);
        setEvaluationSeason(null);

        setRootDomainObject(null);

        super.deleteDomainObject();
    }

    protected void generateCheckSum() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getExamDateYearMonthDay() != null ? getExamDateYearMonthDay().toString() : "")
                .append(getGradeValue());
        stringBuilder.append(getEvaluationSeason().getExternalId());
        stringBuilder.append(getEnrolment().getStudentCurricularPlan().getRegistration().getNumber());
        setCheckSum(FenixDigestUtils.createDigest(stringBuilder.toString()));
    }

    @Override
    public String getGradeValue() {
        return getGrade().getValue();
    }

    @Override
    public void setGrade(final Grade grade) {

        if (isFinal()) {
            throw new DomainException("EnrolmentEvaluation.cannot.set.grade.final");
        }

        super.setGrade(grade);

        // TODO remove this once we're sure migration to Grade went OK
        super.setGradeValue(grade.getValue());
    }

    // Used in reports
    @Deprecated
    public Registration getStudent() {
        return this.getRegistration();
    }

    public Registration getRegistration() {
        return getStudentCurricularPlan().getRegistration();
    }

    public DegreeCurricularPlan getDegreeCurricularPlan() {
        return getStudentCurricularPlan().getDegreeCurricularPlan();
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        return getEnrolment().getStudentCurricularPlan();
    }

    public boolean hasGrade() {
        return !getGrade().isEmpty();
    }

    public ExecutionInterval getExecutionInterval() {
        if (getEvaluationSeason().isImprovement()) {
            return super.getExecutionPeriod();
        }

        return getEnrolment().getExecutionInterval();
    }

    public LocalDate getExamLocalDate() {
        return Optional.ofNullable(getExamDateYearMonthDay()).map(YearMonthDay::toLocalDate).orElse(null);
    }

    @Deprecated
    public void setExamDate(java.util.Date date) {
        if (date == null) {
            setExamDateYearMonthDay(null);
        } else {
            setExamDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    public LocalDate getGradeAvailableLocalDate() {
        return Optional.ofNullable(getGradeAvailableDateYearMonthDay()).map(YearMonthDay::toLocalDate).orElse(null);

    }

    @Deprecated
    public void setGradeAvailableDate(java.util.Date date) {
        if (date == null) {
            setGradeAvailableDateYearMonthDay(null);
        } else {
            setGradeAvailableDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

}
