package org.fenixedu.academic.domain.person.contacts;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class PersonEmailTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            return null;
        });
    }

    @Test
    public void testGetEmailAddressForSendingEmailsWithNotValidAddress() {
        UserProfile userProfile = new UserProfile("John", "Doe", "John Doe", "john.doe@example.com", Locale.getDefault());
        Person person = new Person(userProfile);
        EmailAddress email = EmailAddress.createEmailAddress(person, "default@example.com", PartyContactType.PERSONAL, true);

        assertNull(person.getEmailAddressForSendingEmails());

        email.setValid();
        assertEquals("default@example.com", person.getEmailAddressForSendingEmails().getValue());
    }

    @Test
    public void testGetEmailAddressForSendingEmailsWithCustomLogic() {
        UserProfile userProfile = new UserProfile("John", "Doe", "John Doe", "john.doe@example.com", Locale.getDefault());
        Person person = new Person(userProfile);
        EmailAddress.createEmailAddress(person, "john.doe@example.com", PartyContactType.PERSONAL, true).setValid();

        EmailAddress defaultEmailAddress = person.getEmailAddressForSendingEmails();
        assertEquals("john.doe@example.com", defaultEmailAddress.getValue());

        EmailAddress otherEmailAddress =
                EmailAddress.createEmailAddress(person, "custom@example.com", PartyContactType.PERSONAL, false);

        Person.registerCustomEmailAddressForSendingEmailsProvider(p -> otherEmailAddress);
        EmailAddress emailAddress = person.getEmailAddressForSendingEmails();

        assertEquals("custom@example.com", emailAddress.getValue());

        Person.registerCustomEmailAddressForSendingEmailsProvider(null);
    }

    @Test
    public void testGetEmailAddressForSendingEmailsWhenDisabled() {
        UserProfile userProfile = new UserProfile("John", "Doe", "John Doe", "john.doe@example.com", Locale.getDefault());
        Person person = new Person(userProfile);
        EmailAddress.createEmailAddress(person, "john.doe@example.com", PartyContactType.PERSONAL, true).setValid();

        assertNotNull(person.getEmailAddressForSendingEmails());

        person.setDisableSendEmails(true);

        assertNull(person.getEmailAddressForSendingEmails());

        EmailAddress otherEmailAddress =
                EmailAddress.createEmailAddress(person, "custom@example.com", PartyContactType.PERSONAL, false);
        Person.registerCustomEmailAddressForSendingEmailsProvider(p -> otherEmailAddress);
        assertNull(person.getEmailAddressForSendingEmails());

        Person.registerCustomEmailAddressForSendingEmailsProvider(null);
    }

    @Test
    public void testGetEmailAddressForSendingEmailsWithForcedInstitutional() {
        Installation.getInstance().setForceSendingEmailsToInstituitionAddress(true);

        UserProfile userProfile = new UserProfile("John", "Doe", "John Doe", "john.doe@example.com", Locale.getDefault());
        Person person = new Person(userProfile);
        EmailAddress.createEmailAddress(person, "personal@example.com", PartyContactType.PERSONAL, true).setValid();
        EmailAddress.createEmailAddress(person, "institutional@example.com", PartyContactType.INSTITUTIONAL, false).setValid();

        assertEquals("institutional@example.com", person.getEmailAddressForSendingEmails().getValue());

        Installation.getInstance().setForceSendingEmailsToInstituitionAddress(false);
    }

    @Test
    public void testGetEmailAddressForSendingEmailsWithMultipleAddresses() {
        UserProfile userProfile = new UserProfile("John", "Doe", "John Doe", "john.doe@example.com", Locale.getDefault());
        Person person = new Person(userProfile);
        EmailAddress.createEmailAddress(person, "personal1@example.com", PartyContactType.PERSONAL, false).setValid();
        EmailAddress.createEmailAddress(person, "personal2@example.com", PartyContactType.PERSONAL, true).setValid();

        assertEquals("personal2@example.com", person.getEmailAddressForSendingEmails().getValue());
    }

}
