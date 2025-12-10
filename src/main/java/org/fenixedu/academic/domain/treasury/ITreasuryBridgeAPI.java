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


    // Used by StudentCurricularPlanNoCourseGroupEnrolmentManager
    public void standaloneUnenrolment(Enrolment standaloneEnrolment);

    // Used by StudentCurricularPlanNoCourseGroupEnrolmentManager
    public void extracurricularUnenrolment(Enrolment extracurricularEnrolment);

    // @formatter:off
    /* ------------------------------------
     * ACADEMIC TREASURY MODULE INTEGRATION
     * ------------------------------------
     */
    // @formatter:on

    // Used by PartySocialSecurityNumber
    public boolean isValidFiscalNumber(String fiscalAddressCountryCode, String fiscalNumber);

    // Used by PartySocialSecurityNumber
    public boolean updateCustomer(Person person, String fiscalAddressCountryCode, String fiscalNumber);

    // Used by PartySocialSecurityNumber
    public void saveFiscalAddressFieldsFromPersonInActiveCustomer(Person person);

}
