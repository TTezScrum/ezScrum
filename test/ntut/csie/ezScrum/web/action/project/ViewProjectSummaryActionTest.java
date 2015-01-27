package ntut.csie.ezScrum.web.action.project;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import ntut.csie.ezScrum.issue.sql.service.core.Configuration;
import ntut.csie.ezScrum.pic.core.IUserSession;
import ntut.csie.ezScrum.pic.internal.UserSession;
import ntut.csie.ezScrum.refactoring.manager.ProjectManager;
import ntut.csie.ezScrum.test.TestTool;
import ntut.csie.ezScrum.test.CreateData.AddStoryToSprint;
import ntut.csie.ezScrum.test.CreateData.AddTaskToStory;
import ntut.csie.ezScrum.test.CreateData.AddUserToRole;
import ntut.csie.ezScrum.test.CreateData.CreateAccount;
import ntut.csie.ezScrum.test.CreateData.CreateProductBacklog;
import ntut.csie.ezScrum.test.CreateData.CreateProject;
import ntut.csie.ezScrum.test.CreateData.CreateSprint;
import ntut.csie.ezScrum.test.CreateData.InitialSQL;
import ntut.csie.ezScrum.web.control.TaskBoard;
import ntut.csie.ezScrum.web.dataObject.ProjectObject;
import ntut.csie.ezScrum.web.dataObject.ProjectRole;
import ntut.csie.ezScrum.web.dataObject.AccountObject;
import ntut.csie.ezScrum.web.form.LogonForm;
import ntut.csie.ezScrum.web.mapper.AccountMapper;
import ntut.csie.ezScrum.web.mapper.ProjectMapper;
import ntut.csie.jcis.project.core.IProjectDescription;
import ntut.csie.jcis.resource.core.IProject;
import servletunit.struts.MockStrutsTestCase;

public class ViewProjectSummaryActionTest extends MockStrutsTestCase {
	private Configuration configuration;
	private CreateProject CP;
	private CreateAccount CA;
	private int ProjectCount = 1;
	private int AccountCount = 1;

	public ViewProjectSummaryActionTest(String testMethod) {
		super(testMethod);
	}

	private void setRequestPathInformation(String actionPath) {
		setContextDirectory(new File(configuration.getBaseDirPath() + "/WebContent"));		// 設定讀取的 struts-config 檔案路徑
		setServletConfigFile("/WEB-INF/struts-config.xml");
		setRequestPathInfo(actionPath);
	}

	/**
	 * clean previous action info
	 */
	private void cleanActionInformation() {
		clearRequestParameters();
		this.response.reset();
	}

	private IUserSession getUserSession(AccountObject account) {
		IUserSession userSession = new UserSession(account);
		return userSession;
	}

	protected void setUp() throws Exception {
		configuration = new Configuration();
		configuration.setTestMode(true);
		configuration.save();
		
		//	刪除資料庫
		InitialSQL ini = new InitialSQL(configuration);
		ini.exe();

		// 新增Project
		this.CP = new CreateProject(this.ProjectCount);
		this.CP.exeCreate();

		// 新增使用者
		this.CA = new CreateAccount(this.AccountCount);
		this.CA.exe();

		super.setUp();
		// ============= release ==============
		ini = null;
	}

	protected void tearDown() throws Exception {
		//	刪除資料庫
		InitialSQL ini = new InitialSQL(configuration);
		ini.exe();

		//	刪除外部檔案
		ProjectManager projectManager = new ProjectManager();
		projectManager.deleteAllProject();
		projectManager.initialRoleBase(configuration.getDataPath());
		
		configuration.setTestMode(false);
		configuration.save();

		super.tearDown();

		ini = null;
		projectManager = null;
		configuration = null;
	}

