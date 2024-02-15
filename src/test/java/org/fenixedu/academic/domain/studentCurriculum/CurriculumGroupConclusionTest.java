package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.CompetenceCourseTest.createCompetenceCourse;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_TYPE_CODE;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.DegreeModulesSelectionLimit;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CurriculumGroupConclusionTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String STUDENT_CONCLUSION_A_USERNAME = "student.test.conclusion.a";
    private static final String GRADE_SCALE_NUMERIC = "TYPE20";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initData();
            return null;
        });
    }

    private static void initData() {
        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        EnrolmentTest.init();
        GradeScale.create(GRADE_SCALE_NUMERIC, new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true);

        StudentTest.createStudent("Student Test Conclusion A", STUDENT_CONCLUSION_A_USERNAME);
    }

    @Test
    public void testCreditsLimit_RootConcluded() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testCreditsLimit_RootConcluded");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(true, curricularPlan.getRoot().isConcluded());
    }

    @Test
    public void testCreditsLimit_RootNotConcluded() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testCreditsLimit_RootNotConcluded");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, curricularPlan.getRoot().isConcluded());
    }

    //Beware that no course groups always report concluded so they are accounted as concluded modules
    //but in this case minimum credits are not satisfied
    @Test
    public void testCreditsLimit_RootConclusionWithNoCourseGroupsOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");

//        System.out.println("testCreditsLimit_RootConclusionWithNoCourseGroupsOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, curricularPlan.getRoot().isConcluded());
    }

    //Beware that no course groups always report concluded so they are accounted as concluded modules if we apply rule to root
    @Test
    public void testDegreeModulesLimit_RootConclusionWithNoCourseGroupsOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new DegreeModulesSelectionLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");

//        System.out.println("testDegreeModulesLimit_RootConclusionWithNoCourseGroupsOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(true, curricularPlan.getRoot().isConcluded());
    }

    private DegreeCurricularPlan createDegreeCurricularPlan(ExecutionYear executionYear) {
        final ExecutionInterval firstExecutionPeriod = executionYear.getFirstExecutionPeriod();
        final DegreeType degreeType = DegreeType.findByCode(DEGREE_TYPE_CODE).get();
        final Degree degree = DegreeTest.createDegree(degreeType, "D" + System.currentTimeMillis(),
                "D" + System.currentTimeMillis(), executionYear);
        final User user = User.findByUsername(ADMIN_USERNAME);

        final DegreeCurricularPlan degreeCurricularPlan =
                degree.createDegreeCurricularPlan("Plan 1", user.getPerson(), AcademicPeriod.THREE_YEAR);
        degreeCurricularPlan.setCurricularStage(CurricularStage.APPROVED);
        final CurricularPeriod firstYearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        new CurricularPeriod(AcademicPeriod.SEMESTER, 1, firstYearPeriod);
        new CurricularPeriod(AcademicPeriod.SEMESTER, 2, firstYearPeriod);

        final CurricularPeriod secondYearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 2, degreeCurricularPlan.getDegreeStructure());
        new CurricularPeriod(AcademicPeriod.SEMESTER, 1, secondYearPeriod);
        new CurricularPeriod(AcademicPeriod.SEMESTER, 2, secondYearPeriod);

        final CourseGroup cycleGroup =
                new CourseGroup(degreeCurricularPlan.getRoot(), "Cycle", "Cycle", executionYear, null, null);

        final CourseGroup mandatoryGroup = new CourseGroup(cycleGroup, "Mandatory", "Mandatory", executionYear, null, null);
        final CurricularPeriod period1Y1S = degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod period1Y2S = degreeCurricularPlan.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod period2Y1S = degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER);
        createCurricularCourse("C1", "Course 1", new BigDecimal(6), period1Y1S, firstExecutionPeriod, mandatoryGroup);
        createCurricularCourse("C2", "Course 2", new BigDecimal(6), period1Y2S, firstExecutionPeriod, mandatoryGroup);
        createCurricularCourse("C3", "Course 3", new BigDecimal(6), period2Y1S, firstExecutionPeriod, mandatoryGroup);

        final CourseGroup optionalGroup = new CourseGroup(cycleGroup, "Optional", "Optional", executionYear, null, null);
        createCurricularCourse("C4", "Course 4", new BigDecimal(6), period2Y1S, firstExecutionPeriod, optionalGroup);
        createCurricularCourse("C5", "Course 5", new BigDecimal(6), period2Y1S, firstExecutionPeriod, optionalGroup);

//      System.out.println(degreeCurricularPlan.print());

        degreeCurricularPlan.createExecutionDegree(executionYear);

        return degreeCurricularPlan;
    }

    private Registration createRegistration(final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {
        final Student student = User.findByUsername(STUDENT_CONCLUSION_A_USERNAME).getPerson().getStudent();
        return StudentTest.createRegistration(student, degreeCurricularPlan, executionYear);
    }

    private void enrol(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear, String... codes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        Stream.of(codes).forEach(c -> {
            final Context context = degreeCurricularPlan.getCurricularCourseByCode(c).getParentContextsSet().iterator().next();
            final ExecutionInterval enrolmentInterval =
                    executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(), SEMESTER);
            EnrolmentTest.createEnrolment(studentCurricularPlan, enrolmentInterval, context, ADMIN_USERNAME);
        });
    }

    private void approve(StudentCurricularPlan studentCurricularPlan, String... codes) {
        Stream.of(codes).forEach(c -> {
            final Enrolment enrolment = studentCurricularPlan.getEnrolmentsSet().stream()
                    .filter(e -> Objects.equals(e.getCode(), c)).findFirst().get();
            final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
            evaluation.setGrade(Grade.createGrade("10", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()));
            evaluation.setExamDateYearMonthDay(new YearMonthDay());
            evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
            enrolment.setEnrollmentState(EnrollmentState.APROVED);
        });
    }

    private CurricularCourse createCurricularCourse(String code, String name, BigDecimal credits,
            CurricularPeriod curricularPeriod, ExecutionInterval interval, CourseGroup courseGroup) {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final CompetenceCourse competenceCourse = Optional.ofNullable(CompetenceCourse.find(code))
                .orElseGet(() -> createCompetenceCourse(name, code, credits, SEMESTER, interval, coursesUnit));

        return new CurricularCourse(credits.doubleValue(), competenceCourse, courseGroup, curricularPeriod, interval, null);
    }

}
