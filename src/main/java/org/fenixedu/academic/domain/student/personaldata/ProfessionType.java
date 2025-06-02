package org.fenixedu.academic.domain.student.personaldata;

import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProfessionType extends ProfessionType_Base {

    protected ProfessionType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static ProfessionType create(String code, LocalizedString name, boolean active) {
        if (findByCode(code).isPresent()) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,"error.code.already.exists", code));
        }

        ProfessionType professionType = new ProfessionType();
        professionType.setCode(code);
        professionType.setName(name);
        professionType.setActive(active);
        return professionType;
    }

    public void delete() {
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static Optional<ProfessionType> findByCode(String code) {
        return findAll().filter(t -> code.equals(t.getCode())).findFirst();
    }

    public static Stream<ProfessionType> findAll() {
        return Bennu.getInstance().getProfessionTypesSet().stream();
    }

    public static Stream<ProfessionType> findActive() {
        return Bennu.getInstance().getProfessionTypesSet().stream().filter(ProfessionType::getActive);
    }
}
