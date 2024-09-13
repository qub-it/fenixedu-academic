package org.fenixedu.academic.domain;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class AttendsTest {

    private static Registration registration;

    private static CurricularCourse curricularCourse;

    private static ExecutionInterval executionInterval;

    private static final String ADMIN_USERNAME = "admin";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EnrolmentTest.initEnrolments();
            initLocalVars();
            return null;
        });
    }

    private static void initLocalVars() {
        registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        executionInterval = ExecutionInterval.findFirstCurrentChild(scp.getDegree().getCalendar());
        curricularCourse = scp.getDegreeCurricularPlan().getCurricularCourseByCode(CompetenceCourseTest.COURSE_A_CODE);
    }

    @Test
    public void find() {
        final Set<Attends> attends = registration.getAssociatedAttendsSet();

        final Set<String> attendsCoursesCodes =
                attends.stream().map(a -> a.getExecutionCourse().getCode()).distinct().collect(Collectors.toSet());
        assertTrue(attendsCoursesCodes.contains(COURSE_A_CODE));

        curricularCourse.findExecutionCourses(executionInterval).forEach(executionCourse -> {
            final Optional<Attends> attendsOpt = registration.findAttends(executionCourse);
            assertTrue(attendsOpt.isPresent());
            assertTrue(attends.contains(attendsOpt.get()));
        });
    }

    @Test
    public void duplicateExecutionInterval() {
        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.Attends.enrolmentAlreadyHasAttendsForExecutionInterval");

        for (Attends attends : registration.getAssociatedAttendsSet()) {
            attends.getExecutionInterval();
            final String uuid = UUID.randomUUID().toString();
            attends.getEnrolment().findOrCreateAttends(new ExecutionCourse(uuid, uuid, executionInterval));
        }
    }

    @Test
    public void existingForExecutionCourse() {
        for (Attends attends : registration.getAssociatedAttendsSet()) {
            final Attends foundAttends = attends.getEnrolment().findOrCreateAttends(attends.getExecutionCourse());
            assertEquals(attends, foundAttends);
        }
    }

    @Test
    public void existingButWithoutEnrolment() {
        for (Attends attends : registration.getAssociatedAttendsSet()) {
            final Enrolment enrolment = attends.getEnrolment();
            attends.setEnrolment(null);

            final Attends foundAttends = enrolment.findOrCreateAttends(attends.getExecutionCourse());
            assertEquals(attends, foundAttends);
        }
    }

    @Test
    public void existingButWithEnrolment() {
        for (Attends attends : registration.getAssociatedAttendsSet()) {
            final Enrolment enrolment = attends.getEnrolment();
            attends.setEnrolment(new Enrolment());

            Attends foundAttends = null;
            try {
                foundAttends = enrolment.findOrCreateAttends(attends.getExecutionCourse());
            } catch (DomainException e) {
                attends.setEnrolment(enrolment); // rollback in order to other tests run in initial conditions
                assertEquals(e.getMessage(), "error.cannot.create.multiple.enrolments.for.student.in.execution.course");
            }
            assertNull(foundAttends);
        }
    }

    @Test
    public void existingButWithEnrolmentAnnulled() {
        for (Attends attends : registration.getAssociatedAttendsSet()) {
            final Enrolment enrolment = attends.getEnrolment();

            final Enrolment newEnrolment = new Enrolment();
            newEnrolment.annul();
            attends.setEnrolment(newEnrolment);

            assertTrue(enrolment.getAttendsSet().isEmpty());

            final Attends foundAttends = enrolment.findOrCreateAttends(attends.getExecutionCourse());
            assertEquals(attends, foundAttends);
            assertFalse(enrolment.getAttendsSet().isEmpty());
        }
    }

    @Test
    public void createdOnPostExecutionCourseCreation() {
        final Enrolment enrolment = createAdhocEnrolmentWithoutAttends();
        assertTrue(enrolment.getAttendsSet().isEmpty());

        final CurricularCourse curricularCourse = enrolment.getCurricularCourse();
        final ExecutionCourse executionCourse =
                new ExecutionCourse(curricularCourse.getName(), curricularCourse.getCode(), executionInterval);
        executionCourse.addAssociatedCurricularCourses(curricularCourse);
        assertEquals(enrolment.getAttendsSet().size(), 1);

        final Attends attends = enrolment.getAttendsSet().iterator().next();
        assertEquals(enrolment.findOrCreateAttends(executionCourse), attends);
    }

    @Test
    public void createdOnMultiplePostExecutionCourseCreationAndEnrolmentAnnulation() {
        final Enrolment enrolment1 = createAdhocEnrolmentWithoutAttends();
        final CurricularCourse curricularCourse1 = enrolment1.getCurricularCourse();
        final ExecutionCourse executionCourse =
                new ExecutionCourse(curricularCourse1.getName(), curricularCourse1.getCode(), executionInterval);
        executionCourse.addAssociatedCurricularCourses(curricularCourse1);
        final Attends attends1 = enrolment1.getAttendsSet().iterator().next();

        final Enrolment enrolment2 = createAdhocEnrolmentWithoutAttends();
        final CurricularCourse curricularCourse2 = enrolment2.getCurricularCourse();
        executionCourse.addAssociatedCurricularCourses(curricularCourse2);

        assertEquals(enrolment1.getAttendsSet().size(), 1);
        assertTrue(enrolment2.getAttendsSet().isEmpty()); // not created because student already had attends for execution course

        try {
            enrolment2.findOrCreateAttends(executionCourse);
        } catch (DomainException e) {
            assertEquals(e.getMessage(), "error.cannot.create.multiple.enrolments.for.student.in.execution.course");
        }

        enrolment1.annul();
        final Attends attends2 = enrolment2.findOrCreateAttends(executionCourse);

        assertTrue(enrolment1.getAttendsSet().isEmpty());
        assertEquals(enrolment2.getAttendsSet().size(), 1);
        assertEquals(attends1, attends2);
    }

    @Test
    public void existingFromAnnulledEnrolmentOnPostExecutionCourseCreation() {
        final Enrolment enrolment1 = createAdhocEnrolmentWithoutAttends();

        final CurricularCourse curricularCourse1 = enrolment1.getCurricularCourse();
        final ExecutionCourse executionCourse =
                new ExecutionCourse(curricularCourse1.getName(), curricularCourse1.getCode(), executionInterval);
        executionCourse.addAssociatedCurricularCourses(curricularCourse1);

        assertEquals(enrolment1.getAttendsSet().size(), 1);

        enrolment1.annul();
        assertEquals(enrolment1.getAttendsSet().size(), 1);
        final Attends attends1 = enrolment1.getAttendsSet().iterator().next();

        final Enrolment enrolment2 = createAdhocEnrolmentWithoutAttends();

        executionCourse.addAssociatedCurricularCourses(enrolment2.getCurricularCourse());

        assertTrue(enrolment1.getAttendsSet().isEmpty());
        assertEquals(enrolment2.getAttendsSet().size(), 1);
        assertTrue(enrolment2.getAttendsSet().contains(attends1));
    }

    @Test
    public void addAttendsForDifferentExecutionInterval() {
        final Enrolment enrolment = createAdhocEnrolmentWithoutAttends();

        final CurricularCourse curricularCourse = enrolment.getCurricularCourse();
        final ExecutionCourse executionCourse =
                new ExecutionCourse(curricularCourse.getName(), curricularCourse.getCode(), executionInterval);
        executionCourse.addAssociatedCurricularCourses(curricularCourse);

        assertEquals(enrolment.getAttendsSet().size(), 1);

        final ExecutionInterval executionIntervalNext = executionInterval.getNext();
        final ExecutionCourse executionCourseNext =
                new ExecutionCourse(curricularCourse.getName(), curricularCourse.getCode(), executionIntervalNext);
        enrolment.findOrCreateAttends(executionCourseNext);

        assertEquals(enrolment.getAttendsSet().size(), 2);

        final Attends attends = enrolment.findAttends(executionInterval).orElseThrow();
        final Attends attendsNext = enrolment.findAttends(executionIntervalNext).orElseThrow();

        assertEquals(attends.getExecutionCourse(), executionCourse);
        assertEquals(attendsNext.getExecutionCourse(), executionCourseNext);
        assertTrue(enrolment.findAttends(executionIntervalNext.getNext()).isEmpty());
        assertTrue(enrolment.findAttends(executionInterval.getExecutionYear()).isEmpty());

        assertEquals(enrolment.getExecutionCourseFor(executionInterval), executionCourse);
        assertEquals(enrolment.getExecutionCourseFor(executionIntervalNext), executionCourseNext);
        assertNull(enrolment.getExecutionCourseFor(executionIntervalNext.getNext()));
        assertNull(enrolment.getExecutionCourseFor(executionInterval.getExecutionYear()));

        final ExecutionCourse executionCoursePrevious =
                new ExecutionCourse(curricularCourse.getName(), curricularCourse.getCode(), executionInterval.getPrevious());
        assertEquals(enrolment.getAttendsByExecutionCourse(executionCourse), attends);
        assertEquals(enrolment.getAttendsByExecutionCourse(executionCourseNext), attendsNext);
        assertNull(enrolment.getAttendsByExecutionCourse(executionCoursePrevious));

        enrolment.getAttendsSet().forEach(Attends::delete);
    }

    private static Enrolment createAdhocEnrolmentWithoutAttends() {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final String uuid = UUID.randomUUID().toString();
        final AcademicPeriod academicPeriod = executionInterval.getAcademicPeriod();
        final CompetenceCourse competenceCourse =
                CompetenceCourseTest.createCompetenceCourse(uuid, uuid, BigDecimal.TEN, academicPeriod, executionInterval,
                        coursesUnit);

        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        final DegreeCurricularPlan dcp = scp.getDegreeCurricularPlan();
        final CurricularPeriod curricularPeriod =
                dcp.getCurricularPeriodFor(1, executionInterval.getChildOrder(), academicPeriod);
        final CurricularCourse curricularCourse =
                new CurricularCourse(null, competenceCourse, dcp.getRoot(), curricularPeriod, executionInterval, null);
        final Context context = curricularCourse.getParentContextsSet().iterator().next();

        EnrolmentTest.createEnrolment(scp, executionInterval, context, ADMIN_USERNAME);
        return scp.getEnrolments(curricularCourse).iterator().next();
    }

}
