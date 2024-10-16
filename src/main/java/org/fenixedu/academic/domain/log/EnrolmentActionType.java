package org.fenixedu.academic.domain.log;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public enum EnrolmentActionType {
    
    ENROL, UNENROL, ANNUL_ENROLMENT, ACTIVATE_ENROLMENT;
    
    public String getName() {
        return name();
    }

    public String getLocalizedName() {
        return BundleUtil.getString(Bundle.ENUMERATION, name());
    }
}
