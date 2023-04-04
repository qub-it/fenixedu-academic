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

        final Degree degree = new Degree("Computer Science", "Computer Science", "CS", degreeType, new GradeScale(),
                new GradeScale(), executionYear);
        degree.setCode("CS");
        degree.setCalendar(executionYear.getAcademicInterval().getAcademicCalendar());

        return degree;
    }

    @Test
    public void testDegree_find() {
        assertEquals(Degree.findAll().count(), 1l);
        assertNotNull(Degree.find("CS"));
        assertEquals(Degree.find("CS"), degree);
        assertNull(Degree.find("XX"));
    }

}
