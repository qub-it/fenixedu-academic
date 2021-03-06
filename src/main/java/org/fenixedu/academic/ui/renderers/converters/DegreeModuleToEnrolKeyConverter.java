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
/**
 * 
 */
package org.fenixedu.academic.ui.renderers.converters;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;

import pt.ist.fenixWebFramework.renderers.components.converters.ConversionException;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;

public class DegreeModuleToEnrolKeyConverter extends Converter {

    @Override
    public Object convert(Class type, Object value) {
        if (value == null) {
            return null;
        }

        final pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter converter =
                new pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter();
        final List<DegreeModuleToEnrol> result = new ArrayList<DegreeModuleToEnrol>();
        final String[] values = (String[]) value;
        for (String key : values) {
            String[] parts = key.split(",");
            if (parts.length < 3) {
                throw new ConversionException("invalid key format: " + key);
            }

            final Context context = (Context) converter.convert(type, parts[0]);
            final CurriculumGroup curriculumGroup = (CurriculumGroup) converter.convert(type, parts[1]);
            final ExecutionInterval executionInterval = (ExecutionInterval) converter.convert(type, parts[2]);
            result.add(new DegreeModuleToEnrol(curriculumGroup, context, executionInterval));
        }

        return result;
    }
}