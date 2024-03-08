package org.fenixedu.academic.domain.student.curriculum.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class CreditsWeightedCalculator extends CreditsWeightedCalculator_Base {

    private CreditsWeightedCalculator() {
        super();
    }

    public static CreditsWeightedCalculator create(RoundingMode roundingMode, BigDecimal numberOfDecimals) {
        final CreditsWeightedCalculator calculator = new CreditsWeightedCalculator();

        calculator.setRoundingMode(roundingMode);
        calculator.setNumberOfDecimals(numberOfDecimals);

        return calculator;
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
