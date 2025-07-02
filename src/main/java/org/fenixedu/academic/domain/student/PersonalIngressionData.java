/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.student;

import java.util.Comparator;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.ProfessionType;
import org.fenixedu.academic.domain.ProfessionalSituationConditionType;
import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.personaldata.EducationLevelType;
import org.fenixedu.academic.domain.student.personaldata.ProfessionCategoryType;
import org.fenixedu.academic.domain.student.personaldata.ProfessionalStatusType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

import jvstm.cps.ConsistencyPredicate;

public class PersonalIngressionData extends PersonalIngressionData_Base {

    public static Comparator<PersonalIngressionData> COMPARATOR_BY_EXECUTION_YEAR = new Comparator<PersonalIngressionData>() {
        @Override
        public int compare(PersonalIngressionData data1, PersonalIngressionData data2) {
            return data1.getExecutionYear().getYear().compareTo(data2.getExecutionYear().getYear());
        }
    };

    public PersonalIngressionData() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setLastModifiedDate(new DateTime());
    }

    public PersonalIngressionData(ExecutionYear executionYear) {
        this();
        setExecutionYear(executionYear);
    }

    public PersonalIngressionData(Student student, ExecutionYear executionYear) {
        this(executionYear);
        setStudent(student);
    }

    @Override
    public void setExecutionYear(ExecutionYear executionYear) {
        super.setExecutionYear(executionYear);

        if (executionYear != null && getStudent() != null && studentHasRepeatedPID(getStudent(), executionYear)) {
            throw new DomainException("A Student cannot have two PersonalIngressionData objects for the same ExecutionYear.");
        }
    }

    @Override
    public void setStudent(Student student) {
        super.setStudent(student);

        if (student != null && getExecutionYear() != null && studentHasRepeatedPID(student, getExecutionYear())) {
            throw new DomainException("A Student cannot have two PersonalIngressionData objects for the same ExecutionYear.");
        }
    }

    @Override
    public void setProfessionType(ProfessionType professionType) {
        super.setProfessionCategoryType(findProfessionCategoryType(professionType));
        super.setProfessionType(professionType);
    }

    @Override
    public void setProfessionCategoryType(ProfessionCategoryType professionCategoryType) {
        super.setProfessionType(findProfessionType(professionCategoryType));
        super.setProfessionCategoryType(professionCategoryType);
    }

    @Override
    public void setMotherProfessionType(ProfessionType motherProfessionType) {
        super.setMotherProfessionCategoryType(findProfessionCategoryType(motherProfessionType));
        super.setMotherProfessionType(motherProfessionType);
    }

    @Override
    public void setMotherProfessionCategoryType(ProfessionCategoryType motherProfessionCategoryType) {
        super.setMotherProfessionType(findProfessionType(motherProfessionCategoryType));
        super.setMotherProfessionCategoryType(motherProfessionCategoryType);
    }

    @Override
    public void setFatherProfessionType(ProfessionType fatherProfessionType) {
        super.setFatherProfessionCategoryType(findProfessionCategoryType(fatherProfessionType));
        super.setFatherProfessionType(fatherProfessionType);
    }

    @Override
    public void setFatherProfessionCategoryType(ProfessionCategoryType fatherProfessionCategoryType) {
        super.setFatherProfessionType(findProfessionType(fatherProfessionCategoryType));
        super.setFatherProfessionCategoryType(fatherProfessionCategoryType);
    }

    @Override
    public void setSpouseProfessionType(ProfessionType spouseProfessionType) {
        super.setSpouseProfessionCategoryType(findProfessionCategoryType(spouseProfessionType));
        super.setSpouseProfessionType(spouseProfessionType);
    }

    @Override
    public void setSpouseProfessionCategoryType(ProfessionCategoryType spouseProfessionCategoryType) {
        super.setSpouseProfessionType(findProfessionType(spouseProfessionCategoryType));
        super.setSpouseProfessionCategoryType(spouseProfessionCategoryType);
    }

    @Override
    public void setProfessionalCondition(ProfessionalSituationConditionType professionalCondition) {
        super.setProfessionalStatusType(findProfessionalStatusType(professionalCondition));
        super.setProfessionalCondition(professionalCondition);
    }

    @Override
    public void setProfessionalStatusType(ProfessionalStatusType professionalStatusType) {
        super.setProfessionalCondition(findProfessionalCondition(professionalStatusType));
        super.setProfessionalStatusType(professionalStatusType);
    }

    @Override
    public void setMotherProfessionalCondition(ProfessionalSituationConditionType motherProfessionalCondition) {
        super.setMotherProfessionalStatusType(findProfessionalStatusType(motherProfessionalCondition));
        super.setMotherProfessionalCondition(motherProfessionalCondition);
    }

    @Override
    public void setMotherProfessionalStatusType(ProfessionalStatusType motherProfessionalStatusType) {
        super.setMotherProfessionalCondition(findProfessionalCondition(motherProfessionalStatusType));
        super.setMotherProfessionalStatusType(motherProfessionalStatusType);
    }

    @Override
    public void setFatherProfessionalCondition(ProfessionalSituationConditionType fatherProfessionalCondition) {
        super.setFatherProfessionalStatusType(findProfessionalStatusType(fatherProfessionalCondition));
        super.setFatherProfessionalCondition(fatherProfessionalCondition);
    }

    @Override
    public void setFatherProfessionalStatusType(ProfessionalStatusType fatherProfessionalStatusType) {
        super.setFatherProfessionalCondition(findProfessionalCondition(fatherProfessionalStatusType));
        super.setFatherProfessionalStatusType(fatherProfessionalStatusType);
    }

    @Override
    public void setSpouseProfessionalCondition(ProfessionalSituationConditionType spouseProfessionalCondition) {
        super.setSpouseProfessionalStatusType(findProfessionalStatusType(spouseProfessionalCondition));
        super.setSpouseProfessionalCondition(spouseProfessionalCondition);
    }

    @Override
    public void setSpouseProfessionalStatusType(ProfessionalStatusType spouseProfessionalStatusType) {
        super.setSpouseProfessionalCondition(findProfessionalCondition(spouseProfessionalStatusType));
        super.setSpouseProfessionalStatusType(spouseProfessionalStatusType);
    }

    @Override
    public void setMotherSchoolLevel(SchoolLevelType motherSchoolLevel) {
        super.setMotherEducationLevelType(findEducationLevelType(motherSchoolLevel));
        super.setMotherSchoolLevel(motherSchoolLevel);
    }

    @Override
    public void setMotherEducationLevelType(EducationLevelType motherEducationLevelType) {
        super.setMotherSchoolLevel(findSchoolLevel(motherEducationLevelType));
        super.setMotherEducationLevelType(motherEducationLevelType);
    }

    @Override
    public void setFatherSchoolLevel(SchoolLevelType fatherSchoolLevel) {
        super.setFatherEducationLevelType(findEducationLevelType(fatherSchoolLevel));
        super.setFatherSchoolLevel(fatherSchoolLevel);
    }

    @Override
    public void setFatherEducationLevelType(EducationLevelType fatherEducationLevelType) {
        super.setFatherSchoolLevel(findSchoolLevel(fatherEducationLevelType));
        super.setFatherEducationLevelType(fatherEducationLevelType);
    }

    @Override
    public void setSpouseSchoolLevel(SchoolLevelType spouseSchoolLevel) {
        super.setSpouseEducationLevelType(findEducationLevelType(spouseSchoolLevel));
        super.setSpouseSchoolLevel(spouseSchoolLevel);
    }

    @Override
    public void setSpouseEducationLevelType(EducationLevelType spouseEducationLevelType) {
        super.setSpouseSchoolLevel(findSchoolLevel(spouseEducationLevelType));
        super.setSpouseEducationLevelType(spouseEducationLevelType);
    }

    private static boolean studentHasRepeatedPID(Student student, ExecutionYear executionYear) {
        PersonalIngressionData existingPid = null;
        for (PersonalIngressionData pid : student.getPersonalIngressionsDataSet()) {
            if (pid.getExecutionYear().equals(executionYear)) {
                if (existingPid == null) {
                    existingPid = pid;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public void delete() {
        setStudent(null);
        setExecutionYear(null);
        setRootDomainObject(null);
        setCountryOfResidence(null);
        setGrantOwnerProvider(null);
        setDistrictSubdivisionOfResidence(null);
        setSchoolTimeDistrictSubDivisionOfResidence(null);
        setProfessionCategoryType(null);
        setMotherProfessionCategoryType(null);
        setFatherProfessionCategoryType(null);
        setSpouseProfessionCategoryType(null);
        setProfessionalStatusType(null);
        setMotherProfessionalStatusType(null);
        setFatherProfessionalStatusType(null);
        setSpouseProfessionalStatusType(null);
        setMotherEducationLevelType(null);
        setFatherEducationLevelType(null);
        setSpouseEducationLevelType(null);
        deleteDomainObject();
    }

    @ConsistencyPredicate
    public boolean checkHasExecutionYear() {
        return getExecutionYear() != null;
    }

    @ConsistencyPredicate
    public boolean checkHasStudent() {
        return getStudent() != null;
    }

    private ProfessionType findProfessionType(ProfessionCategoryType professionCategoryType) {
        if (professionCategoryType == null) {
            return null;
        }
        return ProfessionType.findByCode(professionCategoryType.getCode()).orElseThrow(
                () -> new DomainException("error.ProfessionCategoryType.not.found", professionCategoryType.getCode()));
    }

    private ProfessionCategoryType findProfessionCategoryType(ProfessionType professionType) {
        if (professionType == null) {
            return null;
        }
        return ProfessionCategoryType.findByCode(professionType.getName())
                .orElseThrow(() -> new DomainException("error.ProfessionType.not.found", professionType.getName()));
    }

    private ProfessionalSituationConditionType findProfessionalCondition(ProfessionalStatusType professionalStatusType) {
        if (professionalStatusType == null) {
            return null;
        }
        return ProfessionalSituationConditionType.findByCode(professionalStatusType.getCode()).orElseThrow(
                () -> new DomainException("error.ProfessionalStatusType.not.found", professionalStatusType.getCode()));
    }

    private ProfessionalStatusType findProfessionalStatusType(ProfessionalSituationConditionType professionalCondition) {
        if (professionalCondition == null) {
            return null;
        }
        return ProfessionalStatusType.findByCode(professionalCondition.getName())
                .orElseThrow(() -> new DomainException("error.ProfessionalCondition.not.found", professionalCondition.getName()));
    }

    private SchoolLevelType findSchoolLevel(EducationLevelType educationLevelType) {
        if (educationLevelType == null) {
            return null;
        }
        return SchoolLevelType.findByCode(educationLevelType.getCode())
                .orElseThrow(() -> new DomainException("error.EducationLevelType.not.found", educationLevelType.getCode()));
    }

    private EducationLevelType findEducationLevelType(SchoolLevelType schoolLevel) {
        if (schoolLevel == null) {
            return null;
        }
        return EducationLevelType.findByCode(schoolLevel.getName())
                .orElseThrow(() -> new DomainException("error.SchoolLevelType.not.found", schoolLevel.getName()));
    }
}
