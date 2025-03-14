package org.fenixedu.academic.domain.student.curriculum.calculator;

import org.fenixedu.academic.domain.Grade;

public class ConclusionGradeCalculatorResultsDTO {

    private final Grade unroundedGrade;
    private final Grade intermediateRoundedGrade;
    private final Grade finalGrade;

    private String description;

    public ConclusionGradeCalculatorResultsDTO(Grade unroundedGrade, Grade intermediateRoundedGrade, Grade finalGrade) {
        this.unroundedGrade = unroundedGrade;
        this.intermediateRoundedGrade = intermediateRoundedGrade;
        this.finalGrade = finalGrade;
        this.description = null;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
