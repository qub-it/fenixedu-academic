package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.fenixedu.academic.domain.student.Registration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import org.mockito.Mockito;

@RunWith(FenixFrameworkRunner.class)
public class EvaluationTest {

    private Evaluation evaluation;
    private Attends firstAttend;
    private Attends secondAttend;
    private Mark firstMark;
    private Mark secondMark;
    private ExecutionCourse attendedExecutionCourse;
    private ExecutionCourse otherExecutionCourse;

    @Before
    public void setUp() {
        final Attends unrequestedAttend = mock(Attends.class);
        final Mark unrequestedMark = markFor(unrequestedAttend);

        firstAttend = mock(Attends.class);
        secondAttend = mock(Attends.class);
        firstMark = markFor(firstAttend);
        secondMark = markFor(secondAttend);

        evaluation = evaluationWithMarks(firstMark, secondMark, unrequestedMark);

        attendedExecutionCourse = mock(ExecutionCourse.class);
        otherExecutionCourse = mock(ExecutionCourse.class);
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
        Evaluation courseEvaluation = evaluationWithExecutionCourses(attendedExecutionCourse, otherExecutionCourse);
        Registration registration = registrationAttendingTo(attendedExecutionCourse);
        List<ExecutionCourse> result = courseEvaluation.getAttendingExecutionCoursesFor(registration);
        assertEquals(1, result.size());
        assertTrue(result.contains(attendedExecutionCourse));

        registration = registrationAttendingTo(attendedExecutionCourse, otherExecutionCourse);
        result = courseEvaluation.getAttendingExecutionCoursesFor(registration);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(attendedExecutionCourse, otherExecutionCourse)));

        // return all associated courses when the registration attends none of them
        registration = registrationAttendingTo();
        result = courseEvaluation.getAttendingExecutionCoursesFor(registration);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(attendedExecutionCourse, otherExecutionCourse)));

        // return an empty list when there are no associated courses at all
        courseEvaluation = evaluationWithExecutionCourses();
        registration = registrationAttendingTo(attendedExecutionCourse);
        assertTrue(courseEvaluation.getAttendingExecutionCoursesFor(registration).isEmpty());
    }

    private Evaluation newEvaluationMock() {
        return mock(Evaluation.class, Mockito.withSettings().defaultAnswer(CALLS_REAL_METHODS));
    }

    private Evaluation evaluationWithMarks(final Mark... marks) {
        final Evaluation result = newEvaluationMock();
        when(result.getMarksSet()).thenReturn(new HashSet<>(Arrays.asList(marks)));
        return result;
    }

    private Evaluation evaluationWithExecutionCourses(final ExecutionCourse... executionCourses) {
        final Evaluation result = newEvaluationMock();
        when(result.getAssociatedExecutionCoursesSet()).thenReturn(new HashSet<>(Arrays.asList(executionCourses)));
        return result;
    }

    private Registration registrationAttendingTo(final ExecutionCourse... attendedExecutionCourses) {
        final Registration result = mock(Registration.class);
        when(result.attends(any())).thenAnswer(invocation ->
                Arrays.asList(attendedExecutionCourses).contains(invocation.getArgument(0, ExecutionCourse.class)));
        return result;
    }

    private Mark markFor(final Attends attends) {
        final Mark result = mock(Mark.class);
        when(result.getAttend()).thenReturn(attends);
        return result;
    }

}