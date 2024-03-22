package org.fenixedu.academic.domain.curricularRules;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.CYCLE_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.MANDATORY_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.OPTIONAL_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.approve;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCurricularCourse;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.flunk;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.getChildGroup;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeModulesSelectionLimitTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            return null;
        });
    }

    //Beware that no course groups always report concluded so they are accounted as concluded modules if we apply rule to root
    @Test
    public void testRootConclusionWithNoCourseGroupsOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new DegreeModulesSelectionLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");

//        System.out.println("testRootConclusionWithNoCourseGroupsOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        assertEquals(true, curricularPlan.getRoot().isConcluded());
    }

    @Test
    public void testRootCanConcludeWithNoCourseGroupsOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        new DegreeModulesSelectionLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        assertEquals(true, curricularPlan.getRoot().isConcluded());
        assertEquals(true, curricularPlan.getRoot().canConclude(executionYear));
    }

    @Test
    public void testConclusionWithoutRequiredModules() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testCannotCanConcludeWithoutRequiredModules() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);
        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);
        final CurriculumGroup optioanlCurriculumGroup = curricularPlan.findCurriculumGroupFor(optionalGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
        assertEquals(false, optioanlCurriculumGroup.canConclude(executionYear));
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testConclusionWithRequiredModules() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testCanConcludeWithRequiredModules() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));

        flunk(curricularPlan, "C5");

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testConclusionWithRequiredModulesMixedWithCreditsLimitInChildren() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void testCanConcludeWithRequiredModulesMixedWithCreditsLimitInChildren() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));

        flunk(curricularPlan, "C5");

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    //behaviour difference regarding CreditsLimit since DegreeModulesLimit checks for value not isValid
    //UNKNOWN (group without rules) is false althought isValid returns true
    @Test
    public void testConclusionWithoutRulesOnChildGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
    }

    //behaviour difference regarding CreditsLimit since DegreeModulesLimit checks for value not isValid
    //UNKNOWN (group without rules) is false althought isValid returns true
    @Test
    public void testCannotConcludeWithoutRulesOnChildGroups() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);
        new DegreeModulesSelectionLimit(cycleGroup, null, executionYear, null, 2, 2);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C4", "C5");
        approve(curricularPlan, "C1", "C2", "C3", "C4", "C5");

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));

        degreeModulesRule.setMinimumLimit(2);
        degreeModulesRule.setMaximumLimit(2);

        assertEquals(true, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testConclusionWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());

        approve(curricularPlan, "C3");

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testCanConcludeWithCurriculumLinesOnly() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 3, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void testConclusionWithCurriculumLinesAndGroupsMixed() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());

        approve(curricularPlan, "C6");

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void testCanConcludeWithCurriculumLinesAndGroupsMixed() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

        enrol(curricularPlan, executionYear, "C6");

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));

    }

    @Test
    public void testCanConcludeWhenMinModulesIsZero() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 0, 4);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

    }

    @Test
    public void testCanConcludeWhenMinModulesIsZeroAndMaxIsFull() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 0, 3);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));

    }

    @Test
    public void testCanConcludeWhenMinModulesIsGreatherThanZero() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModulesSelectionLimit(mandatoryGroup, null, executionYear, null, 2, 4);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));

    }

}
