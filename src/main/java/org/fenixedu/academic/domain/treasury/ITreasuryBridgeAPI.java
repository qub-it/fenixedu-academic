package org.fenixedu.academic.domain.treasury;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.joda.time.LocalDate;

public interface ITreasuryBridgeAPI {

    // @formatter:off
    /* ---------------------------------
     * TREASURY INSTITUTION AND PRODUCTS
     * ---------------------------------
     */
    // @formatter:on

    public Set<ITreasuryInstitution> getTreasuryInstitutions();
    
    public ITreasuryInstitution getTreasuryInstitutionByFiscalNumber(final String fiscalNumber);
    
    public Set<ITreasuryProduct> getProducts(final ITreasuryInstitution treasuryInstitution);

    public ITreasuryProduct getProductByCode(final String code);

    // @formatter:off
    /* ------------------------
     * ACADEMIC SERVICE REQUEST
     * ------------------------
     */
    // @formatter:on

    public static String ACADEMIC_SERVICE_REQUEST_NEW_SITUATION_EVENT = "ACADEMIC_SERVICE_REQUEST_NEW_SITUATION_EVENT";
    public static String ACADEMIC_SERVICE_REQUEST_REJECT_OR_CANCEL_EVENT = "ACADEMIC_SERVICE_REQUEST_REJECT_OR_CANCEL_EVENT";

    public void registerNewAcademicServiceRequestSituationHandler();

    public void registerAcademicServiceRequestCancelOrRejectHandler();

    public IAcademicServiceRequestAndAcademicTaxTreasuryEvent academicTreasuryEventForAcademicServiceRequest(
            final AcademicServiceRequest academicServiceRequest);

    // @formatter:off
    /* ----------
     * ENROLMENTS
     * ----------
     */
    // @formatter:on

    public static String STANDALONE_ENROLMENT = "STANDALONE_ENROLMENT";
    public static String EXTRACURRICULAR_ENROLMENT = "EXTRACURRICULAR_ENROLMENT";
    public static String IMPROVEMENT_ENROLMENT = "IMPROVEMENT_ENROLMENT";
    public static String NORMAL_ENROLMENT = "NORMAL_ENROLMENT";

    public void registerStandaloneEnrolmentHandler();

    public void registerExtracurricularEnrolmentHandler();

    public void registerImprovementEnrolmentHandler();

    public void standaloneUnenrolment(final Enrolment standaloneEnrolment);

    public void extracurricularUnenrolment(final Enrolment extracurricularEnrolment);

    public void improvementUnrenrolment(final EnrolmentEvaluation improvementEnrolmentEvaluation);

    // @formatter:off
    /* --------
     * TUITIONS
     * --------
     */
    // @formatter:on

    public boolean isToPayTuition(final Registration registration, final ExecutionYear executionYear);

    public ITuitionTreasuryEvent getTuitionForRegistrationTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear);

    public ITuitionTreasuryEvent getTuitionForStandaloneTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear);

    public ITuitionTreasuryEvent getTuitionForExtracurricularTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear);

    public ITuitionTreasuryEvent getTuitionForImprovementTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear);

    // @formatter:off
    /* --------------
     * ACADEMIC TAXES
     * --------------
     */
    // @formatter:on

    public IImprovementTreasuryEvent getImprovementTaxTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear);

    public List<IAcademicTreasuryEvent> getAcademicTaxesList(final Registration registration, final ExecutionYear executionYear);

    // @formatter:off
    /* ------------------------
     * ACADEMIC TREASURY TARGET
     * ------------------------
     */
    // @formatter:on

    public IAcademicTreasuryEvent getAcademicTreasuryEventForTarget(final IAcademicTreasuryTarget target);

    public IAcademicTreasuryEvent createDebt(final ITreasuryInstitution treasuryInstitution, final Person person,
            final ITreasuryProduct product, final IAcademicTreasuryTarget target, final LocalDate entryDate,
            final LocalDate dueDate, final boolean applyInterestGlobalTax);

    public IAcademicTreasuryEvent createDebt(final ITreasuryInstitution treasuryInstitution, final Person person,
            final ITreasuryProduct product, final IAcademicTreasuryTarget target, final BigDecimal amount,
            final LocalDate dueDate, final boolean applyInterestGlobalTax);

    // @formatter:off
    /* --------------
     * ACADEMICAL ACT
     * --------------
     */
    // @formatter:on

    public boolean isAcademicalActsBlocked(final Person person, final LocalDate when);

    public boolean isAcademicalActBlockingSuspended(final Person person, final LocalDate when);

    // @formatter:off
    /* -----
     * OTHER
     * -----
     */
    // @formatter:on

    public List<IAcademicTreasuryEvent> getAllAcademicTreasuryEventsList(final Person person, final ExecutionYear executionYear);

    public List<IAcademicTreasuryEvent> getAllAcademicTreasuryEventsList(final Person person);

    // @formatter:off
    /* ------------------------------------
     * ACADEMIC TREASURY MODULE INTEGRATION
     * ------------------------------------
     */
    // @formatter:on

    public boolean isPersonAccountTreasuryManagementAvailable(final Person person);

    public String getPersonAccountTreasuryManagementURL(final Person person);

    public String getRegistrationAccountTreasuryManagementURL(Registration registration);

    public void createAcademicDebts(final Registration registration);

}
