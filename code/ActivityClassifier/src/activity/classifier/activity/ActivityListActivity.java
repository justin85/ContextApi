/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */

package activity.classifier.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import activity.classifier.R;
import activity.classifier.common.Constants;
import activity.classifier.common.ExceptionHandler;
import activity.classifier.db.ActivitiesTable;
import activity.classifier.db.OptionsTable;
import activity.classifier.db.SqlLiteAdapter;
import activity.classifier.rpc.ActivityRecorderBinder;
import activity.classifier.rpc.Classification;
import activity.classifier.service.RecorderService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;

/**
 * 
 * @author chris, modified Justin Lee
 * 
 */
public class ActivityListActivity extends Activity {
	
	private final static SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static int SINGLE_DAY = (24*60*60*1000);

	public static boolean serviceIsRunning = false;

	private final Handler handler = new Handler();

	private ActivityRecorderBinder service = null;
	
	private SqlLiteAdapter sqlLiteAdapter;
	private OptionsTable optionsTable;
	private ActivitiesTable activitiesTable;
	
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

		this.sqlLiteAdapter = SqlLiteAdapter.getInstance(this);
		this.optionsTable = this.sqlLiteAdapter.getOptionsTable();
		this.activitiesTable = this.sqlLiteAdapter.getActivitiesTable();
		
		setContentView(R.layout.main);

		ListView listView = (ListView) findViewById(R.id.list); 
		listView.setAdapter(new ArrayAdapter<Classification>(this,R.layout.item));
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

		//	avoids conflicts between scheduled updates,
		//		and once-off updates 
		private ReentrantLock reentrantLock = new ReentrantLock();

		private long lastListUpdateAt = 0;
		
		//	reusable items
		private Classification reusableClassification = new Classification();
		private Map<Long,Integer> currentlyDisplayedItems = new TreeMap<Long,Integer>();
		private List<Classification> filteredOut = new ArrayList<Classification>(200);
		private int itemsInserted;
		private int itemsUpdated;
		private long maxUpdateTime;
		
		//	starts scheduled interface updates
		public void start() {
			handler.postDelayed(this, 100);
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
				updateUI();
				reentrantLock.unlock();
			}
		}

		public void run() {
			if (reentrantLock.tryLock()) {
				updateUI();
				reentrantLock.unlock();
			}
			
			handler.postDelayed(this, Constants.DELAY_UI_GRAPHIC_UPDATE);
		}
		
		/**
		 * updates the list,
		 * 
		 * removing any items that shouldn't be displayed,
		 * adding any new items recently inserted
		 * and updating any items recently updated
		 */
		private void updateUI() {
			@SuppressWarnings("unchecked")
			final ArrayAdapter<Classification> adapter = (ArrayAdapter<Classification>) ((ListView) findViewById(R.id.list)).getAdapter();
			
			long currentTime = System.currentTimeMillis();
			
			//	define the required period in which we want items to appear on the list
			long periodStart = currentTime;
			long periodEnd = currentTime - SINGLE_DAY;
			
			//	remove all items older than the required period
			{
				filteredOut.clear();
				// also, obtain a list of items currently on the list, and their locations
				currentlyDisplayedItems.clear();
				int index = 0;
				//	go through each item in the list
				for (int len=adapter.getCount(), i=0; i<len; ++i) {
					Classification cl = adapter.getItem(i);
					long end = cl.getEnd();
					if (end<periodEnd) {
						filteredOut.add(cl);
					} else {
						currentlyDisplayedItems.put(cl.getStart(), index);
						++index;
					}
				}
				//	remove the items
				for (Classification cl:filteredOut)
					adapter.remove(cl);
				filteredOut.clear();
			}
			
			this.itemsInserted = 0;
			this.itemsUpdated = 0;
			this.maxUpdateTime = Long.MIN_VALUE;			
			
			//	fetch newly updated items between the period
			activitiesTable.loadUpdated(
					periodStart, periodEnd,
					lastListUpdateAt,
					reusableClassification,
					new ActivitiesTable.ClassificationDataCallback() {
						@Override
						public void onRetrieve(Classification cl) {
							//	check if item is on the list (updated) or is new (inserted)
							if (currentlyDisplayedItems.containsKey(cl.getStart())) {
								int index = currentlyDisplayedItems.get(cl.getStart());
								//shift the original index by the number of items inserted
								Classification dst = adapter.getItem(itemsInserted+index);
								//	update the one on the list
								dst.setClassification(cl.getClassification());
								dst.setEnd(cl.getEnd());
								++itemsUpdated;
							} else {
								//	insert at the top
								Classification classification = new Classification(cl.getClassification(), cl.getStart(), cl.getEnd());
								classification.withContext(ActivityListActivity.this);
								adapter.insert(classification, 0);
								++itemsInserted;
							}
							
							//	find the time the latest item was updated
							long lastUpdate = cl.getLastUpdate(); 
							if (lastUpdate>maxUpdateTime) {
								maxUpdateTime = lastUpdate;
							}
						}
					}
				);
			
			//	if any item has been updated or inserted
			if (itemsUpdated>0 || itemsInserted>0) {
				//	notify that the list data set has changed
				adapter.notifyDataSetChanged();
				
				//	set the time the list was last updated
				//		to the time the latest item was updated
				lastListUpdateAt = maxUpdateTime;
			}
		}
	}
	
}
