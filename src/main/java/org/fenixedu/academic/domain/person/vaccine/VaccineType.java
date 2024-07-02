package org.fenixedu.academic.domain.person.vaccine;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class VaccineType extends VaccineType_Base {

    public VaccineType(LocalizedString name) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setName(name);
    }

    public void delete() {
        setName(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

}
