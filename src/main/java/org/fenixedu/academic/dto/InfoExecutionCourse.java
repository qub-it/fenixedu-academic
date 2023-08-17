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
/*
 * InfoExecutionCourse.java
 * 
 * Created on 28 de Novembro de 2002, 3:41
 */
package org.fenixedu.academic.dto;

import org.fenixedu.academic.domain.ExecutionCourse;

/**
 * @author tfc130
 */
@Deprecated
public class InfoExecutionCourse extends InfoObject {

    private final ExecutionCourse executionCourseDomainReference;

    public InfoExecutionCourse(final ExecutionCourse executionCourse) {
        executionCourseDomainReference = executionCourse;
    }

    public static InfoExecutionCourse newInfoFromDomain(final ExecutionCourse executionCourse) {
        return executionCourse == null ? null : new InfoExecutionCourse(executionCourse);
    }

    public ExecutionCourse getExecutionCourse() {
        return executionCourseDomainReference;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof InfoExecutionCourse && getExecutionCourse() == ((InfoExecutionCourse) obj).getExecutionCourse();
    }

    @Override
    public int hashCode() {
        return getExecutionCourse().hashCode();
    }

    @Override
    public String getExternalId() {
        return getExecutionCourse().getExternalId();
    }

    @Override
    public void setExternalId(String integer) {
        throw new Error("Method should not be called!");
    }

    // =================== FIELDS RETRIEVED BY DOMAIN LOGIC
    // =======================

    public String getNome() {
        return getExecutionCourse().getName();
    }

    public String getSigla() {
        return getExecutionCourse().getSigla();
    }

    @Override
    public String toString() {
        return getExecutionCourse().toString();
    }

}
