package org.fenixedu.academic.domain;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionsAndSchedulesTest {

    private static ExecutionCourse executionCourse;
    private static ExecutionDegree executionDegree;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initExecutions();
            initSchedules();
            return null;
        });
    }

    static void initExecutions() {
        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

        DegreeCurricularPlanTest.initDegreeCurricularPlan();
        final Degree degree = Degree.find("CS");
        final DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().iterator().next();
        final CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode("CA");

        executionDegree = degreeCurricularPlan.createExecutionDegree(executionYear);

        final CompetenceCourse competenceCourse = CompetenceCourse.find("CA");

        executionCourse = new ExecutionCourse(competenceCourse.getName(), competenceCourse.getCode(), executionInterval);
        executionCourse.addAssociatedCurricularCourses(curricularCourse);
    }

    private static void initSchedules() {
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();
        final Shift shift = new Shift(executionCourse, Set.of(ShiftType.TEORICA), 10, null);

        int year = executionInterval.getBeginDateYearMonthDay().getYear();
        final OccupationPeriod occupationPeriod =
                new OccupationPeriod(new Interval(new DateTime(year, 9, 15, 0, 0), new DateTime(year, 12, 15, 0, 0)));
        new OccupationPeriodReference(occupationPeriod, executionDegree, executionInterval, new CurricularYearList(List.of(-1)));

        createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY, occupationPeriod,
                null);
    }

    private static Lesson createLesson(final Shift shift, final WeekDay weekDay, final LocalTime startTime,
            final LocalTime endTime, final FrequencyType frequency, final OccupationPeriod period, final Space space) {

        final DiaSemana diaSemana = DiaSemana.fromWeekDay(weekDay);

        final Calendar startTimeCalendar = Calendar.getInstance();
        startTimeCalendar.setTimeInMillis(startTime.toDateTimeToday().getMillis());
        final Calendar endTimeCalendar = Calendar.getInstance();
        endTimeCalendar.setTimeInMillis(endTime.toDateTimeToday().getMillis());

        final Lesson lesson = new Lesson(diaSemana, startTimeCalendar, endTimeCalendar, shift, frequency,
                shift.getExecutionPeriod(), period, space);
//        lesson.setInitialFullPeriod(period);
        return lesson;

    }

    @Test
    public void testExecutionDegree_find() {
        assertTrue(ExecutionYear.findCurrent(null).getExecutionDegreesSet().size() == 1);
    }

}
