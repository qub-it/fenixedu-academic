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
package org.fenixedu.academic.dto.student;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusionConfig;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import com.google.common.base.Strings;

public class RegistrationConclusionBean implements Serializable, IRegistrationBean {

    private static final long serialVersionUID = 5825221957160251388L;

    private Registration registration;

    private Boolean hasAccessToRegistrationConclusionProcess = Boolean.TRUE;

    private LocalDate enteredConclusionDate;

    private String enteredFinalAverageGrade;

    private String enteredAverageGrade;

    private String enteredDescriptiveGrade;

    private String observations;

    private StudentCurricularPlan studentCurricularPlan;

    private ProgramConclusionConfig programConclusionConfig;

    private ConclusionProcess conclusionProcess;

    private Curriculum curriculum;

    protected RegistrationConclusionBean() {
        super();
    }

    public RegistrationConclusionBean(final StudentCurricularPlan studentCurricularPlan,
            final ProgramConclusion programConclusion) {
        setStudentCurricularPlan(studentCurricularPlan);
        setRegistration(studentCurricularPlan.getRegistration());
        programConclusionConfig = ProgramConclusionConfig.findBy(
                getStudentCurricularPlan().getDegreeCurricularPlan(), programConclusion).orElseThrow(() -> new RuntimeException(
                "No ProgramConclusionConfig found for the given DegreeCurricularPlan and ProgramConclusion"));
    }

    public CurriculumGroup getCurriculumGroup() {
        if (getProgramConclusion() == null) {
            return null;
        }

        if (getStudentCurricularPlan() != null && getProgramConclusion().groupFor(studentCurricularPlan).isPresent()) {
            return getProgramConclusion().groupFor(studentCurricularPlan).get();
        }

        return getProgramConclusion() == null ? null : getProgramConclusion().groupFor(getRegistration()).orElse(null);
    }

    public ProgramConclusion getProgramConclusion() {
        return getProgramConclusionConfig().getProgramConclusion();
    }

    public ProgramConclusionConfig getProgramConclusionConfig() {
        return programConclusionConfig;
    }

    @Override
    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public ExecutionYear getStartExecutionYear() {
        return getRegistration().getStartExecutionYear();
    }

