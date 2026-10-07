package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_B_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

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
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CreditsDismissalTest {
    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static DegreeCurricularPlan dcp;
    private static StudentCurricularPlan scp;
    private static CurricularPeriod semesterPeriod;

    private static CurricularCourse courseA;
    private static CurricularCourse courseB;
    private static CurricularCourse equivalentCourse;
    private static CurricularCourse unknownCourse;
    private static Enrolment enrolment;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            EvaluationSeasonTest.initEvaluationSeasons();
            DegreeTest.initDegree();
            CompetenceCourseTest.initCompetenceCourse();
            StudentTest.initRegistrationConfigEntities();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            secondSemester = executionYear.getLastExecutionPeriod();

            final Degree degree = Degree.find(DEGREE_A_CODE);
            dcp = new DegreeCurricularPlan(degree, "Credits Dismissal DCP", AcademicPeriod.THREE_YEAR, executionYear);
            dcp.createExecutionDegree(executionYear);

            final CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
            semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

            final Student student = StudentTest.createStudent("Credits Dismissal Test Student", "credits.dismissal.test");
            scp = StudentTest.createRegistration(student, dcp, executionYear).getLastStudentCurricularPlan();

            courseA = newCourse(COURSE_A_CODE, "Course A Dismissal");
            courseB = newCourse(COURSE_B_CODE, "Course B Dismissal");
            equivalentCourse = newCourse(COURSE_A_CODE, "Course A Equivalent");
            unknownCourse = newCourse(null, "Course Unknown");

            enrolment = new Enrolment(scp, newGroup("EnrolmentHolder"), courseA, firstSemester, EnrollmentCondition.FINAL,
                    "credits.dismissal.test");

            return null;
        });
    }

    @Test
    public void testCreditsDismissal_checkIfCanCreate() {
        final CurriculumGroup group = newGroup("CheckIfCanCreate");

        new CreditsDismissal(newCredits(6d), group, null);
        assertEquals(1, group.getChildDismissals().size());

        final DomainException thrown = assertThrows(DomainException.class,
                () -> new CreditsDismissal(newCredits(6d), group, null));
        assertEquals("error.CreditsDismissal.already.exists.similar", thrown.getKey());

        new CreditsDismissal(newCredits(6d), newGroup("CheckIfCanCreateOther"), null);
    }

    @Test
    public void testCreditsDismissal_hasSimilarCreditsDismissal() {
        final CurriculumGroup group = newGroup("HasSimilarCreditsDismissal");
        final CurriculumGroup otherGroup = newGroup("HasSimilarCreditsDismissalOther");
        new CreditsDismissal(newCredits(6d), otherGroup, null);

        new CreditsDismissal(newCredits(9d), group, null);
        assertEquals(1, group.getChildDismissals().size());

        assertThrows(DomainException.class, () -> new CreditsDismissal(newCredits(9d), group, null));

        new CreditsDismissal(newCredits(6d), group, null);
        assertEquals(2, group.getChildDismissals().size());
    }

    @Test
    public void testCreditsDismissal_isSimilar() {
        final CurriculumGroup group = newGroup("IsSimilar");
        new CreditsDismissal(newCredits(6d), group, List.of(courseA));
        assertEquals(1, group.getChildDismissals().size());

        new CreditsDismissal(newCredits(9d), group, null);

        assertThrows(DomainException.class, () -> new CreditsDismissal(newCredits(6d), group, null));

        new CreditsDismissal(newCredits(6d), group, List.of(courseB));

        assertThrows(DomainException.class, () -> new CreditsDismissal(newCredits(6d), group, List.of(courseA)));

        final Credits creditsWithEnrolment = newCredits(6d);
        EnrolmentWrapper.create(creditsWithEnrolment, enrolment);
        new CreditsDismissal(creditsWithEnrolment, group, null);
    }

    @Test
    public void testCreditsDismissal_hasEquivalentNoEnrolCurricularCourse() {
        final CurriculumGroup group = newGroup("HasEquivalentNoEnrolCurricularCourse");
        final CreditsDismissal dismissal = new CreditsDismissal(newCredits(6d), group, List.of(courseA));

        // approved when the course is in the set
        assertTrue(dismissal.isApproved(courseA, null));
        // approved when equivalent to a course in the set (same competence course code)
        assertTrue(dismissal.isApproved(equivalentCourse, null));
        // a different course is not approved
        assertFalse(dismissal.isApproved(courseB, null));
        // no competence course -> not equivalent -> not approved
        assertFalse(dismissal.isApproved(unknownCourse, null));

        // execution interval must not be before the dismissal's interval (firstSemester)
        assertTrue(dismissal.isApproved(courseA, firstSemester));
        assertTrue(dismissal.isApproved(courseA, secondSemester));
        assertFalse(dismissal.isApproved(courseA, ((ExecutionYear) executionYear.getPrevious()).getLastExecutionPeriod()));
    }

    private static CurriculumGroup newGroup(final String name) {
        final CourseGroup courseGroup = new CourseGroup(dcp.getRoot(), name, name, firstSemester, null);
        return new CurriculumGroup(scp.getRoot(), courseGroup);
    }

    private static CurricularCourse newCourse(final String code, final String name) {
        final CurricularCourse curricularCourse = new CurricularCourse();
        curricularCourse.setName(name);
        if (code != null) {
            curricularCourse.setCompetenceCourse(CompetenceCourse.find(code));
        }
        new Context(dcp.getRoot(), curricularCourse, semesterPeriod, firstSemester, null);
        return curricularCourse;
    }

    private static Credits newCredits(final double ects) {
        final Credits credits = new Credits();
        credits.setStudentCurricularPlan(scp);
        credits.setGivenCredits(ects);
        credits.setExecutionPeriod(firstSemester);
        return credits;
    }
}
