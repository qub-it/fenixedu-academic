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

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

public class StudentCandidacy extends StudentCandidacy_Base {

    protected StudentCandidacy() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setStartDate(new YearMonthDay());
    }

    public StudentCandidacy(final Person person) {
        this();
        init(person);
    }

    @Deprecated
    public StudentCandidacy(final Person person, final ExecutionDegree executionDegree) {
        this();
        init(person);
        setExecutionDegree(executionDegree);
    }

    protected void init(Person person) {
        String[] args1 = {};
        if (person == null) {
            throw new DomainException("person cannot be null", args1);
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
     * @deprecated use {@link Registration#getCompletedDegreeInformation()}
     */
    @Deprecated
    @Override
    public PrecedentDegreeInformation getCompletedDegreeInformation() {
        return super.getCompletedDegreeInformation();
    }

    /**
     * @deprecated use {@link Registration#setCompletedDegreeInformation(PrecedentDegreeInformation)}
     */
    @Deprecated
    @Override
    public void setCompletedDegreeInformation(final PrecedentDegreeInformation completedDegreeInformation) {
        super.setCompletedDegreeInformation(completedDegreeInformation);
    }

    /**
     * @deprecated use {@link Registration#getPreviousDegreeInformation()}
     */
    @Deprecated
    @Override
    public PrecedentDegreeInformation getPreviousDegreeInformation() {
        return super.getPreviousDegreeInformation();
    }

    /**
     * @deprecated use {@link Registration#setPreviousDegreeInformation(PrecedentDegreeInformation)}
     */
    @Deprecated
    @Override
    public void setPreviousDegreeInformation(final PrecedentDegreeInformation previousDegreeInformation) {
        super.setPreviousDegreeInformation(previousDegreeInformation);
    }

    /**
     * @deprecated use {@link Registration#getFirstStudentCurricularPlan().getExecutionDegree()}
     */
    @Deprecated
    @Override
    public ExecutionDegree getExecutionDegree() {
        return super.getExecutionDegree();
    }

    @Deprecated
    @Override
    public void setExecutionDegree(final ExecutionDegree executionDegree) {
        super.setExecutionDegree(executionDegree);
    }

    /**
     * @deprecated use {@link Registration#getEntryGrade()}
     */
    @Deprecated
    @Override
    public Double getEntryGrade() {
        return super.getEntryGrade();
    }

    /**
     * @deprecated use {@link Registration#setEntryGrade(Double)}
     */
    @Deprecated
    @Override
    public void setEntryGrade(final Double entryGrade) {
        super.setEntryGrade(entryGrade);
    }

    /**
     * @deprecated use {@link Registration#getPlacingOption()}
     */
    @Deprecated
    @Override
    public Integer getPlacingOption() {
        return super.getPlacingOption();
    }

    /**
     * @deprecated use {@link Registration#setPlacingOption(Integer)}
     */
    @Deprecated
    @Override
    public void setPlacingOption(final Integer placingOption) {
        super.setPlacingOption(placingOption);
    }
}
