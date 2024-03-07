package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.CompetenceCourseTest.createCompetenceCourse;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_TYPE_CODE;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;

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
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.DegreeModuleSetApprovalRule;
import org.fenixedu.academic.domain.curricularRules.DegreeModulesSelectionLimit;
import org.fenixedu.academic.domain.curricularRules.EnrolmentToBeApprovedByCoordinator;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
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
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.administrativeOffice.dismissal.DismissalBean.SelectedCurricularCourse;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

//TODO: split into multiple tests (one per conclusion rule)
@RunWith(FenixFrameworkRunner.class)
public class CurriculumGroupConclusionTest {

    private static final String CYCLE_GROUP = "Cycle";
    private static final String OPTIONAL_GROUP = "Optional";
    private static final String MANDATORY_GROUP = "Mandatory";
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
    public void testCreditsLimit_RootCanConclude() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testCreditsLimit_RootCanConclude");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, curricularPlan.getRoot().isConcluded());
        assertEquals(true, curricularPlan.getRoot().canConclude(executionYear));
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

    @Test
    public void testCreditsLimit_RootCannotConclude() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");
        flunk(curricularPlan, "C3");

//        System.out.println("testCreditsLimit_RootCannotConclude");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, curricularPlan.getRoot().isConcluded());
        assertEquals(false, curricularPlan.getRoot().canConclude(executionYear));
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

    @Test
    public void testCreditsLimit_RootCannotConcludeWithNoCourseGroupsOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

//        System.out.println("testCreditsLimit_RootCannotConcludeWithNoCourseGroupsOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, curricularPlan.getRoot().isConcluded());
        assertEquals(false, curricularPlan.getRoot().canConclude(executionYear));
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

    @Test
    public void testDegreeModulesLimit_RootCanConcludeWithNoCourseGroupsOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new DegreeModulesSelectionLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

//        System.out.println("testDegreeModulesLimit_RootCanConcludeWithNoCourseGroupsOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(true, curricularPlan.getRoot().isConcluded());
        assertEquals(false, curricularPlan.getRoot().canConclude(executionYear));
    }

    @Test
    public void testDegreeModulesLimit_ConclusionWithoutRequiredModules() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new DegreeModulesSelectionLimit(optionalGroup, null, executionYear, null, 2, 2);

//        System.out.println("testDegreeModulesLimit_ConclusionWithoutRequiredModules");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModulesLimit_CannotCanConcludeWithoutRequiredModules() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new DegreeModulesSelectionLimit(optionalGroup, null, executionYear, null, 2, 2);

