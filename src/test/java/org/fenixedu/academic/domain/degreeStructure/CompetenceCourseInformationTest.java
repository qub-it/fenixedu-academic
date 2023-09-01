package org.fenixedu.academic.domain.degreeStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseInformationTest {

    private static CompetenceCourse competenceCourseA;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            CompetenceCourseTest.initCompetenceCourse();
            competenceCourseA = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);
            return null;
        });
    }

    @Test
    public void testCompetenceCourseInformation_find() {
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final ExecutionInterval previousInterval = currentInterval.getPrevious();
        final ExecutionInterval nextInterval = currentInterval.getNext();
        final ExecutionInterval nextNextInterval = nextInterval.getNext();

        assertNotNull(currentInterval);
        assertNotNull(previousInterval);
        assertNotNull(nextInterval);
        assertNotNull(nextNextInterval);

        final CompetenceCourseInformation currentInformation = competenceCourseA.findInformationMostRecentUntil(currentInterval);
        final CompetenceCourseInformation previousInformation =
                competenceCourseA.findInformationMostRecentUntil(previousInterval);
        final CompetenceCourseInformation nextInformation = competenceCourseA.findInformationMostRecentUntil(nextInterval);
        final CompetenceCourseInformation nextNextInformation =
                competenceCourseA.findInformationMostRecentUntil(nextNextInterval);

        assertNotNull(currentInformation);
        assertNotNull(previousInformation);
        assertNotNull(nextInformation);
        assertNotNull(nextNextInformation);

        assertEquals(currentInformation, previousInformation);
        assertEquals(currentInformation, nextInformation);
        assertNotEquals(currentInformation, nextNextInformation);

        assertTrue(currentInformation.findNext().isPresent());
        assertEquals(currentInformation.findNext().get(), nextNextInformation);
        assertFalse(currentInformation.findPrevious().isPresent());

        assertTrue(nextNextInformation.findPrevious().isPresent());
        assertEquals(nextNextInformation.findPrevious().get(), currentInformation);
        assertFalse(nextNextInformation.findNext().isPresent());
    }

    @Test
    public void testCompetenceCourseInformation_executionIntervalsRange() {

        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final ExecutionInterval nextInterval = ExecutionYear.readExecutionYearByName("2021/2022").getFirstExecutionPeriod();

        final CompetenceCourseInformation currentInformation = competenceCourseA.findInformationMostRecentUntil(currentInterval);
        final CompetenceCourseInformation nextNextInformation = competenceCourseA.findInformationMostRecentUntil(nextInterval);

        final ExecutionYear executionYear_20_21 = ExecutionYear.readExecutionYearByName("2020/2021");
        final ExecutionYear executionYear_21_22 = ExecutionYear.readExecutionYearByName("2021/2022");
        final ExecutionYear executionYear_22_23 = ExecutionYear.readExecutionYearByName("2022/2023");

        final Set<ExecutionInterval> currentInformationExecutionIntervalsRange =
                currentInformation.getExecutionIntervalsRange().collect(Collectors.toSet());
        assertEquals(currentInformationExecutionIntervalsRange.size(), 2);
        assertTrue(
                currentInformationExecutionIntervalsRange.stream().allMatch(ei -> ei.getExecutionYear() == executionYear_20_21));

        final Set<ExecutionInterval> nextInformationExecutionIntervalsRange =
                nextNextInformation.getExecutionIntervalsRange().collect(Collectors.toSet());
        assertEquals(nextInformationExecutionIntervalsRange.size(), 4);
        assertTrue(nextInformationExecutionIntervalsRange.stream()
                .allMatch(ei -> ei.getExecutionYear() == executionYear_21_22 || ei.getExecutionYear() == executionYear_22_23));
    }

}
