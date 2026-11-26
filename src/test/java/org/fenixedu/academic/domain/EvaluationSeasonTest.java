package org.fenixedu.academic.domain;

import static org.fenixedu.academic.util.Bundle.ENUMERATION;
import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class EvaluationSeasonTest {

    public static final String SPECIAL_SEASON_CODE = "SPECIAL_SEASON";
    public static final String IMPROVEMENT_SEASON_CODE = "IMPROVEMENT";
    public static final String NORMAL_SEASON_CODE = "NORMAL";
    public static final String SPECIAL_AUTHORIZATION_SEASON_CODE = "SPECIAL_AUTHORIZATION";

    private static EvaluationSeason first, second, inactiveThird, fourth;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @After
    public void cleanUp() {
        EvaluationSeason.all().forEach(EvaluationSeason::delete);
    }

    @Test
    public void testFindByCode() {
        createEvaluationSeason(NORMAL_SEASON_CODE, true, false, false);
        assertNotNull(EvaluationSeason.findByCode(NORMAL_SEASON_CODE).orElse(null));
    }

    @Test
    public void testCreateDuplicateCode() {
        createEvaluationSeason(NORMAL_SEASON_CODE, true, false, false);
        exceptionRule.expect(DomainException.class);
        createEvaluationSeason(NORMAL_SEASON_CODE, true, false, false);
    }

    @Test
    public void testFindPreviousSeason() {
        initSpecialAuthorizationSeasons();

        // the nearest same-type predecessor is returned, regardless of the active flag
        assertEquals(first, second.findPreviousSeason(false).orElseThrow());
        assertEquals(first, second.findPreviousSeason(true).orElseThrow());

        // the first season has no previous season, so the result is empty
        assertTrue(first.findPreviousSeason(false).isEmpty());
        assertTrue(first.findPreviousSeason(true).isEmpty());

        // with inactive seasons allowed, the immediate predecessor is returned even if inactive
        assertEquals(inactiveThird, fourth.findPreviousSeason(false).orElseThrow());

        // with only active seasons allowed, the inactive predecessor is skipped for the previous active one
        assertEquals(second, fourth.findPreviousSeason(true).orElseThrow());

        // an inactive season still resolves its own previous season
        assertEquals(second, inactiveThird.findPreviousSeason(false).orElseThrow());
    }

    @Test
    public void testFindNextSeason() {
        initSpecialAuthorizationSeasons();

        // the nearest same-type successor is returned, regardless of the active flag
        assertEquals(second, first.findNextSeason(false).orElseThrow());
        assertEquals(second, first.findNextSeason(true).orElseThrow());

        // the last season has no next season, so the result is empty
        assertTrue(fourth.findNextSeason(false).isEmpty());
        assertTrue(fourth.findNextSeason(true).isEmpty());

        // with inactive seasons allowed, the immediate successor is returned even if inactive
        assertEquals(inactiveThird, second.findNextSeason(false).orElseThrow());

        // with only active seasons allowed, the inactive successor is skipped for the next active one
        assertEquals(fourth, second.findNextSeason(true).orElseThrow());

        // an inactive season still resolves its own next season
        assertEquals(fourth, inactiveThird.findNextSeason(false).orElseThrow());
    }

    @Test
    public void testMoveUp() {
        initSpecialAuthorizationSeasons();

        // moving a season up swaps its order with the direct predecessor
        inactiveThird.moveUp();
        assertEquals(20, inactiveThird.getSeasonOrder().intValue());
        assertEquals(30, second.getSeasonOrder().intValue());
        assertEquals(10, first.getSeasonOrder().intValue());

        // the first season has no predecessor, so moving it up changes nothing
        first.moveUp();
        assertEquals(10, first.getSeasonOrder().intValue());

        // moving the season that is now second up swaps it back to the first position
        inactiveThird.moveUp();
        assertEquals(10, inactiveThird.getSeasonOrder().intValue());
        assertEquals(20, first.getSeasonOrder().intValue());

        final EvaluationSeason improvement = createEvaluationSeason("UP_IMPROVEMENT", false, true, false);
        improvement.setSeasonOrder(15);
        // first order is 20 and the inactiveThird is 10, the swap still happens between them ignoring improvement
        first.moveUp();
        assertEquals(10, first.getSeasonOrder().intValue());
        assertEquals(20, inactiveThird.getSeasonOrder().intValue());
        assertEquals(15, improvement.getSeasonOrder().intValue());

    }

    @Test
    public void testMoveDown() {
        initSpecialAuthorizationSeasons();

        // moving a season down swaps its order with the direct successor
        second.moveDown();
        assertEquals(30, second.getSeasonOrder().intValue());
        assertEquals(20, inactiveThird.getSeasonOrder().intValue());

        // the last season has no successor, so moving it down changes nothing
        fourth.moveDown();
        assertEquals(40, fourth.getSeasonOrder().intValue());

        // the first season moves down one position, to the second place
        first.moveDown();
        assertEquals(20, first.getSeasonOrder().intValue());
        assertEquals(10, inactiveThird.getSeasonOrder().intValue());

        final EvaluationSeason improvement = createEvaluationSeason("UP_IMPROVEMENT", false, true, false);
        improvement.setSeasonOrder(15);
        // first order is 20 and the inactiveThird is 10, the swap still happens between them ignoring improvement
        inactiveThird.moveDown();
        assertEquals(10, first.getSeasonOrder().intValue());
        assertEquals(20, inactiveThird.getSeasonOrder().intValue());
        assertEquals(15, improvement.getSeasonOrder().intValue());
    }

    public static void initEvaluationSeasons() {
        final EvaluationSeason normal = createEvaluationSeason(NORMAL_SEASON_CODE, true, false, false);
        EvaluationConfiguration.getInstance().setDefaultEvaluationSeason(normal);
        
        createEvaluationSeason(IMPROVEMENT_SEASON_CODE, false, true, false);
        createEvaluationSeason(SPECIAL_SEASON_CODE, false, false, true);
    }

    public static EvaluationSeason createEvaluationSeason(final String code, boolean normal, boolean improvement,
            boolean special) {
        final LocalizedString label = getLocalizedString(ENUMERATION, code);
        return EvaluationSeason.create(code, label, label, normal, improvement, special, false, false, false, true, false, true);
    }

    private static void initSpecialAuthorizationSeasons() {
        first = createSpecialAuthorizationSeason(SPECIAL_AUTHORIZATION_SEASON_CODE + 1, 10, true);
        second = createSpecialAuthorizationSeason(SPECIAL_AUTHORIZATION_SEASON_CODE + 2, 20, true);
        inactiveThird = createSpecialAuthorizationSeason(SPECIAL_AUTHORIZATION_SEASON_CODE + 3, 30, false);
        fourth = createSpecialAuthorizationSeason(SPECIAL_AUTHORIZATION_SEASON_CODE + 4, 40, true);
    }

    private static EvaluationSeason createSpecialAuthorizationSeason(final String code, final int seasonOrder,
            final boolean active) {
        final LocalizedString label = new LocalizedString(Locale.ENGLISH, code);
        final EvaluationSeason season =
                EvaluationSeason.create(code, label, label, false, false, false, true, active, false, false, false, false);
        season.setSeasonOrder(seasonOrder);
        return season;
    }
}
