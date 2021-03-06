package activity.classifier.activity;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import activity.classifier.common.ActivityNames;
import activity.classifier.common.Constants;
import activity.classifier.common.StringComparator;
import activity.classifier.db.ActivitiesTable;
import activity.classifier.db.SqlLiteAdapter;
import activity.classifier.rpc.Classification;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class ChartHelper {
	
	/**
	 * The durations of the columns displayed
	 * 
	 * PLEASE NOTE: THE DURATIONS SHOULD AT ALL TIMES BE SORTED
	 * IN AN INCREASING ORDER. I.E. THE SMALLEST ITEM FIRST,
	 * AND LARGEST LAST.
	 */
	private final static long COL_DURATIONS[] = new long[] {
		1*60*60*1000L,
		4*60*60*1000L,
		24*60*60*1000L,
	};
	
	public static class ChartData {
		
		public final int numOfActivities;
		public final int numOfDurations;
		public final float[][] percentageMatrix;
		
		public ChartData(int numOfActivities) {
			this.numOfDurations = COL_DURATIONS.length;
			this.numOfActivities = numOfActivities;
			this.percentageMatrix = new float[COL_DURATIONS.length][numOfActivities];
		}
		
	}
	
	private final int NUM_OF_DATA_SETS = 1;
	
	private ChartData[] dataSets = new ChartData[NUM_OF_DATA_SETS];
	
	// the index of the ChartData in the dataSets array, where the chart should load from
	private int currentLoadData = -1;
	
	//	an index of the activities available to the system
	private Map<String,Integer> activityIndexes;
	
	//	map of activity names to nice names
	private Map<String,String> activityNiceNames;
	
	private Context context;
	private SqlLiteAdapter sqlLiteAdapter;
	private ActivitiesTable activitiesTable;
	private Set<String> allActivities;
	
	//	reusable instances
	private Classification classification = new Classification();
	public long[][] sumMatrix;
	
	private int col_size;
	private int row_size;
	
	public int getColumnSize(){
		return col_size;
	}
	public int getRowSize(){
		return row_size;
	}
	public ChartHelper(Context context) {
		this.context = context;
		this.sqlLiteAdapter = SqlLiteAdapter.getInstance(context);
		this.activitiesTable = this.sqlLiteAdapter.getActivitiesTable();
		this.allActivities = ActivityNames.getAllActivities(context);
		
		this.sumMatrix = new long[COL_DURATIONS.length][allActivities.size()];
		this.col_size = COL_DURATIONS.length;
		this.row_size = allActivities.size();
		
		this.activityIndexes = new TreeMap<String,Integer>(new StringComparator(false));
		{
			int index = 0;
			for (String activity:allActivities) {
				this.activityIndexes.put(activity, index);
				++index;
			}
		}
		
		this.activityNiceNames = new TreeMap<String,String>(new StringComparator(false));
		for (String activity:allActivities) {
			String niceName = Classification.getNiceName(context, activity);
			this.activityNiceNames.put(activity, niceName);
		}
		
		for (int i=0; i<NUM_OF_DATA_SETS; ++i)
			dataSets[i] = new ChartData(this.allActivities.size());
	}
	
	public Map<String, Integer> getActivityIndexes(){
		return activityIndexes;
	}
	
	public Map<String, String> getActivityNiceNames(){
		return activityNiceNames;
	}
	
	public ChartData computeData()
	{
		final long periodStart = System.currentTimeMillis();
		
		long maxDuration = COL_DURATIONS[COL_DURATIONS.length-1];
		
		final long periodEnd = periodStart - maxDuration;
		
		for (int i=0; i<COL_DURATIONS.length; ++i) {
			for (int j=0; j<allActivities.size(); ++j) {
				sumMatrix[i][j] = 0;
			}
		}
		activitiesTable.loadAllBetween(periodStart, periodEnd, classification,
				new ActivitiesTable.ClassificationDataCallback() {
					@Override
					public void onRetrieve(Classification classification) {
						if (!activityIndexes.containsKey(classification.getClassification())) {
							Log.e(Constants.DEBUG_TAG, "ERROR: UNKNOWN ACTIVITY '"+classification.getClassification()+"' FOUND");
							return;
						}
						int index = activityIndexes.get(classification.getClassification());
						long howLongAgoStarted = periodStart - classification.getStart();
						long howLongAgoEnded = periodStart- classification.getEnd();
						for (int i=0; i<COL_DURATIONS.length; ++i) {
							long colDuration = COL_DURATIONS[i];
							long activityDuration=0;
							if (colDuration>=howLongAgoStarted) {
								activityDuration = classification.getEnd() - classification.getStart();
							}else{
								if(colDuration>=howLongAgoEnded){
									activityDuration = colDuration - howLongAgoEnded;
								}
							}
							sumMatrix[i][index] +=  activityDuration;
						}
					}
				}
			);
		
		
		for (int i=0; i<COL_DURATIONS.length; ++i) {
			long totalNonSystem = 0;
			for (String activity:allActivities) {
				if (!ActivityNames.OFF.equals(activity)) {
					int index = activityIndexes.get(activity);
					totalNonSystem += sumMatrix[i][index];
				}
			}
			
			int indexOff = activityIndexes.get(ActivityNames.OFF);
			sumMatrix[i][indexOff] = COL_DURATIONS[i] - totalNonSystem;
		}
		
		int currentComputeData = (currentLoadData+1)%NUM_OF_DATA_SETS;
		
		ChartData data = dataSets[currentComputeData];
		
		for (int i=0; i<COL_DURATIONS.length; ++i) {
			for (int j=0; j<allActivities.size(); ++j) {
				data.percentageMatrix[i][j] = 100.0f * (float)sumMatrix[i][j] / (float)COL_DURATIONS[i];
				Log.i("info",sumMatrix[i][j]+"");
			}
		}
		
		currentLoadData = currentComputeData;
		return data;
	}
	
}
