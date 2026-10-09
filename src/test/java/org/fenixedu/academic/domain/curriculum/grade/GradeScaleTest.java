package org.fenixedu.academic.domain.curriculum.grade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
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

    @Test
    public void testGradeScale_hasContinuousGrades() {
        // a scale without any interval defined has no continuous grades
        assertFalse(gradeScale.hasContinuousGrades());

        // an approved interval alone makes the scale continuous
        gradeScale.edit(GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE, true, false);
        assertTrue(gradeScale.hasContinuousGrades());

        // a reproved interval alone also makes the scale continuous
        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, null, null, true, false);
        assertTrue(gradeScale.hasContinuousGrades());

        // both intervals defined also make the scale continuous
        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, MINIMUM_APPROVED_GRADE,
                MAXIMUM_APPROVED_GRADE, true, false);
        assertTrue(gradeScale.hasContinuousGrades());

        // removing both intervals makes the scale non continuous again
        gradeScale.edit(GRADE_SCALE_NAME, null, null, null, null, true, false);
        assertFalse(gradeScale.hasContinuousGrades());
    }

    // NOTE: This test will be deleted after deleting deprecated #hasRestrictedGrades
    @Test
    public void testGradeScale_hasRestrictedGrades_equalsNegationOfHasContinuousGrades() {
        // a scale without any interval defined, has no continuous grades and is reported as restricted
        assertFalse(gradeScale.hasContinuousGrades());
        assertTrue(gradeScale.hasRestrictedGrades());
        assertEquals(!gradeScale.hasContinuousGrades(), gradeScale.hasRestrictedGrades());

        // an approved interval alone makes the scale continuous
        gradeScale.edit(GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE, true, false);
        assertTrue(gradeScale.hasContinuousGrades());
        assertFalse(gradeScale.hasRestrictedGrades());
        assertEquals(!gradeScale.hasContinuousGrades(), gradeScale.hasRestrictedGrades());

        // a reproved interval alone also makes the scale continuous
        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, null, null, true, false);
        assertTrue(gradeScale.hasContinuousGrades());
        assertFalse(gradeScale.hasRestrictedGrades());
        assertEquals(!gradeScale.hasContinuousGrades(), gradeScale.hasRestrictedGrades());

        // both intervals at once keep the same relationship
        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, MINIMUM_APPROVED_GRADE,
                MAXIMUM_APPROVED_GRADE, true, false);
        assertTrue(gradeScale.hasContinuousGrades());
        assertFalse(gradeScale.hasRestrictedGrades());
        assertEquals(!gradeScale.hasContinuousGrades(), gradeScale.hasRestrictedGrades());

        // removing both intervals makes the scale non continuous and restricted again
        gradeScale.edit(GRADE_SCALE_NAME, null, null, null, null, true, false);
        assertFalse(gradeScale.hasContinuousGrades());
        assertTrue(gradeScale.hasRestrictedGrades());
        assertEquals(!gradeScale.hasContinuousGrades(), gradeScale.hasRestrictedGrades());
    }

    @Test
    public void testGradeScale_hasContinuousApprovedGrades() {
        // without any interval the approved grades are not continuous
        assertFalse(gradeScale.hasContinuousApprovedGrades());

        // a minimum without a maximum does not define a continuous interval
        gradeScale.setMinimumApprovedGrade(MINIMUM_APPROVED_GRADE);
        gradeScale.setMaximumApprovedGrade(null);
        assertFalse(gradeScale.hasContinuousApprovedGrades());

        // a maximum without a minimum does not define a continuous interval either
        gradeScale.setMinimumApprovedGrade(null);
        gradeScale.setMaximumApprovedGrade(MAXIMUM_APPROVED_GRADE);
        assertFalse(gradeScale.hasContinuousApprovedGrades());

        // both bounds together define a continuous approved interval
        gradeScale.setMinimumApprovedGrade(MINIMUM_APPROVED_GRADE);
        gradeScale.setMaximumApprovedGrade(MAXIMUM_APPROVED_GRADE);
        assertTrue(gradeScale.hasContinuousApprovedGrades());

        // removing the approved interval turns it back to a qualitative scale
        gradeScale.setMinimumApprovedGrade(null);
        gradeScale.setMaximumApprovedGrade(null);
        assertFalse(gradeScale.hasContinuousApprovedGrades());
    }

    @Test
    public void testGradeScale_hasContinuousReprovedGrades() {
        // without any interval the reproved grades are not continuous
        assertFalse(gradeScale.hasContinuousReprovedGrades());

        // a minimum without a maximum does not define a continuous interval
        gradeScale.setMinimumReprovedGrade(MINIMUM_REPROVED_GRADE);
        gradeScale.setMaximumReprovedGrade(null);
        assertFalse(gradeScale.hasContinuousReprovedGrades());

        // a maximum without a minimum does not define a continuous interval either
        gradeScale.setMinimumReprovedGrade(null);
        gradeScale.setMaximumReprovedGrade(MAXIMUM_REPROVED_GRADE);
        assertFalse(gradeScale.hasContinuousReprovedGrades());

        // both bounds together define a continuous reproved interval
        gradeScale.setMinimumReprovedGrade(MINIMUM_REPROVED_GRADE);
        gradeScale.setMaximumReprovedGrade(MAXIMUM_REPROVED_GRADE);
        assertTrue(gradeScale.hasContinuousReprovedGrades());
    }

    @Test
    public void testGradeScale_findByCode() {
        GradeScale otherScale = GradeScale.create("FIND_BY_CODE_OTHER", GRADE_SCALE_NAME, null, null, null, null, false, true);
        assertEquals(1, GradeScale.findByCode("FIND_BY_CODE_OTHER").count());
        assertEquals(otherScale, GradeScale.findByCode("FIND_BY_CODE_OTHER").findFirst().get());

        // an unknown code matches nothing
        assertEquals(0, GradeScale.findByCode("UNKNOWN_CODE").count());

        // a null code matches nothing instead of failing
        assertEquals(0, GradeScale.findByCode(null).count());
    }

    @Test
    public void testGradeScale_findActive() {
        GradeScale inactiveScale =
                GradeScale.create("FIND_ACTIVE_INACTIVE", GRADE_SCALE_NAME, null, null, null, null, false, false);
        //inactiveScale.edit(GRADE_SCALE_NAME, null, null, null, null, false, false);
        GradeScale internalScale =
                GradeScale.create("FIND_ACTIVE_INTERNAL", GRADE_SCALE_NAME, null, null, null, null, true, true);
        //internalScale.edit(GRADE_SCALE_NAME, null, null, null, null, true, true);

        // only the active scales are returned, the inactive one is left out
        assertEquals(2, GradeScale.findActive().count());
        assertEquals(List.of(gradeScale, internalScale), GradeScale.findActive().toList());

        // test internalGradeScale flag
        assertEquals(1, GradeScale.findActive(true).count());
        assertEquals(internalScale, GradeScale.findActive(true).findFirst().get());

        assertEquals(1, GradeScale.findActive(false).count());
        assertEquals(gradeScale, GradeScale.findActive(false).findFirst().get());
    }

    /**
     * The methods below are private, that is why the tests are commented out
     */

