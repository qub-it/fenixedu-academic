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
 * CriarTurno.java Created on 27 de Outubro de 2002, 18:48
 */

package org.fenixedu.academic.service.services.resourceAllocationManager;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.dto.InfoShift;
import org.fenixedu.academic.dto.InfoShiftEditor;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class CriarTurno {

    @Atomic
    public static InfoShift run(InfoShiftEditor infoTurno) {
        final ExecutionCourse executionCourse =
                FenixFramework.getDomainObject(infoTurno.getInfoDisciplinaExecucao().getExternalId());
        final Shift newShift = new Shift(executionCourse, infoTurno.getTipos(), infoTurno.getLotacao());
        return InfoShift.newInfoFromDomain(newShift);
    }
}