package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_TYPE_CODE;
import static org.junit.Assert.assertFalse;

@RunWith(FenixFrameworkRunner.class)
public class DegreeCurricularPlanTest {

    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse;
    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;

    public static final String DCP_NAME_V1 = "DCP_NAME_V1";
    public static final String DCP_NAME_V2 = "DCP_NAME_V2";
    public static final String DCP_NAME_V3 = "DCP_NAME_V3";

    private static ExecutionYear currentYear, previousYear, nextYear;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            initDegreeCurricularPlan();

            currentYear = ExecutionYear.findCurrent(null);
            nextYear = (ExecutionYear) currentYear.getNext();
            previousYear = (ExecutionYear) currentYear.getPrevious();

            return null;
        });
    }

    static void initDegreeCurricularPlan() {
        DegreeTest.initDegree();
        final Degree degree = Degree.find(DEGREE_A_CODE);

        UserUtil.initAdminUser();

        degreeCurricularPlan = new DegreeCurricularPlan(degree, DCP_NAME_V1, AcademicPeriod.THREE_YEAR);

        new DegreeCurricularPlan(degree, DCP_NAME_V2, AcademicPeriod.THREE_YEAR);
        new DegreeCurricularPlan(degree, DCP_NAME_V3, AcademicPeriod.THREE_YEAR);

        CompetenceCourseTest.initCompetenceCourse();
        final CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(competenceCourse);

        final CurricularPeriod yearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

        executionYear = ExecutionYear.findCurrent(null);
        executionInterval = executionYear.getFirstExecutionPeriod();
        new Context(degreeCurricularPlan.getRoot(), curricularCourse, semesterPeriod, executionInterval, null);
    }

    private static DegreeCurricularPlan createDegreeCurricularPlan(final String dcpName) {
        DegreeType degreeType = DegreeType.findByCode(DEGREE_TYPE_CODE).get();
        Degree degree = DegreeTest.createDegree(degreeType, "DCP_TEST_" + UUID.randomUUID(), "Degree " + dcpName, currentYear);
        return new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR, currentYear);
    }

    @Test
    public void createDCP_withExistingName() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final String dcpName = UUID.randomUUID().toString();
        new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.existing.name.and.degree");

        new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR);
    }

    @Test
    public void editDCP_withExistingName() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final String dcp1Name = UUID.randomUUID().toString();
        final DegreeCurricularPlan dcp1 = new DegreeCurricularPlan(degree, dcp1Name, AcademicPeriod.THREE_YEAR);

        dcp1.setName(dcp1Name); // no error, it's the same dcp

        final String dcp2Name = UUID.randomUUID().toString();
        final DegreeCurricularPlan dcp2 = new DegreeCurricularPlan(degree, dcp2Name, AcademicPeriod.THREE_YEAR);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.existing.name.and.degree");

        dcp2.setName(dcp1Name);
    }

    @Test
    public void findDegree_byCode() {
        final Degree degreeCS = Degree.find(DEGREE_A_CODE);
        assertTrue(degreeCS.getDegreeCurricularPlansSet().contains(degreeCurricularPlan));
    }

    @Test
    public void getAllCurricularCourses() {
        final Set<CurricularCourse> allCurricularCourses = degreeCurricularPlan.getAllCurricularCourses();
        assertTrue(allCurricularCourses.size() == 1);
        assertTrue(allCurricularCourses.contains(curricularCourse));
    }

    @Test
    public void testFindExecutionDegree() {
        ExecutionYear next = executionYear.getNext().getExecutionYear();

        DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        Degree testDegree = DegreeTest.createDegree(degreeType, "EXEC_DEGREE_TEST", "Exec Degree Test", executionYear);
        DegreeCurricularPlan dcp = new DegreeCurricularPlan(testDegree, "DCP Test", AcademicPeriod.THREE_YEAR, executionInterval);

        assertTrue(dcp.findExecutionDegree(executionYear).isEmpty());
        assertTrue(dcp.findExecutionDegree(executionInterval).isEmpty());
        assertTrue(dcp.findExecutionDegree(next).isEmpty());

        ExecutionDegree executionDegree = dcp.createExecutionDegree(executionYear);

        assertEquals(executionDegree, dcp.findExecutionDegree(executionYear).orElse(null));
        assertEquals(executionDegree, dcp.findExecutionDegree(executionInterval).orElse(null));
        assertTrue(dcp.findExecutionDegree(next).isEmpty());

        assertTrue(dcp.findExecutionDegree(null).isEmpty());

        executionDegree.delete();
        dcp.delete();
        testDegree.delete();
    }

    @Test
    public void testFindExecutionDegree_AcademicInterval_matches_ExecutionInterval() {
        ExecutionYear previous = executionYear.getPrevious().getExecutionYear();
        ExecutionYear next = executionYear.getNext().getExecutionYear();

        DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        Degree testDegree = DegreeTest.createDegree(degreeType, "EXEC_DEGREE_TEST", "Exec Degree Test", executionYear);
        DegreeCurricularPlan dcp = new DegreeCurricularPlan(testDegree, "DCP Test", AcademicPeriod.THREE_YEAR, executionInterval);

        ExecutionDegree executionDegree = dcp.createExecutionDegree(executionYear);

        assertEquals(executionDegree, dcp.findExecutionDegree(executionYear).orElse(null));
        assertEquals(executionDegree, dcp.getExecutionDegreeByAcademicInterval(executionYear.getAcademicInterval()));

        assertEquals(executionDegree, dcp.findExecutionDegree(executionInterval).orElse(null));
        assertEquals(executionDegree, dcp.getExecutionDegreeByAcademicInterval(executionInterval.getAcademicInterval()));

        assertTrue(dcp.findExecutionDegree(previous).isEmpty());
        assertNull(dcp.getExecutionDegreeByAcademicInterval(previous.getAcademicInterval()));

        assertTrue(dcp.findExecutionDegree(next).isEmpty());
        assertNull(dcp.getExecutionDegreeByAcademicInterval(next.getAcademicInterval()));

        executionDegree.delete();
        dcp.delete();
        testDegree.delete();
    }

    @Test
    public void testDegreeCurricularPlan_getExecutionYears() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_GET_EXECUTION_YEARS");

        assertTrue(dcp.getExecutionYears().isEmpty());

        dcp.createExecutionDegree(currentYear);
        dcp.createExecutionDegree(previousYear);
        dcp.createExecutionDegree(nextYear);

        Set<ExecutionYear> executionYears = dcp.getExecutionYears();
        assertEquals(3, executionYears.size());
        assertTrue(executionYears.contains(currentYear));
        assertTrue(executionYears.contains(previousYear));
        assertTrue(executionYears.contains(nextYear));
    }

    @Test
    public void testDegreeCurricularPlan_hasAnyExecutionDegreeFor() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_HAS_ANY_EXECUTION_DEGREE");

        assertFalse(dcp.hasAnyExecutionDegreeFor(null));
        assertFalse(dcp.hasAnyExecutionDegreeFor(currentYear));

        dcp.createExecutionDegree(currentYear);
        assertTrue(dcp.hasAnyExecutionDegreeFor(currentYear));
    }

    @Test
    public void testDegreeCurricularPlan_getMostRecentExecutionDegree() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_MOST_RECENT_EXECUTION_DEGREE");
        ExecutionYear afterNextExecutionYear = (ExecutionYear) nextYear.getNext();

        assertNull(dcp.getMostRecentExecutionDegree());

        ExecutionDegree currentYearExecutionDegree = dcp.createExecutionDegree(currentYear);
        ExecutionDegree previousYearExecutionDegree = dcp.createExecutionDegree(previousYear);
        ExecutionDegree nextYearExecutionDegree = dcp.createExecutionDegree(nextYear);
        dcp.createExecutionDegree(afterNextExecutionYear);

        // current year wins over past and future
        assertEquals(currentYearExecutionDegree, dcp.getMostRecentExecutionDegree());

        // no current year -> latest past year wins over future years
        dcp.removeExecutionDegrees(currentYearExecutionDegree);
        assertEquals(previousYearExecutionDegree, dcp.getMostRecentExecutionDegree());

        // no past/current years -> earliest future year
        dcp.removeExecutionDegrees(previousYearExecutionDegree);
        assertEquals(nextYearExecutionDegree, dcp.getMostRecentExecutionDegree());
    }

    @Test
    public void testDegreeCurricularPlan_readBolonhaDegreeCurricularPlans() {
        DegreeCurricularPlan dcp1 = createDegreeCurricularPlan("DCP_READ_BOLONHA_1");
        DegreeCurricularPlan dcp2 = createDegreeCurricularPlan("DCP_READ_BOLONHA_2");

        Set<DegreeCurricularPlan> result = DegreeCurricularPlan.readBolonhaDegreeCurricularPlans();
        assertTrue(result.contains(dcp1));
        assertTrue(result.contains(dcp2));
    }

    @Test
    public void testDegreeCurricularPlan_readByNameAndDegreeSigla() {
        String dcpName = "DCP_READ_BY_NAME_AND_SIGLA";
        DegreeCurricularPlan dcp = createDegreeCurricularPlan(dcpName);
        String sigla = dcp.getDegree().getSigla();

        assertEquals(dcp, DegreeCurricularPlan.readByNameAndDegreeSigla(dcpName, sigla));
        assertEquals(dcp, DegreeCurricularPlan.readByNameAndDegreeSigla(dcpName.toLowerCase(), sigla.toLowerCase()));

        assertNull(DegreeCurricularPlan.readByNameAndDegreeSigla("no match", sigla));
        assertNull(DegreeCurricularPlan.readByNameAndDegreeSigla(dcpName, "no match"));
        assertNull(DegreeCurricularPlan.readByNameAndDegreeSigla(null, sigla));
        assertNull(DegreeCurricularPlan.readByNameAndDegreeSigla(dcpName, null));
    }

    @Test
    public void testDegreeCurricularPlan_getBegin() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_GET_BEGIN");
        assertNull(dcp.getBegin());

        new CourseGroup(dcp.getRoot(), "Group Later", "Group Later", currentYear.getFirstExecutionPeriod(), null);
        new CourseGroup(dcp.getRoot(), "Group Earlier", "Group Earlier", previousYear.getFirstExecutionPeriod(), null);

        assertEquals(previousYear, dcp.getBegin());
    }
}
