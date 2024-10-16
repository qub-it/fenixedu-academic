package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.EvaluationSeasonTest.IMPROVEMENT_SEASON_CODE;
import static org.fenixedu.academic.domain.EvaluationSeasonTest.SPECIAL_SEASON_CODE;
import static org.fenixedu.academic.domain.ExecutionsAndSchedulesTest.SCHOOL_CLASS_A_NAME;
import static org.fenixedu.academic.domain.ExecutionsAndSchedulesTest.SCHOOL_CLASS_B_NAME;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.THEORETICAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.accessControl.SchoolClassStudentsGroup;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.log.CurriculumLineLog;
import org.fenixedu.academic.domain.log.EnrolmentActionType;
import org.fenixedu.academic.domain.log.EnrolmentLog;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.util.EnrolmentAction;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EnrolmentTest {

    private static Registration registration;

    private static CurricularCourse curricularCourse;

    private static ExecutionInterval executionInterval;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initEnrolments();
            return null;
        });
    }

    @After
    public void clearEnrolmentEvaluations() {
        registration.findEnrolments().flatMap(e -> e.getEvaluationsSet().stream()).forEach(e -> {
            e.setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
            e.delete();
        });
    }

    public static void initEnrolments() {
        StudentTest.initStudentAndRegistration();
        EvaluationSeasonTest.initEvaluationSeasons();

        registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();

        executionInterval = ExecutionInterval.findFirstCurrentChild(scp.getDegree().getCalendar());

        curricularCourse = scp.getDegreeCurricularPlan().getCurricularCourseByCode(CompetenceCourseTest.COURSE_A_CODE);
        final Context context =
                curricularCourse.getParentContextsSet().stream().filter(ctx -> ctx.isValid(executionInterval)).findAny()
                        .orElseThrow();

        createEnrolment(scp, executionInterval, context, StudentTest.STUDENT_A_USERNAME);

        ExecutionsAndSchedulesTest.initSchedules();

    }

    public static void createEnrolment(StudentCurricularPlan curricularPlan, ExecutionInterval interval, Context context,
            String username) {
        try {
            Authenticate.mock(User.findByUsername(username), "none");
            final CurriculumGroup curriculumGroup =
                    findOrCreateCurriculumGroupFor(curricularPlan, context.getParentCourseGroup());
            final DegreeModuleToEnrol degreeModuleToEnrol = new DegreeModuleToEnrol(curriculumGroup, context, interval);

            curricularPlan.enrol(interval, Set.of(degreeModuleToEnrol), List.of(), CurricularRuleLevel.ENROLMENT_WITH_RULES);
        } finally {
            Authenticate.unmock();
        }
    }

    public static void createOptionalEnrolment(StudentCurricularPlan curricularPlan, ExecutionInterval interval, Context context,
            CurricularCourse curricularCourse, String username) {
        try {
            Authenticate.mock(User.findByUsername(username), "none");
            final CurriculumGroup curriculumGroup =
                    findOrCreateCurriculumGroupFor(curricularPlan, context.getParentCourseGroup());
            final DegreeModuleToEnrol degreeModuleToEnrol =
                    new OptionalDegreeModuleToEnrol(curriculumGroup, context, interval, curricularCourse);

            curricularPlan.enrol(interval, Set.of(degreeModuleToEnrol), List.of(), CurricularRuleLevel.ENROLMENT_WITH_RULES);
        } finally {
            Authenticate.unmock();
        }
    }

    //TODO: move this method to domain && remove StudentCurricularPlanServices.initializeGroupIfRequired(StudentCurricularPlan, CourseGroup)
    public static CurriculumGroup findOrCreateCurriculumGroupFor(StudentCurricularPlan scp, CourseGroup courseGroup) {

        final List<CourseGroup> path = new ArrayList<CourseGroup>();
        CourseGroup groupToAdd = courseGroup;
        while (groupToAdd != null) {
            if (!groupToAdd.isRoot()) {
                path.add(0, groupToAdd);
            }
            groupToAdd = groupToAdd.getParentCourseGroupStream().findAny().orElse(null);
        }

        CurriculumGroup current = scp.getRoot();
        for (final CourseGroup pathElement : path) {
            final CurriculumGroup existing = current.findCurriculumGroupFor(pathElement);

            if (existing == null) {
                current = CurriculumGroupFactory.createGroup(current, pathElement);
            } else {
                current = existing;
            }
        }
        return current;
    }

    @Test
    public void testEnrolment_find() {
        final Collection<Enrolment> enrolments =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()));
        assertEquals(enrolments.size(), 1);
    }

    @Test
    public void testEnrolment_path() {
        final Context context =
                curricularCourse.getParentContextsSet().stream().filter(ctx -> ctx.isValid(executionInterval)).findAny()
                        .orElseThrow();

        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();

        final List<CourseGroup> contextPath = Stream.concat(context.getParentCourseGroup().getAllParentCourseGroups().stream(),
                        Stream.of(context.getParentCourseGroup())).sorted(Comparator.comparing(CourseGroup::getOneFullName))
                .collect(Collectors.toList());

        final List<CourseGroup> enrolmentPath =
                enrolment.getCurriculumGroup().getPath().stream().map(cg -> (CourseGroup) cg.getDegreeModule())
                        .collect(Collectors.toList());

        assertEquals(contextPath, enrolmentPath);
    }

    @Test
    public void testEnrolment_hasImprovementForNextIntervals() {
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        final ExecutionInterval executionInterval = enrolment.getExecutionInterval();
        final ExecutionInterval nextInterval = executionInterval.getNext();
        final ExecutionInterval nextNextInterval = nextInterval.getNext();

        final CurricularCourse curricularCourse = enrolment.getCurricularCourse();
        final ExecutionCourse executionCourse = findOrCreateExecutionCourse(curricularCourse, executionInterval);
        final ExecutionCourse executionCourseNext = findOrCreateExecutionCourse(curricularCourse, nextInterval);
        final ExecutionCourse executionCourseNextNext = findOrCreateExecutionCourse(curricularCourse, nextNextInterval);

        final Optional<Attends> attendsEnrolment = enrolment.findAttends(executionInterval);
        assertTrue(attendsEnrolment.isPresent());
        assertEquals(attendsEnrolment.get().getExecutionCourse(), executionCourse);
        assertFalse(enrolment.hasImprovementFor(executionInterval));
        assertFalse(enrolment.hasImprovementFor(nextInterval));
        assertEquals(1, registration.getRegistrationDataByExecutionYearSet().size());

        final EnrolmentEvaluation improvementEvaluation = createImprovementEvaluation(enrolment, nextInterval);
        assertTrue(enrolment.hasImprovementFor(nextInterval));
        final Optional<Attends> attendsNext = enrolment.findAttends(nextInterval);
        assertTrue(attendsNext.isPresent());
        assertEquals(attendsNext.get().getExecutionCourse(), executionCourseNext);
        assertNotEquals(attendsEnrolment, attendsNext);
        assertEquals(attendsEnrolment, enrolment.findAttends(executionInterval));
        assertEquals(1, registration.getRegistrationDataByExecutionYearSet().size()); // because it's the same year

        final Optional<Attends> attendsNextNext = enrolment.findAttends(nextNextInterval);
        assertFalse(attendsNextNext.isPresent());
        improvementEvaluation.editImprovementExecutionInterval(nextNextInterval);
        assertTrue(enrolment.hasImprovementFor(nextNextInterval));
        assertFalse(enrolment.hasImprovementFor(nextInterval));
        final Optional<Attends> attendsNextNextAfter = enrolment.findAttends(nextNextInterval);
        assertTrue(attendsNextNextAfter.isPresent());
        assertEquals(attendsNextNextAfter.get().getExecutionCourse(), executionCourseNextNext);
        assertNotEquals(attendsEnrolment, attendsNextNextAfter);
        assertFalse(enrolment.findAttends(nextInterval).isPresent());
        assertEquals(2, registration.getRegistrationDataByExecutionYearSet().size()); // because it's next year
    }

    @Test
    public void testEnrolment_deleteImprovement() {
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        final ExecutionInterval executionInterval = enrolment.getExecutionInterval();
        final ExecutionInterval nextInterval = executionInterval.getNext();

        final CurricularCourse curricularCourse = enrolment.getCurricularCourse();
        final ExecutionCourse executionCourse = findOrCreateExecutionCourse(curricularCourse, executionInterval);
        final ExecutionCourse executionCourseNext = findOrCreateExecutionCourse(curricularCourse, nextInterval);

        final Optional<Attends> attendsEnrolment = enrolment.findAttends(executionInterval);
        assertTrue(attendsEnrolment.isPresent());
        assertEquals(attendsEnrolment.get().getExecutionCourse(), executionCourse);

        final EnrolmentEvaluation improvementEvaluation = createImprovementEvaluation(enrolment, nextInterval);
        assertTrue(enrolment.hasImprovementFor(nextInterval));
        final Optional<Attends> attendsNext = enrolment.findAttends(nextInterval);
        assertTrue(attendsNext.isPresent());
        assertEquals(attendsNext.get().getExecutionCourse(), executionCourseNext);

        improvementEvaluation.delete();
        assertFalse(enrolment.hasImprovementFor(nextInterval));
        assertFalse(enrolment.findAttends(nextInterval).isPresent());
    }

    private static ExecutionCourse findOrCreateExecutionCourse(final CurricularCourse curricularCourse,
            final ExecutionInterval executionInterval) {
        return curricularCourse.findExecutionCourses(executionInterval).findAny().orElseGet(() -> {
            final ExecutionCourse result =
                    new ExecutionCourse(curricularCourse.getName(), curricularCourse.getCode(), executionInterval);
            result.addAssociatedCurricularCourses(curricularCourse);
            return result;
        });
    }

    @Test
    public void testEnrolment_hasNotImprovementForInterval() {
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        createImprovementEvaluation(enrolment, enrolment.getExecutionInterval());
        final ExecutionInterval otherInterval =
                enrolment.getExecutionYear().getChildIntervals().stream().filter(ei -> ei != enrolment.getExecutionInterval())
                        .findFirst().get();

        assertEquals(false, enrolment.hasImprovementFor(otherInterval));
    }

    @Test
    public void testEnrolment_hasImprovementForSameInterval() {
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        final Optional<Attends> attendsBefore = enrolment.findAttends(enrolment.getExecutionInterval());
        createImprovementEvaluation(enrolment, enrolment.getExecutionInterval());
        final Optional<Attends> attendsAfter = enrolment.findAttends(enrolment.getExecutionInterval());

        assertTrue(enrolment.hasImprovementFor(enrolment.getExecutionInterval()));
        assertEquals(attendsBefore, attendsAfter);
    }

    @Test
    public void testEnrolment_hasImprovementForExecutionYear() {
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        createImprovementEvaluation(enrolment, enrolment.getExecutionInterval());

        assertTrue(enrolment.hasImprovementFor(enrolment.getExecutionYear()));
    }

    @Test
    public void testEnrolment_duplicateSeasonEnrolment() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.enrolmentEvaluation.duplicate.season");
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        createImprovementEvaluation(enrolment, enrolment.getExecutionInterval());
        createImprovementEvaluation(enrolment, enrolment.getExecutionInterval());
    }

    @Test
    public void testEnrolment_wrongImprovementSeason() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Evaluation season is not of improvement");
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();

        final EvaluationSeason season = EvaluationSeason.findByCode(SPECIAL_SEASON_CODE).orElseThrow();
        final EnrolmentEvaluation evaluation = new EnrolmentEvaluation(enrolment, season);
        evaluation.editImprovementExecutionInterval(enrolment.getExecutionInterval());
    }

    @Test
    public void testEnrolment_improvementWithoutInterval() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Improvement period is required");
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        createImprovementEvaluation(enrolment, null);
    }

    @Test
    public void testEnrolment_improvementInvalidInterval() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Improvement period is invalid, before original enrolment");
        final Enrolment enrolment =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                        .iterator().next();
        createImprovementEvaluation(enrolment, enrolment.getExecutionInterval().getPrevious());
    }

    private EnrolmentEvaluation createImprovementEvaluation(final Enrolment enrolment,
            final ExecutionInterval executionInterval) {
        final EvaluationSeason season = EvaluationSeason.findByCode(IMPROVEMENT_SEASON_CODE).orElseThrow();
        final EnrolmentEvaluation evaluation = new EnrolmentEvaluation(enrolment, season);
        evaluation.editImprovementExecutionInterval(executionInterval);
        return evaluation;
    }

    @Test
    public void testShift_enrolments() {
        final ExecutionCourse executionCourse =
                curricularCourse.getExecutionCoursesByExecutionPeriod(executionInterval).iterator().next();

        final CourseLoadType theoreticalLoad = CourseLoadType.of(THEORETICAL);

        final Shift shiftA = new Shift(executionCourse, theoreticalLoad, 10, "T_testShiftEnrolments_A");
        final Shift shiftB = new Shift(executionCourse, theoreticalLoad, 10, "T_testShiftEnrolments_B");

        assertEquals(registration.getShiftEnrolmentsSet().size(), 0);
        assertEquals(shiftA.getVacancies(), 10);
        assertEquals(shiftB.getVacancies(), 10);

        assertTrue(shiftA.enrol(registration));
        assertEquals(registration.getShiftEnrolmentsSet().size(), 1);
        assertEquals(registration.findEnrolledShiftFor(executionCourse, theoreticalLoad).get(), shiftA);
        assertEquals(shiftA.getVacancies(), 9);
        assertEquals(shiftB.getVacancies(), 10);

        assertTrue(shiftB.enrol(registration));
        assertEquals(registration.getShiftEnrolmentsSet().size(), 1);
        assertEquals(registration.findEnrolledShiftFor(executionCourse, theoreticalLoad).get(), shiftB);
        assertEquals(shiftA.getVacancies(), 10);
        assertEquals(shiftB.getVacancies(), 9);

        shiftB.unenrol(registration);
        assertEquals(registration.getShiftEnrolmentsSet().size(), 0);
        assertTrue(registration.findEnrolledShiftFor(executionCourse, theoreticalLoad).isEmpty());
        assertEquals(shiftA.getVacancies(), 10);
        assertEquals(shiftB.getVacancies(), 10);
    }

    @Test
    public void testSchoolClass_enrolments() {
        final ExecutionDegree executionDegree =
                registration.getLastDegreeCurricularPlan().findExecutionDegree(executionInterval).orElseThrow();
        final SchoolClass schoolClassA =
                SchoolClass.findBy(executionDegree, executionInterval, 1).filter(sc -> sc.getName().equals(SCHOOL_CLASS_A_NAME))
                        .findAny().orElseThrow();
        final SchoolClass schoolClassB =
                SchoolClass.findBy(executionDegree, executionInterval, 1).filter(sc -> sc.getName().equals(SCHOOL_CLASS_B_NAME))
                        .findAny().orElseThrow();

        assertTrue(registration.findSchoolClass(executionInterval).isEmpty());

        SchoolClass.replaceSchoolClass(registration, schoolClassA, executionInterval);
        assertTrue(registration.findSchoolClass(executionInterval).isPresent());
        assertTrue(registration.findSchoolClass(executionInterval).stream().anyMatch(sc -> sc == schoolClassA));
        assertTrue(registration.findSchoolClass(executionInterval).stream().noneMatch(sc -> sc == schoolClassB));

        final SchoolClassStudentsGroup schoolClassAStudentsGroup = SchoolClassStudentsGroup.get(schoolClassA);
        final SchoolClassStudentsGroup schoolClassBStudentsGroup = SchoolClassStudentsGroup.get(schoolClassB);

        final User registrationUser = registration.getStudent().getPerson().getUser();

        assertEquals(schoolClassAStudentsGroup.getMembers().count(), 1);
        assertTrue(schoolClassAStudentsGroup.getMembers().anyMatch(u -> u == registrationUser));
        assertTrue(schoolClassAStudentsGroup.isMember(registrationUser));
        assertTrue(schoolClassBStudentsGroup.getMembers().findAny().isEmpty());
    }

    @Test
    public void testEnrolmentLog_createCurriculumLineLog() {
        final int registrationLogCounter = registration.getCurriculumLineLogs(executionInterval).size();
        final int curricularCourseLogCounter = curricularCourse.getCurriculumLineLogsSet().size();
        final int executionIntervalLogCounter = executionInterval.getCurriculumLineLogsSet().size();

        final Enrolment enrolment = registration
                .getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar())).iterator().next();

        enrolment.createCurriculumLineLog(EnrolmentActionType.UNENROL);

        assertEquals(registration.getCurriculumLineLogs(executionInterval).size(), registrationLogCounter + 1);
        assertEquals(curricularCourse.getCurriculumLineLogsSet().size(), curricularCourseLogCounter + 1);
        assertEquals(executionInterval.getCurriculumLineLogsSet().size(), executionIntervalLogCounter + 1);
    }

    @Test
    public void testCurriculumLineLog_setType() {
        final CurriculumLineLog unenrolLog =
                new EnrolmentLog(EnrolmentActionType.UNENROL, registration, curricularCourse, executionInterval, "testWho");

        final CurriculumLineLog enrolLog =
                new EnrolmentLog(EnrolmentActionType.ENROL, registration, curricularCourse, executionInterval, "testWho");

        assertEquals(unenrolLog.getType(), EnrolmentActionType.UNENROL);
        assertEquals(enrolLog.getType(), EnrolmentActionType.ENROL);

        // Remove assertions below once EnrolmentAction refactoring is completed
        assertEquals(unenrolLog.getAction(), EnrolmentAction.UNENROL);
        assertEquals(enrolLog.getAction(), EnrolmentAction.ENROL);
    }
}
