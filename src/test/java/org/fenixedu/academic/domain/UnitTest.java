package org.fenixedu.academic.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.ConnectionRule;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class UnitTest {

    private static Unit testUnit, testParent;
    private static AccountabilityType aType;
    private static PartyType pType, uType;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initUnit();
            return null;
        });
    }

    private static void initUnit() {
        pType = PartyType.create("PARENT_PARTY_TYPE", new LocalizedString(Locale.ENGLISH, "PARENT_PARTY_TYPE"));
        uType = PartyType.create("UNIT_PARTY_TYPE", new LocalizedString(Locale.ENGLISH, "UNIT_PARTY_TYPE"));
        aType = AccountabilityType.create("PARENT_TYPE", new LocalizedString(Locale.ENGLISH, "PARENT_TYPE"));

        pType.setType(PartyTypeEnum.PLANET);
        pType.addAllowedChildConnectionRules(new ConnectionRule(null, uType, aType));

        uType.setType(PartyTypeEnum.DEGREE_UNIT);
        uType.addAllowedParentConnectionRules(new ConnectionRule(pType, null, aType));

        testParent = Unit.createNewUnit(Optional.ofNullable(pType), new LocalizedString(Locale.ENGLISH, "PARENT_UNIT"), "PARENT",
                null, aType);
        testUnit = Unit.createNewUnit(Optional.ofNullable(uType), new LocalizedString(Locale.ENGLISH, "NEW_UNIT"), "NEW",
                testParent, aType);
    }

    @Test
    public void testGetParentUnitsPresentationNameSucessful() {
        testUnit.getAllSubUnits().stream().forEach(unit -> {
            assertTrue(StringUtils.countMatches(unit.getParentUnitsPresentationName(" > "),
                    ">") == unit.getParentUnitsPath().stream().filter(parent -> !parent.isAggregateUnit()).count() - 1);
        });
    }

}
