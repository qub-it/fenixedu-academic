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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.dml.DynamicField;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.commons.i18n.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tania Pousao Created on 30/Out/2003
 */
public class DegreeInfo extends DegreeInfo_Base {

    private static final String DESCRIPTION = "description";
    private static final String HISTORY = "history";
    private static final String OBJECTIVES = "objectives";
    private static final String DESIGNED_FOR = "designedFor";
    private static final String PROFESSIONAL_EXITS = "professionalExits";
    private static final String OPERATIONAL_REGIME = "operationalRegime";
    private static final String GRATUITY = "gratuity";
    private static final String ADDITIONAL_INFO = "additionalInfo";
    private static final String LEARNING_LANGUAGES = "learningLanguages";
    private static final String LINKS = "links";
    private static final String TEST_INGRESSION = "testIngression";
    private static final String CLASSIFICATIONS = "classifications";
    private static final String ACCESS_REQUISITES = "accessRequisites";
    private static final String CANDIDACY_DOCUMENTS = "candidacyDocuments";
    private static final String DRIFTS_INITIAL = "driftsInitial";
    private static final String DRIFTS_FIRST = "driftsFirst";
    private static final String DRIFTS_SECOND = "driftsSecond";
    private static final String MARK_MIN = "markMin";
    private static final String MARK_MAX = "markMax";
    private static final String MARK_AVERAGE = "markAverage";
    private static final String QUALIFICATION_LEVEL = "qualificationLevel";
    private static final String RECOGNITIONS = "recognitions";
    private static final String PREVAILING_SCIENTIFIC_AREA = "prevailingScientificArea";

    private static final Logger logger = LoggerFactory.getLogger(DegreeInfo.class);

    public static final String DEGREE_INFO_CREATION_EVENT = "DEGREE_INFO_CREATION_EVENT";

    public static Comparator<DegreeInfo> COMPARATOR_BY_EXECUTION_YEAR = new Comparator<DegreeInfo>() {
        @Override
        public int compare(final DegreeInfo info1, final DegreeInfo info2) {
            int result = ExecutionYear.COMPARATOR_BY_YEAR.compare(info1.getExecutionYear(), info2.getExecutionYear());
            if (result != 0) {
                return result;
            }
            return DomainObjectUtil.COMPARATOR_BY_ID.compare(info1, info2);
        }
    };

    public DegreeInfo(final Degree degree, final ExecutionYear executionYear) {
        super();
        setRootDomainObject(Bennu.getInstance());

        DegreeInfo degreeInfo = degree.getMostRecentDegreeInfo(executionYear);

        if (degreeInfo != null && degreeInfo.getExecutionYear() == executionYear) {
            throw new DomainException(
                    "error.net.sourceforge.fenixdu.domain.cannot.create.degreeInfo.already.exists.one.for.that.degree.and.executionYear");
        }

        super.setExecutionYear(executionYear);
        super.setName(degree.getNameFor(executionYear));
        super.setDegree(degree);

        new DegreeInfoCandidacy(this);
        new DegreeInfoFuture(this);
        Signal.emit(DEGREE_INFO_CREATION_EVENT, new DomainObjectEvent<>(this));

    }

    protected DegreeInfo() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public ExecutionInterval getExecutionInterval() {
        return getExecutionYear();
    }

