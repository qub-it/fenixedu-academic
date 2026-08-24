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
package org.fenixedu.academic.domain.degreeStructure;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;

public class RootCourseGroup extends RootCourseGroup_Base {

    public RootCourseGroup() {
        super();
    }

    public RootCourseGroup(final DegreeCurricularPlan degreeCurricularPlan, final String name, final String nameEn) {
        if (degreeCurricularPlan == null) {
            throw new DomainException("error.degreeStructure.CourseGroup.degreeCurricularPlan.cannot.be.null");
        }
        init(name, nameEn);
        setParentDegreeCurricularPlan(degreeCurricularPlan);
        createCycleCourseGroups(degreeCurricularPlan.getDegreeType());
    }

    private void createCycleCourseGroups(DegreeType degreeType) {
        ExecutionInterval executionInterval = ExecutionYear.findCurrent(getDegree().getCalendar()).getFirstExecutionPeriod();
        for (final CycleType cycleType : degreeType.getCycleTypes()) {
            new CycleCourseGroup(this, cycleType.getDescription(Locale.getDefault()), cycleType.getDescription(Locale.ENGLISH),
                    cycleType, executionInterval, null);
        }
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public void delete() {
        if (!getCanBeDeleted()) {
            throw new DomainException("courseGroup.notEmptyCourseGroupContexts");
        }
        removeChildDegreeModules();
        setParentDegreeCurricularPlan(null);
        super.delete();
    }

    private void removeChildDegreeModules() {
        for (final DegreeModule degreeModule : getChildDegreeModules()) {
            degreeModule.delete();
        }
    }

    @Override
    public Boolean getCanBeDeleted() {
        return getCurriculumModulesSet().isEmpty() && childsCanBeDeleted();
    }

    private boolean childsCanBeDeleted() {
        return getChildContextsSet().stream().map(Context::getChildDegreeModule).allMatch(DegreeModule::getCanBeDeleted);
    }

    static public RootCourseGroup createRoot(final DegreeCurricularPlan degreeCurricularPlan, final String name,
            final String nameEn) {
        return new RootCourseGroup(degreeCurricularPlan, name, nameEn);
    }

    @Override
    public void addParentContexts(Context parentContexts) {
        throw new DomainException("error.degreeStructure.RootCourseGroup.cannot.have.parent.contexts");
    }

    public CycleCourseGroup getFirstCycleCourseGroup() {
        return getCycleCourseGroup(CycleType.FIRST_CYCLE);
    }

    public CycleCourseGroup getSecondCycleCourseGroup() {
        return getCycleCourseGroup(CycleType.SECOND_CYCLE);
    }

    public CycleCourseGroup getThirdCycleCourseGroup() {
        return getCycleCourseGroup(CycleType.THIRD_CYCLE);
    }

    private Stream<CycleCourseGroup> getCycleCourseGroupsStream() {
        return getChildContextsSet().stream().map(Context::getChildDegreeModule).filter(DegreeModule::isCycleCourseGroup)
                .map(CycleCourseGroup.class::cast);
    }

    public CycleCourseGroup getCycleCourseGroup(CycleType cycle) {
        return getCycleCourseGroupsStream().filter(group -> cycle == group.getCycleType()).findFirst().orElse(null);
    }

    public Collection<CycleCourseGroup> getCycleCourseGroups() {
        return getCycleCourseGroupsStream().collect(Collectors.toSet());
    }

    public boolean hasCycleGroups() {
        return !getCycleCourseGroups().isEmpty();
    }

    @Override
    public Collection<CycleCourseGroup> getParentCycleCourseGroups() {
        return Collections.emptySet();
    }

}
