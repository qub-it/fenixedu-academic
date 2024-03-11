package org.fenixedu.academic.domain.student.curriculum.calculator;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.domain.Bennu;

public abstract class ConclusionGradeCalculator extends ConclusionGradeCalculator_Base {

    protected ConclusionGradeCalculator() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static Stream<ConclusionGradeCalculator> findAll() {
        return Bennu.getInstance().getConclusionGradeCalculatorSet().stream();
    }

    public abstract ConclusionGradeCalculatorResultsDTO calculate(final Curriculum curriculum);

    public abstract void delete();

    public abstract String getType();

}
