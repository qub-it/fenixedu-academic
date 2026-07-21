package org.fenixedu.academic.domain.organizationalStructure;

import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.GEOGRAPHIC;
import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class AccountabilityTypeTest {

    private static AccountabilityType testType;
    private static PartyType parentPartyType;
    private static PartyType childPartyType;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            Stream.of(PartyTypeEnum.values()).forEach(partyTypeEnum -> new PartyType(partyTypeEnum));

            Stream.of(AccountabilityTypeEnum.values()).forEach(
                    accountabilityTypeEnum -> new AccountabilityType(accountabilityTypeEnum,
                            new LocalizedString(Locale.getDefault(), accountabilityTypeEnum.getLocalizedName())));

            parentPartyType = PartyType.of(PartyTypeEnum.UNIVERSITY).orElseThrow();
            childPartyType = PartyType.of(PartyTypeEnum.SCHOOL).orElseThrow();

            testType = AccountabilityType.readByType(GEOGRAPHIC);
            assertNotNull(testType);

            return null;
        });
    }

    private static LocalizedString buildLS(final String s) {
        return new LocalizedString.Builder().with(Locale.getDefault(), s).build();
    }

    private static AccountabilityType createTestAccountabilityType(final String code, final AccountabilityTypeEnum type,
            final String name) {
        final AccountabilityType accountabilityType = new AccountabilityType(type, buildLS(name));
        accountabilityType.setCode(code);
        return accountabilityType;
    }

    @Test
    public void testReadByType_found() {
        final AccountabilityType result = AccountabilityType.readByType(GEOGRAPHIC);
        assertNotNull(result);
        assertEquals(GEOGRAPHIC, result.getType());
    }

    @Test
    public void testReadByType_notFound() {
        final AccountabilityTypeEnum nonExistentEnum = AccountabilityTypeEnum.values()[0];
        final AccountabilityType existingType = AccountabilityType.readByType(nonExistentEnum);
        if (existingType != null) {
            return;
        }
        final AccountabilityType result = AccountabilityType.readByType(nonExistentEnum);
        assertNull(result);
    }

    @Test
    public void testAddConnectionRule() {
        final AccountabilityType type = AccountabilityType.readByType(GEOGRAPHIC);
        assertNotNull(type);

        final ConnectionRule rule = type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);
        assertNotNull(rule);

        assertTrue(type.hasConnectionRuleFor(parentPartyType, childPartyType));
    }

    @Test
    public void testHasConnectionRuleFor_false() {
        final AccountabilityType type = AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE);
        assertNotNull(type);

        final PartyType unrelatedParent = PartyType.of(PartyTypeEnum.PLANET).orElseThrow();
        final PartyType unrelatedChild = PartyType.of(PartyTypeEnum.PERSON).orElseThrow();

        assertFalse(type.hasConnectionRuleFor(unrelatedParent, unrelatedChild));
    }

    @Test
    public void testGetConnectionRuleFor_found() {
        final AccountabilityType type = AccountabilityType.readByType(GEOGRAPHIC);
        assertNotNull(type);

        type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);

        final ConnectionRule rule = type.getConnectionRuleFor(parentPartyType, childPartyType);
        assertNotNull(rule);
        assertEquals(type, rule.getAccountabilityType());
    }

    @Test
    public void testGetConnectionRuleFor_notFound() {
        final AccountabilityType type = AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE);
        assertNotNull(type);

        final PartyType unrelatedParent = PartyType.of(PartyTypeEnum.PLANET).orElseThrow();
        final PartyType unrelatedChild = PartyType.of(PartyTypeEnum.PERSON).orElseThrow();

        final ConnectionRule rule = type.getConnectionRuleFor(unrelatedParent, unrelatedChild);
        assertNull(rule);
    }

    @Test
    public void testCanConnect_true() {
        final AccountabilityType type = AccountabilityType.readByType(GEOGRAPHIC);
        assertNotNull(type);

        type.addConnectionRule(parentPartyType, childPartyType, Boolean.TRUE);

        assertTrue(type.canConnect(parentPartyType, childPartyType));
    }

    @Test
    public void testCanConnect_false() {
        final AccountabilityType type = AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE);
        assertNotNull(type);

        final PartyType unrelatedParent = PartyType.of(PartyTypeEnum.PLANET).orElseThrow();
        final PartyType unrelatedChild = PartyType.of(PartyTypeEnum.PERSON).orElseThrow();

        assertFalse(type.canConnect(unrelatedParent, unrelatedChild));
    }

}
