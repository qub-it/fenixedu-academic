package org.fenixedu.academic.domain.student.curriculum.calculator;

import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;

public abstract class ConclusionGradeCalculator extends ConclusionGradeCalculator_Base {

    public ConclusionGradeCalculator() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static Stream<ConclusionGradeCalculator> readAll() {
        return Bennu.getInstance().getConclusionGradeCalculatorSet().stream();
    }

    public abstract void delete();

    public abstract String getType();

}
