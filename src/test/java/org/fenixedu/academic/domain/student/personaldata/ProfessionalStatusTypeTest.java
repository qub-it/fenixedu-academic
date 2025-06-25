package org.fenixedu.academic.domain.student.personaldata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.ProfessionalSituationConditionType;
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
public class ProfessionalStatusTypeTest {

    private static final String EXAMPLE_CODE = "exampleCode";
    private static final String OTHER_EXAMPLE_CODE = "exampleCode2";

    private static PersonalIngressionData personalIngressionData;

    // Example professionalSituationConditionType, can be any valid value
    private static final ProfessionalSituationConditionType professionalSituationConditionType =
            ProfessionalSituationConditionType.UNEMPLOYED;
    private static final String CODE = professionalSituationConditionType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Unemployed");

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            personalIngressionData = new PersonalIngressionData();

            return null;
        });
    }

    private ProfessionalStatusType create(String code, LocalizedString name, boolean active) {
        return ProfessionalStatusType.create(code, name, active);
    }

    private void initializeProfessionalStatusTypeRelations(ProfessionalStatusType professionalStatusType) {
        // Initialize relations for the created ProfessionalStatusType
        personalIngressionData.setProfessionalStatusType(professionalStatusType);
        personalIngressionData.setMotherProfessionalStatusType(professionalStatusType);
        personalIngressionData.setFatherProfessionalStatusType(professionalStatusType);
        personalIngressionData.setSpouseProfessionalStatusType(professionalStatusType);
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getProfessionalStatusTypesSet().forEach(t -> {
            clearProfessionalStatusTypeRelations(t);
            t.delete();
        });
    }

    private void clearProfessionalStatusTypeRelations(ProfessionalStatusType professionalStatusType) {
        professionalStatusType.getPersonalIngressionDatasSet().clear();
        professionalStatusType.getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet().clear();
        professionalStatusType.getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet().clear();
        professionalStatusType.getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet().clear();
    }

    @Test
    public void testProfessionalStatusType_create() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        assertNotNull(professionalStatusType);
        assertEquals(CODE, professionalStatusType.getCode());
        assertEquals(QUALIFIED_NAME, professionalStatusType.getName());
    }

    @Test
    public void testProfessionalStatusType_createDuplicate() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        assertThrows(DomainException.class, () -> ProfessionalStatusType.create(CODE, QUALIFIED_NAME, true));
    }

    @Test
    public void testProfessionalStatusType_delete() {
        // Tests deletion of ProfessionalStatusType and its relations

        // Create a new ProfessionalStatusType and its relations
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations(professionalStatusType);

        // Verify initial state before deletion
        assertEquals(false, Bennu.getInstance().getProfessionalStatusTypesSet().isEmpty());
        assertNotEquals(null, professionalStatusType.getRootDomainObject());
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasSet().contains(personalIngressionData));
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet()
                .contains(personalIngressionData));

        // Perform deletion
        clearProfessionalStatusTypeRelations(professionalStatusType);
        professionalStatusType.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getProfessionalStatusTypesSet().isEmpty());
        assertEquals(null, professionalStatusType.getRootDomainObject());
        assertEquals(null, personalIngressionData.getProfessionalStatusType());
        assertEquals(null, personalIngressionData.getMotherProfessionalStatusType());
        assertEquals(null, personalIngressionData.getFatherProfessionalStatusType());
        assertEquals(null, personalIngressionData.getSpouseProfessionalStatusType());
    }

    @Test
    public void testProfessionalStatusType_deleteFailsBecauseRelationsNotCleared() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations(professionalStatusType);

        assertThrows(DomainException.class, () -> professionalStatusType.delete());
    }

    @Test
    public void testProfessionalStatusType_personalIngressionDataRelations() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations(professionalStatusType);

        // Verify initial relations with PersonalIngressionData
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasSet().contains(personalIngressionData));
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet()
                .contains(personalIngressionData));

        // Delete PersonalIngressionData
        personalIngressionData.delete();

        // Verify that relations with ProfessionalStatusType are removed
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasSet().isEmpty());
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet().isEmpty());
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet().isEmpty());
        assertEquals(true, professionalStatusType.getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet().isEmpty());
    }

    @Test
    public void testProfessionalStatusType_findByCode() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        Optional<ProfessionalStatusType> found = ProfessionalStatusType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testProfessionalStatusType_findByCodeNotFound() {
        Optional<ProfessionalStatusType> found = ProfessionalStatusType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testProfessionalStatusType_findAll() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        assertEquals(2, ProfessionalStatusType.findAll().count());
    }

    @Test
    public void testProfessionalStatusType_findActive() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        create(OTHER_EXAMPLE_CODE, QUALIFIED_NAME, false);

        assertEquals(2, ProfessionalStatusType.findActive().count());
    }

    @Test
    public void testProfessionalStatusType_personalIngressionDataSettersAreSynced() {
        ProfessionalStatusType professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations(professionalStatusType);

        ProfessionalSituationConditionType other = ProfessionalSituationConditionType.OTHER;
        ProfessionalStatusType pst = create(other.getName(), QUALIFIED_NAME, true);

        // Assert initial state of PersonalIngressionData
        PersonalIngressionData pid = new PersonalIngressionData();
        assertEquals(null, pid.getProfessionalCondition());
        assertEquals(null, pid.getProfessionalStatusType());
        assertEquals(null, pid.getMotherProfessionalCondition());
        assertEquals(null, pid.getMotherProfessionalStatusType());
        assertEquals(null, pid.getFatherProfessionalCondition());
        assertEquals(null, pid.getFatherProfessionalStatusType());
        assertEquals(null, pid.getSpouseProfessionalCondition());
        assertEquals(null, pid.getSpouseProfessionalStatusType());

        // Call setter in ProfessionalSituationConditionType Enum to trigger the sync in ProfessionalStatusType
        pid.setProfessionalCondition(other);
        pid.setMotherProfessionalCondition(other);
        pid.setFatherProfessionalCondition(other);
        pid.setSpouseProfessionalCondition(other);

        // Assert ProfessionalSituationConditionType and ProfessionalStatusType have the same value
        // (edge case: they can be both null instead of the correct value, next assert will verify that)
        assertEquals(pid.getProfessionalCondition().getName(), pid.getProfessionalStatusType().getCode());

        // Assert ProfessionalStatusType is set to the correct value (instead of being null for example)
        assertEquals(pst, pid.getProfessionalStatusType());

        assertEquals(pid.getMotherProfessionalCondition().getName(), pid.getMotherProfessionalStatusType().getCode());
        assertEquals(pst, pid.getMotherProfessionalStatusType());

        assertEquals(pid.getFatherProfessionalCondition().getName(), pid.getFatherProfessionalStatusType().getCode());
        assertEquals(pst, pid.getFatherProfessionalStatusType());

        assertEquals(pid.getSpouseProfessionalCondition().getName(), pid.getSpouseProfessionalStatusType().getCode());
        assertEquals(pst, pid.getSpouseProfessionalStatusType());

        // Set to null with Enum Setter
        pid.setProfessionalCondition(null);
        pid.setMotherProfessionalCondition(null);
        pid.setFatherProfessionalCondition(null);
        pid.setSpouseProfessionalCondition(null);

        assertEquals(null, pid.getProfessionalCondition());
        assertEquals(null, pid.getProfessionalStatusType());
        assertEquals(null, pid.getMotherProfessionalCondition());
        assertEquals(null, pid.getMotherProfessionalStatusType());
        assertEquals(null, pid.getFatherProfessionalCondition());
        assertEquals(null, pid.getFatherProfessionalStatusType());
        assertEquals(null, pid.getSpouseProfessionalCondition());
        assertEquals(null, pid.getSpouseProfessionalStatusType());

        // Call setter in ProfessionalStatusType to trigger the sync in PersonalIngressionData
        pid.setProfessionalStatusType(pst);
        pid.setMotherProfessionalStatusType(pst);
        pid.setFatherProfessionalStatusType(pst);
        pid.setSpouseProfessionalStatusType(pst);

        // Assert ProfessionalSituationConditionType and ProfessionalStatusType have the same value
        assertEquals(pid.getProfessionalCondition().getName(), pid.getProfessionalStatusType().getCode());
        assertEquals(pst, pid.getProfessionalStatusType());

        assertEquals(pid.getMotherProfessionalCondition().getName(), pid.getMotherProfessionalStatusType().getCode());
        assertEquals(pst, pid.getMotherProfessionalStatusType());

        assertEquals(pid.getFatherProfessionalCondition().getName(), pid.getFatherProfessionalStatusType().getCode());
        assertEquals(pst, pid.getFatherProfessionalStatusType());

        assertEquals(pid.getSpouseProfessionalCondition().getName(), pid.getSpouseProfessionalStatusType().getCode());
        assertEquals(pst, pid.getSpouseProfessionalStatusType());

        // Set to null with ProfessionalStatusType Setter
        pid.setProfessionalStatusType(null);
        pid.setMotherProfessionalStatusType(null);
        pid.setFatherProfessionalStatusType(null);
        pid.setSpouseProfessionalStatusType(null);

        assertEquals(null, pid.getProfessionalCondition());
        assertEquals(null, pid.getProfessionalStatusType());
        assertEquals(null, pid.getMotherProfessionalCondition());
        assertEquals(null, pid.getMotherProfessionalStatusType());
        assertEquals(null, pid.getFatherProfessionalCondition());
        assertEquals(null, pid.getFatherProfessionalStatusType());
        assertEquals(null, pid.getSpouseProfessionalCondition());
        assertEquals(null, pid.getSpouseProfessionalStatusType());
    }
}
