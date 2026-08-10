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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PhysicalAddressTest {

    private static final Comparator<PhysicalAddress> COMPARATOR_BY_ADDRESS = PhysicalAddress.COMPARATOR_BY_ADDRESS;

    private static Country portugal;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            CountryTest.initCountries();

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

    @Test
    public void testComparatorByAddress() {
        final Person person = createPerson("Person", "address.comparator.person");

        final PhysicalAddress personal = createPhysicalAddress(person, PartyContactType.PERSONAL, "Rua das Flores");
        final PhysicalAddress work = createPhysicalAddress(person, PartyContactType.WORK, "Rua das Flores");
        final PhysicalAddress avenue = createPhysicalAddress(person, PartyContactType.PERSONAL, "Avenida da Liberdade");

        // addresses ordered by the address value
        assertTrue(COMPARATOR_BY_ADDRESS.compare(avenue, personal) < 0);
        assertTrue(COMPARATOR_BY_ADDRESS.compare(personal, avenue) > 0);

        // equal addresses fall back to the contact type (PERSONAL before WORK)
        assertTrue(COMPARATOR_BY_ADDRESS.compare(personal, work) < 0);

        assertEquals(0, COMPARATOR_BY_ADDRESS.compare(personal, personal));
    }

    @Test
    public void testGetPostalCode() {
        final Person person = createPerson("Person", "postal.code.person");
        final PhysicalAddress address = createPhysicalAddress(person, PartyContactType.PERSONAL, "Rua das Flores");

        address.setAreaCode("1234");
        address.setAreaOfAreaCode("567");

        assertEquals("1234 567", address.getPostalCode());
    }

    @Test
    public void testGetUiFiscalPresentationValue() {
        final Person person = createPerson("Person", "ui.fiscal.person");

        final PhysicalAddress address = createPhysicalAddress(person, PartyContactType.PERSONAL, "Rua das Flores");
        assertEquals("Rua das Flores 1000 Lisboa Portugal", address.getUiFiscalPresentationValue());

        // empty fields are skipped
        address.setAreaCode(null);
        address.setDistrictSubdivisionOfResidence(null);
        address.setCountryOfResidence(null);
        assertEquals("Rua das Flores", address.getUiFiscalPresentationValue());
    }
}