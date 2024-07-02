package org.fenixedu.academic.domain.person.vaccine;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
        if (Objects.equals(this.getCode(), code)) {
            return;
        }
        if (!findByCode(code).isEmpty()) {
            throw new DomainException("error.VaccineType.code.cannotBeDuplicated");
        }
        super.setCode(code);
    }

    public static Stream<VaccineType> findAll() {
        return Bennu.getInstance().getVaccineTypesSet().stream();
    }

    public static Optional<VaccineType> findByCode(String code) {
        return findAll().filter(vT -> Objects.equals(vT.getCode(), code)).findAny();
    }

    public void delete() {
        setName(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }
}
