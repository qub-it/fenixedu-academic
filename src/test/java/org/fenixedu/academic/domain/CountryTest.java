package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private static Country brazil;
    private static Country angola;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            portugal = new Country(localizeString("Portugal"), localizeString("Portuguese"), "PT", "PRT");
            spain = new Country(localizeString("Spain"), localizeString("Spanish"), "ES", "ESP");
            france = new Country(localizeString("France"), localizeString("Fraench"), "FR", "FRA");
            brazil = new Country(localizeString("Brazil"), localizeString("Brazilian"), "BR", "BRA");
            angola = new Country(localizeString("Angola"), localizeString("Angolan"), "AO", "AGO");
            return null;
        });
    }

    @Test
    public void testComparatorOrderByNames() {
        // France < Portugal < Spain
        assertTrue(Country.COMPARATOR_BY_NAME.compare(france, portugal) < 0);
        assertTrue(Country.COMPARATOR_BY_NAME.compare(portugal, spain) < 0);

        assertEquals(0, Country.COMPARATOR_BY_NAME.compare(france, france));
    }

    @Test
    public void testComparator_reverseOrder() {
        int result = Country.COMPARATOR_BY_NAME.compare(france, spain);
        assertEquals(-result, Country.COMPARATOR_BY_NAME.compare(spain, france));
    }

    @Test
    public void testComparator_sortedList() {
        List<Country> countries = new ArrayList<>(Arrays.asList(spain, france, portugal));
        countries.sort(Country.COMPARATOR_BY_NAME);
        assertEquals(Arrays.asList(france, portugal, spain), countries);
    }

    @Test
    public void testCountry_readAll() {
        assertEquals(Country.readAll().isEmpty(), false);
        assertEquals(Country.readAll().size(), 5);
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

    @Test
    public void testCountry_getCPLPCountries() {
        Set<Country> cplpCountries = Country.getCPLPCountries();
        assertTrue(cplpCountries.contains(portugal));
        assertTrue(cplpCountries.contains(brazil));
        assertTrue(cplpCountries.contains(angola));
        assertFalse(cplpCountries.contains(spain));
        assertFalse(cplpCountries.contains(france));

        assertTrue(Country.isCPLPCountry(portugal));
        assertTrue(Country.isCPLPCountry(brazil));
        assertFalse(Country.isCPLPCountry(spain));
        assertFalse(Country.isCPLPCountry(france));
    }

    private static LocalizedString localizeString(final String value) {
        return new LocalizedString.Builder().with(Locale.getDefault(), value).build();
    }

}
