package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReferenceType;
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
public class StranglerFigSwitchBibliographicReferencesTest {

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
    public void testSwitch_crudOperationsAffectBothValueTypeAndEntity() {
        //ADD
        final CompetenceCourseInformation courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();
        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().isEmpty());
        assertTrue(courseInformation.getBibliographiesSet().isEmpty());

        BibliographicReferences bibliographicReferences = courseInformation.getBibliographicReferences();
        bibliographicReferences = bibliographicReferences.with("2024", "Felicidade na Produtividade de Trabalho",
                "Sofia Nascimento", "qubIT", null, BibliographicReferenceType.MAIN);
        courseInformation.setBibliographicReferences(bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 1);
        assertTrue(courseInformation.getBibliographiesSet().size() == 1);
        assertTrue(equals(courseInformation.getBibliographicReferences().getBibliographicReferencesList().get(0),
                courseInformation.getBibliographiesSet().iterator().next()));

        //"EDIT"
        bibliographicReferences = courseInformation.getBibliographicReferences().replacing(0, "2024",
                "Como a Qualidade do código cria Bem-Estar", "Sofia Nascimento", "qubIT", null, BibliographicReferenceType.MAIN);
        courseInformation.setBibliographicReferences(bibliographicReferences);

        assertTrue(courseInformation.getBibliographicReferences().getBibliographicReferencesList().size() == 1);
        assertTrue(courseInformation.getBibliographiesSet().size() == 1);
        assertTrue(equals(courseInformation.getBibliographicReferences().getBibliographicReferencesList().get(0),
                courseInformation.getBibliographiesSet().iterator().next()));

        //DELETE
        bibliographicReferences = courseInformation.getBibliographicReferences().without(0);
        courseInformation.setBibliographicReferences(bibliographicReferences);

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

}
