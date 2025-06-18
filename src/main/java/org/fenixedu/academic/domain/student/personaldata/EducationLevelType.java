package org.fenixedu.academic.domain.student.personaldata;

import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class EducationLevelType extends EducationLevelType_Base {
    protected EducationLevelType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static EducationLevelType create(String code, LocalizedString name, boolean active)
    {
        EducationLevelType educationLevelType = new EducationLevelType();
        educationLevelType.setCode(code);
        educationLevelType.setName(name);
        educationLevelType.setActive(active);

        return educationLevelType;
    }

    public void delete() {
        getDegreeClassificationsSet().clear();
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    public void setCode(String code) {
        if (isDuplicateCode(code)) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.EducationLevelType.code.already.exists", code));
        }

        super.setCode(code);
    }

    private boolean isDuplicateCode(String code) {
        return findByCode(code).filter(t -> t != this).isPresent();
    }

    public static Optional<EducationLevelType> findByCode(String code) {
        return findAll().filter(t -> code != null && code.equals(t.getCode())).findFirst();
    }

    public static Stream<EducationLevelType> findAll() {
        return Bennu.getInstance().getEducationLevelTypesSet().stream();
    }

    public static Stream<EducationLevelType> findActive() {
        return Bennu.getInstance().getEducationLevelTypesSet().stream().filter(EducationLevelType::getActive);
    }
}
