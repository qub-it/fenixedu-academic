package org.fenixedu.academic.domain.student.curriculum.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

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

    public static CreditsWeightedCalculator create(RoundingMode roundingMode, Integer numberOfDecimals) {
        final CreditsWeightedCalculator calculator = new CreditsWeightedCalculator();

        calculator.setRoundingMode(roundingMode);
        calculator.setNumberOfDecimals(numberOfDecimals == null ? 0 : numberOfDecimals);

        return calculator;
    }

    @Override
    public ConclusionGradeCalculatorResultsDTO calculate(final Curriculum curriculum) {
        BigDecimal avg = calculateAverage(curriculum);
        GradeScale gradeScale = curriculum.getStudentCurricularPlan().getRegistration().getDegree().getNumericGradeScale();

        Grade unroundedGrade = Grade.createGrade(avg.toString(), gradeScale);
        Grade rawGrade = Grade.createGrade(avg.setScale(getNumberOfDecimals(), getRoundingMode()).toString(), gradeScale);
        Grade finalGrade = Grade.createGrade(avg.setScale(0, getRoundingMode()).toString(), gradeScale);
        return new ConclusionGradeCalculatorResultsDTO(rawGrade, unroundedGrade, finalGrade);
    }

    private BigDecimal calculateAverage(final Curriculum curriculum) {
        BigDecimal sumOfGradesWeighted = BigDecimal.ZERO;
        BigDecimal sumOfWeights = BigDecimal.ZERO;

        countAverage(curriculum.getEnrolmentRelatedEntries(), sumOfGradesWeighted, sumOfWeights);
        countAverage(curriculum.getDismissalRelatedEntries(), sumOfGradesWeighted, sumOfWeights);

        if (sumOfWeights.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sumOfGradesWeighted.divide(sumOfWeights, getNumberOfDecimals() * 2 + 1, getRoundingMode());
    }

    private void countAverage(final Set<ICurriculumEntry> entries, BigDecimal sumOfGradesWeighted, BigDecimal sumOfWeights) {
        for (final ICurriculumEntry entry : entries) {
            if (entry.getGrade().isNumeric()) {
                final BigDecimal weigth = entry.getWeigthForCurriculum();
                sumOfWeights = sumOfWeights.add(weigth);
                sumOfGradesWeighted =
                        sumOfGradesWeighted.add(entry.getWeigthForCurriculum().multiply(entry.getGrade().getNumericValue()));
            }
        }
    }

    public void delete() {
        if (!getDegreeCurricularPlansSet().isEmpty()) {
            throw new DomainException("error.GradeCalculator.delete.impossible.withDCP");
        }
        super.setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public String getType() {
        return CreditsWeightedCalculator.getTypeI18N();
    }

    public static String getTypeI18N() {
        return BundleUtil.getString(Bundle.APPLICATION, "student.curriculum.calculator.creditsWeightedCalculator");
    }

}
