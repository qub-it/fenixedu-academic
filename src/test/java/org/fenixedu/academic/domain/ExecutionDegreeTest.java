package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ExecutionDegreeTest {

    private static ExecutionDegree edA, edB, edC, edA2, edA_new, edMaster;
    private static DegreeType degreeType, masterDegreeType;
    private static ExecutionYear currentYear, nextYear;
    private static Person personA1, personA2, personB;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initExecutionDegrees();
            return null;
        });
    }

    private static void initExecutionDegrees() {
        DegreeTest.initDegree();

        currentYear = ExecutionYear.findCurrent(null);
        nextYear = currentYear.getNext().getExecutionYear();

        degreeType = DegreeType.findByCode(DegreeTest.DEGREE_TYPE_CODE).orElseThrow();
        masterDegreeType = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).orElseThrow();

        Degree degreeA = DegreeTest.createDegree(degreeType, "AAA", "Degree A", currentYear);
        Degree degreeB = DegreeTest.createDegree(degreeType, "BBB", "Degree B", currentYear);
        Degree degreeC = DegreeTest.createDegree(degreeType, "CCC", "Degree C", currentYear);
        Degree degreeA2 = DegreeTest.createDegree(degreeType, " AAA ", "Degree A2", currentYear);
        Degree degreeMaster = DegreeTest.createDegree(masterDegreeType, "MST", "Master A", currentYear);

        edA = createExecutionDegree(degreeA, "DCP_A", currentYear);
        edA_new = createExecutionDegree(degreeA, "DCP_A_NEW", nextYear);
        edB = createExecutionDegree(degreeB, "DCP_B", currentYear);
        edC = createExecutionDegree(degreeC, "DCP_C", currentYear);
        edA2 = createExecutionDegree(degreeA2, "DCP_A2", currentYear);
        edMaster = createExecutionDegree(degreeMaster, "DCP_MASTER", currentYear);

        personA1 = createPerson("coordinatorA1");
        personA2 = createPerson("coordinatorA2");
        personB = createPerson("coordinatorB");
        Coordinator.createCoordinator(edA, personA1, true);   // responsible
        Coordinator.createCoordinator(edA, personA2, false);   // non-responsible
        Coordinator.createCoordinator(edB, personB, false);   // non-responsible

    }

    private static ExecutionDegree createExecutionDegree(Degree degree, String dcpName, ExecutionYear year) {
        DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR, year.getFirstExecutionPeriod());
        dcp.setCurricularStage(CurricularStage.APPROVED);
        return dcp.createExecutionDegree(year);
    }

    private static Person createPerson(final String username) {
        final UserProfile profile = new UserProfile("User", "", "User", username + "@test.com", Locale.getDefault());
        new User(username, profile);
        return new Person(profile);
    }

    @Test
    public void comparatorByDegreeName() {
        List<ExecutionDegree> sorted = Stream.of(edC, edA, edB, edA2).sorted(ExecutionDegree.COMPARATOR_BY_DEGREE_NAME).toList();

        assertEquals(4, sorted.size());
        assertEquals(edA, sorted.get(0));  // "Degree A" first
        assertEquals(edA2, sorted.get(1)); // "Degree A2" second
        assertEquals(edB, sorted.get(2));  // "Degree B" third
        assertEquals(edC, sorted.get(3));  // "Degree C" fourth
    }

    @Test
    public void comparatorByExecutionYear() {
        List<ExecutionDegree> sorted =
                Stream.of(edA_new, edA).sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_YEAR).toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));  // 2019/2020
        assertEquals(edA_new, sorted.get(1));  // 2021/2022
    }

    @Test
    public void comparatorByDegreeTypeAndName_differentTypes() {
        List<ExecutionDegree> sorted =
                Stream.of(edMaster, edA).sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME)
                        .toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));       // "Degree" < "Master Degree"
        assertEquals(edMaster, sorted.get(1));
    }

    @Test
    public void comparatorByDegreeTypeAndName_sameType() {
        List<ExecutionDegree> sorted =
                Stream.of(edC, edA, edB).sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME)
                        .toList();

        assertEquals(3, sorted.size());
        assertEquals(edA, sorted.get(0));  // "Degree A"
        assertEquals(edB, sorted.get(1));  // "Degree B"
        assertEquals(edC, sorted.get(2));  // "Degree C"
    }

    @Test
    public void comparatorByDegreeTypeAndNameAndExecutionYear() {
        List<ExecutionDegree> sorted = Stream.of(edA_new, edA)
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR).toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));     // 2019/2020
        assertEquals(edA_new, sorted.get(1)); // 2021/2022
    }

    @Test
    public void comparatorByDegreeTypeAndNameAndExecutionYear_differentTypes() {
        List<ExecutionDegree> sorted = Stream.of(edMaster, edA)
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR).toList();

        assertEquals(2, sorted.size());
        assertEquals(edA, sorted.get(0));       // "Degree" < "Master Degree"
        assertEquals(edMaster, sorted.get(1));
    }

    @Test
    public void getCoordinatorByTeacher_returnsCoordinator() {
        Coordinator coordinatorA1 = edA.getCoordinatorByTeacher(personA1);
        Coordinator coordinatorA2 = edA.getCoordinatorByTeacher(personA2);

        assertEquals(personA1, coordinatorA1.getPerson());
        assertEquals(personA2, coordinatorA2.getPerson());
    }

    @Test
    public void getCoordinatorByTeacher_returnsNull() {
        Person person = createPerson("notCoordinator");
        Coordinator notCoordinator = edA.getCoordinatorByTeacher(person);
        assertNull(notCoordinator);

        // coordinator for another execution degree
        Coordinator coordinatorB = edA.getCoordinatorByTeacher(personB);
        assertNull(coordinatorB);
    }

    @Test
    public void getAllByExecutionYearAndDegreeType_returnsExecutionDegrees() {
        Collection<ExecutionDegree> resultByType = ExecutionDegree.getAllByExecutionYearAndDegreeType(currentYear, degreeType);
        assertEquals(4, resultByType.size()); // edA, edB, edC, edA2
        assertTrue(resultByType.stream().allMatch(ed -> ed.getDegreeType() == degreeType));

        Collection<ExecutionDegree> resultByMultipleTypes =
                ExecutionDegree.getAllByExecutionYearAndDegreeType(currentYear, degreeType, masterDegreeType);
        assertEquals(5, resultByMultipleTypes.size()); // all execution degrees except for edA_new (nextYear)
        assertFalse(resultByMultipleTypes.contains(edA_new));
    }

    @Test
    public void getAllByExecutionYearAndDegreeType_returnsEmpty() {
        assertTrue(ExecutionDegree.getAllByExecutionYearAndDegreeType(null, degreeType).isEmpty());
        assertTrue(ExecutionDegree.getAllByExecutionYearAndDegreeType(currentYear, (DegreeType[]) null).isEmpty());
    }

    @Test
    public void getByDegreeCurricularPlanAndExecutionYear_returnsExecutionDegree() {
        DegreeCurricularPlan dcpA = edA.getDegreeCurricularPlan();
        ExecutionYear year = edA.getExecutionYear();

        ExecutionDegree result = ExecutionDegree.getByDegreeCurricularPlanAndExecutionYear(dcpA, year);
        assertEquals(edA, result);
    }

    @Test
    public void getByDegreeCurricularPlanAndExecutionYear_returnsNull() {
        DegreeCurricularPlan dcpA = edA.getDegreeCurricularPlan(); // DCP_A — only has edA in currentYear
        ExecutionDegree result = ExecutionDegree.getByDegreeCurricularPlanAndExecutionYear(dcpA, nextYear);
        assertNull(result);

        assertNull(ExecutionDegree.getByDegreeCurricularPlanAndExecutionYear(null, currentYear));
        assertNull(ExecutionDegree.getByDegreeCurricularPlanAndExecutionYear(dcpA, null));
    }

    @Test
    public void getResponsibleCoordinators_returnsResponsibleCoordinators() {
        List<Coordinator> result = edA.getResponsibleCoordinators();

        assertEquals(1, result.size());
        assertEquals(personA1, result.get(0).getPerson());
        assertTrue(result.get(0).getResponsible());
    }

    @Test
    public void getResponsibleCoordinators_returnsEmpty() {
        // has coordinator but it's not responsible
        assertTrue(edB.getResponsibleCoordinators().isEmpty());
        // does not have any coordinators
        assertTrue(edC.getResponsibleCoordinators().isEmpty());
    }

    @Test
    public void getSortedSchoolClasses_returnsSchoolClassesSortedByName() {
        SchoolClass classB = new SchoolClass(edA, currentYear.getFirstExecutionPeriod(), "B", 1);
        SchoolClass classA = new SchoolClass(edA, currentYear.getFirstExecutionPeriod(), "A", 1);
        SchoolClass classC = new SchoolClass(edA, currentYear.getFirstExecutionPeriod(), "C", 1);

        SortedSet<SchoolClass> result = edA.getSortedSchoolClasses();

        assertEquals(List.of(classA, classB, classC), new ArrayList<>(result));
    }
}