	/**
	 * 1. admin 建立專案
	 * 2. admin 瀏覽專案
	 */
	public void testAdminViewProjectSummary() {
		/**
		 * 1. admin 建立專案
		 */
		// ================ set action info ========================
		String actionPath_createProject = "/AjaxCreateProject";
		setRequestPathInformation(actionPath_createProject);

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ set request info ========================
		//	設定專案資訊
		String projectID = "test";
		String projectDisplayName = "Project for Test Create Project";
		String comment = "";
		String projectManager = "ezScrum tester";
		String attachFileSize = "";
		addRequestParameter("Name", projectID);
		addRequestParameter("DisplayName", projectDisplayName);
		addRequestParameter("Comment", comment);
		addRequestParameter("ProjectManager", projectManager);
		addRequestParameter("AttachFileSize", attachFileSize);
		addRequestParameter("from", "createProject");

		//	設定ITS參數資料
		addRequestParameter("ServerUrl", configuration.getServerUrl());
		addRequestParameter("ServicePath", configuration.getWebServicePath());
		addRequestParameter("DBAccount", configuration.getDBAccount());
		addRequestParameter("DBPassword", configuration.getDBPassword());
		addRequestParameter("SQLType", configuration.getDBType());
		addRequestParameter("DBName", configuration.getDBName());

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		//	assert response text
		StringBuilder expectedResponseText = new StringBuilder();
		expectedResponseText.append("<Root>")
		        .append("<CreateProjectResult>")
		        .append("<Result>Success</Result>")
		        .append("<ID>test</ID>")
		        .append("</CreateProjectResult>")
		        .append("</Root>");

		String actualResponseText = response.getWriterBuffer().toString();
		assertEquals(expectedResponseText.toString(), actualResponseText);

		//	assert database information
		ProjectMapper projectMapper = new ProjectMapper();
		IProject project = projectMapper.getProjectByID(projectID);
		IProjectDescription projectDesc = project.getProjectDesc();
		assertEquals(projectID, projectDesc.getName());
		assertEquals(projectDisplayName, projectDesc.getDisplayName());
		assertEquals("2", projectDesc.getAttachFileSize());
		assertEquals(comment, projectDesc.getComment());
		assertEquals(projectManager, projectDesc.getProjectManager());

		//	assert 外部檔案路徑及檔名
		//		IWorkspace workspace = ResourceFacade.getWorkspace();
		//		IWorkspaceRoot root = workspace.getRoot();
		//    	IProject Actual = root.getProject(projectID);
		//    	
		//    	assertEquals(Actual.getName(), project.getName());
		//    	assertEquals(Actual.getFullPath(), project.getFullPath());

		/**
		 * 2. admin 瀏覽專案
		 */
		cleanActionInformation();
		String pathViewProjectSummary = "/viewProject";
		setRequestPathInformation(pathViewProjectSummary);

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ set request info ========================
		addRequestParameter("PID", projectID);

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("SummaryView");					//	define in ViewProjectSummaryAction.java
		verifyForwardPath("/Pages/ezScrumContent.jsp");	//	define in tiles-defs.xml

		String expectIsGuest = "false";
		String actualIsGuest = (String) request.getSession().getAttribute("isGuest");
		assertEquals(expectIsGuest, actualIsGuest);

		verifyNoActionMessages();
		verifyNoActionErrors();
	}

