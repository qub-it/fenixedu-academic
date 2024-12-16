package org.fenixedu.academic.domain.person.personIdentifier;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class PersonIdentifier extends PersonIdentifier_Base {

    public PersonIdentifier() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    @Override
    public void setIdentifier(String identifier) {
        Optional<PersonIdentifier> findByIdentifierAndType = findByIdentifierAndType(identifier, getType());
        if (findByIdentifierAndType.isPresent() && findByIdentifierAndType != null) {
            throw new DomainException("error.person.personIdentifier.identifier");
        }
        super.setIdentifier(identifier);
    }

    public static Optional<PersonIdentifier> findByIdentifierAndType(String identifier, PersonIdentifierType type) {
        return Bennu.getInstance().getPersonIdentifiersSet().stream()
                .filter(pI -> StringUtils.equals(pI.getIdentifier(), identifier) && pI.getType() == type).findAny();
    }

    public void delete() {
        setRootDomainObject(null);
        this.deleteDomainObject();
    }
}
