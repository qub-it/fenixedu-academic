package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Information;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.domain.SpaceClassification;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Partial;
import org.joda.time.YearMonthDay;
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
    private static SpaceClassification classification;
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
        classification = new SpaceClassification("ROOM", new LocalizedString());
        classification.setIsAllocatable(true);

        int year = 2023; // executionInterval.getBeginDateYearMonthDay().getYear(); 
        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(year, 9, 15, 0, 0), new DateTime(year, 12, 15, 0, 0))).iterator();

        shift = new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, null);
        createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                createDefaultOccupationPeriod(intervals), null);
    }

    static OccupationPeriod createDefaultOccupationPeriod(Iterator<Interval> intervals) {
        final ExecutionInterval executionInterval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
        final OccupationPeriod occupationPeriod = new OccupationPeriod(intervals);
        new OccupationPeriodReference(occupationPeriod, executionDegree, executionInterval, new CurricularYearList(List.of(-1)));
        return occupationPeriod;
    }

    static Lesson createLesson(final Shift shift, final WeekDay weekDay, final LocalTime startTime, final LocalTime endTime,
            final FrequencyType frequency, final OccupationPeriod period, final Space space) {

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
    public void testShift_findByCourseLoad() {
        final CourseLoadType theoreticalLoad = CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow();
        final CourseLoadType seminarLoad = CourseLoadType.findByCode(CourseLoadType.SEMINAR).orElseThrow();

        assertTrue(executionCourse.findShiftsByLoadType(theoreticalLoad).findAny().isPresent());
        assertTrue(executionCourse.findShiftsByLoadType(theoreticalLoad).anyMatch(s -> s == shift));

        assertTrue(executionCourse.findShiftsByLoadType(seminarLoad).findAny().isEmpty());
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

    @Test
    public void testShift_nameGeneration() {
        assertEquals(shift.getName(), "CAT01");

        final Shift shiftNameCustom1 =
                new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, "CAPL01");
        assertEquals(shiftNameCustom1.getName(), "CAPL01");

        final Shift shiftNameCustom2 = new Shift(executionCourse,
                CourseLoadType.findByCode(CourseLoadType.PRACTICAL_LABORATORY).orElseThrow(), 10, "Custom Name");
        assertEquals(shiftNameCustom2.getName(), "Custom Name");

        final Shift shift1 = new Shift(executionCourse,
                CourseLoadType.findByCode(CourseLoadType.PRACTICAL_LABORATORY).orElseThrow(), 10, null);
        assertEquals(shift1.getName(), "CAPL02");

        final Shift shift2 =
                new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, null);
        assertEquals(shift.getName(), "CAT01"); // ensure it's not changed anymore
        assertEquals(shift2.getName(), "CAT02");

        final Shift shift3 =
                new Shift(executionCourse, CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow(), 10, null);
        assertEquals(shift3.getName(), "CAT03");
    }

    @Test
    public void testLesson_datesWithPeriodChange() {

        new Holiday(new Partial(new LocalDate(2023, 10, 9)));

        Space space = new Space(new Information.Builder().classification(classification).name("Room 1").build());

        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2023, 9, 15, 0, 0), new DateTime(2023, 12, 15, 0, 0))).iterator();
        final OccupationPeriod occupationPeriod = createDefaultOccupationPeriod(intervals);
        Lesson lesson = createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, space);

        assertEquals(lesson.getAllLessonIntervals().size(), 12);

        final SortedSet<YearMonthDay> originalDates = lesson.getAllLessonDates();

        assertEquals(originalDates.size(), 12);
        assertTrue(originalDates.contains(new YearMonthDay(2023, 9, 18)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 9, 25)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 10, 2)));
        assertFalse(originalDates.contains(new YearMonthDay(2023, 10, 9))); // holiday
        assertTrue(originalDates.contains(new YearMonthDay(2023, 10, 16)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 10, 23)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 10, 30)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 11, 6)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 11, 13)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 11, 20)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 11, 27)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 12, 4)));
        assertTrue(originalDates.contains(new YearMonthDay(2023, 12, 11)));

        int year = 2023;
        final Interval interval1 = new Interval(new DateTime(year, 9, 20, 0, 0), new DateTime(year, 10, 31, 23, 59));
        final Interval interval2 = new Interval(new DateTime(year, 11, 13, 0, 0), new DateTime(year, 11, 20, 23, 59));
        final Interval interval3 = new Interval(new DateTime(year, 12, 5, 0, 0), new DateTime(year, 12, 20, 23, 59));
        occupationPeriod.editDates(List.of(interval1, interval2, interval3).iterator());

        assertEquals(lesson.getAllLessonIntervals().size(), 9);

        final SortedSet<YearMonthDay> newDates = lesson.getAllLessonDates();

        assertEquals(newDates.size(), 9);
        assertFalse(newDates.contains(new YearMonthDay(2023, 9, 18)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 9, 25)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 10, 2)));
        assertFalse(newDates.contains(new YearMonthDay(2023, 10, 9)));  // holiday
        assertTrue(newDates.contains(new YearMonthDay(2023, 10, 16)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 10, 23)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 10, 30)));
        assertFalse(newDates.contains(new YearMonthDay(2023, 11, 6)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 11, 13)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 11, 20)));
        assertFalse(newDates.contains(new YearMonthDay(2023, 11, 27)));
        assertFalse(newDates.contains(new YearMonthDay(2023, 12, 4)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 12, 11)));
        assertTrue(newDates.contains(new YearMonthDay(2023, 12, 18)));

        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 20, 10, 15), new DateTime(year, 11, 20, 10, 30))));
        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));

        lesson.createAllLessonInstances();
        assertNull(lesson.getPeriod());
        assertEquals(lesson.getLessonInstancesSet().size(), 9);
        assertEquals(lesson.getAllLessonIntervals().size(), 9);

        final SortedSet<YearMonthDay> datesAfterInstancesCreation = lesson.getAllLessonDates();
        assertEquals(datesAfterInstancesCreation.size(), 9);
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 9, 25)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 10, 2)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 10, 16)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 10, 23)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 10, 30)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 11, 13)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 11, 20)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 12, 11)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 12, 18)));

        assertNull(lesson.getLessonSpaceOccupation());
        assertTrue(lesson.getSpaces().findAny().get() == space);

        assertTrue(lesson.getLessonInstancesSet().stream().allMatch(li -> li.getSpaces().findAny().get() == space));
        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 20, 10, 15), new DateTime(year, 11, 20, 10, 30))));
        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));

