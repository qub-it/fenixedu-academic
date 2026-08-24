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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;

public abstract class Evaluation extends Evaluation_Base {

    public Evaluation() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public List<ExecutionCourse> getAttendingExecutionCoursesFor(final Registration registration) {
        final List<ExecutionCourse> result = getAssociatedExecutionCoursesSet().stream().filter(registration::attends).toList();
        return result.isEmpty() ? new ArrayList<>(getAssociatedExecutionCoursesSet()) : result;
    }

    public void delete() {
        getAssociatedExecutionCoursesSet().clear();
        while (!getMarksSet().isEmpty()) {
            getMarksSet().iterator().next().delete();
        }
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public Mark getMarkByAttend(Attends attends) {
        return getMarksSet().stream().filter(mark -> mark.getAttend().equals(attends)).findFirst().orElse(null);
    }

    public boolean isFinal() {
        return false;
    }

    public String getPresentationName() {
        return null;
    }

    protected void logCreate() {
        logAuxBasic("log.executionCourse.evaluation.generic.created");
    }

    protected void logEdit() {
        logAuxBasic("log.executionCourse.evaluation.generic.edited");
    }

    protected void logRemove() {
        logAuxBasic("log.executionCourse.evaluation.generic.removed");
    }

    private void logAuxBasic(String key) {
        getAssociatedExecutionCoursesSet().forEach(
                ec -> EvaluationManagementLog.createLog(ec, Bundle.MESSAGING, key, getPresentationName(), ec.getName(),
                        ec.getDegreePresentationString()));
    }

    public Date getEvaluationDate() {
        return null;
    }

}
