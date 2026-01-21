package org.fenixedu.academic.domain.person.identificationDocument;

import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;

public class IdentificationDocument extends IdentificationDocument_Base {

    public IdentificationDocument() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static IdentificationDocument create(final Person person, final String value,
            final IdentificationDocumentType identificationDocumentType) {
        final IdentificationDocument identificationDocument = new IdentificationDocument();
        identificationDocument.setPerson(person);
        identificationDocument.setValue(value);
        identificationDocument.setIdentificationDocumentType(identificationDocumentType);
        return identificationDocument;
    }

    public void delete() {
        setPerson(null);
        setIdentificationDocumentType(null);

        setRootDomainObject(null);
        this.deleteDomainObject();
    }

    public static Optional<IdentificationDocument> findFirst(final String idDocumentValue,
            final IdentificationDocumentType identificationDocumentType) {
        return identificationDocumentType.getIdentificationDocumentsSet().stream()
                .filter(idDoc -> idDoc.getValue().equalsIgnoreCase(idDocumentValue)).findFirst();
    }

    public static Optional<IdentificationDocument> findFirst(String idDocumentValue, String identificationDocumentTypeCode) {
        return IdentificationDocumentType.findByCode(identificationDocumentTypeCode)
                .flatMap(type -> findFirst(idDocumentValue, type));
    }

}
