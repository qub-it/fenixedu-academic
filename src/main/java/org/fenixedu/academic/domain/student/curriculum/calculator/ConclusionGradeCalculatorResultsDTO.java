package org.fenixedu.academic.domain.student.curriculum.calculator;

import org.fenixedu.academic.domain.Grade;

public class ConclusionGradeCalculatorResultsDTO {

    private final Grade unroundedGrade;
    private final Grade intermediateRoundedGrade;
    private final Grade finalGrade;

    public ConclusionGradeCalculatorResultsDTO(Grade unroundedGrade, Grade intermediateRoundedGrade, Grade finalGrade) {
        this.unroundedGrade = unroundedGrade;
        this.intermediateRoundedGrade = intermediateRoundedGrade;
        this.finalGrade = finalGrade;
    }

    public Grade getUnroundedGrade() {
        return unroundedGrade;
    }

    public Grade getIntermediateRoundedGrade() {
        return intermediateRoundedGrade;
    }

    public Grade getFinalGrade() {
        return finalGrade;
    }
}
