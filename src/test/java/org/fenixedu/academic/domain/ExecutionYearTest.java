package org.fenixedu.academic.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionYearTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            return null;
        });
    }

    @Test
    public void testExecutionYear_readNotClosedExecutionYears() {
        ExecutionYear executionYearFirst = ExecutionYear.readExecutionYearByName("2019/2020");
        ExecutionYear executionYearCurrent = ExecutionYear.readExecutionYearByName("2020/2021");
        ExecutionYear executionYearLast = ExecutionYear.readExecutionYearByName("2023/2024");

        List<ExecutionYear> notClosedExecutionYears = ExecutionYear.readNotClosedExecutionYears();
        assertTrue(notClosedExecutionYears.containsAll(List.of(executionYearFirst, executionYearCurrent, executionYearLast)));

        executionYearFirst.setState(PeriodState.CLOSED);
        assertFalse(ExecutionYear.readNotClosedExecutionYears().contains(executionYearFirst));
    }

    @Test
    public void testExecutionYear_readExecutionYearByName() {
        assertNull(ExecutionYear.readExecutionYearByName(null));
        assertNull(ExecutionYear.readExecutionYearByName("unknown"));

        ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        assertEquals("2020/2021", executionYear.getName());
    }
}
