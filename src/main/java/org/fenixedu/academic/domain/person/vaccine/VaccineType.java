package org.fenixedu.academic.domain.person.vaccine;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class VaccineType extends VaccineType_Base {

    public VaccineType(LocalizedString name, String code) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setName(name);
        setCode(code);
    }

    @Override
    public void setCode(String code) {
        if (!findByCode(code).isEmpty()) {
            throw new DomainException("error.VaccineType.code.cannotBeDuplicated");
        }
        super.setCode(code);
    }

    public static Set<VaccineType> findAll() {
        return Bennu.getInstance().getVaccineTypesSet();
    }

    public static Optional<VaccineType> findByCode(String code) {
        return findAll().stream().filter(vT -> Objects.equals(vT.getCode(), code)).findAny();
    }

    public void delete() {
        setName(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }
}
