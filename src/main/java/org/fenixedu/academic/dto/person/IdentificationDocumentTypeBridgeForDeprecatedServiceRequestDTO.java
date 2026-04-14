package org.fenixedu.academic.dto.person;

import org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentType;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * @deprecated This class is only used as a bridge for the deprecated document service requests, and should not be used in
 *         any new code.
 */
@Deprecated
public class IdentificationDocumentTypeBridgeForDeprecatedServiceRequestDTO {

    private IdentificationDocumentType identificationDocumentType;

    public IdentificationDocumentTypeBridgeForDeprecatedServiceRequestDTO(IdentificationDocumentType identificationDocumentType) {
        this.identificationDocumentType = identificationDocumentType;
    }

    public LocalizedString getLocalizedNameI18N() {
        return identificationDocumentType != null ? identificationDocumentType.getName() : null;
    }

    @Override
    public String toString() {
        return identificationDocumentType != null ? identificationDocumentType.getCode() : super.toString();
    }
}
