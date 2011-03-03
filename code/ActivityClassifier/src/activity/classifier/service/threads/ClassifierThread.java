/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activity.classifier.service.threads;

import activity.classifier.accel.SampleBatch;
import activity.classifier.accel.SampleBatchBuffer;
import activity.classifier.classifier.Classifier;
import activity.classifier.classifier.KnnClassifier;
import activity.classifier.common.Constants;
import activity.classifier.repository.OptionQueries;
import activity.classifier.repository.TestAVQueries;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.service.RecorderService;
import activity.classifier.utils.CalcStatistics;
import activity.classifier.utils.Calibrator;
import activity.classifier.utils.RotateSamplesToVerticalHorizontal;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * ClassifierService class is a Service analyse the sensor data to classify
 * activities. RecorderService class invokes this class when sampling is done,
 * and send parameters (data collection, size of data array,battery status, etc)
 * which is useful to determine the activities. After done with classification,
 * it notices RecorderService about what activity is classified.
 * 
 * 
 * Standard Deviation(sd) and Average values for accelerations(average) are used
 * to classify Uncarried state. chargingState(battery status) is used to
 * classify Charging state.
 * 
 * Other activities are classified through KNN algorithm (with K=1). (This KNN
 * classification is implemented in Aggregator.java)
 * 
 * Local database is used to store some meaningful information such as sd,
 * average, lastaverage (the average of acceleration values when the activity is
 * Uncarried, if the activity is not a Uncarried, then the values is 0.0).
 * 
 * <p>
 * Changes made by Umran: <br>
 * The class used to be called ClassifierService. Now changed to a thread.
 * Communication between {@link RecorderService} and this class is done through
 * the {@link SampleBatch} and {@link SampleBatchBuffer}.
 * <p>
 * Filled batches are posted into the buffer in {@link RecorderService} and
 * removed here, after analysis, the batches are posted back into the buffer as
 * empty batches where the recorder class removes them and fills them with
 * sampled data.
 * 
 * @author chris, modified by Justin Lee
 * 
 * 
 */
public class ClassifierThread extends Thread {
	
	private ActivityRecorderBinder service;
	private SampleBatchBuffer batchBuffer;
	
	private OptionQueries optionQuery;
	private TestAVQueries testavQuery;

	private CalcStatistics calcSampleStatistics = new CalcStatistics(Constants.ACCEL_DIM);	
	private RotateSamplesToVerticalHorizontal rotateSamples = new RotateSamplesToVerticalHorizontal();
	private Classifier classifier;

	private boolean isCalibrated;
	private Calibrator calibrator;
	
	private boolean shouldExit;
	
	public ClassifierThread(Context context, ActivityRecorderBinder service,
			SampleBatchBuffer sampleBatchBuffer) {
		this.service = service;
		this.batchBuffer = sampleBatchBuffer;
		
		testavQuery = new TestAVQueries(context);

		this.classifier = new KnnClassifier(RecorderService.model.entrySet());
		this.optionQuery = new OptionQueries(context);
		
		this.optionQuery.load();
		isCalibrated = this.optionQuery.isCalibrated();
		if (isCalibrated) {
			calibrator = new Calibrator(
					service, 
					calcSampleStatistics,
					true,
					this.optionQuery.getCount(),
					new float[] {
						this.optionQuery.getStandardDeviationX(),
						this.optionQuery.getStandardDeviationY(),
						this.optionQuery.getStandardDeviationZ(),
					},
					new float[] {
						this.optionQuery.getMeanX(),
						this.optionQuery.getMeanY(),
						this.optionQuery.getMeanZ(),
					},
					this.optionQuery.getValueOfGravity()
				);
		} else {
			calibrator = new Calibrator(
					service,
					calcSampleStatistics,
					false,
					0,
					new float[] {
						Constants.CALIBARATION_ALLOWED_DEVIATION,
						Constants.CALIBARATION_ALLOWED_DEVIATION,
						Constants.CALIBARATION_ALLOWED_DEVIATION
					},
					new float[] {
						0,
						0,
						0
					},
					Constants.GRAVITY
				);
		}
		
		this.shouldExit = false;
	}

	/**
	 * Stops the thread cautiously
	 */
	public synchronized void exit() {
		// signal the thread to exit
		this.shouldExit = false;

		// if the thread is blocked waiting for a filled batch
		// interrupt the thread
		this.interrupt();
	}

