package org.fenixedu.academic.domain.degreeStructure;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class CourseLoadDuration extends CourseLoadDuration_Base {

    protected CourseLoadDuration() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static CourseLoadDuration create(final CompetenceCourseInformation courseInformation,
            final CourseLoadType courseLoadType, final BigDecimal hours) {

        if (courseInformation.findLoadDurationByType(courseLoadType).isPresent()) {
            throw new DomainException("error.CourseLoadDuration.create.alreadyExistsWithSameLoadType");
        }

        final CourseLoadDuration result = new CourseLoadDuration();
        result.setCompetenceCourseInformation(courseInformation);
        result.setCourseLoadType(courseLoadType);
        result.setHours(hours);

        return result;
    }

    @Override
    public void setHours(BigDecimal hours) {
        if (hours != null && hours.signum() < 0) {
            throw new DomainException("error.CourseLoadDuration.hours.cannotBeNegative");
        }

        super.setHours(hours);
    }

    void deleteTriggeredByCompetenceCourseInformation() {
        final Boolean previousInformationHasNotThisLoadType = getCompetenceCourseInformation().findPrevious()
                .map(cci -> cci.findLoadDurationByType(getCourseLoadType()).isEmpty()).orElse(false);
        if (previousInformationHasNotThisLoadType) {
            checkIfCanBeDeleted();
        }

        setCompetenceCourseInformation(null); // to bypass further deletion check
        delete();
    }

    public void delete() {
        checkIfCanBeDeleted();

        setRoot(null);
        setCompetenceCourseInformation(null);
        setCourseLoadType(null);
        super.deleteDomainObject();
    }

    private void checkIfCanBeDeleted() {
        final CompetenceCourseInformation competenceCourseInformation = getCompetenceCourseInformation();

        if (competenceCourseInformation != null) { // If it's null, the entire competence course information is to be deleted
            final Stream<ExecutionInterval> executionIntervals = competenceCourseInformation.getExecutionIntervalsRange();
            final CompetenceCourse competenceCourse = competenceCourseInformation.getCompetenceCourse();

            boolean shiftExistsForThisDurationLoadType = executionIntervals
                    .flatMap(ei -> competenceCourse.getExecutionCoursesByExecutionPeriod(ei).stream())
                    .flatMap(ec -> ec.getShiftsSet().stream()).anyMatch(s -> s.getCourseLoadType() == getCourseLoadType());

            if (shiftExistsForThisDurationLoadType) {
                throw new DomainException("error.CourseLoadDuration.delete.shiftsExistsForDuration");
            }
        }
    }
}
