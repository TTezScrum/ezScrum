package ntut.csie.ezScrum.web.dataObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ntut.csie.ezScrum.dao.TaskDAO;
import ntut.csie.ezScrum.issue.sql.service.core.Configuration;
import ntut.csie.ezScrum.issue.sql.service.core.InitialSQL;
import ntut.csie.ezScrum.test.CreateData.CreateProject;
import ntut.csie.ezScrum.web.databaseEnum.AccountEnum;
import ntut.csie.ezScrum.web.databaseEnum.IssueTypeEnum;
import ntut.csie.ezScrum.web.databaseEnum.TaskEnum;
import ntut.csie.jcis.core.util.DateUtil;

public class TaskObjectTest {
	private Configuration mConfig = null;
	private CreateProject mCP = null;
	private final static int mPROJECT_COUNT = 1;
	private long mProjectId = -1;

	@Before
	public void setUp() throws Exception {
		mConfig = new Configuration();
		mConfig.setTestMode(true);
		mConfig.save();

		InitialSQL ini = new InitialSQL(mConfig);
		ini.exe();

		mCP = new CreateProject(mPROJECT_COUNT);
		mCP.exeCreateForDb();
		
		mProjectId = mCP.getAllProjects().get(0).getId();
	}

	@After
	public void tearDown() throws Exception {
		InitialSQL ini = new InitialSQL(mConfig);
		ini.exe();

		// 讓 config 回到  Production 模式
		mConfig.setTestMode(false);
		mConfig.save();
		
		mConfig = null;
		mCP = null;
	}

	@Test
	public void testSetPartnersId() {
		long TEST_TASK_ID = 1;
		long projectId = mCP.getAllProjects().get(0).getId();
		// Create account
		AccountObject account1 = new AccountObject("account1"); // handler
		account1.save();
		AccountObject account2 = new AccountObject("account2"); // old partner1
		account2.save();
		AccountObject account3 = new AccountObject("account3"); // old partner2, new partner1
		account3.save();
		AccountObject account4 = new AccountObject("account4"); // new partner2
		account4.save();
		ArrayList<Long> oldPartnersId = new ArrayList<>();
		oldPartnersId.add(account2.getId());
		oldPartnersId.add(account3.getId());
		ArrayList<Long> newPartnersId = new ArrayList<>();
		newPartnersId.add(account3.getId());
		newPartnersId.add(account4.getId());
		// create a task
		TaskObject task = new TaskObject(projectId);
		task.setName("TEST_NAME").setHandlerId(account1.getId()).setPartnersId(oldPartnersId).save();
		// before testSetPartnersId
		ArrayList<Long> oldPartnersFromDAO = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(2, oldPartnersFromDAO.size());
		assertEquals(account2.getId(), oldPartnersFromDAO.get(0));
		assertEquals(account3.getId(), oldPartnersFromDAO.get(1));
		// set new partners
		task.setPartnersId(newPartnersId).save();
		// test partners
		ArrayList<Long> newPartnersFromDAO = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(2, newPartnersFromDAO.size());
		assertEquals(account3.getId(), newPartnersFromDAO.get(0));
		assertEquals(account4.getId(), newPartnersFromDAO.get(1));
		// test partners history
		ArrayList<HistoryObject> histories = task.getHistories();
		assertEquals(3, histories.size());
		assertEquals(HistoryObject.TYPE_REMOVE_PARTNER, histories.get(1).getHistoryType());
		assertEquals(IssueTypeEnum.TYPE_TASK, histories.get(1).getIssueType());
		assertEquals(task.getId(), histories.get(1).getIssueId());
		assertEquals(account2.getId(), Long.parseLong(histories.get(1).getNewValue())); // account2 remove from task
		assertEquals("Remove Partner", histories.get(1).getHistoryTypeString());
		assertEquals(account2.getUsername(), histories.get(1).getDescription());
		assertEquals(HistoryObject.TYPE_ADD_PARTNER, histories.get(2).getHistoryType());
		assertEquals(IssueTypeEnum.TYPE_TASK, histories.get(2).getIssueType());
		assertEquals(task.getId(), histories.get(2).getIssueId());
		assertEquals(account4.getId(), Long.parseLong(histories.get(2).getNewValue())); // add account4 to task
		assertEquals("Add Partner", histories.get(2).getHistoryTypeString());
		assertEquals(account4.getUsername(), histories.get(2).getDescription());
	}

