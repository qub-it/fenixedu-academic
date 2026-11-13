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

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

public class ExecutionInterval extends ExecutionInterval_Base implements Comparable<ExecutionInterval> {

    public static final Comparator<ExecutionInterval> COMPARATOR_BY_BEGIN_DATE =
            Comparator.comparing((ExecutionInterval ei) -> ei.getAcademicCalendarEntry().getBegin())
                    .thenComparing(ei -> ei.getAcademicCalendarEntry().getEnd(), Comparator.reverseOrder());

    protected ExecutionInterval() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setState(PeriodState.NOT_OPEN);
    }

    public ExecutionInterval(ExecutionYear executionYear, AcademicCalendarEntry academicCalendarEntry) {
        this();
        setRootDomainObjectForExecutionPeriod(Bennu.getInstance());
        setExecutionYear(executionYear);
        setAcademicCalendarEntry(academicCalendarEntry);
    }

    @Override
    public void setState(PeriodState state) {
        if (state == null) {
            throw new DomainException("error.executionInterval.empty.state");
        }
        super.setState(state);
    }

    public void changeState(final PeriodState newState) {
        if (getState().equals(newState)) {
            return;
        }

        if (PeriodState.CURRENT.equals(newState)) {
            final AcademicCalendarRootEntry calendar = (AcademicCalendarRootEntry) getAcademicCalendar();

            // Deactivate the current interval and year
            final ExecutionInterval oldCurrentExecutionInterval =
                    ExecutionInterval.findCurrentChild(getAcademicPeriod(), calendar);
            if (oldCurrentExecutionInterval != null) {
                oldCurrentExecutionInterval.openInterval();
            }

            final ExecutionYear newCurrentExecutionYear = getExecutionYear();

            // Deactivate eventual other current intervals of other years from the same calendar
            ExecutionInterval.findCurrentsChilds().stream().filter(ei -> ei.getAcademicCalendar() == calendar)
                    .filter(ei -> ei.getExecutionYear() != newCurrentExecutionYear).forEach(ei -> ei.openInterval());

            // Update year
            newCurrentExecutionYear.setState(PeriodState.CURRENT);
            newCurrentExecutionYear.setRootDomainObjectForCurrentExecutionInterval(Bennu.getInstance());

            setRootDomainObjectForCurrentExecutionInterval(Bennu.getInstance());
        }

        setState(newState);
    }

    private void openInterval() {
        setState(PeriodState.OPEN);
        getExecutionYear().setState(PeriodState.OPEN);
        setRootDomainObjectForCurrentExecutionInterval(null);
        getExecutionYear().setRootDomainObjectForCurrentExecutionInterval(null);
    }

    public String getName() {
        return getAcademicCalendarEntry().getTitle().getContent();
    }

    public String getQualifiedName() {
        return getAcademicCalendarEntry().getTitle().getContent() + " " + this.getExecutionYear().getName();
    }

    public LocalizedString getQualifiedNameI18N() {
        final LocalizedString localizedName = getAcademicCalendarEntry().getTitle();
        return localizedName.append(this.getExecutionYear().getAcademicCalendarEntry().getTitle(), " ");
    }

    public Interval toInterval() {
        final AcademicCalendarEntry entry = getAcademicCalendarEntry();
        return new Interval(entry.getBegin(), entry.getEnd());
    }

    public boolean isCurrent() {
        return getState().equals(PeriodState.CURRENT);
    }

    public boolean isActive() {
        return getState().equals(PeriodState.CURRENT) || getState().equals(PeriodState.OPEN);
    }

    public static ExecutionInterval getExecutionInterval(String qualifiedName) {
        return Bennu.getInstance().getExecutionIntervalsSet().stream()
                .filter(interval -> interval.getQualifiedName().equals(qualifiedName)).findFirst().orElse(null);
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

        return COMPARATOR_BY_BEGIN_DATE.thenComparing(ExecutionInterval::getOid).compare(this, anotherInterval);
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

    public LocalDate getBeginLocalDate() {
        return getAcademicCalendarEntry().getBegin().toLocalDate();
    }

    @Deprecated
    public YearMonthDay getBeginDateYearMonthDay() {
        return getAcademicCalendarEntry().getBegin().toYearMonthDay();
    }

    public LocalDate getEndLocalDate() {
        return getAcademicCalendarEntry().getEnd().toLocalDate();
    }

    @Deprecated
    public YearMonthDay getEndDateYearMonthDay() {
        return getAcademicCalendarEntry().getEnd().toYearMonthDay();
    }

    public <E extends ExecutionInterval> E convert(final Class<E> input) {
        return ExecutionYear.class.equals(input) ? (E) getExecutionYear() : (E) this;
    }

    public Integer getChildOrder() {
        return getAcademicCalendarEntry().getCardinality();
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
        return Bennu.getInstance().getCurrentExecutionIntervalsSet().stream().filter(ei -> !ei.isAggregator())
                .filter(ExecutionInterval::isCurrent).collect(Collectors.toSet());
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
        return findCurrentsChilds().stream().filter(ey -> ey.getAcademicCalendarEntry().getRootEntry() == calendarToCheck)
                .collect(Collectors.toSet());
    }

    public static Optional<ExecutionInterval> findFirstChild(final AcademicCalendarEntry calendar) {
        final AcademicCalendarEntry calendarToCheck =
                calendar != null ? calendar.getRootEntry() : AcademicCalendarEntry.findDefaultCalendar();

        return findAllChilds().stream().filter(ei -> ei.getAcademicCalendar() == calendarToCheck).min(Comparator.naturalOrder());
    }

    public static Optional<ExecutionInterval> findLastChild(final AcademicCalendarEntry calendar) {
        final AcademicCalendarEntry calendarToCheck =
                calendar != null ? calendar.getRootEntry() : AcademicCalendarEntry.findDefaultCalendar();

        return findAllChilds().stream().filter(ei -> ei.getAcademicCalendar() == calendarToCheck).max(Comparator.naturalOrder());
    }

    public AcademicPeriod getAcademicPeriod() {
        return Optional.ofNullable(getAcademicCalendarEntry()).map(ace -> ace.getAcademicPeriod()).orElse(null);
    }

    public AcademicCalendarEntry getAcademicCalendar() {
        return getAcademicCalendarEntry().getRootEntry();
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
        setRootDomainObjectForCurrentExecutionInterval(null);
        deleteDomainObject();
    }

    public String getShortName() {
        return getAcademicPeriod().getCode() + Optional.ofNullable(getChildOrder()).map(o -> o.toString()).orElse("");
    }

}
