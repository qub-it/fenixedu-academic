package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CycleCurriculumGroupTest {

    private static final List<CycleType> DEFAULT_CYCLE_TYPES = List.of(CycleType.FIRST_CYCLE, CycleType.SECOND_CYCLE);

    private static ExecutionInterval executionInterval;
    private static ExecutionYear executionYear;

    private static RootCurriculumGroup rootCurriculumGroup;
    private static CycleCurriculumGroup firstCycleGroup;
    private static CycleCurriculumGroup secondCycleGroup;
    private static CycleCurriculumGroup singleCycleGroup;

    private static CycleCurriculumGroup otherFirstCycleGroup;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.initData();
            executionYear = ExecutionYear.findCurrent(null);
            executionInterval = executionYear.getFirstExecutionPeriod();

            DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).get();
            degreeType.setCycleTypes(DEFAULT_CYCLE_TYPES);

            DegreeCurricularPlan dcp = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
            rootCurriculumGroup = createRegistrationRoot("Cycle Comparator Test Student", "cycle.comparator.test.student", dcp);
            firstCycleGroup = rootCurriculumGroup.getCycleCurriculumGroup(CycleType.FIRST_CYCLE);
            secondCycleGroup = createCycleGroup(rootCurriculumGroup, CycleType.SECOND_CYCLE);
            singleCycleGroup = createCycleGroup(rootCurriculumGroup, CycleType.SINGLE_CYCLE);

            RootCurriculumGroup otherRoot = createRegistrationRoot("Cycle Id Comparator Test Student",
                    "cycle.id.comparator.test.student", ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear));
            otherFirstCycleGroup = otherRoot.getCycleCurriculumGroup(CycleType.FIRST_CYCLE);

            return null;
        });
    }

    @Test
    public void testCycleCurriculumGroup_ComparatorByCycleTypeAndId() {
        Comparator<CycleCurriculumGroup> comparator = CycleCurriculumGroup.COMPARATOR_BY_CYCLE_TYPE_AND_ID;

        // comparing a group with itself is always equal
        assertEquals(0, comparator.compare(firstCycleGroup, firstCycleGroup));
        assertEquals(0, comparator.compare(singleCycleGroup, singleCycleGroup));

        // (FIRST_CYCLE < SECOND_CYCLE < SINGLE_CYCLE), the id is never consulted
        assertTrue(comparator.compare(firstCycleGroup, secondCycleGroup) < 0);
        assertTrue(comparator.compare(secondCycleGroup, firstCycleGroup) > 0);
        assertTrue(comparator.compare(firstCycleGroup, singleCycleGroup) < 0);
        assertTrue(comparator.compare(secondCycleGroup, singleCycleGroup) < 0);
        assertTrue(comparator.compare(singleCycleGroup, secondCycleGroup) > 0);

        // same cycle type, different owning students: falls back to DomainObjectUtil.COMPARATOR_BY_ID
        List<CycleCurriculumGroup> sameCycleSorted = new ArrayList<>(List.of(otherFirstCycleGroup, firstCycleGroup));
        sameCycleSorted.sort(comparator);
        assertEquals(firstCycleGroup.getCycleType(), sameCycleSorted.get(0).getCycleType());
        assertEquals(firstCycleGroup.getCycleType(), sameCycleSorted.get(1).getCycleType());
        assertTrue(sameCycleSorted.get(0).getExternalId().compareTo(sameCycleSorted.get(1).getExternalId()) < 0);

        // cycle type still takes precedence over id when types differ
        List<CycleCurriculumGroup> sorted = new ArrayList<>(List.of(singleCycleGroup, secondCycleGroup, firstCycleGroup));
        sorted.sort(comparator);
        assertEquals(firstCycleGroup, sorted.get(0));
        assertEquals(secondCycleGroup, sorted.get(1));
        assertEquals(singleCycleGroup, sorted.get(2));
    }

    @Test
    public void testCycleCurriculumGroup_DeleteRecursive() {
        CycleCurriculumGroup emptyGroup = createCycleGroup(rootCurriculumGroup, CycleType.SPECIALIZATION_CYCLE);
        emptyGroup.deleteRecursive();
        assertTrue(emptyGroup.getCurriculumModulesSet().isEmpty());
        assertFalse(rootCurriculumGroup.getCurriculumModulesSet().contains(emptyGroup));

        // must differ from emptyGroup's cycle type (SPECIALIZATION_CYCLE) and from the singleCycleGroup fixture
        CycleCurriculumGroup group = createCycleGroup(rootCurriculumGroup, CycleType.THIRD_CYCLE);
        assertTrue(group.getCurriculumModulesSet().isEmpty());

        CourseGroup childCourseGroup = new CourseGroup(group.getCycleCourseGroup(), "Child Group", "Child Group",
                executionInterval, null, null);
        CurriculumGroup childGroup = new CurriculumGroup(group, childCourseGroup, executionInterval);
        assertTrue(group.getCurriculumModulesSet().contains(childGroup));
        assertTrue(group.getCurriculumGroups().contains(childGroup));
        assertTrue(rootCurriculumGroup.getCurriculumModulesSet().contains(group));

        group.deleteRecursive();

        assertTrue(group.getCurriculumModulesSet().isEmpty());
        assertTrue(group.getCurriculumGroups().isEmpty());
        assertFalse(rootCurriculumGroup.getCurriculumModulesSet().contains(group));
        assertFalse(firstCycleGroup.getCurriculumModulesSet().contains(childGroup));
    }

    private static CycleCurriculumGroup createCycleGroup(RootCurriculumGroup root, CycleType cycleType) {
        CycleCourseGroup cycleCourseGroup = new CycleCourseGroup(root.getDegreeModule(), "Cycle " + cycleType.name(),
                "Cycle " + cycleType.name(), cycleType, executionInterval, null);
        return new CycleCurriculumGroup(root, cycleCourseGroup, executionInterval);
    }

    private static RootCurriculumGroup createRegistrationRoot(String studentName, String usernamePrefix,
            DegreeCurricularPlan dcp) {
        Student student = StudentTest.createStudent(studentName, usernamePrefix + "." + UUID.randomUUID());
        Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
        StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        return scp.getRoot();
    }
}