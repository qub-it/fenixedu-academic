package org.fenixedu.academic.domain.accessControl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.security.Authenticate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class ProfessorshipsGroupTest {

    private static ExecutionInterval currentSemester, otherSemesterOfCurrentYear, semesterOfAnotherYear;
    private static Teacher teacherOfCurrentSemester, teacherOfOtherSemesterOfCurrentYear, teacherOfAnotherYear, teacherWithoutProfessorship, teacherWithoutUser;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initData();
            return null;
        });
    }

    private static void initData() {
        ExecutionIntervalTest.initRootCalendarAndExecutionYears();

        ExecutionYear currentExecutionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        currentSemester = currentExecutionYear.getFirstExecutionPeriod();
        otherSemesterOfCurrentYear = currentExecutionYear.getLastExecutionPeriod();
        semesterOfAnotherYear = ExecutionYear.readExecutionYearByName("2021/2022").getFirstExecutionPeriod();
        currentSemester.changeState(PeriodState.CURRENT);

        teacherOfCurrentSemester = createTeacher("0professorshipgroup.currentSemester", "PG01");
        teacherOfOtherSemesterOfCurrentYear = createTeacher("0professorshipgroup.otherSemester", "PG02");
        teacherOfAnotherYear = createTeacher("0professorshipgroup.anotherYear", "PG03");
        teacherWithoutProfessorship = createTeacher("0professorshipgroup.noProfessorship", "PG04");
        teacherWithoutUser = createTeacherWithoutUser("PG05");

        Authenticate.mock(userOf(teacherOfCurrentSemester), "none");
        try {
            createProfessorship(teacherOfCurrentSemester, currentSemester, "PGCurrentSemester");
            createProfessorship(teacherOfOtherSemesterOfCurrentYear, otherSemesterOfCurrentYear, "PGOtherSemester");
            createProfessorship(teacherOfAnotherYear, semesterOfAnotherYear, "PGAnotherYear");
            createProfessorship(teacherWithoutUser, currentSemester, "PGWithoutUser");
        } finally {
            Authenticate.unmock();
        }
    }

    private static Teacher createTeacher(String username, String number) {
        UserProfile userProfile = new UserProfile(username, "", username, username + "@fenixedu.com", Locale.getDefault());
        new User(username, userProfile);
        Teacher teacher = new Teacher(new Person(userProfile));
        teacher.setNumber(number);
        return teacher;
    }

    private static Teacher createTeacherWithoutUser(String number) {
        UserProfile userProfile = new UserProfile("Professor without user", "", "professorshipgroup.nouser",
                "professorshipgroup.nouser@fenixedu.com", Locale.getDefault());
        Teacher teacher = new Teacher(new Person(userProfile));
        teacher.setNumber(number);
        return teacher;
    }

    private static void createProfessorship(Teacher teacher, ExecutionInterval executionInterval, String executionCourseName) {
        ExecutionCourse executionCourse = new ExecutionCourse(executionCourseName, executionCourseName, executionInterval);
        Professorship.create(true, executionCourse, teacher.getPerson());
    }

    private static User userOf(Teacher teacher) {
        return teacher.getPerson().getUser();
    }

    @Test
    public void testProfessorshipsGroup_getMembers() {
        // the semester group only contains the professors of the current semester
        Set<User> semesterMembers =
                ProfessorshipsGroup.get(false, AcademicPeriod.SEMESTER).getMembers().collect(Collectors.toSet());
        assertTrue(semesterMembers.contains(userOf(teacherOfCurrentSemester)));
        assertFalse(semesterMembers.contains(userOf(teacherOfOtherSemesterOfCurrentYear)));
        assertFalse(semesterMembers.contains(userOf(teacherOfAnotherYear)));
        assertFalse(semesterMembers.contains(userOf(teacherWithoutProfessorship)));

        // a professor that is not associated with any user is not a member
        assertFalse(semesterMembers.stream().anyMatch(user -> user.getPerson() == teacherWithoutUser.getPerson()));

        // the year group contains the professors of both semesters of the current year
        Set<User> yearMembers = ProfessorshipsGroup.get(false, AcademicPeriod.YEAR).getMembers().collect(Collectors.toSet());
        assertTrue(yearMembers.contains(userOf(teacherOfCurrentSemester)));
        assertTrue(yearMembers.contains(userOf(teacherOfOtherSemesterOfCurrentYear)));
        assertFalse(yearMembers.contains(userOf(teacherOfAnotherYear)));
        assertFalse(yearMembers.contains(userOf(teacherWithoutProfessorship)));

        // a professor that is not associated with any user is not a member
        assertFalse(yearMembers.stream().anyMatch(user -> user.getPerson() == teacherWithoutUser.getPerson()));
    }

    @Test
    public void testProfessorshipsGroup_isMember() {
        ProfessorshipsGroup semesterGroup = ProfessorshipsGroup.get(false, AcademicPeriod.SEMESTER);
        ProfessorshipsGroup yearGroup = ProfessorshipsGroup.get(false, AcademicPeriod.YEAR);

        // a null user is never a member
        assertFalse(semesterGroup.isMember(null));
        assertFalse(yearGroup.isMember(null));

        // a professor of the current semester is a member of the semester group and of the year group
        assertTrue(semesterGroup.isMember(userOf(teacherOfCurrentSemester)));
        assertTrue(yearGroup.isMember(userOf(teacherOfCurrentSemester)));

        // a professor of another semester of the current year is not a member of the semester group, but is a
        // member of the year group because the current academic year contains both of its semesters
        assertFalse(semesterGroup.isMember(userOf(teacherOfOtherSemesterOfCurrentYear)));
        assertTrue(yearGroup.isMember(userOf(teacherOfOtherSemesterOfCurrentYear)));

        // a professor of another year is not a member of any group
        assertFalse(semesterGroup.isMember(userOf(teacherOfAnotherYear)));
        assertFalse(yearGroup.isMember(userOf(teacherOfAnotherYear)));

        // a teacher without professorships is not a member of any group
        assertFalse(semesterGroup.isMember(userOf(teacherWithoutProfessorship)));
        assertFalse(yearGroup.isMember(userOf(teacherWithoutProfessorship)));
    }
}
