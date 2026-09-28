package org.fenixedu.academic.domain.person.identificationDocument.validators;

public interface IdentificationDocumentValidator {

    void validateValue(String identificationDocumentValue);

    void validateExtraInfo(String extraInfo, String identificationDocumentValue);

    String getLocalizedName();
}
