package org.fenixedu.academic.domain.degreeStructure;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE;
import static org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum.*;
import static org.junit.Assert.*;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseScientificAreaTest {

    private final static Function<String, LocalizedString> buildLS =
            s -> new LocalizedString.Builder().with(Locale.getDefault(), s).build();

    private static Unit schoolUnit;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            CompetenceCourseTest.initCompetenceCourse();
            schoolUnit = Unit.findInternalUnitByAcronymPath("QS").orElseThrow();
            return null;
        });
    }

    @After
    public void tearDown() {
        CompetenceCourseScientificArea.findAll().forEach(CompetenceCourseScientificArea::delete);
    }

    @Test
    public void testCreate() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();

        final Unit scientificAreaUnit = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final CompetenceCourseScientificArea createdArea =
                CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit);

        final Optional<CompetenceCourseScientificArea> found =
                CompetenceCourseScientificArea.find(competenceCourseInformation, scientificAreaUnit);
        assertTrue(found.isPresent());
        assertEquals(createdArea, found.get());
    }

    @Test
    public void testCreate_errorInvalidUnit() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();

        final Unit aggregateUnit = createUnit(AGGREGATE_UNIT, schoolUnit);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CompetenceCourseScientificArea.scientificAreaUnit.mustBeScientificAreaUnit");

        CompetenceCourseScientificArea.create(competenceCourseInformation, aggregateUnit);
    }

    @Test
    public void testCreate_errorExistingArea() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();

        final Unit scientificAreaUnit = createUnit(SCIENTIFIC_AREA, schoolUnit);

        CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage(
                "error.CompetenceCourseScientificArea.already.exists.forCompetenceCourseInformationAndScientificAreaUnit");

        CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit);
    }

    @Test
    public void testFind_notFound() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();

        final Unit scientificAreaUnit = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final Optional<CompetenceCourseScientificArea> notFound =
                CompetenceCourseScientificArea.find(competenceCourseInformation, scientificAreaUnit);
        assertTrue(notFound.isEmpty());
    }

    @Test
    public void testSetCredits_competenceCourseScientificArea() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();
        competenceCourseInformation.setCredits(new BigDecimal(6));

        final Unit scientificAreaUnit1 = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final CompetenceCourseScientificArea createdScientific1 =
                CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit1);
        createdScientific1.setCredits(new BigDecimal(3));

        final Unit scientificAreaUnit2 = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final CompetenceCourseScientificArea createdScientific2 =
                CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit2);
        createdScientific2.setCredits(new BigDecimal(3));

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CompetenceCourseScientificArea.credits.exceedingTotalCourseCredits");

        final Unit scientificAreaUnit3 = createUnit(SCIENTIFIC_AREA, schoolUnit);
        final CompetenceCourseScientificArea createdScientific3 =
                CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit3);
        createdScientific3.setCredits(new BigDecimal(3));
    }

    @Test
    public void testSetCredits_competenceCourseInformation() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();
        competenceCourseInformation.setCredits(new BigDecimal(6));

        final Unit scientificAreaUnit = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final CompetenceCourseScientificArea createdArea =
                CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit);
        createdArea.setCredits(new BigDecimal(2));

        competenceCourseInformation.setCredits(new BigDecimal(3));
        competenceCourseInformation.setCredits(new BigDecimal(2));

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.CompetenceCourseScientificArea.credits.exceedingTotalCourseCredits");

        competenceCourseInformation.setCredits(new BigDecimal(1));
    }

    @Test
    public void testDelete() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();
        final Unit scientificAreaUnit = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final CompetenceCourseScientificArea createdArea =
                CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit);

        createdArea.delete();

        final Optional<CompetenceCourseScientificArea> notFound =
                CompetenceCourseScientificArea.find(competenceCourseInformation, scientificAreaUnit);
        assertTrue(notFound.isEmpty());
    }

    @Test
    public void testDeleteUnit() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();
        final Unit scientificAreaUnit = createUnit(SCIENTIFIC_AREA, schoolUnit);

        CompetenceCourseScientificArea.create(competenceCourseInformation, scientificAreaUnit);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("key.return.argument");

        scientificAreaUnit.delete();
    }

    @Test
    public void testSetCompetenceCourseInformationGroupUnit() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();

        final Unit sa1 = createUnit(SCIENTIFIC_AREA, schoolUnit);
        final Unit ccgu1 = createUnit(COMPETENCE_COURSE_GROUP, sa1);

        final Unit sa2 = createUnit(SCIENTIFIC_AREA, schoolUnit);

        final Unit sa3 = createUnit(SCIENTIFIC_AREA, schoolUnit);
        final Unit ccgu3 = createUnit(COMPETENCE_COURSE_GROUP, sa3);

        // Scenario 1:
        competenceCourseInformation.setCompetenceCourseGroupUnit(ccgu1);
        assertTrue(CompetenceCourseScientificArea.find(competenceCourseInformation, sa1).isPresent());
        assertFalse(CompetenceCourseScientificArea.find(competenceCourseInformation, sa2).isPresent());
        assertFalse(CompetenceCourseScientificArea.find(competenceCourseInformation, sa3).isPresent());

        // Scenario 2:
        CompetenceCourseScientificArea.create(competenceCourseInformation, sa2);
        assertTrue(CompetenceCourseScientificArea.find(competenceCourseInformation, sa1).isPresent());
        assertTrue(CompetenceCourseScientificArea.find(competenceCourseInformation, sa2).isPresent());
        assertFalse(CompetenceCourseScientificArea.find(competenceCourseInformation, sa3).isPresent());

        // Scenario 3:
        competenceCourseInformation.setCompetenceCourseGroupUnit(ccgu3);
        assertFalse(CompetenceCourseScientificArea.find(competenceCourseInformation, sa1).isPresent());
        assertTrue(CompetenceCourseScientificArea.find(competenceCourseInformation, sa2).isPresent());
        assertTrue(CompetenceCourseScientificArea.find(competenceCourseInformation, sa3).isPresent());
    }

    @Test
    public void testCopyScientificAreasOnCompetenceCourseInformationCreation() {
        final CompetenceCourseInformation competenceCourseInformation = createCompetenceCourseAndInformation();

        final Unit sa1 = createUnit(SCIENTIFIC_AREA, schoolUnit);
        final Unit ccgu1 = createUnit(COMPETENCE_COURSE_GROUP, sa1);
        competenceCourseInformation.setCompetenceCourseGroupUnit(ccgu1);

        final Unit sa2 = createUnit(SCIENTIFIC_AREA, schoolUnit);
        CompetenceCourseScientificArea.create(competenceCourseInformation, sa2);

        CompetenceCourseInformation newCompetenceCourseInformation = new CompetenceCourseInformation(competenceCourseInformation,
                competenceCourseInformation.getExecutionInterval().getNext());

        assertEquals(2, newCompetenceCourseInformation.getCompetenceCourseScientificAreasSet().size());
        assertTrue(CompetenceCourseScientificArea.find(newCompetenceCourseInformation, sa1).isPresent());
        assertTrue(CompetenceCourseScientificArea.find(newCompetenceCourseInformation, sa2).isPresent());
    }

    private static Unit createUnit(PartyTypeEnum partyType, Unit parentUnit) {
        final String uuid = UUID.randomUUID().toString();
        return Unit.createNewUnit(PartyType.of(partyType), buildLS.apply(uuid), uuid, parentUnit,
                AccountabilityType.readByType(ORGANIZATIONAL_STRUCTURE));
    }

    private static CompetenceCourseInformation createCompetenceCourseAndInformation() {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
        final String uuidCC = UUID.randomUUID().toString();
        final CompetenceCourse competenceCourse =
                CompetenceCourseTest.createCompetenceCourse(uuidCC, uuidCC, BigDecimal.ZERO, AcademicPeriod.SEMESTER,
                        ExecutionInterval.findFirstCurrentChild(null), coursesUnit);

        return competenceCourse.findInformationMostRecentUntil(null);
    }

}
