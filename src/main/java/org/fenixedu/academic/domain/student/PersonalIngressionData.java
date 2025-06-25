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

import java.util.Arrays;
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
        if (professionType == null) {
            super.setProfessionCategoryType(null);
        } else {
            ProfessionCategoryType.findByCode(professionType.getName()).ifPresent(this::setProfessionCategoryType);
        }

        super.setProfessionType(professionType);
    }

    @Override
    public void setProfessionCategoryType(ProfessionCategoryType professionCategoryType) {
        if (professionCategoryType == null) {
            super.setProfessionType(null);
        } else {
            Arrays.stream(ProfessionType.values())
                    .filter(professionType -> professionType.getName().equals(professionCategoryType.getCode())).findFirst()
                    .ifPresent(super::setProfessionType);
        }

        super.setProfessionCategoryType(professionCategoryType);
    }

    @Override
    public void setMotherProfessionType(ProfessionType motherProfessionType) {
        if (motherProfessionType == null) {
            super.setMotherProfessionCategoryType(null);
        } else {
            ProfessionCategoryType.findByCode(motherProfessionType.getName()).ifPresent(this::setMotherProfessionCategoryType);
        }

        super.setMotherProfessionType(motherProfessionType);
    }

    @Override
    public void setMotherProfessionCategoryType(ProfessionCategoryType motherProfessionCategoryType) {
        if (motherProfessionCategoryType == null) {
            super.setMotherProfessionType(null);
        } else {
            Arrays.stream(ProfessionType.values())
                    .filter(professionType -> professionType.getName().equals(motherProfessionCategoryType.getCode())).findFirst()
                    .ifPresent(super::setMotherProfessionType);
        }

        super.setMotherProfessionCategoryType(motherProfessionCategoryType);
    }

    @Override
    public void setFatherProfessionType(ProfessionType fatherProfessionType) {
        if (fatherProfessionType == null) {
            super.setFatherProfessionCategoryType(null);
        } else {
            ProfessionCategoryType.findByCode(fatherProfessionType.getName()).ifPresent(this::setFatherProfessionCategoryType);
        }

        super.setFatherProfessionType(fatherProfessionType);
    }

    @Override
    public void setFatherProfessionCategoryType(ProfessionCategoryType fatherProfessionCategoryType) {
        if (fatherProfessionCategoryType == null) {
            super.setFatherProfessionType(null);
        } else {
            Arrays.stream(ProfessionType.values())
                    .filter(professionType -> professionType.getName().equals(fatherProfessionCategoryType.getCode())).findFirst()
                    .ifPresent(super::setFatherProfessionType);
        }

        super.setFatherProfessionCategoryType(fatherProfessionCategoryType);
    }

    @Override
    public void setSpouseProfessionType(ProfessionType spouseProfessionType) {
        if (spouseProfessionType == null) {
            super.setSpouseProfessionCategoryType(null);
        } else {
            ProfessionCategoryType.findByCode(spouseProfessionType.getName()).ifPresent(this::setSpouseProfessionCategoryType);
        }

        super.setSpouseProfessionType(spouseProfessionType);
    }

    @Override
    public void setSpouseProfessionCategoryType(ProfessionCategoryType spouseProfessionCategoryType) {
        if (spouseProfessionCategoryType == null) {
            super.setSpouseProfessionType(null);
        } else {
            Arrays.stream(ProfessionType.values())
                    .filter(professionType -> professionType.getName().equals(spouseProfessionCategoryType.getCode())).findFirst()
                    .ifPresent(super::setSpouseProfessionType);
        }

        super.setSpouseProfessionCategoryType(spouseProfessionCategoryType);
    }

    @Override
    public void setProfessionalCondition(ProfessionalSituationConditionType professionalCondition) {
        if (professionalCondition == null) {
            super.setProfessionalStatusType(null);
        } else {
            ProfessionalStatusType.findByCode(professionalCondition.getName()).ifPresent(this::setProfessionalStatusType);
        }

        super.setProfessionalCondition(professionalCondition);
    }

    @Override
    public void setProfessionalStatusType(ProfessionalStatusType professionalStatusType) {
        if (professionalStatusType == null) {
            super.setProfessionalCondition(null);
        } else {
            Arrays.stream(ProfessionalSituationConditionType.values())
                    .filter(conditionType -> conditionType.getName().equals(professionalStatusType.getCode())).findFirst()
                    .ifPresent(super::setProfessionalCondition);
        }

        super.setProfessionalStatusType(professionalStatusType);
    }

    @Override
    public void setMotherProfessionalCondition(ProfessionalSituationConditionType motherProfessionalCondition) {
        if (motherProfessionalCondition == null) {
            super.setMotherProfessionalStatusType(null);
        } else {
            ProfessionalStatusType.findByCode(motherProfessionalCondition.getName())
                    .ifPresent(this::setMotherProfessionalStatusType);
        }

        super.setMotherProfessionalCondition(motherProfessionalCondition);
    }

    @Override
    public void setMotherProfessionalStatusType(ProfessionalStatusType motherProfessionalStatusType) {
        if (motherProfessionalStatusType == null) {
            super.setMotherProfessionalCondition(null);
        } else {
            Arrays.stream(ProfessionalSituationConditionType.values())
                    .filter(conditionType -> conditionType.getName().equals(motherProfessionalStatusType.getCode())).findFirst()
                    .ifPresent(super::setMotherProfessionalCondition);
        }

        super.setMotherProfessionalStatusType(motherProfessionalStatusType);
    }

    @Override
    public void setFatherProfessionalCondition(ProfessionalSituationConditionType fatherProfessionalCondition) {
        if (fatherProfessionalCondition == null) {
            super.setFatherProfessionalStatusType(null);
        } else {
            ProfessionalStatusType.findByCode(fatherProfessionalCondition.getName())
                    .ifPresent(this::setFatherProfessionalStatusType);
        }

        super.setFatherProfessionalCondition(fatherProfessionalCondition);
    }

    @Override
    public void setFatherProfessionalStatusType(ProfessionalStatusType fatherProfessionalStatusType) {
        if (fatherProfessionalStatusType == null) {
            super.setFatherProfessionalCondition(null);
        } else {
            Arrays.stream(ProfessionalSituationConditionType.values())
                    .filter(conditionType -> conditionType.getName().equals(fatherProfessionalStatusType.getCode())).findFirst()
                    .ifPresent(super::setFatherProfessionalCondition);
        }

        super.setFatherProfessionalStatusType(fatherProfessionalStatusType);
    }

    @Override
    public void setSpouseProfessionalCondition(ProfessionalSituationConditionType spouseProfessionalCondition) {
        if (spouseProfessionalCondition == null) {
            super.setSpouseProfessionalStatusType(null);
        } else {
            ProfessionalStatusType.findByCode(spouseProfessionalCondition.getName())
                    .ifPresent(this::setSpouseProfessionalStatusType);
        }

        super.setSpouseProfessionalCondition(spouseProfessionalCondition);
    }

    @Override
    public void setSpouseProfessionalStatusType(ProfessionalStatusType spouseProfessionalStatusType) {
        if (spouseProfessionalStatusType == null) {
            super.setSpouseProfessionalCondition(null);
        } else {
            Arrays.stream(ProfessionalSituationConditionType.values())
                    .filter(conditionType -> conditionType.getName().equals(spouseProfessionalStatusType.getCode())).findFirst()
                    .ifPresent(super::setSpouseProfessionalCondition);
        }

        super.setSpouseProfessionalStatusType(spouseProfessionalStatusType);
    }

    @Override
    public void setMotherSchoolLevel(SchoolLevelType motherSchoolLevel) {
        if (motherSchoolLevel == null) {
            super.setMotherEducationLevelType(null);
        } else {
            EducationLevelType.findByCode(motherSchoolLevel.getName()).ifPresent(this::setMotherEducationLevelType);
        }

        super.setMotherSchoolLevel(motherSchoolLevel);
    }

    @Override
    public void setMotherEducationLevelType(EducationLevelType motherEducationLevelType) {
        if (motherEducationLevelType == null) {
            super.setMotherSchoolLevel(null);
        } else {
            Arrays.stream(SchoolLevelType.values())
                    .filter(schoolLevelType -> schoolLevelType.getName().equals(motherEducationLevelType.getCode())).findFirst()
                    .ifPresent(super::setMotherSchoolLevel);
        }

        super.setMotherEducationLevelType(motherEducationLevelType);
    }

    @Override
    public void setFatherSchoolLevel(SchoolLevelType fatherSchoolLevel) {
        if (fatherSchoolLevel == null) {
            super.setFatherEducationLevelType(null);
        } else {
            EducationLevelType.findByCode(fatherSchoolLevel.getName()).ifPresent(this::setFatherEducationLevelType);
        }

        super.setFatherSchoolLevel(fatherSchoolLevel);
    }

    @Override
    public void setFatherEducationLevelType(EducationLevelType fatherEducationLevelType) {
        if (fatherEducationLevelType == null) {
            super.setFatherSchoolLevel(null);
        } else {
            Arrays.stream(SchoolLevelType.values())
                    .filter(schoolLevelType -> schoolLevelType.getName().equals(fatherEducationLevelType.getCode())).findFirst()
                    .ifPresent(super::setFatherSchoolLevel);
        }

        super.setFatherEducationLevelType(fatherEducationLevelType);
    }

    @Override
    public void setSpouseSchoolLevel(SchoolLevelType spouseSchoolLevel) {
        if (spouseSchoolLevel == null) {
            super.setSpouseEducationLevelType(null);
        } else {
            EducationLevelType.findByCode(spouseSchoolLevel.getName()).ifPresent(this::setSpouseEducationLevelType);
        }

        super.setSpouseSchoolLevel(spouseSchoolLevel);
    }

    @Override
    public void setSpouseEducationLevelType(EducationLevelType spouseEducationLevelType) {
        if (spouseEducationLevelType == null) {
            super.setSpouseSchoolLevel(null);
        } else {
            Arrays.stream(SchoolLevelType.values())
                    .filter(schoolLevelType -> schoolLevelType.getName().equals(spouseEducationLevelType.getCode())).findFirst()
                    .ifPresent(super::setSpouseSchoolLevel);
        }

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

}
