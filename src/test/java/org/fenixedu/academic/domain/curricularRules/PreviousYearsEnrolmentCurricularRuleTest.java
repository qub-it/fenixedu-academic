package org.fenixedu.academic.domain.curricularRules;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.EnrollmentDomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.CYCLE_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.MANDATORY_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.getChildGroup;

/*
 * C1: 1Y1S
 * C2: 1Y2S
 * C3: 2Y1S
 */
@RunWith(FenixFrameworkRunner.class)
public class PreviousYearsEnrolmentCurricularRuleTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            return null;
        });
    }

    @Test
    public void givenModelByYear_whenEnrollingIn2Y1SWithoutEnrollingInAllCoursesFromPreviousYear_thenFail() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

    }

    @Test
    public void givenModelByYear_whenEnrollingIs2Y1SAfterEnrollingInAllCoursesFromPreviousYear_thenSuccess() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelBySemester_whenEnrollingIn2YS1WithoutEnrollingIn1YS2_thenSuccess() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        degreeCurricularPlan.setCurricularRuleValidationType(EnrolmentModel.SEMESTER);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelByYearWithPeriodsSplitByS1AndS2_whenEnrollingIn2YS1WithoutEnrollingIn1YS2_thenSuccess() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final EnrolmentModelConfigEntry firstSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        firstSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).get());
        final EnrolmentModelConfigEntry secondSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        secondSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).get());

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelByYearWithPeriodsSplitByS1AndS2_whenEnrollingIn2YS1WithoutEnrollingIn1YS1_thenFail() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final EnrolmentModelConfigEntry firstSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        firstSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).get());
        final EnrolmentModelConfigEntry secondSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        secondSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).get());

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelByYearWithPeriodsSplitByS1AndS2_whenEnrollingIn2YS1WithoutEnrollingIn1YS1ButCanConcludeCreditsInS2_thenSuccess() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 12d, 12d);

        final EnrolmentModelConfigEntry firstSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        firstSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).get());
        final EnrolmentModelConfigEntry secondSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        secondSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).get());

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelByYearWithPeriodsSplitByS1AndS2_whenEnrollingIn2YS1WithoutEnrollingIn1YS1ButCannotConcludeCreditsInS2_thenFail() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

//        System.out.println(degreeCurricularPlan.print());

        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);

        final EnrolmentModelConfigEntry firstSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        firstSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 1).get());
        final EnrolmentModelConfigEntry secondSemesterConfig = EnrolmentModelConfigEntry.create(degreeCurricularPlan);
        secondSemesterConfig.getAcademicPeriodOrdersSet().add(AcademicPeriodOrder.findBy(AcademicPeriod.SEMESTER, 2).get());

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelBySemester_whenEnrollingIn2YS1WithoutEnrollingIn1YS1ButCanConcludeCreditsInS2_thenSuccess() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        degreeCurricularPlan.setCurricularRuleValidationType(EnrolmentModel.SEMESTER);

        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 12d, 12d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

    @Test
    public void givenModelBySemester_whenEnrollingIn2YS1WithoutEnrollingIn1YS1ButCannotConcludeCreditsInS2_thenFail() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        degreeCurricularPlan.setCurricularRuleValidationType(EnrolmentModel.SEMESTER);

//        System.out.println(degreeCurricularPlan.print());

        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));
    }

}