//    @Test
//    public void testGradeScale_isWithinInterval() {
//        // a value strictly between the bounds is within the interval
//        assertTrue(GradeScale.isWithinInterval(new BigDecimal("15"), MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE));
//
//        // the lower bound is included
//        assertTrue(GradeScale.isWithinInterval(MINIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE));
//
//        // the upper bound is included
//        assertTrue(GradeScale.isWithinInterval(MAXIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE));
//
//        // a single value interval accepts that exact value and nothing else
//        assertTrue(GradeScale.isWithinInterval(MINIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE));
//        assertFalse(GradeScale.isWithinInterval(MAXIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE));
//
//        // a value just below the lower bound is outside the interval
    //        assertFalse(GradeScale.isWithinInterval(MINIMUM_APPROVED_GRADE.subtract(ONE_CENT), MINIMUM_APPROVED_GRADE,
//                MAXIMUM_APPROVED_GRADE));
//
//        // a value just above the upper bound is outside the interval
    //        assertFalse(GradeScale.isWithinInterval(MAXIMUM_APPROVED_GRADE.add(ONE_CENT), MINIMUM_APPROVED_GRADE,
//                MAXIMUM_APPROVED_GRADE));
//    }
//
//    @Test
//    public void testGradeScale_isGradeValueContinuousAndApproved() {
//        // no approved intervals configured, so must be return false
//        assertFalse(gradeScale.isGradeValueContinuousAndApproved("15"));
//
//        // configuring an approved interval makes the values inside it continuous and approved
//        gradeScale.edit(GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE, true, false);
//        assertTrue(gradeScale.isGradeValueContinuousAndApproved("15"));
//
//        // both bounds of the approved interval are accepted
//        assertTrue(gradeScale.isGradeValueContinuousAndApproved(MINIMUM_APPROVED_GRADE.toPlainString()));
//        assertTrue(gradeScale.isGradeValueContinuousAndApproved(MAXIMUM_APPROVED_GRADE.toPlainString()));
//
//        // values just outside the approved interval are rejected
//        assertFalse(gradeScale.isGradeValueContinuousAndApproved(
    //                MINIMUM_APPROVED_GRADE.subtract(ONE_CENT).toPlainString()));
