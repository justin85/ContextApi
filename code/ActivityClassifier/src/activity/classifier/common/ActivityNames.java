package activity.classifier.common;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import activity.classifier.R;
import activity.classifier.model.ModelReader;
import activity.classifier.service.RecorderService;
import android.content.Context;
import android.util.Log;

public class ActivityNames {
	
	public static final String OFF						= "OFF";
	public static final String END						= "END";
	public static final String UNKNOWN					= "UNKNOWN";
	public static final String UNCARRIED				= "CLASSIFIED/UNCARRIED";
	public static final String CHARGING					= "CLASSIFIED/CHARGING";
	public static final String CHARGING_TRAVELLING		= "CLASSIFIED/CHARGING/TRAVELLING";
	
	/**
	 * Check if the given activity is a system-based activity,
	 * activities such as END, are not there for the user but for the system.
	 */
	public static boolean isSystemActivity(String activity) {
		return END.equals(activity) || OFF.equals(activity);
	}
	
	public static Set<String> getAllActivities(Context context) {
		
		Map<Float[],String> model = ModelReader.getModel(context, R.raw.basic_model);
		
		Set<String> allActivities = new TreeSet<String>(new StringComparator(false));
		
        for (Map.Entry<Float[], String> entry : model.entrySet()) {
        	String activity = entry.getValue();
        	allActivities.add(activity);
        }
        
        allActivities.add(OFF);
        allActivities.add(UNKNOWN);
        allActivities.add(UNCARRIED);
        allActivities.add(CHARGING);
        
        return allActivities;
	}

}