	/**
	 * Integration Test
	 * Steps
	 * 	1. admin 新增專案 (setup done) 
	 * 	2. admin 新增帳號 (setup done)
	 * 	3. admin assign this account to the project
	 * 	4. user login ezScrum
	 * 	5. user view project list
	 * 	6. user select project
	 * @throws Exception 
	 */
	public void testUserViewProjectSummary() throws Exception {
		//	=============== common data ============================
		AccountObject account = this.CA.getAccountList().get(0);
		IUserSession userSession = getUserSession(account);
		String userId = account.getId();		// 取得第一筆 Account ID
		String projectId = this.CP.getProjects().get(0).getId();
		String pid = this.CP.getProjects().get(0).getName();

		/**
		 * 3. admin assign this account to the project
		 */

		// ================ set action info ========================
		String actionPath_AddUser = "/addUser";	// defined in "struts-config.xml"
		setRequestPathInformation(actionPath_AddUser);

		// ================ set initial data =======================
		String scrumRole = "ScrumTeam";

		// ================== set parameter info ====================
		addRequestParameter("id", userId);
		addRequestParameter("resource", projectId);
		addRequestParameter("operation", scrumRole);

		// ================ set session info with admin ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ set URL parameter ========================    	
		request.setHeader("Referer", "?PID=" + pid);	// SessionManager 會對URL的參數作分析 ,未帶入此參數無法存入session

		// ================ 執行 add user action ======================
		actionPerform();

		// ================ assert ========================
		//	assert response text
		String expectedUserRole_ST = (new TestTool()).getRole(pid, scrumRole);
		//		String expectedUserRole_USER = "user";
		String expectedUserId = account.getId();
		String expectedUserAccount = account.getUsername();
		String expectedUserName = account.getNickName();
		String expectedUserPassword = (new TestTool()).getMd5(this.CA.getAccount_PWD(1));
		String expectedUserMail = account.getEmail();
		String expectedUserEnable = account.getEnable();
		//    	account.addRole(new Role(expectedUserRole_ST, expectedUserRole_ST));
		//    	HashMap<String, ProjectRole> roleArr = account.getRoles();
		//    	String expectedUserRole = roleArr[0].getRoleId() + ", " + roleArr[1].getRoleId();
		StringBuilder addUserExpectedResponseText = new StringBuilder();
		addUserExpectedResponseText.append("<Accounts>")
		        .append("<AccountInfo>")
		        .append("<ID>").append(expectedUserId).append("</ID>")
		        .append("<Account>").append(expectedUserAccount).append("</Account>")
		        .append("<Name>").append(expectedUserName).append("</Name>")
		        .append("<Mail>").append(expectedUserMail).append("</Mail>")
		        .append("<Roles>").append(expectedUserRole_ST).append("</Roles>")
		        .append("<Enable>").append(expectedUserEnable).append("</Enable>")
		        .append("</AccountInfo>")
		        .append("</Accounts>");

		String addUserActualResponseText = this.response.getWriterBuffer().toString();
		assertEquals(addUserExpectedResponseText.toString(), addUserActualResponseText);

		//	assert database information
		AccountObject actualAccount = new AccountMapper().getAccount(userId);
		assertNotNull(account);
		assertEquals(expectedUserId, actualAccount.getId());
		assertEquals(expectedUserAccount, actualAccount.getUsername());
		assertEquals(expectedUserName, actualAccount.getNickName());
		assertEquals(expectedUserPassword, actualAccount.getPassword());
		assertEquals(expectedUserMail, actualAccount.getEmail());
		assertEquals(expectedUserEnable, actualAccount.getEnable());

		// 測試 Role 是否正確
		HashMap<String, ProjectRole> roleMap = actualAccount.getRoles();
		boolean isExisted = false;
		for (Entry<String, ProjectRole> role : roleMap.entrySet()) {
			if (scrumRole.equals(role.getValue().getScrumRole().getRoleName())) {
				isExisted = true;
				break;
			}
		}
		assertEquals(roleMap.size(), 1);	// ScrumTeam
		assertTrue(isExisted);				// ScrumTeam
		//		String[] userRole = {expectedUserRole_USER, expectedUserRole_ST};
		//		for (String roleID : userRole) {
		//			boolean isExisted = false;
		//			for (IRole role : roleArr) {
		//				if (roleID.equals(role.getRoleId())) {
		//					isExisted = true;
		//					break;
		//				}
		//			}
		//			assertEquals(true, isExisted);
		//		}
		//		IRole[] roles = account.getRoles();
		//		assertEquals(roleMap.size(), 1);		//	include ProductOwner

		/**
		 * 4. user login ezScrum
		 */
		// ================ clean previous action info ========================
		cleanActionInformation();

		// ================ set action info ========================
		String actionPath_logonSubmit = "/logonSubmit";
		setRequestPathInformation(actionPath_logonSubmit);

		// ================== set parameter info ====================
		String loginUserID = account.getUsername();
		String loginUserPassword = this.CA.getAccount_PWD(1);
		LogonForm logonForm = new LogonForm();
		logonForm.setUserId(loginUserID);
		logonForm.setPassword(loginUserPassword);
		setActionForm(logonForm);

		// ================ 執行 login action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("success");

		/**
		 * 5. view project list
		 */
		userSession = getUserSession(new AccountMapper().getAccount(userId));
		// ================ clean previous action info ========================
		cleanActionInformation();
		ProjectObject projectObject = this.CP.getProjects().get(0);
		IProject project = this.CP.getProjectList().get(0);

		// ================ set action info ========================
		String actionPath_viewProjectList = "/viewProjectList";
		setRequestPathInformation(actionPath_viewProjectList);

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", userSession);

		// ================ 執行 view project list action ======================
		actionPerform();

		// ================ assert ========================
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String expectedProjectID = projectObject.getId();
		String expectedProjectName = projectObject.getName();
		String expectedProjectDisplayName = projectObject.getDisplayName();
		String expectedProjectComment = projectObject.getComment();
		String expectedProjectManager = projectObject.getManager();
		String expectedProjectCreateDate = dateFormat.format(projectObject.getCreateTime());
		String expectedProjectDemoDate = "No Plan!";
		//	assert response text
		StringBuilder viewProjectListExpectedResponseText = new StringBuilder();
		viewProjectListExpectedResponseText.append("<Projects>")
		        .append("<Project>")
		        .append("<ID>").append(expectedProjectName).append("</ID>")
		        .append("<Name>").append(expectedProjectDisplayName).append("</Name>")
		        .append("<Comment>").append(expectedProjectComment).append("</Comment>")
		        .append("<ProjectManager>").append(expectedProjectManager).append("</ProjectManager>")
		        .append("<CreateDate>").append(expectedProjectCreateDate).append("</CreateDate>")
		        .append("<DemoDate>").append(expectedProjectDemoDate).append("</DemoDate>")
		        .append("</Project>")
		        .append("</Projects>");

		String viewProjectListActualResponseText = this.response.getWriterBuffer().toString();
		assertEquals(viewProjectListExpectedResponseText.toString(), viewProjectListActualResponseText);

		/**
		 * 6.1 user select project - get Project Description
		 */
		// ================ clean previous action info ========================
		cleanActionInformation();

		// ================ set action info ========================
		String ActionPath_GetProjectDescription = "/GetProjectDescription";
		setRequestPathInfo(ActionPath_GetProjectDescription);

		// ================ set URL parameter ========================
		request.setHeader("Referer", "?PID=" + pid);	// SessionManager 會對URL的參數作分析 ,未帶入此參數無法存入session

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		//	assert response text
		dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		String expectAttachFileSize = "2";
		String expectProjectCreateDate = dateFormat.format(projectObject.getCreateTime());
		StringBuilder expectedResponseText = new StringBuilder();
		expectedResponseText.append("{")
		        .append("\"ID\":\"").append(expectedProjectID).append("\",")
		        .append("\"ProjectName\":\"").append(expectedProjectName).append("\",")
		        .append("\"ProjectDisplayName\":\"").append(expectedProjectDisplayName).append("\",")
		        .append("\"Commnet\":\"").append(expectedProjectComment).append("\",")
		        .append("\"ProjectManager\":\"").append(expectedProjectManager).append("\",")
		        .append("\"AttachFileSize\":\"").append(expectAttachFileSize).append("\",")
		        .append("\"ProjectCreateDate\":\"").append(expectProjectCreateDate).append("\"")
		        .append("}");

		String actualResponseText = response.getWriterBuffer().toString();
		assertEquals(expectedResponseText.toString(), actualResponseText);

		/**
		 * 6.2 user select project - get Sprint Description
		 */
		// ================ clean previous action info ========================
		cleanActionInformation();

		// ================ set action info ========================
		String actionPath_GetTaskBoardDescription = "/GetTaskBoardDescription";
		setRequestPathInfo(actionPath_GetTaskBoardDescription);

		// ================ set URL parameter ========================
		request.setHeader("Referer", "?PID=" + pid);	// SessionManager 會對URL的參數作分析 ,未帶入此參數無法存入session

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		//	assert response text
		expectedResponseText.setLength(0);
		expectedResponseText.append("{\"ID\":\"0\",")
		        .append("\"SprintGoal\":\"\",")
		        .append("\"Current_Story_Undone_Total_Point\":\"\",")
		        .append("\"Current_Task_Undone_Total_Point\":\"\"}");

		actualResponseText = response.getWriterBuffer().toString();
		assertEquals(expectedResponseText.toString(), actualResponseText);

		/**
		 * 6.3 user select project - get Story Burndown Chat
		 */

		// ================ clean previous action info ========================
		cleanActionInformation();

		int sprintCount = 1;
		int storyCount = 2;
		int taskCount = 2;
		int storyEstValue = 8;
		int taskEstValue = 3;
		CreateSprint createSprint = new CreateSprint(sprintCount, this.CP);
		createSprint.exe();
		AddStoryToSprint addStoryToSprint = new AddStoryToSprint(storyCount, storyEstValue, createSprint, this.CP, CreateProductBacklog.TYPE_ESTIMATION);
		addStoryToSprint.exe();
		AddTaskToStory addTaskToStory = new AddTaskToStory(taskCount, taskEstValue, addStoryToSprint, CP);
		addTaskToStory.exe();
		String ActionPath_GetSprintBurndownChartData = "/getSprintBurndownChartData";
		setRequestPathInfo(ActionPath_GetSprintBurndownChartData);

		// ================ set URL parameter ========================
		request.setHeader("Referer", "?PID=" + pid);	// SessionManager 會對URL的參數作分析 ,未帶入此參數無法存入session

		// ================ set request info ========================
		addRequestParameter("SprintID", "-1");	//	-1:代表離現在時間最近的Sprint
		addRequestParameter("Type", "story");

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", this.getUserSession(account));

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyNoActionErrors();
		verifyNoActionMessages();

		//	assert response text
		TestTool tool = new TestTool();
		List<String> sprintDateList = tool.getSprintDate(project, this.getUserSession(account));
		//	減一代表Sprint開始的第一天是SprintPlanning所以第一天不工作，因此總工作天必須減一。
		int workDateCount = sprintDateList.size() - 1;
		List<String> storyIdealLinePoints = tool.getStoryIdealLinePoint(workDateCount, 16.0);
		expectedResponseText.setLength(0);
		;
		expectedResponseText.append("{\"success\":true,")
		        .append("\"Points\":[");
		for (int i = 0; i <= workDateCount; i++) {
			expectedResponseText.append("{")
			        .append("\"Date\":\"" + sprintDateList.get(i) + "\",")
			        .append("\"IdealPoint\":").append(storyIdealLinePoints.get(i)).append(",")
			        .append("\"RealPoint\":");
			if (i == 0) {
				expectedResponseText.append("16.0},");
			} else {
				expectedResponseText.append("\"null\"},");
			}
		}
		expectedResponseText.deleteCharAt(expectedResponseText.length() - 1);
		expectedResponseText.append("]}");

		actualResponseText = response.getWriterBuffer().toString();
		assertEquals(expectedResponseText.toString(), actualResponseText);

		/**
		 * 6.4 user select project - get Task Burndown Chat
		 */
		// ================ clean previous action info ========================
		cleanActionInformation();
		ActionPath_GetSprintBurndownChartData = "/getSprintBurndownChartData";
		setRequestPathInfo(ActionPath_GetSprintBurndownChartData);

		// ================ set URL parameter ========================
		request.setHeader("Referer", "?PID=" + pid);	// SessionManager 會對URL的參數作分析 ,未帶入此參數無法存入session

		// ================ set request info ========================
		addRequestParameter("SprintID", "-1");	//	-1:代表離現在時間最近的Sprint
		addRequestParameter("Type", "task");

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", this.getUserSession(account));

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyNoActionErrors();
		verifyNoActionMessages();

		//	assert response text
		sprintDateList = tool.getSprintDate(project, this.getUserSession(account));
		// 減一代表Sprint開始的第一天是SprintPlanning所以第一天不工作，因此總工作天必須減一。
		workDateCount = sprintDateList.size() - 1;	//	
		List<String> taskIdealLinePoints = tool.getTaskIdealLinePoint(workDateCount, 12.0);
		expectedResponseText.setLength(0);
		;
		expectedResponseText.append("{\"success\":true,")
		        .append("\"Points\":[");
		for (int i = 0; i <= workDateCount; i++) {
			expectedResponseText.append("{")
			        .append("\"Date\":\"" + sprintDateList.get(i) + "\",")
			        .append("\"IdealPoint\":").append(taskIdealLinePoints.get(i)).append(",")
			        .append("\"RealPoint\":");
			if (i == 0) {
				expectedResponseText.append("12.0},");
			} else {
				expectedResponseText.append("\"null\"},");
			}
		}
		expectedResponseText.deleteCharAt(expectedResponseText.length() - 1);
		expectedResponseText.append("]}");

		actualResponseText = response.getWriterBuffer().toString();
		assertEquals(expectedResponseText.toString(), actualResponseText);

		/**
		 * 6.5 user select project
		 */
		// ================ clean previous action info ========================
		cleanActionInformation();

		// ================ set action info ========================
		String actionPath_viewProject = "/viewProject";
		setRequestPathInformation(actionPath_viewProject);

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", userSession);

		// ================== set parameter info ====================
		addRequestParameter("PID", pid);

		// ================ 執行 view project action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("SummaryView");					//	define in ViewProjectSummaryAction.java
		verifyForwardPath("/Pages/ezScrumContent.jsp");	//	define in tiles-defs.xml
		String expectIsGuest = "false";
		String actualIsGuest = (String) request.getSession().getAttribute("isGuest");
		assertEquals(expectIsGuest, actualIsGuest);
		int expectedSprintID = 1;
		String expectedSprintGoal = "TEST_SPRINTGOAL_1";
		String expectedSprint_Current_Story_Undone_Total_Point = "16.0 / 16.0";
		String expectedSprint_Current_Task_Undone_Total_Point = "12.0 / 12.0";
		TaskBoard taskBoard = (TaskBoard) request.getAttribute("TaskBoard");
		assertEquals(expectedSprintID, taskBoard.getSprintID());
		assertEquals(expectedSprintGoal, taskBoard.getSprintGoal());
		assertEquals(expectedSprint_Current_Story_Undone_Total_Point, taskBoard.getStoryPoint());
		assertEquals(expectedSprint_Current_Task_Undone_Total_Point, taskBoard.getTaskPoint());
		verifyNoActionMessages();
		verifyNoActionErrors();
	}

