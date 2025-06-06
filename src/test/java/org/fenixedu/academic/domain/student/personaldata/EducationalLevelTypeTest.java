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
public class EducationalLevelTypeTest {

    private static EducationalLevelType educationalLevelType;
    private static DegreeClassification degreeClassification;
    private static PersonalIngressionData personalIngressionData;
    private static PrecedentDegreeInformation precedentDegreeInformation;

    // Example school level type, can be any valid value
    private static final SchoolLevelType schoolLevelType = SchoolLevelType.BACHELOR_DEGREE;
    private static final String CODE = schoolLevelType.getName();
    private static final LocalizedString QUALIFIED_NAME = new LocalizedString(Locale.getDefault(), "Bachelor's Degree");

    @Before
    public void init() {
        // Create EducationalLevelType based on SchoolLevelType
        FenixFramework.getTransactionManager().withTransaction(() -> {
            educationalLevelType = EducationalLevelType.create(CODE, QUALIFIED_NAME, true, schoolLevelType.isForStudent(),
                    schoolLevelType.isForStudentHousehold(), schoolLevelType.isForMobilityStudent(), schoolLevelType.isOther(),
                    schoolLevelType.isPhDDegree(), schoolLevelType.isSchoolLevelBasicCycle(),
                    schoolLevelType.isHighSchoolOrEquivalent(), schoolLevelType.isHigherEducation());

            // initialize relations
            degreeClassification = new DegreeClassification("exampleCode", "description1", "description2", "exampleAbbreviation");
            educationalLevelType.addDegreeClassifications(degreeClassification);

            personalIngressionData = new PersonalIngressionData();
            personalIngressionData.setMotherEducationalLevelType(educationalLevelType);
            personalIngressionData.setFatherEducationalLevelType(educationalLevelType);
            personalIngressionData.setSpouseEducationalLevelType(educationalLevelType);

            precedentDegreeInformation = new PrecedentDegreeInformation();
            precedentDegreeInformation.setEducationalLevelType(educationalLevelType);

            return null;
        });
    }

    @After
    public void cleanup() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            educationalLevelType.delete();
            degreeClassification.delete();
            personalIngressionData.delete();
            precedentDegreeInformation.delete();

