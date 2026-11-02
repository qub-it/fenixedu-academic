package org.fenixedu.academic.domain.time.calendarStructure;

import java.util.Collection;
import java.util.Collections;

public class AcademicOtherPeriod extends AcademicPeriod {

    protected AcademicOtherPeriod(int period, String name, String code) {
        super(period, name, code);
    }

    @Override
    public float getWeight() {
        return 0.01f;
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
