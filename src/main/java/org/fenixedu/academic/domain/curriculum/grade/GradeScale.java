package org.fenixedu.academic.domain.curriculum.grade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
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

    private Map<String, Object> CACHE_APPROVED_GRADE_VALUES = null;
    private Map<String, Object> CACHE_NOT_APPROVED_GRADE_VALUES = null;

    private static final Map<GradeScale, Map<String, GradeScaleEntry>> INTERNAL_CACHE = new ConcurrentHashMap<>();

    public static final Comparator<GradeScale> COMPARE_BY_NAME =
            Comparator.comparing(GradeScale::getName).thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

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

        {
            if (rightGrade == null || rightGrade.isEmpty()) {
                return 1;
            }

            if (leftGrade == null || leftGrade.isEmpty()) {
                return -1;
            }
        }

        if (!leftGrade.getGradeScale().equals(rightGrade.getGradeScale())) {
            throw new DomainException("Grade.unsupported.comparison.of.grades.of.different.scales", leftGrade.toString(),
                    rightGrade.toString());
        }

        {
            final boolean isLeftApproved = isApproved(leftGrade);
            final boolean isRightApproved = isApproved(rightGrade);

            if (isLeftApproved && !isRightApproved) {
                return 1;
            } else if (!isLeftApproved && isRightApproved) {
                return -1;
            }
        }

        final Optional<GradeScaleEntry> gradeEntryLeft = findGradeScaleEntry(leftGrade.getValue());
        final Optional<GradeScaleEntry> gradeEntryRight = findGradeScaleEntry(rightGrade.getValue());

        if (gradeEntryLeft.isPresent() && gradeEntryRight.isPresent()) {
            return Integer.compare(gradeEntryLeft.get().getGradeOrder(), gradeEntryRight.get().getGradeOrder());
        } else {
            final boolean isLeftGradeValueContinuous = isGradeValueContinuous(leftGrade.getValue());
            final boolean isRightGradeValueContinuous = isGradeValueContinuous(rightGrade.getValue());

            if (isLeftGradeValueContinuous && !isRightGradeValueContinuous) {
                return 1;
            } else if (!isLeftGradeValueContinuous && isRightGradeValueContinuous) {
                return -1;
            } else if (isLeftGradeValueContinuous && isRightGradeValueContinuous) {
                return leftGrade.getNumericValue().compareTo(rightGrade.getNumericValue());
            } else {
                throw new DomainException("Grade.unsupported.comparison.of.grades.of.different.scales");
            }
        }
    }

    public boolean isApproved(final String value) {
        if (CACHE_APPROVED_GRADE_VALUES == null) {
            CACHE_APPROVED_GRADE_VALUES = new HashMap<>();
        }

        if (CACHE_APPROVED_GRADE_VALUES.containsKey(value)) {
            return true;
        }

        Optional<GradeScaleEntry> matchEntry = findGradeScaleEntry(value);

        if (matchEntry.isPresent() && matchEntry.get().isAllowsApproval()) {
            CACHE_APPROVED_GRADE_VALUES.put(value, matchEntry.get());
            return true;
        }

        if (isGradeValueContinuousAndApproved(value)) {
            CACHE_APPROVED_GRADE_VALUES.put(value, value);
            return true;
        }

        return false;
    }

    public boolean isNotApproved(final String value) {
        if (CACHE_NOT_APPROVED_GRADE_VALUES == null) {
            CACHE_NOT_APPROVED_GRADE_VALUES = new HashMap<>();
        }

        if (CACHE_NOT_APPROVED_GRADE_VALUES.containsKey(value)) {
            return true;
        }

        Optional<GradeScaleEntry> matchEntry = findGradeScaleEntry(value);

        if (matchEntry.isPresent() && !matchEntry.get().isAllowsApproval()) {
            CACHE_NOT_APPROVED_GRADE_VALUES.put(value, matchEntry.get());
            return true;
        }

        if (isGradeValueContinuousAndNotApproved(value)) {
            CACHE_NOT_APPROVED_GRADE_VALUES.put(value, value);
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

    @Deprecated(forRemoval = true)
    public List<GradeScaleEntry> getOrderedGradeScaleEntriesList() {
        final List<GradeScaleEntry> gradeEntriesList = new ArrayList<>(getGradeScaleEntriesSet());
        gradeEntriesList.sort(GradeScaleEntry.COMPARE_BY_GRADE_ORDER);

        return gradeEntriesList;
    }

    @Deprecated(forRemoval = true)
    public Set<GradeScaleEntry> getOrderedGradeScaleEntriesSet() {
        final Set<GradeScaleEntry> result = new TreeSet<>(GradeScaleEntry.COMPARE_BY_GRADE_ORDER);
        result.addAll(getGradeScaleEntriesSet());

        return result;
    }

    public void delete() {
        setDomainRoot(null);

        for (GradeScaleEntry entry : getGradeScaleEntriesSet()) {
            entry.delete();
        }

        super.deleteDomainObject();
    }

    /**
     * @deprecated Use {@link #moveDown()}
     */
    @Deprecated(forRemoval = true)
    public void orderNextGradeScaleEntry(final GradeScaleEntry entry) {
        if (entry.isLast()) {
            return;
        }

        final List<GradeScaleEntry> orderedEntries = getOrderedGradeScaleEntriesList();
        int indexOf = orderedEntries.indexOf(entry);
        GradeScaleEntry nextEntry = orderedEntries.get(indexOf + 1);

        int tempOrder = nextEntry.getGradeOrder();
        nextEntry.setGradeOrder(entry.getGradeOrder());
        entry.setGradeOrder(tempOrder);
    }

    /**
     * @deprecated Use {@link #moveUp()}
     */
    @Deprecated(forRemoval = true)
    public void orderPreviousGradeScaleEntry(final GradeScaleEntry entry) {
        if (entry.isFirst()) {
            return;
        }

        final List<GradeScaleEntry> orderedEntries = getOrderedGradeScaleEntriesList();
        int indexOf = orderedEntries.indexOf(entry);
        GradeScaleEntry previousEntry = orderedEntries.get(indexOf - 1);

        int tempOrder = previousEntry.getGradeOrder();
        previousEntry.setGradeOrder(entry.getGradeOrder());
        entry.setGradeOrder(tempOrder);
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
        List<GradeScaleEntry> orderedEntries = getOrderedGradeScaleEntriesList();
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

    /**
     * @deprecated Use {@link #hasContinuousGrades()} instead. Beware that the replacement has the opposite meaning:
     *             this method returns {@code true} when this grade scale has no intervals defined, while
     *             {@code hasContinuousGrades()} returns {@code true} when at least one interval is defined.
     *             Also, this method only checks the minimum interval bound while hasContinuousGrades checks minimum and maximum bounds are both defined
     *             but {@link ##checksRules()} ensures there can't be half intervals defined. So the method equivalence holds because of that
     */
    @Deprecated(forRemoval = true)
    public boolean hasRestrictedGrades() {
        return !(getMinimumApprovedGrade() != null || getMinimumReprovedGrade() != null);
    }

    public void invalidateCache() {
        if (this.CACHE_APPROVED_GRADE_VALUES != null) {
            this.CACHE_APPROVED_GRADE_VALUES.clear();
        }

        if (this.CACHE_NOT_APPROVED_GRADE_VALUES != null) {
            this.CACHE_NOT_APPROVED_GRADE_VALUES.clear();
        }

        INTERNAL_CACHE.clear();
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
        return Optional.ofNullable(of(value));
    }

    private GradeScaleEntry of(final String value) {
        final Map<String, GradeScaleEntry> entriesCache = INTERNAL_CACHE.computeIfAbsent(this, c -> new ConcurrentHashMap<>());
        return entriesCache.computeIfAbsent(value,
                c -> getGradeScaleEntriesSet().stream().filter(e -> Objects.equals(e.getValue(), value)).findAny().orElse(null));
    }

    private void reorderGrades() {
        final List<GradeScaleEntry> gradeEntriesList = getOrderedGradeScaleEntriesList();

        int order = 1;
        for (GradeScaleEntry gradeScaleEntry : gradeEntriesList) {
            gradeScaleEntry.setGradeOrder(order);
            order++;
        }
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

    private static Map<String, GradeScale> GRADE_SCALE_CACHE = new HashMap<>();

    public static GradeScale getGradeScaleByCode(final String code) {
        if (!GRADE_SCALE_CACHE.containsKey(code)) {
            GRADE_SCALE_CACHE.put(code, findUniqueByCode(code).get());
        }

        return GRADE_SCALE_CACHE.get(code);
    }

    public static boolean isNumeric(final String value) {
        return NumberUtils.isNumber(value);
    }

    // ##########
    // # CACHES #
    // ##########

}
