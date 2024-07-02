package org.fenixedu.academic.domain.person.vaccine;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.vaccine.VaccineAdministration;
import org.fenixedu.academic.domain.person.vaccine.VaccineType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class VaccineTest {

    private static Person person;
    private static VaccineType type1, type2;
    private static LocalizedString ls1, ls2;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initVaccines();
            return null;
        });
    }

    public static void initVaccines() {
        final UserProfile userProfile =
                new UserProfile("John Smith", "", "John Smith", "username" + "@fenixedu.com", Locale.getDefault());
        new User("username", userProfile);
        person = new Person(userProfile);
        ls1 = new LocalizedString(Locale.ENGLISH, "COVID");
        ls2 = new LocalizedString(Locale.ENGLISH, "Tetanus");
        type1 = new VaccineType(ls1, "C19");
        type2 = new VaccineType(ls2, "TET");
    }

    @Test
    public void testCreateVaccineTypeSucessful() {
        assertTrue(!Bennu.getInstance().getVaccineTypesSet().isEmpty());
    }

    @Test
    public void testSetCodeDuplicateValueThrowsDomainException() {
        assertThrows(DomainException.class, () -> type1.setCode("TET"));
    }

    @Test
    public void testSetAndGetCodeSuccessful() {
        type1.setCode("ABC");
        assertTrue(type1.getCode().equals("ABC"));
    }

    @Test
    public void testFindAllSucessful() {
        assertTrue(VaccineType.findAll().collect(Collectors.toSet()).size() == 2);
    }

    @Test
    public void testFindByCodeSucessful() {
        assertTrue(VaccineType.findByCode("TET").isPresent());
        assertTrue(VaccineType.findByCode("HEPB").isEmpty());
    }

    @Test
    public void testCreateSucessful() {
        VaccineAdministration.createOrUpdate(type2, person, DateTime.now());
        assertTrue(!person.getVaccineAdministrationsSet().isEmpty());
    }

    @Test
    public void testUpdateSucessful() {
        DateTime dateTime = DateTime.now().plusHours(7);
        VaccineAdministration.createOrUpdate(type2, person, dateTime);
        assertTrue(person.getVaccineAdministrationsSet().size() == 1);
        assertTrue(person.getVaccineAdministrationsSet().stream().filter(vA -> vA.getValidityLimit().equals(dateTime))
                .collect(Collectors.toSet()).size() == 1);
    }

    @AfterAll
    public void testDeleteSucessful() {
        VaccineType.findAll().forEach(vT -> vT.delete());
        assertTrue(VaccineType.findAll().collect(Collectors.toSet()).isEmpty());

        person.getVaccineAdministrationsSet().forEach(vA -> vA.delete());
        assertTrue(person.getVaccineAdministrationsSet().isEmpty());
    }

}
