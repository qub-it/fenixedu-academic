package org.fenixedu.academic.domain.student.registration;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EntryPhaseToAdmissionPhaseTest {

    private static Registration registration;
    private static StudentCandidacy studentCandidacy;

    public static final String DCP_NAME_V1 = "DCP_NAME_V1";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            initRegistration();
            return null;
        });
    }

    static void initRegistration() {
        Degree degree = DegreeTest.initDegree();

        final UserProfile userProfile =
                new UserProfile("Fenix", "Admin", "Fenix Admin", "fenix.admin@fenixedu.com", Locale.getDefault());
        new User("admin", userProfile);
        Person person = new Person(userProfile);
        DegreeCurricularPlan degreeCurricularPlan =
                degree.createDegreeCurricularPlan(DCP_NAME_V1, person, AcademicPeriod.THREE_YEAR);
        degreeCurricularPlan.setCurricularStage(CurricularStage.APPROVED);

        final UserProfile studentProfile =
                new UserProfile("Josefa", "Ferreira", "Josefa Ferreira", "josefa.ferreira@fenixedu.com", Locale.getDefault());
        new User("student", userProfile);
        Person personForStudent = new Person(studentProfile);
        Student student = new Student(personForStudent);

        final ExecutionYear executionYear = ExecutionYear.findCurrent(null);

        degreeCurricularPlan.createExecutionDegree(executionYear);

        RegistrationStateType.create("REGISTERED", new LocalizedString(Locale.getDefault(), "Registado"), true);

        RegistrationProtocol protocol = RegistrationProtocol.create("test", new LocalizedString(Locale.getDefault(), "test"));

        IngressionType ingressionType =
                IngressionType.createIngressionType("normal", new LocalizedString(Locale.getDefault(), "Normal"));

        registration = Registration.create(student, degreeCurricularPlan, executionYear, protocol, ingressionType);

        studentCandidacy = registration.getStudentCandidacy();

    }

    public static Degree createDegree(final DegreeType degreeType, String code, String name, final ExecutionYear executionYear) {
        final Degree result = new Degree(name, name, code, degreeType, new GradeScale(), new GradeScale(), executionYear);
        result.setCode(code);
        result.setCalendar(executionYear.getAcademicInterval().getAcademicCalendar());

        return result;
    }

    @Test
    public void testAdmissionPhase_addToRegistration() {
        registration.setEntryPhase(EntryPhase.FIRST_PHASE);
        assertTrue(registration.getEntryPhase().equals(EntryPhase.FIRST_PHASE));
        assertTrue(registration.getAdmissionPhase().equals(1));

        registration.setAdmissionPhase(2);
        assertTrue(registration.getEntryPhase().equals(EntryPhase.SECOND_PHASE));
        assertTrue(registration.getAdmissionPhase().equals(2));
    }

    @Test
    public void testAdmissionPhase_deleteEntryPhaseFromRegistration() {
        registration.setEntryPhase(EntryPhase.FIRST_PHASE);
        assertTrue(registration.getEntryPhase().equals(EntryPhase.FIRST_PHASE));
        assertTrue(registration.getAdmissionPhase().equals(1));

        registration.setEntryPhase(null);
        assertNull(registration.getEntryPhase());
        assertNull(registration.getAdmissionPhase());
    }

    @Test
    public void testAdmissionPhase_deleteAdmissionPhaseFromRegistration() {
        registration.setEntryPhase(EntryPhase.FIRST_PHASE);
        assertTrue(registration.getEntryPhase().equals(EntryPhase.FIRST_PHASE));
        assertTrue(registration.getAdmissionPhase().equals(1));

        registration.setAdmissionPhase(null);
        assertNull(registration.getEntryPhase());
        assertNull(registration.getAdmissionPhase());
    }

    @Test
    public void testAdmissionPhase_addToStudentCandidacy() {
        studentCandidacy.setEntryPhase(EntryPhase.FIRST_PHASE);
        assertTrue(studentCandidacy.getEntryPhase().equals(EntryPhase.FIRST_PHASE));
        assertTrue(studentCandidacy.getAdmissionPhase().equals(1));

        studentCandidacy.setAdmissionPhase(2);
        assertTrue(studentCandidacy.getEntryPhase().equals(EntryPhase.SECOND_PHASE));
        assertTrue(studentCandidacy.getAdmissionPhase().equals(2));
    }

    @Test
    public void testAdmissionPhase_deleteEntryPhaseFromStudentCandidacy() {
        studentCandidacy.setEntryPhase(EntryPhase.FIRST_PHASE);
        assertTrue(studentCandidacy.getEntryPhase().equals(EntryPhase.FIRST_PHASE));
        assertTrue(studentCandidacy.getAdmissionPhase().equals(1));

        studentCandidacy.setEntryPhase(null);
        assertNull(studentCandidacy.getEntryPhase());
        assertNull(studentCandidacy.getAdmissionPhase());
    }

    @Test
    public void testAdmissionPhase_deleteAdmissionPhaseFromStudentCandidacy() {
        studentCandidacy.setEntryPhase(EntryPhase.FIRST_PHASE);
        assertTrue(studentCandidacy.getEntryPhase().equals(EntryPhase.FIRST_PHASE));
        assertTrue(studentCandidacy.getAdmissionPhase().equals(1));

        studentCandidacy.setAdmissionPhase(null);
        assertNull(studentCandidacy.getEntryPhase());
        assertNull(studentCandidacy.getAdmissionPhase());
    }

}
