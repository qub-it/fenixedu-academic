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
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.space.LessonInstanceSpaceOccupation;
import org.fenixedu.academic.domain.space.LessonSpaceOccupation;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Minutes;
import org.joda.time.YearMonthDay;

public class Lesson extends Lesson_Base {

    public static int NUMBER_OF_MINUTES_IN_HOUR = 60;
    public static int NUMBER_OF_DAYS_IN_WEEK = 7;

    public static final Comparator<Lesson> LESSON_COMPARATOR_BY_WEEKDAY_AND_STARTTIME =
            Comparator.comparing(Lesson::getWeekDay).thenComparing(Lesson::getBeginHourMinuteSecond);

    public Lesson(DiaSemana diaSemana, Calendar inicio, Calendar fim, Shift shift, FrequencyType frequency,
            ExecutionInterval executionInterval, OccupationPeriod period, Space room) {
        super();

        setRootDomainObject(Bennu.getInstance());
        setWeekDay(diaSemana == null ? null : WeekDay.getWeekDay(diaSemana)); //setDiaSemana(diaSemana);
        setInicio(inicio);
        setFim(fim);
        setShift(shift);
        setFrequency(frequency);
        setPeriod(period);
        setInitialFullPeriod(period);

        if (room != null) {
            new LessonSpaceOccupation(room, this);
        }
    }

    public void edit(final Space newRoom) {
        lessonSpaceOccupationManagement(newRoom);
    }

