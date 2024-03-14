package org.fenixedu.academic.domain.student.curriculum.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class CreditsWeightedCalculator extends CreditsWeightedCalculator_Base {

    private CreditsWeightedCalculator() {
        super();
    }

    public static CreditsWeightedCalculator create(RoundingMode roundingMode, int numberOfDecimals) {
        final CreditsWeightedCalculator calculator = new CreditsWeightedCalculator();
        calculator.setRoundingMode(roundingMode);
        calculator.setNumberOfDecimals(numberOfDecimals);
        return calculator;
    }

    @Override
    public ConclusionGradeCalculatorResultsDTO calculate(final Curriculum curriculum) {
        BigDecimal avg = calculateAverage(curriculum);

        final GradeScale gradeScale = curriculum.getStudentCurricularPlan().getRegistration().getDegree().getNumericGradeScale();
        Grade unroundedGrade = Grade.createGrade(avg.toString(), gradeScale);
        Grade intermediateRoundedGrade =
                Grade.createGrade(avg.setScale(getNumberOfDecimals(), getRoundingMode()).toString(), gradeScale);
        Grade finalGrade = Grade
                .createGrade(intermediateRoundedGrade.getNumericValue().setScale(0, RoundingMode.HALF_UP).toString(), gradeScale);
        return new ConclusionGradeCalculatorResultsDTO(unroundedGrade, intermediateRoundedGrade, finalGrade);
    }

    private BigDecimal calculateAverage(final Curriculum curriculum) {
        BigDecimal sumOfGradesWeighted = BigDecimal.ZERO;
        BigDecimal sumOfWeights = BigDecimal.ZERO;

        List<ICurriculumEntry> numericCurriculumEntries =
                Stream.concat(curriculum.getEnrolmentRelatedEntries().stream(), curriculum.getDismissalRelatedEntries().stream())
                        .filter(entry -> entry.getGrade().isNumeric()).collect(Collectors.toList());
        for (ICurriculumEntry entry : numericCurriculumEntries) {
            final BigDecimal weight = entry.getWeigthForCurriculum();
            sumOfWeights = sumOfWeights.add(weight);
            sumOfGradesWeighted = sumOfGradesWeighted.add(weight.multiply(entry.getGrade().getNumericValue()));
        }

        if (sumOfWeights.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return sumOfGradesWeighted.divide(sumOfWeights, getNumberOfDecimals() * 2 + 1, getRoundingMode());
    }

    public void delete() {
        if (!getDegreeCurricularPlansSet().isEmpty()) {
            throw new DomainException("error.GradeCalculator.delete.impossible.withDCP");
        }
        super.setRootDomainObject(null);
        super.deleteDomainObject();
    }

//    @Override
    public void setNumberOfDecimals(int numberOfDecimals) {
        if (numberOfDecimals < 0) {
            throw new DomainException("error.conclusionGradeCalculator.creditsweightedCalculator.cantSetNegativeDecimals");
        }
        super.setNumberOfDecimals(numberOfDecimals);
    }

    public String getType() {
        return CreditsWeightedCalculator.getTypeI18N();
    }

    public static String getTypeI18N() {
        return BundleUtil.getString(Bundle.APPLICATION, "student.curriculum.calculator.creditsWeightedCalculator");
    }

}
