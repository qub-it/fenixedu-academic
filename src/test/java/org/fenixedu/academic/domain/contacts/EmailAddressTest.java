package org.fenixedu.academic.domain.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
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
public class EmailAddressTest {

    private static final Comparator<EmailAddress> COMPARATOR = EmailAddress.COMPARATOR_BY_EMAIL;

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
    public void testComparatorByEmail() {
        final Person personA = createPerson("Person A", "person.A");
        final EmailAddress emailA = EmailAddress.createEmailAddress(personA, "a@example.com", PartyContactType.PERSONAL, true);
        final EmailAddress emailB = EmailAddress.createEmailAddress(personA, "b@example.com", PartyContactType.PERSONAL, true);

        assertTrue(COMPARATOR.compare(emailA, emailB) < 0);
        assertTrue(COMPARATOR.compare(emailB, emailA) > 0);
        assertEquals(0, COMPARATOR.compare(emailA, emailA));

        // addresses with equal values are ordered by the contact type (PERSONAL before WORK)
        final EmailAddress emailAwork = EmailAddress.createEmailAddress(personA, "a@example.com", PartyContactType.WORK, true);
        assertTrue(COMPARATOR.compare(emailA, emailAwork) < 0);
        assertTrue(COMPARATOR.compare(emailAwork, emailA) > 0);
    }
}