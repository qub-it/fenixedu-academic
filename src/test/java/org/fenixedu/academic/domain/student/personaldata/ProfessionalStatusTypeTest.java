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

    private static ProfessionalStatusType professionalStatusType;
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

    private void initializeProfessionalStatusTypeRelations() {
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

    private void clearProfessionalStatusTypeRelations(ProfessionalStatusType pst) {
        pst.getPersonalIngressionDatasSet().clear();
        pst.getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet().clear();
        pst.getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet().clear();
        pst.getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet().clear();
    }

    @Test
    public void testProfessionalStatusType_create() {
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        assertNotNull(professionalStatusType);
        assertEquals(CODE, professionalStatusType.getCode());
        assertEquals(QUALIFIED_NAME, professionalStatusType.getName());
    }

    @Test
    public void testProfessionalStatusType_createDuplicate() {
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        assertThrows(DomainException.class, () -> ProfessionalStatusType.create(CODE, QUALIFIED_NAME, true));
    }

    @Test
    public void testProfessionalStatusType_delete() {
        // Tests deletion of ProfessionalStatusType and its relations

        // Create a new ProfessionalStatusType and its relations
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations();

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
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations();

        assertThrows(DomainException.class, () -> professionalStatusType.delete());
    }

    @Test
    public void testProfessionalStatusType_personalIngressionDataRelations() {
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations();

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
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);

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
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        assertEquals(2, ProfessionalStatusType.findAll().count());
    }

    @Test
    public void testProfessionalStatusType_findActive() {
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        create(OTHER_EXAMPLE_CODE, QUALIFIED_NAME, false);

        assertEquals(2, ProfessionalStatusType.findActive().count());
    }

    @Test
    public void testProfessionalStatusType_personalIngressionDataSettersAreSynced() {
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);
        initializeProfessionalStatusTypeRelations();

        ProfessionalSituationConditionType other = ProfessionalSituationConditionType.OTHER;
        ProfessionalStatusType pst = create(other.getName(), QUALIFIED_NAME, true);

        // Call setter in ProfessionalSituationConditionType Enum to trigger the sync in ProfessionalStatusType
        personalIngressionData.setProfessionalCondition(other);
        personalIngressionData.setMotherProfessionalCondition(other);
        personalIngressionData.setFatherProfessionalCondition(other);
        personalIngressionData.setSpouseProfessionalCondition(other);

        // Assert ProfessionalSituationConditionType and ProfessionalStatusType have the same value
        // (edge case: they can be both null instead of the correct value, next assert will verify that)
        assertEquals(personalIngressionData.getProfessionalCondition().getName(),
                personalIngressionData.getProfessionalStatusType().getCode());

        // Assert ProfessionalStatusType is set to the correct value (instead of being null for example)
        assertEquals(pst, personalIngressionData.getProfessionalStatusType());

        assertEquals(personalIngressionData.getMotherProfessionalCondition().getName(),
                personalIngressionData.getMotherProfessionalStatusType().getCode());
        assertEquals(pst, personalIngressionData.getMotherProfessionalStatusType());

        assertEquals(personalIngressionData.getFatherProfessionalCondition().getName(),
                personalIngressionData.getFatherProfessionalStatusType().getCode());
        assertEquals(pst, personalIngressionData.getFatherProfessionalStatusType());

        assertEquals(personalIngressionData.getSpouseProfessionalCondition().getName(),
                personalIngressionData.getSpouseProfessionalStatusType().getCode());
        assertEquals(pst, personalIngressionData.getSpouseProfessionalStatusType());
    }
}
