package ntut.csie.ezScrum.restful.dataMigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import ntut.csie.ezScrum.issue.sql.service.core.Configuration;
import ntut.csie.ezScrum.issue.sql.service.core.InitialSQL;
import ntut.csie.ezScrum.restful.dataMigration.jsonEnum.AccountJSONEnum;
import ntut.csie.ezScrum.restful.dataMigration.jsonEnum.HistoryJSONEnum;
import ntut.csie.ezScrum.restful.dataMigration.jsonEnum.ResponseJSONEnum;
import ntut.csie.ezScrum.restful.dataMigration.jsonEnum.UnplanJSONEnum;
import ntut.csie.ezScrum.restful.dataMigration.support.BaseUrlDistributor;
import ntut.csie.ezScrum.restful.dataMigration.security.SecurityModule;import ntut.csie.ezScrum.test.CreateData.CreateAccount;
import ntut.csie.ezScrum.test.CreateData.CreateProject;
import ntut.csie.ezScrum.test.CreateData.CreateSprint;
import ntut.csie.ezScrum.test.CreateData.CreateUnplanItem;
import ntut.csie.ezScrum.web.dataObject.AccountObject;
import ntut.csie.ezScrum.web.dataObject.HistoryObject;
import ntut.csie.ezScrum.web.dataObject.ProjectObject;
import ntut.csie.ezScrum.web.dataObject.SprintObject;
import ntut.csie.ezScrum.web.dataObject.UnplanObject;
import ntut.csie.ezScrum.web.databaseEnum.RoleEnum;
import ntut.csie.ezScrum.web.databaseEnum.UnplanEnum;

public class UnplanRESTfulApiTest extends JerseyTest {
	private Configuration mConfig;
	private CreateAccount mCA;
	private CreateProject mCP;
	private CreateSprint mCS;
	private CreateUnplanItem mCUI;
	private ResourceConfig mResourceConfig;
	private Client mClient;
	private HttpServer mHttpServer;
	private static String BASE_URL = BaseUrlDistributor.TEST_MODE_BASE_URL;
	private URI mBaseUri = URI.create(BASE_URL);

	@Override
	protected Application configure() {
		mResourceConfig = new ResourceConfig(UnplanRESTfulApi.class);
		return mResourceConfig;
	}

	@Before
	public void setUp() throws Exception {
		// Set Test Mode
		mConfig = new Configuration();
		mConfig.setTestMode(true);
		mConfig.save();

		// 初始化 SQL
		InitialSQL ini = new InitialSQL(mConfig);
		ini.exe();

		// Create Account
		mCA = new CreateAccount(2);
		mCA.exe();

		// Create Project
		mCP = new CreateProject(1);
		mCP.exeCreateForDb();

		// Create Sprint
		mCS = new CreateSprint(1, mCP);
		mCS.exe();

		// Start Server
		mHttpServer = JdkHttpServerFactory.createHttpServer(mBaseUri, mResourceConfig, true);

		// Create Client
		mClient = ClientBuilder.newClient();
	}

	@After
	public void tearDown() {
		// 初始化 SQL
		InitialSQL ini = new InitialSQL(mConfig);
		ini.exe();

		// 讓 config 回到 Production 模式
		mConfig.setTestMode(false);
		mConfig.save();

		// Stop Server
		mHttpServer.stop(0);

		// ============= release ==============
		ini = null;
		mCP = null;
		mCS = null;
		mCUI = null;
		mHttpServer = null;
		mClient = null;
	}
	
	@Test
	public void testCreateUnplan() throws JSONException {
		// Test Data
		AccountObject handlerAccount = mCA.getAccountList().get(0);
		AccountObject partnerAccount = mCA.getAccountList().get(0);
		String name = "TEST_UNPLAN_NAME";
		String handler = handlerAccount.getUsername();
		String partner = partnerAccount.getUsername();
		int estimate = 3;
		//int actual = 2;
		String notes = "TEST_CREATE_UNPLAN";
		String status = "assigned";
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);

