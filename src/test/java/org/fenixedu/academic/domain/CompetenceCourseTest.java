package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevelType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLoad;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CompetenceCourseTest {

    public static final String COURSE_A_CODE = "CA";
    private static CompetenceCourse competenceCourse;

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

        LocalizedString name = new LocalizedString.Builder().with(Locale.getDefault(), "Courses Unit").build();
        Unit coursesUnit = Unit.createNewUnit(PartyType.of(PartyTypeEnum.COMPETENCE_COURSE_GROUP), name, "CC", planetUnit,
                AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE));

        ExecutionIntervalTest.initRootCalendarAndExecutionYears();

        competenceCourse = new CompetenceCourse("Course A", "Course A", Boolean.TRUE, AcademicPeriod.SEMESTER,
                CompetenceCourseLevelType.UNKNOWN().orElse(null), CompetenceCourseType.REGULAR, CurricularStage.APPROVED,
                coursesUnit, ExecutionInterval.findFirstCurrentChild(null), new GradeScale());
        competenceCourse.setCode(COURSE_A_CODE);

        final CompetenceCourseInformation courseInformation =
                competenceCourse.getCompetenceCourseInformationsSet().iterator().next();

        new CompetenceCourseLoad(courseInformation, 30d, 0d, 10d, 0d, 0d, 0d, 0d, 0d, 0d, 6d, 1, AcademicPeriod.SEMESTER);

        final CompetenceCourseInformation nextCourseInformation = new CompetenceCourseInformation(courseInformation);
        final ExecutionYear nextExecutionYear = (ExecutionYear) ExecutionYear.findCurrentAggregator(null).getNext();
        nextCourseInformation.setExecutionInterval(nextExecutionYear.getFirstExecutionPeriod());

        final CompetenceCourseLoad nextLoad = nextCourseInformation.getCompetenceCourseLoadsSet().iterator().next();
        nextLoad.setTheoreticalHours(0d);
        nextLoad.setProblemsHours(15d);
    }

    @Test
    public void testCourse_find() {
        assertEquals(CompetenceCourse.find(COURSE_A_CODE), competenceCourse);
        assertNull(CompetenceCourse.find("XX"));
        assertEquals(CompetenceCourse.findAll().size(), 1);
        assertTrue(CompetenceCourse.findAll().contains(competenceCourse));
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

        final CompetenceCourseInformation currentInformation = competenceCourse.findInformationMostRecentUntil(currentInterval);
        final CompetenceCourseInformation previousInformation = competenceCourse.findInformationMostRecentUntil(previousInterval);
        final CompetenceCourseInformation nextInformation = competenceCourse.findInformationMostRecentUntil(nextInterval);
        final CompetenceCourseInformation nextNextInformation = competenceCourse.findInformationMostRecentUntil(nextNextInterval);

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
        assertEquals(competenceCourse.getTheoreticalHours(), 0d, 0d);
        assertEquals(competenceCourse.getProblemsHours(), 15d, 0d);

        final ExecutionInterval currentInterval = ExecutionInterval.findFirstCurrentChild(null);
        assertEquals(competenceCourse.getTheoreticalHours(currentInterval), 30d, 0d);
        assertEquals(competenceCourse.getProblemsHours(currentInterval), 0d, 0d);
    }

}
