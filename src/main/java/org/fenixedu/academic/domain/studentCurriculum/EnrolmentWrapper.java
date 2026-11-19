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
package org.fenixedu.academic.domain.studentCurriculum;

import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

abstract public class EnrolmentWrapper extends EnrolmentWrapper_Base {

    protected EnrolmentWrapper() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    protected void init(final Credits credits) {
        if (credits == null) {
            throw new DomainException("error.EnrolmentWrapper.credits.cannot.be.null");
        }
        super.setCredits(credits);
    }

    @Override
    public void setCredits(Credits credits) {
        throw new DomainException("error.EnrolmentWrapper.cannot.modify.credits");
    }

    public void delete() {
        super.setCredits(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    abstract public IEnrolment getIEnrolment();

}
