package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_B_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StandaloneCurriculumGroupTest {

    private static final String STUDENT_USERNAME = "standalone.group.test.student";

    private static StandaloneCurriculumGroup standaloneGroup;
    private static StandaloneCurriculumGroup emptyStandaloneGroup;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            DegreeTest.initDegree();
            CompetenceCourseTest.initCompetenceCourse();
            StudentTest.initRegistrationConfigEntities();
            EvaluationSeasonTest.initEvaluationSeasons();

            final Degree degree = Degree.find(DEGREE_A_CODE);
            final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            secondSemester = executionYear.getLastExecutionPeriod();
            final DegreeCurricularPlan dcp =
                    new DegreeCurricularPlan(degree, "Standalone DCP", AcademicPeriod.THREE_YEAR, firstSemester);
            dcp.createExecutionDegree(executionYear);

            final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1,
                    new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure()));
            // COURSE_A_CODE's CompetenceCourse is SEMESTER (only valid in firstSemester)
            final CurricularCourse courseA = createCourse(dcp, COURSE_A_CODE, semesterPeriod);
            // COURSE_B_CODE's CompetenceCourse is AcademicPeriod.YEAR (CompetenceCourseTest#createCompetenceCourseBAnnual) valid in both semesters
            final CurricularCourse courseB = createCourse(dcp, COURSE_B_CODE, semesterPeriod);

            final StudentCurricularPlan scp = createRegistration(dcp, STUDENT_USERNAME, executionYear);
            final StudentCurricularPlan emptyScp = createRegistration(dcp, "standalone.group.empty.student", executionYear);

            standaloneGroup = (StandaloneCurriculumGroup) NoCourseGroupCurriculumGroup
                    .create(NoCourseGroupCurriculumGroupType.STANDALONE, scp.getRoot());
            emptyStandaloneGroup = (StandaloneCurriculumGroup) NoCourseGroupCurriculumGroup
                    .create(NoCourseGroupCurriculumGroupType.STANDALONE, emptyScp.getRoot());

            final Enrolment courseAEnrolment =
                    new Enrolment(scp, standaloneGroup, courseA, firstSemester, EnrollmentCondition.FINAL, STUDENT_USERNAME);
            approve(courseAEnrolment);

            final Enrolment courseBEnrolment =
                    new Enrolment(scp, standaloneGroup, courseB, firstSemester, EnrollmentCondition.FINAL, STUDENT_USERNAME);
            approve(courseBEnrolment);

            return null;
        });
    }

    @Test
    public void testStandaloneCurriculumGroup_getNumberOfAllApprovedEnrolments() {
        assertEquals(2, standaloneGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(1, standaloneGroup.getNumberOfAllApprovedEnrolments(secondSemester));
        assertEquals(0, emptyStandaloneGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(0, emptyStandaloneGroup.getNumberOfAllApprovedEnrolments(secondSemester));
    }

    private static void approve(final Enrolment enrolment) {
        final GradeScale gradeScale = enrolment.getStudentCurricularPlan().getDegree().getNumericGradeScale();
        gradeScale.setMinimumApprovedGrade(new BigDecimal("10"));
        gradeScale.setMaximumApprovedGrade(new BigDecimal("20"));
        final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
        evaluation.setGrade(Grade.createGrade("14", gradeScale));
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
    }

    private static StudentCurricularPlan createRegistration(final DegreeCurricularPlan dcp, final String username,
            final ExecutionYear executionYear) {
        final Student student = StudentTest.createStudent("Standalone Curriculum Group Test Student", username);
        return StudentTest.createRegistration(student, dcp, executionYear).getLastStudentCurricularPlan();
    }

    private static CurricularCourse createCourse(final DegreeCurricularPlan dcp, final String code,
            final CurricularPeriod semesterPeriod) {
        final CurricularCourse curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(CompetenceCourse.find(code));
        curricularCourse.setName("Course " + code);
        new Context(dcp.getRoot(), curricularCourse, semesterPeriod, firstSemester, null);
        return curricularCourse;
    }
}