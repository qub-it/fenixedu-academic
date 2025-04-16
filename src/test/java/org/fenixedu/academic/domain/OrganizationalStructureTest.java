package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.GEOGRAPHIC;
import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class OrganizationalStructureTest {

    private final static Function<String, LocalizedString> buildLS =
            s -> new LocalizedString.Builder().with(Locale.getDefault(), s).build();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initTypes();
            initUnits();
            return null;
        });
    }

    static void initTypes() {
        Stream.of(PartyTypeEnum.values()).forEach(partyTypeEnum -> new PartyType(partyTypeEnum));

        Stream.of(AccountabilityTypeEnum.values())
                .forEach(accountabilityTypeEnum -> new AccountabilityType(accountabilityTypeEnum,
                        new LocalizedString(Locale.getDefault(), accountabilityTypeEnum.getLocalizedName())));
    }

    static void initUnits() {
        final Unit planetUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.PLANET), buildLS.apply("Earth"), "E", null, null);

        final Unit countryUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.COUNTRY), buildLS.apply("Portugal"), "PT",
                planetUnit, AccountabilityType.readByType(GEOGRAPHIC));

        final Unit universityUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.UNIVERSITY), buildLS.apply("qub University"),
                "QU", countryUnit, AccountabilityType.readByType(GEOGRAPHIC));

        final Unit schoolUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.SCHOOL), buildLS.apply("qub School"), "QS",
                universityUnit, AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));

        final Unit coursesAgregatorUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.AGGREGATE_UNIT), buildLS.apply("Courses"),
                "Courses", schoolUnit, AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));

        Unit.createNewUnit(PartyType.of(PartyTypeEnum.AGGREGATE_UNIT), buildLS.apply("Degrees"), "Degrees", schoolUnit,
                AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));

        Unit.createNewUnit(PartyType.of(PartyTypeEnum.COMPETENCE_COURSE_GROUP), buildLS.apply("Courses Group"), "CC",
                coursesAgregatorUnit, AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));

        final Bennu rootDomainObject = Bennu.getInstance();
        rootDomainObject.setEarthUnit(planetUnit);
        rootDomainObject.setInstitutionUnit(universityUnit);
    }

    @Test
    public void testUnits_readAll() {
        assertEquals(Unit.readAllUnits().size(), 7);
    }

    @Test
    public void testUnits_institution() {
        assertNotNull(UnitUtils.readInstitutionUnit());
        assertEquals(UnitUtils.readInstitutionUnit().getAcronym(), "QU");

    }

    @Test
    public void testUnits_find() {
        final Optional<Unit> coursesUnit = Unit.findInternalUnitByAcronymPath("QS  > Courses   > CC  ");
        assertTrue(coursesUnit.isPresent());
        assertEquals(coursesUnit.get().getAcronym(), "CC");

        assertTrue(Unit.findInternalUnitByAcronymPath("XX").isEmpty());
        assertTrue(Unit.findInternalUnitByAcronymPath("QS>XXX").isEmpty());
        assertTrue(Unit.findInternalUnitByAcronymPath("QS>XXX>CC").isEmpty());
        assertTrue(Unit.findInternalUnitByAcronymPath("QS>courses>CC").isEmpty());
        assertTrue(Unit.findInternalUnitByAcronymPath("QS>Courses>CC").isPresent());

        assertTrue(Unit.findInternalUnitByAcronymPath("").isPresent());
        assertTrue(Unit.findInternalUnitByAcronymPath(null).isPresent());

        final Unit institutionUnit = UnitUtils.readInstitutionUnit();
        assertEquals(Unit.findInternalUnitByAcronymPath("").get(), institutionUnit);
        assertEquals(Unit.findInternalUnitByAcronymPath(null).get(), institutionUnit);
    }

    @Test
    public void testUnits_isSubUnit() {
        final Unit schoolUnit = Unit.findInternalUnitByAcronymPath("QS").orElseThrow();
        final Unit degreesAgregatorUnit = Unit.findInternalUnitByAcronymPath("QS>Degrees").orElseThrow();
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath("QS>Courses>CC").orElseThrow();

        assertTrue(coursesUnit.isSubUnitOf(List.of(coursesUnit)));
        assertTrue(coursesUnit.isSubUnitOf(List.of(schoolUnit)));
        assertTrue(coursesUnit.isSubUnitOf(List.of(schoolUnit, degreesAgregatorUnit)));

        assertTrue(!schoolUnit.isSubUnitOf(List.of(coursesUnit)));
        assertTrue(!coursesUnit.isSubUnitOf(List.of(degreesAgregatorUnit)));
    }

    @Test
    public void testUnits_getParentUnitsPresentationName() {
        final Unit universityUnit = UnitUtils.readInstitutionUnit();
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath("QS>Courses>CC").orElseThrow();
        final Unit earthUnit = UnitUtils.readEarthUnit();

        String parentUnitsPresentationName = coursesUnit.getParentUnitsPresentationName(" > ");

        assertTrue(StringUtils.isBlank(earthUnit.getParentUnitsPresentationName(" > ")));
        assertEquals(universityUnit.getParentUnitsPresentationName(" : "), "Earth (E) : Portugal (PT)");
        
        assertEquals(StringUtils.countMatches(parentUnitsPresentationName, ">"), 1);
        assertEquals(parentUnitsPresentationName, "qub University (QU) > qub School (QS)");
    }

    @Test
    public void testUnits_findInternalUnitsByPartyType() {
        final PartyType schoolType = PartyType.findByCode("SCHOOL").orElseThrow();
        final Collection<Unit> schools = Unit.findInternalUnitsByPartyType(schoolType).toList();
        assertFalse(schools.isEmpty());
        assertTrue(schools.stream().allMatch(unit -> unit.getPartyType() == schoolType));

        final PartyType countryType = PartyType.findByCode(PartyTypeEnum.COUNTRY.name()).orElseThrow();
        final Stream<Unit> countries = Unit.findInternalUnitsByPartyType(countryType);
        assertTrue(countries.findAny().isEmpty());
    }

    @Test
    public void testPartyType_findByCode() {
        PartyType partyType = PartyType.create("AD_HOC_CODE", new LocalizedString(Locale.getDefault(), "Ad Hoc Type"));
        Optional<PartyType> result = PartyType.findByCode("AD_HOC_CODE");
        assertTrue(result.isPresent());
        assertEquals("AD_HOC_CODE", result.get().getCode());

        result = PartyType.findByCode("NON_EXISTENT");
        assertTrue(result.isEmpty());

        result = PartyType.findByCode("SCHOOL");
        assertTrue(result.isPresent());
        assertEquals(PartyTypeEnum.SCHOOL, result.get().getType());

        result = PartyType.findByCode("  ");
        assertTrue(result.isEmpty());
    }

}
