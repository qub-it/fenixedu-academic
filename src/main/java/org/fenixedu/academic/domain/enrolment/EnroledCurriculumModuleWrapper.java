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
package org.fenixedu.academic.domain.enrolment;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.CurricularRule;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;

public class EnroledCurriculumModuleWrapper implements Serializable, IDegreeModuleToEvaluate {

    private static final long serialVersionUID = 8730987603988026373L;

    private CurriculumModule curriculumModule;

    protected Context context;

    private ExecutionInterval executionInterval;

    public EnroledCurriculumModuleWrapper(final CurriculumModule curriculumModule, final ExecutionInterval executionInterval) {
        setCurriculumModule(curriculumModule);
        setExecutionInterval(executionInterval);
    }

    public CurriculumModule getCurriculumModule() {
        return this.curriculumModule;
    }

    public void setCurriculumModule(CurriculumModule curriculumModule) {
        this.curriculumModule = curriculumModule;
    }

    @Override
    public Context getContext() {
        if (context == null) {
            if (!getCurriculumModule().isRoot()) {
                findContext();
            }
        }
        return context;
    }

    private void findContext() {
        Context result = null;

        final CurriculumGroup parent = getCurriculumModule().getCurriculumGroup();
        if (parent.getDegreeModule() != null) {
            for (final Context context : parent.getDegreeModule().getValidChildContexts(getExecutionInterval())) {
                if (context.getChildDegreeModule() == getDegreeModule()) {
                    if (result == null || context.getCurricularYear().intValue() < result.getCurricularYear().intValue()) {
                        result = context;
                    }
                }
            }
        }

        setContext(result);
    }

    public void setContext(Context context) {
        this.context = context;
    }

//    @Override
//    public ExecutionSemester getExecutionPeriod() {
//        return this.executionInterval.convert(ExecutionSemester.class);
//    }

    public void setExecutionInterval(ExecutionInterval executionInterval) {
        this.executionInterval = executionInterval;
    }

    @Override
    public ExecutionInterval getExecutionInterval() {
        return this.executionInterval;
    }

    @Override
    public CurriculumGroup getCurriculumGroup() {
        return getCurriculumModule().getCurriculumGroup();
    }

    @Override
    public DegreeModule getDegreeModule() {
        return getCurriculumModule().getDegreeModule();
    }

    public boolean hasDegreeModule() {
        return getDegreeModule() != null;
    }

    @Override
    public boolean isLeaf() {
        if (!getCurriculumModule().isLeaf()) {
            return false;
        }
        final CurriculumLine curriculumLine = (CurriculumLine) getCurriculumModule();
        return curriculumLine.isEnrolment();
    }

    @Override
    final public boolean isEnroled() {
        return true;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public boolean isDissertation() {
        return false;
    }

    @Override
    public boolean canCollectRules() {
        if (getCurriculumModule().isLeaf()) {
            return true;
        } else {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) getCurriculumModule();
            return curriculumGroup.getCurriculumModulesSet().isEmpty();
        }
    }

    @Override
    public Double getEctsCredits(final ExecutionInterval executionInterval) {
        return getCurriculumModule().getEctsCredits();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EnroledCurriculumModuleWrapper) {
            final EnroledCurriculumModuleWrapper moduleEnroledWrapper = (EnroledCurriculumModuleWrapper) obj;
            return getCurriculumModule() == moduleEnroledWrapper.getCurriculumModule();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getCurriculumModule().hashCode();
    }

    @Override
    public List<CurricularRule> getCurricularRulesFromDegreeModule(ExecutionInterval executionInterval) {
        return hasDegreeModule() ? getDegreeModule().getCurricularRules(getContext(), executionInterval) : Collections.EMPTY_LIST;
    }

    @Override
    public Set<ICurricularRule> getCurricularRulesFromCurriculumGroup(ExecutionInterval executionInterval) {
        return getCurriculumModule().isRoot() ? Collections.EMPTY_SET : getCurriculumGroup()
                .getCurricularRules(executionInterval);
    }

    @Override
    public String getName() {
        return getCurriculumModule().getName().getContent();
    }

    @Override
    public String getYearFullLabel() {
        if (getExecutionInterval() != null) {
            return getExecutionInterval().getQualifiedName();
        }
        return "";
    }

    @Override
    public boolean isOptionalCurricularCourse() {
        return false;
    }

    @Override
    public Double getEctsCredits() {
        return getCurriculumModule().getEctsCredits();
    }

    @Override
    public String getKey() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getCurriculumModule().getClass().getName()).append(":")
                .append(this.getCurriculumModule().getExternalId()).append(",")
                .append(this.getExecutionInterval().getClass().getName()).append(":")
                .append(this.getExecutionInterval().getExternalId());
        return stringBuilder.toString();
    }

    @Override
    final public boolean isEnroling() {
        return false;
    }

    @Override
    public boolean isFor(final DegreeModule degreeModule) {
        return getDegreeModule() == degreeModule;
    }

    @Override
    public boolean isAnnualCurricularCourse(final ExecutionYear executionYear) {
        if (getDegreeModule().isLeaf()) {
            return ((CurricularCourse) getDegreeModule()).isAnual(executionYear);
        }
        return false;
    }
}
