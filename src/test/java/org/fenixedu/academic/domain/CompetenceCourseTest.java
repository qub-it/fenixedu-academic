package org.fenixedu.academic.domain;

import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseType;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.exceptions.DomainException;
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

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Function;

import static org.fenixedu.academic.domain.degreeStructure.CompetenceCourseTypeTest.initCompetenceCourseType;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseTest {

    public static final String COURSES_UNIT_PATH = "QS>Courses>CC";

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

    public static void initCompetenceCourse() {
        OrganizationalStructureTest.initTypes();
        OrganizationalStructureTest.initUnits();

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();

        initCourseLoadTypes();

        Unit coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();
        createCompetenceCourseASemester(coursesUnit);
        createCompetenceCourseBAnnual(coursesUnit);
    }

    private static void createCompetenceCourseASemester(Unit coursesUnit) {
        competenceCourseA = createCompetenceCourse("Course A", COURSE_A_CODE, new BigDecimal("6.0"), SEMESTER,
                ExecutionInterval.findFirstCurrentChild(null), coursesUnit);

        final CompetenceCourseInformation courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), new BigDecimal("30.0"));
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY), new BigDecimal("10.0"));
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.AUTONOMOUS_WORK), new BigDecimal("20.0"));

        final ExecutionYear nextExecutionYear = (ExecutionYear) ExecutionYear.findCurrentAggregator(null).getNext();
        final CompetenceCourseInformation nextCourseInformation =
                new CompetenceCourseInformation(courseInformation, nextExecutionYear.getFirstExecutionPeriod());

        nextCourseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), new BigDecimal("5.0"));
        nextCourseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL), new BigDecimal("15.0"));
    }

    private static void createCompetenceCourseBAnnual(Unit coursesUnit) {
        competenceCourseB = createCompetenceCourse("Course B", COURSE_B_CODE, new BigDecimal("15.0"), AcademicPeriod.YEAR,
                ExecutionInterval.findFirstCurrentChild(null), coursesUnit);

        final CompetenceCourseInformation courseInformation =
                competenceCourseB.getCompetenceCourseInformationsSet().iterator().next();
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), new BigDecimal("40.0"));
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY), new BigDecimal("5.0"));
    }

    public static CompetenceCourse createCompetenceCourse(final String name, final String code, BigDecimal credits,
            final AcademicPeriod duration, ExecutionInterval executionInterval, Unit coursesUnit) {
        CompetenceCourseType competenceCourseType = initCompetenceCourseType();
        return new CompetenceCourse(code, new LocalizedString(Locale.getDefault(), name), null, credits, coursesUnit, duration,
                competenceCourseType, CompetenceCourseLevelType.UNKNOWN().orElse(null), executionInterval, new GradeScale());
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
//        assertEquals(CompetenceCourse.findAll().size(), 2);
        assertTrue(CompetenceCourse.findAll().contains(competenceCourseA));
    }

    @Test
    public void testCourse_courseLoad() {
        assertEquals(competenceCourseA.getTheoreticalHours(), 5d, 0d);
        assertEquals(competenceCourseA.getProblemsHours(), 15d, 0d);

        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        assertEquals(competenceCourseA.getTheoreticalHours(currentInterval), 30d, 0d);
        assertEquals(competenceCourseA.getProblemsHours(currentInterval), 0d, 0d);

        final CompetenceCourseInformation informationA = competenceCourseA.findInformationMostRecentUntil(currentInterval);

        assertEquals(informationA.getContactLoad(), new BigDecimal("40.0"));
        assertEquals(informationA.getTotalLoad(), new BigDecimal("60.0"));

        assertEquals(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), currentInterval).orElseThrow(),
                new BigDecimal("30.0"));
        assertNotNull(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL)).orElse(null));
        assertNull(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL), currentInterval)
                .orElse(null));
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
        assertEquals(informationA.getCredits(), new BigDecimal("6.0"));

        assertEquals(competenceCourseB.getEctsCredits(currentInterval), 15d, 0d);
        assertEquals(informationB.getCredits(), new BigDecimal("15.0"));
    }

    @Test
    public void testCourse_creation() {
        Unit coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();
        CompetenceCourseType competenceCourseType = initCompetenceCourseType();
        ExecutionInterval startInterval = ExecutionInterval.findFirstCurrentChild(null);
        GradeScale gradeScale = new GradeScale();
        LocalizedString name = new LocalizedString(Locale.getDefault(), "Test Course");
        BigDecimal credits = new BigDecimal("7.5");

        CompetenceCourse course = new CompetenceCourse("TC001", name, "TST", credits, coursesUnit, SEMESTER, competenceCourseType,
                CompetenceCourseLevelType.UNKNOWN().orElse(null), startInterval, gradeScale);

        assertEquals("TC001", course.getCode());
        assertEquals(credits.doubleValue(), course.getEctsCredits(), 0d);
        assertEquals("TST", course.getAcronym());
        assertEquals("Test Course", course.getName());
        assertEquals("Test Course", course.getNameEn());
        assertEquals(coursesUnit, course.getCompetenceCourseGroupUnit());
        assertEquals(SEMESTER, course.getAcademicPeriod());
        assertEquals(competenceCourseType, course.getCompetenceCourseType());
        assertEquals(gradeScale, course.getGradeScale());

        CompetenceCourse courseWitGeneratedAcronym =
                new CompetenceCourse("GAC001", new LocalizedString(Locale.getDefault(), "Generated Acronym Course"), null,
                        credits, coursesUnit, SEMESTER, competenceCourseType, CompetenceCourseLevelType.UNKNOWN().orElse(null),
                        startInterval, gradeScale);
        assertEquals("GAC", courseWitGeneratedAcronym.getAcronym());
    }

    @Test
    public void testCourse_creationWithExistingCode() {
        Unit coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();
        ExecutionInterval startInterval = ExecutionInterval.findFirstCurrentChild(null);
        String existingCode = "EXISTS";

        createCompetenceCourse("A", existingCode, new BigDecimal("1.0"), SEMESTER, startInterval, coursesUnit);

        assertThrows(DomainException.class,
                () -> createCompetenceCourse("B", existingCode, new BigDecimal("2.0"), SEMESTER, startInterval, coursesUnit));
    }

    @Test
    public void testCourse_creationWithInvalidExecutionIntervals() {
        Unit coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();
        ExecutionInterval startInterval = ExecutionInterval.findCurrentAggregator(null);

        assertThrows(DomainException.class,
                () -> createCompetenceCourse("Invalid interval course", "INVALID_INTERVAL_CODE", new BigDecimal("1.0"), SEMESTER,
                        null, coursesUnit));

        assertThrows(DomainException.class,
                () -> createCompetenceCourse("Invalid interval course", "INVALID_INTERVAL_CODE", new BigDecimal("1.0"), SEMESTER,
                        startInterval, coursesUnit));
    }

}
