package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.DegreeCurricularPlanDurationTest;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.util.WeekDay;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ShiftTest {

    private static ExecutionDegree executionDegree;
    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;
    private static ExecutionCourse ec;
    private static SchoolClass schoolClass1;
    private static SchoolClass schoolClass2;
    private static Shift shift;
    private static Lesson lesson1, lesson2;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initShiftTest();
            return null;
        });
    }

    public static void initShiftTest() {
        DegreeCurricularPlanTest.initDegreeCurricularPlan();
        StudentTest.initRegistrationConfigEntities();

        executionYear = ExecutionYear.findCurrent(null);
        executionInterval = executionYear.getFirstExecutionPeriod();

        final Degree degree = Degree.find(DEGREE_A_CODE);
        final DegreeCurricularPlan dcp =
                degree.getDegreeCurricularPlansSet().stream().filter(d -> DCP_NAME_V1.equals(d.getName())).findAny()
                        .orElseThrow();
        DegreeCurricularPlanDurationTest.populateCurricularPeriodStructure(dcp);

        executionDegree = dcp.createExecutionDegree(executionYear);

        schoolClass1 = new SchoolClass(executionDegree, executionInterval, "SC1", 1);
        schoolClass2 = new SchoolClass(executionDegree, executionInterval, "SC2", 1);

        ec = new ExecutionCourse("EC", UUID.randomUUID().toString(), executionInterval);
        shift = new Shift(ec, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "TP1");

        // two weeks with lessons on Monday and Tuesday, 3 hours and 1 hour 20 minutes respectively
        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2026, 6, 29, 0, 0), new DateTime(2026, 7, 10, 0, 0))).iterator();
        OccupationPeriod occupationPeriod = ExecutionsAndSchedulesTest.createDefaultOccupationPeriod(intervals);

        lesson1 = Lesson.create(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(13, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        lesson2 = Lesson.create(shift, WeekDay.TUESDAY, new LocalTime(10, 0), new LocalTime(11, 20), FrequencyType.WEEKLY,
                occupationPeriod, null);
    }

    @Test
    public void testGetClassesPrettyPrint() {
        assertEquals("", shift.getClassesPrettyPrint());

        try {
            shift.getAssociatedClassesSet().add(schoolClass1);
            assertEquals("SC1", shift.getClassesPrettyPrint());

            shift.getAssociatedClassesSet().add(schoolClass2);
            String result = shift.getClassesPrettyPrint();
            assertEquals("SC1, SC2", result);
        } finally {
            shift.getAssociatedClassesSet().clear();
        }
    }

    @Test
    public void testGetTotalHours() {
        assertEquals(new BigDecimal("6.00"), lesson1.getTotalHours());
        // 2h40min = 2.6666666666666665, rounded to 2.67
        assertEquals(new BigDecimal("2.67"), lesson2.getTotalHours());
        assertEquals(new BigDecimal("8.67"), shift.getTotalHours());
    }

    @Test
    public void testGetTotalDuration() {
        assertEquals(Duration.standardHours(3), lesson1.getTotalDuration());
        assertEquals(Duration.standardMinutes(80), lesson2.getTotalDuration());
        assertEquals(Duration.standardMinutes(260), shift.getTotalDuration());
    }

    @Test
    public void testGetMaxLessonDuration() {
        // largest shift lesson duration is 3 hours
        assertEquals(new BigDecimal("3.00"), shift.getMaxLessonDuration());
    }

    @Test
    public void testGetUnitHours() {
        assertEquals(new BigDecimal("3.00"), lesson1.getUnitHours());
        // 1h20min = 1,33333333333333, rounded to 1.33
        assertEquals(new BigDecimal("1.33"), lesson2.getUnitHours());
        assertEquals(new BigDecimal("4.33"), shift.getUnitHours());
    }

    @Test
    public void testGetLessonsOrderedByWeekDayAndStartTime() {
        SortedSet<Lesson> lessons = shift.getLessonsOrderedByWeekDayAndStartTime();
        assertEquals(2, lessons.size());

        Iterator<Lesson> it = lessons.iterator();
        assertEquals(lesson1, it.next());
        assertEquals(lesson2, it.next());
    }
}