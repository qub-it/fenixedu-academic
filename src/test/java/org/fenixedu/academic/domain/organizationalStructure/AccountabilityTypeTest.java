package org.fenixedu.academic.domain.organizationalStructure;

import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.GEOGRAPHIC;
import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AccountabilityTypeTest {

    private PartyType parentPartyType;
    private PartyType childPartyType;
    private PartyType unrelatedParentPartyType;
    private PartyType unrelatedChildPartyType;

    @Before
    public void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Stream.of(PartyTypeEnum.values()).forEach(PartyType::new);

            Stream.of(AccountabilityTypeEnum.values()).forEach(
                    accountabilityTypeEnum -> new AccountabilityType(accountabilityTypeEnum,
                            new LocalizedString(Locale.getDefault(), accountabilityTypeEnum.getLocalizedName())));

            parentPartyType = PartyType.of(PartyTypeEnum.UNIVERSITY).orElseThrow();
            childPartyType = PartyType.of(PartyTypeEnum.SCHOOL).orElseThrow();
            unrelatedParentPartyType = PartyType.of(PartyTypeEnum.PLANET).orElseThrow();
            unrelatedChildPartyType = PartyType.of(PartyTypeEnum.PERSON).orElseThrow();

            return null;
        });
    }

    @After
    public void cleanup() {
        Bennu.getInstance().getConnectionRulesSet().forEach(ConnectionRule::delete);
        Bennu.getInstance().getAccountabilityTypesSet().forEach(AccountabilityType::delete);
        Bennu.getInstance().getPartyTypesSet().forEach(PartyType::delete);
    }

    @Test
    public void testReadByType_found() {
        final AccountabilityType result = AccountabilityType.readByType(GEOGRAPHIC);
        assertNotNull(result);
        assertEquals(GEOGRAPHIC, result.getType());

        // typeEnum == null should never match
        assertNull(AccountabilityType.readByType(null));
    }

    @Test
    public void testReadByType_notFound() {
        final AccountabilityType existing = AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE);
        assertNotNull(existing);

        existing.delete();

        final AccountabilityType result = AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE);
        assertNull(result);
    }

    @Test
    public void testAddConnectionRule() {
        final AccountabilityType type = getExistingAccountabilityType(GEOGRAPHIC);
        assertNotNull(type);

        // before adding the rule, no connection should be possible
        assertFalse(type.hasConnectionRuleFor(parentPartyType, childPartyType));
        assertFalse(type.canConnect(parentPartyType, childPartyType));

        final ConnectionRule rule = type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);

        assertNotNull(rule);
        assertEquals(type, rule.getAccountabilityType());
        assertEquals(parentPartyType, rule.getAllowedParentPartyType());
        assertEquals(childPartyType, rule.getAllowedChildPartyType());
        assertTrue(type.getConnectionRulesSet().contains(rule));

        // after adding the rule, the connection should be possible
        assertTrue(type.hasConnectionRuleFor(parentPartyType, childPartyType));
        assertTrue(type.canConnect(parentPartyType, childPartyType));
    }

    @Test
    public void testHasConnectionRuleForAndCanConnect_falseForDifferentPairWhenRuleExists() {
        final AccountabilityType type = getExistingAccountabilityType(GEOGRAPHIC);
        assertNotNull(type);

        type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);

        // the existing rule should not match an unrelated pair
        assertFalse(type.hasConnectionRuleFor(unrelatedParentPartyType, unrelatedChildPartyType));
        assertFalse(type.canConnect(unrelatedParentPartyType, unrelatedChildPartyType));

        // should still match the pair it was created for
        assertTrue(type.canConnect(parentPartyType, childPartyType));
    }

    @Test
    public void testGetConnectionRuleFor_found() {
        final AccountabilityType type = getExistingAccountabilityType(GEOGRAPHIC);
        assertNotNull(type);

        type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);

        final ConnectionRule rule = type.getConnectionRuleFor(parentPartyType, childPartyType);
        assertNotNull(rule);
        assertEquals(type, rule.getAccountabilityType());
    }

    @Test
    public void testGetConnectionRuleForAndCanConnect_falseWhenNoRuleExists() {
        final AccountabilityType type = getExistingAccountabilityType(ORGANIZATIONAL_STRUCTURE);
        assertNotNull(type);

        assertNull(type.getConnectionRuleFor(unrelatedParentPartyType, unrelatedChildPartyType));
        assertFalse(type.canConnect(unrelatedParentPartyType, unrelatedChildPartyType));
    }

    @Test
    public void testGetConnectionRuleFor_multipleRules() {
        final AccountabilityType type = getExistingAccountabilityType(GEOGRAPHIC);
        assertNotNull(type);

        type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);
        type.addConnectionRule(unrelatedParentPartyType, unrelatedChildPartyType, Boolean.FALSE);

        final ConnectionRule rule1 = type.getConnectionRuleFor(parentPartyType, childPartyType);
        assertNotNull(rule1);
        assertEquals(parentPartyType, rule1.getAllowedParentPartyType());
        assertEquals(childPartyType, rule1.getAllowedChildPartyType());

        final ConnectionRule rule2 = type.getConnectionRuleFor(unrelatedParentPartyType, unrelatedChildPartyType);
        assertNotNull(rule2);
        assertEquals(unrelatedParentPartyType, rule2.getAllowedParentPartyType());
        assertEquals(unrelatedChildPartyType, rule2.getAllowedChildPartyType());

        assertNotEquals(rule1, rule2);
    }

    private AccountabilityType getExistingAccountabilityType(AccountabilityTypeEnum accountabilityTypeEnum) {
        return AccountabilityType.readByType(accountabilityTypeEnum);
    }
}
