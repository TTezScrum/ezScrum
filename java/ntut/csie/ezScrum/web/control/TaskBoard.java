package ntut.csie.ezScrum.web.control;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import ntut.csie.ezScrum.iteration.core.ScrumEnum;
import ntut.csie.ezScrum.web.dataObject.ProjectObject;
import ntut.csie.ezScrum.web.dataObject.SprintObject;
import ntut.csie.ezScrum.web.dataObject.StoryObject;
import ntut.csie.ezScrum.web.dataObject.TaskObject;
import ntut.csie.ezScrum.web.logic.SprintBacklogLogic;
import ntut.csie.ezScrum.web.mapper.SprintBacklogMapper;
import ntut.csie.jcis.core.util.ChartUtil;
import ntut.csie.jcis.core.util.DateUtil;
import ntut.csie.jcis.resource.core.IProject;

public class TaskBoard {
	private final String STORY_CHART_FILE = "StoryBurnDown.png";
	private final String TASK_CHART_FILE = "TaskBurnDown.png";
	private final String NAME = "TaskBoard";

	private SprintBacklogMapper mSprintBacklogMapper;
	private SprintBacklogLogic mSprintBacklogLogic;
	private ArrayList<StoryObject> m_dropedStories;
	private ArrayList<TaskObject> m_dropedTasks;	
	private ArrayList<StoryObject> mStories;
	private LinkedHashMap<Date, Double> mDateToStoryIdealPoint;
	private LinkedHashMap<Date, Double> mDateToStoryPoint;
	private LinkedHashMap<Date, Double> mDateToTaskIdealPoint;
	private LinkedHashMap<Date, Double> mDateToTaskRealPoint;
	private Date mCurrentDate = new Date();
	private Date mGeneratedTime = new Date();
	final private long mOneDay = ScrumEnum.DAY_MILLISECOND;

	public TaskBoard(SprintBacklogLogic sprintBacklogLogic, SprintBacklogMapper sprintBacklogMapper) {
		mSprintBacklogLogic = sprintBacklogLogic;
		mSprintBacklogMapper = sprintBacklogMapper;
		init();
	}

	// ======================= for unit test ===========================
	public LinkedHashMap<Date, Double> getStoryRealPointMap() {
		return mDateToStoryPoint;
	}

	public LinkedHashMap<Date, Double> getTaskRealPointMap() {
		return mDateToTaskRealPoint;
	}

	public LinkedHashMap<Date, Double> getStoryIdealPointMap() {
		return mDateToStoryIdealPoint;
	}

	public LinkedHashMap<Date, Double> getTaskIdealPointMap() {
		return mDateToTaskIdealPoint;
	}

	private void init() {
		// 取得目前最新的Story與Task狀態
		mStories = mSprintBacklogLogic.getStoriesSortedByImpInSprint();
		m_dropedStories = mSprintBacklogMapper.getDroppedStories();
		ProjectObject project = mSprintBacklogMapper.getProject();
		m_dropedTasks = mSprintBacklogMapper.getDroppedTasks(project.getId());
		if (mSprintBacklogMapper != null) {
			// Sprint的起始與結束日期資訊
			Date sprintStartWorkDate = mSprintBacklogLogic.getSprintStartWorkDate();
			Date sprintEndWorkDate = mSprintBacklogLogic.getSprintEndWorkDate();
			Date sprintEndDate = mSprintBacklogMapper.getSprintEndDate();

			if (sprintStartWorkDate == null || sprintEndWorkDate == null || sprintEndDate == null) {
				return;
			}

			Calendar indexDate = Calendar.getInstance();
			indexDate.setTime(sprintStartWorkDate);

			mDateToStoryIdealPoint = new LinkedHashMap<Date, Double>();	// Story的理想線
			mDateToTaskIdealPoint = new LinkedHashMap<Date, Double>();	// Task的理想線
			mDateToStoryPoint = new LinkedHashMap<Date, Double>();		// Story的真實線
			mDateToTaskRealPoint = new LinkedHashMap<Date, Double>();	// Task的真實線
			
			double[] initPoint = getPointByDate(sprintStartWorkDate);	// 第一天Story與Task的點數
			int dayOfSprint = mSprintBacklogLogic.getSprintWorkDays();	// 扣除假日後，Sprint的總天數W
			long endTime = sprintEndWorkDate.getTime();					// End Time
			long today = mCurrentDate.getTime();						// 今天的日期，如果今天已經在EndDate之後，那就設為EndDate
			int sprintDayCount = 0;										// Sprint中第幾天的Counter
			
			if (mCurrentDate.getTime() > sprintEndDate.getTime()) {
				// end date 為當日的 00:00:00 ，所以還要加入OneDay，這樣時間才會是 00:00:00 - 23:59:59
				today = sprintEndDate.getTime() + mOneDay;
			}
			
			// 每一天的理想與真實點數
			while (!(indexDate.getTimeInMillis() > endTime) || indexDate.getTimeInMillis() == endTime) {
				Date key = indexDate.getTime();
				// 扣除假日
				if (!DateUtil.isHoliday(key)) {
					// 記錄Story與Task理想線的點數
					// 理想線直線方程式 y = - (起始點數 / 總天數) * 第幾天 + 起始點數
					mDateToStoryIdealPoint.put(key, (((-initPoint[0]) / (dayOfSprint - 1)) * sprintDayCount) + initPoint[0]);
					mDateToTaskIdealPoint.put(key, (((-initPoint[1]) / (dayOfSprint - 1)) * sprintDayCount) + initPoint[1]);

					// 記錄Story與Task實際線的點數
					// 只取出今天以前的資料
					if (indexDate.getTimeInMillis() < today) {
						double point[] = getPointByDate(key);
						mDateToStoryPoint.put(key, point[0]);
						mDateToTaskRealPoint.put(key, point[1]);
					} else {
						mDateToStoryPoint.put(key, null);
						mDateToTaskRealPoint.put(key, null);
					}
					sprintDayCount++;
				}
				indexDate.add(Calendar.DATE, 1);
			}
		}
	}

