package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CreditsTest {

    private static ExecutionYear executionYear;
    private static ExecutionYear previousYear;
    private static ExecutionInterval firstSemester;

    private static Unit coursesUnit;

    private static StudentCurricularPlan studentCurricularPlan;
    private static CourseGroup mandatoryGroup;
    private static CourseGroup enrolledGroup;

    private static Enrolment firstEnrolment, secondEnrolment, previousYearEnrolment;
    private static CurricularCourse firstDismissedCourse, secondDismissedCourse;

    @BeforeClass
    public static void init() {
        // Root
        //   └─ Cycle
        //      ├─ Mandatory  C1 6 (1Y1S), C2 6 (1Y2S), C3 6 (1Y1S)                       max 18
        //      └─ Enrolled   E1 6 (1Y1S), E2 6 (1Y2S), PREV 6 (1Y1S), D1 0.1, D2 0.2   max 18.3

        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            DegreeTest.initDegree();
            CompetenceCourseTest.initCompetenceCourse();
            EvaluationSeasonTest.initEvaluationSeasons();
            StudentTest.initRegistrationConfigEntities();

            executionYear = ExecutionYear.findCurrent(null);
            previousYear = (ExecutionYear) executionYear.getPrevious();
            firstSemester = executionYear.getFirstExecutionPeriod();
            final ExecutionInterval secondSemester = executionYear.getLastExecutionPeriod();
            coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();

            final DegreeCurricularPlan degreeCurricularPlan = createDcp(executionYear, DegreeType.findByCode(
                    DegreeTest.DEGREE_TYPE_CODE).orElseThrow());
            final CurricularPeriod firstPeriod = new CurricularPeriod(SEMESTER, 1, new CurricularPeriod(AcademicPeriod.YEAR, 1,
                    degreeCurricularPlan.getDegreeStructure()));
            final CurricularPeriod secondPeriod = new CurricularPeriod(SEMESTER, 2, firstPeriod.getParent());

            final CourseGroup cycleGroup = new CourseGroup(degreeCurricularPlan.getRoot(), "Cycle", "Cycle", firstSemester, null);
            mandatoryGroup = new CourseGroup(cycleGroup, "Mandatory", "Mandatory", firstSemester, null);
            enrolledGroup = new CourseGroup(cycleGroup, "Enroled", "Enroled", firstSemester, null);

            createCourse("C1", "6", firstPeriod, firstSemester, mandatoryGroup);
            createCourse("C2", "6", secondPeriod, firstSemester, mandatoryGroup);
            createCourse("C3", "6", firstPeriod, firstSemester, mandatoryGroup);

            studentCurricularPlan = createRegistration(degreeCurricularPlan, "credits.test.student", executionYear);

            firstEnrolment = enrol("E1", "6", firstPeriod, firstSemester);
            secondEnrolment = enrol("E2", "6", secondPeriod, secondSemester);
            previousYearEnrolment = enrol("PREV", "6", firstPeriod, previousYear.getFirstExecutionPeriod());

            firstDismissedCourse = createCourse("D1", "0.1", firstPeriod, firstSemester, enrolledGroup);
            secondDismissedCourse = createCourse("D2", "0.2", firstPeriod, firstSemester, enrolledGroup);

            return null;
        });
    }

    @Test
    public void testCredits_allowsEctsCredits() {
        assertEquals(18, mandatoryGroup.getMaxEctsCredits(firstSemester), 0);

        // 18 + 0 (nothing is concluded in the plan yet) <= 18 (group's max)
        final Credits credits = new Credits(studentCurricularPlan, mandatoryGroup, Set.of(), Set.of(), 18.0, firstSemester);
        assertEquals(18.0, credits.getGivenCredits(), 0);

        // 18.5 + 0 > 18, it is rejected
        assertThrows(DomainException.class,
                () -> new Credits(studentCurricularPlan, mandatoryGroup, Set.of(), Set.of(), 18.5, firstSemester));
    }

    @Test
    public void testCredits_getEnrolmentsSetBefore() {
        final Credits credits = newCredits(studentCurricularPlan, firstEnrolment, previousYearEnrolment);

        // a null year filters nothing (an enrolment without an execution year is always kept too)
        assertEquals(Set.of(firstEnrolment, previousYearEnrolment), enrolmentOf(credits.getEnrolmentsSetBefore(null)));

        // only the previous year enrolment is before the current execution year
        assertEquals(Set.of(previousYearEnrolment), enrolmentOf(credits.getEnrolmentsSetBefore(executionYear)));

        // nothing is strictly before the previous year
        assertTrue(credits.getEnrolmentsSetBefore(previousYear).isEmpty());
    }

    @Test
    public void testCredits_getIEnrolments() {
        final Credits credits = newCredits(studentCurricularPlan, firstEnrolment, firstEnrolment, secondEnrolment);
        final Collection<IEnrolment> result = credits.getIEnrolments();
        assertTrue(result instanceof Set);
        assertEquals(2, result.size());
        assertTrue(result.contains(firstEnrolment));
        assertTrue(result.contains(secondEnrolment));
    }

    @Test
    public void testCredits_hasIEnrolments() {
        final Credits credits = newCredits(studentCurricularPlan, firstEnrolment, secondEnrolment);
        assertTrue(credits.hasIEnrolments(firstEnrolment));
        assertTrue(credits.hasIEnrolments(secondEnrolment));

        // an enrolment of this student that was never added to these credits
        assertFalse(credits.hasIEnrolments(previousYearEnrolment));
    }

    @Test
    public void testCredits_getGivenCredits() {
        final Credits explicit = newCredits(studentCurricularPlan);
        explicit.setGivenCredits(123.0);
        assertEquals(123.0, explicit.getGivenCredits(), 0);

        // without an explicit given-credits value, the credits are the sum of the dismissals
        final CurriculumGroup enrolledCurriculumGroup = studentCurricularPlan.findCurriculumGroupFor(enrolledGroup);
        final Credits byDismissals = newCredits(studentCurricularPlan);
        new Dismissal(byDismissals, enrolledCurriculumGroup, firstDismissedCourse);
        new Dismissal(byDismissals, enrolledCurriculumGroup, secondDismissedCourse);
        assertEquals(0.1+0.2, byDismissals.getGivenCredits(), 0);
    }

    @Test
    public void testCredits_getEnrolmentsEcts() {
        // summed over the enrolments (6 + 6), dismissals are not counted here
        final Credits credits = newCredits(studentCurricularPlan, firstEnrolment, secondEnrolment);
        assertEquals(12.0, credits.getEnrolmentsEcts(), 0);

        final Credits empty = newCredits(studentCurricularPlan);
        assertEquals(0.0, empty.getEnrolmentsEcts(), 0);
    }

    private static DegreeCurricularPlan createDcp(final ExecutionYear executionYear, final DegreeType degreeType) {
        final Degree degree =
                DegreeTest.createDegree(degreeType, "D" + UUID.randomUUID(), "D" + UUID.randomUUID(), executionYear);
        final Person person =
                new Person(new UserProfile("DCP", "Creator", "DCP Creator", "dcp.person@qubit.com", Locale.getDefault()));
        final DegreeCurricularPlan dcp = degree.createDegreeCurricularPlan("Plan 1", person, AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        return dcp;
    }

    private static StudentCurricularPlan createRegistration(final DegreeCurricularPlan dcp, final String username,
            final ExecutionYear executionYear) {
        final Student student = StudentTest.createStudent("Credits Test Student", username);
        return StudentTest.createRegistration(student, dcp, executionYear).getLastStudentCurricularPlan();
    }

    private static CurricularCourse createCourse(final String code, final String ects, final CurricularPeriod period,
            final ExecutionInterval interval, final CourseGroup courseGroup) {
        final CompetenceCourse competenceCourse = CompetenceCourseTest.createCompetenceCourse("Course " + code, code,
                new BigDecimal(ects), SEMESTER, interval, coursesUnit);
        return new CurricularCourse(new BigDecimal(ects).doubleValue(), competenceCourse, courseGroup, period, interval, null);
    }

    private static Enrolment enrol(final String code, final String ects, final CurricularPeriod period,
            final ExecutionInterval interval) {
        final CurricularCourse course = createCourse(code, ects, period, interval, enrolledGroup);
        return new Enrolment(studentCurricularPlan, studentCurricularPlan.findCurriculumGroupFor(enrolledGroup), course, interval,
                EnrollmentCondition.FINAL, "credits.test.student");
    }

    private static Credits newCredits(final StudentCurricularPlan scp, final IEnrolment... enrolments) {
        final Credits credits = new Credits();
        credits.setStudentCurricularPlan(scp);
        credits.setExecutionPeriod(firstSemester);
        for (final IEnrolment enrolment : enrolments) {
            EnrolmentWrapper.create(credits, enrolment);
        }
        return credits;
    }

    private static Set<IEnrolment> enrolmentOf(final Set<EnrolmentWrapper> wrappers) {
        return wrappers.stream().map(EnrolmentWrapper::getIEnrolment).collect(Collectors.toSet());
    }

}
