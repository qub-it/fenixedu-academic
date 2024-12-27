package org.fenixedu.academic.domain.curricularPeriod;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class DegreeCurricularPlanDurationTest {

    private static CurricularCourse curricularCourse;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            DegreeTest.initDegree();
            return null;
        });
    }

    @Test
    public void createDuration_1year() {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.YEAR);
        populatedCurricularPeriodStructure(dcp);

        final CurricularPeriod y1s1 = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod y1s2 = dcp.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod y2s1 = dcp.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod y2s2 = dcp.getCurricularPeriodFor(2, 2, SEMESTER);
        final CurricularPeriod y3s1 = dcp.getCurricularPeriodFor(3, 1, SEMESTER);
        final CurricularPeriod y3s2 = dcp.getCurricularPeriodFor(3, 2, SEMESTER);

        assertEquals(dcp.getDegreeStructure().getAcademicPeriod(), AcademicPeriod.YEAR);
        assertNull(dcp.getDegreeStructure().getChildOrder());
        assertNotNull(y1s1);
        assertNotNull(y1s2);
        assertNull(y2s1);
        assertNull(y2s2);
        assertNull(y3s1);
        assertNull(y3s2);
    }

    @Test
    public void createDuration_3years() {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(dcp);

        final CurricularPeriod y1s1 = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod y1s2 = dcp.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod y2s1 = dcp.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod y2s2 = dcp.getCurricularPeriodFor(2, 2, SEMESTER);
        final CurricularPeriod y3s1 = dcp.getCurricularPeriodFor(3, 1, SEMESTER);
        final CurricularPeriod y3s2 = dcp.getCurricularPeriodFor(3, 2, SEMESTER);

        assertEquals(dcp.getDegreeStructure().getAcademicPeriod(), AcademicPeriod.THREE_YEAR);
        assertNull(dcp.getDegreeStructure().getChildOrder());
        assertNotNull(y1s1);
        assertNotNull(y1s2);
        assertNotNull(y2s1);
        assertNotNull(y2s2);
        assertNotNull(y3s1);
        assertNotNull(y3s2);
    }

    @Test
    public void editDuration_toNull() {
        final DegreeCurricularPlan degreeCurricularPlan =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(degreeCurricularPlan);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.duration.must.be.specified.in.years");

        degreeCurricularPlan.editDuration(null);
    }

    @Test
    public void editDuration_toNotYear() {
        final DegreeCurricularPlan degreeCurricularPlan =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(degreeCurricularPlan);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.duration.must.be.specified.in.years");

        degreeCurricularPlan.editDuration(SEMESTER);
    }

    @Test
    public void editDuration_directChangeStructure() {
        final DegreeCurricularPlan degreeCurricularPlan =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(degreeCurricularPlan);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.degreeStructure.cannot.be.invoked.publicly");

        degreeCurricularPlan.setDegreeStructure(new CurricularPeriod(AcademicPeriod.TWO_YEAR));
    }

    @Test
    public void editDuration_from3To3Years() {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(dcp);

        final CurricularPeriod beforeStructure = dcp.getDegreeStructure();
        final CurricularPeriod y1s1 = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod y1s2 = dcp.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod y2s1 = dcp.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod y2s2 = dcp.getCurricularPeriodFor(2, 2, SEMESTER);
        final CurricularPeriod y3s1 = dcp.getCurricularPeriodFor(3, 1, SEMESTER);
        final CurricularPeriod y3s2 = dcp.getCurricularPeriodFor(3, 2, SEMESTER);

        final CurricularPeriod y3 = beforeStructure.findChild(AcademicPeriod.YEAR, 3).orElseThrow();

//        System.out.println("BEFORE:");
//        printPeriodStructure(beforeStructure, 0);
        dcp.editDuration(AcademicPeriod.THREE_YEAR);
//        System.out.println("AFTER:");
//        printPeriodStructure(dcp.getDegreeStructure(), 0);

        assertEquals(dcp.getDegreeStructure(), beforeStructure);
        assertEquals(dcp.getDegreeStructure().getAcademicPeriod(), AcademicPeriod.THREE_YEAR);
        assertNull(dcp.getDegreeStructure().getChildOrder());
        assertEquals(y1s1, dcp.getCurricularPeriodFor(1, 1, SEMESTER));
        assertEquals(y1s2, dcp.getCurricularPeriodFor(1, 2, SEMESTER));
        assertEquals(y2s1, dcp.getCurricularPeriodFor(2, 1, SEMESTER));
        assertEquals(y2s2, dcp.getCurricularPeriodFor(2, 2, SEMESTER));
        assertEquals(y3s1, dcp.getCurricularPeriodFor(3, 1, SEMESTER));
        assertEquals(y3s2, dcp.getCurricularPeriodFor(3, 2, SEMESTER));
    }

    @Test
    public void editDuration_from3To2Years() {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(dcp);

        final CurricularPeriod beforeStructure = dcp.getDegreeStructure();
        final CurricularPeriod y1s1 = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod y1s2 = dcp.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod y2s1 = dcp.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod y2s2 = dcp.getCurricularPeriodFor(2, 2, SEMESTER);
        final CurricularPeriod y3s1 = dcp.getCurricularPeriodFor(3, 1, SEMESTER);
        final CurricularPeriod y3s2 = dcp.getCurricularPeriodFor(3, 2, SEMESTER);

//        System.out.println("BEFORE:");
//        printPeriodStructure(beforeStructure, 0);
        dcp.editDuration(AcademicPeriod.TWO_YEAR);
//        System.out.println("AFTER:");
//        printPeriodStructure(dcp.getDegreeStructure(), 0);

        assertEquals(dcp.getDegreeStructure(), beforeStructure);
        assertEquals(dcp.getDegreeStructure().getAcademicPeriod(), AcademicPeriod.TWO_YEAR);
        assertNull(dcp.getDegreeStructure().getChildOrder());
        assertEquals(y1s1, dcp.getCurricularPeriodFor(1, 1, SEMESTER));
        assertEquals(y1s2, dcp.getCurricularPeriodFor(1, 2, SEMESTER));
        assertEquals(y2s1, dcp.getCurricularPeriodFor(2, 1, SEMESTER));
        assertEquals(y2s2, dcp.getCurricularPeriodFor(2, 2, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(3, 1, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(3, 2, SEMESTER));
//        assertFalse(FenixFramework.isDomainObjectValid(y3s1)); // not working..
//        assertFalse(FenixFramework.isDomainObjectValid(y3s2)); // not working..

    }

    @Test
    public void editDuration_from3To1Year() {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        populatedCurricularPeriodStructure(dcp);

        final CurricularPeriod beforeStructure = dcp.getDegreeStructure();
        final CurricularPeriod y1 = beforeStructure.findChild(AcademicPeriod.YEAR, 1).orElseThrow();
        final CurricularPeriod y1s1 = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod y1s2 = dcp.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod y2s1 = dcp.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod y2s2 = dcp.getCurricularPeriodFor(2, 2, SEMESTER);
        final CurricularPeriod y3s1 = dcp.getCurricularPeriodFor(3, 1, SEMESTER);
        final CurricularPeriod y3s2 = dcp.getCurricularPeriodFor(3, 2, SEMESTER);

//        System.out.println("BEFORE:");
//        printPeriodStructure(beforeStructure, 0);
        dcp.editDuration(AcademicPeriod.YEAR);
//        System.out.println("AFTER:");
//        printPeriodStructure(dcp.getDegreeStructure(), 0);

        assertEquals(dcp.getDegreeStructure(), y1);
        assertEquals(dcp.getDegreeStructure().getAcademicPeriod(), AcademicPeriod.YEAR);
        assertNull(dcp.getDegreeStructure().getChildOrder());
        assertEquals(y1s1, dcp.getCurricularPeriodFor(1, 1, SEMESTER));
        assertEquals(y1s2, dcp.getCurricularPeriodFor(1, 2, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(2, 1, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(2, 2, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(3, 1, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(3, 2, SEMESTER));
    }

    @Test
    public void editDuration_from1To3Years() {
        final DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(Degree.find(DEGREE_A_CODE), UUID.randomUUID().toString(), AcademicPeriod.YEAR);
        populatedCurricularPeriodStructure(dcp);

        final CurricularPeriod beforeStructure = dcp.getDegreeStructure();
        final CurricularPeriod y1s1 = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod y1s2 = dcp.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod y2s1 = dcp.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod y2s2 = dcp.getCurricularPeriodFor(2, 2, SEMESTER);
        final CurricularPeriod y3s1 = dcp.getCurricularPeriodFor(3, 1, SEMESTER);
        final CurricularPeriod y3s2 = dcp.getCurricularPeriodFor(3, 2, SEMESTER);

        assertNotNull(y1s1);
        assertNotNull(y1s2);
        assertNull(y2s1);
        assertNull(y2s2);
        assertNull(y3s1);
        assertNull(y3s2);

//        System.out.println("BEFORE:");
//        printPeriodStructure(beforeStructure, 0);
        dcp.editDuration(AcademicPeriod.THREE_YEAR);
//        System.out.println("AFTER:");
//        printPeriodStructure(dcp.getDegreeStructure(), 0);

        assertNotEquals(dcp.getDegreeStructure(), beforeStructure);
        assertEquals(dcp.getDegreeStructure().getAcademicPeriod(), AcademicPeriod.THREE_YEAR);
        assertNull(dcp.getDegreeStructure().getChildOrder());
        assertEquals(y1s1, dcp.getCurricularPeriodFor(1, 1, SEMESTER));
        assertEquals(y1s2, dcp.getCurricularPeriodFor(1, 2, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(2, 1, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(2, 2, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(3, 1, SEMESTER));
        assertNull(dcp.getCurricularPeriodFor(3, 2, SEMESTER));
    }

    private static void populatedCurricularPeriodStructure(final DegreeCurricularPlan degreeCurricularPlan) {
        final CurricularPeriod rootPeriod = degreeCurricularPlan.getDegreeStructure();
        if (rootPeriod.getAcademicPeriod().equals(AcademicPeriod.YEAR)) {
            new CurricularPeriod(SEMESTER, 1, rootPeriod);
            new CurricularPeriod(SEMESTER, 2, rootPeriod);
        } else if (rootPeriod.getAcademicPeriod().equals(AcademicPeriod.THREE_YEAR)) {
            final CurricularPeriod firstYear = new CurricularPeriod(AcademicPeriod.YEAR, 1, rootPeriod);
            new CurricularPeriod(SEMESTER, 1, firstYear);
            new CurricularPeriod(SEMESTER, 2, firstYear);
            final CurricularPeriod secondYear = new CurricularPeriod(AcademicPeriod.YEAR, 2, rootPeriod);
            new CurricularPeriod(SEMESTER, 1, secondYear);
            new CurricularPeriod(SEMESTER, 2, secondYear);
            final CurricularPeriod thirdYear = new CurricularPeriod(AcademicPeriod.YEAR, 3, rootPeriod);
            new CurricularPeriod(SEMESTER, 1, thirdYear);
            new CurricularPeriod(SEMESTER, 2, thirdYear);
        }
    }

    private static void printPeriodStructure(CurricularPeriod curricularPeriod, int level) {
        IntStream.rangeClosed(0, level).forEach(i -> System.out.print("_"));
        System.out.println(curricularPeriod.getLabel() + " (" + curricularPeriod.getExternalId() + ")");
        curricularPeriod.getChildsSet().stream().sorted().forEach(cp -> printPeriodStructure(cp, level + 1));
    }
}
