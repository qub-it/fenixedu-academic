package org.fenixedu.academic.domain.time.calendarStructure;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AcademicDays extends AcademicPeriod {

    protected AcademicDays(int period, String name, String code) {
        super(period, name, code);
    }

    @Override
    public float getWeight() {
        return getValue() / 365f;
    }

    @Override
    public AcademicPeriod getPossibleChild() {
        return null;
    }

    @Override
    public Collection<AcademicPeriod> getPossibleChilds() {
        final Set<AcademicPeriod> result = new HashSet<>();

        return result;
    }

}
