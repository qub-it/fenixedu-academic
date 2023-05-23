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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.core.domain.Bennu;

/**
 * 
 * @author Luis Cruz &amp; Sara Ribeiro
 */
public class SchoolClass extends SchoolClass_Base {

    public static final Comparator<SchoolClass> COMPARATOR_BY_NAME =
            Comparator.comparing(SchoolClass::getName).thenComparing(SchoolClass::getExternalId);

    public SchoolClass(final ExecutionDegree executionDegree, final ExecutionInterval executionInterval, final String name,
            final Integer curricularYear) {
        super();

        setRootDomainObject(Bennu.getInstance());
        setExecutionDegree(executionDegree);
        setExecutionPeriod(executionInterval);
        setCurricularYear(curricularYear);
        setName(name); // must be set after executionDegree, executionInterval and curricularYear, in order to check duplicate names
        setAvailableForEnrolment(true);
    }

    public void delete() {
        getAssociatedShiftsSet().clear();
        super.setExecutionDegree(null);
        super.setExecutionPeriod(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

    @Override
    public void setExecutionDegree(ExecutionDegree executionDegree) {
        if (executionDegree == null) {
            throw new DomainException("error.SchoolClass.empty.executionDegree");
        }
        super.setExecutionDegree(executionDegree);
    }

    @Override
    public void setExecutionPeriod(ExecutionInterval executionInterval) {
        if (executionInterval == null) {
            throw new DomainException("error.SchoolClass.empty.executionPeriod");
        }
        super.setExecutionPeriod(executionInterval);
    }

    @Override
    public void setCurricularYear(Integer curricularYear) {
        if (curricularYear == null) {
            throw new DomainException("error.SchoolClass.empty.curricularYear");
        }
        super.setCurricularYear(curricularYear);
    }

    public void setName(final String name) {
        if (StringUtils.isBlank(name)) {
            throw new DomainException("error.SchoolClass.empty.name");
        }

        // check duplicated names
        if (getExecutionDegree() != null && getExecutionInterval() != null && getCurricularYear() != null) {
            if (getExecutionDegree().getSchoolClassesSet().stream().filter(sc -> sc != this)
                    .filter(sc -> getExecutionInterval() == sc.getExecutionInterval())
                    .filter(sc -> getCurricularYear().equals(sc.getCurricularYear()))
                    .anyMatch(sc -> name.equalsIgnoreCase(sc.getName()))) {
                throw new DomainException("Duplicate Entry: " + name + " for curricular year " + getCurricularYear()
                        + " and degree curricular plan " + getExecutionDegree().getDegreeCurricularPlan().getName());
            }
        }

        super.setName(name);
    }

    public Set<Shift> findPossibleShiftsToAdd() {
        final ExecutionDegree executionDegree = getExecutionDegree();
        final DegreeCurricularPlan degreeCurricularPlan = executionDegree.getDegreeCurricularPlan();

        final Set<Shift> shifts = new HashSet<Shift>();
        for (final CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            if (curricularCourse.isActive(getExecutionInterval(), getCurricularYear())) {
                for (final ExecutionCourse executionCourse : curricularCourse.getAssociatedExecutionCoursesSet()) {
                    if (executionCourse.getExecutionInterval() == getExecutionInterval()) {
                        shifts.addAll(executionCourse.getAssociatedShifts());
                    }
                }
            }
        }
        shifts.removeAll(getAssociatedShiftsSet());
        return shifts;
    }

    public AcademicInterval getAcademicInterval() {
        return getExecutionInterval().getAcademicInterval();
    }

    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionPeriod();
    }

    @Override
    public Boolean getAvailableForEnrolment() {
        return Optional.ofNullable(super.getAvailableForEnrolment()).orElse(Boolean.TRUE);
    }

    public boolean isFreeFor(final Registration registration) {

        final Map<ExecutionCourse, List<Shift>> shiftsByExecutionCourse =
                findShiftsFor(registration).collect(Collectors.groupingBy(Shift::getExecutionCourse));

        for (final ExecutionCourse executionCourse : shiftsByExecutionCourse.keySet()) {
            final Map<CourseLoadType, List<Shift>> shiftsByLoadType = shiftsByExecutionCourse.get(executionCourse).stream()
                    .collect(Collectors.groupingBy(Shift::getCourseLoadType));

            for (final CourseLoadType courseLoadType : shiftsByLoadType.keySet()) {
                final Optional<Shift> enrolledShift = registration.findEnrolledShiftFor(executionCourse, courseLoadType);
                if (enrolledShift.isEmpty() || !enrolledShift.get().getAssociatedClassesSet().contains(this)) {
                    if (shiftsByLoadType.get(courseLoadType).stream().noneMatch(s -> s.isFreeFor(registration))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public Stream<Shift> findShiftsFor(final Registration registration) {
        final List<ExecutionCourse> attendingCourses = registration.getAttendingExecutionCoursesFor(getExecutionInterval());
        return getAssociatedShiftsSet().stream().filter(s -> attendingCourses.contains(s.getExecutionCourse()));
    }

    /*
     * Unenrolls school class of provided execution interval, and enrolls in new school class
     */
    public static void replaceSchoolClass(final Registration registration, final SchoolClass schoolClass,
            final ExecutionInterval executionInterval) {

        registration.findSchoolClass(executionInterval).ifPresent(sc -> sc.unenrolInSchoolClassAndShifts(registration));

        if (schoolClass != null) {
            schoolClass.enrolInSchoolClassAndShifts(registration);
        }
    }

    private void enrolInSchoolClassAndShifts(final Registration registration) {
        registration.getSchoolClassesSet().add(this); // must add first in order to correct evaluation of shift capacities based on school classes

        final Comparator<Shift> vacanciesComparator = Comparator.comparing(Shift::getVacancies).reversed();

        final Map<ExecutionCourse, List<Shift>> shiftsByExecutionCourse =
                findShiftsFor(registration).collect(Collectors.groupingBy(Shift::getExecutionCourse));

        for (final ExecutionCourse executionCourse : shiftsByExecutionCourse.keySet()) {
            final Map<CourseLoadType, List<Shift>> shiftsByLoadType = shiftsByExecutionCourse.get(executionCourse).stream()
                    .collect(Collectors.groupingBy(Shift::getCourseLoadType));

            for (final CourseLoadType courseLoadType : shiftsByLoadType.keySet()) {
                if (registration.findEnrolledShiftFor(executionCourse, courseLoadType).isEmpty()) {
                    final List<Shift> shiftsOrderedByVacancies = shiftsByLoadType.get(courseLoadType).stream()
                            .sorted(vacanciesComparator).collect(Collectors.toList());

                    if (!enrolInOneShift(shiftsOrderedByVacancies, registration)) {
                        throw new DomainException("error.registration.enrolSchoolClass.shiftFull",
                                shiftsOrderedByVacancies.iterator().next().getName(), courseLoadType.getName().getContent(),
                                executionCourse.getName());
                    }
                }
            }
        }
    }

    private static boolean enrolInOneShift(Collection<Shift> shiftsOrderedByVacancies, Registration registration) {
        for (final Shift shift : shiftsOrderedByVacancies) {
            if (shift.getStudentsSet().contains(registration) || shift.enrol(registration)) {
                return true;
            }
        }
        return false;
    }

    private void unenrolInSchoolClassAndShifts(final Registration registration) {
        getAssociatedShiftsSet().forEach(s -> s.unenrol(registration));
        registration.getSchoolClassesSet().remove(this);
    }

    public static Stream<SchoolClass> findBy(final ExecutionDegree executionDegree, final ExecutionInterval interval,
            final Integer curricularYear) {
        return executionDegree.getSchoolClassesSet().stream().filter(sc -> sc.getCurricularYear().equals(curricularYear))
                .filter(sc -> sc.getExecutionPeriod() == interval);
    }

    @Deprecated
    public String getNome() {
        return super.getName();
    }

    @Deprecated
    public void setNome(String name) {
        setName(name);
    }

    @Deprecated
    public Object getEditablePartOfName() {
        return super.getName();
    }

    @Deprecated
    public Integer getAnoCurricular() {
        return getCurricularYear();
    }

    @Deprecated
    public void setAnoCurricular(Integer anoCurricular) {
        setCurricularYear(anoCurricular);
    }

}
