package ntut.csie.ezScrum.web.action.export;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ntut.csie.ezScrum.iteration.core.IReleasePlanDesc;
import ntut.csie.ezScrum.web.helper.ReleasePlanHelper;
import ntut.csie.ezScrum.web.support.SessionManager;
import ntut.csie.jcis.resource.core.IProject;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class AjaxGetReleasePlanAction extends Action {
	/*
	 * @Override public boolean isValidAction() { return super.getScrumRole().getAccessExport(); }
	 * 
	 * @Override public boolean isXML() { return false; }
	 */
	public ActionForward execute(ActionMapping mapping, ActionForm form,
	        HttpServletRequest request, HttpServletResponse response) {
		StringBuilder result = getResponse(mapping, form, request, response);
		try {
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(result.toString());
			response.getWriter().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public StringBuilder getResponse(ActionMapping mapping, ActionForm form,
	        HttpServletRequest request, HttpServletResponse response) {
		// get session info
		IProject project = (IProject) SessionManager.getProject(request);

		ReleasePlanHelper RPhelper = new ReleasePlanHelper(project);
		List<IReleasePlanDesc> releaseDescs = RPhelper.loadReleasePlansList();
		StringBuilder result = new StringBuilder(RPhelper.setReleaseListToJSon(releaseDescs));
		return result;
	}
}