	private double getStoryPoint(Date date, StoryObject story) throws Exception {
		double point = 0;
		// 確認這個Story在那個時間是否存在
		if(story.checkVisableByDate(date)){
			try{
				point = story.getStoryPointByDate(date);
			}catch(Exception e){
				return 0;
			}
		} else {
			// 表示這個Story在當時不存在於這個Sprint裡面
			throw new Exception("this story isn't at this sprint");
		}
		return point;
	}

	private double getTaskPoint(Date date, TaskObject task)throws Exception {
		double point = 0;
		if(task.checkVisableByDate(date))
		{
			try{
				point = task.getTaskPointByDate(date);
			}catch (Exception e1) {
				// 如果沒有，那就回傳 0
				return 0;
			}
			
		}else{
			throw new Exception("this task isn't at this sprint");
		}
		return point;
	}

	private double[] getPointByDate(Date date) {
		double[] point = {0, 0};

		// 依照Type取出當天的Story或者是Task來進行計算
		// 因為輸入的日期為當日的0:0:0,但在23:59:59之前也算當日，所以必需多加一日做為當天的計算
		Date endDate = new Date(date.getTime() + mOneDay);

		// 尋訪現有的所有Story
		for (StoryObject story : mStories) {
			// 已經closed的Story就不用算他的點數啦，連Task都省掉了
			if (story.getStatus(endDate) == StoryObject.STATUS_DONE) {
				continue;
			}

			try {
				// 計算Story點數
				point[0] += getStoryPoint(endDate, story);

				// 計算Task點數
				// 取得這個Story底下的Task點數
				ArrayList<TaskObject> tasks = story.getTasks();
				for (TaskObject task : tasks) {
					if(task.getStatus(endDate) == TaskObject.STATUS_DONE) {
						continue;
					}
					point[1] += getTaskPoint(endDate, task);
				}
			} catch (Exception e) {
				// 如果會有Exception表示此時間Story不在此Sprint中，所以getTagValue回傳null乘parseDouble產生exception
				continue;
			}
		}
		
		//Visit all DropStory
		for(StoryObject story:m_dropedStories)
		{
			if (story.getStatus(endDate) == StoryObject.STATUS_DONE) {
				continue;
			}
			
			
			try{
				//Calculate story Point
				point[0] += getStoryPoint(endDate, story);
				
				//Calculate task Point
				
				ArrayList<TaskObject> tasks = story.getTasks();
				for (TaskObject task : tasks) {
					if(task.getStatus(endDate) == TaskObject.STATUS_DONE) {
						continue;
					}
					point[1] += getTaskPoint(endDate, task);
				}
			}catch(Exception e){
				// 如果會有Exception表示此時間Story不在此Sprint中，所以getTagValue回傳null乘parseDouble產生exception
				continue;
			}
			
		}
		
		for(TaskObject task : m_dropedTasks){
			if(task.getStatus(endDate) == TaskObject.STATUS_DONE){
				continue;
			}
			
			try{
				point[1] += getTaskPoint(endDate, task);
			}catch(Exception e){
				continue;
			}
		}
		return point;
	}