	/**
	 * Classification start
	 */
	public void run() {

		Log.v(Constants.DEBUG_TAG, "Classification thread started.");
		while (!this.shouldExit) {
			try {
				// in case of too sampling too fast, or too slow CPU, or the
				// classification taking too long
				// check how many batches are pending
				int pendingBatches = batchBuffer.getPendingFilledInstances();
				if (pendingBatches == SampleBatchBuffer.TOTAL_BATCH_COUNT) {
					// issue an error if too many
					service.showServiceToast("Unable to classify sensor data fast enough!");
				}

				// this function blocks until a filled sample batch is obtained
				SampleBatch batch = batchBuffer.takeFilledInstance();
				
				Log.v(Constants.DEBUG_TAG, "Classifier thread received batch");

				// process the sample batch to obtain the classification
				String classification = processData(batch);
				Log.v(Constants.DEBUG_TAG, "Classification found: '"+classification+"'");

				// return the sample batch to the buffer as an empty batch
				batchBuffer.returnEmptyInstance(batch);
				if (classification!=null && classification.length()>0) {
					// submit the classification
					service.submitClassification(classification);
				}
			} catch (RemoteException ex) {
				Log.e(Constants.DEBUG_TAG,
						"Exception error occured in connection in ClassifierService class");
			} catch (InterruptedException e) {
				Log.e(Constants.DEBUG_TAG,
						"Exception occured while performing classification", e);
			}
		}
		Log.v(Constants.DEBUG_TAG, "Classification thread exiting.");

	}

	private String processData(SampleBatch batch) throws InterruptedException, RemoteException {
		Log.v(Constants.DEBUG_TAG, "Processing batch.");
		
		//	get local copies of the data
		float[][] data = batch.data;
		int size = batch.getSize();
		long sampleTime = batch.sampleTime;
		boolean chargingState = false;//batch.isCharging();
		float[] dataMeans = calcSampleStatistics.createVector(),
				dataSd = calcSampleStatistics.createVector();
		
		calibrator.processData(sampleTime, data, size, dataMeans, dataSd);
		
		if (!isCalibrated && calibrator.isCalibrated()) {
			Log.v(Constants.DEBUG_TAG, "Calibration just finished. Saving values to DB.");
			
			float[] sd = calibrator.getSd();
			float[] mean = calibrator.getMean();
			int count = calibrator.getCount();
			
			optionQuery.setCalibrationState(true);
			optionQuery.setMeanX(mean[Constants.ACCEL_X_AXIS]);
			optionQuery.setMeanY(mean[Constants.ACCEL_Y_AXIS]);
			optionQuery.setMeanZ(mean[Constants.ACCEL_Z_AXIS]);
			optionQuery.setStandardDeviationX(sd[Constants.ACCEL_X_AXIS]);
			optionQuery.setStandardDeviationY(sd[Constants.ACCEL_Y_AXIS]);
			optionQuery.setStandardDeviationZ(sd[Constants.ACCEL_Z_AXIS]);
			optionQuery.setCount(count);
			optionQuery.setValueOfGravity(calibrator.getValueOfGravity());
			
			optionQuery.save();
			
			isCalibrated = true;
		}
		
		String classification = "UNCLASSIFIED/UNKNOWN";
		
		//	check the current gravity, rotate and perform classification
		{			
			float calcGravity = calcSampleStatistics.calcMag(dataMeans);
			float diffGravity = calcGravity-Constants.GRAVITY;
			if (diffGravity<0.0)
				diffGravity = -diffGravity;
			if (diffGravity>Constants.GRAVITY_DEV) {
				Log.v(Constants.DEBUG_TAG, "Deviation from gravity too large! Found gravity="+calcGravity+", expected gravity="+Constants.GRAVITY);
				return "";
			}

			// first rotate samples to world-orientation
			//	TODO: Gravity and Gravity Deviation values should come from the calibration values,
			//		which means calibration should be handled in some other way.
			if (rotateSamples.rotateToWorldCoordinates(dataMeans, data)) {
				classification = classifier.classifyRotated(data);
			} else {
				Log.v(Constants.DEBUG_TAG, "Unable to perform classification, data could not be rotated!");
			}
		}
		
		if (calibrator.isUncarried()) {
			if (classification.contains("TRAVELLING"))
				return "CLASSIFIED/UNCARRIED/TRAVELLING";
			else
				return "CLASSIFIED/UNCARRIED";
		}
		
		if (chargingState) {
			if (classification.contains("TRAVELLING"))
				return "CLASSIFIED/CHARGING/TRAVELLING";
			else
				return "CLASSIFIED/CHARGING";
		}
		
		return classification;
	}
	
}
