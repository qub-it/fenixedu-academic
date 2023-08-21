package org.fenixedu.academic.domain;

import static org.fenixedu.academic.util.Bundle.ENUMERATION;
import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;
import static org.junit.Assert.assertNotNull;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EvaluationSeasonTest {

    public static final String SPECIAL_SEASON_CODE = "SPECIAL_SEASON";
    public static final String IMPROVEMENT_SEASON_CODE = "IMPROVEMENT";
    public static final String NORMAL_SEASON_CODE = "NORMAL";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initEvaluationSeasons();
            return null;
        });
    }

    @Test
    public void testFindByCode() {
        assertNotNull(EvaluationSeason.findByCode(NORMAL_SEASON_CODE).orElse(null));
    }

    @Test
    public void testCreateDuplicateCode() {
        exceptionRule.expect(DomainException.class);
        createEvaluationSeason(NORMAL_SEASON_CODE, true, false, false);
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
        final EvaluationSeason result = new EvaluationSeason(label, label, normal, improvement, false, special);
        result.setCode(code);

        return result;
    }

}
