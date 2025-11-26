package org.fenixedu.academic.domain.treasury;

import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.joda.time.LocalDate;

@Deprecated
public interface ITreasuryBridgeAPI {

    // @formatter:off
    /* ----------
     * ENROLMENTS
     * ----------
     */
    // @formatter:on


    @Deprecated
    public void standaloneUnenrolment(Enrolment standaloneEnrolment);

    @Deprecated
    public void extracurricularUnenrolment(Enrolment extracurricularEnrolment);

    // @formatter:off
    /* --------------
     * ACADEMIC TAXES
     * --------------
     */
    // @formatter:on

    @Deprecated
    public IImprovementTreasuryEvent getImprovementTaxTreasuryEvent(Registration registration, ExecutionYear executionYear);

    // @formatter:off
    /* --------------
     * ACADEMICAL ACT
     * --------------
     */
    // @formatter:on

    @Deprecated
    public boolean isAcademicalActsBlocked(Person person, LocalDate when);

    // @formatter:off
    /* -----
     * OTHER
     * -----
     */
    // @formatter:on

    @Deprecated
    public List<IAcademicTreasuryEvent> getAllAcademicTreasuryEventsList(Person person);

    // @formatter:off
    /* ------------------------------------
     * ACADEMIC TREASURY MODULE INTEGRATION
     * ------------------------------------
     */
    // @formatter:on

    @Deprecated
    public String getPersonAccountTreasuryManagementURL(Person person);

    @Deprecated
    public String getRegistrationAccountTreasuryManagementURL(Registration registration);

    @Deprecated
    public boolean isValidFiscalNumber(String fiscalAddressCountryCode, String fiscalNumber);

    @Deprecated
    public boolean updateCustomer(Person person, String fiscalAddressCountryCode, String fiscalNumber);

    @Deprecated
    public boolean createCustomerIfMissing(Person person);

    @Deprecated
    public void saveFiscalAddressFieldsFromPersonInActiveCustomer(Person person);

    @Deprecated
    public PhysicalAddress createSaftDefaultPhysicalAddress(Person person);

}
