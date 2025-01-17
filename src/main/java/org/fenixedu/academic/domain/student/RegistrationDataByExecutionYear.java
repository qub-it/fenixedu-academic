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

import java.util.Collection;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.LocalDate;

/**
 * 
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
public class RegistrationDataByExecutionYear extends RegistrationDataByExecutionYear_Base {

    private RegistrationDataByExecutionYear(Registration registration, ExecutionYear executionYear) {
        setExecutionYear(executionYear);
        setRegistration(registration);
        setRootDomainObject(Bennu.getInstance());
        checkRules();
    }

    public void createReingression(LocalDate reingressionDate) {
        setReingression(true);
        setReingressionDate(reingressionDate);

        checkRules();
    }

    public void deleteReingression() {
        setReingression(false);
        setReingressionDate(null);

        checkRules();
    }

    protected void checkRules() {
        Optional<RegistrationDataByExecutionYear> result = getRegistration().getRegistrationDataByExecutionYearSet().stream()
                .filter(registrationDataByExecutionYear -> registrationDataByExecutionYear
                        .getExecutionYear() == getExecutionYear() && registrationDataByExecutionYear != this)
                .findAny();
        if (result.isPresent()) {
            throw new DomainException("error.RegistrationDataByExecutionYear.executionYearShouldBeUnique");
        }

        if (isReingression()) {
            LocalDate reingressionDate = getReingressionDate();
            if (reingressionDate == null) {
                throw new DomainException("error.RegistrationDataByExecutionYear.reingressionDate.required");
            }

        }
    }

    public boolean isReingression() {
        return getReingression();
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        setExecutionYear(null);
        setRegistration(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (isReingression()) {
            blockers.add(BundleUtil.getString(Bundle.ACADEMIC,
                    "error.RegistrationDataByExecutionYear.impossible.delete.found.Reingression",
                    getExecutionYear().getQualifiedName()));
        }
    }

    public static RegistrationDataByExecutionYear getOrCreateRegistrationDataByYear(final Registration registration,
            final ExecutionYear executionYear) {
        final Optional<RegistrationDataByExecutionYear> result = registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(registrationDataByExecutionYear -> registrationDataByExecutionYear.getExecutionYear() == executionYear)
                .findAny();

        return result.isPresent() ? result.get() : new RegistrationDataByExecutionYear(registration, executionYear);
    }

    public void edit(LocalDate enrolmentDate) {
        setEnrolmentDate(enrolmentDate);
        setActive(enrolmentDate != null);
        checkRules();
    }

    //methods to provide public visibility over protected slots

    @Override
    public LocalDate getEnrolmentDate() {
        return super.getEnrolmentDate();
    }

    @Override
    public ExecutionYear getExecutionYear() {
        return super.getExecutionYear();
    }

    @Override
    public Registration getRegistration() {
        return super.getRegistration();
    }
}
