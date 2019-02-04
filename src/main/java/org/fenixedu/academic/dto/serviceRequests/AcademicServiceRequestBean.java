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
package org.fenixedu.academic.dto.serviceRequests;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.predicate.AccessControl;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

public class AcademicServiceRequestBean implements Serializable {

    protected AcademicServiceRequestSituationType academicServiceRequestSituationType;

    private AcademicServiceRequest request;

    private String justification;

    private YearMonthDay situationDate;

    protected Integer serviceRequestYear;

    private Person responsible;

    protected AcademicServiceRequestBean() {
        super();
    }

    public AcademicServiceRequestBean(final AcademicServiceRequest request,
            final AcademicServiceRequestSituationType situationType) {
        this();
        setAcademicServiceRequest(request);
        setAcademicServiceRequestSituationType(situationType);
        setSituationDate(new YearMonthDay());
    }

    public AcademicServiceRequestBean(final AcademicServiceRequestSituationType academicServiceRequestSituationType,
            final Person responsible) {
        this();
        setAcademicServiceRequestSituationType(academicServiceRequestSituationType);
        setResponsible(responsible);
        setSituationDate(new YearMonthDay());
    }

    public AcademicServiceRequestBean(final AcademicServiceRequestSituationType academicServiceRequestSituationType,
            final Person responsible, final Integer serviceRequestYear) {
        this(academicServiceRequestSituationType, responsible);
        setServiceRequestYear(serviceRequestYear);
    }

    public AcademicServiceRequestBean(final AcademicServiceRequestSituationType academicServiceRequestSituationType,
            final Person responsible, final String justification) {
        this(academicServiceRequestSituationType, responsible);
        setJustification(justification);
    }

    public AcademicServiceRequestBean(final AcademicServiceRequestSituationType academicServiceRequestSituationType,
            final Person responsible, final YearMonthDay situationDate, final String justification) {
        this(academicServiceRequestSituationType, responsible, justification);
        setSituationDate(situationDate);
    }

    public AcademicServiceRequestBean(final Person responsible, final String justification) {
        this((AcademicServiceRequestSituationType) null, responsible, justification);
    }

    public AcademicServiceRequestSituationType getAcademicServiceRequestSituationType() {
        return academicServiceRequestSituationType;
    }

    public void setAcademicServiceRequestSituationType(AcademicServiceRequestSituationType academicServiceRequestSituationType) {
        this.academicServiceRequestSituationType = academicServiceRequestSituationType;
    }

    public boolean hasAcademicServiceRequestSituationType() {
        return this.academicServiceRequestSituationType != null;
    }

    DateTime finalSituationDate;

    public DateTime getFinalSituationDate() {
        if (finalSituationDate == null) {
            return getSituationDate().toDateTimeAtCurrentTime();
        }

        return finalSituationDate;
    }

    public void setFinalSituationDate(final DateTime finalSituationDate) {
        this.finalSituationDate = finalSituationDate;
    }

    public YearMonthDay getSituationDate() {
        return situationDate;
    }

    public void setSituationDate(YearMonthDay situationDate) {
        this.situationDate = situationDate;
    }

    public Person getResponsible() {
        return responsible;
    }

    public void setResponsible(Person responsible) {
        this.responsible = responsible;
    }

    private AcademicServiceRequest getAcademicServiceRequest() {
        return request;
    }

    private void setAcademicServiceRequest(final AcademicServiceRequest request) {
        this.request = request;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public boolean hasJustification() {
        return !StringUtils.isEmpty(this.justification);
    }

    public Integer getServiceRequestYear() {
        return serviceRequestYear;
    }

    public void setServiceRequestYear(Integer serviceRequestYear) {
        this.serviceRequestYear = serviceRequestYear;
    }

    public boolean isNew() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.NEW;
    }

    public boolean isToProcess() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.PROCESSING;
    }

    public boolean isToDeliver() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.DELIVERED;
    }

    public boolean isToCancel() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.CANCELLED;
    }

    public boolean isToReject() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.REJECTED;
    }

    public boolean isToConclude() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.CONCLUDED;
    }

    public boolean isToSendToExternalEntity() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.SENT_TO_EXTERNAL_ENTITY;
    }

    public boolean isToReceiveFromExternalUnit() {
        return this.academicServiceRequestSituationType == AcademicServiceRequestSituationType.RECEIVED_FROM_EXTERNAL_ENTITY;
    }

    public boolean isToCancelOrReject() {
        return isToCancel() || isToReject();
    }

    public Collection<AcademicServiceRequest> searchAcademicServiceRequests() {
        return AcademicServiceRequest.getAcademicServiceRequests(AccessControl.getPerson(), serviceRequestYear,
                academicServiceRequestSituationType, null);
    }
}
