package org.fenixedu.academic.domain.student;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.GRADE_SCALE_NUMERIC;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.GRADE_SCALE_QUALITATIVE;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.approve;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.getChildGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusionConfig;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.curriculum.ProgramConclusionProcess;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.academic.service.services.administrativeOffice.student.RegistrationConclusionProcess;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationConclusionTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private ExecutionYear executionYear;
    private Registration registration;
    private StudentCurricularPlan curricularPlan;
    private ProgramConclusion finalConclusion;
    private ProgramConclusion partialConclusion;
    private DegreeCurricularPlan degreeCurricularPlan;
    private Set<Registration> skipConclusionValidation = new HashSet<>();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            new ProgramConclusion("FINAL", new LocalizedString(), new LocalizedString(), new LocalizedString(),
                    new LocalizedString(), true, false, false,
                    RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).get());
            new ProgramConclusion("PARTIAL", new LocalizedString(), new LocalizedString(), new LocalizedString(),
                    new LocalizedString(), true, false, false, null);
            return null;
        });
    }

    @Before
    public void setup() {

        //NEW
        //        Registration.setConclusionProcessEnabler((bean) -> skipConclusionValidation.contains(bean.getRegistration()));

        //OLD
        CurriculumGroup.setConclusionProcessEnabler(() -> new CurriculumGroup.ConclusionProcessEnabler() {
            @Override
            public boolean isAllowed(final CurriculumGroup curriculumGroup) {
                return curriculumGroup.isConcluded() || skipConclusionValidation.contains(curriculumGroup.getRegistration());
            }
        });

        FenixFramework.getTransactionManager().withTransaction(() -> {
            this.finalConclusion = ProgramConclusion.findByCode("FINAL").get();
            this.partialConclusion = ProgramConclusion.findByCode("PARTIAL").get();

            this.executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
            this.degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
            degreeCurricularPlan.getDegree().setNumericGradeScale(GradeScale.getGradeScaleByCode(GRADE_SCALE_NUMERIC));
            degreeCurricularPlan.getDegree().setQualitativeGradeScale(GradeScale.getGradeScaleByCode(GRADE_SCALE_QUALITATIVE));

            new CreditsLimit(degreeCurricularPlan.getRoot(), null, executionYear, null, 12.0, 12.0);
            degreeCurricularPlan.getRoot().setProgramConclusion(finalConclusion);
            ProgramConclusionConfig.create(new LocalizedString(), degreeCurricularPlan, finalConclusion)
                    .addIncludedModules(degreeCurricularPlan.getRoot());

            final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
            new CreditsLimit(cycleGroup, null, executionYear, null, 12.0, 12.0);
            cycleGroup.setProgramConclusion(partialConclusion);
            ProgramConclusionConfig.create(new LocalizedString(), degreeCurricularPlan, partialConclusion)
                    .addIncludedModules(cycleGroup);

            this.registration = createRegistration(degreeCurricularPlan, executionYear);
            this.curricularPlan = this.registration.getLastStudentCurricularPlan();

            enrol(curricularPlan, executionYear, "C1");
            approve(curricularPlan, "C1");
            enrol(curricularPlan, executionYear, "C2");
            approve(curricularPlan, "C2");

            return null;
        });
    }

    @Test
    public void givenProgramConclusionWithTargetState_whenConclusionIsProcessed_thenRegistrationShouldHaveTargetStateAndLastApprovalPeriod() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, finalConclusion);
        RegistrationConclusionProcess.run(conclusionBean);
        final RegistrationStateType targetStateType =
                RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).get();
        final RegistrationState activeState = registration.getActiveState();

        assertTrue(conclusionBean.isConclusionProcessed());
        assertEquals(activeState.getType(), targetStateType);
        assertEquals(activeState.getExecutionYear(), conclusionBean.getConclusionYear());
        assertEquals(activeState.getExecutionInterval(), conclusionBean.getConclusionExecutionInterval());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, finalConclusion, conclusionBean));
    }

    @Test
    public void givenProgramConclusionWithTargetState_whenRegistrationIsAlreadyInConcludedState_thenShouldRemainInSameState() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, finalConclusion);
        RegistrationConclusionProcess.run(conclusionBean);
        final RegistrationStateType targetStateType =
                RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).get();

        final RegistrationState activeState = registration.getActiveState();
        assertTrue(conclusionBean.isConclusionProcessed());
        assertEquals(activeState.getType(), targetStateType);
        assertEquals(activeState.getExecutionYear(), conclusionBean.getConclusionYear());
        assertEquals(activeState.getExecutionInterval(), conclusionBean.getConclusionExecutionInterval());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, finalConclusion, conclusionBean));

        RegistrationConclusionProcess.run(conclusionBean);
        final RegistrationState activeStateAfter = registration.getActiveState();
        assertTrue(conclusionBean.isConclusionProcessed());
        assertEquals(activeStateAfter.getType(), targetStateType);
        assertEquals(activeStateAfter.getExecutionYear(), conclusionBean.getConclusionYear());
        assertEquals(activeStateAfter.getExecutionInterval(), conclusionBean.getConclusionExecutionInterval());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, finalConclusion, conclusionBean));

    }

    @Test
    public void givenProgramConclusionWithoutTargetState_whenConclusionIsProcessed_thenRegistrationRemainInSameState() {
        final RegistrationState initialState = registration.getActiveState();
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        RegistrationConclusionProcess.run(conclusionBean);
        final RegistrationState activeState = registration.getActiveState();

        assertTrue(conclusionBean.isConclusionProcessed());
        assertEquals(activeState, initialState);
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
    }

    @Test
    public void givenConfigurationWithNumericGrade_whenEnteredGradeIsInvalid_thenThrowDomainException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.RegistrationConclusionProcess.final.average.is.invalid");

        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("INVALID");
        RegistrationConclusionProcess.run(conclusionBean);
    }

    //NEW
    //    @Test
    //    public void givenConfigurationWithNumericGrade_whenEnteredGradeIsInvalid_thenThrowDomainException() {
    //        exceptionRule.expect(DomainException.class);
    //        exceptionRule.expectMessage("error.grade.invalid.grade");
    //
    //        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
    //        conclusionBean.setEnteredAverageGrade("INVALID");
    //        RegistrationConclusionProcess.run(conclusionBean);
    //    }

    @Test
    public void givenConfigurationWithNumericGrade_whenEnteredGradeIsValid_thenSuccess() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        conclusionBean.setEnteredDescriptiveGrade("AP");
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
    }

    @Test
    public void givenRegistrationWithStartDate_whenEnteredConclusionDateIsBeforeStartDate_thenThrowDomainException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.RegistrationConclusionProcess.start.date.is.after.entered.date");

        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        conclusionBean.setEnteredConclusionDate(conclusionBean.getRegistration().getStartDate().minusDays(1).toLocalDate());
        RegistrationConclusionProcess.run(conclusionBean);
    }

    //NEW
    //    @Test
    //    public void givenRegistrationWithStartDate_whenEnteredConclusionDateIsBeforeStartDate_thenThrowDomainException() {
    //        exceptionRule.expect(DomainException.class);
    //        exceptionRule.expectMessage("error.ConclusionProcessVersion.start.date.is.after.conclusion.date");
    //
    //        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
    //        conclusionBean.setEnteredAverageGrade("10");
    //        conclusionBean.setEnteredConclusionDate(conclusionBean.getRegistration().getStartDate().minusDays(1).toLocalDate());
    //        RegistrationConclusionProcess.run(conclusionBean);
    //    }

    @Test
    public void givenRegistrationWithStartDate_whenEnteredConclusionDateIsAfterOrEqualsStartDate_thenSuccess() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        conclusionBean.setEnteredConclusionDate(conclusionBean.getRegistration().getStartDate().toLocalDate());
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
    }

    //OLD
    @Test
    public void givenRegistrationWithoutCurriculumGroupForProgramConclusion_whenConcludeIsExecuted_thenThrowDomainException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Registration.invalid.cycleCurriculumGroup");

        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, ConclusionRulesTestUtil.MANDATORY_GROUP);
        cycleGroup.setProgramConclusion(null);
        mandatoryGroup.setProgramConclusion(this.partialConclusion);

        final Registration newRegistration = createRegistration(degreeCurricularPlan, executionYear);
        newRegistration.getLastStudentCurricularPlan().getAllCurriculumGroups().stream()
                .sorted(Comparator.comparing(CurriculumGroup::getFullPath).reversed())
                .filter(cg -> !cg.isRoot() && !cg.isCycleCurriculumGroup()).forEach(cg -> cg.delete());

        newRegistration.conclude(null);
    }

    //OLD
    @Test
    public void givenRegistrationWithCurriculumGroupForProgramConclusion_whenConcludeIsExecutedWithInvalidCurriculumGroup_thenThrowDomainException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Registration.invalid.cycleCurriculumGroup");

        final CourseGroup cycleGroup = getChildGroup(degreeCurricularPlan.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        final CourseGroup mandatoryGroup = getChildGroup(cycleGroup, ConclusionRulesTestUtil.MANDATORY_GROUP);
        cycleGroup.setProgramConclusion(null);
        mandatoryGroup.setProgramConclusion(this.partialConclusion);

        final Registration newRegistration = createRegistration(degreeCurricularPlan, executionYear);
        newRegistration.getLastStudentCurricularPlan().getAllCurriculumGroups().stream()
                .sorted(Comparator.comparing(CurriculumGroup::getFullPath).reversed())
                .filter(cg -> !cg.isRoot() && !cg.isCycleCurriculumGroup()).forEach(cg -> cg.delete());

        newRegistration.conclude(registration.getLastStudentCurricularPlan().findCurriculumGroupFor(cycleGroup));
    }

    @Test
    public void givenRegistrationWithConclusionProcess_whenConcludeIsExecuted_thenShouldUpdateExistingProcess() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

        conclusionBean.setEnteredAverageGrade("12");
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
    }

    @Test
    public void givenRegistrationWithConclusionProcess_whenConcludeWithUpdatedConclusionYear_thenShouldUpdateConclusionYear() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);

        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

        final Enrolment c2Enrolment = curricularPlan.getEnrolmentStream().filter(e -> e.getCode().equals("C2")).findAny().get();
        final EnrolmentEvaluation c2Evaluation = c2Enrolment.getFinalEnrolmentEvaluation();
        c2Evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
        c2Evaluation.setGrade(createNumericGrade("0"));
        c2Evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        c2Enrolment.setEnrollmentState(EnrollmentState.NOT_APROVED);

        enrol(curricularPlan, (ExecutionYear) executionYear.getNext(), "C2");
        approve(curricularPlan, (ExecutionYear) executionYear.getNext(), "C2");

        final RegistrationConclusionBean newConclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        RegistrationConclusionProcess.run(newConclusionBean);
        assertTrue(newConclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, newConclusionBean));
    }

    @Test
    public void givenRegistrationNotConcluded_whenConcludeIsExecuted_thenThrowDomainException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CycleCurriculumGroup.cycle.is.not.concluded");

        final Registration newRegistration = createRegistration(degreeCurricularPlan, executionYear);
        enrol(newRegistration.getLastStudentCurricularPlan(), executionYear, "C1");
        approve(newRegistration.getLastStudentCurricularPlan(), "C1");

        final RegistrationConclusionBean conclusionBean =
                createConclusionBean(newRegistration.getLastStudentCurricularPlan(), partialConclusion);
        RegistrationConclusionProcess.run(conclusionBean);
    }

    @Test
    public void givenRegistrationWithConclusionProcess_whenConcludeAfterEnrolmentIsChangedToFlunked_thenShouldAllowConclusion() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

        final Enrolment c2Enrolment = curricularPlan.getEnrolmentStream().filter(e -> e.getCode().equals("C2")).findAny().get();
        final EnrolmentEvaluation c2Evaluation = c2Enrolment.getFinalEnrolmentEvaluation();
        c2Evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
        c2Evaluation.setGrade(createNumericGrade("0"));
        c2Evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        c2Enrolment.setEnrollmentState(EnrollmentState.NOT_APROVED);

        //isConcluded should return true when conclusion is already processed, regardless of the current state of the enrolments,
        //to allow processing the conclusion and updating the conclusion data with the new enrolment state or migrated conclusion processes
        //with enrolments that are not in approved state but the registration is concluded.
        assertTrue(conclusionBean.isConcluded());

        RegistrationConclusionProcess.run(conclusionBean);

        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

    }

    @Test
    public void givenRegistrationNotConcluded_whenRegisteredToSkipConclusionValidation_thenShouldBeAbleToConclude() {
        final Registration registration = createRegistration(this.degreeCurricularPlan, executionYear);
        skipConclusionValidation.add(registration);
        final StudentCurricularPlan curricularPlan = registration.getLastStudentCurricularPlan();

        enrol(curricularPlan, executionYear, "C1");
        approve(curricularPlan, executionYear, "C1");

        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConcluded());
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
    }

    //NEW