//        System.out.println("testDegreeModulesLimit_CannotCanConcludeWithoutRequiredModules");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);
        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);
        final CurriculumGroup optioanlCurriculumGroup = curricularPlan.findCurriculumGroupFor(optionalGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
        assertEquals(false, optioanlCurriculumGroup.canConclude(executionYear));
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModulesLimit_ConclusionWithRequiredModules() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new DegreeModulesSelectionLimit(optionalGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testDegreeModulesLimit_ConclusionWithRequiredModules");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModulesLimit_CanConcludeWithRequiredModules() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new DegreeModulesSelectionLimit(optionalGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4");

//        System.out.println("testDegreeModulesLimit_CanConcludeWithRequiredModules");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));

        flunk(curricularPlan, "C5");

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModulesLimit_ConclusionWithRequiredModulesMixedWithCreditsLimitInChildren() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new CreditsLimit(optionalGroup, null, executionYear, null, 12d, 12d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testDegreeModulesLimit_ConclusionWithRequiredModulesMixedWithCreditsLimitInChildren");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModulesLimit_CanConcludeWithRequiredModulesMixedWithCreditsLimitInChildren() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new CreditsLimit(optionalGroup, null, executionYear, null, 12d, 12d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4");

//        System.out.println("testDegreeModulesLimit_CanConcludeWithRequiredModulesMixedWithCreditsLimitInChildren");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));

        flunk(curricularPlan, "C5");

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    //behaviour difference regarding CreditsLimit since DegreeModulesLimit checks for value not isValid
    //UNKNOWN (group without rules) is false althought isValid returns true
    @Test
    public void testDegreeModulesLimit_ConclusionWithoutRulesOnChildGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testDegreeModulesLimit_ConclusionWithoutRulesOnChildGroups");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
    }

    //behaviour difference regarding CreditsLimit since DegreeModulesLimit checks for value not isValid
    //UNKNOWN (group without rules) is false althought isValid returns true
    @Test
    public void testDegreeModulesLimit_CannotConcludeWithoutRulesOnChildGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testDegreeModulesLimit_CannotConcludeWithoutRulesOnChildGroups");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    //behaviour difference regarding DegreeModulesLimit since CreditsLimit checks for isValid not value
    //UNKNOWN (group without rules) is false althought isValid returns true
    @Test
    public void testCreditsLimit_ConclusionWithoutRulesOnChildGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new CreditsLimit(cycleGroup, null, executionYear, null, 30d, 30d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testCreditsLimit_ConclusionWithoutRulesOnChildGroups");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.isConcluded());
    }

    //behaviour difference regarding DegreeModulesLimit since CreditsLimit checks for isValid not value
    //UNKNOWN (group without rules) is false althought isValid returns true
    @Test
    public void testCreditsLimit_CanConcludeWithoutRulesOnChildGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new CreditsLimit(cycleGroup, null, executionYear, null, 30d, 30d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4");

//        System.out.println("testCreditsLimit_CanConcludeWithoutRulesOnChildGroups");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testMultipleConclusionRulesMixed_ConclusionWithRequiredModules() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new CreditsLimit(cycleGroup, null, executionYear, null, 30d, 30d);
        final DegreeModulesSelectionLimit degreeModulesRule =
                new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 3, 3);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new CreditsLimit(optionalGroup, null, executionYear, null, 12d, 12d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testMultipleConclusionRulesMixed_ConclusionWithRequiredModules");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());

        degreeModulesRule.setMinimumLimit(2);
        degreeModulesRule.setMaximumLimit(2);

        assertEquals(true, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testMultipleConclusionRulesMixed_CanConcludeWithRequiredModules() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new CreditsLimit(cycleGroup, null, executionYear, null, 30d, 30d);
        final DegreeModulesSelectionLimit degreeModulesRule =
                new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 3, 3);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new CreditsLimit(optionalGroup, null, executionYear, null, 12d, 12d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");

//        System.out.println("testMultipleConclusionRulesMixed_CanConcludeWithRequiredModules");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));

        degreeModulesRule.setMinimumLimit(2);
        degreeModulesRule.setMaximumLimit(2);

        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModulesLimit_ConclusionWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testDegreeModulesLimit_ConclusionWithCurriculumLinesOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());

        approve(curricularPlan, "C3");

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModulesLimit_CanConcludeWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testDegreeModulesLimit_CanConcludeWithCurriculumLinesOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testCreditsLimit_ConclusionWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testCreditsLimit_ConclusionWithCurriculumLinesOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());

        approve(curricularPlan, "C3");

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testCreditsLimit_CanConcludeWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testCreditsLimit_CanConcludeWithCurriculumLinesOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));

    }

    @Test
    public void testCreditsLimit_ConclusionWithCurriculumLinesAndGroupsMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 24d, 24d);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testCreditsLimit_ConclusionWithCurriculumLinesAndGroupsMixed");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());

        approve(curricularPlan, "C6");

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testCreditsLimit_CanConcludeWithCurriculumLinesAndGroupsMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 24d, 24d);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testCreditsLimit_CanConcludeWithCurriculumLinesAndGroupsMixed");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

        enrol(curricularPlan, executionYear, "C6");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModulesLimit_ConclusionWithCurriculumLinesAndGroupsMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 4, 4);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testDegreeModulesLimit_ConclusionWithCurriculumLinesAndGroupsMixed");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());

        approve(curricularPlan, "C6");

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModulesLimit_CanConcludeWithCurriculumLinesAndGroupsMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 4, 4);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testDegreeModulesLimit_CanConcludeWithCurriculumLinesAndGroupsMixed");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

        enrol(curricularPlan, executionYear, "C6");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));

    }

    @Test
    public void testIsConcludedFalseAndCanConcludeFalse() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testIsConcludedFalseAndCanConcludeFalse");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

    }

    @Test
    public void testIsConcludedFalseAndCanConcludeTrue() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println("testIsConcludedFalseAndCanConcludeTrue");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testIsConcludedTrueAndCanConcludeFalse() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testIsConcludedTrueAndCanConcludeFalse");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    //// Degree Module Set Approval Tests

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

