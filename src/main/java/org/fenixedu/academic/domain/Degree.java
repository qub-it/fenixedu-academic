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

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Strings;

public class Degree extends Degree_Base implements Comparable<Degree> {
    public static final String CREATED_SIGNAL = "academic.degree.create";

    public static final String DEFAULT_MINISTRY_CODE = "9999";

    private static final Collator collator = Collator.getInstance();

    static final public Comparator<Degree> COMPARATOR_BY_NAME =
            Comparator.comparing(d -> d.getNameFor(null).getContent(), collator);

    static final public Comparator<Degree> COMPARATOR_BY_NAME_AND_ID =
            COMPARATOR_BY_NAME.thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

    @Deprecated(forRemoval = true)
    static final private Comparator<Degree> COMPARATOR_BY_DEGREE_TYPE_NAME = new Comparator<Degree>() {
        @Override
        public int compare(final Degree o1, final Degree o2) {
            return collator.compare(o1.getDegreeType().getName().getContent(), o2.getDegreeType().getName().getContent());
        }
    };

    static final private Comparator<Degree> COMPARATOR_BY_DEGREE_TYPE = Comparator.comparing(Degree::getDegreeType);

    @Deprecated(forRemoval = true)
    private static class ComparatorByDegreeTypeAndNameAndId implements Serializable, Comparator<Degree> {
        @Override
        public int compare(final Degree o1, final Degree o2) {
            final int typeResult = COMPARATOR_BY_DEGREE_TYPE_NAME.compare(o1, o2);
            return typeResult == 0 ? COMPARATOR_BY_NAME_AND_ID.compare(o1, o2) : typeResult;
        }
    }

    static final public Comparator<Degree> COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID =
            COMPARATOR_BY_DEGREE_TYPE.thenComparing(COMPARATOR_BY_NAME_AND_ID);

    @Deprecated(forRemoval = true)
    static final public Comparator<Degree> COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID = new ComparatorByDegreeTypeAndNameAndId();

    @Override
    public int compareTo(final Degree o) {
        return Degree.COMPARATOR_BY_NAME_AND_ID.compare(this, o);
    }

    protected Degree() {
        super();

        setRootDomainObject(Bennu.getInstance());
    }

    public Degree(final String name, final String nameEn, final String code, final DegreeType degreeType,
            final GradeScale numericGradeScale, final GradeScale qualitativeGradeScale, final ExecutionYear executionYear) {
        this();
        commonFieldsChange(name, nameEn, code, numericGradeScale, qualitativeGradeScale, executionYear);

        if (degreeType == null) {
            throw new DomainException("degree.degree.type.not.null");
        }
        this.setDegreeType(degreeType);
    }

    public Degree(final String name, final String nameEn, final String acronym, final DegreeType degreeType,
            final Double ectsCredits, final GradeScale numericGradeScale, final GradeScale qualitativeGradeScale,
            final String prevailingScientificArea, final AdministrativeOffice administrativeOffice) {
        this();
        commonFieldsChange(name, nameEn, acronym, numericGradeScale, qualitativeGradeScale,
                ExecutionYear.findCurrent(getCalendar()));
        newStructureFieldsChange(degreeType, ectsCredits, prevailingScientificArea);
        setAdministrativeOffice(administrativeOffice);
    }

    private void commonFieldsChange(final String name, final String nameEn, final String code, final GradeScale gradeScale,
            final GradeScale qualitativeGradeScale, final ExecutionYear executionYear) {
        if (name == null) {
            throw new DomainException("degree.name.not.null");
        } else if (nameEn == null) {
            throw new DomainException("degree.name.en.not.null");
        } else if (code == null) {
            throw new DomainException("degree.code.not.null");
        }

        DegreeInfo degreeInfo = getDegreeInfoFor(executionYear);
        if (degreeInfo == null) {
            degreeInfo = tryCreateUsingMostRecentInfo(executionYear);
        }
        degreeInfo.setName(new LocalizedString().with(org.fenixedu.academic.util.LocaleUtils.PT, name.trim())
                .with(org.fenixedu.academic.util.LocaleUtils.EN, nameEn.trim()));

        this.setNome(name);
        this.setNameEn(nameEn);
        this.setSigla(code.trim());
        this.setNumericGradeScale(gradeScale);
        this.setQualitativeGradeScale(qualitativeGradeScale);

        if (this.getNumericGradeScale() == null) {
            throw new DomainException("error.Degree.numericGradeScale.required");
        }

        if (this.getQualitativeGradeScale() == null) {
            throw new DomainException("error.Degree.qualitativeGradeScale.required");
        }
    }

