package org.fenixedu.academic.domain.person.pedagogicalTraining;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class PedagogicalTraining extends PedagogicalTraining_Base {
    
    protected PedagogicalTraining() {
        super();
        setRoot(Bennu.getInstance());
    }


    public static PedagogicalTraining create (LocalizedString description, Person person) {
        final PedagogicalTraining pedagogicalTraining = new PedagogicalTraining();

        pedagogicalTraining.setDescription(description);
        pedagogicalTraining.setPerson(person);

        return pedagogicalTraining;
    }

    @Override
    public void setDescription(final LocalizedString description) {
        if (description == null || description.isEmpty()) {
            throw new DomainException("pedagogicalTraining.description.cannot.be.null");
        }
        super.setDescription(description);
    }

    public void delete() {
        setPerson(null);
        setRoot(null);
        this.deleteDomainObject();
    }
    
}
