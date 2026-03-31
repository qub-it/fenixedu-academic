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
import java.util.Set;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
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

    public static void createIdentificationDocumentTypes() {
        Set.of(IdentificationDocumentType.IDENTITY_CARD_CODE, IdentificationDocumentType.PASSPORT_CODE,
                IdentificationDocumentType.FOREIGNER_IDENTITY_CARD_CODE,
                IdentificationDocumentType.NATIVE_COUNTRY_IDENTITY_CARD_CODE, IdentificationDocumentType.NAVY_IDENTITY_CARD_CODE,
                IdentificationDocumentType.AIR_FORCE_IDENTITY_CARD_CODE, IdentificationDocumentType.OTHER_CODE,
                IdentificationDocumentType.MILITARY_IDENTITY_CARD_CODE, IdentificationDocumentType.EXTERNAL_CODE,
                IdentificationDocumentType.RESIDENCE_AUTHORIZATION_CODE,
                IdentificationDocumentType.EU_PERMANENT_RESIDENCE_CARD_CODE,
                IdentificationDocumentType.EU_REGISTRATION_CERTIFICATE_CODE).forEach(
                type -> IdentificationDocumentType.findByCode(type).orElseGet(
                        () -> IdentificationDocumentType.create(type, BundleUtil.getLocalizedString(Bundle.ENUMERATION, type))));
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getIdentificationDocumentsSet().forEach(IdentificationDocument::delete);
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
        //IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);
        IdentificationDocument identificationDocument =
                IdentificationDocument.find(ID_DOCUMENT_VALUE, identificationDocumentType).orElse(null);
        assertNotNull(identificationDocument);
        identificationDocument.setIdentificationDocumentType(identificationDocumentType);

        // Verify initial state before deletion
        assertFalse(Bennu.getInstance().getIdentificationDocumentTypesSet().isEmpty());
        assertNotEquals(null, identificationDocumentType.getRootDomainObject());
        assertTrue(identificationDocumentType.getIdentificationDocumentsSet().contains(identificationDocument));

        // Perform deletion
        identificationDocumentType.getIdentificationDocumentsSet().clear();
        identificationDocumentType.delete();

        // Verify deletion
        assertFalse(Bennu.getInstance().getIdentificationDocumentTypesSet().contains(identificationDocumentType));
        assertNull(identificationDocumentType.getRootDomainObject());
        assertNull(identificationDocument.getIdentificationDocumentType());
    }

    @Test
    public void testIdentificationDocumentType_deleteFailsBecauseRelationsNotCleared() {
        //IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);
        IdentificationDocument identificationDocument =
                IdentificationDocument.find(ID_DOCUMENT_VALUE, identificationDocumentType).orElse(null);
        assertNotNull(identificationDocument);
        identificationDocument.setIdentificationDocumentType(identificationDocumentType);

        assertThrows(DomainException.class, identificationDocumentType::delete);
    }

    @Test
    public void testIdentificationDocumentType_identificationDocumentRelations() {
        //IdentificationDocumentType identificationDocumentType = IdentificationDocumentType.create(CODE, NAME);
        IdentificationDocumentType identificationDocumentType =
                IdentificationDocumentType.findByCode(ID_DOCUMENT_TYPE).orElse(null);
        assertNotNull(identificationDocumentType);
        IdentificationDocument identificationDocument =
                IdentificationDocument.find(ID_DOCUMENT_VALUE, identificationDocumentType).orElse(null);
        assertNotNull(identificationDocument);
        identificationDocument.setIdentificationDocumentType(identificationDocumentType);

        // Verify initial relations with IdDocument
        assertTrue(identificationDocumentType.getIdentificationDocumentsSet().contains(identificationDocument));

        // Delete with IdDocument
        identificationDocument.delete();

        // Verify that relations with IdentificationDocumentType are removed
        assertTrue(identificationDocumentType.getIdentificationDocumentsSet().isEmpty());
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
}
