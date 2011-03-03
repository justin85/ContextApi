package activity.classifier.accel;

import activity.classifier.common.Constants;
import android.util.Log;

/**
 * 
 * @author Umran
 * 
 * <p>
 * Represents a batch of consecutive samples taken within one sampling period.
 * 
 * <p>
 * By placing the samples taken within a single sampling period in one class,
 * it is possible to move a reference to the batch instead of copying the
 * batch. In addition, we can place statistical information extracted from
 * the batch, in the same class.
 *
 */
public class SampleBatch {
	
	public long sampleTime;
	public final float[][] data;
	private int currentSample;
	private boolean charging;
	
	public SampleBatch() {
		data = new float[Constants.NUM_OF_SAMPLES_PER_BATCH][Constants.ACCEL_DIM];
		currentSample = 0;
	}
	
	public float[] getCurrentSample() {
		return data[currentSample];
	}
	
	public boolean nextSample() {
		++currentSample;
		
		if (currentSample>=Constants.NUM_OF_SAMPLES_PER_BATCH) {
			currentSample = Constants.NUM_OF_SAMPLES_PER_BATCH;
			return false;
		}
		else {
			return true;
		}
	}
	
	public void reset() {
		currentSample = 0;
	}
	
	public int getSize() {
		return currentSample;
	}

	public boolean isCharging() {
		return charging;
	}

	public void setCharging(boolean charging) {
		this.charging = charging;
	}
	
	
}
