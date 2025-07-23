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
package org.fenixedu.academic.domain.raides;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.personaldata.EducationLevelType;
import org.fenixedu.bennu.core.domain.Bennu;

public class DegreeDesignation extends DegreeDesignation_Base {

    public DegreeDesignation(String code, String description, DegreeClassification degreeClassification) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setCode(code);
        setDescription(description);
        setDegreeClassification(degreeClassification);
    }

    public static DegreeDesignation readByNameAndEducationLevelType(String degreeDesignationName,
            EducationLevelType educationLevelType) {
        if ((educationLevelType == null) || (degreeDesignationName == null)) {
            return null;
        }

        Set<DegreeClassification> possibleClassifications = educationLevelType.getDegreeClassificationsSet();

        List<DegreeDesignation> possibleDesignations = new ArrayList<DegreeDesignation>();
        for (DegreeClassification classification : possibleClassifications) {
            if (!classification.getDegreeDesignationsSet().isEmpty()) {
                possibleDesignations.addAll(classification.getDegreeDesignationsSet());
            }
        }

        for (DegreeDesignation degreeDesignation : possibleDesignations) {
            if (degreeDesignation.getDescription().equalsIgnoreCase(degreeDesignationName)) {
                return degreeDesignation;
            }
        }
        return null;
    }

    public void delete() {
        for (Unit institution : getInstitutionUnitSet()) {
            removeInstitutionUnit(institution);
        }
        setDegreeClassification(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }
}
