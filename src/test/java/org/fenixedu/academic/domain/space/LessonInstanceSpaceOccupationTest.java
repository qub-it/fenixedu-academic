package org.fenixedu.academic.domain.space;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionsAndSchedulesTest;
import org.fenixedu.academic.domain.FrequencyType;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.util.WeekDay;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Information;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.domain.SpaceClassification;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import org.joda.time.YearMonthDay;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class LessonInstanceSpaceOccupationTest {

    @Before
    public void setUp() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionsAndSchedulesTest.initExecutions();
            return null;
        });
    }

    @Test
    public void testYearsIndexUpdatedWhenLessonInstanceAdded() {
        List<Interval> intervals2020_2021 = createIntervals(2020, 2021);
        List<Interval> intervals2021_2022 = createIntervals(2021, 2022);
        List<Interval> intervals2022_2023 = createIntervals(2022, 2023);
        List<Interval> intervals2023_2024 = createIntervals(2023, 2024);
        List<Interval> intervals2024_2025 = createIntervals(2024, 2025);
        List<Interval> intervals2025_2026 = createIntervals(2025, 2026);

        SpaceClassification classification = new SpaceClassification("ROOM", new LocalizedString(Locale.getDefault(), "Room"));
        classification.setIsAllocatable(true);

        // ExecutionCourse executionCourse = Bennu.getInstance().getExecutionCoursesSet().iterator().next();
        ExecutionCourse executionCourse = new ExecutionCourse("Course Name", "CN", ExecutionInterval.findFirstCurrentChild(null));

        Shift shift = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);

        // a big interval, from 2022 to 2024, to ensure we support years gaps in the index.
        Iterator<Interval> lessonIntervals = createIntervals(2022, 2024).iterator();
        OccupationPeriod occupationPeriod = ExecutionsAndSchedulesTest.createDefaultOccupationPeriod(lessonIntervals);

        Space lessonRoom = new Space(new Information.Builder().classification(classification).name("Lesson Room")
                .validFrom(DateTime.now().minusDays(1)).build());

        Lesson lesson = ExecutionsAndSchedulesTest.createLesson(shift, WeekDay.MONDAY, new LocalTime(10, 0), new LocalTime(11, 0),
                FrequencyType.WEEKLY, occupationPeriod, lessonRoom);

        LessonSpaceOccupation lessonSpaceOccupation = lesson.getLessonSpaceOccupation();
        assertNotNull(lessonSpaceOccupation);
        String lessonSpaceOccupationYearsIndex = lessonSpaceOccupation.getYearsIndex();
        assertFalse(lessonSpaceOccupationYearsIndex.contains("2021"));
        assertTrue(lessonSpaceOccupationYearsIndex.contains("2022"));
        assertTrue(lessonSpaceOccupationYearsIndex.contains("2023"));
        assertTrue(lessonSpaceOccupationYearsIndex.contains("2024"));
        assertFalse(lessonSpaceOccupationYearsIndex.contains("2025"));
        assertFalse(lessonSpaceOccupation.overlaps(intervals2020_2021));
        assertTrue(lessonSpaceOccupation.overlaps(intervals2021_2022));
        assertTrue(lessonSpaceOccupation.overlaps(intervals2022_2023));
        assertTrue(lessonSpaceOccupation.overlaps(intervals2023_2024));
        assertTrue(lessonSpaceOccupation.overlaps(intervals2024_2025));
        assertFalse(lessonSpaceOccupation.overlaps(intervals2025_2026));
        assertTrue(lessonRoom.isFree(intervals2020_2021));
        assertFalse(lessonRoom.isFree(intervals2021_2022));
        assertTrue(lessonRoom.isFree(intervals2025_2026));

        Space lessonInstanceRoom =
                new Space(new Information.Builder().classification(classification).name("Lesson Instance Room").build());

        LessonInstance instance1 = new LessonInstance(lesson, new YearMonthDay(2023, 9, 1));
        LessonInstanceSpaceOccupation lessonInstanceSpaceOccupation =
                new LessonInstanceSpaceOccupation(lessonInstanceRoom, instance1);

        String lessonInstanceSpaceOccupationYearsIndex = lessonInstanceSpaceOccupation.getYearsIndex();
        assertNotNull(lessonInstanceSpaceOccupationYearsIndex);
        assertFalse(lessonInstanceSpaceOccupationYearsIndex.contains("2022"));
        assertTrue(lessonInstanceSpaceOccupationYearsIndex.contains("2023"));
        assertFalse(lessonInstanceSpaceOccupationYearsIndex.contains("2024"));
        assertFalse(lessonInstanceSpaceOccupation.overlaps(intervals2021_2022));
        assertTrue(lessonInstanceSpaceOccupation.overlaps(intervals2022_2023));
        assertTrue(lessonInstanceSpaceOccupation.overlaps(intervals2023_2024));
        assertFalse(lessonInstanceSpaceOccupation.overlaps(intervals2024_2025));
        assertTrue(lessonInstanceRoom.isFree(intervals2021_2022));
        assertFalse(lessonInstanceRoom.isFree(intervals2022_2023));
        assertTrue(lessonInstanceRoom.isFree(intervals2024_2025));

        LessonInstance lessonInstance2 = new LessonInstance(lesson, new YearMonthDay(2024, 1, 1));
        lessonInstanceSpaceOccupation.add(lessonInstance2);

        lessonInstanceSpaceOccupationYearsIndex = lessonInstanceSpaceOccupation.getYearsIndex();
        assertNotNull(lessonInstanceSpaceOccupationYearsIndex);
        assertFalse(lessonInstanceSpaceOccupationYearsIndex.contains("2022"));
        assertTrue(lessonInstanceSpaceOccupationYearsIndex.contains("2023"));
        assertTrue(lessonInstanceSpaceOccupationYearsIndex.contains("2024"));
        assertFalse(lessonInstanceSpaceOccupationYearsIndex.contains("2025"));
        assertFalse(lessonInstanceSpaceOccupation.overlaps(intervals2021_2022));
        assertTrue(lessonInstanceSpaceOccupation.overlaps(intervals2022_2023));
        assertTrue(lessonInstanceSpaceOccupation.overlaps(intervals2023_2024));
        assertTrue(lessonInstanceSpaceOccupation.overlaps(intervals2024_2025));
        assertFalse(lessonInstanceSpaceOccupation.overlaps(intervals2025_2026));
        assertTrue(lessonInstanceRoom.isFree(intervals2021_2022));
        assertFalse(lessonInstanceRoom.isFree(intervals2024_2025));
        assertTrue(lessonInstanceRoom.isFree(intervals2025_2026));
    }

    private static List<Interval> createIntervals(int startYear, int endYear) {
        return List.of(new Interval(new DateTime(startYear, 1, 1, 0, 0), new DateTime(endYear, 12, 31, 0, 0)));
    }
}
