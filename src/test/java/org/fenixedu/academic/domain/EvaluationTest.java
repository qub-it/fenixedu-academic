package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import org.mockito.Mockito;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EvaluationTest {

    private static Registration registration;
    private static ExecutionInterval executionInterval;
    private static ExecutionCourse attendedExecutionCourse;
    private static ExecutionCourse otherExecutionCourse;
    private static Attends firstAttend;
    private static Attends secondAttend;
    private static Mark firstMark;
    private static Mark secondMark;
    private static Evaluation evaluation;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();
            registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
            final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
            executionInterval = ExecutionInterval.findFirstCurrentChild(scp.getDegree().getCalendar());

            attendedExecutionCourse = new ExecutionCourse("Course A", "CA", executionInterval);
            otherExecutionCourse = new ExecutionCourse("Course B", "CB", executionInterval);

            firstAttend = new Attends(registration, attendedExecutionCourse);
            secondAttend = new Attends(registration, otherExecutionCourse);

            firstMark = markFor(firstAttend);
            secondMark = markFor(secondAttend);

            evaluation = mock(Evaluation.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            when(evaluation.getMarksSet()).thenReturn(new HashSet<>(Arrays.asList(firstMark, secondMark)));
            when(evaluation.getAssociatedExecutionCoursesSet()).thenReturn(
                    new HashSet<>(Arrays.asList(attendedExecutionCourse, otherExecutionCourse)));
            return null;
        });
    }

    @Test
    public void testEvaluation_getMarkByAttend() {
        assertEquals(firstMark, evaluation.getMarkByAttend(firstAttend));
        assertEquals(secondMark, evaluation.getMarkByAttend(secondAttend));

        // return null when the Attends has no associated Mark in this Evaluation
        assertNull(evaluation.getMarkByAttend(mock(Attends.class)));

        // return null when Attends is null, instead of throwing
        assertNull(evaluation.getMarkByAttend(null));
    }

    @Test
    public void testEvaluation_getAttendingExecutionCoursesFor() {
        Registration registrationMock = mock(Registration.class);
        when(registrationMock.attends(attendedExecutionCourse)).thenReturn(true);
        when(registrationMock.attends(otherExecutionCourse)).thenReturn(false);

        List<ExecutionCourse> result = evaluation.getAttendingExecutionCoursesFor(registrationMock);
        assertEquals(1, result.size());
        assertTrue(result.contains(attendedExecutionCourse));

        when(registrationMock.attends(otherExecutionCourse)).thenReturn(true);
        result = evaluation.getAttendingExecutionCoursesFor(registrationMock);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(attendedExecutionCourse, otherExecutionCourse)));

        // return all associated courses when the registration attends none of them
        when(registrationMock.attends(attendedExecutionCourse)).thenReturn(false);
        when(registrationMock.attends(otherExecutionCourse)).thenReturn(false);
        result = evaluation.getAttendingExecutionCoursesFor(registrationMock);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(attendedExecutionCourse, otherExecutionCourse)));

        // return an empty list when there are no associated courses at all
        evaluation.getAssociatedExecutionCoursesSet().clear();
        when(registrationMock.attends(any())).thenReturn(false);
        assertTrue(evaluation.getAttendingExecutionCoursesFor(registrationMock).isEmpty());
    }

    @Test
    public void testEvaluation_delete() {
        final Set<Mark> marksSet = new HashSet<>(Arrays.asList(firstMark, secondMark));
        final Set<ExecutionCourse> executionCoursesSet =
                new HashSet<>(Arrays.asList(attendedExecutionCourse, otherExecutionCourse));

        final Evaluation evaluationToDelete =
                mock(Evaluation.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        when(evaluationToDelete.getAssociatedExecutionCoursesSet()).thenReturn(executionCoursesSet);
        when(evaluationToDelete.getMarksSet()).thenReturn(marksSet);

        Mockito.doAnswer(invocation -> marksSet.remove(firstMark)).when(firstMark).delete();
        Mockito.doAnswer(invocation -> marksSet.remove(secondMark)).when(secondMark).delete();
        Mockito.doNothing().when(evaluationToDelete).setRootDomainObject(any());

        evaluationToDelete.delete();

        assertTrue(executionCoursesSet.isEmpty());
        Mockito.verify(firstMark).delete();
        Mockito.verify(secondMark).delete();
        assertTrue(marksSet.isEmpty());

        Mockito.verify(evaluationToDelete).setRootDomainObject(null);
    }

    private static Mark markFor(final Attends attends) {
        final Mark result = mock(Mark.class);
        when(result.getAttend()).thenReturn(attends);
        return result;
    }

}
