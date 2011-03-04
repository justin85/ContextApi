package activity.classifier.common;

import java.text.SimpleDateFormat;

import activity.classifier.service.RecorderService;
import activity.classifier.service.threads.AccountThread;
import activity.classifier.service.threads.UploadActivityHistoryThread;

/**
 * 
 * @author Umran
 *
 */
public class Constants {
	
	/**
	 * Duration for the {@link AccountThread} to wait for the user's account
	 * to be set on the phone before checking again.
	 */
	public static final int DURATION_WAIT_FOR_USER_ACCOUNT = 60*60*1000; // 1 hr in ms
	
	/**
	 * The interval between successive user interface updates in the {@link ActivityRecorderActivity}
	 */
	public static final int DELAY_UI_UPDATE = 500;
	
	public static final int DELAY_UI_GRAPHIC_UPDATE = 15000;
	
	/**
	 * The interval between successive data uploads in {@link UploadActivityHistoryThread}
	 */
	public static final int DELAY_UPLOAD_DATA = 300*1000;	//	5min in ms
	
	/**
	 * The delay after the dialog appears and before the {@link RecorderService} in
	 * the 'start service' display sequence in the class {@link ActivityRecorderActivity}
	 */
	public static final int DELAY_SERVICE_START = 500;

	/**
	 * The delay between two consecutive sampling batches.
	 */
	//	TODO: CHANGE THIS BEFORE COMMIT
	public static final int DELAY_SAMPLE_BATCH = 30*1000; //	30 secs in ms
//	public static final int DELAY_SAMPLE_BATCH = 10*1000; //	debugging use
	
	/**
	 * The delay between two consecutive samples in a sample batch.
	 */
	public static final int DELAY_BETWEEN_SAMPLES = 50; //	50ms
	
	/**
	 * A tag that can be used to identify this application's log entries.
	 */
	public static final String DEBUG_TAG = "ActivityClassifier";
	
	/**
	 * Records file name.
	 */
	public static final String RECORDS_FILE_NAME = "activityrecords.db";	
	
	/**
	 * Path to the activity records database
	 */
	public static final String PATH_ACTIVITY_RECORDS_DB = "data/data/activity.classifier/databases/"+RECORDS_FILE_NAME;

	/**
	 * Path to the activity records file
	 */
	public static final String PATH_ACTIVITY_RECORDS_FILE = "data/data/activity.classifier/files/"+RECORDS_FILE_NAME;
	
	/**
	 * URL where the user's information is posted.
	 */
	public static final String URL_USER_DETAILS_POST = "http://activity.urremote.com/accountservlet";
	
	/**
	 * URL to where the user's activity history can be viewed.
	 */
	public static final String URL_ACTIVITY_HISTORY = "http://activity.urremote.com/actihistory.jsp";
	
	/**
	 * URL where the user's activity data is posted
	 */
	public static final String URL_ACTIVITY_POST = "http://activity.urremote.com/activity";

	
	/**
	 *	The number of accelerometer (x,y & z) samples in a batch of samples.
	 */
	public static final int NUM_OF_SAMPLES_PER_BATCH = 128;
	
	/**
	 *	The value of gravity
	 */
	public static final float GRAVITY = 9.8f;
	
	/**
	 *	The deviation from gravity that a sample is allowed,
	 *	any deviation greater than this makes the sample invalid.
	 *
	 *	Lesser values can be caused due to the phone being rotated
	 *	during the sampling period, while larger values can be caused
	 *	because of the phone accelerating fast during the sampling period
	 *	e.g. in a car after a traffic light.
	 */
	public static final float GRAVITY_DEV = 0.15f; // 15% of gravity	
	
	/**
	 * The number of axi on the accelerometer
	 */
	public final static int ACCEL_DIM = 3;
	
	/**
	 * The indexes of the x axis on the accelerometer
	 */
	public final static int ACCEL_X_AXIS = 0;

	/**
	 * The indexes of the y axis on the accelerometer
	 */
	public final static int ACCEL_Y_AXIS = 1;

	/**
	 * The indexes of the z axis on the accelerometer
	 */
	public final static int ACCEL_Z_AXIS = 2;
	
	/**
	 * The duration the phone is required to be stationary before doing calibration
	 */
	//	TODO: CHANGE THIS BEFORE COMMIT
	//public final static long DURATION_OF_CALIBRATION = 60*1000; // 60 seconds
	public final static long DURATION_OF_CALIBRATION = 10*60*1000; // 60 seconds

	/**
	 * The deviation in the means of the accelerometer axis
	 * 	within the calibration waiting duration given as {@link #DURATION_WAIT_FOR_CALIBRATION}
	 * 
	 * This value is only used initially, after calibration, the values used in
	 * the calibration are used instead.
	 * 
	 * See {@link #CALIBARATION_MAGNITUDE_ALLOWED_DEVIATION} for more info.
	 */
	public final static float CALIBARATION_BASE_ALLOWED_DEVIATION = 9.8f*0.03f;	// 3% of Gravity

	/**
	 * This value is multiplied by the {@value #CALIBARATION_BASE_ALLOWED_DEVIATION} value
	 * to get the range which the phone's accelerometer axis standard deviation is allowed to be in 
	 * for the phone to be detected as stationary. 
	 * 
	 * The {@value #CALIBARATION_BASE_ALLOWED_DEVIATION} value is only used initially, 
	 * after calibration, the values used in the calibration are used instead.
	 * 
	 */
	public final static float CALIBARATION_MAGNITUDE_ALLOWED_DEVIATION = 5.0f;	// 5 times the standard deviation
	
	/**
	 * The duration which the means of different axis should be the same for the state
	 * to be uncarried
	 */
	public final static long DURATION_WAIT_FOR_UNCARRIED = 60*1000;
	
	/**
	 * The format used to store date values into the database
	 */
	public final static SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z");
}
