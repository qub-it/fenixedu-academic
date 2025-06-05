package org.fenixedu.academic.domain.student.personaldata;

import static org.junit.jupiter.api.Assertions.*;

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
        // Create EducationLevelType based on SchoolLevelType
        FenixFramework.getTransactionManager().withTransaction(() -> {
            educationLevelType = EducationLevelType.create(CODE, QUALIFIED_NAME, true, schoolLevelType.isForStudent(),
                    schoolLevelType.isForStudentHousehold(), schoolLevelType.isForMobilityStudent(), schoolLevelType.isOther(),
                    schoolLevelType.isPhDDegree(), schoolLevelType.isSchoolLevelBasicCycle(),
                    schoolLevelType.isHighSchoolOrEquivalent(), schoolLevelType.isHigherEducation());

            // initialize relations
            degreeClassification = new DegreeClassification("exampleCode", "description1", "description2", "exampleAbbreviation");
            educationLevelType.addDegreeClassifications(degreeClassification);

            personalIngressionData = new PersonalIngressionData();
            personalIngressionData.setMotherEducationLevelType(educationLevelType);
            personalIngressionData.setFatherEducationLevelType(educationLevelType);
            personalIngressionData.setSpouseEducationLevelType(educationLevelType);

            precedentDegreeInformation = new PrecedentDegreeInformation();
            precedentDegreeInformation.setEducationLevelType(educationLevelType);

            return null;
        });
    }

    @After
    public void cleanup() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            clearEducationalLevelTypeRelations();
            educationLevelType.delete();
            degreeClassification.delete();
            personalIngressionData.delete();
            precedentDegreeInformation.delete();

            return null;
        });
    }

    private void clearEducationalLevelTypeRelations() {
        educationLevelType.getPersonalIngressionDatasAsMotherEducationLevelTypeSet().clear();
        educationLevelType.getPersonalIngressionDatasAsFatherEducationLevelTypeSet().clear();
        educationLevelType.getPersonalIngressionDatasAsSpouseEducationLevelTypeSet().clear();
        educationLevelType.getPrecedentDegreeInformationsSet().clear();
    }

    @Test
    public void testEducationLevelType_Create() {
        assertNotEquals(null, educationLevelType);
        assertEquals(CODE, educationLevelType.getCode());
        assertEquals(QUALIFIED_NAME, educationLevelType.getName());
        assertEquals(schoolLevelType.isForStudent(), educationLevelType.getForStudent());
        assertEquals(schoolLevelType.isForStudentHousehold(), educationLevelType.getForStudentHousehold());
        assertEquals(schoolLevelType.isForMobilityStudent(), educationLevelType.getForMobilityStudent());
        assertEquals(schoolLevelType.isOther(), educationLevelType.getOther());
        assertEquals(schoolLevelType.isPhDDegree(), educationLevelType.getPhDDegree());
        assertEquals(schoolLevelType.isSchoolLevelBasicCycle(), educationLevelType.getSchoolLevelBasicCycle());
        assertEquals(schoolLevelType.isHighSchoolOrEquivalent(), educationLevelType.getHighSchoolOrEquivalent());
        assertEquals(schoolLevelType.isHigherEducation(), educationLevelType.getHigherEducation());
    }

    @Test
    public void testEducationLevelType_CreateDuplicateCode() {
        assertThrows(DomainException.class, () -> {
            EducationLevelType.create(CODE, QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
        });
    }

    @Test
    public void testEducationLevelType_Delete() {
        // Tests deletion of EducationLevelType and its relations

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
        clearEducationalLevelTypeRelations();
        educationLevelType.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getEducationLevelTypesSet().isEmpty());
        assertEquals(null, educationLevelType.getRootDomainObject());
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().isEmpty());
        assertEquals(null, personalIngressionData.getMotherEducationLevelType());
        assertEquals(null, personalIngressionData.getFatherEducationLevelType());
        assertEquals(null, personalIngressionData.getSpouseEducationLevelType());
        assertEquals(null, precedentDegreeInformation.getEducationLevelType());

        // create EducationLevelType again so cleanup doesn't fail
        educationLevelType =
                EducationLevelType.create(CODE, QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
    }

    @Test
    public void testEducationLevelType_DeleteFailsBecauseRelationsNotCleared() {
        assertThrows(DomainException.class, () -> educationLevelType.delete());
    }

    @Test
    public void testEducationLevelType_DegreeClassificationRelations() {

        // Verify initial relation with DegreeClassification
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().contains(degreeClassification));

        // Delete DegreeClassification
        degreeClassification.delete();

        // Verify that relation with EducationLevelType is removed
        assertEquals(true, educationLevelType.getDegreeClassificationsSet().isEmpty());
    }

    @Test
    public void testEducationLevelType_PersonalIngressionDataRelations() {

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
    public void testEducationLevelType_PrecedentDegreeInformationRelations() {

        // Verify initial relation with PrecedentDegreeInformation
        assertEquals(true, educationLevelType.getPrecedentDegreeInformationsSet().contains(precedentDegreeInformation));

        // Delete PrecedentDegreeInformation
        precedentDegreeInformation.delete();

        // Verify that relation with EducationLevelType is removed
        assertEquals(true, educationLevelType.getPrecedentDegreeInformationsSet().isEmpty());
    }

    @Test
    public void testEducationLevelType_FindByCode() {
        Optional<EducationLevelType> found = EducationLevelType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testEducationLevelType_FindByCodeNotFound() {
        Optional<EducationLevelType> found = EducationLevelType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testEducationLevelType_FindAll() {
        EducationLevelType e =
                EducationLevelType.create("exampleCode2", QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
        assertEquals(2, EducationLevelType.findAll().count());
        e.delete();
    }

    @Test
    public void testEducationLevelType_FindActive() {
        EducationLevelType e1 =
                EducationLevelType.create("exampleCode3", QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
        EducationLevelType e2 =
                EducationLevelType.create("exampleCode4", QUALIFIED_NAME, false, true, true, true, true, true, true, true,
                        true);

        assertEquals(2, EducationLevelType.findActive().count());

        e1.delete();
        e2.delete();
    }
}