    private void newStructureFieldsChange(final DegreeType degreeType, final Double ectsCredits,
            final String prevailingScientificArea) {
        if (degreeType == null) {
            throw new DomainException("degree.degree.type.not.null");
        } else if (ectsCredits == null) {
            throw new DomainException("degree.ectsCredits.not.null");
        }

        this.setDegreeType(degreeType);
        this.setEctsCredits(ectsCredits);
        this.setPrevailingScientificArea(prevailingScientificArea == null ? null : prevailingScientificArea.trim());
    }

    public void edit(final String name, final String nameEn, final String code, final DegreeType degreeType,
            final GradeScale numericGradeScale, final GradeScale qualitativeGradeScale, final ExecutionYear executionYear) {
        commonFieldsChange(name, nameEn, code, numericGradeScale, qualitativeGradeScale, executionYear);

        if (degreeType == null) {
            throw new DomainException("degree.degree.type.not.null");
        }
        this.setDegreeType(degreeType);
    }

    public void edit(final String name, final String nameEn, final String acronym, final DegreeType degreeType,
            final Double ectsCredits, final GradeScale numericGradeScale, final GradeScale qualitativeGradeScale,
            final String prevailingScientificArea, final ExecutionYear executionYear) {
        checkIfCanEdit(degreeType);
        commonFieldsChange(name, nameEn, acronym, numericGradeScale, qualitativeGradeScale, executionYear);
        newStructureFieldsChange(degreeType, ectsCredits, prevailingScientificArea);
    }

    private void checkIfCanEdit(final DegreeType degreeType) {
        if (!this.getDegreeType().equals(degreeType) && !getDegreeCurricularPlansSet().isEmpty()) {
            throw new DomainException("degree.cant.edit.bolonhaDegreeType");
        }
    }

    public Boolean getCanBeDeleted() {
        return getDeletionBlockers().isEmpty();
    }

