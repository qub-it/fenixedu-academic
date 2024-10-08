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

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.EnrolmentEvaluationContext;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.exceptions.EnrolmentNotPayedException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.treasury.IImprovementTreasuryEvent;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.FenixDigestUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import static org.fenixedu.academic.util.Bundle.APPLICATION;

public class EnrolmentEvaluation extends EnrolmentEvaluation_Base {

    public static final Comparator<EnrolmentEvaluation> COMPARATORY_BY_WHEN = new Comparator<EnrolmentEvaluation>() {

        @Override
        public int compare(EnrolmentEvaluation o1, EnrolmentEvaluation o2) {
            final DateTime o1When = o1.getWhenDateTime();
            final DateTime o2When = o2.getWhenDateTime();

            if (o1When != null && o2When != null) {
                return o1When.compareTo(o2When);
            }

            return o1When == null ? -1 : 1;
        }

    };

    public static final Comparator<EnrolmentEvaluation> SORT_BY_STUDENT_NUMBER = new Comparator<EnrolmentEvaluation>() {

        @Override
        public int compare(EnrolmentEvaluation o1, EnrolmentEvaluation o2) {
            final Student s1 = o1.getRegistration().getStudent();
            final Student s2 = o2.getRegistration().getStudent();
            return s1.getNumber().compareTo(s2.getNumber());
        }

    };

    static final public Comparator<EnrolmentEvaluation> COMPARATOR_BY_EXAM_DATE = new Comparator<EnrolmentEvaluation>() {
        @Override
        public int compare(EnrolmentEvaluation o1, EnrolmentEvaluation o2) {
            if (o1.getExamDateYearMonthDay() == null && o2.getExamDateYearMonthDay() == null) {
                return 0;
            }
            if (o1.getExamDateYearMonthDay() == null) {
                return -1;
            }
            if (o2.getExamDateYearMonthDay() == null) {
                return 1;
            }
            return o1.getExamDateYearMonthDay().compareTo(o2.getExamDateYearMonthDay());
        }
    };

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

    private EnrolmentEvaluation(Enrolment enrolment, EnrolmentEvaluationState enrolmentEvaluationState, EvaluationSeason season,
            Person responsibleFor, Grade grade, Date availableDate, Date examDate) {

        this(enrolment, season);

        if (enrolmentEvaluationState == null || responsibleFor == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }
        setEnrolmentEvaluationState(enrolmentEvaluationState);
        setPersonResponsibleForGrade(responsibleFor);
        setGrade(grade);
        setGradeAvailableDate(availableDate);
        setExamDate(examDate);

        generateCheckSum();
    }

    protected EnrolmentEvaluation(Enrolment enrolment, EnrolmentEvaluationState enrolmentEvaluationState, EvaluationSeason season,
            Person responsibleFor, Grade grade, Date availableDate, Date examDate, DateTime when) {
        this(enrolment, enrolmentEvaluationState, season, responsibleFor, grade, availableDate, examDate);
        setWhenDateTime(when);
    }

    protected EnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season, EnrolmentEvaluationState evaluationState) {
        this(enrolment, season);
        if (evaluationState == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }
        setEnrolmentEvaluationState(evaluationState);
        setWhenDateTime(new DateTime());
    }

    protected EnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season, EnrolmentEvaluationState evaluationState,
            Person person) {
        this(enrolment, season, evaluationState);
        if (person == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }
        setPerson(person);
    }

    protected EnrolmentEvaluation(Enrolment enrolment, EvaluationSeason season, EnrolmentEvaluationState evaluationState,
            Person person, ExecutionInterval executionInterval) {
        this(enrolment, season, evaluationState, person);
        if (executionInterval == null) {
            throw new DomainException("error.enrolmentEvaluation.invalid.parameters");
        }
        setExecutionPeriod(executionInterval);
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

    private ExecutionYear getExecutionYear() {
        return getExecutionInterval().getExecutionYear();
    }

    public void confirmSubmission(Person person, String observation) {
        if (!isTemporary()) {
            throw new DomainException("EnrolmentEvaluation.cannot.submit.not.temporary",
                    getEnrolment().getStudent().getPerson().getUsername());
        }

        if (!hasGrade()) {
            throw new DomainException("EnrolmentEvaluation.cannot.submit.with.empty.grade");
        }

        if (isPayable() && !isPayed()) {
            throw new EnrolmentNotPayedException("EnrolmentEvaluation.cannot.set.grade.on.not.payed.enrolment.evaluation",
                    getEnrolment());
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

    final public boolean hasExamDateYearMonthDay() {
        return getExamDateYearMonthDay() != null;
    }

    public boolean isPayable() {
        final IImprovementTreasuryEvent event =
                TreasuryBridgeAPIFactory.implementation().getImprovementTaxTreasuryEvent(getRegistration(), getExecutionYear());

        return event != null && event.isCharged(this);
    }

    public boolean isPayed() {
        final IImprovementTreasuryEvent event =
                TreasuryBridgeAPIFactory.implementation().getImprovementTaxTreasuryEvent(getRegistration(), getExecutionYear());

        return event != null && event.isPayed(this);
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
        if (getEvaluationSeason().isImprovement()) {
            return super.getExecutionPeriod();
        }

        return getEnrolment().getExecutionInterval();
    }

    @Deprecated
    public java.util.Date getExamDate() {
        org.joda.time.YearMonthDay ymd = getExamDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    @Deprecated
    public void setExamDate(java.util.Date date) {
        if (date == null) {
            setExamDateYearMonthDay(null);
        } else {
            setExamDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getGradeAvailableDate() {
        org.joda.time.YearMonthDay ymd = getGradeAvailableDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    @Deprecated
    public void setGradeAvailableDate(java.util.Date date) {
        if (date == null) {
            setGradeAvailableDateYearMonthDay(null);
        } else {
            setGradeAvailableDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getWhen() {
        org.joda.time.DateTime dt = getWhenDateTime();
        return (dt == null) ? null : new java.util.Date(dt.getMillis());
    }

    @Deprecated
    public void setWhen(java.util.Date date) {
        if (date == null) {
            setWhenDateTime(null);
        } else {
            setWhenDateTime(new org.joda.time.DateTime(date.getTime()));
        }
    }

}
