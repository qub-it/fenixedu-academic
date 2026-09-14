package org.fenixedu.academic.domain.organizationalStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PartyTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            if (PartyType.findByCode(PartyTypeEnum.PLANET.name()).isEmpty()) {
                new PartyType(PartyTypeEnum.PLANET);
            }
            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    private static Unit createUnit(final String name) {
        return Unit.createNewUnit(PartyType.of(PartyTypeEnum.PLANET), new LocalizedString.Builder().with(Locale.getDefault(), name).build(), name, null, null);
    }

    @Test
    public void testGetAllPartyContacts() {
        final Person party = createPerson("Party", "party");

        final EmailAddress personalEmail = EmailAddress.create(party, "personal@example.com", PartyContactType.PERSONAL, true);
        final EmailAddress workEmail = EmailAddress.create(party, "work@example.com", PartyContactType.WORK, true);
        workEmail.setActive(false); // inactive contacts are still returned
        final Phone personalPhone = Phone.create(party, "911111111", PartyContactType.PERSONAL, true);
        final MobilePhone personalMobile = MobilePhone.create(party, "922222222", PartyContactType.PERSONAL, true);

        // only assignable contacts
        final List<? extends PartyContact> emails = party.getAllPartyContacts(EmailAddress.class);
        assertEquals(2, emails.size());
        assertTrue(emails.contains(personalEmail));
        assertTrue(emails.contains(workEmail));
        assertFalse(emails.contains(personalPhone));
        assertFalse(emails.contains(personalMobile));

        // only contacts of a certain type
        final List<? extends PartyContact> personalEmails =
                party.getAllPartyContacts(EmailAddress.class, PartyContactType.PERSONAL);
        assertEquals(1, personalEmails.size());
        assertTrue(personalEmails.contains(personalEmail));
        assertFalse(personalEmails.contains(workEmail));
    }

    @Test
    public void testGetPartyContacts_shouldReturnOnlyActiveAndValid() {
        final Person party = createPerson("Party", "party");

        final EmailAddress personalEmail = EmailAddress.create(party, "personal@example.com", PartyContactType.PERSONAL, true);
        personalEmail.setValid();
        final EmailAddress workEmail = EmailAddress.create(party, "work@example.com", PartyContactType.WORK, true);
        workEmail.setValid();
        final Phone personalPhone = Phone.create(party, "911111111", PartyContactType.PERSONAL, true);
        personalPhone.setValid();

        // active but never validate, must be excluded
        final EmailAddress pendingEmail = EmailAddress.create(party, "pending@example.com", PartyContactType.PERSONAL, true);
        // inactive, must be excluded
        final EmailAddress inactiveEmail = EmailAddress.create(party, "inactive@example.com", PartyContactType.WORK, true);
        inactiveEmail.setValid();
        inactiveEmail.setActive(false);

        // only valid active contacts
        final List<? extends PartyContact> emails = party.getPartyContacts(EmailAddress.class);
        assertEquals(2, emails.size());
        assertTrue(emails.contains(personalEmail));
        assertTrue(emails.contains(workEmail));
        assertFalse(emails.contains(pendingEmail));           // active but not valid
        assertFalse(emails.contains(inactiveEmail));          // valid but inactive
        assertFalse(emails.contains(personalPhone));          // different class
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