	@Test
	public void testSetPartnersId_WithTwoPartners() {
		long TEST_TASK_ID = 1;
		// Create account
		AccountObject account1 = new AccountObject("account1");
		account1.save();
		AccountObject account2 = new AccountObject("account2");
		account2.save();
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").save();
		// before testSetPartnersId
		List<Long> partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(0, partnersId.size());
		// set two partners
		ArrayList<Long> testPartnersId = new ArrayList<Long>();
		testPartnersId.add(account1.getId());
		testPartnersId.add(account2.getId());
		task.setPartnersId(testPartnersId).save();
		// testSetPartnersId_withTwoPartners
		partnersId.clear();
		partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(2, partnersId.size());
		assertEquals(account1.getId(), partnersId.get(0));
		assertEquals(account2.getId(), partnersId.get(1));
	}

	@Test
	public void testSetPartnersId_WithTwoPartnersAddOneAndRemoveOnePartner() {
		long TEST_TASK_ID = 1;
		// Create account
		AccountObject account1 = new AccountObject("account1");
		account1.save();
		AccountObject account2 = new AccountObject("account2");
		account2.save();
		AccountObject account3 = new AccountObject("account3");
		account3.save();
		// set two partners
		ArrayList<Long> oldPartnersId = new ArrayList<Long>();
		oldPartnersId.add(account1.getId());
		oldPartnersId.add(account2.getId());
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setPartnersId(oldPartnersId).save();
		// testSetPartnersId_withTwoPartnersAddOneAndRemoveOnePartner
		ArrayList<Long> newPartnersId = new ArrayList<Long>();
		newPartnersId.add(account2.getId());
		newPartnersId.add(account3.getId());
		task.setPartnersId(newPartnersId).save();
		List<Long> partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		partnersId.clear();
		partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(2, partnersId.size());
		assertEquals(account2.getId(), partnersId.get(0));
		assertEquals(account3.getId(), partnersId.get(1));
	}

	@Test
	public void testAddPartner() {
		long TEST_TASK_ID = 1;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check status before add partner
		List<Long> partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(0, partnersId.size());
		// testAddPartner
		task.addPartner(1);
		partnersId.clear();
		partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(1, partnersId.size());
		assertEquals(1L, partnersId.get(0));
	}

	@Test
	public void testAddPartner_WithExistPartner() {
		long TEST_TASK_ID = 1;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		task.addPartner(1);
		// check status before test
		List<Long> partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(1, partnersId.size());
		assertEquals(1L, partnersId.get(0));
		// testAddPartner_withExistPartner
		task.addPartner(1);
		partnersId.clear();
		partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(1, partnersId.size());
		assertEquals(1L, partnersId.get(0));
	}

