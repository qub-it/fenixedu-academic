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

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanComparator;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;

import pt.ist.fenixframework.Atomic;

/**
 * @author João Mota
 */
public class Professorship extends Professorship_Base {

    public static final Comparator<Professorship> COMPARATOR_BY_PERSON_NAME =
            new BeanComparator("person.name", Collator.getInstance());

    public static final String PROFESSORSHIP_CREATED = "academic.professorship.created";

    public Professorship() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public boolean belongsToExecutionInterval(ExecutionInterval executionInterval) {
        return this.getExecutionCourse().getExecutionInterval().equals(executionInterval);
    }

    /**
     * @deprecated Use {@link #belongsToExecutionInterval(ExecutionInterval)
     */
    @Deprecated
    public boolean belongsToExecutionPeriod(ExecutionInterval executionInterval) {
        return this.getExecutionCourse().getExecutionInterval().equals(executionInterval);
    }

    @Atomic
    public static Professorship create(Boolean responsibleFor, ExecutionCourse executionCourse, Person person) {

        Objects.requireNonNull(responsibleFor);
        Objects.requireNonNull(executionCourse);
        Objects.requireNonNull(person);

        if (executionCourse.getProfessorshipsSet().stream().anyMatch(p -> person.equals(p.getPerson()))) {
            throw new DomainException("error.teacher.already.associated.to.professorship");
        }

        Professorship professorShip = new Professorship();
        professorShip.setExecutionCourse(executionCourse);
        professorShip.setPerson(person);
        professorShip.setCreator(Authenticate.getUser().getPerson());

        professorShip.setResponsibleFor(responsibleFor);

        if (person.getTeacher() != null) {
            executionCourse.getAssociatedSummariesSet().stream()
                    .filter(s -> s.getTeacher() != null && s.getTeacher().equals(person.getTeacher()))
                    .forEach(s -> s.moveFromTeacherToProfessorship(professorShip));
        }

        Signal.emit(PROFESSORSHIP_CREATED, new DomainObjectEvent<>(professorShip));
        ProfessorshipManagementLog.createLog(professorShip.getExecutionCourse(), Bundle.MESSAGING,
                "log.executionCourse.professorship.added", professorShip.getPerson().getPresentationName(),
                professorShip.getExecutionCourse().getNome(), professorShip.getExecutionCourse().getDegreePresentationString());
        return professorShip;
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        ProfessorshipManagementLog.createLog(getExecutionCourse(), Bundle.MESSAGING, "log.executionCourse.professorship.removed",
                getPerson().getPresentationName(), getExecutionCourse().getNome(),
                getExecutionCourse().getDegreePresentationString());
        setExecutionCourse(null);
        setPerson(null);
        setRootDomainObject(null);
        setCreator(null);
        deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getAssociatedSummariesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.remove.professorship.hasAnyAssociatedSummaries"));
        }
        if (!getAssociatedShiftProfessorshipSet().isEmpty()) {
            blockers.add(
                    BundleUtil.getString(Bundle.APPLICATION, "error.remove.professorship.hasAnyAssociatedShiftProfessorship"));
        }
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    public static List<Professorship> readByDegreeCurricularPlanAndExecutionYear(DegreeCurricularPlan degreeCurricularPlan,
            ExecutionYear executionYear) {

        return degreeCurricularPlan.getCurricularCoursesSet().stream()
                .flatMap(cc -> cc.getExecutionCoursesByExecutionYear(executionYear).stream())
                .flatMap(ec -> ec.getProfessorshipsSet().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Professorship> readByDegreeCurricularPlanAndExecutionPeriod(DegreeCurricularPlan degreeCurricularPlan,
            ExecutionInterval executionInterval) {

        return degreeCurricularPlan.getCurricularCoursesSet().stream()
                .flatMap(cc -> cc.getExecutionCoursesByExecutionPeriod(executionInterval).stream())
                .flatMap(ec -> ec.getProfessorshipsSet().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Professorship> readByDegreeCurricularPlansAndExecutionYear(
            List<DegreeCurricularPlan> degreeCurricularPlans, ExecutionYear executionYear) {

        return degreeCurricularPlans.stream()
                .flatMap(dcp -> dcp.getCurricularCoursesSet().stream())
                .flatMap(cc -> (executionYear != null
                        ? cc.getExecutionCoursesByExecutionYear(executionYear).stream()
                        : cc.getAssociatedExecutionCoursesSet().stream()))
                .flatMap(ec -> ec.getProfessorshipsSet().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public Teacher getTeacher() {
        return getPerson().getTeacher();
    }

    public void setTeacher(Teacher teacher) {
        setPerson(teacher.getPerson());
    }

    public boolean isResponsibleFor() {
        return getResponsibleFor().booleanValue();
    }

    public void setResponsibleFor(boolean responsibleFor) {
        super.setResponsibleFor(responsibleFor);
    }

    @Override
    public void setResponsibleFor(Boolean responsibleFor) {
        if (responsibleFor == null) {
            responsibleFor = Boolean.FALSE;
        }
        super.setResponsibleFor(responsibleFor);
    }

    public boolean hasTeacher() {
        return getPerson() != null && getPerson().getTeacher() != null;
    }

    public void removeTeacher() {
        setPerson(null);
    }

    public String getDegreeSiglas() {
        return getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                .map(cc -> cc.getDegreeCurricularPlan().getDegree().getSigla())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public String getDegreePlanNames() {
        return getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                .map(cc -> cc.getDegreeCurricularPlan().getName())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public Stream<Shift> getShifts() {
        return getAssociatedShiftProfessorshipSet().stream().map(ShiftProfessorship::getShift);
    }

}
