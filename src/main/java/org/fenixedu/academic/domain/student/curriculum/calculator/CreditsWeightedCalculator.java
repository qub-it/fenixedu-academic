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
        //FIXME: doesn't accept zero, even if the gradescale accepts it, it says invalid grade
        Grade rawGrade = Grade.createGrade(avg.setScale(getNumberOfDecimals(), getRoundingMode()).toString(), gradeScale);
        Grade finalGrade = Grade.createGrade(avg.setScale(0, getRoundingMode()).toString(), gradeScale);
        return new ConclusionGradeCalculatorResultsDTO(rawGrade, unroundedGrade, finalGrade);
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

        if (sumOfWeights.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(getNumberOfDecimals(), getRoundingMode());
            //FIXME: shouldn't it be the bottom of the gradescale of the degreeCP?
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

    public String getType() {
        return CreditsWeightedCalculator.getTypeI18N();
    }

    public static String getTypeI18N() {
        return BundleUtil.getString(Bundle.APPLICATION, "student.curriculum.calculator.creditsWeightedCalculator");
    }

}
