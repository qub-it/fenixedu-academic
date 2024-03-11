package org.fenixedu.academic.domain.curricularRules;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.ADMIN_USERNAME;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.CYCLE_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.MANDATORY_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.OPTIONAL_GROUP;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.annul;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.approve;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.approveOptionalEnrolment;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCredits;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCurricularCourse;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createEquivalence;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createOptionalCurricularCourse;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.flunk;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.getChildGroup;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroupType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import com.google.common.base.Optional;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeModuleSetApprovalRuleTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            return null;
        });
    }

    @Test
    public void concluded_withCurriculumLinesOnly() {
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

//        System.out.println("testConcludedWithCurriculumLinesOnly");
//        System.out.println(curricularPlan.getRoot().print("\t"));

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void notConcluded_withCurriculumLinesOnly() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withCurriculumLinesOnly() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void cannotConclude_withSomeEnrolmentsApprovedAndOthersFlunked() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void concluded_withApprovalsLimitedToOtherGroup() {
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
        approve(curricularPlan, "C1", "C2", "C3", "C5");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void notConcluded_withApprovalsLimitedToOtherGroup() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withApprovalsLimitedToOtherGroup() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void cannotConclude_withApprovalsLimitedToOtherGroup() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));

        enrol(curricularPlan, executionYear, "C5");
        flunk(curricularPlan, "C5");

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void notConcluded_withApprovalsInNoCourseCurriculumGroups() {
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

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void cannotConclude_withApprovalsInNoCourseCurriculumGroups() {
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
        approve(curricularPlan, "C1", "C2", "C3");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);
        final CurriculumLine c3Approval = mandatoryCurriculumGroup.getCurriculumLines().stream()
                .filter(l -> l.getDegreeModule().getCode().equals("C3")).findFirst().get();
        c3Approval.setCurriculumGroup(
                curricularPlan.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR));

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void concluded_withApprovalsLimitedToSameGroup() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withApprovalsLimitedToSameGroup() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void concluded_withLinesAndGroupMixed() {
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

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3", "C6");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void notConcluded_withLinesAndGroupMixed() {
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

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withLinesAndGroupMixed() {
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

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void notConcludedAndCannotConclude_withLinesAndGroupMixed() {
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

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C6");
        approve(curricularPlan, "C1", "C2", "C3");
        flunk(curricularPlan, "C6");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void notConcluded_whenRequiredGroupIsNotEnroled() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.isConcluded());
    }

    @Test
    public void cannotConclude_whenRequiredGroupIsNotEnroled() {
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

        final CurriculumGroup cycleCurriculumGroup = curricularPlan.findCurriculumGroupFor(cycleGroup);

        assertEquals(false, cycleCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void concluded_withEnrolmentsAndDismissals() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withEnrolmentsAndDismissals() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void concluded_withApprovalsInDismissalNoEnrolCourses() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withApprovalsInDismissalNoEnrolCourses() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void canConclude_withSameCourseEnroledInBothWithOneFlunked() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void concluded_withOptionalEnrolments() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void canConclude_withOptionalEnrolments() {
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

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void showEnrolmentWarning() {
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
    public void showEnrolmentWarning_withApprovalGroupSameAsGroupForRule() {
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
    public void showEnrolmentWarning_withOtherApprovalGroup() {
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

    @Test
    public void concluded_whenValidateEnroledOnlyAndIsNotEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void notConcluded_whenValidateEnroledOnlyAndIsEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void notConcluded_whenValidateEnroledOnlyAndWasPreviouslyEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        final ExecutionYear nextYear = ExecutionYear.readExecutionYearByName("2021/2022");

        assertEquals(false, mandatoryCurriculumGroup.isConcluded(nextYear).value());
    }

    @Test
    public void cannotConclude_whenValidateEnroledOnlyAndWasPreviouslyEnroledAndIsNotEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        final ExecutionYear nextYear = ExecutionYear.readExecutionYearByName("2021/2022");

        assertEquals(false, mandatoryCurriculumGroup.canConclude(nextYear));
    }

    @Test
    public void canConclude_whenValidateEnroledOnlyAndWasPreviouslyEnroledAndIsEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final ExecutionYear nextYear = ExecutionYear.readExecutionYearByName("2021/2022");
        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        flunk(curricularPlan, "C1");
        enrol(curricularPlan, nextYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(nextYear));
    }

    @Test
    public void canConclude_whenValidateEnroledOnlyAndWasPreviouslyEnroledAndIsFlunked() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final ExecutionYear nextYear = ExecutionYear.readExecutionYearByName("2021/2022");
        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        flunk(curricularPlan, "C1");
        enrol(curricularPlan, nextYear, "C1");
        flunk(curricularPlan, nextYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(nextYear));
    }

    @Test
    public void canConclude_whenValidateEnroledOnlyAndIsEnroled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void cannotConclude_whenValidateEnroledOnlyAndEnrolmentIsFlunked() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        flunk(curricularPlan, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void notConcluded_whenValidateEnroledOnlyAndEnrolmentIsFlunked() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        flunk(curricularPlan, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void notConcluded_whenValidateEnroledOnlyAndEnrolmentIsAnnulled() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        annul(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void cannotConclude_whenValidateEnroledOnlyAndEnrolmentIsExtra() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);
        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        final NoCourseGroupCurriculumGroup extraCurriculumGroup = Optional
                .of(curricularPlan.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR)).get();
        curricularPlan.getEnrolmentsSet().stream().filter(e -> e.getCode().equals("C1")).findAny().get()
                .setCurriculumGroup(extraCurriculumGroup);

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.canConclude(executionYear));
    }

    @Test
    public void notConcluded_whenValidateEnroledOnlyAndEnroledInGroup() {
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

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, mandatoryGroup,
                Set.of(mandatoryChildGroup));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(false, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void concluded_whenValidateEnroledOnlyAndNotEnroledInGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);

        final CourseGroup mandatoryChildGroup =
                new CourseGroup(mandatoryGroup, "Mandatory Child", "Mandatory Child", executionYear, null, null);
        //avoid automatic enrolment
        new EnrolmentToBeApprovedByCoordinator(mandatoryChildGroup, null, executionYear, null);
        new CreditsLimit(mandatoryChildGroup, null, executionYear, null, 6d, 6d);
        createCurricularCourse("C6", "Course 6", new BigDecimal("6.0"),
                degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER), executionYear.getFirstExecutionPeriod(),
                mandatoryChildGroup);

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, mandatoryGroup,
                Set.of(mandatoryChildGroup));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void concluded_whenValidateEnroledOnlyAndNotEnroledOptional() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, MANDATORY_GROUP);

        new DegreeModuleSetApprovalRule(mandatoryGroup, null, executionYear, null, false, true, null,
                mandatoryGroup.getChildDegreeModulesValidOnExecutionAggregation(executionYear).stream().filter(dm -> dm.isLeaf())
                        .collect(Collectors.toSet()));

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3");
        approve(curricularPlan, "C1", "C2", "C3");

        final CurriculumGroup mandatoryCurriculumGroup = curricularPlan.findCurriculumGroupFor(mandatoryGroup);

        assertEquals(true, mandatoryCurriculumGroup.isConcluded());
    }

    @Test
    public void concluded_whenValidateEnroledOnlyAndCourseInEnroledInAnotherGroup() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP);

        final CourseGroup optionalGroup = getChildGroup(cycleGroup, OPTIONAL_GROUP);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();

        enrol(curricularPlan, executionYear, "C1");

        final CurricularPeriod period1Y2S = degreeCurricularPlan.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode("C1");
        new Context(optionalGroup, curricularCourse, period1Y2S, executionYear, null);

        new DegreeModuleSetApprovalRule(optionalGroup, null, executionYear, null, false, true, optionalGroup,
                Set.of(curricularCourse));

        final CurriculumGroup optionalCurriculumGroup = curricularPlan.findCurriculumGroupFor(optionalGroup);

        assertEquals(true, optionalCurriculumGroup.isConcluded());
    }

}
