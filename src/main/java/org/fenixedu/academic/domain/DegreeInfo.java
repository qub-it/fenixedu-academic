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
package org.fenixedu.academic.domain;

import java.util.Comparator;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.commons.i18n.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tania Pousao Created on 30/Out/2003
 */
public class DegreeInfo extends DegreeInfo_Base {

    public static final String DESCRIPTION = "description";
    public static final String HISTORY = "history";
    public static final String OBJECTIVES = "objectives";
    public static final String DESIGNED_FOR = "designedFor";
    public static final String PROFESSIONAL_EXITS = "professionalExits";
    public static final String OPERATIONAL_REGIME = "operationalRegime";
    public static final String GRATUITY = "gratuity";
    public static final String ADDITIONAL_INFO = "additionalInfo";
    public static final String LEARNING_LANGUAGES = "learningLanguages";
    public static final String LINKS = "links";
    public static final String TEST_INGRESSION = "testIngression";
    public static final String CLASSIFICATIONS = "classifications";
    public static final String ACCESS_REQUISITES = "accessRequisites";
    public static final String CANDIDACY_DOCUMENTS = "candidacyDocuments";
    public static final String DRIFTS_INITIAL = "driftsInitial";
    public static final String DRIFTS_FIRST = "driftsFirst";
    public static final String DRIFTS_SECOND = "driftsSecond";
    public static final String MARK_MIN = "markMin";
    public static final String MARK_MAX = "markMax";
    public static final String MARK_AVERAGE = "markAverage";
    public static final String QUALIFICATION_LEVEL = "qualificationLevel";
    public static final String RECOGNITIONS = "recognitions";
    public static final String PREVAILING_SCIENTIFIC_AREA = "prevailingScientificArea";
    public static final String SCIENTIFIC_AREAS = "scientificAreas";
    public static final String STUDY_PROGRAMME_DURATION = "studyProgrammeDuration";
    public static final String STUDY_REGIME = "studyRegime";
    public static final String STUDY_PROGRAMME_REQUIREMENTS = "studyProgrammeRequirements";
    public static final String HIGHER_EDUCATION_ACCESS = "higherEducationAccess";
    public static final String PROFESSIONAL_STATUS = "professionalStatus";
    public static final String SUPPLEMENT_EXTRA_INFORMATION = "supplementExtraInformation";
    public static final String SUPPLEMENT_OTHER_SOURCES = "supplementOtherSources";

    private static final Logger logger = LoggerFactory.getLogger(DegreeInfo.class);

    public static final String DEGREE_INFO_CREATION_EVENT = "DEGREE_INFO_CREATION_EVENT";

    public static Comparator<DegreeInfo> COMPARATOR_BY_EXECUTION_YEAR = new Comparator<DegreeInfo>() {
        @Override
        public int compare(final DegreeInfo info1, final DegreeInfo info2) {
            int result = ExecutionYear.COMPARATOR_BY_YEAR.compare(info1.getExecutionYear(), info2.getExecutionYear());
            if (result != 0) {
                return result;
            }
            return DomainObjectUtil.COMPARATOR_BY_ID.compare(info1, info2);
        }
    };

    public DegreeInfo(final Degree degree, final ExecutionYear executionYear) {
        super();
        setRootDomainObject(Bennu.getInstance());

        DegreeInfo degreeInfo = degree.getMostRecentDegreeInfo(executionYear);

        if (degreeInfo != null && degreeInfo.getExecutionYear() == executionYear) {
            throw new DomainException(
                    "error.net.sourceforge.fenixdu.domain.cannot.create.degreeInfo.already.exists.one.for.that.degree.and.executionYear");
        }

        super.setExecutionYear(executionYear);
        super.setName(degree.getNameFor(executionYear));
        super.setDegree(degree);
        Signal.emit(DEGREE_INFO_CREATION_EVENT, new DomainObjectEvent<>(this));

    }

    protected DegreeInfo() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public ExecutionInterval getExecutionInterval() {
        return getExecutionYear();
    }

    public void setExecutionInterval(final ExecutionInterval input) {
        if (input == null) {
            throw new DomainException("error.DegreeInfo.required.ExecutionInterval");
        }
        super.setExecutionYear(ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, input));
    }

    @Override
    public void setName(final LocalizedString name) {
        if (hasSameName(name)) {
            return;
        }

        if (hasName() && !isEditable(this)) {
            throw new DomainException(
                    "error.org.fenixedu.academic.domain.DegreeInfo.can.only.change.name.for.future.execution.years");
        }
        super.setName(name);
    }

    private boolean hasName() {
        return getName() != null && !getName().isEmpty();
    }

    private boolean hasSameName(final LocalizedString name) {
        return hasName() && getName().equals(name);
    }

    public DegreeInfo(final DegreeInfo degreeInfo, final ExecutionYear executionYear) {
        this(degreeInfo.getDegree(), executionYear);
        setName(degreeInfo.getName());
    }

    public void delete() {
        setRootDomainObject(null);
        setDegree(null);
        setExecutionYear(null);

        deleteDomainObject();
    }

    public AcademicInterval getAcademicInterval() {
        return getExecutionYear().getAcademicInterval();
    }

    /*
     * #dsimoes @13JAN2016
     * Any change to the name are now allowed.
     */
    public static boolean isEditable(final DegreeInfo dinfo) {
        return true;
        //        final DegreeCurricularPlan firstDegreeCurricularPlan = dinfo.getDegree().getFirstDegreeCurricularPlan();
        //        final DegreeCurricularPlan lastActiveDegreeCurricularPlan = dinfo.getDegree().getLastActiveDegreeCurricularPlan();
        //        if (firstDegreeCurricularPlan == null) {
        //            return true;
        //        }
        //        ExecutionYear firstExecutionYear =
        //                ExecutionYear.readByDateTime(firstDegreeCurricularPlan.getInitialDateYearMonthDay().toDateTimeAtMidnight());
        //        if (dinfo.getExecutionYear().isBefore(firstExecutionYear)) {
        //            return true;
        //        }
        //        if (lastActiveDegreeCurricularPlan == null) {
        //            return true;
        //        }
        //        if (lastActiveDegreeCurricularPlan.getExecutionDegreesSet().isEmpty()) {
        //            return true;
        //        }
        //        if (dinfo.getExecutionYear().isAfter(ExecutionYear.readCurrentExecutionYear())) {
        //            return true;
        //        }
        //        if (dinfo.getExecutionYear().isCurrent()) {
        //            return true;
        //        }
        //        return false;
    }
}
