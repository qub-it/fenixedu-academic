package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.Interval;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class TeacherTest {

    private static final String TEACHER_USERNAME = "0teacher.test";
    private static final String TEACHER2_USERNAME = "0teacher.test2";

    private static Teacher teacher, teacher2;
    private static ExecutionCourse executionCourseResponsibleS1, executionCourseNotResponsibleS2,
            executionCourseResponsibleNextYearS1, unknownCourse;
    private static ExecutionInterval firstSemester, secondSemester;
    private static ExecutionYear executionYear, nextYear;
    private static CurricularCourse curricularCourse;
    private static Professorship professorship1, professorship2, professorship3;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initData();
            return null;
        });
    }

    public static void initData() {
        DegreeCurricularPlanTest.initDegreeCurricularPlan();

        executionYear = ExecutionYear.findCurrent(null);
        firstSemester = executionYear.getFirstExecutionPeriod();
        secondSemester = executionYear.getLastExecutionPeriod();
        nextYear = (ExecutionYear) executionYear.getNext();

        Degree degree = Degree.find(DegreeTest.DEGREE_A_CODE);
        DegreeCurricularPlan dcp =
                degree.getDegreeCurricularPlansSet().stream().filter(p -> p.getName().equals(DCP_NAME_V1)).findAny()
                        .orElseThrow();
        curricularCourse = dcp.getCurricularCourseByCode(CompetenceCourseTest.COURSE_A_CODE);

        executionCourseResponsibleS1 = new ExecutionCourse("EC1", "EC1", firstSemester);
        executionCourseResponsibleS1.addAssociatedCurricularCourses(curricularCourse);

        executionCourseNotResponsibleS2 = new ExecutionCourse("EC2", "EC2", secondSemester);
        executionCourseNotResponsibleS2.addAssociatedCurricularCourses(curricularCourse);

        executionCourseResponsibleNextYearS1 = new ExecutionCourse("EC3", "EC3", nextYear.getFirstExecutionPeriod());
        executionCourseResponsibleNextYearS1.addAssociatedCurricularCourses(curricularCourse);

        unknownCourse = new ExecutionCourse("Unknown", "XX", firstSemester);

        teacher = createTeacher(TEACHER_USERNAME, "T001");
        teacher2 = createTeacher(TEACHER2_USERNAME, "T002");

        Authenticate.mock(User.findByUsername(UserUtil.ADMIN_USERNAME), "none");
        try {
            professorship1 = Professorship.create(true, executionCourseResponsibleS1, teacher.getPerson());
            professorship2 = Professorship.create(false, executionCourseNotResponsibleS2, teacher.getPerson());
            professorship3 = Professorship.create(true, executionCourseResponsibleNextYearS1, teacher.getPerson());
        } finally {
            Authenticate.unmock();
        }
    }

    private static Teacher createTeacher(String username, String number) {
        UserProfile userProfile = new UserProfile(username, "", username, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        Person person = new Person(userProfile);
        Teacher teacher = new Teacher(person);
        teacher.setNumber(number);
        return teacher;
    }

    private TeacherAuthorization createAuthorization(Teacher teacher, ExecutionInterval interval, TeacherCategory category) {
        Unit unit = Unit.findInternalUnitByAcronymPath("QS").orElseThrow();
        Authenticate.mock(User.findByUsername(UserUtil.ADMIN_USERNAME), "none");
        try {
            return TeacherAuthorization.createOrUpdate(teacher, unit, interval, category, true, 0d, 100d);
        } finally {
            Authenticate.unmock();
        }
    }

    @Test
    public void testTeacher_TEACHER_COMPARATOR_BY_CATEGORY_AND_NUMBER() {
        TeacherCategory highWeightCategory =
                new TeacherCategory("CAT_HIGH", new LocalizedString(Locale.ENGLISH, "High Category"), 100);
        TeacherCategory lowWeightCategory =
                new TeacherCategory("CAT_LOW", new LocalizedString(Locale.ENGLISH, "Low Category"), 10);

        Teacher teacherNoCategory = createTeacher("0teacher.nocategory", "T003");
        Teacher teacherNoCategory2 = createTeacher("teacher.anothernocategory", "T005");
        Teacher teacherSameCategory = createTeacher("teacher.samecategory", "T004");
        Unit unit = Unit.findInternalUnitByAcronymPath("QS").orElseThrow();

        Authenticate.mock(User.findByUsername(UserUtil.ADMIN_USERNAME), "none");
        try {
            TeacherAuthorization.createOrUpdate(teacher, unit, firstSemester, highWeightCategory, true, 0d, 100d);
            TeacherAuthorization.createOrUpdate(teacher2, unit, firstSemester, lowWeightCategory, true, 0d, 100d);
            TeacherAuthorization.createOrUpdate(teacherSameCategory, unit, firstSemester, lowWeightCategory, true, 0d, 100d);
        } finally {
            Authenticate.unmock();
        }

        List<Teacher> teachers = new ArrayList<>();
        teachers.add(teacherNoCategory2);
        teachers.add(teacherNoCategory);
        teachers.add(teacherSameCategory);
        teachers.add(teacher2);
        teachers.add(teacher);

        teachers.sort(Teacher.TEACHER_COMPARATOR_BY_CATEGORY_AND_NUMBER);

        assertEquals(teacher, teachers.get(0));
        assertEquals(teacher2, teachers.get(1));
        assertEquals(teacherSameCategory, teachers.get(2));
        assertEquals(teacherNoCategory, teachers.get(3));
        assertEquals(teacherNoCategory2, teachers.get(4));
    }

    @Test
    public void testTeacher_getAllLecturedExecutionCourses() {
        List<ExecutionCourse> allCourses = teacher.getAllLecturedExecutionCourses();
        assertEquals(3, allCourses.size());
        assertTrue(allCourses.contains(executionCourseResponsibleS1));
        assertTrue(allCourses.contains(executionCourseNotResponsibleS2));
        assertTrue(allCourses.contains(executionCourseResponsibleNextYearS1));
    }

    @Test
    public void testTeacher_getProfessorshipByExecutionCourse() {
        assertEquals(professorship1, teacher.getProfessorshipByExecutionCourse(executionCourseResponsibleS1));
        assertEquals(professorship2, teacher.getProfessorshipByExecutionCourse(executionCourseNotResponsibleS2));
        assertEquals(professorship3, teacher.getProfessorshipByExecutionCourse(executionCourseResponsibleNextYearS1));

        assertNull(teacher.getProfessorshipByExecutionCourse(unknownCourse));
        assertNull(teacher.getProfessorshipByExecutionCourse(null));
    }

    @Test
    public void testTeacher_findByUsername() {
        Optional<Teacher> result = Teacher.findByUsername(TEACHER_USERNAME);
        assertTrue(result.isPresent());
        assertEquals(teacher, result.get());

        result = Teacher.findByUsername("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    public void testTeacher_getCategory() {
        Teacher testTeacher = createTeacher("0teacher.getcat", "TGC");

        TeacherCategory highCategory =
                new TeacherCategory("GET_CATEGORY_HIGH", new LocalizedString(Locale.ENGLISH, "High Category"), 100);
        TeacherCategory lowCategory =
                new TeacherCategory("GET_CATEGORY_LOW", new LocalizedString(Locale.ENGLISH, "Low Category"), 10);

        createAuthorization(testTeacher, firstSemester, highCategory);
        createAuthorization(testTeacher, nextYear.getFirstExecutionPeriod(), lowCategory);

        assertEquals(highCategory, testTeacher.getCategory(firstSemester).orElse(null));
        assertTrue(testTeacher.getCategory(secondSemester).isEmpty());
        assertEquals(lowCategory, testTeacher.getCategory(nextYear.getFirstExecutionPeriod()).orElse(null));
        assertEquals(highCategory, testTeacher.getCategory());
    }

    @Test
    public void testTeacher_getLastCategory() {
        Teacher testTeacher = createTeacher("0teacher.getlastcat", "TGL");

        TeacherCategory highCategory =
                new TeacherCategory("GET_LAST_CATEGORY_HIGH", new LocalizedString(Locale.ENGLISH, "High Category"), 100);
        TeacherCategory lowCategory =
                new TeacherCategory("GET_LAST_CATEGORY_LOW", new LocalizedString(Locale.ENGLISH, "Low Category"), 10);

        createAuthorization(testTeacher, firstSemester, highCategory);
        createAuthorization(testTeacher, nextYear.getFirstExecutionPeriod(), lowCategory);

        assertEquals(highCategory, testTeacher.getLastCategory(firstSemester).orElse(null));
        assertEquals(highCategory, testTeacher.getLastCategory(secondSemester).orElse(null));
        assertEquals(lowCategory, testTeacher.getLastCategory(nextYear.getFirstExecutionPeriod()).orElse(null));
        assertEquals(highCategory, testTeacher.getLastCategory());
    }

    @Test
    public void testTeacher_getTeacherAuthorization() {
        Teacher testTeacher = createTeacher("0teacher.getauth", "TGA");

        TeacherCategory category =
                new TeacherCategory("GET_TEACHER_AUTH", new LocalizedString(Locale.ENGLISH, "Test Category"), 50);

        TeacherAuthorization auth = createAuthorization(testTeacher, firstSemester, category);

        assertEquals(auth, testTeacher.getTeacherAuthorization(firstSemester).orElse(null));
        assertTrue(testTeacher.getTeacherAuthorization(secondSemester).isEmpty());
        assertTrue(testTeacher.getTeacherAuthorization(nextYear.getFirstExecutionPeriod()).isEmpty());
        assertEquals(auth, testTeacher.getTeacherAuthorization().orElse(null));

        auth.setExecutionSemester(secondSemester);

        assertTrue(testTeacher.getTeacherAuthorization(firstSemester).isEmpty());
        assertEquals(auth, testTeacher.getTeacherAuthorization(secondSemester).orElse(null));
        assertTrue(testTeacher.getTeacherAuthorization(nextYear.getFirstExecutionPeriod()).isEmpty());
        assertTrue(testTeacher.getTeacherAuthorization()
                .isEmpty()); // Because this looks for current ExecutionInterval which is firstSemester
    }

    @Test
    public void testTeacher_getLatestTeacherAuthorizationInInterval() {
        Teacher testTeacher = createTeacher("0teacher.latestauth", "TLT");

        TeacherCategory category =
                new TeacherCategory("GET_LATEST_AUTH", new LocalizedString(Locale.ENGLISH, "Test Category"), 50);

        TeacherAuthorization auth = createAuthorization(testTeacher, firstSemester, category);

        AcademicInterval firstSemesterInterval = firstSemester.getAcademicInterval();
        assertEquals(auth, testTeacher.getLatestTeacherAuthorizationInInterval(firstSemesterInterval.toInterval()).orElse(null));

        AcademicInterval nextYearInterval = nextYear.getFirstExecutionPeriod().getAcademicInterval();
        assertTrue(testTeacher.getLatestTeacherAuthorizationInInterval(nextYearInterval.toInterval()).isEmpty());

        Interval dateInsideFirstSemester = new Interval(firstSemesterInterval.getStart().getMillis(),
                firstSemesterInterval.getStart().plusDays(1).getMillis());
        assertEquals(auth, testTeacher.getLatestTeacherAuthorizationInInterval(dateInsideFirstSemester).orElse(null));

        Interval dateOutsideSemesters = new Interval(firstSemesterInterval.getEnd().plusYears(2).getMillis(),
                firstSemesterInterval.getEnd().plusYears(2).plusDays(1).getMillis());
        assertTrue(testTeacher.getLatestTeacherAuthorizationInInterval(dateOutsideSemesters).isEmpty());
    }
}
