package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

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
    private static Professorship respProfessorship, nonRespProfessorship;

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

            respProfessorship = createProfessorship("testprofA", true);
            nonRespProfessorship = createProfessorship("testprofB", false);

            return null;
        });
    }

    private static Professorship createProfessorship(String username, boolean responsibleFor) {
        final UserProfile userProfile =
                new UserProfile("User" + username, "", "User" + username, username + "@test.com", Locale.getDefault());
        User user = new User(username, userProfile);
        Person person = new Person(userProfile);
        new Teacher(person);
        Authenticate.mock(user, username);

        return Professorship.create(responsibleFor, executionCourse, person);
    }

    @Test
    public void testComparatorByPersonName() {
        assertTrue(Professorship.COMPARATOR_BY_PERSON_NAME.compare(respProfessorship, nonRespProfessorship) < 0);
        assertTrue(Professorship.COMPARATOR_BY_PERSON_NAME.compare(nonRespProfessorship, respProfessorship) > 0);
        assertEquals(0, Professorship.COMPARATOR_BY_PERSON_NAME.compare(respProfessorship, respProfessorship));
    }

    @Test
    public void testIsResponsibleFor() {
        assertTrue(respProfessorship.isResponsibleFor());
        assertFalse(nonRespProfessorship.isResponsibleFor());
    }

    @Test
    public void testGetDegreeSiglas() {
        String siglas = respProfessorship.getDegreeSiglas();
        assertTrue(StringUtils.isNotBlank(siglas));
        String[] parts = siglas.split(", ");
        assertEquals(2, parts.length);
        assertEquals(degreeA.getSigla(), parts[0]);
        assertEquals(degreeB.getSigla(), parts[1]);
    }
}