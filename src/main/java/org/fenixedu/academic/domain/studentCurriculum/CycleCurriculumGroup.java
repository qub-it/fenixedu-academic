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
package org.fenixedu.academic.domain.studentCurriculum;

import java.util.Comparator;

import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.exceptions.DomainException;

/**
 *
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 *
 */
public class CycleCurriculumGroup extends CycleCurriculumGroup_Base {

    static final private Comparator<CycleCurriculumGroup> COMPARATOR_BY_CYCLE_TYPE =
            (o1, o2) -> CycleType.COMPARATOR_BY_LESS_WEIGHT.compare(o1.getCycleType(), o2.getCycleType());

    static final public Comparator<CycleCurriculumGroup> COMPARATOR_BY_CYCLE_TYPE_AND_ID =
            COMPARATOR_BY_CYCLE_TYPE.thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

    protected CycleCurriculumGroup() {
        super();
    }

    public CycleCurriculumGroup(RootCurriculumGroup rootCurriculumGroup, CycleCourseGroup cycleCourseGroup,
            ExecutionInterval executionInterval) {
        this();
        init(rootCurriculumGroup, cycleCourseGroup, executionInterval);
    }

    public CycleCurriculumGroup(RootCurriculumGroup rootCurriculumGroup, CycleCourseGroup cycleCourseGroup) {
        this();
        init(rootCurriculumGroup, cycleCourseGroup);
    }

    @Override
    protected void init(CurriculumGroup curriculumGroup, CourseGroup courseGroup) {
        checkInitConstraints((RootCurriculumGroup) curriculumGroup, (CycleCourseGroup) courseGroup);
        super.init(curriculumGroup, courseGroup);
    }

    @Override
    protected void init(CurriculumGroup curriculumGroup, CourseGroup courseGroup, ExecutionInterval executionInterval) {
        checkInitConstraints((RootCurriculumGroup) curriculumGroup, (CycleCourseGroup) courseGroup);
        super.init(curriculumGroup, courseGroup, executionInterval);
    }

    private void checkInitConstraints(final RootCurriculumGroup rootCurriculumGroup, final CycleCourseGroup cycleCourseGroup) {
        if (rootCurriculumGroup.getCycleCurriculumGroup(cycleCourseGroup.getCycleType()) != null) {
            throw new DomainException(
                    "error.studentCurriculum.RootCurriculumGroup.cycle.course.group.already.exists.in.curriculum",
                    cycleCourseGroup.getName());
        }
    }

    @Override
    public void setCurriculumGroup(CurriculumGroup curriculumGroup) {
        if (curriculumGroup != null && !(curriculumGroup instanceof RootCurriculumGroup)) {
            throw new DomainException("error.curriculumGroup.CycleParentCanOnlyBeRootCurriculumGroup");
        }
        super.setCurriculumGroup(curriculumGroup);
    }

    @Override
    public void setDegreeModule(DegreeModule degreeModule) {
        if (degreeModule != null && !(degreeModule instanceof CycleCourseGroup)) {
            throw new DomainException("error.curriculumGroup.CycleParentDegreeModuleCanOnlyBeCycleCourseGroup");
        }
        super.setDegreeModule(degreeModule);
    }

    @Override
    public CycleCourseGroup getDegreeModule() {
        return (CycleCourseGroup) super.getDegreeModule();
    }

    @Override
    public boolean isCycleCurriculumGroup() {
        return true;
    }

    public boolean isCycle(final CycleType cycleType) {
        return getCycleType() == cycleType;
    }

    public boolean isFirstCycle() {
        return isCycle(CycleType.FIRST_CYCLE);
    }

    public CycleCourseGroup getCycleCourseGroup() {
        return getDegreeModule();
    }

    public CycleType getCycleType() {
        return getCycleCourseGroup().getCycleType();
    }

    @Override
    public RootCurriculumGroup getCurriculumGroup() {
        return (RootCurriculumGroup) super.getCurriculumGroup();
    }

    @Override
    public void deleteRecursive() {
        getCurriculumModulesSet().stream().toList().forEach(CurriculumModule::deleteRecursive);

        super.delete();
    }
    
    @Override
    public CycleCurriculumGroup getParentCycleCurriculumGroup() {
        return this;
    }

}
