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
package org.fenixedu.academic.domain;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class Job extends Job_Base {

    static final public Comparator<Job> COMPARATOR_BY_BEGIN_DATE =
            Comparator.comparing(Job::getBeginDate, Comparator.nullsFirst(Comparator.naturalOrder()));

    private Job() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setLastModifiedDate(new DateTime());
    }

    public static Job create(Person person, String employerName, String city, Country country, String position,
            LocalDate beginDate, LocalDate endDate, JobApplicationType applicationType, ContractType contractType,
            Double salary) {
        final Job job = new Job();
        checkParameters(person, employerName, city, country, position, beginDate, endDate, applicationType, contractType, salary);
        checkValidDates(beginDate, endDate);

        job.setPerson(person);
        job.setEmployerName(employerName);
        job.setCity(city);
        job.setCountry(country);
        job.setPosition(position);
        job.setBeginDate(beginDate);
        job.setEndDate(endDate);
        job.setJobApplicationType(applicationType);
        job.setContractType(contractType);
        job.setSalary(salary);

        return job;
    }

    private static void checkParameters(Person person, String employerName, String city, Country country, String position,
            LocalDate beginDate, LocalDate endDate, JobApplicationType applicationType, ContractType contractType,
            Double salary) {
        if (person == null) {
            throw new DomainException("job.creation.person.null");
        }

        if (StringUtils.isEmpty(employerName) && StringUtils.isEmpty(city) && country == null && StringUtils.isEmpty(position)
                && beginDate == null && endDate == null && applicationType == null && contractType == null && salary == null) {
            throw new DomainException("job.creation.allFields.null");
        }
    }

    private static void checkValidDates(LocalDate beginDate, LocalDate endDate) {
        if (beginDate != null && endDate != null) {
            if (beginDate.isAfter(endDate)) {
                throw new DomainException("job.creation.beginDate.after.endDate");
            }
        }
    }

    public void delete() {
        setPerson(null);
        setCreator(null);
        setCountry(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

}
