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
import org.fenixedu.bennu.core.i18n.BundleUtil;
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

    @Deprecated
    public LessonPlanning(LocalizedString title, LocalizedString planning, ShiftType lessonType,
            ExecutionCourse executionCourse) {
        this(title, planning, CourseLoadType.findByShiftType(lessonType).orElseThrow(), executionCourse);
        setLessonType(lessonType);
    }

    public void delete() {
        final ExecutionCourse executionCourse = getExecutionCourse();
        final CourseLoadType courseLoadType = getCourseLoadType();
        final String title = getTitle().getContent();

        super.setExecutionCourse(null);
        super.setCourseLoadType(courseLoadType);
        super.setRootDomainObject(null);
        deleteDomainObject();

        if (executionCourse != null && courseLoadType != null) {
            CurricularManagementLog.createLog(getExecutionCourse(), Bundle.MESSAGING,
                    "log.executionCourse.curricular.planning.removed", title, courseLoadType.getName().getContent(),
                    executionCourse.getNome(), executionCourse.getDegreePresentationString());

            final AtomicInteger newOrder = new AtomicInteger();
            find(executionCourse, courseLoadType).sorted(COMPARATOR_BY_ORDER)
                    .forEach(lp -> lp.setOrderOfPlanning(newOrder.incrementAndGet()));
        }
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkRequiredParameters() {
        return getLessonType() != null && getTitle() != null && !getTitle().isEmpty() && getOrderOfPlanning() != null;
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

//            List<LessonPlanning> lessonPlannings = LessonPlanning.findOrdered(getExecutionCourse(), getLessonType());
//            if (!lessonPlannings.isEmpty() && order != getOrderOfPlanning() && order <= lessonPlannings.size() && order >= 1) {
//                LessonPlanning posPlanning = lessonPlannings.get(order - 1);
//                Integer posOrder = posPlanning.getOrderOfPlanning();
//                posPlanning.setOrderOfPlanning(getOrderOfPlanning());
//                setOrderOfPlanning(posOrder);
//            }
        }
    }

//    private void reOrderLessonPlannings() {
//        if (getExecutionCourse() != null) {
//            List<LessonPlanning> lessonPlannings = findOrdered(getExecutionCourse(), getLessonType());
//            if (!lessonPlannings.isEmpty() && !lessonPlannings.get(lessonPlannings.size() - 1).equals(this)) {
//                for (int i = getOrderOfPlanning(); i < lessonPlannings.size(); i++) {
//                    LessonPlanning planning = lessonPlannings.get(i);
//                    planning.setOrderOfPlanning(planning.getOrderOfPlanning() - 1);
//                }
//            }
//        }
//    }

    private void setLastOrder(ExecutionCourse executionCourse, CourseLoadType courseLoadType) {
//        List<LessonPlanning> lessonPlannings = findOrdered(executionCourse, lessonType);
//        Integer order =
//                (!lessonPlannings.isEmpty()) ? (lessonPlannings.get(lessonPlannings.size() - 1).getOrderOfPlanning() + 1) : 1;
        int maxOrder = find(executionCourse, courseLoadType).mapToInt(LessonPlanning::getOrderOfPlanning).max().orElse(0);
        setOrderOfPlanning(maxOrder + 1);
    }

    public String getLessonPlanningLabel() {
        StringBuilder builder = new StringBuilder();
        builder.append(BundleUtil.getString(Bundle.APPLICATION, "label.lesson")).append(" ");
        builder.append(getOrderOfPlanning()).append(" (");
        builder.append(BundleUtil.getString(Bundle.ENUMERATION, getLessonType().getName())).append(") - ");
        builder.append(getTitle().getContent());
        return builder.toString();
    }

    public void logEditEditLessonPlanning() {
        CurricularManagementLog.createLog(getExecutionCourse(), Bundle.MESSAGING,
                "log.executionCourse.curricular.planning.edited", getTitle().getContent(), getLessonType().getFullNameTipoAula(),
                getExecutionCourse().getNome(), getExecutionCourse().getDegreePresentationString());
    }

    public static Stream<LessonPlanning> find(final ExecutionCourse executionCourse, final CourseLoadType courseLoadType) {
        return executionCourse.getLessonPlanningsSet().stream().filter(lp -> lp.getCourseLoadType() == courseLoadType);
    }

    @Deprecated
    public static List<LessonPlanning> findOrdered(final ExecutionCourse executionCourse, final ShiftType lessonType) {
        return executionCourse.getLessonPlanningsSet().stream().filter(lp -> lp.getLessonType().equals(lessonType))
                .sorted(COMPARATOR_BY_ORDER).collect(Collectors.toUnmodifiableList());
    }

    public static void copyLessonPlanningsFrom(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) {
        final Collection<CourseLoadType> courseLoadTypes = executionCourseTo.getCourseLoadTypes();
        courseLoadTypes.forEach(loadType -> find(executionCourseFrom, loadType).sorted(COMPARATOR_BY_ORDER)
                .forEach(planning -> new LessonPlanning(planning.getTitle(), planning.getPlanning(), planning.getCourseLoadType(),
                        executionCourseTo)));
    }

}
