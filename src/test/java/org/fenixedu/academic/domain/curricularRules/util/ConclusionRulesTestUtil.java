package org.fenixedu.academic.domain.curricularRules.util;

import static org.fenixedu.academic.domain.CompetenceCourseTest.createCompetenceCourse;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_TYPE_CODE;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroupType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.administrativeOffice.dismissal.DismissalBean.SelectedCurricularCourse;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;

import pt.ist.fenixframework.FenixFramework;

public class ConclusionRulesTestUtil {

    public static final String CYCLE_GROUP = "Cycle";
    public static final String OPTIONAL_GROUP = "Optional";
    public static final String MANDATORY_GROUP = "Mandatory";
    public static final String ADMIN_USERNAME = "admin";
    public static final String STUDENT_CONCLUSION_A_USERNAME = "student.test.conclusion.a";
    public static final String GRADE_SCALE_NUMERIC = "TYPE20";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initData();
            return null;
        });
    }

    public static void initData() {
        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        EnrolmentTest.init();
        GradeScale.create(GRADE_SCALE_NUMERIC, new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true);

        StudentTest.createStudent("Student Test Conclusion A", STUDENT_CONCLUSION_A_USERNAME);
    }

    public static DegreeCurricularPlan createDegreeCurricularPlan(ExecutionYear executionYear) {
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
                new CourseGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP, CYCLE_GROUP, executionYear, null, null);

        final CourseGroup mandatoryGroup =
                new CourseGroup(cycleGroup, MANDATORY_GROUP, MANDATORY_GROUP, executionYear, null, null);
        final CurricularPeriod period1Y1S = degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod period1Y2S = degreeCurricularPlan.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod period2Y1S = degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER);
        createCurricularCourse("C1", "Course 1", new BigDecimal(6), period1Y1S, firstExecutionPeriod, mandatoryGroup);
        createCurricularCourse("C2", "Course 2", new BigDecimal(6), period1Y2S, firstExecutionPeriod, mandatoryGroup);
        createCurricularCourse("C3", "Course 3", new BigDecimal(6), period2Y1S, firstExecutionPeriod, mandatoryGroup);

        final CourseGroup optionalGroup = new CourseGroup(cycleGroup, OPTIONAL_GROUP, OPTIONAL_GROUP, executionYear, null, null);
        optionalGroup.setIsOptional(true);
        createCurricularCourse("C4", "Course 4", new BigDecimal(6), period2Y1S, firstExecutionPeriod, optionalGroup);
        createCurricularCourse("C5", "Course 5", new BigDecimal(6), period2Y1S, firstExecutionPeriod, optionalGroup);

