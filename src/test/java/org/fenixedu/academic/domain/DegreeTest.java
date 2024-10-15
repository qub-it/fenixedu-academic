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

    public static final String DEGREE_TYPE_CODE = "DEGREE";

    public static final String MASTER_DEGREE_TYPE_CODE = "MASTER";

    private static Degree degree;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            degree = initDegree();
            return null;
        });
    }

    public static Degree initDegree() {
        final DegreeType degreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Degree").build());
        degreeType.setCode(DEGREE_TYPE_CODE);

        final DegreeType masterDegreeType =
                new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Master Degree").build());
        masterDegreeType.setCode(MASTER_DEGREE_TYPE_CODE);

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);

        return createDegree(degreeType, DEGREE_A_CODE, "Degree A", executionYear);

    }

    public static Degree createDegree(final DegreeType degreeType, String code, String name, final ExecutionYear executionYear) {
        final Degree result = new Degree(name, name, code, degreeType, new GradeScale(), new GradeScale(), executionYear);
        result.setCode(code);
        result.setCalendar(executionYear.getAcademicInterval().getAcademicCalendar());

        return result;
    }

    @Test
    public void testDegree_find() {
        assertEquals(Degree.findAll().count(), 1l);
        assertNotNull(Degree.find(DEGREE_A_CODE));
        assertEquals(Degree.find(DEGREE_A_CODE), degree);
        assertNull(Degree.find("XX"));
    }

}
