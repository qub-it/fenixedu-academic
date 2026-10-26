package org.fenixedu.academic.domain.curriculum.grade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class GradeScaleTest {

    private static final LocalizedString GRADE_SCALE_NAME = new LocalizedString(Locale.ENGLISH, "Grade Scale");
    private static final String QUALITATIVE_GRADE_SCALE_CODE = "QUALITATIVE";
    private static final BigDecimal MINIMUM_APPROVED_GRADE = new BigDecimal("9.5");
    private static final BigDecimal MAXIMUM_APPROVED_GRADE = new BigDecimal("20");
    private static final BigDecimal MINIMUM_REPROVED_GRADE = new BigDecimal("0");
    private static final BigDecimal MAXIMUM_REPROVED_GRADE = new BigDecimal("9.499");
    private GradeScale gradeScale;

    @Before
    public void setUp() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            gradeScale = GradeScale.create(QUALITATIVE_GRADE_SCALE_CODE, GRADE_SCALE_NAME, null, null, null, null, false, true);
            return null;
        });
    }

    @After
    public void cleanUp() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            GradeScale.findAll().forEach(GradeScale::delete);
            return null;
        });
    }

    private static String describeOrderedEntries(final GradeScale target) {
        return target.getOrderedGradeScaleEntriesStream().map(entry -> entry.getValue() + "#" + entry.getGradeOrder())
                .collect(Collectors.joining(" < "));
    }

    @Test
    public void testGradeScale_COMPARE_BY_NAME() {
        gradeScale.setName(new LocalizedString(Locale.ENGLISH, "Alpha"));
        GradeScale betaScale = GradeScale.create("COMPARE_BY_NAME_BETA", GRADE_SCALE_NAME, null, null, null, null, false, true);
        betaScale.setName(new LocalizedString(Locale.ENGLISH, "Beta"));
        assertTrue(GradeScale.COMPARE_BY_NAME.compare(gradeScale, betaScale) < 0);
        assertTrue(GradeScale.COMPARE_BY_NAME.compare(betaScale, gradeScale) > 0);

        // a scale is equal to itself
        assertEquals(0, GradeScale.COMPARE_BY_NAME.compare(gradeScale, gradeScale));

        // scales sharing the same name fall back to the external id
        gradeScale.setName(new LocalizedString(Locale.ENGLISH, "Same Name"));
        betaScale.setName(new LocalizedString(Locale.ENGLISH, "Same Name"));
        assertTrue(GradeScale.COMPARE_BY_NAME.compare(gradeScale, betaScale) < 0);
        assertTrue(GradeScale.COMPARE_BY_NAME.compare(betaScale, gradeScale) > 0);
    }

    @Test
    public void testGradeScale_markAsDefaultGradeScale() {
        // no scale is the default one to begin with
        assertEquals(0, GradeScale.findDefault().count());

        // marking a scale makes it the only default one
        gradeScale.markAsDefaultGradeScale();
        assertTrue(gradeScale.isDefaultGradeScale());

        // marking the same scale again is idempotent and keeps a single default
        gradeScale.markAsDefaultGradeScale();
        assertTrue(gradeScale.isDefaultGradeScale());
        assertEquals(1, GradeScale.findDefault().count());

        // marking another scale transfers the default flag, so there is never more than one default
        GradeScale otherScale = GradeScale.create("MARK_AS_DEFAULT_OTHER", GRADE_SCALE_NAME, null, null, null, null, false, true);
        otherScale.markAsDefaultGradeScale();
        assertFalse(gradeScale.isDefaultGradeScale());
        assertTrue(otherScale.isDefaultGradeScale());
        assertEquals(1, GradeScale.findDefault().count());
        assertSame(otherScale, GradeScale.findUniqueDefault().get());
    }

    @Test
    public void testGradeScale_getOrderedGradeScaleEntriesStream() {
        // a scale without entries streams nothing
        assertEquals(0, gradeScale.getOrderedGradeScaleEntriesStream().count());

        gradeScale.createGradeScaleEntry("AP", new LocalizedString(Locale.ENGLISH, "Approved AP"), true);
        assertEquals("AP#1", describeOrderedEntries(gradeScale));

        // entries that do not allow approval are inserted below the lowest approving entry, which pushes the approving entry to the end
        gradeScale.createGradeScaleEntry("F", new LocalizedString(Locale.ENGLISH, "Not approved F"), false);
        gradeScale.createGradeScaleEntry("C", new LocalizedString(Locale.ENGLISH, "Not approved C"), false);
        assertEquals("F#1 < C#2 < AP#3", describeOrderedEntries(gradeScale));
    }

    @Test
    public void testGradeScale_moveUp() {
        GradeScaleEntry firstReprovedEntry =
                gradeScale.createGradeScaleEntry("F", new LocalizedString(Locale.ENGLISH, "Not approved F"), false);
        GradeScaleEntry secondReprovedEntry =
                gradeScale.createGradeScaleEntry("C", new LocalizedString(Locale.ENGLISH, "Not approved C"), false);
        GradeScaleEntry approvedEntry =
                gradeScale.createGradeScaleEntry("AP", new LocalizedString(Locale.ENGLISH, "Approved AP"), true);
        assertEquals("F#1 < C#2 < AP#3", describeOrderedEntries(gradeScale));

        // moving an entry up swaps its grade order with the entry above it
        gradeScale.moveUp(approvedEntry);
        assertEquals("F#1 < AP#2 < C#3", describeOrderedEntries(gradeScale));

        // moving it up again reaches the first position
        gradeScale.moveUp(approvedEntry);
        assertEquals("AP#1 < F#2 < C#3", describeOrderedEntries(gradeScale));

        // moving the first entry up changes nothing
        gradeScale.moveUp(approvedEntry);
        assertEquals("AP#1 < F#2 < C#3", describeOrderedEntries(gradeScale));

        // a middle entry only swaps with its direct predecessor
        gradeScale.moveUp(secondReprovedEntry);
        assertEquals("AP#1 < C#2 < F#3", describeOrderedEntries(gradeScale));
    }

    @Test
    public void testGradeScale_moveDown() {
        GradeScaleEntry firstReprovedEntry =
                gradeScale.createGradeScaleEntry("F", new LocalizedString(Locale.ENGLISH, "Not approved F"), false);
        GradeScaleEntry secondReprovedEntry =
                gradeScale.createGradeScaleEntry("C", new LocalizedString(Locale.ENGLISH, "Not approved C"), false);
        GradeScaleEntry approvedEntry =
                gradeScale.createGradeScaleEntry("AP", new LocalizedString(Locale.ENGLISH, "Approved AP"), true);
        assertEquals("F#1 < C#2 < AP#3", describeOrderedEntries(gradeScale));

        // moving the last entry down changes nothing
        gradeScale.moveDown(approvedEntry);
        assertEquals("F#1 < C#2 < AP#3", describeOrderedEntries(gradeScale));

        // moving an entry down swaps its grade order with the entry below it
        gradeScale.moveDown(firstReprovedEntry);
        assertEquals("C#1 < F#2 < AP#3", describeOrderedEntries(gradeScale));

        // moving it down again reaches the last position
        gradeScale.moveDown(firstReprovedEntry);
        assertEquals("C#1 < AP#2 < F#3", describeOrderedEntries(gradeScale));

        // the first entry only swaps with its direct successor
        gradeScale.moveDown(secondReprovedEntry);
        assertEquals("AP#1 < C#2 < F#3", describeOrderedEntries(gradeScale));
    }
}
