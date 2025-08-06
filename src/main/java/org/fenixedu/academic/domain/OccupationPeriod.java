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
/*
 * Created on 14/Out/2003
 *
 */
package org.fenixedu.academic.domain;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.date.IntervalTools;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

/**
 * @author Ana e Ricardo
 * 
 */
@SuppressWarnings("deprecation")
public class OccupationPeriod extends OccupationPeriod_Base {

    private OccupationPeriod() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public OccupationPeriod(Interval interval) {
        this();
        if (interval == null) {
            throw new DomainException("error.occupationPeriod.invalid.dates");
        }
        this.setPeriodInterval(interval);
    }

    /**
     * Constructor that creates and links together several instances, allowing
     * for the definition of all the intervals
     * 
     * @param intervals
     */
    public OccupationPeriod(Iterator<Interval> intervals) {
        this();
        if (intervals == null || !intervals.hasNext()) {
            throw new DomainException("error.occupationPeriod.invalid.dates");
        }

        Interval interval = intervals.next();

        this.setPeriodInterval(interval);

        if (intervals.hasNext()) {
            this.setNextPeriod(new OccupationPeriod(intervals));
        }
    }

    public void setNextPeriodWithoutChecks(OccupationPeriod nextPeriod) {
        if (nextPeriod != null && !nextPeriod.getPeriodInterval().isAfter(getPeriodInterval())) {
            throw new DomainException("error.occupationPeriod.invalid.nextPeriod");
        }
        super.setNextPeriod(nextPeriod);
    }

    private void setPreviousPeriodWithoutChecks(OccupationPeriod previousPeriod) {
        if (previousPeriod != null && !previousPeriod.getPeriodInterval().isBefore(getPeriodInterval())) {
            throw new DomainException("error.occupationPeriod.invalid.previousPeriod");
        }
        super.setPreviousPeriod(previousPeriod);
    }

    @Override
    public void setNextPeriod(OccupationPeriod nextPeriod) {
        if (!allNestedPeriodsAreEmpty()) {
            throw new DomainException("error.occupationPeriod.previous.periods.not.empty");
        }
        this.setNextPeriodWithoutChecks(nextPeriod);
    }

    @Override
    public void setPreviousPeriod(OccupationPeriod previousPeriod) {
        if (!allNestedPeriodsAreEmpty()) {
            throw new DomainException("error.occupationPeriod.next.periods.not.empty");
        }
        this.setPreviousPeriodWithoutChecks(previousPeriod);
    }

    private boolean containsDay(YearMonthDay day) {
        return this.getPeriodInterval().contains(day.toDateTimeAtMidnight());
    }

    public boolean isDateInNestedPeriods(final OccupationPeriod rootOccupationPeriod, final DateTime dateToCheck) {
        return getIntervals().stream().anyMatch(interval -> interval.contains(dateToCheck));
    }

    public void delete() {
        if (allNestedPeriodsAreEmpty()) {
            OccupationPeriod first = getFirstOccupationPeriodOfNestedPeriods();
            first.deleteAllNestedPeriods();
        }
    }

    private void deleteAllNestedPeriods() {
        OccupationPeriod nextPeriod = getNextPeriod();

        super.setNextPeriod(null);
        super.setPreviousPeriod(null);
        setRootDomainObject(null);
        deleteDomainObject();

        if (nextPeriod != null) {
            nextPeriod.delete();
        }
    }

    @Deprecated
    public void deleteFromNestedPeriods(final OccupationPeriod period) {
        deleteFromNestedPeriods();
    }

    public void deleteFromNestedPeriods() {
        if (getPreviousPeriod() != null) { // not a 'root' period
            getPreviousPeriod().setNextPeriodWithoutChecks(getNextPeriod());
        }
        delete();
    }

    public List<OccupationPeriod> getAllNestedPeriods() {
        final List<OccupationPeriod> periods = new ArrayList<>();
        OccupationPeriod occupationPeriod = this;
        periods.add(occupationPeriod);
        while (occupationPeriod.getNextPeriod() != null) {
            occupationPeriod = occupationPeriod.getNextPeriod();
            periods.add(occupationPeriod);
        }
        return periods;
    }

