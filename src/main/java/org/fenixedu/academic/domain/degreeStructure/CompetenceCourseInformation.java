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
package org.fenixedu.academic.domain.degreeStructure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.BibliographicReference;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.dml.DynamicField;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * Represents a set of attributes that defines a CompetenceCourse in a given
 * period of time.
 *
 * <pre>
 *
 * This attributes can be:
 * - Deparment which belongs;
 * - Name, descriptions, goals and bibliographic references;
 * - Study and work load (number hours of theoretical or pratical classes)
 *
 * </pre>
 *
 * In the perspective of a CompetenceCourse we can see this class as a version
 * of attributes that defines it. The start period of the version is done by an
 * association with ExecutionSemester.
 *
 * A CompetenceCourseInformation (the version of the CompetenceCourse) belongs
 * to a CompetenceCourseGroup Unit which may belong to a Department Unit.
 *
 * @see CompetenceCourse
 *
 */
public class CompetenceCourseInformation extends CompetenceCourseInformation_Base {

    public static final String OBJECTIVES = "objectives";
    public static final String PROGRAM = "program";
    public static final String EVALUATION_METHOD = "evaluationMethod";

    static public final Comparator<CompetenceCourseInformation> COMPARATORY_BY_EXECUTION_INTERVAL =
            Comparator.comparing(CompetenceCourseInformation::getExecutionInterval);

    @Deprecated
    static public final Comparator<CompetenceCourseInformation> COMPARATORY_BY_EXECUTION_PERIOD =
            new Comparator<CompetenceCourseInformation>() {
                @Override
                public int compare(final CompetenceCourseInformation o1, final CompetenceCourseInformation o2) {
                    return o1.getExecutionPeriod().compareTo(o2.getExecutionPeriod());
                }
            };

    public CompetenceCourseInformation() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public CompetenceCourseInformation(final CompetenceCourseInformation existingInformation,
            final ExecutionInterval executionInterval) {

        this(existingInformation.getName(), existingInformation.getNameEn(), existingInformation.getBasic(),
                existingInformation.getAcademicPeriod(), existingInformation.getLevelType(), executionInterval,
                existingInformation.getCompetenceCourseGroupUnit());

        setCompetenceCourse(existingInformation.getCompetenceCourse());

        existingInformation.getCourseLoadDurationsSet().forEach(
                existingDuration -> CourseLoadDuration.create(this, existingDuration.getCourseLoadType(),
                        existingDuration.getHours()));

        setCredits(existingInformation.getCredits());
        setAcronym(existingInformation.getAcronym());

        Set<BibliographicReference> copyBibliographies =
                existingInformation.getBibliographiesSet().stream().map(b -> b.copy()).collect(Collectors.toSet());

        getBibliographiesSet().addAll(copyBibliographies);

        existingInformation.getDynamicFieldSet().stream().filter(df -> StringUtils.isNotBlank(df.getValue())).forEach(
                df -> DynamicField.setFieldValue(this, df.getDescriptor().getCode(),
                        DynamicField.getFieldValue(existingInformation, df.getDescriptor().getCode())));

        setLanguages(existingInformation.getLanguages());

        existingInformation.getCompetenceCourseScientificAreasSet().stream()
                .filter(ccsa -> CompetenceCourseScientificArea.find(this, ccsa.getScientificAreaUnit()).isEmpty())
                .forEach(ccsa -> CompetenceCourseScientificArea.create(this, ccsa.getScientificAreaUnit()));
    }

    public CompetenceCourseInformation(final String name, final String nameEn, final Boolean basic,
            final AcademicPeriod academicPeriod, final CompetenceCourseLevelType competenceCourseLevel,
            final ExecutionInterval interval, final Unit unit) {

        this();
        checkParameters(name, nameEn, basic, academicPeriod, competenceCourseLevel, unit);
        setName(name);
        setNameEn(nameEn);
        setBasic(basic);
        setAcademicPeriod(academicPeriod);
        setLevelType(competenceCourseLevel);
        setExecutionInterval(interval);
        setCompetenceCourseGroupUnit(unit);
    }

    private void checkParameters(final String name, final String nameEn, final Boolean basic, final AcademicPeriod academicPeriod,
            CompetenceCourseLevelType competenceCourseLevel) {

        if (name == null || nameEn == null || basic == null || academicPeriod == null) {
            throw new DomainException("competence.course.information.invalid.parameters");
        }
    }

