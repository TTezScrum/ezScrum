package ntut.csie.ezScrum.web.action.backlog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ntut.csie.ezScrum.web.action.PermissionAction;
import ntut.csie.ezScrum.web.dataObject.ProjectObject;
import ntut.csie.ezScrum.web.dataObject.SprintObject;
import ntut.csie.ezScrum.web.dataObject.TaskObject;
import ntut.csie.ezScrum.web.helper.SprintBacklogHelper;
import ntut.csie.ezScrum.web.support.SessionManager;
import ntut.csie.ezScrum.web.support.TranslateSpecialChar;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class GetEditTaskInfoAction extends PermissionAction {

	@Override
	public boolean isValidAction() {
		return (super.getScrumRole().getAccessSprintBacklog() && 
				super.getScrumRole().getAccessTaskBoard());
	}

	@Override
	public boolean isXML() {
		// XML
		return true;
	}

	@Override
	public StringBuilder getResponse(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) {
		
		// get session info
		ProjectObject project = SessionManager.getProject(request);
		
		// get parameter info
		long serialSprintId;
		
		String serialSprintIdString = request.getParameter("sprintID");
		
		if (serialSprintIdString == null || serialSprintIdString.length() == 0) {
			serialSprintId = -1;
		} else {
			serialSprintId = Long.parseLong(serialSprintIdString);
		}
		
		long serialTaskId = Long.parseLong(request.getParameter("issueID"));
		
		// Get Sprint
		SprintObject sprint = SprintObject.get(project.getId(), serialSprintId);
		long sprintId = -1;
		if (sprint != null) {
			sprintId = sprint.getId();
		}
		SprintBacklogHelper sprintBacklogHelper = new SprintBacklogHelper(project, sprintId);
		
		StringBuilder result = new StringBuilder();
		TaskObject task = sprintBacklogHelper.getTask(project.getId(), serialTaskId);
		
		String handlerUsername = task.getHandler() != null ? task.getHandler().getUsername() : "";

		result.append("<EditTask><Task>");
		result.append("<Id>").append(task.getSerialId()).append("</Id>");
		result.append("<Name>").append(TranslateSpecialChar.TranslateXMLChar(task.getName())).append("</Name>");
		result.append("<Status>").append(task.getStatusString()).append("</Status>");
		result.append("<Estimate>").append(task.getEstimate()).append("</Estimate>");
		result.append("<Actual>").append(task.getActual()).append("</Actual>");
		result.append("<Handler>").append(TranslateSpecialChar.TranslateXMLChar(handlerUsername)).append("</Handler>");
		result.append("<Remains>").append(task.getRemains()).append("</Remains>");
		result.append("<Partners>").append(TranslateSpecialChar.TranslateXMLChar(task.getPartnersUsername())).append("</Partners>");
		result.append("<Notes>").append(TranslateSpecialChar.TranslateXMLChar(task.getNotes())).append("</Notes>");
		result.append("</Task></EditTask>");
		
		return result;
	}
}