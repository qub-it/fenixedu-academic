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
 * ExecutionDegree.java
 *
 * Created on 2 de Novembro de 2002, 20:53
 */

package org.fenixedu.academic.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.schedule.lesson.ExecutionDegreeLessonPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;

/**
 *
 * @author rpfi
 * @author Joao Carvalho (joao.pedro.carvalho@ist.utl.pt)
 */

public class ExecutionDegree extends ExecutionDegree_Base implements Comparable<ExecutionDegree> {

    public static final Comparator<ExecutionDegree> COMPARATOR_BY_DEGREE_ABBREVIATION =
            Comparator.comparing((ExecutionDegree ed) -> ed.getDegree().getSigla()).thenComparing(ExecutionDegree::getExternalId);

    public static final Comparator<ExecutionDegree> COMPARATOR_BY_DEGREE_NAME =
            Comparator.comparing(ed -> ed.getDegree().getNameFor((AcademicInterval) null).getContent());

    static final public Comparator<ExecutionDegree> EXECUTION_DEGREE_COMPARATOR_BY_YEAR =
            Comparator.comparing(ExecutionDegree::getExecutionYear);

    static final public Comparator<ExecutionDegree> EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME =
            Comparator.comparing(ExecutionDegree::getDegree, Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID);

    static final public Comparator<ExecutionDegree> EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR =
            EXECUTION_DEGREE_COMPARATOR_BY_DEGREE_TYPE_AND_DEGREE_NAME.thenComparing(EXECUTION_DEGREE_COMPARATOR_BY_YEAR);

    private static final Comparator<ExecutionDegree> COMPARATOR_BY_DEGREE_CURRICULAR_PLAN_ID_INTERNAL_DESC =
            Comparator.comparing((ExecutionDegree ed) -> ed.getDegreeCurricularPlan().getExternalId()).reversed();

    private ExecutionDegree() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    protected ExecutionDegree(DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear, Boolean publishedExamMap) {
        this();
        if (degreeCurricularPlan == null || executionYear == null) {
            throw new DomainException("execution.degree.null.args.to.constructor");
        }

        setDegreeCurricularPlan(degreeCurricularPlan);
        setExecutionYear(executionYear);
    }

    @Override
    public void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getSchoolClassesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.ExecutionDegree.cannotBeDeleted.hasSchoolClasses"));
        }
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        getCoordinatorsListSet().forEach(Coordinator::delete);

        setExecutionYear(null);
        setDegreeCurricularPlan(null);

        getExecutionDegreeLessonPeriodsSet().forEach(ExecutionDegreeLessonPeriod::delete);

        setRootDomainObject(null);
        deleteDomainObject();
    }

    @Override
    public int compareTo(ExecutionDegree executionDegree) {
        final ExecutionYear executionYear = executionDegree.getExecutionYear();
        return getExecutionYear().compareTo(executionYear);
    }

    public boolean isAfter(ExecutionDegree executionDegree) {
        return this.compareTo(executionDegree) > 0;
    }

    public boolean isBefore(ExecutionDegree executionDegree) {
        return this.compareTo(executionDegree) < 0;
    }

    public boolean isFirstYear() {
        final Collection<ExecutionDegree> executionDegrees = this.getDegreeCurricularPlan().getExecutionDegreesSet();
        return this == Collections.min(executionDegrees, EXECUTION_DEGREE_COMPARATOR_BY_YEAR);
    }

    public Coordinator getCoordinatorByTeacher(Person person) {
        return getCoordinatorsListSet().stream().filter(coordinator -> coordinator.getPerson() == person).findFirst()
                .orElse(null);
    }

    public static List<ExecutionDegree> getAllByExecutionYear(ExecutionYear executionYear) {
        return executionYear == null ? Collections.emptyList() : executionYear.getExecutionDegreesSet().stream()
                .sorted(COMPARATOR_BY_DEGREE_CURRICULAR_PLAN_ID_INTERNAL_DESC).collect(Collectors.toList());
    }

    public static List<ExecutionDegree> getAllByExecutionYearAndDegreeType(ExecutionYear executionYear,
            DegreeType... degreeTypes) {
        if (executionYear == null || degreeTypes == null) {
            return Collections.emptyList();
        }

        return executionYear.getExecutionDegreesSet().stream()
                .filter(ed -> Arrays.stream(degreeTypes).anyMatch(type -> type == ed.getDegreeType()))
                .sorted(COMPARATOR_BY_DEGREE_CURRICULAR_PLAN_ID_INTERNAL_DESC).collect(Collectors.toList());
    }

    public static ExecutionDegree getByDegreeCurricularPlanAndExecutionYear(DegreeCurricularPlan degreeCurricularPlan,
            ExecutionYear executionYear) {
        if (degreeCurricularPlan == null || executionYear == null) {
            return null;
        }

        return degreeCurricularPlan.getExecutionDegreesSet().stream().filter(ed -> ed.getExecutionYear() == executionYear)
                .findFirst().orElse(null);
    }

    public List<Coordinator> getResponsibleCoordinators() {
        return getCoordinatorsListSet().stream().filter(Coordinator::getResponsible).collect(Collectors.toList());
    }
    
    final public String getPresentationName() {
        return getDegreeCurricularPlan().getPresentationName(getExecutionYear());
    }

    public String getDegreeName() {
        return getDegree().getNameI18N(getExecutionYear()).getContent();
    }

    public Degree getDegree() {
        return getDegreeCurricularPlan().getDegree();
    }

    public DegreeType getDegreeType() {
        return getDegree().getDegreeType();
    }

    public AcademicInterval getAcademicInterval() {
        return getExecutionYear().getAcademicInterval();
    }

    public SortedSet<SchoolClass> getSortedSchoolClasses() {
        return getSchoolClassesSet().stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(SchoolClass.COMPARATOR_BY_NAME)));
    }
}