	@Test
	public void testRemovePartner() {
		long TEST_TASK_ID = 1;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// add a partner
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, 1);
		// check status before test
		List<Long> partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(1, partnersId.size());
		assertEquals(1L, partnersId.get(0));
		// testRemovePartner
		task.removePartner(1);
		partnersId.clear();
		partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(0, partnersId.size());
	}

	@Test
	public void testRemovePartner_WithTwoPartners() {
		long TEST_TASK_ID = 1;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// add two partners
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, 1);
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, 2);
		// check status before test
		List<Long> partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(2, partnersId.size());
		assertEquals(1L, partnersId.get(0));
		assertEquals(2L, partnersId.get(1));
		// testRemovePartner_withTwoPartners
		task.removePartner(1);
		partnersId.clear();
		partnersId = TaskDAO.getInstance().getPartnersId(TEST_TASK_ID);
		assertEquals(1, partnersId.size());
		assertEquals(2L, partnersId.get(0));
	}

	@Test
	public void testGetHandler_UnassignHandler() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// testGetHandler
		assertEquals(null, task.getHandler());
	}

	@Test
	public void testGetHandler() {
		String USERNAME = "test_username";
		String PASSWORD = "test_password";
		boolean ENABLE = true;
		// create a account
		AccountObject account = new AccountObject(USERNAME);
		account.setPassword(PASSWORD).setEnable(ENABLE);
		account.save();
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0)
			.setHandlerId(account.getId());
		task.save();
		// testGetHandler
		assertEquals(account.getId(), task.getHandler().getId());
	}

	@Test
	public void testGetPartnersId() {
		long TEST_TASK_ID = 1;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check status before get partners id
		List<Long> partnersId = task.getPartnersId();
		assertEquals(0, partnersId.size());
		// add a partner
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, 1);
		// testGetPartnersId
		partnersId.clear();
		partnersId = task.getPartnersId();
		assertEquals(1, partnersId.size());
		assertEquals(1L, partnersId.get(0));
	}

	@Test
	public void testGetPartnersId_WithTwoPartners() {
		long TEST_TASK_ID = 1;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check status before get partners id
		List<Long> partnersId = task.getPartnersId();
		assertEquals(0, partnersId.size());
		// add two partners
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, 1);
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, 2);
		// testGetPartnersId_withTwoPartners
		partnersId.clear();
		partnersId = task.getPartnersId();
		assertEquals(2, partnersId.size());
		assertEquals(1L, partnersId.get(0));
		assertEquals(2L, partnersId.get(1));
	}

	@Test
	public void testGetPartners() {
		long TEST_TASK_ID = 1;
		String USERNAME = "test_username";
		String PASSWORD = "test_password";
		boolean ENABLE = true;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// before add a partner
		assertEquals(0, task.getPartners().size());
		// create a account
		AccountObject account = new AccountObject(USERNAME);
		account.setPassword(PASSWORD).setEnable(ENABLE);
		account.save();
		// add partner
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, account.getId());
		// testGetPartners
		assertEquals(1, task.getPartners().size());
		assertEquals(account.getId(), task.getPartners().get(0).getId());
	}

	@Test
	public void testGetPartners_WithTwoPartners() {
		long TEST_TASK_ID = 1;
		String FIRST_USERNAME = "test_first_username";
		String SECOND_USERNAME = "test_second_username";
		String PASSWORD = "test_password";
		boolean ENABLE = true;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// before add partners
		assertEquals(0, task.getPartners().size());
		// create first account
		AccountObject firstAccount = new AccountObject(FIRST_USERNAME);
		firstAccount.setPassword(PASSWORD).setEnable(ENABLE);
		firstAccount.save();
		// create first account
		AccountObject secondAccount = new AccountObject(SECOND_USERNAME);
		secondAccount.setPassword(PASSWORD).setEnable(ENABLE);
		secondAccount.save();
		// add two partners
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, firstAccount.getId());
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, secondAccount.getId());
		// testGetPartners
		assertEquals(2, task.getPartners().size());
		assertEquals(firstAccount.getId(), task.getPartners().get(0).getId());
		assertEquals(secondAccount.getId(), task.getPartners().get(1).getId());
	}

	@Test
	public void testGetPartnersUsername() {
		long TEST_TASK_ID = 1;
		String USERNAME = "test_username";
		String PASSWORD = "test_password";
		boolean ENABLE = true;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// before add a partner get partners username
		assertEquals("", task.getPartnersUsername());
		// create a account
		AccountObject account = new AccountObject(USERNAME);
		account.setPassword(PASSWORD).setEnable(ENABLE);
		account.save();
		// add partner
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, account.getId());
		// testGetPartnersUsername
		assertEquals("test_username", task.getPartnersUsername());
	}

	@Test
	public void testGetPartnersUsername_WithTwoPartners() {
		long TEST_TASK_ID = 1;
		String FIRST_USERNAME = "test_first_username";
		String SECOND_USERNAME = "test_second_username";
		String PASSWORD = "test_password";
		boolean ENABLE = true;
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// before add partners
		assertEquals("", task.getPartnersUsername());
		// create first account
		AccountObject firstAccount = new AccountObject(FIRST_USERNAME);
		firstAccount.setPassword(PASSWORD).setEnable(ENABLE);
		firstAccount.save();
		// create first account
		AccountObject secondAccount = new AccountObject(SECOND_USERNAME);
		secondAccount.setPassword(PASSWORD).setEnable(ENABLE);
		secondAccount.save();
		// add two partners
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, firstAccount.getId());
		TaskDAO.getInstance().addPartner(TEST_TASK_ID, secondAccount.getId());
		// testGetPartnersUsername_withTwoPartners
		assertEquals("test_first_username;test_second_username", task.getPartnersUsername());
	}

	@Test
	public void testGetHistories() {
		String TEST_NAME = "TEST_NAME", TEST_NAME_NEW = "TEST_NAME_NEW";
		String TEST_NOTE = "TEST_NOTE", TEST_NOTE_NEW = "TEST_NOTE_NEW";
		int TEST_ESTIMATE = 1, TEST_ESTIMATE_NEW = 2;
		int TEST_ACTUAL = 3, TEST_ACTUAL_NEW = 4;
		int TEST_REMAIN = 5, TEST_REMAIN_NEW = 6;
		int TEST_STATUS = TaskObject.STATUS_UNCHECK, TEST_STATUS_NEW = TaskObject.STATUS_DONE;
		long TEST_HANDLER = 1;
		long TEST_STORY_ID = 2;

		TaskObject task = new TaskObject(1);
		task.setName(TEST_NAME).setNotes(TEST_NOTE).setEstimate(TEST_ESTIMATE)
				.setActual(TEST_ACTUAL).setRemains(TEST_REMAIN)
				.setStatus(TEST_STATUS).save();

		task.setName(TEST_NAME_NEW).setNotes(TEST_NOTE_NEW)
				.setEstimate(TEST_ESTIMATE_NEW).setActual(TEST_ACTUAL_NEW)
				.setRemains(TEST_REMAIN_NEW).setStatus(TEST_STATUS_NEW)
				.setHandlerId(TEST_HANDLER).setStoryId(TEST_STORY_ID).save();

		assertEquals(9, task.getHistories().size());
	}

	@Test
	public void testGetHistories_WithSetNoParent() {
		String TEST_NAME = "TEST_NAME";
		String TEST_NOTE = "TEST_NOTE";
		int TEST_ESTIMATE = 1;
		int TEST_ACTUAL = 3;
		int TEST_REMAIN = 5;
		int TEST_STATUS = TaskObject.STATUS_UNCHECK;
		long TEST_STORY_ID = 2;
		long TEST_STORY_ID_NO_PARENT = -1;

		TaskObject task = new TaskObject(1);
		task.setName(TEST_NAME).setNotes(TEST_NOTE).setEstimate(TEST_ESTIMATE)
				.setActual(TEST_ACTUAL).setRemains(TEST_REMAIN)
				.setStatus(TEST_STATUS).setStoryId(TEST_STORY_ID).save();

		task.setStoryId(TEST_STORY_ID_NO_PARENT).save();

		assertEquals(3, task.getHistories().size());
		assertEquals(HistoryObject.TYPE_CREATE, task.getHistories().get(0).getHistoryType());
		assertEquals(HistoryObject.TYPE_APPEND, task.getHistories().get(1).getHistoryType());
		assertEquals(HistoryObject.TYPE_REMOVE, task.getHistories().get(2).getHistoryType());
	}

	@Test
	public void testGetAttachFiles_OneFile() {
		String TEST_NAME = "TEST_NAME";
		String TEST_NOTE = "TEST_NOTE";
		int TEST_ESTIMATE = 0;
		int TEST_ACTUAL = 1;
		int TEST_REMAIN = 2;
		long TEST_PROJECT = 1;
		long TEST_STORY_ID = 5;

		// the task will be added attach files
		TaskObject task = new TaskObject(TEST_PROJECT);
		task.setName(TEST_NAME).setNotes(TEST_NOTE).setEstimate(TEST_ESTIMATE)
				.setActual(TEST_ACTUAL).setRemains(TEST_REMAIN)
				.setStatus(TaskObject.STATUS_DONE).setStoryId(TEST_STORY_ID)
				.save();

		// create a attach file
		String TEST_FILE_NAME = "TEST_FILE_NAME";
		String TEST_FILE_PATH = "/TEST_PATH";
		String TEST_FILE_CONTENT_TYPE = "jpg";
		long TEST_CREATE_TIME = System.currentTimeMillis();

		AttachFileObject attachFile = new AttachFileObject();
		attachFile.setContentType(TEST_FILE_CONTENT_TYPE).setIssueId(task.getId())
				.setIssueType(IssueTypeEnum.TYPE_TASK).setName(TEST_FILE_NAME)
				.setPath(TEST_FILE_PATH).setCreateTime(TEST_CREATE_TIME).save();

		assertEquals(1, task.getAttachFiles().size());
	}

	@Test
	public void testGetAttachFiles_TwoFiles() {
		String TEST_NAME = "TEST_NAME";
		String TEST_NOTE = "TEST_NOTE";
		int TEST_ESTIMATE = 0;
		int TEST_ACTUAL = 1;
		int TEST_REMAIN = 2;
		long TEST_PROJECT = 1;
		long TEST_STORY_ID = 5;

		// the task will be added attach files
		TaskObject task = new TaskObject(TEST_PROJECT);
		task.setName(TEST_NAME).setNotes(TEST_NOTE).setEstimate(TEST_ESTIMATE)
				.setActual(TEST_ACTUAL).setRemains(TEST_REMAIN)
				.setStatus(TaskObject.STATUS_DONE).setStoryId(TEST_STORY_ID)
				.save();

		// first attach file
		String TEST_FILE_NAME = "TEST_FILE_NAME";
		String TEST_FILE_PATH = "/TEST_PATH";
		String TEST_FILE_CONTENT_TYPE = "jpg";
		long TEST_CREATE_TIME = System.currentTimeMillis();

		AttachFileObject attachFile = new AttachFileObject();
		attachFile.setContentType(TEST_FILE_CONTENT_TYPE).setIssueId(task.getId())
				.setIssueType(IssueTypeEnum.TYPE_TASK).setName(TEST_FILE_NAME)
				.setPath(TEST_FILE_PATH).setCreateTime(TEST_CREATE_TIME).save();

		String TEST_FILE2_NAME = "TEST_FILE_NAME";
		String TEST_FILE2_PATH = "/TEST_PATH";
		String TEST_FILE2_CONTENT_TYPE = "jpg";

		// second attach file
		AttachFileObject attachFile2 = new AttachFileObject();
		attachFile2.setContentType(TEST_FILE2_CONTENT_TYPE)
				.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setName(TEST_FILE2_NAME).setPath(TEST_FILE2_PATH)
				.setCreateTime(TEST_CREATE_TIME).save();

		assertEquals(2, task.getAttachFiles().size());
	}

	@Test
	public void testToJSON() throws JSONException {
		// init handler and partner, and setup a task
		String TEST_USERNAME = "TEST_ACCOUNT";
		String TEST_EMAIL = "TEST@ezscrum.tw";
		String TEST_ACCOUNT_NAME = "ACCOUNT_NAME";
		String TEST_ACCOUNT_PW = "123123123";
		AccountObject handler = new AccountObject(TEST_USERNAME);
		handler.setEmail(TEST_EMAIL).setNickName(TEST_ACCOUNT_NAME)
				.setPassword(TEST_ACCOUNT_PW).save();

		String TEST_PARTNER_USERNAME = "TEST_PAERTNER_ACCOUNT";
		String TEST_PARTNER_EMAIL = "PARTNER@ezscrum.tw";
		String TEST_PARTNER_NAME = "TEST_PAERTNER_NAME";
		AccountObject partner = new AccountObject(TEST_PARTNER_USERNAME);
		partner.setEmail(TEST_PARTNER_EMAIL).setNickName(TEST_PARTNER_NAME)
				.setPassword(TEST_ACCOUNT_PW).save();

		String TEST_NAME = "TEST_NAME";
		String TEST_NOTE = "TEST_NOTE";
		int TEST_ESTIMATE = 0;
		int TEST_ACTUAL = 1;
		int TEST_REMAIN = 2;
		long TEST_PROJECT = 1;
		
		String sprintGoal = "TEST_SPRINT_GOAL";
		String sprintDailyInfo = "TEST_SPRINT_DAILY_INFO";
		String sprintDemoPlace = "TEST_SPRINT_DEMO_PLACE";
		String sprintStartDate = "2015/05/28";
		String sprintDemoDate = "2015/06/11";
		String sprintEndDate = "2015/06/11";
		
		// create sprint
		SprintObject sprint = new SprintObject(mProjectId);
		sprint.setInterval(2).setTeamSize(5)
				.setAvailableHours(100).setFocusFactor(70)
				.setGoal(sprintGoal).setStartDate(sprintStartDate)
				.setEndDate(sprintEndDate).setDailyInfo(sprintDailyInfo)
				.setDemoDate(sprintDemoDate).setDemoPlace(sprintDemoPlace)
				.save();
		
		// create story
		StoryObject story = new StoryObject(mProjectId);
		story.setName("TEST_NAME").setNotes("TEST_NOTE")
				.setHowToDemo("TEST_HOW_TO_DEMO").setImportance(1).setValue(2)
				.setEstimate(3).setStatus(StoryObject.STATUS_DONE)
				.setSprintId(sprint.getId()).save();

		TaskObject task = new TaskObject(TEST_PROJECT);
		task.setName(TEST_NAME).setNotes(TEST_NOTE).setEstimate(TEST_ESTIMATE)
				.setActual(TEST_ACTUAL).setRemains(TEST_REMAIN)
				.setStatus(TaskObject.STATUS_DONE)
				.setHandlerId(handler.getId()).setStoryId(story.getId()).save();
		ArrayList<Long> partners = new ArrayList<Long>();
		partners.add(partner.getId());
		task.addPartner(partner.getId());

		// assert json object
		JSONObject json = task.toJSON();
		assertEquals(TEST_NAME, json.getString(TaskEnum.NAME));
		assertEquals(TEST_NOTE, json.getString(TaskEnum.NOTES));
		assertEquals(TEST_ESTIMATE, json.getInt(TaskEnum.ESTIMATE));
		assertEquals(TEST_ACTUAL, json.getInt(TaskEnum.ACTUAL));
		assertEquals(TEST_ESTIMATE, json.getInt(TaskEnum.REMAIN));
		assertEquals(TEST_PROJECT, json.getInt(TaskEnum.PROJECT_ID));
		assertEquals(story.getId(), json.getInt(TaskEnum.STORY_ID));

		JSONObject handlerJson = json.getJSONObject(TaskEnum.HANDLER);
		assertEquals(handler.getId(), handlerJson.getLong(AccountEnum.ID));
		assertEquals(handler.getUsername(), handlerJson.getString(AccountEnum.USERNAME));
		assertEquals(handler.getEmail(), handlerJson.getString(AccountEnum.EMAIL));
		assertEquals(handler.getNickName(), handlerJson.getString(AccountEnum.NICK_NAME));

		JSONObject partnerJosn = json.getJSONArray("partners").getJSONObject(0);
		assertEquals(partner.getId(), partnerJosn.getLong(AccountEnum.ID));
		assertEquals(partner.getUsername(), partnerJosn.getString(AccountEnum.USERNAME));
		assertEquals(partner.getEmail(), partnerJosn.getString(AccountEnum.EMAIL));
		assertEquals(partner.getNickName(), partnerJosn.getString(AccountEnum.NICK_NAME));
	}

	/**
	 * 測試新增一個 task
	 */
	@Test
	public void testSave_CreateANewTask() {
		TaskObject task = new TaskObject(1);
		task.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10)
			.setActual(0);
		task.save();

		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME", task.getName());
		assertEquals("TEST_NOTES", task.getNotes());
		assertEquals(10, task.getEstimate());
		assertEquals(10, task.getRemains());
		assertEquals(0, task.getActual());
	}

	/**
	 * 測試一個已存在的 task
	 */
	@Test
	public void testSave_UpdateTask() {
		TaskObject task = new TaskObject(1);
		task.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10)
			.setActual(0);
		task.save();

		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME", task.getName());
		assertEquals("TEST_NOTES", task.getNotes());
		assertEquals(10, task.getEstimate());
		assertEquals(10, task.getRemains());
		assertEquals(0, task.getActual());

		task.setName("TEST_NAME2").setNotes("TEST_NOTES2").setEstimate(3)
			.setRemains(5).setActual(1);
		task.save();

		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME2", task.getName());
		assertEquals("TEST_NOTES2", task.getNotes());
		assertEquals(3, task.getEstimate());
		assertEquals(5, task.getRemains());
		assertEquals(1, task.getActual());
	}

	@Test
	public void testDelete() {
		TaskObject task = new TaskObject(1);
		task.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10).setActual(0);
		task.save();

		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME", task.getName());
		assertEquals("TEST_NOTES", task.getNotes());
		assertEquals(10, task.getEstimate());
		assertEquals(10, task.getRemains());
		assertEquals(0, task.getActual());

		boolean deleteSuccess = task.delete();

		assertTrue(deleteSuccess);
		assertEquals(-1, task.getId());
		assertEquals(-1, task.getSerialId());
		assertEquals(null, TaskDAO.getInstance().get(1));
	}

	@Test
	public void testReload() {
		TaskObject task = new TaskObject(1);
		task.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10).setActual(0);
		task.save();

		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME", task.getName());
		assertEquals("TEST_NOTES", task.getNotes());
		assertEquals(10, task.getEstimate());
		assertEquals(10, task.getRemains());
		assertEquals(0, task.getActual());

		task.setName("TEST_NAME2").setNotes("TEST_NOTES2").setEstimate(5).setRemains(3).setActual(1);

		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME2", task.getName());
		assertEquals("TEST_NOTES2", task.getNotes());
		assertEquals(5, task.getEstimate());
		assertEquals(3, task.getRemains());
		assertEquals(1, task.getActual());
		task.reload();
		assertEquals(1, task.getId());
		assertEquals(1, task.getSerialId());
		assertEquals("TEST_NAME", task.getName());
		assertEquals("TEST_NOTES", task.getNotes());
		assertEquals(10, task.getEstimate());
		assertEquals(10, task.getRemains());
		assertEquals(0, task.getActual());
	}

	@Test
	public void testGetStatus_WithSpecificDate() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		Date specificDate = DateUtil.dayFillter("2015/02/04", DateUtil._8DIGIT_DATE_1);
		assertEquals(TaskObject.STATUS_UNCHECK, task.getStatus(specificDate));
		// clean task storage histories data
		task.cleanHistories();
		// create a check task out history
		Date changeStatusDate = DateUtil.dayFillter("2015/02/04-13:14:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history = new HistoryObject();
		history.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_CHECK))
				.setCreateTime(changeStatusDate.getTime());
		history.save();
		// check task status after add a history
		assertEquals(TaskObject.STATUS_CHECK, task.getStatus(specificDate));
	}
	
	@Test
	public void testGetStatus_WithSpecificDateChangeStatusTwoTimes() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		Date specificDate = DateUtil.dayFillter("2015/02/04", DateUtil._8DIGIT_DATE_1);
		
		assertEquals(TaskObject.STATUS_UNCHECK, task.getStatus(specificDate));
		// clean task storage histories data
		task.cleanHistories();
		// create check task out history1
		Date changeStatusDate1 = DateUtil.dayFillter("2015/02/04-13:14:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history1 = new HistoryObject();
		history1.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_CHECK))
				.setCreateTime(changeStatusDate1.getTime());
		history1.save();
		// create check task out history2
		Date changeStatusDate2 = DateUtil.dayFillter("2015/02/04-16:14:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history2 = new HistoryObject();
		history2.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_DONE))
				.setCreateTime(changeStatusDate2.getTime());
		history2.save();
		// check task status after add two change status histories
		assertEquals(TaskObject.STATUS_DONE, task.getStatus(specificDate));
	}

	/*
	 * 之後要拔掉,為了符合目前的IIssue
	 */
	@Test
	public void testGetStatusString() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		assertEquals("new", task.getStatusString());
		// check task status after check it out
		task.setStatus(TaskObject.STATUS_CHECK);
		assertEquals("assigned", task.getStatusString());
		// check task status after close it
		task.setStatus(TaskObject.STATUS_DONE);
		assertEquals("closed", task.getStatusString());
		// check task status after reopen it
		task.setStatus(TaskObject.STATUS_UNCHECK);
		assertEquals("new", task.getStatusString());
	}

	@Test
	public void testGetCreateTimeFromHistories() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		long createTime = 0;
		ArrayList<HistoryObject> histories = task.getHistories();
		for (HistoryObject history : histories) {
			long historyTime = history.getCreateTime();
			if (history.getHistoryType() == HistoryObject.TYPE_CREATE) {
				if (createTime < historyTime) {
					createTime = historyTime;
				}
			}
		}
		assertEquals(createTime, task.getCreateTimeFromHistories());
	}
	
	@Test
	public void testGetDoneTime() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		assertEquals(0, task.getDoneTime());
		// create a close task history
		Date specificDate = DateUtil.dayFillter("2015/02/04-13:15:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history = new HistoryObject();
		history.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_DONE))
				.setCreateTime(specificDate.getTime());
		history.save();
		// check task status after close task
		assertEquals(specificDate.getTime(), task.getDoneTime());
	}

	@Test
	public void testGetDoneTime_WithTwoDoneHistoriesInSameDay() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		assertEquals(0, task.getDoneTime());
		// create a close task history
		Date specificDate1 = DateUtil.dayFillter("2015/02/04-13:15:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history1 = new HistoryObject();
		history1.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_DONE))
				.setCreateTime(specificDate1.getTime());
		history1.save();
		// create a close task history
		Date specificDate2 = DateUtil.dayFillter("2015/02/04-13:15:02", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history2 = new HistoryObject();
		history2.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_DONE))
				.setCreateTime(specificDate2.getTime());
		history2.save();
		// check task status after close task
		assertEquals(specificDate2.getTime(), task.getDoneTime());
	}

	@Test
	public void testGetDoneTime_WithTwoDoneHistoriesInDifferentDay() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		assertEquals(0, task.getDoneTime());
		// create a close task history
		Date specificDate1 = DateUtil.dayFillter("2015/02/04-13:15:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history1 = new HistoryObject();
		history1.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_DONE))
				.setCreateTime(specificDate1.getTime());
		history1.save();
		// create a close task history
		Date specificDate2 = DateUtil.dayFillter("2015/02/06-09:05:01", DateUtil._16DIGIT_DATE_TIME);
		HistoryObject history2 = new HistoryObject();
		history2.setIssueId(task.getId()).setIssueType(IssueTypeEnum.TYPE_TASK)
				.setHistoryType(HistoryObject.TYPE_STATUS)
				.setOldValue(String.valueOf(task.getStatus()))
				.setNewValue(String.valueOf(TaskObject.STATUS_DONE))
				.setCreateTime(specificDate2.getTime());
		history2.save();
		// check task status after close task
		assertEquals(specificDate2.getTime(), task.getDoneTime());
	}

	@Test
	public void testGetRemains_WithSpecificDate() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		Date specificDate = DateUtil.dayFillter("2015/02/04", DateUtil._8DIGIT_DATE_1);
		assertEquals(10, task.getRemains(specificDate));
		// create a update task remains history
		Date changeStatusDate = DateUtil.dayFillter("2015/02/04-13:14:01", DateUtil._16DIGIT_DATE_TIME);
		task.setRemains(5).save(changeStatusDate.getTime());
		// check task remains after add a update task remains history
		assertEquals(5, task.getRemains(specificDate));
	}
	
	@Test
	public void testGetRemains_WithSpecificDateChangeRemainsTwoTimes() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		Date specificDate = DateUtil.dayFillter("2015/02/04", DateUtil._8DIGIT_DATE_1);
		assertEquals(10, task.getRemains(specificDate));
		// create a update task remains history1
		Date changeRemainsDate1 = DateUtil.dayFillter("2015/02/04-13:14:01", DateUtil._16DIGIT_DATE_TIME);
		task.setRemains(5).save(changeRemainsDate1.getTime());
		// create a update task remains history2
		Date changeRemainsDate2 = DateUtil.dayFillter("2015/02/04-16:14:01", DateUtil._16DIGIT_DATE_TIME);
		task.setRemains(8).save(changeRemainsDate2.getTime());
		// check task remains after add two change remains histories
		assertEquals(8, task.getRemains(specificDate));
	}
	
	@Test
	public void testGetRemains_WithChangeRemainsInFiveDates() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(10).setActual(0);
		task.save();
		// check task status before test
		Date firstDate = DateUtil.dayFillter("2015/02/04", DateUtil._8DIGIT_DATE_1);
		assertEquals(10, task.getRemains(firstDate));
		// create a update task remains history1
		Date changeRemainsDate1 = DateUtil.dayFillter("2015/02/05", DateUtil._8DIGIT_DATE_1);
		task.setRemains(9).save(changeRemainsDate1.getTime());
		// create a update task remains history2
		Date changeRemainsDate2 = DateUtil.dayFillter("2015/02/06", DateUtil._8DIGIT_DATE_1);
		task.setRemains(8).save(changeRemainsDate2.getTime());
		// create a update task remains history3
		Date changeRemainsDate3 = DateUtil.dayFillter("2015/02/07", DateUtil._8DIGIT_DATE_1);
		task.setRemains(7).save(changeRemainsDate3.getTime());
		// create a update task remains history4
		Date changeRemainsDate4 = DateUtil.dayFillter("2015/02/08", DateUtil._8DIGIT_DATE_1);
		task.setRemains(6).save(changeRemainsDate4.getTime());
		// check task remains after add five change remains histories
		assertEquals(10, task.getRemains(firstDate));
		assertEquals(9, task.getRemains(changeRemainsDate1));
		assertEquals(8, task.getRemains(changeRemainsDate2));
		assertEquals(7, task.getRemains(changeRemainsDate3));
		assertEquals(6, task.getRemains(changeRemainsDate4));
	}
	
	@Test
	public void testGetRemains_WithChangeRemainsThreeTimesInSixDates() {
		// create a task
		TaskObject task = new TaskObject(mProjectId);
		task.setName("TEST_NAME").setEstimate(13).setActual(0);
		task.save();
		// check task status before test
		assertEquals(13, task.getRemains(DateUtil.dayFillter("2015/02/01", DateUtil._8DIGIT_DATE_1)));
		// create a update task remains history1
		Date changeRemainsDate1 = DateUtil.dayFillter("2015/02/03", DateUtil._8DIGIT_DATE_1);
		task.setRemains(8).save(changeRemainsDate1.getTime());
		// create a update task remains history2
		Date changeRemainsDate2 = DateUtil.dayFillter("2015/02/05", DateUtil._8DIGIT_DATE_1);
		task.setRemains(6).save(changeRemainsDate2.getTime());
		// create a update task remains history3
		Date changeRemainsDate3 = DateUtil.dayFillter("2015/02/06", DateUtil._8DIGIT_DATE_1);
		task.setRemains(3).save(changeRemainsDate3.getTime());
		// check task remains after add three change remains histories
		assertEquals(13, task.getRemains(DateUtil.dayFillter("2015/02/01", DateUtil._8DIGIT_DATE_1)));
		assertEquals(13, task.getRemains(DateUtil.dayFillter("2015/02/02", DateUtil._8DIGIT_DATE_1)));
		assertEquals(8, task.getRemains(DateUtil.dayFillter("2015/02/03", DateUtil._8DIGIT_DATE_1)));
		assertEquals(8, task.getRemains(DateUtil.dayFillter("2015/02/04", DateUtil._8DIGIT_DATE_1)));
		assertEquals(6, task.getRemains(DateUtil.dayFillter("2015/02/05", DateUtil._8DIGIT_DATE_1)));
		assertEquals(3, task.getRemains(DateUtil.dayFillter("2015/02/06", DateUtil._8DIGIT_DATE_1)));
	}
}