//    @Test
//    public void givenRegistrationWithConclusionOnOnePlan_whenConcludeWithSameConclusionTypeInAnotherPlan_thenThrowDomainException() {
//        final Registration registration = createRegistration(this.degreeCurricularPlan, executionYear);
//        skipConclusionValidation.add(registration);
//
//        final StudentCurricularPlan curricularPlan = registration.getLastStudentCurricularPlan();
//        enrol(curricularPlan, executionYear, "C1");
//        approve(curricularPlan, executionYear, "C1");
//
//        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
//        conclusionBean.setEnteredAverageGrade("10");
//        RegistrationConclusionProcess.run(conclusionBean);
//        assertTrue(conclusionBean.isConcluded());
//        assertTrue(conclusionBean.isConclusionProcessed());
//        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
//
//        final RegistrationConclusionBean finalConclusionBean = createConclusionBean(curricularPlan, finalConclusion);
//        finalConclusionBean.setEnteredAverageGrade("12");
//        RegistrationConclusionProcess.run(finalConclusionBean);
//        assertTrue(finalConclusionBean.isConcluded());
//        assertTrue(finalConclusionBean.isConclusionProcessed());
//        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, finalConclusion, finalConclusionBean));
//
//        final DegreeCurricularPlan newDegreeCurricularPlan =
//                createDegreeCurricularPlan(this.degreeCurricularPlan.getDegree(), "Plan 2", executionYear.getExecutionYear());
//        final CourseGroup cycleGroup = getChildGroup(newDegreeCurricularPlan.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
//        new CreditsLimit(cycleGroup, null, executionYear.getNextExecutionYear(), null, 12.0, 12.0);
//        cycleGroup.setProgramConclusion(partialConclusion);
//        ProgramConclusionConfig.create(new LocalizedString(), newDegreeCurricularPlan, partialConclusion)
//                .addIncludedModules(cycleGroup);
//
//        final StudentCurricularPlan newStudentCurricularPlan =
//                registration.createStudentCurricularPlan(newDegreeCurricularPlan, executionYear.getNextExecutionYear());
//        enrol(newStudentCurricularPlan, executionYear.getNextExecutionYear(), "C2");
//        approve(newStudentCurricularPlan, executionYear.getNextExecutionYear(), "C2");
//
//        exceptionRule.expect(DomainException.class);
//        exceptionRule.expectMessage("error.ProgramConclusionProcess.already.exists.in.registration.for.same.conclusion.type");
//
//        final RegistrationConclusionBean newConclusionBean = createConclusionBean(newStudentCurricularPlan, partialConclusion);
//        newConclusionBean.setEnteredAverageGrade("12");
//        RegistrationConclusionProcess.run(newConclusionBean);
//    }

    @Test
    public void givenRegistrationWithConclusionOnOnePlan_whenConcludeWithSameConclusionTypeInAnotherPlanAfterRevertingPreviousConclusion_thenSuccess() {
        final Registration registration = createRegistration(this.degreeCurricularPlan, executionYear);
        skipConclusionValidation.add(registration);

        final StudentCurricularPlan curricularPlan = registration.getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1");
        approve(curricularPlan, executionYear, "C1");

        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConcluded());
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

        final DegreeCurricularPlan newDegreeCurricularPlan =
                createDegreeCurricularPlan(this.degreeCurricularPlan.getDegree(), "Plan 2", executionYear.getExecutionYear());
        final CourseGroup cycleGroup = getChildGroup(newDegreeCurricularPlan.getRoot(), ConclusionRulesTestUtil.CYCLE_GROUP);
        new CreditsLimit(cycleGroup, null, executionYear.getNextExecutionYear(), null, 12.0, 12.0);
        cycleGroup.setProgramConclusion(partialConclusion);
        ProgramConclusionConfig.create(new LocalizedString(), newDegreeCurricularPlan, partialConclusion)
                .addIncludedModules(cycleGroup);

        final StudentCurricularPlan newStudentCurricularPlan =
                registration.createStudentCurricularPlan(newDegreeCurricularPlan, executionYear.getNextExecutionYear());
        enrol(newStudentCurricularPlan, executionYear.getNextExecutionYear(), "C2");
        approve(newStudentCurricularPlan, executionYear.getNextExecutionYear(), "C2");

        RegistrationConclusionProcess.revert(conclusionBean);
        if (conclusionBean.getConclusionProcess() != null) {
            //TODO: remove after refactoring since old conclusion code always creates two versions of conclusion process
            RegistrationConclusionProcess.revert(conclusionBean);
        }

        final RegistrationConclusionBean newConclusionBean = createConclusionBean(newStudentCurricularPlan, partialConclusion);
        newConclusionBean.setEnteredAverageGrade("12");
        RegistrationConclusionProcess.run(newConclusionBean);
        assertTrue(newConclusionBean.isConcluded());
        assertTrue(newConclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(newStudentCurricularPlan, partialConclusion, newConclusionBean));
    }

    @Test
    public void givenRegistrationConclusionWithNumber_whenRevertedAndConcludedAgain_thenNumberShouldRemainUnchanged() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);
        conclusionBean.getConclusionProcess().setNumber("12345");

        assertTrue(conclusionBean.isConcluded());
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

        RegistrationConclusionProcess.revert(conclusionBean);
        if (conclusionBean.getConclusionProcess() != null) {
            //TODO: remove after refactoring since old conclusion code always creates two versions of conclusion process
            RegistrationConclusionProcess.revert(conclusionBean);
        }

        assertNull(conclusionBean.getConclusionProcess());
        assertFalse(conclusionBean.isConclusionProcessed());

        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConcluded());
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
        assertEquals(conclusionBean.getConclusionProcess().getNumber(), "12345");
    }

    @Test
    public void givenRegistrationConclusionWithNumber_whenRevertedAndConcludedAgain_thenShouldActivatePreviousConclusionProcess() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        RegistrationConclusionProcess.run(conclusionBean);
        final ConclusionProcess conclusionProcess = conclusionBean.getConclusionProcess();

        assertTrue(conclusionBean.isConcluded());
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));

        RegistrationConclusionProcess.revert(conclusionBean);
        if (conclusionBean.getConclusionProcess() != null) {
            //TODO: remove after refactoring since old conclusion code always creates two versions of conclusion process
            RegistrationConclusionProcess.revert(conclusionBean);
        }

        assertNull(conclusionBean.getConclusionProcess());
        assertFalse(conclusionBean.isConclusionProcessed());

        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConcluded());
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
        assertEquals(conclusionProcess, conclusionBean.getConclusionProcess());

    }

    //NEW
