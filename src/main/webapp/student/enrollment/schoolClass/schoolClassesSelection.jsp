<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<link href='${pageContext.request.contextPath}/themes/fenixedu-learning-theme/static/css/fullcalendar.css' rel='stylesheet' />
<link href='${pageContext.request.contextPath}/themes/fenixedu-learning-theme/static/css/fullcalendar.print.css' rel='stylesheet' media='print' />
<link href='${pageContext.request.contextPath}/themes/fenixedu-learning-theme/static/css/schedule.css' rel='stylesheet' rel='stylesheet' />

<script src='${pageContext.request.contextPath}/themes/fenixedu-learning-theme/static/js/moment.min.js'></script>
<script src='${pageContext.request.contextPath}/themes/fenixedu-learning-theme/static/js/jquery-ui.fullCalendar.custom.min.js'></script>
<script src='${pageContext.request.contextPath}/themes/fenixedu-learning-theme/static/js/fullcalendar.js'></script>

<style>
.nav-tabs > li > a:hover, .nav-tabs > li > a:focus {
    text-decoration: none;
    background-color: #eee;
}
.fc-today {
   background: transparent !important;
} 
.calendarHoverIn {
	background: #F00;
	cursor:pointer;
}
.calendarHoverIn:after { 
    content: "[X] <bean:message bundle="APPLICATION_RESOURCES" key="label.remove" />";
    font-weight: bold;
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
	  		<p class="text-muted"><bean:message bundle="STUDENT_RESOURCES" key="label.shiftEnrolment.schoolClassesCurricularYear" />: <strong><c:out value="${enrollmentBean.curricularYear}" /></strong></p>
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
				    	<c:set value="true" var="renderingCurrentSchoolClass"/>
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
		
				<c:set value="${enrollmentBean.schoolClassToDisplayLessonsJson}" var="schoolClassToDisplayLessonsJson"/>
				<c:if test="${not empty schoolClassToDisplayLessonsJson}">
					<c:forEach items="${enrollmentBean.schoolClassToDisplayShifts}" var="schoolClassShift">
						<bean:define id="removeShiftLink">/schoolClassStudentEnrollment.do?method=removeShift&registrationID=<c:out value="${enrollmentBean.registration.externalId}" />&enrolmentPeriodID=<c:out value="${enrollmentBean.enrolmentPeriod.externalId}" />&shiftID=<c:out value="${schoolClassShift.externalId}" /></bean:define>
						<html:link page="<%= removeShiftLink %>" styleClass="btn btn-danger removeShiftLink hidden" styleId="removeShiftLink-${schoolClassShift.externalId}">
							<bean:message bundle="APPLICATION_RESOURCES" key="label.remove" />&nbsp;&nbsp;<span class="glyphicon glyphicon-remove" style="color: #FFF"></span>
						</html:link>					 
					</c:forEach>
				 
					<div id="calendar"></div>
					<script>
					$(document).ready(function() {
						
						var i18nDayNames = [
							"<bean:message bundle="ENUMERATION_RESOURCES" key="SUNDAY.short" />",
							"<bean:message bundle="ENUMERATION_RESOURCES" key="MONDAY.short" />",
							"<bean:message bundle="ENUMERATION_RESOURCES" key="TUESDAY.short" />",
							"<bean:message bundle="ENUMERATION_RESOURCES" key="WEDNESDAY.short" />",
							"<bean:message bundle="ENUMERATION_RESOURCES" key="THURSDAY.short" />",
							"<bean:message bundle="ENUMERATION_RESOURCES" key="FRIDAY.short" />",
							"<bean:message bundle="ENUMERATION_RESOURCES" key="SATURDAY.short" />"
						];						
						
						$('#calendar').fullCalendar({
								header: { left: '', center: '', right: '' },
								defaultView: 'agendaWeek',
								columnFormat: { week: 'ddd' },			
								minTime: '08:00',
								maxTime: '24:00',
								timeFormat: 'HH:mm',
								axisFormat: 'HH:mm',
								allDaySlot : false,
								dayNamesShort: i18nDayNames,
								firstDay: 1,	
								hiddenDays: [0],						
								editable: false,
								eventLimit: true, // allow "more" link when too many events
								events: <c:out value="${schoolClassToDisplayLessonsJson}" escapeXml="false" />,
								<c:if test="${renderingCurrentSchoolClass}">
									eventClick: function(calEvent, jsEvent, view) {
										$('#removeShiftModal .modal-title').text(calEvent.title);
										$('#removeShiftModal .modal-footer .removeShiftLink').remove();
										$('#removeShiftModal .modal-footer').append($('#removeShiftLink-' + calEvent.shiftId).clone());
										$('#removeShiftModal .modal-footer #removeShiftLink-' + calEvent.shiftId).removeClass('hidden');
										$('#removeShiftModal').modal({ });							        
								    },
								    eventMouseover: function(calEvent, jsEvent, view) {
								     	$(this).addClass('calendarHoverIn');
								    },
								    eventMouseout: function(calEvent, jsEvent, view) {
								     	$(this).removeClass('calendarHoverIn');
								    }								    
								    
								</c:if>
							});
						
					});
					</script>	
					
					<div id="removeShiftModal" class="modal fade bs-remove-shift-modal-sm" tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel" aria-hidden="true">
						<div class="modal-dialog modal-sm" >
							<div class="modal-content">
								<div class="modal-header">
								  <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
								  <h4 class="modal-title">"label.shiftEnrolment.removeShift"</h4>
								</div>
								<div class="modal-body">
									<bean:message bundle="STUDENT_RESOURCES" key="label.shiftEnrolment.removeShift.confirmation" />
								</div>
								<div class="modal-footer">
									<button type="button" class="btn btn-default" data-dismiss="modal"><bean:message bundle="APPLICATION_RESOURCES" key="button.cancel" /></button>	
								</div>
							</div>
						</div>
					</div>										
					
				</c:if>
			</c:if>
			
	  	</c:if>	  	
	  </div>
	</div>

</c:forEach>