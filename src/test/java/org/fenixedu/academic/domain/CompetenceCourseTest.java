package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLoad;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseTest {

    public static final String COURSE_A_CODE = "CA";
    public static final String COURSE_B_CODE = "CB"; // annual

    private static CompetenceCourse competenceCourseA;
    private static CompetenceCourse competenceCourseB;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initCompetenceCourse();
            return null;
        });
    }

    static void initCompetenceCourse() {
        OrganizationalStructureTest.initTypes();
        Unit planetUnit = OrganizationalStructureTest.initPlanetUnit();

        LocalizedString unitName = new LocalizedString.Builder().with(Locale.getDefault(), "Courses Unit").build();
        Unit coursesUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.COMPETENCE_COURSE_GROUP), unitName, "CC", planetUnit,
                AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE));

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();

        initCourseLoadTypes();

        createCompetenceCourseASemester(coursesUnit);
        createCompetenceCourseBAnnual(coursesUnit);
    }

    private static void createCompetenceCourseASemester(Unit coursesUnit) {
        competenceCourseA = createCompetenceCourse("Course A", COURSE_A_CODE, AcademicPeriod.SEMESTER, coursesUnit);

        final CompetenceCourseInformation courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();

        new CompetenceCourseLoad(courseInformation, 30d, 0d, 10d, 0d, 0d, 0d, 0d, 0d, 20d, 6d, 1, AcademicPeriod.SEMESTER);

        final CompetenceCourseInformation nextCourseInformation = new CompetenceCourseInformation(courseInformation);
        final ExecutionYear nextExecutionYear = (ExecutionYear) ExecutionYear.findCurrentAggregator(null).getNext();
        nextCourseInformation.setExecutionInterval(nextExecutionYear.getFirstExecutionPeriod());

        final CompetenceCourseLoad nextLoad = nextCourseInformation.getCompetenceCourseLoadsSet().iterator().next();
        nextLoad.setTheoreticalHours(0d);
        nextLoad.setProblemsHours(15d);
    }

    private static void createCompetenceCourseBAnnual(Unit coursesUnit) {
        competenceCourseB = createCompetenceCourse("Course B", COURSE_B_CODE, AcademicPeriod.YEAR, coursesUnit);

        final CompetenceCourseInformation courseInformation =
                competenceCourseB.getCompetenceCourseInformationsSet().iterator().next();

        new CompetenceCourseLoad(courseInformation, 30d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 6d, 1, AcademicPeriod.SEMESTER);
        new CompetenceCourseLoad(courseInformation, 10d, 0d, 5d, 0d, 0d, 0d, 0d, 0d, 0d, 9d, 2, AcademicPeriod.SEMESTER);
    }

    private static CompetenceCourse createCompetenceCourse(final String name, final String code, final AcademicPeriod duration,
            Unit coursesUnit) {
        final CompetenceCourse result = new CompetenceCourse(name, name, Boolean.TRUE, duration,
                CompetenceCourseLevelType.UNKNOWN().orElse(null), CompetenceCourseType.REGULAR, CurricularStage.APPROVED,
                coursesUnit, ExecutionInterval.findFirstCurrentChild(null), new GradeScale());
        result.setCode(code);

        return result;
    }

    private static void initCourseLoadTypes() {
        if (CourseLoadType.findAll().findAny().isEmpty()) {
            Function<String, LocalizedString> nameProvider = type -> BundleUtil.getLocalizedString(Bundle.ENUMERATION,
                    CourseLoadType.class.getName() + "." + type + ".name");

            Function<String, LocalizedString> initialsProvider = type -> BundleUtil.getLocalizedString(Bundle.ENUMERATION,
                    CourseLoadType.class.getName() + "." + type + ".initials");

            CourseLoadType.create(CourseLoadType.THEORETICAL, nameProvider.apply(CourseLoadType.THEORETICAL),
                    initialsProvider.apply(CourseLoadType.THEORETICAL), true);
            CourseLoadType.create(CourseLoadType.THEORETICAL_PRACTICAL, nameProvider.apply(CourseLoadType.THEORETICAL_PRACTICAL),
                    initialsProvider.apply(CourseLoadType.THEORETICAL_PRACTICAL), true);
            CourseLoadType.create(CourseLoadType.PRACTICAL_LABORATORY, nameProvider.apply(CourseLoadType.PRACTICAL_LABORATORY),
                    initialsProvider.apply(CourseLoadType.PRACTICAL_LABORATORY), true);
            CourseLoadType.create(CourseLoadType.FIELD_WORK, nameProvider.apply(CourseLoadType.FIELD_WORK),
                    initialsProvider.apply(CourseLoadType.FIELD_WORK), true);
            CourseLoadType.create(CourseLoadType.SEMINAR, nameProvider.apply(CourseLoadType.SEMINAR),
                    initialsProvider.apply(CourseLoadType.SEMINAR), true);
            CourseLoadType.create(CourseLoadType.INTERNSHIP, nameProvider.apply(CourseLoadType.INTERNSHIP),
                    initialsProvider.apply(CourseLoadType.INTERNSHIP), true);
            CourseLoadType.create(CourseLoadType.TUTORIAL_ORIENTATION, nameProvider.apply(CourseLoadType.TUTORIAL_ORIENTATION),
                    initialsProvider.apply(CourseLoadType.TUTORIAL_ORIENTATION), true);
            CourseLoadType.create(CourseLoadType.OTHER, nameProvider.apply(CourseLoadType.OTHER),
                    initialsProvider.apply(CourseLoadType.OTHER), true);

            CourseLoadType.create(CourseLoadType.AUTONOMOUS_WORK, nameProvider.apply(CourseLoadType.AUTONOMOUS_WORK),
                    initialsProvider.apply(CourseLoadType.AUTONOMOUS_WORK), false);
        }
    }

    @Test
    public void testCourseLoad_init() {
        assertNotNull(CourseLoadType.of(CourseLoadType.THEORETICAL));
        assertNotNull(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL));
        assertNotNull(CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY));
        assertEquals(CourseLoadType.of(CourseLoadType.THEORETICAL).getCode(), CourseLoadType.THEORETICAL);
    }

    @Test
    public void testCourse_find() {
        assertEquals(CompetenceCourse.find(COURSE_A_CODE), competenceCourseA);
        assertNull(CompetenceCourse.find("XX"));
        assertEquals(CompetenceCourse.findAll().size(), 2);
        assertTrue(CompetenceCourse.findAll().contains(competenceCourseA));
    }

    @Test
    public void testCourse_findInformation() {
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final ExecutionInterval previousInterval = currentInterval.getPrevious();
        final ExecutionInterval nextInterval = currentInterval.getNext();
        final ExecutionInterval nextNextInterval = nextInterval.getNext();

        assertNotNull(currentInterval);
        assertNotNull(previousInterval);
        assertNotNull(nextInterval);
        assertNotNull(nextNextInterval);

        final CompetenceCourseInformation currentInformation = competenceCourseA.findInformationMostRecentUntil(currentInterval);
        final CompetenceCourseInformation previousInformation =
                competenceCourseA.findInformationMostRecentUntil(previousInterval);
        final CompetenceCourseInformation nextInformation = competenceCourseA.findInformationMostRecentUntil(nextInterval);
        final CompetenceCourseInformation nextNextInformation =
                competenceCourseA.findInformationMostRecentUntil(nextNextInterval);

        assertNotNull(currentInformation);
        assertNotNull(previousInformation);
        assertNotNull(nextInformation);
        assertNotNull(nextNextInformation);

        assertEquals(currentInformation, previousInformation);
        assertEquals(currentInformation, nextInformation);
        assertNotEquals(currentInformation, nextNextInformation);

    }

    @Test
    public void testCourse_courseLoad() {
        assertEquals(competenceCourseA.getTheoreticalHours(), 0d, 0d);
        assertEquals(competenceCourseA.getProblemsHours(), 15d, 0d);

        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        assertEquals(competenceCourseA.getTheoreticalHours(currentInterval), 30d, 0d);
        assertEquals(competenceCourseA.getProblemsHours(currentInterval), 0d, 0d);

        final CompetenceCourseInformation informationA = competenceCourseA.findInformationMostRecentUntil(currentInterval);

        assertEquals(informationA.getContactLoad(), new BigDecimal("40.0"));
        assertEquals(informationA.getTotalLoad(), new BigDecimal("60.0"));

        assertEquals(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), currentInterval).orElseThrow(),
                new BigDecimal("30.0"));
        assertNull(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL)).orElse(null));
        assertEquals(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL)).orElseThrow(),
                new BigDecimal("15.0"));
    }

    @Test
    public void testCourse_courseLoadAnnual() {
        assertEquals(competenceCourseB.getTheoreticalHours(), 40d, 0d);
        assertEquals(competenceCourseB.getProblemsHours(), 0d, 0d);
        assertEquals(competenceCourseB.getLaboratorialHours(), 5d, 0d);

        assertEquals(competenceCourseB.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL)).orElseThrow(),
                new BigDecimal("40.0"));
        assertNull(competenceCourseB.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL)).orElse(null));
        assertEquals(competenceCourseB.getLoadHours(CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY)).orElseThrow(),
                new BigDecimal("5.0"));
    }

    @Test
    public void testCourse_credits() {
        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        final CompetenceCourseInformation informationA = competenceCourseA.findInformationMostRecentUntil(currentInterval);
        final CompetenceCourseInformation informationB = competenceCourseB.findInformationMostRecentUntil(currentInterval);

        assertEquals(competenceCourseA.getEctsCredits(currentInterval), 6d, 0d);
        assertEquals(informationA.getEctsCredits(1), 6d, 0d);
        assertEquals(informationA.getCredits(), new BigDecimal("6.0"));

        assertEquals(competenceCourseB.getEctsCredits(currentInterval), 15d, 0d);
        assertEquals(informationB.getCredits(), new BigDecimal("15.0"));

        Map<Integer, CompetenceCourseLoad> loadsBySemester = informationB.getCompetenceCourseLoadsSet().stream()
                .collect(Collectors.toMap(CompetenceCourseLoad::getLoadOrder, l -> l));

        loadsBySemester.get(1).setEctsCredits(7d);
        assertEquals(informationB.getCredits(), new BigDecimal("16.0"));

        loadsBySemester.get(2).setEctsCredits(10d);
        assertEquals(informationB.getCredits(), new BigDecimal("17.0"));

        loadsBySemester.get(2).setEctsCredits(0d);
        assertEquals(informationB.getCredits(), new BigDecimal("7.0"));
    }

}
