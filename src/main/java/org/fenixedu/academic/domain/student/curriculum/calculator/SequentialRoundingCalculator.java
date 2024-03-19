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

public class SequentialRoundingCalculator extends ConclusionGradeCalculator {//extends SequentialRoundingCalculator_Base {

    public static final int NUMBER_OF_DECIMALS = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private SequentialRoundingCalculator() {
        super();
    }

    public static SequentialRoundingCalculator create() {
        return new SequentialRoundingCalculator();
    }

    @Override
    public ConclusionGradeCalculatorResultsDTO calculate(final Curriculum curriculum) {
        BigDecimal avg = calculateAverage(curriculum);

        final GradeScale gradeScale = curriculum.getStudentCurricularPlan().getDegree().getNumericGradeScale();
        Grade unroundedGrade = Grade.createGrade(avg.toString(), gradeScale);
        Grade intermediateRoundedGrade =
                Grade.createGrade(avg.setScale(NUMBER_OF_DECIMALS, ROUNDING_MODE).toString(), gradeScale);
        Grade finalGrade = Grade.createGrade(sequentiallyRound(avg, 0).toString(), gradeScale);
        return new ConclusionGradeCalculatorResultsDTO(unroundedGrade, intermediateRoundedGrade, finalGrade);
    }

    private BigDecimal sequentiallyRound(BigDecimal grade, int toScale) {
        if (grade.scale() <= toScale) {
            return grade;
        }
        return sequentiallyRound(grade.setScale(grade.scale() - 1, ROUNDING_MODE), toScale);
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
        return sumOfGradesWeighted.divide(sumOfWeights, 2 * 2 + 1, RoundingMode.HALF_UP);
    }

    @Override
    public void delete() {
        if (!getDegreeCurricularPlansSet().isEmpty()) {
            throw new DomainException("error.GradeCalculator.delete.impossible.withDCP");
        }
        super.setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public String getType() {
        return getTypeI18N();
    }

    public static String getTypeI18N() {
        return BundleUtil.getString(Bundle.APPLICATION, "student.curriculum.calculator.sequentialRoundingCalculator");
    }

}
