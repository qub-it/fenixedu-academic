package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Set;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class DegreeCurricularPlanTest {

    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse;

    public static final String DCP_NAME_V1 = "DCP_NAME_V1";
    public static final String DCP_NAME_V3 = "DCP_NAME_V3";
    public static final String DCP_NAME_V2 = "DCP_NAME_V2";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            initDegreeCurricularPlan();
            return null;
        });
    }

    static void initDegreeCurricularPlan() {
        DegreeTest.initDegree();
        final Degree degree = Degree.find(DEGREE_A_CODE);

        final UserProfile userProfile =
                new UserProfile("Fenix", "Admin", "Fenix Admin", "fenix.admin@fenixedu.com", Locale.getDefault());
        new User("admin", userProfile);
        Person person = new Person(userProfile);
        degreeCurricularPlan = degree.createDegreeCurricularPlan(DCP_NAME_V1, person, AcademicPeriod.THREE_YEAR);
        degreeCurricularPlan.setCurricularStage(CurricularStage.APPROVED);

        degree.createDegreeCurricularPlan(DCP_NAME_V2, person, AcademicPeriod.THREE_YEAR);
        degree.createDegreeCurricularPlan(DCP_NAME_V3, person, AcademicPeriod.THREE_YEAR);

        CompetenceCourseTest.initCompetenceCourse();
        final CompetenceCourse competenceCourse = CompetenceCourse.find(CompetenceCourseTest.COURSE_A_CODE);

        curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(competenceCourse);

        final CurricularPeriod yearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

        final ExecutionInterval executionInterval = ExecutionYear.findCurrent(null).getFirstExecutionPeriod();
        new Context(degreeCurricularPlan.getRoot(), curricularCourse, semesterPeriod, executionInterval, null);
    }

    @Test
    public void testDegreeCurricularPlan_find() {
        final Degree degreeCS = Degree.find(DEGREE_A_CODE);
        assertTrue(degreeCS.getDegreeCurricularPlansSet().size() == 3);
        assertTrue(degreeCS.getDegreeCurricularPlansSet().contains(degreeCurricularPlan));
    }

    @Test
    public void testDegreeCurricularPlan_curricularCourse() {
        final Set<CurricularCourse> allCurricularCourses = degreeCurricularPlan.getAllCurricularCourses();
        assertTrue(allCurricularCourses.size() == 1);
        assertTrue(allCurricularCourses.contains(curricularCourse));
    }

}
