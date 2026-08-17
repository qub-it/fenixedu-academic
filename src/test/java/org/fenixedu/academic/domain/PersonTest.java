package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.ADMIN_USERNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.contacts.WebAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.person.vaccine.VaccineAdministration;
import org.fenixedu.academic.domain.person.vaccine.VaccineType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
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

    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester, secondSemester;
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
            CountryTest.initCountries();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            secondSemester = executionYear.getLastExecutionPeriod();

            personA = createPerson("Person A", "person.test.a");
            personB = createPerson("Person B", "person.test.b");
            personC = createPerson("Person C", "person.test.c");

            studentA = new Student(personA);

            final Degree degree = Degree.find(DegreeTest.DEGREE_A_CODE);
            final DegreeCurricularPlan dcp = degree.getDegreeCurricularPlansSet().stream()
                    .filter(p -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(p.getName())).findAny().orElseThrow();

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
        assertNull(personA.getStudentByType(degreeTypeB));
        assertNull(personA.getStudentByType(null));
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

        // empty result when date of birth is null
        result = Person.findByDateOfBirth(null, List.of(personA, personB));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLogSetterNullString() {
        Person person = createPerson("LogString Test", "person.logstr.test");
        person.getPersonInformationLogsSet().clear();

        // null to value: old value should be "(no value)", not null
        person.setProfession("Engineer");
        assertLogPreviousValue(person, "(no value)");

        // value to different value: old value should be "Engineer"
        person.setProfession("Doctor");
        assertLogPreviousValue(person, "Engineer");

        // empty to value: old value should be "(no value)" because empty is treated as null
        person.setProfession("");
        person.getPersonInformationLogsSet().clear();
        person.setProfession("Engineer");
        assertLogPreviousValue(person, "(no value)");

        // value to null: old value should be "Engineer"
        person.setProfession(null);
        assertLogPreviousValue(person, "Engineer");
    }

    @Test
    public void testLogSetterNullYearMonthDay() {
        Person person = createPerson("LogYMD Test", "person.logydm.test");
        person.getPersonInformationLogsSet().clear();

        // null to value: old value should be "Date Empty" not null
        person.setDateOfBirthYearMonthDay(new YearMonthDay(1990, 6, 15));
        assertLogPreviousValue(person, "Date Empty");

        // value to different value: old value should be the formatted date "1990/06/15"
        person.setDateOfBirthYearMonthDay(new YearMonthDay(2000, 1, 1));
        assertLogPreviousValue(person, "1990/06/15");

        // value to null: old value should be the formatted date "2000/01/01"
        person.setDateOfBirthYearMonthDay(null);
        assertLogPreviousValue(person, "2000/01/01");
    }

    @Test
    public void testLogSetterNullEnum() {
        Person person = createPerson("LogEnum Test", "person.logenum.test");
        person.getPersonInformationLogsSet().clear();

        // null to value: old value should be "(no value)", not null
        person.setGender(Gender.MALE);
        assertLogPreviousValue(person, "(no value)");

        // value to different value: old value should be the localized name of MALE
        person.setGender(Gender.FEMALE);
        assertLogPreviousValue(person, Gender.MALE.getLocalizedName());

        // value to null: old value should be the localized name of FEMALE
        person.setGender(null);
        assertLogPreviousValue(person, Gender.FEMALE.getLocalizedName());
    }

    @Test
    public void testLogSetterCountry() {
        Person person = createPerson("LogCountry Test", "person.logcountry.test");
        person.getPersonInformationLogsSet().clear();

        Country portugal = Country.readByTwoLetterCode("PT");
        Country france = Country.readByTwoLetterCode("FR");

        // null to value: old value should be "(no value)"
        person.setCountry(portugal);
        assertLogPreviousValue(person, "(no value)");

        // same value: no log should be created
        person.setCountry(portugal);
        assertTrue(person.getPersonInformationLogsSet().isEmpty());

        // value to different value: old value should be the previous country nationality
        france.setCountryNationality(null);
        person.setCountry(france);
        assertLogPreviousValue(person, "Portuguese");

        // value to null: old value should be the previous country name (instead of nationality)
        person.setCountry(null);
        assertLogPreviousValue(person, "France");
    }

    @Test
    public void testLogSetterCountryOfBirth() {
        Person person = createPerson("LogCOB Test", "person.logcob.test");
        person.getPersonInformationLogsSet().clear();

        Country portugal = Country.readByTwoLetterCode("PT");
        Country spain = Country.readByTwoLetterCode("ES");

        // null to value: old value should be "(no value)"
        person.setCountryOfBirth(portugal);
        assertLogPreviousValue(person, "(no value)");

        // same value: no log should be created
        person.setCountryOfBirth(portugal);
        assertTrue(person.getPersonInformationLogsSet().isEmpty());

        // value to different value: old value should be previous country name
        person.setCountryOfBirth(spain);
        assertLogPreviousValue(person, "Portugal");

        // value to null: old value should be previous country name
        person.setCountryOfBirth(null);
        assertLogPreviousValue(person, "Spain");
    }

    @Test
    public void testHasEmailAddress() {
        EmailAddress.createEmailAddress(personA, "contact@test.com", PartyContactType.PERSONAL, true);
        EmailAddress.createEmailAddress(personA, "institutional@test.com", PartyContactType.INSTITUTIONAL, false);

        assertTrue(personA.hasEmailAddress("contact@test.com"));
        assertTrue(personA.hasEmailAddress("institutional@test.com"));
        assertTrue(personA.hasEmailAddress("CONTACT@TEST.COM"));

        assertFalse(personA.hasEmailAddress("other@test.com"));
        assertFalse(personA.hasEmailAddress(null));

        // partyContact is not email address
        PartyContact phone = Phone.createPhone(personA, "912345678", PartyContactType.PERSONAL, false);
        assertFalse(phone.isEmailAddress());
        assertFalse(personA.hasEmailAddress("912345678"));
    }

    @Test
    public void testIsDefaultEmailVisible() {
        // no email address
        assertFalse(personB.isDefaultEmailVisible());

        // personal email is not default, not valid and not visible to public
        EmailAddress personalEmail =
                EmailAddress.createEmailAddress(personB, "personal@test.com", PartyContactType.PERSONAL, false);
        assertNull(personB.getDefaultEmailAddress());
        assertFalse(personB.isDefaultEmailVisible());

        // validate and set to default
        personalEmail.setValid();
        personalEmail.changeToDefault();
        assertTrue(personB.getDefaultEmailAddress().isDefault());
        assertFalse(personB.isDefaultEmailVisible());

        // visible to public
        personalEmail.setVisibleToPublic(true);
        assertTrue(personB.isDefaultEmailVisible());
    }

    @Test
    public void testIsDefaultWebAddressVisible() {
        // no web address
        assertFalse(personB.isDefaultWebAddressVisible());

        // personal web address is not visible to public
        WebAddress personalWeb = WebAddress.create(personB, "http://personal.test", PartyContactType.PERSONAL, true);
        assertTrue(personB.getDefaultWebAddress().isDefault());
        assertFalse(personB.getDefaultWebAddress().getVisibleToPublic());
        assertFalse(personB.isDefaultWebAddressVisible());

        // visible to public
        personalWeb.setVisibleToPublic(true);
        assertTrue(personB.isDefaultWebAddressVisible());
    }

    @Test
    public void testGetProfessorships() {
        ExecutionCourse courseA = new ExecutionCourse("Course A", "CIA", firstSemester);
        ExecutionCourse courseB = new ExecutionCourse("Course B", "CIB", firstSemester);
        ExecutionCourse courseC = new ExecutionCourse("Course C", "CIC", secondSemester);
        ExecutionCourse courseD =
                new ExecutionCourse("Course D", "CID", executionYear.getNext().getExecutionYear().getFirstExecutionPeriod());

        List<Professorship> professorships = new ArrayList<>();
        try {
            Authenticate.mock(User.findByUsername(ADMIN_USERNAME), "none");

            professorships.add(Professorship.create(false, courseA, personC));
            professorships.add(Professorship.create(false, courseB, personC));
            professorships.add(Professorship.create(false, courseC, personC));
            professorships.add(Professorship.create(false, courseD, personC));

            // first semester, 2 professorships
            List<Professorship> firstSemProfs = personC.getProfessorships(firstSemester);
            assertEquals(2, firstSemProfs.size());
            assertTrue(firstSemProfs.stream().allMatch(p -> p.getExecutionCourse().getExecutionInterval().equals(firstSemester)));

            // second semester, 1 professorship
            List<Professorship> secondSemProfs = personC.getProfessorships(secondSemester);
            assertEquals(1, secondSemProfs.size());
            assertEquals(courseC, secondSemProfs.get(0).getExecutionCourse());

            // entire execution year, professorship for courseD is not included because it belongs to the next execution year
            List<Professorship> yearProfs = personC.getProfessorships(executionYear);
            assertEquals(3, yearProfs.size());
            assertTrue(yearProfs.stream().noneMatch(p -> p.getExecutionCourse() == courseD));
        } finally {
            Authenticate.unmock();
            professorships.forEach(Professorship::delete);
            List.of(courseA, courseB, courseC, courseD).forEach(ExecutionCourse::delete);
        }
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

    private void assertLogPreviousValue(Person person, String expected) {
        PersonInformationLog log = person.getPersonInformationLogsSet().iterator().next();
        assertTrue(log.getDescription().contains(expected));
        person.getPersonInformationLogsSet().clear();
    }
}
