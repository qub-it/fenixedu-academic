package org.fenixedu.academic.domain.curricularRules;

import java.util.Collection;
import java.util.Optional;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;
import org.fenixedu.bennu.core.domain.Bennu;

public class EnrolmentModelConfigEntry extends EnrolmentModelConfigEntry_Base {

    protected EnrolmentModelConfigEntry() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static EnrolmentModelConfigEntry create(final DegreeCurricularPlan degreeCurricularPlan) {
        final EnrolmentModelConfigEntry result = new EnrolmentModelConfigEntry();
        result.setDegreeCurricularPlan(degreeCurricularPlan);
        return result;
    }

    public void editPeriods(final Collection<AcademicPeriodOrder> periods) {
        if (periods.stream().flatMap(period -> findFor(getDegreeCurricularPlan(), period).stream())
                .anyMatch(entry -> entry != this)) {
            throw new DomainException("error.EnrolmentModelConfigEntry.already.exists.period.in.other.config");
        }

        getAcademicPeriodOrdersSet().clear();
        getAcademicPeriodOrdersSet().addAll(periods);
    }

//    public boolean isFor(ExecutionInterval executionInterval) {
//        return getAcademicPeriodOrdersSet().stream().anyMatch(apo -> apo.isFor(executionInterval));
//    }
//
//    public Collection<ExecutionInterval> getIntervalsFrom(final ExecutionYear executionYear) {
//        return executionYear.getChildIntervals().stream().filter(ei -> isFor(ei)).collect(Collectors.toSet());
//    }

    public void delete() {
        setRoot(null);
        setDegreeCurricularPlan(null);
        getAcademicPeriodOrdersSet().clear();
        super.deleteDomainObject();
    }

    public static Optional<EnrolmentModelConfigEntry> findFor(final DegreeCurricularPlan degreeCurricularPlan,
            final AcademicPeriodOrder period) {
        return degreeCurricularPlan.getEnrolmentModelConfigEntriesSet().stream()
                .filter(entry -> entry.getAcademicPeriodOrdersSet().contains(period)).findAny();
    }
}
