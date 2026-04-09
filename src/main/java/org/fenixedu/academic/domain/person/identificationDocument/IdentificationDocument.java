package org.fenixedu.academic.domain.person.identificationDocument;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;

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

    @Override
    public void setEmissionLocation(final String emissionLocation) {
        super.setEmissionLocation(emissionLocation);

        final Person person = getPerson();
        if (person != null) {
            person.syncEmissionLocationOfDocumentIdFromIdentificationDocument(emissionLocation);
        }
    }

    @Override
    public void setEmissionDate(final LocalDate emissionDate) {
        super.setEmissionDate(emissionDate);

        final Person person = getPerson();
        if (person != null) {
            person.syncEmissionDateOfDocumentIdYearMonthDayFromIdentificationDocument(emissionDate);
        }
    }

    @Override
    public void setExpirationDate(final LocalDate expirationDate) {
        super.setExpirationDate(expirationDate);

        final Person person = getPerson();
        if (person != null) {
            person.syncExpirationDateOfDocumentIdYearMonthDayFromIdentificationDocument(expirationDate);
        }
    }

    public void syncEmissionLocationOfDocumentIdFromPerson(final String value) {
        super.setEmissionLocation(value);
    }

    public void syncEmissionDateOfDocumentIdYearMonthDayFromPerson(final LocalDate value) {
        super.setEmissionDate(value);
    }

    public void syncExpirationDateOfDocumentIdYearMonthDayFromPerson(final LocalDate value) {
        super.setExpirationDate(value);
    }

    public static Optional<IdentificationDocument> find(final String idDocumentValue,
            final IdentificationDocumentType identificationDocumentType) {
        if (identificationDocumentType == null) {
            return Optional.empty();
        }

        return identificationDocumentType.getIdentificationDocumentsSet().stream()
                .filter(idDoc -> idDoc.getValue().equalsIgnoreCase(idDocumentValue))
                .findAny();
    }

    public static Stream<IdentificationDocument> find(final String idDocumentValue) {
        if (StringUtils.isBlank(idDocumentValue)) {
            return Stream.empty();
        }

        return Bennu.getInstance().getIdentificationDocumentsSet().stream()
                .filter(idDoc -> idDocumentValue.equalsIgnoreCase(idDoc.getValue()));
    }

}
