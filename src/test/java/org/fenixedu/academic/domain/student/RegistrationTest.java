package org.fenixedu.academic.domain.student;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.ExternalCurricularCourse;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.ExternalEnrolment;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationTest {

    private static ExecutionYear executionYear, nextExecutionYear, presentYear, futureYear;
    private static ExecutionInterval firstSemester, secondSemester, nextYearFirstSemester, nextYearSecondSemester,
            presentFirstSemester, futureFirstSemester;
    private static Registration registration;
    private static StudentCurricularPlan studentCurricularPlan;
    private static ExecutionInterval executionInterval;
    private static CurricularCourse curricularCourse;
    private static Context context;
    private static ExecutionCourse executionCourseA;

    private static final String PRESENT_ACADEMIC_YEAR_NAME = "PRESENT_YEAR";
    private static final String FUTURE_ACADEMIC_YEAR_NAME = "FUTURE_YEAR";
    public static final String INGRESSION_CODE = "I";
    public static final String PROTOCOL_CODE = "P";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();

            registration = Student.readStudentByNumber(1)
                    .getRegistrationStream()
                    .findAny()
                    .orElseThrow();

            studentCurricularPlan = registration.getLastStudentCurricularPlan();
            executionInterval = ExecutionInterval.findFirstCurrentChild(studentCurricularPlan.getDegree().getCalendar());
            curricularCourse =
                    studentCurricularPlan.getDegreeCurricularPlan().getCurricularCourseByCode(CompetenceCourseTest.COURSE_A_CODE);
            executionCourseA = curricularCourse.findExecutionCourses(executionInterval).iterator().next();

            context = curricularCourse.getParentContextsSet().stream().filter(ctx -> ctx.isValid(executionInterval)).findAny()
                    .orElseThrow();

            final int presentYear = LocalDate.now().getYear();
            AcademicYearCE presentAcademicYearEntry =
                    createStandardYearInterval(Bennu.getInstance().getDefaultAcademicCalendar(), PRESENT_ACADEMIC_YEAR_NAME,
                            presentYear);
            AcademicYearCE futureAcademicYearEntry =
                    createStandardYearInterval(Bennu.getInstance().getDefaultAcademicCalendar(), FUTURE_ACADEMIC_YEAR_NAME,
                            presentYear + 1);

            createFirstSemesterInterval(presentAcademicYearEntry);
            createFirstSemesterInterval(futureAcademicYearEntry);

            return null;
        });
    }

    private static AcademicYearCE createStandardYearInterval(final AcademicCalendarRootEntry calendar, final String name,
            final int year) {
        return createYearInterval(calendar, name, new LocalDate(year, 9, 1), new LocalDate(year + 1, 8, 30));
    }

    private static AcademicYearCE createYearInterval(AcademicCalendarRootEntry calendar, String name, LocalDate startDate,
            LocalDate endDate) {
        return new AcademicYearCE(calendar, new LocalizedString().with(Locale.getDefault(), name), null,
                startDate.toDateTimeAtStartOfDay(), endDate.toDateTimeAtStartOfDay(), calendar);
    }

    private static AcademicIntervalCE createFirstSemesterInterval(AcademicYearCE academicYearEntry) {
        final int year = academicYearEntry.getBegin().getYear();
        final AcademicIntervalCE firstSemesterEntry = new AcademicIntervalCE(AcademicPeriod.SEMESTER, academicYearEntry,
                new LocalizedString().with(Locale.getDefault(), "1st Semester"), null, new DateTime(year, 9, 1, 0, 0, 0),
                new DateTime(year + 1, 1, 31, 23, 59, 59), academicYearEntry.getRootEntry());

        firstSemesterEntry.getExecutionInterval().setState(PeriodState.OPEN);
        return firstSemesterEntry;
    }

    @Before
    public void setupTests() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            registration.getRegistrationStatesSet().forEach(RegistrationState::delete);

            executionYear = ExecutionYear.findCurrent(null);
            nextExecutionYear = executionYear.getNext().getExecutionYear();
            executionYear.setState(PeriodState.CURRENT);
            nextExecutionYear.setState(PeriodState.OPEN);

            firstSemester = executionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            secondSemester = executionYear.getChildInterval(2, AcademicPeriod.SEMESTER);
            firstSemester.setState(PeriodState.CURRENT);
            secondSemester.setState(PeriodState.OPEN);

            nextYearFirstSemester = nextExecutionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            nextYearSecondSemester = nextExecutionYear.getChildInterval(2, AcademicPeriod.SEMESTER);
            nextYearFirstSemester.setState(PeriodState.OPEN);
            nextYearSecondSemester.setState(PeriodState.OPEN);

            presentYear = ExecutionYear.readExecutionYearByName(PRESENT_ACADEMIC_YEAR_NAME);
            futureYear = ExecutionYear.readExecutionYearByName(FUTURE_ACADEMIC_YEAR_NAME);
            presentYear.setState(PeriodState.OPEN);
            futureYear.setState(PeriodState.OPEN);

            presentFirstSemester = presentYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            futureFirstSemester = futureYear.getChildInterval(1, AcademicPeriod.SEMESTER);
            presentFirstSemester.setState(PeriodState.OPEN);
            futureFirstSemester.setState(PeriodState.OPEN);

            return null;
        });
    }

    public static Registration createRegistration(final Student student, final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return Registration.create(student, degreeCurricularPlan, executionYear, RegistrationProtocol.findByCode(PROTOCOL_CODE),
                IngressionType.findIngressionTypeByCode(INGRESSION_CODE).orElseThrow());
    }

    @Test
    public void testRegistration_create() {
        Student student = StudentTest.createStudent("Student B", "studentB");
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        RegistrationProtocol protocol = RegistrationProtocol.findByCode(PROTOCOL_CODE);
        IngressionType ingressionType = IngressionType.findIngressionTypeByCode(INGRESSION_CODE).orElseThrow();

        Registration result = Registration.create(student, degreeCurricularPlan, executionYear, protocol, ingressionType);

        assertEquals(student.getPerson(), result.getPerson());
        assertEquals(degreeCurricularPlan.getDegree(), result.getDegree());
        assertEquals(executionYear, result.getRegistrationYear());
        assertEquals(protocol, result.getRegistrationProtocol());
        assertEquals(ingressionType, result.getIngressionType());
        assertTrue(result.isActive());
        assertEquals(degreeCurricularPlan, result.getLastStudentCurricularPlan().getDegreeCurricularPlan());
        assertNotNull(student.getPersonalIngressionDataByExecutionYear(executionYear));
    }

    @Test
    public void testGetEnrolmentsExecutionYearStream_matchesGetEnrolmentsExecutionYears() {
        Registration newRegistration = createFreshRegistration();

        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), executionInterval, context, "admin");
        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), nextYearFirstSemester, context, "admin");

        Set<ExecutionYear> expected = new HashSet<>(newRegistration.getEnrolmentsExecutionYears());
        Set<ExecutionYear> actual = newRegistration.getEnrolmentsExecutionYearStream().collect(Collectors.toSet());
        assertEquals(Set.of(executionYear, nextExecutionYear), expected);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetStateInDate_dateTimeMatchesLocalDate() {
        RegistrationState interrupted = RegistrationState.createRegistrationState(registration, null, new DateTime("2020-02-01"),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).orElseThrow(), executionInterval);
        RegistrationState registered = RegistrationState.createRegistrationState(registration, null, new DateTime("2020-03-01"),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).orElseThrow(), executionInterval);
        RegistrationState concluded = RegistrationState.createRegistrationState(registration, null, new DateTime("2020-04-01"),
                RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).orElseThrow(), executionInterval);

        Map<LocalDate, RegistrationState> expected = new HashMap<>();
        expected.put(new LocalDate(2020, 1, 1), null); // before any state
        expected.put(new LocalDate(2020, 2, 1), interrupted);
        expected.put(new LocalDate(2020, 3, 15), registered);
        expected.put(new LocalDate(2020, 4, 1), concluded);
        expected.put(new LocalDate(2020, 12, 31), concluded); // after the latest state

        for (Map.Entry<LocalDate, RegistrationState> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), registration.getStateInDate(entry.getKey().toDateTimeAtStartOfDay()));
            assertEquals(registration.getStateInDate(entry.getKey()),
                    registration.getStateInDate(entry.getKey().toDateTimeAtStartOfDay()));
        }
    }

    @Test
    public void testRegistration_numberComparator() {
        Registration newRegistration = createFreshRegistration();

        assertEquals(0, Registration.NUMBER_COMPARATOR.compare(registration, registration));
        assertTrue(Registration.NUMBER_COMPARATOR.compare(registration, newRegistration) < 0);
        assertTrue(Registration.NUMBER_COMPARATOR.compare(newRegistration, registration) > 0);
    }

    @Test
    public void testRegistration_comparatorByStartDate() {
        Registration newRegistration = createFreshRegistration();

        registration.setStartDate(new YearMonthDay(2020, 1, 1));
        newRegistration.setStartDate(new YearMonthDay(2021, 1, 1));

        assertEquals(0, Registration.COMPARATOR_BY_START_DATE.compare(registration, registration));
        assertTrue(Registration.COMPARATOR_BY_START_DATE.compare(registration, newRegistration) < 0);
        assertTrue(Registration.COMPARATOR_BY_START_DATE.compare(newRegistration, registration) > 0);
    }

    @Test
    public void testRegistration_attends() {
        assertTrue(registration.attends(executionCourseA));

        ExecutionCourse newExecutionCourse = new ExecutionCourse("Course for Attends Test", "ATT", executionInterval);

        assertFalse(registration.attends(newExecutionCourse));
    }

    @Test
    public void testRegistration_hasAnyEnrolments() {
        Registration newRegistration = createFreshRegistration();

        assertFalse(newRegistration.hasAnyEnrolments());

        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), executionInterval, context, "admin");

        assertTrue(newRegistration.hasAnyEnrolments());
    }

    @Test
    public void testRegistration_getApprovedEnrolments() {
        Registration newRegistration = createFreshRegistration();
        StudentCurricularPlan newStudentCurricularPlan = newRegistration.getLastStudentCurricularPlan();
        EnrolmentTest.createEnrolment(newStudentCurricularPlan, executionInterval, context, "admin");

        assertTrue(newRegistration.getApprovedEnrolments().isEmpty());

        Enrolment enrolment = newStudentCurricularPlan.getEnrolmentsSet().iterator().next();
        approveEnrolment(enrolment);

        assertTrue(newRegistration.getApprovedEnrolments().contains(enrolment));
        assertEquals(1, newRegistration.getApprovedEnrolments().size());
    }

    @Test
    public void testRegistration_getApprovedExternalEnrolments() {
        Registration newRegistration = createFreshRegistration();
        Unit unit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        ExternalCurricularCourse externalCourse = new ExternalCurricularCourse(unit, "External Course", "EXT");
        Grade grade = Grade.createGrade("15", createGradeScale());

        assertTrue(newRegistration.getApprovedExternalEnrolments().isEmpty());

        ExternalEnrolment externalEnrolment =
                new ExternalEnrolment(newRegistration, externalCourse, grade, executionInterval, new YearMonthDay(2024, 1, 10),
                        6.0);

        Collection<ExternalEnrolment> approved = newRegistration.getApprovedExternalEnrolments();
        assertEquals(1, approved.size());
        assertTrue(approved.contains(externalEnrolment));
    }

    @Test
    public void testRegistration_hasAnyEnroledEnrolments() {
        Registration newRegistration = createFreshRegistration();

        assertFalse(newRegistration.hasAnyEnroledEnrolments(executionYear));
        assertFalse(newRegistration.hasAnyEnroledEnrolments(nextExecutionYear));

        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), executionInterval, context, "admin");

        assertTrue(newRegistration.hasAnyEnroledEnrolments(executionYear));
        assertFalse(newRegistration.hasAnyEnroledEnrolments(nextExecutionYear));
    }

    @Test
    public void testRegistration_hasAnyEnrolmentsIn_executionYear() {
        Registration newRegistration = createFreshRegistration();

        assertFalse(newRegistration.hasAnyEnrolmentsIn(executionYear));
        assertFalse(newRegistration.hasAnyEnrolmentsIn(nextExecutionYear));

        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), executionInterval, context, "admin");

        assertTrue(newRegistration.hasAnyEnrolmentsIn(executionYear));
        assertFalse(newRegistration.hasAnyEnrolmentsIn(nextExecutionYear));
    }

    @Test
    public void testRegistration_hasAnyEnrolmentsIn_executionInterval() {
        Registration newRegistration = createFreshRegistration();

        assertFalse(newRegistration.hasAnyEnrolmentsIn(executionInterval));
        assertFalse(newRegistration.hasAnyEnrolmentsIn(executionInterval.getNext()));

        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), executionInterval, context, "admin");

        assertTrue(newRegistration.hasAnyEnrolmentsIn(executionInterval));
        assertFalse(newRegistration.hasAnyEnrolmentsIn(executionInterval.getNext()));
    }

    @Test
    public void testRegistration_getCurriculumLinesExecutionYears() {
        Registration newRegistration = createFreshRegistration();

        assertTrue(newRegistration.getCurriculumLinesExecutionYears().isEmpty());

        EnrolmentTest.createEnrolment(newRegistration.getLastStudentCurricularPlan(), executionInterval, context, "admin");

        assertFalse(newRegistration.getCurriculumLinesExecutionYears().isEmpty());
        assertTrue(newRegistration.getCurriculumLinesExecutionYears().contains(executionYear));
        assertFalse(newRegistration.getCurriculumLinesExecutionYears().contains(nextExecutionYear));
    }

    @Test
    public void testRegistration_getLastEnrolmentExecutionYear() {
        Registration newRegistration = createFreshRegistration();
        StudentCurricularPlan newStudentCurricularPlan = newRegistration.getLastStudentCurricularPlan();

        assertNull(newRegistration.getLastEnrolmentExecutionYear());

        EnrolmentTest.createEnrolment(newStudentCurricularPlan, executionInterval, context, "admin");

        assertEquals(executionYear, newRegistration.getLastEnrolmentExecutionYear());

        EnrolmentTest.createEnrolment(newStudentCurricularPlan, nextExecutionYear.getFirstExecutionPeriod(), context, "admin");

        assertNotEquals(executionYear, newRegistration.getLastEnrolmentExecutionYear());
        assertEquals(nextExecutionYear, newRegistration.getLastEnrolmentExecutionYear());
    }

    @Test
    public void testRegistration_activeState_shouldReturnStateFromRegistration() {
        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        RegistrationState activeState = registration.getActiveState();

        assertNotNull("Active state should not be null", activeState);
        assertEquals("Active state should be the first state created", activeState, firstState);
        assertTrue("Active state should belong to this registration",
                registration.getRegistrationStatesSet().contains(activeState));
    }

    @Test
    public void testRegistration_activeState_sameExecutionInterval_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        assertTrue(executionYear.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), firstSemester);

        assertEquals(firstState.getExecutionYear(), lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For the same current execution interval with two states, the active state should be the last one",
                lastState,
                activeState
        );
    }

    @Test
    public void testRegistration_activeState_sameYearDifferentExecutionIntervals_shouldReturnLastCreatedState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        assertTrue(executionYear.isCurrent());
        assertTrue(firstSemester.isCurrent());
        assertFalse(secondSemester.isCurrent());

        final RegistrationState semester1State =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        final RegistrationState semester2State = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), secondSemester);

        assertEquals(semester1State.getExecutionYear(), semester2State.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals(
                "For the same year with two states in different execution intervals, the active state should be the last state created",
                semester2State,
                activeState
        );
    }

    @Test
    public void testRegistration_activeState_differentExecutionYear_shouldReturnFirstState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        presentYear.setState(PeriodState.CURRENT);
        assertTrue(presentYear.isCurrent());
        assertFalse(futureYear.isCurrent());

        presentFirstSemester.setState(PeriodState.CURRENT);
        assertTrue(presentFirstSemester.isCurrent());
        assertFalse(futureFirstSemester.isCurrent());

        final RegistrationState firstState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), presentFirstSemester);

        final RegistrationState lastState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().plusDays(1),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), futureFirstSemester);

        assertEquals(presentYear, firstState.getExecutionYear());
        assertEquals(futureYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different execution years, the active state should be the state from the current execution year",
                firstState,
                activeState
        );
    }

    @Test
    public void testRegistration_activeState_differentExecutionYear_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        futureYear.setState(PeriodState.CURRENT);
        assertFalse(presentYear.isCurrent());
        assertTrue(futureYear.isCurrent());

        futureFirstSemester.setState(PeriodState.CURRENT);
        assertFalse(presentFirstSemester.isCurrent());
        assertTrue(futureFirstSemester.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusDays(1),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), presentFirstSemester);

        final RegistrationState lastState = RegistrationState.createRegistrationState(registration, null, DateTime.now(),
                RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(), futureFirstSemester);

        assertEquals(presentYear, firstState.getExecutionYear());
        assertEquals(futureYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different execution years, the active state should be the state from the current execution year",
                lastState, activeState);
    }

    @Test
    public void testRegistration_activeState_pastExecutionYears_shouldReturnLastState() {
        assertEquals(0, registration.getRegistrationStatesSet().size());

        assertTrue(executionYear.isCurrent());
        assertFalse(nextExecutionYear.isCurrent());

        firstSemester.setState(PeriodState.CURRENT);
        assertTrue(firstSemester.isCurrent());
        assertFalse(nextYearFirstSemester.isCurrent());

        final RegistrationState firstState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusMonths(6),
                        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).get(), firstSemester);

        final RegistrationState lastState =
                RegistrationState.createRegistrationState(registration, null, DateTime.now().minusMonths(5),
                        RegistrationStateType.findByCode(StudentTest.REGISTRATION_STATE_INTERRUPTED).get(),
                        nextYearFirstSemester);

        assertEquals(executionYear, firstState.getExecutionYear());
        assertEquals(nextExecutionYear, lastState.getExecutionYear());

        RegistrationState activeState = registration.getActiveState();

        assertEquals("For different past execution years, the active state should be the last state created",
                lastState, activeState);
    }

    // Helpers

    private static Registration createFreshRegistration() {
        String username = UUID.randomUUID().toString();
        Student student = StudentTest.createStudent(username, username);
        return createRegistration(student, studentCurricularPlan.getDegreeCurricularPlan(), ExecutionYear.findCurrent(null));
    }

    private static GradeScale createGradeScale() {
        return GradeScale.findUniqueByCode("TYPE20").orElseGet(
                () -> GradeScale.create("TYPE20", new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                        new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true));
    }

    private static void approveEnrolment(final Enrolment enrolment) {
        enrolment.getEvaluationsSet().forEach(e -> {
            e.setGrade(Grade.createGrade("10", createGradeScale()));
            e.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        });
    }
}
