package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.fenixedu.academic.domain.curricularPeriod.DegreeCurricularPlanDurationTest;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SchoolClassTest {

    private static ExecutionDegree executionDegree;
    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;
    private static ExecutionCourse ec1, ec2;
    private static DegreeCurricularPlan dcp;
    private static CurricularCourse cc1, cc2;
    private static Shift shift1, shift2;
    private static SchoolClass schoolClass;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initSchoolClassTest();
            return null;
        });
    }

    public static void initSchoolClassTest() {
        DegreeCurricularPlanTest.initDegreeCurricularPlan();
        StudentTest.initRegistrationConfigEntities();

        executionYear = ExecutionYear.findCurrent(null);
        executionInterval = executionYear.getFirstExecutionPeriod();

        final Degree degree = Degree.find(DEGREE_A_CODE);
        dcp = degree.getDegreeCurricularPlansSet().stream().filter(d -> DCP_NAME_V1.equals(d.getName())).findAny().orElseThrow();
        DegreeCurricularPlanDurationTest.populateCurricularPeriodStructure(dcp);

        cc1 = dcp.getCurricularCourseByCode(COURSE_A_CODE);
        cc2 = new CurricularCourse();
        new Context(dcp.getRoot(), cc2, dcp.getCurricularPeriodFor(2, 1, AcademicPeriod.SEMESTER), executionInterval, null);

        executionDegree = dcp.createExecutionDegree(executionYear);

        schoolClass = new SchoolClass(executionDegree, executionInterval, "SC", 1);

        ec1 = new ExecutionCourse("EC", UUID.randomUUID().toString(), executionInterval);
        ec1.addAssociatedCurricularCourses(cc1);
        ec2 = new ExecutionCourse("EC-Y2", UUID.randomUUID().toString(), executionInterval);
        ec2.addAssociatedCurricularCourses(cc2);

        shift1 = new Shift(ec1, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, "Shift");
        shift2 = new Shift(ec2, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
    }

    @Test
    public void testFindPossibleShiftsToAdd() {
        assertTrue(schoolClass.findPossibleShiftsToAdd().contains(shift1));

        // exclude shift that is already associated with the school class
        try {
            schoolClass.getAssociatedShiftsSet().add(shift1);
            assertFalse(schoolClass.findPossibleShiftsToAdd().contains(shift1));
        } finally {
            schoolClass.getAssociatedShiftsSet().remove(shift1);
        }
    }

    @Test
    public void testFindPossibleShiftsToAdd_excludesShiftsNotMatchingSchoolClass() {
        // cc2 is not active (curricular year 2)
        assertFalse(schoolClass.findPossibleShiftsToAdd().contains(shift2));
        assertFalse(cc2.isActive(executionInterval, schoolClass.getCurricularYear()));

        // otherShift is on second semester, school class is on first
        ExecutionInterval otherInterval = executionYear.getLastExecutionPeriod();
        ExecutionCourse otherEc = new ExecutionCourse("EC-Other", UUID.randomUUID().toString(), otherInterval);
        otherEc.addAssociatedCurricularCourses(cc1);
        Shift otherShift = new Shift(otherEc, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);

        assertFalse(schoolClass.findPossibleShiftsToAdd().contains(otherShift));
        assertNotEquals(otherEc.getExecutionInterval(), schoolClass.getExecutionInterval());
    }

    @Test
    public void testReplaceSchoolClass_enrolInOneShift() {
        Student student = StudentTest.createStudent("enrol-one", UUID.randomUUID().toString());
        Registration reg = StudentTest.createRegistration(student, dcp, executionYear);
        new Attends(reg, ec1);

        // shift has capacity, enrols student registration
        schoolClass.getAssociatedShiftsSet().add(shift1);
        SchoolClass.replaceSchoolClass(reg, schoolClass, executionInterval);
        assertTrue(reg.findSchoolClass(executionInterval).filter(sc -> sc == schoolClass).isPresent());
        assertTrue(reg.getShiftsSet().contains(shift1));

        // student already enrolled, replaceSchoolClass still works (unenrols + re-enrols)
        try {
            SchoolClass.replaceSchoolClass(reg, schoolClass, executionInterval);
            assertTrue(reg.findSchoolClass(executionInterval).filter(sc -> sc == schoolClass).isPresent());
            assertTrue(reg.getShiftsSet().contains(shift1));
        } finally {
            SchoolClass.replaceSchoolClass(reg, null, executionInterval);
            assertTrue(reg.findSchoolClass(executionInterval).isEmpty());
            schoolClass.getAssociatedShiftsSet().clear();
        }

        // all shifts full, throws DomainException
        try {
            Shift fullShift = new Shift(ec1, CourseLoadType.of(CourseLoadType.THEORETICAL), 0, "FS-" + UUID.randomUUID());
            schoolClass.getAssociatedShiftsSet().add(fullShift);
            assertThrows(DomainException.class, () -> SchoolClass.replaceSchoolClass(reg, schoolClass, executionInterval));
        } finally {
            SchoolClass.replaceSchoolClass(reg, null, executionInterval);
            schoolClass.getAssociatedShiftsSet().clear();
        }

        // first shift full, second available, enrols in second
        try {
            Shift fullShift = new Shift(ec1, CourseLoadType.of(CourseLoadType.THEORETICAL), 0, "F-" + UUID.randomUUID());
            Shift availableShift = new Shift(ec1, CourseLoadType.of(CourseLoadType.THEORETICAL), 5, "A-" + UUID.randomUUID());
            schoolClass.getAssociatedShiftsSet().add(fullShift);
            schoolClass.getAssociatedShiftsSet().add(availableShift);
            SchoolClass.replaceSchoolClass(reg, schoolClass, executionInterval);
            assertTrue(reg.findSchoolClass(executionInterval).filter(sc -> sc == schoolClass).isPresent());
            assertTrue(reg.getShiftsSet().contains(availableShift));
            assertFalse(reg.getShiftsSet().contains(fullShift));
        } finally {
            schoolClass.getAssociatedShiftsSet().clear();
        }
    }
}