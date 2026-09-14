package org.fenixedu.academic.domain.organizationalStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PartyTest {

    private static Person party;
    private static PartyContact defaultEmail, pendingEmail, nonDefaultEmail, institutionalEmail, defaultPhone;
    private static Optional<PartyType> planetPartyType;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            party = createPerson("Party", "party");

            planetPartyType = PartyType.findByCode(PartyTypeEnum.PLANET.name());
            if (planetPartyType.isEmpty()) {
                planetPartyType = Optional.of(new PartyType(PartyTypeEnum.PLANET));
            }
            return null;
        });
    }

    @Before
    public void setUp() {
        defaultEmail = EmailAddress.create(party, "default@example.com", PartyContactType.PERSONAL, true);
        defaultEmail.setValid();
        nonDefaultEmail = EmailAddress.create(party, "other@example.com", PartyContactType.PERSONAL, false);
        nonDefaultEmail.setValid();
        pendingEmail = EmailAddress.create(party, "pending@example.com", PartyContactType.PERSONAL, true);
        institutionalEmail = EmailAddress.create(party, "institutional@example.com", PartyContactType.INSTITUTIONAL, true);
        institutionalEmail.setValid();
        defaultPhone = Phone.create(party, "911111111", PartyContactType.PERSONAL, true);
        defaultPhone.setValid();
    }

    @After
    public void cleanUp() {
        while (!party.getPartyContactsSet().isEmpty()) {
            party.getPartyContactsSet().forEach(PartyContact::deleteWithoutCheckRules);
        }
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    private static Unit createUnit(final String name) {
        return Unit.createNewUnit(planetPartyType, new LocalizedString.Builder().with(Locale.getDefault(), name).build(), name,
                null, null);
    }

    @Test
    public void testGetAllPartyContacts() {
        // only assignable contacts
        final List<? extends PartyContact> emails = party.getAllPartyContacts(EmailAddress.class);
        assertEquals(4, emails.size());
        assertTrue(emails.contains(defaultEmail));
        assertTrue(emails.contains(nonDefaultEmail));
        assertTrue(emails.contains(pendingEmail));
        assertTrue(emails.contains(institutionalEmail));
        assertFalse(emails.contains(defaultPhone));

        // only contacts of a certain type
        final List<? extends PartyContact> institutionalEmails =
                party.getAllPartyContacts(EmailAddress.class, PartyContactType.INSTITUTIONAL);
        assertEquals(1, institutionalEmails.size());
        assertTrue(institutionalEmails.contains(institutionalEmail));
        assertFalse(institutionalEmails.contains(defaultPhone));
    }

    @Test
    public void testGetPartyContacts() {
        // inactive, must be excluded
        nonDefaultEmail.setActive(false);

        // only valid active contacts
        final List<? extends PartyContact> emails = party.getPartyContacts(EmailAddress.class);
        assertEquals(2, emails.size());
        assertTrue(emails.contains(defaultEmail));
        assertTrue(emails.contains(institutionalEmail));
        assertFalse(emails.contains(pendingEmail));           // active but not valid
        assertFalse(emails.contains(nonDefaultEmail));          // valid but inactive
        assertFalse(emails.contains(defaultPhone));          // different class
    }

    @Test
    public void testDelete_removesAllPartyContacts() {
        final Unit unitToDelete = createUnit("Delete unit");

        EmailAddress.create(unitToDelete, "delete@example.com", PartyContactType.PERSONAL, true);
        Phone.create(unitToDelete, "911111111", PartyContactType.PERSONAL, true);
        Phone inactivePhone = Phone.create(unitToDelete, "933333333", PartyContactType.PERSONAL, false);
        inactivePhone.setActive(false);

        assertFalse(unitToDelete.getPartyContactsSet().isEmpty());
        assertEquals(3, unitToDelete.getPartyContactsSet().size());

        unitToDelete.delete();

        assertTrue(unitToDelete.getPartyContactsSet().isEmpty());
    }
}