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
package org.fenixedu.academic.domain.organizationalStructure;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.bennu.core.domain.Bennu;

public class UnitName extends UnitName_Base implements Comparable<UnitName> {

    public UnitName(Unit unit) {
        super();
        this.setRootDomainObject(Bennu.getInstance());
        setUnit(unit);
        setIsExternalUnit(Boolean.valueOf(!unit.isInternal()));
    }

    @Override
    public int compareTo(UnitName unitName) {
        final int stringCompare = getName().compareTo(unitName.getName());
        return stringCompare == 0 ? getExternalId().compareTo(unitName.getExternalId()) : stringCompare;
    }

    @Override
    public void setName(String name) {
        super.setName(UnitNamePart.normalize(name));
        UnitNamePart.reindex(this);
    }

    public void delete() {
        final Set<UnitNamePart> unitNameParts = new HashSet<UnitNamePart>(getUnitNamePartSet());
        getUnitNamePartSet().clear();
        setUnit(null);
        setRootDomainObject(null);
        deleteDomainObject();
        for (final UnitNamePart unitNamePart : unitNameParts) {
            unitNamePart.deleteIfEmpty();
        }
    }

}
