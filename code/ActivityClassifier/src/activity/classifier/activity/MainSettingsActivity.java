package activity.classifier.activity;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.flurry.android.FlurryAgent;

import activity.classifier.R;
import activity.classifier.common.Constants;
import activity.classifier.repository.OptionQueries;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.service.RecorderService;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

public class MainSettingsActivity extends PreferenceActivity {

	ActivityRecorderBinder service = null;
	CheckBox checkBox;

	private int isWakeLockSet;
	private boolean wakelock;

	private OptionQueries optionQuery;

	/**
	 * When the Service connection is established in this class,
	 * bind the Wake Lock status to notify RecorderService.
	 */
	private final ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = ActivityRecorderBinder.Stub.asInterface(iBinder);
			Log.i("isrunning","Connection "+service+"");
			try {
				if(service==null || !service.isRunning()){
				}
				else{
					Log.i("Wakelock", "setWakelock from Setting");
					service.setWakeLock();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName componentName) {
			service = null;

			Log.i(Constants.DEBUG_TAG, "Service Disconnected");
		}


	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		optionQuery = new OptionQueries(this);
		init();
		wakelock=false;
		Intent intent = new Intent(this, RecorderService.class);
		if(!getApplicationContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)){
			throw new IllegalStateException("Binding to service failed " + intent);
		}
		Log.i("isrunning",connection+"");
		Log.i("isrunning","Createy "+service+"");
		setPreferenceScreen(createPreferenceHierarchy());


	}
	/**
	 * 
	 */
	protected void onResume() {
		super.onResume();
		Log.i("isrunning","Resume "+service+"");
		if(service!=null)
			try {
				Log.i("isrunning","Stop "+service.isRunning()+"");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	/**
	 * 
	 */
	protected void onPause() {
		super.onPause();
		Log.i("isrunning","Pause "+service+"");
		if(service!=null)
			try {
				Log.i("isrunning","Stop "+service.isRunning()+"");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	/**
	 * 
	 */
	@Override
	protected void onStart() {
		super.onStart();
		Log.i("isrunning","Start "+service+"");
		FlurryAgent.onStartSession(this, "EMKSQFUWSCW51AKBL2JJ");
	}

	/**
	 * 
	 */
	@Override
	protected void onStop() {
		super.onStop();
		Log.i("isrunning","Stop "+service+"");
		FlurryAgent.onEndSession(this);
	}
	/**
	 * 
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i("isrunning","Destory "+service);
		getApplicationContext().unbindService(connection);
	}
	/**
	 * 
	 * @param intent
	 * @return null
	 */
	public IBinder onBind(Intent intent) {
		return null;
	}
	private void init(){
		this.optionQuery.load();
	}
	private PreferenceScreen createPreferenceHierarchy() {
		
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

		// Inline preferences 
		PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
		inlinePrefCat.setTitle("Screen Wake Lock Settings ");
		root.addPreference(inlinePrefCat);

		// Toggle preference
		final CheckBoxPreference screenPref = new CheckBoxPreference(this);
		screenPref.setKey("screen_preference");
		screenPref.setTitle("Screen Locked On");
		screenPref.setSummary("It's necessary for some phones (e.g. HTC Desire) but higher battery consumption.");



		wakelock = optionQuery.isWakeLockSet();
		Log.i("wake",wakelock+"");
		screenPref.setChecked(wakelock);  
		Log.i("wake",screenPref.isChecked()+"");
		getApplicationContext().bindService(new Intent(this, RecorderService.class),
				connection, Context.BIND_AUTO_CREATE);

		screenPref.setOnPreferenceChangeListener(new CheckBoxPreference.OnPreferenceChangeListener(){

			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				if((Boolean) arg1){
					wakelock=(Boolean) arg1;//true value
					Toast.makeText(getBaseContext(), "Screen Locked On", Toast.LENGTH_SHORT).show();
					//update Wake Lock state to 1 (true)
					optionQuery.setWakeLockState(true);
//					getApplicationContext().unbindService(connection);
//					getApplicationContext().bindService(new Intent(getBaseContext(), RecorderService.class),
//							connection, Context.BIND_AUTO_CREATE);

				}
				else{
					wakelock=(Boolean) arg1;
					Toast.makeText(getBaseContext(), "Screen Locked Off", Toast.LENGTH_SHORT).show();
					optionQuery.setWakeLockState(false);
					
				}
				optionQuery.save();
				getApplicationContext().unbindService(connection);
				getApplicationContext().bindService(new Intent(getBaseContext(), RecorderService.class),
						connection, Context.BIND_AUTO_CREATE);
				screenPref.setChecked(wakelock);
				
				return false;
			}

		});



		inlinePrefCat.addPreference(screenPref);


		// Dialog based preferences
		PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(this);
		dialogBasedPrefCat.setTitle("Calibration Settings");
		root.addPreference(dialogBasedPrefCat);


		caliPref = getPreferenceManager().createPreferenceScreen(this);
		PreferenceScreen resetPref = getPreferenceManager().createPreferenceScreen(this);
		resetPref.setKey("screen_preference");
		resetPref.setTitle("Re-set Calibration values");
		resetPref.setSummary("Reset calibration values to default.");
		resetPref.setOnPreferenceClickListener(new PreferenceScreen.OnPreferenceClickListener(){

			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_YES_NO_MESSAGE_FOR_RESET_CALIBRATION);
				
				return false;
			}

		});
		dialogBasedPrefCat.addPreference(resetPref);


		caliPref.setKey("cali_preference");
		caliPref.setTitle("Current Calibration Values");
		setScreenSummary();
		caliPref.setSummary(getScreenSummary());
		caliPref.setSelectable(false);
		dialogBasedPrefCat.addPreference(caliPref);

		PreferenceCategory filePrefCat = new PreferenceCategory(this);
		filePrefCat.setTitle("Data settings");
		root.addPreference(filePrefCat);

		PreferenceScreen deletePref = getPreferenceManager().createPreferenceScreen(this);
		deletePref.setKey("delete_preference");
		deletePref.setTitle("Delete Database");
		deletePref.setSummary("Delete all activity data and user information.");
		deletePref.setOnPreferenceClickListener(new PreferenceScreen.OnPreferenceClickListener(){

			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_YES_NO_MESSAGE_FOR_DELETION);
				return false;
			}

		});
		filePrefCat.addPreference(deletePref);

		PreferenceScreen copyPref = getPreferenceManager().createPreferenceScreen(this);
		copyPref.setKey("copy_preference");
		copyPref.setTitle("Copy Database to SDcard");
		copyPref.setSummary("Copy database file to SD card.");
		copyPref.setOnPreferenceClickListener(new PreferenceScreen.OnPreferenceClickListener(){

			public boolean onPreferenceClick(Preference preference) {
				File sdcard = Environment.getExternalStorageDirectory();
				File dbpath = new File(sdcard.getAbsolutePath() + File.separator
						+ "activityclassfier");
				if (!dbpath.exists()) {
					if (true)
						Log.d(Constants.DEBUG_TAG, "Create DB directory. " + dbpath.getAbsolutePath());
					dbpath.mkdirs();
				}

				String dbfile = dbpath.getAbsolutePath() + File.separator
				+ "activityrecords.db";
				copy(Constants.PATH_ACTIVITY_RECORDS_DB, dbfile);
				Toast.makeText(getBaseContext(), "Database copied into " + dbpath.getAbsolutePath()+"/ directory.", Toast.LENGTH_LONG).show();

				return false;
			}

		});
		filePrefCat.addPreference(copyPref);



		return root;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_YES_NO_MESSAGE_FOR_DELETION:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.arrow_down_float)
			.setTitle("Warning")
			.setMessage("Your all activity history data will be deleted. Do you really want to delete the database?")
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked OK so do some stuff */
					try {
						if (service==null || !service.isRunning()) {
							File f1 = new File(Constants.PATH_ACTIVITY_RECORDS_DB);
							f1.delete();
							Toast.makeText(getBaseContext(), "Database deleted", Toast.LENGTH_LONG).show();

						} else {
							Toast.makeText(getBaseContext(), "Stop Service first!", Toast.LENGTH_LONG).show();
						}
					} catch (RemoteException ex) {
						Log.e(Constants.DEBUG_TAG, "Unable to get service state", ex);
					}
					
					
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					/* User clicked Cancel so do some stuff */
				}
			})
			.create();
		case DIALOG_YES_NO_MESSAGE_FOR_RESET_CALIBRATION:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.arrow_down_float)
			.setTitle("Warning")
			.setMessage("Do you really want to reset the calibration values?")
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					optionQuery.setCalibrationState(false);
					optionQuery.setValueOfGravity(1);
					optionQuery.setStandardDeviationX((float)0.05);
					optionQuery.setStandardDeviationY((float)0.05);
					optionQuery.setStandardDeviationZ((float)0.05);
					optionQuery.save();
					setScreenSummary();
					caliPref.setSummary(getScreenSummary());
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					/* User clicked Cancel so do some stuff */
				}
			})
			.create();
		}
		return null;
	}
	
	private String screenSummary ="";
	private static final int DIALOG_YES_NO_MESSAGE_FOR_DELETION = 0;
	private static final int DIALOG_YES_NO_MESSAGE_FOR_RESET_CALIBRATION = 1;
	PreferenceScreen caliPref;
	private void setScreenSummary(){
		screenSummary = "Gravity Value               : "+optionQuery.getValueOfGravity()+"\n" +
		"Standard Deviation X : "+optionQuery.getStandardDeviationX()+"\n" +
		"Standard Deviation Y : "+optionQuery.getStandardDeviationY()+"\n" +
		"Standard Deviation Z : "+optionQuery.getStandardDeviationZ()+"\n";
	}
	private String getScreenSummary(){
		return this.screenSummary;
	}
	private void copy(String targetFile, String copyFile) {
		try {
			InputStream lm_oInput = new FileInputStream(new File(targetFile));
			byte[] buff = new byte[128];
			FileOutputStream lm_oOutPut = new FileOutputStream(copyFile);
			while (true) {
				int bytesRead = lm_oInput.read(buff);
				if (bytesRead == -1)
					break;
				lm_oOutPut.write(buff, 0, bytesRead);
			}

			lm_oInput.close();
			lm_oOutPut.close();
			lm_oOutPut.flush();
			lm_oOutPut.close();
		} catch (Exception e) {
			Log.e(Constants.DEBUG_TAG, "Copy database to SD Card Error", e);
		}
	}
}
