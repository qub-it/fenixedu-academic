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
import java.util.List;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.chronologies.AcademicChronology;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

public class AcademicCalendarRootEntry extends AcademicCalendarRootEntry_Base {

    private transient volatile AcademicChronology academicChronology;

    public AcademicCalendarRootEntry(LocalizedString title, LocalizedString description) {
        super();
        setRootDomainObjectForRootEntries(Bennu.getInstance());
        setTitle(title);
        setDescription(description);
    }

    @Override
    public AcademicCalendarEntry edit(LocalizedString title, LocalizedString description, DateTime begin, DateTime end,
            AcademicCalendarRootEntry rootEntryDestination) {

        setTitle(title);
        setDescription(description);
        return this;
    }

    @Override
    public void delete(AcademicCalendarRootEntry rootEntry) {
        super.setRootDomainObjectForRootEntries(null);
        super.delete(rootEntry);
    }

    @Override
    public AcademicChronology getAcademicChronology() {
        if (academicChronology == null) {
            synchronized (AcademicCalendarRootEntry.class) {
                if (academicChronology == null) {
                    academicChronology = new AcademicChronology(this);
                }
            }
        }
        return academicChronology;
    }

    @Override
    public void setRootDomainObjectForRootEntries(Bennu rootDomainObjectForRootEntries) {
        if (rootDomainObjectForRootEntries == null) {
            throw new DomainException("error.RootEntry.empty.rootDomainObject.to.academic.calendars");
        }
        super.setRootDomainObjectForRootEntries(rootDomainObjectForRootEntries);
    }

    @Override
    public DateTime getBegin() {
        return getChildEntriesSet().stream().map(AcademicCalendarEntry::getBegin).min(DateTime::compareTo).orElse(null);
    }

    public AcademicCalendarEntry getEntryByInstant(long instant, AcademicPeriod academicPeriod) {
        return getAllChildEntries(academicPeriod).stream().filter(entry -> entry.containsInstant(instant))
                .max(Comparator.comparing(AcademicCalendarEntry::getBegin)).orElse(null);
    }

    public Integer getEntryIndexByInstant(long instant, AcademicPeriod academicPeriod) {
        long count = getAllChildEntries(academicPeriod).stream()
                .filter(entry -> entry.containsInstant(instant) || entry.getEnd().isBefore(instant)).count();
        return count > 0 ? Long.valueOf(count).intValue() : null;
    }

    public AcademicCalendarEntry getEntryByIndex(int index, AcademicPeriod academicPeriod) {
        List<AcademicCalendarEntry> allChildEntries = getAllChildEntries(academicPeriod);
        allChildEntries.sort(COMPARATOR_BY_BEGIN_DATE);
        return index > 0 && index <= allChildEntries.size() ? allChildEntries.get(index - 1) : null;
    }

    public static AcademicCalendarRootEntry getAcademicCalendarByTitle(String title) {
        return Bennu.getInstance().getAcademicCalendarsSet().stream().filter(re -> re.getTitle().getContent().equals(title))
                .findFirst().orElse(null);
    }

    @Override
    public DateTime getEnd() {
        return null;
    }

    @Override
    public void setBegin(DateTime begin) {
        throw new DomainException("error.unsupported.operation");
    }

    @Override
    public void setEnd(DateTime end) {
        throw new DomainException("error.unsupported.operation");
    }

    @Override
    public void setParentEntry(AcademicCalendarEntry parentEntry) {
        throw new DomainException("error.unsupported.operation");
    }

    @Override
    protected boolean exceededNumberOfChildEntries(AcademicCalendarEntry childEntry) {
        return false;
    }

    @Override
    protected boolean isParentEntryInvalid(AcademicCalendarEntry parentEntry) {
        return true;
    }

    @Override
    public boolean isOfType(AcademicPeriod period) {
        return false;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public boolean containsInstant(final long instant) {
        return true;
    }

}
