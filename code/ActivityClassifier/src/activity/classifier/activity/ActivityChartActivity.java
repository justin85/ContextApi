
package activity.classifier.activity;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.concurrent.locks.ReentrantLock;

import activity.classifier.R;
import activity.classifier.common.Constants;
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
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
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
	private LinearLayout linearLayout1,linearLayout2,linearLayout3,linearLayout4 ;
	private TextView textView1, textView2, textView3, textView4;
	private int height,width;
	//	private String strNowTextNew,strBeforeText,strNowTextOld;
	//	String strDurationNew="";
	//	String strDurationBefore="";
	//	String strDurationOld="";
	//	private MySurfaceThread thread;
	final int CHARGING=0, UNCARRIED=1, WALKING=2, TRAVELLING=3, PADDLING=4, ACTIVE=5, UNKNOWN=6;

	long chargingDuration=0, uncarriedDuration=2, walkingDuration=3, travellingDuration=4, paddlingDuration=5, activeDuration=6, unknownDuration=7;
	private ArrayList<ArrayList<String[]>> activityGroup = new ArrayList<ArrayList<String[]>>();
	private Date fourHourTime;
	private Date hourTime;

	public ArrayList<Long> todayDuration = new ArrayList<Long>(); 
	public ArrayList<Long> fourHoursDuration = new ArrayList<Long>(); 
	public ArrayList<Long> hourDuration = new ArrayList<Long>(); 


	private ActivityQueries activityQuery;
	private ActivityRecorderBinder service = null;
	private final Handler handler = new Handler();
	private UpdateInterfaceRunnable updateInterfaceRunnable = new UpdateInterfaceRunnable();

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
		setTimeDurationClear();
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

	private void setTimeDuration(int ACTIVITY) throws ParseException{ 
		long tempDurationToay=0;
		long tempDurationHour=0;
		long tempDuration4Hours=0;

		ArrayList<Long> todayDuration = new ArrayList<Long>(); 
		ArrayList<Long> fourHoursDuration = new ArrayList<Long>(); 
		ArrayList<Long> hourDuration = new ArrayList<Long>(); 
		//		Log.i("duration",fourHourTime+"");
		//		Log.i("duration",hourTime+"");
		for(int j=0;j<activityGroup.get(ACTIVITY).size();j++){
			long startDate = Constants.DB_DATE_FORMAT.parse(activityGroup.get(ACTIVITY).get(j)[2]).getTime();
			long endDate = Constants.DB_DATE_FORMAT.parse(activityGroup.get(ACTIVITY).get(j)[3]).getTime();

			tempDurationToay+=((endDate-startDate)/1000);
			long fourHourAgo = fourHourTime.getTime();

			if((fourHourAgo<=endDate && fourHourAgo>=startDate) || startDate>=fourHourAgo){
				tempDuration4Hours+=((endDate-startDate)/1000);
				//				Log.i("duration",activityGroup.get(ACTIVITY).get(j)[1]+" "+activityGroup.get(ACTIVITY).get(j)[2]+" "+activityGroup.get(ACTIVITY).get(j)[3]+" : "+(endDate-startDate)/1000);
			}
			long hourAgo = hourTime.getTime();

			//			Log.i("duration",dateFormat.parse(activityGroup.get(ACTIVITY).get(j)[2])+" "+dateFormat.format(hourAgo));
			if((hourAgo<=endDate && hourAgo>=startDate) || startDate>=hourAgo){
				tempDurationHour+=((endDate-startDate)/1000);
			}
			Log.i("duration",activityGroup.get(ACTIVITY).get(j)[1]+tempDurationToay + " "+ tempDuration4Hours+" "+tempDurationHour+"");


		}
		setTimeDuration(tempDurationToay,tempDuration4Hours,tempDurationHour);


	}
	private void setTimeDurationClear(){
		todayDuration.clear();
		fourHoursDuration.clear();
		hourDuration.clear();
	}
	private void setTimeDuration(long tempDurationToay,long tempDuration4Hours,long tempDurationHour){

		todayDuration.add(tempDurationToay);
		fourHoursDuration.add(tempDuration4Hours);
		hourDuration.add(tempDurationHour);
	}
	private void updateTimeDuration() {
		int ACTIVITY=0;

		ArrayList<String[]> items = new ArrayList<String[]>();
		items = activityQuery.getTodayItemsFromActivityTable();
		activityGroup=activityQuery.getActivityGroup(items);
		fourHourTime = activityQuery.getFourHourBefore();
		hourTime = activityQuery.getHourBefore();


		for(ACTIVITY=0;ACTIVITY<7;ACTIVITY++){
			try {
				switch(ACTIVITY){

				case CHARGING:	
					setTimeDurationClear();
					setTimeDuration(CHARGING);
					break;
				case UNCARRIED:	
					setTimeDuration(UNCARRIED);
					break;
				case WALKING:	
					setTimeDuration(WALKING);
					break;
				case TRAVELLING:	
					setTimeDuration(TRAVELLING);
					break;
				case PADDLING:	
					setTimeDuration(PADDLING);
					break;
				case ACTIVE:	
					setTimeDuration(ACTIVE);
					break;
				case UNKNOWN:	
					setTimeDuration(UNKNOWN);
					break;
				}

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		Log.i("time","end");
	}


	private ArrayList<Float> activityProportion(ArrayList<Long> todayDuration){

		ArrayList<Float> proportion = new ArrayList<Float>();
		double sum=0;
		for(int i=0;i<7;i++){
			sum+=todayDuration.get(i);

		}
		for(int i=0;i<7;i++){
			proportion.add((float) ((float)(height-height/6)*(todayDuration.get(i)/sum)));
		}

		return proportion;

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
			
			long currentTime = System.currentTimeMillis();
			if (currentTime-lastUpdateTime<Constants.DELAY_UI_GRAPHIC_UPDATE) {
				//	avoid refreshing before the required time
				return;
			} else {
				lastUpdateTime = currentTime;
			}
			
			Log.v(Constants.DEBUG_TAG, "Update Chart UI");
			
			
			int activitySize = activityQuery.getSizeOfTable();
			Log.i("time",activitySize+"");

			//	update list either if service state has changed, or it's still running
			Log.i("time","start");
			updateTimeDuration();
			//					Log.i("duration"," "+todayDuration.get(0));						
			//					update.setRunningState(true);
			String newActivityStartDate = activityQuery.getItemStartDateFromActivityTable(activitySize);
			String newActivityEndDate = activityQuery.getItemEndDateFromActivityTable(activitySize);
			long newDuration = (Constants.DB_DATE_FORMAT.parse(newActivityEndDate).getTime()-Constants.DB_DATE_FORMAT.parse(newActivityStartDate).getTime())/1000;
			String beforeActivityStartDate = activityQuery.getItemStartDateFromActivityTable(activitySize-1);
			String beforeActivityEndDate = activityQuery.getItemEndDateFromActivityTable(activitySize-1);
			long beforeDuration = (Constants.DB_DATE_FORMAT.parse(beforeActivityEndDate).getTime()-Constants.DB_DATE_FORMAT.parse(beforeActivityStartDate).getTime())/1000;

			String newActivityName = activityQuery.getItemNameFromActivityTable(activitySize);
			String beforeActivityName = activityQuery.getItemNameFromActivityTable(activitySize-1);
			
			String newDurationText = "";
			String beforeDurationText = "";
			
			String newText = "";
			String beforeText = "";
			
			if (!ActivityQueries.isSystemActivity(newActivityName)) {
				newText = Classification.getNiceName(ActivityChartActivity.this, newActivityName);
				newDurationText = getTimeText(newDuration);
			}
			
			if (!ActivityQueries.isSystemActivity(beforeActivityName)) {
				beforeText = Classification.getNiceName(ActivityChartActivity.this, beforeActivityName);
				beforeDurationText = getTimeText(beforeDuration);
			}

			String newNiceText = String.format("%1$-10s", newText).toString();
			String beforeNiceText = String.format("%1$-10s", beforeText).toString();

			textView1.setText(" Now    : " + newNiceText +" "+newDurationText );
			textView2.setText(" Before : " + beforeNiceText +" "+beforeDurationText);
			textView3.setText(" Now    : " + newNiceText +" "+newDurationText);
			textView4.setText(" Before : " + beforeNiceText +" "+beforeDurationText);

			flipper.startFlipping();
			flipper.stopFlipping();
			
			chartview.postInvalidate();
		}

	}

	//	updateTimeThread update;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		flipper = new ViewFlipper(this);
		activityQuery = new ActivityQueries(this);
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
		linearLayout1 = new LinearLayout(this);
		linearLayout2 = new LinearLayout(this);
		linearLayout3 = new LinearLayout(this);
		linearLayout4 = new LinearLayout(this);

		linearLayout1.setOrientation(LinearLayout.VERTICAL);
		linearLayout2.setOrientation(LinearLayout.VERTICAL);
		linearLayout3.setOrientation(LinearLayout.VERTICAL);
		linearLayout4.setOrientation(LinearLayout.VERTICAL);

		linearLayout1.setMinimumHeight(height/7);
		linearLayout2.setMinimumHeight(height/7);
		linearLayout3.setMinimumHeight(height-height/7);


		//		strNowText1 = (service!=null && service.isRunning()) ? activityQuery.getItemNameFromActivityTable(activitySize) : "";

		//		strNowTextNew = "";
		//		strBeforeText = "";
		//		strNowTextOld = "";

		textView1 = new TextView(this);
		textView2 = new TextView(this);
		textView3 = new TextView(this);
		textView4 = new TextView(this);

		textView1.setText(" Now    : " );
		textView2.setText(" Before : ");
		textView3.setText(" Now    : ");
		textView4.setText(" Before : ");

		textView1.setTextSize(17);
		textView2.setTextSize(17);
		textView3.setTextSize(17);
		textView4.setTextSize(17);

		linearLayout1.addView(textView1, params);
		linearLayout1.addView(textView2, params);

		linearLayout2.addView(textView3, params);
		linearLayout2.addView(textView4, params);

		linearLayout3.addView(chartview);

		flipper.addView(linearLayout1);
		flipper.addView(linearLayout2);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this,
				R.anim.push_up_out));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this,
				R.anim.push_up_in));



		linearLayout4.addView(flipper);
		linearLayout4.addView(linearLayout3);

