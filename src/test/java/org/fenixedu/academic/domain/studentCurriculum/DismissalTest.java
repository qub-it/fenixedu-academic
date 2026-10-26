package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeCurricularPlanTest;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.administrativeOffice.dismissal.DismissalBean.SelectedCurricularCourse;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DismissalTest {

    private static final double MAX_GROUP_CREDITS = Double.MAX_VALUE;
    private static final String COURSE_PREFIX = "DISM_" + UUID.randomUUID().toString().substring(0, 8) + "_";
    private static final String GRADE_SCALE_CODE = "TYPE20";

    private static ExecutionYear executionYear;
    private static ExecutionYear previousYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static CurricularPeriod firstPeriod;
    private static CurricularPeriod secondPeriod;
    private static Unit coursesUnit;
    private static DegreeCurricularPlan dcp;
    private static StudentCurricularPlan scp;
    private static CourseGroup enrolledGroup;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initStudentAndRegistration();
            EvaluationSeasonTest.initEvaluationSeasons();

            executionYear = ExecutionYear.findCurrent(null);
            previousYear = (ExecutionYear) executionYear.getPrevious();
            firstSemester = executionYear.getFirstExecutionPeriod();
            secondSemester = executionYear.getLastExecutionPeriod();
            coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();

            dcp = Degree.find(DegreeTest.DEGREE_A_CODE).getDegreeCurricularPlansSet().stream()
                    .filter(plan -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(plan.getName())).findAny().orElseThrow();
            final CurricularPeriod yearPeriod = dcp.getDegreeStructure().findChild(AcademicPeriod.YEAR, 1).orElseThrow();
            firstPeriod = yearPeriod.findChild(SEMESTER, 1).orElseThrow();
            secondPeriod = yearPeriod.findChild(SEMESTER, 2).orElseGet(() -> new CurricularPeriod(SEMESTER, 2, yearPeriod));

            enrolledGroup = dcp.getRoot();
            scp = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow()
                    .getLastStudentCurricularPlan();

            GradeScale.findUniqueByCode(GRADE_SCALE_CODE).orElseGet(
                    () -> GradeScale.create(GRADE_SCALE_CODE, new LocalizedString(Locale.getDefault(), "Type 20"),
                            new BigDecimal("0"), new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false,
                            true));

            return null;
        });
    }

    @Test
    public void testDismissal_buildSourcesToPrint() {
        final Enrolment first = enrol(firstPeriod, firstSemester);
        final Enrolment second = enrol(secondPeriod, secondSemester);
        final CreditsDismissal dismissal = newCreditsDismissal(12d, first, second);

        final String printed = dismissal.print("").toString();
        final String sources = printed.substring(printed.lastIndexOf("[ ") + 2, printed.lastIndexOf(" ]"));

        final String firstSource = first.getCode() + "(" + first.getEctsCreditsForCurriculum().toPlainString() + ")";
        final String secondSource = second.getCode() + "(" + second.getEctsCreditsForCurriculum().toPlainString() + ")";

        assertEquals(Set.of(firstSource, secondSource),
                Arrays.stream(sources.split(", ")).collect(Collectors.toSet()));
    }

    @Test
    public void testDismissal_getIEnrolmentsLastExecutionYear() {
        final Enrolment previous = enrol(firstPeriod, previousYear.getFirstExecutionPeriod());
        final Enrolment current = enrol(firstPeriod, firstSemester);

        // Sources from different years: returns the most recent one
        assertEquals(executionYear, newCreditsDismissal(12d, previous, current).getIEnrolmentsLastExecutionYear());
        assertEquals(executionYear, newCreditsDismissal(12d, current, previous).getIEnrolmentsLastExecutionYear());

        // Only sources from a previous year: returns that year, not the dismissal's year
        assertEquals(previousYear, newCreditsDismissal(12d, previous).getIEnrolmentsLastExecutionYear());

        // No sources: no year
        assertNull(newCreditsDismissal(0d).getIEnrolmentsLastExecutionYear());
    }

    @Test
    public void testDismissal_getEctsCreditsForCurriculum() {
        // Credits in a group: uses the credits given to the Credits
        final CreditsDismissal creditsDismissal = newCreditsDismissal(12d);
        assertEquals(BigDecimal.valueOf(creditsDismissal.getCredits().getGivenCredits()),
                creditsDismissal.getEctsCreditsForCurriculum());
        assertEquals(new BigDecimal("12.0"), creditsDismissal.getEctsCreditsForCurriculum());

        // Substitution: uses the ECTS of the dismissed curricular course
        final Dismissal substitutionDismissal = newSubstitutionDismissal();
        assertEquals(BigDecimal.valueOf(substitutionDismissal.getCurricularCourse().getEctsCredits()),
                substitutionDismissal.getEctsCreditsForCurriculum());
        assertEquals(new BigDecimal("6.0"), substitutionDismissal.getEctsCreditsForCurriculum());

        // Equivalence: uses the ECTS of the dismissed curricular course
        final Dismissal equivalenceDismissal = newEquivalenceDismissal();
        assertEquals(BigDecimal.valueOf(equivalenceDismissal.getCurricularCourse().getEctsCredits()),
                equivalenceDismissal.getEctsCreditsForCurriculum());
        assertEquals(new BigDecimal("6.0"), equivalenceDismissal.getEctsCreditsForCurriculum());
    }

    @Test
    public void testDismissal_calculateConclusionDate() {
        final YearMonthDay firstExamDate = new YearMonthDay(2021, 2, 15);
        final YearMonthDay secondExamDate = new YearMonthDay(2021, 6, 30);
        final Enrolment first = approve(enrol(firstPeriod, firstSemester), firstExamDate);
        final Enrolment second = approve(enrol(secondPeriod, secondSemester), secondExamDate);

        final CreditsDismissal dismissal = newCreditsDismissal(12d, first, second);
        assertEquals(secondExamDate, dismissal.calculateConclusionDate());

        final LocalDate officialDate = new LocalDate(2025, 3, 1);
        dismissal.getCredits().setOfficialDate(officialDate);
        assertEquals(new YearMonthDay(officialDate), dismissal.calculateConclusionDate());

        final CreditsDismissal emptyDismissal = newCreditsDismissal(0d);
        assertEquals(emptyDismissal.getExecutionInterval().getBeginDateYearMonthDay(),
                emptyDismissal.calculateConclusionDate());
    }

    @Test
    public void testDismissal_getWeigthForCurriculum() {
        // Credits in a group: no own grade, so no weight
        final CreditsDismissal creditsDismissal = newCreditsDismissal(12d);
        assertFalse(creditsDismissal.getCredits().isEquivalence());
        assertNull(creditsDismissal.getWeigthForCurriculum());

        // Substitution: the grade comes from the source enrolments, so the dismissal has no weight
        final Dismissal substitutionDismissal = newSubstitutionDismissal();
        assertTrue(substitutionDismissal.getCredits().isSubstitution());
        assertNull(substitutionDismissal.getWeigthForCurriculum());

        // Equivalence: has its own grade, so its ECTS are used as weight
        final Dismissal equivalenceDismissal = newEquivalenceDismissal();
        assertTrue(equivalenceDismissal.getCredits().isEquivalence());
        assertEquals(equivalenceDismissal.getEctsCreditsForCurriculum(), equivalenceDismissal.getWeigthForCurriculum());
        assertEquals(new BigDecimal("6.0"), equivalenceDismissal.getWeigthForCurriculum());
    }

    private static CurricularCourse createCourse(final CurricularPeriod period, final ExecutionInterval interval,
            final CourseGroup courseGroup) {
        final String code = COURSE_PREFIX + "C_" + UUID.randomUUID().toString().substring(0, 8);
        final BigDecimal ects = new BigDecimal("6");
        final CompetenceCourse competenceCourse =
                CompetenceCourseTest.createCompetenceCourse("Course " + code, code, ects, SEMESTER, interval, coursesUnit);
        return new CurricularCourse(ects.doubleValue(), competenceCourse, courseGroup, period, interval, null);
    }

    private static Enrolment enrol(final CurricularPeriod period, final ExecutionInterval interval) {
        final CurricularCourse course = createCourse(period, interval, enrolledGroup);
        return new Enrolment(scp, scp.findCurriculumGroupFor(enrolledGroup), course, interval, EnrollmentCondition.FINAL,
                "dismissal.test");
    }

    private static Enrolment approve(final Enrolment enrolment, final YearMonthDay examDate) {
        final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
        evaluation.setGrade(Grade.createGrade("14", GradeScale.findUniqueByCode(GRADE_SCALE_CODE).orElseThrow()));
        evaluation.setExamDateYearMonthDay(examDate);
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        enrolment.setEnrollmentState(EnrollmentState.APROVED);
        return enrolment;
    }

    private static Dismissal newEquivalenceDismissal() {
        final Grade grade = Grade.createGrade("14", GradeScale.findUniqueByCode(GRADE_SCALE_CODE).orElseThrow());
        final SelectedCurricularCourse selected =
                new SelectedCurricularCourse(createCourse(firstPeriod, firstSemester, enrolledGroup), scp);
        selected.setCurriculumGroup(scp.findCurriculumGroupFor(enrolledGroup));
        return new Equivalence(scp, List.of(selected), List.of(), grade, firstSemester).getDismissalsSet().iterator().next();
    }

    private static Dismissal newSubstitutionDismissal() {
        final Enrolment source = approve(enrol(firstPeriod, firstSemester), new YearMonthDay(2021, 2, 15));
        final SelectedCurricularCourse selected =
                new SelectedCurricularCourse(createCourse(firstPeriod, firstSemester, enrolledGroup), scp);
        selected.setCurriculumGroup(scp.findCurriculumGroupFor(enrolledGroup));
        return new Substitution(scp, List.of(selected), List.of(source), firstSemester).getDismissalsSet().iterator().next();
    }

    private static CreditsDismissal newCreditsDismissal(final double ects, final IEnrolment... enrolments) {
        final Credits credits =
                new Credits(scp, newGroup().getDegreeModule(), List.of(enrolments), List.of(), ects, firstSemester);
        return (CreditsDismissal) credits.getDismissalsSet().iterator().next();
    }

    private static CurriculumGroup newGroup() {
        final String name = COURSE_PREFIX + "G_" + UUID.randomUUID().toString().substring(0, 8);
        final CourseGroup courseGroup = new CourseGroup(dcp.getRoot(), name, name, firstSemester, null);
        new CreditsLimit(courseGroup, null, firstSemester, null, 0d, MAX_GROUP_CREDITS);
        return new CurriculumGroup(scp.getRoot(), courseGroup);
    }
}