/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activity.classifier.rpc;

import java.text.SimpleDateFormat;
import java.util.Date;

import activity.classifier.common.Constants;
import activity.classifier.db.ActivitiesTable;
import activity.classifier.repository.ActivityQueries;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.Log;

/**
 *
 * @author chris
 */
public class Classification implements Parcelable, Comparable<Classification> {
	
	private CharSequence niceClassification;
	private String startTime;
	private String endTime;
	private String duration="";
	private String classification;
	private long start;
	private long end;
	private boolean isChecked;
	private long lastUpdate;
	
	public Classification() {
		this.classification = "";
		this.start = 0;
		this.end = 0;
		this.isChecked = false;
	}
	
	public Classification(String classification, long start) {
		this.classification = classification;
		if (this.classification==null)
			throw new RuntimeException("Invalid classification with classification name as NULL");
		this.start = start;
		this.end = start;
		this.isChecked = false;
		
		computeDuration();
	}
	
	public Classification(String classification, long start, long end) {
		this.classification = classification;
		if (this.classification==null)
			throw new RuntimeException("Invalid classification with classification name as NULL");
		this.start = start;
		this.end = end;
		this.isChecked = false;
		
		computeDuration();
	}
	
	private void computeDuration() {
		//        final String duration;
		int length = (int) ((end - start) / 1000);

		if (length < 60) {
			duration = "<1 min";
		} else if (length < 60 * 60) {
			duration = (length / 60) + " mins";
		} else {
			duration = (length / (60 * 60)) + " hrs";
		}
	}
	

	public String getStartTime(){
		return startTime;
	}
	
	public String getEndTime(){
		return endTime;
	}
	
	public String getDuration(){
		return duration;
	}
	
	public String getNiceClassification(){
		return (String) niceClassification;
	}
	
	public int describeContents() {
		return 0;
	}
	
	/**
	 * @param classification the classification to set
	 */
	public void setClassification(String classification) {
		this.classification = classification;
	}
	
	public String getClassification() {
		return classification;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(long start) {
		this.start = start;
		computeDuration();
	}
	
	public void setEnd(long end) {
		this.end = end;
		computeDuration();
	}

	public long getEnd() {
		return end;
	}

	public long getStart() {
		return start;
	}
	
	/**
	 * @return the isChecked
	 */
	public boolean isChecked() {
		return isChecked;
	}

	/**
	 * @param isChecked the isChecked to set
	 */
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
	
	/**
	 * @return the lastUpdate
	 */
	public long getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @param lastUpdate the lastUpdate to set
	 */
	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	@Override 
	public String toString() {
		if (ActivitiesTable.isSystemActivity(classification)) {
			return niceClassification+"";
		}
		else{
			return niceClassification + "\n" + startTime + " for " + duration;
		}
	}

	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeString(classification);
		arg0.writeLong(start);
		arg0.writeLong(end);
	}

	public void withContext(Context context) {
		if (classification==null) {
			throw new RuntimeException("No classification exists");
		}
		
		this.niceClassification = getNiceName(context, classification);

		Date date = new Date(start);
		Date enddate = new Date(end);

		startTime = Constants.DB_DATE_FORMAT.format(date);
		endTime = Constants.DB_DATE_FORMAT.format(enddate);
	}
	
	public static String getNiceName(Context context, String classification)
	{
		String name = "activity_" + 
				((classification==null || classification.length() == 0)? 
					"unknown" :
					classification.replace("/", "_").toLowerCase()
					);
	
		//Log.v(Constants.DEBUG_TAG, "Classification derived name: '"+name+"' from: '"+classification+"'");

		int id = context.getResources().getIdentifier(
				name, "string", "activity.classifier");
		if (id>0)
			return (String) context.getResources().getText(id);
		else {
			throw new RuntimeException("Unrecognized Activity classified as '"+classification+"' ('"+name+"')");
			//return classification;
		}
	}

	public static final Parcelable.Creator<Classification> CREATOR
	= new Parcelable.Creator<Classification>() {

		public Classification createFromParcel(Parcel arg0) {
			final Classification res = new Classification(arg0.readString(), arg0.readLong());
			res.setEnd(arg0.readLong());
			return res;
		}

		public Classification[] newArray(int arg0) {
			return new Classification[arg0];
		}

	};

	@Override
	public int compareTo(Classification another) {
		return (int)(this.start-another.start);
	}

}
