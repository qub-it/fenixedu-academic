package org.fenixedu.academic.domain.person.identificationDocument;

import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTest.ID_DOCUMENT_TYPE;
import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTest.ID_DOCUMENT_VALUE;
import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTest.initIdentificationDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.person.IdDocument;
import org.fenixedu.academic.domain.person.IdDocumentTypeObject;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class IdentificationDocumentTypeTest {
    private static final String CODE = IdentificationDocumentType.OTHER_CODE;
    private static final LocalizedString NAME = new LocalizedString(Locale.getDefault(), "Other");

    @Before
    public void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initIdentificationDocumentType();
            initIdentificationDocument();
            return null;
        });
    }

    public static IdentificationDocumentType initIdentificationDocumentType() {
        String code = IdentificationDocumentType.IDENTITY_CARD_CODE;
        LocalizedString name = new LocalizedString(Locale.getDefault(), code);

        return IdentificationDocumentType.findByCode(code)
                .orElseGet(() -> IdentificationDocumentType.create(code, name));
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getIdDocumentsSet().forEach(IdDocument::delete);
        Bennu.getInstance().getIdentificationDocumentTypesSet().forEach(IdentificationDocumentType::delete);
    }

    @Test
    public void testIdentificationDocumentType_create() {
        IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);

        assertNotNull(identificationDocumentType);
        assertEquals(CODE, identificationDocumentType.getCode());
        assertEquals(NAME, identificationDocumentType.getName());
    }

    @Test
    public void testIdentificationDocumentType_createDuplicate() {
        IdentificationDocumentType.create(CODE, NAME);

        assertThrows(DomainException.class, () -> IdentificationDocumentType.create(CODE, NAME));
    }

    @Test
    public void testIdentificationDocumentType_delete() {
        // Create a new IdentificationDocumentType and its relations
        IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);
        IdDocument idDocument = IdDocument.findFirst(ID_DOCUMENT_VALUE, ID_DOCUMENT_TYPE);
        assertNotNull(idDocument);
        idDocument.setIdentificationDocumentType(identificationDocumentType);

        // Verify initial state before deletion
        assertFalse(Bennu.getInstance().getIdentificationDocumentTypesSet().isEmpty());
        assertNotEquals(null, identificationDocumentType.getRootDomainObject());
        assertTrue(identificationDocumentType.getIdDocumentsSet().contains(idDocument));

        // Perform deletion
        identificationDocumentType.getIdDocumentsSet().clear();
        identificationDocumentType.delete();

        // Verify deletion
        assertFalse(Bennu.getInstance().getIdentificationDocumentTypesSet().contains(identificationDocumentType));
        assertNull(identificationDocumentType.getRootDomainObject());
        assertNull(idDocument.getIdentificationDocumentType());
    }

    @Test
    public void testIdentificationDocumentType_deleteFailsBecauseRelationsNotCleared() {
        IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);
        IdDocument idDocument = IdDocument.findFirst(ID_DOCUMENT_VALUE, ID_DOCUMENT_TYPE);
        assertNotNull(idDocument);
        idDocument.setIdentificationDocumentType(identificationDocumentType);

        assertThrows(DomainException.class, identificationDocumentType::delete);
    }

    @Test
    public void testIdentificationDocumentType_identificationDocumentRelations() {
        IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);
        IdDocument idDocument = IdDocument.findFirst(ID_DOCUMENT_VALUE, ID_DOCUMENT_TYPE);
        assertNotNull(idDocument);
        idDocument.setIdentificationDocumentType(identificationDocumentType);

        // Verify initial relations with IdDocument
        assertTrue(identificationDocumentType.getIdDocumentsSet().contains(idDocument));

        // Delete with IdDocument
        idDocument.delete();

        // Verify that relations with IdentificationDocumentType are removed
        assertTrue(identificationDocumentType.getIdDocumentsSet().isEmpty());
    }

    @Test
    public void testIdentificationDocumentType_findByCode() {
        IdentificationDocumentType.create(CODE, NAME);

        Optional<IdentificationDocumentType> identificationDocumentTypeOpt = IdentificationDocumentType.findByCode(CODE);
        assertTrue(identificationDocumentTypeOpt.isPresent());
        assertEquals(CODE, identificationDocumentTypeOpt.get().getCode());
    }

    @Test
    public void testIdentificationDocumentType_findByCodeNotFound() {
        Optional<IdentificationDocumentType> identificationDocumentTypeOpt = IdentificationDocumentType.findByCode("NON_EXISTENT_CODE");
        assertFalse(identificationDocumentTypeOpt.isPresent());
    }

    @Test
    public void testIdentificationDocumentType_findAll() {
        IdentificationDocumentType.create(CODE, NAME);

        assertEquals(2, IdentificationDocumentType.findAll().count());
    }

    @Test
    public void testIdentificationDocumentType_identificationDocumentSettersAreSynced() {
        IDDocumentType typeEnum = ID_DOCUMENT_TYPE;
        IdDocumentTypeObject typeObject = IdDocumentTypeObject.readByIDDocumentType(typeEnum);
        IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.findByCode(typeEnum.name()).orElseThrow();
        IdDocument idDocument = IdDocument.findFirst(ID_DOCUMENT_VALUE, ID_DOCUMENT_TYPE);
        assertNotNull(idDocument);
        idDocument.setIdDocumentType((IDDocumentType) null);

        // Assert initial state of IdentificationDocument
        assertNull(idDocument.getIdentificationDocumentType());
        assertNull(idDocument.getIdDocumentType());

        // Call setter in IDDocumentType Enum to trigger the sync in IdentificationDocumentType Entity
        idDocument.setIdDocumentType(typeEnum);

        // Assert IDDocumentType Enum and IdentificationDocumentType Entity have the same value
        assertEquals(identificationDocumentType, idDocument.getIdentificationDocumentType());
        assertEquals(idDocument.getIdDocumentType().getValue().name(), idDocument.getIdentificationDocumentType().getCode());

        // Set to null with Enum Setter
        idDocument.setIdDocumentType((IDDocumentType) null);

        assertNull(idDocument.getIdentificationDocumentType());
        assertNull(idDocument.getIdDocumentType());

        // Call setter in IdDocumentTypeObject to trigger the sync in IdentificationDocument
        idDocument.setIdDocumentType(typeObject);

        // Assert IDDocumentType Enum and IdentificationDocumentType Entity have the same value
        assertEquals(identificationDocumentType, idDocument.getIdentificationDocumentType());
        assertEquals(idDocument.getIdDocumentType().getValue().name(), idDocument.getIdentificationDocumentType().getCode());

        // Set to null with IdDocumentTypeObject Setter
        idDocument.setIdDocumentType((IdDocumentTypeObject) null);

        assertNull(idDocument.getIdDocumentType());
        assertNull(idDocument.getIdentificationDocumentType());

        // Call setter in IdentificationDocumentType to trigger the sync in IdentificationDocument
        idDocument.setIdentificationDocumentType(identificationDocumentType);

        // Assert IDDocumentType Enum and IdentificationDocumentType Entity have the same value
        assertEquals(identificationDocumentType, idDocument.getIdentificationDocumentType());
        assertEquals(idDocument.getIdDocumentType().getValue().name(), idDocument.getIdentificationDocumentType().getCode());

        // Set to null with IdentificationDocumentType Setter
        idDocument.setIdentificationDocumentType(null);

        assertNull(idDocument.getIdDocumentType());
        assertNull(idDocument.getIdentificationDocumentType());
    }
}