//        assertFalse(
    //                gradeScale.isGradeValueContinuousAndApproved(MAXIMUM_APPROVED_GRADE.add(ONE_CENT).toPlainString()));
//
//        // a non numeric grade value is never continuous and approved
//        assertFalse(gradeScale.isGradeValueContinuousAndApproved("MB"));
//
//        // a null grade value is never continuous and approved
//        assertFalse(gradeScale.isGradeValueContinuousAndApproved(null));
//    }
//
//    @Test
//    public void testGradeScale_isGradeValueContinuousAndNotApproved() {
//        // no reproved intervals configured, so must be return false
//        assertFalse(gradeScale.isGradeValueContinuousAndNotApproved("5"));
//
//        // configuring a reproved interval makes the values inside it continuous and not approved
//        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, null, null, true, false);
//        assertTrue(gradeScale.isGradeValueContinuousAndNotApproved("5"));
//
//        // both bounds of the reproved interval are accepted
//        assertTrue(gradeScale.isGradeValueContinuousAndNotApproved(MINIMUM_REPROVED_GRADE.toPlainString()));
//        assertTrue(gradeScale.isGradeValueContinuousAndNotApproved(MAXIMUM_REPROVED_GRADE.toPlainString()));
//
//        // values just outside the reproved interval are rejected
//        assertFalse(gradeScale.isGradeValueContinuousAndNotApproved(
    //                MINIMUM_REPROVED_GRADE.subtract(ONE_CENT).toPlainString()));
//        assertFalse(gradeScale.isGradeValueContinuousAndNotApproved(
    //                MAXIMUM_REPROVED_GRADE.add(ONE_CENT).toPlainString()));
