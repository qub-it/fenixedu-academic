package org.fenixedu.academic.domain.person;

import org.fenixedu.academic.domain.Person;
import org.joda.time.DateTime;

public class VaccineAdministration extends VaccineAdministration_Base {

    private VaccineAdministration(VaccineType type, Person person, DateTime validationLimit) {
        super();
        setVaccineType(type);
        setPerson(person);
        setValidityLimit(validationLimit);
    }

    public VaccineAdministration create(VaccineType type, Person person, DateTime validityLimit) {
        VaccineAdministration result = person.getVaccineAdministrationsSet().stream()
                .filter(vA -> vA.getVaccineType().equals(type)).findAny().orElse(null);

        if (result != null) {
            result.setValidityLimit(validityLimit);
        } else {
            result = new VaccineAdministration(type, person, validityLimit);
        }

        return result;
    }

    public void delete() {
        setPerson(null);
        setVaccineType(null);
        setValidityLimit(null);
        super.deleteDomainObject();
    }
}
