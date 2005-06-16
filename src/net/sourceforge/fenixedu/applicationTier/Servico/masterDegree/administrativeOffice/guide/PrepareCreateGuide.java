/*
 * Created on 21/Mar/2003
 *  
 */
package net.sourceforge.fenixedu.applicationTier.Servico.masterDegree.administrativeOffice.guide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.FenixServiceException;
import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.NonExistingContributorServiceException;
import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.NonExistingServiceException;
import net.sourceforge.fenixedu.dataTransferObject.InfoExecutionDegree;
import net.sourceforge.fenixedu.dataTransferObject.InfoGuide;
import net.sourceforge.fenixedu.dataTransferObject.InfoGuideEntry;
import net.sourceforge.fenixedu.dataTransferObject.InfoGuideWithPersonAndExecutionDegreeAndContributor;
import net.sourceforge.fenixedu.domain.Contributor;
import net.sourceforge.fenixedu.domain.DocumentType;
import net.sourceforge.fenixedu.domain.ExecutionDegree;
import net.sourceforge.fenixedu.domain.GraduationType;
import net.sourceforge.fenixedu.domain.Guide;
import net.sourceforge.fenixedu.domain.IContributor;
import net.sourceforge.fenixedu.domain.IExecutionDegree;
import net.sourceforge.fenixedu.domain.IGuide;
import net.sourceforge.fenixedu.domain.IMasterDegreeCandidate;
import net.sourceforge.fenixedu.domain.IPrice;
import net.sourceforge.fenixedu.domain.IStudent;
import net.sourceforge.fenixedu.domain.IStudentCurricularPlan;
import net.sourceforge.fenixedu.domain.degree.DegreeType;
import net.sourceforge.fenixedu.domain.masterDegree.GuideRequester;
import net.sourceforge.fenixedu.domain.studentCurricularPlan.Specialization;
import net.sourceforge.fenixedu.persistenceTier.ExcepcaoPersistencia;
import net.sourceforge.fenixedu.persistenceTier.ISuportePersistente;
import net.sourceforge.fenixedu.persistenceTier.PersistenceSupportFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import pt.utl.ist.berserk.logic.serviceManager.IService;

/**
 * @author Nuno Nunes (nmsn@rnl.ist.utl.pt) Joana Mota (jccm@rnl.ist.utl.pt)
 */
public class PrepareCreateGuide implements IService {

