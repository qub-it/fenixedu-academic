package org.fenixedu.academic.domain.schedule.lesson;

import java.util.List;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.bennu.core.domain.Bennu;

public class ExecutionDegreeLessonPeriod extends ExecutionDegreeLessonPeriod_Base {

    protected ExecutionDegreeLessonPeriod() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static ExecutionDegreeLessonPeriod create(final ExecutionDegree executionDegree, final LessonPeriod lessonPeriod) {
        final ExecutionDegreeLessonPeriod result = new ExecutionDegreeLessonPeriod();
        result.setExecutionDegree(executionDegree);
        result.setLessonPeriod(lessonPeriod);
        result.setCurricularYears(new LessonPeriodCurricularYears(List.of()));
        return result;
    }

    public void delete() {
        setRoot(null);
        setLessonPeriod(null);
        setExecutionDegree(null);
        setOccupationPeriodReferenceFromMigration(null);
        super.deleteDomainObject();
    }
}
