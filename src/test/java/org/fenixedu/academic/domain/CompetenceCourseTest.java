package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.degreeStructure.CompetenceCourseTypeTest.initCompetenceCourseType;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Function;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseTest {

    public static final String COURSES_UNIT_PATH = "QS>Courses>CC";
    public static final String COURSE_A_CODE = "CA";
    public static final String COURSE_B_CODE = "CB"; // annual

    private static ExecutionYear executionYear, nextExecutionYear;
    private static ExecutionInterval executionInterval;
    private static CurricularPeriod firstSemester;
    private static CompetenceCourse competenceCourseA, competenceCourseB;
    private static CompetenceCourseInformation courseInformation, nextCourseInformation;
    private static CurricularCourse testCurricularCourse;
    private static DegreeCurricularPlan testDegreeCurricularPlan;
    private static Unit coursesUnit;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initCompetenceCourse();
            initDomainTestData();
            return null;
        });
    }

    public static void initCompetenceCourse() {
        OrganizationalStructureTest.initTypes();
        OrganizationalStructureTest.initUnits();

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();

        initCourseLoadTypes();

        executionYear = ExecutionYear.findCurrent(null);
        executionInterval = executionYear.getFirstExecutionPeriod();
        nextExecutionYear = (ExecutionYear) executionYear.getNext();
        coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();

        createCompetenceCourseASemester();
        createCompetenceCourseBAnnual();
    }

    private static void initDomainTestData() {
        DegreeType degreeType =
                new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "CC Test Degree Type").build());
        degreeType.setCode("CC_DEGREE_TYPE");

        Degree degree =
                new Degree("CC Test Degree", "CC Test Degree", "CC_DEGREE", degreeType, new GradeScale(), new GradeScale(),
                        executionYear);
        degree.setCode("CC_DEGREE");
        degree.setCalendar(executionYear.getAcademicInterval().getAcademicCalendar());

        testDegreeCurricularPlan = new DegreeCurricularPlan(degree, "CC_DCP", AcademicPeriod.THREE_YEAR);
        testDegreeCurricularPlan.setCurricularStage(CurricularStage.APPROVED);

        CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, testDegreeCurricularPlan.getDegreeStructure());
        firstSemester = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

        testCurricularCourse = new CurricularCourse();
        testCurricularCourse.setCompetenceCourse(competenceCourseA);
    }

    @After
    public void cleanup() {
        CompetenceCourse.findAll().stream().filter(cc -> cc != competenceCourseA && cc != competenceCourseB)
                .forEach(CompetenceCourse::delete);

        competenceCourseA.getCompetenceCourseInformationsSet().stream()
                .filter(cci -> cci != courseInformation && cci != nextCourseInformation)
                .forEach(CompetenceCourseInformation::delete);

        Unit.findInternalUnitByAcronymPath("TCG").ifPresent(Unit::delete);
        Unit.findInternalUnitByAcronymPath("TD").ifPresent(Unit::delete);
    }

    private static void createCompetenceCourseASemester() {
        competenceCourseA = createCompetenceCourse("Course A", COURSE_A_CODE, new BigDecimal("6.0"), SEMESTER, executionInterval,
                coursesUnit);

        courseInformation =
                competenceCourseA.getCompetenceCourseInformationsSet().iterator().next();
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), new BigDecimal("30.0"));
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY), new BigDecimal("10.0"));
        courseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.AUTONOMOUS_WORK), new BigDecimal("20.0"));

        nextCourseInformation =
                new CompetenceCourseInformation(courseInformation, nextExecutionYear.getFirstExecutionPeriod());

        nextCourseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), new BigDecimal("5.0"));
        nextCourseInformation.setLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL), new BigDecimal("15.0"));
    }

    private static void createCompetenceCourseBAnnual() {
        competenceCourseB = createCompetenceCourse("Course B", COURSE_B_CODE, new BigDecimal("15.0"), AcademicPeriod.YEAR,
                executionInterval, coursesUnit);

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
    public void testCompetenceCourse_courseLoadType_init() {
        assertNotNull(CourseLoadType.of(CourseLoadType.THEORETICAL));
        assertNotNull(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL));
        assertNotNull(CourseLoadType.of(CourseLoadType.PRACTICAL_LABORATORY));
        assertEquals(CourseLoadType.of(CourseLoadType.THEORETICAL).getCode(), CourseLoadType.THEORETICAL);
    }

    @Deprecated(forRemoval = true)
    @Test
    public void testCourse_find() {
        assertEquals(CompetenceCourse.find(COURSE_A_CODE), competenceCourseA);
        assertNull(CompetenceCourse.find("XX"));
//        assertEquals(CompetenceCourse.findAll().size(), 2);
        assertTrue(CompetenceCourse.findAll().contains(competenceCourseA));
    }

    @Test
    public void testCompetenceCourse_courseLoad() {
        assertEquals(competenceCourseA.getTheoreticalHours(), 5d, 0d);
        assertEquals(competenceCourseA.getProblemsHours(), 15d, 0d);

        assertEquals(competenceCourseA.getTheoreticalHours(executionInterval), 30d, 0d);
        assertEquals(competenceCourseA.getProblemsHours(executionInterval), 0d, 0d);

        final CompetenceCourseInformation informationA = competenceCourseA.findInformationMostRecentUntil(executionInterval);

        assertEquals(informationA.getContactLoad(), new BigDecimal("40.0"));
        assertEquals(informationA.getTotalLoad(), new BigDecimal("60.0"));

        assertEquals(
                competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL), executionInterval).orElseThrow(),
                new BigDecimal("30.0"));
        assertNotNull(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL)).orElse(null));
        assertNull(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL), executionInterval)
                .orElse(null));
        assertEquals(competenceCourseA.getLoadHours(CourseLoadType.of(CourseLoadType.THEORETICAL_PRACTICAL)).orElseThrow(),
                new BigDecimal("15.0"));
    }

    @Test
    public void testCompetenceCourse_courseLoadAnnual() {
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
    public void testCompetenceCourse_credits() {
        final CompetenceCourseInformation informationA = competenceCourseA.findInformationMostRecentUntil(executionInterval);
        final CompetenceCourseInformation informationB = competenceCourseB.findInformationMostRecentUntil(executionInterval);

        assertEquals(competenceCourseA.getEctsCredits(executionInterval), 6d, 0d);
        assertEquals(informationA.getCredits(), new BigDecimal("6.0"));

        assertEquals(competenceCourseB.getEctsCredits(executionInterval), 15d, 0d);
        assertEquals(informationB.getCredits(), new BigDecimal("15.0"));
    }

    @Test
    public void testCompetenceCourse_creation() {
        CompetenceCourseType competenceCourseType = initCompetenceCourseType();
        GradeScale gradeScale = new GradeScale();
        LocalizedString name = new LocalizedString(Locale.getDefault(), "Test Course");
        BigDecimal credits = new BigDecimal("7.5");

        CompetenceCourse course = new CompetenceCourse("TC001", name, "TST", credits, coursesUnit, SEMESTER, competenceCourseType,
                CompetenceCourseLevelType.UNKNOWN().orElse(null), executionInterval, gradeScale);

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
                        executionInterval, gradeScale);
        assertEquals("GAC", courseWitGeneratedAcronym.getAcronym());
    }

    @Test
    public void testCompetenceCourse_creationWithExistingCode() {
        String existingCode = "EXISTS";

        createCompetenceCourse("A", existingCode, new BigDecimal("1.0"), SEMESTER, executionInterval, coursesUnit);

        assertThrows(DomainException.class,
                () -> createCompetenceCourse("B", existingCode, new BigDecimal("2.0"), SEMESTER, executionInterval, coursesUnit));
    }

    @Test
    public void testCompetenceCourse_creationWithInvalidExecutionIntervals() {
        ExecutionInterval startInterval = ExecutionInterval.findCurrentAggregator(null);

        assertThrows(DomainException.class,
                () -> createCompetenceCourse("Invalid interval course", "INVALID_INTERVAL_CODE", new BigDecimal("1.0"), SEMESTER,
                        null, coursesUnit));

        assertThrows(DomainException.class,
                () -> createCompetenceCourse("Invalid interval course", "INVALID_INTERVAL_CODE", new BigDecimal("1.0"), SEMESTER,
                        startInterval, coursesUnit));
    }

    @Test
    public void testCompetenceCourse_delete() {
        CompetenceCourse competenceCourse =
                createCompetenceCourse("Course to Delete", "DELETE", new BigDecimal("6.0"), SEMESTER, executionInterval,
                        coursesUnit);

        assertNotNull(CompetenceCourse.find("DELETE"));

        competenceCourse.delete();

        assertNull(CompetenceCourse.find("DELETE"));

        assertThrows(DomainException.class, () -> competenceCourseA.delete(), "mustDeleteCurricularCoursesFirst");
    }

    @Test
    public void testCompetenceCourse_comparatorByName() {
        assertTrue(CompetenceCourse.COMPETENCE_COURSE_COMPARATOR_BY_NAME.compare(competenceCourseA, competenceCourseB) < 0);
        assertTrue(CompetenceCourse.COMPETENCE_COURSE_COMPARATOR_BY_NAME.compare(competenceCourseB, competenceCourseA) > 0);
        assertEquals(0, CompetenceCourse.COMPETENCE_COURSE_COMPARATOR_BY_NAME.compare(competenceCourseA, competenceCourseA));
    }

    @Test
    public void testCompetenceCourse_findInformationMostRecentUntil_withExecutionInterval() {
        assertEquals(courseInformation, competenceCourseA.findInformationMostRecentUntil(executionInterval));

        // Creating a new CompetenceCourseInformation in the same execution interval
        CompetenceCourseInformation newInformation = new CompetenceCourseInformation(courseInformation, executionInterval);

        assertEquals(newInformation, competenceCourseA.findInformationMostRecentUntil(executionInterval));

        // Testing findInformationMostRecentUntil for a later interval
        assertEquals(nextCourseInformation,
                competenceCourseA.findInformationMostRecentUntil(nextExecutionYear.getFirstExecutionPeriod()));
    }

    @Test
    public void testCompetenceCourse_findInformationMostRecentUntil_withExecutionYear() {
        assertEquals(courseInformation, competenceCourseA.findInformationMostRecentUntil(executionYear));

        // Creating a new CompetenceCourseInformation in the next execution year
        CompetenceCourseInformation newInformation =
                new CompetenceCourseInformation(courseInformation, nextExecutionYear.getFirstExecutionPeriod());

        assertEquals(newInformation, competenceCourseA.findInformationMostRecentUntil(nextExecutionYear));

        newInformation.delete();

        assertEquals(nextCourseInformation, competenceCourseA.findInformationMostRecentUntil(nextExecutionYear));
    }

    @Test
    public void testCompetenceCourse_isBasic() {
        // Initial state:
        //  - courseInformation in executionInterval (basic=false)
        //  - nextCourseInformation in nextExecutionYear.getFirstExecutionPeriod() (basic=false)
        assertFalse(competenceCourseA.isBasic(executionInterval)); // courseInformation
        assertFalse(competenceCourseA.isBasic()); // when no argument is passed, we get max by interval -> nextCourseInformation

        // Create newInformation in executionInterval, override basic to true
        CompetenceCourseInformation newInformation = new CompetenceCourseInformation(courseInformation, executionInterval);
        newInformation.setBasic(true);

        assertTrue(competenceCourseA.isBasic(executionInterval));  // newInformation is the latest in executionInterval
        assertFalse(competenceCourseA.isBasic());                  // nextCourseInformation in nextYear is still the max

        // Set newInformation's execution interval to become the latest version
        newInformation.setExecutionInterval(nextExecutionYear.getLastExecutionPeriod());

        assertTrue(competenceCourseA.isBasic(nextExecutionYear));  // newInformation is the latest in nextExecutionYear
        assertTrue(competenceCourseA.isBasic());                   // newInformation is now the max by interval
    }

    @Test
    public void testCompetenceCourse_getCurricularCourseContexts() {
        assertTrue(competenceCourseA.getCurricularCourseContexts().isEmpty());

        Context context =
                new Context(testDegreeCurricularPlan.getRoot(), testCurricularCourse, firstSemester, executionInterval, null);

        assertEquals(1, competenceCourseA.getCurricularCourseContexts().size());
        assertEquals(context, competenceCourseA.getCurricularCourseContexts().stream().findFirst().orElseThrow());

        context.delete();

        assertTrue(competenceCourseA.getCurricularCourseContexts().isEmpty());
    }

    @Test
    public void testCompetenceCourse_getCurricularCourse() {
        // competenceCourseA doesn't have associated curricular courses with a context in testDegreeCurricularPlan yet
        assertNull(competenceCourseA.getCurricularCourse(testDegreeCurricularPlan));

        // Create a context for testCurricularCourse in testDegreeCurricularPlan
        Context context =
                new Context(testDegreeCurricularPlan.getRoot(), testCurricularCourse, firstSemester, executionInterval, null);

        assertEquals(testCurricularCourse, competenceCourseA.getCurricularCourse(testDegreeCurricularPlan));

        context.delete();

        assertNull(competenceCourseA.getCurricularCourse(testDegreeCurricularPlan));
    }
}
