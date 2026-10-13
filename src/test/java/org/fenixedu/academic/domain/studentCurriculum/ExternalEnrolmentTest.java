package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.ExternalCurricularCourse;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.RegimeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExternalEnrolmentTest {

    private static final String DUPLICATE_MESSAGE_KEY =
            "error.studentCurriculum.ExternalEnrolment.already.exists.externalEnrolment.for.externalCurricularCourse";

    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval lastSemester;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static ExternalCurricularCourse courseA;
    private static ExternalCurricularCourse courseB;
    private static GradeScale gradeScale;
    private static Grade grade;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            DegreeTest.initDegree();
            CompetenceCourseTest.initCompetenceCourse();
            EvaluationSeasonTest.initEvaluationSeasons();
            StudentTest.initRegistrationConfigEntities();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            lastSemester = executionYear.getLastExecutionPeriod();

            degreeCurricularPlan = createDegreeCurricularPlan();

            gradeScale = GradeScale.findUniqueByCode("TYPE20").orElseGet(() -> GradeScale.create("TYPE20",
                    new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"), new BigDecimal("9.49"),
                    new BigDecimal("9.50"), new BigDecimal("20"), false, true));
            grade = Grade.createGrade("10", gradeScale);

            final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
            courseA = new ExternalCurricularCourse(coursesUnit, "External Course A", "ECA");
            courseB = new ExternalCurricularCourse(coursesUnit, "External Course B", "ECB");

            return null;
        });
    }

    @Test
    public void testExternalEnrolment_COMPARATOR_BY_NAME() {
        final ExternalEnrolment courseAEnrolment =
                new ExternalEnrolment(newRegistration("ee.cmp.name.a"), courseA, grade, firstSemester, null, 6.0);
        final ExternalEnrolment courseBEnrolment =
                new ExternalEnrolment(newRegistration("ee.cmp.name.b"), courseB, grade, firstSemester, null, 6.0);

        assertEquals(0, ExternalEnrolment.COMPARATOR_BY_NAME.compare(courseAEnrolment, courseAEnrolment));
        assertTrue(ExternalEnrolment.COMPARATOR_BY_NAME.compare(courseAEnrolment, courseBEnrolment) < 0);
        assertTrue(ExternalEnrolment.COMPARATOR_BY_NAME.compare(courseBEnrolment, courseAEnrolment) > 0);

        final ExternalEnrolment sameNameFirst =
                new ExternalEnrolment(newRegistration("ee.cmp.name.tie.a"), courseA, grade, firstSemester, null, 6.0);
        final ExternalEnrolment sameNameSecond =
                new ExternalEnrolment(newRegistration("ee.cmp.name.tie.b"), courseA, grade, firstSemester, null, 6.0);

        final int expected = Integer.signum(sameNameFirst.getExternalId().compareTo(sameNameSecond.getExternalId()));
        assertNotEquals(0, expected);
        assertEquals(expected,
                Integer.signum(ExternalEnrolment.COMPARATOR_BY_NAME.compare(sameNameFirst, sameNameSecond)));
        assertEquals(-expected,
                Integer.signum(ExternalEnrolment.COMPARATOR_BY_NAME.compare(sameNameSecond, sameNameFirst)));
    }

    @Test
    public void testExternalEnrolment_COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE() {
        final ExternalEnrolment earlier =
                new ExternalEnrolment(newRegistration("ee.cmp.period.earlier"), courseA, grade, firstSemester, null, 6.0);
        final ExternalEnrolment later =
                new ExternalEnrolment(newRegistration("ee.cmp.period.later"), courseB, grade, lastSemester, null, 6.0);

        assertEquals(0,
                ExternalEnrolment.COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE.compare(earlier, earlier));
        assertTrue(
                ExternalEnrolment.COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE.compare(earlier, later) < 0);
        assertTrue(
                ExternalEnrolment.COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE.compare(later, earlier) > 0);

        final ExternalEnrolment samePeriodFirst =
                new ExternalEnrolment(newRegistration("ee.cmp.period.tie.a"), courseA, grade, firstSemester, null, 6.0);
        final ExternalEnrolment samePeriodSecond =
                new ExternalEnrolment(newRegistration("ee.cmp.period.tie.b"), courseB, grade, firstSemester, null, 6.0);

        final int expected = Integer.signum(samePeriodFirst.getExternalId().compareTo(samePeriodSecond.getExternalId()));
        assertNotEquals(0, expected);
        assertEquals(expected, Integer.signum(ExternalEnrolment.COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE
                .compare(samePeriodFirst, samePeriodSecond)));
        assertEquals(-expected, Integer.signum(ExternalEnrolment.COMPARATOR_BY_EXECUTION_PERIOD_AND_EVALUATION_DATE
                .compare(samePeriodSecond, samePeriodFirst)));
    }

    @Test
    public void testExternalEnrolment_constructor() {
        final Registration registration = newRegistration("ee.constructor");
        final YearMonthDay evaluationDate = new YearMonthDay(2024, 6, 15);

        final ExternalEnrolment enrolment =
                new ExternalEnrolment(registration, courseA, grade, firstSemester, evaluationDate, 6.0);

        assertEquals(registration, enrolment.getRegistration());
        assertEquals(courseA, enrolment.getExternalCurricularCourse());
        assertEquals(grade, enrolment.getGrade());
        assertEquals(firstSemester, enrolment.getExecutionInterval());
        assertEquals(evaluationDate, enrolment.getEvaluationDate());
        assertEquals(6.0, enrolment.getEctsCredits(), 0);
        assertTrue(registration.getExternalEnrolmentsSet().contains(enrolment));
    }

    @Test
    public void testExternalEnrolment_checkIfCanCreateExternalEnrolment() {
        final Registration registration = newRegistration("ee.check.duplicate");
        final ExternalEnrolment existing = new ExternalEnrolment(registration, courseA, grade, firstSemester, null, 6.0);

        // same registration, same course: rejected
        final DomainException exception = assertThrows(DomainException.class,
                () -> new ExternalEnrolment(registration, courseA, grade, firstSemester, null, 6.0));
        assertEquals(DUPLICATE_MESSAGE_KEY, exception.getKey());

        // same registration, different course: allowed
        final ExternalEnrolment otherCourse = new ExternalEnrolment(registration, courseB, grade, firstSemester, null, 6.0);
        assertEquals(2, registration.getExternalEnrolmentsSet().size());
        assertTrue(registration.getExternalEnrolmentsSet().contains(existing));
        assertTrue(registration.getExternalEnrolmentsSet().contains(otherCourse));

        // different registration, same course: allowed
        final Registration otherRegistration = newRegistration("ee.check.duplicate.other");
        final ExternalEnrolment sameCourse =
                new ExternalEnrolment(otherRegistration, courseA, grade, firstSemester, null, 6.0);
        assertTrue(otherRegistration.getExternalEnrolmentsSet().contains(sameCourse));
    }

    @Test
    public void testExternalEnrolment_edit() {
        final Registration registration = newRegistration("ee.edit");
        final ExternalEnrolment enrolment =
                new ExternalEnrolment(registration, courseA, grade, firstSemester, null, 6.0);

        final Grade newGrade = Grade.createGrade("15", gradeScale);
        final YearMonthDay newEvaluationDate = new YearMonthDay(2025, 1, 20);

        enrolment.edit(registration, newGrade, lastSemester, newEvaluationDate, 9.0);

        assertEquals(registration, enrolment.getRegistration());
        assertEquals(courseA, enrolment.getExternalCurricularCourse());
        assertEquals(newGrade, enrolment.getGrade());
        assertEquals(lastSemester, enrolment.getExecutionInterval());
        assertEquals(newEvaluationDate, enrolment.getEvaluationDate());
        assertEquals(9.0, enrolment.getEctsCredits(), 0);

        final Registration otherRegistration = newRegistration("ee.edit.other");
        new ExternalEnrolment(otherRegistration, courseA, grade, firstSemester, null, 6.0);

        final DomainException exception = assertThrows(DomainException.class,
                () -> enrolment.edit(otherRegistration, grade, firstSemester, null, 6.0));
        assertEquals(DUPLICATE_MESSAGE_KEY, exception.getKey());
        assertEquals(registration, enrolment.getRegistration());

        final DomainException nullException = assertThrows(DomainException.class,
                () -> enrolment.edit(null, grade, firstSemester, null, 6.0));
        assertEquals("error.externalEnrolment.student.cannot.be.null", nullException.getKey());
        assertEquals(registration, enrolment.getRegistration());
    }

    @Test
    public void testExternalEnrolment_setFields() {
        final Registration registration = newRegistration("ee.set.fields");
        final YearMonthDay evaluationDate = new YearMonthDay(2023, 3, 10);

        final ExternalEnrolment enrolment =
                new ExternalEnrolment(registration, courseA, grade, firstSemester, evaluationDate, 5.0);
        assertEquals(registration, enrolment.getRegistration());
        assertEquals(grade, enrolment.getGrade());
        assertEquals(firstSemester, enrolment.getExecutionInterval());
        assertEquals(evaluationDate, enrolment.getEvaluationDate());
        assertEquals(5.0, enrolment.getEctsCredits(), 0);

        final Grade newGrade = Grade.createGrade("18", gradeScale);
        enrolment.edit(registration, newGrade, lastSemester, null, 12.0);
        assertEquals(registration, enrolment.getRegistration());
        assertEquals(newGrade, enrolment.getGrade());
        assertEquals(lastSemester, enrolment.getExecutionInterval());
        assertNull(enrolment.getEvaluationDate());
        assertEquals(12.0, enrolment.getEctsCredits(), 0);
    }

    @Test
    public void testExternalEnrolment_getName() {
        final ExternalEnrolment enrolment =
                new ExternalEnrolment(newRegistration("ee.name"), courseA, grade, firstSemester, null, 6.0);

        // same name replicated in every supported locale
        final LocalizedString name = enrolment.getName();
        CoreConfiguration.supportedLocales()
                .forEach(locale -> assertEquals(courseA.getName(), name.getContent(locale)));
    }


    @Test
    public void testExternalEnrolment_isAnual() {
        final ExternalEnrolment enrolment =
                new ExternalEnrolment(newRegistration("ee.anual"), courseA, grade, firstSemester, null, 6.0);

        enrolment.setRegime(null);
        assertFalse(enrolment.isAnual());

        enrolment.setRegime(RegimeType.ANUAL);
        assertTrue(enrolment.isAnual());

        enrolment.setRegime(RegimeType.SEMESTRIAL);
        assertFalse(enrolment.isAnual());
    }

    private static DegreeCurricularPlan createDegreeCurricularPlan() {
        final Degree degree = DegreeTest.createDegree(DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow(),
                "EE_DEGREE_" + UUID.randomUUID(), "EE Degree", executionYear);
        final Person person =
                new Person(new UserProfile("EE", "Creator", "EE Creator", "ee.creator@qubit.com", Locale.getDefault()));
        final DegreeCurricularPlan dcp = degree.createDegreeCurricularPlan("EE Plan", person, AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        return dcp;
    }

    private static Registration newRegistration(final String username) {
        final Student student = StudentTest.createStudent("EE Student " + username, username);
        return StudentTest.createRegistration(student, degreeCurricularPlan, executionYear);
    }

}
