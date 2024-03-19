package org.fenixedu.academic.domain.student.curriculum.calculator;

import static org.junit.Assert.assertEquals;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.curriculum.calculator.util.ConclusionGradeCalculatorTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SequentialRoundingCalculatorTest {

    public static final String STUDENT_CONCLUSION_A_USERNAME = "student.test.conclusion.a";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionGradeCalculatorTestUtil.initData();
            return null;
        });
    }

    @Test
    public void calculateAvgGrade_objectiveCase() {
        ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");
        ExecutionYear year3 = ExecutionYear.readExecutionYearByName("2021/2022");
        StudentCurricularPlan scp = ConclusionGradeCalculatorTestUtil.createStudentCurricularPlan(year1);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year1, "C1", "C2", "C3", "C6", "C7");
        ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C8", "C9", "C10", "C11", "C12", "C13");
        assertEquals(true, scp.getAllCurriculumLines().size() == 11);

        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C6", "19.5");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C7", "19.5");

        ConclusionGradeCalculatorTestUtil.approve(scp, "C8", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C9", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C10", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C11", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C12", "19.4");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C13", "19.4");

        //Actual value: 19.44545455
        ConclusionGradeCalculator calculatorHalfUP = SequentialRoundingCalculator.create();
        ConclusionGradeCalculatorResultsDTO calculatedResultsHalfUP = calculatorHalfUP.calculate(scp.getRoot().getCurriculum());

        assertEquals(grade("19.44545"), calculatedResultsHalfUP.getUnroundedGrade());
        assertEquals(grade("19.45"), calculatedResultsHalfUP.getIntermediateRoundedGrade());
        assertEquals(grade("20"), calculatedResultsHalfUP.getFinalGrade());
    }

    private static Grade grade(String confirm) {
        return ConclusionGradeCalculatorTestUtil.createGrade(confirm);
    }

}
