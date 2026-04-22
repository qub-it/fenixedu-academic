package org.fenixedu.academic.domain.person.identificationDocument.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class IdentificationDocumentIdentityCardValidator implements IdentificationDocumentExtraInfoValidator{

    @Override
    public void validate(final String extraInfo, final String identificationDocumentValue) {
        if (extraInfo != null && !extraInfo.isEmpty()) {
            if (extraInfo.length() == 1){
                validateBI(extraInfo, identificationDocumentValue);
            } else {
                validateCC(extraInfo, identificationDocumentValue);
            }
        }
    }

    public void validateCC(final String extraInfo, final String identificationDocumentValue) {
        Pattern pattern = Pattern.compile("^[0-9][A-Z,0-9][A-Z,0-9][0-9]$");
        Matcher matcher = pattern.matcher(extraInfo);
        if (matcher.matches()) {
            if (!isValidCC(identificationDocumentValue + extraInfo)) {
                throw new DomainException("label.identificationDocumentSeriesNumber.invalid");
            }
        } else {
            throw new DomainException("label.identificationDocumentSeriesNumber.invalid.format");
        }
    }

    public void validateBI(final String extraInfo, final String identificationDocumentValue) {
        if (!StringUtils.isNumeric(extraInfo)) {
            throw new DomainException("label.identificationDocumentExtraDigit.invalid.format");
        }
        if (!isValidBI(identificationDocumentValue + extraInfo)) {
            throw new DomainException("label.identificationDocumentExtraDigit.invalid");
        }
    }

    final static int[] factor = new int[] { 9, 8, 7, 6, 5, 4, 3, 2 };
    private static boolean isValidBI(final String num) {
        final int l = num.length();
        if (l == 9) {
            int sum = 0;
            for (int i = 0; i < l - 1; sum += toInt(num.charAt(i)) * factor[i++]);
            int checkDigit = toInt(num.charAt(l - 1));
            final int mod = sum % 11;
            return mod == 0 || mod == 1 ? checkDigit == 0 : checkDigit == 11 - mod;
        }
        return l < 9 && isValidBI("0" + num);
    }

    private static boolean isValidCC(final String num) {
        final int l = num.length();
        if (l == 12) {
            int sum = 0;
            for (int i = 0; i < l; i++, i++) {
                final char c0 = num.charAt(i);
                final char c1 = num.charAt(i + 1);

                if (i != 8 && !Character.isDigit(c1)) {
                    return false;
                }
                if (i != 10 && !Character.isDigit(c0)) {
                    return false;
                }

                final int d0 = toInt(c0) * 2;
                final int d1 = toInt(c1);

                final int d09 = d0 > 9 ? d0 - 9 : d0;
                sum += d09 + d1;
            }
            return sum % 10 == 0;
        }
        return false;
    }

    private static int toInt(final char c) {
        return Character.isDigit(c) ? Character.getNumericValue(c) : ((int) c) - ((int) 'A') + 10;
    }

    @Override
    public String getLocalizedName() {
        return BundleUtil.getString(Bundle.APPLICATION, getClass().getName());
    }
}
