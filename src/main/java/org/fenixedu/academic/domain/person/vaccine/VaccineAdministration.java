package org.fenixedu.academic.domain.person.vaccine;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;

public class VaccineAdministration extends VaccineAdministration_Base {

    private VaccineAdministration(VaccineType type, Person person) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setVaccineType(type);
        setPerson(person);
    }

    public static VaccineAdministration createOrUpdate(VaccineType type, Person person, LocalDate administrationDate,
            LocalDate validityDate) {
        VaccineAdministration result = person.getVaccineAdministrationsSet().stream().filter(vA -> vA.getVaccineType() == type)
                .findAny().orElseGet(() -> new VaccineAdministration(type, person));
        result.setAdministrationDate(administrationDate);
        result.setValidityDate(validityDate);
        return result;
    }

    public void delete() {
        setPerson(null);
        setVaccineType(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }
}
