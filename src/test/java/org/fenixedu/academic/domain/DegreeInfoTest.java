package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeInfoTest {

    private static DegreeInfo infoA2020;
    private static DegreeInfo infoA2019;
    private static DegreeInfo infoA2021;
    private static DegreeInfo infoB2020;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();

            final DegreeType degreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Degree").build());
            degreeType.setCode("DEGREE");

            final ExecutionYear year2020 = ExecutionYear.findCurrent(null);
            final ExecutionYear year2019 = (ExecutionYear) year2020.getPrevious();
            final ExecutionYear year2021 = (ExecutionYear) year2020.getNext();

            final Degree degreeA = DegreeTest.createDegree(degreeType, "DA", "Degree A", year2020);
            final Degree degreeB = DegreeTest.createDegree(degreeType, "DB", "Degree B", year2020);

            infoA2020 = degreeA.getDegreeInfoFor(year2020);
            infoA2019 = new DegreeInfo(degreeA, year2019);
            infoA2021 = new DegreeInfo(degreeA, year2021);
            infoB2020 = degreeB.getDegreeInfoFor(year2020);

            return null;
        });
    }

    @Test
    public void testDegreeInfoConstructor() {
        Degree degree = infoB2020.getDegree();
        ExecutionYear year = infoA2019.getExecutionYear();

        DegreeInfo info = new DegreeInfo(degree, year);
        assertEquals(degree, info.getDegree());
        assertEquals(year, info.getExecutionYear());
        assertEquals(degree.getNameFor(year.getAcademicInterval()), info.getName());

        assertThrows(DomainException.class, () -> new DegreeInfo(degree, year));
    }

    @Test
    public void compareDifferentYears() {
        assertTrue(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2019, infoA2020) < 0);
        assertTrue(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2020, infoA2019) > 0);
        assertTrue(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2019, infoA2021) < 0);
        assertTrue(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2021, infoA2019) > 0);
        assertTrue(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2020, infoA2021) < 0);
        assertTrue(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2021, infoA2020) > 0);
    }

    @Test
    public void compareAntisymmetric() {
        final int ab = DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.compare(infoA2020, infoB2020);
        final int ba = DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR.reversed().compare(infoA2020, infoB2020);
        assertEquals(ab, -ba);
    }

    @Test
    public void sortByExecutionYear() {
        final List<DegreeInfo> list = new ArrayList<>(Arrays.asList(infoA2021, infoA2019, infoA2020));
        list.sort(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR);
        assertEquals(infoA2019, list.get(0));
        assertEquals(infoA2020, list.get(1));
        assertEquals(infoA2021, list.get(2));
    }

    @Test
    public void sortSameYearTieBreakByExternalId() {
        final List<DegreeInfo> list = new ArrayList<>(Arrays.asList(infoB2020, infoA2020));
        list.sort(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR);
        final List<DegreeInfo> expected = new ArrayList<>(Arrays.asList(infoA2020, infoB2020));
        expected.sort(DomainObjectUtil.COMPARATOR_BY_ID);
        assertEquals(expected, list);
    }

}