	public String getSprintGoal() {
		return mSprintBacklogMapper.getSprintGoal();
	}

	public long getSprintId() {
		return mSprintBacklogMapper.getSprintId();
	}

	public String getStoryPoint() {
		SprintObject sprint = mSprintBacklogMapper.getSprint();
		if(sprint == null){
			return "0.0 / 0.0";
		}
		return sprint.getStoryUnclosedPoints() + " / " + sprint.getTotalStoryPoints();
	}

	public String getTaskPoint() {
		SprintObject sprint = mSprintBacklogMapper.getSprint();
		if(sprint == null){
			return "0.0 / 0.0";
		}
		return sprint.getTaskRemainsPoints() + " / " + sprint.getTotalTaskPoints();
	}

	public String getInitialStoryPoint() {
		SprintObject sprint = mSprintBacklogMapper.getSprint();
		if(sprint == null){
			return "0.0 / 0.0";
		}
		return (getPointByDate(mSprintBacklogMapper.getSprintStartDate())[0]) + " / " + sprint.getLimitedPoint();
	}

	public String getInitialTaskPoint() {
		return (getPointByDate(mSprintBacklogMapper.getSprintStartDate())[1]) + " / -";
	}

	public ArrayList<StoryObject> getStories() {
		return mStories;
	}

	public void setStories(ArrayList<StoryObject> stories) {
		mStories = stories;
	}

	public String getStoryChartLink() {
		ProjectObject project = mSprintBacklogMapper.getProject();
		// workspace/project/_metadata/TaskBoard/ChartLink
		String chartPath = "./Workspace/" + project.getName() + "/"
		        + IProject.METADATA + "/" + NAME + File.separator + "Sprint"
		        + getSprintId() + File.separator + STORY_CHART_FILE;

		// 繪圖
		drawGraph(ScrumEnum.STORY_ISSUE_TYPE, chartPath, "Story Points");

		String link = "./Workspace/" + project.getName() + "/"
		        + IProject.METADATA + "/" + NAME + "/Sprint"
		        + getSprintId() + "/" + STORY_CHART_FILE;

		return link;
	}

	public String getTaskChartLink() {
		ProjectObject project = mSprintBacklogMapper.getProject();
		// workspace/project/_metadata/TaskBoard/Sprint1/ChartLink
		String chartPath = "./Workspace/" + project.getName() + "/"
		        + IProject.METADATA + "/" + NAME + File.separator + "Sprint"
		        + getSprintId() + File.separator + TASK_CHART_FILE;

		// 繪圖
		drawGraph(ScrumEnum.TASK_ISSUE_TYPE, chartPath, "Remaining Hours");

		String link = "./Workspace/" + project.getName() + "/"
		        + IProject.METADATA + "/" + NAME + "/Sprint"
		        + getSprintId() + "/" + TASK_CHART_FILE;

		return link;
	}

	private synchronized void drawGraph(String type, String chartPath, String Y_axis_value) {
		// 設定圖表內容
		ChartUtil chartUtil = new ChartUtil((type
		        .equals(ScrumEnum.TASK_ISSUE_TYPE) ? "Tasks" : "Stories")
		        + " Burndown Chart in Sprint #" + getSprintId(),
		        mSprintBacklogMapper.getSprintStartDate(), new Date(mSprintBacklogMapper.getSprintEndDate().getTime() + 24 * 3600 * 1000));

		chartUtil.setChartType(ChartUtil.LINECHART);

		// TODO:要新增的data set
		if (type.equals(ScrumEnum.TASK_ISSUE_TYPE)) {
			chartUtil.addDataSet("current", mDateToTaskRealPoint);
			chartUtil.addDataSet("ideal", mDateToTaskIdealPoint);
		} else {
			chartUtil.addDataSet("current", mDateToStoryPoint);
			chartUtil.addDataSet("ideal", mDateToStoryIdealPoint);
		}
		chartUtil.setInterval(1);
		chartUtil.setValueAxisLabel(Y_axis_value);
		// 依照輸入的順序來呈現顏色
		Color[] colors = {Color.RED, Color.GRAY};
		chartUtil.setColor(colors);

		float[] dashes = {8f};
		BasicStroke[] strokes = {
		        new BasicStroke(1.5f),
		        new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 16f, dashes, 0.f)};
		chartUtil.setStrokes(strokes);

		// 產生圖表
		chartUtil.createChart(chartPath);
	}

	public String getGeneratedTime() {
		return DateUtil.format(mGeneratedTime, DateUtil._16DIGIT_DATE_TIME);
	}
}
