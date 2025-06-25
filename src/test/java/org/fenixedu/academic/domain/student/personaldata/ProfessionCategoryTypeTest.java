package org.fenixedu.academic.domain.student.personaldata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.ProfessionType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ProfessionCategoryTypeTest {

    private static final String EXAMPLE_CODE = "exampleCode";
    private static final String OTHER_EXAMPLE_CODE = "exampleCode2";

    private static ProfessionCategoryType professionCategoryType;
    private static PersonalIngressionData personalIngressionData;

    // Example professionType, can be any valid value
    private static final ProfessionType professionType = ProfessionType.ARMED_FORCES_OFFICERS;
    private static final String CODE = professionType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Armed Forces Officers");

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            personalIngressionData = new PersonalIngressionData();

            return null;
        });
    }

    private ProfessionCategoryType create(String code, LocalizedString name, boolean active) {
        return ProfessionCategoryType.create(code, name, active);
    }

    private void initializeProfessionCategoryTypeRelations() {
        // Initialize relations for the created ProfessionCategoryType
        personalIngressionData.setProfessionCategoryType(professionCategoryType);
        personalIngressionData.setMotherProfessionCategoryType(professionCategoryType);
        personalIngressionData.setFatherProfessionCategoryType(professionCategoryType);
        personalIngressionData.setSpouseProfessionCategoryType(professionCategoryType);
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getProfessionCategoryTypesSet().forEach(t -> {
            clearProfessionCategoryTypeRelations(t);
            t.delete();
        });
    }

    private void clearProfessionCategoryTypeRelations(ProfessionCategoryType pct) {
        pct.getPersonalIngressionDatasSet().clear();
        pct.getPersonalIngressionDatasAsMotherProfessionCategoryTypeSet().clear();
        pct.getPersonalIngressionDatasAsFatherProfessionCategoryTypeSet().clear();
        pct.getPersonalIngressionDatasAsSpouseProfessionCategoryTypeSet().clear();
    }

    @Test
    public void testProfessionCategoryType_create() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);

        assertNotEquals(null, professionCategoryType);
        assertEquals(CODE, professionCategoryType.getCode());
        assertEquals(QUALIFIED_NAME, professionCategoryType.getName());
    }

    @Test
    public void testProfessionCategoryType_createDuplicate() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);

        assertThrows(DomainException.class, () -> ProfessionCategoryType.create(CODE, QUALIFIED_NAME, true));
    }

    @Test
    public void testProfessionCategoryType_delete() {
        // Tests deletion of ProfessionCategoryType and its relations

        // Create a new ProfessionalStatusType and its relations
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionCategoryTypeRelations();

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
        clearProfessionCategoryTypeRelations(professionCategoryType);
        professionCategoryType.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getProfessionCategoryTypesSet().isEmpty());
        assertEquals(null, professionCategoryType.getRootDomainObject());
        assertEquals(null, personalIngressionData.getProfessionCategoryType());
        assertEquals(null, personalIngressionData.getMotherProfessionCategoryType());
        assertEquals(null, personalIngressionData.getFatherProfessionCategoryType());
        assertEquals(null, personalIngressionData.getSpouseProfessionCategoryType());
    }

    @Test
    public void testProfessionCategoryType_deleteFailsBecauseRelationsNotCleared() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionCategoryTypeRelations();

        assertThrows(DomainException.class, () -> professionCategoryType.delete());
    }

    @Test
    public void testProfessionCategoryType_personalIngressionDataRelations() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionCategoryTypeRelations();

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
    public void testProfessionCategoryType_findByCode() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);

        Optional<ProfessionCategoryType> found = ProfessionCategoryType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testProfessionCategoryType_findByCodeNotFound() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);

        Optional<ProfessionCategoryType> found = ProfessionCategoryType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testProfessionCategoryType_findAll() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);

        ProfessionCategoryType.create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        assertEquals(2, ProfessionCategoryType.findAll().count());
    }

    @Test
    public void testProfessionCategoryType_findActive() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);

        ProfessionCategoryType.create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        ProfessionCategoryType.create(OTHER_EXAMPLE_CODE, QUALIFIED_NAME, false);

        assertEquals(2, ProfessionCategoryType.findActive().count());
    }

    @Test
    public void testProfessionCategoryType_personalIngressionDataSettersAreSynced() {
        professionCategoryType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionCategoryTypeRelations();

        ProfessionType other = ProfessionType.OTHER;
        ProfessionCategoryType pct = create(other.getName(), QUALIFIED_NAME, true);

        // Assert initial state of PersonalIngressionData
        PersonalIngressionData pid = new PersonalIngressionData();
        assertEquals(null, pid.getProfessionType());
        assertEquals(null, pid.getProfessionCategoryType());
        assertEquals(null, pid.getMotherProfessionType());
        assertEquals(null, pid.getMotherProfessionCategoryType());
        assertEquals(null, pid.getFatherProfessionType());
        assertEquals(null, pid.getFatherProfessionCategoryType());
        assertEquals(null, pid.getSpouseProfessionType());
        assertEquals(null, pid.getSpouseProfessionCategoryType());

        // Call setter in ProfessionType Enum to trigger the sync in ProfessionCategoryType
        pid.setProfessionType(other);
        pid.setMotherProfessionType(other);
        pid.setFatherProfessionType(other);
        pid.setSpouseProfessionType(other);

        // Assert ProfessionType and ProfessionCategoryType have the same value
        // (edge case: they can be both null instead of the correct value, next assert will verify that)
        assertEquals(pid.getProfessionType().getName(), pid.getProfessionCategoryType().getCode());

        // Assert ProfessionCategoryType is set to the correct value (instead of being null for example)
        assertEquals(pct, pid.getProfessionCategoryType());

        assertEquals(pid.getMotherProfessionType().getName(), pid.getMotherProfessionCategoryType().getCode());
        assertEquals(pct, pid.getMotherProfessionCategoryType());

        assertEquals(pid.getFatherProfessionType().getName(), pid.getFatherProfessionCategoryType().getCode());
        assertEquals(pct, pid.getFatherProfessionCategoryType());

        assertEquals(pid.getSpouseProfessionType().getName(), pid.getSpouseProfessionCategoryType().getCode());
        assertEquals(pct, pid.getSpouseProfessionCategoryType());

        // Set to null with Enum setter
        pid.setProfessionType(null);
        pid.setMotherProfessionType(null);
        pid.setFatherProfessionType(null);
        pid.setSpouseProfessionType(null);

        assertEquals(null, pid.getProfessionType());
        assertEquals(null, pid.getProfessionCategoryType());
        assertEquals(null, pid.getMotherProfessionType());
        assertEquals(null, pid.getMotherProfessionCategoryType());
        assertEquals(null, pid.getFatherProfessionType());
        assertEquals(null, pid.getFatherProfessionCategoryType());
        assertEquals(null, pid.getSpouseProfessionType());
        assertEquals(null, pid.getSpouseProfessionCategoryType());

        // Call setter in ProfessionCategoryType to trigger the sync in ProfessionType
        pid.setProfessionCategoryType(pct);
        pid.setMotherProfessionCategoryType(pct);
        pid.setFatherProfessionCategoryType(pct);
        pid.setSpouseProfessionCategoryType(pct);

        // Assert ProfessionType and ProfessionCategoryType have the same value
        assertEquals(pid.getProfessionType().getName(), pid.getProfessionCategoryType().getCode());
        assertEquals(pct, pid.getProfessionCategoryType());

        assertEquals(pid.getMotherProfessionType().getName(), pid.getMotherProfessionCategoryType().getCode());
        assertEquals(pct, pid.getMotherProfessionCategoryType());

        assertEquals(pid.getFatherProfessionType().getName(), pid.getFatherProfessionCategoryType().getCode());
        assertEquals(pct, pid.getFatherProfessionCategoryType());

        assertEquals(pid.getSpouseProfessionType().getName(), pid.getSpouseProfessionCategoryType().getCode());
        assertEquals(pct, pid.getSpouseProfessionCategoryType());

        // Set to null with ProfessionCategoryType setter
        pid.setProfessionCategoryType(null);
        pid.setMotherProfessionCategoryType(null);
        pid.setFatherProfessionCategoryType(null);
        pid.setSpouseProfessionCategoryType(null);

        assertEquals(null, pid.getProfessionType());
        assertEquals(null, pid.getProfessionCategoryType());
        assertEquals(null, pid.getMotherProfessionType());
        assertEquals(null, pid.getMotherProfessionCategoryType());
        assertEquals(null, pid.getFatherProfessionType());
        assertEquals(null, pid.getFatherProfessionCategoryType());
        assertEquals(null, pid.getSpouseProfessionType());
        assertEquals(null, pid.getSpouseProfessionCategoryType());
    }
}
