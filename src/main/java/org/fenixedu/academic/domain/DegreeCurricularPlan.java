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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CurricularRule;
import org.fenixedu.academic.domain.curricularRules.EnrolmentModel;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.BranchType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusionConfig;
import org.fenixedu.academic.domain.degreeStructure.RootCourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYears;
import org.fenixedu.academic.dto.CurricularPeriodInfoDTO;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.YearMonthDay;

public class DegreeCurricularPlan extends DegreeCurricularPlan_Base {

    public static final Comparator<DegreeCurricularPlan> COMPARATOR_BY_NAME = Comparator.comparing(DegreeCurricularPlan::getName);

    public static final Comparator<DegreeCurricularPlan> COMPARATOR_BY_PRESENTATION_NAME =
            Comparator.comparing((DegreeCurricularPlan dcp) -> dcp.getPresentationName())
                    .thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

    /**
     * This might look a strange comparator, but the idea is to show a list of
     * degree curricular plans according to, in the following order: 1. It's
     * degree type 2. Reverse order of ExecutionDegrees 3. It's degree code (in
     * order to roughly order them by prebolonha/bolonha) OR reverse order of
     * their own name
     *
     * For an example, see the coordinator's portal.
     */
    public static final Comparator<DegreeCurricularPlan>
            DEGREE_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_EXECUTION_DEGREE_AND_DEGREE_CODE =
            Comparator.comparing((DegreeCurricularPlan dcp) -> dcp.getDegreeType().getName())
                    .thenComparing(dcp -> dcp.getDegree().getSigla())
                    .thenComparing(dcp -> dcp.getName(), Comparator.reverseOrder())
                    .thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

    protected DegreeCurricularPlan() {
        super();
        super.setRootDomainObject(Bennu.getInstance());
        super.setApplyPreviousYearsEnrolmentRule(Boolean.TRUE);
        super.setCurricularRuleValidationType(EnrolmentModel.YEAR);
        super.setActive(true);
    }

    @Deprecated(forRemoval = true)
    public DegreeCurricularPlan(final Degree degree, final String name, final AcademicPeriod duration) {
        this();

        if (degree == null) {
            throw new DomainException("error.degreeCurricularPlan.degree.not.null");
        }

        setDegree(degree);
        setName(name);
        createDefaultCourseGroups();
        editDuration(duration);
    }

    public DegreeCurricularPlan(final Degree degree, final String name, final AcademicPeriod duration,
            final ExecutionInterval begin) {
        this();

        if (degree == null) {
            throw new DomainException("error.degreeCurricularPlan.degree.not.null");
        }

        setDegree(degree);
        setName(name);
        createDefaultCourseGroups();
        editDuration(duration);
        initBeginExecutionPeriodForDegreeCurricularPlan(getRoot(), begin.getExecutionYear().getFirstExecutionPeriod());
    }

    private void createDefaultCourseGroups() {
        RootCourseGroup.createRoot(this, getName(), getName());
    }

    private void initBeginExecutionPeriodForDegreeCurricularPlan(final CourseGroup courseGroup,
            final ExecutionInterval beginExecutionPeriod) {

        if (beginExecutionPeriod == null) {
            throw new DomainException("");
        }

        for (final CurricularRule curricularRule : courseGroup.getCurricularRulesSet()) {
            curricularRule.setBegin(beginExecutionPeriod);
        }
        for (final Context context : courseGroup.getChildContextsSet()) {
            context.setBeginExecutionPeriod(beginExecutionPeriod);
            if (!context.getChildDegreeModule().isLeaf()) {
                initBeginExecutionPeriodForDegreeCurricularPlan((CourseGroup) context.getChildDegreeModule(),
                        beginExecutionPeriod);
            }
        }
    }

