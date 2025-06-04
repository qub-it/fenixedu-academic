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
package org.fenixedu.academic.domain.candidacy;

import java.util.Optional;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

public class StudentCandidacy extends StudentCandidacy_Base {

    protected StudentCandidacy() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setStartDate(new YearMonthDay());
    }

    @Deprecated
    public StudentCandidacy(final Person person, final ExecutionDegree executionDegree) {
        this();
        init(person);
        setExecutionDegree(executionDegree);
    }

    public StudentCandidacy(final Person person) {
        this();
        init(person);
    }

    protected void init(Person person) {
        if (person == null) {
            throw new DomainException("person cannot be null");
        }

        setPerson(person);
    }

    /**
     * @deprecated use {@link #getState()}
     */
    @Deprecated
    public CandidacySituationType getActiveCandidacySituationType() {
        return getState();
    }

    public boolean isActive() {
        final CandidacySituationType situationType = getState();
        return situationType != null && situationType.isActive();
    }

    public void delete() {
        setPerson(null);

        setRootDomainObject(null);
        setRegistration(null);
        setIngressionType(null);
        setExecutionDegree(null);

        Optional.ofNullable(getCompletedDegreeInformation()).ifPresent(pdi -> pdi.delete());
        Optional.ofNullable(getPreviousDegreeInformation()).ifPresent(pdi -> pdi.delete());

        deleteDomainObject();
    }

    @Override
    public ExecutionDegree getExecutionDegree() {
        return getDegreeCurricularPlan().findExecutionDegree(getRegistration().getRegistrationYear()).orElse(null);
    }

    public DegreeCurricularPlan getDegreeCurricularPlan() {
        return getRegistration().getFirstStudentCurricularPlan().getDegreeCurricularPlan();
    }

    public ExecutionYear getExecutionYear() {
        return getRegistration().getRegistrationYear();
    }

    @Override
    public void setState(CandidacySituationType state) {
        super.setState(state);
        setStateDate(new DateTime());
    }

    @Override
    public Boolean getFirstTimeCandidacy() {
        return Boolean.TRUE.equals(super.getFirstTimeCandidacy());
    }

    @Override
    public void setAdmissionPhase(Integer admissionPhase) {
        if (admissionPhase != null && admissionPhase <= 0) {
            throw new DomainException("error.StudentCandidacy.admission.phase.has.to.be.positive.number");
        }
        super.setAdmissionPhase(admissionPhase);
    }

    /**
     * Please use Registration.getCompletedDegreeInformation()
     */
    @Deprecated
    @Override
    public PrecedentDegreeInformation getCompletedDegreeInformation() {
        return super.getCompletedDegreeInformation();
    }

    /**
     * Please use Registration.getPreviousDegreeInformation()
     */
    @Deprecated
    @Override
    public PrecedentDegreeInformation getPreviousDegreeInformation() {
        return super.getPreviousDegreeInformation();
    }

}
