package activity.classifier.repository;

import activity.classifier.common.Constants;
import android.content.Context;
import android.util.Log;

public class TestAVQueries extends Queries{

	private DbAdapter dbAdapter;

	private long sampleTime;
	private float meanX;
	private float meanY;
	private float meanZ;
	private Float horMean;
	private Float verMean;
	private Float horRange;
	private Float verRange;
	private Float horSd;
	private Float verSd;
	private String classifierAlgoOutput;
	private String finalClassifierOutput;
	
	/**
	 * @see Queries
	 * @param context context from Activity or Service classes 
	 */
	public TestAVQueries(Context context){
		super(context);
		dbAdapter = super.dbAdapter;
	}
	
	public void reset(long sampleTime) {
		this.sampleTime = sampleTime;
		this.meanX = Float.NaN;
		this.meanY = Float.NaN;
		this.meanZ = Float.NaN;
		this.horMean = null;
		this.verMean = null;
		this.horRange = null;
		this.verRange = null;
		this.horSd = null;
		this.verSd = null;
		this.classifierAlgoOutput = null;
		this.finalClassifierOutput = null;
	}
	
	public void setUnrotatedMeans(float x, float y, float z) {
		this.meanX = x;
		this.meanY = y;
		this.meanZ = z;
	}
	
	public void setRotatedStats(
			float horMean,
			float verMean,
			float horRange,
			float verRange,
			float horSd,
			float verSd
			)
	{
		this.horMean = horMean;
		this.verMean = verMean;
		this.horRange = horRange;
		this.verRange = verRange;
		this.horSd = horSd;
		this.verSd = verSd;
	}
	
	public void setClassifierAlgoOutput(String classifierAlgoOutput)
	{
		this.classifierAlgoOutput = classifierAlgoOutput;
	}

	public void setFinalClassifierOutput(String finalClassifierOutput)
	{
		this.finalClassifierOutput = finalClassifierOutput;
	}
	
	public synchronized  void insertTestValues()
	{
		dbAdapter.open();
		dbAdapter.insertValuesToTestAVTable(
				sampleTime,
				meanX, meanY, meanZ,
				horMean, verMean,
				horRange, verRange,
				horSd, verSd,
				classifierAlgoOutput,
				finalClassifierOutput);
		dbAdapter.close();
	}
	
	public synchronized void deleteTestValuesBefore(long time)
	{
		dbAdapter.open();
		dbAdapter.deleteTestAVTableBefore(time);
		dbAdapter.close();
	}
	
	public void updateFinalClassification(long sampleTime, String finalClassification)
	{
		dbAdapter.open();
		dbAdapter.updateFinalClassificationToTestAVTable(sampleTime, finalClassification);
		dbAdapter.close();
	}
}
