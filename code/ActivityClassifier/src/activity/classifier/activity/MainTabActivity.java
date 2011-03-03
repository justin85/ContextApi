package activity.classifier.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.flurry.android.FlurryAgent;

import activity.classifier.R;
import activity.classifier.common.Constants;
import activity.classifier.common.ExceptionHandler;
import activity.classifier.repository.OptionQueries;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.rpc.Classification;
import activity.classifier.service.RecorderService;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

public class MainTabActivity extends TabActivity {
	public static boolean serviceIsRunning = false;

	private final Handler handler = new Handler();

	private ActivityRecorderBinder service = null;

	private ProgressDialog dialog;

	/**
	 * enable to delete database in the device repository.
	 */
	private boolean EnableDeletion;

	private OptionQueries optionQuery;

	/**
	 * Updates the user interface.
	 */

	/**
	 * Displays the progress dialog, waits some time, starts the service, waits some more,
	 * 	then hides the dialog.
	 */
	private final StartServiceRunnable startServiceRunnable = new StartServiceRunnable();
	private final UpdateButtonRunnable updateButtonRunnable = new UpdateButtonRunnable();

	/**
	 *	Performs necessary tasks when the connection to the service
	 *	is established, and after it is disconnected.
	 */
	private final ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = ActivityRecorderBinder.Stub.asInterface(iBinder);
			updateButtonRunnable.updateNow();
		}

		public void onServiceDisconnected(ComponentName componentName) {
			service = null;

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
	 * Enable to use menu button to make option menus.
	 */
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu, menu);
		return true;
	}

	/**
	 * Set the number of option menus on this Activity.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.startService).setIcon(android.R.drawable.ic_media_play).setEnabled(EnableDeletion);
		menu.findItem(R.id.stopService).setIcon(android.R.drawable.ic_media_pause).setEnabled(!EnableDeletion);

		return true;
	}

	/**
	 * Performs the option menu with an appropriate option clicked.   
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.startService:
			try {

				EnableDeletion = false;
				FlurryAgent.onEvent("recording_start");
				startServiceRunnable.startService();

			} catch (Exception ex) {
				Log.e(Constants.DEBUG_TAG, "Unable to get service state", ex);
			}
			break;
		case R.id.stopService:
			try {
				if (service.isRunning()) {

					EnableDeletion = true;
					FlurryAgent.onEvent("recording_stop");

					onDestroy();
					stopService(new Intent(MainTabActivity.this, RecorderService.class));
					//					unbindService(connection);
					bindService(new Intent(MainTabActivity.this, RecorderService.class),
							connection, BIND_AUTO_CREATE);

				} 
			} catch (RemoteException ex) {
				Log.e(Constants.DEBUG_TAG, "Unable to get service state", ex);
			}
			break;
		}
		return true;

	}



	/**
	 * 
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}



	/**
	 * 
	 */
	@Override
	protected void onStart() {
		super.onStart();
		updateButtonRunnable.start();
		FlurryAgent.onStartSession(this, "EMKSQFUWSCW51AKBL2JJ");
	}

	/**
	 * 
	 */
	@Override
	protected void onStop() {
		super.onStop();
		updateButtonRunnable.stop();
		FlurryAgent.onEndSession(this);
	}





	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		optionQuery = new OptionQueries(this);


		final TabHost tabHost = getTabHost();

		tabHost.addTab(tabHost.newTabSpec("tab1")
				.setIndicator(" ",getResources().getDrawable(R.drawable.chart72))
				.setContent(new Intent(this, ActivityChartActivity.class)
				));

		tabHost.addTab(tabHost.newTabSpec("tab2")
				.setIndicator(" ",getResources().getDrawable(R.drawable.database72))
				.setContent(new Intent(this, ActivityListActivity.class)));

		tabHost.addTab(tabHost.newTabSpec("tab3")
				.setIndicator(" ",getResources().getDrawable(R.drawable.settings72))
				.setContent(new Intent(this, MainSettingsActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
		bindService(new Intent(this, RecorderService.class), connection, BIND_AUTO_CREATE);
		EnableDeletion = true;

	}

	/**
	 * 
	 * @author Umran
	 *
	 */
	private class StartServiceRunnable implements Runnable {

		private static final int DISPLAY_START_DIALOG = 0;
		private static final int CLOSE_START_DIALOG = 1;

		private int nextStep;

		public void startService() {
			if (dialog!=null) {
				return;
			}


			dialog = ProgressDialog.show(	MainTabActivity.this,
					"Starting service",
					"Please wait...", true);			
			nextStep = DISPLAY_START_DIALOG;
			handler.postDelayed(this, 100);
		}

		public void run() {
			//			Log.i("button","run");
			switch (nextStep) {
			case DISPLAY_START_DIALOG:
			{
				Intent intent = new Intent(	MainTabActivity.this,
						RecorderService.class	);
				MainTabActivity.this.startService(intent);

				nextStep = CLOSE_START_DIALOG;					
				handler.postDelayed(this, Constants.DELAY_SERVICE_START);
				break;
			}
			case CLOSE_START_DIALOG:
			{
				try {
					//	hide the progress dialog box
					dialog.dismiss();
					dialog = null;


					//	if the service is still not running by the end of the delay
					//		display an error message
					if (service==null || !service.isRunning())
					{
						AlertDialog.Builder builder = new AlertDialog.Builder(MainTabActivity.this);
						builder.setTitle("Error");
						builder.setMessage("Unable to start service"); // TODO: Put this into the string file
						builder.show();							
					}
				} catch (RemoteException e) {
					Log.e(Constants.DEBUG_TAG, "Error Starting Recorder Service", e);
				}
				break;
			}
			}


		}

	}
	private class UpdateButtonRunnable implements Runnable {

		//	save the state of the service, if it was previously running or not
		//		to avoid unnecessary updates
		private boolean prevServiceRunning = false;

		//	avoids conflicts between scheduled updates,
		//		and once-off updates 
		private ReentrantLock reentrantLock = new ReentrantLock();

		//	starts scheduled interface updates
		public void start() {
			handler.postDelayed(updateButtonRunnable, Constants.DELAY_UI_UPDATE);
		}

		//	stops scheduled interface updates
		public void stop() {
			handler.removeCallbacks(updateButtonRunnable);
		}

		//	performs a once-off unsynchronised (unscheduled) interface update
		//		please note that this can be called from another thread
		//		without interfering with the normal scheduled updates.
		public void updateNow() {
			if (reentrantLock.tryLock()) {

				try {
					updateButton();
				} catch (ParseException ex) {
					Log.e(Constants.DEBUG_TAG, "Error while performing scheduled UI update.", ex);
				}

				reentrantLock.unlock();
			}
		}

		public void run() {
			if (reentrantLock.tryLock()) {

				try {
					updateButton();
				} catch (ParseException e) {
					e.printStackTrace();
				}

				reentrantLock.unlock();
			}

			handler.postDelayed(updateButtonRunnable, Constants.DELAY_UI_UPDATE);
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
		private void updateButton() throws ParseException {

			try {
				boolean isServiceRunning = service!=null && service.isRunning();

				//	update toggle text only if service state has changed
				if (isServiceRunning!=prevServiceRunning) {
					try {
						if(service==null || !service.isRunning()){
							//							Log.i("button","true");
							EnableDeletion = true;
						}else{
							//							Log.i("button","false");
							EnableDeletion = false;
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}


			} catch (RemoteException ex) {
				Log.e(Constants.DEBUG_TAG, "Error while updating user interface", ex);
			}
		}

	}

}

