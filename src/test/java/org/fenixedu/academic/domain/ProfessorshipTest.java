package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ProfessorshipTest {

    public static final String DEGREE_B_CODE = "DB";
    public static final String DCP_B_NAME = "DCP_B_NAME";

    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;
    private static ExecutionCourse executionCourse;
    private static DegreeCurricularPlan dcpA, dcpB;
    private static Degree degreeA, degreeB;
    private static Professorship professorship;


    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            DegreeCurricularPlanTest.initDegreeCurricularPlan();

            executionYear = ExecutionYear.findCurrent(null);
            executionInterval = executionYear.getFirstExecutionPeriod();

            degreeA = Degree.find(DEGREE_A_CODE);
            dcpA = degreeA.getDegreeCurricularPlansSet().stream().filter(d -> DCP_NAME_V1.equals(d.getName())).findAny()
                    .orElseThrow();
            final CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);

            degreeB = DegreeTest.createDegree(degreeA.getDegreeType(), DEGREE_B_CODE, "Degree B", executionYear);
            dcpB = new DegreeCurricularPlan(degreeB, DCP_B_NAME, AcademicPeriod.THREE_YEAR);
            dcpB.setCurricularStage(CurricularStage.APPROVED);
            final CurricularCourse ccB = new CurricularCourse(0d, competenceCourse, dcpB.getRoot(),
                    new CurricularPeriod(AcademicPeriod.YEAR, 1, dcpB.getDegreeStructure()), executionInterval, null);

            executionCourse = new ExecutionCourse(competenceCourse.getName(), competenceCourse.getCode(), executionInterval);
            executionCourse.addAssociatedCurricularCourses(dcpA.getCurricularCourseByCode(COURSE_A_CODE));
            executionCourse.addAssociatedCurricularCourses(ccB);

            professorship = createProfessorship("testprof", "Test", "User", "Test User", "test.user@fenixedu.com");

            return null;
        });
    }

    private static Professorship createProfessorship(String username, String givenNames, String familyNames, String displayName,
            String email) {
        UserProfile userProfile = new UserProfile(givenNames, familyNames, displayName, email, java.util.Locale.getDefault());
        User user = new User(username, userProfile);
        Person person = new Person(userProfile);
        user.setPerson(person);
        person.setUser(user);
        new Teacher(person);
        Authenticate.mock(user, username);

        return Professorship.create(true, executionCourse, person);
    }

    @Test
    public void testComparatorByPersonName() {
        Professorship professorship2 = createProfessorship("testprof2", "Alpha", "User", "Alpha User", "alpha.user@fenixedu.com");
        try {
            assertTrue(Professorship.COMPARATOR_BY_PERSON_NAME.compare(professorship, professorship2) > 0);
            assertTrue(Professorship.COMPARATOR_BY_PERSON_NAME.compare(professorship2, professorship) < 0);
            assertEquals(0, Professorship.COMPARATOR_BY_PERSON_NAME.compare(professorship, professorship));
        } finally {
            professorship2.delete();
        }
    }

    @Test
    public void testGetDegreeSiglas() {
        String siglas = professorship.getDegreeSiglas();
        assertTrue(StringUtils.isNotBlank(siglas));
        assertEquals(2, siglas.split(", ").length);
        assertTrue(siglas.contains(degreeA.getSigla()));
        assertTrue(siglas.contains(degreeB.getSigla()));
        // check that the siglas are sorted
        assertEquals(Stream.of(degreeA.getSigla(), degreeB.getSigla()).sorted().collect(Collectors.joining(", ")), siglas);
    }
}
