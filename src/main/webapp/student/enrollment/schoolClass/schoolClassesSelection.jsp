<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/struts-example-1.0" prefix="app" %>
<%@ page import="org.fenixedu.academic.servlet.taglib.sop.v3.TimeTableType" %>
<link href="${pageContext.request.contextPath}/CSS/dotist_timetables.css" rel="stylesheet" type="text/css" />

<style>
.nav > li > a:hover, .nav > li > a:focus {
    text-decoration: none;
    background-color: #eee;
}
</style>

<h1><bean:message bundle="STUDENT_RESOURCES" key="title.student.shift.enrollment" /></h1>

<c:if test="${not empty enrollmentBeans}">
	<div class="alert alert-warning" role="alert"><bean:message bundle="STUDENT_RESOURCES" key="message.warning.student.enrolmentClasses" /> 
		<html:link page="<%= "/studentEnrollmentManagement.do?method=prepare" %>" styleClass="alert-link" style="color: #7F3C00"><bean:message bundle="STUDENT_RESOURCES" key="message.warning.student.enrolmentClasses.Fenix" /></html:link>.
	</div>
</c:if>
<c:if test="${empty enrollmentBeans}">
	<div class="alert alert-danger" role="alert"><bean:message bundle="STUDENT_RESOURCES" key="message.schoolClassStudentEnrollment.noOpenPeriods" /></div>
</c:if>

<logic:messagesPresent message="true" property="error">
	<div class="alert alert-danger alert-dismissible" role="alert">
	  <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
	  <strong><bean:message bundle="STUDENT_RESOURCES" key="label.enrollment.errors.in.enrolment" />: </strong>
	  <html:messages id="messages" message="true" bundle="STUDENT_RESOURCES" property="error"><bean:write name="messages" /></html:messages>
	</div>
</logic:messagesPresent>
<logic:messagesPresent message="true" property="success">
	<div class="alert alert-success alert-dismissible" role="alert">
	  <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
	  <html:messages id="messages" message="true" bundle="STUDENT_RESOURCES" property="success"><bean:write name="messages" /></html:messages>
	</div>
</logic:messagesPresent>

