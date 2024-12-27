package org.fenixedu.academic.domain.person.personIdentifier;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class PersonIdentifier extends PersonIdentifier_Base {

    protected PersonIdentifier() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public static PersonIdentifier create(PersonIdentifierType type, String identifier, Person person) {
        PersonIdentifier id = new PersonIdentifier();

        id.setPerson(person);
        id.setType(type);
        id.setIdentifier(identifier);

        return id;
    }

    @Override
    public void setType(PersonIdentifierType type) {
        if (getPerson().getIdentifiersSet().stream().anyMatch(id -> id.getType() == type)) {
            throw new DomainException("error.person.personIdentifier.type");
        }
        super.setType(type);
    }

    @Override
    public void setIdentifier(String identifier) {
        Optional<PersonIdentifier> findByIdentifierAndType = findByIdentifierAndType(identifier, getType());
        if (findByIdentifierAndType.isPresent() && findByIdentifierAndType.get() != this) {
            throw new DomainException("error.person.personIdentifier.identifier");
        }
        super.setIdentifier(identifier);
    }

    public static Optional<PersonIdentifier> findByIdentifierAndType(String identifier, PersonIdentifierType type) {
        return type.getIdentifiersSet().stream().filter(pI -> StringUtils.equals(pI.getIdentifier(), identifier)).findAny();
    }

    public static Optional<Person> findPersonByIdentifierAndType(String identifier, PersonIdentifierType type) {
        return type.getIdentifiersSet().stream().filter(pI -> StringUtils.equals(pI.getIdentifier(), identifier))
                .map(i -> i.getPerson()).findAny();
    }

    public void delete() {
        setType(null);
        setPerson(null);
        setRootDomainObject(null);
        this.deleteDomainObject();
    }
}
