package activity.classifier.repository;

import activity.classifier.common.Constants;
import android.content.Context;
import android.util.Log;

/**
 * A utility class which extends superclass {@link Queries} 
 * for handling queries to get the system information.
 * 
 * Edit from Umran:
 * Changed all methods dealing with database open/close methods
 * to <code>synchronized</code> in order to ensure thread safety when called 
 * from multiple threads.
 * 
 * @author Justin Lee
 *
 */
public class OptionQueries extends Queries {
	
	private static OptionQueries optionQueries = null;
	
	synchronized
	public static OptionQueries getOptionQueries(Context context) {
		if (optionQueries==null)
			optionQueries = new OptionQueries(context);
		return optionQueries;
	}

	private DbAdapter dbAdapter;
	
	private boolean isServiceStarted;
	private boolean isCalibrated;
	private boolean isAccountSent;
	private boolean isWakeLockSet;
	private float valueOfGravity;
	private float[] standardDeviation = new float[3];
	private float[] mean = new float[3];
	private int count;
	private boolean useAggregator;
	private float allowedMultiplesOfDeviation;
	
	/**
	 * @see Queries
	 * @param context context from Activity or Service classes 
	 */
	public OptionQueries(Context context){
		super(context);
		dbAdapter = super.dbAdapter;
//		isServiceRunning = false;
//		isCalibrated = false;
//		isAccountSent = false;
//		isWakeLockSet = false;
//		valueOfGravity = 1;
//		for(int i = 0; i < 3; i++){
//			standardDeviation[i] = (float)0.1;
//		}
	}

	
	public synchronized void load(){
		int isServiceStarted;
		int isCalibrated;
		float valueOfGravity;
		int isAccountSent;
		int isWakeLockSet;
		float[] standardDeviation = new float[3];
		float[] mean = new float[3];
		int count;
		int useAggregator;
		float allowedMultiplesOfDeviation;
		
		dbAdapter.open();
		isServiceStarted = dbAdapter.fetchFromStartTableInt(DbAdapter.KEY_IS_SERVICE_STARTED);
		isCalibrated = dbAdapter.fetchFromStartTableInt(DbAdapter.KEY_IS_CALIBRATED);
		valueOfGravity = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_VALUE_OF_GRAVITY);
		isAccountSent = dbAdapter.fetchFromStartTableInt(DbAdapter.KEY_IS_ACCOUNT_SENT);
		isWakeLockSet = dbAdapter.fetchFromStartTableInt(DbAdapter.KEY_IS_WAKE_LOCK_SET);
		standardDeviation[0] = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_SD_X);
		standardDeviation[1] = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_SD_Y);
		standardDeviation[2] = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_SD_Z);
		mean[0] = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_MEAN_X);
		mean[1] = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_MEAN_Y);
		mean[2] = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_MEAN_Z);
		count = dbAdapter.fetchFromStartTableInt(DbAdapter.KEY_COUNT);
		useAggregator = dbAdapter.fetchFromStartTableInt(DbAdapter.KEY_USE_AGGREGATOR);
		allowedMultiplesOfDeviation = dbAdapter.fetchFromStartTableFloat(DbAdapter.KEY_ALLOWED_MULTIPLES_OF_SD);
		dbAdapter.close();
		
		this.isServiceStarted = isServiceStarted!=0;
		this.isCalibrated = isCalibrated!=0;
		this.isAccountSent = isAccountSent!=0;
		this.isWakeLockSet = isWakeLockSet!=0;
		
		for(int i = 0; i < 3; i++){
			this.standardDeviation[i] = standardDeviation[i];
			this.mean[i] = mean[i];
		}
		
		this.count = count;
		this.valueOfGravity = valueOfGravity;
		this.useAggregator = useAggregator!=0;
		this.allowedMultiplesOfDeviation = allowedMultiplesOfDeviation;
	}

	public synchronized void save(){
		dbAdapter.open();
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_IS_SERVICE_STARTED, this.isServiceStarted?"1":"0");
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_IS_CALIBRATED, this.isCalibrated?"1":"0");
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_IS_ACCOUNT_SENT, this.isAccountSent?"1":"0");
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_IS_WAKE_LOCK_SET, this.isWakeLockSet?"1":"0");
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_VALUE_OF_GRAVITY, Float.toString(this.valueOfGravity));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_SD_X, Float.toString(this.standardDeviation[0]));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_SD_Y, Float.toString(this.standardDeviation[1]));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_SD_Z, Float.toString(this.standardDeviation[2]));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_MEAN_X, Float.toString(this.mean[0]));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_MEAN_Y, Float.toString(this.mean[1]));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_MEAN_Z, Float.toString(this.mean[2]));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_COUNT, Integer.toString(count));
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_USE_AGGREGATOR, this.useAggregator?"1":"0");
		dbAdapter.updateToSelectedStartTable(DbAdapter.KEY_ALLOWED_MULTIPLES_OF_SD, Float.toString(allowedMultiplesOfDeviation));
		dbAdapter.close();
	}
	

	/**
	 * Set the background service running state
	 * @param value should be 1 if service is running, 0 otherwise.
	 */
	public synchronized void setServiceStartedState(boolean value){
		this.isServiceStarted = value;
	}
	
	/**
	 * Get whether the background service was started previously or not
	 * @return 1 if service is running, 0 otherwise.
	 */
	public synchronized boolean getServiceStartedState(){
		return this.isServiceStarted;
	}

	/**
	 * Set the calibration state
	 * @param value should be 1 if calibration is done, 0 otherwise.
	 */
	public synchronized void setCalibrationState(boolean value){
		this.isCalibrated = value;
	}

	/**
	 * Get the calibration state
	 * @return 1 if calibration is done, 0 otherwise.
	 */
	public synchronized boolean isCalibrated(){
		return this.isCalibrated;
	}

	/**
	 * Set the calibration value
	 * @param value calibration value
	 */
	public synchronized void setValueOfGravity(float value){
		this.valueOfGravity = value;
	}

	/**
	 * Get the calibration value
	 * @return return float data type of calibration value
	 */
	public synchronized float getValueOfGravity(){
		return this.valueOfGravity;
	}

	/**
	 * Set the standard deviation of X axis over certain amount of times
	 * @param value standard deviation of X axis
	 */
	public synchronized void setStandardDeviationX(float value){
		this.standardDeviation[0] = value;
	}

	/**
	 * Get the standard deviation of X axis
	 * @return float data type of standard deviation of X axis
	 */
	public synchronized float getStandardDeviationX(){
		return this.standardDeviation[0];
	}

	/**
	 * Set the standard deviation of Y axis over certain amount of times
	 * @param value standard deviation of Y axis
	 */
	public synchronized void setStandardDeviationY(float value){
		this.standardDeviation[1] = value;
	}

	/**
	 * Get the standard deviation of Y axis
	 * @return float data type of standard deviation of Y axis
	 */
	public synchronized float getStandardDeviationY(){
		return this.standardDeviation[1];
	}
	
	/**
	 * Set the standard deviation of Z axis over certain amount of times
	 * @param value standard deviation of Z axis
	 */
	public synchronized void setStandardDeviationZ(float value){
		this.standardDeviation[2] = value;
	}

	/**
	 * Get the standard deviation of Z axis
	 * @return float data type of standard deviation of Z axis
	 */
	public synchronized float getStandardDeviationZ(){
		return this.standardDeviation[2];
	}

	/**
	 * Set the mean of X axis over certain amount of times
	 * @param value mean of X axis
	 */
	public synchronized void setMeanX(float value){
		this.mean[0] = value;
	}

	/**
	 * Get the mean of X axis
	 * @return float data type of mean of X axis
	 */
	public synchronized float getMeanX(){
		return this.mean[0];
	}

	/**
	 * Set the mean of Y axis over certain amount of times
	 * @param value mean of Y axis
	 */
	public synchronized void setMeanY(float value){
		this.mean[1] = value;
	}

	/**
	 * Get the mean of Y axis
	 * @return float data type of mean of Y axis
	 */
	public synchronized float getMeanY(){
		return this.mean[1];
	}
	
	/**
	 * Set the mean of Z axis over certain amount of times
	 * @param value mean of Z axis
	 */
	public synchronized void setMeanZ(float value){
		this.mean[2] = value;
	}

	/**
	 * Get the mean of Z axis
	 * @return float data type of mean of Z axis
	 */
	public synchronized float getMeanZ(){
		return this.mean[2];
	}

	/**
	 * Set the number of windows used to compute the mean and standard deviation
	 * @param value
	 * the number of windows used to compute the mean and standard deviation
	 */
	public synchronized void setCount(int value){
		this.count = value;
	}

	/**
	 * Get the number of samples used to compute the mean and standard deviation
	 * @return float
	 * the number of windows used to compute the mean and standard deviation
	 */
	public synchronized int getCount(){
		return this.count;
	}
	
	/**
	 * Set the posting account state 
	 * @param value should be 1 if account details is posted, 0 otherwise
	 */
	public synchronized void setAccountState(boolean value){
		this.isAccountSent = value;
	}

	/**
	 * Get the posting account state 
	 * @return 1 if account details is posted, 0 otherwise
	 */
	public synchronized boolean isAccountSent(){
		return this.isAccountSent;
	}

	/**
	 * Set the Wake Lock state
	 * @param value should be 1 if wake lock is set, 0 otherwise
	 */
	public synchronized void setWakeLockState(boolean value){
		this.isWakeLockSet = value;
	}

	/**
	 * Get the Wake Lock state
	 * @return 1 if wake lock is set, 0 otherwise
	 */
	public synchronized boolean isWakeLockSet(){
		return this.isWakeLockSet;
	}


	/**
	 * @return whether to use the aggregator or not
	 */
	public boolean getUseAggregator() {
		return useAggregator;
	}


	/**
	 * @param useAggregator
	 * whether to use the aggregator or not
	 */
	public void setUseAggregator(boolean useAggregator) {
		this.useAggregator = useAggregator;
	}


	/**
	 * @return the allowed Multiples Of Deviation
	 */
	public float getAllowedMultiplesOfDeviation() {
		return allowedMultiplesOfDeviation;
	}


	/**
	 * @param allowedMultiplesOfDeviation the allowed Multiples Of Deviation to set
	 */
	public void setAllowedMultiplesOfDeviation(float allowedMultiplesOfDeviation) {
		this.allowedMultiplesOfDeviation = allowedMultiplesOfDeviation;
	}
}
