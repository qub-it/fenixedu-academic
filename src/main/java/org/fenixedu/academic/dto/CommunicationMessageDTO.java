package org.fenixedu.academic.dto;

import java.io.Serializable;

import org.fenixedu.academic.domain.Person;

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.locale.LocaleInformationProvider;
import com.qubit.terra.framework.tools.primitives.LocalizedString;

public class CommunicationMessageDTO implements Serializable {

    private LocalizedString subject;
    private LocalizedString contents;
    private Person person;

    public CommunicationMessageDTO() {
    }

    public CommunicationMessageDTO(LocalizedString subject, LocalizedString contents, Person person) {
        this.subject = subject;
        this.contents = contents;
        this.person = person;
    }

    public CommunicationMessageDTO(String subject, String contents, Person person) {
        this.person = person;

        LocalizedString localizedSubject = new LocalizedString();
        LocalizedString localizedContents = new LocalizedString();
        ServiceProvider.getService(LocaleInformationProvider.class).getAvailableLocales().forEach(locale -> {
            localizedSubject.setValue(locale, subject);
            localizedContents.setValue(locale, contents);
        });

        this.subject = localizedSubject;
        this.contents = localizedContents;
    }

    public LocalizedString getSubject() {
        return subject;
    }

    public LocalizedString getContents() {
        return contents;
    }

    public Person getPerson() {
        return person;
    }

}