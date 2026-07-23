package org.fenixedu.academic.domain.degreeStructure;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeCurricularPlanTest;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ContextTest {

    private static ExecutionYear executionYear, previousExecutionYear, nextExecutionYear;
    private static ExecutionInterval executionInterval, previousYearExecutionInterval, nextYearExecutionInterval;
    private static CurricularPeriod firstSemesterFirstYear, secondSemesterFirstYear, firstSemesterSecondYear;
    private static RootCourseGroup root;

    private static CurricularCourse curricularCourseA, curricularCourseD;
    private static Context contextA_1Y1S, contextB_1Y1S, contextC_2Y1S;

    private static CourseGroup childCourseGroup;
    private static Context contextCG_root, contextA_CG;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initSetup();
            return null;
        });
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getContextsSet().stream()
                .filter(ctx -> ctx != contextA_1Y1S && ctx != contextA_CG && ctx != contextB_1Y1S && ctx != contextC_2Y1S
                        && ctx != contextCG_root).filter(ctx -> ctx.getChildDegreeModule() != null).forEach(Context::delete);
    }

    private static void initSetup() {
        DegreeCurricularPlanTest.init();

        Degree degree = Degree.find(DegreeTest.DEGREE_A_CODE);
        DegreeCurricularPlan degreeCurricularPlan =
                degree.getDegreeCurricularPlansSet().stream().filter(p -> DCP_NAME_V1.equals(p.getName())).findAny()
                        .orElseThrow();

        executionYear = ExecutionYear.findCurrent(degree.getCalendar());
        executionInterval = executionYear.getFirstExecutionPeriod();
        previousExecutionYear = (ExecutionYear) executionYear.getPrevious();
        previousYearExecutionInterval = previousExecutionYear.getFirstExecutionPeriod();
        nextExecutionYear = (ExecutionYear) executionYear.getNext();
        nextYearExecutionInterval = nextExecutionYear.getFirstExecutionPeriod();

        CompetenceCourse competenceCourseA = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);
        curricularCourseA = createCurricularCourse("CurricularCourse A", competenceCourseA);

        CompetenceCourse competenceCourseB = CompetenceCourse.find(CompetenceCourseTest.COURSE_B_CODE);
        CurricularCourse curricularCourseB = createCurricularCourse("CurricularCourse B", competenceCourseB);

        // Same CompetenceCourse as curricularCourseA
        CurricularCourse curricularCourseC = createCurricularCourse("CurricularCourse C", competenceCourseA);
        curricularCourseD = createCurricularCourse("CurricularCourse D", competenceCourseA);

        CurricularPeriod firstYear = new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        firstSemesterFirstYear = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, firstYear);
        secondSemesterFirstYear = new CurricularPeriod(AcademicPeriod.SEMESTER, 2, firstYear);

        CurricularPeriod secondYear = new CurricularPeriod(AcademicPeriod.YEAR, 2, degreeCurricularPlan.getDegreeStructure());
        firstSemesterSecondYear = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, secondYear);

        root = degreeCurricularPlan.getRoot();
        contextA_1Y1S = createContext(root, curricularCourseA, firstSemesterFirstYear, executionInterval, null);
        contextB_1Y1S = createContext(root, curricularCourseB, firstSemesterFirstYear, executionInterval, null);
        contextC_2Y1S = createContext(root, curricularCourseC, firstSemesterSecondYear, executionInterval, null);

        childCourseGroup = new CourseGroup(root, "Child CourseGroup", "Child CourseGroup", executionInterval, null);
        contextCG_root = childCourseGroup.getParentContextsSet().iterator().next();

        // Create a new context for curricularCourseA under childCourseGroup
        contextA_CG = createContext(childCourseGroup, curricularCourseA, firstSemesterFirstYear, executionInterval, null);
    }

    @Test
    public void testContext_comparatorByDegreeModuleName() {
        List<Context> contexts = new ArrayList<>();
        contexts.add(contextB_1Y1S);
        contexts.add(contextC_2Y1S);
        contexts.add(contextA_1Y1S);

        contexts.sort(Context.COMPARATOR_BY_DEGREE_MODULE_NAME);

        assertEquals(contextA_1Y1S, contexts.get(0));
        assertEquals(contextC_2Y1S, contexts.get(
                1)); // curricularCourseC and curricularCourseA have the same CompetenceCourse, therefore, the same degree module name (Course A)
        assertEquals(contextB_1Y1S, contexts.get(2));
    }

    @Test
    public void testContext_comparatorByCurricularYear() {
        List<Context> contexts = new ArrayList<>();
        contexts.add(contextB_1Y1S);
        contexts.add(contextC_2Y1S);
        contexts.add(contextA_1Y1S);

        contexts.sort(Context.COMPARATOR_BY_CURRICULAR_YEAR);

        assertEquals(contextA_1Y1S, contexts.get(0));
        assertEquals(contextB_1Y1S, contexts.get(1)); // First compare by curricular year, then by external id
        assertEquals(contextC_2Y1S, contexts.get(2));
    }

    @Test
    public void testContext_create() {
        Context context = createContext(root, curricularCourseD, firstSemesterFirstYear, nextYearExecutionInterval, null);

        assertEquals(root, context.getParentCourseGroup());
        assertEquals(curricularCourseD, context.getChildDegreeModule());
        assertEquals(firstSemesterFirstYear, context.getCurricularPeriod());
        assertNotNull(context.getRootDomainObject());
    }

    @Test
    public void testContext_create_duplicateCourseGroupNamesThrows() {
        // A second CourseGroup with the same name in the same parent should throw
        assertThrows(DomainException.class,
                () -> new CourseGroup(root, "Child CourseGroup", "Child CourseGroup", executionInterval, null),
                "error.existingCourseGroupWithSameNameInParent");
    }

    @Test
    public void testContext_checkExistingCourseGroupContexts() {
        assertNotNull(contextA_1Y1S);
        assertNotNull(contextB_1Y1S);
        assertTrue(root.getChildContextsSet().contains(contextA_1Y1S));
        assertTrue(root.getChildContextsSet().contains(contextB_1Y1S));

        assertNotNull(contextCG_root);
        assertTrue(root.getChildContextsSet().contains(contextCG_root));
        assertEquals(childCourseGroup, contextCG_root.getChildDegreeModule());

        // Same CurricularCourse (curricularCourseA) in two different parent CourseGroups
        assertNotNull(contextA_CG);
        assertTrue(childCourseGroup.getChildContextsSet().contains(contextA_CG));
        assertEquals(2, curricularCourseA.getParentContextsSet().size());
        assertTrue(curricularCourseA.getParentContextsSet().stream().anyMatch(ctx -> ctx.getParentCourseGroup().equals(root)));
        assertTrue(curricularCourseA.getParentContextsSet().stream()
                .anyMatch(ctx -> ctx.getParentCourseGroup().equals(childCourseGroup)));
    }

    @Test
    public void testContext_checkExistingCourseGroupContexts_duplicateThrows() {
        // Testing duplicate for context under root
        assertThrows(DomainException.class,
                () -> createContext(root, curricularCourseA, firstSemesterFirstYear, executionInterval, null),
                "courseGroup.contextAlreadyExistForCourseGroup");

        // Testing duplicate for context under childCourseGroup
        assertThrows(DomainException.class,
                () -> createContext(childCourseGroup, curricularCourseA, firstSemesterFirstYear, executionInterval, null),
                "courseGroup.contextAlreadyExistForCourseGroup");
    }

    @Test
    public void testContext_edit() {
        Context context =
                createContext(root, curricularCourseD, firstSemesterFirstYear, previousYearExecutionInterval, executionInterval);

        context.edit(root, secondSemesterFirstYear, executionInterval, nextYearExecutionInterval);

        assertEquals(secondSemesterFirstYear, context.getCurricularPeriod());

        // getBeginExecutionInterval and getEndExecutionInterval actually return the ExecutionYear (see implementation)
        assertEquals(executionYear, context.getBeginExecutionInterval());
        assertEquals(nextExecutionYear, context.getEndExecutionInterval());
    }

    @Test
    public void testContext_delete() {
        Context context = createContext(root, curricularCourseD, firstSemesterFirstYear, nextYearExecutionInterval, null);

        context.delete();

        assertNull(context.getParentCourseGroup());
        assertNull(context.getChildDegreeModule());
        assertNull(context.getCurricularPeriod());
        assertNull(context.getRootDomainObject());
    }

    @Test
    public void testContext_isValid() {
        // Semestral course: only open in first semester
        assertFalse(contextA_1Y1S.isValid(previousYearExecutionInterval));
        assertTrue(contextA_1Y1S.isValid(executionInterval));
        assertFalse(contextA_1Y1S.isValid(executionInterval.getNext()));
        assertTrue(contextA_1Y1S.isValid(nextYearExecutionInterval));
        assertFalse(contextA_1Y1S.isValid(nextYearExecutionInterval.getNext()));

        // Annual course: open in first and second semesters
        assertFalse(contextB_1Y1S.isValid(previousYearExecutionInterval));
        assertTrue(contextB_1Y1S.isValid(executionInterval));
        assertTrue(contextB_1Y1S.isValid(executionInterval.getNext()));
        assertTrue(contextB_1Y1S.isValid(nextYearExecutionInterval));
        assertTrue(contextB_1Y1S.isValid(nextYearExecutionInterval.getNext()));

        // CourseGroup contexts are always valid when open (no curricular period matching needed)
        assertFalse(contextCG_root.isValid(previousYearExecutionInterval));
        assertTrue(contextCG_root.isValid(executionInterval));
        assertTrue(contextCG_root.isValid(nextYearExecutionInterval));
    }

    @Test
    public void testContext_isValidForExecutionAggregation() {
        assertFalse(contextA_1Y1S.isValidForExecutionAggregation(previousExecutionYear));
        assertTrue(contextA_1Y1S.isValidForExecutionAggregation(executionYear));
        assertTrue(contextA_1Y1S.isValidForExecutionAggregation(nextExecutionYear)); // because end is null
    }

    @Test
    public void testContext_isOpen_withExecutionInterval() {
        assertFalse(contextA_1Y1S.isOpen(previousYearExecutionInterval));
        assertTrue(contextA_1Y1S.isOpen(executionInterval));
        assertTrue(contextA_1Y1S.isOpen(executionInterval.getNext()));
        assertTrue(contextA_1Y1S.isOpen(nextYearExecutionInterval));

        assertFalse(contextCG_root.isOpen(previousYearExecutionInterval));
        assertTrue(contextCG_root.isOpen(executionInterval));
        assertTrue(contextCG_root.isOpen(executionInterval.getNext()));
        assertTrue(contextCG_root.isOpen(nextYearExecutionInterval));
    }

    @Test
    public void testContext_isOpen_withExecutionYear() {
        assertFalse(contextA_1Y1S.isOpen(previousExecutionYear));
        assertTrue(contextA_1Y1S.isOpen(executionYear));
        assertTrue(contextA_1Y1S.isOpen(nextExecutionYear));

        assertFalse(contextCG_root.isOpen(previousExecutionYear));
        assertTrue(contextCG_root.isOpen(executionYear));
        assertTrue(contextCG_root.isOpen(nextExecutionYear));
    }

    @Test
    public void testContext_intersects() {
        assertFalse(contextA_1Y1S.intersects(previousYearExecutionInterval, previousYearExecutionInterval.getNext()));
        assertTrue(contextA_1Y1S.intersects(previousYearExecutionInterval, executionInterval));
        assertTrue(contextA_1Y1S.intersects(executionInterval, executionInterval));
        assertTrue(contextA_1Y1S.intersects(executionInterval, nextYearExecutionInterval));
        assertTrue(contextA_1Y1S.intersects(executionInterval, null));
        assertThrows(DomainException.class, () -> contextA_1Y1S.intersects(nextYearExecutionInterval, executionInterval),
                "context.begin.is.after.end.execution.period");

        assertTrue(contextCG_root.intersects(executionInterval, executionInterval.getNext()));
        assertTrue(contextCG_root.intersects(executionInterval, null));
        assertFalse(contextCG_root.intersects(previousYearExecutionInterval, previousYearExecutionInterval.getNext()));
    }

    @Test
    public void testContext_containsInterval() {
        // contextA contains the intervals from executionInterval onwards (end == null)
        assertFalse(contextA_1Y1S.containsInterval(previousYearExecutionInterval, executionInterval));
        assertTrue(contextA_1Y1S.containsInterval(executionInterval, executionInterval));
        assertTrue(contextA_1Y1S.containsInterval(executionInterval, nextYearExecutionInterval));

        Context context =
                createContext(root, curricularCourseD, firstSemesterSecondYear, previousExecutionYear, nextExecutionYear);

        assertTrue(context.containsInterval(executionYear, nextExecutionYear));
        assertTrue(context.containsInterval(executionYear, previousExecutionYear));
        ExecutionInterval afternextYearExecutionInterval =
                nextExecutionYear.getNext().getExecutionYear().getFirstExecutionPeriod();
        assertFalse(context.containsInterval(executionYear, afternextYearExecutionInterval));
    }

    // Helpers
    private static Context createContext(CourseGroup courseGroup, DegreeModule degreeModule, CurricularPeriod curricularPeriod,
            ExecutionInterval begin, ExecutionInterval end) {
        return new Context(courseGroup, degreeModule, curricularPeriod, begin, end);
    }

    private static CurricularCourse createCurricularCourse(String name, CompetenceCourse competenceCourse) {
        CurricularCourse curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(competenceCourse);
        curricularCourse.setName(name);
        return curricularCourse;
    }
}
