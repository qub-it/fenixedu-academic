package org.fenixedu.academic.domain;

import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.stream.Collectors;

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
    private static VaccineAdministration administration1, administration2;

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
        type1 = new VaccineType(ls1);
        type2 = new VaccineType(ls2);
    }

    @Test
    public void successfulVaccineAdministrationCreationTest() {
        administration1 = VaccineAdministration.create(type1, person, DateTime.now());
        assertTrue(!person.getVaccineAdministrationsSet().isEmpty());
        DateTime dateTime = DateTime.now().plusHours(7);

        VaccineAdministration.create(type1, person, dateTime);
        assertTrue(person.getVaccineAdministrationsSet().size() == 1);

        administration2 = VaccineAdministration.create(type2, person, DateTime.now().plusHours(10));
        assertTrue(person.getVaccineAdministrationsSet().size() == 2);

        assertTrue(person.getVaccineAdministrationsSet().stream().filter(vA -> vA.getValidityLimit() == dateTime)
                .collect(Collectors.toSet()).size() == 1);
    }

    @Test
    public void successfulVaccineTypeCreationTest() {
        assertTrue(!Bennu.getInstance().getVaccineTypesSet().isEmpty());
    }

    @AfterAll
    public void successfulDeletionTest() {
        Bennu.getInstance().getVaccineTypesSet().forEach(vT -> vT.delete());
        assertTrue(Bennu.getInstance().getVaccineTypesSet().isEmpty());

        person.getVaccineAdministrationsSet().forEach(vA -> vA.delete());
        assertTrue(person.getVaccineAdministrationsSet().isEmpty());
    }

}
