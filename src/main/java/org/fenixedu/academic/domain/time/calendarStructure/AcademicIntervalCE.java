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
package org.fenixedu.academic.domain.time.calendarStructure;

import java.util.Comparator;
import java.util.Objects;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

public class AcademicIntervalCE extends AcademicIntervalCE_Base {

    private static Comparator<AcademicCalendarEntry> COMPARATOR_BY_PARENT_AND_PERIOD_ORDER =
            Comparator.comparing(AcademicCalendarEntry::getParentEntry, AcademicCalendarEntry.COMPARATOR_BY_BEGIN_DATE)
                    .thenComparing(AcademicCalendarEntry::getAcademicPeriodOrder,
                            Comparator.nullsLast(Comparator.comparingInt(AcademicPeriodOrder::getPeriodOrder)))
                    // if PeriodOrder is null (e.g. AcademicPeriod.OTHER -> fallback to compare by begin date)
                    .thenComparing(AcademicCalendarEntry.COMPARATOR_BY_BEGIN_DATE);

    @Deprecated(forRemoval = true)
    public AcademicIntervalCE(AcademicPeriod academicPeriod, AcademicCalendarEntry parentEntry, LocalizedString title,
            LocalizedString description, DateTime begin, DateTime end, AcademicCalendarRootEntry rootEntry) {

        super();
        setAcademicPeriod(academicPeriod);
        super.initEntry(parentEntry, title, description, begin, end, rootEntry);
        createNewExecutionPeriod();
    }

    public AcademicIntervalCE(AcademicPeriod academicPeriod, AcademicPeriodOrder academicPeriodOrder,
            AcademicCalendarEntry parentEntry, LocalizedString title, LocalizedString description, DateTime begin, DateTime end,
            AcademicCalendarRootEntry rootEntry) {

        super();
        setAcademicPeriod(academicPeriod);
        super.initEntry(parentEntry, title, description, begin, end, rootEntry);
        assignAcademicPeriodOrder(academicPeriodOrder);
        createNewExecutionPeriod();
    }

    @Override
    public AcademicPeriod getAcademicPeriod() {
        return super.getAcademicPeriod() != null ? super.getAcademicPeriod() : AcademicPeriod.SEMESTER;
    }

    @Override
    public AcademicCalendarEntry getNextAcademicCalendarEntry() {
        return getRootEntry().getAllChildEntries(getAcademicPeriod()).stream()
                .filter(entry -> COMPARATOR_BY_PARENT_AND_PERIOD_ORDER.compare(entry, this) > 0)
                .min(COMPARATOR_BY_PARENT_AND_PERIOD_ORDER).orElse(null);
    }

    @Override
    public AcademicCalendarEntry getPreviousAcademicCalendarEntry() {
        return getRootEntry().getAllChildEntries(getAcademicPeriod()).stream()
                .filter(entry -> COMPARATOR_BY_PARENT_AND_PERIOD_ORDER.compare(entry, this) < 0)
                .max(COMPARATOR_BY_PARENT_AND_PERIOD_ORDER).orElse(null);
    }

    @Override
    protected boolean isParentEntryInvalid(AcademicCalendarEntry parentEntry) {
        return !parentEntry.isAcademicYear();
    }

    @Override
    protected boolean exceededNumberOfChildEntries(AcademicCalendarEntry childEntry) {
        return true;
    }

    private void assignAcademicPeriodOrder(AcademicPeriodOrder academicPeriodOrder) {
        if (academicPeriodOrder == null) {
            if (getAcademicPeriod().equals(AcademicPeriod.OTHER)) {
                return;
            }
            throw new DomainException("error.AcademicIntervalCE.academicPeriodOrder.required");
        }

        boolean isAlreadyUsed = getParentEntry().getChildEntriesSet().stream().filter(entry -> entry != this)
                .filter(entry -> getAcademicPeriod().equals(entry.getAcademicPeriod()))
                .anyMatch(entry -> Objects.equals(academicPeriodOrder, entry.getAcademicPeriodOrder()));

        if (isAlreadyUsed) {
            throw new DomainException("error.AcademicIntervalCE.academicPeriodOrder.duplicated");
        }

        setAcademicPeriodOrder(academicPeriodOrder);
    }

    private void createNewExecutionPeriod() {
        ExecutionYear executionYear = ExecutionYear.getExecutionYear((AcademicYearCE) getParentEntry());
        new ExecutionInterval(executionYear, this, getTitle().getContent());
    }

    @Override
    public String getPresentationName() {
        return getParentEntry().getTitle().getContent() + " - " + getTitle().getContent() + " - [" + getType() + "]";
    }
}
