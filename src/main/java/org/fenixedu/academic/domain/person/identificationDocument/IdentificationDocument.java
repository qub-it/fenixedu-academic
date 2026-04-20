package org.fenixedu.academic.domain.person.identificationDocument;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.identificationDocument.validators.IdentificationDocumentExtraInfoValidator;
import org.fenixedu.academic.domain.person.identificationDocument.validators.IdentificationDocumentValidatorRegistry;
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

    public boolean hasExtraInfo() {
        return StringUtils.isNotBlank(getExtraInfo());
    }

    public void setExtraInfo(final String extraInfo) {
        if (StringUtils.isNotBlank(extraInfo)) {
            if (!getIdentificationDocumentType().getHasExtraInfo()) {
                throw new DomainException("error.IdentificationDocument.extraInfo.not.allowed",
                        getIdentificationDocumentType().getName().getContent());
            }

            if (getIdentificationDocumentType().hasExtraInfoValidator()) {
                IdentificationDocumentExtraInfoValidator validator =
                        IdentificationDocumentValidatorRegistry.get(getIdentificationDocumentType().getExtraInfoValidator());
                if (validator == null) {
                    throw new DomainException("error.IdentificationDocument.validator.not.found",
                            getIdentificationDocumentType().getExtraInfoValidator());
                }

                validator.validate(extraInfo, getValue());
            }

            super.setExtraInfo(extraInfo);
        }
    }

    public void forceExtraInfo(final String extraInfo) {
        super.setExtraInfo(extraInfo);
    }

    public void clearExtraInfo() {
        super.setExtraInfo(null);
    }

    public static Optional<IdentificationDocument> find(final String identificationDocumentValue,
            final IdentificationDocumentType identificationDocumentType) {
        if (identificationDocumentType == null) {
            return Optional.empty();
        }

        return identificationDocumentType.getIdentificationDocumentsSet().stream()
                .filter(idDoc -> idDoc.getValue().equalsIgnoreCase(identificationDocumentValue))
                .findAny();
    }

    public static Stream<IdentificationDocument> find(final String identificationDocumentValue) {
        if (StringUtils.isBlank(identificationDocumentValue)) {
            return Stream.empty();
        }

        return Bennu.getInstance().getIdentificationDocumentsSet().stream()
                .filter(idDoc -> identificationDocumentValue.equalsIgnoreCase(idDoc.getValue()));
    }

}
