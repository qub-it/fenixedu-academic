package org.fenixedu.academic.domain.person.identificationDocument;

import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTypeTest.initIdentificationDocumentType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.dto.person.PersonBean;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.YearMonthDay;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class IdentificationDocumentTest {

    public static final String ID_DOCUMENT_VALUE = "00000000";
    public static final String ID_DOCUMENT_TYPE = IdentificationDocumentType.IDENTITY_CARD_CODE;
    private static Person person;
    private static IdentificationDocument idDoc;

    @Before
    public void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initIdentificationDocumentType();
            initIdentificationDocument();
            return null;
        });
    }

    public static void initIdentificationDocument() {
        if (person == null) {
            person = StudentTest.createStudent("Student", "student").getPerson();
        }
        IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE)
                .orElseGet(IdentificationDocumentTypeTest::initIdentificationDocumentType);
        idDoc = IdentificationDocument.create(person, ID_DOCUMENT_VALUE, identificationDocumentType);
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getIdentificationDocumentsSet().forEach(IdentificationDocument::delete);
        Bennu.getInstance().getIdentificationDocumentTypesSet().forEach(IdentificationDocumentType::delete);
    }

    @Test
    public void testIdentificationDocument_create() {
        String value = "123";
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        IdentificationDocument identificationDocument = IdentificationDocument.create(person, value, identificationDocumentType);

        assertNotNull(identificationDocument);
        assertEquals(value, identificationDocument.getValue());
        assertEquals(ID_DOCUMENT_TYPE, identificationDocument.getIdentificationDocumentType().getCode());
        assertEquals(person, identificationDocument.getPerson());
    }

    @Test
    public void testIdentificationDocument_delete() {
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);
        IdentificationDocument identificationDocument =
                IdentificationDocument.find(ID_DOCUMENT_VALUE, identificationDocumentType).orElse(null);
        assertNotNull(identificationDocument);

        // Verify initial state before deletion
        assertFalse(Bennu.getInstance().getIdentificationDocumentTypesSet().isEmpty());
        assertNotNull(identificationDocument.getRootDomainObject());
        assertNotNull(identificationDocument.getPerson());
        assertSame(identificationDocument.getIdentificationDocumentType(), identificationDocumentType);

        // Perform deletion
        identificationDocument.delete();

        // Verify deletion
        assertFalse(Bennu.getInstance().getIdentificationDocumentsSet().contains(identificationDocument));
        assertNull(identificationDocument.getRootDomainObject());
        assertNull(identificationDocument.getIdentificationDocumentType());
        assertNull(identificationDocument.getPerson());
        assertNotNull(identificationDocumentType);
    }

    @Test
    public void testIdentificationDocument_findFirst() {
        // Find first by IdentificationDocumentType
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);
        Optional<IdentificationDocument> identificationDocumentOptByType =
                IdentificationDocument.find(ID_DOCUMENT_VALUE, identificationDocumentType);
        assertTrue(identificationDocumentOptByType.isPresent());
        assertEquals(ID_DOCUMENT_VALUE, identificationDocumentOptByType.get().getValue());

        // Find first by IdentificationDocumentType code
        Optional<IdentificationDocument> identificationDocumentOptByCode =
                IdentificationDocument.find(ID_DOCUMENT_VALUE, identificationDocumentType);
        assertTrue(identificationDocumentOptByCode.isPresent());
        assertEquals(ID_DOCUMENT_VALUE, identificationDocumentOptByCode.get().getValue());
    }

    @Test
    public void testIdentificationDocument_findFirstNotFound() {
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);

        Optional<IdentificationDocument> identificationDocumentOpt =
                IdentificationDocument.find("NON_EXISTENT_VALUE", identificationDocumentType);
        assertFalse(identificationDocumentOpt.isPresent());
    }

    @Test
    public void testIdentificationDocument_createWithNewPerson() {
        Person newPerson = StudentTest.createStudent("New Test Person", "newtestperson").getPerson();
        assertNotNull(newPerson);
        assertEquals("New Test Person", newPerson.getName());

        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);

        String newDocValue = "12345678";
        IdentificationDocument newDoc = IdentificationDocument.create(newPerson, newDocValue, identificationDocumentType);

        assertNotNull(newDoc);
        assertEquals(newDocValue, newDoc.getValue());
        assertEquals(newPerson, newDoc.getPerson());
        assertEquals(identificationDocumentType, newDoc.getIdentificationDocumentType());
        assertTrue(Bennu.getInstance().getIdentificationDocumentsSet().contains(newDoc));
    }

    @Test
    public void testIdentificationDocument_settersWithNullPerson() {
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        IdentificationDocument identificationDocument =
                IdentificationDocument.create(null, "12345678", identificationDocumentType);

        assertNull(identificationDocument.getPerson());

        YearMonthDay emissionDate = new YearMonthDay(2020, 1, 15);
        identificationDocument.setEmissionDate(emissionDate.toLocalDate());
        assertEquals(emissionDate.toLocalDate(), identificationDocument.getEmissionDate());

        String emissionLocation = "Lisbon";
        identificationDocument.setEmissionLocation(emissionLocation);
        assertEquals(emissionLocation, identificationDocument.getEmissionLocation());

        YearMonthDay expirationDate = new YearMonthDay(2030, 12, 31);
        identificationDocument.setExpirationDate(expirationDate.toLocalDate());
        assertEquals(expirationDate.toLocalDate(), identificationDocument.getExpirationDate());
    }

    @Test
    public void testIdentificationDocument_extraInfoValidatorIsNull() {
        IdentificationDocumentType identificationDocumentType = idDoc.getIdentificationDocumentType();
        assertNotNull(identificationDocumentType);

        identificationDocumentType.setValidator(null);
        identificationDocumentType.setHasExtraInfo(true);

        String extraInfo = "0";
        assertDoesNotThrow(() -> idDoc.setExtraInfo(extraInfo));
        assertEquals(extraInfo, idDoc.getExtraInfo());
    }

    @Test
    public void testIdentificationDocument_extraInfoNotAllowed() {
        IdentificationDocumentType identificationDocumentType = idDoc.getIdentificationDocumentType();
        assertNotNull(identificationDocumentType);

        identificationDocumentType.setHasExtraInfo(false);

        String extraInfo = "0";
        DomainException exception = assertThrows(DomainException.class, () -> idDoc.setExtraInfo(extraInfo));
        assertEquals("error.IdentificationDocument.extraInfo.not.allowed", exception.getKey());
    }

    @Test
    public void testPerson_setIdentification() {
        Person newPerson = StudentTest.createStudent("Identification Create Test", "identification.create.test").getPerson();
        IdentificationDocumentType type = IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(type);

        // accepts the value and creates the document.
        newPerson.setIdentification("12345678", type);

        assertNotNull(newPerson.getDefaultIdentificationDocument());
        assertEquals("12345678", newPerson.getDefaultIdentificationDocument().getValue());
        assertEquals(type, newPerson.getDefaultIdentificationDocument().getIdentificationDocumentType());

        // updating identification value
        newPerson.setIdentification("87654321", type);

        assertEquals("87654321", newPerson.getDefaultIdentificationDocument().getValue());
        assertEquals(type, newPerson.getDefaultIdentificationDocument().getIdentificationDocumentType());

        // throws when setting blank values to a person default document
        assertEquals("error.person.empty.idDocumentType",
                assertThrows(DomainException.class, () -> newPerson.setIdentification("00000000", null)).getKey());
        assertEquals("error.person.empty.documentIdNumber",
                assertThrows(DomainException.class, () -> newPerson.setIdentification(null, type)).getKey());
        assertEquals("87654321", newPerson.getDefaultIdentificationDocument().getValue());
        assertEquals(type, newPerson.getDefaultIdentificationDocument().getIdentificationDocumentType());

    }

    @Test
    public void testPersonBean_save_updatesExistingDocument() {
        PersonBean personBean = new PersonBean(person);
        personBean.setDocumentIdNumber("12345678");

        // person editing path
        person.editPersonalInformation(personBean);

        assertEquals("12345678", person.getDefaultIdentificationDocument().getValue());
        assertEquals(idDoc.getIdentificationDocumentType(),
                person.getDefaultIdentificationDocument().getIdentificationDocumentType());
    }
}