    @Override
    protected void checkForDeletionBlockers(final Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getDegreeCurricularPlansSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.DOMAIN_EXCEPTION, "error.degree.has.degree.curricular.plans"));
        }

        if (!getStudentGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,
                    "error.academicProgram.cannotDeleteBacauseUsedInAccessControl"));
        }

        if (!getTeacherGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,
                    "error.academicProgram.cannotDeleteBacauseUsedInAccessControl"));
        }

        if (!getCoordinatorGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,
                    "error.academicProgram.cannotDeleteBacauseUsedInAccessControl"));
        }

        if (!getStudentsConcludedInExecutionYearGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,
                    "error.academicProgram.cannotDeleteBacauseUsedInAccessControl"));
        }

        if (getAlumniGroup() != null) {
            blockers.add(BundleUtil.getString(Bundle.DOMAIN_EXCEPTION,
                    "error.academicProgram.cannotDeleteBacauseUsedInAccessControl"));
        }
    }

    @Override
    protected void disconnect() {
        Iterator<DegreeInfo> degreeInfosIterator = getDegreeInfosSet().iterator();
        while (degreeInfosIterator.hasNext()) {
            DegreeInfo degreeInfo = degreeInfosIterator.next();
            degreeInfosIterator.remove();
            degreeInfo.delete();
        }

        getDegreeLogsSet().forEach(DegreeLog::delete);

        if (getUnit() != null) {
            getUnit().delete();
        }

        setNumericGradeScale(null);
        setCalendar(null);
        setDegreeType(null);
        setRootDomainObject(null);
        setQualitativeGradeScale(null);
        super.disconnect();
    }

    public TreeSet<DegreeInfo> getDegreeInfosSorted() {
        final TreeSet<DegreeInfo> result = new TreeSet<>(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR);
        result.addAll(getDegreeInfosSet());
        return result;
    }

    public DegreeCurricularPlan createDegreeCurricularPlan(final String name, final Person creator,
            final AcademicPeriod duration) {
        if (creator == null) {
            throw new DomainException("DEGREE.degree.curricular.plan.creator.cannot.be.null");
        }
        return new DegreeCurricularPlan(this, name, duration);
    }

    @Override
    public Collection<CycleType> getCycleTypes() {
        return getDegreeType().getCycleTypes();
    }

    @Deprecated(forRemoval = true)
    public boolean isEmpty() {
        return false;
    }

    public static Degree find(final String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }

        return Degree.findAll().filter(degree -> StringUtils.equalsIgnoreCase(degree.getCode(), code)).findFirst().orElse(null);
    }

    public List<DegreeCurricularPlan> getActiveDegreeCurricularPlans() {
        return getDegreeCurricularPlansSet().stream().filter(DegreeCurricularPlan::isActive).toList();
    }

    public List<DegreeCurricularPlan> getDegreeCurricularPlansForYear(final ExecutionYear year) {
        return getDegreeCurricularPlansSet().stream().filter(dcp -> dcp.hasAnyExecutionDegreeFor(year)).toList();
    }

    public List<ExecutionDegree> getExecutionDegrees() {
        return getDegreeCurricularPlansSet().stream().flatMap(dcp -> dcp.getExecutionDegreesSet().stream()).toList();
    }

    public List<ExecutionDegree> getExecutionDegreesForExecutionYear(final ExecutionYear executionYear) {
        return getDegreeCurricularPlansSet().stream().flatMap(dcp -> dcp.findExecutionDegree(executionYear).stream()).toList();
    }

    public List<ExecutionYear> getDegreeCurricularPlansExecutionYears() {
        return getDegreeCurricularPlansSet().stream().flatMap(dcp -> dcp.getExecutionDegreesSet().stream())
                .map(ExecutionDegree::getExecutionYear).distinct().sorted().toList();
    }

    public LocalizedString getNameFor(final ExecutionInterval executionInterval) {
        DegreeInfo degreeInfo = executionInterval == null ? getMostRecentDegreeInfo() : getMostRecentDegreeInfo(
                executionInterval.getExecutionYear());
        return degreeInfo == null ? new LocalizedString().with(LocaleUtils.PT, super.getNome())
                .with(LocaleUtils.EN, super.getNameEn()) : degreeInfo.getName();
    }

    @Override
    @Deprecated
    public String getNome() {
        return getName();
    }

    /**
     * @deprecated Use {@link #getNameFor(ExecutionInterval)}
     */
    @Deprecated
    public String getName() {
        DegreeInfo degreeInfo = getMostRecentDegreeInfo();
        return degreeInfo == null ? StringUtils.EMPTY : degreeInfo.getName()
                .getContent(org.fenixedu.academic.util.LocaleUtils.PT);
    }

    /**
     * @deprecated Use {@link #getNameFor(ExecutionInterval)}
     */
    @Override
    @Deprecated
    public String getNameEn() {
        DegreeInfo degreeInfo = getMostRecentDegreeInfo();
        return degreeInfo == null ? StringUtils.EMPTY : degreeInfo.getName()
                .getContent(org.fenixedu.academic.util.LocaleUtils.EN);
    }

    final public LocalizedString getNameI18N() {
        return getNameFor(ExecutionYear.findCurrent(getCalendar()));
    }

    final public LocalizedString getNameI18N(final ExecutionYear executionYear) {
        return getNameFor(executionYear);
    }

    public LocalizedString getPresentationNameI18N() {
        return getPresentationNameI18N(ExecutionYear.findCurrent(getCalendar()));
    }

    public LocalizedString getPresentationNameI18N(final ExecutionYear executionYear) {
        LocalizedString degreeType = getDegreeType().getName();

        LocalizedString.Builder builder = new LocalizedString.Builder();
        CoreConfiguration.supportedLocales().forEach(l -> builder.with(l, degreeType.getContent(l) + " "
                + BundleUtil.getString(Bundle.APPLICATION, l, "label.in") + " " + getNameI18N(executionYear).getContent(l)));
        return builder.build();
    }

    final public String getPresentationName() {
        return getPresentationNameI18N().getContent();
    }

    public String getPresentationName(final ExecutionYear executionYear) {
        return getPresentationNameI18N(executionYear).getContent();
    }

    public String getPresentationName(final ExecutionYear executionYear, final Locale locale) {
        return getPresentationNameI18N(executionYear).getContent(locale);
    }

    public DegreeCurricularPlan getMostRecentDegreeCurricularPlan() {
        ExecutionDegree mostRecentExecutionDegree = null;
        boolean mustGetByInitialDate = false;

        for (final DegreeCurricularPlan degreeCurricularPlan : this.getActiveDegreeCurricularPlans()) {
            ExecutionDegree executionDegree = degreeCurricularPlan.getMostRecentExecutionDegree();
            if (executionDegree == null) {
                continue;
            }

            if (mostRecentExecutionDegree == null) {
                mostRecentExecutionDegree = executionDegree;
            } else {
                if (mostRecentExecutionDegree.getExecutionYear().equals(executionDegree.getExecutionYear())) {
                    mustGetByInitialDate = true;
                } else if (mostRecentExecutionDegree.isBefore(executionDegree)) {
                    mustGetByInitialDate = false;
                    mostRecentExecutionDegree = executionDegree;
                }
            }
        }

        if (mustGetByInitialDate) {
            // investigate dcps initial dates
            return getMostRecentDegreeCurricularPlanByInitialDate();
        } else {
            return mostRecentExecutionDegree != null ? mostRecentExecutionDegree.getDegreeCurricularPlan() : null;
        }
    }

    private DegreeCurricularPlan getMostRecentDegreeCurricularPlanByInitialDate() {
        return getActiveDegreeCurricularPlans().stream().filter(dcp -> dcp.getInitialDateYearMonthDay() != null)
                .max(Comparator.comparing(DegreeCurricularPlan::getInitialDateYearMonthDay)).orElse(null);
    }

    public Collection<Registration> getActiveRegistrations() {
        return getActiveDegreeCurricularPlans().stream().flatMap(dcp -> dcp.getActiveRegistrations().stream())
                .collect(Collectors.toSet());
    }

    public DegreeCurricularPlan getFirstDegreeCurricularPlan() {
        return getDegreeCurricularPlansSet().stream().filter(dcp -> dcp.getInitialDateYearMonthDay() != null)
                .min(Comparator.comparing(DegreeCurricularPlan::getInitialDateYearMonthDay)).orElse(null);
    }

    // -------------------------------------------------------------
    // read static methods
    // -------------------------------------------------------------

    private static final Map<String, SoftReference<Degree>> degrees = new Hashtable<>();

    private static void loadCache() {
        synchronized (degrees) {
            degrees.clear();
            Degree.findAll().forEach(degree -> degrees.put(degree.getSigla().toLowerCase(), new SoftReference<>(degree)));
        }
    }

    private static void updateCache(final Degree degree, final String newLowerCaseSigla) {
        final String currentLowerCaseSigla = degree.getSigla() != null ? degree.getSigla().toLowerCase() : StringUtils.EMPTY;
        synchronized (degrees) {
            degrees.remove(currentLowerCaseSigla);
            degrees.put(newLowerCaseSigla, new SoftReference<>(degree));
        }
    }

    @Override
    public void setSigla(final String sigla) {
        updateCache(this, sigla.toLowerCase());
        super.setSigla(sigla);
    }

    public String getAcronym() {
        return super.getSigla();
    }

    public void setAcronym(final String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new DomainException("error.Degree.required.acronym");
        }

        setSigla(input.trim());
    }

    public static Degree readBySigla(final String sigla) {
        if (degrees.isEmpty()) {
            loadCache();
        }
        final String lowerCaseString = sigla.toLowerCase();
        final SoftReference<Degree> degreeReference = degrees.get(lowerCaseString);
        if (degreeReference != null) {
            final Degree degree = degreeReference.get();
            if (degree != null && degree.getRootDomainObject() == Bennu.getInstance()
                    && degree.getSigla().equalsIgnoreCase(lowerCaseString)) {
                return degree;
            } else {
                loadCache();
                final SoftReference<Degree> otherDegreeReference = degrees.get(lowerCaseString);
                if (otherDegreeReference != null) {
                    final Degree otherDegree = otherDegreeReference.get();
                    if (otherDegree != null && otherDegree.getRootDomainObject() == Bennu.getInstance()
                            && otherDegree.getSigla().equalsIgnoreCase(lowerCaseString)) {
                        return otherDegree;
                    }
                }
            }
        }

        return null;
    }

    public static Stream<Degree> findAll() {
        return Bennu.getInstance().getDegreesSet().stream();
    }

    /**
     * @deprecated Degrees cannot be empty anymore so usage of this method is unecessary
     */
    @Deprecated(forRemoval = true)
    public static List<Degree> readNotEmptyDegrees() {
        return new ArrayList<>(Bennu.getInstance().getDegreesSet());
    }

    /**
     * @deprecated Degrees cannot be non bolonha anymore so usage of this method is unecessary
     */
    @Deprecated(forRemoval = true)
    public static List<Degree> readBolonhaDegrees() {
        return new ArrayList<>(Bennu.getInstance().getDegreesSet());
    }

    public static List<Degree> readAllMatching(final Predicate<DegreeType> predicate) {
        return DegreeType.all().filter(predicate).flatMap(type -> type.getDegreeSet().stream()).collect(Collectors.toList());
    }

    public DegreeInfo getMostRecentDegreeInfo() {
        return getMostRecentDegreeInfo(ExecutionYear.findCurrent(getCalendar()));
    }

    public DegreeInfo getDegreeInfoFor(final ExecutionYear executionYear) {
        return getDegreeInfosSet().stream().filter(di -> di.getExecutionYear() == executionYear).findFirst().orElse(null);
    }

    public DegreeInfo getMostRecentDegreeInfo(final ExecutionYear executionYear) {
        DegreeInfo result = null;
        for (final DegreeInfo degreeInfo : getDegreeInfosSet()) {
            final ExecutionYear executionYear2 = degreeInfo.getExecutionYear();
            if (executionYear == executionYear2) {
                return degreeInfo;
            }
            if (executionYear2.isBefore(executionYear)) {
                if (result == null || executionYear2.isAfter(result.getExecutionYear())) {
                    result = degreeInfo;
                }
            }
        }

        if (result == null && executionYear.getNextExecutionYear() != null) {
            result = getMostRecentDegreeInfo(executionYear.getNextExecutionYear());
        }

        return result;
    }

    private DegreeInfo tryCreateUsingMostRecentInfo(final ExecutionYear executionYear) {
        final DegreeInfo mostRecentDegreeInfo = getMostRecentDegreeInfo(executionYear);
        return mostRecentDegreeInfo != null ? new DegreeInfo(mostRecentDegreeInfo, executionYear) : new DegreeInfo(this,
                executionYear);
    }

    public boolean isFirstCycle() {
        return getDegreeType().isFirstCycle();
    }

    public boolean isSecondCycle() {
        return getDegreeType().isSecondCycle();
    }

    public boolean isThirdCycle() {
        return getDegreeType().isThirdCycle();
    }

    @Override
    public Double getEctsCredits() {
        final Double ectsCredits = super.getEctsCredits();
        return ectsCredits != null ? ectsCredits : 0.0;
    }

    @Override
    public String getMinistryCode() {
        final String ministryCode = super.getMinistryCode();
        if (!StringUtils.isEmpty(ministryCode)) {
            return ministryCode;
        }

        return DEFAULT_MINISTRY_CODE;
    }

    @Override
    public void setMinistryCode(final String ministryCode) {
        super.setMinistryCode(ministryCode == null || ministryCode.isEmpty() ? null : ministryCode);
    }

    public String getDegreeTypeName() {
        return getDegreeType().getName().getContent();
    }

    @Override
    public void setCode(final String code) {
        final Degree existingDegree = Degree.find(code);
        if (existingDegree != null && existingDegree != this) {
            throw new DomainException("error.degree.already.exists.degree.with.same.code");
        }

        super.setCode(code);
    }

    @Override
    public void setIdCardName(final String idCardName) {
        super.setIdCardName(idCardName.toUpperCase());
    }

    @Override
    public void setNome(final String nome) {
        super.setNome(nome);
        setIdCardName(nome);
    }

}
