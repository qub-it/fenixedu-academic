package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.StudentTest.REGISTRATION_STATE_INTERRUPTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
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
        assertNull(Degree.find(null));
    }

    @Test
    public void testDegree_getActiveDegreeCurricularPlans() {
        Degree testDegree = createDegree(degreeType, "ACTIVE_DCP_TEST", "Degree for Active DCPs test", executionYear);

        assertTrue(testDegree.getActiveDegreeCurricularPlans().isEmpty());

        DegreeCurricularPlan dcpA = new DegreeCurricularPlan(testDegree, "Active DCP A", AcademicPeriod.THREE_YEAR);
        DegreeCurricularPlan dcpB = new DegreeCurricularPlan(testDegree, "Active DCP B", AcademicPeriod.THREE_YEAR);

        assertEquals(2, testDegree.getActiveDegreeCurricularPlans().size());

        dcpA.setActive(false);

        List<DegreeCurricularPlan> activePlans = testDegree.getActiveDegreeCurricularPlans();
        assertEquals(1, activePlans.size());
        assertTrue(activePlans.contains(dcpB));
    }

    @Test
    public void testDegree_getDegreeCurricularPlansForYear() {
        Degree testDegree = createDegree(degreeType, "YEAR_DCP_TEST", "Degree for DCPs by year test", executionYear);

        assertTrue(testDegree.getDegreeCurricularPlansForYear(executionYear).isEmpty());

        DegreeCurricularPlan dcpA = new DegreeCurricularPlan(testDegree, "Year DCP A", AcademicPeriod.THREE_YEAR);
        dcpA.createExecutionDegree(executionYear);
        DegreeCurricularPlan dcpB = new DegreeCurricularPlan(testDegree, "Year DCP B", AcademicPeriod.THREE_YEAR);
        dcpB.createExecutionDegree(executionYear.getNext().getExecutionYear());

        List<DegreeCurricularPlan> currentYearDCPs = testDegree.getDegreeCurricularPlansForYear(executionYear);
        assertEquals(1, currentYearDCPs.size());
        assertEquals(dcpA, currentYearDCPs.get(0));

        List<DegreeCurricularPlan> nextYearDCPs =
                testDegree.getDegreeCurricularPlansForYear(executionYear.getNext().getExecutionYear());
        assertEquals(1, nextYearDCPs.size());
        assertEquals(dcpB, nextYearDCPs.get(0));
    }

    @Test
    public void testDegree_getExecutionDegrees() {
        Degree testDegree = createDegree(degreeType, "EXEC_DEGREE_TEST", "Degree for Execution Degrees test", executionYear);

        assertTrue(testDegree.getExecutionDegrees().isEmpty());

        DegreeCurricularPlan dcpA = new DegreeCurricularPlan(testDegree, "Execution DCP A", AcademicPeriod.THREE_YEAR);
        ExecutionDegree executionDegreeA = dcpA.createExecutionDegree(executionYear);
        DegreeCurricularPlan dcpB = new DegreeCurricularPlan(testDegree, "Execution DCP B", AcademicPeriod.THREE_YEAR);
        ExecutionDegree executionDegreeB = dcpB.createExecutionDegree(executionYear.getNext().getExecutionYear());

        List<ExecutionDegree> executionDegrees = testDegree.getExecutionDegrees();
        assertEquals(2, executionDegrees.size());
        assertTrue(executionDegrees.contains(executionDegreeA));
        assertTrue(executionDegrees.contains(executionDegreeB));
    }

    @Test
    public void testDegree_getDegreeCurricularPlansExecutionYears() {
        Degree testDegree = createDegree(degreeType, "DCP_YEARS_TEST", "Degree for DCPs Execution Years test", executionYear);

        assertTrue(testDegree.getDegreeCurricularPlansExecutionYears().isEmpty());

        DegreeCurricularPlan dcpA = new DegreeCurricularPlan(testDegree, "Years DCP A", AcademicPeriod.THREE_YEAR);
        dcpA.createExecutionDegree(executionYear);
        DegreeCurricularPlan dcpB = new DegreeCurricularPlan(testDegree, "Years DCP B", AcademicPeriod.THREE_YEAR);
        dcpB.createExecutionDegree(executionYear.getNext().getExecutionYear());

        List<ExecutionYear> years = testDegree.getDegreeCurricularPlansExecutionYears();
        assertEquals(2, years.size());
        assertTrue(years.contains(executionYear));
        assertTrue(years.contains(executionYear.getNext().getExecutionYear()));
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
    public void testDegree_getMostRecentDegreeCurricularPlan() {
        Degree testDegree = createDegree(degreeType, "MOST_RECENT_DCP_TEST", "Degree for Most Recent DCP test", executionYear);

        assertNull(testDegree.getMostRecentDegreeCurricularPlan());

        DegreeCurricularPlan dcpA = new DegreeCurricularPlan(testDegree, "Most Recent DCP A", AcademicPeriod.THREE_YEAR);
        dcpA.createExecutionDegree(executionYear);

        assertEquals(dcpA, testDegree.getMostRecentDegreeCurricularPlan());

        DegreeCurricularPlan dcpB = new DegreeCurricularPlan(testDegree, "Most Recent DCP B", AcademicPeriod.THREE_YEAR);
        dcpB.createExecutionDegree(executionYear.getNext().getExecutionYear());

        assertEquals(dcpB, testDegree.getMostRecentDegreeCurricularPlan());

        DegreeCurricularPlan dcpC = new DegreeCurricularPlan(testDegree, "Most Recent DCP C", AcademicPeriod.THREE_YEAR);
        dcpC.createExecutionDegree(executionYear.getNext().getExecutionYear());

        // Both dcpB and dcpC have the same execution year = tie
        // Tiebreaker is InitialDate
        dcpB.setInitialDateYearMonthDay(new YearMonthDay(2026, 1, 1));
        dcpC.setInitialDateYearMonthDay(new YearMonthDay(2025, 1, 1));

        assertEquals(dcpB, testDegree.getMostRecentDegreeCurricularPlan());
    }

    @Test
    public void testDegree_getActiveRegistrations() {
        StudentTest.initRegistrationConfigEntities();

        Degree testDegree = createDegree(degreeType, "ACTIVE_REGS_TEST", "Degree for Active Registrations test", executionYear);

        DegreeCurricularPlan dcp = new DegreeCurricularPlan(testDegree, "DCP Active Registrations", AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        Student student = StudentTest.createStudent("John Doe", "johndoe@test.com");

        assertTrue(testDegree.getActiveRegistrations().isEmpty());

        Registration registration = StudentTest.createRegistration(student, dcp, executionYear);

        assertFalse(testDegree.getActiveRegistrations().isEmpty());
        assertTrue(testDegree.getActiveRegistrations().stream().anyMatch(r -> r.getStudent().equals(student)));

        // Set registration state to INTERRUPTED to assert that it is no longer active
        RegistrationStateType interruptedType = RegistrationStateType.findByCode(REGISTRATION_STATE_INTERRUPTED).orElseThrow();
        RegistrationState.createRegistrationState(registration, AccessControl.getPerson(), new DateTime(), interruptedType,
                executionYear.getFirstExecutionPeriod());

        assertTrue(testDegree.getActiveRegistrations().isEmpty());
    }

    @Test
    public void testDegree_getFirstDegreeCurricularPlan() {
        Degree testDegree = createDegree(degreeType, "FIRST_DCP_TEST", "Degree for First DCP test", executionYear);

        DegreeCurricularPlan dcpA = new DegreeCurricularPlan(testDegree, "First DCP A", AcademicPeriod.THREE_YEAR);
        DegreeCurricularPlan dcpB = new DegreeCurricularPlan(testDegree, "First DCP B", AcademicPeriod.THREE_YEAR);

        // No initialDate set, should return null
        assertNull(testDegree.getFirstDegreeCurricularPlan());

        dcpA.setInitialDateYearMonthDay(new YearMonthDay(2025, 6, 1));
        dcpB.setInitialDateYearMonthDay(new YearMonthDay(2024, 1, 1));

        assertEquals(dcpB, testDegree.getFirstDegreeCurricularPlan());
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