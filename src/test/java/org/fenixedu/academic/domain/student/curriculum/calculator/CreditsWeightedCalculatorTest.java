package org.fenixedu.academic.domain.student.curriculum.calculator;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.ExecutionYear;
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
        assertTrue(checkIfEqualsGrades(calculatedResults, "10.00000", "10.00", "10"));
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
        assertTrue(checkIfEqualsGrades(calculatedResults, "20.00000", "20.00", "20"));

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
        assertTrue(checkIfEqualsGrades(calculatedResults, "14.00000", "14.00", "14"));

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
        assertTrue(checkIfEqualsGrades(calculatedResults, "15.00000000000", "15.00000", "15"));
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
        assertTrue(checkIfEqualsGrades(calculatedResults, "13.33333", "13.33", "13"));
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
        assertTrue(checkIfEqualsGrades(calculatedResults, "10.00000", "10.00", "10"));
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
        assertTrue(checkIfEqualsGrades(calculatedResults, "10.50000000000", "10.50000", "11"));
    }

    @Test
    public void calculateAvgGrade_roundingMode() {
        ExecutionYear year = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1", "C2", "C3");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "10.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "10.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "10.5");

        ConclusionGradeCalculator calculator = CreditsWeightedCalculator.create(RoundingMode.DOWN, 2);

        ConclusionGradeCalculatorResultsDTO calculatedResults = calculator.calculate(scp.getRoot().getCurriculum());
        assertTrue(checkIfEqualsGrades(calculatedResults, "10.50000", "10.50", "10"));
    }

    public static boolean checkIfEqualsGrades(ConclusionGradeCalculatorResultsDTO results, String unroundedGrade, String rawGrade,
            String finalGrade) {
        ConclusionGradeCalculatorResultsDTO expectedResults =
                new ConclusionGradeCalculatorResultsDTO(ConclusionGradeCalculatorTestUtil.createGrade(unroundedGrade),
                        ConclusionGradeCalculatorTestUtil.createGrade(rawGrade),
                        ConclusionGradeCalculatorTestUtil.createGrade(finalGrade));
        return results.getFinalGrade() == expectedResults.getFinalGrade()
                && results.getIntermediateRoundedGrade() == expectedResults.getIntermediateRoundedGrade()
                && results.getUnroundedGrade() == expectedResults.getUnroundedGrade();
    }

}