//    @Test
//    public void givenConclusionProcess_whenCreatingConclusionProcessForSamePlanAndConclusionType_thenThrowDomainException() {
//        exceptionRule.expect(DomainException.class);
//        exceptionRule.expectMessage("error.ProgramConclusionProcess.already.exists.in.curricular.plan.for.same.conclusion.type");
//
//        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
//        conclusionBean.setEnteredAverageGrade("10");
//        RegistrationConclusionProcess.run(conclusionBean);
//        assertTrue(conclusionBean.isConclusionProcessed());
//        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
//
//        final RegistrationConclusionBean finalConclusionBean = createConclusionBean(curricularPlan, finalConclusion);
//        finalConclusionBean.setEnteredAverageGrade("12");
//        RegistrationConclusionProcess.run(finalConclusionBean);
//        assertTrue(finalConclusionBean.isConclusionProcessed());
//        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, finalConclusion, finalConclusionBean));
//
//        new ProgramConclusionProcess(conclusionBean);
//    }

    private RegistrationConclusionBean createConclusionBean(StudentCurricularPlan curricularPlan,
            ProgramConclusion programConclusion) {
        final RegistrationConclusionBean result = new RegistrationConclusionBean(curricularPlan, programConclusion);
        result.setObservations("Test conclusion");

        return result;
    }

    private boolean conclusionProcessDataMatchesConclusionBean(StudentCurricularPlan studentCurricularPlan,
            ProgramConclusion programConclusion, RegistrationConclusionBean input) {
        final ConclusionProcess conclusionProcess =
                new RegistrationConclusionBean(studentCurricularPlan, programConclusion).getConclusionProcess();

        if (input.hasEnteredConclusionDate()) {
            if (!Objects.equals(conclusionProcess.getConclusionDate(), input.getEnteredConclusionDate())) {
                System.out.println("Conclusion date doesn't match. Expected: " + input.getEnteredConclusionDate() + ", Actual: "
                        + conclusionProcess.getConclusionDate());
                return false;
            }
        } else {
            if (!Objects.equals(conclusionProcess.getConclusionDate(), input.getConclusionDate())) {
                System.out.println("Conclusion date doesn't match. Expected: " + input.getConclusionDate() + ", Actual: "
                        + conclusionProcess.getConclusionDate());
                return false;
            }
        }

        if (input.hasEnteredAverageGrade()) {
            if (!gradesEqual(conclusionProcess.getRawGrade(), createNumericGrade(input.getEnteredAverageGrade()))) {
                System.out.println("Average grade doesn't match. Expected: " + input.getEnteredAverageGrade() + ", Actual: "
                        + conclusionProcess.getRawGrade());
                return false;
            }
        } else {
            if (!gradesEqual(conclusionProcess.getRawGrade(), input.getRawGrade())) {
                System.out.println("Average grade doesn't match. Expected: " + input.getRawGrade() + ", Actual: "
                        + conclusionProcess.getRawGrade());
                return false;
            }
        }

        if (input.hasEnteredFinalAverageGrade()) {
            if (!gradesEqual(conclusionProcess.getFinalGrade(), createNumericGrade(input.getEnteredFinalAverageGrade()))) {
                System.out.println(
                        "Final average grade doesn't match. Expected: " + input.getEnteredFinalAverageGrade() + ", Actual: "
                                + conclusionProcess.getFinalGrade());
                return false;
            }
        } else {
            if (!gradesEqual(conclusionProcess.getFinalGrade(), input.getFinalGrade())) {
                System.out.println("Final average grade doesn't match. Expected: " + input.getFinalGrade() + ", Actual: "
                        + conclusionProcess.getFinalGrade());
                return false;
            }
        }

        if (input.hasEnteredDescriptiveGrade()) {
            if (!gradesEqual(conclusionProcess.getDescriptiveGrade(),
                    createQualitativeGrade(input.getEnteredDescriptiveGrade()))) {
                System.out.println(
                        "Descriptive grade doesn't match. Expected: " + input.getEnteredDescriptiveGrade() + ", Actual: "
                                + conclusionProcess.getDescriptiveGrade());
                return false;
            }
        } else {
            if (!gradesEqual(conclusionProcess.getDescriptiveGrade(), input.getDescriptiveGrade())) {
                System.out.println("Descriptive grade doesn't match. Expected: " + input.getDescriptiveGrade() + ", Actual: "
                        + conclusionProcess.getDescriptiveGrade());
                return false;
            }
        }

        if (conclusionProcess.getConclusionYear() != input.getConclusionYear()) {
            System.out.println(
                    "Conclusion year doesn't match. Expected: " + input.getConclusionYear().getQualifiedName() + ", Actual: "
                            + conclusionProcess.getConclusionYear().getQualifiedName());
            return false;
        }

        if (conclusionProcess.getGroup() != input.getCurriculumGroup()) {
            System.out.println("Curriculum group doesn't match. Expected: " + input.getCurriculumGroup().getName() + ", Actual: "
                    + conclusionProcess.getGroup().getName());
            return false;
        }

        if (conclusionProcess.getStudentCurricularPlan() != input.getStudentCurricularPlan()) {
            System.out.println(
                    "Student curricular plan doesn't match. Expected: " + input.getStudentCurricularPlan().getExternalId()
                            + ", Actual: " + conclusionProcess.getStudentCurricularPlan().getExternalId());
            return false;
        }

        if (conclusionProcess.getProgramConclusionConfig() != input.getProgramConclusionConfig()) {
            System.out.println(
                    "Program conclusion config doesn't match. Expected: " + input.getProgramConclusionConfig().getExternalId()
                            + ", Actual: " + conclusionProcess.getProgramConclusionConfig().getExternalId());
            return false;
        }

        if (conclusionProcess.getCredits().doubleValue() != input.getEctsCredits()) {
            System.out.println(
                    "Credits don't match. Expected: " + input.getEctsCredits() + ", Actual: " + conclusionProcess.getCredits());
            return false;
        }

        if (!Objects.equals(conclusionProcess.getLastVersion().getCurriculum().toString(),
                input.getCurriculumForConclusion().toString())) {
            System.out.println("Curriculums don't match. Expected: " + input.getCurriculumForConclusion() + ", \nActual: "
                    + conclusionProcess.getLastVersion().getCreationDateTime().toString() + " \n"
                    + conclusionProcess.getLastVersion().getCurriculum());

            return false;
        }

        if (conclusionProcess.getIngressionYear() != input.getIngressionYear()) {
            System.out.println(
                    "Ingression year doesn't match. Expected: " + input.getIngressionYear().getQualifiedName() + ", Actual: "
                            + conclusionProcess.getIngressionYear().getQualifiedName());
            return false;
        }

        if (conclusionProcess.getLastVersion().getResponsible() != input.getConclusionProcessResponsible()) {
            System.out.println(
                    "Responsible doesn't match. Expected: " + input.getConclusionProcessResponsible().getName() + ", Actual: "
                            + conclusionProcess.getLastVersion().getResponsible().getName());
            return false;
        }

        //TODO: observations are only written when some of the entered values is filled
        if (input.hasEnteredConclusionDate() || input.hasEnteredAverageGrade() || input.hasEnteredAverageGrade()
                || input.hasEnteredDescriptiveGrade()) {
            if (!Objects.equals(conclusionProcess.getNotes(), input.getObservations())) {
                System.out.println("Observations don't match. Expected: " + input.getObservations() + ", Actual: "
                        + conclusionProcess.getNotes());
                return false;
            }
        }

        return true;
    }

    private static Grade createNumericGrade(String value) {
        return Grade.createGrade(value, GradeScale.findUniqueByCode(GRADE_SCALE_NUMERIC).get());
    }

    private static Grade createQualitativeGrade(String value) {
        return StringUtils.isBlank(value) ? Grade.createEmptyGrade() : Grade.createGrade(value,
                GradeScale.findUniqueByCode(GRADE_SCALE_QUALITATIVE).get());
    }

    private boolean gradesEqual(Grade grade1, Grade grade2) {
        if (grade1 == null && grade2 == null) {
            return true;
        }

        if (grade1 == null || grade2 == null) {
            return false;
        }

        return grade1.compareTo(grade2) == 0;
    }

}
