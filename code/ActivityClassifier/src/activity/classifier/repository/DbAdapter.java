
package activity.classifier.repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import activity.classifier.common.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
/**
 * class for creating SQLite database in the device memory.
 * and allow other classes {@link ActivityQueries} {@link OptionQueries} to use this functionality.
 * 
 * 
 * 
 * @author Justin Lee
 * 			
 */
public class DbAdapter {

	//	used to lock access to database to one thread at a time,
	//		TODO: Consider re-coding database access to avoid bugs due to multiple thread access.
	private static final java.util.concurrent.locks.ReentrantLock reentrantLock = new ReentrantLock(true);
	
	//	this exception is created when a database is locked (during an open() operation),
	//		but thrown the next time another thread attempts to lock it but finds that it is locked.
	//		this is meant to help in finding database dead-locks.
	private static RuntimeException deadLockFound = null;

	public static final String KEY_ROWID = "_id";

	/**
	 * Column names in startinfo Table
	 */
	public static final String KEY_IS_SERVICE_STARTED = "isServiceStarted";
	public static final String KEY_IS_CALIBRATED = "isCalibrated";
	public static final String KEY_VALUE_OF_GRAVITY = "valueOfGravity";
	public static final String KEY_SD_X = "sdX";
	public static final String KEY_SD_Y = "sdY";
	public static final String KEY_SD_Z = "sdZ";
	public static final String KEY_MEAN_X = "meanX";
	public static final String KEY_MEAN_Y = "meanY";
	public static final String KEY_MEAN_Z = "meanZ";
	public static final String KEY_COUNT = "count";
	public static final String KEY_ALLOWED_MULTIPLES_OF_SD = "allowedMultiplesOfSd";
	public static final String KEY_IS_ACCOUNT_SENT = "isAccountSent";
	public static final String KEY_IS_WAKE_LOCK_SET = "isWakeLockSet";
	public static final String KEY_USE_AGGREGATOR = "useAggregator";

	/**
	 * Column names in activity Table
	 */
	public static final String KEY_ACTIVITY = "activity";
	public static final String KEY_IS_CHECKED = "isChecked";
	public static final String KEY_STARTDATE = "startDate";
	public static final String KEY_END_DATE = "endDate";

	/**
	 * Column names in testav Table
	 */
	public static final String KEY_TEST_MEAN_X = "meanx";
	public static final String KEY_TEST_MEAN_Y = "meany";
	public static final String KEY_TEST_MEAN_Z = "meanz";
	public static final String KEY_TEST_HOR_MEAN = "mean_hor";
	public static final String KEY_TEST_VER_MEAN = "mean_ver";
	public static final String KEY_TEST_HOR_RANGE = "range_hor";
	public static final String KEY_TEST_VER_RANGE = "range_ver";
	public static final String KEY_TEST_HOR_SD = "sd_hor";
	public static final String KEY_TEST_VER_SD = "sd_ver";
	public static final String KEY_TEST_CLASSIFIER_ALGO_OUTPUT = "classifier_algo_output";
	public static final String KEY_TEST_FINAL_CLASSIFIER_OUTPUT = "final_classifier_output";
	public static final String KEY_TEST_FINAL_SYSTEM_OUTPUT = "final_system_output";
	public static final String KEY_TEST_CREATED_AT = "createdAt";

	private static final String TAG = "DbAdapter";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase _db;  

	/**
	 * startinfo Table creation sql statement
	 */
	private static final String DATABASE_STARTINFO_CREATE =
		"create table startinfo (" +
			KEY_ROWID+" integer primary key autoincrement, " +
			KEY_IS_SERVICE_STARTED+" text not null, " +
			KEY_IS_CALIBRATED+" text not null, " +
			KEY_VALUE_OF_GRAVITY+" text not null, " +
			KEY_SD_X+" text not null, " +
			KEY_SD_Y+" text not null, " +
			KEY_SD_Z+" text not null, " +
			KEY_MEAN_X+" text not null, " +
			KEY_MEAN_Y+" text not null, " +
			KEY_MEAN_Z+" text not null, " +
			KEY_COUNT+" text not null, " +
			KEY_ALLOWED_MULTIPLES_OF_SD+" text not null, " + 
			KEY_IS_ACCOUNT_SENT+" text not null, " +
			KEY_IS_WAKE_LOCK_SET+" text not null, " +
			KEY_USE_AGGREGATOR+" text not null " + 
		");";

