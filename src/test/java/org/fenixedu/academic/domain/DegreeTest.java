package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeTest {

    public static final String DEGREE_A_CODE = "DA";

    public static final String DEGREE_TYPE_CODE = "DEGREE";

    public static final String MASTER_DEGREE_TYPE_CODE = "MASTER";

    private static DegreeType degreeType;

    private static DegreeType masterDegreeType;

    private static Degree degree;

    private static ExecutionYear executionYear;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            degree = initDegree();
            return null;
        });
    }

    public static Degree initDegree() {
        initDegreeTypes();
        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        executionYear = ExecutionYear.findCurrent(null);

        return createDegree(degreeType, DEGREE_A_CODE, "Degree A", executionYear);
    }

    private static void initDegreeTypes() {
        degreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Degree").build());
        degreeType.setCode(DEGREE_TYPE_CODE);

        masterDegreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Master Degree").build());
        masterDegreeType.setCode(MASTER_DEGREE_TYPE_CODE);
    }

    public static Degree createDegree(final DegreeType degreeType, String code, String name, final ExecutionYear executionYear) {
        final Degree result = new Degree(name, name, code, degreeType, new GradeScale(), new GradeScale(), executionYear);
        result.setCode(code);
        result.setCalendar(executionYear.getAcademicInterval().getAcademicCalendar());

        return result;
    }

    @Test
    public void testDegree_comparatorByName() {
        Degree degreeA = createDegree(degreeType, "CMP_NAME_A", "Degree A", executionYear);
        Degree degreeB = createDegree(degreeType, "CMP_NAME_B", "Degree B", executionYear);

        assertEquals(0, Degree.COMPARATOR_BY_NAME.compare(degreeA, degreeA));
        assertTrue(Degree.COMPARATOR_BY_NAME.compare(degreeA, degreeB) < 0);
        assertTrue(Degree.COMPARATOR_BY_NAME.compare(degreeB, degreeA) > 0);
    }

    @Test
    public void testDegree_comparatorByNameAndId() {
        Degree degreeA = createDegree(degreeType, "CMP_NAME_ID_A", "Degree A", executionYear);
        Degree degreeA2 = createDegree(degreeType, "CMP_NAME_ID_A2", "Degree A", executionYear);

        assertEquals(0, Degree.COMPARATOR_BY_NAME_AND_ID.compare(degreeA, degreeA));
        assertEquals(0, degreeA.compareTo(degreeA)); // compareTo calls COMPARATOR_BY_NAME_AND_ID

        assertNotEquals(0, Degree.COMPARATOR_BY_NAME_AND_ID.compare(degreeA, degreeA2));
        assertNotEquals(0, degreeA.compareTo(degreeA2));
    }

    @Test
    public void testDegree_comparatorByDegreeTypeDegreeNameAndId() {
        Degree bachelorA = createDegree(degreeType, "CMP_DT_N_ID_BA", "Bachelor A", executionYear);
        Degree copyBachelorA = createDegree(degreeType, "CMP_DT_N_ID_BA_COPY", "Bachelor A", executionYear);
        Degree bachelorB = createDegree(degreeType, "CMP_DT_N_ID_BB", "Bachelor B", executionYear);
        Degree masterA = createDegree(masterDegreeType, "CMP_DT_N_ID_MA", "Master A", executionYear);

        List<Degree> degrees = Arrays.asList(copyBachelorA, bachelorB, masterA, bachelorA);
        degrees.sort(Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID);

        // Ordered by degree type, then name, then id
        assertEquals(bachelorA, degrees.get(0));
        assertEquals(copyBachelorA, degrees.get(1));
        assertEquals(bachelorB, degrees.get(2));
        assertEquals(masterA, degrees.get(3));

        assertEquals(0, Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID.compare(bachelorA, bachelorA));
        assertNotEquals(0, Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID.compare(bachelorA, copyBachelorA));
        assertTrue(Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID.compare(bachelorA, bachelorB) < 0);
        assertTrue(Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID.compare(masterA, bachelorA) > 0);
    }

    @Test
    public void testDegree_find() {
        assertNotNull(Degree.find(DEGREE_A_CODE));
        assertEquals(Degree.find(DEGREE_A_CODE), degree);
        assertNull(Degree.find("XX"));
    }

    @Test
    public void testDegree_getNameFor() {
        Degree testDegree = createDegree(degreeType, "GET_NAME_FOR_TEST", "Get Name For test", executionYear);

        assertEquals("Get Name For test", testDegree.getNameFor(executionYear.getFirstExecutionPeriod()).getContent());
        assertEquals("Get Name For test", testDegree.getNameFor(null).getContent());

        // Test that it also works with ExecutionYear
        assertEquals("Get Name For test", testDegree.getNameFor(executionYear).getContent());
        assertEquals(testDegree.getNameFor(executionYear), testDegree.getNameFor(executionYear.getFirstExecutionPeriod()));

        testDegree.delete();
    }

    @Test
    public void testDegree_getMostRecentDegreeInfo() {
        DegreeInfo currentInfo = degree.getDegreeInfoFor(executionYear);
        assertNotNull(currentInfo);

        ExecutionYear previousYear = executionYear.getPrevious().getExecutionYear();
        ExecutionYear nextYear = executionYear.getNext().getExecutionYear();

        assertEquals(currentInfo, degree.getMostRecentDegreeInfo(executionYear));
        assertEquals(currentInfo, degree.getMostRecentDegreeInfo(previousYear));
        assertEquals(currentInfo, degree.getMostRecentDegreeInfo(nextYear));

        // Create a new degree info for nextYear
        DegreeInfo newDegreeInfo = new DegreeInfo(currentInfo, nextYear);

        assertEquals(currentInfo, degree.getMostRecentDegreeInfo(executionYear));
        assertEquals(currentInfo, degree.getMostRecentDegreeInfo(previousYear));
        assertEquals(newDegreeInfo, degree.getMostRecentDegreeInfo(nextYear));
    }
}