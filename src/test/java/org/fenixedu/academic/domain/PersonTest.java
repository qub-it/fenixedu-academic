package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.person.vaccine.VaccineAdministration;
import org.fenixedu.academic.domain.person.vaccine.VaccineType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PersonTest {

    private static Person personA, personB, personC;
    private static Student studentA;
    private static Registration registrationA;
    private static DegreeType degreeTypeA, degreeTypeB;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Installation.ensureInstallation();
            StudentTest.initRegistrationConfigEntities();
            ExecutionsAndSchedulesTest.initExecutions();

            personA = createPerson("Person A", "person.test.a");
            personB = createPerson("Person B", "person.test.b");
            personC = createPerson("Person C", "person.test.c");

            studentA = new Student(personA);

            final Degree degree = Degree.find(DegreeTest.DEGREE_A_CODE);
            final DegreeCurricularPlan dcp = degree.getDegreeCurricularPlansSet().stream()
                    .filter(p -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(p.getName())).findAny().orElseThrow();
            final ExecutionYear executionYear = ExecutionYear.findCurrent(degree.getCalendar());

            degreeTypeA = degree.getDegreeType();
            degreeTypeB = DegreeType.findByCode(DegreeTest.MASTER_DEGREE_TYPE_CODE).orElseThrow();

            registrationA = StudentTest.createRegistration(studentA, dcp, executionYear);

            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

    @Test
    public void testGetPartyName() {
        final LocalizedString partyName = personA.getPartyName();
        assertNotNull(partyName);
        CoreConfiguration.supportedLocales().forEach(locale -> assertEquals(personA.getName(), partyName.getContent(locale)));
    }

    @Test
    public void testGetStudentByType() {
        // registration found for degree type
        Registration result = personA.getStudentByType(degreeTypeA);
        assertNotNull(result);
        assertEquals(registrationA, result);

        // no registration matches degree type
        result = personA.getStudentByType(degreeTypeB);
        assertNull(result);
    }

    @Test
    public void testFindByDateOfBirth() {
        personA.setDateOfBirthYearMonthDay(new YearMonthDay(1990, 1, 15));
        personB.setDateOfBirthYearMonthDay(new YearMonthDay(1985, 6, 20));

        Collection<Person> result = Person.findByDateOfBirth(new YearMonthDay(1990, 1, 15), List.of(personA, personB));
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertTrue(result.contains(personA));
        assertFalse(result.contains(personB));

        // person with null date of birth should also be included in the results
        result = Person.findByDateOfBirth(new YearMonthDay(1990, 1, 15), List.of(personA, personB, personC));
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.contains(personC));
        assertTrue(result.contains(personA));

        // empty result when no persons match the date of birth
        result = Person.findByDateOfBirth(new YearMonthDay(2000, 12, 25), List.of(personA, personB));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDelete_cleansUpAssociatedCollections() {
        // create a dedicated person for deletion testing
        Person personToDelete = createPerson("Delete Me", "person.delete.test");

        // create associated objects
        Phone phone = Phone.createPhone(personToDelete, "912345678", PartyContactType.PERSONAL, true);
        EmailAddress email = EmailAddress.createEmailAddress(personToDelete, "delete@test.com", PartyContactType.PERSONAL, true);
        VaccineType vaccineType = new VaccineType(new LocalizedString(Locale.ENGLISH, "COVID"), "C19");
        VaccineAdministration vaccine = VaccineAdministration.createOrUpdate(vaccineType, personToDelete, null, LocalDate.now());
        PersonInformationLog log = new PersonInformationLog(personToDelete, "test log");

        // assert pre-conditions
        assertFalse(personToDelete.getPartyContactsSet().isEmpty());
        assertTrue(personToDelete.getPartyContactsSet().contains(phone));
        assertTrue(personToDelete.getPartyContactsSet().contains(email));

        assertFalse(personToDelete.getVaccineAdministrationsSet().isEmpty());
        assertTrue(personToDelete.getVaccineAdministrationsSet().contains(vaccine));

        assertFalse(personToDelete.getPersonInformationLogsSet().isEmpty());
        assertTrue(personToDelete.getPersonInformationLogsSet().contains(log));

        personToDelete.delete();

        // verify collections are cleaned up
        assertTrue(personToDelete.getPartyContactsSet().isEmpty());
        assertTrue(personToDelete.getVaccineAdministrationsSet().isEmpty());
        assertTrue(personToDelete.getPersonInformationLogsSet().isEmpty());
    }
}
