package org.fenixedu.academic.domain.curriculum.grade;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.FenixFramework;

public class GradeScale extends GradeScale_Base {

    public static final Comparator<GradeScale> COMPARE_BY_NAME =
            Comparator.comparing(GradeScale::getName).thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

    private static final Map<String, GradeScale> GRADE_SCALE_CACHE = new ConcurrentHashMap<>();
    private static final Map<GradeScale, Map<String, GradeScaleEntry>> INTERNAL_CACHE = new ConcurrentHashMap<>();
    private Set<String> approvedGradeValuesCache = null;
    private Set<String> notApprovedGradeValuesCache = null;

    public GradeScale() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected GradeScale(final String code, final LocalizedString name, final BigDecimal minimumReprovedGrade,
            final BigDecimal maximumReprovedGrade, final BigDecimal minimumApprovedGrade, final BigDecimal maximumApprovedGrade,
            final boolean internalGradeScale, final boolean active) {
        this();

        setCode(code);
        setName(name);
        setMinimumReprovedGrade(minimumReprovedGrade);
        setMaximumReprovedGrade(maximumReprovedGrade);
        setMinimumApprovedGrade(minimumApprovedGrade);
        setMaximumApprovedGrade(maximumApprovedGrade);
        setInternalGradeScale(internalGradeScale);
        setActive(active);
        setDefaultGradeScale(false);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new DomainException("error.GradeScale.domainRoot.required");
        }

        if (StringUtils.isEmpty(getCode())) {
            throw new DomainException("error.GradeScale.code.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new DomainException("error.GradeScale.code.duplicated");
        }

        if ((getMinimumApprovedGrade() == null) ^ (getMaximumApprovedGrade() == null)) {
            throw new DomainException("error.GradeScale.numericApprovedGrade.incomplete");
        }

        if ((getMinimumReprovedGrade() == null) ^ (getMaximumReprovedGrade() == null)) {
            throw new DomainException("error.GradeScale.numericReprovedGrade.incomplete");
        }

        if (hasContinuousApprovedGrades() && getMinimumApprovedGrade().compareTo(getMaximumApprovedGrade()) > 0) {
            throw new DomainException("error.GradeScale.minimumApprovedGrade.invalid");
        }

        if (hasContinuousReprovedGrades() && getMinimumReprovedGrade().compareTo(getMaximumReprovedGrade()) > 0) {
            throw new DomainException("error.GradeScale.minimumReprovedGrade.invalid");
        }

        if (hasContinuousReprovedGrades() && hasContinuousApprovedGrades()) {
            if (getMinimumReprovedGrade().compareTo(getMinimumApprovedGrade()) < 0) {
                if (getMaximumReprovedGrade().compareTo(getMinimumApprovedGrade()) >= 0) {
                    throw new DomainException("error.GradeScale.reproved.overlap.with.approved");
                }
            } else {
                if (getMaximumApprovedGrade().compareTo(getMinimumReprovedGrade()) >= 0) {
                    throw new DomainException("error.GradeScale.approved.overlap.with.reproved");
                }
            }
        }

        if (findDefault().count() > 1) {
            throw new DomainException("error.GradeScale.more.than.one.default");
        }
    }

    public GradeScaleEntry createGradeScaleEntry(final String value, final LocalizedString description,
            final boolean allowsApproval) {
        final GradeScaleEntry entry = GradeScaleEntry.create(this, value, description, allowsApproval);
        invalidateCache();

        return entry;
    }

    public void markAsDefaultGradeScale() {
        findUniqueDefault().ifPresent(oldGradeScale -> {
            oldGradeScale.setDefaultGradeScale(false);
            oldGradeScale.checkRules();
        });

        super.setDefaultGradeScale(true);
    }

    public void edit(final LocalizedString name, final BigDecimal minimumReprovedGrade, final BigDecimal maximumReprovedGrade,
            final BigDecimal minimumApprovedGrade, final BigDecimal maximumApprovedGrade, final boolean activeGradeScale,
            boolean internalGradeScale) {

        setName(name);
        setMinimumReprovedGrade(minimumReprovedGrade);
        setMaximumReprovedGrade(maximumReprovedGrade);
        setMinimumApprovedGrade(minimumApprovedGrade);
        setMaximumApprovedGrade(maximumApprovedGrade);
        setInternalGradeScale(internalGradeScale);
        setActive(activeGradeScale);

        checkRules();

        invalidateCache();
    }

    public boolean belongsTo(final String value) {
        return isApproved(value) || isNotApproved(value);
    }

