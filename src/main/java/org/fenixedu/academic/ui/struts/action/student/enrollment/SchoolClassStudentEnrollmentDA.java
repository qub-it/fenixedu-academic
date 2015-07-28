/**
 * 
 */
package org.fenixedu.academic.ui.struts.action.student.enrollment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.EnrolmentPeriod;
import org.fenixedu.academic.domain.EnrolmentPeriodInClasses;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.dto.InfoLessonInstanceAggregation;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.student.StudentApplication.StudentEnrollApp;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

/**
 * @author shezad - Jul 9, 2015
 *
 */
@StrutsFunctionality(app = StudentEnrollApp.class, path = "schoolClass-student-enrollment",
        titleKey = "link.schoolClass.student.enrolment")
@Mapping(module = "student", path = "/schoolClassStudentEnrollment")
@Forwards(@Forward(name = "showSchoolClasses", path = "/student/enrollment/schoolClass/schoolClassesSelection.jsp"))
public class SchoolClassStudentEnrollmentDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepare(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {

        final Student student = getUserView(request).getPerson().getStudent();

        final SchoolClass selectedSchoolClass = (SchoolClass) request.getAttribute("selectedSchoolClass");
        final EnrolmentPeriod selectedEnrolmentPeriod = (EnrolmentPeriod) request.getAttribute("selectedEnrolmentPeriod");

        final List<SchoolClassStudentEnrollmentDTO> enrollmentBeans = new ArrayList<SchoolClassStudentEnrollmentDTO>();

        for (Registration registration : student.getRegistrationsToEnrolInShiftByStudent()) {
            for (EnrolmentPeriod enrolmentPeriod : registration.getActiveDegreeCurricularPlan().getEnrolmentPeriodsSet()) {
                if (enrolmentPeriod instanceof EnrolmentPeriodInClasses && enrolmentPeriod.isValid()) {
                    enrollmentBeans.add(new SchoolClassStudentEnrollmentDTO(registration, enrolmentPeriod,
                            selectedEnrolmentPeriod == enrolmentPeriod ? selectedSchoolClass : null));
                }
            }
        }

        enrollmentBeans.sort((eb1, eb2) -> eb1.compareTo(eb2));
        request.setAttribute("enrollmentBeans", enrollmentBeans);
        return mapping.findForward("showSchoolClasses");
    }

    public ActionForward viewSchoolClass(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        final SchoolClass schoolClass = getDomainObject(request, "schoolClassID");
        final EnrolmentPeriod enrolmentPeriod = getDomainObject(request, "enrolmentPeriodID");

        request.setAttribute("selectedSchoolClass", schoolClass);
        request.setAttribute("selectedEnrolmentPeriod", enrolmentPeriod);

        return prepare(mapping, form, request, response);
    }

    public ActionForward enrollInSchoolClass(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        final SchoolClass schoolClass = getDomainObject(request, "schoolClassID");
        final Registration registration = getDomainObject(request, "registrationID");
        final EnrolmentPeriod enrolmentPeriod = getDomainObject(request, "enrolmentPeriodID");

        try {
            atomic(() -> registration.replaceSchoolClass(schoolClass, enrolmentPeriod.getExecutionPeriod()));
            final String successMessage =
                    schoolClass != null ? "message.schoolClassStudentEnrollment.enrollInSchoolClass.success" : "message.schoolClassStudentEnrollment.unenrollInSchoolClass.success";
            addActionMessage("success", request, successMessage);
        } catch (DomainException e) {
            addActionMessage("error", request, e.getKey(), e.getArgs());
        }

        request.setAttribute("selectedSchoolClass", schoolClass);
        request.setAttribute("selectedEnrolmentPeriod", enrolmentPeriod);

        return prepare(mapping, form, request, response);
    }

    public static class SchoolClassStudentEnrollmentDTO implements Serializable, Comparable<SchoolClassStudentEnrollmentDTO> {

        private Registration registration;
        private EnrolmentPeriod enrolmentPeriod;
        private SchoolClass schoolClassToDisplay;

        public SchoolClassStudentEnrollmentDTO(Registration registration, EnrolmentPeriod enrolmentPeriod,
                SchoolClass schoolClassToDisplay) {
            super();
            this.registration = registration;
            this.enrolmentPeriod = enrolmentPeriod;
            this.schoolClassToDisplay = schoolClassToDisplay;
        }

        public Registration getRegistration() {
            return registration;
        }

        public EnrolmentPeriod getEnrolmentPeriod() {
            return enrolmentPeriod;
        }

        public SchoolClass getCurrentSchoolClass() {
            return getRegistration().getSchoolClassBy(getEnrolmentPeriod().getExecutionPeriod()).orElse(null);
        }

        public SchoolClass getSchoolClassToDisplay() {
            if (schoolClassToDisplay != null) {
                return schoolClassToDisplay;
            }
            final SchoolClass currentSchoolClass = getCurrentSchoolClass();
            return currentSchoolClass != null ? currentSchoolClass : getSchoolClassesToEnrol().stream().findFirst().orElse(null);
        }

        public boolean isSchoolClassToDisplayFree() {
            final SchoolClass schoolClassToDisplay = getSchoolClassToDisplay();
            if (schoolClassToDisplay != null) {
                final List<ExecutionCourse> attendingExecutionCourses =
                        registration.getAttendingExecutionCoursesFor(schoolClassToDisplay.getExecutionPeriod());
                return !schoolClassToDisplay.getAssociatedShiftsSet().stream()
                        .filter(s -> attendingExecutionCourses.contains(s.getExecutionCourse()))
                        .anyMatch(s -> s.getLotacao().intValue() <= s.getStudentsSet().size());
            }
            return false;
        }

        public List<InfoLessonInstanceAggregation> getSchoolClassToDisplayLessons() {
            final SchoolClass schoolClassToDisplay = getSchoolClassToDisplay();
            return schoolClassToDisplay != null ? getLessonsForRegistration(schoolClassToDisplay, registration) : Collections
                    .emptyList();
        }

        protected static List<InfoLessonInstanceAggregation> getLessonsForRegistration(final SchoolClass schoolClass,
                final Registration registration) {
            final List<ExecutionCourse> attendingExecutionCourses =
                    registration.getAttendingExecutionCoursesFor(schoolClass.getExecutionPeriod());
            return schoolClass.getAssociatedShiftsSet().stream()
                    .filter(s -> attendingExecutionCourses.contains(s.getExecutionCourse()))
                    .flatMap(s -> InfoLessonInstanceAggregation.getAggregations(s).stream()).collect(Collectors.toList());
        }

        public Set<SchoolClass> getSchoolClassesToEnrol() {
            final ExecutionSemester executionSemester = getEnrolmentPeriod().getExecutionPeriod();
            int curricularYear = getRegistration().getCurricularYear(executionSemester.getExecutionYear());
            return getRegistration()
                    .getSchoolClassesToEnrolBy(getRegistration().getActiveDegreeCurricularPlan(), executionSemester).stream()
                    .filter(s -> s.getAnoCurricular().equals(curricularYear))
                    .sorted((s1, s2) -> s1.getNome().compareTo(s2.getNome())).collect(Collectors.toSet());
        }

        @Override
        public int compareTo(SchoolClassStudentEnrollmentDTO o) {
            int result = Degree.COMPARATOR_BY_NAME_AND_ID.compare(getRegistration().getDegree(), o.getRegistration().getDegree());
            return result == 0 ? getEnrolmentPeriod().getExecutionPeriod().compareTo(o.getEnrolmentPeriod().getExecutionPeriod()) : result;
        }

    }
}
