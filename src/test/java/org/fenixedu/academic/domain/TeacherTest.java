package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.DCP_NAME_V1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
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
}
