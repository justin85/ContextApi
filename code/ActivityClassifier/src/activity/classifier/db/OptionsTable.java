package activity.classifier.db;

import java.security.acl.LastOwnerException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import activity.classifier.BootReceiver;
import activity.classifier.common.Constants;
import activity.classifier.utils.Calibrator;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Handles storage and retrieval of system options.
 * Please don't create an instance of this class,
 * instead use the {@link SqlLiteAdapter} class to obtain
 * an instance of it.
 * 
 * @author Umran
 *
 */
public class OptionsTable implements DbTableAdapter {
	
	public static final String TABLE_NAME = "options";
	
	/**
	 * Column names in startinfo Table
	 */
	public static final String KEY_ID = "id";
	public static final String KEY_IS_SERVICE_STARTED = "isServiceStarted";
	public static final String KEY_IS_CALIBRATED = "isCalibrated";
	public static final String KEY_VALUE_OF_GRAVITY = "valueOfGravity";
	public static final String KEY_SD_X = "sdX";
	public static final String KEY_SD_Y = "sdY";
	public static final String KEY_SD_Z = "sdZ";
	public static final String KEY_MEAN_X = "meanX";
	public static final String KEY_MEAN_Y = "meanY";
	public static final String KEY_MEAN_Z = "meanZ";
	public static final String KEY_OFFSET_X = "offsetX";
	public static final String KEY_OFFSET_Y = "offsetY";
	public static final String KEY_OFFSET_Z = "offsetZ";
	public static final String KEY_SCALE_X = "scaleX";
	public static final String KEY_SCALE_Y = "scaleY";
	public static final String KEY_SCALE_Z = "scaleZ";
	public static final String KEY_COUNT = "count";
	public static final String KEY_ALLOWED_MULTIPLES_OF_SD = "allowedMultiplesOfSd";
	public static final String KEY_IS_ACCOUNT_SENT = "isAccountSent";
	public static final String KEY_IS_WAKE_LOCK_SET = "isWakeLockSet";
	public static final String KEY_USE_AGGREGATOR = "useAggregator";
	private static final String KEY_LAST_UPDATED_AT = "lastUpdatedAt";	//	for system use only
	
	private static final int DEFAULT_ROW_ID = 1;
	
	private static final String SELECT_SQL =
		"SELECT " +
		KEY_IS_SERVICE_STARTED + ", " +
		KEY_IS_CALIBRATED + ", " +
		KEY_VALUE_OF_GRAVITY + ", " +
		KEY_SD_X + ", " +
		KEY_SD_Y + ", " +
		KEY_SD_Z + ", " +
		KEY_MEAN_X + ", " +
		KEY_MEAN_Y + ", " +
		KEY_MEAN_Z + ", " +
		KEY_OFFSET_X + ", " +
		KEY_OFFSET_Y + ", " +
		KEY_OFFSET_Z + ", " +
		KEY_SCALE_X + ", " +
		KEY_SCALE_Y + ", " +
		KEY_SCALE_Z + ", " +
		KEY_COUNT + ", " +
		KEY_ALLOWED_MULTIPLES_OF_SD + ", " +
		KEY_IS_ACCOUNT_SENT + ", " +
		KEY_IS_WAKE_LOCK_SET + ", " +
		KEY_USE_AGGREGATOR + ", " +
		KEY_LAST_UPDATED_AT + 
		" FROM " + TABLE_NAME;
	
	private Context context;
	private SQLiteDatabase database;
	
	//	reusable
	private ContentValues contentValues;
	
	//	various system states
	private boolean isServiceStarted = true;
	private boolean useAggregator = true;
	private boolean isAccountSent = false;
	private boolean isWakeLockSet = false;
	
	// calibration
	private boolean isCalibrated;
	private float valueOfGravity;
	private float[] sd = new float[Constants.ACCEL_DIM];
	private float[] mean = new float[Constants.ACCEL_DIM];
	private float[] offset = new float[Constants.ACCEL_DIM];
	private float[] scale = new float[Constants.ACCEL_DIM];
	private int count;
	private float allowedMultiplesOfSd;
	
