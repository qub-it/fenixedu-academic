package org.fenixedu.academic.domain.studentCurriculum;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_B_CODE;
import static org.fenixedu.academic.domain.DegreeTest.DEGREE_A_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CompetenceCourseTest;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
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
    private static final String STANDALONE_STUDENT_USERNAME = "standalone.group.test.student";

    private static StudentCurricularPlan scp;
    private static DegreeCurricularPlan dcp;
    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionInterval secondSemester;
    private static CurricularCourse courseA;
    private static CurricularCourse courseB;
    private static CurricularCourse nonEnrolledCourse;
    private static NoCourseGroupCurriculumGroup extraGroup;
    private static CurriculumGroup childCurriculumGroup;
    private static CourseGroup childCourseGroup;

    private static StandaloneCurriculumGroup standaloneGroup;
    private static StandaloneCurriculumGroup emptyStandaloneGroup;
    private static Enrolment nonApprovedEnrolment;

    @BeforeClass
    public static void init() {
        // SCP structure under test:
        //
        //   Root (dcp.getRoot())
        //   ├─ Propaedeutics (auto-created)
        //   ├─ extraGroup (EXTRA_CURRICULAR auto-created)
        //   │  ├─ Enrolment courseA
        //   │  ├─ Enrolment courseB
        //   │  └─ childCurriculumGroup -> childCourseGroup ("Child Group")
        //   └─ (other default NoCourseGroup groups, unused here)
        //
        // Structure of the enrolment tree under test (independent of the SCP root tree above):
        //
        //   standaloneScp                           emptyStandaloneScp
        //   └─ standaloneGroup (STANDALONE)         └─ emptyStandaloneGroup (STANDALONE)
        //      ├─ Enrolment standaloneCourseA       └─ (no enrolments)
        //      │   (SEMESTER competence course, approved)
        //      ├─ Enrolment standaloneCourseB
        //      │   (annual competence course, approved)
        //      └─ Enrolment courseCCurricular (enrolled, NOT approved)
        //
        FenixFramework.getTransactionManager().withTransaction(() -> {
            EvaluationSeasonTest.initEvaluationSeasons();
            DegreeTest.initDegree();
            CompetenceCourseTest.initCompetenceCourse();
            StudentTest.initRegistrationConfigEntities();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            secondSemester = executionYear.getLastExecutionPeriod();

            final Degree degree = Degree.find(DEGREE_A_CODE);
            dcp = new DegreeCurricularPlan(degree, "No Course Group DCP", AcademicPeriod.THREE_YEAR, executionYear);
            dcp.createExecutionDegree(executionYear);

            final CurricularPeriod yearPeriod = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
            final CurricularPeriod semesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, yearPeriod);

            courseA = new CurricularCourse();
            courseA.setCompetenceCourse(CompetenceCourse.find(COURSE_A_CODE));
            new Context(dcp.getRoot(), courseA, semesterPeriod, firstSemester, null);

            courseB = new CurricularCourse();
            courseB.setCompetenceCourse(CompetenceCourse.find(COURSE_B_CODE));
            new Context(dcp.getRoot(), courseB, semesterPeriod, firstSemester, null);

            nonEnrolledCourse = new CurricularCourse();
            nonEnrolledCourse.setCompetenceCourse(CompetenceCourse.find(COURSE_A_CODE));

            final Student student = StudentTest.createStudent("No Course Group Test Student", STUDENT_USERNAME);
            scp = StudentTest.createRegistration(student, dcp, executionYear).getLastStudentCurricularPlan();

            // The extracurricular group is auto-created with the SCP root
            extraGroup = scp.getNoCourseGroupCurriculumGroup(NoCourseGroupCurriculumGroupType.EXTRA_CURRICULAR);

            // Two enrolments directly under the extracurricular group
            new Enrolment(scp, extraGroup, courseA, firstSemester, EnrollmentCondition.FINAL, STUDENT_USERNAME);
            new Enrolment(scp, extraGroup, courseB, firstSemester, EnrollmentCondition.FINAL, STUDENT_USERNAME);

            // Child CurriculumGroup under the group
            childCourseGroup = new CourseGroup(dcp.getRoot(), "Child Group", "Child Group", firstSemester, null);
            childCurriculumGroup = new CurriculumGroup(extraGroup, childCourseGroup);

            // COURSE_A_CODE's CompetenceCourse is SEMESTER (only valid in firstSemester)
            final CurricularCourse standaloneCourseA = createCourse(dcp, COURSE_A_CODE, semesterPeriod);
            // COURSE_B_CODE's CompetenceCourse is AcademicPeriod.YEAR (annual), valid in both semesters
            final CurricularCourse standaloneCourseB = createCourse(dcp, COURSE_B_CODE, semesterPeriod);

            final StudentCurricularPlan standaloneScp = createRegistration(dcp, STANDALONE_STUDENT_USERNAME, executionYear);
            final StudentCurricularPlan emptyStandaloneScp =
                    createRegistration(dcp, "standalone.group.empty.student", executionYear);

            standaloneGroup =
                    (StandaloneCurriculumGroup) NoCourseGroupCurriculumGroup.create(NoCourseGroupCurriculumGroupType.STANDALONE,
                            standaloneScp.getRoot());
            emptyStandaloneGroup =
                    (StandaloneCurriculumGroup) NoCourseGroupCurriculumGroup.create(NoCourseGroupCurriculumGroupType.STANDALONE,
                            emptyStandaloneScp.getRoot());

            final Enrolment standaloneCourseAEnrolment =
                    new Enrolment(standaloneScp, standaloneGroup, standaloneCourseA, firstSemester, EnrollmentCondition.FINAL,
                            STANDALONE_STUDENT_USERNAME);
            approve(standaloneCourseAEnrolment);

            final Enrolment standaloneCourseBEnrolment =
                    new Enrolment(standaloneScp, standaloneGroup, standaloneCourseB, firstSemester, EnrollmentCondition.FINAL,
                            STANDALONE_STUDENT_USERNAME);
            approve(standaloneCourseBEnrolment);

            // "CC" CompetenceCourse must be created before use since it's not in CompetenceCourseTest.initCompetenceCourse()
            CompetenceCourseTest.createCompetenceCourse("Course C", "CC", new BigDecimal("6.0"), AcademicPeriod.SEMESTER,
                    firstSemester, Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow());
            final CurricularCourse courseCCurricular = createCourse(dcp, "CC", semesterPeriod);

            nonApprovedEnrolment =
                    new Enrolment(standaloneScp, standaloneGroup, courseCCurricular, firstSemester, EnrollmentCondition.FINAL,
                            STANDALONE_STUDENT_USERNAME);

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
        assertTrue(extraGroup.hasCourseGroup(childCourseGroup));
        assertFalse(extraGroup.hasCourseGroup(dcp.getRoot()));
        assertFalse(extraGroup.hasCourseGroup(null));
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_findCurriculumGroupFor() {
        assertEquals(childCurriculumGroup, extraGroup.findCurriculumGroupFor(childCourseGroup));
        assertNull(extraGroup.findCurriculumGroupFor(dcp.getRoot()));
        assertNull(extraGroup.findCurriculumGroupFor(null));
    }

    @Test
    public void testNoCourseGroupCurriculumGroup_searchChildOrderForChild() {
        // Non-child groups have no position in the ordered child list -> -1
        final int orderNonChild = extraGroup.searchChildOrderForChild(scp.getRoot(), firstSemester);
        assertEquals(-1, orderNonChild);

        // The group itself is not its own child -> -1
        final int orderSameGroup = extraGroup.searchChildOrderForChild(extraGroup, firstSemester);
        assertEquals(-1, orderSameGroup);

        // Real child group -> non-negative position
        final int orderChild = extraGroup.searchChildOrderForChild(childCurriculumGroup, firstSemester);
        assertTrue(orderChild >= 0);
    }

    @Test
    public void testStandaloneCurriculumGroup_getNumberOfAllApprovedEnrolments() {
        // standaloneGroup has 3 enrolments: CA (approved), CB annual (approved), CC (enroled, not approved)
        assertFalse(nonApprovedEnrolment.isApproved());
        assertEquals(2, standaloneGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(1, standaloneGroup.getNumberOfAllApprovedEnrolments(secondSemester));
        assertEquals(0, emptyStandaloneGroup.getNumberOfAllApprovedEnrolments(firstSemester));
        assertEquals(0, emptyStandaloneGroup.getNumberOfAllApprovedEnrolments(secondSemester));
    }

    private static void approve(final Enrolment enrolment) {
        final GradeScale gradeScale = enrolment.getStudentCurricularPlan().getDegree().getNumericGradeScale();
        gradeScale.setMinimumApprovedGrade(new BigDecimal("10"));
        gradeScale.setMaximumApprovedGrade(new BigDecimal("20"));
        final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
        evaluation.setGrade(Grade.createGrade("14", gradeScale));
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
    }

    private static StudentCurricularPlan createRegistration(final DegreeCurricularPlan dcp, final String username,
            final ExecutionYear executionYear) {
        final Student student = StudentTest.createStudent("Standalone Curriculum Group Test Student", username);
        return StudentTest.createRegistration(student, dcp, executionYear).getLastStudentCurricularPlan();
    }

    private static CurricularCourse createCourse(final DegreeCurricularPlan dcp, final String code,
            final CurricularPeriod semesterPeriod) {
        final CurricularCourse curricularCourse = new CurricularCourse();
        curricularCourse.setCompetenceCourse(CompetenceCourse.find(code));
        curricularCourse.setName("Course " + code);
        new Context(dcp.getRoot(), curricularCourse, semesterPeriod, firstSemester, null);
        return curricularCourse;
    }
}