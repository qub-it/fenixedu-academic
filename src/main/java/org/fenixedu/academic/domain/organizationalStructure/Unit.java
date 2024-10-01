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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.util.email.UnitBasedSender;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;

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
    }

    @Override
    public Space getCampus() {

        Space campus = super.getCampus();

        if (campus != null) {
            return campus;
        }

        Collection<Unit> parentUnits = getParentUnits();
        if (parentUnits.size() == 1) {
            campus = parentUnits.iterator().next().getCampus();
        }

        return campus;
    }

    public boolean isInternal() {
        if (this.equals(UnitUtils.readInstitutionUnit())) {
            return true;
        }

        for (final Unit parentUnit : getParentUnits()) {
            if (parentUnit.isInternal()) {
                return true;
            }
        }

        return false;
    }

    public boolean isNoOfficialExternal() {
        if (this.equals(UnitUtils.readExternalInstitutionUnit())) {
            return true;
        }
        for (final Unit parentUnit : getParentUnits()) {
            if (parentUnit.isNoOfficialExternal()) {
                return true;
            }
        }
        return false;
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
        List<Unit> allSubUnits = new ArrayList<Unit>();
        for (Unit subUnit : this.getSubUnits()) {
            if (subUnit.isActive(currentDate) == state) {
                allSubUnits.add(subUnit);
            }
        }
        return allSubUnits;
    }

    private List<Unit> getAllInactiveSubUnits(YearMonthDay currentDate) {
        Set<Unit> allInactiveSubUnits = new HashSet<Unit>();
        allInactiveSubUnits.addAll(getInactiveSubUnits(currentDate));
        for (Unit subUnit : getSubUnits()) {
            allInactiveSubUnits.addAll(subUnit.getAllInactiveSubUnits(currentDate));
        }
        return new ArrayList<Unit>(allInactiveSubUnits);
    }

    private List<Unit> getAllActiveSubUnits(YearMonthDay currentDate) {
        Set<Unit> allActiveSubUnits = new HashSet<Unit>();
        allActiveSubUnits.addAll(getActiveSubUnits(currentDate));
        for (Unit subUnit : getSubUnits()) {
            allActiveSubUnits.addAll(subUnit.getAllActiveSubUnits(currentDate));
        }
        return new ArrayList<Unit>(allActiveSubUnits);
    }

    public List<Unit> getAllActiveSubUnitsWithAllowedChildParties(final YearMonthDay currentDate, final PartyType childType) {
        final Set<Unit> allActiveSubUnits = new HashSet<Unit>();
        allActiveSubUnits.addAll(getActiveSubUnitsWithAllowedChildParties(currentDate, childType));
        for (Unit subUnit : getSubUnits()) {
            allActiveSubUnits.addAll(subUnit.getAllActiveSubUnitsWithAllowedChildParties(currentDate, childType));
        }
        return new ArrayList<Unit>(allActiveSubUnits);
    }

    private List<Unit> getActiveSubUnitsWithAllowedChildParties(YearMonthDay currentDate, final PartyType childType) {
        final List<Unit> allSubUnits = new ArrayList<Unit>();
        for (Unit subUnit : this.getSubUnits()) {
            if (subUnit.isActive(currentDate) && subUnit.getAllowedChildPartyTypes(null).contains(childType)) {
                allSubUnits.add(subUnit);
            }
        }
        return allSubUnits;
    }

    public Collection<PartyType> getAllowedChildPartyTypes(final Boolean managedByUser) {
        if (isAggregateUnit()) {
            return getParentUnits().stream().flatMap(u -> u.getAllowedChildPartyTypes(managedByUser).stream())
                    .collect(Collectors.toSet());
        }
        return Optional.ofNullable(getPartyType()).map(pt -> pt.getAllowedChildPartyTypes(managedByUser))
                .orElseGet(() -> Set.of());
    }

    public Collection<Unit> getAllSubUnits() {
        Set<Unit> allSubUnits = new HashSet<Unit>();
        Collection<Unit> subUnits = getSubUnits();
        allSubUnits.addAll(subUnits);
        for (Unit subUnit : subUnits) {
            allSubUnits.addAll(subUnit.getAllSubUnits());
        }
        return allSubUnits;
    }

    public Collection<Unit> getAllParentUnits() {
        Set<Unit> allParentUnits = new HashSet<Unit>();
        Collection<Unit> parentUnits = getParentUnits();
        allParentUnits.addAll(parentUnits);
        for (Unit subUnit : parentUnits) {
            allParentUnits.addAll(subUnit.getAllParentUnits());
        }
        return allParentUnits;
    }

    public Collection<Unit> getSubUnits(List<AccountabilityTypeEnum> accountabilityTypeEnums) {
        return (Collection<Unit>) getChildParties(accountabilityTypeEnums, Unit.class);
    }

    public Collection<Unit> getSubUnits(final PartyTypeEnum type) {
        return (Collection<Unit>) getChildParties(type, Unit.class);
    }

    @Atomic
    /*
     * @See UnitMailSenderAction
     */
    public UnitBasedSender getOneUnitBasedSender() {
        if (!getUnitBasedSenderSet().isEmpty()) {
            return getUnitBasedSenderSet().iterator().next();
        } else {
            return UnitBasedSender.newInstance(this);
        }
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
        for (Unit subUnit : getSubUnits()) {
            if ((subUnit.getAcronym() != null) && (subUnit.getAcronym().equals(acronym))) {
                return subUnit;
            }
        }
        return null;
    }

    public static List<Unit> readAllUnits() {
        final List<Unit> allUnits = new ArrayList<Unit>();
        for (final Party party : Bennu.getInstance().getPartysSet()) {
            if (party.isUnit()) {
                allUnits.add((Unit) party);
            }
        }
        return allUnits;
    }

    /**
     * @param path Acronyms path separated by character '>' (greater-than). The path start after
     *            institution unit and contains the parent acronyms.
     */
    public static Optional<Unit> findInternalUnitByAcronymPath(final String path) {
        final List<String> separatedPath = StringUtils.isNotBlank(path) ? List.of(path.split(">")) : List.of();

        Unit unit = UnitUtils.readInstitutionUnit();

        for (String acronym : separatedPath) {
            unit = unit.getChildUnitByAcronym(acronym.trim());
            if (unit == null) {
                return Optional.empty();
            }
        }

        return Optional.of(unit);
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
        return (!StringUtils.isEmpty(parentUnits.trim())) ? parentUnits + " - " + getPresentationName() : getPresentationName();
    }

    public String getParentUnitsPresentationName() {
        return getParentUnitsPresentationName(" - ");
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

    /**
     * still used in: student/enrollment/bolonha/chooseExternalUnit.jsp
     * after that usage, method should be removed
     */
    @Deprecated
    public SortedSet<Unit> getSortedExternalChilds() {
        final SortedSet<Unit> result = new TreeSet<Unit>(Unit.COMPARATOR_BY_NAME_AND_ID);
        for (final Unit unit : getSubUnits()) {
            if (!unit.isInternal()) {
                result.add(unit);
            }
        }
        return result;
    }

    public LocalizedString getNameI18n() {
        return getPartyName();
    }

    @Override
    public String getPartyPresentationName() {
        return getPresentationNameWithParents();
    }

    static public LocalizedString getInstitutionName() {
        return Optional.ofNullable(Bennu.getInstance().getInstitutionUnit()).map(Unit::getNameI18n)
                .orElseGet(() -> BundleUtil.getLocalizedString(Bundle.GLOBAL, "error.institutionUnit.notconfigured"));
    }

    static public String getInstitutionAcronym() {
        return Optional.ofNullable(Bennu.getInstance().getInstitutionUnit()).map(Unit::getAcronym)
                .orElseGet(() -> BundleUtil.getString(Bundle.GLOBAL, "error.institutionUnit.notconfigured"));
    }

    @Override
    public Country getCountry() {
        if (super.getCountry() != null) {
            return super.getCountry();
        }
        for (final Unit unit : getParentUnits()) {
            final Country country = unit.getCountry();
            if (country != null) {
                return country;
            }
        }
        return null;
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
}
