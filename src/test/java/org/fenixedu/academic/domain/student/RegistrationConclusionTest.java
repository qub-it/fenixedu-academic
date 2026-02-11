package org.fenixedu.academic.domain.student;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.GRADE_SCALE_NUMERIC;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.GRADE_SCALE_QUALITATIVE;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.approve;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.getChildGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusionConfig;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.academic.service.services.administrativeOffice.student.RegistrationConclusionProcess;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

//TODO: how to test accumulated registrations bypassing conclusion validation

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

    @Test
    public void givenRegistrationWithStartDate_whenEnteredConclusionDateIsAfterOrEqualsStartDate_thenSuccess() {
        final RegistrationConclusionBean conclusionBean = createConclusionBean(curricularPlan, partialConclusion);
        conclusionBean.setEnteredAverageGrade("10");
        conclusionBean.setEnteredConclusionDate(conclusionBean.getRegistration().getStartDate().toLocalDate());
        RegistrationConclusionProcess.run(conclusionBean);
        assertTrue(conclusionBean.isConclusionProcessed());
        assertTrue(conclusionProcessDataMatchesConclusionBean(curricularPlan, partialConclusion, conclusionBean));
    }

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
    public void givenRegistrationNotConcluded_whenConcludeIsExecuted_thenThrowDomainException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CycleCurriculumGroup.cycle.is.not.concluded");

        final Registration newRegistration = createRegistration(degreeCurricularPlan, executionYear);

        final RegistrationConclusionBean conclusionBean =
                createConclusionBean(newRegistration.getLastStudentCurricularPlan(), partialConclusion);
        RegistrationConclusionProcess.run(conclusionBean);
    }

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
                return false;
            }
        } else {
            if (!Objects.equals(conclusionProcess.getConclusionDate(), input.getConclusionDate())) {
                return false;
            }
        }

        if (input.hasEnteredAverageGrade()) {
            if (!gradesEqual(conclusionProcess.getRawGrade(), createNumericGrade(input.getEnteredAverageGrade()))) {
                return false;
            }
        } else {
            if (!gradesEqual(conclusionProcess.getRawGrade(), input.getRawGrade())) {
                return false;
            }
        }

        if (input.hasEnteredFinalAverageGrade()) {
            if (!gradesEqual(conclusionProcess.getFinalGrade(), createNumericGrade(input.getEnteredFinalAverageGrade()))) {
                return false;
            }
        } else {
            if (!gradesEqual(conclusionProcess.getFinalGrade(), input.getFinalGrade())) {
                return false;
            }
        }

        if (input.hasEnteredDescriptiveGrade()) {
            if (!gradesEqual(conclusionProcess.getDescriptiveGrade(),
                    createQualitativeGrade(input.getEnteredDescriptiveGrade()))) {
                return false;
            }
        } else {
            if (!gradesEqual(conclusionProcess.getDescriptiveGrade(), input.getDescriptiveGrade())) {
                return false;
            }
        }

        if (conclusionProcess.getConclusionYear() != input.getConclusionYear()) {
            return false;
        }

        if (conclusionProcess.getGroup() != input.getCurriculumGroup()) {
            return false;
        }

        if (conclusionProcess.getStudentCurricularPlan() != input.getStudentCurricularPlan()) {
            return false;
        }

        if (conclusionProcess.getProgramConclusionConfig() != input.getProgramConclusionConfig()) {
            return false;
        }

        if (conclusionProcess.getCredits().doubleValue() != input.getEctsCredits()) {
            return false;
        }

        if (!Objects.equals(conclusionProcess.getLastVersion().getCurriculum().toString(),
                input.getCurriculumForConclusion().toString())) {
            return false;
        }

        if (conclusionProcess.getIngressionYear() != input.getIngressionYear()) {
            return false;
        }

        if (conclusionProcess.getLastVersion().getResponsible() != input.getConclusionProcessResponsible()) {
            return false;
        }

        //TODO: observations are only written when some of the entered values is filled
        if (input.hasEnteredConclusionDate() || input.hasEnteredAverageGrade() || input.hasEnteredAverageGrade()
                || input.hasEnteredDescriptiveGrade()) {
            if (!Objects.equals(conclusionProcess.getNotes(), input.getObservations())) {
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
