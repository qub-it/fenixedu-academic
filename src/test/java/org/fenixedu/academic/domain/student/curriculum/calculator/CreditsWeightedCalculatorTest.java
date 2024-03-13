package org.fenixedu.academic.domain.student.curriculum.calculator;

import static org.junit.Assert.assertTrue;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.calculator.util.ConclusionGradeCalculatorTestUtil;
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

        Curriculum curriculum = new Curriculum(scp.getRoot(), null, scp.getEnrolmentsSet().stream().collect(Collectors.toList()),
                scp.getDismissals().stream().collect(Collectors.toList()), (Collection) Collections.EMPTY_LIST);

        ConclusionGradeCalculator calc = CreditsWeightedCalculator.create(RoundingMode.UP, 2);

        ConclusionGradeCalculatorResultsDTO results = calc.calculate(curriculum);
        ConclusionGradeCalculatorResultsDTO expectedResults =
                new ConclusionGradeCalculatorResultsDTO(ConclusionGradeCalculatorTestUtil.createGrade("10"),
                        ConclusionGradeCalculatorTestUtil.createGrade("10"), ConclusionGradeCalculatorTestUtil.createGrade("10"));
        assertTrue(checkIfEqualsGrades(results, expectedResults));
    }

    @Test
    public void calculateAvgGrade_dismissals() {
        //TODO
    }

    @Test
    public void calculateAvgGrade_enrolmentsAndDismissals() {
        //TODO 
    }

    @Test
    public void calculateAvgGrade_numberOfDecimals() {
        //TODO
    }

    @Test
    public void calculateAvgGrade_roundingMode() {
        //TODO
    }

    public static boolean checkIfEqualsGrades(ConclusionGradeCalculatorResultsDTO results,
            ConclusionGradeCalculatorResultsDTO expected) {
        return results.getFinalGrade() == expected.getFinalGrade() && results.getRawGrade() == expected.getRawGrade()
                && results.getUnroundedGrade() == expected.getUnroundedGrade();
    }

}
