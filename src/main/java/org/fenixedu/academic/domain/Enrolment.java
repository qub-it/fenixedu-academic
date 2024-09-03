/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with FenixEdu Academic.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.enrolment.EnroledEnrolmentWrapper;
import org.fenixedu.academic.domain.enrolment.ExternalDegreeEnrolmentWrapper;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.log.EnrolmentLog;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.EctsAndWeightProviderRegistry;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.EnrolmentAction;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dcs-rjao
 *
 *         24/Mar/2003
 */

public class Enrolment extends Enrolment_Base implements IEnrolment {

    static final public Logger logger = LoggerFactory.getLogger(Enrolment.class);

    static final public Comparator<Enrolment> REVERSE_COMPARATOR_BY_EXECUTION_PERIOD_AND_ID = new Comparator<Enrolment>() {
        @Override
        public int compare(final Enrolment o1, final Enrolment o2) {
            return -COMPARATOR_BY_EXECUTION_PERIOD_AND_ID.compare(o1, o2);
        }
    };

    static final public Comparator<Enrolment> COMPARATOR_BY_STUDENT_NUMBER = new Comparator<Enrolment>() {
        @Override
        public int compare(final Enrolment e1, final Enrolment e2) {
            final int s1 = e1.getStudent().getNumber().intValue();
            final int s2 = e2.getStudent().getNumber().intValue();
            return s1 == s2 ? e1.getExternalId().compareTo(e2.getExternalId()) : s1 - s2;
        }
    };

    public static final String SIGNAL_CREATED = "fenixedu.academic.enrolment.created";

    protected Enrolment() {
        super();
        super.setIsExtraCurricular(Boolean.FALSE);
        Signal.emit(Enrolment.SIGNAL_CREATED, new DomainObjectEvent<>(this));
    }

    // TODO: DELETE
//    public Enrolment(final StudentCurricularPlan studentCurricularPlan, final CurricularCourse curricularCourse,
//            final ExecutionInterval executionInterval, final EnrollmentCondition enrolmentCondition, final String createdBy) {
//        this();
//        initializeAsNew(studentCurricularPlan, curricularCourse, executionInterval, enrolmentCondition, createdBy);
//        createCurriculumLineLog(EnrolmentAction.ENROL);
//    }

