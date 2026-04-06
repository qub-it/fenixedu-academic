package org.fenixedu.academic.domain.person.identificationDocument.validators;

import java.util.HashMap;
import java.util.Map;

public class IdentificationDocumentValidatorRegistry {

    private static final Map<String, IdentificationDocumentExtraInfoValidator> validators = new HashMap<>();

    public static void register(String key, IdentificationDocumentExtraInfoValidator validator) {
        validators.put(key, validator);
    }

    public static IdentificationDocumentExtraInfoValidator get(String key) {
        return validators.get(key);
    }
}