    public boolean allNestedPeriodsAreEmpty() {
        OccupationPeriod firstOccupationPeriod = getFirstOccupationPeriodOfNestedPeriods();
        if (!firstOccupationPeriod.isEmpty()) {
            return false;
        }
        while (firstOccupationPeriod.getNextPeriod() != null) {
            if (!firstOccupationPeriod.getNextPeriod().isEmpty()) {
                return false;
            }
            firstOccupationPeriod = firstOccupationPeriod.getNextPeriod();
        }
        return true;
    }

    private boolean isEmpty() {
        return getLessonsSet().isEmpty() && getInitialLessonsSet().isEmpty() && getLessonPeriod() == null;
    }

    public OccupationPeriod getLastOccupationPeriodOfNestedPeriods() {
        OccupationPeriod occupationPeriod = this;
        while (occupationPeriod.getNextPeriod() != null) {
            occupationPeriod = occupationPeriod.getNextPeriod();
        }
        return occupationPeriod;
    }

    private OccupationPeriod getFirstOccupationPeriodOfNestedPeriods() {
        OccupationPeriod occupationPeriod = this;
        while (occupationPeriod.getPreviousPeriod() != null) {
            occupationPeriod = occupationPeriod.getPreviousPeriod();
        }
        return occupationPeriod;
    }

    public boolean nestedOccupationPeriodsContainsDay(YearMonthDay day) {
        OccupationPeriod firstOccupationPeriod = this;
        while (firstOccupationPeriod != null) {
            if (firstOccupationPeriod.containsDay(day)) {
                return true;
            }
            firstOccupationPeriod = firstOccupationPeriod.getNextPeriod();
        }
        return false;
    }

    private YearMonthDay getEndYearMonthDayWithNextPeriods() {
        return getNextPeriod() != null ? getNextPeriod().getEndYearMonthDayWithNextPeriods() : getEndYearMonthDay();
    }

    public Interval getIntervalWithNextPeriods() {
        return new Interval(getStartYearMonthDay().toLocalDate().toDateTimeAtStartOfDay(),
                getEndYearMonthDayWithNextPeriods().toLocalDate().toDateTimeAtStartOfDay());
    }

    /*
     * Deprecated getters and setters, meant exclusively for compatibility. New
     * clients of the Class should use the interval ones Instead.
     */

    @Deprecated
    public YearMonthDay getStartYearMonthDay() {
        Interval interval = this.getPeriodInterval();
        return IntervalTools.getStartYMD(interval);
    }

    @Deprecated
    public YearMonthDay getEndYearMonthDay() {
        Interval interval = this.getPeriodInterval();
        return IntervalTools.getEndYMD(interval);
    }

    public List<Interval> getIntervals() {
        List<Interval> intervals = new LinkedList<Interval>();

        OccupationPeriod period = this;

        while (period != null) {
            intervals.add(period.getPeriodInterval());
            period = period.getNextPeriod();
        }

        return intervals;
    }

    public void editDates(Iterator<Interval> intervals) {

        this.setPeriodInterval(intervals.next());

        if (!intervals.hasNext()) {
            this.setNextPeriodWithoutChecks(null);
        } else {
            if (this.getNextPeriod() != null) {
                this.getNextPeriod().editDates(intervals);
            } else {
                this.setNextPeriodWithoutChecks(new OccupationPeriod(intervals));
            }
        }

    }

    @Override
    public void setPeriodInterval(Interval periodInterval) {
        if (periodInterval != null) {
            if (getPreviousPeriod() != null && !getPreviousPeriod().getPeriodInterval().isBefore(periodInterval)) {
                throw new DomainException("error.occupationPeriod.invalid.dates");
            }
            if (getNextPeriod() != null && !getNextPeriod().getPeriodInterval().isAfter(periodInterval)) {
                throw new DomainException("error.occupationPeriod.invalid.dates");
            }
        }
        super.setPeriodInterval(periodInterval);
    }

}
