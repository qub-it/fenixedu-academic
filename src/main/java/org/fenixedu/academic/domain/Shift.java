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
import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadDuration;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.schedule.shiftCapacity.ShiftCapacity;
import org.fenixedu.academic.domain.schedule.shiftCapacity.ShiftCapacityType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.util.i18n.Languages;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class Shift extends Shift_Base {

    private static final Logger LOG = LoggerFactory.getLogger(Shift.class);

    public static final Comparator<Shift> SHIFT_COMPARATOR_BY_NAME =
            (s1, s2) -> Collator.getInstance().compare(s1.getName(), s2.getName());

    static {
        Registration.getRelationShiftStudent().addListener(new ShiftStudentListener());
    }

    public Shift(final ExecutionCourse executionCourse, final CourseLoadType type, final Integer capacity, final String name) {
        super();
        setRootDomainObject(Bennu.getInstance());

        setExecutionCourse(executionCourse);
        setCourseLoadType(type);
        shiftTypeManagement(List.of(type.getShiftType()), executionCourse); // TEMP, until full refactor is concluded

        new ShiftCapacity(this, ShiftCapacityType.findOrCreateDefault(), capacity);

        if (StringUtils.isNotBlank(name)
                && executionCourse.getShiftsSet().stream().anyMatch(s -> s != this && name.equals(s.getName()))) {
            throw new DomainException("error.Shift.with.this.name.already.exists");
        }

        setName(StringUtils.isBlank(name) ? generateShiftName(executionCourse, type) : name);
    }

    private static String generateShiftName(final ExecutionCourse executionCourse, final CourseLoadType type) {
        final Set<String> existingShiftsNames =
                executionCourse.getShiftsSet().stream().map(Shift::getName).collect(Collectors.toSet());

        final String courseSigla = executionCourse.getSigla();
        final String typeInitials = type.getInitials().getContent();

        final Function<Integer, String> nameGenerator = n -> courseSigla + typeInitials + String.format("%02d", n);

        final AtomicInteger counter = new AtomicInteger();

        String generatedName = nameGenerator.apply(counter.incrementAndGet());
        while (existingShiftsNames.contains(generatedName)) {
            generatedName = nameGenerator.apply(counter.incrementAndGet());
        }
        return generatedName;
    }

    public void edit(final CourseLoadType type, final String name, final Languages languages, final String comment) {

        if (StringUtils.isBlank(name)) {
            throw new DomainException("error.Shift.name.is.empty");
        }

        if (getExecutionCourse().getShiftsSet().stream().anyMatch(s -> s != this && name.equals(s.getName()))) {
            throw new DomainException("error.Shift.with.this.name.already.exists");
        }

        setCourseLoadType(type);
        shiftTypeManagement(List.of(type.getShiftType()), getExecutionCourse()); // TEMP, until full refactor is concluded
//        if (getCourseLoadsSet().isEmpty()) {
//            throw new DomainException("error.Shift.empty.courseLoads");
//        }

        setName(name);
        setLanguages(languages);
        setComment(comment);
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        for (; !getAssociatedLessonsSet().isEmpty(); getAssociatedLessonsSet().iterator().next().delete()) {
            ;
        }
        for (; !getAssociatedShiftProfessorshipSet().isEmpty(); getAssociatedShiftProfessorshipSet().iterator().next().delete()) {
            ;
        }

        getShiftCapacitiesSet().forEach(sc -> sc.delete());

        getAssociatedClassesSet().clear();
        setCourseLoadType(null);
        setExecutionCourse(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public ExecutionInterval getExecutionPeriod() {
        return getExecutionCourse().getExecutionInterval();
    }

    private void shiftTypeManagement(Collection<ShiftType> types, ExecutionCourse executionCourse) {

        if (types.isEmpty()) {
            throw new DomainException("error.Shift.empty.shiftTypes");
        }

        if (types.size() > 1) {
            throw new DomainException("error.Shift.multiple.shiftTypes");
        }
    }

    @Deprecated
    public List<ShiftType> getTypes() {
        return List.of(getCourseLoadType().getShiftType());
    }

    @Deprecated
    public boolean containsType(ShiftType shiftType) {
        return getCourseLoadType().getShiftType() == shiftType;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getStudentsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.RESOURCE_ALLOCATION, "error.deleteShift.with.students", getName()));
        }
        if (!getAssociatedSummariesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.RESOURCE_ALLOCATION, "error.deleteShift.with.summaries", getName()));
        }
    }

    public BigDecimal getTotalHours() {
        Collection<Lesson> lessons = getAssociatedLessonsSet();
        BigDecimal lessonTotalHours = BigDecimal.ZERO;
        for (Lesson lesson : lessons) {
            lessonTotalHours = lessonTotalHours.add(lesson.getTotalHours());
        }
        return lessonTotalHours;
    }

    public Duration getTotalDuration() {
        Duration duration = Duration.ZERO;
        Collection<Lesson> lessons = getAssociatedLessonsSet();
        for (Lesson lesson : lessons) {
            duration = duration.plus(lesson.getTotalDuration());
        }
        return duration;
    }

    public BigDecimal getMaxLessonDuration() {
        BigDecimal maxHours = BigDecimal.ZERO;
        for (Lesson lesson : getAssociatedLessonsSet()) {
            BigDecimal lessonHours = lesson.getUnitHours();
            if (maxHours.compareTo(lessonHours) == -1) {
                maxHours = lessonHours;
            }
        }
        return maxHours;
    }

    public BigDecimal getUnitHours() {
        BigDecimal hours = BigDecimal.ZERO;
        Collection<Lesson> lessons = getAssociatedLessonsSet();
        for (Lesson lesson : lessons) {
            hours = hours.add(lesson.getUnitHours());
        }
        return hours;
    }

    public BigDecimal getCourseLoadTotalHours() {
        return getExecutionCourse().getCompetenceCoursesInformations().stream().distinct()
                .flatMap(cci -> cci.findLoadDurationByType(getCourseLoadType()).stream()).filter(Objects::nonNull)
                .map(CourseLoadDuration::getHours).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    public SortedSet<Lesson> getLessonsOrderedByWeekDayAndStartTime() {
        final SortedSet<Lesson> lessons = new TreeSet<Lesson>(Lesson.LESSON_COMPARATOR_BY_WEEKDAY_AND_STARTTIME);
        lessons.addAll(getAssociatedLessonsSet());
        return lessons;
    }

    public boolean isFreeFor(final Registration registration) {
        return getShiftCapacitiesSet().stream().filter(ShiftCapacity::isFreeIncludingExtraCapacities)
                .anyMatch(sc -> sc.accepts(registration));
    }

    public static Stream<ShiftCapacity> findPossibleShiftsToEnrol(final Registration registration,
            final ExecutionCourse executionCourse, final CourseLoadType courseLoadType) {
        return executionCourse.getShiftsSet().stream().filter(s -> s.getCourseLoadType() == courseLoadType)
                .flatMap(s -> s.getShiftCapacitiesSet().stream()).filter(ShiftCapacity::isFreeIncludingExtraCapacities)
                .filter(sc -> sc.accepts(registration));
    }

    /**
     * Enrolls provided registration in this shift and its first suitable capacity, and unenroll it from other shifts of same
     * type and course
     * 
     * @param registration registration to enroll in this shift
     * @return <code>true</code> if registration enrolled successfully in this shift
     */
    public boolean enrol(final Registration registration) {
        final List<ShiftCapacity> sortedCapacities = getShiftCapacitiesSet().stream()
                .sorted(ShiftCapacity.TYPE_EVALUATION_PRIORITY_COMPARATOR).collect(Collectors.toUnmodifiableList());

        for (final ShiftCapacity shiftCapacity : sortedCapacities) {
            if (shiftCapacity.accepts(registration)) {
                if (shiftCapacity.isFreeIncludingExtraCapacities()) {
                    return doEnrol(registration, shiftCapacity);
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    public static boolean enrol(final Registration registration, final ShiftCapacity shiftCapacity) {
        if (shiftCapacity != null && shiftCapacity.isFreeIncludingExtraCapacities() && shiftCapacity.accepts(registration)) {
            return doEnrol(registration, shiftCapacity);
        }
        return false;
    }

    private static boolean doEnrol(final Registration registration, final ShiftCapacity shiftCapacityParam) {

        // if shiftCapacity isn't free, check if is there an extra capacity configured
        final ShiftCapacity shiftCapacity = shiftCapacityParam.isFree() ? shiftCapacityParam : shiftCapacityParam
                .getExtraCapacitiesSet().stream().filter(sc -> sc.isFree())
                .sorted(ShiftCapacity.TYPE_EVALUATION_PRIORITY_COMPARATOR).findFirst().orElse(null);
        if (shiftCapacity == null) {
            return false;
        }

        final Shift shift = shiftCapacity.getShift();

        final ExecutionCourse executionCourse = shift.getExecutionCourse();

        // remove registration from shifts of the same type
        registration.findEnrolledShiftFor(executionCourse, shift.getCourseLoadType()).ifPresent(s -> s.unenrol(registration));

        if (ShiftEnrolment.find(shift, registration).isPresent()) {
            throw new DomainException("error.Shift.enrolment.alreadyEnrolled"); // this should never happen
        }

        new ShiftEnrolment(shiftCapacity, registration);

        shift.addStudents(registration); // to deprecate

        GroupsAndShiftsManagementLog.createLog(executionCourse, Bundle.MESSAGING,
                "log.executionCourse.groupAndShifts.shifts.attends.added", registration.getNumber().toString(), shift.getName(),
                executionCourse.getName(), executionCourse.getDegreePresentationString());

        LOG.info(
                "SHIFT ENROLMENT: student-{} degree-{} shift-{} course-{} shiftCapacity-{}"
                        + (shiftCapacityParam != shiftCapacity ? " originalShiftCapacity-{}" : ""),
                registration.getStudent().getNumber(), registration.getDegree().getCode(), shift.getName(),
                executionCourse.getCode(), shiftCapacity.getType().getName().getContent(),
                shiftCapacityParam.getType().getName().getContent());
        return true;
    }

    public void unenrol(final Registration registration) {

        ShiftEnrolment.find(this, registration).ifPresent(se -> se.delete());

        removeStudents(registration); // to deprecate

        final ExecutionCourse executionCourse = getExecutionCourse();
        GroupsAndShiftsManagementLog.createLog(getExecutionCourse(), Bundle.MESSAGING,
                "log.executionCourse.groupAndShifts.shifts.attends.removed", registration.getNumber().toString(), getName(),
                executionCourse.getName(), executionCourse.getDegreePresentationString());

        LOG.info("SHIFT UNENROLMENT: student-{} degree-{} shift-{} course-{}", registration.getStudent().getNumber(),
                registration.getDegree().getCode(), getName(), executionCourse.getCode());
    }

    public String getClassesPrettyPrint() {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (SchoolClass schoolClass : getAssociatedClassesSet()) {
            builder.append(schoolClass.getName());
            index++;
            if (index < getAssociatedClassesSet().size()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    @Deprecated
    public String getShiftTypesPrettyPrint() {
        return getCourseLoadType().getName().getContent();
    }

    @Deprecated
    public String getShiftTypesCodePrettyPrint() {
        return getCourseLoadType().getInitials().getContent();
    }

    private static class ShiftStudentListener extends RelationAdapter<Registration, Shift> {

        @Override
        public void afterAdd(Registration registration, Shift shift) {
            ShiftEnrolment.find(shift, registration).orElseGet(() -> new ShiftEnrolment(shift, registration));
        }

        @Override
        public void afterRemove(Registration registration, Shift shift) {
            ShiftEnrolment.find(shift, registration).ifPresent(se -> se.delete());
        }
    }

    public String getPresentationName() {
        final Set<Lesson> lessons = getAssociatedLessonsSet();
        final String lessonsPresentation = lessons.isEmpty() ? "" : lessons.stream().filter(l -> !l.getExtraLesson())
                .sorted(Lesson.LESSON_COMPARATOR_BY_WEEKDAY_AND_STARTTIME).map(Lesson::getPresentationName)
                .collect(Collectors.joining("; ", " (", ")"));
        return this.getName() + lessonsPresentation;
    }

    @Deprecated
    public String getLessonPresentationString() {
        return getPresentationName();
    }

    public Integer getVacancies() {
//        return getLotacao() - getStudentsSet().size();
        return ShiftCapacity.getTotalCapacity(this) - ShiftEnrolment.getTotalEnrolments(this);
    }

    public Integer getTotalCapacity() {
        return ShiftCapacity.getTotalCapacity(this);
    }

    @Deprecated
    public Integer getLotacao() {
        return ShiftCapacity.getTotalCapacity(this);
    }

    @Deprecated
    public String getNome() {
        return getName();
    }

}
