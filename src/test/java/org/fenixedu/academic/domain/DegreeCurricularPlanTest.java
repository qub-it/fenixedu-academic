package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_TYPE_CODE;
import static org.fenixedu.academic.domain.DegreeTest.MASTER_DEGREE_TYPE_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
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

    public static final String DCP_NAME_V1 = "DCP_NAME_V1";
    public static final String DCP_NAME_V2 = "DCP_NAME_V2";
    public static final String DCP_NAME_V3 = "DCP_NAME_V3";
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse;
    private static ExecutionYear currentYear, previousYear, nextYear;
    private static ExecutionInterval currentInterval;
    private static CurricularPeriod yearPeriod, semesterPeriod;
    private static CompetenceCourse competenceCourseA, competenceCourseB;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            initDegreeCurricularPlan();

            nextYear = currentYear.getNext().getExecutionYear();
            previousYear = currentYear.getPrevious().getExecutionYear();
            competenceCourseB = CompetenceCourse.find(CompetenceCourseTest.COURSE_B_CODE);

            if (RegistrationProtocol.findByCode(StudentTest.PROTOCOL_CODE) == null) {
                StudentTest.initRegistrationConfigEntities();
            }

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
        competenceCourseA = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(competenceCourseA);

        yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

        currentYear = ExecutionYear.findCurrent(null);
        currentInterval = currentYear.getFirstExecutionPeriod();
        new Context(degreeCurricularPlan.getRoot(), curricularCourse, semesterPeriod, currentInterval, null);
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
        assertEquals(1, allCurricularCourses.size());
        assertTrue(allCurricularCourses.contains(curricularCourse));
    }

    @Test
    public void testFindExecutionDegree() {
        DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        Degree testDegree = DegreeTest.createDegree(degreeType, "EXEC_DEGREE_TEST", "Exec Degree Test", currentYear);
        DegreeCurricularPlan dcp = new DegreeCurricularPlan(testDegree, "DCP Test", AcademicPeriod.THREE_YEAR, currentInterval);

        assertTrue(dcp.findExecutionDegree(currentYear).isEmpty());
        assertTrue(dcp.findExecutionDegree(currentInterval).isEmpty());
        assertTrue(dcp.findExecutionDegree(nextYear).isEmpty());

        ExecutionDegree executionDegree = dcp.createExecutionDegree(currentYear);

        assertEquals(executionDegree, dcp.findExecutionDegree(currentYear).orElse(null));
        assertEquals(executionDegree, dcp.findExecutionDegree(currentInterval).orElse(null));
        assertTrue(dcp.findExecutionDegree(nextYear).isEmpty());

        assertTrue(dcp.findExecutionDegree(null).isEmpty());

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

        new CourseGroup(dcp.getRoot(), "Group Later", "Group Later", currentInterval, null);
        new CourseGroup(dcp.getRoot(), "Group Earlier", "Group Earlier", previousYear.getFirstExecutionPeriod(), null);

        assertEquals(previousYear, dcp.getBegin());
    }

    @Test
    public void testDegreeCurricularPlan_comparatorByName() {
        DegreeCurricularPlan dcpA = createDegreeCurricularPlan("AAA");
        DegreeCurricularPlan dcpB = createDegreeCurricularPlan("BBB");
        DegreeCurricularPlan dcpC = createDegreeCurricularPlan("CCC");

        List<DegreeCurricularPlan> list = new ArrayList<>(List.of(dcpB, dcpC, dcpA));
        Comparator<DegreeCurricularPlan> comparator = DegreeCurricularPlan.COMPARATOR_BY_NAME;
        list.sort(comparator);

        assertEquals(dcpA, list.get(0));
        assertEquals(dcpB, list.get(1));
        assertEquals(dcpC, list.get(2));
        assertEquals(0, comparator.compare(dcpA, dcpA));
        assertTrue(comparator.compare(dcpA, dcpB) < 0);
        assertTrue(comparator.compare(dcpB, dcpA) > 0);
        assertTrue(comparator.compare(dcpA, dcpC) < 0);
        assertTrue(comparator.compare(dcpC, dcpA) > 0);
        assertTrue(comparator.compare(dcpB, dcpC) < 0);
        assertTrue(comparator.compare(dcpC, dcpB) > 0);
    }

    @Test
    public void testDegreeCurricularPlan_comparatorByPresentationName() {
        DegreeCurricularPlan dcpA = createDegreeCurricularPlan("PPP_AAA");
        DegreeCurricularPlan dcpB = createDegreeCurricularPlan("PPP_BBB");
        DegreeCurricularPlan dcpC = createDegreeCurricularPlan("PPP_CCC");

        List<DegreeCurricularPlan> list = new ArrayList<>(List.of(dcpB, dcpC, dcpA));
        Comparator<DegreeCurricularPlan> comparator = DegreeCurricularPlan.COMPARATOR_BY_PRESENTATION_NAME;
        list.sort(comparator);

        assertEquals(dcpA, list.get(0));
        assertEquals(dcpB, list.get(1));
        assertEquals(dcpC, list.get(2));
        assertEquals(0, comparator.compare(dcpA, dcpA));
        assertTrue(comparator.compare(dcpA, dcpB) < 0);
        assertTrue(comparator.compare(dcpB, dcpA) > 0);
        assertTrue(comparator.compare(dcpA, dcpC) < 0);
        assertTrue(comparator.compare(dcpC, dcpA) > 0);
        assertTrue(comparator.compare(dcpB, dcpC) < 0);
        assertTrue(comparator.compare(dcpC, dcpB) > 0);
    }

    @Test
    public void testDegreeCurricularPlan_comparatorByDegreeTypeAndSiglaAndName_comparesBySigla() {
        DegreeCurricularPlan dcpA = createDegreeCurricularPlan("REVERSE_NAME_C");
        DegreeCurricularPlan dcpB = createDegreeCurricularPlan("REVERSE_NAME_B");
        DegreeCurricularPlan dcpC = createDegreeCurricularPlan("REVERSE_NAME_A");

        // Test compare by reverseName
        dcpA.getDegree().setSigla("A");
        dcpB.getDegree().setSigla("A");
        dcpC.getDegree().setSigla("A");

        List<DegreeCurricularPlan> list = new ArrayList<>(List.of(dcpB, dcpC, dcpA));
        Comparator<DegreeCurricularPlan> comparator =
                DegreeCurricularPlan.DEGREE_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_EXECUTION_DEGREE_AND_DEGREE_CODE;
        list.sort(comparator);

        assertEquals(dcpA, list.get(0));
        assertEquals(dcpB, list.get(1));
        assertEquals(dcpC, list.get(2));

        // Test compare by Sigla
        dcpA.getDegree().setSigla("A");
        dcpB.getDegree().setSigla("B");
        dcpC.getDegree().setSigla("C");
        dcpA.setName("0_SCRAMBLE_NAMES");
        dcpB.setName("1_TO_MAKE_SURE");
        dcpC.setName("2_IT_IS_USING_SIGLA");

        list = new ArrayList<>(List.of(dcpB, dcpC, dcpA));
        list.sort(comparator);

        assertEquals(dcpA, list.get(0));
        assertEquals(dcpB, list.get(1));
        assertEquals(dcpC, list.get(2));

        // Test compare by DegreeType
        dcpC.getDegree().setDegreeType(DegreeType.findByCode(MASTER_DEGREE_TYPE_CODE).get());
        dcpA.getDegree().setSigla("1_SCRAMBLE_SIGLA");
        dcpB.getDegree().setSigla("2_TO_MAKE_SURE");
        dcpC.getDegree().setSigla("0_IT_IS_USING_DEGREE_TYPE");

        list = new ArrayList<>(List.of(dcpB, dcpC, dcpA));
        list.sort(comparator);
        assertEquals(dcpA, list.get(0));
        assertEquals(dcpB, list.get(1));
        assertEquals(dcpC, list.get(2));

        assertEquals(0, comparator.compare(dcpA, dcpA));
        assertTrue(comparator.compare(dcpA, dcpB) < 0);
        assertTrue(comparator.compare(dcpB, dcpA) > 0);
        assertTrue(comparator.compare(dcpA, dcpC) < 0);
        assertTrue(comparator.compare(dcpC, dcpA) > 0);
        assertTrue(comparator.compare(dcpB, dcpC) < 0);
        assertTrue(comparator.compare(dcpC, dcpB) > 0);
    }

    @Test
    public void testDegreeCurricularPlan_getExecutionCourses() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_GET_EXECUTION_COURSES");
        CurricularCourse curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(competenceCourseA);
        CourseGroup courseGroup = new CourseGroup(dcp.getRoot(), "Test Group", "Test Group", currentInterval, null);
        ExecutionCourse executionCourse = new ExecutionCourse("Test EC", "TEC", currentInterval);
        curricularCourse.addAssociatedExecutionCourses(executionCourse);

        assertTrue(dcp.getExecutionCourses(currentInterval).isEmpty());

        new Context(courseGroup, curricularCourse, semesterPeriod, currentInterval, null);
        assertTrue(dcp.getExecutionCourses(currentInterval).contains(executionCourse));

        // different interval returns empty set
        assertTrue(dcp.getExecutionCourses(nextYear.getFirstExecutionPeriod()).isEmpty());
    }

    @Test
    public void testDegreeCurricularPlan_getAllCurricularCourses() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_GET_ALL_CURR_COURSES");

        // empty DCP returns empty set
        assertTrue(dcp.getAllCurricularCourses().isEmpty());

        // courses are returned from child groups and sorted by name
        CurricularCourse courseA = new CurricularCourse();
        courseA.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseA, semesterPeriod, currentInterval, null);

        CourseGroup childGroup = new CourseGroup(dcp.getRoot(), "Nested Group", "Nested Group", currentInterval, null);
        CurricularCourse courseB = new CurricularCourse();
        courseB.setCompetenceCourse(competenceCourseB);
        new Context(childGroup, courseB, semesterPeriod, currentInterval, null);

        assertEquals(List.of(courseA, courseB), new ArrayList<>(dcp.getAllCurricularCourses()));
    }

    @Test
    public void testDegreeCurricularPlan_getCurricularCourses() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_GET_CURR_COURSES");

        // empty DCP returns empty set
        assertTrue(dcp.getCurricularCourses(currentInterval).isEmpty());

        // course with context in the queried interval is returned
        CurricularCourse courseA = new CurricularCourse();
        courseA.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseA, semesterPeriod, currentInterval, currentInterval);

        assertTrue(dcp.getCurricularCourses(currentInterval).contains(courseA));

        // multiple courses across intervals - only matching ones returned
        ExecutionInterval nextInterval = nextYear.getFirstExecutionPeriod();
        CurricularCourse courseB = new CurricularCourse();
        courseB.setCompetenceCourse(competenceCourseB);
        new Context(dcp.getRoot(), courseB, semesterPeriod, nextInterval, nextInterval);

        Set<CurricularCourse> resultCurrent = dcp.getCurricularCourses(currentInterval);
        assertEquals(1, resultCurrent.size());
        assertTrue(resultCurrent.contains(courseA));

        Set<CurricularCourse> resultNext = dcp.getCurricularCourses(nextInterval);
        assertEquals(1, resultNext.size());
        assertTrue(resultNext.contains(courseB));

        // TODO FIX: returns courses for the whole execution year instead of the exact interval
        // a context that only exists in the 2nd semester is returned when querying the 1st semester.
        ExecutionInterval secondSemesterInterval = currentYear.getLastExecutionPeriod();
        CurricularCourse courseC = new CurricularCourse();
        courseC.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseC, semesterPeriod, secondSemesterInterval, secondSemesterInterval);

        Set<CurricularCourse> firstSemesterCourses = dcp.getCurricularCourses(currentYear.getFirstExecutionPeriod());
        assertEquals(2, firstSemesterCourses.size());
        assertTrue(firstSemesterCourses.contains(courseA));
        assertTrue(firstSemesterCourses.contains(courseC));
    }

    @Test
    public void testDegreeCurricularPlan_getCompetenceCourses() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_COMP_COURSES");

        // empty DCP returns empty list
        assertTrue(dcp.getCompetenceCourses(currentYear).isEmpty());

        // excludes optionalCurricularCourses
        CurricularCourse optional =
                dcp.createOptionalCurricularCourse(dcp.getRoot(), "Op", "Op", semesterPeriod, currentInterval, null);
        optional.setCompetenceCourse(competenceCourseA);

        assertTrue(dcp.getCompetenceCourses(currentYear).isEmpty());

        // returns non-optional courses sorted by CompetenceCourse name
        CurricularCourse courseA = new CurricularCourse();
        courseA.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseA, semesterPeriod, currentInterval, null);

        CurricularCourse courseB = new CurricularCourse();
        courseB.setCompetenceCourse(competenceCourseB);
        new Context(dcp.getRoot(), courseB, semesterPeriod, currentInterval, null);

        assertEquals(List.of(competenceCourseA, competenceCourseB), dcp.getCompetenceCourses(currentYear));

        // distinct() - two CurricularCourses share the same CompetenceCourse
        CurricularCourse courseA2 = new CurricularCourse();
        courseA2.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseA2, semesterPeriod, currentInterval, null);

        assertEquals(List.of(competenceCourseA, competenceCourseB), dcp.getCompetenceCourses(currentYear));
    }

    @Test
    public void testDegreeCurricularPlan_getActiveCurricularCourses() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_ACTIVE_COURSES");

        // empty DCP returns empty set
        assertTrue(dcp.getActiveCurricularCourses(currentInterval).isEmpty());

        // returns only courses with an active context for the requested interval
        CurricularCourse course = new CurricularCourse();
        course.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), course, semesterPeriod, currentInterval, currentInterval);

        assertTrue(dcp.getActiveCurricularCourses(currentInterval).contains(course));
        assertTrue(dcp.getActiveCurricularCourses(nextYear.getFirstExecutionPeriod()).isEmpty());
        assertTrue(dcp.getActiveCurricularCourses(previousYear.getFirstExecutionPeriod()).isEmpty());
    }

    @Test
    public void testDegreeCurricularPlan_getCurricularCoursesByExecutionYearAndCurricularYear() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_FILTER_CURR_YEAR");
        CurricularPeriod year1 = new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        CurricularPeriod year2 = new CurricularPeriod(AcademicPeriod.YEAR, 2, degreeCurricularPlan.getDegreeStructure());
        CurricularPeriod semester1Y1 = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, year1);
        CurricularPeriod semester1Y2 = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, year2);

        // filters correctly by curricular year
        CurricularCourse courseA = new CurricularCourse();
        courseA.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseA, semester1Y1, currentInterval, null);
        ExecutionCourse executionCourseA = new ExecutionCourse("ECA", "ECA", currentInterval);
        courseA.addAssociatedExecutionCourses(executionCourseA);

        assertEquals(Set.of(courseA), dcp.getCurricularCoursesByExecutionYearAndCurricularYear(currentYear, 1));
        assertTrue(dcp.getCurricularCoursesByExecutionYearAndCurricularYear(currentYear, 2).isEmpty());

        // adding a second course in year 2 - each returned only for its matching curricular year
        CurricularCourse courseB = new CurricularCourse();
        courseB.setCompetenceCourse(competenceCourseB);
        new Context(dcp.getRoot(), courseB, semester1Y2, currentInterval, null);
        ExecutionCourse executionCourseB = new ExecutionCourse("ECB", "ECB", currentInterval);
        courseB.addAssociatedExecutionCourses(executionCourseB);

        assertEquals(Set.of(courseA), dcp.getCurricularCoursesByExecutionYearAndCurricularYear(currentYear, 1));
        assertEquals(Set.of(courseB), dcp.getCurricularCoursesByExecutionYearAndCurricularYear(currentYear, 2));

        // querying for a curricular year that doesn't exist returns empty
        assertTrue(dcp.getCurricularCoursesByExecutionYearAndCurricularYear(currentYear, 99).isEmpty());

        // querying for a different execution year returns empty
        assertTrue(dcp.getCurricularCoursesByExecutionYearAndCurricularYear(nextYear, 1).isEmpty());
    }

    @Test
    public void testDegreeCurricularPlan_getCurricularCoursesWithExecutionIn() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_CURR_COURSES_WITH_EXEC");

        // empty DCP returns empty list
        assertTrue(dcp.getCurricularCoursesWithExecutionIn(currentYear).isEmpty());

        // course without execution course is not returned
        CurricularCourse courseB = new CurricularCourse();
        courseB.setCompetenceCourse(competenceCourseB);
        new Context(dcp.getRoot(), courseB, semesterPeriod, currentInterval, null);
        assertTrue(dcp.getCurricularCoursesWithExecutionIn(currentYear).isEmpty());

        // course with execution course in current year is returned
        CurricularCourse courseA = new CurricularCourse();
        courseA.setCompetenceCourse(competenceCourseA);
        new Context(dcp.getRoot(), courseA, semesterPeriod, currentInterval, null);
        ExecutionCourse executionCourseA = new ExecutionCourse("ECA", "ECA", currentInterval);
        courseA.addAssociatedExecutionCourses(executionCourseA);

        List<CurricularCourse> resultCurrentYear = dcp.getCurricularCoursesWithExecutionIn(currentYear);
        assertEquals(1, resultCurrentYear.size());
        assertTrue(resultCurrentYear.contains(courseA));

        // querying for a different year returns empty
        assertTrue(dcp.getCurricularCoursesWithExecutionIn(nextYear).isEmpty());

        // course with execution in next year is returned for next year
        ExecutionInterval nextInterval = nextYear.getFirstExecutionPeriod();
        ExecutionCourse executionCourseB = new ExecutionCourse("ECB", "ECB", nextInterval);
        courseB.addAssociatedExecutionCourses(executionCourseB);

        List<CurricularCourse> resultNextYear = dcp.getCurricularCoursesWithExecutionIn(nextYear);
        assertEquals(1, resultNextYear.size());
        assertTrue(resultNextYear.contains(courseB));

        // course with execution in both years appears in both results
        ExecutionCourse executionCourseAForNextYear = new ExecutionCourse("ECA2", "ECA2", nextInterval);
        courseA.addAssociatedExecutionCourses(executionCourseAForNextYear);

        List<CurricularCourse> resultNextYearWithA = dcp.getCurricularCoursesWithExecutionIn(nextYear);
        assertEquals(2, resultNextYearWithA.size());
        assertTrue(resultNextYearWithA.contains(courseA));
        assertTrue(resultNextYearWithA.contains(courseB));

        List<CurricularCourse> resultCurrentYearWithA = dcp.getCurricularCoursesWithExecutionIn(currentYear);
        assertEquals(1, resultCurrentYearWithA.size());
        assertTrue(resultCurrentYearWithA.contains(courseA));

        // course with execution in multiple intervals of same year appears only once
        ExecutionInterval secondSemester = currentYear.getLastExecutionPeriod();
        ExecutionCourse executionCourseASecondSemester = new ExecutionCourse("ECA3", "ECA3", secondSemester);
        courseA.addAssociatedExecutionCourses(executionCourseASecondSemester);

        List<CurricularCourse> resultCurrentYearDeduped = dcp.getCurricularCoursesWithExecutionIn(currentYear);
        assertEquals(1, resultCurrentYearDeduped.size());
        assertTrue(resultCurrentYearDeduped.contains(courseA));
    }

    @Test
    public void testDegreeCurricularPlan_getActiveStudentCurricularPlans() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_ACTIVE_SCP");

        // empty DCP returns empty set
        assertTrue(dcp.getActiveStudentCurricularPlans().isEmpty());

        // active registration creates an active student curricular plan
        dcp.createExecutionDegree(currentYear);
        Student student = StudentTest.createStudent("SCP Active Test", "scp.active.test");
        Registration registration = StudentTest.createRegistration(student, dcp, currentYear);

        Collection<StudentCurricularPlan> activePlans = dcp.getActiveStudentCurricularPlans();
        assertEquals(1, activePlans.size());

        StudentCurricularPlan activePlan = activePlans.iterator().next();
        assertEquals(registration, activePlan.getRegistration());
        assertEquals(dcp, activePlan.getDegreeCurricularPlan());

        // concluded registration makes the plan inactive
        RegistrationStateType interruptedType = RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).get();
        RegistrationState.createRegistrationState(registration, null, null, interruptedType,
                currentYear.getFirstExecutionPeriod());

        assertTrue(dcp.getActiveStudentCurricularPlans().isEmpty());

        // re-activating makes the plan active again
        RegistrationStateType registeredType = RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get();
        RegistrationState.createRegistrationState(registration, null, null, registeredType,
                currentYear.getFirstExecutionPeriod());

        assertEquals(1, dcp.getActiveStudentCurricularPlans().size());
    }

    @Test
    public void testDegreeCurricularPlan_getActiveRegistrations() {
        DegreeCurricularPlan dcp = createDegreeCurricularPlan("DCP_ACTIVE_REG");

        // empty DCP returns empty set
        assertTrue(dcp.getActiveRegistrations().isEmpty());

        // active registration is returned
        dcp.createExecutionDegree(currentYear);
        Student student = StudentTest.createStudent("Reg Active Test", "reg.active.test");
        Registration registration = StudentTest.createRegistration(student, dcp, currentYear);

        Collection<Registration> activeRegistrations = dcp.getActiveRegistrations();
        assertEquals(1, activeRegistrations.size());
        assertTrue(activeRegistrations.contains(registration));

        // concluded registration is not returned
        RegistrationStateType interruptedType = RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).get();
        RegistrationState.createRegistrationState(registration, null, null, interruptedType,
                currentYear.getFirstExecutionPeriod());

        assertTrue(dcp.getActiveRegistrations().isEmpty());

        // re-activating registration makes it returned again
        RegistrationStateType registeredType = RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get();
        RegistrationState.createRegistrationState(registration, null, null, registeredType,
                currentYear.getFirstExecutionPeriod());

        assertEquals(1, dcp.getActiveRegistrations().size());
        assertTrue(dcp.getActiveRegistrations().contains(registration));
    }
}