	/**
	 * 比對資料庫中是否存在此專案的PID
	 * 1. assert 不存在
	 * 2. assert 存在
	 */
	public void testPIDIsExisted() {
		String pathViewProjectSummary = "/viewProject";

		/**
		 * project ID does not existed 
		 */
		String notexistedProjectID = "testNotExisted";
		setRequestPathInformation(pathViewProjectSummary);

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ set request info ========================
		addRequestParameter("PID", notexistedProjectID);

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("error");					//	define in ViewProjectSummaryAction.java
		verifyForwardPath("/Error.jsp");	//	define in tiles-defs.xml

		verifyNoActionMessages();
		verifyNoActionErrors();

		/**
		 * project ID existed
		 */
		this.cleanActionInformation();
		String existedProjectID = "TEST_PROJECT_1";
		setRequestPathInformation(pathViewProjectSummary);

		// ================ set session info ========================
		request.getSession().setAttribute("UserSession", configuration.getUserSession());

		// ================ set request info ========================
		addRequestParameter("PID", existedProjectID);

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("SummaryView");					//	define in ViewProjectSummaryAction.java
		verifyForwardPath("/Pages/ezScrumContent.jsp");	//	define in tiles-defs.xml

		String expectIsGuest = "false";
		String actualIsGuest = (String) request.getSession().getAttribute("isGuest");
		assertEquals(expectIsGuest, actualIsGuest);

		verifyNoActionMessages();
		verifyNoActionErrors();
	}

