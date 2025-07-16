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
        if (getPerson().getIdentifiersSet().stream().anyMatch(id -> id.getType() == type && id != this)) {
            throw new DomainException("error.person.personIdentifier.type");
        }
        super.setType(type);
    }

    @Override
    public void setIdentifier(String identifier) {
        String regexExpression = getType().getExpression();
        if (!StringUtils.isBlank(regexExpression) && !identifier.matches(regexExpression)) {
            throw new DomainException("error.person.personIdentifier.identifier.invalidFormat");
        }

        Optional<PersonIdentifier> findByIdentifierAndType = findByIdentifierAndType(identifier, getType());
        if (findByIdentifierAndType.isPresent() && findByIdentifierAndType.get() != this) {
            throw new DomainException("error.person.personIdentifier.identifier");
        }
        super.setIdentifier(identifier);
    }

    public static Optional<PersonIdentifier> findByIdentifierAndType(String identifier, PersonIdentifierType type) {
        return type.getIdentifiersSet().stream().filter(pI -> StringUtils.equals(pI.getIdentifier(), identifier)).findAny();
    }

    public static Optional<Person> findPerson(String identifier, PersonIdentifierType type) {
        return findByIdentifierAndType(identifier, type).map(i -> i.getPerson());
    }

    public static Optional<PersonIdentifier> findByTypeAndPerson(PersonIdentifierType type, Person person) {
        return person.getIdentifiersSet().stream().filter(i -> i.getType() == type).findAny();
    }

    public static void updateIdentifier(PersonIdentifierType type, Person person, String value) {
        PersonIdentifier.findByTypeAndPerson(type, person).ifPresentOrElse(id -> {
            if (StringUtils.isBlank(value)) {
                id.delete();
            } else {
                id.setIdentifier(value);
            }
        }, () -> {
            if (StringUtils.isNotBlank(value)) {
                PersonIdentifier.create(type, value, person);
            }
        });
    }

    public void delete() {
        setType(null);
        setPerson(null);
        setRootDomainObject(null);
        this.deleteDomainObject();
    }
}
