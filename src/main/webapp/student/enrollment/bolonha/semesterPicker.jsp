<style>
		.semesterPicker div{
			display: inline-block;
		}
		.semesterPicker .semesters{
			margin-left : 20px;
		}
		
		.semesterPicker .finish{
			margin-left : 20px;
		}
	</style>
<div class="semesterPicker">
			<div>
				<p class="mtop15 mbottom025">
					<bean:message bundle="APPLICATION_RESOURCES"  key="label.saveChanges.message"/>:
				</p> 
				<p class="mtop025 mbottom1">
					<html:submit styleClass="enrollSaveButton" bundle="HTMLALT_RESOURCES" altKey="submit.submit" onclick="this.form.method.value='enrolInDegreeModules';$('.enrollSaveButton').attr('disabled', true);this.form.submit();"><bean:message bundle="APPLICATION_RESOURCES"  key="label.save"/></html:submit>
				</p>
			</div>
			 <logic:present name="openedEnrolmentPeriodsSemesters">
				<div class="semesters">
					<p class="mtop15 mbottom025">
						<bean:message bundle="STUDENT_RESOURCES"  key="label.semester"/>:
					</p>
					<p class="mtop025 mbottom1">
					
					<logic:iterate id="period" name="openedEnrolmentPeriodsSemesters">
							<logic:equal name="bolonhaStudentEnrollmentBean" property="executionPeriod.externalId" value="${period.externalId}">
								<span class="btn btn-default" disabled="disabled">${period.qualifiedName}</span>
							</logic:equal>
							<logic:notEqual name="bolonhaStudentEnrollmentBean" property="executionPeriod.externalId" value="${period.externalId}">
								<html:link onclick="return checkState()" action="/bolonhaStudentEnrollment.do?method=prepare&registrationOid=${bolonhaStudentEnrollmentBean.registration.externalId}&executionSemesterID=${period.externalId}" styleClass="btn btn-default">
									${period.qualifiedName}
								</html:link>
							</logic:notEqual>
					</logic:iterate>
					</p>
				</div>
			</logic:present>
				<div class="finish">
				<p class="mtop15 mbottom025">
						
					</p>
					<p class="mtop025 mbottom1">
						<logic:present name="returnURL">
							<a onclick="return checkState()" href="${returnURL}" class="btn btn-default"><bean:message bundle="STUDENT_RESOURCES" key="link.shift.enrollment.item3" /></a>
						</logic:present>
					</p>
					</div>
		</div>
		
		
<script  type="text/javascript">
function checkState(){
	if(changedState){
		result = window.confirm("<bean:message bundle="STUDENT_RESOURCES"  key="label.changeSemesterWithoutSave"/>");
		if(!result){
			return false;
		}
	}
	return true;
}

$(":checkbox").on("change", function(){
	changedState = true;
});
</script>