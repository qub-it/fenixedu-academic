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
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.commons.i18n.LocalizedString;
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

    public static final String SCHOOL_CLASS_B_NAME = "School Class B";

    public static final String SCHOOL_CLASS_A_NAME = "School Class A";

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

    public static void initExecutions() {
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

        shift = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                createDefaultOccupationPeriod(intervals), null);

        final ExecutionInterval executionInterval = executionDegree.getExecutionYear().getFirstExecutionPeriod();
        new SchoolClass(executionDegree, executionInterval, SCHOOL_CLASS_A_NAME, 1);
        new SchoolClass(executionDegree, executionInterval, SCHOOL_CLASS_B_NAME, 1);
    }

    public static OccupationPeriod createDefaultOccupationPeriod(Iterator<Interval> intervals) {
        final ExecutionInterval executionInterval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
        final OccupationPeriod occupationPeriod = new OccupationPeriod(intervals);
        new OccupationPeriodReference(occupationPeriod, executionDegree, executionInterval, new CurricularYearList(List.of(-1)));
        return occupationPeriod;
    }

    public static Lesson createLesson(final Shift shift, final WeekDay weekDay, final LocalTime startTime, final LocalTime endTime,
            final FrequencyType frequency, final OccupationPeriod period, final Space space) {

        final DiaSemana diaSemana = DiaSemana.fromWeekDay(weekDay);

        final Calendar startTimeCalendar = Calendar.getInstance();
        startTimeCalendar.setTimeInMillis(startTime.toDateTimeToday().getMillis());
        final Calendar endTimeCalendar = Calendar.getInstance();
        endTimeCalendar.setTimeInMillis(endTime.toDateTimeToday().getMillis());

        final Lesson lesson =
                new Lesson(diaSemana, startTimeCalendar, endTimeCalendar, shift, frequency, shift.getExecutionPeriod(), period,
                        space);
        return lesson;
    }

    @Test
    public void testExecutionDegree_find() {
        assertTrue(ExecutionYear.findCurrent(null).getExecutionDegreesSet().size() == 1);
    }

    @Test
    public void testExecutionCourse_findFromCurricularCourse() {
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);
        final CurricularCourse curricularCourse = new CurricularCourse();

        final String uuid = UUID.randomUUID().toString();

        final ExecutionInterval firstInterval = executionYear.getChildInterval(1, AcademicPeriod.SEMESTER);
        final ExecutionCourse firstExecutionCourse = new ExecutionCourse(uuid + "1", uuid + "1", firstInterval);
        curricularCourse.addAssociatedExecutionCourses(firstExecutionCourse);

        final ExecutionInterval secondInterval = executionYear.getChildInterval(2, AcademicPeriod.SEMESTER);
        final ExecutionCourse secondExecutionCourse = new ExecutionCourse(uuid + "2", uuid + "2", secondInterval);
        curricularCourse.addAssociatedExecutionCourses(secondExecutionCourse);

        assertEquals(curricularCourse.getAssociatedExecutionCoursesSet().size(), 2);

        assertEquals(curricularCourse.findExecutionCourses(firstInterval).count(), 1);
        assertEquals(curricularCourse.findExecutionCourses(firstInterval).findAny().get(), firstExecutionCourse);

        assertEquals(curricularCourse.findExecutionCourses(secondInterval).count(), 1);
        assertEquals(curricularCourse.findExecutionCourses(secondInterval).findAny().get(), secondExecutionCourse);

        assertEquals(curricularCourse.findExecutionCourses(executionYear).count(), 2);
        assertTrue(curricularCourse.findExecutionCourses(executionYear).anyMatch(ec -> ec == firstExecutionCourse));
        assertTrue(curricularCourse.findExecutionCourses(executionYear).anyMatch(ec -> ec == secondExecutionCourse));
    }

    @Test
    public void testShift_findByCourseLoad() {
        final CourseLoadType theoreticalLoad = CourseLoadType.of(CourseLoadType.THEORETICAL);
        final CourseLoadType seminarLoad = CourseLoadType.of(CourseLoadType.SEMINAR);

        assertTrue(executionCourse.findShiftsByLoadType(theoreticalLoad).findAny().isPresent());
        assertTrue(executionCourse.findShiftsByLoadType(theoreticalLoad).anyMatch(s -> s == shift));

        assertTrue(executionCourse.findShiftsByLoadType(seminarLoad).findAny().isEmpty());
    }

    @Test
    public void testShift_courseLoadTotalHours() {
        assertEquals(shift.getCourseLoadTotalHours(), new BigDecimal("30.0"));
    }

    @Test
    public void testShift_totalHours() {
        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2023, 9, 15, 0, 0), new DateTime(2023, 12, 22, 0, 0))).iterator();
        final OccupationPeriod occupationPeriod = createDefaultOccupationPeriod(intervals);

        Shift shift = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T-test-total-hours");

        Lesson lesson3h00m = createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(13, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        Lesson lesson1h20m =
                createLesson(shift, WeekDay.TUESDAY, new LocalTime(10, 0), new LocalTime(11, 20), FrequencyType.WEEKLY,
                        occupationPeriod, null);

        assertEquals(lesson3h00m.getLessonDates().size(), 13);
        assertEquals(lesson1h20m.getLessonDates().size(), 14);

        assertEquals(lesson3h00m.getUnitHours(), new BigDecimal("3.00"));
        assertEquals(lesson1h20m.getUnitHours(), new BigDecimal("1.33"));

        assertEquals(lesson3h00m.getTotalHours(), new BigDecimal("39.00"));
        assertEquals(lesson1h20m.getTotalHours(), new BigDecimal("18.67"));

        assertEquals(shift.getTotalHours(), new BigDecimal("57.67"));
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testShift_sameNameOnCreation() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Shift.with.this.name.already.exists");

        new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T1");
        new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T1");
    }

    @Test
    public void testShift_sameNameOnEdit() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Shift.with.this.name.already.exists");

        new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T1");
        final Shift shift2 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T2");

        shift2.edit(shift2.getCourseLoadType(), "T1", null, null);
    }

    @Test
    public void testShift_nameGeneration() {
        assertEquals(shift.getName(), "CAT01");

        final Shift shiftNameCustom1 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "CAPL01");
        assertEquals(shiftNameCustom1.getName(), "CAPL01");

        final Shift shiftNameCustom2 =
                new Shift(executionCourse, CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY), 10, "Custom Name");
        assertEquals(shiftNameCustom2.getName(), "Custom Name");

        final Shift shift1 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY), 10, null);
        assertEquals(shift1.getName(), "CAPL02");

        final Shift shift2 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        assertEquals(shift.getName(), "CAT01"); // ensure it's not changed anymore
        assertEquals(shift2.getName(), "CAT02");

        final Shift shift3 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        assertEquals(shift3.getName(), "CAT03");
    }

    @Test
    public void testLesson_datesWithPeriodChange() {

        new Holiday(new Partial(new LocalDate(2023, 10, 9)));

//        Space space = new Space(new Information.Builder().classification(classification).name("Room 1").build());
        Space space = null;

        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2023, 9, 15, 0, 0), new DateTime(2023, 12, 15, 0, 0))).iterator();
        final OccupationPeriod occupationPeriod = createDefaultOccupationPeriod(intervals);
        final Lesson lesson =
                createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                        occupationPeriod, space);

        assertEquals(lesson.getAllLessonIntervals().size(), 12);

        final Set<LocalDate> originalDates = lesson.getLessonDates();

        assertEquals(originalDates.size(), 12);
        assertTrue(originalDates.contains(new LocalDate(2023, 9, 18)));
        assertTrue(originalDates.contains(new LocalDate(2023, 9, 25)));
        assertTrue(originalDates.contains(new LocalDate(2023, 10, 2)));
        assertFalse(originalDates.contains(new LocalDate(2023, 10, 9))); // holiday
        assertTrue(originalDates.contains(new LocalDate(2023, 10, 16)));
        assertTrue(originalDates.contains(new LocalDate(2023, 10, 23)));
        assertTrue(originalDates.contains(new LocalDate(2023, 10, 30)));
        assertTrue(originalDates.contains(new LocalDate(2023, 11, 6)));
        assertTrue(originalDates.contains(new LocalDate(2023, 11, 13)));
        assertTrue(originalDates.contains(new LocalDate(2023, 11, 20)));
        assertTrue(originalDates.contains(new LocalDate(2023, 11, 27)));
        assertTrue(originalDates.contains(new LocalDate(2023, 12, 4)));
        assertTrue(originalDates.contains(new LocalDate(2023, 12, 11)));

        int year = 2023;
        final Interval interval1 = new Interval(new DateTime(year, 9, 20, 0, 0), new DateTime(year, 10, 31, 23, 59));
        final Interval interval2 = new Interval(new DateTime(year, 11, 13, 0, 0), new DateTime(year, 11, 20, 23, 59));
        final Interval interval3 = new Interval(new DateTime(year, 12, 5, 0, 0), new DateTime(year, 12, 20, 23, 59));
        occupationPeriod.editDates(List.of(interval1, interval2, interval3).iterator());

        assertEquals(lesson.getAllLessonIntervals().size(), 9);

        final Set<LocalDate> newDates = lesson.getLessonDates();

        assertEquals(newDates.size(), 9);
        assertFalse(newDates.contains(new LocalDate(2023, 9, 18)));
        assertTrue(newDates.contains(new LocalDate(2023, 9, 25)));
        assertTrue(newDates.contains(new LocalDate(2023, 10, 2)));
        assertFalse(newDates.contains(new LocalDate(2023, 10, 9)));  // holiday
        assertTrue(newDates.contains(new LocalDate(2023, 10, 16)));
        assertTrue(newDates.contains(new LocalDate(2023, 10, 23)));
        assertTrue(newDates.contains(new LocalDate(2023, 10, 30)));
        assertFalse(newDates.contains(new LocalDate(2023, 11, 6)));
        assertTrue(newDates.contains(new LocalDate(2023, 11, 13)));
        assertTrue(newDates.contains(new LocalDate(2023, 11, 20)));
        assertFalse(newDates.contains(new LocalDate(2023, 11, 27)));
        assertFalse(newDates.contains(new LocalDate(2023, 12, 4)));
        assertTrue(newDates.contains(new LocalDate(2023, 12, 11)));
        assertTrue(newDates.contains(new LocalDate(2023, 12, 18)));