    public void setExecutionInterval(final ExecutionInterval input) {
        if (input == null) {
            throw new DomainException("error.DegreeInfo.required.ExecutionInterval");
        }
        super.setExecutionYear(ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, input));
    }

    @Override
    public void setName(final LocalizedString name) {
        if (hasSameName(name)) {
            return;
        }

        if (hasName() && !isEditable(this)) {
            throw new DomainException(
                    "error.org.fenixedu.academic.domain.DegreeInfo.can.only.change.name.for.future.execution.years");
        }
        super.setName(name);
    }

    private boolean hasName() {
        return getName() != null && !getName().isEmpty();
    }

    private boolean hasSameName(final LocalizedString name) {
        return hasName() && getName().equals(name);
    }

    /**
     * @deprecated use ExtendedDegreeInfo.setHigherEducationAccess()
     */
    @Deprecated(forRemoval = true)
    public void setGraduationNextLevelAccess(final LocalizedString graduationNextLevelAccess) {
        getDegreeInfoFuture().setGraduationNextLevelAccess(graduationNextLevelAccess);
    }

    /**
     * @deprecated use ExtendedDegreeInfo.getHigherEducationAccess()
     */
    @Deprecated(forRemoval = true)
    public LocalizedString getGraduationNextLevelAccess() {
        return getDegreeInfoFuture().getGraduationNextLevelAccess();
    }

    /**
     * @deprecated use ExtendedDegreeInfo.setProfessionalStatus()
     */
    @Deprecated(forRemoval = true)
    public void setProfessionalStatute(final LocalizedString professionalStatute) {
        getDegreeInfoFuture().setProfessionalStatute(professionalStatute);
    }

    /**
     * @deprecated use ExtendedDegreeInfo.getProfessionalStatus()
     */
    @Deprecated(forRemoval = true)
    public LocalizedString getProfessionalStatute() {
        return getDegreeInfoFuture().getProfessionalStatute();
    }

    public DegreeInfo(final DegreeInfo degreeInfo, final ExecutionYear executionYear) {
        this(degreeInfo.getDegree(), executionYear);

        setName(degreeInfo.getName());
        setDescription(degreeInfo.getDescription());
        setHistory(degreeInfo.getHistory());
        setObjectives(degreeInfo.getObjectives());
        setDesignedFor(degreeInfo.getDesignedFor());
        setProfessionalExits(degreeInfo.getProfessionalExits());
        setOperationalRegime(degreeInfo.getOperationalRegime());
        setGratuity(degreeInfo.getGratuity());
        setAdditionalInfo(degreeInfo.getAdditionalInfo());
        setLinks(degreeInfo.getLinks());
        setLearningLanguages(degreeInfo.getLearningLanguages());
        setPrevailingScientificArea(degreeInfo.getPrevailingScientificArea());

        setTestIngression(degreeInfo.getTestIngression());
        setClassifications(degreeInfo.getClassifications());
        setAccessRequisites(degreeInfo.getAccessRequisites());
        setCandidacyDocuments(degreeInfo.getCandidacyDocuments());
        setDriftsInitial(degreeInfo.getDriftsInitial());
        setDriftsFirst(degreeInfo.getDriftsFirst());
        setDriftsSecond(degreeInfo.getDriftsSecond());
        setMarkMin(degreeInfo.getMarkMin());
        setMarkMax(degreeInfo.getMarkMax());
        setMarkAverage(degreeInfo.getMarkAverage());

        setQualificationLevel(degreeInfo.getQualificationLevel());
        setRecognitions(degreeInfo.getRecognitions());
    }

    public void delete() {
        getDegreeInfoCandidacy().delete();
        getDegreeInfoFuture().delete();

        setRootDomainObject(null);
        setDegree(null);
        setExecutionYear(null);

        deleteDomainObject();
    }

    public LocalizedString getAccessRequisites() {
        return findFieldByCode(ACCESS_REQUISITES).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoCandidacy().getAccessRequisites());
    }

    public LocalizedString getCandidacyDocuments() {
        return findFieldByCode(CANDIDACY_DOCUMENTS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoCandidacy().getCandidacyDocuments());
    }

    public LocalizedString getCandidacyPeriod() {
        return getDegreeInfoCandidacy().getCandidacyPeriod();
    }

    public LocalizedString getClassifications() {
        return findFieldByCode(CLASSIFICATIONS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoFuture().getClassifications());
    }

    public LocalizedString getDesignedFor() {
        return findFieldByCode(DESIGNED_FOR).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoFuture().getDesignedFor());
    }

    public String getDesignedFor(final Locale language) {
        return hasDesignedFor(language) ? getDesignedFor().getContent(language) : "";
    }

    public boolean hasDesignedFor(final Locale language) {
        return getDesignedFor() != null && getDesignedFor().getContent(language) != null;
    }

    public LocalizedString getEnrolmentPeriod() {
        return getDegreeInfoCandidacy().getEnrolmentPeriod();
    }

    public LocalizedString getObjectives() {
        return findFieldByCode(OBJECTIVES).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoFuture().getObjectives());
    }

    public boolean hasObjectives(final Locale language) {
        return getObjectives() != null && getObjectives().getContent(language) != null;
    }

    public String getObjectives(final Locale language) {
        return hasObjectives(language) ? getObjectives().getContent(language) : "";
    }

    public LocalizedString getProfessionalExits() {
        return findFieldByCode(PROFESSIONAL_EXITS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoFuture().getProfessionalExits());
    }

    public boolean hasProfessionalExits(final Locale language) {
        return getProfessionalExits() != null && getProfessionalExits().getContent(language) != null;
    }

    public String getProfessionalExits(final Locale language) {
        return hasProfessionalExits(language) ? getProfessionalExits().getContent(language) : "";
    }

    public LocalizedString getQualificationLevel() {
        return findFieldByCode(QUALIFICATION_LEVEL).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoFuture().getQualificationLevel());
    }

    public LocalizedString getRecognitions() {
        return findFieldByCode(RECOGNITIONS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoFuture().getRecognitions());
    }

    public LocalizedString getSelectionResultDeadline() {
        return getDegreeInfoCandidacy().getSelectionResultDeadline();
    }

    public LocalizedString getTestIngression() {
        return findFieldByCode(TEST_INGRESSION).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(() -> getDegreeInfoCandidacy().getTestIngression());
    }

    public void setAccessRequisites(final LocalizedString accessRequisites) {
        findFieldByCode(ACCESS_REQUISITES).ifPresent(dF -> dF.edit(accessRequisites));
        getDegreeInfoCandidacy().setAccessRequisites(accessRequisites);
    }

    public void setCandidacyDocuments(final LocalizedString candidacyDocuments) {
        findFieldByCode(CANDIDACY_DOCUMENTS).ifPresent(dF -> dF.edit(candidacyDocuments));
        getDegreeInfoCandidacy().setCandidacyDocuments(candidacyDocuments);
    }

    public void setCandidacyPeriod(final LocalizedString candidacyPeriod) {
        getDegreeInfoCandidacy().setCandidacyPeriod(candidacyPeriod);
    }

    public void setClassifications(final LocalizedString classifications) {
        findFieldByCode(CLASSIFICATIONS).ifPresent(dF -> dF.edit(classifications));
        getDegreeInfoFuture().setClassifications(classifications);
    }

    public void setDesignedFor(final LocalizedString designedFor) {
        findFieldByCode(DESIGNED_FOR).ifPresent(dF -> dF.edit(designedFor));
        getDegreeInfoFuture().setDesignedFor(designedFor);
    }

    public void setEnrolmentPeriod(final LocalizedString enrolmentPeriod) {
        getDegreeInfoCandidacy().setEnrolmentPeriod(enrolmentPeriod);
    }

    public void setObjectives(final LocalizedString objectives) {
        findFieldByCode(OBJECTIVES).ifPresent(dF -> dF.edit(objectives));
        getDegreeInfoFuture().setObjectives(objectives);
    }

    public void setProfessionalExits(final LocalizedString professionalExits) {
        findFieldByCode(PROFESSIONAL_EXITS).ifPresent(dF -> dF.edit(professionalExits));
        getDegreeInfoFuture().setProfessionalExits(professionalExits);
    }

    public void setQualificationLevel(final LocalizedString qualificationLevel) {
        findFieldByCode(QUALIFICATION_LEVEL).ifPresent(dF -> dF.edit(qualificationLevel));
        getDegreeInfoFuture().setQualificationLevel(qualificationLevel);
    }

    public void setRecognitions(final LocalizedString recognitions) {
        findFieldByCode(RECOGNITIONS).ifPresent(dF -> dF.edit(recognitions));
        getDegreeInfoFuture().setRecognitions(recognitions);
    }

    public void setSelectionResultDeadline(final LocalizedString selectionResultDeadline) {
        getDegreeInfoCandidacy().setSelectionResultDeadline(selectionResultDeadline);
    }

    public void setTestIngression(final LocalizedString testIngression) {
        findFieldByCode(TEST_INGRESSION).ifPresent(dF -> dF.edit(testIngression));
        getDegreeInfoCandidacy().setTestIngression(testIngression);
    }

    public boolean hasOperationalRegime(final Locale language) {
        return getOperationalRegime() != null && getOperationalRegime().getContent(language) != null;
    }

    @Override
    public void setOperationalRegime(final LocalizedString operationalRegime) {
        findFieldByCode(OPERATIONAL_REGIME).ifPresent(dF -> dF.edit(operationalRegime));
        super.setOperationalRegime(operationalRegime);
    }

    @Override
    public LocalizedString getOperationalRegime() {
        return findFieldByCode(OPERATIONAL_REGIME).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getOperationalRegime);
    }

    public String getOperationalRegime(final Locale language) {
        return hasOperationalRegime(language) ? getOperationalRegime().getContent(language) : "";
    }

    public boolean hasAdditionalInfo(final Locale language) {
        return getAdditionalInfo() != null && getAdditionalInfo().getContent(language) != null;
    }

    @Override
    public void setAdditionalInfo(final LocalizedString additionalInfo) {
        findFieldByCode(ADDITIONAL_INFO).ifPresent(dF -> dF.edit(additionalInfo));
        super.setAdditionalInfo(additionalInfo);
    }

    @Override
    public LocalizedString getAdditionalInfo() {
        return findFieldByCode(ADDITIONAL_INFO).map(dF -> dF.getValue(LocalizedString.class)).orElseGet(super::getAdditionalInfo);
    }

    public String getAdditionalInfo(final Locale language) {
        return hasAdditionalInfo(language) ? getAdditionalInfo().getContent(language) : "";
    }

    public AcademicInterval getAcademicInterval() {
        return getExecutionYear().getAcademicInterval();
    }

    /*
     * #dsimoes @13JAN2016
     * Any change to the name are now allowed.
     */
    public static boolean isEditable(final DegreeInfo dinfo) {
        return true;
        //        final DegreeCurricularPlan firstDegreeCurricularPlan = dinfo.getDegree().getFirstDegreeCurricularPlan();
        //        final DegreeCurricularPlan lastActiveDegreeCurricularPlan = dinfo.getDegree().getLastActiveDegreeCurricularPlan();
        //        if (firstDegreeCurricularPlan == null) {
        //            return true;
        //        }
        //        ExecutionYear firstExecutionYear =
        //                ExecutionYear.readByDateTime(firstDegreeCurricularPlan.getInitialDateYearMonthDay().toDateTimeAtMidnight());
        //        if (dinfo.getExecutionYear().isBefore(firstExecutionYear)) {
        //            return true;
        //        }
        //        if (lastActiveDegreeCurricularPlan == null) {
        //            return true;
        //        }
        //        if (lastActiveDegreeCurricularPlan.getExecutionDegreesSet().isEmpty()) {
        //            return true;
        //        }
        //        if (dinfo.getExecutionYear().isAfter(ExecutionYear.readCurrentExecutionYear())) {
        //            return true;
        //        }
        //        if (dinfo.getExecutionYear().isCurrent()) {
        //            return true;
        //        }
        //        return false;
    }

    /**
     * @deprecated use ExtendedDegreeInfo.getStudyProgrammeDuration()
     */
    @Deprecated(forRemoval = true)
    @Override
    public LocalizedString getOfficialProgramDuration() {
        return super.getOfficialProgramDuration();
    }

    /**
     * @deprecated use ExtendedDegreeInfo.setStudyProgrammeDuration()
     */
    @Deprecated(forRemoval = true)
    @Override
    public void setOfficialProgramDuration(LocalizedString officialProgramDuration) {
        super.setOfficialProgramDuration(officialProgramDuration);
    }

    /**
     * @deprecated use ExtendedDegreeInfo.getStudyRegime()
     */
    @Deprecated(forRemoval = true)
    @Override
    public LocalizedString getStudyRegime() {
        return super.getStudyRegime();
    }

    /**
     * @deprecated use ExtendedDegreeInfo.setStudyRegime()
     */
    @Deprecated(forRemoval = true)
    @Override
    public void setStudyRegime(LocalizedString studyRegime) {
        super.setStudyRegime(studyRegime);
    }

    @Override
    public void setDescription(final LocalizedString description) {
        findFieldByCode(DESCRIPTION).ifPresent(dF -> dF.edit(description));
        super.setDescription(description);
    }

    @Override
    public LocalizedString getDescription() {
        return findFieldByCode(DESCRIPTION).map(dF -> dF.getValue(LocalizedString.class)).orElseGet(super::getDescription);
    }

    @Override
    public void setHistory(final LocalizedString history) {
        findFieldByCode(HISTORY).ifPresent(dF -> dF.edit(history));
        super.setHistory(history);
    }

    @Override
    public LocalizedString getHistory() {
        return findFieldByCode(HISTORY).map(dF -> dF.getValue(LocalizedString.class)).orElseGet(super::getHistory);
    }

    @Override
    public LocalizedString getGratuity() {
        return findFieldByCode(GRATUITY).map(dF -> dF.getValue(LocalizedString.class)).orElseGet(super::getHistory);
    }

    @Override
    public void setGratuity(final LocalizedString gratuity) {
        findFieldByCode(GRATUITY).ifPresent(dF -> dF.edit(gratuity));
        super.setGratuity(gratuity);
    }

    @Override
    public LocalizedString getLearningLanguages() {
        return findFieldByCode(LEARNING_LANGUAGES).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getLearningLanguages);
    }

    @Override
    public void setLearningLanguages(final LocalizedString learningLanguages) {
        findFieldByCode(LEARNING_LANGUAGES).ifPresent(dF -> dF.edit(learningLanguages));
        super.setLearningLanguages(learningLanguages);
    }

    @Override
    public LocalizedString getLinks() {
        return findFieldByCode(LINKS).map(dF -> dF.getValue(LocalizedString.class)).orElseGet(super::getLinks);
    }

    @Override
    public void setLinks(final LocalizedString links) {
        findFieldByCode(LINKS).ifPresent(dF -> dF.edit(links));
        super.setLinks(links);
    }

    @Override
    public Integer getDriftsInitial() {
        return findFieldByCode(DRIFTS_INITIAL).map(dF -> dF.getValue(Integer.class)).orElseGet(super::getDriftsInitial);
    }

    @Override
    public void setDriftsInitial(final Integer driftsInitial) {
        findFieldByCode(DRIFTS_INITIAL).ifPresent(dF -> dF.edit(driftsInitial));
        super.setDriftsInitial(driftsInitial);
    }

    @Override
    public Integer getDriftsFirst() {
        return findFieldByCode(DRIFTS_FIRST).map(dF -> dF.getValue(Integer.class)).orElseGet(super::getDriftsFirst);
    }

    @Override
    public void setDriftsFirst(final Integer driftsFirst) {
        findFieldByCode(DRIFTS_FIRST).ifPresent(dF -> dF.edit(driftsFirst));
        super.setDriftsFirst(driftsFirst);
    }

    @Override
    public Integer getDriftsSecond() {
        return findFieldByCode(DRIFTS_SECOND).map(dF -> dF.getValue(Integer.class)).orElseGet(super::getDriftsSecond);
    }

    @Override
    public void setDriftsSecond(final Integer driftsSecond) {
        findFieldByCode(DRIFTS_SECOND).ifPresent(dF -> dF.edit(driftsSecond));
        super.setDriftsSecond(driftsSecond);
    }

    @Override
    public Double getMarkMin() {
        return findFieldByCode(MARK_MIN).map(dF -> {
            final BigDecimal value = dF.getValue(BigDecimal.class);
            return value != null ? value.doubleValue() : null;
        }).orElseGet(super::getMarkMin);
    }

    @Override
    public void setMarkMin(final Double markMin) {
        findFieldByCode(MARK_MIN).ifPresent(dF -> dF.edit(markMin == null ? null : BigDecimal.valueOf(markMin)));
        super.setMarkMin(markMin);
    }

    @Override
    public Double getMarkMax() {
        return findFieldByCode(MARK_MAX).map(dF -> {
            final BigDecimal value = dF.getValue(BigDecimal.class);
            return value != null ? value.doubleValue() : null;
        }).orElseGet(super::getMarkMax);
    }

    @Override
    public void setMarkMax(final Double markMax) {
        findFieldByCode(MARK_MAX).ifPresent(dF -> dF.edit(markMax == null ? null : BigDecimal.valueOf(markMax)));
        super.setMarkMax(markMax);
    }

    @Override
    public Double getMarkAverage() {
        return findFieldByCode(MARK_AVERAGE).map(dF -> {
            final BigDecimal value = dF.getValue(BigDecimal.class);
            return value != null ? value.doubleValue() : null;
        }).orElseGet(super::getMarkAverage);
    }

    @Override
    public void setMarkAverage(final Double markAverage) {
        findFieldByCode(MARK_AVERAGE).ifPresent(dF -> dF.edit(markAverage == null ? null : BigDecimal.valueOf(markAverage)));
        super.setMarkAverage(markAverage);
    }

    @Override
    public void setPrevailingScientificArea(final LocalizedString prevailingScientificArea) {
        findFieldByCode(PREVAILING_SCIENTIFIC_AREA).ifPresent(dF -> dF.edit(prevailingScientificArea));
        super.setPrevailingScientificArea(prevailingScientificArea);
    }

    @Override
    public LocalizedString getPrevailingScientificArea() {
        return findFieldByCode(PREVAILING_SCIENTIFIC_AREA).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getPrevailingScientificArea);
    }

    private Optional<DynamicField> findFieldByCode(final String code) {
        return Optional.ofNullable(DynamicField.findField(this, code));
    }
}
