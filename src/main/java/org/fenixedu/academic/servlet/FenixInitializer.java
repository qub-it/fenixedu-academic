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
package org.fenixedu.academic.servlet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.mail.Session;
import javax.mail.Transport;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.fenixedu.academic.FenixEduAcademicConfiguration;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodReference;
import org.fenixedu.academic.domain.ProfessionalSituationConditionType;
import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.dml.DynamicFieldDescriptor;
import org.fenixedu.academic.domain.dml.DynamicFieldTag;
import org.fenixedu.academic.domain.organizationalStructure.UnitNamePart;
import org.fenixedu.academic.domain.raides.DegreeClassification;
import org.fenixedu.academic.domain.schedule.lesson.ExecutionDegreeLessonPeriod;
import org.fenixedu.academic.domain.schedule.lesson.LessonPeriod;
import org.fenixedu.academic.domain.student.personaldata.EducationLevelType;
import org.fenixedu.academic.domain.student.personaldata.ProfessionCategoryType;
import org.fenixedu.academic.domain.student.personaldata.ProfessionalStatusType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.api.SystemResource;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.rest.Healthcheck;
import org.fenixedu.commons.i18n.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qubit.terra.framework.services.logging.Log;
import com.sun.mail.smtp.SMTPTransport;

import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.RequestChecksumFilter;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.RequestChecksumFilter.ChecksumPredicate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@WebListener
public class FenixInitializer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(FenixInitializer.class);

    public static final String SEND_EMAIL_SIGNAL = "email.send.SystemSenderEmail";

    @Override
    @Atomic(mode = TxMode.READ)
    public void contextInitialized(ServletContextEvent event) {

        Installation.ensureInstallation();
        loadUnitNames();

        registerChecksumFilterRules();

        registerHealthchecks();

        initializeAcademicPeriodOrder();

        initializeDynamicFieldTags();

        initializeLessonPeriods();

        initializeExecutionDegreeLessonPeriods();

        initializeEducationLevelTypes();
        initializeProfessionCategoryTypes();
        initializeProfessionalStatusTypes();
        migratePidData();
        migratePrecedentDegreeInformationData();
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeDynamicFieldTags() {
        Log.warn("---------------------------------------");
        Log.warn("Starting population of DynamicFieldTag");
        Log.warn("---------------------------------------");
        long start = System.currentTimeMillis();

        Set<DynamicFieldDescriptor> dynamicFieldDescriptorSet = Bennu.getInstance().getDynamicFieldDescriptorSet();
        dynamicFieldDescriptorSet.forEach((DynamicFieldDescriptor d) -> {
            if (d.getTag() == null) {
                d.setTag(DynamicFieldTag.getOrCreateDefaultTag(d.getDomainObjectClassName()));
            }
        });

        Log.warn("Finished population of DynamicFieldTag in DynamicFieldDescriptors in " + (System.currentTimeMillis() - start)
                + " ms. Migrated " + dynamicFieldDescriptorSet.size() + " DynamicFieldDescriptor instances.");
    }

    private void registerHealthchecks() {
        SystemResource.registerHealthcheck(new Healthcheck() {
            @Override
            public String getName() {
                return "SMTP";
            }

            @Override
            protected Result check() throws Exception {
                final Properties properties = new Properties();
                properties.put("mail.transport.protocol", "smtp");
                Transport transport = Session.getInstance(properties).getTransport();
                transport.connect(FenixEduAcademicConfiguration.getConfiguration().getMailSmtpHost(), null, null);
                String response = ((SMTPTransport) transport).getLastServerResponse();
                transport.close();
                return Result.healthy("SMTP server returned response: " + response);
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {

    }

    private void loadUnitNames() {
        long start = System.currentTimeMillis();
        UnitNamePart.find("...PlaceANonExistingUnitNameHere...");
        long end = System.currentTimeMillis();
        logger.debug("Load of all unit names took: " + (end - start) + "ms.");
    }

    private void registerChecksumFilterRules() {
        RequestChecksumFilter.registerFilterRule(new ChecksumPredicate() {
            @Override
            public boolean shouldFilter(HttpServletRequest request) {
                final String uri = request.getRequestURI().substring(request.getContextPath().length());
                if (uri.indexOf("home.do") >= 0) {
                    return false;
                }
                if (uri.indexOf("/student/fillInquiries.do") >= 0) {
                    return false;
                }
                if ((uri.indexOf("/teacher/executionCourseForumManagement.do") >= 0
                        || uri.indexOf("/student/viewExecutionCourseForuns.do") >= 0)
                        && request.getQueryString().indexOf("method=viewThread") >= 0) {
                    return false;
                }
                if (uri.indexOf("notAuthorized.do") >= 0) {
                    return false;
                }
                return (uri.indexOf("external/") == -1) && (uri.indexOf("login.do") == -1) && (uri.indexOf("loginCAS.do") == -1)
                        && (uri.indexOf("logoff.do") == -1) && (uri.indexOf("publico/") == -1)
                        && (uri.indexOf("siteMap.do") == -1);
            }

        });
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeAcademicPeriodOrder() {
        AcademicPeriodOrder.initialize();
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeLessonPeriods() {
        Log.warn("---------------------------------------");
        Log.warn("Starting population of Lesson Periods");

        final AtomicInteger counter = new AtomicInteger(0);

        final Map<OccupationPeriod, List<OccupationPeriodReference>> referencesByPeriod =
                Bennu.getInstance().getOccupationPeriodReferencesSet().stream()
                        .collect(Collectors.groupingBy(OccupationPeriodReference::getOccupationPeriod));

        referencesByPeriod.forEach((period, degreeReferences) -> {
            if(period.getLessonPeriod() == null){
                final ExecutionInterval executionInterval =
                        degreeReferences.stream().map(OccupationPeriodReference::getExecutionInterval).sorted().findFirst().get();
                LessonPeriod.create(executionInterval, period).getOccupationPeriodReferencesSet().addAll(degreeReferences);
                counter.incrementAndGet();
            }
        });

        Log.warn("Finished population of Lesson Periods. Created: " + counter.get());
        Log.warn("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeExecutionDegreeLessonPeriods() {
        Log.warn("---------------------------------------");
        Log.warn("Starting population of Execution Degree Lesson Periods");

        final List<ExecutionDegreeLessonPeriod> newPeriods = Bennu.getInstance().getOccupationPeriodReferencesSet().stream()
                .map(opr -> opr.createCorrespondingExecutionDegreeLessonPeriodIfMissing()).filter(Objects::nonNull).toList();

        Log.warn("Finished population of Execution Degree Lesson Periods. Created: " + newPeriods.size());
        Log.warn("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeEducationLevelTypes() {
        if (EducationLevelType.findAll().findAny().isPresent()) {
            return;
        }

        Log.warn("---------------------------------------");
        Log.warn("Starting population of Education Level Types");

        for (SchoolLevelType schoolLevelType : SchoolLevelType.values()) {
            String code = schoolLevelType.getName();
            LocalizedString name = BundleUtil.getLocalizedString(Bundle.ENUMERATION, schoolLevelType.getQualifiedName());

            if (EducationLevelType.findByCode(code).isEmpty()) {
                EducationLevelType educationLevelType = EducationLevelType.create(code, name, true);

                schoolLevelType.getEquivalentDegreeClassifications().forEach(c -> {
                    DegreeClassification classification = DegreeClassification.readByCode(c);

                    if (classification != null) {
                        educationLevelType.addDegreeClassifications(classification);
                    }
                });
            }
        }

        Log.warn("Finished population of Education Level Types. Instances created: " + EducationLevelType.findAll().count());
        Log.warn("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeProfessionCategoryTypes() {
        if (ProfessionCategoryType.findAll().findAny().isPresent()) {
            return;
        }

        Log.warn("---------------------------------------");
        Log.warn("Starting population of Profession Category Types");

        for (org.fenixedu.academic.domain.ProfessionType professionType : org.fenixedu.academic.domain.ProfessionType.values()) {
            String code = professionType.getName();
            LocalizedString name = BundleUtil.getLocalizedString(Bundle.ENUMERATION, professionType.getQualifiedName());

            if (ProfessionCategoryType.findByCode(code).isEmpty()) {
                ProfessionCategoryType.create(code, name, professionType.isActive());
            }
        }

        Log.warn("Finished population of Profession Category Types. Instances created: " + ProfessionCategoryType.findAll()
                .count());
        Log.warn("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeProfessionalStatusTypes() {
        if (ProfessionalStatusType.findAll().findAny().isPresent()) {
            return;
        }

        Log.warn("---------------------------------------");
        Log.warn("Starting population of Professional Status Types");

        for (ProfessionalSituationConditionType professionalSituation : ProfessionalSituationConditionType.values()) {
            String code = professionalSituation.getName();
            LocalizedString name = BundleUtil.getLocalizedString(Bundle.ENUMERATION, professionalSituation.getQualifiedName());

            if (ProfessionalStatusType.findByCode(code).isEmpty()) {
                ProfessionalStatusType.create(code, name, professionalSituation.isActive());
            }
        }

        Log.warn("Finished population of Professional Status Types. Instances created: " + ProfessionalStatusType.findAll()
                .count());
        Log.warn("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void migratePidData() {
        Log.warn("---------------------------------------");
        Log.warn("Migrating Personal Ingression Data fields to new entities");

        Bennu.getInstance().getPersonalIngressionsDataSet().forEach(pid -> {
            if (pid.getProfessionType() != null && pid.getProfessionCategoryType() == null) {
                ProfessionCategoryType.findByCode(pid.getProfessionType().getName()).ifPresent(pid::setProfessionCategoryType);
            }

            if (pid.getMotherProfessionType() != null && pid.getMotherProfessionCategoryType() == null) {
                ProfessionCategoryType.findByCode(pid.getMotherProfessionType().getName())
                        .ifPresent(pid::setMotherProfessionCategoryType);
            }

            if (pid.getFatherProfessionType() != null && pid.getFatherProfessionCategoryType() == null) {
                ProfessionCategoryType.findByCode(pid.getFatherProfessionType().getName())
                        .ifPresent(pid::setFatherProfessionCategoryType);
            }

            if (pid.getSpouseProfessionType() != null && pid.getSpouseProfessionCategoryType() == null) {
                ProfessionCategoryType.findByCode(pid.getSpouseProfessionType().getName())
                        .ifPresent(pid::setSpouseProfessionCategoryType);
            }

            if (pid.getProfessionalCondition() != null && pid.getProfessionalStatusType() == null) {
                ProfessionalStatusType.findByCode(pid.getProfessionalCondition().getName())
                        .ifPresent(pid::setProfessionalStatusType);
            }

            if (pid.getMotherProfessionalCondition() != null && pid.getMotherProfessionalStatusType() == null) {
                ProfessionalStatusType.findByCode(pid.getMotherProfessionalCondition().getName())
                        .ifPresent(pid::setMotherProfessionalStatusType);
            }

            if (pid.getFatherProfessionalCondition() != null && pid.getFatherProfessionalStatusType() == null) {
                ProfessionalStatusType.findByCode(pid.getFatherProfessionalCondition().getName())
                        .ifPresent(pid::setFatherProfessionalStatusType);
            }

            if (pid.getSpouseProfessionalCondition() != null && pid.getSpouseProfessionalStatusType() == null) {
                ProfessionalStatusType.findByCode(pid.getSpouseProfessionalCondition().getName())
                        .ifPresent(pid::setSpouseProfessionalStatusType);
            }

            if (pid.getMotherSchoolLevel() != null && pid.getMotherEducationLevelType() == null) {
                EducationLevelType.findByCode(pid.getMotherSchoolLevel().getName()).ifPresent(pid::setMotherEducationLevelType);
            }

            if (pid.getFatherSchoolLevel() != null && pid.getFatherEducationLevelType() == null) {
                EducationLevelType.findByCode(pid.getFatherSchoolLevel().getName()).ifPresent(pid::setFatherEducationLevelType);
            }

            if (pid.getSpouseSchoolLevel() != null && pid.getSpouseEducationLevelType() == null) {
                EducationLevelType.findByCode(pid.getSpouseSchoolLevel().getName()).ifPresent(pid::setSpouseEducationLevelType);
            }
        });

        Log.warn("Finished migrating Personal Ingression Data fields to new entities. Processed " + Bennu.getInstance()
                .getPersonalIngressionsDataSet().size() + " Personal Ingression Data instances.");
        Log.warn("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void migratePrecedentDegreeInformationData() {
        Log.warn("---------------------------------------");
        Log.warn("Migrating Precedent Degree Information fields to new entities");

        Bennu.getInstance().getPrecedentDegreeInformationSet().forEach(pdi -> {
            if (pdi.getSchoolLevel() != null && pdi.getEducationLevelType() == null) {
                EducationLevelType.findByCode(pdi.getSchoolLevel().getName()).ifPresent(pdi::setEducationLevelType);
            }
        });

        Log.warn("Finished migrating Precedent Degree Information fields to new entities. Processed " + Bennu.getInstance()
                .getPrecedentDegreeInformationSet().size() + " Precedent Degree Information instances.");
        Log.warn("---------------------------------------");
    }
}