    private void checkParameters(final String name, final String nameEn, final Boolean basic, final AcademicPeriod academicPeriod,
            final CompetenceCourseLevelType competenceCourseLevel, final Unit unit) {

        checkParameters(name, nameEn, basic, academicPeriod, competenceCourseLevel);
        if (unit == null || !unit.isCompetenceCourseGroupUnit()) {
            throw new DomainException("competence.course.information.invalid.group.unit");
        }
    }

    public void edit(final String name, final String nameEn, final Boolean basic,
            final CompetenceCourseLevelType competenceCourseLevel, final Unit unit) {
        checkParameters(name, nameEn, basic, getAcademicPeriod(), competenceCourseLevel, unit);
        setName(name);
        setNameEn(nameEn);
        setBasic(basic);
        setLevelType(competenceCourseLevel);
        setCompetenceCourseGroupUnit(unit);
    }

    public ExecutionInterval getExecutionInterval() {
        return getExecutionPeriod();
    }

    public void setExecutionInterval(final ExecutionInterval input) {
        if (input == null) {
            throw new DomainException("error.CompetenceCourseInformation.required.ExecutionInterval");
        }

        super.setExecutionPeriod(input);
    }

    public void setAcademicPeriod(final AcademicPeriod input) {
        if (input == null) {
            throw new DomainException("error.CompetenceCourseInformation.required.AcademicPeriod");
        }

        if (input.getWeight() > 1f) {
            throw new DomainException("error.CompetenceCourseInformation.unsupported.AcademicPeriod");
        }

        super.setAcademicPeriod(input);

        // for backward compatibility
        setRegime(input.equals(AcademicPeriod.YEAR) ? RegimeType.ANUAL : RegimeType.SEMESTRIAL);
    }

    public LocalizedString getNameI18N() {
        LocalizedString result = new LocalizedString();

        if (!StringUtils.isEmpty(getName())) {
            result = result.with(org.fenixedu.academic.util.LocaleUtils.PT, getName());
        }
        if (!StringUtils.isEmpty(getNameEn())) {
            result = result.with(org.fenixedu.academic.util.LocaleUtils.EN, getNameEn());
        }

        return result;
    }

    public void setNameI18N(final LocalizedString input) {
        if (input != null) {
            setName(input.getContent(Locale.getDefault()));
            setNameEn(input.getContent(Locale.ENGLISH));
        } else {
            setName(null);
            setNameEn(null);
        }
    }

    public LocalizedString getObjectivesI18N() {
        return DynamicField.getFieldValue(this, OBJECTIVES);
    }

    public void setObjectivesI18N(final LocalizedString input) {
        DynamicField.setFieldValue(this, OBJECTIVES, input);
    }

    public LocalizedString getProgramI18N() {
        return DynamicField.getFieldValue(this, PROGRAM);
    }

    public void setProgramI18N(final LocalizedString input) {
        DynamicField.setFieldValue(this, PROGRAM, input);
    }

    public LocalizedString getEvaluationMethodI18N() {
        return DynamicField.getFieldValue(this, EVALUATION_METHOD);
    }

    public void setEvaluationMethodI18N(final LocalizedString input) {
        DynamicField.setFieldValue(this, EVALUATION_METHOD, input);
    }

    public void delete() {
        getBibliographiesSet().forEach(bb -> bb.delete());
        getCourseLoadDurationsSet().forEach(
                CourseLoadDuration::deleteTriggeredByCompetenceCourseInformation); // must be the initial instruction, in order to perform validations

        setExecutionPeriod(null);
        setCompetenceCourse(null);
        setCompetenceCourseGroupUnit(null);
        getCompetenceCourseScientificAreasSet().forEach(CompetenceCourseScientificArea::delete);

        setLevelType(null);

        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public Optional<BigDecimal> getLoadHours(final CourseLoadType courseLoadType) {
        return findLoadDurationByType(courseLoadType).map(CourseLoadDuration::getHours);
    }

    public Optional<BigDecimal> getLoadHours(final CourseLoadType courseLoadType, final TeachingMethodType teachingMethod) {
        return findLoadDurationByType(courseLoadType).flatMap(d -> d.findLoadDurationByTeachingMethod(teachingMethod))
                .map(CourseLoadDurationByTeachingMethod::getHours);
    }

    public void setLoadHours(final CourseLoadType courseLoadType, final BigDecimal hours) {
        final Optional<CourseLoadDuration> duration = findLoadDurationByType(courseLoadType);
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            duration.ifPresent(CourseLoadDuration::delete);
        } else {
            duration.orElseGet(() -> CourseLoadDuration.create(this, courseLoadType, null)).setHours(hours);
        }
    }

