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
package org.fenixedu.academic.ui.renderers.providers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.MethodUtils;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;

public class ExecutionPeriodsForExecutionYear implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {

        ExecutionYear executionYear;
        try {
            executionYear = (ExecutionYear) MethodUtils.invokeMethod(source, "getExecutionYear", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ExecutionSemester> periods = new ArrayList<ExecutionSemester>();
        if (executionYear != null) {
            periods.addAll(executionYear.getExecutionPeriodsSet());
        }

        return periods;
    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyConverter();
    }

}