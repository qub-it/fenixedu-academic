package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V2;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V3;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.student.StudentStatuteBean;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StudentTest {

    public static final String INGRESSION_CODE = "I";
    public static final String PROTOCOL_CODE = "P";
    public static final String REGISTRATION_STATE_INTERRUPTED = "INTERRUPTED";

    public static final String STUDENT_A_USERNAME = "student.a";

    private static Student student;
    private static Registration registration, registrationWithCoursesActive, registrationWithoutCoursesActive,
            registrationWithCoursesConcludedNextYear;
    private static RegistrationStateType concludedType;
    private static RegistrationProtocol protocol;
    private static IngressionType ingression;
    private static ExecutionYear executionYear, previousYear, nextYear;
    private static ExecutionInterval firstSemester, secondSemester, previousInterval;
    private static Degree degreeWithCourses, degreeWithoutCourses;
    private static DegreeCurricularPlan dcpWithCourses, dcpWithoutCourses;
    private static StatuteType workingStatuteType, regularStatuteType;
    private static ExecutionCourse executionCourse1, executionCourse2;
    private static CurricularCourse cc1, cc2;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initStudentAndRegistration();
            initDomainTestData();
            return null;
        });
    }

    public static void initStudentAndRegistration() {
        initRegistrationConfigEntities();

        student = createStudent("Student A", STUDENT_A_USERNAME);

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        ExecutionsAndSchedulesTest.initExecutions();

        final Degree degree = Degree.find(DEGREE_A_CODE);
        final DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().stream()
                .filter(dcp -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(dcp.getName())).findAny().orElseThrow();

        protocol = RegistrationProtocol.findByCode(PROTOCOL_CODE);
        ingression = IngressionType.findIngressionTypeByCode(INGRESSION_CODE).orElseThrow();

        registration = createRegistration(student, degreeCurricularPlan,
                ExecutionYear.findCurrent(degreeCurricularPlan.getDegree().getCalendar()));
    }

    public static void initDomainTestData() {
        /**
         *   dcpWithCourses (Root -> Cycle -> Mandatory (C1, C2, C3), Optional (C4, C5))
         *
         * Enrolments on registrationWithCoursesActive's SCP:
         *   C1 in firstSemester (state: APROVED)
         *   C2 in secondSemester (state: ENROLLED)
         */

        EvaluationSeasonTest.initEvaluationSeasons();
        executionYear = ExecutionYear.findCurrent(null);
        firstSemester = executionYear.getFirstExecutionPeriod();
        secondSemester = executionYear.getLastExecutionPeriod();
        previousYear = (ExecutionYear) executionYear.getPrevious();
        previousInterval = previousYear.getFirstExecutionPeriod();

        dcpWithCourses = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
        degreeWithCourses = dcpWithCourses.getDegree();

        DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).get();
        degreeWithoutCourses = DegreeTest.createDegree(degreeType, "DEG2", "Degree Two", executionYear);
        dcpWithoutCourses = degreeWithoutCourses.createDegreeCurricularPlan("Plan 2", User.findByUsername("admin").getPerson(),
                AcademicPeriod.THREE_YEAR);
        dcpWithoutCourses.createExecutionDegree(executionYear);

        cc1 = dcpWithCourses.getCurricularCourseByCode("C1");
        cc2 = dcpWithCourses.getCurricularCourseByCode("C2");

        executionCourse1 = new ExecutionCourse(cc1.getName(), cc1.getCode(), firstSemester);
        executionCourse1.addAssociatedCurricularCourses(cc1);

        executionCourse2 = new ExecutionCourse(cc2.getName(), cc2.getCode(), secondSemester);
        executionCourse2.addAssociatedCurricularCourses(cc2);

        // Registrations
        registrationWithCoursesActive = Registration.create(student, dcpWithCourses, executionYear, protocol, ingression);
        registrationWithoutCoursesActive = Registration.create(student, dcpWithoutCourses, executionYear, protocol, ingression);

        nextYear = (ExecutionYear) executionYear.getNext();
        dcpWithCourses.createExecutionDegree(nextYear);
        registrationWithCoursesConcludedNextYear = Registration.create(student, dcpWithCourses, nextYear, protocol, ingression);

        concludedType = RegistrationStateType.findByCode(RegistrationStateType.CONCLUDED_CODE).get();
        RegistrationState.createRegistrationState(registrationWithCoursesConcludedNextYear, null, null, concludedType,
                nextYear.getFirstExecutionPeriod());

        // Enrolments
        StudentCurricularPlan scp1 = registrationWithCoursesActive.getLastStudentCurricularPlan();
        Context contextC1 = cc1.getParentContextsSet().iterator().next();
        Context contextC2 = cc2.getParentContextsSet().iterator().next();

        EnrolmentTest.createEnrolment(scp1, firstSemester, contextC1, "admin");
        EnrolmentTest.createEnrolment(scp1, secondSemester, contextC2, "admin");

        // Make C1 enrolment approved for getApprovedEnrolments test
        Enrolment enrolmentC1 =
                scp1.getEnrolmentsSet().stream().filter(e -> e.getCurricularCourse() == cc1).findAny().orElseThrow();
        EnrolmentEvaluation evaluationC1 = enrolmentC1.getEvaluationsSet().iterator().next();
        GradeScale.create("TYPE20", new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true);
        evaluationC1.setGrade(Grade.createGrade("10", GradeScale.findUniqueByCode("TYPE20").get()));
        evaluationC1.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);

        // Statutes
        workingStatuteType = StatuteType.create("WORKING", new LocalizedString().with(Locale.getDefault(), "Working Student"));
        workingStatuteType.setWorkingStudentStatute(true);
        workingStatuteType.setAppliedOnRegistration(false);

        regularStatuteType = StatuteType.create("REGULAR", new LocalizedString().with(Locale.getDefault(), "Regular Student"));
        regularStatuteType.setAppliedOnRegistration(false);

        new StudentStatute(student, workingStatuteType, firstSemester, firstSemester, null, null,
                "Working student statute for testing", null);
        new StudentStatute(student, regularStatuteType, firstSemester, firstSemester, null, null, "Regular statute for testing",
                null);
    }

    public static Student createStudent(String name, String username) {
        Person person = createPerson(name, username);
        return new Student(person);
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

    public static Registration createRegistration(final Student student, final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return Registration.create(student, degreeCurricularPlan, executionYear, protocol, ingression);
    }

    public static void initRegistrationConfigEntities() {
        RegistrationProtocol.create(PROTOCOL_CODE, new LocalizedString.Builder().with(Locale.getDefault(), "Protocol").build());

        IngressionType.createIngressionType(INGRESSION_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Ingression").build());

        RegistrationStateType.create(RegistrationStateType.REGISTERED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Registered").build(), true);

        RegistrationStateType.create(REGISTRATION_STATE_INTERRUPTED,
                new LocalizedString.Builder().with(Locale.getDefault(), "Interrupted").build(), false);

        RegistrationStateType.create(RegistrationStateType.CONCLUDED_CODE,
                new LocalizedString.Builder().with(Locale.getDefault(), "Concluded").build(), false);
    }

    @Test
    public void testStudent_createAndFind() {
        final Student studentA = new Student(createPerson("test_createAndFind.A", "test_createAndFind.a"));
        final Student studentB = new Student(createPerson("test_createAndFind.B", "test_createAndFind.b"));
        final Student studentC = new Student(createPerson("test_createAndFind.C", "test_createAndFind.c"));

        assertEquals(Student.readStudentByNumber(studentA.getNumber()), studentA);
        assertEquals(Student.readStudentByNumber(studentB.getNumber()), studentB);
        assertEquals(Student.readStudentByNumber(studentC.getNumber()), studentC);
    }

    @Test
    public void testStudent_createWithExistingNumber() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Student.number.already.exists");

        final Student studentA = new Student(createPerson("test_withExistingNumber.A", "test_withExistingNumber.a"));
        new Student(createPerson("test_withExistingNumber.B", "test_withExistingNumber.b"), studentA.getNumber());
    }

    @Test
    public void testStudent_generateNumber() {
        final Student studentA = new Student(createPerson("test_generateNumber.A", "test_generateNumber.a"));
        assertEquals(Student.generateStudentNumber(), Integer.valueOf(studentA.getNumber() + 1));

        final Student studentB = new Student(createPerson("test_generateNumber.B", "test_generateNumber.b"));
        assertEquals(Student.generateStudentNumber(), Integer.valueOf(studentB.getNumber() + 1));

        final Student studentC =
                new Student(createPerson("test_generateNumber.C", "test_generateNumber.c"), studentB.getNumber() + 10);
        assertEquals(Student.generateStudentNumber(), Integer.valueOf(studentC.getNumber() + 1));
        assertEquals(studentC.getNumber(), Integer.valueOf(studentB.getNumber() + 10));
    }

    @Test
    public void testRegistration_find() {
        assertEquals(4, student.getRegistrationsSet().size());
        assertTrue(student.getRegistrationsSet().contains(registration));
        assertTrue(student.getRegistrationsSet().contains(registrationWithCoursesActive));
        assertTrue(student.getRegistrationsSet().contains(registrationWithoutCoursesActive));
        assertTrue(student.getRegistrationsSet().contains(registrationWithCoursesConcludedNextYear));
    }

    @Test
    public void testRegistration_studentCurricularPlans() {
        Map<String, DegreeCurricularPlan> dcpsByName = registration.getDegree().getDegreeCurricularPlansSet().stream()
                .collect(Collectors.toMap(dcp -> dcp.getName(), dcp -> dcp));

        DegreeCurricularPlan dcpV1 = dcpsByName.get(DCP_NAME_V1);
        DegreeCurricularPlan dcpV2 = dcpsByName.get(DCP_NAME_V2);
        DegreeCurricularPlan dcpV3 = dcpsByName.get(DCP_NAME_V3);

        final ExecutionYear currentYear = ExecutionYear.findCurrent(registration.getDegree().getCalendar());
        final ExecutionYear nextYear = (ExecutionYear) currentYear.getNext();
        final ExecutionYear nextNextYear = (ExecutionYear) nextYear.getNext();

        assertEquals(registration.getStudentCurricularPlansSet().size(), 1);

        StudentCurricularPlan firstSCP = registration.getStudentCurricularPlansSet().iterator().next();
        StudentCurricularPlan secondSCP = registration.createStudentCurricularPlan(dcpV2, nextYear);
        StudentCurricularPlan thirdSCP = StudentCurricularPlan.createBolonhaStudentCurricularPlan(registration, dcpV3,
                nextYear.getBeginDateYearMonthDay().plusDays(1), nextYear.getFirstExecutionPeriod(), null);

        assertEquals(firstSCP.getDegreeCurricularPlan(), dcpV1);
        assertEquals(secondSCP.getDegreeCurricularPlan(), dcpV2);
        assertEquals(thirdSCP.getDegreeCurricularPlan(), dcpV3);
        assertEquals(firstSCP.getStartExecutionInterval(), currentYear.getFirstExecutionPeriod());
        assertEquals(secondSCP.getStartExecutionInterval(), thirdSCP.getStartExecutionInterval());

        assertEquals(registration.getFirstStudentCurricularPlan(), firstSCP);
        assertEquals(registration.getLastStudentCurricularPlan(), thirdSCP);

        assertTrue(registration.findStudentCurricularPlan(currentYear.getPrevious()).isEmpty());
        assertEquals(registration.findStudentCurricularPlan(currentYear).get(), firstSCP);
        assertEquals(registration.findStudentCurricularPlan(currentYear.getLastExecutionPeriod()).get(), firstSCP);
        assertEquals(registration.findStudentCurricularPlan(nextYear).get(), thirdSCP);
        assertEquals(registration.findStudentCurricularPlan(nextNextYear).get(), thirdSCP);
    }

    @Test
    public void testStudent_getActiveRegistrationsIn_filtersByInterval() {
        List<Registration> activeInFirstSemester = student.getActiveRegistrationsIn(firstSemester);
        assertEquals(3, activeInFirstSemester.size());
        assertTrue(activeInFirstSemester.contains(registration));
        assertTrue(activeInFirstSemester.contains(registrationWithCoursesActive));
        assertTrue(activeInFirstSemester.contains(registrationWithoutCoursesActive));

        // registrationWithCoursesConcludedNextYear has CONCLUDED state in nextYear -> not active in firstSemester
        assertFalse(activeInFirstSemester.contains(registrationWithCoursesConcludedNextYear));

        // secondSemester: no states created directly for secondSemester,
        // but getRegistrationStates falls back to firstSemester -> same 3 active
        List<Registration> activeInSecond = student.getActiveRegistrationsIn(secondSemester);
        assertEquals(3, activeInSecond.size());
        assertTrue(activeInSecond.contains(registration));
        assertTrue(activeInSecond.contains(registrationWithCoursesActive));
        assertTrue(activeInSecond.contains(registrationWithoutCoursesActive));

        // previousInterval: no states found -> 0
        List<Registration> activeInPrevious = student.getActiveRegistrationsIn(previousInterval);
        assertTrue(activeInPrevious.isEmpty());

        // null interval: returns empty
        List<Registration> activeInNull = student.getActiveRegistrationsIn(null);
        assertTrue(activeInNull.isEmpty());
    }

    @Test
    public void testStudent_getLastRegistration_returnsRegistrationWithLatestStartDate() {
        // this registration is in nextYear, so it has the latest start date
        assertEquals(registrationWithCoursesConcludedNextYear, student.getLastRegistration());

        // test with a new registration (now the latest one)
        Registration newLatestRegistration = Registration.create(student, dcpWithCourses, executionYear, protocol, ingression);
        assertEquals(newLatestRegistration, student.getLastRegistration());
        newLatestRegistration.delete();

        Student emptyStudent = createStudent("Empty Student", "empty.student");
        assertNull(emptyStudent.getLastRegistration());
        emptyStudent.delete();
    }

    @Test
    public void testStudent_getStatutes_filtersByExecutionInterval() {
        // Both statutes are valid only in firstSemester
        Collection<StudentStatuteBean> statutesFirst = student.getStatutes(firstSemester);
        assertEquals(2, statutesFirst.size());
        assertTrue(statutesFirst.stream().anyMatch(bean -> bean.getStatuteType() == workingStatuteType));
        assertTrue(statutesFirst.stream().anyMatch(bean -> bean.getStatuteType() == regularStatuteType));

        assertTrue(student.getStatutes(secondSemester).isEmpty());
        assertTrue(student.getStatutes(previousInterval).isEmpty());
        assertTrue(student.getStatutes(null).isEmpty());
    }

    @Test
    public void testStudent_getStatutesValidOnAnyExecutionSemesterFor_filtersByExecutionYear() {
        Collection<StudentStatuteBean> validInCurrentYear = student.getStatutesValidOnAnyExecutionSemesterFor(executionYear);
        assertEquals(2, validInCurrentYear.size());
        assertTrue(validInCurrentYear.stream().anyMatch(bean -> bean.getStatuteType() == workingStatuteType));
        assertTrue(validInCurrentYear.stream().anyMatch(bean -> bean.getStatuteType() == regularStatuteType));

        assertTrue(student.getStatutesValidOnAnyExecutionSemesterFor(previousYear).isEmpty());
        assertThrows(NullPointerException.class, () -> student.getStatutesValidOnAnyExecutionSemesterFor(null));
    }

    @Test
    public void testStudent_getApprovedEnrolments_returnsOnlyApprovedEnrolments() {
        // C1 (firstSemester) has a FINAL EnrolmentEvaluation with grade "10" (passing on TYPE20).
        // C2 (secondSemester) has only a TEMPORARY evaluation -> not approved.
        Set<Enrolment> approved = student.getApprovedEnrolments();
        assertFalse(approved.isEmpty());
        assertTrue(approved.stream().allMatch(Enrolment::isApproved));
        assertTrue(approved.stream().noneMatch(e -> e.getCurricularCourse() == cc2));
    }

    @Test
    public void testStudent_readAttendByExecutionCourse() {
        Attends attend1 = student.readAttendByExecutionCourse(executionCourse1);
        assertNotNull(attend1);
        assertEquals(executionCourse1, attend1.getExecutionCourse());

        Attends attend2 = student.readAttendByExecutionCourse(executionCourse2);
        assertNotNull(attend2);
        assertEquals(executionCourse2, attend2.getExecutionCourse());

        ExecutionCourse unknownCourse = new ExecutionCourse("Unknown", "XX", firstSemester);
        assertNull(student.readAttendByExecutionCourse(unknownCourse));
        assertNull(student.readAttendByExecutionCourse(null));
    }

    @Test
    public void testStudent_getAttendsForExecutionPeriod_returnsSortedAttendsForGivenInterval() {
        // Add extra Attends in firstSemester to verify sorting
        new Attends(registrationWithCoursesActive, new ExecutionCourse("Am the first one", "X1", firstSemester));
        new Attends(registrationWithCoursesActive, new ExecutionCourse("Better be next", "X2", firstSemester));
        new Attends(registrationWithCoursesActive, new ExecutionCourse("Definitely last", "X4", firstSemester));

        SortedSet<Attends> attendsFirst = student.getAttendsForExecutionPeriod(firstSemester);
        assertEquals(4, attendsFirst.size());
        assertTrue(attendsFirst.stream().allMatch(a -> a.getExecutionCourse().getExecutionInterval() == firstSemester));

        List<Attends> actualOrder = new ArrayList<>(attendsFirst);
        List<Attends> expectedOrder = new ArrayList<>(actualOrder);
        expectedOrder.sort(Attends.ATTENDS_COMPARATOR_BY_EXECUTION_COURSE_NAME);
        assertEquals(expectedOrder, actualOrder);

        SortedSet<Attends> attendsSecond = student.getAttendsForExecutionPeriod(secondSemester);
        assertFalse(attendsSecond.isEmpty());
        assertTrue(attendsSecond.stream().allMatch(a -> a.getExecutionCourse().getExecutionInterval() == secondSemester));

        SortedSet<Attends> attendsPrevious = student.getAttendsForExecutionPeriod(previousInterval);
        assertTrue(attendsPrevious.isEmpty());
    }

    @Test
    public void testStudent_getRegistrationsFor_degreeCurricularPlan() {
        List<Registration> forDcp1 = student.getRegistrationsFor(dcpWithCourses);
        assertEquals(2, forDcp1.size());
        assertTrue(forDcp1.contains(registrationWithCoursesActive));
        assertTrue(forDcp1.contains(registrationWithCoursesConcludedNextYear));

        List<Registration> forDcp2 = student.getRegistrationsFor(dcpWithoutCourses);
        assertEquals(1, forDcp2.size());
        assertTrue(forDcp2.contains(registrationWithoutCoursesActive));

        List<Registration> forNull = student.getRegistrationsFor((DegreeCurricularPlan) null);
        assertTrue(forNull.isEmpty());
    }

    @Test
    public void testStudent_getRegistrationsFor_degree() {
        List<Registration> forDegree1 = student.getRegistrationsFor(degreeWithCourses);
        assertEquals(2, forDegree1.size());
        assertTrue(forDegree1.contains(registrationWithCoursesActive));
        assertTrue(forDegree1.contains(registrationWithCoursesConcludedNextYear));

        List<Registration> forDegree2 = student.getRegistrationsFor(degreeWithoutCourses);
        assertEquals(1, forDegree2.size());
        assertTrue(forDegree2.contains(registrationWithoutCoursesActive));

        List<Registration> forNull = student.getRegistrationsFor((Degree) null);
        assertTrue(forNull.isEmpty());
    }

    @Test
    public void testStudent_hasActiveRegistrations() {
        assertTrue(student.hasActiveRegistrations());

        // student with CONCLUDED registration
        Student inactiveStudent = createStudent("Inactive Student", "inactive.student");
        Registration inactiveReg = Registration.create(inactiveStudent, dcpWithCourses, executionYear, protocol, ingression);
        RegistrationState.createRegistrationState(inactiveReg, null, null, concludedType, firstSemester);

        assertFalse(inactiveStudent.hasActiveRegistrations());
        RegistrationState state = registrationWithCoursesConcludedNextYear.getLastRegistrationState(nextYear);
        assertEquals(concludedType, state.getType());
        assertFalse(state.isActive());

        // student with no registrations
        Student emptyStudent = createStudent("Empty Student 3", "empty.student3");
        assertFalse(emptyStudent.hasActiveRegistrations());
        emptyStudent.delete();
    }

    @Test
    public void testStudent_hasWorkingStudentStatuteInPeriod_returnsTrueWhenExists() {
        assertTrue(student.hasWorkingStudentStatuteInPeriod(firstSemester));
        assertFalse(student.hasWorkingStudentStatuteInPeriod(secondSemester));

        Student emptyStudent = createStudent("Empty Student 4", "empty.student4");
        assertFalse(emptyStudent.hasWorkingStudentStatuteInPeriod(firstSemester));
        assertFalse(emptyStudent.hasWorkingStudentStatuteInPeriod(secondSemester));
    }

    @Test
    public void testStudent_getPersonalIngressionDataByExecutionYear_returnsDataForGivenYear() {
        PersonalIngressionData pid2 = new PersonalIngressionData(student, previousYear);
        assertEquals(pid2, student.getPersonalIngressionDataByExecutionYear(previousYear));

        assertNotNull(student.getPersonalIngressionDataByExecutionYear(executionYear));

        // returns null for missing year
        ExecutionYear nextNextYear = (ExecutionYear) executionYear.getNext().getNext();
        assertNull(student.getPersonalIngressionDataByExecutionYear(nextNextYear));

        assertNull(student.getPersonalIngressionDataByExecutionYear(null));
    }
}
