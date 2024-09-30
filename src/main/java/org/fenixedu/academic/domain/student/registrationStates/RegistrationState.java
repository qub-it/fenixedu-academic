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
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.EnrolmentAction;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class RegistrationState extends RegistrationState_Base {

    static {
        getRelationRegistrationStateRegistration().addListener(new RelationAdapter<RegistrationState, Registration>() {

            @Override
            public void afterAdd(RegistrationState state, Registration registration) {
                super.afterAdd(state, registration);

                if (registration != null && state != null) {
                    new RegistrationStateLog(state, EnrolmentAction.ENROL, AccessControl.getPerson());
                }
            }

            @Override
            public void beforeRemove(RegistrationState state, Registration registration) {
                super.beforeRemove(state, registration);

                if (registration != null && state != null) {
                    new RegistrationStateLog(state, EnrolmentAction.UNENROL, AccessControl.getPerson());
                }
            }

        });
    }

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

    public RegistrationState() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    private static RegistrationState createState(Registration registration, Person person, DateTime dateTime,
            RegistrationStateType stateType) {
        final RegistrationState newState = new RegistrationState();
        newState.init(registration, person, dateTime);
        newState.setType(stateType);

        return newState;
    }

    protected void init(Registration registration, Person responsiblePerson, DateTime stateDate) {
        setStateDate(stateDate != null ? stateDate : new DateTime());
        setRegistration(registration);
        setResponsiblePerson(responsiblePerson != null ? responsiblePerson : AccessControl.getPerson());
    }

    protected void init(Registration registration) {
        init(registration, null, null);
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionInterval().getExecutionYear();
    }

    public void delete() {
        deleteWithoutCheckRules();
    }

    public void deleteWithoutCheckRules() {
        final Registration registration = getRegistration();
        try {

            org.fenixedu.academic.domain.student.RegistrationStateLog.createRegistrationStateLog(getRegistration(),
                    Bundle.MESSAGING, "log.registration.registrationstate.removed", Optional.ofNullable(getType())
                            .map(RegistrationStateType::getName).map(LocalizedString::getContent).orElse("-"),
                    getRemarks());
            setExecutionInterval(null);
            setRegistration(null);
            setResponsiblePerson(null);
            setType(null);
            setRootDomainObject(null);
            super.deleteDomainObject();
        } finally {
            registration.getStudent().updateStudentRole();
        }
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

    public static RegistrationState createRegistrationState(Registration registration, Person responsible, DateTime creation,
            RegistrationStateType stateType, ExecutionInterval executionInterval) {
        RegistrationState createdState = RegistrationState.createState(registration, responsible, creation, stateType);
        createdState.setExecutionInterval(executionInterval);
        registration.getStudent().updateStudentRole();
        return createdState;
    }

    public boolean isActive() {
        return getType().getActive();
    }

}