	/**
	 *  sql statement for initialising values in startinfo Table 
	 */
	private static final String DATABASE_STARTINFO_INIT =
		"insert into startinfo values (" +
			"null, " +		//	_id	
			"1, " +			//	isServiceStarted: initially show that the service was started so as to start it automatically
			"0, " +			//	isCalibrated
			Constants.GRAVITY+", " +	// valueOfGravity
			Constants.CALIBARATION_ALLOWED_BASE_DEVIATION+", " +	//	sdX
			Constants.CALIBARATION_ALLOWED_BASE_DEVIATION+", " +	//	sdY
			Constants.CALIBARATION_ALLOWED_BASE_DEVIATION+", " +	//	sdZ
			"0.0, " +	//	meanX
			"0.0, " +	//	meanY
			"0.0, " +	//	meanZ
			"0, " +		//	count
			Constants.CALIBARATION_ALLOWED_MULTIPLES_DEVIATION+", " +	//	allowedMultiplesOfSd
			"0, " +	//	isAccountSent
			"0," +	//	isWakeLockSet
			"1 " +	//	useAggregator
			" );";
	

	/**
	 * activity Table creation sql statement
	 */
	private static final String DATABASE_ACTIVIT_CREATE =
		"create table activity (" +
			KEY_ROWID+" integer primary key autoincrement, " +
			KEY_ACTIVITY+" text not null, " +
			KEY_STARTDATE+" DATE not null, " +
			KEY_END_DATE+" DATE not null, " +
			KEY_IS_CHECKED+" integer not null " +
		");";

	static String date = Constants.DB_DATE_FORMAT.format(new Date());
//	/**
//	 *  sql statement for initialising values in activity Table 
//	 */
//	private static final String DATABASE_ACTIVITY_INIT =
//		"insert into activity values (null, END, "+date+", "+date+", 1);";


	/**
	 * testav Table creation sql statement
	 */
	private static final String DATABASE_TESTAV_CREATE =
		"create table testav (" +
		KEY_ROWID+" long primary key, " +
		KEY_STARTDATE+" DATE not null, " +
		KEY_TEST_MEAN_X+" float not null, " + 
		KEY_TEST_MEAN_Y+" float not null, " +
		KEY_TEST_MEAN_Z+" float not null, " +
		KEY_TEST_HOR_MEAN+" float null, " +
		KEY_TEST_VER_MEAN+" float null, " +
		KEY_TEST_HOR_RANGE+" float null, " +
		KEY_TEST_VER_RANGE+" float null, " +
		KEY_TEST_HOR_SD+" float null, " +
		KEY_TEST_VER_SD+" float null, " +
		KEY_TEST_CLASSIFIER_ALGO_OUTPUT+" TEXT null, " +
		KEY_TEST_FINAL_CLASSIFIER_OUTPUT+" TEXT null, " +
		KEY_TEST_FINAL_SYSTEM_OUTPUT+" TEXT null, " +
		KEY_TEST_CREATED_AT+" long not null " +
		");";

	private static  String DATABASE_NAME = "activityrecords.db";

	/**
	 * Table names
	 */
	private static final String DATABASE_STARTINFO_TABLE = "startinfo";
	private static final String DATABASE_ACTIVITY_TABLE = "activity";
	private static final String DATABASE_TESTAV_TABLE = "testav";

	private static final int DATABASE_VERSION = 3;

	private final Context mCtx;
	
	/**
	 * Execute sql statement to create tables & initialise startinfo table
	 * @author Justin
	 *
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.w(TAG, "Creating Database Tables");
			
			db.execSQL(DATABASE_STARTINFO_CREATE);
			db.execSQL(DATABASE_STARTINFO_INIT);
			db.execSQL(DATABASE_ACTIVIT_CREATE);
//			db.execSQL(DATABASE_ACTIVITY_INIT);
			db.execSQL(DATABASE_TESTAV_CREATE);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_STARTINFO_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_ACTIVITY_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TESTAV_TABLE);
			onCreate(db);

		}
	}

	/**
	 * Initialise Context
	 * @param ctx context from Activity or Service classes
	 */
	public DbAdapter(Context ctx) {
		isActivityBetweenToday=false;
		this.mCtx = ctx;

	}

