package org.fenixedu.academic.domain.time.calendarStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class AcademicPeriodTest {
    @Test
    public void testAcademicPeriod_getValue() {
        assertEquals(1, AcademicPeriod.SEMESTER.getValue());
        assertEquals(1, AcademicPeriod.TRIMESTER.getValue());
        assertEquals(1, AcademicPeriod.MONTH.getValue());
        assertEquals(1, AcademicPeriod.WEEK.getValue());
        assertEquals(1, AcademicPeriod.DAY.getValue());
        assertEquals(1, AcademicPeriod.OTHER.getValue());

        // multi-year periods carry the number of years
        assertEquals(1, AcademicPeriod.YEAR.getValue());
        assertEquals(2, AcademicPeriod.TWO_YEAR.getValue());
        assertEquals(3, AcademicPeriod.THREE_YEAR.getValue());
        assertEquals(4, AcademicPeriod.FOUR_YEAR.getValue());
        assertEquals(5, AcademicPeriod.FIVE_YEAR.getValue());
        assertEquals(6, AcademicPeriod.SIX_YEAR.getValue());
    }

    @Test
    public void testAcademicPeriod_getWeight() {
        assertEquals(0.5f, AcademicPeriod.SEMESTER.getWeight(), 0);
        assertEquals(0.25f, AcademicPeriod.TRIMESTER.getWeight(), 0);
        assertEquals(1f / 12f, AcademicPeriod.MONTH.getWeight(), 0);
        assertEquals(1f / 52f, AcademicPeriod.WEEK.getWeight(), 0);
        assertEquals(1f / 365f, AcademicPeriod.DAY.getWeight(), 0);
        assertEquals(0.01f, AcademicPeriod.OTHER.getWeight(), 0);

        // multi-year periods weight is the number of years
        assertEquals(1f, AcademicPeriod.YEAR.getWeight(), 0);
        assertEquals(2f, AcademicPeriod.TWO_YEAR.getWeight(), 0);
        assertEquals(3f, AcademicPeriod.THREE_YEAR.getWeight(), 0);
        assertEquals(4f, AcademicPeriod.FOUR_YEAR.getWeight(), 0);
        assertEquals(5f, AcademicPeriod.FIVE_YEAR.getWeight(), 0);
        assertEquals(6f, AcademicPeriod.SIX_YEAR.getWeight(), 0);
    }

    @Test
    public void testAcademicPeriod_compareTo() {
        // inverted natural order
        assertEquals(-1, AcademicPeriod.YEAR.compareTo(AcademicPeriod.SEMESTER));
        assertEquals(1, AcademicPeriod.SEMESTER.compareTo(AcademicPeriod.YEAR));
        assertEquals(-1, AcademicPeriod.SEMESTER.compareTo(AcademicPeriod.TRIMESTER));
        assertEquals(1, AcademicPeriod.TRIMESTER.compareTo(AcademicPeriod.SEMESTER));

        assertEquals(0, AcademicPeriod.YEAR.compareTo(AcademicPeriod.YEAR));

        assertEquals(-1, AcademicPeriod.SIX_YEAR.compareTo(AcademicPeriod.FIVE_YEAR));
        assertEquals(1, AcademicPeriod.FIVE_YEAR.compareTo(AcademicPeriod.SIX_YEAR));
    }
    @Test
    public void testAcademicPeriod_getPossibleChild() {
        assertEquals(AcademicPeriod.SEMESTER, AcademicPeriod.YEAR.getPossibleChild());
        assertEquals(AcademicPeriod.YEAR, AcademicPeriod.TWO_YEAR.getPossibleChild());
        assertEquals(AcademicPeriod.YEAR, AcademicPeriod.SIX_YEAR.getPossibleChild());
        assertEquals(AcademicPeriod.TRIMESTER, AcademicPeriod.SEMESTER.getPossibleChild());
        assertNull(AcademicPeriod.TRIMESTER.getPossibleChild());
        assertNull(AcademicPeriod.MONTH.getPossibleChild());
        assertNull(AcademicPeriod.WEEK.getPossibleChild());
        assertNull(AcademicPeriod.DAY.getPossibleChild());
        assertNull(AcademicPeriod.OTHER.getPossibleChild());
    }

    @Test
    public void testAcademicPeriod_getPossibleChilds() {
        // multi-year periods have no other children than themselves
        assertTrue(AcademicPeriod.TWO_YEAR.getPossibleChilds().contains(AcademicPeriod.YEAR));
        assertFalse(AcademicPeriod.TWO_YEAR.getPossibleChilds().contains(AcademicPeriod.SEMESTER));

        // a single academic year expands to all smaller periods
        assertEquals(6, AcademicPeriod.YEAR.getPossibleChilds().size());
        assertTrue(AcademicPeriod.YEAR.getPossibleChilds().contains(AcademicPeriod.SEMESTER));
        assertTrue(AcademicPeriod.YEAR.getPossibleChilds().contains(AcademicPeriod.TRIMESTER));
        assertTrue(AcademicPeriod.YEAR.getPossibleChilds().contains(AcademicPeriod.MONTH));
        assertTrue(AcademicPeriod.YEAR.getPossibleChilds().contains(AcademicPeriod.WEEK));
        assertTrue(AcademicPeriod.YEAR.getPossibleChilds().contains(AcademicPeriod.DAY));
        assertTrue(AcademicPeriod.YEAR.getPossibleChilds().contains(AcademicPeriod.OTHER));

        assertTrue(AcademicPeriod.SEMESTER.getPossibleChilds().isEmpty());
        assertTrue(AcademicPeriod.TRIMESTER.getPossibleChilds().isEmpty());
        assertTrue(AcademicPeriod.MONTH.getPossibleChilds().isEmpty());
        assertTrue(AcademicPeriod.WEEK.getPossibleChilds().isEmpty());
        assertTrue(AcademicPeriod.DAY.getPossibleChilds().isEmpty());
        assertTrue(AcademicPeriod.OTHER.getPossibleChilds().isEmpty());
    }
}
