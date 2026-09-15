package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_B_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class NoCourseGroupCurriculumGroupTest {

    private static final String STUDENT_USERNAME = "no.course.group.test.student";
    private static final String DCP_NAME = "No Course Group DCP";

    private static StudentCurricularPlan scp;
    private static DegreeCurricularPlan dcp;
    private static ExecutionYear executionYear;
    private static ExecutionInterval executionInterval;
    private static CurricularCourse courseA;
    private static CurricularCourse courseB;
    private static CurricularCourse nonEnrolledCourse;
    private static NoCourseGroupCurriculumGroup extraGroup;
    private static CurriculumGroup childGroup;

    @BeforeClass
    public static void init() {
        // SCP structure under test:
        //
        //   Root (dcp.getRoot())
        //   ├─ Propaedeutics (auto-created)
        //   ├─ extraGroup (EXTRA_CURRICULAR auto-created)
        //   │  ├─ Enrolment courseA
        //   │  ├─ Enrolment courseB
        //   │  └─ childGroup -> childCourseGroup ("Child Group")
        //   └─ (other default NoCourseGroup groups, unused here)
        //
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EvaluationSeasonTest.initEvaluationSeasons();
            DegreeTest.initDegree();
            CompetenceCourseTest.initCompetenceCourse();
            StudentTest.initRegistrationConfigEntities();

            executionYear = ExecutionYear.findCurrent(null);
            executionInterval = executionYear.getFirstExecutionPeriod();

            final Degree degree = Degree.find(DEGREE_A_CODE);
            dcp = new DegreeCurricularPlan(degree, DCP_NAME, AcademicPeriod.THREE_YEAR, executionYear);
            dcp.createExecutionDegree(executionYear);

            final CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
            final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

            courseA = new CurricularCourse();
            courseA.setCompetenceCourse(CompetenceCourse.find(COURSE_A_CODE));
            new Context(dcp.getRoot(), courseA, semesterPeriod, executionInterval, null);

            courseB = new CurricularCourse();
            courseB.setCompetenceCourse(CompetenceCourse.find(COURSE_B_CODE));
            new Context(dcp.getRoot(), courseB, semesterPeriod, executionInterval, null);

            nonEnrolledCourse = new CurricularCourse();
            nonEnrolledCourse.setCompetenceCourse(CompetenceCourse.find(COURSE_A_CODE));

            final Student student = StudentTest.createStudent("No Course Group Test Student", STUDENT_USERNAME);
            scp = StudentTest.createRegistration(student, dcp, executionYear).getLastStudentCurricularPlan();

            // The extracurricular group is auto-created with the SCP root
            extraGroup = scp.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR);

            // Two enrolments directly under the extracurricular group
            new Enrolment(scp, extraGroup, courseA, executionInterval, EnrollmentCondition.FINAL, STUDENT_USERNAME);
            new Enrolment(scp, extraGroup, courseB, executionInterval, EnrollmentCondition.FINAL, STUDENT_USERNAME);

            // Child CurriculumGroup under the group
            final CourseGroup childCourseGroup =
                    new CourseGroup(dcp.getRoot(), "Child Group", "Child Group", executionInterval, null);
            childGroup = new CurriculumGroup(extraGroup, childCourseGroup);

            return null;
        });
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_getName() {
        final LocalizedString name = extraGroup.getName();
        assertNotNull(name);

        final String pt = name.getContent(LocaleUtils.PT);
        final String en = name.getContent(LocaleUtils.EN);
        assertNotNull(pt);
        assertNotNull(en);
        assertFalse(pt.isEmpty());
        assertFalse(en.isEmpty());

        assertEquals(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR.getLocalizedName(LocaleUtils.PT), pt);
        assertEquals(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR.getLocalizedName(LocaleUtils.EN), en);
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_hasDegreeModule() {
        assertTrue(extraGroup.hasDegreeModule(courseA));
        assertTrue(extraGroup.hasDegreeModule(courseB));
        assertFalse(extraGroup.hasDegreeModule(nonEnrolledCourse));
        assertFalse(extraGroup.hasDegreeModule(null));
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_hasCourseGroup() {
        assertFalse(extraGroup.hasCourseGroup(dcp.getRoot()));
        assertFalse(extraGroup.hasCourseGroup(null));
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_findCurriculumGroupFor() {
        assertNull(extraGroup.findCurriculumGroupFor(dcp.getRoot()));
        assertNull(extraGroup.findCurriculumGroupFor(null));
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_searchChildOrderForChild() {
        // Non-child groups have no position in the ordered child list -> -1
        final int orderNonChild = extraGroup.searchChildOrderForChild(scp.getRoot(), executionInterval);
        assertEquals(-1, orderNonChild);

        // The group itself is not its own child -> -1
        final int orderSameGroup = extraGroup.searchChildOrderForChild(extraGroup, executionInterval);
        assertEquals(-1, orderSameGroup);

        // Real child group -> non-negative position
        final int orderChild = extraGroup.searchChildOrderForChild(childGroup, executionInterval);
        assertTrue(orderChild >= 0);
    }
}