    public int compareGrades(Grade leftGrade, Grade rightGrade) {
        if (rightGrade == null || rightGrade.isEmpty()) {
            return 1;
        }

        if (leftGrade == null || leftGrade.isEmpty()) {
            return -1;
        }

        if (!leftGrade.getGradeScale().equals(rightGrade.getGradeScale())) {
            throw new DomainException("Grade.unsupported.comparison.of.grades.of.different.scales", leftGrade.toString(),
                    rightGrade.toString());
        }

        if (isApproved(leftGrade) != isApproved(rightGrade)) {
            return isApproved(leftGrade) ? 1 : -1;
        }

        final Optional<GradeScaleEntry> gradeEntryLeft = findGradeScaleEntry(leftGrade.getValue());
        final Optional<GradeScaleEntry> gradeEntryRight = findGradeScaleEntry(rightGrade.getValue());

        if (gradeEntryLeft.isPresent() && gradeEntryRight.isPresent()) {
            return Integer.compare(gradeEntryLeft.get().getGradeOrder(), gradeEntryRight.get().getGradeOrder());
        }

        final boolean isLeftGradeValueContinuous = isGradeValueContinuous(leftGrade.getValue());
        final boolean isRightGradeValueContinuous = isGradeValueContinuous(rightGrade.getValue());

        if (isLeftGradeValueContinuous != isRightGradeValueContinuous) {
            return isLeftGradeValueContinuous ? 1 : -1;
        } else if (isLeftGradeValueContinuous && isRightGradeValueContinuous) {
            return leftGrade.getNumericValue().compareTo(rightGrade.getNumericValue());
        }

        throw new DomainException("Grade.unsupported.comparison.of.grades.of.different.scales");
    }

    public boolean isApproved(final String value) {
        approvedGradeValuesCache = getApprovedGradeValuesCache();

        if (approvedGradeValuesCache.contains(value)) {
            return true;
        }

        Optional<GradeScaleEntry> matchEntry = findGradeScaleEntry(value);

        if (matchEntry.isPresent() && matchEntry.get().isAllowsApproval() || isGradeValueContinuousAndApproved(value)) {
            approvedGradeValuesCache.add(value);
            return true;
        }

        return false;
    }

    public boolean isNotApproved(final String value) {
        notApprovedGradeValuesCache = getNotApprovedGradeValuesCache();

        if (notApprovedGradeValuesCache.contains(value)) {
            return true;
        }

        Optional<GradeScaleEntry> matchEntry = findGradeScaleEntry(value);

        if (matchEntry.isPresent() && !matchEntry.get().isAllowsApproval() || isGradeValueContinuousAndNotApproved(value)) {
            notApprovedGradeValuesCache.add(value);
            return true;
        }

        return false;
    }

    public boolean isApproved(final Grade grade) {
        return isApproved(grade.getValue());
    }

    public boolean isNotApproved(final Grade grade) {
        return isNotApproved(grade.getValue());
    }

    public void deleteGradeScaleEntry(final GradeScaleEntry entry) {
        entry.delete();
        reorderGrades();

        invalidateCache();
    }

    public Stream<GradeScaleEntry> getOrderedGradeScaleEntriesStream() {
        return getGradeScaleEntriesSet().stream().sorted(GradeScaleEntry.COMPARE_BY_GRADE_ORDER);
    }

    public void delete() {
        setDomainRoot(null);

        for (GradeScaleEntry entry : getGradeScaleEntriesSet()) {
            entry.delete();
        }

        invalidateCache();

        super.deleteDomainObject();
    }

    public void moveUp(GradeScaleEntry entry) {
        if (entry.isFirst()) {
            return;
        }
        swapGradeOrderWithNeighbour(entry, -1);
    }

    public void moveDown(GradeScaleEntry entry) {
        if (entry.isLast()) {
            return;
        }
        swapGradeOrderWithNeighbour(entry, 1);
    }

