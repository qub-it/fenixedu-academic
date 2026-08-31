package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.YearMonthDay;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class LessonTest {

    private static Shift shift;

    private OccupationPeriod occupationPeriod;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initTestData();
            return null;
        });
    }

    @Before
    public void setUp() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Iterator<Interval> intervals =
                    List.of(new Interval(new DateTime(2023, 9, 15, 0, 0), new DateTime(2023, 12, 15, 0, 0))).iterator();
            occupationPeriod = ExecutionsAndSchedulesTest.createDefaultOccupationPeriod(intervals);
            return null;
        });
    }

    private static void initTestData() {
        ExecutionsAndSchedulesTest.init();
        shift = Bennu.getInstance().getShiftsSet().iterator().next();
    }

    private Lesson createLesson(WeekDay weekDay, LocalTime startTime, LocalTime endTime) {
        return Lesson.create(shift, weekDay, startTime, endTime, FrequencyType.WEEKLY, occupationPeriod, null);
    }

    private Lesson createLesson(WeekDay weekDay, LocalTime startTime, LocalTime endTime, Space space) {
        return Lesson.create(shift, weekDay, startTime, endTime, FrequencyType.WEEKLY, occupationPeriod, space);
    }

    @Test
    public void testLesson_delete() {
        Lesson lesson = createLesson(WeekDay.WEDNESDAY, new LocalTime(14, 0), new LocalTime(15, 0));

        assertNotNull(lesson);
        assertNotNull(lesson.getPeriod());

        lesson.delete();

        assertNull(lesson.getPeriod());
    }

    @Test
    public void testLesson_deleteWithSummaries_throwsException() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.deleteLesson.with.summaries");

        LocalizedString title = new LocalizedString.Builder().with(Locale.getDefault(), "Title").build();
        LocalizedString text = new LocalizedString.Builder().with(Locale.getDefault(), "Text").build();

        Lesson lesson = createLesson(WeekDay.THURSDAY, new LocalTime(14, 0), new LocalTime(15, 0));

        new Summary(title, text, null, null, "Teacher A", lesson, new LocalDate(2023, 10, 19));

        assertFalse(lesson.getAssociatedSummaries().isEmpty());

        lesson.delete();
    }

    @Test
    public void testLesson_getPresentationName() {
        Lesson lesson = createLesson(WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0));

        String name = lesson.getPresentationName();
        assertNotNull(name);
        assertTrue(name.contains("10:00"));
        assertTrue(name.contains("11:00"));
    }

    @Test
    public void testLesson_getPresentationName_withSpace() {
        Space space = new Space(new org.fenixedu.spaces.domain.Information.Builder().classification(
                        new org.fenixedu.spaces.domain.SpaceClassification("ROOM", new org.fenixedu.commons.i18n.LocalizedString()))
                .name("Room 101").validFrom(DateTime.now().minusDays(1)).build());

        Lesson lesson = createLesson(WeekDay.FRIDAY, new LocalTime(9, 0), new LocalTime(10, 0), space);

        String name = lesson.getPresentationName();
        assertNotNull(name);
        assertTrue(name.contains("09:00"));
        assertTrue(name.contains("10:00"));
        assertTrue(name.contains("Room 101"));
    }

    @Test
    public void testLesson_getLessonInstanceFor() {
        Lesson lesson = createLesson(WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0));

        assertNull(lesson.getLessonInstanceFor(new YearMonthDay(2023, 10, 23)));

        lesson.createAllLessonInstances();

        LessonInstance instance = lesson.getLessonInstanceFor(new YearMonthDay(2023, 10, 23));
        assertNotNull(instance);
        assertEquals(new YearMonthDay(2023, 10, 23), instance.getDay());

        assertNull(lesson.getLessonInstanceFor(new YearMonthDay(2023, 10, 22)));
    }

    @Test
    public void testLesson_setBeginHourMinuteSecond_stripsSeconds() {
        Lesson lesson = createLesson(WeekDay.MONDAY, new LocalTime(10, 0, 30), new LocalTime(11, 0, 45));

        assertEquals(new HourMinuteSecond(10, 0, 0), lesson.getBeginHourMinuteSecond());
        assertEquals(new HourMinuteSecond(11, 0, 0), lesson.getEndHourMinuteSecond());
    }

    @Test
    public void testLesson_getUnitHours() {
        Lesson lesson1 = createLesson(WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0));
        assertEquals(new BigDecimal("1.00"), lesson1.getUnitHours());

        Lesson lesson2 = createLesson(WeekDay.TUESDAY, new LocalTime(10, 0), new LocalTime(11, 20));
        assertEquals(new BigDecimal("1.33"), lesson2.getUnitHours());
    }

    @Test
    public void testLesson_getTotalHours() {
        Lesson lesson = createLesson(WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0));

        int lessonDatesCount = lesson.getLessonDates().size();
        assertEquals(new BigDecimal(String.valueOf(lessonDatesCount)).setScale(2, RoundingMode.HALF_UP), lesson.getTotalHours());
    }

    @Test
    public void testLesson_getTotalDuration() {
        Lesson lesson = createLesson(WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 30));

        assertEquals(Duration.standardMinutes(90), lesson.getTotalDuration());
    }
}