//        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 20, 10, 15), new DateTime(year, 11, 20, 10, 30))));
//        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));

        lesson.createAllLessonInstances();
        assertNull(lesson.getPeriod());
        assertEquals(lesson.getLessonInstancesSet().size(), 9);
        assertEquals(lesson.getAllLessonIntervals().size(), 9);

        final Set<LocalDate> datesAfterInstancesCreation = lesson.getLessonDates();
        assertEquals(datesAfterInstancesCreation.size(), 9);
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 9, 25)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 10, 2)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 10, 16)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 10, 23)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 10, 30)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 11, 13)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 11, 20)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 12, 11)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 12, 18)));

//        assertNull(lesson.getLessonSpaceOccupation());
//        assertTrue(lesson.getSpaces().findAny().get() == space);

//        assertTrue(lesson.getLessonInstancesSet().stream().allMatch(li -> li.getSpaces().findAny().get() == space));
//        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 20, 10, 15), new DateTime(year, 11, 20, 10, 30))));
//        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));

//        new LessonInstance(lesson, new YearMonthDay(2023, 12, 17)); TODO ERROR: invalid date!

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.lessonInstance.already.exist");
        new LessonInstance(lesson, new YearMonthDay(2023, 12, 18));
    }

    @Test
    public void testLesson_datesBiweekly() {

        new Holiday(new Partial(new LocalDate(2023, 10, 9)));
//        Bennu.getInstance().getHolidaysSet().forEach(Holiday::delete);

//        Space space = new Space(new Information.Builder().classification(classification).name("Room 2").build());
        Space space = null;

        int year = 2023;
        final Interval interval1 = new Interval(new DateTime(year, 9, 20, 0, 0), new DateTime(year, 10, 31, 23, 59));
        final Interval interval2 = new Interval(new DateTime(year, 11, 13, 0, 0), new DateTime(year, 11, 20, 23, 59));
        final Interval interval3 = new Interval(new DateTime(year, 12, 5, 0, 0), new DateTime(year, 12, 20, 23, 59));

        final OccupationPeriod occupationPeriod =
                createDefaultOccupationPeriod(List.of(interval1, interval2, interval3).iterator());
        Lesson lesson = createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.BIWEEKLY,
                occupationPeriod, space);

        final Set<LocalDate> lessonDates = lesson.getLessonDates();

