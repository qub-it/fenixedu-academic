package org.fenixedu.academic.domain.student.personaldata;

import org.fenixedu.academic.util.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public class EducationalLevelType extends EducationalLevelType_Base {
    protected EducationalLevelType() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static EducationalLevelType create(String code, LocalizedString name, boolean active, boolean isForStudent,
            boolean isForStudentHousehold, boolean isForMobilityStudent, boolean isOther, boolean isPhDDegree,
            boolean isSchoolLevelBasicCycle, boolean isHighSchoolOrEquivalent, boolean isHigherEducation)
    {
        if (findByCode(code).isPresent()) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,"error.code.already.exists", code));
        }

        EducationalLevelType educationalLevelType = new EducationalLevelType();

        educationalLevelType.setCode(code);
        educationalLevelType.setName(name);
        educationalLevelType.setActive(active);
        educationalLevelType.setForStudent(isForStudent);
        educationalLevelType.setForStudentHousehold(isForStudentHousehold);
        educationalLevelType.setForMobilityStudent(isForMobilityStudent);
        educationalLevelType.setOther(isOther);
        educationalLevelType.setPhDDegree(isPhDDegree);
        educationalLevelType.setSchoolLevelBasicCycle(isSchoolLevelBasicCycle);
        educationalLevelType.setHighSchoolOrEquivalent(isHighSchoolOrEquivalent);
        educationalLevelType.setHigherEducation(isHigherEducation);

        return educationalLevelType;
    }

    public void delete() {
        getDegreeClassificationsSet().clear();
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static Optional<EducationalLevelType> findByCode(String code) {
        return findAll().filter(t -> code.equals(t.getCode())).findFirst();
    }

    public static Stream<EducationalLevelType> findAll() {
        return Bennu.getInstance().getEducationalLevelTypesSet().stream();
    }

    public static Stream<EducationalLevelType> findActive() {
        return Bennu.getInstance().getEducationalLevelTypesSet().stream().filter(EducationalLevelType::getActive);
    }

    public static List<EducationalLevelType> getTypesForStudent() {
        return findAll().filter(EducationalLevelType::getForStudent).toList();
    }

    public static List<EducationalLevelType> getTypesForMobilityStudent() {
        return findAll().filter(EducationalLevelType::getForMobilityStudent).toList();
    }

    public static List<EducationalLevelType> getTypesForStudentHousehold() {
        return findAll().filter(EducationalLevelType::getForStudentHousehold).toList();
    }
}