    private void swapGradeOrderWithNeighbour(GradeScaleEntry entry, int offset) {
        List<GradeScaleEntry> orderedEntries = getOrderedGradeScaleEntriesStream().toList();
        int index = orderedEntries.indexOf(entry);
        GradeScaleEntry neighbour = orderedEntries.get(index + offset);

        int entryOrder = entry.getGradeOrder();
        entry.setGradeOrder(neighbour.getGradeOrder());
        neighbour.setGradeOrder(entryOrder);
    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isInternalGradeScale() {
        return getInternalGradeScale();
    }

    public boolean isDefaultGradeScale() {
        return getDefaultGradeScale();
    }

    public boolean hasContinuousGrades() {
        return hasContinuousApprovedGrades() || hasContinuousReprovedGrades();
    }

    public void invalidateCache() {
        getApprovedGradeValuesCache().clear();
        getNotApprovedGradeValuesCache().clear();
        INTERNAL_CACHE.clear();
        GRADE_SCALE_CACHE.clear();
    }

    public LocalizedString getExtendedValue(Grade grade) {
        final Optional<GradeScaleEntry> entry = findGradeScaleEntry(grade.getValue());

        if (entry.isPresent()) {
            return entry.get().getDescription();
        }

        return CoreConfiguration.supportedLocales().stream().map(l -> new LocalizedString(l, grade.getValue()))
                .reduce((a, c) -> c.append(a)).orElse(new LocalizedString());
    }

    private Optional<GradeScaleEntry> findGradeScaleEntry(final String value) {
        return value == null ? Optional.empty() : Optional.ofNullable(of(value));
    }

    private GradeScaleEntry of(final String value) {
        final Map<String, GradeScaleEntry> entriesCache = INTERNAL_CACHE.computeIfAbsent(this, c -> new ConcurrentHashMap<>());
        return entriesCache.computeIfAbsent(value,
                c -> getGradeScaleEntriesSet().stream().filter(e -> Objects.equals(e.getValue(), value)).findAny().orElse(null));
    }

    private void reorderGrades() {
        AtomicInteger order = new AtomicInteger(1);
        getOrderedGradeScaleEntriesStream().forEach(entry -> entry.setGradeOrder(order.getAndIncrement()));
    }

    private static boolean isWithinInterval(final BigDecimal value, final BigDecimal minimum, final BigDecimal maximum) {
        return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
    }

    public boolean hasContinuousApprovedGrades() {
        return getMinimumApprovedGrade() != null && getMaximumApprovedGrade() != null;
    }

    public boolean hasContinuousReprovedGrades() {
        return getMinimumReprovedGrade() != null && getMaximumReprovedGrade() != null;
    }

    private boolean isGradeValueContinuousAndApproved(final String gradeValue) {
        if (!isNumeric(gradeValue)) {
            return false;
        }

        if (!hasContinuousApprovedGrades()) {
            return false;
        }

        return isWithinInterval(new BigDecimal(gradeValue), getMinimumApprovedGrade(), getMaximumApprovedGrade());
    }

    private boolean isGradeValueContinuousAndNotApproved(final String gradeValue) {
        if (!isNumeric(gradeValue)) {
            return false;
        }

        if (!hasContinuousReprovedGrades()) {
            return false;
        }

        return isWithinInterval(new BigDecimal(gradeValue), getMinimumReprovedGrade(), getMaximumReprovedGrade());
    }

    private boolean isGradeValueContinuous(final String gradeValue) {
        return hasContinuousGrades() && (isGradeValueContinuousAndApproved(gradeValue) || isGradeValueContinuousAndNotApproved(
                gradeValue));
    }

    // ############
    // # SERVICES #
    // ############

    public static GradeScale create(final String code, final LocalizedString name, final BigDecimal minimumReprovedGrade,
            final BigDecimal maximumReprovedGrade, final BigDecimal minimumApprovedGrade, final BigDecimal maximumApprovedGrade,
            final boolean internalGradeScale, final boolean active) {

        return new GradeScale(code, name, minimumReprovedGrade, maximumReprovedGrade, minimumApprovedGrade, maximumApprovedGrade,
                internalGradeScale, active);
    }

    public static Stream<GradeScale> findAll() {
        return FenixFramework.getDomainRoot().getGradeScalesSet().stream();
    }

    public static Stream<GradeScale> findDefault() {
        return findAll().filter(GradeScale::isDefaultGradeScale);
    }

    public static Optional<GradeScale> findUniqueDefault() {
        return findDefault().findFirst();
    }

    public static Stream<GradeScale> findByCode(final String code) {
        return findAll().filter(gradeScale -> Objects.equals(code, gradeScale.getCode()));
    }

    public static Optional<GradeScale> findUniqueByCode(final String code) {
        return findByCode(code).findFirst();
    }

    public static Stream<GradeScale> findActive() {
        return findAll().filter(GradeScale::isActive);
    }

    public static Stream<GradeScale> findActive(boolean internalGradeScale) {
        return findActive().filter(gradeScale -> gradeScale.isInternalGradeScale() == internalGradeScale);
    }

    public static GradeScale getGradeScaleByCode(final String code) {
        return code == null ? null : GRADE_SCALE_CACHE.computeIfAbsent(code, key -> findUniqueByCode(key).get());
    }

    public static boolean isNumeric(final String value) {
        return NumberUtils.isNumber(value);
    }

    /*
     * Lazily created: Fenix Framework materializes persisted instances without invoking constructors,
     * so field initializers never run for objects loaded from the database — the field
     * starts out null and first use must create the cache here.
     */
    private Set<String> getApprovedGradeValuesCache() {
        if (approvedGradeValuesCache == null) {
            approvedGradeValuesCache = new HashSet<>();
        }
        return approvedGradeValuesCache;
    }

    private Set<String> getNotApprovedGradeValuesCache() {
        if (notApprovedGradeValuesCache == null) {
            notApprovedGradeValuesCache = new HashSet<>();
        }
        return notApprovedGradeValuesCache;
    }
}
