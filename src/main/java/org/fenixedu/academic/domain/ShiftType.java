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

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public enum ShiftType {

    TEORICA,

//    PRATICA,
//
//    TEORICO_PRATICA,

    LABORATORIAL,

//    DUVIDAS,
//
//    RESERVA,

    SEMINARY,

    PROBLEMS,

    FIELD_WORK,

    TRAINING_PERIOD,

    TUTORIAL_ORIENTATION,

    OTHER;

    public String getSiglaTipoAula() {
        String value = this.name();
        return BundleUtil.getString(Bundle.ENUMERATION, getClass().getName() + ".short." + name());
    }

    public String getFullNameTipoAula() {
        String value = this.name();
        return BundleUtil.getString(Bundle.ENUMERATION, getClass().getName() + ".full." + name());
    }

    public String getName() {
        return name();
    }

    public CourseLoadType toCourseLoadType() {

        switch (this) {
        case TEORICA:
            return CourseLoadType.of(CourseLoadType.THEORETICAL);
        case PROBLEMS:
            return CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL);
        case LABORATORIAL:
            return CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY);
        case FIELD_WORK:
            return CourseLoadType.of(CourseLoadType.FIELD_WORK);
        case SEMINARY:
            return CourseLoadType.of(CourseLoadType.SEMINAR);
        case TRAINING_PERIOD:
            return CourseLoadType.of(CourseLoadType.INTERNSHIP);
        case TUTORIAL_ORIENTATION:
            return CourseLoadType.of(CourseLoadType.TUTORIAL_ORIENTATION);
        case OTHER:
            return CourseLoadType.of(CourseLoadType.OTHER);
        }

        return null;
    }

}