	//	updates
	private long lastUpdatedAt;
	
	protected OptionsTable(Context context) {
		this.context = context;
		this.contentValues = new ContentValues();
		
		for (int i=0; i<Constants.ACCEL_DIM; ++i) {
			scale[i] = 1.0f;
		}
	}
	
	@Override
	public void createTable(SQLiteDatabase database) {
		//	create the create sql
		String sql = 
			"CREATE TABLE "+TABLE_NAME+" (" +
			KEY_ID+" INTEGER PRIMARY KEY, " +
			KEY_IS_SERVICE_STARTED+" INTEGER NOT NULL, " +
			KEY_IS_CALIBRATED+" INTEGER NOT NULL, " +
			KEY_VALUE_OF_GRAVITY+" REAL NOT NULL, " +
			KEY_SD_X+" REAL NOT NULL, " +
			KEY_SD_Y+" REAL NOT NULL, " +
			KEY_SD_Z+" REAL NOT NULL, " +
			KEY_MEAN_X+" REAL NOT NULL, " +
			KEY_MEAN_Y+" REAL NOT NULL, " +
			KEY_MEAN_Z+" REAL NOT NULL, " +
			KEY_OFFSET_X+" REAL NOT NULL, " +
			KEY_OFFSET_Y+" REAL NOT NULL, " +
			KEY_OFFSET_Z+" REAL NOT NULL, " +
			KEY_SCALE_X+" REAL NOT NULL, " +
			KEY_SCALE_Y+" REAL NOT NULL, " +
			KEY_SCALE_Z+" REAL NOT NULL, " +
			KEY_COUNT+" INTEGER NOT NULL, " +
			KEY_ALLOWED_MULTIPLES_OF_SD+" REAL NOT NULL, " + 
			KEY_IS_ACCOUNT_SENT+" INTEGER NOT NULL, " +
			KEY_IS_WAKE_LOCK_SET+" INTEGER NOT NULL, " +
			KEY_USE_AGGREGATOR+" INTEGER NOT NULL, " +
			KEY_LAST_UPDATED_AT+" LONG NOT NULL " +
			")";
		//	run the sql
		database.execSQL(sql);
		
		//	insert default values
		Calibrator.resetCalibrationOptions(this);	// set calibration values to defaults
		lastUpdatedAt = System.currentTimeMillis();	// last updated now
		assignValuesToContentValues();				//	assign values to content values
		contentValues.put(KEY_ID, DEFAULT_ROW_ID);	//	set row id
		database.insertOrThrow(TABLE_NAME, null, contentValues);
		contentValues.remove(KEY_ID);				//	remove row id from context values
		lastUpdatedAt = 0;							//	reset last updated time to zero so that system can load
	}

	@Override
	public void dropTable(SQLiteDatabase database) {
		//	drop existing table if it exists
		database.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
	}

	@Override
	public boolean init(SQLiteDatabase database) {
		this.database = database;
		
		this.load();
		
		return true;
	}

	@Override
	public void done() {
		this.save();
	}
	
