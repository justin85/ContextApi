/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activity.classifier.service.threads;

import java.util.Arrays;

import activity.classifier.accel.SampleBatch;
import activity.classifier.accel.SampleBatchBuffer;
import activity.classifier.aggregator.Aggregator;
import activity.classifier.classifier.Classifier;
import activity.classifier.classifier.KnnClassifier;
import activity.classifier.common.Constants;
import activity.classifier.db.ActivitiesTable;
import activity.classifier.db.DebugDataTable;
import activity.classifier.db.OptionsTable;
import activity.classifier.db.SqlLiteAdapter;
import activity.classifier.repository.OptionQueries;
import activity.classifier.repository.TestAVQueries;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.service.RecorderService;
import activity.classifier.utils.CalcStatistics;
import activity.classifier.utils.Calibrator;
import activity.classifier.utils.FeatureExtractor;
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
	
	private SqlLiteAdapter sqlLiteAdapter;
	private OptionsTable optionsTable;
	private DebugDataTable debugDataTable;

	private CalcStatistics rawSampleStatistics = new CalcStatistics(Constants.ACCEL_DIM);
	
	private float[][] rotatedMergedSamples = new float[Constants.NUM_OF_SAMPLES_PER_BATCH][2];
	private CalcStatistics rotatedMergedSampleStatistics = new CalcStatistics(2);	
	
	private RotateSamplesToVerticalHorizontal rotateSamples = new RotateSamplesToVerticalHorizontal();
	private Classifier classifier;
	private Aggregator aggregator;

	private boolean isCalibrated;
	private Calibrator calibrator;
	
	private boolean shouldExit;
	
	public ClassifierThread(Context context, ActivityRecorderBinder service,
			SampleBatchBuffer sampleBatchBuffer) {
    	super(ClassifierThread.class.getName());
    	
		this.service = service;
		this.batchBuffer = sampleBatchBuffer;
		
		this.sqlLiteAdapter = SqlLiteAdapter.getInstance(context);
		this.optionsTable = sqlLiteAdapter.getOptionsTable();
		this.debugDataTable = sqlLiteAdapter.getDebugDataTable();

		this.classifier = new KnnClassifier(RecorderService.model.entrySet());
		this.aggregator = new Aggregator();
		
		calibrator = new Calibrator(
				service, 
				this.optionsTable.isCalibrated(),
				this.optionsTable.getAllowedMultiplesOfSd(),
				this.optionsTable.getCount(),
				this.optionsTable.getSd().clone(),
				this.optionsTable.getMean().clone(),
				this.optionsTable.getValueOfGravity()
			);
		
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
				long sampleTime = batch.sampleTime;
				String classification = processData(batch);
				Log.v(Constants.DEBUG_TAG, "Classification found: '"+classification+"'");

				// return the sample batch to the buffer as an empty batch
				batchBuffer.returnEmptyInstance(batch);
				if (classification!=null && classification.length()>0) {
					// submit the classification
					service.submitClassification(sampleTime, classification);
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
		
		if (Constants.OUTPUT_DEBUG_INFO) {
			debugDataTable.reset(sampleTime);
		}
		
		//	take out accelerometer axis offsets
		if (calibrator.isCalibrated()) {
			float[] axisMeans = optionsTable.getMean();
			float gravity = optionsTable.getValueOfGravity();
			for (int i=0; i<size; ++i) {
				data[i][Constants.ACCEL_X_AXIS] -= axisMeans[Constants.ACCEL_X_AXIS]; 
				data[i][Constants.ACCEL_Y_AXIS] -= axisMeans[Constants.ACCEL_Y_AXIS];
				data[i][Constants.ACCEL_Z_AXIS] -= (axisMeans[Constants.ACCEL_Z_AXIS] - gravity);
			}
		}
		
		rawSampleStatistics.assign(data, size);
		
		float[] dataMin = rawSampleStatistics.getMin();
		float[] dataMax = rawSampleStatistics.getMax();
		float[] dataMeans = rawSampleStatistics.getMean();
		float[] dataSd = rawSampleStatistics.getStandardDeviation();
		
		if (Constants.OUTPUT_DEBUG_INFO) {
			debugDataTable.setUnrotatedStats(
					dataMeans[Constants.ACCEL_X_AXIS],
					dataMeans[Constants.ACCEL_Y_AXIS],
					dataMeans[Constants.ACCEL_Z_AXIS],
					dataSd[Constants.ACCEL_X_AXIS],
					dataSd[Constants.ACCEL_Y_AXIS],
					dataSd[Constants.ACCEL_Z_AXIS],
					dataMax[Constants.ACCEL_X_AXIS]-dataMin[Constants.ACCEL_X_AXIS],
					dataMax[Constants.ACCEL_Y_AXIS]-dataMin[Constants.ACCEL_Y_AXIS],
					dataMax[Constants.ACCEL_Z_AXIS]-dataMin[Constants.ACCEL_Z_AXIS]
					          );
		}
		
		calibrator.processData(sampleTime, dataMeans, dataSd);
		
		if (!isCalibrated && calibrator.isCalibrated()) {
			Log.v(Constants.DEBUG_TAG, "Calibration just finished. Saving values to DB.");
			
			float[] sd = calibrator.getSd();
			float[] mean = calibrator.getMean();
			
			optionsTable.setCalibrated(true);
			optionsTable.setMean(mean);
			optionsTable.setSd(sd);
			optionsTable.setCount(calibrator.getCount());
			optionsTable.setValueOfGravity(calibrator.getValueOfGravity());
			optionsTable.save();
			
			isCalibrated = true;
		}
		
		String classification = "UNCLASSIFIED/UNKNOWN";
		
		//	check the current gravity, rotate and perform classification
		{
			float gravity = calibrator.getValueOfGravity();
			float calcGravity = rawSampleStatistics.calcMag(dataMeans);
			float minGravity = gravity - gravity*Constants.MIN_GRAVITY_DEV;
			float maxGravity = gravity + gravity*Constants.MIN_GRAVITY_DEV;
			
			if (calcGravity>=minGravity && calcGravity<=maxGravity) {
				if (!(calcGravity>=minGravity))
					Log.v(Constants.DEBUG_TAG, "Gravity too low! Found gravity="+calcGravity+", expected ["+minGravity+","+maxGravity+"]");
				if (!(calcGravity<=maxGravity))
					Log.v(Constants.DEBUG_TAG, "Gravity too high! Found gravity="+calcGravity+", expected ["+minGravity+","+maxGravity+"]");
				// first rotate samples to world-orientation
				if (rotateSamples.rotateToWorldCoordinates(dataMeans, data)) {
					classification = classifier.classifyRotated(data);
					
					if (Constants.OUTPUT_DEBUG_INFO) {
						logRotatedValues(dataMeans, data, size);
						debugDataTable.setClassifierAlgoOutput(classification);
					}
					
					if (optionsTable.getUseAggregator()) {
						aggregator.addClassification(classification);
						String aggrClassification = aggregator.getClassification();
						if (aggrClassification!=null && !ActivitiesTable.isSystemActivity(aggrClassification)) {
							classification = aggrClassification;
						}

						if (Constants.OUTPUT_DEBUG_INFO) {
							debugDataTable.setAggregatorAlgoOutput(aggrClassification);
						}
					} else {
						if (Constants.OUTPUT_DEBUG_INFO) {
							debugDataTable.setAggregatorAlgoOutput("NOT USING AGGREGATOR");
						}
					}
					
				} else {
					Log.v(Constants.DEBUG_TAG, "Unable to perform classification, data could not be rotated!");
					if (Constants.OUTPUT_DEBUG_INFO) {
						debugDataTable.setClassifierAlgoOutput("ERROR: Unable to rotate gravity "+Arrays.toString(dataMeans));
					}
				}
			} else {
				if (Constants.OUTPUT_DEBUG_INFO) {
					debugDataTable.setClassifierAlgoOutput("ERROR: Gravity "+calcGravity+" not within limits: ["+minGravity+","+maxGravity+"]");
				}
			}
		}
		
		if (chargingState) {
			if (classification.contains("TRAVELLING"))
				classification = "CLASSIFIED/CHARGING/TRAVELLING";
			else
				classification = "CLASSIFIED/CHARGING";
		} else {
			if (calibrator.isUncarried()) {
//				if (classification.contains("TRAVELLING"))
//					return "CLASSIFIED/UNCARRIED/TRAVELLING";
//				else
					classification = "CLASSIFIED/UNCARRIED";
			}
		}
		
		if (Constants.OUTPUT_DEBUG_INFO) {
			debugDataTable.setFinalClassifierOutput(classification);
			debugDataTable.trim();
			debugDataTable.insert();
		}
		
		return classification;
	}
	
	private void logRotatedValues(float[] dataMeans, float[][] rotatedData, int dataSize)
	{
		for (int j=0; j<Constants.NUM_OF_SAMPLES_PER_BATCH; ++j) {
			rotatedMergedSamples[j][0] = (float)Math.sqrt(
					rotatedData[j][0]*rotatedData[j][0] +
					rotatedData[j][1]*rotatedData[j][1]);
			rotatedMergedSamples[j][1] = rotatedData[j][2];
		}
		
		rotatedMergedSampleStatistics.assign(rotatedMergedSamples, dataSize);
		
		float[] rotatedMin = rotatedMergedSampleStatistics.getMin();
		float[] rotatedMax = rotatedMergedSampleStatistics.getMax();
		float[] rotatedMeans = rotatedMergedSampleStatistics.getMean();
		float[] rotatedSd = rotatedMergedSampleStatistics.getStandardDeviation();
		
		debugDataTable.setRotatedStats(
				rotatedMeans[0],
				rotatedMeans[1],
				
				rotatedMax[0]-rotatedMin[0],
				rotatedMax[1]-rotatedMin[1],
				
				rotatedSd[0],
				rotatedSd[1]
				);
		
	}
	
}
