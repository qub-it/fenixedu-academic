package org.fenixedu.academic.domain.contacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.Locale;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.CountryTest;
import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PhysicalAddressTest {

    private static final Comparator<PhysicalAddress> COMPARATOR_BY_ADDRESS = PhysicalAddress.COMPARATOR_BY_ADDRESS;
    private static final String ADDRESS = "Rua das Flores";

    private static Person person;
    private static PhysicalAddress address;
    private static Country portugal;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            CountryTest.initCountries();

            person = createPerson("Person", "person");
            portugal = Country.readByTwoLetterCode("PT");
            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        return new Person(userProfile);
    }

    private static PhysicalAddress createPhysicalAddress(final Person person, final PartyContactType type, final String address) {
        return PhysicalAddress.createPhysicalAddress(person,
                new PhysicalAddressData(address, "1000", "123", null, null, "Lisboa", null, portugal), type, true);
    }

    @Before
    public void initPersonAddress() {
        person.getPartyContactsSet().forEach(partyContact -> {
            partyContact.setActive(Boolean.FALSE);
            partyContact.delete();
        });
        address = createPhysicalAddress(person, PartyContactType.PERSONAL, ADDRESS);
    }

    @Test
    public void testComparatorByAddress() {
        final PhysicalAddress work = createPhysicalAddress(person, PartyContactType.WORK, ADDRESS);
        final PhysicalAddress avenue = createPhysicalAddress(person, PartyContactType.PERSONAL, "Avenida da Liberdade");

        // addresses ordered by the address value
        assertTrue(COMPARATOR_BY_ADDRESS.compare(avenue, address) < 0);
        assertTrue(COMPARATOR_BY_ADDRESS.compare(address, avenue) > 0);

        // equal addresses fall back to the contact type (PERSONAL before WORK)
        assertTrue(COMPARATOR_BY_ADDRESS.compare(address, work) < 0);

        assertEquals(0, COMPARATOR_BY_ADDRESS.compare(address, address));
    }

    @Test
    public void testGetPostalCode() {
        address.setAreaCode("1234");
        address.setAreaOfAreaCode("567");

        assertEquals("1234 567", address.getPostalCode());
    }

    @Test
    public void testGetUiFiscalPresentationValue() {
        assertEquals("Rua das Flores, 1000 123, Lisboa, Portugal", address.getUiFiscalPresentationValue());

        // empty fields are skipped
        address.setAreaCode(null);
        address.setAreaOfAreaCode(null);
        address.setDistrictSubdivisionOfResidence(null);
        address.setCountryOfResidence(null);
        assertEquals(ADDRESS, address.getUiFiscalPresentationValue());
    }
}