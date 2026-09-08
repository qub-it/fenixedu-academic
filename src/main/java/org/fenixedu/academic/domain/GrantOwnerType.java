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

public enum GrantOwnerType {

    STUDENT_WITHOUT_SCHOLARSHIP,

    HIGHER_EDUCATION_SAS_GRANT_OWNER_CANDIDATE,

    HIGHER_EDUCATION_SAS_GRANT_OWNER,

    HIGHER_EDUCATION_NOT_SAS_GRANT_OWNER,

    FCT_GRANT_OWNER,

    ORIGIN_COUNTRY_GRANT_OWNER,

    OTHER_INSTITUTION_GRANT_OWNER;

    public String getName() {
        return name();
    }

    public String getQualifiedName() {
        return GrantOwnerType.class.getSimpleName() + "." + name();
    }

    public String getFullyQualifiedName() {
        return GrantOwnerType.class.getName() + "." + name();
    }
}
