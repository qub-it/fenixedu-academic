package org.fenixedu.academic.domain.student.enrolment;

import static org.fenixedu.academic.domain.DegreeTest.DEGREE_TYPE_CODE;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCurricularCourse;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.EnrolmentToBeApprovedByCoordinator;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.dto.student.enrollment.bolonha.StudentCurriculumGroupBean;
import org.fenixedu.bennu.core.domain.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class StudentCurriculumBeanForEnrolmentTest {

    private static final String CYCLE_GROUP = "Cycle";
    private static final String MANDATORY_GROUP = "Mandatory";
    private static final String OPTIONAL_GROUP = "Optional";
    private static final String BRANCH_GROUP = "Branch";
    private static final String ADMIN_USERNAME = "admin";

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            return null;
        });
    }

    private static DegreeCurricularPlan createDegreeCurricularPlan(ExecutionYear executionYear) {
        final ExecutionInterval firstExecutionPeriod = executionYear.getFirstExecutionPeriod();
        final DegreeType degreeType = DegreeType.findByCode(DEGREE_TYPE_CODE).get();
        final Degree degree = DegreeTest.createDegree(degreeType, "D" + System.currentTimeMillis(),
                "D" + System.currentTimeMillis(), executionYear);
        final User user = User.findByUsername(ADMIN_USERNAME);

        final DegreeCurricularPlan degreeCurricularPlan =
                degree.createDegreeCurricularPlan("Plan 1", user.getPerson(), AcademicPeriod.THREE_YEAR);
        degreeCurricularPlan.setCurricularStage(CurricularStage.APPROVED);
        final CurricularPeriod firstYearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
        new CurricularPeriod(AcademicPeriod.SEMESTER, 1, firstYearPeriod);
        new CurricularPeriod(AcademicPeriod.SEMESTER, 2, firstYearPeriod);

        final CurricularPeriod secondYearPeriod =
                new CurricularPeriod(AcademicPeriod.YEAR, 2, degreeCurricularPlan.getDegreeStructure());
        new CurricularPeriod(AcademicPeriod.SEMESTER, 1, secondYearPeriod);
        new CurricularPeriod(AcademicPeriod.SEMESTER, 2, secondYearPeriod);

        final CurricularPeriod period1Y1S = degreeCurricularPlan.getCurricularPeriodFor(1, 1, SEMESTER);
        final CurricularPeriod period1Y2S = degreeCurricularPlan.getCurricularPeriodFor(1, 2, SEMESTER);
        final CurricularPeriod period2Y1S = degreeCurricularPlan.getCurricularPeriodFor(2, 1, SEMESTER);
        final CurricularPeriod period2Y2S = degreeCurricularPlan.getCurricularPeriodFor(2, 2, SEMESTER);

        final CourseGroup cycleGroup =
                new CourseGroup(degreeCurricularPlan.getRoot(), CYCLE_GROUP, CYCLE_GROUP, executionYear, null, null);

        final CourseGroup mandatoryGroup =
                new CourseGroup(cycleGroup, MANDATORY_GROUP, MANDATORY_GROUP, executionYear, null, null);

        createCurricularCourse("C1", "Course 1", new BigDecimal(6), period1Y1S, firstExecutionPeriod, mandatoryGroup); // * enrol
        createCurricularCourse("C2", "Course 2", new BigDecimal(6), period1Y2S, firstExecutionPeriod, mandatoryGroup); // * enrol
        createCurricularCourse("C3", "Course 3", new BigDecimal(6), period2Y1S, firstExecutionPeriod, mandatoryGroup); // * enrol
        createCurricularCourse("C4", "Course 4", new BigDecimal(6), period2Y2S, firstExecutionPeriod, mandatoryGroup);

        final CourseGroup optionalGroup = new CourseGroup(cycleGroup, OPTIONAL_GROUP, OPTIONAL_GROUP, executionYear, null, null);
        optionalGroup.setIsOptional(true);
        createCurricularCourse("C5", "Course 5", new BigDecimal(6), period2Y1S, firstExecutionPeriod, optionalGroup);
        createCurricularCourse("C6", "Course 6", new BigDecimal(6), period2Y1S, firstExecutionPeriod, optionalGroup);
        createCurricularCourse("C7", "Course 7", new BigDecimal(6), period2Y2S, firstExecutionPeriod, optionalGroup); // * enrol

        final CourseGroup branchGroup = new CourseGroup(cycleGroup, BRANCH_GROUP, BRANCH_GROUP, executionYear, null, null);
        new EnrolmentToBeApprovedByCoordinator(branchGroup, null, executionYear, null); //avoid automatic enrolment in optional group
        createCurricularCourse("C8", "Course 8", new BigDecimal(6), period1Y1S, firstExecutionPeriod, branchGroup);
        createCurricularCourse("C9", "Course 9", new BigDecimal(6), period2Y2S, firstExecutionPeriod, branchGroup);

//      System.out.println(degreeCurricularPlan.print());

        degreeCurricularPlan.createExecutionDegree(executionYear);

        return degreeCurricularPlan;
    }

    @Test
    public void studentCurriculumGroupBean_forFirstSemester() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C7");

        final StudentCurriculumGroupBean rootBean = new StudentCurriculumGroupBean(curricularPlan.getRoot(),
                Set.of(executionYear.getChildInterval(1, AcademicPeriod.SEMESTER)));

        studentCurriculumGroupBean_asserts_forFirstSemester(rootBean);
    }

    @Test
    public void studentCurriculumGroupBean_forFirstSemester_deprecated() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C7");

        final StudentCurriculumGroupBean rootBean = new StudentCurriculumGroupBean(curricularPlan.getRoot(),
                executionYear.getChildInterval(1, AcademicPeriod.SEMESTER));

        studentCurriculumGroupBean_asserts_forFirstSemester(rootBean);
    }

    private void studentCurriculumGroupBean_asserts_forFirstSemester(final StudentCurriculumGroupBean rootBean) {
        assertEquals(rootBean.getEnrolledCurriculumGroups().size(), 1);
        assertEquals(rootBean.getEnrolledCurriculumCourses().size(), 0);
        assertEquals(rootBean.getCourseGroupsToEnrol().size(), 0);
        assertEquals(rootBean.getCurricularCoursesToEnrol().size(), 0);

        final StudentCurriculumGroupBean cycleBean = rootBean.getEnrolledCurriculumGroups().iterator().next();
        assertEquals(cycleBean.getCurriculumModule().getDegreeModule().getName(), CYCLE_GROUP);

        assertEquals(cycleBean.getEnrolledCurriculumGroups().size(), 2);
        assertEquals(cycleBean.getEnrolledCurriculumCourses().size(), 0);
        assertEquals(cycleBean.getCourseGroupsToEnrol().size(), 1);
        assertEquals(cycleBean.getCurricularCoursesToEnrol().size(), 0);
        assertEquals(cycleBean.getCourseGroupsToEnrol().iterator().next().getName(), BRANCH_GROUP);

        final Optional<StudentCurriculumGroupBean> mandatoryGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(MANDATORY_GROUP)).findAny();

        assertTrue(mandatoryGroup.isPresent());
        assertEquals(mandatoryGroup.get().getEnrolledCurriculumGroups().size(), 0);
        assertEquals(mandatoryGroup.get().getEnrolledCurriculumCourses().size(), 2);
        assertEquals(mandatoryGroup.get().getCourseGroupsToEnrol().size(), 0);
        assertEquals(mandatoryGroup.get().getCurricularCoursesToEnrol().size(), 0);
        assertEquals(getEnrolledCurriculumCoursesCodes(mandatoryGroup.get()), "C1,C3");

        final Optional<StudentCurriculumGroupBean> optionalGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(OPTIONAL_GROUP)).findAny();

        assertTrue(optionalGroup.isPresent());
        assertEquals(optionalGroup.get().getEnrolledCurriculumGroups().size(), 0);
        assertEquals(optionalGroup.get().getEnrolledCurriculumCourses().size(), 0);
        assertEquals(optionalGroup.get().getCourseGroupsToEnrol().size(), 0);
        assertEquals(optionalGroup.get().getCurricularCoursesToEnrol().size(), 2);
        assertEquals(getEnrolledCurriculumCoursesCodes(optionalGroup.get()), "");
    }

    @Test
    public void studentCurriculumGroupBean_forSecondSemester() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C7");

        final StudentCurriculumGroupBean rootBean = new StudentCurriculumGroupBean(curricularPlan.getRoot(),
                Set.of(executionYear.getChildInterval(2, AcademicPeriod.SEMESTER)));

        studentCurriculumGroupBean_asserts_forSecondSemester(rootBean);
    }

    @Test
    public void studentCurriculumGroupBean_forSecondSemester_deprecated() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C7");

        final StudentCurriculumGroupBean rootBean = new StudentCurriculumGroupBean(curricularPlan.getRoot(),
                executionYear.getChildInterval(2, AcademicPeriod.SEMESTER));

        studentCurriculumGroupBean_asserts_forSecondSemester(rootBean);
    }

    private void studentCurriculumGroupBean_asserts_forSecondSemester(final StudentCurriculumGroupBean rootBean) {
        assertEquals(rootBean.getEnrolledCurriculumGroups().size(), 1);
        assertEquals(rootBean.getEnrolledCurriculumCourses().size(), 0);
        assertEquals(rootBean.getCourseGroupsToEnrol().size(), 0);
        assertEquals(rootBean.getCurricularCoursesToEnrol().size(), 0);

        final StudentCurriculumGroupBean cycleBean = rootBean.getEnrolledCurriculumGroups().iterator().next();
        assertEquals(cycleBean.getCurriculumModule().getDegreeModule().getName(), CYCLE_GROUP);

        assertEquals(cycleBean.getEnrolledCurriculumGroups().size(), 2);
        assertEquals(cycleBean.getEnrolledCurriculumCourses().size(), 0);
        assertEquals(cycleBean.getCourseGroupsToEnrol().size(), 1);
        assertEquals(cycleBean.getCurricularCoursesToEnrol().size(), 0);
        assertEquals(cycleBean.getCourseGroupsToEnrol().iterator().next().getName(), BRANCH_GROUP);

        final Optional<StudentCurriculumGroupBean> mandatoryGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(MANDATORY_GROUP)).findAny();

        assertTrue(mandatoryGroup.isPresent());
        assertEquals(mandatoryGroup.get().getEnrolledCurriculumGroups().size(), 0);
        assertEquals(mandatoryGroup.get().getEnrolledCurriculumCourses().size(), 1);
        assertEquals(mandatoryGroup.get().getCourseGroupsToEnrol().size(), 0);
        assertEquals(mandatoryGroup.get().getCurricularCoursesToEnrol().size(), 1);
        assertEquals(getEnrolledCurriculumCoursesCodes(mandatoryGroup.get()), "C2");

        final Optional<StudentCurriculumGroupBean> optionalGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(OPTIONAL_GROUP)).findAny();

        assertTrue(optionalGroup.isPresent());
        assertEquals(optionalGroup.get().getEnrolledCurriculumGroups().size(), 0);
        assertEquals(optionalGroup.get().getEnrolledCurriculumCourses().size(), 1);
        assertEquals(optionalGroup.get().getCourseGroupsToEnrol().size(), 0);
        assertEquals(optionalGroup.get().getCurricularCoursesToEnrol().size(), 0);
        assertEquals(getEnrolledCurriculumCoursesCodes(optionalGroup.get()), "C7");
    }

    @Test
    public void studentCurriculumGroupBean_forBothSemesters() {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);

        final StudentCurricularPlan curricularPlan =
                createRegistration(degreeCurricularPlan, executionYear).getLastStudentCurricularPlan();
        enrol(curricularPlan, executionYear, "C1", "C2", "C3", "C7");

        final StudentCurriculumGroupBean rootBean =
                new StudentCurriculumGroupBean(curricularPlan.getRoot(), executionYear.getChildIntervals());

        assertEquals(rootBean.getEnrolledCurriculumGroups().size(), 1);
        assertEquals(rootBean.getEnrolledCurriculumCourses().size(), 0);
        assertEquals(rootBean.getCourseGroupsToEnrol().size(), 0);
        assertEquals(rootBean.getCurricularCoursesToEnrol().size(), 0);

        final StudentCurriculumGroupBean cycleBean = rootBean.getEnrolledCurriculumGroups().iterator().next();
        assertEquals(cycleBean.getCurriculumModule().getDegreeModule().getName(), CYCLE_GROUP);

        assertEquals(cycleBean.getEnrolledCurriculumGroups().size(), 2);
        assertEquals(cycleBean.getEnrolledCurriculumCourses().size(), 0);
        assertEquals(cycleBean.getCourseGroupsToEnrol().size(), 1);
        assertEquals(cycleBean.getCurricularCoursesToEnrol().size(), 0);
        assertEquals(cycleBean.getCourseGroupsToEnrol().iterator().next().getName(), BRANCH_GROUP);

        final Optional<StudentCurriculumGroupBean> mandatoryGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(MANDATORY_GROUP)).findAny();

        assertTrue(mandatoryGroup.isPresent());
        assertEquals(mandatoryGroup.get().getEnrolledCurriculumGroups().size(), 0);
        assertEquals(mandatoryGroup.get().getEnrolledCurriculumCourses().size(), 3);
        assertEquals(mandatoryGroup.get().getCourseGroupsToEnrol().size(), 0);
        assertEquals(mandatoryGroup.get().getCurricularCoursesToEnrol().size(), 1);
        assertEquals(getEnrolledCurriculumCoursesCodes(mandatoryGroup.get()), "C1,C2,C3");

        final Optional<StudentCurriculumGroupBean> optionalGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(OPTIONAL_GROUP)).findAny();

        assertTrue(optionalGroup.isPresent());
        assertEquals(optionalGroup.get().getEnrolledCurriculumGroups().size(), 0);
        assertEquals(optionalGroup.get().getEnrolledCurriculumCourses().size(), 1);
        assertEquals(optionalGroup.get().getCourseGroupsToEnrol().size(), 0);
        assertEquals(optionalGroup.get().getCurricularCoursesToEnrol().size(), 2);
        assertEquals(getEnrolledCurriculumCoursesCodes(optionalGroup.get()), "C7");

        final Optional<StudentCurriculumGroupBean> branchGroup = cycleBean.getEnrolledCurriculumGroups().stream()
                .filter(bean -> bean.getCurriculumModule().getDegreeModule().getName().equals(BRANCH_GROUP)).findAny();
        assertTrue(branchGroup.isEmpty());
    }

    private static String getEnrolledCurriculumCoursesCodes(final StudentCurriculumGroupBean bean) {
        return bean.getEnrolledCurriculumCourses().stream().map(b -> b.getCurriculumModule().getDegreeModule().getCode()).sorted()
                .collect(Collectors.joining(","));
    }

}
