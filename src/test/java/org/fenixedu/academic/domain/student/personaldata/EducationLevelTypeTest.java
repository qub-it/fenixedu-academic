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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EducationLevelTypeTest {

    private static final String EXAMPLE_CODE = "exampleCode";
    private static final String OTHER_EXAMPLE_CODE = "exampleCode2";

    private static EducationLevelType educationLevelType;
    private static DegreeClassification degreeClassification;
    private static PersonalIngressionData personalIngressionData;
    private static PrecedentDegreeInformation precedentDegreeInformation;

    // Example school level type, can be any valid value
    private static final SchoolLevelType schoolLevelType = SchoolLevelType.BACHELOR_DEGREE;
    private static final String CODE = schoolLevelType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Bachelor's Degree");

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            degreeClassification = new DegreeClassification("code1", "description1", "description2", "exampleAbbreviation");
            personalIngressionData = new PersonalIngressionData();
            precedentDegreeInformation = new PrecedentDegreeInformation();

            return null;
        });
    }

    private EducationLevelType create(String code, LocalizedString name, boolean active) {
        return EducationLevelType.create(code, name, active);
    }

    private void initializeEducationLevelTypeRelations() {
        // Initialize relations for the created EducationLevelType
        educationLevelType.addDegreeClassifications(degreeClassification);

        personalIngressionData.setMotherEducationLevelType(educationLevelType);
        personalIngressionData.setFatherEducationLevelType(educationLevelType);
        personalIngressionData.setSpouseEducationLevelType(educationLevelType);

        precedentDegreeInformation.setEducationLevelType(educationLevelType);
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getEducationLevelTypesSet().forEach(t -> {
            clearEducationLevelTypeRelations(t);
            t.delete();
        });
    }

    private void clearEducationLevelTypeRelations(EducationLevelType elt) {
        elt.getPersonalIngressionDatasAsMotherEducationLevelTypeSet().clear();
        elt.getPersonalIngressionDatasAsFatherEducationLevelTypeSet().clear();
        elt.getPersonalIngressionDatasAsSpouseEducationLevelTypeSet().clear();
        elt.getPrecedentDegreeInformationsSet().clear();
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
        clearEducationLevelTypeRelations(educationLevelType);
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

        create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        assertEquals(2, EducationLevelType.findAll().count());
    }

    @Test
    public void testEducationLevelType_findActive() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);

        create(EXAMPLE_CODE, QUALIFIED_NAME, true);
        create(OTHER_EXAMPLE_CODE, QUALIFIED_NAME, false);

        assertEquals(2, EducationLevelType.findActive().count());
    }

    @Test
    public void testEducationLevelType_personalIngressionDataSettersAreSynced() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();

        SchoolLevelType otherSituation = SchoolLevelType.OTHER_SITUATION;
        EducationLevelType elt = create(otherSituation.getName(), QUALIFIED_NAME, true);

        // Assert initial state of PersonalIngressionData
        PersonalIngressionData pid = new PersonalIngressionData();
        assertEquals(null, pid.getMotherSchoolLevel());
        assertEquals(null, pid.getMotherEducationLevelType());
        assertEquals(null, pid.getFatherSchoolLevel());
        assertEquals(null, pid.getFatherEducationLevelType());
        assertEquals(null, pid.getSpouseSchoolLevel());
        assertEquals(null, pid.getSpouseEducationLevelType());

        // Call setter in SchoolLevelType Enum to trigger the sync in EducationLevelType
        pid.setMotherSchoolLevel(otherSituation);
        pid.setFatherSchoolLevel(otherSituation);
        pid.setSpouseSchoolLevel(otherSituation);

        // Assert SchoolLevelType and EducationLevelType have the same value
        // (edge case: they can be both null instead of the correct value, next assert will verify that)
        assertEquals(pid.getMotherSchoolLevel().getName(), pid.getMotherEducationLevelType().getCode());

        // Assert EducationLevelType is set to the correct value (instead of being null for example)
        assertEquals(elt, pid.getMotherEducationLevelType());

        assertEquals(pid.getFatherSchoolLevel().getName(), pid.getFatherEducationLevelType().getCode());
        assertEquals(elt, pid.getFatherEducationLevelType());

        assertEquals(pid.getSpouseSchoolLevel().getName(), pid.getSpouseEducationLevelType().getCode());
        assertEquals(elt, pid.getSpouseEducationLevelType());

        // Set to null with Enum setter
        pid.setMotherSchoolLevel(null);
        pid.setFatherSchoolLevel(null);
        pid.setSpouseSchoolLevel(null);

        assertEquals(null, pid.getMotherSchoolLevel());
        assertEquals(null, pid.getMotherEducationLevelType());
        assertEquals(null, pid.getFatherSchoolLevel());
        assertEquals(null, pid.getFatherEducationLevelType());
        assertEquals(null, pid.getSpouseSchoolLevel());
        assertEquals(null, pid.getSpouseEducationLevelType());

        // Call setter in EducationLevelType to trigger the sync in SchoolLevelType
        pid.setMotherEducationLevelType(elt);
        pid.setFatherEducationLevelType(elt);
        pid.setSpouseEducationLevelType(elt);

        // Assert SchoolLevelType and EducationLevelType have the same value
        assertEquals(pid.getMotherSchoolLevel().getName(), pid.getMotherEducationLevelType().getCode());
        assertEquals(elt, pid.getMotherEducationLevelType());

        assertEquals(pid.getFatherSchoolLevel().getName(), pid.getFatherEducationLevelType().getCode());
        assertEquals(elt, pid.getFatherEducationLevelType());

        assertEquals(pid.getSpouseSchoolLevel().getName(), pid.getSpouseEducationLevelType().getCode());
        assertEquals(elt, pid.getSpouseEducationLevelType());

        // Set to null with EducationLevelType setter
        pid.setMotherEducationLevelType(null);
        pid.setFatherEducationLevelType(null);
        pid.setSpouseEducationLevelType(null);

        assertEquals(null, pid.getMotherSchoolLevel());
        assertEquals(null, pid.getMotherEducationLevelType());
        assertEquals(null, pid.getFatherSchoolLevel());
        assertEquals(null, pid.getFatherEducationLevelType());
        assertEquals(null, pid.getSpouseSchoolLevel());
        assertEquals(null, pid.getSpouseEducationLevelType());
    }

    @Test
    public void testEducationLevelType_precedentDegreeInformationSettersAreSynced() {
        educationLevelType = create(CODE, QUALIFIED_NAME, true);
        initializeEducationLevelTypeRelations();
        
        SchoolLevelType otherSituation = SchoolLevelType.OTHER_SITUATION;
        EducationLevelType elt = create(otherSituation.getName(), QUALIFIED_NAME, true);

        // Assert initial state of PrecedentDegreeInformation
        PrecedentDegreeInformation pdi = new PrecedentDegreeInformation();
        assertEquals(null, pdi.getSchoolLevel());
        assertEquals(null, pdi.getEducationLevelType());

        // Call setter in SchoolLevelType Enum to trigger the sync in EducationLevelType
        pdi.setSchoolLevel(otherSituation);
        assertEquals(pdi.getSchoolLevel().getName(), pdi.getEducationLevelType().getCode());
        assertEquals(elt, pdi.getEducationLevelType());

        // Set to null with Enum setter
        pdi.setSchoolLevel(null);

        assertEquals(null, pdi.getSchoolLevel());
        assertEquals(null, pdi.getEducationLevelType());

        // Call setter in EducationLevelType to trigger the sync in SchoolLevelType
        pdi.setEducationLevelType(elt);
        assertEquals(pdi.getSchoolLevel().getName(), pdi.getEducationLevelType().getCode());
        assertEquals(elt, pdi.getEducationLevelType());

        // Set to null with EducationLevelType setter
        pdi.setEducationLevelType(null);

        assertEquals(null, pdi.getSchoolLevel());
        assertEquals(null, pdi.getEducationLevelType());
    }
}
