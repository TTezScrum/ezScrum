package ntut.csie.ezScrum.restful.mobile.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import ntut.csie.ezScrum.issue.core.IIssue;
import ntut.csie.ezScrum.pic.internal.UserSession;
import ntut.csie.ezScrum.web.dataObject.TaskObject;
import ntut.csie.ezScrum.web.dataObject.AccountObject;
import ntut.csie.ezScrum.web.helper.ProductBacklogHelper;
import ntut.csie.ezScrum.web.helper.SprintBacklogHelper;
import ntut.csie.jcis.account.core.LogonException;

public class TaskWebService extends ProjectWebService{
	SprintBacklogHelper mSprintBacklogHelper;
	ProductBacklogHelper mProductBacklogHelper;
	
	public TaskWebService(AccountObject user, String projectId) throws LogonException {
		super(user, projectId);
		initialize();
	}
	
	public TaskWebService(String username, String userpwd, String projectId) throws LogonException {
		super(username, userpwd, projectId);
		initialize();
	}
	
	private void initialize() {
		UserSession userSession = new UserSession(super.getAccount());
		mSprintBacklogHelper = new SprintBacklogHelper(super.getProjectList().get(0), userSession);
		mProductBacklogHelper = new ProductBacklogHelper(userSession, super.getProjectList().get(0));
	}
	
	/**
	 * 取得可以被加到 story 的已存在 task
	 * @return
	 * @throws SQLException 
	 */
	public String getExistedTask() throws SQLException {
		IIssue[] existedTask = mProductBacklogHelper.getWildedTasks();
		List<TaskObject> existedTaskList = new ArrayList<TaskObject>();
		for (IIssue task : existedTask)
			existedTaskList.add(new TaskObject(task));
		Gson gson = new Gson();
		return gson.toJson(existedTaskList);
	}
	
	/**
	 * 在 story 中加入新的 task
	 * @param storyId
	 * @param task
	 * @return
	 */
	public String createTaskInStory(String storyId, TaskObject task) {
		return Long.toString(mSprintBacklogHelper.createTaskInStory(storyId, task).getIssueID());
	}
	
	/**
	 * 刪除 task
	 * @param taskId
	 * @return
	 */
	public void deleteTask(String taskId, String storyId) {
		mSprintBacklogHelper.deleteTask(taskId, storyId);
	}
	
	/**
	 * 將 task 從 story 中移除
	 * @param taskId
	 * @param storyId
	 */
	public void dropTask(String taskId, String storyId) {
		mSprintBacklogHelper.dropTask(taskId, storyId);
	}
	
	/**
	 * 編輯 task
	 * @param taskJson
	 * @return
	 */
	public String updateTask(String taskJson) {
		Gson gson = new Gson();
		TaskObject task = gson.fromJson(taskJson, TaskObject.class);
		return Boolean.toString(mSprintBacklogHelper.editTask(task));
	}
}
