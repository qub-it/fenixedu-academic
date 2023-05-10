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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.fenixedu.academic.util.WeekDay;
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

    public static final Comparator<Shift> SHIFT_COMPARATOR_BY_TYPE_AND_ORDERED_LESSONS = new Comparator<Shift>() {

        @Override
        public int compare(Shift o1, Shift o2) {
            final int ce = o1.getExecutionCourse().getName().compareTo(o2.getExecutionCourse().getName());
            if (ce != 0) {
                return ce;
            }
            final int cs = o1.getShiftTypesIntegerComparator().compareTo(o2.getShiftTypesIntegerComparator());
            if (cs != 0) {
                return cs;
            }
            final int cl = o1.getLessonsStringComparator().compareTo(o2.getLessonsStringComparator());
            return cl == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : cl;
        }

    };

    static {
        Registration.getRelationShiftStudent().addListener(new ShiftStudentListener());
    }

    @Deprecated
    public Shift(final ExecutionCourse executionCourse, Collection<ShiftType> types, final Integer capacity,
            final String shiftName) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setExecutionCourse(executionCourse);
        shiftTypeManagement(types, executionCourse);
        new ShiftCapacity(this, ShiftCapacityType.findOrCreateDefault(), capacity);
        setName(shiftName);

        if (StringUtils.isBlank(shiftName)) {
            executionCourse.setShiftNames();
        }

        if (getCourseLoadsSet().isEmpty()) {
            throw new DomainException("error.Shift.empty.courseLoads");
        }
    }

    public Shift(final ExecutionCourse executionCourse, final CourseLoadType type, final Integer capacity, final String name) {
        super();
        setRootDomainObject(Bennu.getInstance());

        setExecutionCourse(executionCourse);
        setCourseLoadType(type);
        shiftTypeManagement(List.of(type.getShiftType()), executionCourse); // TEMP, until full refactor is concluded

        new ShiftCapacity(this, ShiftCapacityType.findOrCreateDefault(), capacity);

        if (StringUtils.isNotBlank(name)
                && executionCourse.getAssociatedShifts().stream().anyMatch(s -> s != this && s.getName().equals(name))) {
            throw new DomainException("error.Shift.with.this.name.already.exists");
        }
        setName(name);

        if (StringUtils.isBlank(name)) {
            executionCourse.setShiftNames();
        }
    }

    @Deprecated
    public void edit(List<ShiftType> newTypes, ExecutionCourse newExecutionCourse, String newName, String comment) {

        ExecutionCourse beforeExecutionCourse = getExecutionCourse();

        if (newExecutionCourse.getAssociatedShifts().stream().anyMatch(s -> s.getName().equals(newName) && s != this)) {
            throw new DomainException("error.Shift.with.this.name.already.exists");
        }

        shiftTypeManagement(newTypes, newExecutionCourse);
        setName(newName);

        beforeExecutionCourse.setShiftNames();
        if (!beforeExecutionCourse.equals(newExecutionCourse)) {
            newExecutionCourse.setShiftNames();
        }

        if (getCourseLoadsSet().isEmpty()) {
            throw new DomainException("error.Shift.empty.courseLoads");
        }

        setExecutionCourse(newExecutionCourse);
        setComment(comment);
    }

    public void edit(final CourseLoadType type, final String name, final Languages languages, final String comment) {

        if (StringUtils.isBlank(name)) {
            throw new DomainException("error.Shift.name.is.empty");
        }

        if (getExecutionCourse().getAssociatedShifts().stream().anyMatch(s -> s != this && s.getName().equals(name))) {
            throw new DomainException("error.Shift.with.this.name.already.exists");
        }

        setCourseLoadType(type);
        shiftTypeManagement(List.of(type.getShiftType()), getExecutionCourse()); // TEMP, until full refactor is concluded
        if (getCourseLoadsSet().isEmpty()) {
            throw new DomainException("error.Shift.empty.courseLoads");
        }

        setName(name);
        setLanguages(languages);
        setComment(comment);
    }

    public boolean isCustomName() {
        final String cleanSigla = getExecutionCourse().getSigla().replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")
                .replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
        return StringUtils.isNotBlank(getName()) && !getName().matches(cleanSigla + "[a-zA-Z]+[0-9]+");
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        final ExecutionCourse executionCourse = getExecutionCourse();

        for (; !getAssociatedLessonsSet().isEmpty(); getAssociatedLessonsSet().iterator().next().delete()) {
            ;
        }
        for (; !getAssociatedShiftProfessorshipSet().isEmpty(); getAssociatedShiftProfessorshipSet().iterator().next().delete()) {
            ;
        }

        getShiftCapacitiesSet().forEach(sc -> sc.delete());

        getAssociatedClassesSet().clear();
        getCourseLoadsSet().clear();
        setCourseLoadType(null);
        setExecutionCourse(null);
        setRootDomainObject(null);
        super.deleteDomainObject();

        executionCourse.setShiftNames();
    }

    @Override
    public ExecutionCourse getExecutionCourse() {
        final ExecutionCourse executionCourse = super.getExecutionCourse();
        if (executionCourse != null) {
            return executionCourse;
        }

        CourseLoad courseLoad = getCourseLoadsSet().iterator().next();
        if (courseLoad != null) {
            return courseLoad.getExecutionCourse();
        } else {
            return null;
        }
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

        CourseLoadType.findByShiftType(types.iterator().next()).ifPresent(loadType -> setCourseLoadType(loadType));

        if (executionCourse != null) {
            getCourseLoadsSet().clear();
            for (ShiftType shiftType : types) {
                CourseLoad courseLoad = executionCourse.getCourseLoadByShiftType(shiftType);
                if (courseLoad != null) {
                    addCourseLoads(courseLoad);
                }
            }
        }
    }

    public List<ShiftType> getTypes() {
        List<ShiftType> result = new ArrayList<ShiftType>();
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            result.add(courseLoad.getType());
        }
        return result;
    }

    public SortedSet<ShiftType> getSortedTypes() {
        SortedSet<ShiftType> result = new TreeSet<ShiftType>();
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            result.add(courseLoad.getType());
        }
        return result;
    }

    public boolean containsType(ShiftType shiftType) {
        if (shiftType != null) {
            for (CourseLoad courseLoad : getCourseLoadsSet()) {
                if (courseLoad.getType().equals(shiftType)) {
                    return true;
                }
            }
        }
        return false;
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

    public int getNumberOfLessonInstances() {
        Collection<Lesson> lessons = getAssociatedLessonsSet();
        int totalLessonsDates = 0;
        for (Lesson lesson : lessons) {
            totalLessonsDates += lesson.getFinalNumberOfLessonInstances();
        }
        return totalLessonsDates;
    }

    public BigDecimal getCourseLoadWeeklyAverage() {
        BigDecimal weeklyHours = BigDecimal.ZERO;
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            weeklyHours = weeklyHours.add(courseLoad.getWeeklyHours());
        }
        return weeklyHours;
    }

    @Deprecated(forRemoval = true)
    public BigDecimal getCourseLoadTotalHoursOld() {
        BigDecimal weeklyHours = BigDecimal.ZERO;
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            weeklyHours = weeklyHours.add(courseLoad.getTotalQuantity());
        }
        return weeklyHours;
    }

    public BigDecimal getCourseLoadTotalHours() {
        return getExecutionCourse().getCompetenceCoursesInformations().stream()
                .flatMap(cci -> cci.findLoadDurationByType(getCourseLoadType()).stream()).filter(Objects::nonNull)
                .map(CourseLoadDuration::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public SortedSet<Lesson> getLessonsOrderedByWeekDayAndStartTime() {
        final SortedSet<Lesson> lessons = new TreeSet<Lesson>(Lesson.LESSON_COMPARATOR_BY_WEEKDAY_AND_STARTTIME);
        lessons.addAll(getAssociatedLessonsSet());
        return lessons;
    }

    public String getLessonsStringComparator() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final Lesson lesson : getLessonsOrderedByWeekDayAndStartTime()) {
            stringBuilder.append(lesson.getDiaSemana().getDiaSemana().toString());
            stringBuilder.append(lesson.getBeginHourMinuteSecond().toString());
        }
        return stringBuilder.toString();
    }

    public Integer getShiftTypesIntegerComparator() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (ShiftType shiftType : getSortedTypes()) {
            stringBuilder.append(shiftType.ordinal() + 1);
        }
        return Integer.valueOf(stringBuilder.toString());
    }

    public boolean isFreeFor(final Registration registration) {
        return getShiftCapacitiesSet().stream().filter(ShiftCapacity::isFreeIncludingExtraCapacities)
                .anyMatch(sc -> sc.accepts(registration));
    }

    public static Stream<ShiftCapacity> findPossibleShiftsToEnrol(final Registration registration,
            final ExecutionCourse executionCourse, final ShiftType shiftType) {
        return executionCourse.getAssociatedShifts().stream().filter(s -> s.containsType(shiftType))
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
        shift.getTypes().stream().map(st -> registration.getShiftFor(executionCourse, st)).filter(Objects::nonNull)
                .forEach(s -> s.unenrol(registration));

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

    public String getShiftTypesPrettyPrint() {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        SortedSet<ShiftType> sortedTypes = getSortedTypes();
        for (ShiftType shiftType : sortedTypes) {
            builder.append(BundleUtil.getString(Bundle.ENUMERATION, shiftType.getName()));
            index++;
            if (index < sortedTypes.size()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public String getShiftTypesCapitalizedPrettyPrint() {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        SortedSet<ShiftType> sortedTypes = getSortedTypes();
        for (ShiftType shiftType : sortedTypes) {
            builder.append(shiftType.getFullNameTipoAula());
            index++;
            if (index < sortedTypes.size()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public String getShiftTypesCodePrettyPrint() {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        SortedSet<ShiftType> sortedTypes = getSortedTypes();
        for (ShiftType shiftType : sortedTypes) {
            builder.append(shiftType.getSiglaTipoAula());
            index++;
            if (index < sortedTypes.size()) {
                builder.append(", ");
            }
        }
        return builder.toString();
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

    public int getCapacityBasedOnSmallestRoom() {
        int capacity = getAssociatedLessonsSet().stream().filter(Lesson::hasSala)
                .mapToInt(lesson -> lesson.getSala().getAllocatableCapacity()).min().orElse(0);
        return capacity + (capacity / 10);
    }

    public boolean hasShiftType(final ShiftType shiftType) {
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            if (courseLoad.getType() == shiftType) {
                return true;
            }
        }
        return false;
    }

    public String getPresentationName() {
        StringBuilder stringBuilder = new StringBuilder(this.getName());
        if (!this.getAssociatedLessonsSet().isEmpty()) {
            stringBuilder.append(" ( ");

            for (Iterator<Lesson> iterator = this.getAssociatedLessonsSet().iterator(); iterator.hasNext();) {
                Lesson lesson = iterator.next();
                stringBuilder.append(WeekDay.getWeekDay(lesson.getDiaSemana()).getLabelShort());
                stringBuilder.append(" ");
                stringBuilder.append(lesson.getBeginHourMinuteSecond().toString("HH:mm"));
                stringBuilder.append(" - ");
                stringBuilder.append(lesson.getEndHourMinuteSecond().toString("HH:mm"));
                if (lesson.hasSala()) {
                    stringBuilder.append(" - ");
                    stringBuilder.append(lesson.getSala().getName());
                }
                if (iterator.hasNext()) {
                    stringBuilder.append(" ; ");
                }
            }
            stringBuilder.append(" ) ");
        }
        return stringBuilder.toString();
    }

    public String getLessonPresentationString() {
        StringBuilder stringBuilder = new StringBuilder(this.getName());
        if (!this.getAssociatedLessonsSet().isEmpty()) {
            for (Iterator<Lesson> iterator = this.getAssociatedLessonsSet().iterator(); iterator.hasNext();) {
                Lesson lesson = iterator.next();
                stringBuilder.append(" ");
                stringBuilder.append(WeekDay.getWeekDay(lesson.getDiaSemana()).getLabelShort());
                stringBuilder.append(" ");
                stringBuilder.append(lesson.getBeginHourMinuteSecond().toString("HH:mm"));
                stringBuilder.append(" - ");
                stringBuilder.append(lesson.getEndHourMinuteSecond().toString("HH:mm"));
                if (lesson.hasSala()) {
                    stringBuilder.append(" - ");
                    stringBuilder.append(lesson.getSala().getName());
                }
                if (iterator.hasNext()) {
                    stringBuilder.append(" ; ");
                }
            }
        }
        return stringBuilder.toString();
    }

    public Integer getVacancies() {
//        return getLotacao() - getStudentsSet().size();
        return ShiftCapacity.getTotalCapacity(this) - ShiftEnrolment.getTotalEnrolments(this);
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