//        new LessonInstance(lesson, new YearMonthDay(2023, 12, 17)); TODO ERROR: invalid date!

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.lessonInstance.already.exist");
        new LessonInstance(lesson, new YearMonthDay(2023, 12, 18));
    }

    @Test
    public void testLesson_datesBiweekly() {

        new Holiday(new Partial(new LocalDate(2023, 10, 9)));
//        Bennu.getInstance().getHolidaysSet().forEach(Holiday::delete);

        Space space = new Space(new Information.Builder().classification(classification).name("Room 2").build());

        int year = 2023;
        final Interval interval1 = new Interval(new DateTime(year, 9, 20, 0, 0), new DateTime(year, 10, 31, 23, 59));
        final Interval interval2 = new Interval(new DateTime(year, 11, 13, 0, 0), new DateTime(year, 11, 20, 23, 59));
        final Interval interval3 = new Interval(new DateTime(year, 12, 5, 0, 0), new DateTime(year, 12, 20, 23, 59));

        final OccupationPeriod occupationPeriod =
                createDefaultOccupationPeriod(List.of(interval1, interval2, interval3).iterator());
        Lesson lesson = createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.BIWEEKLY,
                occupationPeriod, space);

        final SortedSet<YearMonthDay> lessonDates = lesson.getAllLessonDates();

