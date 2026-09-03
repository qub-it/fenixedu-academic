package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EnrolmentEvaluationTest {

    private Enrolment enrolment;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();
            return null;
        });
    }

    @Before
    public void setup() {
        Registration registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
        enrolment = registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()))
                .iterator().next();
    }

    @After
    public void cleanUp() {
        enrolment.getEvaluationsSet().forEach(EnrolmentEvaluation::delete);
    }

    private EnrolmentEvaluation createEvaluation() {
        // EnrolmentEvaluation enforces a unique enrolment+season pair, so each evaluation gets its own season.
        EvaluationSeason season =
                EvaluationSeasonTest.createEvaluationSeason("TEST_SEASON_" + System.nanoTime(), false, false, false);
        return new EnrolmentEvaluation(enrolment, season);
    }

    @Test
    public void testEnrolmentEvaluation_COMPARATORY_BY_WHEN() {
        // evaluation with a defined whenDateTime vs another with null whenDateTime -> null sorts first
        EnrolmentEvaluation withWhen = createEvaluation();
        withWhen.setWhenDateTime(new DateTime(2025, 5, 20, 10, 30));

        EnrolmentEvaluation withoutWhen = createEvaluation();
        withoutWhen.setWhenDateTime(null);

        assertTrue(EnrolmentEvaluation.COMPARATORY_BY_WHEN.compare(withWhen, withoutWhen) > 0);

        // two evaluations ordered by whenDateTime ascending
        EnrolmentEvaluation earlier = createEvaluation();
        earlier.setWhenDateTime(new DateTime(2025, 5, 19, 9, 0));

        assertTrue(EnrolmentEvaluation.COMPARATORY_BY_WHEN.compare(earlier, withWhen) < 0);

        // two evaluations with equal whenDateTime -> tie
        EnrolmentEvaluation sameWhen = createEvaluation();
        sameWhen.setWhenDateTime(withWhen.getWhenDateTime());
        assertEquals(0, EnrolmentEvaluation.COMPARATORY_BY_WHEN.compare(withWhen, sameWhen));

        // two evaluations both with null whenDateTime -> tie
        EnrolmentEvaluation anotherWithoutWhen = createEvaluation();
        anotherWithoutWhen.setWhenDateTime(null);
        assertEquals(0, EnrolmentEvaluation.COMPARATORY_BY_WHEN.compare(withoutWhen, anotherWithoutWhen));
    }

    @Test
    public void testEnrolmentEvaluation_COMPARATOR_BY_EXAM_DATE() {
        // evaluation with null exam date vs another with a defined exam date -> null sorts first
        EnrolmentEvaluation withExamDate = createEvaluation();
        withExamDate.setExamDateYearMonthDay(new YearMonthDay(2025, 7, 15));

        EnrolmentEvaluation withoutExamDate = createEvaluation();
        withoutExamDate.setExamDateYearMonthDay(null);

        assertTrue(EnrolmentEvaluation.COMPARATOR_BY_EXAM_DATE.compare(withExamDate, withoutExamDate) > 0);

        // two evaluations ordered by exam date ascending
        EnrolmentEvaluation earlierExam = createEvaluation();
        earlierExam.setExamDateYearMonthDay(new YearMonthDay(2025, 7, 10));

        assertTrue(EnrolmentEvaluation.COMPARATOR_BY_EXAM_DATE.compare(withExamDate, earlierExam) > 0);

        // two evaluations with equal exam date -> tie
        EnrolmentEvaluation sameExamDate = createEvaluation();
        sameExamDate.setExamDateYearMonthDay(withExamDate.getExamDateYearMonthDay());
        assertEquals(0, EnrolmentEvaluation.COMPARATOR_BY_EXAM_DATE.compare(withExamDate, sameExamDate));

        // two evaluations both with null exam date -> tie
        EnrolmentEvaluation anotherWithoutExamDate = createEvaluation();
        anotherWithoutExamDate.setExamDateYearMonthDay(null);
        assertEquals(0, EnrolmentEvaluation.COMPARATOR_BY_EXAM_DATE.compare(withoutExamDate, anotherWithoutExamDate));
    }

    @Test
    public void testEnrolmentEvaluation_getExamLocalDate_matchesGetExamDate() {
        // new and deprecated getters return the same date for the stored field
        EnrolmentEvaluation evaluation = createEvaluation();
        evaluation.setExamDateYearMonthDay(new YearMonthDay(2025, 7, 15));

        // both getters reflect the same stored date
        assertEquals(evaluation.getExamLocalDate().toDate(), evaluation.getExamDate());
        assertEquals(new LocalDate(2025, 7, 15), evaluation.getExamLocalDate());


        // null stored field -> both getters return null
        evaluation.setExamDateYearMonthDay(null);
        assertNull(evaluation.getExamLocalDate());
        assertNull(evaluation.getExamDate());
    }

    @Test
    public void testEnrolmentEvaluation_getGradeAvailableLocalDate_matchesGetGradeAvailableDate() {
        // new and deprecated getters return the same date for the stored field
        EnrolmentEvaluation evaluation = createEvaluation();
        evaluation.setGradeAvailableDateYearMonthDay(new YearMonthDay(2025, 8, 25));

        // both getters reflect the same stored date
        assertEquals(evaluation.getGradeAvailableLocalDate().toDate(), evaluation.getGradeAvailableDate());
        assertEquals(new LocalDate(2025, 8, 25), evaluation.getGradeAvailableLocalDate());


        // null stored field -> both getters return null
        evaluation.setGradeAvailableDateYearMonthDay(null);
        assertNull(evaluation.getGradeAvailableLocalDate());
        assertNull(evaluation.getGradeAvailableDate());
    }

    @Test
    public void testEnrolmentEvaluation_getWhen_matchesGetWhenDateTime() {
        // new and deprecated getters return the same value for the stored field
        EnrolmentEvaluation evaluation = createEvaluation();
        evaluation.setWhenDateTime(new DateTime(2025, 9, 1, 14, 45, 30));

        // both getters reflect the same stored instant
        assertEquals(new Date(evaluation.getWhenDateTime().getMillis()), evaluation.getWhen());

        // null stored field -> deprecated getter returns null
        evaluation.setWhenDateTime(null);
        assertNull(evaluation.getWhen());
    }
}
