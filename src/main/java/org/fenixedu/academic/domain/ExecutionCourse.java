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

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadDuration;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.schedule.lesson.LessonPeriod;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class ExecutionCourse extends ExecutionCourse_Base {
    public static final String CREATED_SIGNAL = "academic.executionCourse.create";

    public static final String EDITED_SIGNAL = "academic.executionCourse.edit";

    public static final String ACRONYM_CHANGED_SIGNAL = "academic.executionCourse.acronym.edit";

    public static final Comparator<ExecutionCourse> EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR =
            Comparator.comparing(ExecutionCourse::getExecutionInterval);

    public static final Comparator<ExecutionCourse> EXECUTION_COURSE_NAME_COMPARATOR =
            Comparator.comparing(ExecutionCourse::getName, Collator.getInstance()).thenComparing(ExecutionCourse::getExternalId);

    static {
        getRelationCurricularCourseExecutionCourse().addListener(new CurricularCourseExecutionCourseListener());
    }

    public ExecutionCourse(final String name, final String initials, final ExecutionInterval executionInterval) {
        super();

        setRootDomainObject(Bennu.getInstance());

        setNome(name);
        setExecutionPeriod(executionInterval);
        setSigla(initials);

        Signal.emit(ExecutionCourse.CREATED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(this));
    }

    @Override
    public void setNome(final String nome) {
        super.setNome(nome);
        Signal.emit(ExecutionCourse.EDITED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(this));
    }

    @Override
    public void setExecutionPeriod(final ExecutionInterval executionPeriod) {
        super.setExecutionPeriod(executionPeriod);
        Signal.emit(ExecutionCourse.EDITED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(this));
    }

    @Override
    public void removeAssociatedCurricularCourses(final CurricularCourse associatedCurricularCourses) {
        super.removeAssociatedCurricularCourses(associatedCurricularCourses);
        Signal.emit(ExecutionCourse.EDITED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(this));

    }

    public List<Professorship> responsibleFors() {
        return getProfessorshipsSet().stream().filter(Professorship::getResponsibleFor).collect(Collectors.toList());
    }

    public Attends getAttendsByStudent(final Registration registration) {
        return getAttendsSet().stream().filter(a -> a.getRegistration() == registration).findFirst().orElse(null);
    }

    public Attends getAttendsByStudent(final Student student) {
        return getAttendsSet().stream().filter(a -> a.isFor(student)).findFirst().orElse(null);
    }

    // Delete Method
    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        getStudentGroupSet().forEach(g -> {
            g.setExecutionCourse(null);
            deleteGroup(g);
        });

        getSpecialCriteriaOverExecutionCourseGroupSet().forEach(g -> {
            g.setExecutionCourse(null);
            deleteGroup(g);
        });

        while (!getProfessorshipsSet().isEmpty()) {
            getProfessorshipsSet().iterator().next().delete();
        }

        while (!getLessonPlanningsSet().isEmpty()) {
            getLessonPlanningsSet().iterator().next().delete();
        }

        while (!getAttendsSet().isEmpty()) {
            getAttendsSet().iterator().next().delete();
        }

        while (!getExecutionCourseLogsSet().isEmpty()) {
            getExecutionCourseLogsSet().iterator().next().delete();
        }

        getAssociatedCurricularCoursesSet().clear();
        getTeacherGroupSet().clear();
        setExecutionPeriod(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    private void deleteGroup(PersistentGroup group) {
        if (!group.isDeletable()) {
            throw new IllegalStateException(BundleUtil.getString(Bundle.APPLICATION,
                    "error.executionCourse.cannotDeleteExecutionCourseUsedInAccessControl"));
        }
        PersistentGroup.garbageCollect(group);
    }

    @Override
    public void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getAssociatedSummariesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ExecutionCourse.cannotBeDeleted.hasSummaries"));
        }
        if (!getAssociatedEvaluationsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ExecutionCourse.cannotBeDeleted.hasEvaluations"));
        }
        if (!getAssociatedShifts().isEmpty() || !getShiftsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ExecutionCourse.cannotBeDeleted.hasShifts"));
        }
        if (!getAttendsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ExecutionCourse.cannotBeDeleted.hasAttends"));
        }

        for (final Professorship professorship : getProfessorshipsSet()) {
            if (!professorship.isDeletable()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ExecutionCourse.cannotBeDeleted.hasProfessorships"));
            }
        }
    }

    @Deprecated
    public Set<Shift> getAssociatedShifts() {
        return getShiftsSet();
    }

    private static class CurricularCourseExecutionCourseListener extends RelationAdapter<ExecutionCourse, CurricularCourse> {

        @Override
        public void afterAdd(ExecutionCourse execution, CurricularCourse curricular) {
            for (final Enrolment enrolment : curricular.getEnrolments()) {
                if (enrolment.getExecutionInterval().equals(execution.getExecutionInterval())) {
                    associateAttend(enrolment, execution);
                }
            }
        }

        @Override
        public void afterRemove(ExecutionCourse execution, CurricularCourse curricular) {
            if (execution != null) {
                for (Attends attends : execution.getAttendsSet()) {
                    if ((attends.getEnrolment() != null) && (attends.getEnrolment().getCurricularCourse().equals(curricular))) {
                        attends.setEnrolment(null);
                    }
                }
            }
        }

        private static void associateAttend(Enrolment enrolment, ExecutionCourse executionCourse) {
            if (!alreadyHasAttend(enrolment, executionCourse)) {
                enrolment.findOrCreateAttends(executionCourse);
            }
        }

        private static boolean alreadyHasAttend(Enrolment enrolment, ExecutionCourse executionCourse) {
            final ExecutionInterval executionInterval = executionCourse.getExecutionInterval();
            if (enrolment.getAttendsSet().stream().anyMatch(a -> a.getExecutionInterval() == executionInterval)) {
                return true;
            }

            final Attends attendsByStudent = executionCourse.getAttendsByStudent(enrolment.getRegistration().getStudent());
            return attendsByStudent != null && attendsByStudent.getEnrolment() != null && !attendsByStudent.getEnrolment()
                    .isAnnulled();
        }

    }

    public SortedSet<Degree> getDegreesSortedByDegreeName() {
        final SortedSet<Degree> degrees = new TreeSet<Degree>(Degree.COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID);
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final DegreeCurricularPlan degreeCurricularPlan = curricularCourse.getDegreeCurricularPlan();
            degrees.add(degreeCurricularPlan.getDegree());
        }
        return degrees;
    }

    public Set<CompetenceCourse> getCompetenceCourses() {
        return getAssociatedCurricularCoursesSet().stream().map(CurricularCourse::getCompetenceCourse).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<CompetenceCourseInformation> getCompetenceCoursesInformations() {
        return getAssociatedCurricularCoursesSet().stream().map(CurricularCourse::getCompetenceCourse).filter(Objects::nonNull)
                .map(cc -> cc.findInformationMostRecentUntil(getExecutionInterval())).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Stream<Shift> findShiftsByLoadType(final CourseLoadType loadType) {
        return getShiftsSet().stream().filter(s -> s.getCourseLoadType() == loadType);
    }

    private Set<SchoolClass> getAllSchoolClassesOrBy(DegreeCurricularPlan degreeCurricularPlan) {
        return getAssociatedShifts().stream().flatMap(shift -> shift.getAssociatedClassesSet().stream())
                .filter(sc -> degreeCurricularPlan == null
                        || sc.getExecutionDegree().getDegreeCurricularPlan() == degreeCurricularPlan).collect(Collectors.toSet());
    }

    public Set<SchoolClass> getSchoolClassesBy(DegreeCurricularPlan degreeCurricularPlan) {
        return getAllSchoolClassesOrBy(degreeCurricularPlan);
    }

    public Set<SchoolClass> getSchoolClasses() {
        return getAllSchoolClassesOrBy(null);
    }

    public Collection<CourseLoadType> getCourseLoadTypes() {
        return getCompetenceCoursesInformations().stream().flatMap(cci -> cci.getCourseLoadDurationsSet().stream())
                .map(CourseLoadDuration::getCourseLoadType).filter(CourseLoadType::getAllowShifts).collect(Collectors.toSet());
    }

    @Override
    public String getNome() {
        if (I18N.getLocale().getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            return getNameEn();
        }
        return super.getNome();
    }

    private String getNameEn() {
        return getCompetenceCoursesInformations().stream().map(cci -> cci.getNameEn()).filter(Objects::nonNull).distinct()
                .sorted().collect(Collectors.joining(" / "));
    }

    public String getName() {
        return getNome();
    }

    public void updateName() {
        final String newName =
                getCompetenceCoursesInformations().stream().map(cci -> cci.getName()).filter(Objects::nonNull).distinct().sorted()
                        .collect(Collectors.joining(" / "));
        setNome(newName);
    }

    public String getCode() {
        return getCompetenceCourses().stream().map(cc -> cc.getCode()).distinct().sorted().collect(Collectors.joining(", "));
    }

    public String getPrettyAcronym() {
        return getSigla().replaceAll("[0-9]", "");
    }

    public String getDegreePresentationString() {
        return getAssociatedCurricularCoursesSet().stream().map(cc -> cc.getDegree().getSigla()).distinct().sorted()
                .collect(Collectors.joining(", "));
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionInterval().getExecutionYear();
    }

    public Set<ExecutionDegree> getExecutionDegrees() {
        return getAssociatedCurricularCoursesSet().stream().map(CurricularCourse::getDegreeCurricularPlan)
                .flatMap(dcp -> dcp.findExecutionDegree(getExecutionInterval()).stream())
                .collect(Collectors.toSet());
    }

    public Interval getMaxLessonsInterval() {
        final boolean isAnual = getAssociatedCurricularCoursesSet().stream().anyMatch(cc -> cc.isAnual(getExecutionYear()));

        final Set<ExecutionInterval> periodsExecutionIntervals =
                isAnual ? getExecutionInterval().getExecutionYear().getChildIntervals() : Set.of(getExecutionInterval());

        final Function<ExecutionDegree, Stream<OccupationPeriod>> executionDegreeToPeriods =
                ed -> periodsExecutionIntervals.stream()
                        .flatMap(ei -> LessonPeriod.findFor(ed, this).map(LessonPeriod::getOccupationPeriod));

        final Function<DegreeCurricularPlan, Stream<ExecutionDegree>> dcpToExecutionDegree =
                dcp -> dcp.findExecutionDegree(getExecutionInterval()).stream();

        final Collection<Interval> allIntervals =
                getAssociatedCurricularCoursesSet().stream().map(CurricularCourse::getDegreeCurricularPlan)
                        .flatMap(dcpToExecutionDegree).flatMap(executionDegreeToPeriods)
                        .map(OccupationPeriod::getIntervalWithNextPeriods).collect(Collectors.toSet());

        if (!allIntervals.isEmpty()) {
            final DateTime start = allIntervals.stream().map(Interval::getStart).min(Comparator.naturalOrder()).get();
            final DateTime end = allIntervals.stream().map(Interval::getEnd).max(Comparator.naturalOrder()).get();
            return new Interval(start, end);
        }

        return null;
    }

    public AcademicInterval getAcademicInterval() {
        return getExecutionInterval().getAcademicInterval();
    }

    @Override
    public void setSigla(String sigla) {
        final String code = sigla.replace(' ', '_').replace('/', '-');
        final String uniqueCode = findUniqueCode(code);
        if (uniqueCode.equals(this.getSigla())) {
            return;
        }
        super.setSigla(uniqueCode);
        Signal.emit(ExecutionCourse.ACRONYM_CHANGED_SIGNAL, new DomainObjectEvent<>(this));
    }

    private String findUniqueCode(final String code) {
        String candidate = code;
        int c = 0;
        while (existsMatchingCode(candidate)) {
            candidate = code + "-" + c++;
        }
        return candidate;
    }

    private boolean existsMatchingCode(final String code) {
        return getExecutionInterval().getAssociatedExecutionCoursesSet().stream()
                .anyMatch(ec -> ec != this && ec.getSigla().equalsIgnoreCase(code));
    }

    public Collection<DegreeCurricularPlan> getAssociatedDegreeCurricularPlans() {
        return getAssociatedCurricularCoursesSet().stream().map(CurricularCourse::getDegreeCurricularPlan)
                .collect(Collectors.toSet());
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    public Professorship getProfessorship(final Person person) {
        for (final Professorship professorship : getProfessorshipsSet()) {
            if (professorship.getPerson() == person) {
                return professorship;
            }
        }
        return null;
    }

    /*
     * This method returns the portuguese name and the english name
     */
    public LocalizedString getNameI18N() {
        LocalizedString nameI18N = new LocalizedString();
        nameI18N = nameI18N.with(LocaleUtils.PT, super.getNome());
        nameI18N = nameI18N.with(LocaleUtils.EN, getNameEn());
        return nameI18N;
    }

    public Professorship getProfessorshipForCurrentUser() {
        return this.getProfessorship(AccessControl.getPerson());
    }

    @Deprecated
    public int getEnrolmentCount() {
        return (int) getAttendsSet().stream().filter(a -> a.getEnrolment() != null).count();
    }

    @Deprecated
    public List<Enrolment> getActiveEnrollments() {
        return this.getAssociatedCurricularCoursesSet().stream()
                .flatMap(cc -> cc.getEnrolmentsByExecutionPeriod(getExecutionInterval()).stream())
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void addAssociatedCurricularCourses(final CurricularCourse curricularCourse) {
        Collection<ExecutionCourse> executionCourses = curricularCourse.getAssociatedExecutionCoursesSet();

        for (ExecutionCourse executionCourse : executionCourses) {
            if (this != executionCourse && executionCourse.getExecutionInterval() == getExecutionInterval()) {
                throw new DomainException("error.executionCourse.curricularCourse.already.associated");
            }
        }

        super.addAssociatedCurricularCourses(curricularCourse);
        Signal.emit(ExecutionCourse.EDITED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(this));
    }

    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionPeriod();
    }

}
