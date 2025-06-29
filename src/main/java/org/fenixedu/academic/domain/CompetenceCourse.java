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

import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.AUTONOMOUS_WORK;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.FIELD_WORK;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.INTERNSHIP;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.OTHER;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.PRACTICAL_LABORATORY;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.SEMINAR;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.THEORETICAL;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.THEORETICAL_PRACTICAL;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.TUTORIAL_ORIENTATION;

import java.math.BigDecimal;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.degreeStructure.RegimeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class CompetenceCourse extends CompetenceCourse_Base {

    public static final Comparator<CompetenceCourse> COMPETENCE_COURSE_COMPARATOR_BY_NAME = new Comparator<CompetenceCourse>() {

        @Override
        public int compare(CompetenceCourse o1, CompetenceCourse o2) {
            final int result = Collator.getInstance().compare(o1.getName(), o2.getName());
            return result != 0 ? result : DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2);
        }

    };

    protected CompetenceCourse() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public CompetenceCourse(String name, String nameEn, Boolean basic, AcademicPeriod academicPeriod,
            CompetenceCourseLevelType competenceCourseLevel, CompetenceCourseType type, CurricularStage curricularStage,
            Unit unit, ExecutionInterval startInterval, final GradeScale gradeScale) {

        this();
        super.setCurricularStage(curricularStage);
        setType(type);

        super.setGradeScale(Optional.ofNullable(gradeScale).or(() -> GradeScale.findUniqueDefault())
                .orElseThrow(() -> new DomainException("error.CompetenceCourse.gradeScale.required")));

        CompetenceCourseInformation competenceCourseInformation = new CompetenceCourseInformation(name.trim(), nameEn.trim(),
                basic, academicPeriod, competenceCourseLevel, startInterval, unit);
        super.addCompetenceCourseInformations(competenceCourseInformation);

        // acronym creation
        final String strip = name.strip();
        final String normalize = Normalizer.normalize(strip, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        final String capitalize = StringUtils.capitalize(normalize);
        final String initials = WordUtils.initials(capitalize);
        competenceCourseInformation.setAcronym(initials);

    }

    public Stream<BibliographicReference> findBibliographies() {
        return Optional.ofNullable(findInformationMostRecentUntil(null)).stream()
                .flatMap(info -> info.getBibliographiesSet().stream());
    }

    public void delete() {
        setGradeScale(null);
        if (!getAssociatedCurricularCoursesSet().isEmpty()) {
            throw new DomainException("mustDeleteCurricularCoursesFirst");
        }
        for (; !getCompetenceCourseInformationsSet().isEmpty(); getCompetenceCourseInformationsSet().iterator().next().delete()) {
            ;
        }
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    private CompetenceCourseInformation getOldestCompetenceCourseInformation() {
        return getCompetenceCourseInformationsSet().stream().min(CompetenceCourseInformation.COMPARATORY_BY_EXECUTION_INTERVAL)
                .orElse(null);
    }

    /**
     * Find for <b>exactly</b> the given {@link ExecutionInterval}
     */
    public CompetenceCourseInformation findInformation(final ExecutionInterval input) {
        return getCompetenceCourseInformationsSet().stream().filter(cci -> cci.getExecutionInterval() == input).findAny()
                .orElse(null);
    }

    public CompetenceCourseInformation findInformationMostRecentUntil(final ExecutionInterval interval) {

        if (interval == null) {
            return getCompetenceCourseInformationsSet().stream()
                    .max(CompetenceCourseInformation.COMPARATORY_BY_EXECUTION_INTERVAL).orElse(null);
        }

        // special case: if interval is year, normally is intended to check the info of its last period 
        final ExecutionInterval childInterval =
                interval instanceof ExecutionYear ? ((ExecutionYear) interval).getLastExecutionPeriod() : interval;

        CompetenceCourseInformation result = null;

        final List<CompetenceCourseInformation> orderedInformations = getCompetenceCourseInformationsSet().stream()
                .sorted(CompetenceCourseInformation.COMPARATORY_BY_EXECUTION_INTERVAL).collect(Collectors.toList());

        for (CompetenceCourseInformation information : orderedInformations) {
            if (information.getExecutionInterval().isAfter(childInterval)) {
                if (result != null) { // only return if there is an previous information already found
                    return result;
                }
            } else {
                result = information;
            }
        }

        // if no result found and no explicit interval specified, return first information to attempt more null safety
        if (result == null && !orderedInformations.isEmpty()) {
            return orderedInformations.get(0);
        }

        return result;
    }

    /**
     * @deprecated use {@link #findInformationMostRecentUntil(ExecutionInterval)}
     */
    @Deprecated
    public CompetenceCourseInformation findCompetenceCourseInformationForExecutionPeriod(
            final ExecutionInterval executionInterval) {
        return findInformationMostRecentUntil(executionInterval);
    }

    public String getName(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getName() : null;
    }

    @Override
    public String getName() {
        return getName(null);
    }

    public String getNameEn(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getNameEn() : null;
    }

    public String getNameEn() {
        return getNameEn(null);
    }

    public String getAcronym(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getAcronym() : null;
    }

    public String getAcronym() {
        return getAcronym(null);
    }

    public void setAcronym(String acronym) {
        findInformationMostRecentUntil(null).setAcronym(acronym);
    }

    public boolean isBasic(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getBasic() : false;
    }

    public boolean isBasic() {
        return isBasic(null);
    }

    public RegimeType getRegime(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getRegime() : null;
    }

    public RegimeType getRegime() {
        return getRegime(null);
    }

    @Deprecated
    public void setRegime(RegimeType regimeType) {
        findInformationMostRecentUntil(null).setAcademicPeriod(regimeType.convertToAcademicPeriod());
    }

    public String getObjectives(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getObjectives() : null;
    }

    public String getObjectives() {
        return getObjectives(null);
    }

    public String getProgram(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getProgram() : null;
    }

    public String getProgram() {
        return getProgram(null);
    }

    public String getEvaluationMethod(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getEvaluationMethod() : null;
    }

    public String getEvaluationMethod() {
        return getEvaluationMethod(null);
    }

    public String getObjectivesEn(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getObjectivesEn() : null;
    }

    public String getObjectivesEn() {
        return getObjectivesEn(null);
    }

    public String getProgramEn(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getProgramEn() : null;
    }

    public String getProgramEn() {
        return getProgramEn(null);
    }

    public String getEvaluationMethodEn(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getEvaluationMethodEn() : null;
    }

    public String getEvaluationMethodEn() {
        return getEvaluationMethodEn(null);
    }

    public double getTheoreticalHours() {
        return getTheoreticalHours(null);
    }

    public double getTheoreticalHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(THEORETICAL), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getProblemsHours() {
        return getProblemsHours(null);
    }

    public double getProblemsHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(THEORETICAL_PRACTICAL), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getLaboratorialHours() {
        return getLaboratorialHours(null);
    }

    public double getLaboratorialHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(PRACTICAL_LABORATORY), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getSeminaryHours() {
        return getSeminaryHours(null);
    }

    public double getSeminaryHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(SEMINAR), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getFieldWorkHours() {
        return getFieldWorkHours(null);
    }

    public double getFieldWorkHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(FIELD_WORK), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getTrainingPeriodHours() {
        return getTrainingPeriodHours(null);
    }

    public double getTrainingPeriodHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(INTERNSHIP), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getTutorialOrientationHours() {
        return getTutorialOrientationHours(null);
    }

    public double getTutorialOrientationHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(TUTORIAL_ORIENTATION), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getOtherHours() {
        return getOtherHours(null);
    }

    public double getOtherHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(OTHER), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getAutonomousWorkHours() {
        return getAutonomousWorkHours(null);
    }

    public double getAutonomousWorkHours(final ExecutionInterval interval) {
        return getLoadHours(CourseLoadType.of(AUTONOMOUS_WORK), interval).map(BigDecimal::doubleValue).orElse(0.0);
    }

    public double getContactLoad() {
        return getContactLoad(null);
    }

    public Double getContactLoad(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getContactLoad().doubleValue() : 0.0;
    }

    public double getTotalLoad() {
        return getTotalLoad(null);
    }

    public double getTotalLoad(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getTotalLoad().doubleValue() : 0.0;
    }

    public Optional<BigDecimal> getLoadHours(final CourseLoadType courseLoadType) {
        return getLoadHours(courseLoadType, null);
    }

    public Optional<BigDecimal> getLoadHours(final CourseLoadType courseLoadType, final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return information != null ? information.getLoadHours(courseLoadType) : Optional.empty();
    }

    public double getEctsCredits() {
        return getEctsCredits(null);
    }

    public double getEctsCredits(final ExecutionInterval interval) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        return Optional.ofNullable(information).map(i -> i.getCredits()).map(c -> c.doubleValue()).orElse(0.0);
    }

    @SuppressWarnings("unchecked")
    public List<CurricularCourse> getCurricularCoursesWithActiveScopesInExecutionPeriod(final ExecutionInterval interval) {
        return getAssociatedCurricularCoursesSet().stream().filter(cc -> cc.isActive(interval)).collect(Collectors.toList());
    }

    public Collection<Context> getCurricularCourseContexts() {
        final Set<Context> result = new HashSet<Context>();
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            for (Context context : curricularCourse.getParentContextsSet()) {
                result.add(context);
            }
        }
        return result;
    }

    public CurricularCourse getCurricularCourse(final DegreeCurricularPlan degreeCurricularPlan) {
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.getDegreeCurricularPlan() == degreeCurricularPlan) {
                return curricularCourse;
            }
        }

        return null;
    }

    public List<Enrolment> getActiveEnrollments(ExecutionInterval interval) {
        final AcademicInterval academicInterval = interval.getAcademicInterval();
        return getAssociatedCurricularCoursesSet().stream()
                .flatMap(cc -> cc.getEnrolmentsByAcademicInterval(academicInterval).stream()).collect(Collectors.toList());
    }

    public ExecutionInterval getBeginExecutionInterval() {
        final CompetenceCourseInformation firstInformation = getOldestCompetenceCourseInformation();
        return firstInformation != null ? firstInformation.getExecutionInterval() : null;
    }

    public Boolean hasActiveScopesInExecutionYear(ExecutionYear executionYear) {
        Collection<ExecutionInterval> executionIntervals = executionYear.getChildIntervals();
        Collection<CurricularCourse> curricularCourses = this.getAssociatedCurricularCoursesSet();
        for (ExecutionInterval executionInterval : executionIntervals) {
            for (CurricularCourse curricularCourse : curricularCourses) {
                if (curricularCourse.hasAnyActiveContext(executionInterval)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     * @see #getDepartmentUnit(ExecutionYear)
     */
    public Unit getDepartmentUnit() {
        return getDepartmentUnit(null);
    }

    /**
     * In an ExecutionInterval the CompetenceCourse belongs to a Department.
     * This association is built by CompetenceCourseGroupUnit which aggregates
     * versions of CompetenceCourses (CompetenceCourseInformation). We can see
     * CompetenceCourseGroupUnit like a bag of CompetenceCourses beloging to a
     * Department.
     * 
     * The association between a CompetenceCourse and the ExecutionInterval is
     * represented by CompetenceCourseInformation. We can see
     * CompetenceCourseInformation as a version of CompetenceCourse's
     * attributes.
     * 
     * ExecutionInterval assumes the role of start period of this version
     * 
     * @see CompetenceCourseInformation
     * @see CompetenceCourseGroupUnit
     * @param semester semester of the competence course to be searched for
     * @return Department unit for the given semester
     */
    public Unit getDepartmentUnit(ExecutionInterval interval) {
        return getParentUnits(u -> u.isDepartmentUnit(), interval).findFirst().orElse(null);
    }

    /**
     * @see #getDepartmentUnit(ExecutionInterval)
     */
    public Unit getCompetenceCourseGroupUnit() {
        return getCompetenceCourseGroupUnit(null);
    }

    public Unit getCompetenceCourseGroupUnit(ExecutionInterval interval) {
        return findInformationMostRecentUntil(interval).getCompetenceCourseGroupUnit();
    }

    @Override
    public void setCode(String code) {
        final CompetenceCourse existing = CompetenceCourse.find(code);

        if (existing != null && existing != this) {
            throw new DomainException("error.CompetenceCourse.found.duplicate");
        }

        super.setCode(code);
    }

    @Override
    public void setCurricularStage(CurricularStage curricularStage) {
        if (!this.getAssociatedCurricularCoursesSet().isEmpty() && curricularStage.equals(CurricularStage.DRAFT)) {
            throw new DomainException("competenceCourse.has.already.associated.curricular.courses");
        }
        super.setCurricularStage(curricularStage);
    }

    public Stream<Unit> getParentUnits(final Predicate<Unit> predicate, ExecutionInterval interval) {
        final Predicate<Unit> nullSafePredicate = predicate != null ? predicate : u -> true;

        final CompetenceCourseInformation information = findInformationMostRecentUntil(interval);
        if (information == null) {
            return Stream.empty();
        }

        final Unit courseGroupUnit = information.getCompetenceCourseGroupUnit();
        return Stream.concat(Stream.of(courseGroupUnit), courseGroupUnit.getAllParentUnits().stream()).filter(nullSafePredicate);
    }

    public boolean isAnual() {
        return isAnual(null);
    }

    public boolean isApproved() {
        return getCurricularStage() == CurricularStage.APPROVED;
    }

    public LocalizedString getNameI18N() {
        return getNameI18N(null);
    }

    public LocalizedString getNameI18N(ExecutionInterval interval) {
        LocalizedString LocalizedString = new LocalizedString();
        String name = getName(interval);
        if (name != null && name.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.PT, name);
        }
        String nameEn = getNameEn(interval);
        if (nameEn != null && nameEn.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.EN, nameEn);
        }
        return LocalizedString;
    }

    public LocalizedString getObjectivesI18N() {
        return getObjectivesI18N(null);
    }

    public LocalizedString getObjectivesI18N(ExecutionInterval interval) {
        LocalizedString LocalizedString = new LocalizedString();
        String objectives = getObjectives(interval);
        if (objectives != null && objectives.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.PT, objectives);
        }
        String objectivesEn = getObjectivesEn(interval);
        if (objectivesEn != null && objectivesEn.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.EN, objectivesEn);
        }
        return LocalizedString;
    }

    public LocalizedString getProgramI18N() {
        return getProgramI18N(null);
    }

    public LocalizedString getProgramI18N(ExecutionInterval interval) {
        LocalizedString LocalizedString = new LocalizedString();
        String program = getProgram(interval);
        if (program != null && program.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.PT, program);
        }
        String programEn = getProgramEn(interval);
        if (programEn != null && programEn.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.EN, programEn);
        }
        return LocalizedString;
    }

    public LocalizedString getEvaluationMethodI18N() {
        return getEvaluationMethodI18N(null);
    }

    public LocalizedString getEvaluationMethodI18N(ExecutionInterval interval) {
        LocalizedString LocalizedString = new LocalizedString();
        String evaluationMethod = getEvaluationMethod(interval);
        if (evaluationMethod != null && evaluationMethod.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.PT, evaluationMethod);
        }
        String evaluationMethodEn = getEvaluationMethodEn(interval);
        if (evaluationMethodEn != null && evaluationMethodEn.length() > 0) {
            LocalizedString = LocalizedString.with(org.fenixedu.academic.util.LocaleUtils.EN, evaluationMethodEn);
        }
        return LocalizedString;
    }

    public List<ExecutionCourse> getExecutionCoursesByExecutionPeriod(final ExecutionInterval executionInterval) {
        return getCurricularCoursesWithActiveScopesInExecutionPeriod(executionInterval).stream()
                .flatMap(cc -> cc.getExecutionCoursesByExecutionPeriod(executionInterval).stream()).distinct()
                .collect(Collectors.toList());
    }

    /**
     * @deprecated
     * 
     * @use {@link #isFinalWork()}
     */
    @Deprecated
    public boolean isDissertation() {
        return isFinalWork();
    }

    public boolean isFinalWork() {
        return getType().isFinalWork();
    }

    public ExecutionInterval getStartExecutionInterval() {
        return getOldestCompetenceCourseInformation().getExecutionInterval();
    }

    @Deprecated
    static public Collection<CompetenceCourse> readBolonhaCompetenceCourses() {
        final Set<CompetenceCourse> result = new TreeSet<CompetenceCourse>(COMPETENCE_COURSE_COMPARATOR_BY_NAME);
        for (final CompetenceCourse competenceCourse : Bennu.getInstance().getCompetenceCoursesSet()) {
            result.add(competenceCourse);
        }
        return result;
    }

    static public Collection<CompetenceCourse> findAll() {
        return Bennu.getInstance().getCompetenceCoursesSet().stream().collect(Collectors.toSet());
    }

    @Deprecated
    public java.util.Date getCreationDate() {
        org.joda.time.YearMonthDay ymd = getCreationDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    @Deprecated
    public void setCreationDate(java.util.Date date) {
        if (date == null) {
            setCreationDateYearMonthDay(null);
        } else {
            setCreationDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    public static CompetenceCourse find(final String code) {
        if (StringUtils.isNotBlank(code)) {
            for (final CompetenceCourse iter : Bennu.getInstance().getCompetenceCoursesSet()) {
                if (StringUtils.equals(code, iter.getCode())) {
                    return iter;
                }
            }
        }
        return null;
    }

    public boolean isAnual(final ExecutionInterval input) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(input);
        return information != null ? information.isAnual() : null;
    }

    public AcademicPeriod getAcademicPeriod(final ExecutionInterval input) {
        final CompetenceCourseInformation information = findInformationMostRecentUntil(input);
        return information != null ? information.getAcademicPeriod() : null;
    }

    public AcademicPeriod getAcademicPeriod() {
        return getAcademicPeriod(null);
    }

    public static Stream<CompetenceCourse> findByUnit(final Unit unit, final boolean includeSubUnits) {
        final Collection<Unit> units = includeSubUnits ? unit.getAllSubUnits() : new HashSet<>();
        units.add(unit);
        return units.stream().filter(u -> u.isCompetenceCourseGroupUnit())
                .flatMap(u -> u.getCompetenceCourseInformationsSet().stream()
                        .filter(cci -> cci.getCompetenceCourse().getCompetenceCourseGroupUnit() == u)) // ensure that active information is from unit
                .map(cci -> cci.getCompetenceCourse()).distinct();
    }

}
