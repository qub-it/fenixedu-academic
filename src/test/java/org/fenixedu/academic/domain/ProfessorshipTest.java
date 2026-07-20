package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
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

    private static ExecutionCourse executionCourse;
    private static Person person;
    private static Teacher teacher;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static Degree degree;
    private static Shift shift;

    @BeforeClass
    public static void initData() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            DegreeCurricularPlanTest.initDegreeCurricularPlan();

            degree = Degree.find(DEGREE_A_CODE);
            degreeCurricularPlan = degree.getDegreeCurricularPlansSet().iterator().next();
            final CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);

            ExecutionYear executionYear = ExecutionYear.findCurrent(null);
            ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

            executionCourse = new ExecutionCourse(competenceCourse.getName(), competenceCourse.getCode(), executionInterval);
            executionCourse.addAssociatedCurricularCourses(degreeCurricularPlan.getCurricularCourseByCode(COURSE_A_CODE));

            shift = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);

            UserProfile userProfile = new UserProfile("Test", "User", "Test User", "test.user@fenixedu.com",
                    java.util.Locale.getDefault());
            User user = new User("testprof", userProfile);
            person = new Person(userProfile);

            user.setPerson(person);
            person.setUser(user);

            teacher = new Teacher(person);

            Authenticate.mock(user, "testprof");

            return null;
        });
    }

    private static Professorship createProfessorship(ExecutionCourse ec, Person p, Boolean responsibleFor) {
        Professorship professorship = new Professorship();
        professorship.setExecutionCourse(ec);
        professorship.setPerson(p);
        professorship.setCreator(person);
        professorship.setResponsibleFor(responsibleFor);
        return professorship;
    }

    @Test
    public void testGetDegreeSiglas() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            String siglas = professorship.getDegreeSiglas();
            assertNotNull(siglas);
            assertFalse(siglas.isEmpty());
            assertTrue(siglas.contains(degree.getSigla()));
            return null;
        });
    }

    @Test
    public void testGetDegreePlanNames() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            String planNames = professorship.getDegreePlanNames();
            assertNotNull(planNames);
            assertFalse(planNames.isEmpty());
            assertTrue(planNames.contains(degreeCurricularPlan.getName()));
            return null;
        });
    }

    @Test
    public void testReadByDegreeCurricularPlanAndExecutionYear() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            ExecutionYear executionYear = ExecutionYear.findCurrent(null);
            List<Professorship> result = Professorship.readByDegreeCurricularPlanAndExecutionYear(degreeCurricularPlan,
                    executionYear);
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains(professorship));
            return null;
        });
    }

    @Test
    public void testReadByDegreeCurricularPlanAndExecutionYear_empty() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionYear nextYear = (ExecutionYear) ExecutionYear.findCurrent(null).getNext();
            List<Professorship> result = Professorship.readByDegreeCurricularPlanAndExecutionYear(degreeCurricularPlan, nextYear);
            assertNotNull(result);
            assertTrue(result.isEmpty());
            return null;
        });
    }

    @Test
    public void testReadByDegreeCurricularPlanAndExecutionPeriod() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            ExecutionInterval executionInterval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
            List<Professorship> result = Professorship.readByDegreeCurricularPlanAndExecutionPeriod(degreeCurricularPlan,
                    executionInterval);
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains(professorship));
            return null;
        });
    }

    @Test
    public void testReadByDegreeCurricularPlansAndExecutionYear() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            ExecutionYear executionYear = ExecutionYear.findCurrent(null);
            List<DegreeCurricularPlan> dcps = new ArrayList<>();
            dcps.add(degreeCurricularPlan);
            List<Professorship> result = Professorship.readByDegreeCurricularPlansAndExecutionYear(dcps, executionYear);
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains(professorship));
            return null;
        });
    }

    @Test
    public void testReadByDegreeCurricularPlansAndExecutionYear_nullYear() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            List<DegreeCurricularPlan> dcps = new ArrayList<>();
            dcps.add(degreeCurricularPlan);
            List<Professorship> result = Professorship.readByDegreeCurricularPlansAndExecutionYear(dcps, null);
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains(professorship));
            return null;
        });
    }

    @Test
    public void testGetShifts() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            assertNotNull(shift);
            ShiftProfessorship shiftProfessorship = new ShiftProfessorship();
            shiftProfessorship.setShift(shift);
            shiftProfessorship.setProfessorship(professorship);
            List<Shift> shifts = professorship.getShifts().collect(java.util.stream.Collectors.toList());
            assertNotNull(shifts);
            assertFalse(shifts.isEmpty());
            assertTrue(shifts.contains(shift));
            return null;
        });
    }

    @Test
    public void testIsResponsibleFor() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorshipTrue = createProfessorship(executionCourse, person, true);
            assertTrue(professorshipTrue.isResponsibleFor());

            Person person2;
            UserProfile userProfile2 = new UserProfile("Test2", "User2", "Test2 User2", "test2.user@fenixedu.com",
                    java.util.Locale.getDefault());
            User user2 = new User("testprof2", userProfile2);
            person2 = new Person(userProfile2);
            user2.setPerson(person2);
            person2.setUser(user2);

            Professorship professorshipFalse = createProfessorship(executionCourse, person2, false);
            assertFalse(professorshipFalse.isResponsibleFor());
            return null;
        });
    }

    @Test
    public void testSetResponsibleFor_nullDefaultsToFalse() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            professorship.setResponsibleFor((Boolean) null);
            assertFalse(professorship.isResponsibleFor());
            return null;
        });
    }

    @Test
    public void testHasTeacher() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorshipWithTeacher = createProfessorship(executionCourse, person, true);
            assertTrue(professorshipWithTeacher.hasTeacher());

            Person personNoTeacher;
            UserProfile userProfileNoTeacher = new UserProfile("Test3", "User3", "Test3 User3", "test3.user@fenixedu.com",
                    java.util.Locale.getDefault());
            User userNoTeacher = new User("testprof3", userProfileNoTeacher);
            personNoTeacher = new Person(userProfileNoTeacher);
            userNoTeacher.setPerson(personNoTeacher);
            personNoTeacher.setUser(userNoTeacher);

            Professorship professorshipNoTeacher = createProfessorship(executionCourse, personNoTeacher, true);
            assertFalse(professorshipNoTeacher.hasTeacher());
            return null;
        });
    }

    @Test
    public void testIsDeletable() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Professorship professorship = createProfessorship(executionCourse, person, true);
            assertTrue(professorship.isDeletable());
            return null;
        });
    }
}
