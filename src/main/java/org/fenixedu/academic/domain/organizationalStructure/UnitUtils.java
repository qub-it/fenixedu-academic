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
 * Created on Feb 6, 2006
 *	by mrsp
 */
package org.fenixedu.academic.domain.organizationalStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.YearMonthDay;

public class UnitUtils {

    public static Unit readExternalInstitutionUnitByName(final String name) {
        return readExternalInstitutionUnitByName(readExternalInstitutionUnit(), name);
    }

    private static Unit readExternalInstitutionUnitByName(final Unit unit, final String name) {
        if (unit.getName().equals(name)) {
            return unit;
        }
        for (final Accountability acc : unit.getChildsSet()) {
            final Party child = acc.getChildParty();
            if (child instanceof Unit) {
                final Unit childUnit = (Unit) child;
                final Unit result = readExternalInstitutionUnitByName(childUnit, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static List<Unit> readAllActiveUnitsByType(PartyTypeEnum type) {
        final List<Unit> result = new ArrayList<Unit>();
        final YearMonthDay now = new YearMonthDay();
        PartyType partyType = PartyType.readPartyTypeByType(type);
        if (partyType != null) {
            Collection<Party> parties = partyType.getPartiesSet();
            for (Party party : parties) {
                if (party.isUnit()) {
                    Unit unit = (Unit) party;
                    if (unit.isActive(now)) {
                        result.add(unit);
                    }
                }
            }
        }
        return result;
    }

    public static Unit readExternalInstitutionUnit() {
        return Bennu.getInstance().getExternalInstitutionUnit();
    }

    public static Unit readInstitutionUnit() {
        return Bennu.getInstance().getInstitutionUnit();
    }

    public static Unit readEarthUnit() {
        return Bennu.getInstance().getEarthUnit();
    }

    public static List<Unit> getUnitFullPath(final Unit unit, final List<AccountabilityTypeEnum> validAccountabilityTypes) {
        final Collection<Unit> parentUnits = unit.getParentUnits(validAccountabilityTypes);
        if (parentUnits.isEmpty()) {
            return Collections.emptyList();
        }
        if (parentUnits.size() == 1) {
            final List<Unit> result = new ArrayList<Unit>();
            result.add(unit);
            result.addAll(0, getUnitFullPath(parentUnits.iterator().next(), validAccountabilityTypes));
            return result;
        }
        throw new DomainException("error.unitUtils.unit.full.path.has.more.than.one.parent");
    }

    public static StringBuilder getUnitFullPathName(final Unit unit,
            final List<AccountabilityTypeEnum> validAccountabilityTypes) {
        if (unit == readEarthUnit()) {
            return new StringBuilder(0);
        }
        final Collection<Unit> parentUnits = unit.getParentUnits(validAccountabilityTypes);
        if (parentUnits.isEmpty()) {
            return new StringBuilder(unit.getName());
        }
        if (parentUnits.size() == 1) {
            final StringBuilder builder = new StringBuilder();
            builder.append(parentUnits.iterator().next() == readEarthUnit() ? "" : " > ").append(unit.getName());
            builder.insert(0, getUnitFullPathName(parentUnits.iterator().next(), validAccountabilityTypes));
            return builder;
        }
        throw new DomainException("error.unitUtils.unit.full.path.has.more.than.one.parent");
    }

}
