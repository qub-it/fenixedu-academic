package org.fenixedu.academic.domain;

import static org.fenixedu.academic.domain.CompetenceCourseTest.COURSE_A_CODE;
import static org.fenixedu.academic.domain.degreeStructure.CourseLoadType.THEORETICAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class EnrolmentTest {

    private static Registration registration;
    private static CurricularCourse curricularCourse;
    private static ExecutionInterval executionInterval;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            initEnrolments();
            return null;
        });
    }

    public static void initEnrolments() {
        StudentTest.initStudentAndRegistration();
        initDefaultSeason();

        registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();

        executionInterval = ExecutionInterval.findFirstCurrentChild(scp.getDegree().getCalendar());

        curricularCourse = scp.getDegreeCurricularPlan().getCurricularCourseByCode(CompetenceCourseTest.COURSE_A_CODE);
        final Context context = curricularCourse.getParentContextsSet().stream().filter(ctx -> ctx.isValid(executionInterval))
                .findAny().orElseThrow();

        final CourseGroup courseGroup = context.getParentCourseGroup();
        CurriculumGroup curriculumGroup = scp.findCurriculumGroupFor(courseGroup);
        if (curriculumGroup == null) {
            curriculumGroup = findOrCreateCurriculumGroupFor(scp, courseGroup);
        }

        final DegreeModuleToEnrol degreeModuleToEnrol = new DegreeModuleToEnrol(curriculumGroup, context, executionInterval);

        Authenticate.mock(User.findByUsername(StudentTest.STUDENT_A_USERNAME), "none");

        scp.enrol(executionInterval, Set.of(degreeModuleToEnrol), List.of(), CurricularRuleLevel.ENROLMENT_WITH_RULES);

        ExecutionsAndSchedulesTest.initSchedules();

        Authenticate.unmock();
    }

    private static void initDefaultSeason() {
        final LocalizedString seasonLabel = BundleUtil.getLocalizedString(Bundle.ENUMERATION, "NORMAL");
        final EvaluationSeason season = new EvaluationSeason(seasonLabel, seasonLabel, true, true, false, false);
        EvaluationConfiguration.getInstance().setDefaultEvaluationSeason(season);
    }

    //TODO: move this method to domain && remove StudentCurricularPlanServices.initializeGroupIfRequired(StudentCurricularPlan, CourseGroup)
    private static CurriculumGroup findOrCreateCurriculumGroupFor(StudentCurricularPlan scp, CourseGroup courseGroup) {

        final List<CourseGroup> path = new ArrayList<CourseGroup>();
        CourseGroup groupToAdd = courseGroup;
        while (groupToAdd != null) {
            if (!groupToAdd.isRoot()) {
                path.add(0, groupToAdd);
            }
            groupToAdd = groupToAdd.getParentCourseGroupStream().findAny().orElse(null);
        }

        CurriculumGroup current = scp.getRoot();
        for (final CourseGroup pathElement : path) {
            final CurriculumGroup existing = current.findCurriculumGroupFor(pathElement);

            if (existing == null) {
                current = CurriculumGroupFactory.createGroup(current, pathElement);
            } else {
                current = existing;
            }
        }
        return current;
    }

    @Test
    public void testEnrolment_find() {
        final Collection<Enrolment> enrolments =
                registration.getEnrolments(ExecutionInterval.findFirstCurrentChild(registration.getDegree().getCalendar()));
        assertEquals(enrolments.size(), 1);
    }

    @Test
    public void testAttends_find() {
        final Set<Attends> attends = registration.getAssociatedAttendsSet();
        assertEquals(attends.size(), 1);

        final Set<String> attendsCoursesCodes =
                attends.stream().map(a -> a.getExecutionCourse().getCode()).distinct().collect(Collectors.toSet());
        assertTrue(attendsCoursesCodes.contains(COURSE_A_CODE));

        for (final ExecutionCourse executionCourse : curricularCourse.getExecutionCoursesByExecutionPeriod(executionInterval)) {
            final Optional<Attends> attendsOpt = registration.findAttends(executionCourse);
            assertTrue(attendsOpt.isPresent());
            assertTrue(attends.contains(attendsOpt.get()));
        }
    }

    @Test
    public void testShiftEnrolments() {
        final ExecutionCourse executionCourse =
                curricularCourse.getExecutionCoursesByExecutionPeriod(executionInterval).iterator().next();

        final CourseLoadType theoreticalLoad = CourseLoadType.of(THEORETICAL);

        final Shift shiftA = new Shift(executionCourse, theoreticalLoad, 10, "T_testShiftEnrolments_A");
        final Shift shiftB = new Shift(executionCourse, theoreticalLoad, 10, "T_testShiftEnrolments_B");

        assertEquals(registration.getShiftEnrolmentsSet().size(), 0);
        assertEquals(shiftA.getVacancies(), Integer.valueOf(10));
        assertEquals(shiftB.getVacancies(), Integer.valueOf(10));

        assertTrue(shiftA.enrol(registration));
        assertEquals(registration.getShiftEnrolmentsSet().size(), 1);
        assertEquals(registration.findEnrolledShiftFor(executionCourse, theoreticalLoad).get(), shiftA);
        assertEquals(shiftA.getVacancies(), Integer.valueOf(9));
        assertEquals(shiftB.getVacancies(), Integer.valueOf(10));

        assertTrue(shiftB.enrol(registration));
        assertEquals(registration.getShiftEnrolmentsSet().size(), 1);
        assertEquals(registration.findEnrolledShiftFor(executionCourse, theoreticalLoad).get(), shiftB);
        assertEquals(shiftA.getVacancies(), Integer.valueOf(10));
        assertEquals(shiftB.getVacancies(), Integer.valueOf(9));

        shiftB.unenrol(registration);
        assertEquals(registration.getShiftEnrolmentsSet().size(), 0);
        assertTrue(registration.findEnrolledShiftFor(executionCourse, theoreticalLoad).isEmpty());
        assertEquals(shiftA.getVacancies(), Integer.valueOf(10));
        assertEquals(shiftB.getVacancies(), Integer.valueOf(10));
    }
}
