package org.fenixedu.academic.domain.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PartyContactTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    @Test
    public void testComparatorByType() {
        Person person = createPerson("Person", "person");
        EmailAddress personalEmail =
                EmailAddress.createEmailAddress(person, "personal@example.com", PartyContactType.PERSONAL, true);
        EmailAddress personalEmail2 =
                EmailAddress.createEmailAddress(person, "personal2@example.com", PartyContactType.PERSONAL, true);
        EmailAddress workEmail = EmailAddress.createEmailAddress(person, "work@example.com", PartyContactType.WORK, true);

        assertTrue(PartyContact.COMPARATOR_BY_TYPE.compare(workEmail, personalEmail) > 0);
        assertTrue(PartyContact.COMPARATOR_BY_TYPE.compare(personalEmail, workEmail) < 0);
        assertTrue(personalEmail.getType().ordinal() < workEmail.getType().ordinal());

        // tie-breaker by id for the same party contact type
        assertTrue(PartyContact.COMPARATOR_BY_TYPE.compare(personalEmail, personalEmail2) != 0);
    }

    @Test
    public void testSetsVisibilityFlags() {
        Person person = createPerson("Person", "person");
        EmailAddress email =
                EmailAddress.createEmailAddress(person, "test@example.com", PartyContactType.PERSONAL, true,   // defaultContact
                        false,  // visibleToPublic
                        true,   // visibleToStudents
                        false);  // visibleToStaff

        assertEquals(Boolean.FALSE, email.getVisibleToPublic());
        assertEquals(Boolean.TRUE, email.getVisibleToStudents());
        assertEquals(Boolean.FALSE, email.getVisibleToStaff());
    }

    @Test
    public void testIsDefault() {
        Person person = createPerson("Person", "person");
        EmailAddress email = EmailAddress.createEmailAddress(person, "test@example.com", PartyContactType.PERSONAL, true);

        email.setValid();
        assertTrue(email.isDefault());

        email.setDefaultContact(Boolean.FALSE);
        assertFalse(email.isDefault());
        email.setDefaultContact(null);
        assertFalse(email.isDefault());
    }

    @Test
    public void testSetAnotherContactAsDefault() {
        Person person = createPerson("Person", "person");

        // only one contact, stays default
        EmailAddress email = EmailAddress.createEmailAddress(person, "test@example.com", PartyContactType.PERSONAL, true);
        email.setValid();
        assertTrue(email.isDefault());
        email.setAnotherContactAsDefault();
        assertTrue(email.isDefault());

        // caller is not the default, nothing happens
        EmailAddress email2 = EmailAddress.createEmailAddress(person, "test2@example.com", PartyContactType.PERSONAL, false);
        email2.setValid();
        email2.setAnotherContactAsDefault();
        assertTrue(email.isDefault());
        assertFalse(email2.isDefault());

        // set another contact as default
        email.setAnotherContactAsDefault();
        assertTrue(email2.isDefault());
    }
}