//        System.out.println(mandatoryGroup.getCurricularRules(executionYear).stream()
//                .map(r -> CurricularRuleLabelFormatter.getLabel(r)).collect(Collectors.joining("; ")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println("testDegreeModuleSetApproval_ConcludedWithCurriculumLinesOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_NotConcludedWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_CannotConcludeWithSomeEnrolmentsApprovedAndOthersFlunked() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");
        flunk(curricularPlan, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithApprovalsLimitedToOtherGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, optionalGroup,
                Set.of(degreeCurricularPlan.getCurricularCourseByCode("C5")));

//        System.out.println(mandatoryGroup.getCurricularRules(executionYear).stream()
//                .map(r -> CurricularRuleLabelFormatter.getLabel(r)).collect(Collectors.joining("; ")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C5");

//        System.out.println("testDegreeModuleSetApproval_ConcludedWithApprovalsLimitedToGroup");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_NotConcludedWithApprovalsLimitedToOtherGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, optionalGroup,
                Set.of(degreeCurricularPlan.getCurricularCourseByCode("C5")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithApprovalsLimitedToOtherGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, optionalGroup,
                Set.of(degreeCurricularPlan.getCurricularCourseByCode("C5")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_CannotConcludeWithApprovalsLimitedToOtherGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, optionalGroup,
                Set.of(degreeCurricularPlan.getCurricularCourseByCode("C5")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

        enrol(curricularPlan, executionYear, "C5");
        flunk(curricularPlan, "C5");

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_NotConcludedWithApprovalsInNoCourseCurriculumGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);
        final CurriculumLine c3Approval = mandatoryCurriculumGroup.getCurriculumLines().stream()
                .filter(l -> l.getDegreeModule().getCode().equals("C3")).findFirst().get();
        c3Approval.setCurriculumGroup(
                curricularPlan.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR));

//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CannotConcludeWithApprovalsInNoCourseCurriculumGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);
        final CurriculumLine c3Approval = mandatoryCurriculumGroup.getCurriculumLines().stream()
                .filter(l -> l.getDegreeModule().getCode().equals("C3")).findFirst().get();
        c3Approval.setCurriculumGroup(
                curricularPlan.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR));

//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithApprovalsLimitedToSameGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithApprovalsLimitedToSameGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithLinesAndGroupMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear));

//        System.out.println(mandatoryGroup.getCurricularRules(executionYear).stream()
//                .map(r -> CurricularRuleLabelFormatter.getLabel(r)).collect(Collectors.joining("\n")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3", "C6");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_NotConcludedWithLinesAndGroupMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear));

//        System.out.println(mandatoryGroup.getCurricularRules(executionYear).stream()
//                .map(r -> CurricularRuleLabelFormatter.getLabel(r)).collect(Collectors.joining("\n")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithLinesAndGroupMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear));

//        System.out.println(mandatoryGroup.getCurricularRules(executionYear).stream()
//                .map(r -> CurricularRuleLabelFormatter.getLabel(r)).collect(Collectors.joining("\n")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_NotConcludedAndCannotConcludeWithLinesAndGroupMixed() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear));

