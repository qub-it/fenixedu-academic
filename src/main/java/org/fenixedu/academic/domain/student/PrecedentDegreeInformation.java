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
package org.fenixedu.academic.domain.student;

import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.personaldata.EducationLevelType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class PrecedentDegreeInformation extends PrecedentDegreeInformation_Base {

    public PrecedentDegreeInformation() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setLastModifiedDate(new DateTime());
    }

    public String getInstitutionName() {
        return getInstitution() != null ? getInstitution().getName() : null;
    }

    public void delete() {
        setCountry(null);
        setCountryHighSchool(null);
        setInstitution(null);

        setCompletedStudentCandidacy(null);
        setPreviousStudentCandidacy(null);

        setCompletedRegistration(null);
        setPreviousRegistration(null);

        setEducationLevelType(null);

        setRootDomainObject(null);
        deleteDomainObject();
    }

    @Override
    public void setSchoolLevel(SchoolLevelType schoolLevel) {
        super.setEducationLevelType(findEducationLevelType(schoolLevel));
        super.setSchoolLevel(schoolLevel);
    }

    @Override
    public void setEducationLevelType(EducationLevelType educationLevelType) {
        super.setSchoolLevel(findSchoolLevel(educationLevelType));
        super.setEducationLevelType(educationLevelType);
    }

    private SchoolLevelType findSchoolLevel(EducationLevelType educationLevelType) {
        if (educationLevelType == null) {
            return null;
        }
        return SchoolLevelType.findByCode(educationLevelType.getCode())
                .orElseThrow(() -> new DomainException("error.EducationLevelType.not.found", educationLevelType.getCode()));
    }

    private EducationLevelType findEducationLevelType(SchoolLevelType schoolLevel) {
        if (schoolLevel == null) {
            return null;
        }
        return EducationLevelType.findByCode(schoolLevel.getName())
                .orElseThrow(() -> new DomainException("error.SchoolLevelType.not.found", schoolLevel.getName()));
    }
}
