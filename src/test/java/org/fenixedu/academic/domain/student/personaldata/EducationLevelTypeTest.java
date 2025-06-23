package org.fenixedu.academic.domain.student.personaldata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.raides.DegreeClassification;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EducationLevelTypeTest {

    private static EducationLevelType educationLevelType;
    private static DegreeClassification degreeClassification;
    private static PersonalIngressionData personalIngressionData;
    private static PrecedentDegreeInformation precedentDegreeInformation;

    // Example school level type, can be any valid value
    private static final SchoolLevelType schoolLevelType = SchoolLevelType.BACHELOR_DEGREE;
    private static final String CODE = schoolLevelType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Bachelor's Degree");

    @Before
    public void init() {
        // Instantiate the dependencies before each test (@BeforeAll doesn't work here)
        FenixFramework.getTransactionManager().withTransaction(() -> {
            degreeClassification = new DegreeClassification("exampleCode", "description1", "description2", "exampleAbbreviation");
            personalIngressionData = new PersonalIngressionData();
            precedentDegreeInformation = new PrecedentDegreeInformation();

            return null;
        });
    }

    private EducationLevelType create(String code, LocalizedString name, boolean active) {
        return FenixFramework.getTransactionManager().withTransaction(() -> EducationLevelType.create(code, name, active));
    }

    private void initializeEducationLevelTypeRelations() {
        // Initialize relations for the created EducationLevelType
        FenixFramework.getTransactionManager().withTransaction(() -> {
            educationLevelType.addDegreeClassifications(degreeClassification);

            personalIngressionData.setMotherEducationLevelType(educationLevelType);
            personalIngressionData.setFatherEducationLevelType(educationLevelType);
            personalIngressionData.setSpouseEducationLevelType(educationLevelType);

            precedentDegreeInformation.setEducationLevelType(educationLevelType);

            return null;
        });
    }

    @After
    public void cleanup() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            clearEducationLevelTypeRelations();
            Bennu.getInstance().getEducationLevelTypesSet().forEach(EducationLevelType::delete);
            degreeClassification.delete();
            personalIngressionData.delete();
            precedentDegreeInformation.delete();

            return null;
        });
    }

    private void clearEducationLevelTypeRelations() {
        educationLevelType.getPersonalIngressionDatasAsMotherEducationLevelTypeSet().clear();
        educationLevelType.getPersonalIngressionDatasAsFatherEducationLevelTypeSet().clear();
        educationLevelType.getPersonalIngressionDatasAsSpouseEducationLevelTypeSet().clear();
        educationLevelType.getPrecedentDegreeInformationsSet().clear();
    }

    @Test
    public void testEducationLevelType_create() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);

        assertNotEquals(null, educationLevelType);
        assertEquals(CODE, educationLevelType.getCode());
        assertEquals(QUALIFIED_NAME, educationLevelType.getName());
    }

    @Test
    public void testEducationLevelType_createDuplicateCode() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);

        assertThrows(DomainException.class, () -> EducationLevelType.create(CODE, QUALIFIED_NAME, true));
    }

    @Test
    public void testEducationLevelType_delete() {
        // Tests deletion of EducationLevelType and its relations

        // Create a new EducationLevelType and its relations
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();

        // Verify initial state before deletion
        assertEquals(false, Bennu.getInstance().getEducationLevelTypesSet().isEmpty());
        assertNotEquals(null, educationLevelType.getRootDomainObject());
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().contains(degreeClassification));
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsMotherEducationLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsFatherEducationLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsSpouseEducationLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationLevelType.getPrecedentDegreeInformationsSet().contains(precedentDegreeInformation));

        // Perform deletion
        clearEducationLevelTypeRelations();
        educationLevelType.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getEducationLevelTypesSet().isEmpty());
        assertEquals(null, educationLevelType.getRootDomainObject());
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().isEmpty());
        assertEquals(null, personalIngressionData.getMotherEducationLevelType());
        assertEquals(null, personalIngressionData.getFatherEducationLevelType());
        assertEquals(null, personalIngressionData.getSpouseEducationLevelType());
        assertEquals(null, precedentDegreeInformation.getEducationLevelType());
    }

    @Test
    public void testEducationLevelType_deleteFailsBecauseRelationsNotCleared() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();

        assertThrows(DomainException.class, () -> educationLevelType.delete());
    }

    @Test
    public void testEducationLevelType_degreeClassificationRelations() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();

        // Verify initial relation with DegreeClassification
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().contains(degreeClassification));

        // Delete DegreeClassification
        degreeClassification.delete();

        // Verify that relation with EducationLevelType is removed
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().isEmpty());
    }

    @Test
    public void testEducationLevelType_personalIngressionDataRelations() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();

        // Verify initial relations with PersonalIngressionData
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsMotherEducationLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsFatherEducationLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsSpouseEducationLevelTypeSet()
                .contains(personalIngressionData));

        // Delete PersonalIngressionData
        personalIngressionData.delete();

        // Verify that relations with EducationLevelType are removed
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsMotherEducationLevelTypeSet().isEmpty());
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsFatherEducationLevelTypeSet().isEmpty());
        assertEquals(true, educationLevelType.getPersonalIngressionDatasAsSpouseEducationLevelTypeSet().isEmpty());
    }

    @Test
    public void testEducationLevelType_precedentDegreeInformationRelations() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();

        // Verify initial relation with PrecedentDegreeInformation
        assertEquals(true, educationLevelType.getPrecedentDegreeInformationsSet().contains(precedentDegreeInformation));

        // Delete PrecedentDegreeInformation
        precedentDegreeInformation.delete();

        // Verify that relation with EducationLevelType is removed
        assertEquals(true, educationLevelType.getPrecedentDegreeInformationsSet().isEmpty());
    }

    @Test
    public void testEducationLevelType_findByCode() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);

        Optional<EducationLevelType> found = EducationLevelType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testEducationLevelType_findByCodeNotFound() {
        Optional<EducationLevelType> found = EducationLevelType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testEducationLevelType_findAll() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);

        EducationLevelType e = create("exampleCode2", QUALIFIED_NAME, true);
        assertEquals(2, EducationLevelType.findAll().count());
        e.delete();
    }

    @Test
    public void testEducationLevelType_findActive() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);

        EducationLevelType e1 = create("exampleCode3", QUALIFIED_NAME, true);
        EducationLevelType e2 = create("exampleCode4", QUALIFIED_NAME, false);

        assertEquals(2, EducationLevelType.findActive().count());

        e1.delete();
        e2.delete();
    }
}
