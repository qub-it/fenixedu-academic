package org.fenixedu.academic.domain.person;

import org.fenixedu.commons.i18n.LocalizedString;

public class VaccineType extends VaccineType_Base {

    public VaccineType(LocalizedString name) {
        super();
        setName(name);
    }

    public void delete() {
        setName(null);
        deleteDomainObject();
    }

}