    public void delete() {
        if (!getAssociatedSummaries().isEmpty()) {
            throw new DomainException("error.deleteLesson.with.summaries", prettyPrint());
        }

        final OccupationPeriod period = getPeriod();
        super.setPeriod(null);
        if (period != null) {
            period.delete();
        }
        setInitialFullPeriod(null);

        if (getLessonSpaceOccupation() != null) {
            getLessonSpaceOccupation().delete();
        }

        while (hasAnyLessonInstances()) {
            getLessonInstancesSet().iterator().next().delete();
        }

        super.setShift(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkRequiredParameters() {
        return getFrequency() != null && getDiaSemana() != null;
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkTimeInterval() {
        final HourMinuteSecond start = getBeginHourMinuteSecond();
        final HourMinuteSecond end = getEndHourMinuteSecond();
        return start != null && end != null && start.isBefore(end);
    }

    private void lessonSpaceOccupationManagement(Space newRoom) {
        LessonSpaceOccupation lessonSpaceOccupation = getLessonSpaceOccupation();
        if (newRoom != null) {
            if (!wasFinished()) {
                if (lessonSpaceOccupation == null) {
                    lessonSpaceOccupation = new LessonSpaceOccupation(newRoom, this);
                } else {
                    lessonSpaceOccupation.edit(newRoom);
                }
            }
        } else {
            if (lessonSpaceOccupation != null) {
                lessonSpaceOccupation.delete();
            }
        }
        for (final LessonInstance lessonInstance : getLessonInstancesSet()) {
            if (lessonInstance.getDay().isAfter(new LocalDate())) {
                if (newRoom == null) {
                    lessonInstance.setLessonInstanceSpaceOccupation(null);
                } else {
                    LessonInstanceSpaceOccupation.findOccupationForLessonAndSpace(this, newRoom).ifPresentOrElse(
                            o -> o.add(lessonInstance), () -> new LessonInstanceSpaceOccupation(newRoom, lessonInstance));
                }
            }
        }
    }

    @Override
    public void setShift(Shift shift) {
        if (shift == null) {
            throw new DomainException("error.Lesson.empty.shift");
        }
        super.setShift(shift);
    }

    @Override
    public void setFrequency(FrequencyType frequency) {
        if (frequency == null) {
            throw new DomainException("error.Lesson.empty.type");
        }
        super.setFrequency(frequency);
    }

    @Override
    public void setPeriod(OccupationPeriod period) {
        if (period == null) {
            throw new DomainException("error.Lesson.empty.period");
        }
        super.setPeriod(period);
    }

    public boolean wasFinished() {
        return getPeriod() == null;
    }

    public ExecutionCourse getExecutionCourse() {
        return getShift().getExecutionCourse();
    }

    public ExecutionInterval getExecutionPeriod() {
        return getShift().getExecutionPeriod();
    }

    @Deprecated
    public Space getSala() {
        if (getLessonSpaceOccupation() != null) {
            return getLessonSpaceOccupation().getSpace();
        } else if (hasAnyLessonInstances() && wasFinished()) {
            return getLessonInstancesSet().stream().max(LessonInstance.COMPARATOR_BY_BEGIN_DATE_TIME).map(LessonInstance::getRoom)
                    .orElse(null);
        }
        return null;
    }

    @Deprecated
    public boolean hasSala() {
        return getSala() != null;
    }

    public Stream<Space> getSpaces() {
        final LessonSpaceOccupation spaceOccupation = getLessonSpaceOccupation();
        if (spaceOccupation != null) {
            return spaceOccupation.getSpaces().stream();
        }

        return getLessonInstancesSet().stream().flatMap(LessonInstance::getSpaces).distinct();
    }

    public void createAllLessonInstances() {
        final SortedSet<YearMonthDay> dates = getAllLessonDatesWithoutInstanceDates();

        final OccupationPeriod period = getPeriod();
        super.setPeriod(null); // to avoid dates and space overlaps

        dates.forEach(date -> new LessonInstance(this, date));

        if (getLessonSpaceOccupation() != null) {
            getLessonSpaceOccupation().delete();
        }

        if (period != null) {
            period.delete();
        }
    }

    public void removeOccupationPeriod() {
        super.setPeriod(null);
    }

    public LessonSpaceOccupation getRoomOccupation() {
        return getLessonSpaceOccupation();
    }

    private int getUnitMinutes() {
        return Minutes.minutesBetween(getBeginHourMinuteSecond(), getEndHourMinuteSecond()).getMinutes();
    }

    public BigDecimal getTotalHours() {
        return getUnitHours().multiply(BigDecimal.valueOf(getAllLessonDates().size()));
    }

    public Duration getTotalDuration() {
        return Minutes.minutesBetween(getBeginHourMinuteSecond(), getEndHourMinuteSecond()).toStandardDuration();
    }

    public BigDecimal getUnitHours() {
        return BigDecimal.valueOf(getUnitMinutes()).divide(BigDecimal.valueOf(NUMBER_OF_MINUTES_IN_HOUR), 2,
                RoundingMode.HALF_UP);
    }

    public List<Summary> getAssociatedSummaries() {
        return getLessonInstancesSet().stream().map(LessonInstance::getSummary).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public boolean isTimeValidToInsertSummary(HourMinuteSecond timeToInsert, YearMonthDay summaryDate) {

        YearMonthDay currentDate = new YearMonthDay();
        if (timeToInsert == null || summaryDate == null || summaryDate.isAfter(currentDate)) {
            return false;
        }

        if (currentDate.isEqual(summaryDate)) {
            LessonInstance lessonInstance = getLessonInstanceFor(summaryDate);
            HourMinuteSecond lessonStartTime =
                    lessonInstance != null ? lessonInstance.getStartTime() : getBeginHourMinuteSecond();
            return !lessonStartTime.isAfter(timeToInsert);
        }

        return true;
    }

    @Deprecated
    public SortedSet<YearMonthDay> getAllLessonDatesWithoutInstanceDates() {
        final Map<LocalDate, LessonInstance> lessonInstancesMap = getLessonInstancesForDatesMap();
        final Set<LocalDate> deletedLessonDates = getDeletedLessonDates();
        return lessonInstancesMap.entrySet().stream()
                .filter(entry -> entry.getValue() == null && !deletedLessonDates.contains(entry.getKey())).map(e -> e.getKey())
                .map(ld -> new YearMonthDay(ld.getYear(), ld.getMonthOfYear(), ld.getDayOfMonth()))
                .collect(Collectors.toCollection(() -> new TreeSet<YearMonthDay>()));
    }

    @Deprecated
    public SortedSet<YearMonthDay> getAllLessonDates() {
        return getLessonDates().stream().map(ld -> new YearMonthDay(ld.getYear(), ld.getMonthOfYear(), ld.getDayOfMonth()))
                .collect(Collectors.toCollection(() -> new TreeSet<YearMonthDay>()));
    }
    
    // TODO set visibility to private after references in ScheduleServices are merged
    public Set<LocalDate> getLessonDatesForPeriod(final OccupationPeriod period) {
        final SortedSet<LocalDate> result = new TreeSet<LocalDate>();

        if (period != null) {
            final int weekDays = 7;
            final int dayIncrement = getFrequency().getNumberOfDays();

            final HourMinuteSecond beginTime = getBeginHourMinuteSecond();
            final HourMinuteSecond endTime = getEndHourMinuteSecond();

            DateTime dateToCheck =
                    period.getPeriodInterval().getStart().withTime(beginTime.getHour(), beginTime.getMinuteOfHour(), 0, 0);
            final int lessonDayOfWeek = getWeekDay().getDayOfWeek();
            if (lessonDayOfWeek < dateToCheck.getDayOfWeek()) {
                dateToCheck = dateToCheck.plusDays(weekDays);
            }
            dateToCheck = dateToCheck.withDayOfWeek(lessonDayOfWeek);

            final DateTime lastDate =
                    period.getIntervalWithNextPeriods().getEnd().withTime(endTime.getHour(), endTime.getMinuteOfHour(), 0, 0);

            while (dateToCheck.isBefore(lastDate)) {
                boolean dateValid = period.isDateInNestedPeriods(period, dateToCheck);
                if (dateValid && !Holiday.isHoliday(dateToCheck.toLocalDate())) {
                    result.add(dateToCheck.toLocalDate());
                }
                dateToCheck = dateToCheck.plusDays(!dateValid && dayIncrement > weekDays ? weekDays : dayIncrement); // if the frequency is greater than weekly, we want to check the next week again
            }
        }

        return result;
    }

    public Set<LocalDate> getLessonDates() {
        return getLessonInstancesForDatesMap().keySet();
    }

    public Set<LocalDate> getDeletedLessonDates() {
        final SortedSet<LocalDate> result = new TreeSet<LocalDate>();
        final OccupationPeriod period = Optional.ofNullable(getInitialFullPeriod()).orElse(getPeriod());
        result.addAll(getLessonDatesForPeriod(period));
        result.removeAll(getLessonDates());
        return result;
    }

    public Map<LocalDate, LessonInstance> getLessonInstancesForDatesMap() {
        final Map<LocalDate, LessonInstance> result = new TreeMap<>();
        getLessonDatesForPeriod(getPeriod()).forEach(date -> result.put(date, null));
        getLessonInstancesSet().forEach(li -> result.put(li.getBeginDateTime().toLocalDate(), li));
        return result;
    }

    private boolean isDayValid(YearMonthDay day) {
        return getPeriod().nestedOccupationPeriodsContainsDay(day);
    }

    public LessonInstance getLessonInstanceFor(YearMonthDay date) {
        Collection<LessonInstance> lessonInstances = getLessonInstancesSet();
        for (LessonInstance lessonInstance : lessonInstances) {
            if (lessonInstance.getDay().isEqual(date)) {
                return lessonInstance;
            }
        }
        return null;
    }

    public boolean contains(Interval interval) {
        return contains(interval, getAllLessonDates());
    }

    public boolean containsWithoutCheckInstanceDates(Interval interval) {
        return contains(interval, getAllLessonDatesWithoutInstanceDates());
    }

    private boolean contains(Interval interval, SortedSet<YearMonthDay> allLessonDates) {
        return allLessonDates.stream()
                .map(date -> new Interval(date.toDateTimeAtMidnight(), date.toDateTimeAtMidnight().plusDays(1)))
                .anyMatch(i -> i.overlaps(interval));
    }

    public String getPresentationName() {
        final StringBuilder result = new StringBuilder();
        result.append(getWeekDay().getLabelShort()).append(" ");
        result.append(getBeginHourMinuteSecond().toString("HH:mm")).append("-");
        result.append(getEndHourMinuteSecond().toString("HH:mm"));
        final String spaces = getSpaces().map(Space::getName).collect(Collectors.joining(", "));
        if (StringUtils.isNotBlank(spaces)) {
            result.append(" - ").append(spaces);
        }
        return result.toString();
    }

    @Deprecated
    public String prettyPrint() {
        return getPresentationName();
    }

    public Calendar getInicio() {
        if (this.getBegin() != null) {
            Calendar result = Calendar.getInstance();
            result.setTime(this.getBegin());
            return result;
        }
        return null;
    }

    public void setInicio(Calendar inicio) {
        if (inicio != null) {
            this.setBegin(inicio.getTime());
        } else {
            this.setBegin(null);
        }
    }

    public Calendar getFim() {
        if (this.getEnd() != null) {
            Calendar result = Calendar.getInstance();
            result.setTime(this.getEnd());
            return result;
        }
        return null;
    }

    public void setFim(Calendar fim) {
        if (fim != null) {
            this.setEnd(fim.getTime());
        } else {
            this.setEnd(null);
        }
    }

    @Deprecated
    public java.util.Date getBegin() {
        org.fenixedu.academic.util.HourMinuteSecond hms = getBeginHourMinuteSecond();
        return (hms == null) ? null : new java.util.Date(0, 0, 1, hms.getHour(), hms.getMinuteOfHour(), hms.getSecondOfMinute());
    }

    @Deprecated
    public void setBegin(java.util.Date date) {
        if (date == null) {
            setBeginHourMinuteSecond(null);
        } else {
            setBeginHourMinuteSecond(org.fenixedu.academic.util.HourMinuteSecond.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getEnd() {
        org.fenixedu.academic.util.HourMinuteSecond hms = getEndHourMinuteSecond();
        return (hms == null) ? null : new java.util.Date(0, 0, 1, hms.getHour(), hms.getMinuteOfHour(), hms.getSecondOfMinute());
    }

    @Deprecated
    public void setEnd(java.util.Date date) {
        if (date == null) {
            setEndHourMinuteSecond(null);
        } else {
            setEndHourMinuteSecond(org.fenixedu.academic.util.HourMinuteSecond.fromDateFields(date));
        }
    }

    @Deprecated
    public DiaSemana getDiaSemana() {
        return DiaSemana.fromWeekDay(getWeekDay());
    }

    public Set<Interval> getAllLessonIntervals() {
        return getAllLessonDates().stream()
                .map(day -> new Interval(day.toLocalDate().toDateTime(getBeginHourMinuteSecond().toLocalTime()),
                        day.toLocalDate().toDateTime(getEndHourMinuteSecond().toLocalTime())))
                .collect(Collectors.toSet());
    }

    private boolean hasAnyLessonInstances() {
        return !getLessonInstancesSet().isEmpty();
    }
}
