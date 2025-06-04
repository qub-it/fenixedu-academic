package org.fenixedu.academic.domain.student.personaldata;

import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProfessionalStatusType extends ProfessionalStatusType_Base {
    protected ProfessionalStatusType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static ProfessionalStatusType create(String code, LocalizedString name, boolean active) {
        if (findByCode(code).isPresent()) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.ProfessionalStatusType.code.already.exists", code));
        }

        ProfessionalStatusType type = new ProfessionalStatusType();
        type.setCode(code);
        type.setName(name);
        type.setActive(active);
        return type;
    }

    public void delete() {
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static Optional<ProfessionalStatusType> findByCode(String code) {
        return findAll().filter(t -> code.equals(t.getCode())).findFirst();
    }

    public static Stream<ProfessionalStatusType> findAll() {
        return Bennu.getInstance().getProfessionalStatusTypesSet().stream();
    }

    public static Stream<ProfessionalStatusType> findActive() {
        return Bennu.getInstance().getProfessionalStatusTypesSet().stream().filter(ProfessionalStatusType::getActive);
    }
}
