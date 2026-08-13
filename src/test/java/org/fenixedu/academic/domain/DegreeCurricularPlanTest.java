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

@RunWith(FenixFrameworkRunner.class)
public class DegreeCurricularPlanTest {

    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse;
    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;

    public static final String DCP_NAME_V1 = "DCP_NAME_V1";
    public static final String DCP_NAME_V2 = "DCP_NAME_V2";
    public static final String DCP_NAME_V3 = "DCP_NAME_V3";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            initDegreeCurricularPlan();
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

        assertNull(dcp.findExecutionDegree(executionYear).orElse(null));
        assertNull(dcp.findExecutionDegree(executionInterval).orElse(null));
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

}
