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
 * Created on Sep 16, 2005
 *	by mrsp
 */
package org.fenixedu.academic.domain.organizationalStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.YearMonthDay;

public class Unit extends Unit_Base {

    protected Unit() {
        super();
    }

    public static Unit createNewUnit(Optional<PartyType> partyType, LocalizedString unitName, String acronym, Unit parentUnit,
            AccountabilityType accountabilityType) {

        final Unit unit = new Unit();
        partyType.ifPresent(pt -> unit.setPartyType(pt));

        if (parentUnit != null && accountabilityType != null) {
            unit.addParentUnit(parentUnit, accountabilityType); // this must be before setName and setAcronym in order to validations to work
        } else if (!unit.isPlanetUnit()) {
            throw new DomainException("error.unit.create.noParentOrAccountabilityType");
        }

        unit.setPartyName(unitName);
        unit.setAcronym(acronym);

        unit.setBeginDateYearMonthDay(new YearMonthDay());

        return unit;
    }

    public static Unit createNewNoOfficialExternalInstitution(String unitName) {
        final Unit externalInstitutionUnit = UnitUtils.readExternalInstitutionUnit();
        return Unit.createNewUnit(Optional.empty(), new LocalizedString(Locale.getDefault(), unitName), null,
                externalInstitutionUnit, AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE));
    }

    public void edit(LocalizedString name, String acronym) {
        setPartyName(name);
        setAcronym(acronym);
    }

    @Override
    public void setPartyName(LocalizedString partyName) {
        if (partyName == null || partyName.isEmpty()) {
            throw new DomainException("error.Party.empty.partyName");
        }
        super.setPartyName(partyName);

        setName(LocaleUtils.getPreferedContent(partyName));
    }

    @Override
    public void setPartyType(PartyType partyType) {
        super.setPartyType(partyType);
        if (!isScientificAreaUnit() && !getCompetenceCourseScientificAreasSet().isEmpty()) {
            throw new DomainException("error.unit.cannot.change.partytype.when.has.competencecoursescientificareas");
        }
    }

    @Override
    public String getName() {
        return LocaleUtils.getPreferedContent(getPartyName());
    }

    public void setName(String name) {

        if (name == null || StringUtils.isEmpty(name.trim())) {
            throw new DomainException("error.person.empty.name");
        }

        LocalizedString partyName = getPartyName();

        partyName =
                partyName == null ? new LocalizedString(Locale.getDefault(), name) : partyName.with(Locale.getDefault(), name);

        super.setPartyName(partyName);

        UnitName unitName = getUnitName();
        unitName = unitName == null ? new UnitName(this) : unitName;
        unitName.setName(name);
    }

    @Override
    public void setAcronym(String acronym) {
        super.setAcronym(acronym);
        checkUniqueAcronymInSiblingUnits();

        setUnitAcronym(StringUtils.isBlank(acronym) ? null : UnitAcronym.readUnitAcronymByAcronym(acronym)
                .orElseGet(() -> new UnitAcronym(acronym)));
    }

    private void checkUniqueAcronymInSiblingUnits() {
        if (StringUtils.isNotBlank(getAcronym())) {
            final Predicate<Unit> predicate = u -> getAcronym().equalsIgnoreCase(u.getAcronym());
            if (getParentUnits().stream().flatMap(pu -> pu.getSubUnits().stream()).filter(u -> u != this).anyMatch(predicate)) {
                throw new DomainException("error.unit.already.exists.unit.with.same.acronym");
            }
        }
    }

    public void generateAndSetAcronym() {
        final String acronym = Stream.of(getName().split("[^A-Z]+")).collect(Collectors.joining());

        final Set<String> existingAcronyms = getParentUnits().stream().flatMap(u -> u.getSubUnits().stream())
                .map(Unit::getAcronym).filter(Objects::nonNull).collect(Collectors.toSet());

        if (existingAcronyms.contains(acronym)) {
            int version = 1;
            String versionedAcronym = acronym + version;
            while (existingAcronyms.contains(versionedAcronym)) {
                versionedAcronym = acronym + ++version;
            }

            setAcronym(versionedAcronym);
        } else {
            setAcronym(acronym);
        }
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkDateInterval() {
        final YearMonthDay start = getBeginDateYearMonthDay();
        final YearMonthDay end = getEndDateYearMonthDay();
        return start != null && (end == null || !start.isAfter(end));
    }

    @Override
    public void delete() {

        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!getParentsSet().isEmpty()) {
            getParentsSet().iterator().next().delete();
        }

        getUnitName().delete();

        setRootDomainObjectForEarthUnit(null);
        setRootDomainObjectForExternalInstitutionUnit(null);
        setRootDomainObjectForInstitutionUnit(null);
        setCampus(null);
        setUnitAcronym(null);
        setAdministrativeOffice(null);

        super.setDegree(null); // if degree unit

        super.delete();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!(getParentsSet().isEmpty() || (getParentsSet().size() == 1 && getParentUnits().size() == 1))
                && getChildsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
        }

        if (!(getExternalCurricularCoursesSet().isEmpty() && getPrecedentDegreeInformationsSet().isEmpty()
                && getUnitGroupSet().isEmpty())) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
        }

        if (!getCompetenceCourseInformationsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
        }

        if(!getCompetenceCourseScientificAreasSet().isEmpty()){
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
        }
    }

    @Override
    public Space getCampus() {
        Space campus = super.getCampus();
        if (campus != null) {
            return campus;
        }

        Collection<Unit> parentUnits = getParentUnits();
        return parentUnits.size() == 1 ? parentUnits.iterator().next().getCampus() : null;
    }

    public boolean isInternal() {
        return this.equals(UnitUtils.readInstitutionUnit()) || getParentUnits().stream().anyMatch(Unit::isInternal);
    }

    public boolean isNoOfficialExternal() {
        return this.equals(UnitUtils.readExternalInstitutionUnit()) || getParentUnits().stream()
                .anyMatch(Unit::isNoOfficialExternal);
    }

    public boolean isActive(YearMonthDay currentDate) {
        return (!this.getBeginDateYearMonthDay().isAfter(currentDate)
                && (this.getEndDateYearMonthDay() == null || !this.getEndDateYearMonthDay().isBefore(currentDate)));
    }

    @Override
    public boolean isUnit() {
        return true;
    }

    private List<Unit> getInactiveSubUnits(YearMonthDay currentDate) {
        return getSubUnitsByState(currentDate, false);
    }

    private List<Unit> getActiveSubUnits(YearMonthDay currentDate) {
        return getSubUnitsByState(currentDate, true);
    }

    private List<Unit> getSubUnitsByState(YearMonthDay currentDate, boolean state) {
        return getSubUnits().stream().filter(subUnit -> subUnit.isActive(currentDate) == state).toList();
    }

    private List<Unit> getAllInactiveSubUnits(YearMonthDay currentDate) {
        return Stream.concat(getInactiveSubUnits(currentDate).stream(),
                        getSubUnits().stream().flatMap(subUnit -> subUnit.getAllInactiveSubUnits(currentDate).stream())).distinct()
                .toList();
    }

    private List<Unit> getAllActiveSubUnits(YearMonthDay currentDate) {
        return Stream.concat(getActiveSubUnits(currentDate).stream(),
                        getSubUnits().stream().flatMap(subUnit -> subUnit.getAllActiveSubUnits(currentDate).stream())).distinct()
                .toList();
    }

    public Collection<PartyType> getAllowedChildPartyTypes(final Boolean managedByUser) {
        if (isAggregateUnit()) {
            return getParentUnits().stream().flatMap(u -> u.getAllowedChildPartyTypes(managedByUser).stream())
                    .collect(Collectors.toSet());
        }
        return Optional.ofNullable(getPartyType()).map(pt -> pt.getAllowedChildConnectionRulesSet().stream()
                .filter(cr -> managedByUser == null || cr.getManagedByUser() == managedByUser)
                .map(ConnectionRule::getAllowedChildPartyType).collect(Collectors.toSet())).orElseGet(Set::of);
    }

    public Collection<Unit> getAllSubUnits() {
        Collection<Unit> subUnits = getSubUnits();
        Set<Unit> allSubUnits = new HashSet<>(subUnits);

        subUnits.stream().map(Unit::getAllSubUnits).forEach(allSubUnits::addAll);
        return allSubUnits;
    }

    public Collection<Unit> getAllParentUnits() {
        Collection<Unit> parentUnits = getParentUnits();
        Set<Unit> allParentUnits = new HashSet<>(parentUnits);

        parentUnits.stream().map(Unit::getAllParentUnits).forEach(allParentUnits::addAll);
        return allParentUnits;
    }

    public Collection<Unit> getSubUnits(List<AccountabilityTypeEnum> accountabilityTypeEnums) {
        return getChildParties(accountabilityTypeEnums, Unit.class).collect(Collectors.toSet());
    }

    public Collection<Unit> getSubUnits(final PartyTypeEnum type) {
        return getChildParties(type, Unit.class).collect(Collectors.toSet());
    }

    public Accountability addParentUnit(Unit parentUnit, AccountabilityType accountabilityType) {
        if (this.equals(parentUnit)) {
            throw new DomainException("error.unit.equals.parentUnit");
        }
        if (getParentUnits(accountabilityType.getType()).contains(parentUnit)) {
            throw new DomainException("error.unit.parentUnit.is.already.parentUnit");
        }

        YearMonthDay currentDate = new YearMonthDay();
        List<Unit> subUnits =
                (parentUnit.isActive(currentDate)) ? getAllActiveSubUnits(currentDate) : getAllInactiveSubUnits(currentDate);
        if (subUnits.contains(parentUnit)) {
            throw new DomainException("error.unit.parentUnit.is.already.subUnit");
        }

        return new Accountability(parentUnit, this, accountabilityType);
    }

    public Unit getChildUnitByAcronym(String acronym) {
        return getSubUnits().stream().filter(subUnit -> Objects.equals(subUnit.getAcronym(), acronym)).findFirst().orElse(null);
    }

    public static List<Unit> readAllUnits() {
        return Bennu.getInstance().getPartysSet().stream().filter(Party::isUnit).map(Unit.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * @param path Acronyms path separated by character '>' (greater-than). The path start after
     *            institution unit and contains the parent acronyms.
     */
    public static Optional<Unit> findInternalUnitByAcronymPath(final String path) {
        return findUnitByAcronymPath(path, UnitUtils.readInstitutionUnit());
    }

    public static Optional<Unit> findUnitByAcronymPath(final String path, final Unit parentUnit) {
        final List<String> separatedPath = StringUtils.isNotBlank(path) ? List.of(path.split(">")) : List.of();

        Unit unit = parentUnit;

        for (String acronym : separatedPath) {
            unit = unit.getChildUnitByAcronym(acronym.trim());
            if (unit == null) {
                return Optional.empty();
            }
        }

        return Optional.of(unit);
    }

    public static Stream<Unit> findInternalUnitsByPartyType(final PartyType type) {
        return type == null ? Stream.empty() : type.getPartiesSet().stream().filter(Unit.class::isInstance).map(Unit.class::cast)
                .filter(Unit::isInternal);
    }

    public String getNameWithAcronym() {
        String name = getName().trim();
        return (getAcronym() == null || StringUtils.isEmpty(getAcronym().trim())) ? name : name + " (" + getAcronym().trim()
                + ")";
    }

    public String getPresentationName() {
        return getNameWithAcronym();
    }

    public String getPresentationNameWithParents() {
        String parentUnits = getParentUnitsPresentationName();
        return (!StringUtils.isEmpty(parentUnits.trim())) ? parentUnits + " > " + getPresentationName() : getPresentationName();
    }

    public String getParentUnitsPresentationName() {
        return getParentUnitsPresentationName(" > ");
    }

    public String getParentUnitsPresentationName(String separator) {
        return getParentUnitsPath().stream().filter(u -> !u.isAggregateUnit()).map(u -> u.getNameWithAcronym())
                .collect(Collectors.joining(separator));
    }

    public List<Unit> getParentUnitsPath() {
        return getParentUnitsPath(true);
    }

    private List<Unit> getParentUnitsPath(boolean addInstitutionalUnit) {

        List<Unit> parentUnits = new ArrayList<Unit>();
        Unit searchedUnit = this;
        Unit externalInstitutionUnit = UnitUtils.readExternalInstitutionUnit();
        Unit institutionUnit = UnitUtils.readInstitutionUnit();
        Unit earthUnit = UnitUtils.readEarthUnit();

        while (searchedUnit.getParentUnits().size() == 1) {
            Unit parentUnit = searchedUnit.getParentUnits().iterator().next();
            if (addInstitutionalUnit || parentUnit != institutionUnit) {
                parentUnits.add(0, parentUnit);
            }
            if (parentUnit != institutionUnit && parentUnit != externalInstitutionUnit && parentUnit != earthUnit) {
                searchedUnit = parentUnit;
                continue;
            }
            break;
        }

        if (searchedUnit.getParentUnits().size() > 1) {
            if (searchedUnit.isInternal() && addInstitutionalUnit) {
                parentUnits.add(0, institutionUnit);
            } else if (searchedUnit.isNoOfficialExternal()) {
                parentUnits.add(0, externalInstitutionUnit);
            } else {
                parentUnits.add(0, earthUnit);
            }
        }

        return parentUnits;
    }

    public LocalizedString getNameI18n() {
        return getPartyName();
    }

    @Override
    public String getPartyPresentationName() {
        return getPresentationNameWithParents();
    }

    static public LocalizedString getInstitutionName() {
        return Optional.ofNullable(getInstitutionUnit()).map(Unit::getNameI18n)
                .orElseGet(() -> BundleUtil.getLocalizedString(Bundle.GLOBAL, "error.institutionUnit.notconfigured"));
    }

    static public String getInstitutionAcronym() {
        return Optional.ofNullable(getInstitutionUnit()).map(Unit::getAcronym)
                .orElseGet(() -> BundleUtil.getString(Bundle.GLOBAL, "error.institutionUnit.notconfigured"));
    }

    @Override
    public Country getCountry() {
        return Optional.ofNullable(super.getCountry()).orElseGet(
                () -> getParentUnits().stream().map(Unit::getCountry).filter(Objects::nonNull).findFirst().orElse(null));
    }

    public boolean isOfficial() {
        return Boolean.TRUE.equals(getOfficial());
    }

    public void setOfficial(boolean official) {
        super.setOfficial(official);
    }

    @Override
    public boolean isAdministrativeOfficeUnit() {
        return getAdministrativeOffice() != null;
    }

    public boolean isSubUnitOf(final Collection<Unit> units) {
        return units.contains(this) || !Collections.disjoint(units, getAllParentUnits());
    }

    public static Unit getExternalInstitutionUnit() {
        return Bennu.getInstance().getExternalInstitutionUnit();
    }

    public static Unit getInstitutionUnit() {
        return Bennu.getInstance().getInstitutionUnit();
    }

    public static Unit getEarthUnit() {
        return Bennu.getInstance().getEarthUnit();
    }

    public static Optional<Unit> findExternalInstitutionUnitByName(final String name) {
        return Optional.ofNullable(getExternalInstitutionUnitByName(getExternalInstitutionUnit(), name));
    }

    private static Unit getExternalInstitutionUnitByName(final Unit unit, final String name) {
        return unit.getName().equals(name) ? unit : unit.getSubUnits().stream()
                .map(childUnit -> getExternalInstitutionUnitByName(childUnit, name)).findFirst().orElse(null);
    }

    public static Stream<Unit> findAllActiveUnitsByType(final PartyTypeEnum type) {
        final YearMonthDay now = new YearMonthDay();

        return PartyType.of(type).map(PartyType::getPartiesSet).stream().flatMap(Collection::stream).filter(Party::isUnit)
                .map(Unit.class::cast).filter(unit -> unit.isActive(now));
    }

    public List<Unit> getUnitFullPath(final List<AccountabilityTypeEnum> validAccountabilityTypes) {
        final Collection<Unit> parentUnits = getParentUnits(validAccountabilityTypes);
        if (parentUnits.isEmpty()) {
            return Collections.emptyList();
        }

        if (parentUnits.size() > 1) {
            throw new DomainException("error.unit.full.path.has.more.than.one.parent");
        }

        final List<Unit> result = parentUnits.iterator().next().getUnitFullPath(validAccountabilityTypes);
        result.add(this);
        return result;
    }

    public String getUnitFullPathName(final List<AccountabilityTypeEnum> validAccountabilityTypes) {
        if (this == getEarthUnit()) {
            return StringUtils.EMPTY;
        }
        final Collection<Unit> parentUnits = getParentUnits(validAccountabilityTypes);
        if (parentUnits.isEmpty()) {
            return getName();
        }

        if (parentUnits.size() > 1) {
            throw new DomainException("error.unit.full.path.has.more.than.one.parent");
        }

        final Unit parentUnit = parentUnits.iterator().next();
        return parentUnit.getUnitFullPathName(validAccountabilityTypes) + (parentUnit == getEarthUnit() ? "" : " > " + getName());
    }
}
