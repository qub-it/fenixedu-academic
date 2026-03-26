package org.fenixedu.academic.domain.accessControl;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularPeriod.DegreeCurricularPlanDurationTest;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSES_UNIT_PATH;
import static org.fenixedu.academic.domain.CompetenceCourseTest.createCompetenceCourse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class StudentGroupTest {

    private static final String STUDENT_A_USERNAME = "studentgroup.test.studentA";

    private static final String STUDENT_B_USERNAME = "studentgroup.test.studentB";

    private static final String STUDENT_C_USERNAME = "studentgroup.test.studentC";

    private static final String STUDENT_D_USERNAME = "studentgroup.test.studentD";

    private static final String NON_STUDENT_USERNAME = "studentgroup.test.nonstudent";

    private static StudentGroup studentGroup;

    private static User studentAUser;

    private static User studentBUser;

    private static User studentCUser;

    private static User studentDUser;

    private static User nonStudentUser;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initTestData();
            return null;
        });
    }

    private static void initTestData() {
        EnrolmentTest.initEnrolments();

        Degree degree = Degree.find(DegreeTest.DEGREE_A_CODE);
        ExecutionYear executionYear = ExecutionYear.findCurrent(degree.getCalendar());

        studentAUser = createUser(STUDENT_A_USERNAME, "Student A");
        studentBUser = createUser(STUDENT_B_USERNAME, "Student B");
        studentCUser = createUser(STUDENT_C_USERNAME, "Student C");
        studentDUser = createUser(STUDENT_D_USERNAME, "Student D");
        nonStudentUser = createUser(NON_STUDENT_USERNAME, "Non Student");

        Student studentA = new Student(studentAUser.getPerson());
        Student studentB = new Student(studentBUser.getPerson());
        Student studentC = new Student(studentCUser.getPerson());
        Student studentD = new Student(studentDUser.getPerson());

        DegreeCurricularPlan dcp =
                new DegreeCurricularPlan(degree, "dcp for student group test", AcademicPeriod.THREE_YEAR, executionYear);
        DegreeCurricularPlanDurationTest.populateCurricularPeriodStructure(dcp);
        dcp.createExecutionDegree(executionYear);
        ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

        Registration registrationA = StudentTest.createRegistration(studentA, dcp, executionYear);
        Registration registrationB = StudentTest.createRegistration(studentB, dcp, executionYear);
        Registration registrationC = StudentTest.createRegistration(studentC, dcp, executionYear);
        Registration registrationD = StudentTest.createRegistration(studentD, dcp, executionYear);

        RegistrationDataByExecutionYear dataA =
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registrationA, executionYear);
        dataA.setActive(true);

        RegistrationDataByExecutionYear dataB =
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registrationB, executionYear);
        dataB.setActive(false);

        createEnrolment(registrationC, executionInterval, false);
        RegistrationDataByExecutionYear dataC =
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registrationC, executionYear);
        dataC.setActive(false);

        createEnrolment(registrationD, executionInterval, true);
        RegistrationDataByExecutionYear dataD =
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registrationD, executionYear);
        dataD.setActive(true);

        studentGroup = StudentGroup.get(executionYear);
    }

    private static User createUser(String username, String name) {
        UserProfile profile = new UserProfile(name, "", name, username + "@fenixedu.com", Locale.getDefault());
        User user = new User(username, profile);
        new Person(profile);
        return user;
    }

    private static void createEnrolment(Registration registration, ExecutionInterval executionInterval, boolean annulled) {
        final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(COURSES_UNIT_PATH).orElseThrow();
        final String uuid = UUID.randomUUID().toString();
        final AcademicPeriod academicPeriod = executionInterval.getAcademicPeriod();
        final CompetenceCourse competenceCourse =
                createCompetenceCourse(uuid, uuid, BigDecimal.TEN, academicPeriod, executionInterval, coursesUnit);

        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        final DegreeCurricularPlan dcp = scp.getDegreeCurricularPlan();
        final CurricularPeriod curricularPeriod =
                dcp.getCurricularPeriodFor(1, executionInterval.getChildOrder(), academicPeriod);
        final CurricularCourse curricularCourse =
                new CurricularCourse(null, competenceCourse, dcp.getRoot(), curricularPeriod, executionInterval, null);
        final Context context = curricularCourse.getParentContextsSet().iterator().next();

        EnrolmentTest.createEnrolment(scp, executionInterval, context, UserUtil.ADMIN_USERNAME);
        Enrolment enrolment = scp.getEnrolments(curricularCourse).iterator().next();
        if (annulled) {
            enrolment.annul();
        }
    }

    @Test
    public void isMember_nullUser() {
        assertFalse(studentGroup.isMember(null));
    }

    @Test
    public void getMembers_returnsOnlyActiveStudents() {
        Set<User> members = studentGroup.getMembers().collect(Collectors.toSet());

        assertTrue(members.contains(studentAUser));
        assertFalse(members.contains(studentBUser));
        assertTrue(members.contains(studentDUser));
        assertFalse(members.contains(studentCUser));
        assertFalse(members.contains(nonStudentUser));
    }

    @Test
    public void consistency_isMemberReturnsTrueForAllGetMembers() {
        Set<User> members = studentGroup.getMembers().collect(Collectors.toSet());

        for (User member : members) {
            assertTrue("isMember should return true for user in getMembers: " + member.getUsername(),
                    studentGroup.isMember(member));
        }
    }

}