//		linearLayout3.setOnTouchListener(new View.OnTouchListener() {
//
//			public boolean onTouch(View v, MotionEvent event) {
//				float x = event.getX();
//				float y = event.getY();
//				switch(event.getAction()){
//				case MotionEvent.ACTION_DOWN:
//					
//					if((x>=cordinatesForCharging[0][0] && x<=cordinatesForCharging[2][0]) &&
//						(y>=cordinatesForCharging[1][0] && y<=cordinatesForCharging[3][0])	){
//						Log.i("touch","Today Charging"+x+" "+y);
//					}else if((x>=cordinatesForCharging[0][1] && x<=cordinatesForCharging[2][1]) &&
//							(y>=cordinatesForCharging[1][1] && y<=cordinatesForCharging[3][1])	){
//						Log.i("touch","4Hour Charging"+x+" "+y);
//					}else if((x>=cordinatesForCharging[0][2] && x<=cordinatesForCharging[2][2]) &&
//							(y>=cordinatesForCharging[1][2] && y<=cordinatesForCharging[3][2])	){
//						Log.i("touch","Hour Charging"+x+" "+y);
//					}else if((x>=cordinatesForUncarried[0][0] && x<=cordinatesForUncarried[2][0]) &&
//							(y>=cordinatesForUncarried[1][0] && y<=cordinatesForUncarried[3][0])	){
////						 Intent intent = new Intent(getBaseContext(), Sensors.class);
////						 intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						 Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
////			             vibrator.vibrate(100);
////						 getBaseContext().startActivity(intent); 
//
//						Log.i("touch","Today Uncarried"+x+" "+y);
//					}else if((x>=cordinatesForUncarried[0][1] && x<=cordinatesForUncarried[2][1]) &&
//							(y>=cordinatesForUncarried[1][1] && y<=cordinatesForUncarried[3][1])	){
//						Log.i("touch","4Hour Uncarried"+x+" "+y);
//					}else if((x>=cordinatesForUncarried[0][2] && x<=cordinatesForUncarried[2][2]) &&
//							(y>=cordinatesForUncarried[1][2] && y<=cordinatesForUncarried[3][2])	){
//						Log.i("touch","Hour Uncarried"+x+" "+y);
//					}else if((x>=cordinatesForWalking[0][0] && x<=cordinatesForWalking[2][0]) &&
//							(y>=cordinatesForWalking[1][0] && y<=cordinatesForWalking[3][0])	){
//						Log.i("touch","Today Walking"+x+" "+y);
//					}else if((x>=cordinatesForWalking[0][1] && x<=cordinatesForWalking[2][1]) &&
//							(y>=cordinatesForWalking[1][1] && y<=cordinatesForWalking[3][1])	){
//						Log.i("touch","4Hour Walking"+x+" "+y);
//					}else if((x>=cordinatesForWalking[0][2] && x<=cordinatesForWalking[2][2]) &&
//							(y>=cordinatesForWalking[1][2] && y<=cordinatesForWalking[3][2])	){
//						Log.i("touch","Hour Walking"+x+" "+y);
//					}else if((x>=cordinatesForTravelling[0][0] && x<=cordinatesForTravelling[2][0]) &&
//							(y>=cordinatesForTravelling[1][0] && y<=cordinatesForTravelling[3][0])	){
//						Log.i("touch","Today Travelling"+x+" "+y);
//					}else if((x>=cordinatesForTravelling[0][1] && x<=cordinatesForTravelling[2][1]) &&
//							(y>=cordinatesForTravelling[1][1] && y<=cordinatesForTravelling[3][1])	){
//						Log.i("touch","4Hour Travelling"+x+" "+y);
//					}else if((x>=cordinatesForTravelling[0][2] && x<=cordinatesForTravelling[2][2]) &&
//							(y>=cordinatesForTravelling[1][2] && y<=cordinatesForTravelling[3][2])	){
//						Log.i("touch","Hour Travelling"+x+" "+y);
//					}else if((x>=cordinatesForPaddling[0][0] && x<=cordinatesForPaddling[2][0]) &&
//							(y>=cordinatesForPaddling[1][0] && y<=cordinatesForPaddling[3][0])	){
//						Log.i("touch","Today Paddling"+x+" "+y);
//					}else if((x>=cordinatesForPaddling[0][1] && x<=cordinatesForPaddling[2][1]) &&
//							(y>=cordinatesForPaddling[1][1] && y<=cordinatesForPaddling[3][1])	){
//						Log.i("touch","4Hour Paddling"+x+" "+y);
//					}else if((x>=cordinatesForPaddling[0][2] && x<=cordinatesForPaddling[2][2]) &&
//							(y>=cordinatesForPaddling[1][2] && y<=cordinatesForPaddling[3][2])	){
//						Log.i("touch","Hour Paddling"+x+" "+y);
//					}else if((x>=cordinatesForActive[0][0] && x<=cordinatesForActive[2][0]) &&
//							(y>=cordinatesForActive[1][0] && y<=cordinatesForActive[3][0])	){
//						Log.i("touch","Today Active"+x+" "+y);
//					}else if((x>=cordinatesForActive[0][1] && x<=cordinatesForActive[2][1]) &&
//							(y>=cordinatesForActive[1][1] && y<=cordinatesForActive[3][1])	){
//						Log.i("touch","4Hour Active"+x+" "+y);
//					}else if((x>=cordinatesForActive[0][2] && x<=cordinatesForActive[2][2]) &&
//							(y>=cordinatesForActive[1][2] && y<=cordinatesForActive[3][2])	){
//						Log.i("touch","Hour Active"+x+" "+y);
//					}else if((x>=cordinatesForUnknown[0][0] && x<=cordinatesForUnknown[2][0]) &&
//							(y>=cordinatesForUnknown[1][0] && y<=cordinatesForUnknown[3][0])	){
//						Log.i("touch","Today Unknown"+x+" "+y);
//					}else if((x>=cordinatesForUnknown[0][1] && x<=cordinatesForUnknown[2][1]) &&
//							(y>=cordinatesForUnknown[1][1] && y<=cordinatesForUnknown[3][1])	){
//						Log.i("touch","4Hour Unknown"+x+" "+y);
//					}else if((x>=cordinatesForUnknown[0][2] && x<=cordinatesForUnknown[2][2]) &&
//							(y>=cordinatesForUnknown[1][2] && y<=cordinatesForUnknown[3][2])	){
//						Log.i("touch","Hour Unknown"+x+" "+y);
//					}
//					break;
//				}
//				
//				return true;
//			}
//		});

		setContentView(linearLayout4);
	}

	private ArrayList<float[][]> region = new ArrayList<float[][]>();
	private float[][] cordinatesForCharging = new float[4][3];
	private float[][] cordinatesForUncarried = new float[4][3];
	private float[][] cordinatesForWalking = new float[4][3];
	private float[][] cordinatesForTravelling = new float[4][3];
	private float[][] cordinatesForPaddling = new float[4][3];
	private float[][] cordinatesForActive = new float[4][3];
	private float[][] cordinatesForUnknown = new float[4][3];

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
			float textWidth1 = paint.measureText("Today");
			float textWidth2 = paint.measureText("Last 4");
			float textWidth3 = paint.measureText("Last");
			float textWidth4 = paint.measureText("Hours");
			float textWidth5 = paint.measureText("Hour");




			canvas.drawText("Today", ((width/3)-textWidth1)/2, height-(((height/6)-17)/2), paint);
			canvas.drawText("Last 4", (width/3+((width/3)-textWidth2)/2), ((height-(((height/6)-34)/2))-17), paint);
			canvas.drawText("Hours", width/3+((width/3)-textWidth4)/2, height-(((height/6)-34)/2), paint);
			canvas.drawText("Last", 2*width/3+((width/3)-textWidth3)/2, (height-(((height/6)-34)/2))-20, paint);
			canvas.drawText("Hour", 2*width/3+((width/3)-textWidth5)/2, height-(((height/6)-34)/2), paint);

			paint.setARGB(255, 32, 33, 38);
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
			if(!todayDuration.isEmpty()){
				//				Log.i("durationempty","not empty");
				ArrayList<Float> activityProportionToday = new ArrayList<Float>();
				ArrayList<Float> activityProportion4Hours = new ArrayList<Float>();
				ArrayList<Float> activityProportionHour = new ArrayList<Float>();
				ArrayList<Float> activityProportionTodayStack = new ArrayList<Float>();
				ArrayList<Float> activityProportion4HoursStack = new ArrayList<Float>();
				ArrayList<Float> activityProportionHourStack = new ArrayList<Float>();
				activityProportionToday = activityProportion(todayDuration);
				activityProportion4Hours = activityProportion(fourHoursDuration);
				activityProportionHour = activityProportion(hourDuration);
				for(int i=0;i<7;i++){
					if(i==0){
						activityProportionTodayStack.add(0+activityProportionToday.get(i));
						activityProportion4HoursStack.add(0+activityProportion4Hours.get(i));
						activityProportionHourStack.add(0+activityProportionHour.get(i));
					}else{
						activityProportionTodayStack.add(activityProportionTodayStack.get(i-1)+activityProportionToday.get(i));
						activityProportion4HoursStack.add(activityProportion4HoursStack.get(i-1)+activityProportion4Hours.get(i));
						activityProportionHourStack.add(activityProportionHourStack.get(i-1)+activityProportionHour.get(i));
					}
				}

				paint.setARGB(255, 114, 141, 108);
				canvas.drawRect((new RectF(0,0,width/3,activityProportionTodayStack.get(0))),paint);
				cordinatesForCharging[0][0] = 0;
				cordinatesForCharging[1][0] = 0;
				cordinatesForCharging[2][0] = width/3;
				cordinatesForCharging[3][0] = activityProportionTodayStack.get(0);
				paint.setARGB(255, 255, 97, 78);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(0),width/3,activityProportionTodayStack.get(1))),paint);
				cordinatesForUncarried[0][0] = 0;
				cordinatesForUncarried[1][0] = activityProportionTodayStack.get(0);
				cordinatesForUncarried[2][0] = width/3;
				cordinatesForUncarried[3][0] = activityProportionTodayStack.get(1);
				paint.setARGB(255, 109, 206, 250);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(1),width/3,activityProportionTodayStack.get(2))),paint);
				cordinatesForWalking[0][0] = 0;
				cordinatesForWalking[1][0] = activityProportionTodayStack.get(1);
				cordinatesForWalking[2][0] = width/3;
				cordinatesForWalking[3][0] = activityProportionTodayStack.get(2);
				paint.setARGB(255, 244, 141, 62);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(2),width/3,activityProportionTodayStack.get(3))),paint);
				cordinatesForTravelling[0][0] = 0;
				cordinatesForTravelling[1][0] = activityProportionTodayStack.get(2);
				cordinatesForTravelling[2][0] = width/3;
				cordinatesForTravelling[3][0] = activityProportionTodayStack.get(3);
				paint.setARGB(255, 237, 142, 107);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(3),width/3,activityProportionTodayStack.get(4))),paint);
				cordinatesForPaddling[0][0] = 0;
				cordinatesForPaddling[1][0] = activityProportionTodayStack.get(3);
				cordinatesForPaddling[2][0] = width/3;
				cordinatesForPaddling[3][0] = activityProportionTodayStack.get(4);
				paint.setARGB(255, 181, 40, 65);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(4),width/3,activityProportionTodayStack.get(5))),paint);
				cordinatesForActive[0][0] = 0;
				cordinatesForActive[1][0] = activityProportionTodayStack.get(4);
				cordinatesForActive[2][0] = width/3;
				cordinatesForActive[3][0] = activityProportionTodayStack.get(5);
				paint.setARGB(255, 181, 204, 122);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(5),width/3,activityProportionTodayStack.get(6))),paint);
				cordinatesForUnknown[0][0] = 0;
				cordinatesForUnknown[1][0] = activityProportionTodayStack.get(5);
				cordinatesForUnknown[2][0] = width/3;
				cordinatesForUnknown[3][0] = activityProportionTodayStack.get(6);
				
				paint.setARGB(255, 114, 141, 108);
				canvas.drawRect((new RectF(width/3,0,width-width/3,activityProportion4HoursStack.get(0))),paint);
				cordinatesForCharging[0][1] = width/3;
				cordinatesForCharging[1][1] = 0;
				cordinatesForCharging[2][1] = width-width/3;
				cordinatesForCharging[3][1] = activityProportion4HoursStack.get(0);
				paint.setARGB(255, 255, 97, 78);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(0),width-width/3,activityProportion4HoursStack.get(1))),paint);
				cordinatesForUncarried[0][1] = width/3;
				cordinatesForUncarried[1][1] = activityProportion4HoursStack.get(0);
				cordinatesForUncarried[2][1] = width-width/3;
				cordinatesForUncarried[3][1] = activityProportion4HoursStack.get(1);
				paint.setARGB(255, 109, 206, 250);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(1),width-width/3,activityProportion4HoursStack.get(2))),paint);
				cordinatesForWalking[0][1] = width/3;
				cordinatesForWalking[1][1] = activityProportion4HoursStack.get(1);
				cordinatesForWalking[2][1] = width-width/3;
				cordinatesForWalking[3][1] = activityProportion4HoursStack.get(2);
				paint.setARGB(255, 244, 141, 62);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(2),width-width/3,activityProportion4HoursStack.get(3))),paint);
				cordinatesForTravelling[0][1] = width/3;
				cordinatesForTravelling[1][1] = activityProportion4HoursStack.get(2);
				cordinatesForTravelling[2][1] = width-width/3;
				cordinatesForTravelling[3][1] = activityProportion4HoursStack.get(3);
				paint.setARGB(255, 237, 142, 107);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(3),width-width/3,activityProportion4HoursStack.get(4))),paint);
				cordinatesForPaddling[0][1] = width/3;
				cordinatesForPaddling[1][1] = activityProportion4HoursStack.get(3);
				cordinatesForPaddling[2][1] = width-width/3;
				cordinatesForPaddling[3][1] = activityProportion4HoursStack.get(4);
				paint.setARGB(255, 181, 40, 65);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(4),width-width/3,activityProportion4HoursStack.get(5))),paint);
				cordinatesForActive[0][1] = width/3;
				cordinatesForActive[1][1] = activityProportion4HoursStack.get(4);
				cordinatesForActive[2][1] = width-width/3;
				cordinatesForActive[3][1] = activityProportion4HoursStack.get(5);
				paint.setARGB(255, 181, 204, 122);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(5),width-width/3,activityProportion4HoursStack.get(6))),paint);
				cordinatesForUnknown[0][1] = width/3;
				cordinatesForUnknown[1][1] = activityProportion4HoursStack.get(5);
				cordinatesForUnknown[2][1] = width-width/3;
				cordinatesForUnknown[3][1] = activityProportion4HoursStack.get(6);
				
				paint.setARGB(255, 114, 141, 108);
				canvas.drawRect((new RectF(width-width/3,0,width,activityProportionHourStack.get(0))),paint);
				cordinatesForCharging[0][2] = width-width/3;
				cordinatesForCharging[1][2] = 0;
				cordinatesForCharging[2][2] = width;
				cordinatesForCharging[3][2] = activityProportionHourStack.get(0);
				paint.setARGB(255, 255, 97, 78);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(0),width,activityProportionHourStack.get(1))),paint);
				cordinatesForUncarried[0][2] = width-width/3;
				cordinatesForUncarried[1][2] = activityProportionHourStack.get(0);
				cordinatesForUncarried[2][2] = width;
				cordinatesForUncarried[3][2] = activityProportionHourStack.get(1);
				paint.setARGB(255, 109, 206, 250);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(1),width,activityProportionHourStack.get(2))),paint);
				cordinatesForWalking[0][2] = width-width/3;
				cordinatesForWalking[1][2] = activityProportionHourStack.get(1);
				cordinatesForWalking[2][2] = width;
				cordinatesForWalking[3][2] = activityProportionHourStack.get(2);
				paint.setARGB(255, 244, 141, 62);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(2),width,activityProportionHourStack.get(3))),paint);
				cordinatesForTravelling[0][2] = width-width/3;
				cordinatesForTravelling[1][2] = activityProportionHourStack.get(2);
				cordinatesForTravelling[2][2] = width;
				cordinatesForTravelling[3][2] = activityProportionHourStack.get(3);
				paint.setARGB(255, 237, 142, 107);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(3),width,activityProportionHourStack.get(4))),paint);
				cordinatesForPaddling[0][2] = width-width/3;
				cordinatesForPaddling[1][2] = activityProportionHourStack.get(3);
				cordinatesForPaddling[2][2] = width;
				cordinatesForPaddling[3][2] = activityProportionHourStack.get(4);
				paint.setARGB(255, 181, 40, 65);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(4),width,activityProportionHourStack.get(5))),paint);
				cordinatesForActive[0][2] = width-width/3;
				cordinatesForActive[1][2] = activityProportionHourStack.get(4);
				cordinatesForActive[2][2] = width;
				cordinatesForActive[3][2] = activityProportionHourStack.get(5);
				paint.setARGB(255, 181, 204, 122);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(5),width,activityProportionHourStack.get(6))),paint);
				cordinatesForUnknown[0][2] = width-width/3;
				cordinatesForUnknown[1][2] = activityProportionHourStack.get(5);
				cordinatesForUnknown[2][2] = width;
				cordinatesForUnknown[3][2] = activityProportionHourStack.get(6);
				

				paint.setColor(Color.WHITE);
				paint.setAntiAlias(true);
				paint.setTextSize(17);
				float chargingWidth = paint.measureText("Charging");
				float uncarriedWidth = paint.measureText("Uncarried");
				float walkingWidth = paint.measureText("Walking");
				float travellingWidth = paint.measureText("Travelling");
				float paddlingWidth = paint.measureText("Paddling");
				float activeWidth = paint.measureText("Active");
				float unknownWidth = paint.measureText("Unknown");



				canvas.drawText("Charging", ((width/3)-chargingWidth)/2, (activityProportionToday.get(0)<=27)?5000:27, paint);
				canvas.drawText("Uncarried", ((width/3)-uncarriedWidth)/2, (activityProportionToday.get(1)<=27)?5000:27+activityProportionTodayStack.get(0), paint);
				canvas.drawText("Walking", ((width/3)-walkingWidth)/2, (activityProportionToday.get(2)<=27)?5000:27+activityProportionTodayStack.get(1), paint);
				canvas.drawText("Travelling", ((width/3)-travellingWidth)/2, (activityProportionToday.get(3)<=27)?5000:27+activityProportionTodayStack.get(2), paint);
				canvas.drawText("Paddling", ((width/3)-paddlingWidth)/2, (activityProportionToday.get(4)<=27)?5000:27+activityProportionTodayStack.get(3), paint);
				canvas.drawText("Active", ((width/3)-activeWidth)/2, (activityProportionToday.get(5)<=27)?5000:27+activityProportionTodayStack.get(4), paint);
				canvas.drawText("Unknown", ((width/3)-unknownWidth)/2, (activityProportionToday.get(6)<=27)?5000:27+activityProportionTodayStack.get(5), paint);

				canvas.drawText("Charging", width/3+((width/3)-chargingWidth)/2, (activityProportion4Hours.get(0)<=27)?5000:27, paint);
				canvas.drawText("Uncarried", width/3+((width/3)-uncarriedWidth)/2, (activityProportion4Hours.get(1)<=27)?5000:27+activityProportion4HoursStack.get(0), paint);
				canvas.drawText("Walking", width/3+((width/3)-walkingWidth)/2, (activityProportion4Hours.get(2)<=27)?5000:27+activityProportion4HoursStack.get(1), paint);
				canvas.drawText("Travelling", width/3+((width/3)-travellingWidth)/2, (activityProportion4Hours.get(3)<=27)?5000:27+activityProportion4HoursStack.get(2), paint);
				canvas.drawText("Paddling", width/3+((width/3)-paddlingWidth)/2, (activityProportion4Hours.get(4)<=27)?5000:27+activityProportion4HoursStack.get(3), paint);
				canvas.drawText("Active", width/3+((width/3)-activeWidth)/2, (activityProportion4Hours.get(5)<=27)?5000:27+activityProportion4HoursStack.get(4), paint);
				canvas.drawText("Unknown", width/3+((width/3)-unknownWidth)/2, (activityProportion4Hours.get(6)<=27)?5000:27+activityProportion4HoursStack.get(5), paint);

				canvas.drawText("Charging", 2*width/3+((width/3)-chargingWidth)/2, (activityProportionHour.get(0)<=27)?5000:27, paint);
				canvas.drawText("Uncarried", 2*width/3+((width/3)-uncarriedWidth)/2, (activityProportionHour.get(1)<=27)?5000:27+activityProportionHourStack.get(0), paint);
				canvas.drawText("Walking", 2*width/3+((width/3)-walkingWidth)/2, (activityProportionHour.get(2)<=27)?5000:27+activityProportionHourStack.get(1), paint);
				canvas.drawText("Travelling", 2*width/3+((width/3)-travellingWidth)/2, (activityProportionHour.get(3)<=27)?5000:27+activityProportionHourStack.get(2), paint);
				canvas.drawText("Paddling", 2*width/3+((width/3)-paddlingWidth)/2, (activityProportionHour.get(4)<=27)?5000:27+activityProportionHourStack.get(3), paint);
				canvas.drawText("Active", 2*width/3+((width/3)-activeWidth)/2, (activityProportionHour.get(5)<=27)?5000:27+activityProportionHourStack.get(4), paint);
				canvas.drawText("Unknown", 2*width/3+((width/3)-unknownWidth)/2, (activityProportionHour.get(6)<=27)?5000:27+activityProportionHourStack.get(5), paint);

				paint.setARGB(255, 32, 33, 38);
				paint.setStrokeWidth((float) 1.5);
				paint.setStyle(Paint.Style.STROKE);
				canvas.drawRect((new RectF(0,0,width/3,activityProportionTodayStack.get(0))),paint);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(0),width/3,activityProportionTodayStack.get(1))),paint);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(1),width/3,activityProportionTodayStack.get(2))),paint);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(2),width/3,activityProportionTodayStack.get(3))),paint);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(3),width/3,activityProportionTodayStack.get(4))),paint);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(4),width/3,activityProportionTodayStack.get(5))),paint);
				canvas.drawRect((new RectF(0,activityProportionTodayStack.get(5),width/3,activityProportionTodayStack.get(6))),paint);

				canvas.drawRect((new RectF(width/3,0,width-width/3,activityProportion4HoursStack.get(0))),paint);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(0),width-width/3,activityProportion4HoursStack.get(1))),paint);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(1),width-width/3,activityProportion4HoursStack.get(2))),paint);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(2),width-width/3,activityProportion4HoursStack.get(3))),paint);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(3),width-width/3,activityProportion4HoursStack.get(4))),paint);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(4),width-width/3,activityProportion4HoursStack.get(5))),paint);
				canvas.drawRect((new RectF(width/3,activityProportion4HoursStack.get(5),width-width/3,activityProportion4HoursStack.get(6))),paint);

				canvas.drawRect((new RectF(width-width/3,0,width,activityProportionHourStack.get(0))),paint);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(0),width,activityProportionHourStack.get(1))),paint);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(1),width,activityProportionHourStack.get(2))),paint);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(2),width,activityProportionHourStack.get(3))),paint);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(3),width,activityProportionHourStack.get(4))),paint);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(4),width,activityProportionHourStack.get(5))),paint);
				canvas.drawRect((new RectF(width-width/3,activityProportionHourStack.get(5),width,activityProportionHourStack.get(6))),paint);


				paint.setStrokeWidth((float) 7.0);
				canvas.drawLine(0, 0, 0, height, paint);
				canvas.drawLine(0, height, width, height, paint);
				canvas.drawLine(width/3, 0, width/3, height, paint);
				canvas.drawLine(width-width/3, 0, width-width/3, height, paint);
			}else{
				//				Log.i("durationempty","empty");
				paint.setColor(Color.WHITE);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawRect(new RectF(0,0,width,height-height/6), paint);


				paint.setColor(Color.BLACK);
				paint.setAntiAlias(true);
				paint.setTextSize(30);

				float text1 = paint.measureText("If you already started the service,");
				float text2 = paint.measureText("please wait for a second.");
				paint.setARGB(255, 48, 54, 35);
				canvas.drawText("If you already started the service,", ((width)-text1)/2, (((height-height/6)-30)/2), paint);
				paint.setARGB(255, 69, 76, 60);
				canvas.drawText("please wait for a second.", ((width)-text2)/2, (((height-height/6)-30)/2)+32, paint);
			}

		}
	}

}