<c:forEach items="${enrollmentBeans}" var="enrollmentBean">
	<c:set value="${enrollmentBean.currentSchoolClass}" var="currentSchoolClass"/>
	<c:set value="${enrollmentBean.schoolClassToDisplay}" var="schoolClassToDisplay"/>
  	<c:set value="${enrollmentBean.schoolClassesToEnrol}" var="schoolClassesToEnrol"/>
	
	<div class="panel panel-default">
	  <div class="panel-heading">
		<h3>
			<c:out value="${enrollmentBean.enrolmentPeriod.executionPeriod.qualifiedName}" />
			<small><c:out value="${enrollmentBean.registration.activeDegreeCurricularPlan.degree.presentationName}" /></small>
				<c:if test="${not empty schoolClassesToEnrol}">
					<c:if test="${not empty currentSchoolClass}">
						<span title="<bean:message bundle="STUDENT_RESOURCES" key="label.schoolClassStudentEnrollment.selectedSchoolClassForPeriod" />" class="glyphicon glyphicon-ok text-success" aria-hidden="true"></span>
				  	</c:if>
					<c:if test="${empty currentSchoolClass}">
						<span title="<bean:message bundle="STUDENT_RESOURCES" key="label.schoolClassStudentEnrollment.noSelectedSchoolClassForPeriod" />" class="glyphicon glyphicon-exclamation-sign text-warning" aria-hidden="true"></span>
				  	</c:if>				  	
				</c:if>
		</h3>
	  </div>
	  <div class="panel-body">
	  	<c:if test="${empty schoolClassesToEnrol}">
	  		<div class="alert alert-warning" role="alert"><bean:message bundle="STUDENT_RESOURCES" key="message.schoolClassStudentEnrollment.noAvailableSchoolClassesForPeriod" /></div>
	  	</c:if>
	  	<c:if test="${not empty schoolClassesToEnrol}">
			<ul class="nav nav-tabs">
				<c:forEach items="${enrollmentBean.schoolClassesToEnrol}" var="schoolClass">
					
					<bean:define id="activeClass"><c:if test="${schoolClass eq schoolClassToDisplay}">active</c:if> q</bean:define>
					<li class="<%= activeClass %>">
					
						<bean:define id="link">/schoolClassStudentEnrollment.do?method=viewSchoolClass&schoolClassID=<c:out value="${schoolClass.externalId}" />&registrationID=<c:out value="${enrollmentBean.registration.externalId}" />&enrolmentPeriodID=<c:out value="${enrollmentBean.enrolmentPeriod.externalId}" /></bean:define>
						<html:link page="<%= link %>">
							<c:out value="${schoolClass.editablePartOfName}" />
							<c:if test="${(not empty currentSchoolClass) and (schoolClass eq currentSchoolClass)}">
								<span class="badge"><bean:message bundle="STUDENT_RESOURCES" key="label.schoolClassStudentEnrollment.selected" /></span>
							</c:if>
						</html:link>
						
					</li>
				</c:forEach>
			</ul>
			
			<p></p>
			
			<c:if test="${not empty schoolClassToDisplay}">
				<c:choose>
				    <c:when test="${(not empty currentSchoolClass) and (schoolClassToDisplay eq currentSchoolClass)}">
						<bean:define id="removeSchoolClassLink">/schoolClassStudentEnrollment.do?method=enrollInSchoolClass&registrationID=<c:out value="${enrollmentBean.registration.externalId}" />&enrolmentPeriodID=<c:out value="${enrollmentBean.enrolmentPeriod.externalId}" /></bean:define>
						<html:link page="<%= removeSchoolClassLink %>" styleClass="btn btn-warning btn-xs mtop15">
							<span class="glyphicon glyphicon-remove" aria-hidden="true"></span> <bean:message bundle="STUDENT_RESOURCES" key="button.schoolClassStudentEnrollment.unselectSchoolClass" />
						</html:link>			
				    </c:when>
				    <c:otherwise>
						<bean:define id="selectSchoolClassLink">/schoolClassStudentEnrollment.do?method=enrollInSchoolClass&schoolClassID=<c:out value="${schoolClassToDisplay.externalId}" />&registrationID=<c:out value="${enrollmentBean.registration.externalId}" />&enrolmentPeriodID=<c:out value="${enrollmentBean.enrolmentPeriod.externalId}" /></bean:define>
						<bean:define id="selectSchoolClassLinkCssClass">btn btn-primary btn-xs mtop15 <c:if test="${not enrollmentBean.schoolClassToDisplayFree}">disabled</c:if></bean:define>
						<html:link page="<%= selectSchoolClassLink %>" styleClass="<%= selectSchoolClassLinkCssClass %>">
							<span class="glyphicon glyphicon-ok" aria-hidden="true"></span> <bean:message bundle="STUDENT_RESOURCES" key="button.schoolClassStudentEnrollment.selectSchoolClass" />
						</html:link>		    
				    	<c:if test="${not enrollmentBean.schoolClassToDisplayFree}"><span class="text-warning"><bean:message bundle="STUDENT_RESOURCES" key="label.schoolClassStudentEnrollment.fullSchoolClass" /></span></c:if>
				    </c:otherwise>
				</c:choose>
		
				<c:set value="${enrollmentBean.schoolClassToDisplayLessons}" var="schoolClassToDisplayLessons"/>
				<c:if test="${not empty schoolClassToDisplayLessons}">
					<div class="mtop15">
						<app:gerarHorario name="schoolClassToDisplayLessons" type="<%= TimeTableType.CLASS_TIMETABLE %>" application="<%= request.getContextPath() %>"/>
					</div>
				</c:if>
			</c:if>
			
	  	</c:if>	  	
	  </div>
	</div>

</c:forEach>