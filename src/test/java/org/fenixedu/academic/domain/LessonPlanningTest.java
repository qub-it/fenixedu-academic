package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class LessonPlanningTest {

    private static ExecutionCourse executionCourse;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initLessonPlannings();
            return null;
        });
    }

    static void initLessonPlannings() {
        ExecutionsAndSchedulesTest.initExecutions();
        ExecutionsAndSchedulesTest.initSchedules();

        executionCourse = Bennu.getInstance().getExecutionCoursesSet().iterator().next();
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testRequiredTitle() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.LessonPlanning.no.title");

        executionCourse.getLessonPlanningsSet().forEach(LessonPlanning::delete);

        final CourseLoadType theoretical = CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow();

        createLessonPlanning(null, "Planning A", theoretical, executionCourse);
    }

    @Test
    public void testOrder() {
        executionCourse.getLessonPlanningsSet().forEach(LessonPlanning::delete);

        final CourseLoadType theoretical = CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow();
        final CourseLoadType praticalLab = CourseLoadType.findByCode(CourseLoadType.PRACTICAL_LABORATORY).orElseThrow();

        final LessonPlanning lpA_T = createLessonPlanning("A (T)", "Planning A", theoretical, executionCourse);
        final LessonPlanning lpB_T = createLessonPlanning("B (T)", "Planning B", theoretical, executionCourse);
        final LessonPlanning lpC_T = createLessonPlanning("C (T)", "Planning C", theoretical, executionCourse);
        final LessonPlanning lpD_T = createLessonPlanning("D (T)", "Planning D", theoretical, executionCourse);

        final LessonPlanning lpA_PL = createLessonPlanning("A (PL)", "Planning A", praticalLab, executionCourse);
        final LessonPlanning lpB_PL = createLessonPlanning("B (PL)", "Planning B", praticalLab, executionCourse);
        final LessonPlanning lpC_PL = createLessonPlanning("C (PL)", "Planning C", praticalLab, executionCourse);
        final LessonPlanning lpD_PL = createLessonPlanning("D (PL)", "Planning D", praticalLab, executionCourse);

        assertEquals(lpA_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(4));

        assertEquals(lpA_PL.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_PL.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_PL.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_PL.getOrderOfPlanning(), Integer.valueOf(4));
    }

    @Test
    public void testOrderAfterDelete() {
        executionCourse.getLessonPlanningsSet().forEach(LessonPlanning::delete);

        final CourseLoadType theoretical = CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow();
        final CourseLoadType praticalLab = CourseLoadType.findByCode(CourseLoadType.PRACTICAL_LABORATORY).orElseThrow();

        final LessonPlanning lpA_T = createLessonPlanning("A (T)", "Planning A", theoretical, executionCourse);
        final LessonPlanning lpB_T = createLessonPlanning("B (T)", "Planning B", theoretical, executionCourse);
        final LessonPlanning lpC_T = createLessonPlanning("C (T)", "Planning C", theoretical, executionCourse);
        final LessonPlanning lpD_T = createLessonPlanning("D (T)", "Planning D", theoretical, executionCourse);

        final LessonPlanning lpA_PL = createLessonPlanning("A (PL)", "Planning A", praticalLab, executionCourse);
        final LessonPlanning lpB_PL = createLessonPlanning("B (PL)", "Planning B", praticalLab, executionCourse);
        final LessonPlanning lpC_PL = createLessonPlanning("C (PL)", "Planning C", praticalLab, executionCourse);
        final LessonPlanning lpD_PL = createLessonPlanning("D (PL)", "Planning D", praticalLab, executionCourse);

        lpA_T.delete();
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(3));

        lpC_T.delete();
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(2));

        lpD_T.delete();
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(1));

        assertEquals(lpA_PL.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_PL.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_PL.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_PL.getOrderOfPlanning(), Integer.valueOf(4));

        lpC_PL.delete();
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpA_PL.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_PL.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpD_PL.getOrderOfPlanning(), Integer.valueOf(3));
    }

    @Test
    public void testOrderAfterMove() {
        executionCourse.getLessonPlanningsSet().forEach(LessonPlanning::delete);

        final CourseLoadType theoretical = CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow();
        final CourseLoadType praticalLab = CourseLoadType.findByCode(CourseLoadType.PRACTICAL_LABORATORY).orElseThrow();

        final LessonPlanning lpA_T = createLessonPlanning("A (T)", "Planning A", theoretical, executionCourse);
        final LessonPlanning lpB_T = createLessonPlanning("B (T)", "Planning B", theoretical, executionCourse);
        final LessonPlanning lpC_T = createLessonPlanning("C (T)", "Planning C", theoretical, executionCourse);
        final LessonPlanning lpD_T = createLessonPlanning("D (T)", "Planning D", theoretical, executionCourse);

        final LessonPlanning lpA_PL = createLessonPlanning("A (PL)", "Planning A", praticalLab, executionCourse);
        final LessonPlanning lpB_PL = createLessonPlanning("B (PL)", "Planning B", praticalLab, executionCourse);
        final LessonPlanning lpC_PL = createLessonPlanning("C (PL)", "Planning C", praticalLab, executionCourse);
        final LessonPlanning lpD_PL = createLessonPlanning("D (PL)", "Planning D", praticalLab, executionCourse);

        lpA_T.moveTo(0);
        assertEquals(lpA_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(4));

        lpA_T.moveTo(1);
        assertEquals(lpA_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(4));

        lpA_T.moveTo(5);
        assertEquals(lpA_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(4));

        lpA_T.moveTo(3);
        assertEquals(lpA_T.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(4));

        lpD_T.moveTo(2);
        assertEquals(lpA_T.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpB_T.getOrderOfPlanning(), Integer.valueOf(4));
        assertEquals(lpC_T.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpD_T.getOrderOfPlanning(), Integer.valueOf(2));

        assertEquals(lpA_PL.getOrderOfPlanning(), Integer.valueOf(1));
        assertEquals(lpB_PL.getOrderOfPlanning(), Integer.valueOf(2));
        assertEquals(lpC_PL.getOrderOfPlanning(), Integer.valueOf(3));
        assertEquals(lpD_PL.getOrderOfPlanning(), Integer.valueOf(4));
    }

    @Test
    public void testCopyLessonPlannings() {
        executionCourse.getLessonPlanningsSet().forEach(LessonPlanning::delete);

        final CourseLoadType theoretical = CourseLoadType.findByCode(CourseLoadType.THEORETICAL).orElseThrow();
        final CourseLoadType praticalLab = CourseLoadType.findByCode(CourseLoadType.PRACTICAL_LABORATORY).orElseThrow();
        final CourseLoadType seminar = CourseLoadType.findByCode(CourseLoadType.SEMINAR).orElseThrow();

        final LessonPlanning lpA_T = createLessonPlanning("A (T)", "Planning A", theoretical, executionCourse);
        final LessonPlanning lpB_T = createLessonPlanning("B (T)", "Planning B", theoretical, executionCourse);
        final LessonPlanning lpC_T = createLessonPlanning("C (T)", "Planning C", theoretical, executionCourse);
        final LessonPlanning lpD_T = createLessonPlanning("D (T)", "Planning D", theoretical, executionCourse);

        final LessonPlanning lpA_PL = createLessonPlanning("A (PL)", "Planning A", praticalLab, executionCourse);
        final LessonPlanning lpB_PL = createLessonPlanning("B (PL)", "Planning B", praticalLab, executionCourse);
        final LessonPlanning lpC_PL = createLessonPlanning("C (PL)", "Planning C", praticalLab, executionCourse);
        final LessonPlanning lpD_PL = createLessonPlanning("D (PL)", "Planning D", praticalLab, executionCourse);

        final ExecutionCourse executionCourseTo = new ExecutionCourse("TMP", "TMP", executionCourse.getExecutionInterval());
        executionCourseTo.getAssociatedCurricularCoursesSet().addAll(executionCourse.getAssociatedCurricularCoursesSet());

        LessonPlanning.copyLessonPlanningsFrom(executionCourse, executionCourseTo);

        assertEquals(Integer.valueOf(executionCourse.getLessonPlanningsSet().size()), Integer.valueOf(8));
        assertEquals(Integer.valueOf(executionCourseTo.getLessonPlanningsSet().size()), Integer.valueOf(8));

        assertEquals(Integer.valueOf((int) LessonPlanning.find(executionCourseTo, theoretical).count()), Integer.valueOf(4));
        assertEquals(Integer.valueOf((int) LessonPlanning.find(executionCourseTo, praticalLab).count()), Integer.valueOf(4));
        assertEquals(Integer.valueOf((int) LessonPlanning.find(executionCourseTo, seminar).count()), Integer.valueOf(0));

        assertEquals(LessonPlanning.find(executionCourseTo, theoretical)
                .filter(lp -> lp.getOrderOfPlanning().equals(Integer.valueOf(2))).findAny().map(lp -> lp.getTitle()).orElse(null),
                lpB_T.getTitle());

        assertEquals(LessonPlanning.find(executionCourseTo, praticalLab)
                .filter(lp -> lp.getOrderOfPlanning().equals(Integer.valueOf(4))).findAny().map(lp -> lp.getTitle()).orElse(null),
                lpD_PL.getTitle());
    }

    private static LessonPlanning createLessonPlanning(final String title, final String planning,
            final CourseLoadType theoretical, ExecutionCourse executionCourse) {
        final Locale locale = Locale.getDefault();
        return new LessonPlanning(new LocalizedString(locale, title), new LocalizedString(locale, planning), theoretical,
                executionCourse);
    }

}
