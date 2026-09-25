package org.fenixedu.academic.domain.studentCurriculum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

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
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.OrganizationalStructureTest;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.EvenOddRule;
import org.fenixedu.academic.domain.curricularRules.ICurricularRule;
import org.fenixedu.academic.domain.curriculum.EnrollmentCondition;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.util.UserUtil;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CurriculumModuleTest {

    private static CurriculumGroup groupA;
    private static CurriculumGroup groupA2;
    private static CurriculumGroup groupB;
    private static CurriculumGroup groupC;
    private static RootCurriculumGroup root;
    private static DegreeCurricularPlan dcp;

    private static Enrolment enrolmentEnroled;
    private static Enrolment enrolmentApproved;
    private static Enrolment enrolmentOtherInterval;
    private static Enrolment enrolmentOtherYear;

    private static CurricularCourse curricularCourseEnroled;
    private static CurricularCourse curricularCourseApproved;
    private static ExecutionYear executionYear;

    @BeforeClass
    public static void init() {
        OrganizationalStructureTest.init();

        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            UserUtil.initAdminUser();
            StudentTest.initRegistrationConfigEntities();
            EvaluationSeasonTest.initEvaluationSeasons();

            executionYear = ExecutionYear.findCurrent(null);
            final ExecutionInterval executionInterval = executionYear.getFirstExecutionPeriod();

            dcp = createDcp(executionYear);
            final Student student = StudentTest.createStudent("Curriculum Module Test Student",
                    "curriculum.module.test.student." + UUID.randomUUID());
            final Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
            root = registration.getLastStudentCurricularPlan().getRoot();

            // Group Structure:
            //
            // DCP root / SCP root
            //  └── A  (groupA)
            //      └── B  (groupB)
            //          ├── A  (groupA2)
            //          └── C  (groupC)
            final CourseGroup courseGroupA = new CourseGroup(dcp.getRoot(), "A", "A", executionInterval, null);
            final CourseGroup courseGroupB = new CourseGroup(courseGroupA, "B", "B", executionInterval, null);
            final CourseGroup courseGroupC = new CourseGroup(courseGroupB, "C", "C", executionInterval, null);
            final CourseGroup courseGroupA2 = new CourseGroup(courseGroupB, "A", "A", executionInterval, null);

            groupA = new CurriculumGroup(root, courseGroupA);
            groupB = new CurriculumGroup(groupA, courseGroupB);
            groupA2 = new CurriculumGroup(groupB, courseGroupA2);
            groupC = new CurriculumGroup(groupB, courseGroupC);

            final CurricularPeriod year1 = new CurricularPeriod(AcademicPeriod.YEAR, 1, dcp.getDegreeStructure());
            final CurricularPeriod semester1 = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, year1);
            final CurricularPeriod semester2 = new CurricularPeriod(AcademicPeriod.SEMESTER, 2, year1);

            final Unit coursesUnit = Unit.findInternalUnitByAcronymPath(CompetenceCourseTest.COURSES_UNIT_PATH).orElseThrow();
            final CompetenceCourse ccEnroled =
                    CompetenceCourseTest.createCompetenceCourse("Course Enroled", "CC_ENROLED", new BigDecimal("6"),
                            AcademicPeriod.SEMESTER, executionInterval, coursesUnit);
            final CompetenceCourse ccApproved =
                    CompetenceCourseTest.createCompetenceCourse("Course Approved", "CC_APPROVED", new BigDecimal("6"),
                            AcademicPeriod.SEMESTER, executionInterval, coursesUnit);

            curricularCourseEnroled = new CurricularCourse(6d, ccEnroled, courseGroupA, semester1, executionInterval, null);
            new Context(courseGroupA, curricularCourseEnroled, semester2, executionInterval, null);
            curricularCourseApproved = new CurricularCourse(6d, ccApproved, courseGroupA, semester1, executionInterval, null);

            final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
            enrolmentEnroled = new Enrolment(scp, groupA, curricularCourseEnroled, executionInterval, EnrollmentCondition.FINAL,
                    UserUtil.ADMIN_USERNAME);
            enrolmentApproved = new Enrolment(scp, groupA, curricularCourseApproved, executionInterval, EnrollmentCondition.FINAL,
                    UserUtil.ADMIN_USERNAME);

            // same execution year, different execution interval (2nd semester)
            final ExecutionInterval otherInterval = executionYear.getLastExecutionPeriod();
            enrolmentOtherInterval = new Enrolment(scp, groupA, curricularCourseEnroled, otherInterval, EnrollmentCondition.FINAL,
                    UserUtil.ADMIN_USERNAME);

            // different execution year
            final ExecutionYear otherYear = (ExecutionYear) executionYear.getNext();
            enrolmentOtherYear = new Enrolment(scp, groupA, curricularCourseEnroled, otherYear.getFirstExecutionPeriod(),
                    EnrollmentCondition.FINAL, UserUtil.ADMIN_USERNAME);

            GradeScale.findUniqueByCode("TYPE20").orElseGet(
                    () -> GradeScale.create("TYPE20", new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                            new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true));

            approve(enrolmentApproved);

            return null;
        });
    }

    @Test
    public void testCurriculumModule_ComparatorByNameAndId() {
        List<CurriculumModule> modules = new ArrayList<>(List.of(groupC, groupA, groupA2, groupB));
        modules.sort(CurriculumModule.COMPARATOR_BY_NAME_AND_ID);

        assertEquals("A", modules.get(0).getName().getContent());
        assertEquals("A", modules.get(1).getName().getContent());
        assertEquals("B", modules.get(2).getName().getContent());
        assertEquals("C", modules.get(3).getName().getContent());

        CurriculumModule first = modules.get(0);
        CurriculumModule second = modules.get(1);
        assertTrue((first == groupA && second == groupA2) || (first == groupA2 && second == groupA));
        assertTrue(first.getExternalId().compareTo(second.getExternalId()) < 0);
    }

    @Test
    public void testCurriculumModule_ComparatorByFullPathNameAndId() {
        assertEquals(groupA2.getFullPath(), groupB.getFullPath() + " > A");
        assertEquals(groupC.getFullPath(), groupB.getFullPath() + " > C");

        List<CurriculumModule> modules = new ArrayList<>(List.of(groupC, groupA, groupA2, groupB));
        modules.sort(CurriculumModule.COMPARATOR_BY_FULL_PATH_NAME_AND_ID);
        assertEquals(groupA, modules.get(0));
        assertEquals(groupB, modules.get(1));
        assertEquals(groupA2, modules.get(2));
        assertEquals(groupC, modules.get(3));
    }

    @Test
    public void testCurriculumModule_ComparatorByCreationDate() {
        groupA.setCreationDateDateTime(new DateTime(2020, 1, 1, 0, 0, 0));
        groupB.setCreationDateDateTime(new DateTime(2021, 6, 15, 12, 0, 0));
        groupC.setCreationDateDateTime(new DateTime(2022, 12, 31, 23, 59, 59));

        List<CurriculumModule> modules = new ArrayList<>(List.of(groupC, groupA, groupB));
        modules.sort(CurriculumModule.COMPARATOR_BY_CREATION_DATE);
        assertEquals(groupA, modules.get(0));
        assertEquals(groupB, modules.get(1));
        assertEquals(groupC, modules.get(2));
    }

    @Test
    public void testCurriculumModule_GetName() {
        final LocalizedString name = groupA.getName();
        assertEquals(groupA.getDegreeModule().getName(), name.getContent(LocaleUtils.PT));
        assertEquals(groupA.getDegreeModule().getNameEn(), name.getContent(LocaleUtils.EN));
        assertEquals("A", name.getContent(LocaleUtils.PT));
        assertEquals("A", name.getContent(LocaleUtils.EN));
        assertTrue(name.getContent().contains("A"));
        assertEquals(name, groupA.getPresentationName()); // getPresentationName() delegates to getName()
    }

    @Test
    public void testCurriculumModule_GetFullPath() {
        final String rootPath = groupC.getRootCurriculumGroup().getFullPath();
        final String pathA = groupA.getFullPath();
        final String pathB = groupB.getFullPath();
        final String pathC = groupC.getFullPath();

        // each level appends its own name segment separated by " > "
        assertEquals(rootPath + " > A", pathA);
        assertEquals(pathA + " > B", pathB);
        assertEquals(pathB + " > C", pathC);

        // the full path chains every ancestor segment, root first
        assertTrue(pathC.startsWith(rootPath));
        assertTrue(pathC.endsWith(" > C"));
        assertTrue(pathC.contains("A > B > C"));

        // the root has no parent, so it needs no separator
        assertFalse(rootPath.contains(" > "));

    }

    @Test
    public void testCurriculumModule_HasEnrolmentWithEnroledState() {
        assertTrue(enrolmentEnroled.hasEnrolmentWithEnroledState(curricularCourseEnroled, executionYear));
        assertFalse(enrolmentEnroled.hasEnrolmentWithEnroledState(curricularCourseApproved, executionYear));
    }

    @Test
    public void testCurriculumModule_CurriculumModulePredicateByType() {
        final CurriculumModule.CurriculumModulePredicateByType byGroupType =
                new CurriculumModule.CurriculumModulePredicateByType(CurriculumGroup.class);
        final CurriculumModule.CurriculumModulePredicateByType byLineType =
                new CurriculumModule.CurriculumModulePredicateByType(CurriculumLine.class);
        assertTrue(byGroupType.test(groupA));
        assertFalse(byLineType.test(groupA));
        assertTrue(byLineType.test(enrolmentEnroled));
        assertFalse(byGroupType.test(enrolmentEnroled));
    }

    @Test
    public void testCurriculumModule_CurriculumModulePredicateByExecutionInterval() {
        final ExecutionInterval interval = executionYear.getFirstExecutionPeriod();
        final CurriculumModule.CurriculumModulePredicateByExecutionInterval predicate =
                new CurriculumModule.CurriculumModulePredicateByExecutionInterval(interval);
        assertTrue(predicate.test(enrolmentEnroled));
        assertFalse(predicate.test(groupA));

        // enrolment from a different execution interval
        assertNotSame(interval, enrolmentOtherInterval.getExecutionInterval());
        assertFalse(predicate.test(enrolmentOtherInterval));
        assertFalse(predicate.test(enrolmentOtherYear));
    }

    @Test
    public void testCurriculumModule_CurriculumModulePredicateByExecutionYear() {
        final CurriculumModule.CurriculumModulePredicateByExecutionYear predicate =
                new CurriculumModule.CurriculumModulePredicateByExecutionYear(executionYear);
        assertTrue(predicate.test(enrolmentEnroled));
        assertFalse(predicate.test(groupA));

        // enrolment from a different interval of the same year still matches
        assertTrue(predicate.test(enrolmentOtherInterval));

        // enrolment from a different execution year
        assertNotSame(executionYear, enrolmentOtherYear.getExecutionYear());
        assertFalse(predicate.test(enrolmentOtherYear));
    }

    @Test
    public void testCurriculumModule_CurriculumModulePredicateByApproval() {
        final CurriculumModule.CurriculumModulePredicateByApproval predicate =
                new CurriculumModule.CurriculumModulePredicateByApproval();
        assertTrue(predicate.test(enrolmentApproved));
        assertFalse(predicate.test(enrolmentEnroled));
        assertFalse(predicate.test(groupA));
    }

    @Test
    public void testCurriculumModule_GetApprovedCurriculumLinesLastExecutionYear() {
        final ExecutionYear otherYear = (ExecutionYear) executionYear.getNext();

        // single approved line: the result is its execution year
        assertEquals(executionYear, enrolmentApproved.getApprovedCurriculumLinesLastExecutionYear());
        assertEquals(executionYear, groupA.getApprovedCurriculumLinesLastExecutionYear());

        // enrolmentOtherYear (in otherYear) is not approved, so it is ignored
        assertEquals(executionYear, root.getApprovedCurriculumLinesLastExecutionYear());

        // approve a line in a later year: groups now return the most recent year
        final Enrolment approvedOtherYear = new Enrolment(root.getStudentCurricularPlan(), groupA, curricularCourseApproved,
                otherYear.getFirstExecutionPeriod(), EnrollmentCondition.FINAL, UserUtil.ADMIN_USERNAME);
        approve(approvedOtherYear);
        assertEquals(otherYear, approvedOtherYear.getApprovedCurriculumLinesLastExecutionYear());
        assertEquals(otherYear, groupA.getApprovedCurriculumLinesLastExecutionYear());
        assertEquals(otherYear, root.getApprovedCurriculumLinesLastExecutionYear());

        // no approved lines: falls back to the current year
        assertEquals(executionYear, groupB.getApprovedCurriculumLinesLastExecutionYear());
        assertEquals(executionYear, enrolmentEnroled.getApprovedCurriculumLinesLastExecutionYear());
    }

    @Test
    public void testCurriculumModule_GetCurricularRules() {
        final ExecutionInterval interval = executionYear.getFirstExecutionPeriod();

        final ICurricularRule ruleA = new CreditsLimit(groupA.getDegreeModule(), null, interval, null, 0d, 60d);
        final ICurricularRule ruleB = new CreditsLimit(groupB.getDegreeModule(), null, interval, null, 0d, 30d);
        final ICurricularRule ruleCourse =
                new EvenOddRule(curricularCourseEnroled, null, 1, AcademicPeriod.SEMESTER, true, interval, null);

        assertTrue(root.getCurricularRules(interval).isEmpty());
        assertEquals(Set.of(ruleA), groupA.getCurricularRules(interval));
        assertEquals(Set.of(ruleA, ruleB), groupB.getCurricularRules(interval));
        assertEquals(Set.of(ruleA, ruleCourse), enrolmentEnroled.getCurricularRules(interval));

        assertFalse(groupA.getCurricularRules(interval).contains(ruleB));
        assertFalse(enrolmentApproved.getCurricularRules(interval).contains(ruleCourse));
    }

    @Test
    public void testCurriculumModule_GetDegreeCurricularPlanOfDegreeModule() {
        Stream.of(root, groupA, groupA2, groupB, groupC, enrolmentEnroled, enrolmentApproved)
                .forEach(m -> assertEquals(m.getFullPath(), dcp, m.getDegreeCurricularPlanOfDegreeModule()));
    }

    private static void approve(final Enrolment enrolment) {
        final GradeScale type20 = GradeScale.findUniqueByCode("TYPE20").orElseThrow();
        final EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
        evaluation.setGrade(Grade.createGrade("14", type20));
        evaluation.setExamDateYearMonthDay(new YearMonthDay());
        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
        enrolment.setEnrollmentState(EnrollmentState.APROVED);
    }

    private static DegreeCurricularPlan createDcp(final ExecutionYear executionYear) {
        final DegreeType degreeType = new DegreeType(new LocalizedString.Builder().with(Locale.getDefault(), "Degree").build());
        degreeType.setCode("D" + UUID.randomUUID());

        final Degree degree = DegreeTest.createDegree(degreeType, "D" + UUID.randomUUID(), "Curriculum Module Test Degree",
                executionYear);
        final DegreeCurricularPlan dcp = degree.createDegreeCurricularPlan("Curriculum Module Test Degree Plan",
                User.findByUsername(UserUtil.ADMIN_USERNAME).getPerson(), AcademicPeriod.THREE_YEAR);
        dcp.createExecutionDegree(executionYear);
        return dcp;
    }
}