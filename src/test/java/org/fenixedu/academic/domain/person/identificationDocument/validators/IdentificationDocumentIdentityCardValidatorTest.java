package org.fenixedu.academic.domain.person.identificationDocument.validators;

import static org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentTypeTest.initIdentificationDocumentType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.junit.Before;
import org.junit.Test;

import pt.ist.fenixframework.FenixFramework;

public class IdentificationDocumentIdentityCardValidatorTest {

    private static final String validatorName = IdentificationDocumentIdentityCardValidator.class.getName();
    private IdentificationDocumentIdentityCardValidator validator;

    @Before
    public void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initIdentificationDocumentType();
            initIdentityCardValidator();
            validator = (IdentificationDocumentIdentityCardValidator) IdentificationDocumentValidatorRegistry
                    .get(validatorName);
            return null;
        });
    }

    public static void initIdentityCardValidator() {
        if (IdentificationDocumentValidatorRegistry.get(validatorName) == null) {
            IdentificationDocumentValidatorRegistry.register(validatorName,
                    new IdentificationDocumentIdentityCardValidator());
        }
    }

    @Test
    public void testValidate_withOneDigit_validBI() {
        String documentValue = "00000000";
        String extraInfo = "0";

        assertDoesNotThrow(() -> validator.validate(extraInfo, documentValue));
    }

    @Test
    public void testValidate_withOneDigit_invalidChecksum() {
        String documentValue = "12345678";
        String extraInfo = "1";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, documentValue));
        assertEquals("label.identificationDocumentExtraDigit.invalid", exception.getKey());
    }

    @Test
    public void testValidate_withOneDigit_invalidFormat_nonNumeric() {
        String documentValue = "12345678";
        String extraInfo = "A";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, documentValue));
        assertEquals("label.identificationDocumentExtraDigit.invalid.format", exception.getKey());
    }

    @Test
    public void testValidate_withOneDigit_nullExtraInfo() {
        String documentValue = "12345678";

        assertDoesNotThrow(() -> validator.validate(null, documentValue));
    }

    @Test
    public void testValidate_withOneDigit_emptyExtraInfo() {
        String documentValue = "12345678";

        assertDoesNotThrow(() -> validator.validate("", documentValue));
    }

    @Test
    public void testValidate_withMultipleDigits_validCC() {
        String documentValue = "00000000";
        String extraInfo = "0ZZ4";

        assertDoesNotThrow(() -> validator.validate(extraInfo, documentValue));
    }

    @Test
    public void testValidate_withMultipleDigits_invalidFormat() {
        String documentValue = "12345678";
        String extraInfo = "ABCD";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, documentValue));
        assertEquals("label.identificationDocumentSeriesNumber.invalid.format", exception.getKey());
    }

    @Test
    public void testValidate_withMultipleDigits_invalidChecksum() {
        String documentValue = "12345678";
        String extraInfo = "0000";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, documentValue));
        assertEquals("label.identificationDocumentSeriesNumber.invalid", exception.getKey());
    }

    @Test
    public void testValidate_withMultipleDigits_wrongLength() {
        String documentValue = "12345678";
        String extraInfo = "12";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, documentValue));
        assertEquals("label.identificationDocumentSeriesNumber.invalid.format", exception.getKey());
    }
}
