package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionCourseTest {

    private static Registration registration;
    private static Student student;
    private static ExecutionCourse executionCourse;

    private static Registration nonAttendingRegistration;
    private static Student studentWithoutRegistrations;
    private static Registration regA;
    private static Registration regB;
    private ExecutionCourse emptyExecutionCourse;
    private static ExecutionDegree executionDegree;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();

            student = Student.readStudentByNumber(1);
            registration = student.getRegistrationStream().findAny().orElseThrow();
            executionCourse = registration.getAssociatedAttendsSet().iterator().next().getExecutionCourse();

            nonAttendingRegistration = createNewRegistration("Different", "different." + UUID.randomUUID());
            studentWithoutRegistrations = StudentTest.createStudent("Other", "other." + UUID.randomUUID());

            regA = createNewRegistration("RegA", "reg.a." + UUID.randomUUID());
            regB = createNewRegistration("RegB", "reg.b." + UUID.randomUUID());

            final CurricularCourse cc = executionCourse.getAssociatedCurricularCoursesSet().iterator().next();
            final DegreeCurricularPlan dcp = cc.getDegreeCurricularPlan();
            executionDegree = dcp.findExecutionDegree(executionCourse.getExecutionInterval()).orElse(null);

            return null;
        });
    }

    @Before
    public void setUp() {
        emptyExecutionCourse = createEmptyExecutionCourse();
    }

    private static ExecutionCourse createEmptyExecutionCourse() {
        final String uuid = UUID.randomUUID().toString();
        final ExecutionInterval interval = ExecutionInterval.findFirstCurrentChild(null);
        return new ExecutionCourse(uuid, uuid, interval);
    }

    private static Registration createNewRegistration(final String name, final String username) {
        final Student s = StudentTest.createStudent(name, username);
        final Degree degree = Degree.find(DEGREE_A_CODE);
        assertNotNull(degree);
        final DegreeCurricularPlan dcp = degree.getDegreeCurricularPlansSet().stream()
                .filter(p -> DCP_NAME_V1.equals(p.getName())).findAny().orElseThrow();
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        return StudentTest.createRegistration(s, dcp, executionYear);
    }

    @Test
    public void getAttendsByStudent_withRegistration_matchFound() {
        final Attends result = executionCourse.getAttendsByStudent(registration);
        assertNotNull(result);
        assertSame(registration, result.getRegistration());
    }

    @Test
    public void getAttendsByStudent_withRegistration_filtersAmongMultipleAttends() {
        final Attends a = new Attends(regA, emptyExecutionCourse);
        new Attends(regB, emptyExecutionCourse);
        assertSame(a, emptyExecutionCourse.getAttendsByStudent(regA));
    }

    @Test
    public void getAttendsByStudent_withStudent_matchFound() {
        final Attends result = executionCourse.getAttendsByStudent(student);
        assertNotNull(result);
        assertTrue(result.isFor(student));
    }

    @Test
    public void getAttendsByStudent_withNull() {
        assertNull(executionCourse.getAttendsByStudent((Registration) null));
        assertNull(executionCourse.getAttendsByStudent((Student) null));
    }

    @Test
    public void getAttendsByStudent_overloadCoherence() {
        final Attends attends = new Attends(regA, emptyExecutionCourse);
        final Student student = regA.getStudent();
        assertSame(attends, emptyExecutionCourse.getAttendsByStudent(regA));
        assertSame(attends, emptyExecutionCourse.getAttendsByStudent(student));
    }

    @Test
    public void getAttendsByStudent_withStudentWithoutRegistrations_returnsNull() {
        new Attends(regA, emptyExecutionCourse);
        assertNull(emptyExecutionCourse.getAttendsByStudent(studentWithoutRegistrations));
    }

    private static SchoolClass createSchoolClassFor(final ExecutionCourse ec, final DegreeCurricularPlan dcp, final String name) {
        final ExecutionInterval interval = ec.getExecutionInterval();
        final ExecutionDegree executionDegree = dcp.findExecutionDegree(interval).orElseThrow();
        final SchoolClass schoolClass = new SchoolClass(executionDegree, interval, name, 1);
        final Shift shift = new Shift(ec, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        shift.addAssociatedClasses(schoolClass);
        return schoolClass;
    }

    @Test
    public void testSetSigla_sameValue() {
        final String s = "SAME_" + UUID.randomUUID();
        emptyExecutionCourse.setSigla(s);
        emptyExecutionCourse.setSigla(s);
        assertEquals(s, emptyExecutionCourse.getSigla());
    }

    @Test
    public void testSetSigla_conflicts() {
        final ExecutionInterval interval = emptyExecutionCourse.getExecutionInterval();
        final String target = "CONFLICT_" + UUID.randomUUID();

        // existing course holds the sigla in uppercase
        final ExecutionCourse other = new ExecutionCourse("Other", UUID.randomUUID().toString(), interval);
        other.setSigla(target.toUpperCase());

        // first conflict is detected case-insensitively -> target-0
        emptyExecutionCourse.setSigla(target.toLowerCase());
        assertEquals(target.toLowerCase() + "-0", emptyExecutionCourse.getSigla());

        // second conflict -> target-1
        final ExecutionCourse anotherCourse = new ExecutionCourse("Another", UUID.randomUUID().toString(), interval);
        anotherCourse.setSigla(target.toLowerCase());
        assertEquals(target.toLowerCase() + "-1", anotherCourse.getSigla());
    }

    @Test
    public void testSetSigla_normalizesSpecialCharacters() {
        emptyExecutionCourse.setSigla("B C");
        assertEquals("B_C", emptyExecutionCourse.getSigla());

        emptyExecutionCourse.setSigla("A/B");
        assertEquals("A-B", emptyExecutionCourse.getSigla());

        emptyExecutionCourse.setSigla("A/B C");
        assertEquals("A-B_C", emptyExecutionCourse.getSigla());
    }

    @Test
    public void testGetSchoolClasses() {
        final DegreeCurricularPlan dcp = registration.getLastDegreeCurricularPlan();
        final SchoolClass schoolClass = createSchoolClassFor(emptyExecutionCourse, dcp, "TestClass");
        assertEquals(Set.of(schoolClass), emptyExecutionCourse.getSchoolClasses());
    }

    @Test
    public void testGetSchoolClasses_multiple() {
        final DegreeCurricularPlan dcp = registration.getLastDegreeCurricularPlan();
        final SchoolClass a = createSchoolClassFor(emptyExecutionCourse, dcp, "Multi_A");
        final SchoolClass b = createSchoolClassFor(emptyExecutionCourse, dcp, "Multi_B");
        assertEquals(Set.of(a, b), emptyExecutionCourse.getSchoolClasses());
    }

    @Test
    public void testGetSchoolClassesBy() {
        final DegreeCurricularPlan dcp = registration.getLastDegreeCurricularPlan();
        final SchoolClass schoolClass = createSchoolClassFor(emptyExecutionCourse, dcp, "TestClass_By");

        // matches the DCP the school class belongs to
        assertEquals(Set.of(schoolClass), emptyExecutionCourse.getSchoolClassesBy(dcp));

        // does not match a different DCP
        final DegreeCurricularPlan otherDcp =
                dcp.getDegree().getDegreeCurricularPlansSet().stream().filter(d -> !d.equals(dcp)).findAny().orElseThrow();
        assertTrue(emptyExecutionCourse.getSchoolClassesBy(otherDcp).isEmpty());
    }

    @Test
    public void testGetCompetenceCourses() {
        final Set<CompetenceCourse> competenceCourses = executionCourse.getCompetenceCourses();
        assertFalse(competenceCourses.isEmpty());
        assertEquals(1, competenceCourses.size());
        final CompetenceCourse competenceCourse = competenceCourses.iterator().next();
        assertEquals(CompetenceCourseTest.COURSE_A_CODE, competenceCourse.getCode());
    }

    @Test
    public void testGetCompetenceCourses_multipleCurricularCoursesSameCompetenceCourse() {
        final DegreeCurricularPlan dcp =
                executionCourse.getAssociatedCurricularCoursesSet().iterator().next().getDegreeCurricularPlan();
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);

        // two CurricularCourse instances sharing the same CompetenceCourse, associated with an empty execution course
        final CompetenceCourse sharedCompetenceCourse =
                CompetenceCourseTest.createCompetenceCourse("Shared Course", "SHR" + UUID.randomUUID(), new BigDecimal("6.0"),
                        AcademicPeriod.SEMESTER, currentInterval, coursesUnit);

        final CurricularCourse cc1 = createCurricularCourse(dcp, sharedCompetenceCourse, currentInterval);
        final CurricularCourse cc2 = createCurricularCourse(dcp, sharedCompetenceCourse, currentInterval);
        cc1.addAssociatedExecutionCourses(emptyExecutionCourse);
        cc2.addAssociatedExecutionCourses(emptyExecutionCourse);

        final Set<CompetenceCourse> result = emptyExecutionCourse.getCompetenceCourses();
        assertEquals(1, result.size());
        assertTrue(result.contains(sharedCompetenceCourse));
    }

    @Test
    public void testGetCompetenceCourses_multipleCurricularCoursesDifferentCompetenceCourses() {
        final DegreeCurricularPlan dcp =
                executionCourse.getAssociatedCurricularCoursesSet().iterator().next().getDegreeCurricularPlan();
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);

        // two CurricularCourse instances, each with a different CompetenceCourse, associated with an empty execution course
        final CompetenceCourse competenceCourseA =
                CompetenceCourseTest.createCompetenceCourse("Course X", "CX" + UUID.randomUUID(), new BigDecimal("6.0"),
                        AcademicPeriod.SEMESTER, currentInterval, coursesUnit);
        final CompetenceCourse competenceCourseB =
                CompetenceCourseTest.createCompetenceCourse("Course Y", "CY" + UUID.randomUUID(), new BigDecimal("6.0"),
                        AcademicPeriod.SEMESTER, currentInterval, coursesUnit);

        final CurricularCourse ccA = createCurricularCourse(dcp, competenceCourseA, currentInterval);
        final CurricularCourse ccB = createCurricularCourse(dcp, competenceCourseB, currentInterval);
        ccA.addAssociatedExecutionCourses(emptyExecutionCourse);
        ccB.addAssociatedExecutionCourses(emptyExecutionCourse);

        final Set<CompetenceCourse> result = emptyExecutionCourse.getCompetenceCourses();
        assertEquals(2, result.size());
        assertTrue(result.contains(competenceCourseA));
        assertTrue(result.contains(competenceCourseB));
    }

    @Test
    public void testGetCompetenceCoursesInformation() {
        final Set<CompetenceCourseInformation> informations = executionCourse.getCompetenceCoursesInformations();
        assertFalse(informations.isEmpty());
        assertEquals(1, informations.size());

        final CompetenceCourseInformation info = informations.iterator().next();
        assertEquals("Course A", info.getName());
        assertEquals(new BigDecimal("6.0"), info.getCredits());
        assertEquals(AcademicPeriod.SEMESTER, info.getAcademicPeriod());

        final CompetenceCourse cc = executionCourse.getCompetenceCourses().iterator().next();
        assertTrue(informations.contains(cc.findInformationMostRecentUntil(executionCourse.getExecutionInterval())));
    }

    @Test
    public void testGetCompetenceCoursesInformation_multipleCompetenceCoursesOneWithMultipleInformations() {
        final DegreeCurricularPlan dcp =
                executionCourse.getAssociatedCurricularCoursesSet().iterator().next().getDegreeCurricularPlan();
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final ExecutionInterval executionInterval = ExecutionInterval.findFirstCurrentChild(null);
        assertSame(executionInterval, emptyExecutionCourse.getExecutionInterval());

        // first competence course, with a single information
        final CompetenceCourse singleInfoCompetenceCourse =
                CompetenceCourseTest.createCompetenceCourse("Single Info Course", "SIC" + UUID.randomUUID(),
                        new BigDecimal("6.0"), AcademicPeriod.SEMESTER, executionInterval, coursesUnit);
        final CurricularCourse singleInfoCc = createCurricularCourse(dcp, singleInfoCompetenceCourse, executionInterval);
        singleInfoCc.addAssociatedExecutionCourses(emptyExecutionCourse);

        // second competence course, with multiple information
        final CompetenceCourse multiInfoCompetenceCourse =
                CompetenceCourseTest.createCompetenceCourse("Multi Info Course", "MIC" + UUID.randomUUID(), new BigDecimal("6.0"),
                        AcademicPeriod.SEMESTER, executionInterval, coursesUnit);
        final CompetenceCourseInformation currentInfo =
                multiInfoCompetenceCourse.getCompetenceCourseInformationsSet().iterator().next();

        // future information version, should not be picked as current
        final ExecutionYear nextExecutionYear = (ExecutionYear) ExecutionYear.findCurrentAggregator(null).getNext();
        new CompetenceCourseInformation(currentInfo, nextExecutionYear.getFirstExecutionPeriod());

        final CurricularCourse multiInfoCc = createCurricularCourse(dcp, multiInfoCompetenceCourse, executionInterval);
        multiInfoCc.addAssociatedExecutionCourses(emptyExecutionCourse);

        final Set<CompetenceCourseInformation> informations = emptyExecutionCourse.getCompetenceCoursesInformations();
        assertEquals(2, informations.size());
        assertTrue(informations.contains(currentInfo));
        assertSame(currentInfo,
                multiInfoCompetenceCourse.findInformationMostRecentUntil(emptyExecutionCourse.getExecutionInterval()));
    }

    @Test
    public void testGetExecutionDegrees() {
        final Set<ExecutionDegree> executionDegrees = executionCourse.getExecutionDegrees();
        assertFalse(executionDegrees.isEmpty());
        assertEquals(1, executionDegrees.size());
        assertEquals(Degree.find(DEGREE_A_CODE), executionDegrees.iterator().next().getDegree());
        assertTrue(executionDegrees.contains(executionDegree));
    }

    @Test
    public void testGetExecutionDegrees_multipleDegreeCurricularPlans() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final ExecutionYear currentExecutionYear = ExecutionYear.findCurrent(null);
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();

        // DCP with a curricular course associated to the empty execution course
        final DegreeCurricularPlan dcpA =
                createDcpWithAssociatedExecutionCourse(degree, "A", currentInterval, coursesUnit, emptyExecutionCourse);
        final ExecutionDegree executionDegreeA = dcpA.createExecutionDegree(currentExecutionYear);

        // different DCP, with another curricular course associated to the same execution course
        final DegreeCurricularPlan dcpB =
                createDcpWithAssociatedExecutionCourse(degree, "B", currentInterval, coursesUnit, emptyExecutionCourse);
        final ExecutionDegree executionDegreeB = dcpB.createExecutionDegree(currentExecutionYear);

        // DCP with its own execution degree, but with no curricular course associated to the execution course
        final DegreeCurricularPlan unrelatedDcp =
                new DegreeCurricularPlan(degree, "DCP_UNRELATED_" + UUID.randomUUID(), AcademicPeriod.THREE_YEAR,
                        currentInterval);
        final ExecutionDegree unrelatedExecutionDegree = unrelatedDcp.createExecutionDegree(currentExecutionYear);

        final Set<ExecutionDegree> executionDegrees = emptyExecutionCourse.getExecutionDegrees();
        assertEquals(2, executionDegrees.size());
        assertTrue(executionDegrees.contains(executionDegreeA));
        assertTrue(executionDegrees.contains(executionDegreeB));
        assertFalse(executionDegrees.contains(unrelatedExecutionDegree));
    }

    @Test
    public void testGetAssociatedDegreeCurricularPlans() {
        final Collection<DegreeCurricularPlan> plans = executionCourse.getAssociatedDegreeCurricularPlans();
        assertFalse(plans.isEmpty());
        assertEquals(1, plans.size());
        assertTrue(plans.contains(executionDegree.getDegreeCurricularPlan()));
    }

    @Test
    public void testGetAssociatedDegreeCurricularPlans_multipleDegreeCurricularPlans() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();

        // DCP with a curricular course associated to the empty execution course
        final DegreeCurricularPlan dcpA =
                createDcpWithAssociatedExecutionCourse(degree, "A", currentInterval, coursesUnit, emptyExecutionCourse);

        // different DCP with another curricular course associated to the same execution course
        final DegreeCurricularPlan dcpB =
                createDcpWithAssociatedExecutionCourse(degree, "B", currentInterval, coursesUnit, emptyExecutionCourse);

        // DCP with no curricular course associated to the execution course
        final DegreeCurricularPlan unrelatedDcp =
                new DegreeCurricularPlan(degree, "DCP_UNRELATED_" + UUID.randomUUID(), AcademicPeriod.THREE_YEAR,
                        currentInterval);

        final Collection<DegreeCurricularPlan> plans = emptyExecutionCourse.getAssociatedDegreeCurricularPlans();
        assertEquals(2, plans.size());
        assertTrue(plans.contains(dcpA));
        assertTrue(plans.contains(dcpB));
        assertFalse(plans.contains(unrelatedDcp));
    }

    @Test
    public void executionCourseWithoutAssociations_returnsEmptyCollections() {
        final ExecutionInterval interval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
        final ExecutionCourse emptyCourse = new ExecutionCourse("TestCourse", "TC", interval);

        assertTrue(emptyCourse.getCompetenceCourses().isEmpty());
        assertTrue(emptyCourse.getCompetenceCoursesInformations().isEmpty());
        assertTrue(emptyCourse.getExecutionDegrees().isEmpty());
        assertTrue(emptyCourse.getAssociatedDegreeCurricularPlans().isEmpty());
    }

    private static DegreeCurricularPlan createDcpWithAssociatedExecutionCourse(final Degree degree, final String suffix,
            final ExecutionInterval currentInterval, final Unit coursesUnit, final ExecutionCourse executionCourse) {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(degree, "DCP_" + suffix + "_" + UUID.randomUUID(), AcademicPeriod.THREE_YEAR,
                        currentInterval);
        final CompetenceCourse competenceCourse =
                CompetenceCourseTest.createCompetenceCourse("Course DCP " + suffix, suffix + UUID.randomUUID(),
                        new BigDecimal("6.0"), AcademicPeriod.SEMESTER, currentInterval, coursesUnit);
        final CurricularCourse cc = createCurricularCourse(dcp, competenceCourse, currentInterval);
        cc.addAssociatedExecutionCourses(executionCourse);
        return dcp;
    }

    private static CurricularCourse createCurricularCourse(final DegreeCurricularPlan dcp,
            final CompetenceCourse competenceCourse, final ExecutionInterval executionInterval) {
        final CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
        final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);
        return new CurricularCourse(6.0, competenceCourse, dcp.getRoot(), semesterPeriod, executionInterval, null);
    }
}
