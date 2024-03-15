package org.fenixedu.academic.domain.student.curriculum.calculator;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.student.curriculum.calculator.util.ConclusionGradeCalculatorTestUtil;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CreditsWeightedCalculatorTest {

    public static final String STUDENT_CONCLUSION_A_USERNAME = "student.test.conclusion.a";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionGradeCalculatorTestUtil.initData();
            return null;
        });
    }

    @Test
    public void calculateAvgGrade_roundingModeCornerCase() {
        ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");
        ExecutionYear year3 = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year1);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year1, "C1", "C2", "C3", "C6", "C7", "C8", "C9");
        ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C10", "C11", "C12", "C13", "C14", "C15", "C16");
        ConclusionGradeCalculatorTestUtil.enrol(scp, year3, "C17", "C18", "C19", "C20", "C21", "C22");
        assertEquals(true, scp.getAllCurriculumLines().size() == 20);

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "20");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "20");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "18.8");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C6", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C7", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C8", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C9", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C10", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C11", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C12", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C13", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C14", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C15", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C16", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C17", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C18", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C19", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C20", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C21", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C22", "19.5");

        //Actual value: 19.49500000000
        ConclusionGradeCalculator calculatorHalfUP = CreditsWeightedCalculator.create(RoundingMode.HALF_UP, 2);
        ConclusionGradeCalculatorResultsDTO calculatedResultsHalfUP = calculatorHalfUP.calculate(scp.getRoot().getCurriculum());

//        System.out.println("Unrounded Grade: " + calculatedResultsHalfUP.getUnroundedGrade());
        scp.getEnrolmentsSet().stream()
                .forEach(cl -> /*System.out.println*/(cl.getCode() + ": " + cl.getGradeValue()).toString());

        assertEquals(calculatedResultsHalfUP.getUnroundedGrade(), grade("19.49500"));
        assertEquals(calculatedResultsHalfUP.getIntermediateRoundedGrade(), grade("19.50"));
        assertEquals(calculatedResultsHalfUP.getFinalGrade(), grade("20"));

        ConclusionGradeCalculator calculatorHalfDOWN = CreditsWeightedCalculator.create(RoundingMode.HALF_DOWN, 2);
        ConclusionGradeCalculatorResultsDTO calculatedResultsHalfDOWN =
                calculatorHalfDOWN.calculate(scp.getRoot().getCurriculum());

        assertEquals(calculatedResultsHalfDOWN.getUnroundedGrade(), grade("19.49500"));
        assertEquals(calculatedResultsHalfDOWN.getIntermediateRoundedGrade(), grade("19.49"));
        assertEquals(calculatedResultsHalfDOWN.getFinalGrade(), grade("19"));

        ConclusionGradeCalculator calculatorDOWN = CreditsWeightedCalculator.create(RoundingMode.DOWN, 2);
        ConclusionGradeCalculatorResultsDTO calculatedResultsDOWN = calculatorDOWN.calculate(scp.getRoot().getCurriculum());

        assertEquals(calculatedResultsDOWN.getUnroundedGrade(), grade("19.49500"));
        assertEquals(calculatedResultsDOWN.getIntermediateRoundedGrade(), grade("19.49"));
        assertEquals(calculatedResultsDOWN.getFinalGrade(), grade("19"));
    }

    @Test
    public void calculateAvgGrade_enrolments() {

        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3", "C4", "C5");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C4", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C5", "10");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.UP, 2);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("10.00000"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("10.00"));
        assertEquals(calculatedResults.getFinalGrade(), grade("10"));
    }

    @Test
    public void calculateAvgGrade_flunkedEnrolments() {

        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3", "C4", "C5");

        ConclusionGradeCalculatorTestUtil.flunk(scp, "C1", "5");
        ConclusionGradeCalculatorTestUtil.flunk(scp, "C2", "5");
        ConclusionGradeCalculatorTestUtil.flunk(scp, "C4", "5");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "20");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C5", "20");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.UP, 2);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("20.00000"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("20.00"));
        assertEquals(calculatedResults.getFinalGrade(), grade("20"));

    }

    @Test
    public void calculateAvgGrade_harderAvgEnrolments() {

        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3", "C4", "C5");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C4", "10");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "20");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C5", "20");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.UP, 2);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("14.00000"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("14.00"));
        assertEquals(calculatedResults.getFinalGrade(), grade("14"));

    }

    @Test
    public void calculateAvgGrade_equivalenceDismissal() {
        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3");

        ConclusionGradeCalculatorTestUtil.createEquivalence(scp, year, "C1", "20");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "10");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.UP, 5);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("15.00000000000"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("15.00000"));
        assertEquals(calculatedResults.getFinalGrade(), grade("15"));
    }

    @Test
    public void calculateAvgGrade_creditDismissal() {
        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C4");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "20");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C4", "10");
        Credits credits = ConclusionRulesTestUtil.createCredits(scp, year, scp.getDegreeCurricularPlan().getRoot(),
                new BigDecimal("6.0"), "C3");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.HALF_UP, 2);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("13.33333"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("13.33"));
        assertEquals(calculatedResults.getFinalGrade(), grade("13"));
    }

    @Test
    public void calculateAvgGrade_testNonNumericClass() {
        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        GradeScale nonNumberic = GradeScale.findUniqueByCode(ConclusionGradeCalculatorTestUtil.GRADE_SCALE_QUALITATIVE).get();
        CompetenceCourse cc = scp.getDegreeCurricularPlan().getCurricularCourseByCode("C1").getCompetenceCourse();
        cc.setGradeScale(nonNumberic);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3");

        ConclusionGradeCalculatorTestUtil.approveQualitative(scp, "C1", "APPROVED");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "10");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.UP, 2);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("10.00000"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("10.00"));
        assertEquals(calculatedResults.getFinalGrade(), grade("10"));
    }

    @Test
    public void calculateAvgGrade_numberOfDecimals() {
        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "10.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "10.5");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.UP, 5);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertEquals(calculatedResults.getUnroundedGrade(), grade("10.50000000000"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("10.50000"));
        assertEquals(calculatedResults.getFinalGrade(), grade("11"));
    }

    @Test
    public void calculateAvgGrade_roundingMode() {
        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3");
        assertEquals(true, scp.getAllCurriculumLines().size() == 3);

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "12");
        Optional<Enrolment> course = scp.getEnrolmentsSet().stream().filter(e -> Objects.equals(e.getCode(), "C1")).findAny();
        assertEquals(true, course.isPresent());
        assertEquals(course.get().getGrade(), grade("12"));

        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10.5");
        course = scp.getEnrolmentsSet().stream().filter(e -> Objects.equals(e.getCode(), "C2")).findAny();
        assertEquals(true, course.isPresent());
        assertEquals(course.get().getGrade(), grade("10.5"));

        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "10");
        course = scp.getEnrolmentsSet().stream().filter(e -> Objects.equals(e.getCode(), "C3")).findAny();
        assertEquals(true, course.isPresent());
        assertEquals(course.get().getGrade(), grade("10"));

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.DOWN, 1);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());

        assertEquals(calculatedResults.getUnroundedGrade(), grade("10.833"));
        assertEquals(calculatedResults.getIntermediateRoundedGrade(), grade("10.8"));
        assertEquals(calculatedResults.getFinalGrade(), grade("11"));

    }

    private static Grade grade(String confirm) {
        return ConclusionGradeCalculatorTestUtil.createGrade(confirm);
    }

}
