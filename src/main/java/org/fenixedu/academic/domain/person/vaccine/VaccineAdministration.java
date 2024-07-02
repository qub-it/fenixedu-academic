package org.fenixedu.academic.domain.person.vaccine;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class VaccineAdministration extends VaccineAdministration_Base {

    private VaccineAdministration(VaccineType type, Person person) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setVaccineType(type);
        setPerson(person);
    }

    public static VaccineAdministration createOrUpdate(VaccineType type, Person person, DateTime validityLimit) {
        VaccineAdministration result =
                person.getVaccineAdministrationsSet().stream().filter(vA -> vA.getVaccineType() == type).findAny()
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
