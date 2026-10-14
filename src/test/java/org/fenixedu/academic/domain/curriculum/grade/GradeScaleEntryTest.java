package org.fenixedu.academic.domain.curriculum.grade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class GradeScaleEntryTest {

    @After
    public void cleanup() {
        GradeScale.findAll().collect(Collectors.toList()).forEach(GradeScale::delete);
    }

    @Test
    public void testGradeScaleEntry_COMPARE_BY_GRADE_ORDER() {
        GradeScale gradeScale = new GradeScale();
        GradeScaleEntry entryWithOrderOne =
                GradeScaleEntry.create(gradeScale, "ORDER_ONE", new LocalizedString().with(Locale.getDefault(), "Order one"),
                        true);
        GradeScaleEntry entryWithOrderTwo =
                GradeScaleEntry.create(gradeScale, "ORDER_TWO", new LocalizedString().with(Locale.getDefault(), "Order two"),
                        true);

        assertEquals(1, entryWithOrderOne.getGradeOrder());
        assertEquals(2, entryWithOrderTwo.getGradeOrder());
        assertTrue(GradeScaleEntry.COMPARE_BY_GRADE_ORDER.compare(entryWithOrderOne, entryWithOrderTwo) < 0);
        assertTrue(GradeScaleEntry.COMPARE_BY_GRADE_ORDER.compare(entryWithOrderTwo, entryWithOrderOne) > 0);

        // An entry always compares as equal to itself
        assertEquals(0, GradeScaleEntry.COMPARE_BY_GRADE_ORDER.compare(entryWithOrderOne, entryWithOrderOne));

        // Sorting a list with the comparator yields the entries ordered by grade order
        List<GradeScaleEntry> outOfOrderEntries = Arrays.asList(entryWithOrderTwo, entryWithOrderOne);
        outOfOrderEntries.sort(GradeScaleEntry.COMPARE_BY_GRADE_ORDER);
        assertEquals(Arrays.asList(entryWithOrderOne, entryWithOrderTwo), outOfOrderEntries);
    }

    @Test
    public void testGradeScaleEntry_isLast() {
        // An entry created on a scale by itself is both the last and the first entry
        GradeScale singleEntryScale = new GradeScale();
        GradeScaleEntry singleEntry = GradeScaleEntry.create(singleEntryScale, "LAST_SINGLE",
                new LocalizedString().with(Locale.getDefault(), "Single entry"), true);
        assertTrue(singleEntry.isLast());

        // In an ordered scale the entry with the highest order is the last one
        GradeScale gradeScale = new GradeScale();
        GradeScaleEntry approvedEntry =
                GradeScaleEntry.create(gradeScale, "LAST_AP", new LocalizedString().with(Locale.getDefault(), "Approved"), true);
        GradeScaleEntry notApprovedEntry =
                GradeScaleEntry.create(gradeScale, "LAST_RO", new LocalizedString().with(Locale.getDefault(), "Not approved"),
                        false);

        assertEquals(2, approvedEntry.getGradeOrder());
        assertTrue(approvedEntry.isLast());
        assertFalse(notApprovedEntry.isLast());
    }

    @Test
    public void testGradeScaleEntry_isFirst() {
        // An entry created on a scale by itself is both the last and the first entry
        GradeScale singleEntryScale = new GradeScale();
        GradeScaleEntry singleEntry = GradeScaleEntry.create(singleEntryScale, "FIRST_SINGLE",
                new LocalizedString().with(Locale.getDefault(), "Single entry"), true);
        assertTrue(singleEntry.isFirst());

        // In an ordered scale the entry with the lowest order is the first one
        GradeScale gradeScale = new GradeScale();
        GradeScaleEntry approvedEntry =
                GradeScaleEntry.create(gradeScale, "FIRST_AP", new LocalizedString().with(Locale.getDefault(), "Approved"), true);
        GradeScaleEntry notApprovedEntry =
                GradeScaleEntry.create(gradeScale, "FIRST_RO", new LocalizedString().with(Locale.getDefault(), "Not approved"),
                        false);

        assertEquals(1, notApprovedEntry.getGradeOrder());
        assertTrue(notApprovedEntry.isFirst());
        assertFalse(approvedEntry.isFirst());
    }

    @Test
    public void testGradeScaleEntry_create() {
        GradeScale gradeScale = new GradeScale();
        GradeScaleEntry firstApprovedEntry = GradeScaleEntry.create(gradeScale, "CREATE_AP_1",
                new LocalizedString().with(Locale.getDefault(), "First approved entry"), true);
        assertNotNull(firstApprovedEntry);
        assertEquals(gradeScale, firstApprovedEntry.getGradeScale());
        assertEquals("CREATE_AP_1", firstApprovedEntry.getValue());
        assertEquals("First approved entry", firstApprovedEntry.getDescription().getContent(Locale.getDefault()));
        assertTrue(firstApprovedEntry.isAllowsApproval());
        assertEquals(1, firstApprovedEntry.getGradeOrder());
        assertNotNull(firstApprovedEntry.getRootDomainObject());

        // An approval entry is always appended at the end of the scale
        GradeScaleEntry secondApprovedEntry = GradeScaleEntry.create(gradeScale, "CREATE_AP_2",
                new LocalizedString().with(Locale.getDefault(), "Second approved entry"), true);
        assertEquals(2, secondApprovedEntry.getGradeOrder());

        // A reproved entry is appended while the scale has no approved entries
        GradeScale reprovedScale = new GradeScale();
        GradeScaleEntry reprovedA = GradeScaleEntry.create(reprovedScale, "CREATE_RE_A",
                new LocalizedString().with(Locale.getDefault(), "Reproved A"), false);
        GradeScaleEntry reprovedB = GradeScaleEntry.create(reprovedScale, "CREATE_RE_B",
                new LocalizedString().with(Locale.getDefault(), "Reproved B"), false);
        assertEquals(1, reprovedA.getGradeOrder());
        assertEquals(2, reprovedB.getGradeOrder());

        // Once an approved entry exists it is appended after the reproved entries
        GradeScaleEntry mainApprovedEntry = GradeScaleEntry.create(reprovedScale, "CREATE_AP_MAIN",
                new LocalizedString().with(Locale.getDefault(), "Approved"), true);
        assertEquals(3, mainApprovedEntry.getGradeOrder());
        GradeScaleEntry anotherApprovedEntry = GradeScaleEntry.create(reprovedScale, "CREATE_AP_SECOND",
                new LocalizedString().with(Locale.getDefault(), "Approved"), true);
        assertEquals(4, anotherApprovedEntry.getGradeOrder());

        // A not approved entry is inserted right before the approved entry, shifting it up one slot
        GradeScaleEntry notApprovedBeforeApproved = GradeScaleEntry.create(reprovedScale, "CREATE_RO_MAIN",
                new LocalizedString().with(Locale.getDefault(), "Not approved"), false);
        assertEquals(3, notApprovedBeforeApproved.getGradeOrder());
        assertEquals(4, mainApprovedEntry.getGradeOrder());
        assertEquals(5, anotherApprovedEntry.getGradeOrder());
        assertEquals(Arrays.asList(reprovedA, reprovedB, notApprovedBeforeApproved, mainApprovedEntry, anotherApprovedEntry),
                reprovedScale.getOrderedGradeScaleEntriesList());

        // Creating an entry with a blank value is rejected
        assertThrows(DomainException.class,
                () -> GradeScaleEntry.create(gradeScale, "   ", new LocalizedString().with(Locale.getDefault(), "Description"),
                        true));

        // Creating an entry without a description is rejected
        assertThrows(DomainException.class, () -> GradeScaleEntry.create(gradeScale, "CREATE_NULL_DESCRIPTION", null, true));
        assertThrows(DomainException.class,
                () -> GradeScaleEntry.create(gradeScale, "CREATE_EMPTY_DESCRIPTION", new LocalizedString(), true));

        // Creating an entry with a value already registered in the same scale is rejected
        GradeScaleEntry.create(gradeScale, "CREATE_DUP",
                new LocalizedString().with(Locale.getDefault(), "First with the duplicated value"), true);
        assertThrows(DomainException.class, () -> GradeScaleEntry.create(gradeScale, "CREATE_DUP",
                new LocalizedString().with(Locale.getDefault(), "Second with the same value"), true));
    }
}