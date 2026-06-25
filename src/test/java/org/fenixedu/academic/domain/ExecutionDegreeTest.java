package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionDegreeTest {

    private static ExecutionDegree edA, edB, edC, edA2, edA_new, edMaster;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initExecutionDegrees();
            return null;
        });
    }

    private static void initExecutionDegrees() {
        DegreeTest.initDegree();

        final ExecutionYear currentYear = ExecutionYear.findCurrent(null);
        final ExecutionYear nextYear = ExecutionYear.readExecutionYearByName("2021/2022");

        final DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        final DegreeType masterDegreeType = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).orElseThrow();

        Degree degreeA = DegreeTest.createDegree(degreeType, "AAA", "Degree A", currentYear);
        Degree degreeB = DegreeTest.createDegree(degreeType, "BBB", "Degree B", currentYear);
        Degree degreeC = DegreeTest.createDegree(degreeType, "CCC", "Degree C", currentYear);
        Degree degreeA2 = DegreeTest.createDegree(degreeType, " AAA ", "Degree A2", currentYear);
        Degree degreeMaster = DegreeTest.createDegree(masterDegreeType, "MST", "Master A", currentYear);

        edA = createExecutionDegree(degreeA, "DCP_A", currentYear);
        edA_new = createExecutionDegree(degreeA, "DCP_A_NEW", nextYear);
        edB = createExecutionDegree(degreeB, "DCP_B", currentYear);
        edC = createExecutionDegree(degreeC, "DCP_C", currentYear);
        edA2 = createExecutionDegree(degreeA2, "DCP_A2", currentYear);
        edMaster = createExecutionDegree(degreeMaster, "DCP_MASTER", currentYear);
    }

    private static ExecutionDegree createExecutionDegree(Degree degree, String dcpName, ExecutionYear year) {
        DegreeCurricularPlan dcp = new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR, year.getFirstExecutionPeriod());
        dcp.setCurricularStage(CurricularStage.APPROVED);
        return dcp.createExecutionDegree(year);
    }

    @Test
    public void comparatorByDegreeCode() {
        List<ExecutionDegree> sorted = Stream.of(edC, edA, edB)
                .sorted(ExecutionDegree.COMPARATOR_BY_DEGREE_CODE)
                .toList();

        assertEquals(3, sorted.size());
        assertEquals(edA, sorted.get(0));  // "AAA" first
        assertEquals(edB, sorted.get(1));  // "BBB" second
        assertEquals(edC, sorted.get(2));  // "CCC" third
    }

    @Test
    public void comparatorByDegreeCode_tieBreaker() {
        List<ExecutionDegree> sorted = Stream.of(edA2, edA)
                .sorted(ExecutionDegree.COMPARATOR_BY_DEGREE_CODE)
                .toList();

        assertEquals(2, sorted.size());
        assertEquals("AAA", sorted.get(0).getDegree().getSigla());
        assertEquals("AAA", sorted.get(1).getDegree().getSigla());

        String id0 = sorted.get(0).getExternalId();
        String id1 = sorted.get(1).getExternalId();
        assertTrue(id0.compareTo(id1) < 0);
    }

    @Test
    public void comparatorByDegreeName() {
        List<ExecutionDegree> sorted = Stream.of(edC, edA, edB, edA2)
                .sorted(ExecutionDegree.COMPARATOR_BY_DEGREE_NAME)
                .toList();

        assertEquals(4, sorted.size());
        assertEquals(edA, sorted.get(0));  // "Degree A" first
        assertEquals(edA2, sorted.get(1)); // "Degree A2" second
        assertEquals(edB, sorted.get(2));  // "Degree B" third
        assertEquals(edC, sorted.get(3));  // "Degree C" fourth
    }

    @Test
    public void comparatorByExecutionYear() {
        List<ExecutionDegree> sorted = Stream.of(edA_new, edA).sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_YEAR)
                .toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));  // 2019/2020
        assertEquals(edA_new, sorted.get(1));  // 2021/2022
    }

    @Test
    public void comparatorByDegreeTypeAndName_differentTypes() {
        List<ExecutionDegree> sorted = Stream.of(edMaster, edA)
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME)
                .toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));       // "Degree" < "Master Degree"
        assertEquals(edMaster, sorted.get(1));
    }

    @Test
    public void comparatorByDegreeTypeAndName_sameType() {
        List<ExecutionDegree> sorted = Stream.of(edC, edA, edB)
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME)
                .toList();

        assertEquals(3, sorted.size());
        assertEquals(edA, sorted.get(0));  // "Degree A"
        assertEquals(edB, sorted.get(1));  // "Degree B"
        assertEquals(edC, sorted.get(2));  // "Degree C"
    }

    @Test
    public void comparatorByDegreeTypeAndNameAndExecutionYear() {
        List<ExecutionDegree> sorted = Stream.of(edA_new, edA)
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR)
                .toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));     // 2019/2020
        assertEquals(edA_new, sorted.get(1)); // 2021/2022
    }

    @Test
    public void comparatorByDegreeTypeAndNameAndExecutionYear_differentTypes() {
        List<ExecutionDegree> sorted = Stream.of(edMaster, edA)
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR)
                .toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));       // "Degree" < "Master Degree"
        assertEquals(edMaster, sorted.get(1));
    }

}
