package org.fenixedu.academic.domain.person.identificationDocument;

import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTypeTest.initIdentificationDocumentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.person.IdDocument;
import org.fenixedu.academic.domain.person.IdDocumentTypeObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class IdentificationDocumentTest {

    public static final String ID_DOCUMENT_VALUE = "123456789";
    public static final IDDocumentType ID_DOCUMENT_TYPE = IDDocumentType.IDENTITY_CARD;
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
        IdDocumentTypeObject.create(ID_DOCUMENT_TYPE);
        IdDocument idDocument = new IdDocument(person, ID_DOCUMENT_VALUE, ID_DOCUMENT_TYPE);
    }

    @Test
    public void testIdentificationDocument_create() {
        String value = "123";
        IdDocument idDocument = new IdDocument(person, value, ID_DOCUMENT_TYPE);

        assertNotNull(idDocument);
        assertEquals(value, idDocument.getValue());
        assertEquals(ID_DOCUMENT_TYPE, idDocument.getIdDocumentType().getValue());
        assertEquals(person, idDocument.getPerson());
    }
}