    @Override
    public void setName(final String name) {
        if (StringUtils.isBlank(name)) {
            throw new DomainException("error.degreeCurricularPlan.name.not.null");
        }

        if (getDegree() == null) {
            throw new DomainException("error.degreeCurricularPlan.degree.not.null");
        }

        if (getDegree().getDegreeCurricularPlansSet().stream().filter(dcp -> dcp != this)
                .anyMatch(dcp -> name.equalsIgnoreCase(dcp.getName()))) {
            throw new DomainException("error.degreeCurricularPlan.existing.name.and.degree");
        }

        final RootCourseGroup root = getRoot();
        if (root != null) {
            root.setName(name);
            root.setNameEn(name);
        }

        super.setName(name);
    }

    @Deprecated
    public boolean isEmpty() {
        return false;
    }

    public boolean isActive() {
        return getActive();
    }

    private Boolean getCanBeDeleted() {
        return canDeleteRoot() && getStudentCurricularPlansSet().isEmpty() && getCurricularCoursesSet().isEmpty() && getExecutionDegreesSet().isEmpty();
    }

    private boolean canDeleteRoot() {
        return getRoot().getCanBeDeleted();
    }

    public void delete() {
        if (getCanBeDeleted()) {
            getDynamicFieldSet().forEach(df -> {
                df.setDegreeCurricularPlan(null);
                df.delete();
            });

            setDegree(null);
            getRoot().delete();
            if (getDegreeStructure() != null) {
                getDegreeStructure().delete();
            }
            super.setConclusionGradeCalculator(null);
            getProgramConclusionConfigsSet().forEach(ProgramConclusionConfig::delete);
            setRootDomainObject(null);
            deleteDomainObject();
        } else {
            throw new DomainException("error.degree.curricular.plan.cant.delete");
        }
    }

    public String print() {
        StringBuilder dcp = new StringBuilder();

        dcp.append("[DCP ").append(this.getExternalId()).append("] ").append(this.getName()).append("\n");
        this.getRoot().print(dcp, "", null);

        return dcp.toString();
    }

    @Deprecated(forRemoval = true)
    public ExecutionDegree getExecutionDegreeByYear(final ExecutionYear executionYear) {
        for (final ExecutionDegree executionDegree : getExecutionDegreesSet()) {
            if (executionDegree.getExecutionYear() == executionYear) {
                return executionDegree;
            }
        }
        return null;
    }

    public Optional<ExecutionDegree> findExecutionDegree(final ExecutionInterval interval) {
        if (interval == null) {
            return Optional.empty();
        }
        return getExecutionDegreesSet().stream().filter(ed -> ed.getExecutionYear() == interval.getExecutionYear()).findAny();
    }

    // FIXME: Optimization Required
    @Deprecated(forRemoval = true)
    public ExecutionDegree getExecutionDegreeByAcademicInterval(final AcademicInterval academicInterval) {
        AcademicCalendarEntry academicCalendarEntry = academicInterval.getAcademicCalendarEntry();
        while (!(academicCalendarEntry instanceof AcademicCalendarRootEntry)) {
            if (academicCalendarEntry instanceof AcademicYearCE) {
                ExecutionYear year = ExecutionYear.getExecutionYear((AcademicYearCE) academicCalendarEntry);
                for (ExecutionDegree executionDegree : getExecutionDegreesSet()) {
                    if (executionDegree.getExecutionYear().getAcademicInterval().equals(year.getAcademicInterval())) {
                        return executionDegree;
                    }
                }
            }

            academicCalendarEntry = academicCalendarEntry.getParentEntry();
        }

        return null;
    }

    public Set<ExecutionYear> getExecutionYears() {
        return getExecutionDegreesSet().stream().map(ExecutionDegree::getExecutionYear).collect(Collectors.toSet());
    }

    public ExecutionYear getMostRecentExecutionYear() {
        return getMostRecentExecutionDegree().getExecutionYear();
    }

    public boolean hasAnyExecutionDegreeFor(final ExecutionYear executionYear) {
        return getExecutionDegreesSet().stream().anyMatch(executionDegree -> executionDegree.getExecutionYear() == executionYear);
    }

