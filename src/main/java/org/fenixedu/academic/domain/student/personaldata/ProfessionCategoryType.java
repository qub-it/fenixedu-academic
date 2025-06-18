package org.fenixedu.academic.domain.student.personaldata;

import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class ProfessionCategoryType extends ProfessionCategoryType_Base {

    protected ProfessionCategoryType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static ProfessionCategoryType create(String code, LocalizedString name, boolean active) {
        ProfessionCategoryType professionCategoryType = new ProfessionCategoryType();
        professionCategoryType.setCode(code);
        professionCategoryType.setName(name);
        professionCategoryType.setActive(active);
        return professionCategoryType;
    }

    public void delete() {
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    public void setCode(String code) {
        if (isDuplicateCode(code)) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.ProfessionCategoryType.code.already.exists", code));
        }

        super.setCode(code);
    }

    private boolean isDuplicateCode(String code) {
        return findByCode(code).filter(t -> t != this).isPresent();
    }

    public static Optional<ProfessionCategoryType> findByCode(String code) {
        return findAll().filter(t -> code != null && code.equals(t.getCode())).findFirst();
    }

    public static Stream<ProfessionCategoryType> findAll() {
        return Bennu.getInstance().getProfessionCategoryTypesSet().stream();
    }

    public static Stream<ProfessionCategoryType> findActive() {
        return Bennu.getInstance().getProfessionCategoryTypesSet().stream().filter(ProfessionCategoryType::getActive);
    }
}