    @Override
    final public boolean isEnrolment() {
        return true;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    final public boolean isExternalEnrolment() {
        return false;
    }

    @Override
    final public boolean isPropaedeutic() {
        return super.isPropaedeutic();
    }

    @Override
    public boolean isExtraCurricular() {
        return super.isExtraCurricular();
    }

    @Override
    @Deprecated
    public Boolean getIsExtraCurricular() {
        return isExtraCurricular();
    }

    @Override
    @Deprecated
    public void setIsExtraCurricular(final Boolean isExtraCurricular) {
        throw new DomainException("error.org.fenixedu.academic.domain.Enrolment.use.markAsExtraCurricular.method.instead");
    }

    public void markAsExtraCurricular() {
        setCurriculumGroup(getStudentCurricularPlan().getExtraCurriculumGroup());
        super.setIsExtraCurricular(null);
    }

    final public boolean isFinal() {
        return getEnrolmentCondition() == EnrollmentCondition.FINAL;
    }

    @Deprecated
    final public boolean isInvisible() {
        return getEnrolmentCondition() == EnrollmentCondition.INVISIBLE;
    }

    final public boolean isTemporary() {
        return getEnrolmentCondition() == EnrollmentCondition.TEMPORARY;
    }

    @Deprecated
    final public boolean isImpossible() {
        return getEnrolmentCondition() == EnrollmentCondition.IMPOSSIBLE;
    }

    // new student structure methods
    public Enrolment(final StudentCurricularPlan studentCurricularPlan, final CurriculumGroup curriculumGroup,
            final CurricularCourse curricularCourse, final ExecutionInterval executionInterval,
            final EnrollmentCondition enrolmentCondition, final String createdBy) {
        this();
        if (studentCurricularPlan == null || curriculumGroup == null || curricularCourse == null || executionInterval == null || enrolmentCondition == null || createdBy == null) {
            throw new DomainException("invalid arguments");
        }
        checkInitConstraints(studentCurricularPlan, curricularCourse, executionInterval);
        // TODO: check this
        // validateDegreeModuleLink(curriculumGroup, curricularCourse);
        initializeAsNew(studentCurricularPlan, curriculumGroup, curricularCourse, executionInterval, enrolmentCondition,
                createdBy);
        createCurriculumLineLog(EnrolmentAction.ENROL);
    }

    protected void checkInitConstraints(final StudentCurricularPlan studentCurricularPlan,
            final CurricularCourse curricularCourse, final ExecutionInterval executionInterval) {
        if (studentCurricularPlan.isEnroledInExecutionPeriod(curricularCourse, executionInterval)) {
            throw new DomainException("error.Enrolment.duplicate.enrolment", curricularCourse.getName());
        }

        if (curricularCourse.getParentContexts(executionInterval).isEmpty()) {
            throw new DomainException("error.Enrolment.no.valid.context.found");
        }
    }

    protected void initializeAsNew(final StudentCurricularPlan studentCurricularPlan, final CurriculumGroup curriculumGroup,
            final CurricularCourse curricularCourse, final ExecutionInterval executionInterval,
            final EnrollmentCondition enrolmentCondition, final String createdBy) {
        initializeAsNewWithoutEnrolmentEvaluation(studentCurricularPlan, curriculumGroup, curricularCourse, executionInterval,
                enrolmentCondition, createdBy);
        createEnrolmentEvaluationWithoutGrade();
    }

    protected void initializeAsNewWithoutEnrolmentEvaluation(final StudentCurricularPlan studentCurricularPlan,
            final CurriculumGroup curriculumGroup, final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval, final EnrollmentCondition enrolmentCondition, final String createdBy) {
        setCurriculumGroup(curriculumGroup);
        initializeCommon(studentCurricularPlan, curricularCourse, executionInterval, enrolmentCondition, createdBy);
    }

    // end

//    protected void initializeAsNew(final StudentCurricularPlan studentCurricularPlan, final CurricularCourse curricularCourse,
//            final ExecutionInterval executionInterval, final EnrollmentCondition enrolmentCondition, final String createdBy) {
//        initializeAsNewWithoutEnrolmentEvaluation(studentCurricularPlan, curricularCourse, executionInterval, enrolmentCondition,
//                createdBy);
//        createEnrolmentEvaluationWithoutGrade();
//    }

    private void initializeCommon(final StudentCurricularPlan studentCurricularPlan, final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval, final EnrollmentCondition enrolmentCondition, final String createdBy) {
        setCurricularCourse(curricularCourse);
        setWeigth(curricularCourse.getEctsCredits(executionInterval));
        setEnrollmentState(EnrollmentState.ENROLLED);
        setExecutionPeriod(executionInterval);
        setEvaluationSeason(EvaluationConfiguration.getInstance().getDefaultEvaluationSeason());
        setCreatedBy(createdBy);
        setCreationDateDateTime(new DateTime());
        setEnrolmentCondition(enrolmentCondition);
        curricularCourse.findExecutionCourses(executionInterval).findAny().ifPresent(this::findOrCreateAttends);

        super.setIsExtraCurricular(Boolean.FALSE);
    }

//    protected void initializeAsNewWithoutEnrolmentEvaluation(final StudentCurricularPlan studentCurricularPlan,
//            final CurricularCourse curricularCourse, final ExecutionInterval executionInterval,
//            final EnrollmentCondition enrolmentCondition, final String createdBy) {
//        initializeCommon(studentCurricularPlan, curricularCourse, executionInterval, enrolmentCondition, createdBy);
//        setStudentCurricularPlan(studentCurricularPlan);
//    }

    @Override
    public void delete() {
        checkRulesToDelete();
        createCurriculumLineLog(EnrolmentAction.UNENROL);
        deleteInformation();
        setEvaluationSeason(null);

        super.delete();
    }

    protected void deleteInformation() {

        final Registration registration = getRegistration();

        setExecutionPeriod(null);
        setStudentCurricularPlan(null);
        setDegreeModule(null);
        setCurriculumGroup(null);

        Iterator<Attends> attendsIter = getAttendsSet().iterator();
        while (attendsIter.hasNext()) {
            Attends attends = attendsIter.next();

            attendsIter.remove();
            attends.setEnrolment(null);

            if (attends.getAssociatedMarksSet().isEmpty()) {
                boolean hasShiftEnrolment = false;
                for (Shift shift : attends.getExecutionCourse().getAssociatedShifts()) {
                    if (shift.getStudentsSet().contains(registration)) {
                        hasShiftEnrolment = true;
                        break;
                    }
                }

                if (!hasShiftEnrolment) {
                    attends.delete();
                }
            }
        }

        Iterator<EnrolmentEvaluation> evalsIter = getEvaluationsSet().iterator();
        while (evalsIter.hasNext()) {
            EnrolmentEvaluation eval = evalsIter.next();
            evalsIter.remove();
            eval.delete();
        }
    }

    protected void checkRulesToDelete() {
        if (!getEnrolmentWrappersSet().isEmpty()) {
            throw new DomainException("error.Enrolment.is.origin.in.some.Equivalence");
        }
    }

    final public Collection<Enrolment> getBrothers() {
        final Collection<Enrolment> result = new HashSet<>();

        result.addAll(getStudentCurricularPlan().getEnrolments(getCurricularCourse()));
        result.remove(this);

        return result;
    }

    final public Optional<EnrolmentEvaluation> getEnrolmentEvaluationBySeasonAndState(final EnrolmentEvaluationState state,
            final EvaluationSeason season) {

        final Supplier<Stream<EnrolmentEvaluation>> supplier =
                () -> getEnrolmentEvaluationBySeason(season).filter(e -> e.getEnrolmentEvaluationState().equals(state));

        // performance: avoid count
        if (logger.isDebugEnabled() && supplier.get().count() > 1) {

            // just to be precocious
            logger.debug("Multiple Evaluations for pair Season<->STATE! [REG {}] [SCP {}] [{}] [{}] [{}]",
                    getRegistration().getNumber(), getStudentCurricularPlan().getName(), print("").toString().replace("/n", ""),
                    season == null ? "" : season.getName().getContent(), state == null ? "" : state.toString());

        }

        return supplier.get().findAny();
    }

    final public Stream<EnrolmentEvaluation> getEnrolmentEvaluationBySeason(final EvaluationSeason season) {
        return getEvaluationsSet().stream().filter(e -> e.getEvaluationSeason().equals(season));
    }

    public boolean isEvaluatedInSeason(final EvaluationSeason season, final ExecutionInterval interval) {
        return getEnrolmentEvaluation(season, interval, Boolean.TRUE).isPresent();
    }

    public boolean isEnroledInSeason(final EvaluationSeason season, final ExecutionInterval interval) {
        return getTemporaryEvaluation(season, interval).isPresent();
    }

    public Optional<EnrolmentEvaluation> getEnrolmentEvaluation(final EvaluationSeason season, final ExecutionInterval interval,
            final Boolean assertFinal) {

        final Supplier<Stream<EnrolmentEvaluation>> supplier = () -> getEnrolmentEvaluationBySeason(season).filter(evaluation -> {

            if (evaluation.isAnnuled()) {
                return false;
            }

            if (evaluation.getExecutionInterval() != null && interval != null && evaluation.getExecutionInterval() != interval) {
                return false;
            }

            if (assertFinal != null) {

                if (assertFinal && !evaluation.isFinal()) {
                    return false;
                }

                // testing isFinal is insuficient, other states are final
                if (!assertFinal && !evaluation.isTemporary()) {
                    return false;
                }
            }

            return true;
        });

        // performance: avoid count
        if (logger.isDebugEnabled() && supplier.get().count() > 1) {

            // just to be precocious
            logger.debug("Multiple Evaluations for pair Season<->SEMESTER! [REG {}] [SCP {}] [{}] [{}] [{}]",
                    getRegistration().getNumber(), getStudentCurricularPlan().getName(), print("").toString().replace("/n", ""),
                    season == null ? "" : season.getName().getContent(), interval == null ? "" : interval.getQualifiedName());
        }

        return supplier.get().findAny();
    }

    protected void createEnrolmentEvaluationWithoutGrade() {
        boolean existing =
                getEnrolmentEvaluationBySeason(EvaluationConfiguration.getInstance().getDefaultEvaluationSeason()).filter(
                        e -> e.getGrade().equals(null)).findAny().isPresent();
        if (!existing) {
            EnrolmentEvaluation evaluation =
                    new EnrolmentEvaluation(this, EvaluationConfiguration.getInstance().getDefaultEvaluationSeason(),
                            EnrolmentEvaluationState.TEMPORARY_OBJ);
            evaluation.setWhenDateTime(new DateTime());
            addEvaluations(evaluation);
        }
    }

    public Attends findOrCreateAttends(final ExecutionCourse executionCourse) {
        final Registration registration = getRegistration();
        final Attends attends = executionCourse.getAttendsByStudent(registration.getStudent());

        if (attends != null) {
            if (attends.getEnrolment() == this) {
                return attends;
            }
            if (attends.getEnrolment() == null || attends.getEnrolment().isAnnulled()) {
                attends.setRegistration(registration);
                attends.setEnrolment(this);
                return attends;
            }
            throw new DomainException("error.cannot.create.multiple.enrolments.for.student.in.execution.course",
                    executionCourse.getName(), executionCourse.getExecutionInterval().getQualifiedName());
        }

        // create new attends
        if (getAttendsFor(executionCourse.getExecutionInterval()) != null) {
            throw new DomainException("error.Attends.enrolmentAlreadyHasAttendsForExecutionInterval",
                    executionCourse.getExecutionInterval().getQualifiedName());
        }
        final Attends newAttends = new Attends(registration, executionCourse);
        newAttends.setEnrolment(this);
        return newAttends;
    }

    final public List<EnrolmentEvaluation> getAllFinalEnrolmentEvaluations() {
        final List<EnrolmentEvaluation> result = new ArrayList<>();

        for (final EnrolmentEvaluation enrolmentEvaluation : getEvaluationsSet()) {
            if (enrolmentEvaluation.isFinal()) {
                result.add(enrolmentEvaluation);
            }
        }

        return result;
    }

    public boolean hasImprovementFor(final ExecutionInterval interval) {
        final Collection<ExecutionInterval> intervals =
                interval instanceof ExecutionYear ? ((ExecutionYear) interval).getChildIntervals() : Set.of(interval);

        for (EnrolmentEvaluation enrolmentEvaluation : this.getEvaluationsSet()) {
            if (enrolmentEvaluation.getEvaluationSeason().isImprovement()) {
                final ExecutionInterval evalPeriod = enrolmentEvaluation.getExecutionInterval();
                if (evalPeriod != null && intervals.contains(evalPeriod)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasSpecialSeasonFor(final ExecutionInterval interval) {
        return this.getEvaluationsSet().stream().filter(ee -> ee.getEvaluationSeason().isSpecial())
                .anyMatch(ee -> ee.getExecutionInterval() == interval);
    }

    @Deprecated
    final public boolean hasSpecialSeason() {
        for (final EnrolmentEvaluation evaluation : getEvaluationsSet()) {
            final EvaluationSeason season = evaluation.getEvaluationSeason();

            if (season.isSpecial() && isEnroledInSeason(season, getExecutionInterval())) {
                return true;
            }
        }

        return false;
    }

    final public boolean isFlunked() {
        if (isAnnulled()) {
            return true;
        }

        final EnrolmentEvaluation latestEnrolmentEvaluation = getFinalEnrolmentEvaluation();
        return latestEnrolmentEvaluation != null && latestEnrolmentEvaluation.isFlunked();
    }

    @Override
    final public boolean isApproved() {
        if (isAnnulled()) {
            return false;
        }

        final EnrolmentEvaluation latestEnrolmentEvaluation = getFinalEnrolmentEvaluation();
        return latestEnrolmentEvaluation != null && latestEnrolmentEvaluation.isApproved();
    }

    final public boolean isAproved(final ExecutionYear executionYear) {
        return (executionYear == null || getExecutionYear().isBeforeOrEquals(executionYear)) && isApproved();
    }

    @Override
    public boolean isApproved(final CurricularCourse curricularCourse, final ExecutionInterval executionInterval) {
        if (executionInterval == null || getExecutionInterval().isBeforeOrEquals(executionInterval)) {
            return isApproved() && hasCurricularCourse(getCurricularCourse(), curricularCourse, executionInterval);
        } else {
            return false;
        }
    }

    @Override
    public final ConclusionValue isConcluded(final ExecutionYear executionYear) {
        return ConclusionValue.create(isAproved(executionYear));
    }

    @Override
    public boolean canConclude(ExecutionYear executionYear) {
        return (isValid(executionYear) && isEnroled()) || isApproved();
    }

    @Override
    public YearMonthDay calculateConclusionDate() {
        if (!isApproved()) {
            throw new DomainException("error.Enrolment.not.approved");
        }
        return EvaluationConfiguration.getInstance().getEnrolmentEvaluationForConclusionDate(this)
                .map(EnrolmentEvaluation::getExamDateYearMonthDay).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    final public Curriculum getCurriculum(final DateTime when, final ExecutionYear year) {
        if (wasCreated(when) && (year == null || getExecutionYear().isBefore(
                year)) && isApproved() && !isPropaedeutic() && !isExtraCurricular()) {
            return new Curriculum(this, year, Collections.singleton((ICurriculumEntry) this), Collections.EMPTY_SET,
                    Collections.singleton((ICurriculumEntry) this));
        }

        return Curriculum.createEmpty(this, year);
    }

    @Override
    final public Grade getGrade() {
        final EnrolmentEvaluation enrolmentEvaluation = getFinalEnrolmentEvaluation();
        return enrolmentEvaluation == null ? Grade.createEmptyGrade() : enrolmentEvaluation.getGrade();
    }

    @Override
    final public String getGradeValue() {
        return getGrade().getValue();
    }

    @Override
    final public Integer getFinalGrade() {
        return getGrade().getIntegerValue();
    }

    @Override
    final public boolean isEnroled() {
        return this.getEnrollmentState() == EnrollmentState.ENROLLED;
    }

    final public boolean isAnnulled() {
        return this.getEnrollmentState() == EnrollmentState.ANNULED;
    }

    final public int getNumberOfTotalEnrolmentsInThisCourse(final ExecutionInterval untilExecutionInterval) {
        return this.getStudentCurricularPlan()
                .countEnrolmentsByCurricularCourse(this.getCurricularCourse(), untilExecutionInterval);
    }

    @Override
    protected void createCurriculumLineLog(final EnrolmentAction action) {
        new EnrolmentLog(action, getRegistration(), getCurricularCourse(), getExecutionInterval(), getCurrentUser());
    }

    @Override
    public StringBuilder print(String tabs) {
        final StringBuilder builder = new StringBuilder();
        builder.append(tabs);
        builder.append("[E ").append(getCode() + " - " + getDegreeModule().getName()).append(" ").append(isApproved())
                .append(" - ").append(getEctsCreditsForCurriculum()).append(" ects").append(" - ")
                .append(getExecutionInterval().getQualifiedName()).append(" ]\n");
        return builder;
    }

    final public Attends getAttendsByExecutionCourse(final ExecutionCourse executionCourse) {
        for (final Attends attends : this.getAttendsSet()) {
            if (attends.isFor(executionCourse)) {
                return attends;
            }
        }
        return null;
    }

    public Attends getAttendsFor(final ExecutionInterval interval) {
        Attends result = null;

        for (final Attends attends : getAttendsSet()) {
            if (attends.isFor(interval)) {
                if (result == null) {
                    result = attends;
                } else {
                    throw new DomainException("Enrolment.found.two.attends.for.same.execution.period");
                }
            }
        }

        return result;
    }

    final public ExecutionCourse getExecutionCourseFor(final ExecutionInterval executionInterval) {
        for (final Attends attend : getAttendsSet()) {
            if (attend.getExecutionCourse().getExecutionInterval() == executionInterval) {
                return attend.getExecutionCourse();
            }
        }

        return null;
    }

    public EnrolmentEvaluation getLatestEnrolmentEvaluationBySeason(final EvaluationSeason season) {
        return EvaluationConfiguration.getInstance().getCurrentEnrolmentEvaluation(this, season).orElse(null);
    }

    final public EnrolmentEvaluation getFinalEnrolmentEvaluation() {
        return EvaluationConfiguration.getInstance().getFinalEnrolmentEvaluation(this).orElse(null);
    }

    public Optional<EnrolmentEvaluation> getFinalEnrolmentEvaluationBySeason(final EvaluationSeason season) {
        return EvaluationConfiguration.getInstance().getFinalEnrolmentEvaluation(this, season);
    }

    private Optional<EnrolmentEvaluation> getTemporaryEvaluation(final EvaluationSeason season,
            final ExecutionInterval interval) {
        return getEnrolmentEvaluation(season, interval, Boolean.FALSE);
    }

    @Override
    final public List<Enrolment> getEnrolments() {
        return Collections.singletonList(this);
    }

    @Override
    final public boolean hasAnyEnrolments() {
        return true;
    }

    @Override
    final public StudentCurricularPlan getStudentCurricularPlan() {
        return getCurriculumGroup() != null ? getCurriculumGroup().getStudentCurricularPlan() : super.getStudentCurricularPlan();
    }

    @Override
    public boolean isEnroledInExecutionPeriod(final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval) {
        return isValid(executionInterval) && getCurricularCourse().isEquivalent(curricularCourse);
    }

    @Override
    public boolean isValid(final ExecutionInterval executionInterval) {
        return getExecutionInterval() == executionInterval || getCurricularCourse().isAnual() && getExecutionInterval().getExecutionYear() == executionInterval.getExecutionYear();
    }

    public boolean isValid(final ExecutionYear executionYear) {
        for (final ExecutionInterval executionInterval : executionYear.getChildIntervals()) {
            if (isValid(executionInterval)) {
                return true;
            }
        }
        return false;
    }

    @Override
    final public boolean hasEnrolmentWithEnroledState(final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval) {
        return isEnroled() && isEnroledInExecutionPeriod(curricularCourse, executionInterval);
    }

    final public Collection<ExecutionCourse> getExecutionCourses() {
        return this.getCurricularCourse().getAssociatedExecutionCoursesSet();
    }

    final public boolean isEnrolmentTypeNormal() {
        return !getCurricularCourse().isOptionalCurricularCourse() && !isExtraCurricular() && !isOptional();
    }

    @Override
    final public String getEnrolmentTypeName() {
        if (isExtraCurricular()) {
            return "EXTRA_CURRICULAR_ENROLMENT";
        } else if (isOptional()) {
            return "ENROLMENT_IN_OPTIONAL_DEGREE_MODULE";
        } else {
            return "COMPULSORY_ENROLMENT";
        }
    }

    @Override
    public Set<CurriculumLine> getCurriculumLinesForCurriculum(final StudentCurricularPlan studentCurricularPlan) {
        return studentCurricularPlan.getCreditsSet().stream()
                .filter(c -> c.getEnrolmentsSet().stream().anyMatch(ew -> ew.getIEnrolment() == this))
                .flatMap(c -> c.getDismissalsSet().stream()).collect(Collectors.toSet());
    }

    @Override
    final public Double getWeigth() {

        final Function<ICurriculumEntry, BigDecimal> provider = EctsAndWeightProviderRegistry.getWeightProvider(Enrolment.class);
        if (provider != null) {
            BigDecimal providedValue = provider.apply(this);
            return providedValue != null ? providedValue.doubleValue() : null;
        }

        return isExtraCurricular() || isPropaedeutic() ? Double.valueOf(0) : getWeigthForCurriculum().doubleValue();
    }

    @Override
    final public BigDecimal getWeigthForCurriculum() {

        final Function<ICurriculumEntry, BigDecimal> provider =
                EctsAndWeightProviderRegistry.getWeightForCurriculumProvider(Enrolment.class);
        if (provider != null) {
            return provider.apply(this);
        }

        final Double d;
        if (super.getWeigth() == null || super.getWeigth() == 0d) {
            final CurricularCourse curricularCourse = getCurricularCourse();
            d = curricularCourse == null ? null : curricularCourse.getWeigth();
        } else {
            d = super.getWeigth();
        }
        return d == null ? BigDecimal.ZERO : BigDecimal.valueOf(d);
    }

    /**
     * Just for Master Degrees legacy code
     *
     * @return
     */
    @Deprecated
    final public double getCredits() {
        return getEctsCredits();
    }

    @Override
    final public Double getEctsCredits() {

        final Function<ICurriculumEntry, BigDecimal> provider = EctsAndWeightProviderRegistry.getEctsProvider(Enrolment.class);
        if (provider != null) {
            final BigDecimal providedValue = provider.apply(this);
            return providedValue != null ? providedValue.doubleValue() : null;
        }

        return getEctsCreditsForCurriculum().doubleValue();
    }

    @Override
    final public BigDecimal getEctsCreditsForCurriculum() {

        final Function<ICurriculumEntry, BigDecimal> provider =
                EctsAndWeightProviderRegistry.getEctsForCurriculumProvider(Enrolment.class);
        if (provider != null) {
            return provider.apply(this);
        }

        return BigDecimal.valueOf(getCurricularCourse().getEctsCredits(getExecutionInterval()));
    }

    @Override
    final public Double getAprovedEctsCredits() {
        return isApproved() ? getEctsCredits() : Double.valueOf(0d);
    }

    @Override
    final public Double getCreditsConcluded(final ExecutionYear executionYear) {
        return executionYear == null || getExecutionYear().isBeforeOrEquals(executionYear) ? getAprovedEctsCredits() : 0d;
    }

    @Override
    final public Double getEnroledEctsCredits(final ExecutionInterval executionInterval) {
        return isValid(executionInterval) && isEnroled() ? getEctsCredits() : Double.valueOf(0d);
    }

    @Override
    final public Double getEnroledEctsCredits(final ExecutionYear executionYear) {
        return isValid(executionYear) && isEnroled() ? getEctsCredits() : Double.valueOf(0d);
    }

    @Override
    final public Enrolment findEnrolmentFor(final CurricularCourse curricularCourse, final ExecutionInterval executionInterval) {
        return isEnroledInExecutionPeriod(curricularCourse, executionInterval) ? this : null;
    }

    @Override
    final public Enrolment getApprovedEnrolment(final CurricularCourse curricularCourse) {
        return isApproved(curricularCourse) ? this : null;
    }

    @Override
    public Set<IDegreeModuleToEvaluate> getDegreeModulesToEvaluate(final ExecutionInterval executionInterval) {
        if (isValid(executionInterval) && isEnroled()) {
            if (isFromExternalDegree()) {
                return Collections.<IDegreeModuleToEvaluate> singleton(
                        new ExternalDegreeEnrolmentWrapper(this, executionInterval));
            } else {
                return Collections.<IDegreeModuleToEvaluate> singleton(new EnroledEnrolmentWrapper(this, executionInterval));
            }
        }
        return Collections.emptySet();
    }

    private boolean isFromExternalDegree() {
        return getDegreeModule().getParentDegreeCurricularPlan() != getDegreeCurricularPlanOfDegreeModule();
    }

    @Override
    final public String getDescription() {
        return getStudentCurricularPlan().getDegree().getPresentationName(getExecutionYear()) + " > " + getName().getContent();
    }

    final public boolean isBefore(final Enrolment enrolment) {
        return getExecutionInterval().isBefore(enrolment.getExecutionInterval());
    }

    @Override
    final public Unit getAcademicUnit() {
        return Bennu.getInstance().getInstitutionUnit();
    }

    @Override
    final public String getCode() {
        if (getDegreeModule() != null) {
            return getDegreeModule().getCode();
        }
        return null;
    }

    @Override
    public boolean hasEnrolment(final ExecutionInterval executionInterval) {
        return isValid(executionInterval);
    }

    @Override
    public boolean hasEnrolment(final ExecutionYear executionYear) {
        return isValid(executionYear);
    }

    @Override
    public boolean isEnroledInSpecialSeason(final ExecutionInterval executionInterval) {
        return isValid(executionInterval) && hasSpecialSeason();
    }

    @Override
    public boolean isEnroledInSpecialSeason(final ExecutionYear executionYear) {
        return isValid(executionYear) && hasSpecialSeason();
    }

    @Override
    public int getNumberOfAllApprovedEnrolments(final ExecutionInterval executionInterval) {
        return isValid(executionInterval) && isApproved() ? 1 : 0;
    }

    /**
     * After create new Enrolment, must delete OptionalEnrolment (to delete OptionalEnrolment disconnect at least:
     * ProgramCertificateRequests, CourseLoadRequests, ExamDateCertificateRequests)
     *
     * @param optionalEnrolment
     * @param curriculumGroup   : new CurriculumGroup for Enrolment
     * @return Enrolment
     */
    static Enrolment createBasedOn(final OptionalEnrolment optionalEnrolment, final CurriculumGroup curriculumGroup) {
        checkParameters(optionalEnrolment, curriculumGroup);

        final Enrolment enrolment = new Enrolment();
        enrolment.setCurricularCourse(optionalEnrolment.getCurricularCourse());
        enrolment.setWeigth(optionalEnrolment.getWeigth());
        enrolment.setEnrollmentState(optionalEnrolment.getEnrollmentState());
        enrolment.setExecutionPeriod(optionalEnrolment.getExecutionPeriod());
        enrolment.setEvaluationSeason(optionalEnrolment.getEvaluationSeason());
        enrolment.setCreatedBy(Authenticate.getUser().getUsername());
        enrolment.setCreationDateDateTime(optionalEnrolment.getCreationDateDateTime());
        enrolment.setEnrolmentCondition(optionalEnrolment.getEnrolmentCondition());
        enrolment.setCurriculumGroup(curriculumGroup);

        enrolment.getEvaluationsSet().addAll(optionalEnrolment.getEvaluationsSet());
        enrolment.getEnrolmentWrappersSet().addAll(optionalEnrolment.getEnrolmentWrappersSet());
        changeAttends(optionalEnrolment, enrolment);
        enrolment.createCurriculumLineLog(EnrolmentAction.ENROL);

        return enrolment;
    }

    static protected void changeAttends(final Enrolment from, final Enrolment to) {
        final Registration oldRegistration = from.getRegistration();
        final Registration newRegistration = to.getRegistration();

        if (oldRegistration != newRegistration) {
            for (final Attends attend : from.getAttendsSet()) {
                oldRegistration.changeShifts(attend, newRegistration);
                attend.setRegistration(newRegistration);
            }
        }
        to.getAttendsSet().addAll(from.getAttendsSet());
    }

    static private void checkParameters(final OptionalEnrolment optionalEnrolment, final CurriculumGroup curriculumGroup) {
        if (optionalEnrolment == null) {
            throw new DomainException("error.Enrolment.invalid.optionalEnrolment");
        }
        if (curriculumGroup == null) {
            throw new DomainException("error.Enrolment.invalid.curriculumGroup");
        }
    }

    @Override
    public boolean isAnual() {
        final CurricularCourse curricularCourse = getCurricularCourse();
        return curricularCourse != null && curricularCourse.isAnual();
    }

    public boolean hasAnyNonTemporaryEvaluations() {
        for (EnrolmentEvaluation evaluation : this.getEvaluationsSet()) {
            if (!EnrolmentEvaluationState.TEMPORARY_OBJ.equals(evaluation.getEnrolmentEvaluationState())) {
                return true;
            }
        }

        return false;
    }

    public boolean canBeUsedAsCreditsSource() {
        return !isInvisible() && isApproved();
    }

    /**
     * @use {@link #isFinalWork()}
     * @deprecated
     */
    @Deprecated
    public boolean isDissertation() {
        return isFinalWork();
    }

    public boolean isFinalWork() {
        return getCurricularCourse().getCompetenceCourse().isFinalWork();
    }

    @Override
    public String getModuleTypeName() {
        return BundleUtil.getString(Bundle.ENUMERATION, this.getClass().getName());
    }

    public void annul() {
        setEnrollmentState(EnrollmentState.ANNULED);
        setAnnulmentDate(new DateTime());
    }

    public void activate() {
        if (isAnnulled()) {
            final Grade finalGrade = getGrade();
            setEnrollmentState(finalGrade.isEmpty() ? EnrollmentState.ENROLLED : finalGrade.getEnrolmentState());
            setAnnulmentDate(null);
        }
    }

    /**
     * @deprecated use {@link #getExecutionInterval()} instead.
     */
    @Deprecated
    @Override
    public ExecutionInterval getExecutionPeriod() {
        return getExecutionInterval();
    }

    @Override
    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionPeriod();
    }

    @Override
    public void setDegreeModule(DegreeModule degreeModule) {
        if (degreeModule instanceof OptionalCurricularCourse) {
            throw new DomainException("error.Enrolment.optional.curricular.course.cannot.be.set.as.degree.module");
        }

        super.setDegreeModule(degreeModule);
    }
}
