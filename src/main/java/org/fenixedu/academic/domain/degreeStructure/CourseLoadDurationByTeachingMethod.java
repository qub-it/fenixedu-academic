package org.fenixedu.academic.domain.degreeStructure;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class CourseLoadDurationByTeachingMethod extends CourseLoadDurationByTeachingMethod_Base {

    protected CourseLoadDurationByTeachingMethod() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static CourseLoadDurationByTeachingMethod create(final CourseLoadDuration courseLoadDuration,
            final TeachingMethodType teachingMethodType, final BigDecimal hours) {

        if (courseLoadDuration.findLoadDurationByTeachingMethod(teachingMethodType).isPresent()) {
            throw new DomainException("error.CourseLoadDurationByTeachingMethod.create.alreadyExistsForTeachingMethodType",
                    teachingMethodType.getName().getContent());
        }

        final CourseLoadDurationByTeachingMethod result = new CourseLoadDurationByTeachingMethod();
        result.setCourseLoadDuration(courseLoadDuration);
        result.setTeachingMethodType(teachingMethodType);
        result.setHours(hours);
        return result;
    }

    @Override
    public void setHours(BigDecimal hours) {
        if (hours != null && hours.signum() < 0) {
            throw new DomainException("error.CourseLoadDurationByTeachingMethod.hours.cannotBeNegative",
                    getTeachingMethodType() != null ? getTeachingMethodType().getName().getContent() : null);
        }

        if (getCourseLoadDuration() == null) {
            throw new DomainException("error.CourseLoadDurationByTeachingMethod.loadDuration.cannotBeNull");
        }

        final BigDecimal totalHoursOfLoadDuration = getCourseLoadDuration().getHours();
        final BigDecimal totalHoursByTeachingMethods =
                getCourseLoadDuration().getDurationsByTeachingMethodSet().stream().filter(d -> d != this).map(d -> d.getHours())
                        .reduce(BigDecimal.ZERO, BigDecimal::add).add(hours != null ? hours : BigDecimal.ZERO);

        if (totalHoursByTeachingMethods.compareTo(totalHoursOfLoadDuration) > 0) {
            throw new DomainException("error.CourseLoadDurationByTeachingMethod.hours.greaterThanLoadDuration");
        }

        super.setHours(hours);
    }

    public void delete() {
        setRoot(null);
        setCourseLoadDuration(null);
        setTeachingMethodType(null);
        super.deleteDomainObject();
    }

}
