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
import org.fenixedu.academic.domain.PersonInformationLog;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class WebAddress extends WebAddress_Base {

    public static final Comparator<WebAddress> COMPARATOR_BY_URL =
            Comparator.comparing(WebAddress::getUrl, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(COMPARATOR_BY_TYPE);

    public static WebAddress create(Party party, String url, PartyContactType type, boolean isDefault) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }

        WebAddress webAddress = new WebAddress();
        webAddress.init(party, type, isDefault);
        webAddress.setUrl(url);
        return webAddress;
    }

    public static WebAddress create(Party party, String url, PartyContactType type, boolean isDefault, boolean visibleToPublic) {
        WebAddress webAddress = WebAddress.create(party, url, type, isDefault);
        if (webAddress == null) {
            return null;
        }

        webAddress.setVisibleToPublic(visibleToPublic);
        return webAddress;
    }

    public static WebAddress findOrCreate(Party party, String url, PartyContactType type, boolean isDefault) {
        return party.getWebAddresses().stream().filter(webAddress -> webAddress.getUrl().equals(url)).findFirst()
                .orElseGet(() -> WebAddress.create(party, url, type, isDefault));
    }

    @Deprecated(forRemoval = true)
    public static WebAddress createWebAddress(Party party, String url, PartyContactType type, Boolean isDefault,
            Boolean visibleToPublic, Boolean visibleToStudents, Boolean visibleToStaff) {
        return !StringUtils.isEmpty(url) ? new WebAddress(party, type, visibleToPublic, visibleToStudents, visibleToStaff,
                isDefault, url) : null;
    }

    @Deprecated(forRemoval = true)
    public static WebAddress createWebAddress(Party party, String url, PartyContactType type, boolean isDefault) {
        return party.getWebAddresses().stream().filter(webAddress -> webAddress.getUrl().equals(url)).findFirst()
                .orElseGet(() -> !StringUtils.isEmpty(url) ? new WebAddress(party, type, isDefault, url) : null);
    }

    protected WebAddress() {
        super();
        // no validation is necessary
    }

    @Deprecated(forRemoval = true)
    protected WebAddress(final Party party, final PartyContactType type, final boolean defaultContact, final String url) {
        this();
        super.init(party, type, defaultContact);
        setUrl(url);
    }

    @Deprecated(forRemoval = true)
    protected WebAddress(final Party party, final PartyContactType type, final boolean visibleToPublic,
            final boolean visibleToStudents, final boolean visibleToStaff, final boolean defaultContact, final String url) {
        this();
        super.init(party, type, visibleToPublic, visibleToStudents, visibleToStaff, defaultContact);
        setUrl(url);
    }

    private void checkParameters(final String url) {
        if (StringUtils.isEmpty(url)) {
            throw new DomainException("error.domain.contacts.WebAddress.invalid.url");
        }
    }

    @Override
    public String getPresentationValue() {
        return getUrl();
    }

    @Override
    public boolean isWebAddress() {
        return true;
    }

    public void edit(final String url) {
        setUrl(url);
    }

    public boolean hasUrl() {
        return StringUtils.isNotEmpty(getUrl());
    }

    @Override
    public void setUrl(final String url) {
        checkParameters(url);
        super.setUrl(url);
    }

    @Override
    public boolean hasValue(String value) {
        return hasUrl() && getUrl().equals(value);
    }

    @Override
    public void logCreate(Person person) {
        logCreateAux(person, "label.partyContacts.WebAddress");
    }

    @Override
    public void logEdit(Person person, boolean propertiesChanged, boolean valueChanged, boolean createdNewContact,
            String newValue) {
        logEditAux(person, propertiesChanged, valueChanged, createdNewContact, newValue, "label.partyContacts.WebAddress");
    }

    @Override
    public void logEditAux(Person person, boolean propertiesChanged, boolean valueChanged, boolean createdNewContact,
            String newValue, String typeKey) {
        final String infoLabel = BundleUtil.getString(Bundle.APPLICATION, typeKey);

        boolean oldValueDiffersFromNew = false;
        if (valueChanged) {
            if (getPrevPartyContact() != null) {
                oldValueDiffersFromNew = getPrevPartyContact().getPresentationValue().compareTo(getPresentationValue()) != 0;
            }
        }

        if (propertiesChanged && !valueChanged) {
            // only properties were changed
            PersonInformationLog.createLog(person, Bundle.MESSAGING, "log.personInformation.contact.generic.edit", infoLabel,
                    this.getPresentationValue(), person.getUsername());
        } else if (valueChanged) {
            if (oldValueDiffersFromNew) {
                // value was changed
                PersonInformationLog.createLog(person, Bundle.MESSAGING, "log.personInformation.contact.generic.edit.values",
                        infoLabel, getPrevPartyContact().getPresentationValue(), this.getPresentationValue(),
                        person.getUsername());
            }
        }
    }

    @Override
    public void logDelete(Person person) {
        logDeleteAux(person, "label.partyContacts.WebAddress");
    }

}
