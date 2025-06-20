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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ProfessionalStatusTypeTest {

    private static ProfessionalStatusType professionalStatusType;
    private static PersonalIngressionData personalIngressionData;

    // Example professionalSituationConditionType, can be any valid value
    private static final ProfessionalSituationConditionType professionalSituationConditionType =
            ProfessionalSituationConditionType.UNEMPLOYED;
    private static final String CODE = professionalSituationConditionType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Unemployed");

    @Before
    public void init() {
        // Instantiate the dependencies before each test (@BeforeAll doesn't work here)
        FenixFramework.getTransactionManager().withTransaction(() -> {
            personalIngressionData = new PersonalIngressionData();

            return null;
        });
    }

    private ProfessionalStatusType create(String code, LocalizedString name, boolean active) {
        return FenixFramework.getTransactionManager().withTransaction(() -> ProfessionalStatusType.create(code, name, active));
    }

    private void initializeProfessionalStatusTypeRelations() {
        // Initialize relations for the created ProfessionalStatusType
        FenixFramework.getTransactionManager().withTransaction(() -> {
            personalIngressionData.setProfessionalStatusType(professionalStatusType);
            personalIngressionData.setMotherProfessionalStatusType(professionalStatusType);
            personalIngressionData.setFatherProfessionalStatusType(professionalStatusType);
            personalIngressionData.setSpouseProfessionalStatusType(professionalStatusType);

            return null;
        });
    }

    @After
    public void cleanup() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            clearProfessionalStatusTypeRelations();
            Bennu.getInstance().getProfessionalStatusTypesSet().forEach(ProfessionalStatusType::delete);
            personalIngressionData.delete();
            return null;
        });
    }

    private void clearProfessionalStatusTypeRelations() {
        professionalStatusType.getPersonalIngressionDatasSet().clear();
        professionalStatusType.getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet().clear();
        professionalStatusType.getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet().clear();
        professionalStatusType.getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet().clear();
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
        clearProfessionalStatusTypeRelations();
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

        ProfessionalStatusType p = create("exampleCode", QUALIFIED_NAME, true);
        assertEquals(2, ProfessionalStatusType.findAll().count());
        p.delete();
    }

    @Test
    public void testProfessionalStatusType_findActive() {
        professionalStatusType = create(CODE, QUALIFIED_NAME, true);

        ProfessionalStatusType p1 = create("exampleCode2", QUALIFIED_NAME, true);
        ProfessionalStatusType p2 = create("exampleCode3", QUALIFIED_NAME, false);

        assertEquals(2, ProfessionalStatusType.findActive().count());

        p1.delete();
        p2.delete();
    }
}
