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

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

public class DegreeOfficialPublication extends DegreeOfficialPublication_Base {
    public DegreeOfficialPublication(Degree degree, LocalDate date) {
        if (degree == null) {
            throw new DomainException("error.degree.officialpublication.unlinked");
        }
        if (date == null) {
            throw new DomainException("error.degree.officialpublication.undated");
        }
        setDegree(degree);
        setPublication(date);
    }

    @Atomic
    public void delete() {

        setDegree(null);

        super.deleteDomainObject();
    }

    public static DegreeOfficialPublication create(Degree degree, LocalDate publicationDate, String officialReference) {
        checkRules(publicationDate, officialReference, null, null);

        DegreeOfficialPublication result = new DegreeOfficialPublication(degree, publicationDate);
        result.setOfficialReference(officialReference);

        return result;
    }

    public void edit(LocalDate publication, String officialReference, LocalDate beginDate, LocalDate endDate) {
        checkRules(publication, officialReference, beginDate, endDate);

        setPublication(publication);
        setOfficialReference(officialReference);
        setBeginDate(beginDate);
        setEndDate(endDate);
    }

    public DegreeSpecializationArea createSpecializationArea(final LocalizedString name) {
        return new DegreeSpecializationArea(this, name);
    }

    private static void checkRules(LocalDate publication, String officialReference, LocalDate beginDate, LocalDate endDate) {
        if (publication == null) {
            throw new DomainException(DegreeOfficialPublication.class.getName() + ".publication.not.null");
        }

        if (StringUtils.isEmpty(officialReference)) {
            throw new DomainException(DegreeOfficialPublication.class.getName() + ".officialReference.not.null");
        }

        if (beginDate != null && endDate != null && beginDate.isAfter(endDate)) {
            throw new DomainException(DegreeOfficialPublication.class.getName() + ".endDate.before.beginDate");
        }
    }
}
