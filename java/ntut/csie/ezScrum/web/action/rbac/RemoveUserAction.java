package ntut.csie.ezScrum.web.action.rbac;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ntut.csie.ezScrum.pic.core.IUserSession;
import ntut.csie.ezScrum.web.dataObject.AccountObject;
import ntut.csie.ezScrum.web.helper.AccountHelper;
import ntut.csie.ezScrum.web.support.SessionManager;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class RemoveUserAction extends Action {
	public ActionForward execute(ActionMapping mapping, ActionForm form, 
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String id = request.getParameter("id");;
		String resource = request.getParameter("resource");
		String operation = request.getParameter("operation");
		
		IUserSession session = (IUserSession) request.getSession().getAttribute("UserSession");
		AccountHelper ah = new AccountHelper(session);
		
		if (id != null && resource != null && operation != null) {
			try {
				AccountObject account = ah.assignRole_remove(id, resource, operation);
				
				// 刪除Session中關於該使用者的所有專案權限。
				SessionManager.removeScrumRolesMap(request, account);
				response.setContentType("text/xml; charset=utf-8");
				response.getWriter().write(ah.getAccountXML(account));				
				response.getWriter().close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
