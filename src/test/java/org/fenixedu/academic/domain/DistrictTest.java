package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DistrictTest {

    private static District lisbonDistrict;
    private static DistrictSubdivision lisbonSubdivision;
    private static DistrictSubdivision sintraSubdivision;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            lisbonDistrict = new District("11", "Lisboa");
            lisbonSubdivision = new DistrictSubdivision("1111", "Lisboa", lisbonDistrict);
            sintraSubdivision = new DistrictSubdivision("1112", "Sintra", lisbonDistrict);
            return null;
        });
    }

    @Test
    public void testGetDistrictSubdivisionByName() {
        assertEquals(lisbonSubdivision, lisbonDistrict.getDistrictSubdivisionByName("Lisboa"));
        assertEquals(sintraSubdivision, lisbonDistrict.getDistrictSubdivisionByName("Sintra"));

        assertNull(lisbonDistrict.getDistrictSubdivisionByName("NonExistent"));
        assertNull(lisbonDistrict.getDistrictSubdivisionByName(""));
        assertNull(lisbonDistrict.getDistrictSubdivisionByName(null));
    }

    @Test
    public void testDistrict_findByCode() {
        assertEquals(lisbonDistrict, District.readByCode("11"));
        assertNull(District.readByCode("XX"));
        assertNull(District.readByCode(""));
        assertNull(District.readByCode(null));

        assertTrue(District.findByCode("11").filter(d -> d == lisbonDistrict).isPresent());
        assertTrue(District.findByCode("XX").isEmpty());
        assertTrue(District.findByCode("").isEmpty());
        assertTrue(District.findByCode(null).isEmpty());
    }

    @Test
    public void testDistrict_findByName() {
        assertEquals(lisbonDistrict, District.readByName("Lisboa"));
        assertNull(District.readByName("NonExistent"));
        assertNull(District.readByName(""));
        assertNull(District.readByName(null));

        assertTrue(District.findByName("Lisboa").filter(d -> d == lisbonDistrict).isPresent());
        assertTrue(District.findByName("NonExistent").isEmpty());
        assertTrue(District.findByName("").isEmpty());
        assertTrue(District.findByName(null).isEmpty());
    }

    @Test
    public void testDistrictSubdivision_findByCode() {
        assertEquals(lisbonSubdivision, DistrictSubdivision.findByCode("1111").orElseThrow());
        assertEquals(sintraSubdivision, DistrictSubdivision.findByCode("1112").orElseThrow());

        assertTrue(DistrictSubdivision.findByCode("XX").isEmpty());
        assertTrue(DistrictSubdivision.findByCode("").isEmpty());
        assertTrue(DistrictSubdivision.findByCode(null).isEmpty());
    }
}