//        lessonDates.forEach(date -> System.out.println(">> " + date.toString()));

        assertEquals(lessonDates.size(), 4);
        assertTrue(lessonDates.contains(new LocalDate(2023, 9, 25)));
        assertTrue(lessonDates.contains(new LocalDate(2023, 10, 23)));
        assertTrue(lessonDates.contains(new LocalDate(2023, 11, 13)));
        assertTrue(lessonDates.contains(new LocalDate(2023, 12, 11)));

        // FIXME: with holiday, dates should be 5: 2023-09-25, 2023-10-09, 2023-10-23, 2023-11-13, 2023-12-11

//        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 13, 10, 15), new DateTime(year, 11, 13, 10, 30))));
//        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));

        lesson.createAllLessonInstances();
        assertNull(lesson.getPeriod());
        assertEquals(lesson.getLessonInstancesSet().size(), 4);

        final Set<LocalDate> datesAfterInstancesCreation = lesson.getLessonDates();
        assertEquals(datesAfterInstancesCreation.size(), 4);
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 9, 25)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 10, 23)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 11, 13)));
        assertTrue(datesAfterInstancesCreation.contains(new LocalDate(2023, 12, 11)));

//        assertNull(lesson.getLessonSpaceOccupation());
//        assertTrue(lesson.getSpaces().findAny().get() == space);
//
//        assertTrue(lesson.getLessonInstancesSet().stream().allMatch(li -> li.getSpaces().findAny().get() == space));
//        assertFalse(space.isFree(new Interval(new DateTime(year, 11, 13, 10, 15), new DateTime(year, 11, 13, 10, 30))));
//        assertTrue(space.isFree(new Interval(new DateTime(year, 11, 27, 10, 15), new DateTime(year, 11, 27, 10, 30))));
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

        final Set<LocalDate> newDates = lesson.getLessonDates();
        assertEquals(newDates.size(), 9);

        new LessonInstance(lesson, new YearMonthDay(2023, 10, 23));
        new LessonInstance(lesson, new YearMonthDay(2023, 11, 13));
        new LessonInstance(lesson, new YearMonthDay(2023, 12, 11));

        assertEquals(lesson.getLessonInstancesSet().size(), 3);

        final Set<LocalDate> datesAfterInstancesCreation = lesson.getLessonDates();
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
    public void testLesson_noValidDates() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Lesson.create.noValidDates");

        new Holiday(new Partial(new LocalDate(2023, 10, 5)));

        int year = 2023;
        final Interval interval1 = new Interval(new DateTime(year, 10, 5, 0, 0), new DateTime(year, 10, 5, 23, 59));

        final OccupationPeriod occupationPeriod = createDefaultOccupationPeriod(List.of(interval1).iterator());
        createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY, occupationPeriod,
                null);
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

        List<Lesson> sortedLessons =
                Stream.of(lesson3, lesson4, lesson1, lesson2).sorted(Lesson.LESSON_COMPARATOR_BY_WEEKDAY_AND_STARTTIME)
                        .collect(Collectors.toList());

        assertEquals(sortedLessons.get(0), lesson1);
        assertEquals(sortedLessons.get(1), lesson2);
        assertEquals(sortedLessons.get(2), lesson3);
        assertEquals(sortedLessons.get(3), lesson4);
    }

    @Test
    public void testShift_presentationName() {
        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2023, 9, 15, 0, 0), new DateTime(2023, 12, 15, 0, 0))).iterator();
        final OccupationPeriod occupationPeriod = createDefaultOccupationPeriod(intervals);

