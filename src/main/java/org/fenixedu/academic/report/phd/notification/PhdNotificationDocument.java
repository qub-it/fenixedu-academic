/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.report.phd.notification;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.postingRules.FixedAmountPR;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdParticipant;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyProcess;
import org.fenixedu.academic.domain.phd.notification.PhdNotification;
import org.fenixedu.academic.report.FenixReport;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.MultiLanguageString;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class PhdNotificationDocument extends FenixReport {

    private static final String DATE_FORMAT_PT = "dd/MM/yyyy";

    private static final String DATE_FORMAT_EN = "yyyy/MM/dd";

    private PhdNotification notification;

    private Locale language;

    public PhdNotificationDocument(PhdNotification notification, Locale language) {
        setNotification(notification);
        setLanguage(language);
        fillReport();
    }

    private PhdNotification getNotification() {
        return this.notification;
    }

    private void setNotification(PhdNotification notification) {
        this.notification = notification;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    public void setLanguage(Locale language) {
        this.language = language;
    }

    @Override
    protected void fillReport() {

        final PhdProgramCandidacyProcess candidacyProcess = getNotification().getCandidacyProcess();
        final Person person = candidacyProcess.getPerson();
        final PhdIndividualProgramProcess individualProgramProcess = candidacyProcess.getIndividualProgramProcess();

        addParameter("administrativeOfficeCoordinator", individualProgramProcess.getPhdProgram().getAdministrativeOffice()
                .getCoordinator().getProfile().getDisplayName());

        addParameter("name", person.getName());
        addParameter("address", person.getAddress());
        addParameter("areaCode", person.getAreaCode());
        addParameter("areaOfAreaCode", person.getAreaOfAreaCode());
        final ExecutionYear executionYear = individualProgramProcess.getExecutionYear();
        addParameter("programName", individualProgramProcess.getPhdProgram().getName(executionYear).getContent(getLanguage()));

        addParameter("processNumber", individualProgramProcess.getProcessNumber());

        final LocalDate whenRatified = candidacyProcess.getWhenRatified();

        addParameter("ratificationDate", whenRatified != null ? whenRatified.toString(getDateFormat()) : "");

        addParameter("insuranceFee", getInsuranceFee(individualProgramProcess));
        addParameter("registrationFee", getRegistrationFee(individualProgramProcess, whenRatified));

        addParameter("date", new LocalDate().toString(getDateFormat()));
        addParameter("notificationNumber", getNotification().getNotificationNumber());

        addGuidingsParameter(individualProgramProcess);

    }

    private void addGuidingsParameter(final PhdIndividualProgramProcess individualProgramProcess) {
        if (!individualProgramProcess.getGuidingsSet().isEmpty() && !individualProgramProcess.getAssistantGuidingsSet().isEmpty()) {
            addParameter("guidingsInformation", MessageFormat.format(getMessageFromResource(getClass().getName()
                    + ".full.guidings.template"), buildGuidingsInformation(individualProgramProcess.getGuidingsSet()),
                    buildGuidingsInformation(individualProgramProcess.getAssistantGuidingsSet())));
        } else if (!individualProgramProcess.getGuidingsSet().isEmpty()) {
            addParameter("guidingsInformation", MessageFormat.format(getMessageFromResource(getClass().getName()
                    + ".guidings.only.template"), buildGuidingsInformation(individualProgramProcess.getGuidingsSet())));
        } else {
            addParameter("guidingsInformation", "");
        }
    }

    private String buildGuidingsInformation(final Collection<PhdParticipant> guidings) {
        final StringBuilder result = new StringBuilder();
        List<PhdParticipant> guidingsList = new ArrayList<>(guidings);
        for (int i = 0; i < guidingsList.size(); i++) {
            final PhdParticipant guiding = guidingsList.get(i);
            result.append(guiding.getNameWithTitle());
            if (i == guidings.size() - 2) {
                result.append(" ").append(getMessageFromResource("label.and")).append(" ");
            } else {
                result.append(", ");
            }
        }

        if (result.length() > 0) {
            if (result.toString().endsWith(getMessageFromResource("label.and"))) {
                return result.substring(0, result.length() - getMessageFromResource("label.and").length());
            }

            if (result.toString().endsWith(", ")) {
                return result.substring(0, result.length() - 2);
            }
        }

        return result.toString();

    }

    private String getMessageFromResource(String key) {
        return BundleUtil.getString(Bundle.PHD, getLanguage(), key);
    }

    private String getDateFormat() {
        return getLanguage() == MultiLanguageString.pt ? DATE_FORMAT_PT : DATE_FORMAT_EN;
    }

    private String getRegistrationFee(final PhdIndividualProgramProcess individualProgramProcess, final LocalDate whenRatified) {
        return whenRatified != null ? ((FixedAmountPR) individualProgramProcess.getPhdProgram().getServiceAgreementTemplate()
                .findPostingRuleByEventTypeAndDate(EventType.PHD_REGISTRATION_FEE, whenRatified.toDateTimeAtMidnight()))
                .getFixedAmount().toPlainString() : "";
    }

    private String getInsuranceFee(final PhdIndividualProgramProcess individualProgramProcess) {
        return ((FixedAmountPR) Bennu
                .getInstance()
                .getInstitutionUnit()
                .getUnitServiceAgreementTemplate()
                .findPostingRuleBy(EventType.INSURANCE,
                        individualProgramProcess.getExecutionYear().getBeginDateYearMonthDay().toDateTimeAtMidnight(),
                        individualProgramProcess.getExecutionYear().getEndDateYearMonthDay().toDateTimeAtMidnight()))
                .getFixedAmount().toPlainString();
    }

    @Override
    public String getReportFileName() {
        return "Notification-" + getNotification().getNotificationNumber().replace("/", "-") + "-"
                + new DateTime().toString(YYYYMMDDHHMMSS);
    }

    @Override
    public String getReportTemplateKey() {
        return getClass().getName() + "." + getNotification().getType().name() + "." + getLanguage();
    }

}