    public boolean hasExecutionDegreeFor(final ExecutionYear executionYear) {
        return findExecutionDegree(executionYear).isPresent();
    }

    public ExecutionDegree getMostRecentExecutionDegree() {
        if (getExecutionDegreesSet().isEmpty()) {
            return null;
        }

        // Prefer the execution degree for the current year.
        ExecutionYear currentYear = ExecutionYear.findCurrent(getDegree().getCalendar());
        ExecutionDegree current = findExecutionDegree(currentYear).orElse(null);
        if (current != null) {
            return current;
        }

        // If there is no execution degree for the current year, use the most recent previous year.
        // If there are no previous years, use the earliest future execution degree.
        return getExecutionDegreesSet().stream().filter(ed -> ed.getExecutionYear().isBeforeOrEquals(currentYear))
                .max(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_YEAR).orElseGet(
                        () -> getExecutionDegreesSet().stream().min(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_YEAR)
                                .orElse(null));
    }

    public ExecutionDegree getFirstExecutionDegree() {
        return getExecutionDegreesSet().stream().min(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_YEAR).orElse(null);
    }

    public Set<ExecutionCourse> getExecutionCourses(final ExecutionInterval executionInterval) {
        return getExecutionCourses(executionInterval, getRoot().getChildContextsSet()).collect(Collectors.toSet());
    }

    private Stream<ExecutionCourse> getExecutionCourses(final ExecutionInterval executionInterval, final Set<Context> contexts) {
        return contexts.stream().flatMap(context -> {
            DegreeModule degreeModule = context.getChildDegreeModule();
            if (degreeModule instanceof CurricularCourse) {
                return ((CurricularCourse) degreeModule).findExecutionCourses(executionInterval);
            } else if (degreeModule instanceof CourseGroup) {
                return getExecutionCourses(executionInterval, ((CourseGroup) degreeModule).getChildContextsSet());
            }
            return Stream.empty();
        });
    }

    /**
     * @deprecated use {@link #getExecutionCoursesByExecutionInterval(ExecutionInterval)}
     */
    @Deprecated
    public Set<ExecutionCourse> getExecutionCoursesByExecutionPeriod(final ExecutionInterval executionInterval) {
        return getExecutionCourses(executionInterval);
    }

