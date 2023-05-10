package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionsAndSchedulesTest {

    private static ExecutionCourse executionCourse;
    private static ExecutionDegree executionDegree;
    private static Shift shift;

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
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final DegreeCurricularPlan degreeCurricularPlan = degree.getDegreeCurricularPlansSet().iterator().next();
        final CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode(COURSE_A_CODE);

        executionDegree = degreeCurricularPlan.createExecutionDegree(executionYear);

        final CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);

        executionCourse = new ExecutionCourse(competenceCourse.getName(), competenceCourse.getCode(), executionInterval);
        executionCourse.addAssociatedCurricularCourses(curricularCourse);
    }

    static void initSchedules() {
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();
        shift = new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, null);

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
        return lesson;
    }

    @Test
    public void testExecutionDegree_find() {
        assertTrue(ExecutionYear.findCurrent(null).getExecutionDegreesSet().size() == 1);
    }

    @Test
    public void testShift_courseLoadTotalHours() {
        assertEquals(shift.getCourseLoadTotalHours(), new BigDecimal("30.0"));
        assertEquals(shift.getCourseLoadTotalHoursOld(), new BigDecimal("30.0"));
        assertEquals(shift.getCourseLoadTotalHours(), shift.getCourseLoadTotalHoursOld());
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testShift_sameNameOnCreation() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Shift.with.this.name.already.exists");

        new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, "T1");
        new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, "T1");
    }

    @Test
    public void testShift_sameNameOnEdit() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Shift.with.this.name.already.exists");

        new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, "T1");
        final Shift shift2 =
                new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, "T2");

        shift2.edit(shift2.getCourseLoadType(), "T1", null, null);
    }

    @Test
    public void testShift_shiftTypeForMigration() {
        assertTrue(shift.getTypes().contains(ShiftType.TEORICA));
    }

//    @Test
//    public void testShift_name() {
//        System.out.println("Shift Name: " + shift.getName());
//        System.out.println();
//
//        final Shift shift1 = new Shift(executionCourse, Set.of(ShiftType.LABORATORIAL), 10, null);
//        System.out.println("Shift Name: " + shift.getName());
//        System.out.println("Shift 1 Name: " + shift1.getName());
//        System.out.println();
//
//        final Shift shift2 = new Shift(executionCourse, Set.of(ShiftType.TEORICA), 10, null);
//        System.out.println("Shift Name: " + shift.getName());
//        System.out.println("Shift 1 Name: " + shift1.getName());
//        System.out.println("Shift 2 Name: " + shift2.getName());
//        System.out.println();
//
//        final Shift shift3 = new Shift(executionCourse, Set.of(ShiftType.TEORICA), 10, null);
//        System.out.println("Shift Name: " + shift.getName());
//        System.out.println("Shift 1 Name: " + shift1.getName());
//        System.out.println("Shift 2 Name: " + shift2.getName());
//        System.out.println("Shift 3 Name: " + shift3.getName());
//        System.out.println();
//    }

}
