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

import java.util.Optional;

import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class UnitAcronym extends UnitAcronym_Base {

    static {
        getRelationUnitUnitAcronym().addListener(new RelationAdapter<Unit, UnitAcronym>() {

            @Override
            public void afterRemove(Unit unit, UnitAcronym unitAcronym) {
                if (unitAcronym != null) {
                    if (unitAcronym.getUnitsSet().isEmpty()) {
                        unitAcronym.delete();
                    }
                }
            }

        });
    }

    public UnitAcronym(final String acronym) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setAcronym(acronym);
    }

    @Override
    public void setAcronym(String acronym) {
        super.setAcronym(acronym == null ? null : acronym.toLowerCase());
    }

    protected void delete() {
        setRootDomainObject(null);
        deleteDomainObject();
    }

    public static Optional<UnitAcronym> readUnitAcronymByAcronym(final String acronym) {
        if (acronym == null) {
            return Optional.empty();
        }
        final String lowerCaseAcronym = acronym.toLowerCase();
        return Bennu.getInstance().getUnitAcronymsSet().stream()
                .filter(unitAcronym -> unitAcronym.getAcronym().equals(lowerCaseAcronym)).findAny();
    }
}
