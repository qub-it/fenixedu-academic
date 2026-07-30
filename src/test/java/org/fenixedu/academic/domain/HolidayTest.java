package org.fenixedu.academic.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class HolidayTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            newMonthDayHoliday(12, 25); // Christmas
            return null;
        });
    }

    private static Holiday newMonthDayHoliday(int month, int day) {
        return new Holiday(new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.monthOfYear(), DateTimeFieldType.dayOfMonth() },
                new int[] { month, day }));
    }

    @Test
    public void isHoliday_partialWithoutYear_matchesAnyYear() {
        // Christmas should match regardless of the year
        assertTrue(Holiday.isHoliday(new LocalDate(2024, 12, 25)));
        assertTrue(Holiday.isHoliday(new LocalDate(2025, 12, 25)));
        assertTrue(Holiday.isHoliday(new LocalDate(2026, 12, 25)));
        // a different date should not match
        assertFalse(Holiday.isHoliday(new LocalDate(2024, 1, 1)));
    }

    @Test
    public void isHoliday_withFullDatePartial_onlyMatchesThatDate() {
        // Holiday with a full date (year+month+day) should only match that exact date
        new Holiday(new Partial(new LocalDate(2025, 1, 15)));

        assertTrue(Holiday.isHoliday(new LocalDate(2025, 1, 15)));
        // same month/day but different year should not match
        assertFalse(Holiday.isHoliday(new LocalDate(2024, 1, 15)));
        // day before should not match either
        assertFalse(Holiday.isHoliday(new LocalDate(2025, 1, 14)));
    }

    @Test
    public void isHoliday_afterDeletion_returnsFalse() {
        final Holiday mayDay = newMonthDayHoliday(5, 1);

        assertTrue(Holiday.isHoliday(new LocalDate(2024, 5, 1)));

        mayDay.delete();
        assertFalse(Holiday.isHoliday(new LocalDate(2024, 5, 1)));
    }

    @Test
    public void isHoliday_withMultipleHolidays_identifiesCorrectly() {
        newMonthDayHoliday(4, 1); // add a second holiday
        assertTrue(Holiday.isHoliday(new LocalDate(2024, 12, 25)));
        assertTrue(Holiday.isHoliday(new LocalDate(2024, 4, 1)));
    }

    @Test
    public void isHoliday_partialDayOnly_matchesEveryMonth() {
        // Holiday defined only by day of month (no month, no year) should match every month
        new Holiday(new Partial(DateTimeFieldType.dayOfMonth(), 1));

        assertTrue(Holiday.isHoliday(new LocalDate(2024, 1, 1)));
        assertTrue(Holiday.isHoliday(new LocalDate(2024, 2, 1)));
        assertTrue(Holiday.isHoliday(new LocalDate(2024, 3, 1)));
        // any other day of the month should not match
        assertFalse(Holiday.isHoliday(new LocalDate(2024, 1, 2)));
    }

}
