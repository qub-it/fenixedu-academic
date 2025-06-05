package org.fenixedu.academic.domain.student.personaldata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.ProfessionType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ProfessionCategoryTypeTest {

    private static ProfessionCategoryType professionCategoryType;
    private static PersonalIngressionData personalIngressionData;

    // Example professionType, can be any valid value
    private static final ProfessionType professionType = ProfessionType.ARMED_FORCES_OFFICERS;
    private static final String CODE = professionType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Armed Forces Officers");

    @Before
    public void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            professionCategoryType = ProfessionCategoryType.create(CODE, QUALIFIED_NAME, true);

            personalIngressionData = new PersonalIngressionData();
            personalIngressionData.setProfessionCategoryType(professionCategoryType);
            personalIngressionData.setMotherProfessionCategoryType(professionCategoryType);
            personalIngressionData.setFatherProfessionCategoryType(professionCategoryType);
            personalIngressionData.setSpouseProfessionCategoryType(professionCategoryType);

            return null;
        });
    }

    @After
    public void cleanup() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            clearProfessionCategoryTypeRelations();
            professionCategoryType.delete();
            personalIngressionData.delete();
            return null;
        });
    }

    private void clearProfessionCategoryTypeRelations() {
        professionCategoryType.getPersonalIngressionDatasSet().clear();
        professionCategoryType.getPersonalIngressionDatasAsMotherProfessionCategoryTypeSet().clear();
        professionCategoryType.getPersonalIngressionDatasAsFatherProfessionCategoryTypeSet().clear();
        professionCategoryType.getPersonalIngressionDatasAsSpouseProfessionCategoryTypeSet().clear();
    }

    @Test
    public void testProfessionCategoryType_Create() {
        assertNotEquals(null, professionCategoryType);
        assertEquals(CODE, professionCategoryType.getCode());
        assertEquals(QUALIFIED_NAME, professionCategoryType.getName());
    }

    @Test
    public void testProfessionCategoryType_CreateDuplicate() {
        assertThrows(DomainException.class, () -> {
            ProfessionCategoryType.create(CODE, QUALIFIED_NAME, true);
        });
    }

    @Test
    public void testProfessionCategoryType_Delete() {
        // Tests deletion of ProfessionCategoryType and its relations

        // Verify initial state before deletion
        assertEquals(false, Bennu.getInstance().getProfessionCategoryTypesSet().isEmpty());
        assertNotEquals(null, professionCategoryType.getRootDomainObject());
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasSet().contains(personalIngressionData));
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsMotherProfessionCategoryTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsFatherProfessionCategoryTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsSpouseProfessionCategoryTypeSet()
                .contains(personalIngressionData));

        // Perform deletion
        clearProfessionCategoryTypeRelations();
        professionCategoryType.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getProfessionCategoryTypesSet().isEmpty());
        assertEquals(null, professionCategoryType.getRootDomainObject());
        assertEquals(null, personalIngressionData.getProfessionCategoryType());
        assertEquals(null, personalIngressionData.getMotherProfessionCategoryType());
        assertEquals(null, personalIngressionData.getFatherProfessionCategoryType());
        assertEquals(null, personalIngressionData.getSpouseProfessionCategoryType());

        // create ProfessionCategoryType again so cleanup doesn't fail
        professionCategoryType = ProfessionCategoryType.create(CODE, QUALIFIED_NAME, true);
    }

    @Test
    public void testProfessionCategoryType_DeleteFailsBecauseRelationsNotCleared() {
        assertThrows(DomainException.class, () -> professionCategoryType.delete());
    }

    @Test
    public void testProfessionCategoryType_PersonalIngressionDataRelations() {

        // Verify initial relations with PersonalIngressionData
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasSet().contains(personalIngressionData));
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsMotherProfessionCategoryTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsFatherProfessionCategoryTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsSpouseProfessionCategoryTypeSet()
                .contains(personalIngressionData));

        // Delete PersonalIngressionData
        personalIngressionData.delete();

        // Verify that relations with ProfessionCategoryType are removed
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasSet().isEmpty());
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsMotherProfessionCategoryTypeSet().isEmpty());
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsFatherProfessionCategoryTypeSet().isEmpty());
        assertEquals(true, professionCategoryType.getPersonalIngressionDatasAsSpouseProfessionCategoryTypeSet().isEmpty());
    }

    @Test
    public void testProfessionCategoryType_FindByCode() {
        Optional<ProfessionCategoryType> found = ProfessionCategoryType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testProfessionCategoryType_FindByCodeNotFound() {
        Optional<ProfessionCategoryType> found = ProfessionCategoryType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testProfessionCategoryType_FindAll() {
        ProfessionCategoryType p = ProfessionCategoryType.create("exampleCode", QUALIFIED_NAME, true);
        assertEquals(2, ProfessionCategoryType.findAll().count());
        p.delete();
    }

    @Test
    public void testProfessionCategoryType_FindActive() {
        ProfessionCategoryType p1 = ProfessionCategoryType.create("exampleCode2", QUALIFIED_NAME, true);
        ProfessionCategoryType p2 = ProfessionCategoryType.create("exampleCode3", QUALIFIED_NAME, false);

        assertEquals(2, ProfessionCategoryType.findActive().count());

        p1.delete();
        p2.delete();
    }
}
