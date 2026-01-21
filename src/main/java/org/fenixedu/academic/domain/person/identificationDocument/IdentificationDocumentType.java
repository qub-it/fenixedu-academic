package org.fenixedu.academic.domain.person.identificationDocument;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class IdentificationDocumentType extends IdentificationDocumentType_Base {

    public static final String IDENTITY_CARD_CODE = "IDENTITY_CARD";
    public static final String PASSPORT_CODE = "PASSPORT";
    public static final String FOREIGNER_IDENTITY_CARD_CODE = "FOREIGNER_IDENTITY_CARD";
    public static final String NATIVE_COUNTRY_IDENTITY_CARD_CODE = "NATIVE_COUNTRY_IDENTITY_CARD";
    public static final String NAVY_IDENTITY_CARD_CODE = "NAVY_IDENTITY_CARD";
    public static final String AIR_FORCE_IDENTITY_CARD_CODE = "AIR_FORCE_IDENTITY_CARD";
    public static final String OTHER_CODE = "OTHER";
    public static final String MILITARY_IDENTITY_CARD_CODE = "MILITARY_IDENTITY_CARD";
    public static final String EXTERNAL_CODE = "EXTERNAL";
    public static final String CITIZEN_CARD_CODE = "CITIZEN_CARD";
    public static final String RESIDENCE_AUTHORIZATION_CODE = "RESIDENCE_AUTHORIZATION";
    public static final String EU_PERMANENT_RESIDENCE_CARD_CODE = "EU_PERMANENT_RESIDENCE_CARD";
    public static final String EU_REGISTRATION_CERTIFICATE_CODE = "EU_REGISTRATION_CERTIFICATE";

    public IdentificationDocumentType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static IdentificationDocumentType create(final String code, final LocalizedString name) {
        final IdentificationDocumentType identificationDocumentType = new IdentificationDocumentType();
        identificationDocumentType.setCode(code);
        identificationDocumentType.setName(name);
        return identificationDocumentType;
    }

    public void delete() {
        if (!getIdentificationDocumentsSet().isEmpty()) {
            throw new DomainException("error.IdentificationDocumentType.cannot.delete.related.to.IdentificationDocuments");
        }

        setRootDomainObject(null);
        this.deleteDomainObject();
    }

    @Override
    public void setCode(final String code) {
        if (StringUtils.isBlank(code)) {
            throw new DomainException("error.IdentificationDocumentType.code.cannot.be.empty");
        }

        Optional<IdentificationDocumentType> findByCode = findByCode(code);
        if (findByCode.isPresent() && findByCode.get() != this) {
            throw new DomainException("error.IdentificationDocumentType.code.already.exists", code);
        }

        super.setCode(code);
    }

    public String getLocalizedName() {
        return getLocalizedNameI18N().getContent();
    }

    public String getLocalizedName(final Locale locale) {
        return getLocalizedNameI18N().getContent(locale);
    }

    public LocalizedString getLocalizedNameI18N() {
        return BundleUtil.getLocalizedString(Bundle.ENUMERATION, getCode());
    }

    public static Optional<IdentificationDocumentType> findByCode(final String code) {
        return findAll().filter(identificationDocumentType -> Objects.equals(identificationDocumentType.getCode(), code))
                .findAny();
    }

    public static Stream<IdentificationDocumentType> findAll() {
        return Bennu.getInstance().getIdentificationDocumentTypesSet().stream();
    }

    public static IdentificationDocumentType findIdentificationDocumentType(IDDocumentType idDocumentType) {
        if (idDocumentType == null) {
            return null;
        }

        return IdentificationDocumentType.findByCode(idDocumentType.name())
                .orElseThrow(() -> new DomainException("error.IdentificationDocumentType.not.found", idDocumentType.name()));
    }

    public static IDDocumentType findIDDocumentType(IdentificationDocumentType identificationDocumentType) {
        if (identificationDocumentType == null) {
            return null;
        }

        return IDDocumentType.findByCode(identificationDocumentType.getCode())
                .orElseThrow(() -> new DomainException("error.IDDocumentType.not.found", identificationDocumentType.getCode()));
    }

}
