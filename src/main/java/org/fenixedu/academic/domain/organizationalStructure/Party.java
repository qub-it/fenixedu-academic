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
 * Created on Feb 10, 2006
 *	by mrsp
 */
package org.fenixedu.academic.domain.organizationalStructure;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.contacts.PhysicalAddressData;
import org.fenixedu.academic.domain.contacts.WebAddress;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;

public abstract class Party extends Party_Base implements Comparable<Party> {

    static final public Comparator<Party> COMPARATOR_BY_NAME = new Comparator<Party>() {
        @Override
        public int compare(final Party o1, final Party o2) {
            return Collator.getInstance().compare(o1.getName(), o2.getName());
        }
    };

    static final public Comparator<Party> COMPARATOR_BY_SUBPARTY = new Comparator<Party>() {
        @Override
        public int compare(final Party o1, final Party o2) {
            if ((o1 instanceof Person) && (o2 instanceof Unit)) {
                return 1;
            } else if ((o1 instanceof Unit) && (o2 instanceof Person)) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    static final public Comparator<Party> COMPARATOR_BY_NAME_AND_ID = new Comparator<Party>() {
        @Override
        public int compare(final Party o1, final Party o2) {
            final ComparatorChain comparatorChain = new ComparatorChain();
            comparatorChain.addComparator(Party.COMPARATOR_BY_NAME);
            comparatorChain.addComparator(DomainObjectUtil.COMPARATOR_BY_ID);

            return comparatorChain.compare(o1, o2);
        }
    };

    static final public Comparator<Party> COMPARATOR_BY_SUBPARTY_AND_NAME_AND_ID = new Comparator<Party>() {
        @Override
        public int compare(final Party o1, final Party o2) {
            final ComparatorChain comparatorChain = new ComparatorChain();
            comparatorChain.addComparator(Party.COMPARATOR_BY_SUBPARTY);
            comparatorChain.addComparator(Party.COMPARATOR_BY_NAME);
            comparatorChain.addComparator(DomainObjectUtil.COMPARATOR_BY_ID);
            return comparatorChain.compare(o1, o2);
        }
    };

    public abstract String getPartyPresentationName();

    public abstract LocalizedString getPartyName();

    public abstract String getName();

    public Party() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    @Deprecated
    @Override
    final public Country getNationality() {
        return getCountry();
    }

    @Deprecated
    @Override
    public void setNationality(final Country country) {
        setCountry(country);
    }

    public Country getCountry() {
        return super.getNationality();
    }

    public void setCountry(final Country country) {
        super.setNationality(country);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Party> Set<T> getPartysSet(Class<T> input) {
        return Bennu.getInstance().getPartysSet().stream().filter(p -> input.isAssignableFrom(p.getClass())).map(p -> (T) p)
                .collect(Collectors.toSet());
    }

    public PartyTypeEnum getType() {
        return getPartyType() != null ? getPartyType().getType() : null;
    }

    public void setType(PartyTypeEnum partyTypeEnum) {
        if (partyTypeEnum != null) {
            PartyType partyType = PartyType.readPartyTypeByType(partyTypeEnum);
            if (partyType == null) {
                throw new DomainException("error.Party.unknown.partyType");
            }
            setPartyType(partyType);
        } else {
            setPartyType(null);
        }
    }

    private Collection<? extends Party> getParentParties(AccountabilityTypeEnum accountabilityTypeEnum,
            Class<? extends Party> parentPartyClass) {
        final Set<Party> result = new HashSet<Party>();
        for (final Accountability accountability : getParentsSet()) {
            if (accountability.getAccountabilityType().getType() == accountabilityTypeEnum
                    && parentPartyClass.isAssignableFrom(accountability.getParentParty().getClass())) {
                result.add(accountability.getParentParty());
            }
        }
        return result;
    }

    private Collection<? extends Party> getParentParties(Class<? extends Party> parentPartyClass) {
        final Set<Party> result = new HashSet<Party>();
        for (final Accountability accountability : getParentsSet()) {
            if (parentPartyClass.isAssignableFrom(accountability.getParentParty().getClass())) {
                result.add(accountability.getParentParty());
            }
        }
        return result;
    }

    private Collection<? extends Party> getParentParties(List<AccountabilityTypeEnum> accountabilityTypeEnums,
            Class<? extends Party> parentPartyClass) {
        final Set<Party> result = new HashSet<Party>();
        for (final Accountability accountability : getParentsSet()) {
            if (accountabilityTypeEnums.contains(accountability.getAccountabilityType().getType())
                    && parentPartyClass.isAssignableFrom(accountability.getParentParty().getClass())) {
                result.add(accountability.getParentParty());
            }
        }
        return result;
    }

    public Collection<Unit> getParentUnits() {
        return (Collection<Unit>) getParentParties(Unit.class);
    }

    public Collection<Unit> getParentUnits(AccountabilityTypeEnum accountabilityTypeEnum) {
        return (Collection<Unit>) getParentParties(accountabilityTypeEnum, Unit.class);
    }

    public Collection<Unit> getParentUnits(List<AccountabilityTypeEnum> accountabilityTypeEnums) {
        return (Collection<Unit>) getParentParties(accountabilityTypeEnums, Unit.class);
    }

    public Collection<Unit> getSubUnits() {
        return (Collection<Unit>) getChildParties(Unit.class);
    }

    private Collection<? extends Party> getChildParties(Class<? extends Party> childPartyClass) {
        final Set<Party> result = new HashSet<Party>();
        for (final Accountability accountability : getChildsSet()) {
            if (childPartyClass.isAssignableFrom(accountability.getChildParty().getClass())) {
                result.add(accountability.getChildParty());
            }
        }
        return result;
    }

    protected Collection<? extends Party> getChildParties(List<AccountabilityTypeEnum> accountabilityTypeEnums,
            Class<? extends Party> childPartyClass) {
        final Set<Party> result = new HashSet<Party>();
        for (final Accountability accountability : getChildsSet()) {
            if (accountabilityTypeEnums.contains(accountability.getAccountabilityType().getType())
                    && childPartyClass.isAssignableFrom(accountability.getChildParty().getClass())) {
                result.add(accountability.getChildParty());
            }
        }
        return result;
    }

    protected Collection<? extends Party> getChildParties(PartyTypeEnum type, Class<? extends Party> childPartyClass) {
        final Set<Party> result = new HashSet<Party>();
        for (final Accountability accountability : getChildsSet()) {
            if (accountability.getChildParty().getType() == type
                    && childPartyClass.isAssignableFrom(accountability.getChildParty().getClass())) {
                result.add(accountability.getChildParty());
            }
        }
        return result;
    }

    public Collection<? extends Accountability> getChildAccountabilities(AccountabilityTypeEnum accountabilityTypeEnum) {
        final Set<Accountability> result = new HashSet<Accountability>();
        for (final Accountability accountability : getChildsSet()) {
            if (accountability.getAccountabilityType().getType() == accountabilityTypeEnum) {
                result.add(accountability);
            }
        }
        return result;
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        for (; !getPartyContactsSet().isEmpty(); getPartyContactsSet().iterator().next().deleteWithoutCheckRules()) {
            ;
        }

        if (getPartySocialSecurityNumber() != null) {
            getPartySocialSecurityNumber().delete();
        }

        super.setNationality(null);
        super.setPartyType(null);
        super.setRootDomainObject(null);

        deleteDomainObject();
    }

    public static Party readByContributorNumber(String contributorNumber) {
        return PartySocialSecurityNumber.readPartyBySocialSecurityNumber(contributorNumber);
    }

    @Deprecated
    public Country getFiscalCountry() {
        return getPartySocialSecurityNumber() != null ? getPartySocialSecurityNumber().getFiscalCountry() : null;
    }

    public void setFiscalCountry(final Country value) {
        throw new RuntimeException("use editSocialSecurityNumber");
    }

    public String getSocialSecurityNumber() {
        return getPartySocialSecurityNumber() != null ? getPartySocialSecurityNumber().getSocialSecurityNumber() : null;
    }

    public void setSocialSecurityNumber(final String value) {
        throw new RuntimeException("use editSocialSecurityNumber");
    }

    public void editSocialSecurityNumber(final String socialSecurityNumber, final PhysicalAddress fiscalAddress) {
        PartySocialSecurityNumber.editFiscalInformation(this, socialSecurityNumber, fiscalAddress);
    }

    public boolean isPerson() {
        return false;
    }

    public boolean isUnit() {
        return false;
    }

    public boolean isDepartmentUnit() {
        return PartyTypeEnum.DEPARTMENT == getType();
    }

    public boolean isCompetenceCourseGroupUnit() {
        return PartyTypeEnum.COMPETENCE_COURSE_GROUP == getType();
    }

    public boolean isScientificAreaUnit() {
        return PartyTypeEnum.SCIENTIFIC_AREA == getType();
    }

    public boolean isAdministrativeOfficeUnit() {
        return PartyTypeEnum.ADMINISTRATIVE_OFFICE_UNIT == getType();
    }

    public boolean isDegreeUnit() {
        return PartyTypeEnum.DEGREE_UNIT == getType();
    }

    public boolean isAcademicalUnit() {
        return isSchoolUnit() || isUniversityUnit();
    }

    public boolean isSchoolUnit() {
        return PartyTypeEnum.SCHOOL == getType();
    }

    public boolean isUniversityUnit() {
        return PartyTypeEnum.UNIVERSITY == getType();
    }

    public boolean isPlanetUnit() {
        return PartyTypeEnum.PLANET == getType();
    }

    public boolean isCountryUnit() {
        return PartyTypeEnum.COUNTRY == getType();
    }

    public boolean isAggregateUnit() {
        return PartyTypeEnum.AGGREGATE_UNIT == getType();
    }

    public boolean verifyNameEquality(String[] nameWords) {
        if (nameWords == null) {
            return true;
        }
        if (getName() != null) {
            String[] personNameWords = StringNormalizer.normalize(getName()).trim().split(" ");
            int j, i;
            for (i = 0; i < nameWords.length; i++) {
                if (!nameWords[i].equals("")) {
                    for (j = 0; j < personNameWords.length; j++) {
                        if (personNameWords[j].equals(nameWords[i])) {
                            break;
                        }
                    }
                    if (j == personNameWords.length) {
                        return false;
                    }
                }
            }
            if (i == nameWords.length) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPartyContact(final Class<? extends PartyContact> clazz, final PartyContactType type, final String value) {
        final List<? extends PartyContact> allPartyContacts = getPartyContacts(clazz, type);
        for (PartyContact contact : allPartyContacts) {
            if (contact.hasValue(value)) {
                return true;
            }
        }
        return false;
    }

    public List<? extends PartyContact> getAllPartyContacts(final Class<? extends PartyContact> clazz,
            final PartyContactType type) {
        final List<PartyContact> result = new ArrayList<PartyContact>();
        for (final PartyContact contact : getPartyContactsSet()) {
            if (clazz.isAssignableFrom(contact.getClass()) && (type == null || contact.getType() == type)) {
                result.add(contact);
            }
        }
        return result;
    }

    public List<? extends PartyContact> getAllPartyContacts(final Class<? extends PartyContact> clazz) {
        return getAllPartyContacts(clazz, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends PartyContact> Stream<T> getPartyContactStream(final Class<T> clazz, final PartyContactType type) {
        final Stream<PartyContact> stream = getPartyContactsSet().stream();
        return (Stream) stream.filter(
                c -> clazz.isAssignableFrom(c.getClass()) && (type == null || c.getType() == type) && c.isActiveAndValid());
    }

    public List<? extends PartyContact> getPartyContacts(final Class<? extends PartyContact> clazz, final PartyContactType type) {
        final List<PartyContact> result = new ArrayList<PartyContact>();
        for (final PartyContact contact : getPartyContactsSet()) {
            if (clazz.isAssignableFrom(contact.getClass()) && (type == null || contact.getType() == type)
                    && contact.isActiveAndValid()) {
                result.add(contact);
            }
        }
        return result;
    }

    public List<? extends PartyContact> getPendingOrValidPartyContacts(final Class<? extends PartyContact> clazz,
            final PartyContactType type) {
        final List<PartyContact> result = new ArrayList<PartyContact>();
        for (final PartyContact contact : getPartyContactsSet()) {
            if (clazz.isAssignableFrom(contact.getClass()) && (type == null || contact.getType() == type)
                    && (contact.isActiveAndValid() || contact.waitsValidation())) {
                result.add(contact);
            }
        }
        return result;
    }

    public List<? extends PartyContact> getPendingOrValidPartyContacts(final Class<? extends PartyContact> clazz) {
        return getPendingOrValidPartyContacts(clazz, null);
    }

    public List<? extends PartyContact> getPendingPartyContacts(final Class<? extends PartyContact> clazz,
            final PartyContactType type) {
        final List<PartyContact> result = new ArrayList<PartyContact>();
        for (final PartyContact contact : getPartyContactsSet()) {
            if (clazz.isAssignableFrom(contact.getClass()) && (type == null || contact.getType() == type)
                    && contact.waitsValidation()) {
                result.add(contact);
            }
        }
        return result;
    }

    public List<? extends PartyContact> getAllPendingPartyContacts() {
        final List<PartyContact> result = new ArrayList<PartyContact>();
        for (final PartyContact contact : getPartyContactsSet()) {
            if (contact.waitsValidation()) {
                result.add(contact);
            }
        }
        return result;
    }

    public <T extends PartyContact> Stream<T> getPartyContactStream(final Class<T> clazz) {
        return getPartyContactStream(clazz, null);
    }

    public List<? extends PartyContact> getPartyContacts(final Class<? extends PartyContact> clazz) {
        return getPartyContacts(clazz, null);
    }

    public List<? extends PartyContact> getPendingPartyContacts(final Class<? extends PartyContact> clazz) {
        return getPendingPartyContacts(clazz, null);
    }

    public boolean hasPendingPartyContacts(final Class<? extends PartyContact> clazz) {
        return getPendingPartyContacts(clazz, null).size() > 0;
    }

    public boolean hasPendingPartyContacts() {
        return getAllPendingPartyContacts().size() > 0;
    }

    public boolean hasAnyPartyContact(final Class<? extends PartyContact> clazz, final PartyContactType type) {
        for (final PartyContact contact : getPartyContactsSet()) {
            if (clazz.isAssignableFrom(contact.getClass()) && (type == null || contact.getType() == type)
                    && contact.isActiveAndValid()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyPartyContact(final Class<? extends PartyContact> clazz) {
        return hasAnyPartyContact(clazz, null);
    }

    public PartyContact getDefaultPartyContact(final Class<? extends PartyContact> clazz) {
        for (final PartyContact contact : getPartyContactsSet()) {
            if (clazz.isAssignableFrom(contact.getClass()) && contact.isDefault() && contact.isActiveAndValid()) {
                return contact;
            }
        }
        return null;
    }

    public boolean hasDefaultPartyContact(final Class<? extends PartyContact> clazz) {
        return getDefaultPartyContact(clazz) != null;
    }

    public PartyContact getInstitutionalPartyContact(final Class<? extends PartyContact> clazz) {
        List<EmailAddress> institutionals =
                (List<EmailAddress>) getPartyContacts(EmailAddress.class, PartyContactType.INSTITUTIONAL);
        return institutionals.isEmpty() ? null : institutionals.iterator().next();
    }

    public boolean hasInstitutionalPartyContact(final Class<? extends PartyContact> clazz) {
        return getInstitutionalPartyContact(clazz) != null;
    }

    /*
     * WebAddress
     */
    public List<WebAddress> getWebAddresses() {
        return (List<WebAddress>) getPartyContacts(WebAddress.class);
    }

    public List<WebAddress> getPendingWebAddresses() {
        return (List<WebAddress>) getPendingPartyContacts(WebAddress.class);
    }

    public boolean hasDefaultWebAddress() {
        return hasDefaultPartyContact(WebAddress.class);
    }

    public WebAddress getDefaultWebAddress() {
        return (WebAddress) getDefaultPartyContact(WebAddress.class);
    }

    public String getDefaultWebAddressUrl() {
        return hasDefaultWebAddress() ? getDefaultWebAddress().getUrl() : StringUtils.EMPTY;
    }

    public void setDefaultWebAddressUrl(final String url) {
        if (hasDefaultWebAddress()) {
            getDefaultWebAddress().edit(url);
        } else {
            WebAddress.createWebAddress(this, url, PartyContactType.PERSONAL, true);
        }
    }

    /**
     * @deprecated {@link #getDefaultWebAddressUrl()}
     */
    @Deprecated
    public String getWebAddress() {
        return getDefaultWebAddressUrl();
    }

    /**
     * @deprecated {@link #setDefaultWebAddressUrl(String)}
     */
    @Deprecated
    public void setWebAddress(String webAddress) {
        setDefaultWebAddressUrl(webAddress);
    }

    /*
     * Phone
     */
    public List<Phone> getPhones() {
        return (List<Phone>) getPartyContacts(Phone.class);
    }

    public List<Phone> getPendingPhones() {
        return (List<Phone>) getPendingPartyContacts(Phone.class);
    }

    public List<Phone> getPendingOrValidPhones() {
        return (List<Phone>) getPendingOrValidPartyContacts(Phone.class);
    }

    public boolean hasDefaultPhone() {
        return hasDefaultPartyContact(Phone.class);
    }

    public Phone getDefaultPhone() {
        return (Phone) getDefaultPartyContact(Phone.class);
    }

    public String getDefaultPhoneNumber() {
        return hasDefaultPhone() ? getDefaultPhone().getNumber() : StringUtils.EMPTY;
    }

    public void setDefaultPhoneNumber(final String number) {
        setDefaultPhoneNumber(number, false);
    }

    public void setDefaultPhoneNumber(final String number, boolean valid) {
        final Phone defaultPhone;
        if (hasDefaultPhone()) {
            defaultPhone = getDefaultPhone();
            defaultPhone.edit(number);
        } else {
            defaultPhone = Phone.createPhone(this, number, PartyContactType.PERSONAL, true);
        }

        if (valid) {
            defaultPhone.setValid();
        }
    }

    /**
     * This should not be used because assumes that there is only one work phone.
     */
    @Deprecated
    public void setWorkPhoneNumber(final String number) {
        if (hasAnyPartyContact(Phone.class, PartyContactType.WORK)) {
            ((Phone) getPartyContacts(Phone.class, PartyContactType.WORK).iterator().next()).edit(number);
        } else {
            Phone.createPhone(this, number, PartyContactType.WORK, false);
        }
    }

    /**
     * @deprecated {@link #getDefaultPhoneNumber()}
     */
    @Deprecated
    public String getPhone() {
        return getDefaultPhoneNumber();
    }

    /**
     * @deprecated {@link #setDefaultPhoneNumber(String)}
     */
    @Deprecated
    public void setPhone(String phone) {
        setDefaultPhoneNumber(phone);
    }

    // Currently, a Person can only have one WorkPhone (so use get(0) -
    // after
    // interface updates remove these methods)
    public Phone getPersonWorkPhone() {
        final List<Phone> partyContacts = (List<Phone>) getPartyContacts(Phone.class, PartyContactType.WORK);
        // actually exists only one
        return partyContacts.isEmpty() ? null : (Phone) partyContacts.iterator().next();
    }

    @Deprecated
    public String getWorkPhone() {
        final Phone workPhone = getPersonWorkPhone();
        return workPhone != null ? workPhone.getNumber() : null;
    }

    @Deprecated
    public void setWorkPhone(String workPhone) {
        setWorkPhoneNumber(workPhone);
    }

    /*
     * MobilePhone
     */
    public List<MobilePhone> getMobilePhones() {
        return (List<MobilePhone>) getPartyContacts(MobilePhone.class);
    }

    public List<MobilePhone> getPendingMobilePhones() {
        return (List<MobilePhone>) getPendingPartyContacts(MobilePhone.class);
    }

    public List<MobilePhone> getPendingOrValidMobilePhones() {
        return (List<MobilePhone>) getPendingOrValidPartyContacts(MobilePhone.class);
    }

    public boolean hasDefaultMobilePhone() {
        return hasDefaultPartyContact(MobilePhone.class);
    }

    public MobilePhone getDefaultMobilePhone() {
        return (MobilePhone) getDefaultPartyContact(MobilePhone.class);
    }

    public String getDefaultMobilePhoneNumber() {
        return hasDefaultMobilePhone() ? getDefaultMobilePhone().getNumber() : StringUtils.EMPTY;
    }

    public void setDefaultMobilePhoneNumber(final String number) {
        setDefaultMobilePhoneNumber(number, false);
    }

    public void setDefaultMobilePhoneNumber(final String number, final boolean valid) {
        MobilePhone mobilePhone;
        if (hasDefaultMobilePhone()) {
            mobilePhone = getDefaultMobilePhone();
            mobilePhone.edit(number);
        } else {
            mobilePhone = MobilePhone.createMobilePhone(this, number, PartyContactType.PERSONAL, true);
        }

        if (valid) {
            mobilePhone.setValid();
        }
    }

    /**
     * @deprecated {@link getDefaultMobilePhoneNumber}
     */
    @Deprecated
    public String getMobile() {
        return getDefaultMobilePhoneNumber();
    }

    /**
     * @deprecated {@link setDefaultMobilePhoneNumber}
     */
    @Deprecated
    public void setMobile(String mobile) {
        setDefaultMobilePhoneNumber(mobile);
    }

    /*
     * EmailAddress
     */
    public Stream<EmailAddress> getEmailAddressStream() {
        return getPartyContactStream(EmailAddress.class);
    }

    /**
     * @deprecated Use {@link getEmailAddressStream} instead
     */
    @Deprecated
    public List<EmailAddress> getEmailAddresses() {
        return (List<EmailAddress>) getPartyContacts(EmailAddress.class);
    }

    public List<EmailAddress> getPendingEmailAddresses() {
        return (List<EmailAddress>) getPendingPartyContacts(EmailAddress.class);
    }

    public List<EmailAddress> getPendingOrValidEmailAddresses() {
        return (List<EmailAddress>) getPendingOrValidPartyContacts(EmailAddress.class);
    }

    public boolean hasDefaultEmailAddress() {
        return hasDefaultPartyContact(EmailAddress.class);
    }

    public EmailAddress getDefaultEmailAddress() {
        return (EmailAddress) getDefaultPartyContact(EmailAddress.class);
    }

    public boolean hasInstitutionalEmailAddress() {
        return hasInstitutionalPartyContact(EmailAddress.class);
    }

    public EmailAddress getInstitutionalEmailAddress() {
        return (EmailAddress) getInstitutionalPartyContact(EmailAddress.class);
    }

    public EmailAddress getInstitutionalOrDefaultEmailAddress() {
        return hasInstitutionalEmailAddress() ? getInstitutionalEmailAddress() : getDefaultEmailAddress();
    }

    public String getDefaultEmailAddressValue() {
        return hasDefaultEmailAddress() ? getDefaultEmailAddress().getValue() : StringUtils.EMPTY;
    }

    public void setDefaultEmailAddressValue(final String email) {
        setDefaultEmailAddressValue(email, false, false);
    }

    public void setDefaultEmailAddressValue(final String email, final boolean valid) {
        setDefaultEmailAddressValue(email, valid, false);
    }

    public void setDefaultEmailAddressValue(final String email, final boolean valid, final boolean visibleToPublic) {
        if (!StringUtils.isEmpty(email)) {
            final EmailAddress emailAddress;
            if (hasDefaultEmailAddress()) {
                emailAddress = getDefaultEmailAddress();
                emailAddress.edit(email);
            } else {
                emailAddress = EmailAddress.createEmailAddress(this, email, PartyContactType.PERSONAL, true);
            }
            emailAddress.setVisibleToPublic(visibleToPublic);
            if (valid) {
                emailAddress.setValid();
            }
        }
    }

    public String getInstitutionalEmailAddressValue() {
        return hasInstitutionalEmailAddress() ? getInstitutionalEmailAddress().getValue() : StringUtils.EMPTY;
    }

    public void setInstitutionalEmailAddressValue(final String email) {
        if (hasInstitutionalEmailAddress()) {
            getInstitutionalEmailAddress().setValue(email);
        } else {
            EmailAddress emailAddress = EmailAddress.createEmailAddress(this, email, PartyContactType.INSTITUTIONAL, false);
            emailAddress.setValid();
        }
    }

    public String getInstitutionalOrDefaultEmailAddressValue() {
        EmailAddress email = getInstitutionalOrDefaultEmailAddress();
        return (email != null ? email.getValue() : StringUtils.EMPTY);
    }

    /**
     * @deprecated {@link #getDefaultEmailAddressValue()}
     */
    @Deprecated
    public String getEmail() {
        return getDefaultEmailAddressValue();
    }

    /**
     * @deprecated {@link #setDefaultEmailAddressValue(String)}
     */
    @Deprecated
    public void setEmail(String email) {
        setDefaultEmailAddressValue(email);
    }

    /*
     * PhysicalAddress
     */
    public List<PhysicalAddress> getPhysicalAddresses() {
        return (List<PhysicalAddress>) getPartyContacts(PhysicalAddress.class);
    }

    public List<PhysicalAddress> getPendingPhysicalAddresses() {
        return (List<PhysicalAddress>) getPendingPartyContacts(PhysicalAddress.class);
    }

    public List<PhysicalAddress> getPendingOrValidPhysicalAddresses() {
        return (List<PhysicalAddress>) getPendingOrValidPartyContacts(PhysicalAddress.class);
    }

    public List<PhysicalAddress> getValidAddressesForFiscalData() {
        return getPendingOrValidPartyContacts(PhysicalAddress.class).stream().map(PhysicalAddress.class::cast)
                .filter(pa -> pa.isActiveAndValid()).filter(pa -> pa.getCountryOfResidence() != null)
                .filter(pa -> pa.getCurrentPartyContact() == null).collect(Collectors.toList());
    }

    public boolean hasDefaultPhysicalAddress() {
        return hasDefaultPartyContact(PhysicalAddress.class);
    }

    public PhysicalAddress getDefaultPhysicalAddress() {
        return (PhysicalAddress) getDefaultPartyContact(PhysicalAddress.class);
    }

    public void setDefaultPhysicalAddressData(final PhysicalAddressData data) {
        setDefaultPhysicalAddressData(data, false);
    }

    public void setDefaultPhysicalAddressData(final PhysicalAddressData data, final boolean valid) {
        PhysicalAddress defaultPhysicalAddress;
        if (hasDefaultPhysicalAddress()) {
            defaultPhysicalAddress = getDefaultPhysicalAddress();
            defaultPhysicalAddress.edit(data);
        } else {
            defaultPhysicalAddress = PhysicalAddress.createPhysicalAddress(this, data, PartyContactType.PERSONAL, true);
        }
        if (valid) {
            defaultPhysicalAddress.setValid();
        }
    }

    private PhysicalAddress getOrCreateDefaultPhysicalAddress() {
        final PhysicalAddress physicalAdress = getDefaultPhysicalAddress();
        return physicalAdress != null ? physicalAdress : PhysicalAddress.createPhysicalAddress(this, null,
                PartyContactType.PERSONAL, true);
    }

    public String getAddress() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getAddress() : StringUtils.EMPTY;
    }

    public void setAddress(String address) {
        getOrCreateDefaultPhysicalAddress().setAddress(address);
    }

    public String getAreaCode() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getAreaCode() : StringUtils.EMPTY;
    }

    public void setAreaCode(String areaCode) {
        getOrCreateDefaultPhysicalAddress().setAreaCode(areaCode);
    }

    public String getAreaOfAreaCode() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getAreaOfAreaCode() : StringUtils.EMPTY;
    }

    public void setAreaOfAreaCode(String areaOfAreaCode) {
        getOrCreateDefaultPhysicalAddress().setAreaOfAreaCode(areaOfAreaCode);
    }

    public String getPostalCode() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getPostalCode() : StringUtils.EMPTY;
    }

    public String getArea() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getArea() : StringUtils.EMPTY;
    }

    public void setArea(String area) {
        getOrCreateDefaultPhysicalAddress().setArea(area);
    }

    public String getParishOfResidence() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getParishOfResidence() : StringUtils.EMPTY;
    }

    public void setParishOfResidence(String parishOfResidence) {
        getOrCreateDefaultPhysicalAddress().setParishOfResidence(parishOfResidence);
    }

    public String getDistrictSubdivisionOfResidence() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getDistrictSubdivisionOfResidence() : StringUtils.EMPTY;
    }

    public void setDistrictSubdivisionOfResidence(String districtSubdivisionOfResidence) {
        getOrCreateDefaultPhysicalAddress().setDistrictSubdivisionOfResidence(districtSubdivisionOfResidence);
    }

    public String getDistrictOfResidence() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getDistrictOfResidence() : StringUtils.EMPTY;
    }

    public void setDistrictOfResidence(String districtOfResidence) {
        getOrCreateDefaultPhysicalAddress().setDistrictOfResidence(districtOfResidence);
    }

    public Country getCountryOfResidence() {
        return hasDefaultPhysicalAddress() ? getDefaultPhysicalAddress().getCountryOfResidence() : null;
    }

    public void setCountryOfResidence(Country countryOfResidence) {
        PhysicalAddress defaultPhysicalAddress = getOrCreateDefaultPhysicalAddress();

        if (defaultPhysicalAddress.isFiscalAddress() && defaultPhysicalAddress.getCountryOfResidence() != countryOfResidence) {
            throw new DomainException("error.PhysicalAddress.cannot.change.countryOfResidence.in.fiscal.address");
        }

        defaultPhysicalAddress.setCountryOfResidence(countryOfResidence);
    }

    public PhysicalAddress getFiscalAddress() {
        return getAllPartyContacts(PhysicalAddress.class).stream().map(PhysicalAddress.class::cast)
                .filter(address -> address.isActiveAndValid()).filter(address -> address.isFiscalAddress()).findFirst()
                .orElse(null);
    }

    public void markAsFiscalAddress(final PhysicalAddress fiscalAddress) {
        if (!fiscalAddress.isActiveAndValid()) {
            throw new DomainException("error.Party.markAsFiscalAddress.fiscalAddress.must.be.active.and.valid");
        }

        getAllPartyContacts(PhysicalAddress.class).stream().forEach(address -> {
            ((PhysicalAddress) address).setFiscalAddress(false);
        });

        fiscalAddress.setFiscalAddress(true);
    }

    @Override
    public int compareTo(Party party) {
        return COMPARATOR_BY_NAME.compare(this, party);
    }

    public void logCreateContact(PartyContact contact) {
    }

    public void logEditContact(PartyContact contact, boolean propertiesChanged, boolean valueChanged, boolean createdNewContact,
            String newValue) {
    }

    public void logDeleteContact(PartyContact contact) {
    }

    public void logValidContact(PartyContact contact) {
    }

    public void logRefuseContact(PartyContact contact) {
    }

    public static Set<Person> readAllPersons() {
        return Party.getPartysSet(Person.class);
    }

}
