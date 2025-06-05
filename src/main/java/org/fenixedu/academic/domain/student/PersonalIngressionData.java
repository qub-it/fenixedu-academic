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
import org.fenixedu.academic.domain.candidacy.PersonalInformationBean;
import org.fenixedu.academic.domain.exceptions.DomainException;
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

    public void edit(final PersonalInformationBean bean) {
        setCountryOfResidence(bean.getCountryOfResidence());
        setDistrictSubdivisionOfResidence(bean.getDistrictSubdivisionOfResidence());
        setDislocatedFromPermanentResidence(bean.getDislocatedFromPermanentResidence());
        setSchoolTimeDistrictSubDivisionOfResidence(bean.getSchoolTimeDistrictSubdivisionOfResidence());
        setGrantOwnerType(bean.getGrantOwnerType());
        setGrantOwnerProvider(bean.getGrantOwnerProvider());
        setHighSchoolType(bean.getHighSchoolType());
        setMaritalStatus(bean.getMaritalStatus());
        setProfessionType(bean.getProfessionType());
        setProfessionalCondition(bean.getProfessionalCondition());
        setMotherSchoolLevel(bean.getMotherSchoolLevel());
        setMotherProfessionType(bean.getMotherProfessionType());
        setMotherProfessionalCondition(bean.getMotherProfessionalCondition());
        setFatherSchoolLevel(bean.getFatherSchoolLevel());
        setFatherProfessionType(bean.getFatherProfessionType());
        setFatherProfessionalCondition(bean.getFatherProfessionalCondition());
        setLastModifiedDate(new DateTime());
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