	/**
	 * 判斷該使用者是否存在於專案中
	 * 1. assert user(ScrumTeam) 不存在於專案
	 * 2. assert user(ScrumTeam) 存在於專案
	 */
	public void testUserIsInProject() {
		String pathViewProjectSummary = "/viewProject";
		String projectID = "TEST_PROJECT_1";

		/**
		 * Permission Denied
		 */
		setRequestPathInformation(pathViewProjectSummary);

		// ================ set session info ========================
		IUserSession userSession = this.getUserSession(this.CA.getAccountList().get(0));
		request.getSession().setAttribute("UserSession", userSession);

		// ================ set request info ========================
		addRequestParameter("PID", projectID);

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("permissionDenied");					//	define in ViewProjectSummaryAction.java
		verifyForwardPath("/PermissionDenied.jsp");	//	define in tiles-defs.xml

		verifyNoActionMessages();
		verifyNoActionErrors();

		/**
		 * user(ScrumTeam) 存在於專案
		 */
		this.cleanActionInformation();
		AddUserToRole addUserToRole = new AddUserToRole(this.CP, this.CA);
		addUserToRole.exe_SM();

		setRequestPathInformation(pathViewProjectSummary);

		// ================ set session info ========================
		AccountObject account = new AccountMapper().getAccount(CA.getAccountList().get(0).getUsername());
		userSession = getUserSession(account);
		request.getSession().setAttribute("UserSession", userSession);

		// ================ set request info ========================
		addRequestParameter("PID", projectID);

		// ================ 執行 action ======================
		actionPerform();

		// ================ assert ======================
		verifyForward("SummaryView");					//	define in ViewProjectSummaryAction.java
		verifyForwardPath("/Pages/ezScrumContent.jsp");	//	define in tiles-defs.xml

		String expectIsGuest = "false";
		String actualIsGuest = (String) request.getSession().getAttribute("isGuest");
		assertEquals(expectIsGuest, actualIsGuest);

		verifyNoActionMessages();
		verifyNoActionErrors();
	}
}