		// Add Account to Project
		handlerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);
		partnerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);

		JSONObject unplanJSON = new JSONObject();
		unplanJSON.put(UnplanJSONEnum.NAME, name);
		unplanJSON.put(UnplanJSONEnum.HANDLER, handler);
		unplanJSON.put(UnplanJSONEnum.ESTIMATE, estimate);
		//unplanJSON.put(UnplanJSONEnum.ACTUAL, actual);
		unplanJSON.put(UnplanJSONEnum.NOTES, notes);
		unplanJSON.put(UnplanJSONEnum.STATUS, status);
		JSONArray partnersIdJSONArray = new JSONArray();
		JSONObject partnerJSON = new JSONObject();
		partnerJSON.put(AccountJSONEnum.USERNAME, partner);
		partnersIdJSONArray.put(partnerJSON);
		unplanJSON.put(UnplanJSONEnum.PARTNERS, partnersIdJSONArray);

		// Call '/projects/{projectId}/sprints/{sprintId}/unplans' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, SecurityModule.ADMIN_MD5_USERNAME)
		        .header(SecurityModule.PASSWORD_HEADER, SecurityModule.ADMIN_MD5_PASSWORD)
		        .post(Entity.text(unplanJSON.toString()));

		JSONObject jsonResponse = new JSONObject(response.readEntity(String.class));
		JSONObject contentJSON = jsonResponse.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT);

		// Assert
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertTrue(contentJSON.getLong(UnplanEnum.ID) != -1);
		UnplanObject unplan = UnplanObject.get(contentJSON.getLong(UnplanEnum.ID));
		assertEquals(name, unplan.getName());
		assertEquals(handler, unplan.getHandler().getUsername());
		assertEquals(estimate, unplan.getEstimate());
		//assertEquals(actual, unplan.getActual());
		assertEquals(notes, unplan.getNotes());
		assertEquals(status, unplan.getStatusString());
	}
	
	@Test
	public void testCreateHistoryInUnplan() throws JSONException {
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		UnplanObject unplan = new UnplanObject(sprint.getId(), project.getId());
		unplan.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10)
		/*.setActual(0)*/.save();
		
		// Check unplan's histories before create history
		assertEquals(1, unplan.getHistories().size());
		assertEquals(HistoryObject.TYPE_CREATE, unplan.getHistories().get(0).getHistoryType());
		
		long createTime = System.currentTimeMillis();
		JSONObject historyJSON = new JSONObject();
		historyJSON.put(HistoryJSONEnum.HISTORY_TYPE, "STATUS");
		historyJSON.put(HistoryJSONEnum.OLD_VALUE, "new");
		historyJSON.put(HistoryJSONEnum.NEW_VALUE, "assigned");
		historyJSON.put(HistoryJSONEnum.CREATE_TIME, createTime);
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + unplan.getId() +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, SecurityModule.ADMIN_MD5_USERNAME)
		        .header(SecurityModule.PASSWORD_HEADER, SecurityModule.ADMIN_MD5_PASSWORD)
		        .post(Entity.text(historyJSON.toString()));
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(ResponseJSONEnum.SUCCESS_MESSAGE, responseMessage);
		assertEquals(unplan.getHistories().get(1).toString(), responseContent);
	}
	
	@Test
	public void testDeleteHistoryInUnplan() throws Exception {
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		
		// Create Unplan
		mCUI = new CreateUnplanItem(1, mCP, mCS);
		mCUI.exe();
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + mCUI.getUnplansId().get(0) +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, SecurityModule.ADMIN_MD5_USERNAME)
		        .header(SecurityModule.PASSWORD_HEADER, SecurityModule.ADMIN_MD5_PASSWORD)
		        .delete();
		UnplanObject unplan = UnplanObject.get(mCUI.getUnplansId().get(0));
		
		// Assert
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertEquals(0, unplan.getHistories().size());
	}
	
	@Test
	public void testCreateUnplan_AccountIsInvalid() throws JSONException {
		String wrongAdminUsername = "test";
		String wrongAdminPassword = "test";
		
		// Test Data
		AccountObject handlerAccount = mCA.getAccountList().get(0);
		AccountObject partnerAccount = mCA.getAccountList().get(0);
		String name = "TEST_UNPLAN_NAME";
		String handler = handlerAccount.getUsername();
		String partner = partnerAccount.getUsername();
		int estimate = 3;
		//int actual = 2;
		String notes = "TEST_CREATE_UNPLAN";
		String status = "assigned";
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);

		// Add Account to Project
		handlerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);
		partnerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);

		JSONObject unplanJSON = new JSONObject();
		unplanJSON.put(UnplanJSONEnum.NAME, name);
		unplanJSON.put(UnplanJSONEnum.HANDLER, handler);
		unplanJSON.put(UnplanJSONEnum.ESTIMATE, estimate);
		//unplanJSON.put(UnplanJSONEnum.ACTUAL, actual);
		unplanJSON.put(UnplanJSONEnum.NOTES, notes);
		unplanJSON.put(UnplanJSONEnum.STATUS, status);
		JSONArray partnersIdJSONArray = new JSONArray();
		JSONObject partnerJSON = new JSONObject();
		partnerJSON.put(AccountJSONEnum.USERNAME, partner);
		partnersIdJSONArray.put(partnerJSON);
		unplanJSON.put(UnplanJSONEnum.PARTNERS, partnersIdJSONArray);

		// Call '/projects/{projectId}/sprints/{sprintId}/unplans' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, wrongAdminUsername)
		        .header(SecurityModule.PASSWORD_HEADER, wrongAdminPassword)
		        .post(Entity.text(unplanJSON.toString()));

		JSONObject jsonResponse = new JSONObject(response.readEntity(String.class));
		JSONObject contentJSON = jsonResponse.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT);
		String message = jsonResponse.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);

		// Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), contentJSON.toString());
		assertEquals("", message);
	}
	
	@Test
	public void testCreateUnplan_AccountIsNull() throws JSONException {
		String wrongAdminUsername = null;
		String wrongAdminPassword = null;
		
		// Test Data
		AccountObject handlerAccount = mCA.getAccountList().get(0);
		AccountObject partnerAccount = mCA.getAccountList().get(0);
		String name = "TEST_UNPLAN_NAME";
		String handler = handlerAccount.getUsername();
		String partner = partnerAccount.getUsername();
		int estimate = 3;
		//int actual = 2;
		String notes = "TEST_CREATE_UNPLAN";
		String status = "assigned";
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);

		// Add Account to Project
		handlerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);
		partnerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);

		JSONObject unplanJSON = new JSONObject();
		unplanJSON.put(UnplanJSONEnum.NAME, name);
		unplanJSON.put(UnplanJSONEnum.HANDLER, handler);
		unplanJSON.put(UnplanJSONEnum.ESTIMATE, estimate);
		//unplanJSON.put(UnplanJSONEnum.ACTUAL, actual);
		unplanJSON.put(UnplanJSONEnum.NOTES, notes);
		unplanJSON.put(UnplanJSONEnum.STATUS, status);
		JSONArray partnersIdJSONArray = new JSONArray();
		JSONObject partnerJSON = new JSONObject();
		partnerJSON.put(AccountJSONEnum.USERNAME, partner);
		partnersIdJSONArray.put(partnerJSON);
		unplanJSON.put(UnplanJSONEnum.PARTNERS, partnersIdJSONArray);

		// Call '/projects/{projectId}/sprints/{sprintId}/unplans' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, wrongAdminUsername)
		        .header(SecurityModule.PASSWORD_HEADER, wrongAdminPassword)
		        .post(Entity.text(unplanJSON.toString()));

		JSONObject jsonResponse = new JSONObject(response.readEntity(String.class));
		JSONObject contentJSON = jsonResponse.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT);
		String message = jsonResponse.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);

		// Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), contentJSON.toString());
		assertEquals("", message);
	}
	
	@Test
	public void testCreateUnplan_AccountIsEmpty() throws JSONException {
		String wrongAdminUsername = "";
		String wrongAdminPassword = "";
		
		// Test Data
		AccountObject handlerAccount = mCA.getAccountList().get(0);
		AccountObject partnerAccount = mCA.getAccountList().get(0);
		String name = "TEST_UNPLAN_NAME";
		String handler = handlerAccount.getUsername();
		String partner = partnerAccount.getUsername();
		int estimate = 3;
		//int actual = 2;
		String notes = "TEST_CREATE_UNPLAN";
		String status = "assigned";
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);

		// Add Account to Project
		handlerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);
		partnerAccount.joinProjectWithScrumRole(project.getId(), RoleEnum.ScrumTeam);

		JSONObject unplanJSON = new JSONObject();
		unplanJSON.put(UnplanJSONEnum.NAME, name);
		unplanJSON.put(UnplanJSONEnum.HANDLER, handler);
		unplanJSON.put(UnplanJSONEnum.ESTIMATE, estimate);
		//unplanJSON.put(UnplanJSONEnum.ACTUAL, actual);
		unplanJSON.put(UnplanJSONEnum.NOTES, notes);
		unplanJSON.put(UnplanJSONEnum.STATUS, status);
		JSONArray partnersIdJSONArray = new JSONArray();
		JSONObject partnerJSON = new JSONObject();
		partnerJSON.put(AccountJSONEnum.USERNAME, partner);
		partnersIdJSONArray.put(partnerJSON);
		unplanJSON.put(UnplanJSONEnum.PARTNERS, partnersIdJSONArray);

		// Call '/projects/{projectId}/sprints/{sprintId}/unplans' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, wrongAdminUsername)
		        .header(SecurityModule.PASSWORD_HEADER, wrongAdminPassword)
		        .post(Entity.text(unplanJSON.toString()));

		JSONObject jsonResponse = new JSONObject(response.readEntity(String.class));
		JSONObject contentJSON = jsonResponse.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT);
		String message = jsonResponse.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);

		// Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), contentJSON.toString());
		assertEquals("", message);
	}
	
	@Test
	public void testCreateHistoryInUnplan_AccountIsInvalid() throws JSONException {
		String invalidUsername = "test";
		String invalidPassword = "test";
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		UnplanObject unplan = new UnplanObject(sprint.getId(), project.getId());
		unplan.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10)
		/*.setActual(0)*/.save();
		
		// Check unplan's histories before create history
		assertEquals(1, unplan.getHistories().size());
		assertEquals(HistoryObject.TYPE_CREATE, unplan.getHistories().get(0).getHistoryType());
		
		long createTime = System.currentTimeMillis();
		JSONObject historyJSON = new JSONObject();
		historyJSON.put(HistoryJSONEnum.HISTORY_TYPE, "STATUS");
		historyJSON.put(HistoryJSONEnum.OLD_VALUE, "new");
		historyJSON.put(HistoryJSONEnum.NEW_VALUE, "assigned");
		historyJSON.put(HistoryJSONEnum.CREATE_TIME, createTime);
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + unplan.getId() +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, invalidUsername)
		        .header(SecurityModule.PASSWORD_HEADER, invalidPassword)
		        .post(Entity.text(historyJSON.toString()));
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		
		//Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), responseContent.toString());
		assertEquals("", responseMessage);
	}
	
	@Test
	public void testCreateHistoryInUnplan_AccountIsNull() throws JSONException {
		String invalidUsername = null;
		String invalidPassword = null;
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		UnplanObject unplan = new UnplanObject(sprint.getId(), project.getId());
		unplan.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10)
		/*.setActual(0)*/.save();
		
		// Check unplan's histories before create history
		assertEquals(1, unplan.getHistories().size());
		assertEquals(HistoryObject.TYPE_CREATE, unplan.getHistories().get(0).getHistoryType());
		
		long createTime = System.currentTimeMillis();
		JSONObject historyJSON = new JSONObject();
		historyJSON.put(HistoryJSONEnum.HISTORY_TYPE, "STATUS");
		historyJSON.put(HistoryJSONEnum.OLD_VALUE, "new");
		historyJSON.put(HistoryJSONEnum.NEW_VALUE, "assigned");
		historyJSON.put(HistoryJSONEnum.CREATE_TIME, createTime);
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + unplan.getId() +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, invalidUsername)
		        .header(SecurityModule.PASSWORD_HEADER, invalidPassword)
		        .post(Entity.text(historyJSON.toString()));
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		
		//Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), responseContent.toString());
		assertEquals("", responseMessage);
	}
	
	@Test
	public void testCreateHistoryInUnplan_AccountIsEmpty() throws JSONException {
		String invalidUsername = "";
		String invalidPassword = "";
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		UnplanObject unplan = new UnplanObject(sprint.getId(), project.getId());
		unplan.setName("TEST_NAME").setNotes("TEST_NOTES").setEstimate(10)
		/*.setActual(0)*/.save();
		
		// Check unplan's histories before create history
		assertEquals(1, unplan.getHistories().size());
		assertEquals(HistoryObject.TYPE_CREATE, unplan.getHistories().get(0).getHistoryType());
		
		long createTime = System.currentTimeMillis();
		JSONObject historyJSON = new JSONObject();
		historyJSON.put(HistoryJSONEnum.HISTORY_TYPE, "STATUS");
		historyJSON.put(HistoryJSONEnum.OLD_VALUE, "new");
		historyJSON.put(HistoryJSONEnum.NEW_VALUE, "assigned");
		historyJSON.put(HistoryJSONEnum.CREATE_TIME, createTime);
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + unplan.getId() +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, invalidUsername)
		        .header(SecurityModule.PASSWORD_HEADER, invalidPassword)
		        .post(Entity.text(historyJSON.toString()));
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		
		//Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), responseContent.toString());
		assertEquals("", responseMessage);
	}
	
	@Test
	public void testDeleteHistoryInUnplan_AccountIsInvalid() throws Exception {
		String invalidUsername = "test";
		String invalidPassword = "test";
		
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		
		// Create Unplan
		mCUI = new CreateUnplanItem(1, mCP, mCS);
		mCUI.exe();
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + mCUI.getUnplansId().get(0) +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, invalidUsername)
		        .header(SecurityModule.PASSWORD_HEADER, invalidPassword)
		        .delete();
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		
		//Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), responseContent.toString());
		assertEquals("", responseMessage);
	}
	
	@Test
	public void testDeleteHistoryInUnplan_AccountIsNull() throws Exception {
		String nullUsername = null;
		String nullPassword = null;
		
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		
		// Create Unplan
		mCUI = new CreateUnplanItem(1, mCP, mCS);
		mCUI.exe();
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + mCUI.getUnplansId().get(0) +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, nullUsername)
		        .header(SecurityModule.PASSWORD_HEADER, nullPassword)
		        .delete();
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		
		//Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), responseContent.toString());
		assertEquals("", responseMessage);
	}
	
	@Test
	public void testDeleteHistoryInUnplan_AccountIsEmpty() throws Exception {
		String emptyUsername = "";
		String emptyPassword = "";
		
		ProjectObject project = mCP.getAllProjects().get(0);
		SprintObject sprint = mCS.getSprints().get(0);
		
		// Create Unplan
		mCUI = new CreateUnplanItem(1, mCP, mCS);
		mCUI.exe();
		
		// Call '/projects/{projectId}/sprints/{sprintId}/unplans/{unplanId}/histories' API
		Response response = mClient.target(BASE_URL)
		        .path("projects/" + project.getId() +
		                "/sprints/" + sprint.getId() +
		                "/unplans/" + mCUI.getUnplansId().get(0) +
		                "/histories")
		        .request()
		        .header(SecurityModule.USERNAME_HEADER, emptyUsername)
		        .header(SecurityModule.PASSWORD_HEADER, emptyPassword)
		        .delete();
		
		JSONObject responseJSON = new JSONObject(response.readEntity(String.class));
		String responseMessage = responseJSON.getString(ResponseJSONEnum.JSON_KEY_MESSAGE);
		String responseContent = responseJSON.getJSONObject(ResponseJSONEnum.JSON_KEY_CONTENT).toString();
		
		//Assert
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
		assertEquals(new JSONObject().toString(), responseContent.toString());
		assertEquals("", responseMessage);
	}
}