	/**
	 * A convenience function to assign values stored
	 * in this instance to the {@link #contentValues} field
	 * before it is used in saving to the database.
	 * Please note that the {@link #KEY_ID} field isn't assigned
	 * because it isn't always required in the SQL inserts/updates,
	 * rather it forms the WHERE portion of the SQL statement.
	 */
	private void assignValuesToContentValues()
	{
		contentValues.put(KEY_IS_SERVICE_STARTED, isServiceStarted?1:0);
		contentValues.put(KEY_IS_CALIBRATED, isCalibrated?1:0);
		contentValues.put(KEY_VALUE_OF_GRAVITY, valueOfGravity);
		contentValues.put(KEY_SD_X, sd[Constants.ACCEL_X_AXIS]);
		contentValues.put(KEY_SD_Y, sd[Constants.ACCEL_Y_AXIS]);
		contentValues.put(KEY_SD_Z, sd[Constants.ACCEL_Z_AXIS]);
		contentValues.put(KEY_MEAN_X, mean[Constants.ACCEL_X_AXIS]);
		contentValues.put(KEY_MEAN_Y, mean[Constants.ACCEL_Y_AXIS]);
		contentValues.put(KEY_MEAN_Z, mean[Constants.ACCEL_Z_AXIS]);
		contentValues.put(KEY_OFFSET_X, offset[Constants.ACCEL_X_AXIS]);
		contentValues.put(KEY_OFFSET_Y, offset[Constants.ACCEL_Y_AXIS]);
		contentValues.put(KEY_OFFSET_Z, offset[Constants.ACCEL_Z_AXIS]);
		contentValues.put(KEY_SCALE_X, scale[Constants.ACCEL_X_AXIS]);
		contentValues.put(KEY_SCALE_Y, scale[Constants.ACCEL_Y_AXIS]);
		contentValues.put(KEY_SCALE_Z, scale[Constants.ACCEL_Z_AXIS]);
		contentValues.put(KEY_COUNT, count);
		contentValues.put(KEY_ALLOWED_MULTIPLES_OF_SD, allowedMultiplesOfSd);
		contentValues.put(KEY_IS_ACCOUNT_SENT, isAccountSent?1:0);
		contentValues.put(KEY_IS_WAKE_LOCK_SET, isWakeLockSet?1:0);
		contentValues.put(KEY_USE_AGGREGATOR, useAggregator?1:0);
		contentValues.put(KEY_LAST_UPDATED_AT, lastUpdatedAt);
	}
	
