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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class WebAddressTest {

    private static final Comparator<WebAddress> COMPARATOR_BY_URL = WebAddress.COMPARATOR_BY_URL;

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
    public void testComparatorByUrl() {
        final Person person = createPerson("Person", "url.comparator.person");
        final String url = "http://fenixedu.org";

        final WebAddress personal = WebAddress.createWebAddress(person, url, PartyContactType.PERSONAL, true, true, true, true);
        final WebAddress work = WebAddress.createWebAddress(person, url, PartyContactType.WORK, true, true, true, true);
        final WebAddress blog = WebAddress.createWebAddress(person, "http://blog.fenixedu.org", PartyContactType.PERSONAL, true);

        // web addresses ordered by the url value
        assertTrue(COMPARATOR_BY_URL.compare(blog, personal) < 0);
        assertTrue(COMPARATOR_BY_URL.compare(personal, blog) > 0);

        // equal urls fall back to the contact type (PERSONAL before WORK)
        assertTrue(COMPARATOR_BY_URL.compare(personal, work) < 0);

        assertEquals(0, COMPARATOR_BY_URL.compare(personal, personal));
    }

    @Test
    public void testCreateWebAddress() {
        final Person person = createPerson("Person", "web.address.person");
        final String url = "http://fenixedu.org";

        // null for empty url
        assertNull(WebAddress.createWebAddress(person, null, PartyContactType.PERSONAL, true));
        assertNull(WebAddress.createWebAddress(person, "", PartyContactType.PERSONAL, true));

        final WebAddress first = WebAddress.createWebAddress(person, url, PartyContactType.PERSONAL, true);
        assertNotNull(first);
        assertTrue(first.hasValue(url));

        // an existing url is reused instead of creating a new one
        final WebAddress found = WebAddress.createWebAddress(person, url, PartyContactType.PERSONAL, true);
        assertEquals(first, found);
    }

    @Test
    public void testHasUrl() {
        final Person person = createPerson("Person", "url.person");

        final WebAddress webAddress = WebAddress.createWebAddress(person, "http://fenixedu.org", PartyContactType.PERSONAL, true);
        assertTrue(webAddress.hasUrl());
    }

    @Test
    public void testSetUrl() {
        final Person person = createPerson("Person", "set.url.person");
        final WebAddress webAddress = WebAddress.createWebAddress(person, "http://fenixedu.org", PartyContactType.PERSONAL, true);

        assertThrows(DomainException.class, () -> webAddress.setUrl(null));
        assertThrows(DomainException.class, () -> webAddress.setUrl(""));

        final String url = "http://blog.fenixedu.org";
        webAddress.setUrl(url);
        assertEquals(url, webAddress.getUrl());
    }
}