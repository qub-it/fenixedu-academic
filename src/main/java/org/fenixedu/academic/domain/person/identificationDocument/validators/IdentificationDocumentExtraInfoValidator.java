package org.fenixedu.academic.domain.person.identificationDocument.validators;

import org.fenixedu.academic.domain.exceptions.DomainException;

public interface IdentificationDocumentExtraInfoValidator {

    void validate(String extraInfo, String identificationDocumentValue) throws DomainException;

}
