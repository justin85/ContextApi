
package activity.classifier.activity;



import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import activity.classifier.R;
import activity.classifier.activity.ChartHelper.ChartData;
import activity.classifier.common.ActivityNames;
import activity.classifier.common.Constants;
import activity.classifier.common.ExceptionHandler;
import activity.classifier.db.ActivitiesTable;
import activity.classifier.db.SqlLiteAdapter;
import activity.classifier.repository.ActivityQueries;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.rpc.Classification;
import activity.classifier.service.RecorderService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.flurry.android.FlurryAgent;

public class ActivityChartActivity extends Activity {


	/** Called when the activity is first created. */
	private ViewFlipper flipper;
	private ChartView chartview;
	private LinearLayout[] linearLayout = new LinearLayout[4];
	private TextView[] textView = new TextView[4];
	private int height,width;

	private SqlLiteAdapter sqlLiteAdapter;
	private ActivitiesTable activitiesTable;
	private ActivityRecorderBinder service = null;
	private final Handler handler = new Handler();
	private UpdateInterfaceRunnable updateInterfaceRunnable = new UpdateInterfaceRunnable();

	private ChartHelper chartHelper;
	private Map<String,Integer> activityIndexes;
	private Map<String,String> activityNiceNames;
	private ChartHelper.ChartData chartData;
	
