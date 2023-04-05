package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeTest {

    public static final String DEGREE_A_CODE = "DA";
    private static Degree degree;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            degree = initDegree();
            return null;
        });
    }

    static Degree initDegree() {
        final DegreeType degreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Degree").build());

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);

        final Degree degree =
                new Degree("Degree A", "Degree A", DEGREE_A_CODE, degreeType, new GradeScale(), new GradeScale(), executionYear);
        degree.setCode(DEGREE_A_CODE);
        degree.setCalendar(executionYear.getAcademicInterval().getAcademicCalendar());

        return degree;
    }

    @Test
    public void testDegree_find() {
        assertEquals(Degree.findAll().count(), 1l);
        assertNotNull(Degree.find(DEGREE_A_CODE));
        assertEquals(Degree.find(DEGREE_A_CODE), degree);
        assertNull(Degree.find("XX"));
    }

}
