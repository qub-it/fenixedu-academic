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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitAcronym;
import org.fenixedu.academic.domain.organizationalStructure.UnitUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
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

        Stream.of(AccountabilityTypeEnum.values()).forEach(
                accountabilityTypeEnum -> new AccountabilityType(accountabilityTypeEnum,
                        new LocalizedString(Locale.getDefault(), accountabilityTypeEnum.getLocalizedName())));
    }

    static void initUnits() {
        final Unit planetUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.PLANET), buildLS.apply("Earth"), "E", null, null);

        final Unit countryUnit =
                Unit.createNewUnit(PartyType.of(PartyTypeEnum.COUNTRY), buildLS.apply("Portugal"), "PT", planetUnit,
                        AccountabilityType.readByType(GEOGRAPHIC));

        final Unit universityUnit =
                Unit.createNewUnit(PartyType.of(PartyTypeEnum.UNIVERSITY), buildLS.apply("qub University"), "QU", countryUnit,
                        AccountabilityType.readByType(GEOGRAPHIC));

        final Unit schoolUnit =
                Unit.createNewUnit(PartyType.of(PartyTypeEnum.SCHOOL), buildLS.apply("qub School"), "QS", universityUnit,
                        AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));

        final Unit coursesAgregatorUnit =
                Unit.createNewUnit(PartyType.of(PartyTypeEnum.AGGREGATE_UNIT), buildLS.apply("Courses"), "Courses", schoolUnit,
                        AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));

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

    @Test
    public void testParty_nameComparator() {
        Person zPerson = createPerson("Zeca", "z");
        Person afPerson = createPerson("andreia Filipa", "af");
        Person igPerson = createPerson("Ígor", "ig");
        Person amPerson = createPerson("André Miguel", "am");
        Person ivPerson = createPerson("Ivo", "iv");
        Person mmPerson = createPerson("Maria Margarida", "mm");
        Person mdPerson = createPerson("Mariana Dias", "md");
        Person mpPerson = createPerson("Maria Pacheco", "mp");

        List<Person> sortedPersons = Stream.of(zPerson, afPerson, igPerson, amPerson, ivPerson, mmPerson, mdPerson, mpPerson)
                .sorted(Party.COMPARATOR_BY_NAME).toList();
        System.out.println(sortedPersons.stream().map(p -> p.getName()).collect(Collectors.joining(", ")));

        assertEquals(amPerson, sortedPersons.get(0));
        assertEquals(afPerson, sortedPersons.get(1));
        assertEquals(igPerson, sortedPersons.get(2));
        assertEquals(ivPerson, sortedPersons.get(3));
        assertEquals(mmPerson, sortedPersons.get(4));
        assertEquals(mpPerson, sortedPersons.get(5));
        assertEquals(mdPerson, sortedPersons.get(6));
        assertEquals(zPerson, sortedPersons.get(7));
    }

    @Test
    public void testUnitAcronym_readUnitAcronymByAcronym() {
        Unit institutionUnit = UnitUtils.readInstitutionUnit();

        assertTrue(UnitAcronym.readUnitAcronymByAcronym(null).isEmpty());
        assertEquals(institutionUnit.getUnitAcronym().getAcronym(),
                UnitAcronym.readUnitAcronymByAcronym("QU").map(UnitAcronym::getAcronym).orElse(""));
    }

    @Test
    public void testParty_getParentParties() {
        final Unit universityUnit = UnitUtils.readInstitutionUnit();
        final Unit earthUnit = UnitUtils.readEarthUnit();
        final Unit countryUnit = Unit.findUnitByAcronymPath("PT", earthUnit).orElseThrow();

        assertTrue(earthUnit.getParentUnits().isEmpty());
        assertTrue(earthUnit.getParentUnits(GEOGRAPHIC).isEmpty());
        assertTrue(earthUnit.getParentUnits(List.of(GEOGRAPHIC, ORGANIZATIONAL_STRUCTURE)).isEmpty());
        assertTrue(universityUnit.getParentUnits().contains(countryUnit));
        assertTrue(universityUnit.getParentUnits(GEOGRAPHIC).contains(countryUnit));
        assertTrue(universityUnit.getParentUnits(ORGANIZATIONAL_STRUCTURE).isEmpty());
        assertTrue(universityUnit.getParentUnits(List.of(GEOGRAPHIC, ORGANIZATIONAL_STRUCTURE)).contains(countryUnit));
    }

    @Test
    public void testParty_getChildParties() {
        final Unit schoolUnit = Unit.findInternalUnitByAcronymPath("QS").orElseThrow();
        final Unit degreesAgregatorUnit = Unit.findInternalUnitByAcronymPath("QS>Degrees").orElseThrow();
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath("QS>Courses").orElseThrow();
        final Unit coursesGroupUnit = Unit.findInternalUnitByAcronymPath("QS>Courses>CC").orElseThrow();

        // coursesGroupUnit must not have children
        assertTrue(coursesGroupUnit.getSubUnits().isEmpty());

        // schoolUnit must have 2 children, with degreesAggregatorUnit of AggregateUnit Party Type
        // and both degreesAggregatorUnit and coursesUnits of OrganizationalStructure Accountability Type
        assertEquals(2, schoolUnit.getSubUnits().size());

        assertTrue(schoolUnit.getSubUnits(PartyTypeEnum.COUNTRY).isEmpty());
        assertTrue(schoolUnit.getSubUnits(List.of(GEOGRAPHIC)).isEmpty());
        assertTrue(schoolUnit.getSubUnits().contains(degreesAgregatorUnit));

        final Collection<Unit> aggregateUnitSubUnits = schoolUnit.getSubUnits(PartyTypeEnum.AGGREGATE_UNIT);
        assertTrue(aggregateUnitSubUnits.contains(degreesAgregatorUnit));
        assertFalse(aggregateUnitSubUnits.contains(coursesGroupUnit));

        final Collection<Unit> organizationalStructureSubUnits = schoolUnit.getSubUnits(List.of(ORGANIZATIONAL_STRUCTURE));
        assertTrue(organizationalStructureSubUnits.contains(degreesAgregatorUnit));
        assertTrue(organizationalStructureSubUnits.contains(coursesUnit));
    }

    @Test
    public void testParty_getChildAccountabilities() {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath("QS>Courses").orElseThrow();
        final Unit coursesGroupUnit = Unit.findInternalUnitByAcronymPath("QS>Courses>CC").orElseThrow();

        // coursesGroupUnit must not have any child accountabilities setup
        assertTrue(coursesGroupUnit.getChildAccountabilities(ORGANIZATIONAL_STRUCTURE).isEmpty());

        // coursesUnit must be connected to at least coursesGroupUnit through an accountability of type OrganizationalStructure
        assertFalse(coursesUnit.getChildAccountabilities(ORGANIZATIONAL_STRUCTURE).isEmpty());
        assertTrue(coursesUnit.getChildAccountabilities(GEOGRAPHIC).isEmpty());
        assertTrue(coursesUnit.getChildAccountabilities(ORGANIZATIONAL_STRUCTURE).stream()
                .anyMatch(accountability -> accountability.getChildParty() == coursesGroupUnit));
    }

    private static Person createPerson(final String name, final String username) {
        final UserProfile userProfile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        return new Person(userProfile);
    }

}
