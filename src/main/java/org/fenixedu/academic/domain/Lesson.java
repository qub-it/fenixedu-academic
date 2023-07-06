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
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.space.LessonInstanceSpaceOccupation;
import org.fenixedu.academic.domain.space.LessonSpaceOccupation;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.spaces.domain.Space;
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

    private YearMonthDay getLessonStartDay() {
        if (!wasFinished()) {
            YearMonthDay periodBegin = getPeriod().getStartYearMonthDay();
            return getValidBeginDate(periodBegin);
        }
        return null;
    }

    private YearMonthDay getLessonEndDay() {
        if (!wasFinished()) {
            YearMonthDay periodEnd = getPeriod().getLastOccupationPeriodOfNestedPeriods().getEndYearMonthDay();
            return getValidEndDate(periodEnd);
        }
        return null;
    }

    private YearMonthDay getValidBeginDate(YearMonthDay startDate) {
        final YearMonthDay periodEndDate = getPeriod() != null ? getPeriod().getEndYearMonthDayWithNextPeriods() : null;
        if (periodEndDate != null) {

            YearMonthDay lessonBegin = startDate.toDateTimeAtMidnight()
                    .withDayOfWeek(getDiaSemana().getDiaSemanaInDayOfWeekJodaFormat()).toYearMonthDay();
            if (lessonBegin.isBefore(startDate)) {
                lessonBegin = lessonBegin.plusDays(NUMBER_OF_DAYS_IN_WEEK);
            }

            while (!isDayValid(lessonBegin)) {
                if (!lessonBegin.isAfter(periodEndDate)) {
                    lessonBegin = lessonBegin.plusDays(NUMBER_OF_DAYS_IN_WEEK);
                } else {
                    return null;
                }
            }

            return lessonBegin;
        }

        return null;
    }

    private YearMonthDay getValidEndDate(YearMonthDay endDate) {
        YearMonthDay lessonEnd =
                endDate.toDateTimeAtMidnight().withDayOfWeek(getDiaSemana().getDiaSemanaInDayOfWeekJodaFormat()).toYearMonthDay();
        if (lessonEnd.isAfter(endDate)) {
            lessonEnd = lessonEnd.minusDays(NUMBER_OF_DAYS_IN_WEEK);
        }
        return lessonEnd;
    }

    public SortedSet<YearMonthDay> getAllLessonDatesWithoutInstanceDates() {
        SortedSet<YearMonthDay> dates = new TreeSet<YearMonthDay>();
        if (!wasFinished()) {
            dates.addAll(getAllValidLessonDatesWithoutInstancesDates());
        }
        return dates;
    }

    public SortedSet<YearMonthDay> getAllLessonDates() {
        SortedSet<YearMonthDay> dates = getAllLessonInstanceDates();
        if (!wasFinished()) {
            dates.addAll(getAllValidLessonDatesWithoutInstancesDates());
        }
        return dates;
    }

    private SortedSet<YearMonthDay> getAllLessonInstanceDates() {
        return getLessonInstancesSet().stream().map(li -> li.getDay())
                .collect(Collectors.toCollection(() -> new TreeSet<YearMonthDay>()));
    }

    private SortedSet<YearMonthDay> getAllValidLessonDatesWithoutInstancesDates() {

        YearMonthDay startDateToSearch = getLessonStartDay();
        YearMonthDay endDateToSearch = getLessonEndDay();

        SortedSet<YearMonthDay> result = new TreeSet<YearMonthDay>();
        startDateToSearch = startDateToSearch != null ? getValidBeginDate(startDateToSearch) : null;

        if (!wasFinished() && startDateToSearch != null && endDateToSearch != null
                && !startDateToSearch.isAfter(endDateToSearch)) {
            final int dayIncrement = getFrequency() == FrequencyType.BIWEEKLY ? FrequencyType.WEEKLY
                    .getNumberOfDays() : getFrequency().getNumberOfDays();
            boolean shouldAdd = true;
            while (true) {
                if (isDayValid(startDateToSearch)) {
                    if (getFrequency() != FrequencyType.BIWEEKLY || shouldAdd) {
                        if (!Holiday.isHoliday(startDateToSearch.toLocalDate())) {
                            result.add(startDateToSearch);
                        }
                    }
                    shouldAdd = !shouldAdd;
                }
                startDateToSearch = startDateToSearch.plusDays(dayIncrement);
                if (startDateToSearch.isAfter(endDateToSearch)) {
                    break;
                }
            }
        }

        result.removeAll(getAllLessonInstanceDates());

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

    public String prettyPrint() {
        final StringBuilder result = new StringBuilder();
        result.append(getWeekDay().getLabelShort()).append(" (");
        result.append(getBeginHourMinuteSecond().toString("HH:mm")).append("-");
        result.append(getEndHourMinuteSecond().toString("HH:mm")).append(") ");
        result.append(getSpaces().map(Space::getName).collect(Collectors.joining(", ")));
        return result.toString();
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
