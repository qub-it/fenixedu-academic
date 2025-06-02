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

    public static EducationLevelType create(String code, LocalizedString name, boolean active, boolean isForStudent,
            boolean isForStudentHousehold, boolean isForMobilityStudent, boolean isOther, boolean isPhDDegree,
            boolean isSchoolLevelBasicCycle, boolean isHighSchoolOrEquivalent, boolean isHigherEducation)
    {
        if (findByCode(code).isPresent()) {
            throw new DomainException(
                    BundleUtil.getString(Bundle.APPLICATION, "error.EducationLevelType.code.already.exists", code));
        }

        EducationLevelType educationLevelType = new EducationLevelType();

        educationLevelType.setCode(code);
        educationLevelType.setName(name);
        educationLevelType.setActive(active);
        educationLevelType.setForStudent(isForStudent);
        educationLevelType.setForStudentHousehold(isForStudentHousehold);
        educationLevelType.setForMobilityStudent(isForMobilityStudent);
        educationLevelType.setOther(isOther);
        educationLevelType.setPhDDegree(isPhDDegree);
        educationLevelType.setSchoolLevelBasicCycle(isSchoolLevelBasicCycle);
        educationLevelType.setHighSchoolOrEquivalent(isHighSchoolOrEquivalent);
        educationLevelType.setHigherEducation(isHigherEducation);

        return educationLevelType;
    }

    public void delete() {
        getDegreeClassificationsSet().clear();
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public static Optional<EducationLevelType> findByCode(String code) {
        return findAll().filter(t -> code.equals(t.getCode())).findFirst();
    }

    public static Stream<EducationLevelType> findAll() {
        return Bennu.getInstance().getEducationLevelTypesSet().stream();
    }

    public static Stream<EducationLevelType> findActive() {
        return Bennu.getInstance().getEducationLevelTypesSet().stream().filter(EducationLevelType::getActive);
    }
}
