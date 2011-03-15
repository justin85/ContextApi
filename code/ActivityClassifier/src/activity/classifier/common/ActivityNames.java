package activity.classifier.common;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import activity.classifier.service.RecorderService;
import android.util.Log;

public class ActivityNames {
	
	public static final String OFF						= "OFF";
	public static final String END						= "END";
	public static final String UNKNOWN					= "UNKNOWN";
	public static final String UNCARRIED				= "CLASSIFIED/UNCARRIED";
	public static final String CHARGING					= "CLASSIFIED/CHARGING";
	public static final String CHARGING_TRAVELLING		= "CLASSIFIED/CHARGING/TRAVELLING";
	
	public static final Set<String> ALL_ACTIVITIES = new TreeSet<String>(new StringComparator(false));
	
	/**
	 * Check if the given activity is a system-based activity,
	 * activities such as END, are not there for the user but for the system.
	 */
	public static boolean isSystemActivity(String activity) {
		return END.equals(activity) || OFF.equals(activity);
	}
	
	static {
        for (Map.Entry<Float[], String> entry : RecorderService.model.entrySet()) {
        	String activity = entry.getValue();
        	ALL_ACTIVITIES.add(activity);
        }
        
        ALL_ACTIVITIES.add(OFF);
        ALL_ACTIVITIES.add(UNKNOWN);
        ALL_ACTIVITIES.add(UNCARRIED);
        ALL_ACTIVITIES.add(CHARGING);
	}

}
