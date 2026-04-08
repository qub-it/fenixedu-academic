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
    public void testValidate_withOneDigit_valid() {
        String identificationDocumentValue = "00000000";
        String extraInfo = "0";

        assertDoesNotThrow(() -> validator.validate(extraInfo, identificationDocumentValue));
    }

    @Test
    public void testValidate_withOneDigit_invalidChecksum() {
        String identificationDocumentValue = "12345678";
        String extraInfo = "1";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, identificationDocumentValue));
        assertEquals("label.identificationDocumentExtraDigit.invalid", exception.getKey());
    }

    @Test
    public void testValidate_withOneDigit_invalidFormat() {
        String identificationDocumentValue = "12345678";
        String extraInfo = "A";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, identificationDocumentValue));
        assertEquals("label.identificationDocumentExtraDigit.invalid.format", exception.getKey());
    }

    @Test
    public void testValidate_withOneDigit_null() {
        String identificationDocumentValue = "12345678";

        assertDoesNotThrow(() -> validator.validate(null, identificationDocumentValue));
    }

    @Test
    public void testValidate_withOneDigit_empty() {
        String identificationDocumentValue = "12345678";

        assertDoesNotThrow(() -> validator.validate("", identificationDocumentValue));
    }

    @Test
    public void testValidate_withMultipleDigits_valid() {
        String identificationDocumentValue = "00000000";
        String extraInfo = "0ZZ4";

        assertDoesNotThrow(() -> validator.validate(extraInfo, identificationDocumentValue));
    }

    @Test
    public void testValidate_withMultipleDigits_invalidFormat() {
        String identificationDocumentValue = "12345678";
        String extraInfo = "ABCD";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, identificationDocumentValue));
        assertEquals("label.identificationDocumentSeriesNumber.invalid.format", exception.getKey());
    }

    @Test
    public void testValidate_withMultipleDigits_invalidChecksum() {
        String identificationDocumentValue = "12345678";
        String extraInfo = "0000";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, identificationDocumentValue));
        assertEquals("label.identificationDocumentSeriesNumber.invalid", exception.getKey());
    }

    @Test
    public void testValidate_withMultipleDigits_wrongLength() {
        String identificationDocumentValue = "12345678";
        String extraInfo = "12";

        DomainException exception = assertThrows(DomainException.class,
                () -> validator.validate(extraInfo, identificationDocumentValue));
        assertEquals("label.identificationDocumentSeriesNumber.invalid.format", exception.getKey());
    }
}
