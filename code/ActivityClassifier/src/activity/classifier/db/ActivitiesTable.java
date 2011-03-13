/**
 * 
 */
package activity.classifier.db;

import java.util.Date;
import java.util.List;

import activity.classifier.common.Constants;
import activity.classifier.rpc.Classification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * @author Umran
 *
 */
public class ActivitiesTable implements DbTableAdapter {
	
	public static final String ACTIVITY_END = "END";
	
	public static boolean isSystemActivity(String activity) {
		return ACTIVITY_END.equals(activity);
	}
	
	public static final String TABLE_NAME = "activities";
	
	public static interface ClassificationDataCallback {
		void onRetrieve(Classification classification);
	}
	
	/**
	 * Column names in activity Table
	 */
	public static final String KEY_START_LONG		= "start_long";
	public static final String KEY_END_LONG			= "end_long";
	public static final String KEY_ACTIVITY			= "activity";
	public static final String KEY_IS_CHECKED		= "isChecked";
	public static final String KEY_START_STR		= "start_str";
	public static final String KEY_END_STR 			= "end_str";
	private static final String KEY_LAST_UPDATED_AT = "lastUpdatedAt";	//	for system use only

	private static final String SELECT_SQL =
		"SELECT " +
		KEY_START_LONG + ", " +
		KEY_END_LONG + ", " +
		KEY_ACTIVITY + ", " +
		KEY_IS_CHECKED + ", " +
		KEY_START_STR + ", " +
		KEY_END_STR + ", " +
		KEY_LAST_UPDATED_AT +
		" FROM " + TABLE_NAME;
	
	private static final String CALC_STAT_SQL =
		"SELECT COUNT(*)" +
		" FROM " + TABLE_NAME +
		" WHERE " + KEY_START_LONG + " BETWEEN ? AND ? " +
		" AND " + KEY_ACTIVITY + "=?";
	
	private Context context;
	private SQLiteDatabase database;
	private SQLiteStatement calcStatStatement;
	
	//	reusable
	private ContentValues insertContentValues;
	private Date insertStartDate;
	private Date insertEndDate;
	private ContentValues updateContentValues;
	private ContentValues updateCheckedContentValues;
	
	protected ActivitiesTable(Context context) {
		this.context = context;
		
		this.insertContentValues = new ContentValues();
		this.insertStartDate = new Date();
		this.insertEndDate = new Date();
		
		this.updateContentValues = new ContentValues();
		
		this.updateCheckedContentValues = new ContentValues();
		this.updateCheckedContentValues.put(KEY_IS_CHECKED, 1);
	}
	
	@Override
	public void createTable(SQLiteDatabase database) {
		//	create the create sql
		String sql = 
			"CREATE TABLE "+TABLE_NAME+" (" +
			KEY_START_LONG+" LONG PRIMARY KEY, " +
			KEY_END_LONG+" LONG NOT NULL, " +
			KEY_ACTIVITY+" TEXT NOT NULL, " +
			KEY_START_STR+" TEXT NOT NULL, " +
			KEY_END_STR+" TEXT NOT NULL, " +
			KEY_IS_CHECKED+" INTEGER NOT NULL, " +
			KEY_LAST_UPDATED_AT+" LONG NOT NULL " +
			")";
		//	run the sql
		database.execSQL(sql);
	}

	@Override
	public void dropTable(SQLiteDatabase database) {
		//	drop existing table if it exists
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
	}

	@Override
	public boolean init(SQLiteDatabase database) {
		this.database = database;
		this.calcStatStatement = this.database.compileStatement(CALC_STAT_SQL);
		return true;
	}

	@Override
	public void done() {
		this.calcStatStatement.close();
	}
	
	/**
	 * Saves the cached values to the database
	 */
	public void insert(Classification classification)
	{
		synchronized (insertContentValues) {
			assignValuesToInsertContentValues(classification);
			database.insertOrThrow(TABLE_NAME, null, insertContentValues);
		}
	}
	
