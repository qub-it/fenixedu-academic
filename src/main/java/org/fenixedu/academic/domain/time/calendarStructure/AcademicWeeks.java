package org.fenixedu.academic.domain.time.calendarStructure;

import java.util.Collection;
import java.util.Collections;

import org.joda.time.DurationFieldType;
import org.joda.time.PeriodType;

public class AcademicWeeks extends AcademicPeriod {

    protected AcademicWeeks(int period, String name, String code) {
        super(period, name, code);
    }

    @Override
    public float getWeight() {
        return getValue() / 52f;
    }

    @Override
    public DurationFieldType getFieldType() {
        return DurationFieldType.weeks();
    }

    @Override
    public PeriodType getPeriodType() {
        return PeriodType.weeks();
    }

    @Override
    public AcademicPeriod getPossibleChild() {
        return null;
    }

    @Override
    public Collection<AcademicPeriod> getPossibleChilds() {
        return Collections.emptySet();
    }

}