//        Space spaceX = new Space(new Information.Builder().classification(classification).name("Room X").build());
//        Space spaceY = new Space(new Information.Builder().classification(classification).name("Room Y").build());
        Space spaceX = null;
        Space spaceY = null;

        Shift shift1 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T100");

        Lesson lesson3 = createLesson(shift1, WeekDay.FRIDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        Lesson lesson4 = createLesson(shift1, WeekDay.FRIDAY, new LocalTime(11, 0), new LocalTime(12, 0), FrequencyType.WEEKLY,
                occupationPeriod, spaceX);
        Lesson lesson1 = createLesson(shift1, WeekDay.WEDNESDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, null);
        Lesson lesson2 = createLesson(shift1, WeekDay.THURSDAY, new LocalTime(10, 0), new LocalTime(11, 0), FrequencyType.WEEKLY,
                occupationPeriod, spaceY);
        Lesson lessonExtra =
                createLesson(shift1, WeekDay.SATURDAY, new LocalTime(12, 0), new LocalTime(13, 0), FrequencyType.WEEKLY,
                        occupationPeriod, spaceY);
        lessonExtra.setExtraLesson(true);

        Shift shift2 = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "T101");

//        System.out.println(shift2.getPresentationName());
        assertEquals(shift1.getPresentationName(),
                "T100 (Wed. 10:00-11:00; Thu. 10:00-11:00; Fri. 10:00-11:00; Fri. 11:00-12:00)");
        assertEquals(shift2.getPresentationName(), "T101");
    }

    @Test
    public void testShift_deleteCourseLoadDuration() {

        final CompetenceCourse competenceCourse = CompetenceCourse.find(COURSE_A_CODE);

        ExecutionYear nextYear = (ExecutionYear) ExecutionInterval.findCurrentAggregator(null).getNext();
        final ExecutionInterval nextInterval = nextYear.getFirstExecutionPeriod();

        final ExecutionCourse nextExecutionCourse =
                new ExecutionCourse(competenceCourse.getName(), competenceCourse.getCode(), nextInterval);
        nextExecutionCourse.addAssociatedCurricularCourses(executionCourse.getAssociatedCurricularCoursesSet().iterator().next());

        Shift nextExecutionCourseShiftT = new Shift(nextExecutionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        Shift nextExecutionCourseShiftTP =
                new Shift(nextExecutionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL), 10, null);
        // TODO test if course load type is valid for execution course!

        CompetenceCourseInformation nextInformation = competenceCourse.findInformationMostRecentUntil(nextInterval);

        assertThrows(() -> nextInformation.findLoadDurationByType(CourseLoadType.of(CourseLoadType.THEORETICAL))
                .ifPresent(d -> d.delete()), DomainException.class, "error.CourseLoadDuration.delete.shiftsExistsForDuration");

        assertThrows(() -> nextInformation.findLoadDurationByType(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL))
                .ifPresent(d -> d.delete()), DomainException.class, "error.CourseLoadDuration.delete.shiftsExistsForDuration");

        assertThrows(() -> nextInformation.delete(), DomainException.class,
                "error.CourseLoadDuration.delete.shiftsExistsForDuration");

        nextExecutionCourseShiftTP.delete();

        assertThrows(() -> nextInformation.delete(), DomainException.class,
                "error.CourseLoadDuration.delete.shiftsExistsForDuration");
    }

    /*
     * HACK until not using JUnit5 (Assertions.assertThrows(DomainException.class, () -> {])
     */
    private void assertThrows(Runnable codeToRun, Class<? extends Exception> exceptionClass, String exceptionMessage) {
        try {
            codeToRun.run();
        } catch (Exception e) {
            if (!e.getClass().equals(exceptionClass)) {
                throw e;
            }
            if (exceptionMessage != null && !exceptionMessage.equals(e.getMessage())) {
                throw e;
            }
        }
    }

    @Test
    public void testSchoolClass_find() {
        final ExecutionInterval executionInterval = executionDegree.getExecutionYear().getFirstExecutionPeriod();
        assertTrue(SchoolClass.findBy(executionDegree, executionInterval, 1).findAny().isPresent());
        assertTrue(SchoolClass.findBy(executionDegree, executionInterval, 2).findAny().isEmpty());
        assertEquals(SchoolClass.findBy(executionDegree, executionInterval, 1).count(), 2);
    }
}
