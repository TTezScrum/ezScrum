package ntut.csie.ezScrum.web.dataObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import ntut.csie.ezScrum.dao.AccountDAO;
import ntut.csie.ezScrum.dao.AttachFileDAO;
import ntut.csie.ezScrum.dao.HistoryDAO;
import ntut.csie.ezScrum.dao.TaskDAO;
import ntut.csie.ezScrum.web.databasEnum.IssueTypeEnum;
import ntut.csie.ezScrum.web.databasEnum.TaskEnum;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class TaskObject implements IBaseObject {
	public final static int STATUS_UNCHECK = 1;
	public final static int STATUS_CHECK = 2;
	public final static int STATUS_DONE = 3;
	public final static int WILD = -1;

	private long mId = -1;
	private long mSerialId = -1;
	private long mProjectId = -1;
	private long mStoryId = -1;
	private long mHandlerId = -1;
	private String mName = "";
	private String mNotes = "";
	private int mEstimate = 0;
	private int mRemains = 0;
	private int mActual = 0;
	private int mStatus = -1;
	private long mCreateTime = -1;
	private long mUpdateTime = -1;
	private ArrayList<Long> mPartnersId = null;
	private ArrayList<AccountObject> mPartners = null;
	private ArrayList<AttachFileObject> mAttachFiles = null;
	private ArrayList<HistoryObject> mHistories = null;

	public static TaskObject get(long id) throws SQLException {
		return TaskDAO.getInstance().get(id);
	}

	public TaskObject() {
	}

	public TaskObject(String name) {
		mName = name;
	}

	public void setId(long id) {
		this.mId = id;
	}

	public void setSerialId(long serialId) {
		this.mSerialId = serialId;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public void setHandlerId(long handlerId) {
		this.mHandlerId = handlerId;
	}

	public void setEstimate(int estimate) {
		this.mEstimate = estimate;
	}

	public void setRemains(int remains) {
		this.mRemains = remains;
	}

	public void setActual(int actual) {
		this.mActual = actual;
	}

	public void setNotes(String notes) {
		this.mNotes = notes;
	}

	public void setStatus(int status) {
		this.mStatus = status;
	}

	public void setProjectId(long projectId) {
		this.mProjectId = projectId;
	}

	public void setStoryId(long storyId) {
		this.mStoryId = storyId;
	}

	public void setCreateTime(long createtime) {
		this.mCreateTime = createtime;
	}

	public void setUpdateTime(long updatetime) {
		this.mUpdateTime = updatetime;
	}

	public void setPartnersId(ArrayList<Long> partnersId) {
		this.mPartnersId = partnersId;
	}

	public void setAttachFiles(ArrayList<AttachFileObject> attachFiles) {
		this.mAttachFiles = attachFiles;
	}

	public void setHistories(ArrayList<HistoryObject> histories) {
		this.mHistories = histories;
	}

	public void addPartner(long partnerId) {
		this.mPartnersId.add(partnerId);
	}

	public void addAttachFile(AttachFileObject attachFile) {
		this.mAttachFiles.add(attachFile);
	}

	public void addHistory(HistoryObject history) {
		this.mHistories.add(history);
	}

	public void removePartner(long partnerId) {
		this.mPartnersId.remove(partnerId);
	}

	public long getId() {
		return mId;
	}

	public long getSerialId() {
		return mSerialId;
	}

	public String getName() {
		return mName;
	}

	public long getHandlerId() {
		return mHandlerId;
	}

	public int getEstimate() {
		return mEstimate;
	}

	public int getRemains() {
		return mRemains;
	}

	public int getActual() {
		return mActual;
	}

	public String getNotes() {
		return mNotes;
	}

	public int getStatus() {
		return mStatus;
	}

	public int getStatus(Date date) {
		long time = date.getTime();
		time += 1000 * 60 * 60 * 24 - 1;
		date = new Date(time);

		int status = TaskObject.STATUS_UNCHECK;
		for (HistoryObject history : mHistories) {
			if (history.getHistoryType() == HistoryObject.TYPE_STATUS
					&& (new Date(history.getModifiedTime()).before(date))) {
				if (history.getNewValue().equals("\"Checked Out\"")) {
					status = TaskObject.STATUS_CHECK;
				} else if (history.getNewValue().equals("\"Done\"")) {
					status = TaskObject.STATUS_DONE;
				}
			}
		}
		return status;
	}

	public int getRemainsByDate(Date date) {
		return searchValue(HistoryObject.TYPE_REMAIMS, date);
	}

	public int getEstimateByDate(Date date) {
		return searchValue(HistoryObject.TYPE_ESTIMATE, date);
	}

	private int searchValue(int searchType, Date date) {
		int value = -1;
		for (HistoryObject history : mHistories) {
			if (history.getHistoryType() == searchType
					&& (new Date(history.getModifiedTime()).before(date))) {
				value = Integer.parseInt(history.getNewValue());
			}
		}
		return value;
	}

	public long getProjectId() {
		return mProjectId;
	}

	public long getStoryId() {
		return mStoryId;
	}

	public long getCreateTime() {
		return mCreateTime;
	}

	public long getUpdateTime() {
		return mUpdateTime;
	}

	public ArrayList<Long> getPartnersId() {
		if (mPartnersId == null) {
			mPartnersId = TaskDAO.getInstance().getPartnersId(mId);
		}
		return mPartnersId;
	}
	
	public ArrayList<AccountObject> getPartners() {
		if (mPartners == null) {
			mPartners = new ArrayList<AccountObject>();
			for (long partnerId : mPartnersId) {
				try {
					AccountObject partner = AccountDAO.getInstance().get(partnerId);
					mPartners.add(partner);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return mPartners;
	}

	public ArrayList<HistoryObject> getHistories() {
		if (mHistories == null) {
			try {
				mHistories = HistoryDAO.getInstance().getHistoriesByIssue(mId, IssueTypeEnum.TYPE_TASK);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return mHistories;
	}

	public ArrayList<AttachFileObject> getAttachFiles() {
		if (mAttachFiles == null) {
			mAttachFiles = AttachFileDAO.getInstance().getAttachFilesByTaskId(mId);
		}
		return mAttachFiles;
	}

	public int getDateStatus(Date date) {
		int status = -1;
		for (HistoryObject history : mHistories) {
			if (history.getHistoryType() == HistoryObject.TYPE_STATUS) {
				status = Integer.parseInt(history.getOldValue());
			}
		}
		return status;
	}

	public long getAssignedTime() {
		long assignedTime = 0;
		for (HistoryObject history : mHistories) {
			if (history.getHistoryType() == HistoryObject.TYPE_STATUS
					&& history.getNewValue().equals("\"Checked Out\"")) {
				if (assignedTime < history.getModifiedTime()) {
					assignedTime = history.getModifiedTime();
				}
			}
		}
		return assignedTime;
	}

	public long getDoneTime() {
		long doneTime = 0;
		for (HistoryObject history : mHistories) {
			if (history.getHistoryType() == HistoryObject.TYPE_STATUS
					&& history.getNewValue().equals("\"Done\"")) {
				if (doneTime < history.getModifiedTime()) {
					doneTime = history.getModifiedTime();
				}
			}
		}
		return doneTime;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject task = new JSONObject();
		JSONArray partners = new JSONArray();

		for (Long partnerId : mPartnersId) {
			partners.put(partnerId);
		}

		task.put(TaskEnum.NAME, mName).put(TaskEnum.ESTIMATE, mEstimate)
				.put(TaskEnum.ACTUAL, mActual).put(TaskEnum.STORY_ID, mStoryId)
				.put(TaskEnum.PROJECT_ID, mProjectId)
				.put(TaskEnum.NOTES, mNotes).put(TaskEnum.REMAIN, mRemains)
				.put(TaskEnum.STATUS, mStatus)
				.put(TaskEnum.SERIAL_ID, mSerialId).put(TaskEnum.ID, mId)
				.put(TaskEnum.CREATE_TIME, mCreateTime)
				.put(TaskEnum.UPDATE_TIME, mUpdateTime)
				.put("partners", partners);

		return task;
	}

	public String toString() {
		try {
			return toJSON().toString();
		} catch (JSONException e) {
			return "JSON Exception";
		}
	}

	@Override
	public void save() {
		if (isDataExists()) {
			TaskDAO.getInstance().create(this);
		} else {
			TaskDAO.getInstance().update(this);
		}
	}

	@Override
	public void reload() throws Exception {
		if (isDataExists()) {
			try {
				TaskObject task = TaskDAO.getInstance().get(mId);
				updateData(task);
			} catch (SQLException e) {
				System.out.println(TaskObject.class.getName() + ", reload(), "
						+ e.toString());
				e.printStackTrace();
			}
		} else {
			throw new Exception("Record not exists");
		}
	}

	@Override
	public boolean delete() {
		boolean success = TaskDAO.getInstance().delete(mId);
		if (success) {
			mId = -1;			
		}
		return success;
	}

	private boolean isDataExists() {
		if (mId > 0) {
			return true;
		}
		return false;
	}

	private void updateData(TaskObject task) {
		setSerialId(task.getSerialId());
		setName(task.getName());
		setNotes(task.getNotes());
		setProjectId(task.getProjectId());
		setStoryId(task.getStoryId());
		setEstimate(task.getEstimate());
		setRemains(task.getRemains());
		setActual(task.getActual());
		setHandlerId(task.getHandlerId());
		setStatus(task.getStatus());
		setCreateTime(task.getCreateTime());
		setUpdateTime(task.getUpdateTime());
//		setHistories(task.getHistories());
//		setPartnersId(task.getPartnersId());
//		setAttachFiles(task.getAttachFiles());
		mHistories = null;
		mPartnersId = null;
		mAttachFiles = null;
	}
}
