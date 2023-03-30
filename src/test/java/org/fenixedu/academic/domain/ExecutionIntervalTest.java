package org.fenixedu.academic.domain;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionIntervalTest {

    @Test
    public void isThisWorking() {
       // assertTrue("Hello".length() == 5);

        AcademicCalendarRootEntry rootEntry =
                new AcademicCalendarRootEntry(new LocalizedString().with(Locale.getDefault(), "Root entry"), null);

        assertTrue("Root entry".equals(rootEntry.getTitle().getContent()));

        AcademicYearCE academicYearEntryFirst =
                new AcademicYearCE(rootEntry, new LocalizedString().with(Locale.getDefault(), "2019/2020"), null,
                        new LocalDate(2019, 9, 1).toDateTimeAtStartOfDay(), new LocalDate(2020, 8, 30).toDateTimeAtStartOfDay(),
                        rootEntry);

        AcademicYearCE academicYearEntrySecond =
                new AcademicYearCE(rootEntry, new LocalizedString().with(Locale.getDefault(), "2020/2021"), null,
                        new LocalDate(2020, 9, 1).toDateTimeAtStartOfDay(), new LocalDate(2021, 8, 30).toDateTimeAtStartOfDay(),
                        rootEntry);

        assertTrue(academicYearEntryFirst.getExecutionInterval() != null);
        assertTrue(academicYearEntrySecond.getExecutionInterval() != null);
        assertTrue(academicYearEntryFirst.getExecutionInterval().isBefore(academicYearEntrySecond.getExecutionInterval()));
        assertTrue(ExecutionYear.findAllAggregators().size() == 2);
    }

}