    public InfoGuide run(String graduationType, InfoExecutionDegree infoExecutionDegree, Integer number,
            String requesterType, Integer contributorNumber, String contributorName,
            String contributorAddress) throws FenixServiceException {

        ISuportePersistente sp = null;
        IContributor contributor = null;
        IMasterDegreeCandidate masterDegreeCandidate = null;
        IGuide guide = new Guide();
        InfoGuide infoGuide = new InfoGuideWithPersonAndExecutionDegreeAndContributor();

        // Read the Contributor
        try {
            sp = PersistenceSupportFactory.getDefaultPersistenceSupport();
            contributor = sp.getIPersistentContributor().readByContributorNumber(contributorNumber);

            if ((contributor == null)
                    && ((contributorAddress == null) || (contributorAddress.length() == 0)
                            || (contributorName.length() == 0) || (contributorName == null))) {
                throw new NonExistingContributorServiceException();
            }

            if ((contributor == null) && (contributorAddress != null)
                    && (contributorAddress.length() != 0) && (contributorName.length() != 0)
                    && (contributorName != null)) {

                // Create the Contributor
                contributor = new Contributor();
                sp.getIPersistentContributor().simpleLockWrite(contributor);
                contributor.setContributorNumber(contributorNumber);
                contributor.setContributorAddress(contributorAddress);
                contributor.setContributorName(contributorName);
            }
        } catch (ExcepcaoPersistencia ex) {
            FenixServiceException newEx = new FenixServiceException("Persistence layer error", ex);
            throw newEx;
        }

        Integer year = null;
        Calendar calendar = Calendar.getInstance();
        year = new Integer(calendar.get(Calendar.YEAR));

        IExecutionDegree executionDegree = null;
        try {
            executionDegree = (IExecutionDegree) sp.getIPersistentExecutionDegree().readByOID(
                    ExecutionDegree.class, infoExecutionDegree.getIdInternal());
        } catch (ExcepcaoPersistencia ex) {
            FenixServiceException newEx = new FenixServiceException("Persistence layer error", ex);
            throw newEx;
        }

        // Check if the Requester is a Candidate
        if (requesterType.equals(GuideRequester.CANDIDATE.name())) {

            try {
                masterDegreeCandidate = sp.getIPersistentMasterDegreeCandidate()
                        .readByNumberAndExecutionDegreeAndSpecialization(number, executionDegree.getIdInternal(),
                                Specialization.valueOf(graduationType));
            } catch (ExcepcaoPersistencia ex) {
                FenixServiceException newEx = new FenixServiceException("Persistence layer error", ex);
                throw newEx;
            }

            // Check if the Candidate Exists
            if (masterDegreeCandidate == null)
                throw new NonExistingServiceException("O Candidato", null);

            // Get the price for the Candidate Application
            IPrice price = null;
            try {
                //FIXME to be removed when the descriptions in the DB are changed to keys to resource bundles 
                String description = getDescription(graduationType);
                
                price = sp.getIPersistentPrice().readByGraduationTypeAndDocumentTypeAndDescription(
                        GraduationType.MASTER_DEGREE, DocumentType.APPLICATION_EMOLUMENT,
                        description);
            } catch (ExcepcaoPersistencia ex) {
                FenixServiceException newEx = new FenixServiceException("Persistence layer error");
                newEx.fillInStackTrace();
                throw newEx;
            }

            if (price == null) {
                throw new FenixServiceException("Unkown Application Price");
            }

            guide.setContributor(contributor);
            guide.setPerson(masterDegreeCandidate.getPerson());
            guide.setYear(year);
            guide.setTotal(price.getPrice());

            guide.setCreationDate(calendar.getTime());
            guide.setVersion(new Integer(1));
            guide.setExecutionDegree(executionDegree);

            infoGuide = InfoGuideWithPersonAndExecutionDegreeAndContributor.newInfoFromDomain(guide);

            InfoGuideEntry infoGuideEntry = new InfoGuideEntry();
            infoGuideEntry.setDescription(price.getDescription());
            infoGuideEntry.setDocumentType(price.getDocumentType());
            infoGuideEntry.setGraduationType(price.getGraduationType());
            infoGuideEntry.setInfoGuide(infoGuide);
            infoGuideEntry.setPrice(price.getPrice());
            infoGuideEntry.setQuantity(new Integer(1));

            List infoGuideEntries = new ArrayList();
            infoGuideEntries.add(infoGuideEntry);

            infoGuide.setInfoGuideEntries(infoGuideEntries);
            infoGuide.setGuideRequester(GuideRequester.CANDIDATE);

        }

        if (requesterType.equals(GuideRequester.STUDENT.name())) {
            IStudent student = null;

            try {
                student = sp.getIPersistentStudent().readStudentByNumberAndDegreeType(number,
                        DegreeType.MASTER_DEGREE);
                if (student == null)
                    throw new NonExistingServiceException("O Aluno", null);

                final Integer degreeCurricularPlanID = executionDegree.getDegreeCurricularPlan().getIdInternal();
                List studentCurricularPlanList = (List) CollectionUtils.select(student.getStudentCurricularPlans(),new Predicate(){
                    
                    public boolean evaluate(Object arg0) {
                        IStudentCurricularPlan scp = (IStudentCurricularPlan) arg0;
                        return scp.getDegreeCurricularPlan().getIdInternal().equals(degreeCurricularPlanID);
                    }});
                //TODO ver bem isto
               
//                List studentCurricularPlanList = sp.getIStudentCurricularPlanPersistente()
//                        .readAllByStudentAndDegreeCurricularPlan(student,
//                                executionDegree.getDegreeCurricularPlan());

                // check if student curricular plan contains selected execution
                // degree
                if (studentCurricularPlanList.isEmpty()) {
                    throw new NonExistingServiceException("O Aluno", null);
                }
            } catch (ExcepcaoPersistencia ex) {

                FenixServiceException newEx = new FenixServiceException("Persistence layer error");
                newEx.fillInStackTrace();
                throw newEx;
            }

            // Check if the Candidate Exists
            if (student == null)
                throw new NonExistingServiceException("O Aluno", null);

            guide.setContributor(contributor);
            guide.setPerson(student.getPerson());
            guide.setYear(year);

            guide.setCreationDate(calendar.getTime());
            guide.setVersion(new Integer(1));

            guide.setExecutionDegree(executionDegree);

            infoGuide = InfoGuideWithPersonAndExecutionDegreeAndContributor.newInfoFromDomain(guide);

            infoGuide.setInfoGuideEntries(new ArrayList());
            infoGuide.setGuideRequester(GuideRequester.STUDENT);
        }

        return infoGuide;
    }

    private String getDescription(String graduationType) {
        
        switch (Specialization.valueOf(graduationType)) {
        case MASTER_DEGREE:
            return "Mestrado";
        case INTEGRATED_MASTER_DEGREE:
            return "Integrado";
        case SPECIALIZATION:
            return "Especialização";
        }
        
        return null;
        
    }

}