    public Set<CurricularCourse> getAllCurricularCourses() {
        return getRoot().getAllCurricularCourses().stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(DegreeModule.COMPARATOR_BY_NAME)));
    }

    public List<CurricularCourse> getCurricularCoursesWithExecutionIn(final ExecutionYear executionYear) {
        List<CurricularCourse> curricularCourses = new ArrayList<>();
        for (CurricularCourse curricularCourse : getCurricularCoursesSet()) {
            for (ExecutionInterval executionInterval : executionYear.getChildIntervals()) {
                List<ExecutionCourse> executionCourses = curricularCourse.getExecutionCoursesByExecutionPeriod(executionInterval);
                if (!executionCourses.isEmpty()) {
                    curricularCourses.add(curricularCourse);
                    break;
                }
            }
        }
        return curricularCourses;
    }

    public CurricularCourse getCurricularCourseByCode(final String code) {
        for (CurricularCourse curricularCourse : getCurricularCoursesSet()) {
            if (curricularCourse.getCode() != null && curricularCourse.getCode().equals(code)) {
                return curricularCourse;
            }
        }
        return null;
    }

    @Override
    public Set<CurricularCourse> getCurricularCoursesSet() {
        return this.getCurricularCourses((ExecutionYear) null);
    }

    @Deprecated
    public Set<CurricularCourse> getCurricularCoursesSetSuperDoNotUseTempFix() {
        return super.getCurricularCoursesSet();
    }

    public Set<CurricularCourse> getCurricularCourses(final ExecutionInterval executionInterval) {
        final Set<CurricularCourse> curricularCourses = new HashSet<>();
        for (final CurricularCourse curricularCourse : super.getCurricularCoursesSet()) {
            if (curricularCourse.hasScopeInGivenSemesterAndCurricularYearInDCP(null, null, executionInterval)) {
                curricularCourses.add(curricularCourse);
            }
        }
        final ExecutionYear executionYear = executionInterval.getExecutionYear();
        for (final DegreeModule degreeModule : getDcpDegreeModules(CurricularCourse.class, executionYear)) {
            curricularCourses.add((CurricularCourse) degreeModule);
        }
        return curricularCourses;
    }

    /**
     * Method to get a filtered list of a dcp's curricular courses, with at
     * least one open context in the given execution year
     *
     * @return All curricular courses that are present in the dcp
     */
    private Set<CurricularCourse> getCurricularCourses(final ExecutionYear executionYear) {
        return getDcpDegreeModules(CurricularCourse.class, executionYear).stream().map(CurricularCourse.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * Method to get an unfiltered list of a bolonha dcp's competence courses
     *
     * @return All competence courses that were or still are present in the dcp,
     *         ordered by name
     */
    public List<CompetenceCourse> getCompetenceCourses() {
        return getCompetenceCourses(null);
    }

    /**
     * Method to get a filtered list of a dcp's competence courses in the given
     * execution year. Each competence courses is connected with a curricular
     * course with at least one open context in the execution year
     *
     * @return All competence courses that are present in the dcp
     */
    public List<CompetenceCourse> getCompetenceCourses(final ExecutionYear executionYear) {
        return getCurricularCourses(executionYear).stream().filter(cc -> !cc.isOptionalCurricularCourse())
                .map(CurricularCourse::getCompetenceCourse).distinct()
                .sorted(CompetenceCourse.COMPETENCE_COURSE_COMPARATOR_BY_NAME).toList();
    }

    public Set<CurricularCourse> getActiveCurricularCourses(final ExecutionInterval executionInterval) {
        return getCurricularCoursesSet().stream().filter(cc -> cc.hasAnyActiveContext(executionInterval))
                .collect(Collectors.toSet());
    }

    public CourseGroup createCourseGroup(final CourseGroup parentCourseGroup, final String name, final String nameEn,
            final ExecutionInterval begin, final ExecutionInterval end, final ProgramConclusion programConclusion) {
        return new CourseGroup(parentCourseGroup, name, nameEn, begin, end, programConclusion);
    }

    public CourseGroup createBranchCourseGroup(final CourseGroup parentCourseGroup, final String name, final String nameEn,
            final BranchType branchType, final ExecutionInterval begin, final ExecutionInterval end) {
        if (branchType == null) {
            throw new DomainException("error.degreeStructure.BranchCourseGroup.branch.type.cannot.be.null");
        }

        final CourseGroup result = new CourseGroup(parentCourseGroup, name, nameEn, begin, end);
        result.setBranchType(branchType);

        return result;
    }

    public CurricularCourse createOptionalCurricularCourse(final CourseGroup parentCourseGroup, final String name,
            final String nameEn, final CurricularPeriod curricularPeriod, final ExecutionInterval begin,
            final ExecutionInterval end) {

        return new OptionalCurricularCourse(parentCourseGroup, name, nameEn, curricularPeriod, begin, end);
    }

    public List<DegreeModule> getDcpDegreeModules(final Class<? extends DegreeModule> clazz) {
        return getDcpDegreeModules(clazz, (ExecutionYear) null);
    }

    public List<DegreeModule> getDcpDegreeModules(final Class<? extends DegreeModule> clazz, final ExecutionYear executionYear) {
        return new ArrayList<>(getRoot().collectAllChildDegreeModules(clazz, executionYear));
    }

    public void editDuration(final AcademicPeriod newDuration) {
        if (!(newDuration instanceof AcademicYears)) {
            throw new DomainException("error.degreeCurricularPlan.duration.must.be.specified.in.years");
        }

        final CurricularPeriod currentStructure = getDegreeStructure();
        if (currentStructure == null) {
            super.setDegreeStructure(new CurricularPeriod(newDuration));
            return;
        }

        final AcademicPeriod currentDuration = currentStructure.getAcademicPeriod();
        if (currentDuration.equals(newDuration)) {
            return;
        }

        if (newDuration.getWeight() == 1) { // periods tree will shrink from three to two levels
            currentStructure.findChild(AcademicPeriod.YEAR, 1).ifPresentOrElse(existing1stYearPeriod -> {
                existing1stYearPeriod.setParent(null);
                super.setDegreeStructure(existing1stYearPeriod);
                existing1stYearPeriod.setChildOrder(null);
                currentStructure.delete();
            }, () -> {
                super.setDegreeStructure(new CurricularPeriod(newDuration));
                currentStructure.delete();
            });

            return;
        }

        if (newDuration.getWeight() > 1 && currentDuration.getWeight() == 1) { // periods tree will grow from two to three levels
            final CurricularPeriod newStructure = new CurricularPeriod(newDuration);
            super.setDegreeStructure(newStructure);
            currentStructure.setParent(newStructure);
            return;
        }

        if (newDuration.getWeight() < currentDuration.getWeight()) {
            IntStream.rangeClosed((int) newDuration.getWeight() + 1, (int) currentDuration.getWeight()).boxed()
                    .flatMap(outerYear -> currentStructure.findChild(AcademicPeriod.YEAR, outerYear).stream())
                    .forEach(CurricularPeriod::delete);
        }

        currentStructure.setAcademicPeriod(newDuration);
    }

    @Override
    public void setDegreeStructure(final CurricularPeriod degreeStructure) {
        throw new DomainException("error.degreeCurricularPlan.degreeStructure.cannot.be.invoked.publicly");
    }

    public String getPresentationName() {
        return getPresentationName(ExecutionYear.findCurrent(getDegree().getCalendar()), I18N.getLocale());
    }

    public String getPresentationName(final ExecutionYear executionYear) {
        return getPresentationName(executionYear, I18N.getLocale());
    }

    public String getPresentationName(final ExecutionYear executionYear, final Locale locale) {
        return getDegree().getPresentationName(executionYear, locale) + " - " + getName();
    }

    /**
     * @deprecated DegreeCurricularPlans cannot be empty anymore so usage of this method is unecessary
     */
    @Deprecated
    public static List<DegreeCurricularPlan> readNotEmptyDegreeCurricularPlans() {
        return new ArrayList<>(Bennu.getInstance().getDegreeCurricularPlansSet());
    }

    public static Stream<DegreeCurricularPlan> findAll() {
        return Bennu.getInstance().getDegreeCurricularPlansSet().stream();
    }

    public static DegreeCurricularPlan readByNameAndDegreeSigla(final String name, final String degreeSigla) {
        return findAll().filter(dcp -> StringUtils.equalsIgnoreCase(dcp.getName(), name) && StringUtils.equalsIgnoreCase(
                dcp.getDegree().getSigla(), degreeSigla)).findFirst().orElse(null);
    }

    public ExecutionDegree createExecutionDegree(final ExecutionYear executionYear) {
        if (this.hasAnyExecutionDegreeFor(executionYear)) {
            throw new DomainException("degree.curricular.plan.already.has.execution.degree.for.this.year", this.getName(),
                    executionYear.getYear());
        }

        return new ExecutionDegree(this, executionYear, Boolean.FALSE);
    }

    @Deprecated
    public ExecutionDegree createExecutionDegree(final ExecutionYear executionYear, final Space campus,
            final Boolean publishedExamMap) {

        return createExecutionDegree(executionYear);
    }

    /**
     * @deprecated use {@link #getCurricularPeriodFor(int, int, AcademicPeriod)}
     */
    @Deprecated
    public CurricularPeriod getCurricularPeriodFor(final int year, final int semester) {
        final CurricularPeriodInfoDTO[] curricularPeriodInfos = buildCurricularPeriodInfoDTOsFor(year, semester);
        return getDegreeStructure().getCurricularPeriod(curricularPeriodInfos);
    }

    @Deprecated
    private CurricularPeriodInfoDTO[] buildCurricularPeriodInfoDTOsFor(final int year, final int semester) {
        final CurricularPeriodInfoDTO[] curricularPeriodInfos;
        if (getDurationInYears() > 1) {

            curricularPeriodInfos = new CurricularPeriodInfoDTO[] { new CurricularPeriodInfoDTO(year, AcademicPeriod.YEAR),
                    new CurricularPeriodInfoDTO(semester, AcademicPeriod.SEMESTER) };

        } else {
            curricularPeriodInfos =
                    new CurricularPeriodInfoDTO[] { new CurricularPeriodInfoDTO(semester, AcademicPeriod.SEMESTER) };
        }
        return curricularPeriodInfos;
    }

    public CurricularPeriod getCurricularPeriodFor(final int year, final int childOrder,
            final AcademicPeriod childAcademicPeriod) {

        final Predicate<CurricularPeriod> isYearDuration = cp -> cp.getParent() == getDegreeStructure() /*root*/ && year == 1
                && cp.getParent().getAcademicPeriod().equals(AcademicPeriod.YEAR);
        final Predicate<CurricularPeriod> matchesYear = cp -> cp.getParent().getAcademicPeriod().equals(AcademicPeriod.YEAR)
                && cp.getParentOrder() != null && year == cp.getParentOrder().intValue();
        final Predicate<CurricularPeriod> matchesChild =
                cp -> cp.getAcademicPeriod().equals(childAcademicPeriod) && childOrder == cp.getChildOrder().intValue();

        return getAllCurricularPeriodChilds(getDegreeStructure()).stream().filter(isYearDuration.or(matchesYear))
                .filter(matchesChild).findFirst().orElse(null);
    }

    private List<CurricularPeriod> getAllCurricularPeriodChilds(final CurricularPeriod curricularPeriod) {
        final List<CurricularPeriod> result = curricularPeriod.getChildsSet().stream().collect(Collectors.toList());
        result.addAll(curricularPeriod.getChildsSet().stream().flatMap(cp -> getAllCurricularPeriodChilds(cp).stream())
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public YearMonthDay getInitialDateYearMonthDay() {
        final ExecutionDegree firstExecutionDegree = getFirstExecutionDegree();
        return firstExecutionDegree != null ? firstExecutionDegree.getExecutionYear()
                .getBeginDateYearMonthDay() : super.getInitialDateYearMonthDay();
    }

    public Collection<StudentCurricularPlan> getActiveStudentCurricularPlans() {
        final Collection<StudentCurricularPlan> result = new HashSet<>();

        for (StudentCurricularPlan studentCurricularPlan : getStudentCurricularPlansSet()) {
            if (studentCurricularPlan.isActive()) {
                result.add(studentCurricularPlan);
            }
        }

        return result;
    }

    @Deprecated
    public Set<Registration> getRegistrations() {
        final Set<Registration> registrations = new HashSet<>();

        for (StudentCurricularPlan studentCurricularPlan : getActiveStudentCurricularPlans()) {
            registrations.add(studentCurricularPlan.getRegistration());
        }

        return registrations;
    }

    public Collection<Registration> getActiveRegistrations() {
        final Collection<Registration> result = new HashSet<>();

        for (StudentCurricularPlan studentCurricularPlan : getActiveStudentCurricularPlans()) {
            final Registration registration = studentCurricularPlan.getRegistration();

            if (registration.isActive()) {
                result.add(registration);
            }
        }

        return result;
    }

    @Override
    public Integer getDegreeDuration() {
        final Integer degreeDuration = super.getDegreeDuration();
        return degreeDuration == null ? getDurationInYears() : degreeDuration;
    }

    public DegreeType getDegreeType() {
        return getDegree().getDegreeType();
    }

    public boolean isFirstCycle() {
        return getDegree().isFirstCycle();
    }

    public CycleCourseGroup getFirstCycleCourseGroup() {
        return isFirstCycle() ? getRoot().getFirstCycleCourseGroup() : null;
    }

    public boolean isSecondCycle() {
        return getDegree().isSecondCycle();
    }

    public CycleCourseGroup getSecondCycleCourseGroup() {
        return isSecondCycle() ? getRoot().getSecondCycleCourseGroup() : null;
    }

    public CycleCourseGroup getCycleCourseGroup(final CycleType cycleType) {
        return getRoot().getCycleCourseGroup(cycleType);
    }

    public String getGraduateTitle(final ExecutionYear executionYear, final ProgramConclusion programConclusion,
            final Locale locale) {
        return programConclusion.groupFor(this).map(cg -> cg.getGraduateTitle(executionYear, locale)).orElse(null);
    }

    public boolean hasDegreeModule(final DegreeModule degreeModule) {
        return getRoot().hasDegreeModule(degreeModule);
    }

    public final List<StudentCurricularPlan> getLastStudentCurricularPlan() {
        List<StudentCurricularPlan> studentCurricularPlans = new ArrayList<>();
        for (StudentCurricularPlan studentCurricularPlan : this.getStudentCurricularPlansSet()) {
            studentCurricularPlans.add(studentCurricularPlan.getRegistration().getLastStudentCurricularPlan());

        }
        return studentCurricularPlans;
    }

    public Collection<CourseGroup> getAllCoursesGroups() {
        return getAllDegreeModules().filter(dm -> dm.isCourseGroup()).map(CourseGroup.class::cast).collect(Collectors.toSet());
    }

    public Collection<CourseGroup> getAllBranches() {
        return getAllCoursesGroups().stream().filter(cg -> cg.isBranchCourseGroup()).collect(Collectors.toSet());
    }

    public Stream<DegreeModule> getAllDegreeModules() {
        final Set<DegreeModule> degreeModules = new HashSet<>();
        degreeModules.add(getRoot());
        getRoot().getAllDegreeModules(degreeModules);

        return degreeModules.stream();
    }

    public static Set<DegreeCurricularPlan> getDegreeCurricularPlans(final java.util.function.Predicate<DegreeType> predicate) {
        final Set<DegreeCurricularPlan> degreeCurricularPlans =
                new TreeSet<>(DegreeCurricularPlan.COMPARATOR_BY_PRESENTATION_NAME);

        for (final Degree degree : Degree.readNotEmptyDegrees()) {
            if (predicate.test(degree.getDegreeType())) {
                for (final DegreeCurricularPlan degreeCurricularPlan : degree.getDegreeCurricularPlansSet()) {
                    if (degreeCurricularPlan.isActive()) {
                        degreeCurricularPlans.add(degreeCurricularPlan);
                    }
                }
            }
        }
        return degreeCurricularPlans;
    }

    public Set<CurricularCourse> getCurricularCoursesByExecutionYearAndCurricularYear(final ExecutionYear executionYear,
            final Integer curricularYear) {
        return getCurricularCoursesWithExecutionIn(executionYear).stream()
                .filter(cc -> cc.getParentContextsSet().stream().anyMatch(ctx -> ctx.getCurricularYear().equals(curricularYear)))
                .collect(Collectors.toSet());
    }

    /**
     * This must be completely refactored. A pattern of some sort is desirable
     * in order to make this instance-dependent. Just did this due to time
     * constrains.
     */

    public Set<Registration> getRegistrations(final ExecutionYear executionYear, final Set<Registration> registrations) {
        for (final StudentCurricularPlan studentCurricularPlan : this.getStudentCurricularPlansSet()) {
            if (studentCurricularPlan.isActive(executionYear)) {
                if (studentCurricularPlan.getRegistration() != null) {
                    registrations.add(studentCurricularPlan.getRegistration());
                }
            }
        }
        return registrations;
    }

    public boolean isToApplyPreviousYearsEnrolmentRule() {
        return getApplyPreviousYearsEnrolmentRule();
    }

    public ExecutionInterval getBegin() {
        return getBeginContextExecutionYears().stream().min(ExecutionYear.COMPARATOR_BY_YEAR).orElse(null);
    }

    public Set<ExecutionYear> getBeginContextExecutionYears() {
        return getRoot().getBeginContextExecutionYears();
    }

    public LocalizedString getDescriptionI18N() {
        LocalizedString result = new LocalizedString();

        if (!StringUtils.isEmpty(getDescription())) {
            result = result.with(LocaleUtils.PT, getDescription());
        }
        if (!StringUtils.isEmpty(getDescriptionEn())) {
            result = result.with(LocaleUtils.EN, getDescriptionEn());
        }

        return result;
    }

    public void setDescriptionI18N(final LocalizedString input) {
        if (input != null && !input.isEmpty()) {
            setDescription(input.getContent(LocaleUtils.PT));
            setDescriptionEn(input.getContent(LocaleUtils.EN));
        } else {
            setDescription(null);
            setDescriptionEn(null);
        }
    }

    public Collection<CycleCourseGroup> getDestinationAffinities(final CycleType sourceCycleType) {
        final CycleCourseGroup cycleCourseGroup = getRoot().getCycleCourseGroup(sourceCycleType);
        if (cycleCourseGroup != null) {
            return cycleCourseGroup.getDestinationAffinitiesSet();
        }
        return Collections.EMPTY_LIST;
    }

    public Double getEctsCredits() {
        return getDegree().getEctsCredits();
    }

    public ExecutionYear getLastExecutionYear() {
        return getExecutionDegreesSet().stream().max(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_YEAR)
                .map(ExecutionDegree::getExecutionYear).orElse(null);
    }

    @Deprecated
    public java.util.Date getInitialDate() {
        org.joda.time.YearMonthDay ymd = getInitialDateYearMonthDay();
        return ymd == null ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    @Deprecated
    public void setInitialDate(final java.util.Date date) {
        if (date == null) {
            setInitialDateYearMonthDay(null);
        } else {
            setInitialDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    public int getDurationInYears() {
        if (getDegreeStructure() != null) {
            return Float.valueOf(getDegreeStructure().getAcademicPeriod().getWeight()).intValue();
        }
        return 0;
    }

    public int getDurationInSemesters() {
        return Float.valueOf(getDurationInYears() / AcademicPeriod.SEMESTER.getWeight()).intValue();
    }

    public int getDurationInYears(final CycleType cycleType) {

        if (cycleType == null || getDegreeType().hasExactlyOneCycleType()) {
            return getDurationInYears();
        }

        if (!getDegreeType().hasAnyCycleTypes()) {
            return 0;
        }

        return calculateCycleDuration(cycleType, ctx -> ctx.getCurricularPeriod().getParent(),
                cp -> cp.getAcademicPeriod().equals(AcademicPeriod.YEAR));

    }

    private int calculateCycleDuration(final CycleType cycleType,
            final Function<Context, CurricularPeriod> curricularPeriodCollector,
            final java.util.function.Predicate<CurricularPeriod> curricularPeriodFilter) {

        final CycleCourseGroup cycleCourseGroup = getRoot().getCycleCourseGroup(cycleType);
        if (cycleCourseGroup == null) {
            //structure is not correct
            throw new DomainException("error.degreeCurricularPlan.unable.to.find.cycle.in.structure.to.calculate.duration",
                    cycleType.getDescription());
        }

        return Math.toIntExact(
                getAllCoursesGroups().stream().filter(cg -> cg.getParentCycleCourseGroups().contains(cycleCourseGroup))
                        .flatMap(cg -> cg.getChildContextsSet().stream()).filter(ctx -> ctx.getChildDegreeModule().isLeaf())
                        .map(curricularPeriodCollector).filter(curricularPeriodFilter).distinct().count());

    }

}
