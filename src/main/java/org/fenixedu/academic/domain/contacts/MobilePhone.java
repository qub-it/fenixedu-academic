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

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.joda.time.DateTime;

public class MobilePhone extends MobilePhone_Base {

    public static Comparator<MobilePhone> COMPARATOR_BY_NUMBER =
            Comparator.comparing(MobilePhone::getNumber, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(COMPARATOR_BY_TYPE);

    public static MobilePhone create(Party party, String number, PartyContactType type, boolean isDefault) {
        if (StringUtils.isEmpty(number)) {
            return null;
        }

        MobilePhone mobilePhone = new MobilePhone();
        mobilePhone.init(party, type, isDefault);
        mobilePhone.setNumber(number);
        return mobilePhone;
    }

    public static MobilePhone create(Party party, String number, PartyContactType type, boolean isDefault,
            boolean visibleToPublic) {
        MobilePhone mobilePhone = MobilePhone.create(party, number, type, isDefault);
        if (mobilePhone == null) {
            return null;
        }

        mobilePhone.setVisibleToPublic(visibleToPublic);
        return mobilePhone;
    }

    public static MobilePhone findOrCreate(Party party, String number, PartyContactType type, boolean isDefault) {
        return party.getMobilePhones().stream().filter(phone -> phone.getNumber().equals(number)).findFirst()
                .orElseGet(() -> MobilePhone.create(party, number, type, isDefault));
    }

    @Deprecated(forRemoval = true)
    public static MobilePhone createMobilePhone(Party party, String number, PartyContactType type, Boolean isDefault,
            Boolean visibleToPublic, Boolean visibleToStudents, Boolean visibleToStaff) {
        return !StringUtils.isEmpty(number) ? new MobilePhone(party, type, visibleToPublic, visibleToStudents, visibleToStaff,
                isDefault, number) : null;
    }

    @Deprecated(forRemoval = true)
    public static MobilePhone createMobilePhone(Party party, String number, PartyContactType type, boolean isDefault) {
        return party.getMobilePhones().stream().filter(phone -> phone.getNumber().equals(number)).findFirst()
                .orElseGet(() -> !StringUtils.isEmpty(number) ? new MobilePhone(party, type, isDefault, number) : null);
    }

    protected MobilePhone() {
        super();
        new PhoneValidation(this);
    }

    @Deprecated(forRemoval = true)
    protected MobilePhone(final Party party, final PartyContactType type, final boolean defaultContact, final String number) {
        this();
        super.init(party, type, defaultContact);
        checkParameters(number);
        super.setNumber(number);
    }

    @Deprecated(forRemoval = true)
    protected MobilePhone(final Party party, final PartyContactType type, final boolean visibleToPublic,
            final boolean visibleToStudents, final boolean visibleToStaff, final boolean defaultContact, final String number) {
        this();
        super.init(party, type, visibleToPublic, visibleToStudents, visibleToStaff, defaultContact);
        checkParameters(number);
        super.setNumber(number);
    }

    private void checkParameters(final String number) {
        if (StringUtils.isEmpty(number)) {
            throw new DomainException("error.contacts.Phone.invalid.number");
        }
    }

    @Override
    public boolean isMobile() {
        return true;
    }

    public void edit(final String number) {
        if (!StringUtils.equals(getNumber(), number)) {
            setNumber(number);
            if (!waitsValidation()) {
                new PhoneValidation(this);
            }
            setLastModifiedDate(new DateTime());
        }
    }

    @Override
    public void setNumber(final String number) {
        checkParameters(number);
        super.setNumber(number);
    }

    @Override
    public String getPresentationValue() {
        return getNumber();
    }

    public boolean hasNumber() {
        return getNumber() != null && !getNumber().isEmpty();
    }

    @Override
    public boolean hasValue(String value) {
        return hasNumber() && getNumber().equals(value);
    }

    @Override
    public void logCreate(Person person) {
        logCreateAux(person, "label.partyContacts.MobilePhone");
    }

    @Override
    public void logEdit(Person person, boolean propertiesChanged, boolean valueChanged, boolean createdNewContact,
            String newValue) {
        logEditAux(person, propertiesChanged, valueChanged, createdNewContact, newValue, "label.partyContacts.MobilePhone");
    }

    @Override
    public void logDelete(Person person) {
        logDeleteAux(person, "label.partyContacts.MobilePhone");
    }

    @Override
    public void logValid(Person person) {
        logValidAux(person, "label.partyContacts.MobilePhone");
    }

    @Override
    public void logRefuse(Person person) {
        logRefuseAux(person, "label.partyContacts.MobilePhone");
    }

}
