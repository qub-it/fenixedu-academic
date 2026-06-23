package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.SortedSet;
import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionCourseTest {

    private static ExecutionCourse executionCourse;
    private static CurricularCourse curricularCourse;
    private static ExecutionInterval executionInterval;

    private static Person personWithProfessorship;
    private static Professorship professorship;

    private static Degree secondDegree;
    private static CurricularCourse secondCurricularCourse;
    private static ExecutionCourse secondExecutionCourseSameInterval;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            DegreeTest.initDegree();
            UserUtil.initAdminUser();

            executionInterval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();

            curricularCourse = createCurricularCourseForDegree(Degree.find(DEGREE_A_CODE), "DCP", executionInterval);
            executionCourse = new ExecutionCourse("EC", "EC", executionInterval);
            executionCourse.addAssociatedCurricularCourses(curricularCourse);

            personWithProfessorship = createPerson("Professorship Person", "prof.person");
            try {
                Authenticate.mock(User.findByUsername("admin"), "none");
                professorship = Professorship.create(true, executionCourse, personWithProfessorship);
            } finally {
                Authenticate.unmock();
            }

            secondDegree = DegreeTest.createDegree(DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow(), "DB", "Degree B", ExecutionYear.findCurrent(null));
            secondCurricularCourse = createCurricularCourseForDegree(secondDegree, "2nd DCP", executionInterval);
            secondExecutionCourseSameInterval = new ExecutionCourse("2nd EC Same Interval", "2ND-EC", executionInterval);

            return null;
        });
    }

    private static CurricularCourse createCurricularCourseForDegree(final Degree degree, final String dcpName,
            final ExecutionInterval interval) {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR, interval);
        final CurricularCourse cc = new CurricularCourse();
        final CurricularPeriod yearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
        final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);
        new Context(dcp.getRoot(), cc, semesterPeriod, interval, null);
        return cc;
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

    @Test
    public void testGetDegreesSortedByDegreeName_empty() {
        final ExecutionCourse freshEc =
                new ExecutionCourse("Empty EC", "EMPTY", executionInterval);
        final SortedSet<Degree> degrees = freshEc.getDegreesSortedByDegreeName();
        assertTrue(degrees.isEmpty());
    }

    @Test
    public void testGetDegreesSortedByDegreeName_single() {
        final SortedSet<Degree> degrees = executionCourse.getDegreesSortedByDegreeName();
        assertEquals(1, degrees.size());
        assertTrue(degrees.contains(Degree.find(DEGREE_A_CODE)));
    }

    @Test
    public void testGetDegreesSortedByDegreeName_multiple() {
        executionCourse.addAssociatedCurricularCourses(secondCurricularCourse);
        try {
            final SortedSet<Degree> degrees = executionCourse.getDegreesSortedByDegreeName();
            assertEquals(2, degrees.size());
            final Degree first = degrees.first();
            assertEquals("Degree A", first.getName());
        } finally {
            executionCourse.removeAssociatedCurricularCourses(secondCurricularCourse);
        }
    }

    @Test
    public void testGetProfessorship_notFound() {
        final Person unrelatedPerson = createPerson("Unrelated", "unrelated");
        assertNull(executionCourse.getProfessorship(unrelatedPerson));
    }

    @Test
    public void testGetProfessorship_found() {
        final Professorship result = executionCourse.getProfessorship(personWithProfessorship);
        assertNotNull(result);
        assertEquals(professorship, result);
    }

    @Test
    public void testGetProfessorship_nullPerson() {
        assertNull(executionCourse.getProfessorship(null));
    }

    @Test
    public void testAddAssociatedCurricularCourses_success() {
        final ExecutionCourse ec =
                new ExecutionCourse("Success EC", UUID.randomUUID().toString(), executionInterval);
        final CurricularCourse cc = createCurricularCourseForDegree(secondDegree,
                "DCP for testAddAssociatedCurricularCourses_success", executionInterval);
        ec.addAssociatedCurricularCourses(cc);
        assertTrue(ec.getAssociatedCurricularCoursesSet().contains(cc));
    }

    @Test
    public void testAddAssociatedCurricularCourses_duplicateInSameInterval() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.executionCourse.curricularCourse.already.associated");

        secondExecutionCourseSameInterval.addAssociatedCurricularCourses(curricularCourse);
    }

    @Test
    public void testAddAssociatedCurricularCourses_differentInterval() {
        final ExecutionInterval nextInterval = executionInterval.getNext();
        final ExecutionCourse ecDifferentInterval =
                new ExecutionCourse("Different Interval EC", UUID.randomUUID().toString(), nextInterval);
        ecDifferentInterval.addAssociatedCurricularCourses(curricularCourse);
        assertTrue(ecDifferentInterval.getAssociatedCurricularCoursesSet().contains(curricularCourse));
    }
}
