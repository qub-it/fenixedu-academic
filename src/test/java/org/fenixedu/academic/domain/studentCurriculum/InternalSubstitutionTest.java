package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeCurricularPlanTest;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class InternalSubstitutionTest {

    private static final String CREATED_BY = "internal.substitution.student";

    private static ExecutionYear executionYear;
    private static ExecutionYear previousYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static ExecutionInterval previousYearFirstSemester;

    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CourseGroup enrolledGroup;

    private static CurricularCourse currentYearCourse;
    private static CurricularCourse secondSemesterCourse;
    private static CurricularCourse previousYearCourse;
    private static CurricularCourse foreignCourse;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initStudentAndRegistration();
            EvaluationSeasonTest.initEvaluationSeasons();

            executionYear = ExecutionYear.findCurrent(null);
            previousYear = (ExecutionYear) executionYear.getPrevious();
            firstSemester = executionYear.getFirstExecutionPeriod();
            secondSemester = executionYear.getLastExecutionPeriod();
            previousYearFirstSemester = previousYear.getFirstExecutionPeriod();
            final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();

            degreeCurricularPlan = Degree.find(DegreeTest.DEGREE_A_CODE).getDegreeCurricularPlansSet().stream()
                    .filter(dcp -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(dcp.getName())).findAny().orElseThrow();

            final CurricularPeriod firstPeriod = new CurricularPeriod(SEMESTER, 1, new CurricularPeriod(AcademicPeriod.YEAR, 1,
                    degreeCurricularPlan.getDegreeStructure()));
            final CurricularPeriod secondPeriod = new CurricularPeriod(SEMESTER, 2, firstPeriod.getParent());

            final CourseGroup cycleGroup =
                    new CourseGroup(degreeCurricularPlan.getRoot(), "Cycle", "Cycle", firstSemester, null);
            new CourseGroup(cycleGroup, "Mandatory", "Mandatory", firstSemester, null);
            enrolledGroup = new CourseGroup(cycleGroup, "Enroled", "Enroled", firstSemester, null);

            currentYearCourse = createCourse("E1", "6", firstPeriod, firstSemester, enrolledGroup, coursesUnit);
            secondSemesterCourse = createCourse("E2", "6", secondPeriod, secondSemester, enrolledGroup, coursesUnit);
            previousYearCourse = createCourse("PREV", "6", firstPeriod, previousYearFirstSemester, enrolledGroup, coursesUnit);
            foreignCourse = createCourse("FOREIGN", "6", firstPeriod, firstSemester, enrolledGroup, coursesUnit);

            return null;
        });
    }

    @Test
    public void testInternalSubstitution_checkEnrolments() {
        // enrolments of the given student curricular plan are accepted
        final StudentCurricularPlan studentCurricularPlan = newStudentCurricularPlan();
        final Enrolment enrolment = enrol(studentCurricularPlan, currentYearCourse, firstSemester);

        final InternalSubstitution valid = new InternalSubstitution(studentCurricularPlan,
                studentCurricularPlan.findCurriculumGroupFor(enrolledGroup), enrolments(enrolment), 1.0, firstSemester);
        assertTrue(valid.isInternalSubstitution());
        assertTrue(valid.hasIEnrolments(enrolment));

        // a mix with an enrolment of another student curricular plan is rejected without side effects
        final StudentCurricularPlan cleanPlan = newStudentCurricularPlan();
        final Enrolment ownEnrolment = enrol(cleanPlan, currentYearCourse, firstSemester);
        final Enrolment foreignEnrolment = enrol(newStudentCurricularPlan(), foreignCourse, firstSemester);
        final CurriculumGroup ownOriginalGroup = ownEnrolment.getCurriculumGroup();
        final CurriculumGroup foreignOriginalGroup = foreignEnrolment.getCurriculumGroup();

        final DomainException mixedException = assertThrows(DomainException.class,
                () -> new InternalSubstitution(cleanPlan, cleanPlan.findCurriculumGroupFor(enrolledGroup),
                        enrolments(ownEnrolment, foreignEnrolment), 2.0, firstSemester));
        assertEquals("error.InternalSubstitution.invalid.enrolment", mixedException.getKey());

        assertNull(cleanPlan.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.INTERNAL_CREDITS_SOURCE_GROUP));
        assertEquals(ownOriginalGroup, ownEnrolment.getCurriculumGroup());
        assertEquals(foreignOriginalGroup, foreignEnrolment.getCurriculumGroup());

        // only enrolments of another student curricular plan are also rejected
        final DomainException foreignOnlyException = assertThrows(DomainException.class,
                () -> new InternalSubstitution(cleanPlan, cleanPlan.findCurriculumGroupFor(enrolledGroup),
                        enrolments(foreignEnrolment), 2.0, firstSemester));
        assertEquals("error.InternalSubstitution.invalid.enrolment", foreignOnlyException.getKey());
    }

    @Test
    public void testInternalSubstitution_moveExistingEnrolmentsToExtraCurriculumGroup() {
        final StudentCurricularPlan studentCurricularPlan = newStudentCurricularPlan();
        final Enrolment firstEnrolment = enrol(studentCurricularPlan, currentYearCourse, firstSemester);
        final Enrolment secondEnrolment = enrol(studentCurricularPlan, secondSemesterCourse, secondSemester);

        final InternalSubstitution substitution = new InternalSubstitution(studentCurricularPlan,
                studentCurricularPlan.findCurriculumGroupFor(enrolledGroup), enrolments(firstEnrolment, secondEnrolment), 3.0,
                firstSemester);

        final NoCourseGroupCurriculumGroup sourceGroup = studentCurricularPlan
                .getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.INTERNAL_CREDITS_SOURCE_GROUP);
        assertNotNull(sourceGroup);
        assertEquals(sourceGroup, firstEnrolment.getCurriculumGroup());
        assertEquals(sourceGroup, secondEnrolment.getCurriculumGroup());

        // deleting the substitution disconnects it, moving the source enrolments to the extra curriculum group
        substitution.delete();

        final ExtraCurriculumGroup extraCurriculumGroup = studentCurricularPlan.getExtraCurriculumGroup();
        assertNotNull(extraCurriculumGroup);
        assertEquals(extraCurriculumGroup, firstEnrolment.getCurriculumGroup());
        assertEquals(extraCurriculumGroup, secondEnrolment.getCurriculumGroup());
    }

    @Test
    public void testInternalSubstitution_deleteSourceGroupIfEmpty() {
        final StudentCurricularPlan studentCurricularPlan = newStudentCurricularPlan();
        final Enrolment previousYearEnrolment = enrol(studentCurricularPlan, previousYearCourse, previousYearFirstSemester);

        final InternalSubstitution substitution = new InternalSubstitution(studentCurricularPlan,
                studentCurricularPlan.findCurriculumGroupFor(enrolledGroup), enrolments(previousYearEnrolment), 4.0,
                firstSemester);

        final NoCourseGroupCurriculumGroup sourceGroup = studentCurricularPlan
                .getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.INTERNAL_CREDITS_SOURCE_GROUP);
        assertNotNull(sourceGroup);

        // once the enrolments are moved out of the source group it becomes deletable and is deleted
        substitution.delete();

        assertNull(studentCurricularPlan.getNoCourseGroupCurriculumGroup(
                NoCourseGroupCurriculumGroupType.INTERNAL_CREDITS_SOURCE_GROUP));
    }

    @Test
    public void testInternalSubstitution_getCurriculum() {
        final StudentCurricularPlan studentCurricularPlan = newStudentCurricularPlan();
        final Enrolment firstEnrolment = enrol(studentCurricularPlan, currentYearCourse, firstSemester);
        final Enrolment previousYearEnrolment = enrol(studentCurricularPlan, previousYearCourse, previousYearFirstSemester);

        final InternalSubstitution substitution = new InternalSubstitution(studentCurricularPlan,
                studentCurricularPlan.findCurriculumGroupFor(enrolledGroup), enrolments(firstEnrolment, previousYearEnrolment),
                5.0, firstSemester);
        final Dismissal dismissal = substitution.getDismissalsSet().iterator().next();

        // only the previous year enrolment is before the current execution year
        final Curriculum beforeYear = dismissal.getCurriculum(new DateTime(), executionYear);
        assertFalse(beforeYear.isEmpty());
        assertEquals(1, beforeYear.getEnrolmentRelatedEntries().size());
        assertTrue(beforeYear.getEnrolmentRelatedEntries().contains(previousYearEnrolment));
        assertTrue(beforeYear.getCurricularYearEntries().contains(previousYearEnrolment));

        // a null execution year filters nothing, so both enrolments are evaluated
        final Curriculum noYear = dismissal.getCurriculum(new DateTime(), null);
        assertFalse(noYear.isEmpty());
        assertEquals(2, noYear.getEnrolmentRelatedEntries().size());
        assertTrue(noYear.getEnrolmentRelatedEntries().contains(firstEnrolment));
        assertTrue(noYear.getEnrolmentRelatedEntries().contains(previousYearEnrolment));

        // no enrolment was created before the given moment, so the curriculum is empty
        final Curriculum tooEarly = dismissal.getCurriculum(new DateTime().minusDays(1), null);
        assertTrue(tooEarly.isEmpty());
        assertTrue(tooEarly.getCurriculumEntries().isEmpty());
    }

    private static Collection<IEnrolment> enrolments(final IEnrolment... enrolments) {
        return Arrays.asList(enrolments);
    }

    private static StudentCurricularPlan newStudentCurricularPlan() {
        final Student student = StudentTest.createStudent("Internal Substitution Test Student",
                CREATED_BY + "." + UUID.randomUUID());
        return StudentTest.createRegistration(student, degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
    }

    private static CurricularCourse createCourse(final String code, final String ects, final CurricularPeriod period,
            final ExecutionInterval interval, final CourseGroup courseGroup, final Unit coursesUnit) {
        final CompetenceCourse competenceCourse = CompetenceCourseTest.createCompetenceCourse("Course " + code, code,
                new BigDecimal(ects), SEMESTER, interval, coursesUnit);
        return new CurricularCourse(new BigDecimal(ects).doubleValue(), competenceCourse, courseGroup, period, interval, null);
    }

    private static Enrolment enrol(final StudentCurricularPlan studentCurricularPlan, final CurricularCourse curricularCourse,
            final ExecutionInterval interval) {
        return new Enrolment(studentCurricularPlan, studentCurricularPlan.findCurriculumGroupFor(enrolledGroup), curricularCourse,
                interval, EnrollmentCondition.FINAL, CREATED_BY);
    }

}
