package org.fenixedu.academic.domain.person.vaccine;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class VaccineAdministration extends VaccineAdministration_Base {

    private VaccineAdministration() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    private VaccineAdministration(VaccineType type, Person person) {
        this();
        setVaccineType(type);
        setPerson(person);
    }

    private VaccineAdministration(VaccineType type, Person person, DateTime validationLimit) {
        this(type, person);
        setValidityLimit(validationLimit);
    }

    public static VaccineAdministration createOrUpdate(VaccineType type, Person person, DateTime validityLimit) {
        VaccineAdministration result =
                person.getVaccineAdministrationsSet().stream().filter(vA -> vA.getVaccineType().equals(type)).findAny()
                        .orElseGet(() -> new VaccineAdministration(type, person));
        result.setValidityLimit(validityLimit);
        return result;
    }

    public void delete() {
        setPerson(null);
        setVaccineType(null);
        setValidityLimit(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }
}