	/**
	 * Updates the final system's output ({@value #KEY_FINAL_SYSTEM_OUTPUT}),
	 * of the sample given by @param sampleTime.
	 */
	public void update(Classification classification)
	{
		synchronized (updateContentValues) {
			assignValuesToUpdateContentValues(classification);
			int rows = database.update(TABLE_NAME, updateContentValues, KEY_START_LONG+"="+classification.getStart(), null);
			if (rows==0)
				Log.w(Constants.DEBUG_TAG, "Warning: Update Failed: table='"+TABLE_NAME+"', "+KEY_START_LONG+"="+classification.getStart()+
						", "+KEY_END_LONG+"="+classification.getEnd()+
						", "+KEY_LAST_UPDATED_AT+"="+classification.getLastUpdate());
		}
	}

	/**
	 * Loads the latest classification
	 */
	public boolean loadLatest(Classification classification)
	{
		Cursor cursor = database.rawQuery(SELECT_SQL + " WHERE "+KEY_START_LONG+"=(SELECT MAX("+KEY_START_LONG+") FROM "+TABLE_NAME+")", null);
		try {
			if (cursor.moveToNext()) {
				assignValuesToClassification(cursor, classification);
				return true;
			} else {
				return false;
			}
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Loads the classification that started at the given time
	 */
	public boolean load(long startTime, Classification classification)
	{
		Cursor cursor = database.rawQuery(SELECT_SQL + " WHERE "+KEY_START_LONG+"="+startTime, null);
		try {
			if (cursor.moveToNext()) {
				assignValuesToClassification(cursor, classification);
				return true;
			} else {
				return false;
			}
		} finally {
			cursor.close();
		}
	}
	
	/** 
	 * Loads all the classifications that started between the times given.
	 * Please note, make sure to provide enough {@link Classification} instances
	 */
	public void loadUpdated(long startStartTime, long endStartTime, long lastUpdate, Classification reusableClassification, ClassificationDataCallback callback)
	{
		Cursor cursor = database.rawQuery(
				SELECT_SQL + 
				" WHERE "+KEY_END_LONG+" BETWEEN "+Math.min(startStartTime, endStartTime)+" AND "+Math.max(startStartTime, endStartTime) +
				" AND "+KEY_LAST_UPDATED_AT+">"+lastUpdate +
				" ORDER BY " + KEY_START_LONG + " ASC ",
				null);
		try {
			while (cursor.moveToNext()) {
				//	assign it
				assignValuesToClassification(cursor, reusableClassification);
				//	return it
				callback.onRetrieve(reusableClassification);
			}
		} finally {
			cursor.close();
		}
	}
	
	/** 
	 * Loads all the classifications that started between the times given.
	 * Please note, make sure to provide enough {@link Classification} instances
	 */
	public void loadUnchecked(Classification reusableClassification, ClassificationDataCallback callback)
	{
		Cursor cursor = database.rawQuery(
				SELECT_SQL + 
				" WHERE "+KEY_IS_CHECKED+"=0" +
				" ORDER BY " + KEY_START_LONG + " ASC ",
				null);
		try {
			while (cursor.moveToNext()) {
				//	assign it
				assignValuesToClassification(cursor, reusableClassification);
				//	return it
				callback.onRetrieve(reusableClassification);
			}
		} finally {
			cursor.close();
		}
	}
	
	/** 
	 * Computes the number of items available in the database that started
	 * between the period given.
	 */
	public long load(long startStartTime, long endStartTime, String classificationType)
	{
		synchronized (calcStatStatement) {
			calcStatStatement.clearBindings();
			calcStatStatement.bindLong(1, startStartTime);
			calcStatStatement.bindLong(2, endStartTime);
			calcStatStatement.bindString(3, classificationType);
			return calcStatStatement.simpleQueryForLong();
		}
	}
	
	/**
	 * Removes any extra data available in the database table,
	 * leaving only the period required as defined in {@link Constants#DURATION_KEEP_DB_ACTIVITY_DATA}
	 * and that hasn't been uploaded ({@value #KEY_IS_CHECKED});
	 */
	public void trim()
	{
		long cutOffLimit = System.currentTimeMillis() - Constants.DURATION_KEEP_DB_ACTIVITY_DATA;
		database.delete(TABLE_NAME, KEY_LAST_UPDATED_AT+"<"+cutOffLimit+" OR "+KEY_IS_CHECKED+"<>0", null);
	}
	
	/**
	 * Updates the given row's ({@value #KEY_IS_CHECKED}) to true
	 */
	public void updateChecked(long startTime)
	{
		int rows = database.update(TABLE_NAME, updateCheckedContentValues, KEY_START_LONG+"="+startTime, null);
		if (rows==0)
			Log.w(Constants.DEBUG_TAG, "Warning: Update Failed: table='"+TABLE_NAME+"', "+KEY_START_LONG+"="+startTime+", "+KEY_IS_CHECKED+"=1");

	}
	
	/**
	 * A convenience function to assign values stored
	 * in this instance to the {@link #insertContentValues} field
	 * before it is used in saving to the database.
	 */
	private void assignValuesToInsertContentValues(Classification classification)
	{
		insertStartDate.setTime(classification.getStart());
		insertEndDate.setTime(classification.getEnd());
		
		insertContentValues.put(KEY_START_LONG, classification.getStart());
		insertContentValues.put(KEY_END_LONG, classification.getEnd());
		insertContentValues.put(KEY_ACTIVITY, classification.getClassification());
		insertContentValues.put(KEY_START_STR, Constants.DB_DATE_FORMAT.format(insertStartDate));
		insertContentValues.put(KEY_END_STR, Constants.DB_DATE_FORMAT.format(insertEndDate));
		insertContentValues.put(KEY_IS_CHECKED, classification.isChecked()?1:0);
		insertContentValues.put(KEY_LAST_UPDATED_AT, System.currentTimeMillis());
	}
	
	/**
	 * A convenience function to assign values stored
	 * in this instance to the {@link #updateContentValues} field
	 * before it is used in saving to the database.
	 * 
	 * All but the {@link #KEY_START_STR} field of the classification is updated.
	 * The {@link #KEY_START_STR} field is not to change and serves as
	 * reference point to the activity being updated 
	 */
	private void assignValuesToUpdateContentValues(Classification classification)
	{
		insertStartDate.setTime(classification.getStart());
		insertEndDate.setTime(classification.getEnd());
		
		updateContentValues.put(KEY_END_LONG, classification.getEnd());
		updateContentValues.put(KEY_ACTIVITY, classification.getClassification());
		updateContentValues.put(KEY_END_STR, Constants.DB_DATE_FORMAT.format(insertEndDate));
		updateContentValues.put(KEY_IS_CHECKED, classification.isChecked());
		updateContentValues.put(KEY_LAST_UPDATED_AT, System.currentTimeMillis());
	}
	
	/**
	 * A convenience function to assign values from the cursor
	 * to the {@link Classification}. This function assumes
	 * that the cursor was derived from a SELECT statement
	 * based on the SELECT statement described in {@link #SELECT_SQL}
	 * 
	 * @return
	 * a classification with all values set as from the cursor,
	 * and {@link Classification#withContext(Context)} function called.
	 */
	private void assignValuesToClassification(Cursor cursor, Classification classification)
	{
		classification.setStart(cursor.getLong(cursor.getColumnIndex(KEY_START_LONG)));
		classification.setClassification(cursor.getString(cursor.getColumnIndex(KEY_ACTIVITY)));
		classification.setEnd(cursor.getLong(cursor.getColumnIndex(KEY_END_LONG)));
		classification.setChecked(cursor.getInt(cursor.getColumnIndex(KEY_IS_CHECKED))!=0);
		classification.setLastUpdate(cursor.getLong(cursor.getColumnIndex(KEY_LAST_UPDATED_AT)));
		classification.withContext(context);
	}
	
}
