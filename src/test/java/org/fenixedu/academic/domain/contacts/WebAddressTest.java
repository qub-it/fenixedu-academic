package org.fenixedu.academic.domain.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.Locale;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class WebAddressTest {

    private static final Comparator<WebAddress> COMPARATOR_BY_URL = WebAddress.COMPARATOR_BY_URL;
    private static final String URL = "http://fenixedu.org";

    private static Person person;
    private static WebAddress webAddress;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();

            person = createPerson("Person", "person");
            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    @Before
    public void initPersonAddress() {
        person.getPartyContactsSet().forEach(partyContact -> {
            partyContact.setActive(Boolean.FALSE);
            partyContact.delete();
        });
        webAddress = WebAddress.create(person, URL, PartyContactType.PERSONAL, true);
    }

    @Test
    public void testComparatorByUrl() {
        final WebAddress work = WebAddress.create(person, URL, PartyContactType.WORK, true);
        final WebAddress blog = WebAddress.create(person, "http://blog.fenixedu.org", PartyContactType.PERSONAL, true);

        // web addresses ordered by the url value
        assertTrue(COMPARATOR_BY_URL.compare(blog, webAddress) < 0);
        assertTrue(COMPARATOR_BY_URL.compare(webAddress, blog) > 0);

        // equal urls fall back to the contact type (PERSONAL before WORK)
        assertTrue(COMPARATOR_BY_URL.compare(webAddress, work) < 0);

        assertEquals(0, COMPARATOR_BY_URL.compare(webAddress, webAddress));
    }

    @Test
    public void testFindOrCreateWebAddress() {
        // null for empty url
        assertNull(WebAddress.findOrCreate(person, null, PartyContactType.PERSONAL, true));
        assertNull(WebAddress.findOrCreate(person, "", PartyContactType.PERSONAL, true));

        final WebAddress first = WebAddress.findOrCreate(person, URL, PartyContactType.PERSONAL, true);
        assertNotNull(first);
        assertTrue(first.hasValue(URL));

        // an existing url is reused instead of creating a new one
        final WebAddress found = WebAddress.findOrCreate(person, URL, PartyContactType.PERSONAL, true);
        assertEquals(first, found);
    }

    @Test
    public void testHasUrl() {
        assertTrue(webAddress.hasUrl());
        assertEquals(URL, webAddress.getUrl());
    }

    @Test
    public void testSetUrl() {
        assertThrows(DomainException.class, () -> webAddress.setUrl(null));
        assertThrows(DomainException.class, () -> webAddress.setUrl(""));

        final String url = "http://blog.fenixedu.org";
        webAddress.setUrl(url);
        assertEquals(url, webAddress.getUrl());
    }
}