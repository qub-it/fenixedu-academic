package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class OrganizationalStructureTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initTypes();
            initPlanetUnit();
            return null;
        });
    }

    static void initTypes() {
        Stream.of(PartyTypeEnum.values()).forEach(partyTypeEnum -> new PartyType(partyTypeEnum));

        Stream.of(AccountabilityTypeEnum.values())
                .forEach(accountabilityTypeEnum -> new AccountabilityType(accountabilityTypeEnum,
                        new LocalizedString(Locale.getDefault(), accountabilityTypeEnum.getLocalizedName())));
    }

    static Unit initPlanetUnit() {
        LocalizedString name = new LocalizedString.Builder().with(Locale.getDefault(), "Earth").build();
        final Unit planetUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.PLANET), name, "E", null, null);
        final Bennu rootDomainObject = Bennu.getInstance();
        rootDomainObject.setEarthUnit(planetUnit);
        return planetUnit;
    }

    @Test
    public void test_readAll() {
        assertEquals(Unit.readAllUnits().size(), 1);
    }

}
