package org.fenixedu.academic.domain;

import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class DegreeCurricularPlanTest {

    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse;

    public static final String DCP_NAME_V1 = "DCP_NAME_V1";
    public static final String DCP_NAME_V2 = "DCP_NAME_V2";
    public static final String DCP_NAME_V3 = "DCP_NAME_V3";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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

        // TODO: remove this user initialization to proper class
        final UserProfile userProfile =
                new UserProfile("Fenix", "Admin", "Fenix Admin", "fenix.admin@fenixedu.com", Locale.getDefault());
        new User("admin", userProfile);
        new Person(userProfile);

        degreeCurricularPlan = new DegreeCurricularPlan(degree, DCP_NAME_V1, AcademicPeriod.THREE_YEAR);
        degreeCurricularPlan.setCurricularStage(CurricularStage.APPROVED);

        new DegreeCurricularPlan(degree, DCP_NAME_V2, AcademicPeriod.THREE_YEAR);
        new DegreeCurricularPlan(degree, DCP_NAME_V3, AcademicPeriod.THREE_YEAR);

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
    public void createDCP_withExistingName() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final String dcpName = UUID.randomUUID().toString();
        new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.existing.name.and.degree");

        new DegreeCurricularPlan(degree, dcpName, AcademicPeriod.THREE_YEAR);
    }

    @Test
    public void editDCP_withExistingName() {
        final Degree degree = Degree.find(DEGREE_A_CODE);
        final String dcp1Name = UUID.randomUUID().toString();
        final DegreeCurricularPlan dcp1 = new DegreeCurricularPlan(degree, dcp1Name, AcademicPeriod.THREE_YEAR);

        dcp1.setName(dcp1Name); // no error, it's the same dcp

        final String dcp2Name = UUID.randomUUID().toString();
        final DegreeCurricularPlan dcp2 = new DegreeCurricularPlan(degree, dcp2Name, AcademicPeriod.THREE_YEAR);

        exceptionRule.expect(DomainException.class);
        exceptionRule.expectMessage("error.degreeCurricularPlan.existing.name.and.degree");

        dcp2.setName(dcp1Name);
    }

    @Test
    public void findDegree_byCode() {
        final Degree degreeCS = Degree.find(DEGREE_A_CODE);
        assertTrue(degreeCS.getDegreeCurricularPlansSet().contains(degreeCurricularPlan));
    }

    @Test
    public void getAllCurricularCourses() {
        final Set<CurricularCourse> allCurricularCourses = degreeCurricularPlan.getAllCurricularCourses();
        assertTrue(allCurricularCourses.size() == 1);
        assertTrue(allCurricularCourses.contains(curricularCourse));
    }

}
