/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */

package activity.classifier.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import activity.classifier.R;
import activity.classifier.common.Constants;
import activity.classifier.common.ExceptionHandler;
import activity.classifier.repository.ActivityQueries;
import activity.classifier.repository.OptionQueries;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.rpc.Classification;
import activity.classifier.service.RecorderService;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;

/**
 * 
 * @author chris, modified Justin Lee
 * 
 */
public class ActivityListActivity extends Activity {

	public static boolean serviceIsRunning = false;

	private final Handler handler = new Handler();

	private ActivityRecorderBinder service = null;

	private ProgressDialog dialog;

	/**
	 * enable to delete database in the device repository.
	 */
	private boolean EnableDeletion;

	private OptionQueries optionQuery;
	private ActivityQueries activityQuery;
	/**
	 * Updates the user interface.
	 */
	private final UpdateInterfaceRunnable updateInterfaceRunnable = new UpdateInterfaceRunnable();

	/**
	 *	Performs necessary tasks when the connection to the service
	 *	is established, and after it is disconnected.
	 */

	private final ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = ActivityRecorderBinder.Stub.asInterface(iBinder);
			updateInterfaceRunnable.updateNow();
		}

		public void onServiceDisconnected(ComponentName componentName) {
			service = null;

			Log.i(Constants.DEBUG_TAG, "Service Disconnected");
		}


	};

	/**
	 * 
	 * @param intent
	 * @return null
	 */
	public IBinder onBind(Intent intent) {
		return null;
	}



	/**
	 * 
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		//set exception handler
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

		EnableDeletion = false;
		optionQuery = new OptionQueries(this);
		activityQuery = new ActivityQueries(this);
		setContentView(R.layout.main);

		((ListView) findViewById(R.id.list)).setAdapter(new ArrayAdapter<Classification>(	this,
				R.layout.item));
		Intent intent = new Intent(this, RecorderService.class);
		if(!getApplicationContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)){
			throw new IllegalStateException("Binding to service failed " + intent);
		}
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
		// wl.release();
		FlurryAgent.onEndSession(this);
	}
	
	/**
	 * Performs scheduled user interface updates, also allows
	 * other components to request the user interface to be updated,
	 * without interfering with normal scheduled updates.
	 * @author Umran
	 *
	 */
	private class UpdateInterfaceRunnable implements Runnable {

		//	save the state of the service, if it was previously running or not
		//		to avoid unnecessary updates
		private boolean prevServiceRunning = false;

		//	avoids conflicts between scheduled updates,
		//		and once-off updates 
		private ReentrantLock reentrantLock = new ReentrantLock();

		//	starts scheduled interface updates
		public void start() {
			handler.postDelayed(this, Constants.DELAY_UI_UPDATE);
		}

		//	stops scheduled interface updates
		public void stop() {
			handler.removeCallbacks(this);
		}

		//	performs a once-off unsynchronised (unscheduled) interface update
		//		please note that this can be called from another thread
		//		without interfering with the normal scheduled updates.
		public void updateNow() {
			if (reentrantLock.tryLock()) {

				try {
					if(service!=null && service.isRunning()){
						updateUI();
					}
				} catch (ParseException ex) {
					Log.e(Constants.DEBUG_TAG, "Error while performing scheduled UI update.", ex);
				} catch (RemoteException ex) {
					Log.e(Constants.DEBUG_TAG, "Error while performing scheduled UI update.", ex);
				}

				reentrantLock.unlock();
			}
		}

		public void run() {
			try {
				if (reentrantLock.tryLock()) {
					try {
						if(service!=null && service.isRunning()){
							updateUI();
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
					reentrantLock.unlock();
				}
				
				handler.postDelayed(this, Constants.DELAY_UI_GRAPHIC_UPDATE);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
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
			try {
				boolean isServiceRunning = service!=null && service.isRunning();

				int count = activityQuery.getSizeOfTable();
				
				if(count>0){
					//	update list either if service state has changed, or it's still running
					if (isServiceRunning!=prevServiceRunning || isServiceRunning) {

						final ArrayAdapter<Classification> adapter = (ArrayAdapter<Classification>) ((ListView) findViewById(R.id.list)).getAdapter();

						ArrayList<String[]> items = new ArrayList<String[]>();
						items = activityQuery.getTodayItemsFromActivityTable();
						ArrayList<Classification> classification = new ArrayList<Classification>();
						for(int i=0;i<items.size();i++){
							classification.add(new Classification(
									items.get(i)[1],
									Constants.DB_DATE_FORMAT.parse(items.get(i)[2]).getTime(),
									Constants.DB_DATE_FORMAT.parse(items.get(i)[3]).getTime()));
						}

						for (int i = 1; i <= classification.size(); i++) {
							Classification c = classification.get(classification.size()-i);
							if (c!=null)
								c = c.withContext(ActivityListActivity.this);
							if (c!=null)
								adapter.add(c);
						}


						String lastActivity = activityQuery.getItemNameFromActivityTable(count);
						String lastActivityStartDate = activityQuery.getItemStartDateFromActivityTable(count);
						String lastActivityEndDate = activityQuery.getItemEndDateFromActivityTable(count);
						Classification topClassification = null;
						if (!adapter.isEmpty()) {
							topClassification = adapter.getItem(0); 
						}

						if(lastActivity!=null && !topClassification.getClassification().equals(lastActivity)) {
							Classification lastActivityClass = new Classification(
									lastActivity,
									Constants.DB_DATE_FORMAT.parse(lastActivityStartDate).getTime(),
									Constants.DB_DATE_FORMAT.parse(lastActivityEndDate).getTime());
							lastActivityClass = lastActivityClass.withContext(ActivityListActivity.this);
							Log.v(Constants.DEBUG_TAG, "Inserting activity: "+lastActivityClass.getNiceClassification()+", prev="+topClassification);
							adapter.insert(lastActivityClass,0);
						}else{
							adapter.getItem(0).updateEnd(Constants.DB_DATE_FORMAT.parse(lastActivityEndDate).getTime());
							adapter.notifyDataSetChanged();
						}
						
					}
				}
				prevServiceRunning = isServiceRunning;

			} catch (RemoteException ex) {
				Log.e(Constants.DEBUG_TAG, "Error while updating user interface", ex);
			}
		}

	}

}

/*
Changes made by Umran:

1) formatting.
2) changed method name updateButton() to updateUI(0
3) changed field name updateRunnable to updateInterfaceRunnable
4) removed initialisation of update UI sequence
	from
 		connection.onServiceConnected(ComponentName,IBinder)
 		startServiceRunnable.run()
 	to
 		onResume()
5) removed stopping of update UI sequence
	from
		onCreate(Bundle)
		clickListener.onClick(View)
		onServiceDisconnected(ComponentName)
	to
		onPause()
6) Added StartServiceRunnable to handle the service-starting sequence. i.e.
		i) Display progress dialog for 500ms
		ii) Start service
		iii) Display progress dialog for another 500ms
		iv) Close progress dialog
7) Added UpdateInterfaceRunnable to handle user interface updates.

 */
