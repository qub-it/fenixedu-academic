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
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

public class ExecutionInterval extends ExecutionInterval_Base implements Comparable<ExecutionInterval> {

    public static final Comparator<ExecutionInterval> COMPARATOR_BY_BEGIN_DATE = new Comparator<ExecutionInterval>() {

        @Override
        public int compare(final ExecutionInterval o1, final ExecutionInterval o2) {
            return AcademicInterval.COMPARATOR_BY_BEGIN_DATE.compare(o1.getAcademicInterval(), o2.getAcademicInterval());
        }
    };

    protected ExecutionInterval() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setState(PeriodState.NOT_OPEN);
    }

    public ExecutionInterval(ExecutionYear executionYear, AcademicInterval academicInterval, String name) {
        this();
        setRootDomainObjectForExecutionPeriod(Bennu.getInstance());
        setExecutionYear(executionYear);
        setAcademicInterval(academicInterval);
        setAcademicCalendarEntry(academicInterval.getAcademicCalendarEntry());
        setBeginDateYearMonthDay(academicInterval.getBeginYearMonthDayWithoutChronology());
        setEndDateYearMonthDay(academicInterval.getEndYearMonthDayWithoutChronology());
        setName(name);
    }

    @Override
    public void setAcademicInterval(AcademicInterval academicInterval) {
        if (academicInterval == null) {
            throw new DomainException("error.executionInterval.empty.executionInterval");
        }
        super.setAcademicInterval(academicInterval);
    }

    @Override
    public void setState(PeriodState state) {
        if (state == null) {
            throw new DomainException("error.executionInterval.empty.state");
        }
        super.setState(state);
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkDateInterval() {
        return getBeginDateYearMonthDay() != null && getEndDateYearMonthDay() != null
                && getBeginDateYearMonthDay().isBefore(getEndDateYearMonthDay());
    }

//    abstract public String getQualifiedName();
    public String getQualifiedName() {
        final String localizedName = getAcademicInterval().getAcademicCalendarEntry().getTitle().getContent();
        final String name = StringUtils.isNotBlank(localizedName) ? localizedName : getName();

        return new StringBuilder().append(name).append(" ").append(this.getExecutionYear().getName()).toString();
    }

//    abstract public boolean isCurrent();
    public boolean isCurrent() {
        return getState().equals(PeriodState.CURRENT);
    }

    public boolean isActive() {
        return getState().equals(PeriodState.CURRENT) || getState().equals(PeriodState.OPEN);
    }

    public static ExecutionInterval getExecutionInterval(String qualifiedName) {
        for (ExecutionInterval interval : Bennu.getInstance().getExecutionIntervalsSet()) {
            if (interval.getQualifiedName().equals(qualifiedName)) {
                return interval;
            }
        }
        return null;
    }

    public static ExecutionInterval getExecutionInterval(AcademicInterval academicInterval) {
        return academicInterval.getAcademicCalendarEntry().getExecutionInterval();
    }

    public static ExecutionInterval getExecutionInterval(AcademicCalendarEntry entry) {
        return entry != null ? entry.getExecutionInterval() : null;
    }

    public boolean isAfter(ExecutionInterval input) {
        return this.compareTo(input) > 0;
    }

    public boolean isAfterOrEquals(ExecutionInterval input) {
        return this.compareTo(input) >= 0;
    }

    public boolean isBefore(ExecutionInterval input) {
        return this.compareTo(input) < 0;
    }

    public boolean isBeforeOrEquals(ExecutionInterval input) {
        return this.compareTo(input) <= 0;
    }

    @Override
    public int compareTo(ExecutionInterval anotherInterval) {
        if (anotherInterval == null) {
            return 1;
        }

        final Comparator<ExecutionInterval> comparatorByStart = (ei1, ei2) -> ei1.getAcademicInterval()
                .getStartDateTimeWithoutChronology().compareTo(ei2.getAcademicInterval().getStartDateTimeWithoutChronology());

        final Comparator<ExecutionInterval> comparatorByEnd = (ei1, ei2) -> ei1.getAcademicInterval()
                .getEndDateTimeWithoutChronology().compareTo(ei2.getAcademicInterval().getEndDateTimeWithoutChronology());

        return comparatorByStart.thenComparing(comparatorByEnd.reversed()).thenComparing(ExecutionInterval::getOid) // end comparator is reversed in order to year be first than 1st semester (as it is first than 2nd semester)
                .compare(this, anotherInterval);
    }

    @Deprecated
    public int compareExecutionInterval(final ExecutionInterval input) {
        int result = 0;
        if (input == null) {
            result = -1;
        }
        if (result == 0) {
            assertExecutionIntervalType(this.getClass(), input);
            result = COMPARATOR_BY_BEGIN_DATE.compare(this, input);
        }
        return result;
    }

    /**
     * Asserts that the objects being manipulated belong to the same type.
     * E.g: Avoids comparison of ExecutionYears with ExecutionSemesters
     */
    public static <T extends ExecutionInterval> T assertExecutionIntervalType(final Class<T> clazz,
            final ExecutionInterval input) {
        T result = null;
        if (input != null) {
            if (!clazz.isAssignableFrom(input.getClass())) {
                throw new DomainException("error.ExecutionInterval.unexpected", clazz.getSimpleName(),
                        input.getClass().getSimpleName());
            }
            result = (T) input;
        }
        return result;
    }

    @Deprecated
    public java.util.Date getBeginDate() {
        org.joda.time.YearMonthDay ymd = getBeginDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    public LocalDate getBeginLocalDate() {
        final YearMonthDay result = getBeginDateYearMonthDay();
        return result == null ? null : result.toLocalDate();
    }

    @Deprecated
    public void setBeginDate(java.util.Date date) {
        if (date == null) {
            setBeginDateYearMonthDay(null);
        } else {
            setBeginDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    @Deprecated
    public java.util.Date getEndDate() {
        org.joda.time.YearMonthDay ymd = getEndDateYearMonthDay();
        return (ymd == null) ? null : new java.util.Date(ymd.getYear() - 1900, ymd.getMonthOfYear() - 1, ymd.getDayOfMonth());
    }

    public LocalDate getEndLocalDate() {
        final YearMonthDay result = getEndDateYearMonthDay();
        return result == null ? null : result.toLocalDate();
    }

    @Deprecated
    public void setEndDate(java.util.Date date) {
        if (date == null) {
            setEndDateYearMonthDay(null);
        } else {
            setEndDateYearMonthDay(org.joda.time.YearMonthDay.fromDateFields(date));
        }
    }

    //public abstract <E extends ExecutionInterval> E convert(final Class<E> concreteExecution);
    public <E extends ExecutionInterval> E convert(final Class<E> input) {
        return ExecutionYear.class.equals(input) ? (E) getExecutionYear() : (E) this;
    }

//    public abstract ExecutionYear getExecutionYear();

//    public abstract Integer getChildOrder();
    public Integer getChildOrder() {
        return getAcademicInterval().getAcademicCalendarEntry().getCardinality();
    }

    public ExecutionInterval getNext() {
        return Optional.ofNullable(getAcademicCalendarEntry()).map(ace -> ace.getNextAcademicCalendarEntry())
                .map(nace -> nace.getExecutionInterval()).orElse(null);
    }

    public ExecutionInterval getPrevious() {
        return Optional.ofNullable(getAcademicCalendarEntry()).map(ace -> ace.getPreviousAcademicCalendarEntry())
                .map(pace -> pace.getExecutionInterval()).orElse(null);
    }

    public boolean isAggregator() {
        return this instanceof ExecutionYear;
    }

    /**
     * Returns active ExecutionInterval aggregators (ExecutionYear).
     * 
     * @return collection of ExecutionIntervals
     */
    public static Collection<ExecutionInterval> findActiveAggregators() {
        return Bennu.getInstance().getExecutionIntervalsSet().stream().filter(ei -> ei instanceof ExecutionYear)
                .filter(ExecutionInterval::isActive).collect(Collectors.toSet());
    }

    /**
     * Returns all ExecutionInterval aggregators (ExecutionYear).
     * 
     * @return collection of ExecutionIntervals
     */
    public static Collection<ExecutionInterval> findAllAggregators() {
        return Bennu.getInstance().getExecutionIntervalsSet().stream().filter(ei -> ei.isAggregator())
                .collect(Collectors.toSet());
    }

    /**
     * Returns active ExecutionInterval aggregators (ExecutionYear) for provided calendar.
     * If provided calendar is null, use default academic calendar
     * 
     * @param calendar the calendar to search in
     * @return collection of ExecutionIntervals
     */
    public static Collection<ExecutionInterval> findActiveAggregators(final AcademicCalendarRootEntry calendar) {
        final AcademicCalendarEntry calendarToCheck = calendar != null ? calendar : AcademicCalendarEntry.findDefaultCalendar();
        return Bennu.getInstance().getExecutionIntervalsSet().stream().filter(ei -> ei instanceof ExecutionYear)
                .filter(ExecutionInterval::isActive).filter(ei -> ei.getAcademicCalendar() == calendarToCheck)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all ExecutionInterval aggregators (ExecutionYear) for provided calendar.
     * If provided calendar is null, use default academic calendar
     * 
     * @param calendar the calendar to search in
     * @return collection of ExecutionIntervals
     */
    public static Collection<ExecutionInterval> findAllAggregators(final AcademicCalendarRootEntry calendar) {
        final AcademicCalendarEntry calendarToCheck = calendar != null ? calendar : AcademicCalendarEntry.findDefaultCalendar();
        return Bennu.getInstance().getExecutionIntervalsSet().stream().filter(ei -> ei.isAggregator())
                .filter(ei -> ei.getAcademicCalendar() == calendarToCheck).collect(Collectors.toSet());
    }

    /**
     * Returns current ExecutionInterval aggregator (ExecutionYear) for provided calendar.
     * If provided calendar is null, use default academic calendar
     * 
     * @param calendar the calendar to search in
     * @return the current ExecutionInterval aggregator
     */
    public static ExecutionInterval findCurrentAggregator(final AcademicCalendarRootEntry calendar) {
        final AcademicCalendarEntry calendarToCheck = calendar != null ? calendar : AcademicCalendarEntry.findDefaultCalendar();
        return ExecutionYear.findCurrents().stream().filter(ey -> ey.getAcademicCalendar() == calendarToCheck).findFirst()
                .orElse(null);
    }

    public static Collection<ExecutionInterval> findActiveChilds() {
        return findAllChilds().stream().filter(ExecutionInterval::isActive).collect(Collectors.toSet());
    }

    /**
     * Returns active ExecutionInterval childs for provided calendar.
     * If provided calendar is null, use default academic calendar
     * 
     * @param calendar the calendar to search in
     * @return collection of ExecutionIntervals
     */
    public static Collection<ExecutionInterval> findActiveChilds(final AcademicCalendarRootEntry calendar) {
        final AcademicCalendarEntry calendarToCheck = calendar != null ? calendar : AcademicCalendarEntry.findDefaultCalendar();
        return findAllChilds().stream().filter(ExecutionInterval::isActive)
                .filter(ei -> ei.getAcademicCalendar() == calendarToCheck).collect(Collectors.toSet());
    }

    public static Collection<ExecutionInterval> findAllChilds() {
        return Bennu.getInstance().getExecutionIntervalsSet().stream().filter(ei -> !(ei instanceof ExecutionYear))
                .collect(Collectors.toSet());
    }

    /**
     * Returns current ExecutionInterval for provided AcademicPeriod type and calendar.
     * If provided calendar is null, use default academic calendar
     * 
     * @param type the AcademicPeriod type of interval
     * @param calendar the calendar to search in
     * @return the current ExecutionInterval
     */
    public static ExecutionInterval findCurrentChild(final AcademicPeriod type, final AcademicCalendarRootEntry calendar) {
        final AcademicCalendarEntry calendarToCheck = calendar != null ? calendar : AcademicCalendarEntry.findDefaultCalendar();
        return findCurrentsChilds().stream().filter(ey -> ey.getAcademicCalendar() == calendarToCheck)
                .filter(ei -> type != null && type.equals(ei.getAcademicPeriod())).findFirst().orElse(null);
    }

    public static Collection<ExecutionInterval> findCurrentsChilds() {
        return findAllChilds().stream().filter(ei -> ei.isCurrent()).collect(Collectors.toSet());
    }

    public static ExecutionInterval findFirstCurrentChild(final AcademicCalendarRootEntry calendar) {
        return findCurrentChilds(calendar).stream().min(Comparator.naturalOrder()).orElse(null);
    }

    /**
     * Returns current ExecutionIntervals for provided calendar.
     * If provided calendar is null, use default academic calendar
     * 
     * @param calendar the calendar to search in
     * @return the current ExecutionInterval
     */
    private static Collection<ExecutionInterval> findCurrentChilds(final AcademicCalendarRootEntry calendar) {
        final AcademicCalendarEntry calendarToCheck = calendar != null ? calendar : AcademicCalendarEntry.findDefaultCalendar();
        return findCurrentsChilds().stream().filter(ey -> ey.getAcademicInterval().getAcademicCalendar() == calendarToCheck)
                .collect(Collectors.toSet());
    }

    @Deprecated
    public static ExecutionInterval findFirstChild() {
        return findAllChilds().stream().min(Comparator.naturalOrder()).orElse(null);
    }

    public static Optional<ExecutionInterval> findFirstChild(final AcademicCalendarEntry calendar) {
        final AcademicCalendarEntry calendarToCheck =
                calendar != null ? calendar.getRootEntry() : AcademicCalendarEntry.findDefaultCalendar();

        return findAllChilds().stream().filter(ei -> ei.getAcademicCalendar() == calendarToCheck).min(Comparator.naturalOrder());
    }

    @Deprecated
    public static ExecutionInterval findLastChild() {
        return findAllChilds().stream().max(Comparator.naturalOrder()).orElse(null);
    }

    public static Optional<ExecutionInterval> findLastChild(final AcademicCalendarEntry calendar) {
        final AcademicCalendarEntry calendarToCheck =
                calendar != null ? calendar.getRootEntry() : AcademicCalendarEntry.findDefaultCalendar();

        return findAllChilds().stream().filter(ei -> ei.getAcademicCalendar() == calendarToCheck).max(Comparator.naturalOrder());
    }

    public AcademicPeriod getAcademicPeriod() {
        return Optional.ofNullable(getAcademicCalendarEntry()).map(ace -> ace.getAcademicPeriod()).orElse(null);
    }

    // TODO: after ensuring that there aren't anymore ExecutionInterval instances without AcademicCalendarEntry, remove null safety checks 
    public AcademicInterval getAcademicInterval() {
        return Optional.ofNullable(getAcademicCalendarEntry()).map(ace -> new AcademicInterval(ace, ace.getRootEntry()))
                .orElseGet(() -> super.getAcademicInterval());
    }

    // TODO: after ensuring that there aren't anymore ExecutionInterval instances without AcademicCalendarEntry, remove null safety checks
    public AcademicCalendarEntry getAcademicCalendar() {
        return Optional.ofNullable(getAcademicCalendarEntry()).map(ace -> ace.getAcademicCalendar())
                .orElseGet(() -> super.getAcademicInterval().getAcademicCalendar());
    }

    public boolean populateAcademicCalendarEntry() {
        if (getAcademicCalendarEntry() == null) {
            setAcademicCalendarEntry(super.getAcademicInterval().getAcademicCalendarEntry());
            return true;
        }
        return false;
    }

    public void delete() {
        if (!getAssociatedExecutionCoursesSet().isEmpty()) {
            throw new Error("cannot.delete.execution.period.because.execution.courses.exist");
        }
        if (!getEnrolmentsSet().isEmpty()) {
            throw new Error("cannot.delete.execution.period.because.enrolments.exist");
        }
        setAcademicCalendarEntry(null);
        setExecutionYear(null);
        setRootDomainObjectForExecutionPeriod(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

    public String getShortName() {
        return getAcademicPeriod().getCode() + Optional.ofNullable(getChildOrder()).map(o -> o.toString()).orElse("");
    }

}
