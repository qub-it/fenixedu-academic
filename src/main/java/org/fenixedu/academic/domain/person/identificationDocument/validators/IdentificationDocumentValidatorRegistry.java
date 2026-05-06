package org.fenixedu.academic.domain.person.identificationDocument.validators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IdentificationDocumentValidatorRegistry {

    private static final Map<String, IdentificationDocumentExtraInfoValidator> validators = new HashMap<>();

    public static void register(String validatorName, IdentificationDocumentExtraInfoValidator validator) {
        validators.put(validatorName, validator);
    }

    public static IdentificationDocumentExtraInfoValidator get(String validatorName) {
        return validators.get(validatorName);
    }

    public static Collection<IdentificationDocumentExtraInfoValidator> getAllValidators() {
        return validators.values();
    }
}