    public void setLoadHours(final CourseLoadType courseLoadType, final TeachingMethodType teachingMethod,
            final BigDecimal hours) {
        final CourseLoadDuration duration = findLoadDurationByType(courseLoadType).orElseThrow();
        final Optional<CourseLoadDurationByTeachingMethod> durationByTeachingMethod =
                duration.findLoadDurationByTeachingMethod(teachingMethod);

        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            durationByTeachingMethod.ifPresent(CourseLoadDurationByTeachingMethod::delete);
        } else {
            durationByTeachingMethod.orElseGet(() -> CourseLoadDurationByTeachingMethod.create(duration, teachingMethod, null))
                    .setHours(hours);
        }
    }

    public BigDecimal getContactLoad() {
        return getCourseLoadDurationsSet().stream().filter(d -> d.getCourseLoadType().getAllowShifts())
                .map(CourseLoadDuration::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalLoad() {
        return getCourseLoadDurationsSet().stream().map(CourseLoadDuration::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isAnual() {
        return AcademicPeriod.YEAR.equals(getAcademicPeriod());
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionPeriod().getExecutionYear();
    }

    public Optional<CourseLoadDuration> findLoadDurationByType(final CourseLoadType courseLoadType) {
        return getCourseLoadDurationsSet().stream().filter(duration -> duration.getCourseLoadType() == courseLoadType).findAny();
    }

    Stream<ExecutionInterval> getExecutionIntervalsRange() {
        final ExecutionInterval endExecutionInterval = findNext().map(cci -> cci.getExecutionInterval().getPrevious())
                .orElseGet(() -> ExecutionInterval.findLastChild(getExecutionInterval().getAcademicCalendar()).orElse(null));

        ExecutionInterval executionInterval = getExecutionInterval();

        final List<ExecutionInterval> result = new ArrayList<>();
        while (executionInterval != null && executionInterval.isBeforeOrEquals(endExecutionInterval)) {
            result.add(executionInterval);
            executionInterval = executionInterval.getNext();
        }

        return result.stream();
    }

    Optional<CompetenceCourseInformation> findNext() {
        return getCompetenceCourse().getCompetenceCourseInformationsSet().stream().sorted(COMPARATORY_BY_EXECUTION_INTERVAL)
                .dropWhile(cci -> cci != this).dropWhile(cci -> cci == this).findFirst();
    }

    Optional<CompetenceCourseInformation> findPrevious() {
        return getCompetenceCourse().getCompetenceCourseInformationsSet().stream()
                .sorted(COMPARATORY_BY_EXECUTION_INTERVAL.reversed()).dropWhile(cci -> cci != this).dropWhile(cci -> cci == this)
                .findFirst();
    }

    @Override
    public void setCredits(BigDecimal credits) {
        super.setCredits(credits);
        CompetenceCourseScientificArea.checkScientificAreasCreditsRules(this);
    }

    @Override
    public void setCompetenceCourseGroupUnit(Unit unit) {
        if (unit != null) {

            if (!unit.isCompetenceCourseGroupUnit()) {
                throw new DomainException("competence.course.information.invalid.group.unit");
            }

            final Collection<Unit> newScientificAreaUnits =
                    unit.getAllParentUnits().stream().filter(Party::isScientificAreaUnit).collect(Collectors.toSet());

            newScientificAreaUnits.forEach(newUnit -> CompetenceCourseScientificArea.find(this, newUnit)
                    .orElseGet(() -> CompetenceCourseScientificArea.create(this, newUnit)));

            if (getCompetenceCourseGroupUnit() != null) {
                final Collection<Unit> oldScientificAreaUnits =
                        getCompetenceCourseGroupUnit().getAllParentUnits().stream().filter(Party::isScientificAreaUnit)
                                .collect(Collectors.toSet());

                oldScientificAreaUnits.stream().filter(oldUnit -> !newScientificAreaUnits.contains(oldUnit))
                        .flatMap(oldUnit -> CompetenceCourseScientificArea.find(this, oldUnit).stream())
                        .forEach(CompetenceCourseScientificArea::delete);
            }
        }
        super.setCompetenceCourseGroupUnit(unit);
    }

}
