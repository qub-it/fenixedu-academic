package org.fenixedu.academic.domain.degreeStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RootCourseGroupTest {

    private static final List<CycleType> DEFAULT_CYCLE_TYPES = List.of(CycleType.FIRST_CYCLE, CycleType.SECOND_CYCLE);

    private static DegreeType degreeType;
    private static ExecutionYear executionYear;
    private static RootCourseGroup rootCourseGroup;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.initData();
            executionYear = ExecutionYear.findCurrent(null);
            degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).get();
            degreeType.setCycleTypes(DEFAULT_CYCLE_TYPES);
            DegreeCurricularPlan dcp = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
            rootCourseGroup = dcp.getRoot();

            return null;
        });
    }

    private static RootCourseGroup createRootWithCycleTypes(List<CycleType> cycleTypes) {
        degreeType.setCycleTypes(cycleTypes);
        try {
            String uniqueId = "D" + UUID.randomUUID();
            Degree degree = DegreeTest.createDegree(degreeType, uniqueId, uniqueId, executionYear);
            return degree.createDegreeCurricularPlan("DCP" + UUID.randomUUID(), User.findByUsername("admin").getPerson(),
                    AcademicPeriod.THREE_YEAR).getRoot();
        } finally {
            degreeType.setCycleTypes(DEFAULT_CYCLE_TYPES);
        }
    }

    @Test
    public void testRootCourseGroup_getCycleCourseGroups() {
        Collection<CycleCourseGroup> cycleGroups = rootCourseGroup.getCycleCourseGroups();
        assertNotNull(cycleGroups);
        assertEquals(2, cycleGroups.size());
        assertTrue(cycleGroups.stream().allMatch(CycleCourseGroup::isCycleCourseGroup));
        assertEquals(Set.of(CycleType.FIRST_CYCLE, CycleType.SECOND_CYCLE),
                cycleGroups.stream().map(CycleCourseGroup::getCycleType).collect(Collectors.toSet()));
    }

    @Test
    public void testRootCourseGroup_getCycleCourseGroup() {
        CycleCourseGroup first = rootCourseGroup.getCycleCourseGroup(CycleType.FIRST_CYCLE);
        assertNotNull(first);
        assertEquals(CycleType.FIRST_CYCLE, first.getCycleType());

        CycleCourseGroup second = rootCourseGroup.getCycleCourseGroup(CycleType.SECOND_CYCLE);
        assertNotNull(second);
        assertEquals(CycleType.SECOND_CYCLE, second.getCycleType());

        // non-existent cycle type -> null
        assertNull(rootCourseGroup.getCycleCourseGroup(CycleType.THIRD_CYCLE));
    }

    @Test
    public void testRootCourseGroup_getCanBeDeleted() {
        // Root has a "Cycle" CourseGroup with children (mandatory, optional) -> not deletable
        assertFalse(rootCourseGroup.getCanBeDeleted());

        // DCP whose root only has CycleCourseGroup leaf children -> deletable
        RootCourseGroup minimalRoot = createRootWithCycleTypes(DEFAULT_CYCLE_TYPES);
        assertTrue(minimalRoot.getCanBeDeleted());
    }

    @Test
    public void testRootCourseGroup_emptyRoot_noCycleTypesConfigured() {
        RootCourseGroup emptyRoot = createRootWithCycleTypes(List.of());
        assertTrue(emptyRoot.getChildContextsSet().isEmpty());
        assertTrue(emptyRoot.getCycleCourseGroups().isEmpty());
        assertTrue(emptyRoot.getCanBeDeleted());
    }
}