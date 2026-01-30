package org.fenixedu.academic.domain.person.identificationDocument;

import java.util.Collections;
import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;

public class IdentificationDocument extends IdentificationDocument_Base {

    protected IdentificationDocument() {
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

    public static Optional<IdentificationDocument> find(final String idDocumentValue,
            final IdentificationDocumentType identificationDocumentType) {
        return Optional.ofNullable(identificationDocumentType).map(IdentificationDocumentType::getIdentificationDocumentsSet)
                .orElse(Collections.emptySet()).stream().filter(idDoc -> idDoc.getValue().equalsIgnoreCase(idDocumentValue))
                .findAny();
    }

}
