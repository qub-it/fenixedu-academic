/**
 * This file was created on 4 October, 2018
 * 
 * - Fábio Ferreira - Núcleo de Desenvolvimento de Software da Reitoria da Universidade de Lisboa (desenvolvimento.di@reitoria.ulisboa.pt)
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
package org.fenixedu.academic.dto.resourceAllocationManager;

import java.io.Serializable;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;

public class TeacherContextSelectionBean implements Serializable {

    private AcademicInterval academicInterval;
    private String username;

    public TeacherContextSelectionBean(AcademicInterval academicInterval) {
        this.academicInterval = academicInterval;
    }

    public TeacherContextSelectionBean() {
        this(AcademicInterval.readDefaultAcademicInterval(AcademicPeriod.SEMESTER));
    }

    public AcademicInterval getAcademicInterval() {
        return academicInterval;
    }

    public void setAcademicInterval(AcademicInterval academicInterval) {
        this.academicInterval = academicInterval;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
