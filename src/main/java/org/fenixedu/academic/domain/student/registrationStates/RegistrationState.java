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
package org.fenixedu.academic.domain.student.registrationStates;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

public class RegistrationState extends RegistrationState_Base {

    public static Comparator<RegistrationState> DATE_COMPARATOR = new Comparator<RegistrationState>() {
        @Override
        public int compare(RegistrationState leftState, RegistrationState rightState) {
            int comparationResult = leftState.getStateDate().compareTo(rightState.getStateDate());
            return (comparationResult == 0) ? leftState.getExternalId().compareTo(rightState.getExternalId()) : comparationResult;
        }
    };

    public static Comparator<RegistrationState> EXECUTION_INTERVAL_AND_DATE_COMPARATOR =
            Comparator.comparing(RegistrationState::getExecutionInterval, ExecutionInterval.COMPARATOR_BY_BEGIN_DATE)
                    .thenComparing(DATE_COMPARATOR);

    protected RegistrationState() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static RegistrationState createRegistrationState(Registration registration, Person responsible, DateTime creation,
            RegistrationStateType stateType, ExecutionInterval executionInterval) {

        final RegistrationState createdState = new RegistrationState();
        createdState.setRegistration(registration);
        createdState.setResponsiblePerson(responsible != null ? responsible : AccessControl.getPerson());
        createdState.setStateDate(creation != null ? creation : new DateTime());
        createdState.setType(stateType);
        createdState.setExecutionInterval(executionInterval);

        registration.getStudent().updateStudentRole();

        return createdState;
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionInterval().getExecutionYear();
    }

    public void delete() {
        setExecutionInterval(null);
        setRegistration(null);
        setResponsiblePerson(null);
        setType(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public RegistrationState getNext() {
        final List<RegistrationState> sortedStates = getRegistration().getRegistrationStatesSet().stream()
                .sorted(EXECUTION_INTERVAL_AND_DATE_COMPARATOR).collect(Collectors.toList());
        int indexOfThis = sortedStates.indexOf(this);
        return sortedStates.size() > indexOfThis + 1 ? sortedStates.get(indexOfThis + 1) : null;
    }

    public RegistrationState getPrevious() {
        final List<RegistrationState> sortedStates = getRegistration().getRegistrationStatesSet().stream()
                .sorted(EXECUTION_INTERVAL_AND_DATE_COMPARATOR).collect(Collectors.toList());
        int indexOfThis = sortedStates.indexOf(this);
        return indexOfThis > 0 ? sortedStates.get(indexOfThis - 1) : null;
    }

    public DateTime getEndDate() {
        RegistrationState state = getNext();
        return (state != null) ? state.getStateDate() : null;
    }

    public void setStateDate(YearMonthDay yearMonthDay) {
        super.setStateDate(yearMonthDay.toDateTimeAtMidnight());
    }

    public boolean isActive() {
        return getType().getActive();
    }

}