	/**
	 * Make the database readable/writable
	 * 
	 * Edit by Umran: <br/>
	 * Please note that this function is a blocking function.
	 * It will not return until any other thread using the
	 * database calls {@link #close()}.
	 * Hence make sure to follow up each {@link #open()} call
	 * with a {@link #close()} call. 
	 * 
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		if (!reentrantLock.tryLock()) {
			try {
				throw new RuntimeException(
						"'"+Thread.currentThread().getName()+"' thread attempting to open database when it's still open!\n" +
						"Waiting for it to be open.\n");
			} catch (Exception e) {
				Log.v(Constants.DEBUG_TAG, "Error While Attempting to Open Database", e);
			}
			reentrantLock.lock();
			Log.d(Constants.DEBUG_TAG, "Database open.");
		}
		
		deadLockFound = new RuntimeException("Database last openned here, by '"+Thread.currentThread().getName()+"' thread");
		
		mDbHelper = new DatabaseHelper(mCtx);
		try {
			_db = mDbHelper.getWritableDatabase();
		} catch (SQLException e) {
			Log.e(Constants.DEBUG_TAG, "Error while trying to open the database", e);
			throw e;
		}
	}

	/**
	 * Close the database access 
	 */
	public void close() {
		mDbHelper.close();
		deadLockFound = null;
		reentrantLock.unlock();
	}


	//  ---------------------Start Start-info Table----------------------------------

