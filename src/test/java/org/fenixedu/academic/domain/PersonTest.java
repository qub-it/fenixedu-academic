package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PersonTest {

    private static Person personA;
    private static Person personB;
    private static Person personC;
    private static Student studentA;
    private static Registration registrationA;
    private static DegreeType degreeTypeA;
    private static DegreeType degreeTypeNonMatching;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            StudentTest.initRegistrationConfigEntities();
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            ExecutionsAndSchedulesTest.initExecutions();

            personA = createPerson("Person A", "person.test.a");
            personB = createPerson("Person B", "person.test.b");
            personC = createPerson("Person C", "person.test.c");

            personA.setDateOfBirthYearMonthDay(new YearMonthDay(1990, 1, 15));
            personB.setDateOfBirthYearMonthDay(new YearMonthDay(1985, 6, 20));
            personC.setDateOfBirthYearMonthDay(null);

            studentA = new Student(personA);

            final Degree degree = Degree.find(DegreeTest.DEGREE_A_CODE);
            final DegreeCurricularPlan dcp =
                    degree.getDegreeCurricularPlansSet().stream()
                            .filter(p -> DegreeCurricularPlanTest.DCP_NAME_V1.equals(p.getName()))
                            .findAny().orElseThrow();
            final ExecutionYear executionYear = ExecutionYear.findCurrent(degree.getCalendar());

            degreeTypeA = degree.getDegreeType();
            registrationA = StudentTest.createRegistration(studentA, dcp, executionYear);

            degreeTypeNonMatching = new DegreeType(
                    new LocalizedString.Builder().with(Locale.getDefault(), "Non Matching Type").build());
            degreeTypeNonMatching.setCode("NON_MATCHING");

            return null;
        });
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

    @Test
    public void testGetPartyNameReturnsNameForAllSupportedLocales() {
        final LocalizedString partyName = personA.getPartyName();
        assertNotNull(partyName);

        for (Locale locale : CoreConfiguration.supportedLocales()) {
            final String content = partyName.getContent(locale);
            assertNotNull("Party name should have content for locale: " + locale, content);
            assertEquals("Party name should match person's full name for locale: " + locale, personA.getName(), content);
        }
    }

    @Test
    public void testGetStudentByTypeReturnsMatchingRegistration() {
        final Registration result = personA.getStudentByType(degreeTypeA);
        assertNotNull("Should find registration with matching degree type", result);
        assertEquals("Should return the registration with the matching degree type", registrationA, result);
    }

    @Test
    public void testGetStudentByTypeReturnsNullWhenNoMatch() {
        final Registration result = personA.getStudentByType(degreeTypeNonMatching);
        assertNull("Should return null when no registration matches the degree type", result);
    }

    @Test
    public void testFindByDateOfBirthFiltersCorrectly() {
        final Collection<Person> persons = new ArrayList<>();
        persons.add(personA);
        persons.add(personB);

        final Collection<Person> result = Person.findByDateOfBirth(new YearMonthDay(1990, 1, 15), persons);
        assertNotNull(result);
        assertEquals("Should find exactly one person with matching date of birth", 1, result.size());
        assertTrue("Should contain personA", result.contains(personA));
        assertFalse("Should not contain personB", result.contains(personB));
    }

    @Test
    public void testFindByDateOfBirthIncludesNullDates() {
        final Collection<Person> personsWithNullDate = new ArrayList<>();
        personsWithNullDate.add(personA);
        personsWithNullDate.add(personC);

        final Collection<Person> result = Person.findByDateOfBirth(new YearMonthDay(1990, 1, 15), personsWithNullDate);
        assertNotNull(result);
        assertEquals("Should include both matching person and person with null date", 2, result.size());
        assertTrue("Person with null date of birth should be included", result.contains(personC));
        assertTrue("Person with matching date should be included", result.contains(personA));
    }

    @Test
    public void testFindByDateOfBirthReturnsEmptyForNoMatch() {
        final Collection<Person> persons = new ArrayList<>();
        persons.add(personA);
        persons.add(personB);

        final Collection<Person> result = Person.findByDateOfBirth(new YearMonthDay(2000, 12, 25), persons);
        assertNotNull(result);
        assertTrue("Should return empty collection when no persons match", result.isEmpty());
    }
}
