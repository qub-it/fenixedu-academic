package org.fenixedu.academic.domain.person.pedagogicalTraining;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class PedagogicalTraining extends PedagogicalTraining_Base {
    
    protected PedagogicalTraining() {
        super();
        setRoot(Bennu.getInstance());
    }

    protected PedagogicalTraining (LocalizedString description, Person person) {
        final PedagogicalTraining pedagogicalTraining = new PedagogicalTraining();

        pedagogicalTraining.setPerson(person);
        pedagogicalTraining.setDescription(description);
    }

    public static PedagogicalTraining create (LocalizedString description, Person person) {
        return new PedagogicalTraining(description, person);
    }

    public static Stream<PedagogicalTraining> findAllForPerson (Person person) {
        return person.getPedagogicalTrainingsSet().stream();
    }

    public void delete() {
        setPerson(null);
        setRoot(null);
        this.deleteDomainObject();
    }
    
}
