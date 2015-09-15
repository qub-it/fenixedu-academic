package org.fenixedu.academic.task;

import org.fenixedu.academic.domain.contacts.ContactRoot;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ValidateAllPendingPartyContacts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        for (PartyContact contact : ContactRoot.getInstance().getPartyContactsSet()) {
            if (contact.waitsValidation()) {
                contact.setValid();
            }
        }
    }

}