    public Grade getFinalGrade() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getFinalGrade())
                .orElseGet(() -> getCalculatedFinalGrade());
    }

    public Grade getCalculatedFinalGrade() {
        return getCurriculumForConclusion().getFinalGrade();
    }

    public Grade getRawGrade() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getRawGrade()).orElseGet(() -> getCalculatedRawGrade());
    }

    public Grade getCalculatedRawGrade() {
        return getCurriculumForConclusion().getRawGrade();
    }

    public YearMonthDay getConclusionDate() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getConclusionYearMonthDay())
                .orElseGet(() -> calculateConclusionDate());
    }

    public YearMonthDay calculateConclusionDate() {
        return getCurriculumForConclusion().getLastApprovementDate();
    }

    public YearMonthDay getCalculatedConclusionDate() {
        return calculateConclusionDate();
    }

    public Grade getDescriptiveGrade() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getDescriptiveGrade()).orElse(null);
    }

    public String getDescriptiveGradeExtendedValue() {
        return getDescriptiveGrade() == null ? null : getDescriptiveGrade().getExtendedValue().getContent();
    }

    public ExecutionYear getIngressionYear() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getIngressionYear())
                .orElseGet(() -> calculateIngressionYear());
    }

    public ExecutionYear calculateIngressionYear() {
        return getRegistration().calculateIngressionYear();
    }

    public ExecutionYear getConclusionYear() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getConclusionYear()).orElseGet(() -> getCalculatedConclusionYear());
    }

    public ExecutionYear calculateConclusionYear() {
        return getCurriculumForConclusion().getLastExecutionYear();
    }

    public ExecutionYear getCalculatedConclusionYear() {
        return calculateConclusionYear();
    }

    public ExecutionInterval getConclusionExecutionInterval() {
        return getCurriculumForConclusion().getLastExecutionInterval();
    }

    public double getEctsCredits() {
        if (isConclusionProcessed()) {
            return getCurriculumGroup().getCreditsConcluded();
        }

        return calculateCredits();
    }

    public double calculateCredits() {
        return getCurriculumForConclusion().getSumEctsCredits().doubleValue();
    }

    public double getCalculatedEctsCredits() {
        return calculateCredits();
    }

    public Curriculum getCurriculumForConclusion() {
        if (curriculum == null) {
            curriculum = getCurriculumGroup().getCurriculum();
        }

        return curriculum;
    }

    public int getCurriculumEntriesSize() {
        return getCurriculumForConclusion().getCurriculumEntries().size();
    }

    public String getConclusionDegreeDescription() {
        return getRegistration().getDegreeDescription(getConclusionYear(), getProgramConclusion(), I18N.getLocale());
    }

    public boolean isConcluded() {
        return getCurriculumGroup().isConcluded();
    }

    public Collection<CurriculumGroup> getCurriculumGroupsNotVerifyingStructure() {
        final Collection<CurriculumGroup> result = new HashSet<CurriculumGroup>();
        if (!getCurriculumGroup().isSkipConcluded()) {
            getCurriculumGroup().assertCorrectStructure(result, getConclusionYear());
        }
        return result;
    }

    public boolean isConclusionProcessed() {
        return getConclusionProcess() != null;
    }

    public ConclusionProcess getConclusionProcess() {
        if (conclusionProcess == null) {
            conclusionProcess = ConclusionProcess.findBy(getStudentCurricularPlan(), getProgramConclusion()).orElse(null);
        }

        return conclusionProcess;
    }

    public String getConclusionProcessNotes() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getNotes()).orElse(null);
    }

    public Person getConclusionProcessResponsible() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getResponsible()).orElse(null);
    }

    public Person getConclusionProcessLastResponsible() {
        return Optional.ofNullable(getConclusionProcess()).map(cp -> cp.getLastResponsible()).orElse(null);
    }

    public DateTime getConclusionProcessCreationDateTime() {
        return Optional.ofNullable(getConclusionProcess()).map(cp-> cp.getCreationDateTime()).orElse(null);
    }

    public DateTime getConclusionProcessLastModificationDateTime() {
        return Optional.ofNullable(getConclusionProcess()).map(cp-> cp.getLastModificationDateTime()).orElse(null);
    }

    public boolean isSkipValidation() {
        return getProgramConclusion() != null && getProgramConclusion().isSkipValidation();
    }

    public Boolean getHasAccessToRegistrationConclusionProcess() {
        return hasAccessToRegistrationConclusionProcess;
    }

    public void setHasAccessToRegistrationConclusionProcess(Boolean hasAccessToRegistrationConclusionProcess) {
        this.hasAccessToRegistrationConclusionProcess = hasAccessToRegistrationConclusionProcess;
    }

    public LocalDate getEnteredConclusionDate() {
        return enteredConclusionDate;
    }

    public void setEnteredConclusionDate(LocalDate enteredConclusionDate) {
        this.enteredConclusionDate = enteredConclusionDate;
    }

    public boolean hasEnteredConclusionDate() {
        return getEnteredConclusionDate() != null;
    }

    public String getEnteredFinalAverageGrade() {
        return this.enteredFinalAverageGrade;
    }

    public void setEnteredFinalAverageGrade(final String value) {
        this.enteredFinalAverageGrade = value;
    }

    public boolean hasEnteredFinalAverageGrade() {
        return !Strings.isNullOrEmpty(this.enteredFinalAverageGrade);
    }

    public String getEnteredAverageGrade() {
        return this.enteredAverageGrade;
    }

    public void setEnteredAverageGrade(final String averageGrade) {
        this.enteredAverageGrade = averageGrade;
    }

    public boolean hasEnteredAverageGrade() {
        return !Strings.isNullOrEmpty(this.enteredAverageGrade);
    }

    public String getEnteredDescriptiveGrade() {
        return enteredDescriptiveGrade;
    }

    public void setEnteredDescriptiveGrade(String enteredDescriptiveGrade) {
        this.enteredDescriptiveGrade = enteredDescriptiveGrade;
    }

    public boolean hasEnteredDescriptiveGrade() {
        return !Strings.isNullOrEmpty(this.enteredDescriptiveGrade);
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public StudentCurricularPlan getStudentCurricularPlan() {
        return studentCurricularPlan;
    }

    public void setStudentCurricularPlan(StudentCurricularPlan studentCurricularPlan) {
        this.studentCurricularPlan = studentCurricularPlan;
    }

    public String getConclusionNumber() {
        if (isConclusionProcessed()) {
            return this.getConclusionProcess().getNumber();
        }
        return null;
    }

    public String calculateDescription() {
        return getCurriculumForConclusion().getDescription();
    }

    public LocalizedString getConclusionTitle() {
        final LocalizedString conclusionTitle = getProgramConclusionConfig().getConclusionTitle();
        if (conclusionTitle != null && !conclusionTitle.isEmpty()) {
            return conclusionTitle;
        }

        final ExecutionYear conclusionYear = getConclusionYear();
        return conclusionYear == null ? registration.getDegree().getPresentationNameI18N() : registration.getDegree()
                .getPresentationNameI18N(conclusionYear);
    }

    /**
     *
     * This method is deprecated and will be REMOVED after service requests elimination.
     * Use {@link #getConclusionTitle()} instead.
     */
    @Deprecated
    public LocalizedString getRawConclusionTitle() {
        return getProgramConclusionConfig().getConclusionTitle();
    }

    public boolean isForCycle(CycleType cycleType) {
        final CycleCourseGroup cycleCourseGroup =
                getStudentCurricularPlan().getDegreeCurricularPlan().getCycleCourseGroup(cycleType);

        return cycleCourseGroup != null && getProgramConclusionConfig().getIncludedModulesSet().stream()
                .anyMatch(m -> m.hasDegreeModule(cycleCourseGroup));
    }

    public BigDecimal getMinCreditsToConclude() {
        final BigDecimal toAdd = new BigDecimal(getProgramConclusionConfig().getIncludedModulesSet().stream()
                .collect(Collectors.summingDouble(dm -> dm.getMinEctsCredits())));
        final BigDecimal toRemove = new BigDecimal(getProgramConclusionConfig().getExcludedModulesSet().stream()
                .collect(Collectors.summingDouble(dm -> dm.getMinEctsCredits())));

        return toAdd.subtract(toRemove);
    }

}
