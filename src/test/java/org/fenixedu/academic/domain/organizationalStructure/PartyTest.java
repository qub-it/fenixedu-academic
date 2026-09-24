package org.fenixedu.academic.domain.organizationalStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    private static EmailAddress defaultEmail, pendingEmail, nonDefaultEmail, institutionalEmail;
    private static Phone defaultPhone;
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
        // active and valid contacts
        nonDefaultEmail = EmailAddress.create(party, "other@example.com", PartyContactType.PERSONAL, false);
        nonDefaultEmail.setValid();
        institutionalEmail = EmailAddress.create(party, "institutional@example.com", PartyContactType.INSTITUTIONAL, true);
        institutionalEmail.setValid();
        defaultEmail = EmailAddress.create(party, "default@example.com", PartyContactType.PERSONAL, true);
        defaultEmail.setValid();
        defaultPhone = Phone.create(party, "911111111", PartyContactType.PERSONAL, true);
        defaultPhone.setValid();

        // active but never validated
        pendingEmail = EmailAddress.create(party, "pending@example.com", PartyContactType.PERSONAL, true);
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
    public void testGetPartyContactStream() {
        // only active and valid contacts assignable to the given class
        final List<EmailAddress> emails = party.getPartyContactStream(EmailAddress.class).toList();
        assertEquals(3, emails.size());
        assertTrue(emails.contains(defaultEmail));
        assertTrue(emails.contains(nonDefaultEmail));
        assertTrue(emails.contains(institutionalEmail));
        assertFalse(emails.contains(pendingEmail));    // active but not valid
        assertFalse(emails.contains(defaultPhone));

        final List<EmailAddress> personalEmails =
                party.getPartyContactStream(EmailAddress.class, PartyContactType.INSTITUTIONAL).toList();
        assertEquals(1, personalEmails.size());
        assertTrue(personalEmails.contains(institutionalEmail));
        assertFalse(personalEmails.contains(defaultEmail));

        assertTrue(party.getPartyContactStream(MobilePhone.class).findAny().isEmpty());
    }

    @Test
    public void testGetPartyContacts() {
        // inactive, must be excluded
        nonDefaultEmail.setActive(false);

        // only active and valid contacts
        final List<? extends PartyContact> emails = party.getPartyContacts(EmailAddress.class);
        assertEquals(2, emails.size());
        assertTrue(emails.contains(defaultEmail));
        assertTrue(emails.contains(institutionalEmail));
        assertFalse(emails.contains(pendingEmail));           // active but not valid
        assertFalse(emails.contains(nonDefaultEmail));        // valid but inactive
        assertFalse(emails.contains(defaultPhone));           // different class
    }

    @Test
    public void testGetPendingOrValidPartyContacts() {
        // inactive, neither isActiveAndValid() nor waitsValidation()
        final EmailAddress inactiveEmail = EmailAddress.create(party, "inactive@example.com", PartyContactType.PERSONAL, true);
        inactiveEmail.setActive(false);

        // pending or active and valid contacts
        final List<? extends PartyContact> pendingOrValid = party.getPendingOrValidPartyContacts(EmailAddress.class);
        assertTrue(pendingOrValid.contains(defaultEmail));
        assertTrue(pendingOrValid.contains(institutionalEmail));
        assertTrue(pendingOrValid.contains(pendingEmail));
        assertTrue(pendingOrValid.contains(nonDefaultEmail));
        assertFalse(pendingOrValid.contains(inactiveEmail));
        assertEquals(4, pendingOrValid.size());

        // only pending contacts
        final List<? extends PartyContact> pending = party.getPendingPartyContacts(EmailAddress.class);
        assertTrue(pending.contains(pendingEmail));
        assertFalse(pending.contains(defaultEmail));
        assertFalse(pending.contains(inactiveEmail));
        assertEquals(1, pending.size());
    }

    @Test
    public void testHasAnyPartyContact() {
        // active but never validated
        EmailAddress.create(party, "pending@example.com", PartyContactType.WORK, true);
        // inactive
        EmailAddress.create(party, "inactive@example.com", PartyContactType.WORK, true).setActive(false);

        assertTrue(party.hasAnyPartyContact(EmailAddress.class));
        assertTrue(party.hasAnyPartyContact(Phone.class));

        // WORK emails exist but all are excluded (pending or inactive)
        assertTrue(party.hasAnyPartyContact(EmailAddress.class, PartyContactType.PERSONAL));
        assertFalse(party.hasAnyPartyContact(EmailAddress.class, PartyContactType.WORK));

        // class with no contacts at all
        assertFalse(party.hasAnyPartyContact(MobilePhone.class));

        // party with no contacts
        final Person emptyParty = createPerson("Empty Party", "empty.party");
        assertFalse(emptyParty.hasAnyPartyContact(EmailAddress.class));
    }

    @Test
    public void testGetDefaultPartyContact() {
        // only default, active and valid contact is returned
        assertEquals(defaultEmail, party.getDefaultPartyContact(EmailAddress.class));

        // inactive default is excluded
        defaultPhone.setActive(false);
        assertNull(party.getDefaultPartyContact(Phone.class));

        // party with no contacts
        final Person emptyParty = createPerson("Empty Party", "empty.party");
        assertNull(emptyParty.getDefaultPartyContact(EmailAddress.class));
    }

    @Test
    public void testGetInstitutionalPartyContact() {
        // party without any institutional contact
        assertNull(party.getInstitutionalPartyContact(Phone.class));

        // active and valid institutional contact is returned
        PartyContact email = party.getInstitutionalPartyContact(EmailAddress.class);
        assertEquals(institutionalEmail, email);
        assertEquals(PartyContactType.INSTITUTIONAL, email.getType());
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