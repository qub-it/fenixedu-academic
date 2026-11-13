package org.fenixedu.academic.domain.time.calendarStructure;

import java.util.Collection;
import java.util.Collections;

public class AcademicWeeks extends AcademicPeriod {

    protected AcademicWeeks(int period, String name, String code) {
        super(period, name, code);
    }

    @Override
    public float getWeight() {
        return getValue() / 52f;
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
