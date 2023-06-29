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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class LessonPlanning extends LessonPlanning_Base {

    public static final Comparator<LessonPlanning> COMPARATOR_BY_ORDER = Comparator.comparing(LessonPlanning::getOrderOfPlanning);

    protected LessonPlanning() {
        setRootDomainObject(Bennu.getInstance());
    }

    public LessonPlanning(LocalizedString title, LocalizedString planning, CourseLoadType courseLoadType,
            ExecutionCourse executionCourse) {
        this();
        setLastOrder(executionCourse, courseLoadType);
        setTitle(title);
        setPlanning(planning);
        setCourseLoadType(courseLoadType);
        setExecutionCourse(executionCourse);

        CurricularManagementLog.createLog(executionCourse, Bundle.MESSAGING, "log.executionCourse.curricular.planning.added",
                title.getContent(), courseLoadType.getName().getContent(), executionCourse.getNome(),
                executionCourse.getDegreePresentationString());
    }

//    @Deprecated
//    public LessonPlanning(LocalizedString title, LocalizedString planning, ShiftType lessonType,
//            ExecutionCourse executionCourse) {
//        this(title, planning, CourseLoadType.findByShiftType(lessonType).orElseThrow(), executionCourse);
//        setLessonType(lessonType);
//    }

    public void delete() {
        final ExecutionCourse executionCourse = getExecutionCourse();
        final CourseLoadType courseLoadType = getCourseLoadType();
        final String title = getTitle().getContent();

        super.setExecutionCourse(null);
        super.setCourseLoadType(null);
        super.setRootDomainObject(null);
        deleteDomainObject();

        if (executionCourse != null && courseLoadType != null) {
            CurricularManagementLog.createLog(executionCourse, Bundle.MESSAGING,
                    "log.executionCourse.curricular.planning.removed", title, courseLoadType.getName().getContent(),
                    executionCourse.getNome(), executionCourse.getDegreePresentationString());

            final AtomicInteger newOrder = new AtomicInteger();
            find(executionCourse, courseLoadType).sorted(COMPARATOR_BY_ORDER)
                    .forEach(lp -> lp.setOrderOfPlanning(newOrder.incrementAndGet()));
        }
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkRequiredParameters() {
        return getTitle() != null && !getTitle().isEmpty() && getOrderOfPlanning() != null;
    }

    @Override
    public void setLessonType(ShiftType lessonType) {
        if (lessonType == null) {
            throw new DomainException("error.LessonPlanning.no.lessonType");
        }
        super.setLessonType(lessonType);
    }

    @Override
    public void setTitle(LocalizedString title) {
        if (title == null || title.getLocales().isEmpty()) {
            throw new DomainException("error.LessonPlanning.no.title");
        }
        super.setTitle(title);
    }

    @Override
    public void setOrderOfPlanning(Integer orderOfPlanning) {
        if (orderOfPlanning == null) {
            throw new DomainException("error.LessonPlanning.empty.order");
        }
        super.setOrderOfPlanning(orderOfPlanning);
    }

    public void moveTo(Integer order) {
        if (getExecutionCourse() != null) {
            final Map<Integer, LessonPlanning> planningsMap = find(getExecutionCourse(), getCourseLoadType())
                    .collect(Collectors.toMap(LessonPlanning::getOrderOfPlanning, lp -> lp));

            final LessonPlanning existingPlanning = planningsMap.get(order);
            if (existingPlanning != null) {
                existingPlanning.setOrderOfPlanning(getOrderOfPlanning());
                setOrderOfPlanning(order);
            }
        }
    }

    private void setLastOrder(ExecutionCourse executionCourse, CourseLoadType courseLoadType) {
        int maxOrder = find(executionCourse, courseLoadType).mapToInt(LessonPlanning::getOrderOfPlanning).max().orElse(0);
        setOrderOfPlanning(maxOrder + 1);
    }

    public static Stream<LessonPlanning> find(final ExecutionCourse executionCourse, final CourseLoadType courseLoadType) {
        return executionCourse.getLessonPlanningsSet().stream().filter(lp -> lp.getCourseLoadType() == courseLoadType);
    }

//    @Deprecated
//    public static List<LessonPlanning> findOrdered(final ExecutionCourse executionCourse, final ShiftType lessonType) {
//        return executionCourse.getLessonPlanningsSet().stream().filter(lp -> lp.getLessonType().equals(lessonType))
//                .sorted(COMPARATOR_BY_ORDER).collect(Collectors.toUnmodifiableList());
//    }

    public static void copyLessonPlanningsFrom(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) {
        final Collection<CourseLoadType> courseLoadTypes = executionCourseTo.getCourseLoadTypes();
        courseLoadTypes.forEach(loadType -> find(executionCourseFrom, loadType).sorted(COMPARATOR_BY_ORDER)
                .forEach(planning -> new LessonPlanning(planning.getTitle(), planning.getPlanning(), planning.getCourseLoadType(),
                        executionCourseTo)));
    }

}