//      System.out.println(degreeCurricularPlan.print());

        degreeCurricularPlan.createExecutionDegree(executionYear);

        return degreeCurricularPlan;
    }

    public static CurricularCourse createCurricularCourse(String code, String name, BigDecimal credits,
            CurricularPeriod curricularPeriod, ExecutionInterval interval, CourseGroup courseGroup) {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final CompetenceCourse competenceCourse = Optional.ofNullable(CompetenceCourse.find(code))
                .orElseGet(() -> createCompetenceCourse(name, code, credits, SEMESTER, interval, coursesUnit));

        final CurricularCourse existingCourse = courseGroup.getParentDegreeCurricularPlan().getCurricularCourseByCode(code);
        if (existingCourse != null) {
            new Context(courseGroup, existingCourse, curricularPeriod, interval, null);
            return existingCourse;
        }

        return new CurricularCourse(credits.doubleValue(), competenceCourse, courseGroup, curricularPeriod, interval, null);
    }

    public static OptionalCurricularCourse createOptionalCurricularCourse(String name, CurricularPeriod curricularPeriod,
            ExecutionInterval interval, CourseGroup courseGroup) {
        return new OptionalCurricularCourse(courseGroup, name, name, curricularPeriod, interval, null);
    }

    public static CourseGroup getChildGroup(CourseGroup parent, String name) {
        return (CourseGroup) parent.getChildDegreeModules().stream().filter(dm -> dm.getName().equals(name)).findFirst().get();
    }

    public static Registration createRegistration(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        final Student student = User.findByUsername(STUDENT_CONCLUSION_A_USERNAME).getPerson().getStudent();
        return StudentTest.createRegistration(student, degreeCurricularPlan, executionYear);
    }

    public static void enrol(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear, String... codes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        Stream.of(codes).forEach(c -> {
            final Context context = degreeCurricularPlan.getCurricularCourseByCode(c).getParentContextsSet().iterator().next();
            final ExecutionInterval enrolmentInterval = executionYear.getChildInterval(
                    context.getCurricularPeriod().getChildOrder(), context.getCurricularPeriod().getAcademicPeriod());
            EnrolmentTest.createEnrolment(studentCurricularPlan, enrolmentInterval, context, ADMIN_USERNAME);
        });
    }

    public static void enrolInNoCourseGroup(StudentCurricularPlan curricularPlan, ExecutionYear executionYear,
            NoCourseGroupCurriculumGroupType groupType, DegreeCurricularPlan targetCurricularPlan, String code) {
        final Context context = targetCurricularPlan.getCurricularCourseByCode(code).getParentContextsSet().iterator().next();
        final ExecutionInterval enrolmentInterval = executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(),
                context.getCurricularPeriod().getAcademicPeriod());
        final NoCourseGroupCurriculumGroup group = Optional.ofNullable(curricularPlan.getNoCourseGroupCurriculumGroup(groupType))
                .orElseGet(() -> NoCourseGroupCurriculumGroup.create(groupType, curricularPlan.getRoot()));

        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");
            final DegreeModuleToEnrol degreeModuleToEnrol = new DegreeModuleToEnrol(group, context, enrolmentInterval);

            curricularPlan.enrol(enrolmentInterval, Set.of(degreeModuleToEnrol), List.of(),
                    CurricularRuleLevel.ENROLMENT_WITH_RULES);
        } finally {
            Authenticate.unmock();
        }

    }

    public static void approve(StudentCurricularPlan studentCurricularPlan, String... codes) {
        approve(studentCurricularPlan, null, codes);
    }

    public static void approve(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear, String... codes) {
        Stream.of(codes).forEach(c -> {
            final Enrolment enrolment = studentCurricularPlan.getEnrolmentsSet().stream()
                    .filter(e -> executionYear == null || e.getExecutionYear() == executionYear)
                    .filter(e -> Objects.equals(e.getCode(), c)).findFirst().get();
            final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
            evaluation.setGrade(Grade.createGrade("10", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()));
            evaluation.setExamDateYearMonthDay(new YearMonthDay());
            evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
            enrolment.setEnrollmentState(EnrollmentState.APROVED);
        });
    }

    public static void approveOptionalEnrolment(final StudentCurricularPlan curricularPlan, final CurricularCourse targetCourse) {
        final OptionalEnrolment optionalEnrolment =
                (OptionalEnrolment) curricularPlan.getEnrolments(targetCourse).iterator().next();
        final EnrolmentEvaluation evaluation = optionalEnrolment.getEvaluationsSet().iterator().next();
        evaluation.setGrade(Grade.createGrade("10", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()));
        evaluation.setExamDateYearMonthDay(new YearMonthDay());
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        optionalEnrolment.setEnrollmentState(EnrollmentState.APROVED);
    }

    public static void flunk(StudentCurricularPlan studentCurricularPlan, String... codes) {
        flunk(studentCurricularPlan, null, codes);
    }

    public static void flunk(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear, String... codes) {
        Stream.of(codes).forEach(c -> {
            final Enrolment enrolment =
                    studentCurricularPlan.getEnrolmentsSet().stream().filter(e -> Objects.equals(e.getCode(), c))
                            .filter(e -> executionYear == null || e.getExecutionYear() == executionYear).findFirst().get();
            final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
            evaluation.setGrade(Grade.createGrade("0", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()));
            evaluation.setExamDateYearMonthDay(new YearMonthDay());
            evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
            enrolment.setEnrollmentState(EnrollmentState.NOT_APROVED);
        });
    }

    public static void annul(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear, String... codes) {
        Stream.of(codes).forEach(c -> {
            final Enrolment enrolment =
                    studentCurricularPlan.getEnrolmentsSet().stream().filter(e -> Objects.equals(e.getCode(), c))
                            .filter(e -> executionYear == null || e.getExecutionYear() == executionYear).findFirst().get();
            enrolment.annul();
        });
    }

    public static void createEquivalence(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear,
            String... codes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        Stream.of(codes).forEach(c -> {
            final CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode(c);
            final Context context = curricularCourse.getParentContextsSet().iterator().next();
            final ExecutionInterval executionInterval = executionYear.getChildInterval(
                    context.getCurricularPeriod().getChildOrder(), context.getCurricularPeriod().getAcademicPeriod());
            final SelectedCurricularCourse dismissalDTO = new SelectedCurricularCourse(curricularCourse, studentCurricularPlan);
            dismissalDTO.setCurriculumGroup(studentCurricularPlan.findCurriculumGroupFor(context.getParentCourseGroup()));

            new Equivalence(studentCurricularPlan, Set.of(dismissalDTO), Set.of(),
                    Grade.createGrade("10", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()), executionInterval);
        });
    }

    public static Credits createCredits(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear,
            CourseGroup courseGroup, BigDecimal credits, String... noEnrolCodes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        final Collection<CurricularCourse> noEnrolCourses =
                Stream.of(noEnrolCodes).map(c -> degreeCurricularPlan.getCurricularCourseByCode(c)).collect(Collectors.toSet());
        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

        return new Credits(studentCurricularPlan, courseGroup, Set.of(), noEnrolCourses, credits.doubleValue(),
                executionInterval);
    }

}
