/*
 * Created on 2003/07/31
 *
 */
package ServidorApresentacao.Action.sop.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import ServidorApresentacao.Action.utils.ContextUtils;

/**
 * @author Luis Cruz & Sara Ribeiro
 */
public abstract class FenixExecutionCourseAndExecutionDegreeAndCurricularYearContextDispatchAction
	extends FenixExecutionDegreeAndCurricularYearContextDispatchAction {
	/**
	 * Tests if the session is valid.
	 * @see SessionUtils#validSessionVerification(HttpServletRequest, ActionMapping)
	 * @see org.apache.struts.action.Action#execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
	 */
	public ActionForward execute(
		ActionMapping mapping,
		ActionForm actionForm,
		HttpServletRequest request,
		HttpServletResponse response)
		throws Exception {


		ContextUtils.setExecutionCourseContext(request);

		ActionForward actionForward =
			super.execute(mapping, actionForm, request, response);

		return actionForward;
	}

}
