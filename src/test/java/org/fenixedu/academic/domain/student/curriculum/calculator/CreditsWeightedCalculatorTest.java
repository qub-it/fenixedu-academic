package org.fenixedu.academic.domain.student.curriculum.calculator;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.domain.User;
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
            ConclusionRulesTestUtil.initData(); //maybe
            return null;
        });
    }

    @Test
    public void testCalculator_enrolments() {
        DegreeCurricularPlan dcp =
                ConclusionRulesTestUtil.createDegreeCurricularPlan(ExecutionYear.readExecutionYearByName("2021/2022"));
        Student student = User.findByUsername(STUDENT_CONCLUSION_A_USERNAME).getPerson().getStudent();
        StudentCurricularPlan scp = student.getRegistrationsFor(dcp).get(0).getLastStudentCurricularPlan();

        Curriculum curriculum =
                new Curriculum(scp.getRoot(), null, scp.getEnrolmentsSet().stream().collect(Collectors.toCollection(null)),
                        scp.getDismissals().stream().collect(Collectors.toCollection(null)), (Collection) Collections.EMPTY_LIST);

//    public Curriculum(final CurriculumModule curriculumModule, final ExecutionYear executionYear,
//                final Collection<ICurriculumEntry> averageEnrolmentRelatedEntries,
//                final Collection<ICurriculumEntry> averageDismissalRelatedEntries,
//                final Collection<ICurriculumEntry> curricularYearEntries) {
    }

    @Test
    public void testCalculator_dismissals() {

    }

    @Test
    public void testCalculator_enrolmentsAndDismissals() {

    }

    @Test
    public void testCalculator_numberOfDecimals() {

    }

    @Test
    public void testCalculator_roundingMode() {

    }

    public static boolean checkIfEqualsGrades(ConclusionGradeCalculatorResultsDTO results,
            ConclusionGradeCalculatorResultsDTO expected) {
        return results.getFinalGrade() == expected.getFinalGrade() && results.getRawGrade() == expected.getRawGrade()
                && results.getUnroundedGrade() == expected.getUnroundedGrade();
    }

}
