package org.fenixedu.academic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
public class TeacherAuthorizationTest {

    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static ExecutionYear nextYear;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initData();
            return null;
        });
    }

    public static void initData() {
        ExecutionIntervalTest.initRootCalendarAndExecutionYears();
        OrganizationalStructureTest.initTypes();
        OrganizationalStructureTest.initUnits();

        executionYear = ExecutionYear.findCurrent(null);
        firstSemester = executionYear.getFirstExecutionPeriod();
        nextYear = (ExecutionYear) executionYear.getNext();
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
        return TeacherAuthorization.createOrUpdate(teacher, unit, interval, category, true, 0d, 100d);
    }

    @Test
    public void testTeacherAuthorization_compareTo() {
        TeacherCategory highWeightCategory =
                new TeacherCategory("COMPARE_HIGH", new LocalizedString(Locale.ENGLISH, "High Category"), 100);
        TeacherCategory lowWeightCategory =
                new TeacherCategory("COMPARE_LOW", new LocalizedString(Locale.ENGLISH, "Low Category"), 10);

        Teacher firstSemesterHighTeacher = createTeacher("0authorization.firstSemesterHigh", "A001");
        Teacher firstSemesterLowTeacher = createTeacher("0authorization.firstSemesterLow", "A002");
        Teacher nextYearLowTeacher = createTeacher("0authorization.nextYearLow", "A003");
        Teacher nextYearHighTeacher = createTeacher("0authorization.nextYearHigh", "A005");
        Teacher nextYearLowSecondTeacher = createTeacher("0authorization.nextYearLowSecond", "A004");

        TeacherAuthorization firstSemesterHighAuthorization =
                createAuthorization(firstSemesterHighTeacher, firstSemester, highWeightCategory);
        TeacherAuthorization firstSemesterLowAuthorization =
                createAuthorization(firstSemesterLowTeacher, firstSemester, lowWeightCategory);
        TeacherAuthorization nextYearLowAuthorization =
                createAuthorization(nextYearLowTeacher, nextYear.getFirstExecutionPeriod(), lowWeightCategory);
        TeacherAuthorization nextYearHighAuthorization =
                createAuthorization(nextYearHighTeacher, nextYear.getFirstExecutionPeriod(), highWeightCategory);
        TeacherAuthorization nextYearLowSecondAuthorization =
                createAuthorization(nextYearLowSecondTeacher, nextYear.getFirstExecutionPeriod(), lowWeightCategory);

        // comparing an authorization with itself yields zero
        assertEquals(0, firstSemesterHighAuthorization.compareTo(firstSemesterHighAuthorization));
        assertEquals(0, nextYearLowSecondAuthorization.compareTo(nextYearLowSecondAuthorization));

        // LowerCategory > HigherCategory
        assertTrue(firstSemesterHighAuthorization.compareTo(firstSemesterLowAuthorization) < 0);

        // interval wins over category
        assertTrue(firstSemesterLowAuthorization.compareTo(nextYearHighAuthorization) < 0);

        // same interval and category falls back to externalId, keeping the comparison asymmetric
        assertTrue(nextYearLowAuthorization.compareTo(nextYearLowSecondAuthorization) != 0);
    }
}