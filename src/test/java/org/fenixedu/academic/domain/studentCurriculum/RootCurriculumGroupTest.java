package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
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

    private static ExecutionYear executionYear;

    private static RootCurriculumGroup root;
    private static RootCurriculumGroup externalRoot;
    private static ExternalCurriculumGroup externalCycle;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.initData();
            executionYear = ExecutionYear.findCurrent(null);

            // A two-cycle bachelor: its root has a first and a second internal cycle
            final DegreeType twoCycleBachelorType =
                    new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Two Cycle Bachelor Type").build());
            twoCycleBachelorType.setCode("BACC1" + UUID.randomUUID());
            twoCycleBachelorType.setCycleTypes(TWO_CYCLES);
            final DegreeCurricularPlan twoCycleBachelorDcp =
                    createDegreeWithPlan(twoCycleBachelorType, "BAC1", "Two Cycle Bachelor");
            root = createRoot(StudentTest.createStudent("Student", "student.test.two.cycle"), twoCycleBachelorDcp);
            CurriculumGroupFactory.createGroup(root, twoCycleBachelorDcp.getRoot().getCycleCourseGroup(CycleType.SECOND_CYCLE),
                    executionYear.getFirstExecutionPeriod());

            // Configuring conclusion rules on both cycles (isSkipValidation + credits limit) so isConcluded returns true
            final ProgramConclusion skipFirst =
                    new ProgramConclusion("SKIP1", new LocalizedString(), new LocalizedString(), new LocalizedString(),
                            new LocalizedString(), true, false, true, null);
            final ProgramConclusion skipSecond =
                    new ProgramConclusion("SKIP2", new LocalizedString(), new LocalizedString(), new LocalizedString(),
                            new LocalizedString(), true, false, true, null);
            twoCycleBachelorDcp.getRoot().getCycleCourseGroup(CycleType.FIRST_CYCLE).setProgramConclusion(skipFirst);
            twoCycleBachelorDcp.getRoot().getCycleCourseGroup(CycleType.SECOND_CYCLE).setProgramConclusion(skipSecond);
            new CreditsLimit(twoCycleBachelorDcp.getRoot().getCycleCourseGroup(CycleType.FIRST_CYCLE), null, executionYear, null,
                    0d, 0d);
            new CreditsLimit(twoCycleBachelorDcp.getRoot().getCycleCourseGroup(CycleType.SECOND_CYCLE), null, executionYear, null,
                    0d, 0d);

            // A single-cycle bachelor whose root holds an external second cycle from a master
            final DegreeType bachelorType =
                    new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "External Bachelor Type").build());
            bachelorType.setCode("EXTBACC" + UUID.randomUUID());
            bachelorType.setCycleTypes(List.of(CycleType.FIRST_CYCLE));
            final DegreeCurricularPlan bachelorDcp = createDegreeWithPlan(bachelorType, "EXTBAC", "External Bachelor");
            externalRoot = createRoot(StudentTest.createStudent("Student External", "student.test.external"), bachelorDcp);

            // Two-cycle master: its second cycle becomes the external target
            final DegreeType masterType = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).get();
            masterType.setCycleTypes(TWO_CYCLES);
            final DegreeCurricularPlan masterDcp = createDegreeWithPlan(masterType, "EXTMAS", "External Master");

            // Link the bachelor's first cycle to the master's second cycle, then create the external cycle group
            final CycleCourseGroup masterSecondCycleGroup = masterDcp.getRoot().getCycleCourseGroup(CycleType.SECOND_CYCLE);
            bachelorDcp.getRoot().getCycleCourseGroup(CycleType.FIRST_CYCLE).addDestinationAffinities(masterSecondCycleGroup);

            externalCycle = (ExternalCurriculumGroup) CurriculumGroupFactory.createGroup(externalRoot, masterSecondCycleGroup,
                    executionYear.getFirstExecutionPeriod());

            return null;
        });
    }

    @Test
    public void testRootCurriculumGroup_GetCycleCurriculumGroups() {
        // root's cycles are its own internal first and second cycles
        final Collection<CycleCurriculumGroup> cycles = root.getCycleCurriculumGroups();
        assertEquals(2, cycles.size());
        assertTrue(cycles.contains(root.getCycleCurriculumGroup(CycleType.FIRST_CYCLE)));
        assertTrue(cycles.contains(root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE)));
        assertFalse(cycles.contains(externalCycle));

        // the external root mixes its own first cycle with the master's second cycle as a single external group
        final Collection<CycleCurriculumGroup> externalCycles = externalRoot.getCycleCurriculumGroups();
        assertEquals(2, externalCycles.size());
        assertTrue(externalCycles.contains(externalCycle));
        assertTrue(externalCycles.contains(externalRoot.getCycleCurriculumGroup(CycleType.FIRST_CYCLE)));
    }

    @Test
    public void testRootCurriculumGroup_GetCycleCurriculumGroup() {
        assertSame(CycleType.FIRST_CYCLE, root.getCycleCurriculumGroup(CycleType.FIRST_CYCLE).getCycleType());
        assertSame(CycleType.SECOND_CYCLE, root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE).getCycleType());
        assertNull(root.getCycleCurriculumGroup(CycleType.THIRD_CYCLE));
        assertSame(externalCycle, externalRoot.getCycleCurriculumGroup(externalCycle.getCycleType()));
    }

    @Test
    public void testRootCurriculumGroup_GetInternalCycleCurriculumGroups() {
        final List<CycleCurriculumGroup> internal = root.getInternalCycleCurriculumGroups();
        assertEquals(2, internal.size());
        assertTrue(internal.contains(root.getCycleCurriculumGroup(CycleType.FIRST_CYCLE)));
        assertTrue(internal.contains(root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE)));

        final List<CycleCurriculumGroup> externalRootInternals = externalRoot.getInternalCycleCurriculumGroups();
        assertEquals(1, externalRootInternals.size());
        assertTrue(externalRootInternals.contains(externalRoot.getCycleCurriculumGroup(CycleType.FIRST_CYCLE)));
        assertFalse(externalRootInternals.contains(externalCycle));
    }

    @Test
    public void testRootCurriculumGroup_GetExternalCycleCurriculumGroups() {
        final List<ExternalCurriculumGroup> external = externalRoot.getExternalCycleCurriculumGroups();
        assertEquals(1, external.size());
        assertTrue(external.contains(externalCycle));
        assertTrue(root.getExternalCycleCurriculumGroups().isEmpty());
    }

    @Test
    public void testRootCurriculumGroup_HasExternalCycles() {
        // two-cycle root without externals has no external cycles
        assertFalse(root.hasExternalCycles());

        // single-cycle bachelor with a master's second cycle as external has external cycles
        assertTrue(externalRoot.hasExternalCycles());
    }

    @Test
    public void testRootCurriculumGroup_GetFirstOrderedCycleCurriculumGroup() {
        // two-cycle root: first ordered cycle is FIRST_CYCLE (lower weight)
        assertSame(root.getCycleCurriculumGroup(CycleType.FIRST_CYCLE), root.getFirstOrderedCycleCurriculumGroup());

        // external root with one internal cycle: returns that cycle
        assertSame(externalRoot.getCycleCurriculumGroup(CycleType.FIRST_CYCLE),
                externalRoot.getFirstOrderedCycleCurriculumGroup());
    }

    @Test
    public void testRootCurriculumGroup_GetLastOrderedCycleCurriculumGroup() {
        // two-cycle root: last ordered cycle is SECOND_CYCLE (higher weight)
        assertSame(root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE), root.getLastOrderedCycleCurriculumGroup());

        // external root with one internal cycle: returns that cycle
        assertSame(externalRoot.getCycleCurriculumGroup(CycleType.FIRST_CYCLE),
                externalRoot.getLastOrderedCycleCurriculumGroup());
    }

    @Test
    public void testRootCurriculumGroup_GetLastConcludedCycleCurriculumGroup() {
        // root has both cycles concluded, last ordered cycle is SECOND_CYCLE
        assertSame(root.getCycleCurriculumGroup(CycleType.SECOND_CYCLE), root.getLastConcludedCycleCurriculumGroup());

        // externalRoot cycles have no conclusion rules, so nothing is concluded
        assertNull(externalRoot.getLastConcludedCycleCurriculumGroup());
    }

    @Test
    public void testRootCurriculumGroup_HasConcludedCycle() {
        // with isSkipValidation + credits limit on CycleCourseGroup: concluded
        assertTrue(root.hasConcludedCycle(CycleType.FIRST_CYCLE));
        assertTrue(root.hasConcludedCycle(CycleType.FIRST_CYCLE, executionYear));
        assertTrue(root.hasConcludedCycle(CycleType.SECOND_CYCLE));
        assertTrue(root.hasConcludedCycle(CycleType.SECOND_CYCLE, executionYear));
        assertTrue(root.hasConcludedCycle(null));
        assertTrue(root.hasConcludedCycle(null, executionYear));

        // externalRoot cycles have no conclusion rules: not concluded
        assertFalse(externalRoot.hasConcludedCycle(CycleType.FIRST_CYCLE));
        assertFalse(externalRoot.hasConcludedCycle(CycleType.FIRST_CYCLE, executionYear));

        // with null cycle type: not concluded as well
        assertFalse(externalRoot.hasConcludedCycle(null));
        assertFalse(externalRoot.hasConcludedCycle(null, executionYear));

        // with cycle type not present in degree type: returns false
        assertFalse(root.hasConcludedCycle(CycleType.THIRD_CYCLE));
        assertFalse(root.hasConcludedCycle(CycleType.THIRD_CYCLE, executionYear));
    }

    private static RootCurriculumGroup createRoot(final Student student, final DegreeCurricularPlan dcp) {
        final Registration registration = ConclusionRulesTestUtil.createRegistration(student, dcp, executionYear);
        return registration.getLastStudentCurricularPlan().getRoot();
    }

    private static DegreeCurricularPlan createDegreeWithPlan(final DegreeType degreeType, final String code, final String name) {
        final Degree degree = DegreeTest.createDegree(degreeType, code + UUID.randomUUID(), name, executionYear);
        final DegreeCurricularPlan dcp = degree.createDegreeCurricularPlan(name + " Plan",
                User.findByUsername(ConclusionRulesTestUtil.ADMIN_USERNAME).getPerson(), AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        return dcp;
    }

}