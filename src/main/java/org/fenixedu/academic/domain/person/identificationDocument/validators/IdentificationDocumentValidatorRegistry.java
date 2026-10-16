package org.fenixedu.academic.domain.person.identificationDocument.validators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IdentificationDocumentValidatorRegistry {

    private static final Map<String, IdentificationDocumentValidator> validators = new HashMap<>();

    public static void register(String validatorName, IdentificationDocumentValidator validator) {
        validators.put(validatorName, validator);
    }

    public static IdentificationDocumentValidator get(String validatorName) {
        return validators.get(validatorName);
    }

    public static Collection<IdentificationDocumentValidator> getAllValidators() {
        return validators.values();
    }
}