	/**
	 * Fetch the value from a specific column 
	 * @param fieldName column name
	 * @return String data type value 
	 * @throws SQLException
	 */
	public synchronized String fetchFromStartTableString(String fieldName) throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_STARTINFO_TABLE, 
					new String[] { KEY_ROWID, fieldName }, KEY_ROWID + "=" + 1, null, null, null, null, null);

		if(mCursor!=null)
			mCursor.moveToNext();
		
		String value = mCursor.getString(1); 
		
		mCursor.close();

		return value;
	}

	/**
	 * Fetch the value from a specific column 
	 * @param fieldName column name
	 * @return Integer data type value 
	 * @throws SQLException
	 */
	public synchronized  int fetchFromStartTableInt(String fieldName) throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_STARTINFO_TABLE, 
					new String[] { KEY_ROWID, fieldName }, KEY_ROWID + "=" + 1, null, null, null, null, null);

		if(mCursor!=null)
			mCursor.moveToNext();
		
		int value =(int) Float.valueOf(mCursor.getString(1).trim()).floatValue(); 
		
		mCursor.close();

		return value;
	}

	/**
	 * Fetch the value from a specific column 
	 * @param fieldName column name
	 * @return Float data type value 
	 * @throws SQLException
	 */
	public synchronized  float fetchFromStartTableFloat(String fieldName) throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_STARTINFO_TABLE, 
					new String[] { KEY_ROWID, fieldName }, KEY_ROWID + "=" + 1, null, null, null, null, null);

		mCursor.moveToNext();
		
		float value = Float.valueOf(mCursor.getString(1).trim()).floatValue(); 

		mCursor.close();
		
		return value;
	}

	/**
	 * Update changed values in a specific column
	 * @param fieldName column name
	 * @param value changed value
	 * @return true if update is successfully completed
	 */
	public synchronized  boolean updateToSelectedStartTable(String fieldName, String value) {
		ContentValues args = new ContentValues();
		args.put(fieldName, value);		
		return _db.update(DATABASE_STARTINFO_TABLE, args, KEY_ROWID + "=" + 1, null) > 0;
	}


	//  ---------------------End Start-info Table----------------------------------

	//  ---------------------Start Activity Table----------------------------------

	public synchronized int fetchSizeOfRow()throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_ACTIVITY_TABLE, 
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_STARTDATE,KEY_END_DATE,KEY_IS_CHECKED },  null, null, null, null, null, null);
		int count=mCursor.getCount();
		mCursor.close();
		return count;
	}
	/**
	 * Insert new activity information 
	 * @param activity activity name
	 * @param time date and time
	 * @param isChecked activity sent state
	 * @return the row ID of the newly inserted row, or -1 if an error occurred 
	 */
	public synchronized  long insertToActivityTable(String activity, String time, int isChecked) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_ACTIVITY, activity);
		initialValues.put(KEY_STARTDATE, time);
		initialValues.put(KEY_END_DATE, time);
		initialValues.put(KEY_IS_CHECKED,isChecked);

		return _db.insert(DATABASE_ACTIVITY_TABLE, null, initialValues);
	}

	/**
	 * Delete activity information in selected row
	 * @param rowId row ID
	 * @return 
	 */
	public synchronized  boolean deleteActivity(long rowId) {

		Log.i("Delete called", "value__" + rowId);
		return _db.delete(DATABASE_ACTIVITY_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Fetch all un-posted activities
	 * @param isChecked activity sent state
	 * @return cursor that contain activity information
	 * @throws SQLException
	 */
	public synchronized  Cursor fetchUnCheckedItemsFromActivityTable(int isChecked) throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_ACTIVITY_TABLE, 
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_STARTDATE,KEY_END_DATE,KEY_IS_CHECKED }, KEY_IS_CHECKED + "=" + isChecked, null, null, null, null, null);
		return mCursor;
	}
	private boolean isActivityBetweenToday;

	public synchronized  ArrayList<String[]> fetchTodayItemsFromActivityTable(Date todayTime, Date currDate) throws SQLException {
		
		String currTime = Constants.DB_DATE_FORMAT.format(currDate);
		String dayAgoTime = Constants.DB_DATE_FORMAT.format(todayTime);
		
		Cursor mCursor =
			_db.query(
					true, DATABASE_ACTIVITY_TABLE, 
					new String[] { 
					KEY_ROWID, KEY_ACTIVITY,KEY_STARTDATE, KEY_END_DATE,KEY_IS_CHECKED
					},
					"("+KEY_STARTDATE+" BETWEEN "+"'"+dayAgoTime+"' AND '"+currTime+"')",
					null, null, null, null, null
					);
		
		ArrayList<String[]> todayItems = new ArrayList<String[]>();
		int i =0;
		while(mCursor.moveToNext()) {
			String[] tempPara = {mCursor.getString(0),mCursor.getString(1),mCursor.getString(2),mCursor.getString(3)};
			todayItems.add(tempPara);
			i++;
		}
		
		mCursor.close();
		isActivityBetweenToday=false;

		return todayItems;
	}

	public synchronized ArrayList<String> fetchBetweenTimeItemsFromActivityTable(Date timeDiff,int state, Integer[] times, String itemName) throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_ACTIVITY_TABLE, 
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_STARTDATE,KEY_END_DATE,KEY_IS_CHECKED }, "("+KEY_ROWID + ">=" + times[0] + " AND " +  KEY_ROWID + "<=" + times[1]+")" + " AND " +  KEY_ACTIVITY + " = "+itemName+"", null, null, null, null, null);
		ArrayList<String> items = new ArrayList<String>();
		int i=0;
		while(mCursor.moveToNext()) {
			if(i==0 && state==1){ 
				items.add(
						Integer.parseInt(mCursor.getString(0))+","+
						mCursor.getString(1)+","+
						Constants.DB_DATE_FORMAT.format(timeDiff)+","+
						mCursor.getString(3));

			}else{

				items.add(Integer.parseInt(mCursor.getString(0))+","+mCursor.getString(1)+","+mCursor.getString(2)+","+mCursor.getString(3));
				//	    	Log.i("query",mCursor.getString(0)+" "+mCursor.getString(1)+" "+mCursor.getString(2)+" "+mCursor.getString(3));
				//	    	Log.i("uncheckedItems",items.get(i));
			}
			i++;
		}
		mCursor.close();
		return items;
	}


	/**
	 * 
	 * @param rowId activity table row ID
	 * @return activity name related to the row ID
	 */
	public synchronized  String fetchLastItemNames(long rowId){
		Cursor mCursor = _db.query(true, DATABASE_ACTIVITY_TABLE,
				new String[] { KEY_ACTIVITY}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		mCursor.moveToNext();
		String activityName = mCursor.getCount()>0?mCursor.getString(0):null;
		mCursor.close();
		return activityName;
	}

	/**
	 * 
	 * @param rowId activity table row ID
	 * @return activity end date related to the row ID
	 */
	public synchronized  String fetchLastItemEndDate(long rowId){
		Cursor mCursor = _db.query(true, DATABASE_ACTIVITY_TABLE,
				new String[] { KEY_END_DATE}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		String activityDate = "";
		if (mCursor.moveToNext())
			activityDate = mCursor.getString(0);
		mCursor.close();
		return activityDate;
	}

	/**
	 * 
	 * @param rowId activity table row ID
	 * @return activity end date related to the row ID
	 */
	public synchronized  String fetchLastItemStartDate(long rowId){
		Cursor mCursor = _db.query(true, DATABASE_ACTIVITY_TABLE,
				new String[] { KEY_STARTDATE}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		String activityDate = "";
		if (mCursor.moveToNext())
			activityDate = mCursor.getString(0);
		mCursor.close();
		return activityDate;
	}

	public synchronized  Cursor fetchItemsFromActivityTable(String itemName){
		Cursor mCursor = _db.query(true, DATABASE_ACTIVITY_TABLE,
				new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_STARTDATE,KEY_END_DATE,KEY_IS_CHECKED }, KEY_ACTIVITY + "=" + itemName, null, null, null, null, null);

		mCursor.moveToNext();
		
		return mCursor;
	}
	
	/**
	 * Update values in selected row
	 * @param rowId row ID
	 * @param activity activity name
	 * @param date date and time
	 * @param isChecked activity sent state
	 * @return true if update is successfully completed
	 */
	public synchronized  boolean updateItemsToActivityTable(long rowId, String activity, String startDate, String endDate, int isChecked) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACTIVITY, activity);
		//        args.put(KEY_TIME, time);
		args.put(KEY_STARTDATE, startDate);
		args.put(KEY_END_DATE, endDate);
		args.put(KEY_IS_CHECKED, isChecked);
		return _db.update(DATABASE_ACTIVITY_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * 
	 * @param rowId activity table row ID
	 * @param endDate activity end date
	 * @return true if update is successfully completed
	 */
	public synchronized  boolean updateNewItemstoActivityTable(long rowId, String endDate) {
		ContentValues args = new ContentValues();
		args.put(KEY_END_DATE, endDate);
		return _db.update(DATABASE_ACTIVITY_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
	//  ---------------------End Activity Table----------------------------------

	//  ---------------------Start AVERAGE TEST Table----------------------------------

	/**
	 * Insert average acceleration, previous acceleration, and standard deviation.
	 * @param sdx average standard deviation of X axis
	 * @param sdy average standard deviation of Y axis
	 * @param sdz average standard deviation of Z axis
	 * @param lastx previous average acceleration of X axis
	 * @param lasty previous average acceleration of Y axis
	 * @param lastz previous average acceleration of Z axis
	 * @param currx average acceleration of X axis
	 * @param curry average acceleration of Y axis
	 * @param currz average acceleration of Z axis
	 * @return the row ID of the newly inserted row, or -1 if an error occurred 
	 */
	public synchronized long insertValuesToTestAVTable(
				long sampleTime, 
				float meanx,
				float meany,
				float meanz,
				Float mean_hor,
				Float mean_ver,
				Float range_hor,
				Float range_ver,
				Float sd_hor,
				Float sd_ver,
				String classifier_algo_output,
				String final_classifier_output
			)
	{
		ContentValues initialValues = new ContentValues();
		Date date = new Date(sampleTime);
		String time = Constants.DB_DATE_FORMAT.format(date);
		initialValues.put(KEY_ROWID, sampleTime);
		initialValues.put(KEY_STARTDATE, time);
		initialValues.put(KEY_TEST_MEAN_X, meanx);
		initialValues.put(KEY_TEST_MEAN_Y, meany);
		initialValues.put(KEY_TEST_MEAN_Z, meanz);
		initialValues.put(KEY_TEST_HOR_MEAN, mean_hor);
		initialValues.put(KEY_TEST_VER_MEAN, mean_ver);
		initialValues.put(KEY_TEST_HOR_RANGE, range_hor);
		initialValues.put(KEY_TEST_VER_RANGE, range_ver);
		initialValues.put(KEY_TEST_HOR_SD, sd_hor);
		initialValues.put(KEY_TEST_VER_SD, sd_ver);
		initialValues.put(KEY_TEST_CLASSIFIER_ALGO_OUTPUT, classifier_algo_output);
		initialValues.put(KEY_TEST_FINAL_CLASSIFIER_OUTPUT, final_classifier_output);
		initialValues.put(KEY_TEST_FINAL_SYSTEM_OUTPUT, "");
		initialValues.put("createdAt", System.currentTimeMillis());
		
		return _db.insert(DATABASE_TESTAV_TABLE, null, initialValues);
	}
	
	public synchronized boolean updateFinalClassificationToTestAVTable(
			long sampleTime,
			String final_system_output
		)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_TEST_FINAL_SYSTEM_OUTPUT, final_system_output);		
		return _db.update(DATABASE_TESTAV_TABLE, args, KEY_ROWID + "=" + sampleTime, null) > 1;
	}
	
	
	public synchronized void deleteTestAVTableBefore(long time)
	{
		_db.delete(DATABASE_TESTAV_TABLE, "createdAt < "+time, null);
	}

	//    ---------------------End AVERAGE TEST Table----------------------------------
}
