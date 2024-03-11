package org.fenixedu.academic.domain.student.curriculum.calculator;

import org.fenixedu.academic.domain.Grade;

public class ConclusionGradeCalculatorResultsDTO {

    private final Grade rawGrade;
    private final Grade unroundedGrade;
    private final Grade finalGrade;

    public ConclusionGradeCalculatorResultsDTO(Grade rawGrade, Grade unroundedGrade, Grade finalGrade) {
        this.rawGrade = rawGrade;
        this.unroundedGrade = unroundedGrade;
        this.finalGrade = finalGrade;
    }

    public Grade getRawGrade() {
        return rawGrade;
    }

    public Grade getUnroundedGrade() {
        return unroundedGrade;
    }

    public Grade getFinalGrade() {
        return finalGrade;
    }
}
