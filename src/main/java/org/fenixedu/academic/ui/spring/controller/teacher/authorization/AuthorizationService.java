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
package org.fenixedu.academic.ui.spring.controller.teacher.authorization;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.TeacherCategory;
import org.fenixedu.bennu.core.domain.Bennu;
import org.springframework.stereotype.Service;

/***
 * Teacher authorization and categories service
 * 
 * This service provides methods to manage teacher authorizations and categories.
 * 
 * @author Sérgio Silva (sergio.silva@tecnico.ulisboa.pt)
 *
 */
@Service
public class AuthorizationService {

    /***
     * Get all teacher categories
     * 
     * @return
     */
    public List<TeacherCategory> getCategories() {
        return Bennu.getInstance().getTeacherCategorySet().stream().distinct().sorted().collect(Collectors.toList());
    }

}