//
//        // a non numeric grade value is never continuous and not approved
//        assertFalse(gradeScale.isGradeValueContinuousAndNotApproved("MB"));
//
//        // a null grade value is never continuous and not approved
//        assertFalse(gradeScale.isGradeValueContinuousAndNotApproved(null));
//    }

    //    @Test
    //    public void testGradeScale_checkRules() {
    //        // an empty code is rejected
    //        try {
    //            GradeScale.create("", GRADE_SCALE_NAME, null, null, null, null, false, true);
    //            fail("a scale without a code should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.code.required", domainException.getKey());
    //        }
    //
    //        // a scale with a code and without numeric intervals satisfies the rules
    //        assertNotNull(GradeScale.create("WITH_CODE_PASSES", GRADE_SCALE_NAME, null, null, null, null, false, true));
    //
    //        // an approved interval declared without its maximum is rejected
    //        try {
    //            GradeScale.create("INCOMPLETE_APPROVED", GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE, null, false, true);
    //            fail("an approved interval without its maximum should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.numericApprovedGrade.incomplete", domainException.getKey());
    //        }
    //
    //        // an approved interval declared without its minimum is rejected
    //        try {
    //            GradeScale.create("INCOMPLETE_APPROVED_WITHOUT_MINIMUM", GRADE_SCALE_NAME, null, null, null, MAXIMUM_APPROVED_GRADE,
    //                    false, true);
    //            fail("an approved interval without its minimum should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.numericApprovedGrade.incomplete", domainException.getKey());
    //        }
    //
    //        // an approved interval declared with both bounds satisfies the rules
    //        assertNotNull(GradeScale.create("CONTINUOUS_APPROVED", GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE,
    //                MAXIMUM_APPROVED_GRADE, false, true));
    //
    //        // a reproved interval declared without its minimum is rejected
    //        try {
    //            GradeScale.create("INCOMPLETE_REPROVED", GRADE_SCALE_NAME, null, MAXIMUM_REPROVED_GRADE, null, null, false, true);
    //            fail("a reproved interval without its minimum should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.numericReprovedGrade.incomplete", domainException.getKey());
    //        }
    //
    //        // a reproved interval declared without its maximum is rejected
    //        try {
    //            GradeScale.create("INCOMPLETE_REPROVED_WITHOUT_MAXIMUM", GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, null, null, null,
    //                    false, true);
    //            fail("a reproved interval without its maximum should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.numericReprovedGrade.incomplete", domainException.getKey());
    //        }
    //
    //        // a reproved interval declared with both bounds satisfies the rules
    //        assertNotNull(
    //                GradeScale.create("CONTINUOUS_REPROVED", GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, null,
    //                        null, false, true));
    //
    //        // an approved minimum greater than its maximum is rejected
    //        try {
    //            GradeScale.create("INVALID_APPROVED", GRADE_SCALE_NAME, null, null, MAXIMUM_APPROVED_GRADE, MINIMUM_APPROVED_GRADE,
    //                    false, true);
    //            fail("an approved minimum greater than its maximum should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.minimumApprovedGrade.invalid", domainException.getKey());
    //        }
    //
    //        // a reproved minimum greater than its maximum is rejected
    //        try {
    //            GradeScale.create("INVALID_REPROVED", GRADE_SCALE_NAME, MAXIMUM_REPROVED_GRADE, MINIMUM_REPROVED_GRADE, null, null,
    //                    false, true);
    //            fail("a reproved minimum greater than its maximum should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.minimumReprovedGrade.invalid", domainException.getKey());
    //        }
    //
    //        // a reproved interval reaching into the approved interval is rejected
    //        try {
    //            GradeScale.create("REPROVED_OVERLAPS_APPROVED", GRADE_SCALE_NAME, new BigDecimal("0"), new BigDecimal("12"),
    //                    MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE, false, true);
    //            fail("a reproved interval reaching into the approved interval should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.reproved.overlap.with.approved", domainException.getKey());
    //        }
    //
    //        // an approved interval reaching into the reproved interval is rejected as well.
    //        try {
    //            GradeScale.create("APPROVED_OVERLAPS_REPROVED", GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE,
    //                    new BigDecimal("0"), MAXIMUM_APPROVED_GRADE, false, true);
    //            fail("an approved interval reaching into the reproved interval should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.approved.overlap.with.reproved", domainException.getKey());
    //        }
    //
    //        // configuring a single continuous approved interval on the existing scale satisfies the rules
    //        gradeScale.edit(GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE, true, false);
    //        assertEquals(MINIMUM_APPROVED_GRADE, gradeScale.getMinimumApprovedGrade());
    //
    //        // extending the same scale with a continuous reproved interval still satisfies the rules
    //        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, MINIMUM_APPROVED_GRADE,
    //                MAXIMUM_APPROVED_GRADE, true, false);
    //        assertEquals(MAXIMUM_REPROVED_GRADE, gradeScale.getMaximumReprovedGrade());
    //
    //        // a duplicated code is rejected
    //        try {
    //            GradeScale.create(QUALITATIVE_GRADE_SCALE_CODE, GRADE_SCALE_NAME, null, null, null, null, false, true);
    //            fail("a duplicated code should have been rejected");
    //        } catch (DomainException domainException) {
    //            assertEquals("error.GradeScale.code.duplicated", domainException.getKey());
    //        }
    //    }
    //
    //    @Test
    //    public void testGradeScale_isGradeValueContinuous() {
    //        // no intervals configured, so must return false
    //        assertFalse(gradeScale.isGradeValueContinuous("15"));
    //
    //        // values inside the approved intervals are continuous
    //        gradeScale.edit(GRADE_SCALE_NAME, null, null, MINIMUM_APPROVED_GRADE, MAXIMUM_APPROVED_GRADE, true, false);
    //        assertTrue(gradeScale.isGradeValueContinuous(MINIMUM_APPROVED_GRADE.toPlainString()));
    //        assertTrue(gradeScale.isGradeValueContinuous(MINIMUM_APPROVED_GRADE.add(ONE_CENT).toPlainString()));
    //        assertTrue(gradeScale.isGradeValueContinuous(MAXIMUM_APPROVED_GRADE.subtract(ONE_CENT).toPlainString()));
    //        assertTrue(gradeScale.isGradeValueContinuous(MAXIMUM_APPROVED_GRADE.toPlainString()));
    //
    //        // since no reproved interval is configured, must return false
    //        assertFalse(gradeScale.isGradeValueContinuous(MINIMUM_REPROVED_GRADE.toPlainString()));
    //        assertFalse(gradeScale.isGradeValueContinuous(MAXIMUM_REPROVED_GRADE.toPlainString()));
    //
    //        // values inside the reproved interval are continuous
    //        gradeScale.edit(GRADE_SCALE_NAME, MINIMUM_REPROVED_GRADE, MAXIMUM_REPROVED_GRADE, MINIMUM_APPROVED_GRADE,
    //                MAXIMUM_APPROVED_GRADE, true, false);
    //        assertTrue(gradeScale.isGradeValueContinuous(MINIMUM_REPROVED_GRADE.toPlainString()));
    //        assertTrue(gradeScale.isGradeValueContinuous(MINIMUM_REPROVED_GRADE.add(ONE_CENT).toPlainString()));
    //        assertTrue(gradeScale.isGradeValueContinuous(MAXIMUM_REPROVED_GRADE.subtract(ONE_CENT).toPlainString()));
    //        assertTrue(gradeScale.isGradeValueContinuous(MAXIMUM_REPROVED_GRADE.toPlainString()));
    //
    //        // values outside both intervals are not continuous
    //        assertFalse(gradeScale.isGradeValueContinuous(MINIMUM_REPROVED_GRADE.subtract(ONE_CENT).toPlainString()));
    //        assertFalse(gradeScale.isGradeValueContinuous(MAXIMUM_APPROVED_GRADE.add(ONE_CENT).toPlainString()));
    //
    //        // a non numeric grade value is never continuous
    //        assertFalse(gradeScale.isGradeValueContinuous("MB"));
    //
    //        // a null grade value is never continuous
    //        assertFalse(gradeScale.isGradeValueContinuous(null));
    //
    //        // removing both intervals turns every grade value non continuous again
    //        gradeScale.edit(GRADE_SCALE_NAME, null, null, null, null, true, false);
    //        assertFalse(gradeScale.isGradeValueContinuous("15"));
    //    }
}
