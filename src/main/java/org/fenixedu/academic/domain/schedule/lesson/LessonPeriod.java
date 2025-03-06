package org.fenixedu.academic.domain.schedule.lesson;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.bennu.core.domain.Bennu;

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
