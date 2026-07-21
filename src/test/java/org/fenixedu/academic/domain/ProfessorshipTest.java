package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V2;
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
    private static Person person;
    private static DegreeCurricularPlan dcpA, dcpB, dcp2;
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
            dcp2 = degreeA.getDegreeCurricularPlansSet().stream().filter(d -> DCP_NAME_V2.equals(d.getName())).findAny()
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

    //    @Test
    //    public void testGetDegreePlanNames() {
    //        String planNames = professorship.getDegreePlanNames();
    //        assertTrue(StringUtils.isNotBlank(planNames));
    //        String[] parts = planNames.split(", ");
    //        assertEquals(2, parts.length);
    //        assertTrue(planNames.contains(dcpA.getName()));
    //        assertTrue(planNames.contains(dcpB.getName()));
    //    }
    //
    //    @Test
    //    public void testReadByDegreeCurricularPlanAndExecutionYear() {
    //        List<Professorship> result = Professorship.readByDegreeCurricularPlanAndExecutionYear(dcpA, executionYear);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //
    //        result = Professorship.readByDegreeCurricularPlanAndExecutionYear(dcpB, executionYear);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //
    //        // DCP without curricular courses in the execution year
    //        result = Professorship.readByDegreeCurricularPlanAndExecutionYear(dcp2, executionYear);
    //        assertTrue(result.isEmpty());
    //
    //        // execution year without professorships
    //        result = Professorship.readByDegreeCurricularPlanAndExecutionYear(dcpA, (ExecutionYear) executionYear.getPrevious());
    //        assertTrue(result.isEmpty());
    //    }
    //
    //    @Test
    //    public void testReadByDegreeCurricularPlanAndExecutionPeriod() {
    //        List<Professorship> result = Professorship.readByDegreeCurricularPlanAndExecutionPeriod(dcpA, executionInterval);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //
    //        // different DCP on different degree, same execution course
    //        result = Professorship.readByDegreeCurricularPlanAndExecutionPeriod(dcpB, executionInterval);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //
    //        // DCP without curricular courses
    //        result = Professorship.readByDegreeCurricularPlanAndExecutionPeriod(dcp2, executionInterval);
    //        assertTrue(result.isEmpty());
    //
    //        // execution interval without professorships
    //        ExecutionInterval previousInterval = executionYear.getPreviousExecutionYear().getFirstExecutionPeriod();
    //        result = Professorship.readByDegreeCurricularPlanAndExecutionPeriod(dcpA, previousInterval);
    //        assertTrue(result.isEmpty());
    //    }
    //
    //    @Test
    //    public void testReadByDegreeCurricularPlansAndExecutionYear() {
    //        List<DegreeCurricularPlan> dcps = new ArrayList<>();
    //        dcps.add(dcpA);
    //
    //        // single DCP, current year
    //        List<Professorship> result = Professorship.readByDegreeCurricularPlansAndExecutionYear(dcps, executionYear);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //
    //        // multiple DCPs, current year
    //        dcps.add(dcpB);
    //        result = Professorship.readByDegreeCurricularPlansAndExecutionYear(dcps, executionYear);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //        assertEquals(1, result.size());
    //
    //        // DCP without curricular courses
    //        result = Professorship.readByDegreeCurricularPlansAndExecutionYear(List.of(dcp2), executionYear);
    //        assertTrue(result.isEmpty());
    //
    //        // different execution year
    //        result = Professorship.readByDegreeCurricularPlansAndExecutionYear(dcps, (ExecutionYear) executionYear.getPrevious());
    //        assertTrue(result.isEmpty());
    //
    //        // empty DCP list
    //        result = Professorship.readByDegreeCurricularPlansAndExecutionYear(List.of(), executionYear);
    //        assertTrue(result.isEmpty());
    //
    //        // null execution year, returns all execution courses unfiltered
    //        result = Professorship.readByDegreeCurricularPlansAndExecutionYear(dcps, null);
    //        assertFalse(result.isEmpty());
    //        assertTrue(result.contains(professorship));
    //    }
}
