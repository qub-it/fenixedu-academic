package org.fenixedu.academic.ui.struts.action.tests.enrolment;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.academicAdministration.AcademicAdministrationApplication.AcademicAdminStudentsApp;
import org.fenixedu.academic.ui.struts.action.administrativeOffice.studentEnrolment.bolonha.AcademicAdminOfficeBolonhaStudentEnrollmentDA;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

@StrutsFunctionality(app = AcademicAdminStudentsApp.class, path = "enrolment-load-test",
        titleKey = "title.tests.enrolmentLoadTest", accessGroup = "#managers")
@Mapping(path = "/enrolmentLoadTest", module = "academicAdministration", formBean = "bolonhaStudentEnrollmentForm")
@Forwards({

        @Forward(name = "showDegreeModulesToEnrol", path = "/tests/enrolment/showDegreeModulesToEnrol.jsp"),

        @Forward(name = "chooseOptionalCurricularCourseToEnrol",
                path = "/tests/enrolment/chooseOptionalCurricularCourseToEnrol.jsp"),

        @Forward(name = "chooseCycleCourseGroupToEnrol", path = "/tests/enrolment/chooseCycleCourseGroupToEnrol.jsp"),

        @Forward(name = "notAuthorized", path = "/student/notAuthorized_bd.jsp")

})
public class EnrolmentLoadTestDA extends AcademicAdminOfficeBolonhaStudentEnrollmentDA {

    private List<StudentCurricularPlan> scpsCache;

    @EntryPoint
    @Override
    public ActionForward prepare(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {
        return prepareShowDegreeModulesToEnrol(mapping, form, request, response, getStudentCurricularPlan(request),
                getExecutionPeriod(request));
    }

    @Override
    protected void addDebtsWarningMessages(Student student, ExecutionInterval executionInterval, HttpServletRequest request) {

    }

    @Override
    protected String getAction() {
        return "/enrolmentLoadTest.do";
    }

    @Override
    protected StudentCurricularPlan getStudentCurricularPlan(final HttpServletRequest request) {
        loadScpsCache();
        return !scpsCache.isEmpty() ? scpsCache.get(new Random().nextInt(scpsCache.size())) : null;
    }

    private void loadScpsCache() {
        if (scpsCache == null) {
            scpsCache = ExecutionInterval.findActiveAggregators().stream()
                    .sorted(ExecutionInterval.COMPARATOR_BY_BEGIN_DATE.reversed()).skip(0).limit(5).map(ei -> (ExecutionYear) ei)
                    .flatMap(ey -> ey.getRegistrationDataByExecutionYearSet().stream())
                    .map(rd -> rd.getRegistration().getLastStudentCurricularPlan()).collect(Collectors.toList());
        }
    }

    @Override
    protected ExecutionInterval getExecutionPeriod(final HttpServletRequest request) {
        return ExecutionYear.findCurrents().iterator().next().getFirstExecutionPeriod();
    }

    @Override
    protected CurricularRuleLevel getCurricularRuleLevel(final ActionForm form) {
        return CurricularRuleLevel.ENROLMENT_WITH_RULES;
    }

    @Override
    public ActionForward enrolInCycleCourseGroup(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActionForward enrolInDegreeModules(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActionForward enrolInOptionalCurricularCourse(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        throw new UnsupportedOperationException();
    }

}
