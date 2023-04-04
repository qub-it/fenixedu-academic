package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CountryTest {

    private static Country portugal;
    private static Country spain;
    private static Country france;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            portugal = new Country(localizeString("Portugal"), localizeString("Portuguese"), "PT", "PRT");
            spain = new Country(localizeString("Spain"), localizeString("Spanish"), "ES", "ESP");
            france = new Country(localizeString("France"), localizeString("Fraench"), "FR", "FRA");
            return null;
        });
    }

    @Test
    public void testCountry_readAll() {
        assertEquals(Country.readAll().isEmpty(), false);
        assertEquals(Country.readAll().size(), 3);
    }

    @Test
    public void testCountry_default() {
        assertNull(Country.readDefault());
        portugal.setDefaultCountry(true);
        assertEquals(Country.readDefault(), portugal);
    }

    @Test
    public void testCountry_readByCode() {
        assertNull(Country.readByTwoLetterCode(""));
        assertNull(Country.readByThreeLetterCode(""));
        assertNull(Country.readByTwoLetterCode("XX"));
        assertNull(Country.readByThreeLetterCode("XXX"));

        assertEquals(Country.readByTwoLetterCode("ES"), spain);
        assertEquals(Country.readByThreeLetterCode("FRA"), france);
    }

    private static LocalizedString localizeString(final String value) {
        return new LocalizedString.Builder().with(Locale.getDefault(), value).build();
    }

}
