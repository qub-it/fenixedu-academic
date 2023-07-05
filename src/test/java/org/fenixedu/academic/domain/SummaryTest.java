package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SummaryTest {

    private static LocalizedString defaultTitle;
    private static LocalizedString defaultText;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    private static OccupationPeriod occupationPeriod;
    private static Shift shift;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initExecutionsAndSchedules();
            return null;
        });
    }

    private static void initExecutionsAndSchedules() {
        ExecutionsAndSchedulesTest.initExecutions();
        ExecutionsAndSchedulesTest.initSchedules();

        shift = Bennu.getInstance().getShiftsSet().iterator().next();

        Iterator<Interval> intervals =
                List.of(new Interval(new DateTime(2023, 5, 1, 0, 0), new DateTime(2023, 5, 31, 0, 0))).iterator();

        occupationPeriod = ExecutionsAndSchedulesTest.createDefaultOccupationPeriod(intervals);

        defaultTitle = new LocalizedString.Builder().with(Locale.getDefault(), "Title").build();
        defaultText = new LocalizedString.Builder().with(Locale.getDefault(), "Text").build();
    }

    @Test
    public void testSummary_lessons() {
        Lesson lesson = ExecutionsAndSchedulesTest.createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0),
                FrequencyType.WEEKLY, occupationPeriod, null);

        assertEquals(lesson.getLessonInstancesSet().size(), 0);
        assertEquals(lesson.getAssociatedSummaries().size(), 0);

        new Summary(defaultTitle, defaultText, null, null, "Teacher A", lesson, new LocalDate(2023, 5, 15));
        assertEquals(lesson.getLessonInstancesSet().size(), 5);
        assertEquals(lesson.getAssociatedSummaries().size(), 1);

        new Summary(defaultTitle, defaultText, null, null, "Teacher A", lesson, new LocalDate(2023, 5, 22));
        assertEquals(lesson.getLessonInstancesSet().size(), 5);
        assertEquals(lesson.getAssociatedSummaries().size(), 2);
    }

    @Test
    public void testSummary_invalidDate() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.summary.no.valid.date.to.lesson");

        Lesson lesson = ExecutionsAndSchedulesTest.createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0),
                FrequencyType.WEEKLY, occupationPeriod, null);

        new Summary(defaultTitle, defaultText, null, null, "Teacher A", lesson, new LocalDate(2023, 5, 16));
    }
}
