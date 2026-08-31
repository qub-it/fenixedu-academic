package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RootCurriculumGroupTest {

    private static final List<CycleType> TWO_CYCLES = List.of(CycleType.FIRST_CYCLE, CycleType.SECOND_CYCLE);

    private static RootCurriculumGroup root;
    private static CycleCurriculumGroup firstCycle;
    private static CycleCurriculumGroup secondCycle;
    private static ExecutionYear executionYear;

    private static RootCurriculumGroup externalRoot;
    private static ExternalCurriculumGroup externalCycle;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.initData();
            executionYear = ExecutionYear.findCurrent(null);

            final DegreeType degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).get();
            degreeType.setCycleTypes(TWO_CYCLES);

            final DegreeCurricularPlan dcp = ConclusionRulesTestUtil.createDegreeCurricularPlan(executionYear);
            final Registration registration = ConclusionRulesTestUtil.createRegistration(dcp, executionYear);
            final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();

            root = scp.getRoot();
            final CycleCourseGroup secondCycleGroup =
                    root.getDegreeModule().getCycleCourseGroup(CycleType.SECOND_CYCLE);
            CurriculumGroupFactory.createGroup(root, secondCycleGroup, executionYear.getFirstExecutionPeriod());
            firstCycle = root.getCycleCurriculumGroup(CycleType.FIRST_CYCLE);
            secondCycle = root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE);
            assertNotNull(firstCycle);
            assertNotNull(secondCycle);

            final Student externalStudent = StudentTest.createStudent("Student External", "student.test.external");

            // Single-cycle bachelor: its first cycle will point to the master's second cycle as destination
            final DegreeType bachelorType =
                    new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "External Bachelor Type").build());
            bachelorType.setCode("EXTBACC" + UUID.randomUUID());
            bachelorType.setCycleTypes(List.of(CycleType.FIRST_CYCLE));
            final DegreeCurricularPlan bachelorDcp = createDegreeWithPlan(bachelorType, "EXTBAC", "External Bachelor");

            final Registration bachelorRegistration =
                    ConclusionRulesTestUtil.createRegistration(externalStudent, bachelorDcp, executionYear);
            externalRoot = bachelorRegistration.getLastStudentCurricularPlan().getRoot();

            // Two-cycle master, whose second cycle acts as the external cycle target
            final DegreeType masterType = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).get();
            masterType.setCycleTypes(TWO_CYCLES);
            final DegreeCurricularPlan masterDcp = createDegreeWithPlan(masterType, "EXTMAS", "External Master");

            // Make the bachelor's first cycle point to the master's second cycle, then create the external group
            final CycleCourseGroup masterSecondCycleGroup = masterDcp.getRoot().getCycleCourseGroup(CycleType.SECOND_CYCLE);
            bachelorDcp.getRoot().getCycleCourseGroup(CycleType.FIRST_CYCLE).addDestinationAffinities(masterSecondCycleGroup);

            externalCycle = (ExternalCurriculumGroup) CurriculumGroupFactory.createGroup(externalRoot, masterSecondCycleGroup,
                    executionYear.getFirstExecutionPeriod());

            return null;
        });
    }

    @Test
    public void testRootCurriculumGroup_GetCycleCurriculumGroups() {
        final Collection<CycleCurriculumGroup> cycles = root.getCycleCurriculumGroups();
        assertEquals(2, cycles.size());
        assertEquals(Set.of(CycleType.FIRST_CYCLE, CycleType.SECOND_CYCLE),
                cycles.stream().map(CycleCurriculumGroup::getCycleType).collect(Collectors.toSet()));
    }

    @Test
    public void testRootCurriculumGroup_GetCycleCurriculumGroup() {
        assertSame(firstCycle, root.getCycleCurriculumGroup(CycleType.FIRST_CYCLE));
        assertSame(secondCycle, root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE));
        assertNull(root.getCycleCurriculumGroup(CycleType.THIRD_CYCLE));
        assertSame(externalCycle, externalRoot.getCycleCurriculumGroup(externalCycle.getCycleType()));
    }

    @Test
    public void testRootCurriculumGroup_GetInternalCycleCurriculumGroups() {
        final List<CycleCurriculumGroup> internal = root.getInternalCycleCurriculumGroups();
        assertEquals(2, internal.size());
        assertTrue(internal.contains(firstCycle));
        assertTrue(internal.contains(secondCycle));
    }

    @Test
    public void testRootCurriculumGroup_GetExternalCycleCurriculumGroups() {
        final List<ExternalCurriculumGroup> external = root.getExternalCycleCurriculumGroups();
        assertTrue(external.isEmpty());

        final List<ExternalCurriculumGroup> externalWithCycles = externalRoot.getExternalCycleCurriculumGroups();
        assertEquals(1, externalWithCycles.size());
        assertTrue(externalWithCycles.contains(externalCycle));
    }

    @Test
    public void testRootCurriculumGroup_HasExternalCycles() {
        assertFalse(root.hasExternalCycles());
        assertTrue(externalRoot.hasExternalCycles());
    }

    private static DegreeCurricularPlan createDegreeWithPlan(final DegreeType degreeType, final String code, final String name) {
        final Degree degree = DegreeTest.createDegree(degreeType, code + UUID.randomUUID(), name, executionYear);
        final DegreeCurricularPlan dcp = degree.createDegreeCurricularPlan(name + " Plan",
                User.findByUsername(ConclusionRulesTestUtil.ADMIN_USERNAME).getPerson(), AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        return dcp;
    }

}
