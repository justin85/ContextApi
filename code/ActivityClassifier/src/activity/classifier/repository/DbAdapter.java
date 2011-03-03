
package activity.classifier.repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

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

	public static final String KEY_ROWID = "_id";

	/**
	 * Column names in startinfo Table
	 */
	public static final String KEY_isServiceRunning = "isServiceRunning";
	public static final String KEY_isCalibrated = "isCalibrated";
	public static final String KEY_valueOfGravity = "valueOfGravity";
	public static final String KEY_sdX = "sdX";
	public static final String KEY_sdY = "sdY";
	public static final String KEY_sdZ = "sdZ";
	public static final String KEY_isAccountSent = "isAccountSent";
	public static final String KEY_isWakeLockSet = "isWakeLockSet";

	/**
	 * Column names in activity Table
	 */
	public static final String KEY_ACTIVITY = "activity";
	public static final String KEY_isChecked = "isChecked";
	public static final String KEY_START_DATE = "startDate";
	public static final String KEY_END_DATE = "endDate";

	/**
	 * Column names in testav Table
	 */
	public static final String KEY_LASTX = "lastx";
	public static final String KEY_LASTY = "lasty";
	public static final String KEY_LASTZ = "lastz";
	public static final String KEY_CURRX = "currx";
	public static final String KEY_CURRY = "curry";
	public static final String KEY_CURRZ = "currz";
	public static final String KEY_SDX = "sdx";
	public static final String KEY_SDY = "sdy";
	public static final String KEY_SDZ = "sdz";

	private static final String TAG = "DbAdapter";

	private DatabaseHelper mDbHelper;
	private static SQLiteDatabase _db;  

	/**
	 * startinfo Table creation sql statement
	 */
	private static final String DATABASE_STARTINFO_CREATE =
		"create table startinfo (" +
			"_id integer primary key autoincrement, " +
			"isServiceRunning text not null, " +
			"isCalibrated text not null, " +
			"valueOfGravity text not null, " +
			"sdX text not null, " +
			"sdY text not null, " +
			"sdZ text not null, " +
			"meanX text not null, " +
			"meanY text not null, " +
			"meanZ text not null, " +
			"count text not null, " +
			"isAccountSent text not null, " +
			"isWakeLockSet text not null" +
		");";

	/**
	 *  sql statement for initialising values in startinfo Table 
	 */
	private static final String DATABASE_STARTINFO_INIT =
		"insert into startinfo values (" +
			"null, " +
			"0, " +
			"0, " +
			"1, " +
			"0.05, " +
			"0.05, " +
			"0.05, " +
			"0.0, " +
			"0.0, " +
			"0.0, " +
			"0, " +
			"0, " +
			"0);";
	

	/**
	 * activity Table creation sql statement
	 */
	private static final String DATABASE_ACTIVIT_CREATE =
		"create table activity (_id integer primary key autoincrement, "
		+ "activity text not null, startDate DATE not null, endDate DATE not null, isChecked integer not null);";

	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z"); 
	static String date = dateFormat.format(new Date());