            return null;
        });
    }

    @Test
    public void testEducationalLevelType_Create() {
        assertNotEquals(null, educationalLevelType);
        assertEquals(CODE, educationalLevelType.getCode());
        assertEquals(QUALIFIED_NAME, educationalLevelType.getName());
        assertEquals(schoolLevelType.isForStudent(), educationalLevelType.getForStudent());
        assertEquals(schoolLevelType.isForStudentHousehold(), educationalLevelType.getForStudentHousehold());
        assertEquals(schoolLevelType.isForMobilityStudent(), educationalLevelType.getForMobilityStudent());
        assertEquals(schoolLevelType.isOther(), educationalLevelType.getOther());
        assertEquals(schoolLevelType.isPhDDegree(), educationalLevelType.getPhDDegree());
        assertEquals(schoolLevelType.isSchoolLevelBasicCycle(), educationalLevelType.getSchoolLevelBasicCycle());
        assertEquals(schoolLevelType.isHighSchoolOrEquivalent(), educationalLevelType.getHighSchoolOrEquivalent());
        assertEquals(schoolLevelType.isHigherEducation(), educationalLevelType.getHigherEducation());
    }

    @Test
    public void testEducationalLevelType_CreateDuplicateCode() {
        assertThrows(DomainException.class, () -> {
            EducationalLevelType.create(CODE, QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
        });
    }

    @Test
    public void testEducationalLevelType_Delete() {
        // Tests deletion of EducationalLevelType and its relations

        // Verify initial state before deletion
        assertEquals(false, Bennu.getInstance().getEducationalLevelTypesSet().isEmpty());
        assertNotEquals(null, educationalLevelType.getRootDomainObject());
        assertEquals(true, educationalLevelType.getDegreeClassificationsSet().contains(degreeClassification));
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsMotherEducationalLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsFatherEducationalLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsSpouseEducationalLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationalLevelType.getPrecedentDegreeInformationsSet().contains(precedentDegreeInformation));

        // Perform deletion
        educationalLevelType.delete();

        // Verify deletion
        assertEquals(true, Bennu.getInstance().getEducationalLevelTypesSet().isEmpty());
        assertEquals(null, educationalLevelType.getRootDomainObject());
        assertEquals(true, educationalLevelType.getDegreeClassificationsSet().isEmpty());
        assertEquals(null, personalIngressionData.getMotherEducationalLevelType());
        assertEquals(null, personalIngressionData.getFatherEducationalLevelType());
        assertEquals(null, personalIngressionData.getSpouseEducationalLevelType());
        assertEquals(null, precedentDegreeInformation.getEducationalLevelType());

        // create EducationalLevelType again so cleanup doesn't fail
        educationalLevelType =
                EducationalLevelType.create(CODE, QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
    }

    @Test
    public void testEducationalLevelType_DegreeClassificationRelations() {

        // Verify initial relation with DegreeClassification
        assertEquals(true, educationalLevelType.getDegreeClassificationsSet().contains(degreeClassification));

        // Delete DegreeClassification
        degreeClassification.delete();

        // Verify that relation with EducationalLevelType is removed
        assertEquals(true, educationalLevelType.getDegreeClassificationsSet().isEmpty());
    }

    @Test
    public void testEducationalLevelType_PersonalIngressionDataRelations() {

        // Verify initial relations with PersonalIngressionData
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsMotherEducationalLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsFatherEducationalLevelTypeSet()
                .contains(personalIngressionData));
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsSpouseEducationalLevelTypeSet()
                .contains(personalIngressionData));

        // Delete PersonalIngressionData
        personalIngressionData.delete();

        // Verify that relations with EducationalLevelType are removed
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsMotherEducationalLevelTypeSet().isEmpty());
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsFatherEducationalLevelTypeSet().isEmpty());
        assertEquals(true, educationalLevelType.getPersonalIngressionDatasAsSpouseEducationalLevelTypeSet().isEmpty());
    }

    @Test
    public void testEducationalLevelType_PrecedentDegreeInformationRelations() {

        // Verify initial relation with PrecedentDegreeInformation
        assertEquals(true, educationalLevelType.getPrecedentDegreeInformationsSet().contains(precedentDegreeInformation));

        // Delete PrecedentDegreeInformation
        precedentDegreeInformation.delete();

        // Verify that relation with EducationalLevelType is removed
        assertEquals(true, educationalLevelType.getPrecedentDegreeInformationsSet().isEmpty());
    }

    @Test
    public void testEducationalLevelType_FindByCode() {
        Optional<EducationalLevelType> found = EducationalLevelType.findByCode(CODE);
        assertEquals(true, found.isPresent());
        assertEquals(CODE, found.get().getCode());
    }

    @Test
    public void testEducationalLevelType_FindByCodeNotFound() {
        Optional<EducationalLevelType> found = EducationalLevelType.findByCode("NON_EXISTENT_CODE");
        assertEquals(false, found.isPresent());
    }

    @Test
    public void testEducationalLevelType_FindAll() {
        EducationalLevelType e =
                EducationalLevelType.create("exampleCode2", QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
        assertEquals(2, EducationalLevelType.findAll().count());
        e.delete();
    }

    @Test
    public void testEducationalLevelType_FindActive() {
        EducationalLevelType e1 =
                EducationalLevelType.create("exampleCode3", QUALIFIED_NAME, true, true, true, true, true, true, true, true, true);
        EducationalLevelType e2 =
                EducationalLevelType.create("exampleCode4", QUALIFIED_NAME, false, true, true, true, true, true, true, true,
                        true);

        assertEquals(2, EducationalLevelType.findActive().count());

        e1.delete();
        e2.delete();
    }
}
