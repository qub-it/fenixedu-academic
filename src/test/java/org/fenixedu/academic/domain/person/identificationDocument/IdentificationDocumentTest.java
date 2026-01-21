package org.fenixedu.academic.domain.person.identificationDocument;

import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTypeTest.initIdentificationDocumentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.bennu.core.domain.Bennu;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class IdentificationDocumentTest {

    public static final String ID_DOCUMENT_VALUE = "123456789";
    public static final String ID_DOCUMENT_TYPE = IdentificationDocumentType.IDENTITY_CARD_CODE;
    private static Person person;

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
        IdentificationDocument identificationDocumentDocument =
                IdentificationDocument.create(person, ID_DOCUMENT_VALUE, identificationDocumentType);
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
                IdentificationDocument.findFirst(ID_DOCUMENT_VALUE, identificationDocumentType).orElse(null);
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
                IdentificationDocument.findFirst(ID_DOCUMENT_VALUE, identificationDocumentType);
        assertTrue(identificationDocumentOptByType.isPresent());
        assertEquals(ID_DOCUMENT_VALUE, identificationDocumentOptByType.get().getValue());

        // Find first by IdentificationDocumentType code
        Optional<IdentificationDocument> identificationDocumentOptByCode =
                IdentificationDocument.findFirst(ID_DOCUMENT_VALUE, ID_DOCUMENT_TYPE);
        assertTrue(identificationDocumentOptByCode.isPresent());
        assertEquals(ID_DOCUMENT_VALUE, identificationDocumentOptByCode.get().getValue());
    }

    @Test
    public void testIdentificationDocument_findFirstNotFound() {
        Optional<IdentificationDocument> identificationDocumentOpt =
                IdentificationDocument.findFirst("NON_EXISTENT_VALUE", ID_DOCUMENT_TYPE);
        assertFalse(identificationDocumentOpt.isPresent());
    }
}