//	/**
//	 *  sql statement for initialising values in activity Table 
//	 */
//	private static final String DATABASE_ACTIVITY_INIT =
//		"insert into activity values (null, END, "+date+", "+date+", 1);";


	/**
	 * testav Table creation sql statement
	 */
	private static final String DATABASE_TESTAV_CREATE =
		"create table testav (_id integer primary key autoincrement, "
		+ "startDate DATE not null, sdx text not null, sdy text not null, sdz text not null, lastx text not null, lasty text not null, lastz text not null," +
		" currx text not null, curry text not null, currz text not null);";

	private static  String DATABASE_NAME = "activityrecords.db";

	/**
	 * Table names
	 */
	private static final String DATABASE_STARTINFO_TABLE = "startinfo";
	private static final String DATABASE_ACTIVITY_TABLE = "activity";
	private static final String DATABASE_TESTAV_TABLE = "testav";

	private static final int DATABASE_VERSION = 2;

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

			db.execSQL("DROP TABLE IF EXISTS startinfo");
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
	 * @return
	 * @throws SQLException
	 */
	public DbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		try {
			_db = mDbHelper.getWritableDatabase();
		} catch (SQLException e) {
			Log.e(Constants.DEBUG_TAG, "Error while trying to open the database", e);
			throw e;
		}
		return this;
	}

	/**
	 * Close the database access 
	 */
	public void close() {
		mDbHelper.close();
	}


	//  ---------------------Start Start-info Table----------------------------------

	/**
	 * Fetch the value from a specific column 
	 * @param fieldName column name
	 * @return String data type value 
	 * @throws SQLException
	 */
	public synchronized  String fetchFromStartTableString(String fieldName) throws SQLException {
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
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_START_DATE,KEY_END_DATE,KEY_isChecked },  null, null, null, null, null, null);
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
		initialValues.put(KEY_START_DATE, time);
		initialValues.put(KEY_END_DATE, time);
		initialValues.put(KEY_isChecked,isChecked);

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
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_START_DATE,KEY_END_DATE,KEY_isChecked }, KEY_isChecked + "=" + isChecked, null, null, null, null, null);
		return mCursor;
	}
	private boolean isActivityBetweenToday;

	public synchronized  ArrayList<String[]> fetchTodayItemsFromActivityTable(Date todayTime, Date currDate) throws SQLException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z"); 
		
		String currTime = dateFormat.format(currDate);
		String dayAgoTime = dateFormat.format(todayTime);
		
		Cursor mCursor =
			_db.query(true, DATABASE_ACTIVITY_TABLE, 
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_START_DATE,KEY_END_DATE,KEY_isChecked }, "("+KEY_START_DATE+" BETWEEN "+"'"+dayAgoTime+"' AND '"+currTime+"')" , null, null, null, null, null);
 
		//        Log.i("DB","Day "+day+" Month"+month);
		//        Log.i("DB","DB SIZE"+mCursor.getCount());
		ArrayList<String[]> todayItems = new ArrayList<String[]>();
		int i =0;
		while(mCursor.moveToNext()) {
					String[] tempPara = {mCursor.getString(0),mCursor.getString(1),mCursor.getString(2),mCursor.getString(3)};
					todayItems.add(tempPara);
//					//					Log.i("DB",tempPara[0]+" "+tempPara[1]+" "+tempPara[2]+" "+tempPara[3]+" "+tempPara[4]+" ");

				i++;


		}
		mCursor.close();
		isActivityBetweenToday=false;

		return todayItems;
	}

	public synchronized ArrayList<String> fetchBetweenTimeItemsFromActivityTable(Date timeDiff,int state, Integer[] times, String itemName) throws SQLException {
		Cursor mCursor =
			_db.query(true, DATABASE_ACTIVITY_TABLE, 
					new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_START_DATE,KEY_END_DATE,KEY_isChecked }, "("+KEY_ROWID + ">=" + times[0] + " AND " +  KEY_ROWID + "<=" + times[1]+")" + " AND " +  KEY_ACTIVITY + " = "+itemName+"", null, null, null, null, null);
		ArrayList<String> items = new ArrayList<String>();
		int i=0;
		while(mCursor.moveToNext()) {
			if(i==0 && state==1){
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z"); 
				items.add(Integer.parseInt(mCursor.getString(0))+","+mCursor.getString(1)+","+dateFormat.format(timeDiff)+","+mCursor.getString(3));

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
		mCursor.moveToNext();
		String activityDate = mCursor.getString(0);
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
				new String[] { KEY_START_DATE}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		mCursor.moveToNext();
		String activityDate = mCursor.getString(0);
		mCursor.close();
		return activityDate;
	}

	public synchronized  Cursor fetchItemsFromActivityTable(String itemName){
		Cursor mCursor = _db.query(true, DATABASE_ACTIVITY_TABLE,
				new String[] { KEY_ROWID, KEY_ACTIVITY,KEY_START_DATE,KEY_END_DATE,KEY_isChecked }, KEY_ACTIVITY + "=" + itemName, null, null, null, null, null);

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
		args.put(KEY_START_DATE, startDate);
		args.put(KEY_END_DATE, endDate);
		args.put(KEY_isChecked, isChecked);
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
	public synchronized  long insertValuesToTestAVTable(String sdx,String sdy,String sdz, String lastx,String lasty,String lastz,String currx,String curry,String currz) {
		ContentValues initialValues = new ContentValues();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z");  
		Date date1 = new Date(); 
		String time = dateFormat.format(date1);
		initialValues.put(KEY_START_DATE, time);
		initialValues.put(KEY_SDX, sdx);
		initialValues.put(KEY_SDY, sdy);
		initialValues.put(KEY_SDZ, sdz);
		initialValues.put(KEY_LASTX, lastx);
		initialValues.put(KEY_LASTY, lasty);
		initialValues.put(KEY_LASTZ, lastz);
		initialValues.put(KEY_CURRX, currx);
		initialValues.put(KEY_CURRY, curry);
		initialValues.put(KEY_CURRZ, currz);


		return _db.insert(DATABASE_TESTAV_TABLE, null, initialValues);
	}

	//    ---------------------End AVERAGE TEST Table----------------------------------
}