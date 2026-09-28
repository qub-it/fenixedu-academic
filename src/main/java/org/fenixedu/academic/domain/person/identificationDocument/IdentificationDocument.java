package org.fenixedu.academic.domain.person.identificationDocument;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.identificationDocument.validators.IdentificationDocumentValidator;
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
        identificationDocument.setIdentificationDocumentType(identificationDocumentType);
        identificationDocument.setValue(value);

        return identificationDocument;
    }

    public void delete() {
        setPerson(null);
        setIdentificationDocumentType(null);

        setRootDomainObject(null);
        this.deleteDomainObject();
    }

    @Override
    public void setValue(final String value) {
        if (StringUtils.isNotBlank(value) && getIdentificationDocumentType().hasValidator()) {
            IdentificationDocumentValidator validator =
                    IdentificationDocumentValidatorRegistry.get(getIdentificationDocumentType().getValidator());
            if (validator == null) {
                throw new DomainException("error.IdentificationDocument.validator.not.found",
                        getIdentificationDocumentType().getValidator());
            }

            validator.validateValue(value);
        }
        super.setValue(value);
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

            if (getIdentificationDocumentType().hasValidator()) {
                IdentificationDocumentValidator validator =
                        IdentificationDocumentValidatorRegistry.get(getIdentificationDocumentType().getValidator());
                if (validator == null) {
                    throw new DomainException("error.IdentificationDocument.validator.not.found",
                            getIdentificationDocumentType().getValidator());
                }

                validator.validateExtraInfo(extraInfo, getValue());
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
