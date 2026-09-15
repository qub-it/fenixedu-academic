package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.fenixedu.academic.domain.schedule.lesson.ExecutionDegreeLessonPeriod;
import org.fenixedu.academic.domain.schedule.lesson.LessonPeriod;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class OccupationPeriodTest {

    private static final Interval SEPTEMBER_INTERVAL =
            new Interval(new DateTime(2026, 9, 1, 0, 0), new DateTime(2026, 10, 1, 0, 0));
    private static final Interval NOVEMBER_INTERVAL =
            new Interval(new DateTime(2026, 11, 1, 0, 0), new DateTime(2026, 12, 1, 0, 0));
    private static final Interval JANUARY_INTERVAL = new Interval(new DateTime(2027, 1, 1, 0, 0), new DateTime(2027, 2, 1, 0, 0));

    private static ExecutionDegree executionDegree;
    private static ExecutionInterval executionInterval;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            DegreeCurricularPlanTest.initDegreeCurricularPlan();

            Degree degree = Degree.find(DEGREE_A_CODE);
            DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().iterator().next();
            ExecutionYear executionYear = ExecutionYear.findCurrent(null);

            executionDegree = degreeCurricularPlan.createExecutionDegree(executionYear);
            executionInterval = executionYear.getFirstExecutionPeriod();

            return null;
        });
    }

    @Test
    public void testOccupationPeriod_isDateInNestedPeriods() {
        OccupationPeriod singleOccupationPeriod = new OccupationPeriod(SEPTEMBER_INTERVAL);

        // the start instant is inclusive
        assertTrue(singleOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 9, 1, 0, 0)));
        // the end instant is exclusive
        assertFalse(singleOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 10, 1, 0, 0)));
        // an instant before the interval start is not contained
        assertFalse(singleOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 8, 31, 0, 0)));

        // nested periods
        OccupationPeriod rootOccupationPeriod = new OccupationPeriod(List.of(SEPTEMBER_INTERVAL, NOVEMBER_INTERVAL).iterator());
        assertTrue(rootOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 11, 1, 0, 0)));

        // an instant in the gap between the linked periods is not contained
        assertFalse(rootOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 10, 1, 0, 0)));

        // only the receiver and its following periods are considered
        OccupationPeriod nestedOccupationPeriod = rootOccupationPeriod.getNextPeriod();
        assertFalse(nestedOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 9, 1, 0, 0)));
        assertTrue(nestedOccupationPeriod.isDateInNestedPeriods(new DateTime(2026, 11, 1, 0, 0)));
    }

    @Test
    public void testOccupationPeriod_getAllNestedPeriods() {
        // single period: the receiver itself is the only nested period
        OccupationPeriod singleOccupationPeriod = new OccupationPeriod(SEPTEMBER_INTERVAL);
        assertEquals(List.of(singleOccupationPeriod), singleOccupationPeriod.getAllNestedPeriods());

        // nested periods: all linked periods are returned in linking order
        OccupationPeriod rootOccupationPeriod =
                new OccupationPeriod(List.of(SEPTEMBER_INTERVAL, NOVEMBER_INTERVAL, JANUARY_INTERVAL).iterator());

        OccupationPeriod middleOccupationPeriod = rootOccupationPeriod.getNextPeriod();
        OccupationPeriod leafOccupationPeriod = middleOccupationPeriod.getNextPeriod();

        assertEquals(List.of(rootOccupationPeriod, middleOccupationPeriod, leafOccupationPeriod),
                rootOccupationPeriod.getAllNestedPeriods());

        // from an inner period only the receiver and the following periods are returned
        assertEquals(List.of(middleOccupationPeriod, leafOccupationPeriod), middleOccupationPeriod.getAllNestedPeriods());

        // the last nested period returns only itself
        assertEquals(List.of(leafOccupationPeriod), leafOccupationPeriod.getAllNestedPeriods());
    }

    @Test
    public void testOccupationPeriod_allNestedPeriodsAreEmpty() {
        // a newly created period with no lessons is empty
        OccupationPeriod singleOccupationPeriod = new OccupationPeriod(SEPTEMBER_INTERVAL);
        assertTrue(singleOccupationPeriod.allNestedPeriodsAreEmpty());

        // an empty chain of nested periods is also empty
        OccupationPeriod rootOccupationPeriod = new OccupationPeriod(List.of(SEPTEMBER_INTERVAL, NOVEMBER_INTERVAL).iterator());
        assertTrue(rootOccupationPeriod.allNestedPeriodsAreEmpty());

        // a lesson period without execution degrees does not make the occupation period non empty
        LessonPeriod lessonPeriod = LessonPeriod.create(executionInterval, rootOccupationPeriod);
        assertTrue(rootOccupationPeriod.allNestedPeriodsAreEmpty());

        // an execution degree lesson period makes the whole chain non empty
        ExecutionDegreeLessonPeriod executionDegreeLessonPeriod =
                ExecutionDegreeLessonPeriod.create(executionDegree, lessonPeriod);
        assertFalse(rootOccupationPeriod.allNestedPeriodsAreEmpty());

        // after removing the execution degree lesson period the chain becomes empty again
        executionDegreeLessonPeriod.delete();
        assertTrue(rootOccupationPeriod.allNestedPeriodsAreEmpty());

        // a non empty leaf period makes the root report the chain as non empty
        OccupationPeriod leafOccupationPeriod = rootOccupationPeriod.getNextPeriod();
        LessonPeriod leafLessonPeriod = LessonPeriod.create(executionInterval, leafOccupationPeriod);
        ExecutionDegreeLessonPeriod.create(executionDegree, leafLessonPeriod);
        assertFalse(rootOccupationPeriod.allNestedPeriodsAreEmpty());
        assertFalse(leafOccupationPeriod.allNestedPeriodsAreEmpty());
    }
}