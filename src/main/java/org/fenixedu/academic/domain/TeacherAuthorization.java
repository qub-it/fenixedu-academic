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

import java.util.Objects;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

public class TeacherAuthorization extends TeacherAuthorization_Base implements Comparable<TeacherAuthorization> {
    protected TeacherAuthorization() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setCreationDate(DateTime.now());
    }

    protected TeacherAuthorization(Teacher teacher, Unit unit, ExecutionInterval executionInterval,
            TeacherCategory teacherCategory, Boolean contracted, Double lessonHours, Double workPercentageInInstitution) {
        this();
        setTeacher(teacher);
        setUnit(unit);
        setExecutionSemester(executionInterval);
        setTeacherCategory(teacherCategory);
        setContracted(contracted);
        setLessonHours(lessonHours);
        setWorkPercentageInInstitution(workPercentageInInstitution);
        setAuthorizer(Authenticate.getUser());
    }

    public static TeacherAuthorization createOrUpdate(Teacher teacher, Unit unit, ExecutionInterval executionInterval,
            TeacherCategory teacherCategory, Boolean contracted, Double lessonHours, Double workPercentageInInstitution) {
        Objects.requireNonNull(teacher);
        Objects.requireNonNull(unit);
        Objects.requireNonNull(executionInterval);
        Objects.requireNonNull(teacherCategory);
        Objects.requireNonNull(contracted);
        Objects.requireNonNull(lessonHours);
        TeacherAuthorization existing = teacher.getTeacherAuthorization(executionInterval).orElse(null);
        if (existing != null) {
            if (existing.getUnit() == unit && existing.getContracted().equals(contracted)
                    && existing.getLessonHours().equals(lessonHours)) {
                return existing;
            } else {
                existing.revoke();
            }
        }
        return new TeacherAuthorization(teacher, unit, executionInterval, teacherCategory, contracted, lessonHours,
                workPercentageInInstitution);
    }

    public void revoke() {
        setRevokedTeacher(getTeacher());
        setTeacher(null);
        setRevokedUnit(getUnit());
        setUnit(null);
        setRevokedExecutionSemester(getExecutionInterval());
        setExecutionSemester(null);
        setRevoker(Authenticate.getUser());
        setRevokeTime(new DateTime());
        setRevokedRootDomainObject(getRootDomainObject());
        setRootDomainObject(null);
    }



    @Override
    public Unit getUnit() {
        if (getRevokedRootDomainObject() != null) {
            return getRevokedUnit();
        }
        return super.getUnit();
    }

    /**
     * @deprecated use {@link #getExecutionInterval()} instead.
     */
    @Deprecated
    @Override
    public ExecutionInterval getExecutionSemester() {
        return getExecutionInterval();
    }

    public ExecutionInterval getExecutionInterval() {
        if (getRevokedRootDomainObject() != null) {
            return getRevokedExecutionSemester();
        }
        return super.getExecutionSemester();
    }

    @Override
    public Teacher getTeacher() {
        if (getRevokedRootDomainObject() != null) {
            return getRevokedTeacher();
        }

        return super.getTeacher();
    }

    public boolean isContracted() {
        return super.getContracted();
    }

    @Override
    public int compareTo(TeacherAuthorization o) {
        int semester = getExecutionInterval().compareTo(o.getExecutionInterval());
        if (semester != 0) {
            return semester;
        }
        int category = getTeacherCategory().compareTo(o.getTeacherCategory());
        if (category != 0) {
            return category;
        }
        return getExternalId().compareTo(o.getExternalId());
    }

    protected ExecutionInterval getRevokedExecutionInterval() {
        return super.getRevokedExecutionSemester();
    }

    public void delete() {
        super.setTeacher(null);
        super.setUnit(null);
        super.setExecutionSemester(null);
        super.setAuthorizer(null);
        super.setTeacherCategory(null);
        super.setRootDomainObject(null);

        super.setRevokedExecutionSemester(null);
        super.setRevokedTeacher(null);
        super.setRevokedUnit(null);
        super.setRevokedRootDomainObject(null);
        super.setRevoker(null);

        super.deleteDomainObject();
    }

}
