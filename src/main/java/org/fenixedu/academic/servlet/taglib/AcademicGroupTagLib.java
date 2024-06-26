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
package org.fenixedu.academic.servlet.taglib;

import java.util.Collections;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.fenixedu.academic.domain.AcademicProgram;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.groups.PermissionService;
import org.fenixedu.bennu.core.security.Authenticate;

public class AcademicGroupTagLib extends TagSupport {

    private static final long serialVersionUID = -8050082985849930419L;

    private String operation;

    private String permission;

    private AcademicProgram program;

    private AdministrativeOffice office;

    @Override
    public int doStartTag() throws JspException {
        Set<AcademicProgram> programs =
                program != null ? Collections.singleton(program) : Collections.<AcademicProgram> emptySet();
        Set<AdministrativeOffice> offices =
                office != null ? Collections.singleton(office) : Collections.<AdministrativeOffice> emptySet();
        AcademicAuthorizationGroup group =
                AcademicAuthorizationGroup.get(AcademicOperationType.valueOf(operation), programs, offices, null);

        if (group.isMember(Authenticate.getUser()) || PermissionService.hasAccess(permission, Authenticate.getUser())) {
            return EVAL_BODY_INCLUDE;
        }

        return SKIP_BODY;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public AcademicProgram getProgram() {
        return program;
    }

    public void setProgram(AcademicProgram program) {
        this.program = program;
    }

    public AdministrativeOffice getOffice() {
        return office;
    }

    public void setOffice(AdministrativeOffice office) {
        this.office = office;
    }
}
