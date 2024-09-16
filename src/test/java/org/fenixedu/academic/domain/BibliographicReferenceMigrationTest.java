package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReferenceType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@SuppressWarnings("deprecation")
@RunWith(FenixFrameworkRunner.class)
public class BibliographicReferenceMigrationTest {

    public static final String COURSES_UNIT_PATH = "QS>Courses>CC";
    public static final String COURSE_A_CODE = "CA";

    private static CompetenceCourse competenceCourseA;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initCompetenceCourse();
            return null;
        });
    }

    public static void initCompetenceCourse() {
        OrganizationalStructureTest.initTypes();
        OrganizationalStructureTest.initUnits();

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();

        Unit coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();
        competenceCourseA = CompetenceCourseTest.createCompetenceCourse("Course A", COURSE_A_CODE, new BigDecimal("6.0"),
                AcademicPeriod.SEMESTER, ExecutionInterval.findFirstCurrentChild(null), coursesUnit);
    }

    @BeforeAll
    public void testSwitch_init() {
        assertEquals(CompetenceCourse.find(COURSE_A_CODE), competenceCourseA);
        assertNull(CompetenceCourse.find("XX"));
        assertEquals(CompetenceCourse.findAll().size(), 1);
        assertTrue(CompetenceCourse.findAll().contains(competenceCourseA));
        assertTrue(competenceCourseA.getCompetenceCourseInformationsSet().size() == 1);
    }

    @Test
    public void test() { //to bypass concurrency problems while running tests
        testSwitch_crudOperationsAffectBothValueTypeAndEntity();
        testSwitch_referenceOrder();
        testSwitch_copy();
    }

    public void testSwitch_crudOperationsAffectBothValueTypeAndEntity() {
        //ADD
        final CompetenceCourseInformation courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();
        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().isEmpty());
        assertTrue(courseInformation.getBibliographiesSet().isEmpty());

        BibliographicReferences bibliographicReferences = courseInformation.getBibliographicReferences();
        bibliographicReferences = bibliographicReferences.with("2024", "Felicidade na Produtividade de Trabalho",
                "Me, Myself and I", "qubIT", null, BibliographicReferenceType.MAIN);
        setBibliographicReferences(courseInformation, bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 1);
        assertTrue(courseInformation.getBibliographiesSet().size() == 1);
        assertTrue(equals(courseInformation.getBibliographicReferences().getBibliographicReferencesList().get(0),
                courseInformation.getBibliographiesSet().iterator().next()));

        //"EDIT"
        bibliographicReferences = courseInformation.getBibliographicReferences().replacing(0, "2024",
                "Como a Qualidade do código cria Bem-Estar", "Me, Myself and I", "qubIT", null, BibliographicReferenceType.MAIN);
        setBibliographicReferences(courseInformation, bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 1);
        assertTrue(courseInformation.getBibliographiesSet().size() == 1);
        assertTrue(equals(courseInformation.getBibliographicReferences().getBibliographicReferencesList().get(0),
                courseInformation.getBibliographiesSet().iterator().next()));

        //DELETE
        bibliographicReferences = courseInformation.getBibliographicReferences().without(0);
        setBibliographicReferences(courseInformation, bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().isEmpty());
        assertTrue(courseInformation.getBibliographiesSet().isEmpty());
    }

    public static boolean equals(org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReference a,
            BibliographicReference b) {
        return StringUtils.equals(a.getTitle(), b.getTitle())
                && StringUtils.equals(a.getTitle(),
                        b.getLocalizedTitle() == null ? null : b.getLocalizedTitle().getContent(Locale.getDefault()))
                && StringUtils.equals(a.getAuthors(), b.getAuthors()) && StringUtils.equals(a.getReference(), b.getReference())
                && StringUtils.equals(a.getReference(),
                        b.getLocalizedReference() == null ? null : b.getLocalizedReference().getContent(Locale.getDefault()))
                && StringUtils.equals(a.getYear(), b.getYear()) && StringUtils.equals(a.getUrl(), b.getUrl())
                && a.getOrder() == (b.getReferenceOrder() == null ? null : b.getReferenceOrder().intValue())
                && (BibliographicReferenceType.SECONDARY.equals(a.getType()) == Boolean.TRUE.equals(b.getOptional()));
    }

    public void testSwitch_referenceOrder() {

        final CompetenceCourseInformation courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();
        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().isEmpty());
        assertTrue(courseInformation.getBibliographiesSet().isEmpty());

        BiPredicate<BibliographicReferences, Set<BibliographicReference>> isOrderEqual = (vt, de) ->  //valueType, domainEntity
        vt.getBibliographicReferencesList().stream()
                .allMatch(vtBr -> de.stream().anyMatch(deBr -> deBr.getTitle().equals(vtBr.getTitle())
                        && deBr.getReferenceOrder() != null && vtBr.getOrder() == deBr.getReferenceOrder().intValue()));;

        //1
        BibliographicReferences bibliographicReferences = courseInformation.getBibliographicReferences();
        bibliographicReferences = bibliographicReferences.with("2024", "Código Engraçado", "Me, Myself and I", "qubIT", null,
                BibliographicReferenceType.MAIN);
        setBibliographicReferences(courseInformation, bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 1);
        assertTrue(courseInformation.getBibliographiesSet().size() == 1);
        assertTrue(equals(courseInformation.getBibliographicReferences().getBibliographicReferencesList().get(0),
                courseInformation.getBibliographiesSet().iterator().next()));
        assertTrue(isOrderEqual.test(courseInformation.getBibliographicReferences(), courseInformation.getBibliographiesSet()));

        //2
        bibliographicReferences = courseInformation.getBibliographicReferences();
        bibliographicReferences = bibliographicReferences.with("2024", "Trabalho Feliz", "Me, Myself and I", "qubIT", null,
                BibliographicReferenceType.SECONDARY);
        setBibliographicReferences(courseInformation, bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 2);
        assertTrue(courseInformation.getBibliographiesSet().size() == 2);
        assertTrue(isOrderEqual.test(courseInformation.getBibliographicReferences(), courseInformation.getBibliographiesSet()));

        //3
        bibliographicReferences = courseInformation.getBibliographicReferences();
        bibliographicReferences = bibliographicReferences.with("2024", "*Someone* is Amazing", "Me, Myself and I", "qubIT", null,
                BibliographicReferenceType.MAIN);
        setBibliographicReferences(courseInformation, bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 3);
        assertTrue(courseInformation.getBibliographiesSet().size() == 3);
        assertTrue(isOrderEqual.test(courseInformation.getBibliographicReferences(), courseInformation.getBibliographiesSet()));

        //test CompetenceCourse api
        final CompetenceCourse competenceCourse = courseInformation.getCompetenceCourse();
        assertTrue(competenceCourse.getMainBibliographicReferences().size() == 2);
        assertTrue(competenceCourse.findBibliographies().filter(br -> !br.isOptional()).count() == 2);

        assertTrue(competenceCourse.getSecondaryBibliographicReferences().size() == 1);
        assertTrue(competenceCourse.findBibliographies().filter(br -> br.isOptional()).count() == 1);
        assertTrue(equals(competenceCourse.getSecondaryBibliographicReferences().get(0),
                competenceCourse.findBibliographies().filter(br -> br.isOptional()).findAny().get()));

        //Clear test data
        courseInformation.setBibliographicReferences(new BibliographicReferences());
        courseInformation.getBibliographiesSet().stream().forEach(br -> br.delete());
    }

    public void testSwitch_copy() {

        final CompetenceCourseInformation courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();
        assertTrue(courseInformation.getBibliographiesSet().isEmpty());

        //1
        BibliographicReference bibliographicReference =
                BibliographicReference.create("Código Engraçado", "Me, Myself and I", "qubIT", "2024", false);
        bibliographicReference.setCompetenceCourseInformation(courseInformation);

        BibliographicReference copy = bibliographicReference.copy();
        assertTrue(copy.getCompetenceCourseInformation() == null);
        assertTrue(!bibliographicReference.equals(copy));

        //2
        BibliographicReference bibliographicReference1 =
                BibliographicReference.create("Código Engraçado", null, null, null, false);
        bibliographicReference1.setCompetenceCourseInformation(courseInformation);

        BibliographicReference copy1 = bibliographicReference1.copy();
        assertTrue(copy1.getCompetenceCourseInformation() == null);
        assertTrue(!bibliographicReference1.equals(copy1));

        //3
        assertThrows(IllegalArgumentException.class,
                () -> BibliographicReference.create(null, "Me, Myself and I", null, null, false));

        //4
        bibliographicReference.setTitle(null);
        assertThrows(IllegalArgumentException.class, () -> bibliographicReference.copy());
    }

    private void setBibliographicReferences(final CompetenceCourseInformation courseInformation,
            BibliographicReferences bibliographicReferences) {
        courseInformation.setBibliographicReferences(bibliographicReferences);
        courseInformation.getBibliographiesSet().stream().forEach(br -> br.delete());
        courseInformation.getBibliographiesSet().addAll(BibliographicReference.createDomainEntitiesFrom(bibliographicReferences));
    }

}