	private final ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = ActivityRecorderBinder.Stub.asInterface(iBinder);
			updateInterfaceRunnable.updateNow();
		}

		public void onServiceDisconnected(ComponentName componentName) {
			service = null;
		}


	};

	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		getApplicationContext().unbindService(connection);
	}

	/**
	 * 
	 */
	protected void onResume() {
		super.onResume();
		updateInterfaceRunnable.start();
	}

	/**
	 * 
	 */
	protected void onPause() {
		super.onPause();
		updateInterfaceRunnable.stop();
	}

	/**
	 * 
	 */
	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "EMKSQFUWSCW51AKBL2JJ");
	}

	/**
	 * 
	 */
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
	
	private class UpdateInterfaceRunnable implements Runnable {

		//	avoids conflicts between scheduled updates,
		//		and once-off updates 
		private ReentrantLock reentrantLock = new ReentrantLock();
		
		//	last update time
		private long lastUpdateTime = 0;

		//	starts scheduled interface updates
		public void start() {
			Log.v(Constants.DEBUG_TAG, "UpdateInterfaceRunnable started");
			handler.postDelayed(updateInterfaceRunnable, 1);
		}

		//	stops scheduled interface updates
		public void stop() {
			Log.v(Constants.DEBUG_TAG, "UpdateInterfaceRunnable stopped");
			handler.removeCallbacks(updateInterfaceRunnable);
		}

		//	performs a once-off unsynchronised (unscheduled) interface update
		//		please note that this can be called from another thread
		//		without interfering with the normal scheduled updates.
		public void updateNow() {
			if (reentrantLock.tryLock()) {

				try {
					updateUI();
				} catch (ParseException ex) {
					Log.e(Constants.DEBUG_TAG, "Error while performing scheduled UI update.", ex);
				}

				reentrantLock.unlock();
			}

		}

		public void run() {
			if (reentrantLock.tryLock()) {
				
				try {
					updateUI();
				} catch (ParseException e) {
					e.printStackTrace();
				}

				reentrantLock.unlock();
			}
			handler.postDelayed(updateInterfaceRunnable, Constants.DELAY_UI_GRAPHIC_UPDATE);
		}

		private String getTimeText(long duration){
			Formatter fmt1 = new Formatter();
			Formatter fmt2 = new Formatter();
			String strDurationNew="";
			if(duration>=60){
				long sec = 0;
				sec = duration%60;
				duration = duration/60;
				if(duration>=60){
					long min = 0;
					min = duration%60;
					duration = duration/60;
					strDurationNew = fmt1.format("%1$3d %2$-4s %3$3d %4$-4s %5$3d %6$-4s",duration,"hours",min,"mins",sec,"secs").toString();
				}else{
					strDurationNew = fmt2.format("%1$3d %2$-4s %3$3d %4$-4s",duration,"mins",sec,"secs").toString();
				}

			}else{
				strDurationNew = " < 1 minute";
			}
			return strDurationNew;
		}

		/**
		 * 
		 * changed from updateButton to updateUI
		 * 
		 * updates the user interface:
		 * 	the toggle button's text is changed.
		 * 	the classification list's entries are updated.
		 * 
		 * @throws ParseException
		 */
		@SuppressWarnings("unchecked")
		private void updateUI() throws ParseException {
			
			//	please note:
			//		In the nexus s (not sure about other phones), there seems to be
			//		two events that occur about 5 seconds apart, even though
			//		its the same handler, and the sequence is started only once.
			//		You can use this code to check the stack trace.
			//
			//				try {
			//					throw new RuntimeException();
			//				} catch (Exception e) {
			//					Log.v(Constants.DEBUG_TAG, "Update Chart UI Exception", e);
			//				}
			//
			//		To avoid this, we make sure that the last call was at least
			//		the required interval ago.
			ChartData chartData = chartHelper.computeData();
			
			ActivityChartActivity.this.chartData = chartData;
			
			long currentTime = System.currentTimeMillis();
			if (currentTime-lastUpdateTime<Constants.DELAY_UI_GRAPHIC_UPDATE) {
				//	avoid refreshing before the required time
				return;
			} else {
				lastUpdateTime = currentTime;
			}
			
			Log.v(Constants.DEBUG_TAG, "Update Chart UI");
			
			Classification latest = new Classification();
			Classification beforeLatest = new Classification();
			
			if (!activitiesTable.loadLatest(latest)) {
				latest = null;
			}
			if (latest==null || !activitiesTable.loadLatestBefore(latest.getStart(), beforeLatest)) {
				beforeLatest = null;
			}
			
			String newText = "";
			String newDurationText = "";
			String beforeText = "";
			String beforeDurationText = "";
			
			if (latest!=null && !ActivityQueries.isSystemActivity(latest.getClassification())) {
				newText = latest.getNiceClassification();
				long period = latest.getEnd()-latest.getStart();
				period /= 1000;
				newDurationText = getTimeText(period);
				Log.v(Constants.DEBUG_TAG, "Latest "+newText+", start="+latest.getStart()+", end="+latest.getEnd()+", duration="+(latest.getEnd()-latest.getStart())+"='"+newDurationText+"'");
			}
			
			if (beforeLatest!=null && !ActivityQueries.isSystemActivity(beforeLatest.getClassification())) {
				beforeText = beforeLatest.getNiceClassification();
				long period = beforeLatest.getEnd()-beforeLatest.getStart();
				period /= 1000;
				beforeDurationText = getTimeText(period);
			}

			String newNiceText = String.format("%1$-10s", newText).toString();
			String beforeNiceText = String.format("%1$-10s", beforeText).toString();

			for(int i=0;i<textView.length;i++){
				if(i==0 || i==2){
					textView[i].setText(" Now    : " + newNiceText +" "+newDurationText );
				}else if(i==1 || i==3){
					textView[i].setText(" Before : " + beforeNiceText +" "+beforeDurationText);
				}
			}

			flipper.startFlipping();
			flipper.stopFlipping();
			
			chartview.postInvalidate();
		}

	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		chartHelper = new ChartHelper(this);
		activityIndexes = chartHelper.getActivityIndexes();
		activityNiceNames = chartHelper.getActivityNiceNames();
		
		flipper = new ViewFlipper(this);
		sqlLiteAdapter = SqlLiteAdapter.getInstance(this);
		activitiesTable = sqlLiteAdapter.getActivitiesTable();
		chartview = new ChartView(this);
		//		update = new updateTimeThread();
		//		update.start();
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		Intent intent = new Intent(this, RecorderService.class);
		if(!getApplicationContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)){
			throw new IllegalStateException("Binding to service failed " + intent);
		}
		Paint paint = new Paint();
		for(int i=0;i<linearLayout.length;i++){
			linearLayout[i] = new LinearLayout(this);
			linearLayout[i].setOrientation(LinearLayout.VERTICAL);
			if(i==0 || i==1){
				linearLayout[i].setMinimumHeight(height/7);
			}else if(i==2){
				linearLayout[i].setMinimumHeight(height-height/7);
			}
		}

		for(int i=0; i<textView.length;i++){
			textView[i] = new TextView(this);
			textView[i].setTextSize(17);
			if(i==0 || i==2){
				textView[i].setText(" Now    : ");
			}else if(i==1 || i==3){
				textView[i].setText(" Before : ");
			}
		}

		for(int i=0;i<2;i++){
			for(int j=0;j<2;j++){
				linearLayout[i].addView(textView[(2*i)+j], params);
			}
		}
		linearLayout[2].addView(chartview);

		flipper.addView(linearLayout[0]);
		flipper.addView(linearLayout[1]);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this,
				R.anim.push_up_out));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this,
				R.anim.push_up_in));

		linearLayout[3].addView(flipper);
		linearLayout[3].addView(linearLayout[2]);

		setContentView(linearLayout[3]);
	}


	private class ChartView extends View{
		public ChartView(Context context){
			super(context);

		}
		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			// custom drawing code here
			// remember: y increases from top to bottom
			// x increases from left to right
			int x = 0;
			int y = 0;
			//			DisplayMetrics displayMatrics = new DisplayMetrics();

			height = getHeight();
			width = getWidth();

			//			Log.i("saltfactory", "width : " + width +", height : " + height);
			Paint paint = new Paint();
			paint.setStyle(Paint.Style.FILL);

			//			Log.i("DB","onDraw");


			// make the entire canvas white
			paint.setColor(Color.WHITE);
			canvas.drawPaint(paint);
			// another way to do this is to use:
			// canvas.drawColor(Color.WHITE);


			paint.setARGB(255, 53, 57, 64);
			canvas.drawRect(new RectF(0,height-height/6,width,height), paint);

			paint.setColor(Color.WHITE);
			paint.setAntiAlias(true);
			paint.setTextSize(17);
			float[] sizeOfFooters = new float[Constants.FOOTER_SIZE];
			for(int i=0;i<Constants.FOOTER_SIZE;i++){
				sizeOfFooters[i] = paint.measureText(Constants.FOOTER_NAMES[i]);
				canvas.drawText(Constants.FOOTER_NAMES[i], i*width/3+((width/3)-sizeOfFooters[i])/2, height-(((height/6)-17)/2), paint);
			}
			
			String[] tempActivityNames = new String[activityIndexes.keySet().toArray().length];
			for(int i=0;i<activityIndexes.keySet().toArray().length;i++){
				tempActivityNames[i] = ""+activityIndexes.keySet().toArray()[i];
			}
			String[] activityNames = new String[tempActivityNames.length];
			float[][] sizeOfActivityNames = new float[chartData.numOfDurations][chartData.numOfActivities];
			
			
			for(int i=0;i<tempActivityNames.length;i++){
				activityNames[i] = activityNiceNames.get(tempActivityNames[i]);
			}
			for(int i=0;i<chartData.numOfDurations;i++){
			for(int j=0;j<chartData.numOfActivities;j++){
				Log.i("matrix",activityIndexes.keySet().toArray()[j]+"["+i+"]"+"["+j+"]"+chartData.percentageMatrix[i][j]+"");
			}
		}
			for(int i=0;i<chartData.numOfDurations;i++){
				float percentageStack = 0;
				for(int j=0;j<chartData.numOfActivities;j++){
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(Constants.COLOR_ACTIVITIES[j]);
					canvas.drawRect((new RectF(i*width/3,percentageStack,(i+1)*width/3,(percentageStack+(chartData.percentageMatrix[i][j]*(height-height/6)/100)))),paint);
					
					paint.setColor(Constants.COLOR_LINE);
					paint.setStrokeWidth((float) 1.5);
					paint.setStyle(Paint.Style.STROKE);
					canvas.drawRect((new RectF(i*width/3,percentageStack,(i+1)*width/3,(percentageStack+(chartData.percentageMatrix[i][j]*(height-height/6)/100)))),paint);
					
					
					String niceDisplayName = activityNames[j]+"("+(int)chartData.percentageMatrix[i][j]+"%)";
					sizeOfActivityNames[i][j] = paint.measureText(niceDisplayName);
					
					paint.setColor(Color.WHITE);
					paint.setStyle(Paint.Style.FILL);
					canvas.drawText(niceDisplayName, i*(width/3)+((width/3)-sizeOfActivityNames[i][j])/2, (((chartData.percentageMatrix[i][j]*(height-height/6)/100))<=27)?5000:percentageStack+27, paint);
					
					percentageStack += chartData.percentageMatrix[i][j]*(height-height/6)/100;
				}
			}
			
			paint.setColor(Constants.COLOR_LINE);
			paint.setStrokeWidth((float) 1.5);
			canvas.drawLine(0, 0, width, 0, paint);
			canvas.drawLine(width, 0, width, height, paint);
			canvas.drawLine(0, height-height/6, width, height-height/6, paint);
			paint.setStrokeWidth((float) 7.0);
			canvas.drawLine(0, 0, 0, height, paint);
			canvas.drawLine(0, height, width, height, paint);
			canvas.drawLine(width/3, 0, width/3, height, paint);
			canvas.drawLine(width-width/3, 0, width-width/3, height, paint);
			paint.setStrokeWidth((float) 1.5);
			
			

		}
	}

}
