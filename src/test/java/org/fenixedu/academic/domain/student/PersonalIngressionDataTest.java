package org.fenixedu.academic.domain.student;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PersonalIngressionDataTest {

    private static Student student;
    private static PersonalIngressionData pid2019, pid2020;
    private static ExecutionYear year2019, year2020, year2021;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            student = StudentTest.createStudent("Student A", "student.a");

            year2020 = ExecutionYear.findCurrent(null);
            year2019 = year2020.getPrevious().getExecutionYear();
            year2021 = year2020.getNext().getExecutionYear();
            return null;
        });
    }

    @Before
    public void setUp(){
        student.getPersonalIngressionsDataSet().forEach(PersonalIngressionData::delete);
        pid2019 = new PersonalIngressionData(student, year2019);
        pid2020 = new PersonalIngressionData(student, year2020);
    }

    @Test
    public void testComparatorByExecutionYear() {
        assertTrue(PersonalIngressionData.COMPARATOR_BY_EXECUTION_YEAR.compare(pid2019, pid2020) < 0);
        assertTrue(PersonalIngressionData.COMPARATOR_BY_EXECUTION_YEAR.compare(pid2020, pid2019) > 0);
        assertEquals(0, PersonalIngressionData.COMPARATOR_BY_EXECUTION_YEAR.compare(pid2020, pid2020));
    }

    @Test
    public void testSetExecutionYear() {
        pid2019.setExecutionYear(year2021);
        assertEquals(year2021, pid2019.getExecutionYear());

        pid2019.setExecutionYear(null);
        assertNull(pid2019.getExecutionYear());

        // moving to a year already held by another PID for the same student throws
        assertThrows(DomainException.class, () -> pid2019.setExecutionYear(year2020));

        // no student bound, duplicate check never triggers
        final PersonalIngressionData pidNoStudent = new PersonalIngressionData();
        pidNoStudent.setExecutionYear(year2020);
        assertEquals(year2020, pidNoStudent.getExecutionYear());
    }

    @Test
    public void testSetStudent() {
        final Student studentB = StudentTest.createStudent("Student B", "student.setsb");
        final PersonalIngressionData pidStudent = new PersonalIngressionData();
        pidStudent.setExecutionYear(year2020);

        pidStudent.setStudent(studentB);
        assertEquals(studentB, pidStudent.getStudent());

        pidStudent.setStudent(null);
        assertNull(pidStudent.getStudent());

        // moving to a student who already owns this year throws
        assertThrows(DomainException.class, () -> pidStudent.setStudent(student));

        // no execution year bound, duplicate check never triggers
        final PersonalIngressionData pidNoYear = new PersonalIngressionData();
        pidNoYear.setStudent(student);
        assertEquals(student, pidNoYear.getStudent());
    }
}