	private void load()
	{
		Cursor cursor = database.rawQuery(
				 SELECT_SQL + 
				 " WHERE " + KEY_ID + "=" + DEFAULT_ROW_ID + " AND " + KEY_LAST_UPDATED_AT + ">"+lastUpdatedAt, 
				 null);
		
		try {
			if (cursor.moveToNext()) {
				isServiceStarted = cursor.getInt(cursor.getColumnIndex(KEY_IS_SERVICE_STARTED))!=0;
				isCalibrated = cursor.getInt(cursor.getColumnIndex(KEY_IS_CALIBRATED))!=0;
				valueOfGravity = cursor.getFloat(cursor.getColumnIndex(KEY_VALUE_OF_GRAVITY));
				sd[Constants.ACCEL_X_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_SD_X));
				sd[Constants.ACCEL_Y_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_SD_Y));
				sd[Constants.ACCEL_Z_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_SD_Z));
				mean[Constants.ACCEL_X_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_MEAN_X));
				mean[Constants.ACCEL_Y_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_MEAN_Y));
				mean[Constants.ACCEL_Z_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_MEAN_Z));
				offset[Constants.ACCEL_X_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_OFFSET_X));
				offset[Constants.ACCEL_Y_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_OFFSET_Y));
				offset[Constants.ACCEL_Z_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_OFFSET_Z));
				scale[Constants.ACCEL_X_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_SCALE_X));
				scale[Constants.ACCEL_Y_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_SCALE_Y));
				scale[Constants.ACCEL_Z_AXIS] = cursor.getFloat(cursor.getColumnIndex(KEY_SCALE_Z));
				count = cursor.getInt(cursor.getColumnIndex(KEY_COUNT));
				allowedMultiplesOfSd = cursor.getFloat(cursor.getColumnIndex(KEY_ALLOWED_MULTIPLES_OF_SD));
				isAccountSent = cursor.getInt(cursor.getColumnIndex(KEY_IS_ACCOUNT_SENT))!=0;
				isWakeLockSet = cursor.getInt(cursor.getColumnIndex(KEY_IS_WAKE_LOCK_SET))!=0;
				useAggregator = cursor.getInt(cursor.getColumnIndex(KEY_USE_AGGREGATOR))!=0;
				lastUpdatedAt = cursor.getLong(cursor.getColumnIndex(KEY_LAST_UPDATED_AT));
			}
		} finally {
			cursor.close();
		}
	}
	
	/**
	 * Saves the cached values to the database
	 */
	public void save()
	{
		synchronized (contentValues) {
			this.lastUpdatedAt = System.currentTimeMillis();
			assignValuesToContentValues();
			int rows = database.update(TABLE_NAME, contentValues, KEY_ID+"="+DEFAULT_ROW_ID, null);
			if (rows==0)
				throw new RuntimeException("Unable to update option values for table '"+TABLE_NAME+"'");
		}
	}

	/**
	 * @return the isServiceStarted
	 */
	public boolean isServiceStarted() {
		return isServiceStarted;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * Please Note:
	 * This value is set to true when the service starts,
	 * not when the menu item responsible for starting
	 * the service is clicked. This is done for 2 reasons:
	 * 	1) During the service starting process, any
	 * 		number of things could go wrong resulting to
	 * 		the service not being started.
	 * 	2) The service could also be started through the
	 * 		boot process. see {@link BootReceiver}
	 *	
	 *	On the other hand, the value is set to false, when
	 *	and only when the user explicitly turns the service
	 *	off (i.e. using the menu item is clicked.)
	 * 
	 * @param isServiceStarted
	 * whether the service has been started or not
	 */
	public void setServiceStarted(boolean isServiceStarted) {
		this.isServiceStarted = isServiceStarted;
	}

	/**
	 * @return the isCalibrated
	 */
	public boolean isCalibrated() {
		return isCalibrated;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param isCalibrated the isCalibrated to set
	 */
	public void setCalibrated(boolean isCalibrated) {
		this.isCalibrated = isCalibrated;
	}

	/**
	 * @return the isAccountSent
	 */
	public boolean isAccountSent() {
		return isAccountSent;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param isAccountSent the isAccountSent to set
	 */
	public void setAccountSent(boolean isAccountSent) {
		this.isAccountSent = isAccountSent;
	}

	/**
	 * @return the isWakeLockSet
	 */
	public boolean isWakeLockSet() {
		return isWakeLockSet;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param isWakeLockSet the isWakeLockSet to set
	 */
	public void setWakeLockSet(boolean isWakeLockSet) {
		this.isWakeLockSet = isWakeLockSet;
	}

	/**
	 * @return the valueOfGravity
	 */
	public float getValueOfGravity() {
		return valueOfGravity;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param valueOfGravity the valueOfGravity to set
	 */
	public void setValueOfGravity(float valueOfGravity) {
		this.valueOfGravity = valueOfGravity;
	}

	/**
	 * @return the sd
	 */
	public float[] getSd() {
		return sd;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param sd the sd to set
	 */
	public void setSd(float[] sd) {
		for (int i=0; i<Constants.ACCEL_DIM; ++i)
			this.sd[i] = sd[i];
	}

	/**
	 * @return the mean
	 */
	public float[] getMean() {
		return mean;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param mean the mean to set
	 */
	public void setMean(float[] mean) {
		for (int i=0; i<Constants.ACCEL_DIM; ++i)
			this.mean[i] = mean[i];
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return the useAggregator
	 */
	public boolean getUseAggregator() {
		return useAggregator;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param useAggregator the useAggregator to set
	 */
	public void setUseAggregator(boolean useAggregator) {
		this.useAggregator = useAggregator;
	}

	/**
	 * @return the allowedMultiplesOfSd
	 */
	public float getAllowedMultiplesOfSd() {
		return allowedMultiplesOfSd;
	}

	/**
	 * Make sure to call {@link #save()} after setting.
	 * 
	 * @param allowedMultiplesOfSd the allowedMultiplesOfSd to set
	 */
	public void setAllowedMultiplesOfSd(float allowedMultiplesOfSd) {
		this.allowedMultiplesOfSd = allowedMultiplesOfSd;
	}

	/**
	 * @return the offset
	 */
	public float[] getOffset() {
		return offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(float[] offset) {
		for (int i=0; i<Constants.ACCEL_DIM; ++i)
			this.offset[i] = offset[i];
	}

	/**
	 * @return the scale
	 */
	public float[] getScale() {
		return scale;
	}

	/**
	 * @param scale the scale to set
	 */
	public void setScale(float[] scale) {
		for (int i=0; i<Constants.ACCEL_DIM; ++i)
			this.scale[i] = scale[i];
	}

}
