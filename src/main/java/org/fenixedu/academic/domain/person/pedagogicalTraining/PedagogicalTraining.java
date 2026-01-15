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
        if (description == null || description.isEmpty()) {
            throw new DomainException("pedagogicalTraining.description.cannot.be.null");
        }
        final PedagogicalTraining pedagogicalTraining = new PedagogicalTraining();

        pedagogicalTraining.setPerson(person);
        pedagogicalTraining.setDescription(description);

        return pedagogicalTraining;
    }

    public void delete() {
        setPerson(null);
        setRoot(null);
        this.deleteDomainObject();
    }
    
}
