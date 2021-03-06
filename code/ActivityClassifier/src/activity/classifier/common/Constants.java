package activity.classifier.common;

import java.io.File;
import java.text.SimpleDateFormat;

import activity.classifier.activity.ActivityChartActivity;
import activity.classifier.service.RecorderService;
import activity.classifier.service.threads.AccountThread;
import activity.classifier.service.threads.UploadActivityHistoryThread;
import android.graphics.Color;
import android.os.Environment;

/**
 * 
 * @author Umran
 *
 */
public class Constants {
	
	/**
	 *	<p>Affects the options application has.</p>
	 *	<p>
	 *  Extra options are given to developers. Like:<br/>
	 *		pick whether aggregation is used or not.
	 *	</p> 
	 */
	public final static boolean IS_DEV_VERSION = true;
	
	/**
	 * Should we or should we not output debugging information?
	 */
	public static final boolean OUTPUT_DEBUG_INFO = true;
	
	/**
	 * The delay between two consecutive sampling batches.
	 */
	//	TODO: CHANGE THIS BEFORE COMMIT
//	public static final int DELAY_SAMPLE_BATCH = 30*1000; //	30 secs in ms
	public static final int DELAY_SAMPLE_BATCH = 10*1000; //	debugging use
	
	/**
	 * The delay between two consecutive samples in a sample batch.
	 */
	public static final int DELAY_BETWEEN_SAMPLES = 50; //	50ms
	
	/**
	 * Duration for the {@link AccountThread} to wait for the user's account
	 * to be set on the phone before checking again.
	 */
	public static final int DURATION_WAIT_FOR_USER_ACCOUNT = 60*60*1000; // 1 hr in ms
	
	/**
	 * The interval between successive user interface updates in the {@link ActivityRecorderActivity}
	 */
	public static final int DELAY_UI_UPDATE = 1000;
	
	/**
	 * The interval between successive user interface updates in the {@link ActivityChartActivity}
	 * This value should be quite large because updating the chart takes a large amount of
	 * processing.
	 */
	public static final int DELAY_UI_GRAPHIC_UPDATE = DELAY_SAMPLE_BATCH;
	
	/**
	 * The interval between successive data uploads in {@link UploadActivityHistoryThread}
	 */
	public static final int DELAY_UPLOAD_DATA = 5*60*1000;	//	5min in ms
	
	/**
	 * The delay after the dialog appears and before the {@link RecorderService} in
	 * the 'start service' display sequence in the class {@link ActivityRecorderActivity}
	 */
	public static final int DELAY_SERVICE_START = 1000;

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
	public static final String PATH_SD_CARD_DUMP_DB = 
		Environment.getExternalStorageDirectory() + File.separator + 
		"activityclassifier" + File.separator + RECORDS_FILE_NAME;
	
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
	public static final float GRAVITY = 9.81f;
	
	/**
	 *	The deviation from gravity that a sample is allowed,
	 *	any deviation greater than this makes the sample invalid.
	 *
	 *	Lesser values can be caused due to the phone being rotated
	 *	during the sampling period, while larger values can be caused
	 *	because of the phone accelerating fast during the sampling period
	 *	e.g. in a car after a traffic light.
	 */
	public static final float MIN_GRAVITY_DEV = 0.5f; // 15% of gravity	
	public static final float MAX_GRAVITY_DEV = 0.5f; // 100% of gravity	
	
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
	public final static long DURATION_OF_CALIBRATION = 60*1000; // 60 seconds
//	public final static long DURATION_OF_CALIBRATION = 10*60*1000; // 60 seconds

	/**
	 * The deviation in the means & sdof the accelerometer axis
	 * 	within the calibration waiting duration given as {@link #DURATION_WAIT_FOR_CALIBRATION}
	 * 
	 * This value is only used initially, after calibration, the values used in
	 * the calibration are used instead.
	 * 
	 * See {@link #CALIBARATION_ALLOWED_MULTIPLES_DEVIATION} for more info.
	 * See {@link #CALIBARATION_MIN_ALLOWED_BASE_DEVIATION} for more info.
	 */
	public final static float CALIBARATION_ALLOWED_BASE_DEVIATION = 0.05f;

	/**
	 * The minimum deviation in the means & sd of the accelerometer axis
	 * 	within the calibration waiting duration given as {@link #DURATION_WAIT_FOR_CALIBRATION}
	 * 
	 * See {@link #CALIBARATION_ALLOWED_MULTIPLES_DEVIATION} for more info.
	 * See {@link #CALIBARATION_ALLOWED_BASE_DEVIATION} for more info.
	 */
	public final static float CALIBARATION_MIN_ALLOWED_BASE_DEVIATION = 0.1f;
	
	/**
	 * This value is multiplied by the {@value #CALIBARATION_ALLOWED_BASE_DEVIATION} value
	 * to get the range which the phone's accelerometer axis standard deviation is allowed to be in 
	 * for the phone to be detected as stationary. 
	 * 
	 * The {@value #CALIBARATION_ALLOWED_BASE_DEVIATION} value is only used initially, 
	 * after calibration, the values used in the calibration are used instead.
	 * 
	 */
	public final static float CALIBARATION_ALLOWED_MULTIPLES_DEVIATION = 2.0f;	// 2 times the standard deviation
	
	/**
	 * The duration which the means of different axis should be the same for the state
	 * to be uncarried
	 */
	public final static long DURATION_WAIT_FOR_UNCARRIED = 60*1000;
	
	/**
	 * The format used to store date values into the database
	 */
	public final static SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z");
	
	/**
	 * The maximum duration that debugging data should be maintained in the database
	 */
	public final static long DURATION_KEEP_DB_DEBUG_DATA = 12*60*60*1000L;
	
	/**
	 * The maximum duration that activity data should be maintained in the database
	 */
	public final static long DURATION_KEEP_DB_ACTIVITY_DATA = 7*24*60*60*1000L;
	
	/**
	 * The size of footer texts
	 */
	public final static int FOOTER_SIZE = 3;
	
	/**
	 * The names of footers
	 */
	public final static String[] FOOTER_NAMES = { "Last Hour", "Last 4Hours", "Today",}; 
	
	/**
	 * Color of line
	 */
	public final static int COLOR_LINE = Color.argb(255, 32, 33, 38);
	
	/**
	 * Colors of activities
	 */
	public final static int[] COLOR_ACTIVITIES = {
		
		Color.argb(255, 114, 141, 108),
		Color.argb(255, 255, 97, 78),
		Color.argb(255, 109, 206, 250),
		Color.argb(255, 244, 141, 62),
		Color.argb(255, 237, 142, 107),
		Color.argb(255, 181, 40, 65),
		Color.argb(255, 181, 204, 122),
		
	};
	
	
}
