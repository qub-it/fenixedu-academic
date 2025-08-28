package org.fenixedu.academic.domain.student.personaldata;

import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProfessionalStatusType extends ProfessionalStatusType_Base {

    public static final String STUDENT = "STUDENT";
    public static final String UNKNOWN = "UNKNOWN";

    protected ProfessionalStatusType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static ProfessionalStatusType create(String code, LocalizedString name, boolean active) {
        ProfessionalStatusType type = new ProfessionalStatusType();
        type.setCode(code);
        type.setName(name);
        type.setActive(active);
        return type;
    }

    public void delete() {
        if (!getPersonalIngressionDatasSet().isEmpty() || !getPersonalIngressionDatasAsMotherProfessionalStatusTypeSet().isEmpty()
                || !getPersonalIngressionDatasAsFatherProfessionalStatusTypeSet().isEmpty()
                || !getPersonalIngressionDatasAsSpouseProfessionalStatusTypeSet().isEmpty()) {
            throw new DomainException(BundleUtil.getString(Bundle.APPLICATION,
                    "error.ProfessionalStatusType.cannot.delete.related.to.PersonalIngressionData"));
        }

        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    public void setCode(String code) {
        if (isDuplicateCode(code)) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.ProfessionalStatusType.code.already.exists", code));
        }

        super.setCode(code);
    }

    private boolean isDuplicateCode(String code) {
        return findByCode(code).filter(t -> t != this).isPresent();
    }

    public static Optional<ProfessionalStatusType> findByCode(String code) {
        return findAll().filter(t -> code != null && code.equals(t.getCode())).findFirst();
    }

    public static Stream<ProfessionalStatusType> findAll() {
        return Bennu.getInstance().getProfessionalStatusTypesSet().stream();
    }

    public static Stream<ProfessionalStatusType> findActive() {
        return Bennu.getInstance().getProfessionalStatusTypesSet().stream().filter(ProfessionalStatusType::getActive);
    }
}
