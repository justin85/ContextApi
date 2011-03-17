package activity.classifier.activity;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import activity.classifier.R;
import activity.classifier.common.Constants;
import activity.classifier.db.OptionsTable;
import activity.classifier.db.SqlLiteAdapter;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.service.RecorderService;
import activity.classifier.utils.Calibrator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

public class MainSettingsActivity extends PreferenceActivity {

	ActivityRecorderBinder service = null;
	CheckBox checkBox;

	private int isWakeLockSet;
	private boolean wakelock;

	private SqlLiteAdapter sqlLiteAdapter;
	private OptionsTable optionsTable;
	
	private CheckBoxPreference aggregatePref;

	/**
	 * When the Service connection is established in this class,
	 * bind the Wake Lock status to notify RecorderService.
	 */
	private final ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = ActivityRecorderBinder.Stub.asInterface(iBinder);
			Log.i("isrunning","Connection "+service+"");
			try {
				if(service==null || !service.isRunning()) {
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
		this.sqlLiteAdapter = SqlLiteAdapter.getInstance(this);
		this.optionsTable = this.sqlLiteAdapter.getOptionsTable();
		
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
			
		aggregatePref.setChecked(optionsTable.getUseAggregator());
	}

	/**
	 * 
	 */
	protected void onPause() {
		super.onPause();
		optionsTable.save();
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



		wakelock = optionsTable.isWakeLockSet();
		Log.i("wake",wakelock+"");
		screenPref.setChecked(wakelock);  
		Log.i("wake",screenPref.isChecked()+"");
//		getApplicationContext().bindService(new Intent(this, RecorderService.class),
//				connection, Context.BIND_AUTO_CREATE);

		screenPref.setOnPreferenceChangeListener(new CheckBoxPreference.OnPreferenceChangeListener(){

			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				if((Boolean) arg1){
					wakelock=(Boolean) arg1;//true value
					Toast.makeText(getBaseContext(), "Screen Locked On", Toast.LENGTH_SHORT).show();
					//update Wake Lock state to 1 (true)
					optionsTable.setWakeLockSet(true);
//					getApplicationContext().unbindService(connection);
//					getApplicationContext().bindService(new Intent(getBaseContext(), RecorderService.class),
//							connection, Context.BIND_AUTO_CREATE);

				}
				else{
					wakelock=(Boolean) arg1;
					Toast.makeText(getBaseContext(), "Screen Locked Off", Toast.LENGTH_SHORT).show();
					optionsTable.setWakeLockSet(false);
					
				}
				optionsTable.save();
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

		CheckBoxPreferenceWithLongSummary testing =  new CheckBoxPreferenceWithLongSummary(this);
		testing.setKey("cali_preference");
		testing.setTitle("Current Calibration Values");
		setScreenSummary();
		testing.setSummary(getScreenSummary());
		testing.setSelectable(false);
		
		dialogBasedPrefCat.addPreference(testing);

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
				File dbfile = new File(Constants.PATH_SD_CARD_DUMP_DB);
				File dbPathParent = dbfile.getParentFile();
				if (dbPathParent!=null && !dbPathParent.exists()) {
					if (true)
						Log.d(Constants.DEBUG_TAG, "Create DB directory. " + dbPathParent.getAbsolutePath());
					dbPathParent.mkdirs();
				}

				copy(new File(Constants.PATH_ACTIVITY_RECORDS_DB), dbfile);
				Toast.makeText(getBaseContext(), "Database copied into " + dbfile.getAbsolutePath(), Toast.LENGTH_LONG).show();

				return false;
			}

		});
		filePrefCat.addPreference(copyPref);

		if (Constants.IS_DEV_VERSION) {
			PreferenceCategory developerPrefCat = new PreferenceCategory(this);
			developerPrefCat.setTitle("Developer Settings");
	
			aggregatePref = new CheckBoxPreference(this);
			aggregatePref.setKey("aggregate_preference");
			aggregatePref.setTitle("Aggregate Activities");
			aggregatePref.setSummary("Smoothen activity classification\n(requires service restart)");
			aggregatePref.setOnPreferenceChangeListener(new CheckBoxPreference.OnPreferenceChangeListener(){
	
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					boolean checked = (Boolean) arg1; 
					optionsTable.setUseAggregator(checked);
					optionsTable.save();
					aggregatePref.setChecked(checked);
					return false;
				}
	
			});
			aggregatePref.setChecked(optionsTable.getUseAggregator());

			root.addPreference(developerPrefCat);
			developerPrefCat.addPreference(aggregatePref);
		}

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
							sqlLiteAdapter.close();
							File f1 = new File(Constants.PATH_ACTIVITY_RECORDS_DB);
							f1.delete();
							Toast.makeText(getBaseContext(), "Database deleted", Toast.LENGTH_LONG).show();
							sqlLiteAdapter.open();
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
					Calibrator.resetCalibrationOptions(optionsTable);
					optionsTable.save();
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
		float[] sd = optionsTable.getSd();
		float[] offSet = optionsTable.getOffset();
		screenSummary =
		"Standard Deviation X : "+sd[Constants.ACCEL_X_AXIS]+"\n" +
		"Standard Deviation Y : "+sd[Constants.ACCEL_Y_AXIS]+"\n" +
		"Standard Deviation Z : "+sd[Constants.ACCEL_Z_AXIS]+"\n" +
		"Offset X                       : "+offSet[Constants.ACCEL_X_AXIS]+"\n" +
		"Offset Y                       : "+offSet[Constants.ACCEL_Y_AXIS]+"\n" +
		"Offset Z                       : "+offSet[Constants.ACCEL_Z_AXIS]+"\n";
	} 
	
	private String getScreenSummary(){
		return this.screenSummary;
	}
	
	private void copy(File sourceFile, File destinationFile) {
		try {
			InputStream lm_oInput = new FileInputStream(sourceFile);
			byte[] buff = new byte[128];
			FileOutputStream lm_oOutPut = new FileOutputStream(destinationFile);
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
	public class CheckBoxPreferenceWithLongSummary extends CheckBoxPreference{

	    public CheckBoxPreferenceWithLongSummary(Context context) {
	        super(context);
	    }

	    public CheckBoxPreferenceWithLongSummary(Context context, AttributeSet attrs) {
	        super(context, attrs);
	    }
	    public CheckBoxPreferenceWithLongSummary(Context context, AttributeSet attrs, int defStyle) {
	        super(context, attrs, defStyle);
	    }

	    @Override
	    protected void onBindView(View view) {
	        super.onBindView(view);
	        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
	        summaryView.setMaxLines(10);
	        CheckBox checkBox = (CheckBox) view.findViewById(android.R.id.checkbox);
	        checkBox.setVisibility(view.GONE);
	    }
	}

}
