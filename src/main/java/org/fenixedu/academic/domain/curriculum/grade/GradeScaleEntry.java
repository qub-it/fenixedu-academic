package org.fenixedu.academic.domain.curriculum.grade;


import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class GradeScaleEntry extends GradeScaleEntry_Base {

    public static final Comparator<GradeScaleEntry> COMPARE_BY_GRADE_ORDER =
            Comparator.comparingInt(GradeScaleEntry::getGradeOrder).thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);

    public GradeScaleEntry() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    protected GradeScaleEntry(final GradeScale gradeScaleInstance, final String value, final LocalizedString description,
            final boolean allowsApproval) {
        this();

        setGradeScale(gradeScaleInstance);
        setValue(value);
        setDescription(description);
        setAllowsApproval(allowsApproval);
        setGradeOrder(calculateGradeOrder());

        checkRules();
    }

    private int calculateGradeOrder() {
        if (isAllowsApproval()) {
            // Approval entries are appended after every existing entry
            return nextGradeOrder();
        }

        // Non-approved entries are inserted right before the first approved entry.
        // Defaults to appending at the end when the scale has no approved entries yet.
        AtomicInteger newEntryOrder = new AtomicInteger(nextGradeOrder());
        Optional<GradeScaleEntry> lowestApprovedEntry =
                getGradeScale().getOrderedGradeScaleEntriesSet().stream().filter(GradeScaleEntry::isAllowsApproval).findFirst();

        lowestApprovedEntry.ifPresent(approvedEntry -> {
            // The new entry takes the approved entry's current order
            int newOrder = approvedEntry.getGradeOrder();
            newEntryOrder.set(newOrder);

            // Make room by moving the approved entry up one slot
            approvedEntry.setGradeOrder(newOrder + 1);

            // shift every following entry up by one
            AtomicInteger nextOrderToAssign = new AtomicInteger(newOrder + 2);
            approvedEntry.getEntriesAfter().forEach(e -> e.setGradeOrder(nextOrderToAssign.getAndIncrement()));
        });

        return newEntryOrder.get();
    }

    private int nextGradeOrder() {
        return getGradeScale().getGradeScaleEntriesSet().stream().max(COMPARE_BY_GRADE_ORDER)
                .map(entry -> entry.getGradeOrder() + 1).orElse(1);
    }

    private void checkRules() {
        if (getGradeScale() == null) {
            throw new DomainException("error.GradeScaleEntry.gradeScale.is.required");
        }

        if (StringUtils.isBlank(getValue())) {
            throw new DomainException("error.GradeScaleEntry.value.is.required");
        }

        if (getDescription() == null || getDescription().isEmpty()) {
            throw new DomainException("error.GradeScaleEntry.description.is.required");
        }

        if (find(getGradeScale(), getValue()).count() >= 2) {
            throw new DomainException("error.GradeScaleEntry.already.registered.in.scale");
        }

    }

    public void edit(final LocalizedString description) {
        super.setDescription(description);

        checkRules();
    }

    public boolean isAllowsApproval() {
        return getAllowsApproval();
    }

    void delete() {
        setRootDomainObject(null);
        setGradeScale(null);

        super.deleteDomainObject();
    }

    private List<GradeScaleEntry> getEntriesAfter() {
        List<GradeScaleEntry> orderedEntries = getGradeScale().getOrderedGradeScaleEntriesList();
        return orderedEntries.subList(orderedEntries.indexOf(this) + 1, orderedEntries.size());
    }

    public boolean isLast() {
        final List<GradeScaleEntry> orderedEntries = getGradeScale().getOrderedGradeScaleEntriesList();
        return orderedEntries.indexOf(this) == orderedEntries.size() - 1;
    }

    public boolean isFirst() {
        return getGradeScale().getOrderedGradeScaleEntriesList().indexOf(this) == 0;
    }

    // ########
    // SERVICES
    // ########

    static GradeScaleEntry create(final GradeScale gradeScaleInstance, final String value, final LocalizedString description,
            final boolean allowsApproval) {
        return new GradeScaleEntry(gradeScaleInstance, value, description, allowsApproval);
    }

    public static Stream<GradeScaleEntry> findAll() {
        return Bennu.getInstance().getGradeScaleEntriesSet().stream();
    }

    public static Stream<GradeScaleEntry> find(GradeScale gradeScale) {
        return gradeScale.getGradeScaleEntriesSet().stream();
    }

    public static Stream<GradeScaleEntry> find(GradeScale gradeScale, final String value) {
        return find(gradeScale).filter(e -> e.getValue().equals(value));
    }
    
}