//        lessonDates.forEach(date -> System.out.println(">> " + date.toString()));

        assertEquals(lessonDates.size(), 4);
        assertTrue(lessonDates.contains(new YearMonthDay(2023, 9, 25)));
        assertTrue(lessonDates.contains(new YearMonthDay(2023, 10, 23)));
        assertTrue(lessonDates.contains(new YearMonthDay(2023, 11, 13)));
        assertTrue(lessonDates.contains(new YearMonthDay(2023, 12, 11)));

        // FIXME: with holiday, dates should be 5: 2023-09-25, 2023-10-09, 2023-10-23, 2023-11-13, 2023-12-11

        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 13, 10, 15), new DateTime(year, 11, 13, 10, 30))));
        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));

        lesson.createAllLessonInstances();
        assertNull(lesson.getPeriod());
        assertEquals(lesson.getLessonInstancesSet().size(), 4);

        final SortedSet<YearMonthDay> datesAfterInstancesCreation = lesson.getAllLessonDates();
        assertEquals(datesAfterInstancesCreation.size(), 4);
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 9, 25)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 10, 23)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 11, 13)));
        assertTrue(datesAfterInstancesCreation.contains(new YearMonthDay(2023, 12, 11)));

        assertNull(lesson.getLessonSpaceOccupation());
        assertTrue(lesson.getSpaces().findAny().get() == space);

        assertTrue(lesson.getLessonInstancesSet().stream().allMatch(li -> li.getSpaces().findAny().get() == space));
        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 13, 10, 15), new DateTime(year, 11, 13, 10, 30))));
        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));
    }

    @Test
    public void testLesson_datesWithoutInstances() {

        new Holiday(new Partial(new LocalDate(2023, 10, 9)));

        int year = 2023;
        final Interval interval1 = new Interval(new DateTime(year, 9, 20, 0, 0), new DateTime(year, 10, 31, 23, 59));
        final Interval interval2 = new Interval(new DateTime(year, 11, 13, 0, 0), new DateTime(year, 11, 20, 23, 59));
        final Interval interval3 = new Interval(new DateTime(year, 12, 5, 0, 0), new DateTime(year, 12, 20, 23, 59));

        final OccupationPeriod occupationPeriod =
                createDefaultOccupationPeriod(List.of(interval1, interval2, interval3).iterator());
        Lesson lesson = createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);

        final SortedSet<YearMonthDay> newDates = lesson.getAllLessonDates();
        assertEquals(newDates.size(), 9);

        new LessonInstance(lesson, new YearMonthDay(2023, 10, 23));
        new LessonInstance(lesson, new YearMonthDay(2023, 11, 13));
        new LessonInstance(lesson, new YearMonthDay(2023, 12, 11));

        assertEquals(lesson.getLessonInstancesSet().size(), 3);

        final SortedSet<YearMonthDay> datesAfterInstancesCreation = lesson.getAllLessonDates();
        assertEquals(datesAfterInstancesCreation.size(), 9);

        SortedSet<YearMonthDay> datesWithoutInstances = lesson.getAllLessonDatesWithoutInstanceDates();
        assertEquals(datesWithoutInstances.size(), 6);

        assertTrue(datesWithoutInstances.contains(new YearMonthDay(2023, 9, 25)));
        assertTrue(datesWithoutInstances.contains(new YearMonthDay(2023, 10, 2)));
        assertTrue(datesWithoutInstances.contains(new YearMonthDay(2023, 10, 16)));
        assertFalse(datesWithoutInstances.contains(new YearMonthDay(2023, 10, 23)));
        assertTrue(datesWithoutInstances.contains(new YearMonthDay(2023, 10, 30)));
        assertFalse(datesWithoutInstances.contains(new YearMonthDay(2023, 11, 13)));
        assertTrue(datesWithoutInstances.contains(new YearMonthDay(2023, 11, 20)));
        assertFalse(datesWithoutInstances.contains(new YearMonthDay(2023, 12, 11)));
        assertTrue(datesWithoutInstances.contains(new YearMonthDay(2023, 12, 18)));
    }

    @Test
    public void testLesson_comparator() {
        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2023, 9, 15, 0, 0), new DateTime(2023, 12, 15, 0, 0))).iterator();
        final OccupationPeriod occupationPeriod = createDefaultOccupationPeriod(intervals);

        Lesson lesson3 = createLesson(shift, WeekDay.FRIDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        Lesson lesson4 = createLesson(shift, WeekDay.FRIDAY, new LocalTime(11, 0), new LocalTime(12, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        Lesson lesson1 = createLesson(shift, WeekDay.WEDNESDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        Lesson lesson2 = createLesson(shift, WeekDay.THURSDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);

        List<Lesson> sortedLessons = Stream.of(lesson3, lesson4, lesson1, lesson2)
                .sorted(Lesson.LESSON_COMPARATOR_BY_WEEKDAY_AND_STARTTIME).collect(Collectors.toList());

        assertEquals(sortedLessons.get(0), lesson1);
        assertEquals(sortedLessons.get(1), lesson2);
        assertEquals(sortedLessons.get(2), lesson3);
        assertEquals(sortedLessons.get(3), lesson4);
    }

}
