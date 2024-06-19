package org.fenixedu.academic.domain.student.statute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StudentStatuteTest {

    private static final String TYPE_B = "ST-B";
    private static final String TYPE_A = "ST-A";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initStudentAndRegistration();

            StatuteType.create(TYPE_A, new LocalizedString.Builder().with(Locale.getDefault(), "Statute A").build());
            StatuteType.create(TYPE_B, new LocalizedString.Builder().with(Locale.getDefault(), "Statute B").build());

            return null;
        });
    }

    @After
    public void clearStatutes() {
        Bennu.getInstance().getStudentStatutesSet().forEach(StudentStatute::delete);
    }

    @Test
    public void registrationStatutes_isValidOn() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();

        final StudentStatute studentStatute =
                new StudentStatute(student, statuteTypeA, executionInterval, executionInterval, null, null, null, registration);

        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval));
        assertFalse(studentStatute.isValidInExecutionInterval(executionInterval.getNext()));
        assertFalse(studentStatute.isValidInExecutionInterval(executionInterval.getExecutionYear()));
        assertFalse(studentStatute.isValidOn(executionInterval.getExecutionYear()));
        assertTrue(studentStatute.isValidOnAnyExecutionPeriodFor(executionInterval.getExecutionYear()));
        assertFalse(
                studentStatute.isValidOnAnyExecutionPeriodFor((ExecutionYear) executionInterval.getExecutionYear().getNext()));
    }

    @Test
    public void registrationStatutes_isValidOn_onlyBeginInterval() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();

        final StudentStatute studentStatute =
                new StudentStatute(student, statuteTypeA, executionInterval, null, null, null, null, registration);

        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval));
        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval.getNext()));
        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval.getExecutionYear()));
        assertFalse(studentStatute.isValidInExecutionInterval(executionInterval.getPrevious()));
        assertTrue(studentStatute.isValidOn(executionInterval.getExecutionYear()));
        assertTrue(studentStatute.isValidOnAnyExecutionPeriodFor(executionInterval.getExecutionYear()));
        assertTrue(studentStatute.isValidOnAnyExecutionPeriodFor((ExecutionYear) executionInterval.getExecutionYear().getNext()));
    }

    @Test
    public void registrationStatutes_isValidOn_onlyEndInterval() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();

        final StudentStatute studentStatute =
                new StudentStatute(student, statuteTypeA, null, executionInterval, null, null, null, registration);

        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval));
        assertFalse(studentStatute.isValidInExecutionInterval(executionInterval.getNext()));
        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval.getPrevious()));
        assertFalse(studentStatute.isValidInExecutionInterval(executionInterval.getExecutionYear()));
        assertTrue(studentStatute.isValidInExecutionInterval(executionInterval.getExecutionYear().getPrevious()));
        assertFalse(studentStatute.isValidOn(executionInterval.getExecutionYear()));
        assertTrue(studentStatute.isValidOnAnyExecutionPeriodFor(executionInterval.getExecutionYear()));
        assertTrue(studentStatute
                .isValidOnAnyExecutionPeriodFor((ExecutionYear) executionInterval.getExecutionYear().getPrevious()));
    }

    @Test
    public void registrationStatutes_findByExecutionInterval() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();
        final StatuteType statuteTypeB = StatuteType.findByCode(TYPE_B).orElseThrow();

        new StudentStatute(student, statuteTypeA, executionInterval, executionInterval, null, null, null, registration);

        Set<StatuteType> statuteTypesForInterval =
                StatuteType.findforRegistration(registration, executionInterval).collect(Collectors.toSet());

        assertTrue(statuteTypesForInterval.contains(statuteTypeA));
        assertFalse(statuteTypesForInterval.contains(statuteTypeB));

        final ExecutionInterval nextInterval = executionInterval.getNext();
        new StudentStatute(student, statuteTypeB, executionInterval, nextInterval, null, null, null, registration);

        statuteTypesForInterval = StatuteType.findforRegistration(registration, executionInterval).collect(Collectors.toSet());

        assertTrue(statuteTypesForInterval.contains(statuteTypeA));
        assertTrue(statuteTypesForInterval.contains(statuteTypeB));

        final Set<StatuteType> statuteTypesForNextInterval =
                StatuteType.findforRegistration(registration, nextInterval).collect(Collectors.toSet());

        assertFalse(statuteTypesForNextInterval.contains(statuteTypeA));
        assertTrue(statuteTypesForNextInterval.contains(statuteTypeB));

        final ExecutionInterval previousInterval = executionInterval.getPrevious();
        final Set<StatuteType> statuteTypesForPreviousInterval =
                StatuteType.findforRegistration(registration, previousInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForPreviousInterval.isEmpty());
    }

    @Test
    public void registrationStatutes_findByExecutionInterval_onlyBeginInterval() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();

        new StudentStatute(student, statuteTypeA, executionInterval, null, null, null, null, registration);

        Set<StatuteType> statuteTypesForInterval =
                StatuteType.findforRegistration(registration, executionInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForInterval.contains(statuteTypeA));

        final ExecutionInterval nextInterval = executionInterval.getNext();
        final Set<StatuteType> statuteTypesForNextInterval =
                StatuteType.findforRegistration(registration, nextInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForNextInterval.contains(statuteTypeA));

        final ExecutionInterval previousInterval = executionInterval.getPrevious();
        final Set<StatuteType> statuteTypesForPreviousInterval =
                StatuteType.findforRegistration(registration, previousInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForPreviousInterval.isEmpty());
    }

    @Test
    public void registrationStatutes_findByExecutionInterval_onlyEndInterval() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();

        new StudentStatute(student, statuteTypeA, null, executionInterval, null, null, null, registration);

        Set<StatuteType> statuteTypesForInterval =
                StatuteType.findforRegistration(registration, executionInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForInterval.contains(statuteTypeA));

        final ExecutionInterval nextInterval = executionInterval.getNext();
        final Set<StatuteType> statuteTypesForNextInterval =
                StatuteType.findforRegistration(registration, nextInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForNextInterval.isEmpty());

        final ExecutionInterval previousInterval = executionInterval.getPrevious();
        final Set<StatuteType> statuteTypesForPreviousInterval =
                StatuteType.findforRegistration(registration, previousInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForPreviousInterval.contains(statuteTypeA));
    }

    @Test
    public void registrationStatutes_findByExecutionInterval_createdWithoutAnyInterval() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();
        final StatuteType statuteTypeB = StatuteType.findByCode(TYPE_B).orElseThrow();

        new StudentStatute(student, statuteTypeA, null, null, null, null, null, registration);

        Set<StatuteType> statuteTypesForInterval =
                StatuteType.findforRegistration(registration, executionInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForInterval.contains(statuteTypeA));
        assertFalse(statuteTypesForInterval.contains(statuteTypeB));

        final ExecutionInterval nextInterval = executionInterval.getNext();
        final Set<StatuteType> statuteTypesForNextInterval =
                StatuteType.findforRegistration(registration, nextInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForNextInterval.contains(statuteTypeA));
        assertFalse(statuteTypesForNextInterval.contains(statuteTypeB));

        final ExecutionInterval previousInterval = executionInterval.getPrevious();
        final Set<StatuteType> statuteTypesForPreviousInterval =
                StatuteType.findforRegistration(registration, previousInterval).collect(Collectors.toSet());
        assertTrue(statuteTypesForPreviousInterval.contains(statuteTypeA));
        assertFalse(statuteTypesForPreviousInterval.contains(statuteTypeB));
    }

    @Test
    public void registrationStatutes_findByExecutionYear() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();
        final StatuteType statuteTypeB = StatuteType.findByCode(TYPE_B).orElseThrow();

        new StudentStatute(student, statuteTypeA, executionInterval, executionInterval, null, null, null, registration);

        final ExecutionInterval executionYear = ExecutionInterval.findCurrentAggregator(registration.getDegree().getCalendar());

        Set<StatuteType> statuteTypesForInterval =
                StatuteType.findforRegistration(registration, executionYear).collect(Collectors.toSet());

        assertTrue(statuteTypesForInterval.contains(statuteTypeA));
        assertFalse(statuteTypesForInterval.contains(statuteTypeB));

        final ExecutionInterval nextYear = executionYear.getNext();
        new StudentStatute(student, statuteTypeB, executionInterval, ((ExecutionYear) nextYear).getFirstExecutionPeriod(), null,
                null, null, registration);

        statuteTypesForInterval = StatuteType.findforRegistration(registration, executionYear).collect(Collectors.toSet());

        assertTrue(statuteTypesForInterval.contains(statuteTypeA));
        assertTrue(statuteTypesForInterval.contains(statuteTypeB));

        final Set<StatuteType> statuteTypesForNextYear =
                StatuteType.findforRegistration(registration, nextYear).collect(Collectors.toSet());

        assertFalse(statuteTypesForNextYear.contains(statuteTypeA));
        assertTrue(statuteTypesForNextYear.contains(statuteTypeB));

        final ExecutionInterval previousYear = executionYear.getPrevious();
        final Set<StatuteType> statuteTypesForPreviousInterval =
                StatuteType.findforRegistration(registration, previousYear).collect(Collectors.toSet());
        assertTrue(statuteTypesForPreviousInterval.isEmpty());
    }

    @Test
    public void registrationStatutes_findByDates() {
        final Student student = Student.readStudentByNumber(1);
        final Registration registration = student.getRegistrationStream().findAny().orElseThrow();

        final ExecutionInterval executionInterval =
                ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar());

        final StatuteType statuteTypeA = StatuteType.findByCode(TYPE_A).orElseThrow();

        final LocalDate startDate = new LocalDate(2020, 9, 15);
        final LocalDate endDate = new LocalDate(2020, 9, 25);

        new StudentStatute(student, statuteTypeA, executionInterval, executionInterval, startDate, endDate, null, registration);

        assertTrue(StatuteType
                .findforRegistration(registration, executionInterval,
                        new Interval(new DateTime(2020, 9, 15, 0, 0), new DateTime(2020, 9, 20, 0, 0)))
                .anyMatch(st -> st == statuteTypeA));

        assertTrue(StatuteType
                .findforRegistration(registration, executionInterval,
                        new Interval(new DateTime(2020, 9, 10, 0, 0), new DateTime(2020, 9, 25, 0, 0)))
                .anyMatch(st -> st == statuteTypeA));

        assertTrue(StatuteType
                .findforRegistration(registration, executionInterval,
                        new Interval(new DateTime(2020, 9, 20, 0, 0), new DateTime(2020, 9, 30, 0, 0)))
                .anyMatch(st -> st == statuteTypeA));

        assertTrue(
                StatuteType
                        .findforRegistration(registration, executionInterval,
                                new Interval(new DateTime(2020, 9, 5, 0, 0), new DateTime(2020, 9, 10, 0, 0)))
                        .findAny().isEmpty());

        assertTrue(
                StatuteType
                        .findforRegistration(registration, executionInterval,
                                new Interval(new DateTime(2020, 9, 26, 0, 0), new DateTime(2020, 9, 30, 0, 0)))
                        .findAny().isEmpty());

        assertTrue(
                StatuteType
                        .findforRegistration(registration, executionInterval.getNext(),
                                new Interval(new DateTime(2020, 9, 10, 0, 0), new DateTime(2020, 9, 25, 0, 0)))
                        .findAny().isEmpty());

    }
}
