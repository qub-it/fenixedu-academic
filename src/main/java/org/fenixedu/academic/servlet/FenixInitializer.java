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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Session;
import javax.mail.Transport;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.fenixedu.academic.FenixEduAcademicConfiguration;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Installation;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusionConfig;
import org.fenixedu.academic.domain.dml.DynamicFieldDescriptor;
import org.fenixedu.academic.domain.dml.DynamicFieldTag;
import org.fenixedu.academic.domain.organizationalStructure.UnitNamePart;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.person.identificationDocument.IdentificationDocumentType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriodOrder;
import org.fenixedu.bennu.core.api.SystemResource;
import org.fenixedu.bennu.core.domain.Bennu;
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

        initializeCurrentExecutionIntervals();

        initializeProgramConclusionConfigs();

        initializeIdentificationDocumentTypeDomainEntity();
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

    @Atomic(mode = TxMode.WRITE)
    private void initializeCurrentExecutionIntervals() {
        final Bennu root = Bennu.getInstance();
        if (!root.getCurrentExecutionIntervalsSet().isEmpty()) {
            return;
        }

        Log.warn("---------------------------------------");
        Log.warn("Starting initialization of current execution intervals");
        Log.warn("---------------------------------------");

        final List<ExecutionInterval> currentIntervals =
                root.getExecutionIntervalsSet().stream().filter(ExecutionInterval::isCurrent).toList();
        root.getCurrentExecutionIntervalsSet().addAll(currentIntervals);

        Log.warn("Finished initialization of current execution intervals");
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
                        && (uri.indexOf("logoff.do") == -1) && (uri.indexOf("publico/") == -1) && (uri.indexOf("siteMap.do")
                        == -1);
            }

        });
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeAcademicPeriodOrder() {
        AcademicPeriodOrder.initialize();
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeProgramConclusionConfigs() {
        Log.info("---------------------------------------");
        Log.info("Starting initialization of ProgramConclusionConfigs");

        final AtomicInteger counter = new AtomicInteger(0);
        ProgramConclusion.findAll().flatMap(pc -> pc.getCourseGroupSet().stream())
                .filter(cg -> cg.getProgramConclusionConfigsForIncludedModulesSet().isEmpty()).forEach(cg -> {
                    final ProgramConclusionConfig conclusionConfig =
                            ProgramConclusionConfig.create(cg.getConclusionTitle(), cg.getParentDegreeCurricularPlan(),
                                    cg.getProgramConclusion());
                    conclusionConfig.addIncludedModules(cg);
                    if (cg.getProgramConclusion().isTerminal()) {
                        conclusionConfig.moveTop();
                    } else {
                        conclusionConfig.moveBottom();
                    }
                    counter.incrementAndGet();
                });

        Log.info("Finished initialization of ProgramConclusionConfig. Processed " + counter.get() + " CourseGroup instances.");
        Log.info("---------------------------------------");
    }

    @Atomic(mode = TxMode.WRITE)
    private void initializeIdentificationDocumentTypeDomainEntity() {
        if (IdentificationDocumentType.findAll().findAny().isPresent()) {
            return;
        }

        Log.warn("---------------------------------------");
        Log.warn("Starting population of Identification Document Types");

        for (IDDocumentType idDocumentType : IDDocumentType.values()) {
            String code = idDocumentType.name();
            LocalizedString name = idDocumentType.getLocalizedNameI18N();

            if (IdentificationDocumentType.findByCode(code).isEmpty()) {
                IdentificationDocumentType.create(code, name);
            }
        }

        Log.warn(
                "Finished population of Identification Document Types. Instances created: " + IdentificationDocumentType.findAll()
                        .count());
        Log.warn("---------------------------------------");
    }
}
