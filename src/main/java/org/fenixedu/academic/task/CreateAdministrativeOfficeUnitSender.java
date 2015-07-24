package org.fenixedu.academic.task;

import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.util.email.UnitBasedSender;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CreateAdministrativeOfficeUnitSender extends CustomTask {

    @Override
    public void runTask() throws Exception {
        //Unit administrativeOfficeUnit = FenixFramework.getDomainObject("285241663029249");
        Unit administrativeOfficeUnit = AdministrativeOffice.readDegreeAdministrativeOffice().getUnit();
        if (administrativeOfficeUnit.getUnitBasedSenderSet().isEmpty()) {
            UnitBasedSender.newInstance(administrativeOfficeUnit);
        }
    }

}
