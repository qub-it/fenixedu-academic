package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;
import java.util.Locale;
import java.util.SortedSet;
import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
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

    @Test
    public void testGetDegreesSortedByDegreeName() {
        // empty executioncourse
        final ExecutionCourse newEmptyExecutionCourse =
                new ExecutionCourse("Empty EC", "EMPTY", executionCourse.getExecutionInterval());
        final SortedSet<Degree> emptyDegrees = newEmptyExecutionCourse.getDegreesSortedByDegreeName();
        assertTrue(emptyDegrees.isEmpty());

        // add second degree
        final String DEGREE_B_CODE = "DB";
        associateDegreeToExecutionCourse(DEGREE_B_CODE);

        final SortedSet<Degree> degrees = executionCourse.getDegreesSortedByDegreeName();
        assertEquals(2, degrees.size());
        assertTrue(degrees.contains(Degree.find(DEGREE_A_CODE)));
        assertTrue(degrees.contains(Degree.find(DEGREE_B_CODE)));

        // check sorting
        final Iterator<Degree> it = degrees.iterator();
        assertSame(Degree.find(DEGREE_A_CODE), it.next());
        assertSame(Degree.find(DEGREE_B_CODE), it.next());
    }

    @Test
    public void testGetProfessorship_notFound() {
        // check null person
        assertNull(executionCourse.getProfessorship(null));

        final Person professorPerson = createPerson("Professor", "prof");
        final Person unrelatedPerson = createPerson("Unrelated", "unrelated");

        final Professorship professorship;
        try {
            Authenticate.mock(User.findByUsername(UserUtil.ADMIN_USERNAME), "none");
            professorship = Professorship.create(false, executionCourse, professorPerson);
        } finally {
            Authenticate.unmock();
        }

        assertSame(professorship, executionCourse.getProfessorship(professorPerson));
        assertNull(executionCourse.getProfessorship(unrelatedPerson));
    }

    @Test
    public void addAssociatedCurricularCourses_success() {
        final CurricularCourse curricularCourse = createCurricularCourse("CC1");
        emptyExecutionCourse.addAssociatedCurricularCourses(curricularCourse);
        assertTrue(emptyExecutionCourse.getAssociatedCurricularCoursesSet().contains(curricularCourse));
    }

    @Test
    public void addAssociatedCurricularCourses_throwsWhenAlreadyAssociatedInSameInterval() {
        final CurricularCourse curricularCourse = createCurricularCourse("CC2");
        final ExecutionInterval interval = ExecutionInterval.findFirstCurrentChild(null);
        final ExecutionCourse anotherEc = new ExecutionCourse("Another", "ANOTHER", interval);

        emptyExecutionCourse.addAssociatedCurricularCourses(curricularCourse);
        assertThrows(DomainException.class, () -> anotherEc.addAssociatedCurricularCourses(curricularCourse));
    }

    @Test
    public void addAssociatedCurricularCourses_sameCurricularCourseDifferentInterval() {
        final CurricularCourse curricularCourse = createCurricularCourse("CC3");
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final ExecutionInterval nextInterval = currentInterval.getNext();
        final ExecutionCourse nextEc = new ExecutionCourse("Next", "NEXT", nextInterval);

        emptyExecutionCourse.addAssociatedCurricularCourses(curricularCourse);
        nextEc.addAssociatedCurricularCourses(curricularCourse);

        assertTrue(emptyExecutionCourse.getAssociatedCurricularCoursesSet().contains(curricularCourse));
        assertTrue(nextEc.getAssociatedCurricularCoursesSet().contains(curricularCourse));
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

    private static void associateDegreeToExecutionCourse(final String degreeCode) {
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        final Degree degreeA = Degree.find(DEGREE_A_CODE);
        final Person creator = User.findByUsername(UserUtil.ADMIN_USERNAME).getPerson();
        final Degree degree = DegreeTest.createDegree(degreeA.getDegreeType(), degreeCode, "Degree " + degreeCode, executionYear);

        final DegreeCurricularPlan dcp =
                degree.createDegreeCurricularPlan("DCP_" + degreeCode, creator, AcademicPeriod.THREE_YEAR);

        final CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);
        final CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
        final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);
        final CurricularCourse curricularCourse =
                new CurricularCourse(0d, competenceCourse, dcp.getRoot(), semesterPeriod, executionYear.getFirstExecutionPeriod(),
                        null);

        executionCourse.addAssociatedCurricularCourses(curricularCourse);
    }

    private static CurricularCourse createCurricularCourse(final String degreeCode) {
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        final Degree degreeA = Degree.find(DEGREE_A_CODE);
        final Person creator = User.findByUsername(UserUtil.ADMIN_USERNAME).getPerson();
        final Degree degree = DegreeTest.createDegree(degreeA.getDegreeType(), degreeCode, "Degree " + degreeCode, executionYear);

        final DegreeCurricularPlan dcp =
                degree.createDegreeCurricularPlan("DCP_" + degreeCode, creator, AcademicPeriod.THREE_YEAR);

        final CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);
        final CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
        final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);
        return new CurricularCourse(0d, competenceCourse, dcp.getRoot(), semesterPeriod, executionYear.getFirstExecutionPeriod(),
                null);
    }
}