//        System.out.println(mandatoryGroup.getCurricularRules(executionYear).stream()
//                .map(r -> CurricularRuleLabelFormatter.getLabel(r)).collect(Collectors.joining("\n")));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");
        flunk(curricularPlan, "C6");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_NotConcludedWhenRequiredGroupIsNotEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);

        //avoid automatic enrolment in optional group
        new EnrolmentToBeApprovedByCoordinator(optionalGroup, null, executionYear, null);

        new DegreeModuleSetApprovalRule(cycleGroup, null, executionYear, null, false, false, null,
                Set.of(mandatoryGroup, optionalGroup));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CannotConcludeWhenRequiredGroupIsNotEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new CreditsLimit(mandatoryGroup, null, executionYear, null, 18d, 18d);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new CreditsLimit(optionalGroup, null, executionYear, null, 12d, 12d);
        //avoid automatic enrolment in optional group
        new EnrolmentToBeApprovedByCoordinator(optionalGroup, null, executionYear, null);

        new DegreeModuleSetApprovalRule(cycleGroup, null, executionYear, null, false, false, null,
                Set.of(mandatoryGroup, optionalGroup));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithEnrolmentsAndDismissals() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1", "C2");
        createEquivalence(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithEnrolmentsAndDismissals() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1");
        createEquivalence(curricularPlan, executionYear, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithApprovalsInDismissalNoEnrolCourses() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1", "C2");
        createCredits(curricularPlan, executionYear, mandatoryGroup, BigDecimal.ZERO, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithApprovalsInDismissalNoEnrolCourses() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1");
        createCredits(curricularPlan, executionYear, mandatoryGroup, BigDecimal.ZERO, "C3");

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithSameCourseEnroledInBothWithOneFlunked() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C2", "C3");
        flunk(curricularPlan, "C1");

        //create enrolment for 2nd semester
        final CurricularPeriod period1Y2S = degreeCurricularPlan.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode("C1");
        final Context context = new Context(mandatoryGroup, curricularCourse, period1Y2S, executionYear, null);
        EnrolmentTest.createEnrolment(curricularPlan, executionYear.getChildInterval(period1Y2S.getChildOrder(), SEMESTER),
                context, ADMIN_USERNAME);

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ConcludedWithOptionalEnrolments() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        final CurricularPeriod period1Y1S = degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER);
        final ExecutionInterval firstSemester = executionYear.getChildInterval(1, SEMESTER);
        final OptionalCurricularCourse optionalCourse =
                createOptionalCurricularCourse("Optional 1", period1Y1S, firstSemester, mandatoryGroup);
        final Context optionalCourseContext = optionalCourse.getParentContextsSet().iterator().next();

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1", "C2");

        final CurricularCourse targetCourse = degreeCurricularPlan.getCurricularCourseByCode("C3");
        EnrolmentTest.createOptionalEnrolment(curricularPlan, firstSemester, optionalCourseContext, targetCourse, ADMIN_USERNAME);
        approveOptionalEnrolment(curricularPlan, targetCourse);

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testDegreeModuleSetApproval_CanConcludeWithOptionalEnrolments() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        final CurricularPeriod period1Y1S = degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER);
        final ExecutionInterval firstSemester = executionYear.getChildInterval(1, SEMESTER);
        final OptionalCurricularCourse optionalCurricularCourse =
                createOptionalCurricularCourse("Optional 1", period1Y1S, firstSemester, mandatoryGroup);
        final Context optionalCourseContext = optionalCurricularCourse.getParentContextsSet().iterator().next();

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2");
        approve(curricularPlan, "C1", "C2");

        final CurricularCourse targetCourse = degreeCurricularPlan.getCurricularCourseByCode("C3");
        EnrolmentTest.createOptionalEnrolment(curricularPlan, firstSemester, optionalCourseContext, targetCourse, ADMIN_USERNAME);

//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testDegreeModuleSetApproval_ShowEnrolmentWarning() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, true, false, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");
            final ExecutionInterval interval = executionYear.getChildInterval(1, SEMESTER);
            final Context context = degreeCurricularPlan.getCurricularCourseByCode("C1").getParentContextsSet().iterator().next();
            final CurriculumGroup curriculumGroup = curricularPlan.findCurriculumGroupFor(context.getParentCourseGroup());
            final DegreeModuleToEnrol degreeModuleToEnrol = new DegreeModuleToEnrol(curriculumGroup, context, interval);

            final RuleResult ruleResult = curricularPlan.enrol(interval, Set.of(degreeModuleToEnrol), List.of(),
                    CurricularRuleLevel.ENROLMENT_WITH_RULES);
            assertEquals(true, ruleResult.isWarning());
            assertEquals(true, ruleResult.getMessages().stream()
                    .anyMatch(m -> m.getMessage().equals("label.DegreeModuleSetApprovalRule.conclusion.warning")));
        } finally {
            Authenticate.unmock();
        }

    }

    @Test
    public void testDegreeModuleSetApproval_ShowEnrolmentWarningWithApprovalGroupSameAsGroupForRule() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, true, false, mandatoryGroup,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");
            final ExecutionInterval interval = executionYear.getChildInterval(1, SEMESTER);
            final Context context = degreeCurricularPlan.getCurricularCourseByCode("C1").getParentContextsSet().iterator().next();
            final CurriculumGroup curriculumGroup = curricularPlan.findCurriculumGroupFor(context.getParentCourseGroup());
            final DegreeModuleToEnrol degreeModuleToEnrol = new DegreeModuleToEnrol(curriculumGroup, context, interval);

            final RuleResult ruleResult = curricularPlan.enrol(interval, Set.of(degreeModuleToEnrol), List.of(),
                    CurricularRuleLevel.ENROLMENT_WITH_RULES);
            assertEquals(true, ruleResult.isWarning());
            assertEquals(true, ruleResult.getMessages().stream()
                    .anyMatch(m -> m.getMessage().equals("label.DegreeModuleSetApprovalRule.conclusion.warning")));
        } finally {
            Authenticate.unmock();
        }

    }

    @Test
    public void testDegreeModuleSetApproval_ShowEnrolmentWarningWithOtherApprovalGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, true, false, optionalGroup,
                optionalGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");
            final ExecutionInterval interval = executionYear.getChildInterval(1, SEMESTER);
            final Context context = degreeCurricularPlan.getCurricularCourseByCode("C1").getParentContextsSet().iterator().next();
            final CurriculumGroup curriculumGroup = curricularPlan.findCurriculumGroupFor(context.getParentCourseGroup());
            final DegreeModuleToEnrol degreeModuleToEnrol = new DegreeModuleToEnrol(curriculumGroup, context, interval);

            final RuleResult ruleResult = curricularPlan.enrol(interval, Set.of(degreeModuleToEnrol), List.of(),
                    CurricularRuleLevel.ENROLMENT_WITH_RULES);
            assertEquals(true, ruleResult.isWarning());
            assertEquals(true, ruleResult.getMessages().stream()
                    .anyMatch(m -> m.getMessage().equals("label.DegreeModuleSetApprovalRule.conclusion.warning.with.group")));
        } finally {
            Authenticate.unmock();
        }

    }

    // ----------------------------------------------
    // TODO: Move to utility classes for common usage
    // ----------------------------------------------
    private static DegreeCurricularPlan createDegreeCurricularPlan(ExecutionYear executionYear) {
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

    private static CurricularCourse createCurricularCourse(String code, String name, BigDecimal credits,
            CurricularPeriod curricularPeriod, ExecutionInterval interval, CourseGroup courseGroup) {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final CompetenceCourse competenceCourse = Optional.ofNullable(CompetenceCourse.find(code))
                .orElseGet(() -> createCompetenceCourse(name, code, credits, SEMESTER, interval, coursesUnit));

        return new CurricularCourse(credits.doubleValue(), competenceCourse, courseGroup, curricularPeriod, interval, null);
    }

    private static OptionalCurricularCourse createOptionalCurricularCourse(String name, CurricularPeriod curricularPeriod,
            ExecutionInterval interval, CourseGroup courseGroup) {
        return new OptionalCurricularCourse(courseGroup, name, name, curricularPeriod, interval, null);
    }

    private static CourseGroup getChildGroup(CourseGroup parent, String name) {
        return (CourseGroup) parent.getChildDegreeModules().stream().filter(dm -> dm.getName().equals(name)).findFirst().get();
    }

    private static Registration createRegistration(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        final Student student = User.findByUsername(STUDENT_CONCLUSION_A_USERNAME).getPerson().getStudent();
        return StudentTest.createRegistration(student, degreeCurricularPlan, executionYear);
    }

    private static void enrol(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear, String... codes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        Stream.of(codes).forEach(c -> {
            final Context context = degreeCurricularPlan.getCurricularCourseByCode(c).getParentContextsSet().iterator().next();
            final ExecutionInterval enrolmentInterval =
                    executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(), SEMESTER);
            EnrolmentTest.createEnrolment(studentCurricularPlan, enrolmentInterval, context, ADMIN_USERNAME);
        });
    }

    private static void approve(StudentCurricularPlan studentCurricularPlan, String... codes) {
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

    private static void approveOptionalEnrolment(final StudentCurricularPlan curricularPlan,
            final CurricularCourse targetCourse) {
        final OptionalEnrolment optionalEnrolment =
                (OptionalEnrolment) curricularPlan.getEnrolments(targetCourse).iterator().next();
        final EnrolmentEvaluation evaluation = optionalEnrolment.getEvaluationsSet().iterator().next();
        evaluation.setGrade(Grade.createGrade("10", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()));
        evaluation.setExamDateYearMonthDay(new YearMonthDay());
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        optionalEnrolment.setEnrollmentState(EnrollmentState.APROVED);
    }

    private static void flunk(StudentCurricularPlan studentCurricularPlan, String... codes) {
        Stream.of(codes).forEach(c -> {
            final Enrolment enrolment = studentCurricularPlan.getEnrolmentsSet().stream()
                    .filter(e -> Objects.equals(e.getCode(), c)).findFirst().get();
            final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
            evaluation.setGrade(Grade.createGrade("0", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()));
            evaluation.setExamDateYearMonthDay(new YearMonthDay());
            evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
            enrolment.setEnrollmentState(EnrollmentState.NOT_APROVED);
        });
    }

    private static void createEquivalence(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear,
            String... codes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        Stream.of(codes).forEach(c -> {
            final CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode(c);
            final Context context = curricularCourse.getParentContextsSet().iterator().next();
            final ExecutionInterval executionInterval =
                    executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(), SEMESTER);
            final SelectedCurricularCourse dismissalDTO = new SelectedCurricularCourse(curricularCourse, studentCurricularPlan);
            dismissalDTO.setCurriculumGroup(studentCurricularPlan.findCurriculumGroupFor(context.getParentCourseGroup()));

            new Equivalence(studentCurricularPlan, Set.of(dismissalDTO), Set.of(),
                    Grade.createGrade("10", GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get()), executionInterval);

        });
    }

    private static void createCredits(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear,
            CourseGroup courseGroup, BigDecimal credits, String... noEnrolCodes) {
        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        final Collection<CurricularCourse> noEnrolCourses =
                Stream.of(noEnrolCodes).map(c -> degreeCurricularPlan.getCurricularCourseByCode(c)).collect(Collectors.toSet());
        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

        new Credits(studentCurricularPlan, courseGroup, Set.of(), noEnrolCourses, credits.doubleValue(), executionInterval);
    }

}
