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
package org.fenixedu.academic.domain.contacts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.joda.time.DateTime;

public class PhysicalAddress extends PhysicalAddress_Base {

    public static final Comparator<PhysicalAddress> COMPARATOR_BY_ADDRESS =
            Comparator.comparing(PhysicalAddress::getAddress, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(COMPARATOR_BY_TYPE);

    public static PhysicalAddress create(Party party, PhysicalAddressData data, PartyContactType type, boolean isDefault,
            boolean hasCheckRules) {
        PhysicalAddress address = new PhysicalAddress();
        address.init(party, type, isDefault);
        address.setVisibleToPublic(Boolean.FALSE);
        address.setVisibleToStudents(Boolean.FALSE);
        address.setVisibleToStaff(Boolean.FALSE);
        address.edit(data, hasCheckRules);

        if (hasCheckRules) {
            address.checkRules();
        }
        return address;
    }

    @Deprecated(forRemoval = true)
    static public PhysicalAddress createPhysicalAddress(final Party party, final PhysicalAddressData data,
            final PartyContactType type, final Boolean isDefault) {
        return new PhysicalAddress(party, type, isDefault, data);
    }

    protected PhysicalAddress() {
        super();
        new PhysicalAddressValidation(this);
    }

    @Deprecated(forRemoval = true)
    protected PhysicalAddress(final Party party, final PartyContactType type, final boolean defaultContact,
            final PhysicalAddressData data) {
        this(party, type, defaultContact, data, true);
    }

    @Deprecated(forRemoval = true)
    protected PhysicalAddress(final Party party, final PartyContactType type, final boolean defaultContact,
            final PhysicalAddressData data, final boolean hasCheckRules) {
        this();
        super.init(party, type, defaultContact);
        setVisibleToPublic(Boolean.FALSE);
        setVisibleToStudents(Boolean.FALSE);
        setVisibleToStaff(Boolean.FALSE);
        edit(data, hasCheckRules);

        if (hasCheckRules) {
            checkRules();
        }
    }

    // Called from renders with constructor clause.
    @Deprecated(forRemoval = true)
    public PhysicalAddress(final Party party, final PartyContactType type, final Boolean defaultContact, final String address,
            final String areaCode, final String areaOfAreaCode, final String area, final String parishOfResidence,
            final String districtSubdivisionOfResidence, final String districtOfResidence, final Country countryOfResidence) {
        this(party, type, defaultContact.booleanValue(),
                new PhysicalAddressData(address, areaCode, areaOfAreaCode, area, parishOfResidence,
                        districtSubdivisionOfResidence, districtOfResidence, countryOfResidence));

        checkRules();
    }

    public void edit(final PhysicalAddressData data) {
        edit(data, true);
    }

    protected void edit(final PhysicalAddressData data, final boolean hasCheckRules) {
        if (data == null) {
            return;
        }

        if (!data.equals(new PhysicalAddressData(this))) {
            if (isFiscalAddress() && getCountryOfResidence() != data.getCountryOfResidence()) {
                throw new DomainException("error.PhysicalAddress.cannot.change.countryOfResidence.in.fiscal.address");
            }

            super.setAddress(data.getAddress());
            super.setAreaCode(data.getAreaCode());
            super.setAreaOfAreaCode(data.getAreaOfAreaCode());
            super.setArea(data.getArea());
            super.setParishOfResidence(data.getParishOfResidence());
            super.setDistrictSubdivisionOfResidence(data.getDistrictSubdivisionOfResidence());
            super.setDistrictOfResidence(data.getDistrictOfResidence());
            super.setCountryOfResidence(data.getCountryOfResidence());

            if (!waitsValidation()) {
                new PhysicalAddressValidation(this);
            }
            setLastModifiedDate(new DateTime());
        }

        if (hasCheckRules) {
            checkRules();
        }
    }

    // Called from renders with edit clause.
    public void edit(final PartyContactType type, final Boolean defaultContact, final String address, final String areaCode,
            final String areaOfAreaCode, final String area, final String parishOfResidence,
            final String districtSubdivisionOfResidence, final String districtOfResidence, final Country countryOfResidence) {
        super.edit(type, defaultContact);
        edit(new PhysicalAddressData(address, areaCode, areaOfAreaCode, area, parishOfResidence, districtSubdivisionOfResidence,
                districtOfResidence, countryOfResidence));

        checkRules();
    }

    private void checkRules() {
        if (getCountryOfResidence() == null) {
            throw new DomainException("error.PhysicalAddres.countryOfResidence.required");
        }
    }

    @Override
    public String getPresentationValue() {
        List<String> addressParts = new ArrayList<>();

        if (StringUtils.isNotBlank(getAddress())) {
            addressParts.add(getAddress());
        }

        if (StringUtils.isNotBlank(getPostalCode())) {
            addressParts.add(getPostalCode().trim());
        }

        Country country = getCountryOfResidence();
        if (country != null) {
            if (!country.isDefaultCountry() && StringUtils.isNotBlank(getDistrictSubdivisionOfResidence())) {
                addressParts.add(getDistrictSubdivisionOfResidence());
            }

            addressParts.add(country.getLocalizedName().getContent());
        }

        return String.join(", ", addressParts);
    }

    @Override
    public boolean isPhysicalAddress() {
        return true;
    }

    public String getCountryOfResidenceName() {
        return getCountryOfResidence() != null ? getCountryOfResidence().getName() : StringUtils.EMPTY;
    }

    @Override
    public void deleteWithoutCheckRules() {
        if (getParty().getFiscalAddress() == this) {
            throw new DomainException("error.domain.contacts.PhysicalAddress.cannot.remove.fiscal.address");
        }

        setCountryOfResidence(null);
        super.deleteWithoutCheckRules();
    }

    @Override
    public void delete() {
        if (getParty().getFiscalAddress() == this) {
            throw new DomainException("error.domain.contacts.PhysicalAddress.cannot.remove.fiscal.address");
        }

        setCountryOfResidence(null);
        super.delete();
    }

    public String getPostalCode() {
        List<String> postalCode = new ArrayList<>();

        if (StringUtils.isNotBlank(getAreaCode())) {
            postalCode.add(getAreaCode());
        }

        if (StringUtils.isNotBlank(getAreaOfAreaCode())) {
            postalCode.add(getAreaOfAreaCode());
        }

        return String.join(" ", postalCode);
    }

    @Override
    public boolean hasValue(final String value) {
        return false;
    }

    @Override
    public void logCreate(final Person person) {
        logCreateAux(person, "label.partyContacts.PhysicalAddress");
    }

    @Override
    public void logEdit(final Person person, final boolean propertiesChanged, final boolean valueChanged,
            final boolean createdNewContact, final String newValue) {
        logEditAux(person, propertiesChanged, valueChanged, createdNewContact, newValue, "label.partyContacts.PhysicalAddress");
    }

    @Override
    public void logDelete(final Person person) {
        logDeleteAux(person, "label.partyContacts.PhysicalAddress");
    }

    @Override
    public void logValid(final Person person) {
        logValidAux(person, "label.partyContacts.PhysicalAddress");
    }

    @Override
    public void logRefuse(final Person person) {
        logRefuseAux(person, "label.partyContacts.PhysicalAddress");
    }

    public boolean isFiscalAddress() {
        return Boolean.TRUE.equals(super.getFiscalAddress());
    }

    /**
     * @deprecated use {@link #getPresentationValue()}
     */
    @Deprecated
    public String getUiFiscalPresentationValue() {
        return getPresentationValue();
    }
}