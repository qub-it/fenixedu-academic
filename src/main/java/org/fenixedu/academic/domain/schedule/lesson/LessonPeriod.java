package org.fenixedu.academic.domain.schedule.lesson;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.bennu.core.domain.Bennu;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LessonPeriod extends LessonPeriod_Base {

    protected LessonPeriod() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static LessonPeriod create(final ExecutionInterval executionInterval, final OccupationPeriod occupationPeriod) {
        final LessonPeriod result = new LessonPeriod();
        result.setExecutionInterval(executionInterval);
        result.setOccupationPeriod(occupationPeriod);
        return result;
    }

    public static Stream<LessonPeriod> findAll() {
        return Bennu.getInstance().getLessonPeriodsSet().stream();
    }

    public static Stream<LessonPeriod> findFor(final ExecutionDegree executionDegree, final ExecutionInterval interval) {
        return executionDegree.getExecutionDegreeLessonPeriodsSet().stream().map(ExecutionDegreeLessonPeriod::getLessonPeriod)
                .filter(p -> interval == null || interval == p.getExecutionInterval());
    }

    public static Stream<LessonPeriod> findFor(final ExecutionDegree executionDegree, final ExecutionCourse executionCourse) {
        final ExecutionInterval interval = executionCourse.getExecutionInterval();

        final boolean isAnual = executionCourse.getAssociatedCurricularCoursesSet().stream()
                .filter(cc -> cc.getDegreeCurricularPlan() == executionDegree.getDegreeCurricularPlan())
                .anyMatch(cc -> cc.isAnual(executionDegree.getExecutionYear()));

        final Set<Integer> courseYears = executionCourse.getAssociatedCurricularCoursesSet().stream()
                .filter(cc -> cc.getDegreeCurricularPlan() == executionDegree.getDegreeCurricularPlan())
                .flatMap(cc -> cc.getParentContexts(interval).stream()).map(Context::getCurricularYear).distinct()
                .collect(Collectors.toSet());

        final Predicate<ExecutionDegreeLessonPeriod> yearsPredicate = p -> Optional.ofNullable(p.getCurricularYears())
                .map(years -> years.hasAll() || !Collections.disjoint(years.getYears(), courseYears)).orElse(true);

        // if course is anual, show all periods of that year
        final Predicate<LessonPeriod> intervalPredicate = lp -> isAnual ?
                interval.getExecutionYear() == lp.getExecutionInterval().getExecutionYear() :
                interval == lp.getExecutionInterval();

        return executionDegree.getExecutionDegreeLessonPeriodsSet().stream().filter(yearsPredicate)
                .map(ExecutionDegreeLessonPeriod::getLessonPeriod).filter(intervalPredicate);
    }

    public void delete() {
        setRoot(null);
        if (!getOccupationPeriod().allNestedPeriodsAreEmpty()) {
            setOccupationPeriod(null);
//        } else {
            // getOccupationPeriod().delete(); //TODO: activate delete, after domain migration
        }
        setExecutionInterval(null);
        setOccupationPeriod(null);
        deleteDomainObject();
    }

}
