package org.fenixedu.academic.domain.schedule.shiftCapacity;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.util.UUID;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class ShiftCapacityTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initStudentAndRegistration();
            return null;
        });
    }

    @Test
    public void testAcceptsWithDCP() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final ExecutionYear executionYear = ExecutionYear.findCurrent(degree.getCalendar());

        final DegreeCurricularPlan dcp1 =
                new DegreeCurricularPlan(degree, UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        final DegreeCurricularPlan dcp2 =
                new DegreeCurricularPlan(degree, UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);

        dcp1.setCurricularStage(CurricularStage.APPROVED);
        final ExecutionDegree executionDegree = dcp1.createExecutionDegree(executionYear);

        final Student student = StudentTest.createStudent("Test Student", UUID.randomUUID().toString());
        final Registration registration = StudentTest.createRegistration(student, dcp1, executionYear);

        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();
        final SchoolClass schoolClass = new SchoolClass(executionDegree, executionInterval, "A", 1);

        final ExecutionCourse executionCourse =
                new ExecutionCourse("Test Course", UUID.randomUUID().toString(), executionInterval);
        final Shift shift = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        final ShiftCapacity shiftCapacity = new ShiftCapacity(shift, ShiftCapacityType.findOrCreateDefault(), 10);
        final ShiftCapacity shiftCapacityNegation = new ShiftCapacity(shift, ShiftCapacityType.findOrCreateDefault(), 10);
        shiftCapacityNegation.setNegation(true);

        // both capacities accept the registration by default
        assertTrue(shiftCapacity.accepts(registration));
        assertTrue(shiftCapacityNegation.accepts(registration));

        // add a different DCP (dcp2) - regular capacity should now reject, negation still accepts
        shiftCapacity.addDegreeCurricularPlans(dcp2);
        shiftCapacityNegation.addDegreeCurricularPlans(dcp2);
        assertFalse(shiftCapacity.accepts(registration));
        assertTrue(shiftCapacityNegation.accepts(registration));

        // add the school class restriction - regular capacity remains rejecting, negation still accepts
        shiftCapacity.addSchoolClasses(schoolClass);
        shiftCapacityNegation.addSchoolClasses(schoolClass);
        assertFalse(shiftCapacity.accepts(registration));
        assertTrue(shiftCapacityNegation.accepts(registration));

        // add the registration's own DCP (dcp1) - regular capacity accepts again, negation now rejects
        shiftCapacity.addDegreeCurricularPlans(dcp1);
        shiftCapacityNegation.addDegreeCurricularPlans(dcp1);
        assertTrue(shiftCapacity.accepts(registration));
        assertFalse(shiftCapacityNegation.accepts(registration));
    }

    @Test
    public void testAcceptsWithSchoolClass() {

        final Degree degree = Degree.find(DEGREE_A_CODE);
        final DegreeCurricularPlan dcp1 =
                new DegreeCurricularPlan(degree, UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);
        final DegreeCurricularPlan dcp2 =
                new DegreeCurricularPlan(degree, UUID.randomUUID().toString(), AcademicPeriod.THREE_YEAR);

        final ExecutionYear executionYear = ExecutionYear.findCurrent(dcp1.getDegree().getCalendar());
        dcp1.setCurricularStage(CurricularStage.APPROVED);
        final ExecutionDegree executionDegree = dcp1.createExecutionDegree(executionYear);

        final Student student = StudentTest.createStudent("Test Student", UUID.randomUUID().toString());
        final Registration registration = StudentTest.createRegistration(student, dcp1, executionYear);

        final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();
        final SchoolClass schoolClass1 = new SchoolClass(executionDegree, executionInterval, "A", 1);
        final SchoolClass schoolClass2 = new SchoolClass(executionDegree, executionInterval, "B", 1);

        final ExecutionCourse executionCourse =
                new ExecutionCourse("Test Course", UUID.randomUUID().toString(), executionInterval);
        final Shift shift = new Shift(executionCourse, CourseLoadType.of(CourseLoadType.THEORETICAL), 10, null);
        final ShiftCapacity shiftCapacity = new ShiftCapacity(shift, ShiftCapacityType.findOrCreateDefault(), 10);
        final ShiftCapacity shiftCapacityNegation = new ShiftCapacity(shift, ShiftCapacityType.findOrCreateDefault(), 10);
        shiftCapacityNegation.setNegation(true);

        // both capacities accept the registration by default
        assertTrue(shiftCapacity.accepts(registration));
        assertTrue(shiftCapacityNegation.accepts(registration));

        // restrict to two specific school classes (registration not in them) -> regular capacity rejects
        shiftCapacity.addSchoolClasses(schoolClass1);
        shiftCapacity.addSchoolClasses(schoolClass2);
        assertFalse(shiftCapacity.accepts(registration));

        // negation capacity: adding school classes means "exclude these" -> still accepts the registration
        shiftCapacityNegation.addSchoolClasses(schoolClass1);
        shiftCapacityNegation.addSchoolClasses(schoolClass2);
        assertTrue(shiftCapacityNegation.accepts(registration));

        // add a different DCP (dcp2) - regular capacity still rejects (registration's DCP not included),
        // negation capacity continues to accept
        shiftCapacity.addDegreeCurricularPlans(dcp2);
        shiftCapacityNegation.addDegreeCurricularPlans(dcp2);
        assertFalse(shiftCapacity.accepts(registration));
        assertTrue(shiftCapacityNegation.accepts(registration));

        // enrol the registration into one of the allowed school classes -> regular accepts, negation rejects
        SchoolClass.replaceSchoolClass(registration, schoolClass1, executionInterval);
        assertTrue(shiftCapacity.accepts(registration));
        assertFalse(shiftCapacityNegation.accepts(registration));

        // include the registration's own DCP (dcp1) in the restrictions -> regular accepts, negation rejects
        shiftCapacity.addDegreeCurricularPlans(dcp1);
        shiftCapacityNegation.addDegreeCurricularPlans(dcp1);
        assertTrue(shiftCapacity.accepts(registration));
        assertFalse(shiftCapacityNegation.accepts(registration));
    }

}
