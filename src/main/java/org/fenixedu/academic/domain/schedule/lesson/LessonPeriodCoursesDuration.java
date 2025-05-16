package org.fenixedu.academic.domain.schedule.lesson;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public enum LessonPeriodCoursesDuration {

    ALL, ANNUAL, NOT_ANNUAL;

    public boolean isFor(final ExecutionCourse executionCourse, final ExecutionDegree executionDegree) {
        if (this == ALL) {
            return true;
        }

        final boolean isAnual = executionCourse.getAssociatedCurricularCoursesSet().stream()
                .filter(cc -> cc.getDegreeCurricularPlan() == executionDegree.getDegreeCurricularPlan())
                .anyMatch(cc -> cc.isAnual(executionDegree.getExecutionYear()));

        return switch (this) {
            case ANNUAL -> isAnual;
            case NOT_ANNUAL -> !isAnual;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }

    public String getLocalizedName() {
        return BundleUtil.getString(Bundle.ENUMERATION, LessonPeriodCoursesDuration.class.getSimpleName() + "." + name());
    }
}
