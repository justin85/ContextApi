package activity.classifier.service.threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import activity.classifier.common.Constants;
import activity.classifier.repository.ActivityQueries;
import activity.classifier.utils.PhoneInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

/**
 * 
 * @author Justin Lee
 *
 *	<p>
 *	Changes made by Umran: <br>
 *	Class used to be called <code>UploadActivityHistory</code>
 *	Changed class from using a Timer, to being a thread on its own. As a thread,
 *	the delays involved in uploading content to the internet will be sheltered
 *	from the rest of the application. The thread rests for a period given
 *	as {@link Constants.DELAY_UPLOAD_DATA} before uploading another batch of data.
 *
 */
public class UploadActivityHistoryThread extends Thread {

	protected AccountManager accountManager;

	private boolean shouldExit;
	private PhoneInfo phoneInfo;
	private ActivityQueries activityQuery;
	private boolean uploading;		//	is the thread currently uploading (to avoid interrupting)
	private long lastUploadTime;	//	last time data was uploaded

	/**
	 * Initialise {@link ActivityQueries} class instance.
	 * @param context context from Activity or Service classes passes to {@link ActivityQueries} class instance.
	 */
	public UploadActivityHistoryThread(Context context, ActivityQueries activityQuery, PhoneInfo phoneInfo) {
		this.activityQuery = activityQuery;
		this.phoneInfo = phoneInfo;

		String dbfile = Constants.PATH_ACTIVITY_RECORDS_FILE;
		copy(Constants.PATH_ACTIVITY_RECORDS_DB,dbfile);
	}

	/**
	 * cancel timer when the background service is destroyed.
	 */
	public synchronized void cancelUploads(){
		//	signal the thread to exit
		this.shouldExit = true;

		//	not sure what problems may happen if interrupted while uploading
		//	so lets avoid interrupting during an upload,
		//	even if it means that the service may delay during destruction and
		//	probably be force-closed.
		if (!this.uploading)
			this.interrupt();
	}

	/**
	 * By using {@link ActivityQueries} class, check every un-sent activities from device repository,
	 * and upload un-sent activities from device repository to Web server.
	 * Timer scheduler is set to every 5 min.
	 *
	 * @param accountName
	 */
	public void startUploads() {

		if (this.isAlive()) {
			return;
		}

		this.shouldExit = false;
		this.uploading = false;
		this.lastUploadTime = System.currentTimeMillis();
		this.start();
	}


	@Override
	public void run() {

		String accountName = null;
		long currentTime;

		while (!shouldExit) {
			//	wait for next time
			try {
				//	sleep for 1 second
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			if (accountName==null) {
				accountName = phoneInfo.getAccountName();
			}

			currentTime = System.currentTimeMillis();
			//	wait until our download time has reached
			if (accountName!=null && currentTime-lastUploadTime>=Constants.DELAY_UPLOAD_DATA) {
				//	upload the data
				uploading = true;
				uploadData(accountName);
				uploading = false;
				lastUploadTime = currentTime;
				Log.i("upload","ok");
			}
		}

	}

	private void uploadData(String accountName)
	{
		HttpClient client = new DefaultHttpClient();

		final HttpPost post = new HttpPost(Constants.URL_ACTIVITY_POST);
		final File file = new File(Constants.PATH_ACTIVITY_RECORDS_FILE);
		final FileEntity entity = new FileEntity(file, "text/plain");

		// ArrayList data type for un-sent activities.
		ArrayList<String> itemNames = new ArrayList<String>();
		ArrayList<String> itemStartDates = new ArrayList<String>();
		ArrayList<String> itemEndDates = new ArrayList<String>();
		ArrayList<Integer> itemIDs = new ArrayList<Integer>();
		//open database and check the un-posted data
		activityQuery.getUncheckedItemsFromActivityTable(0);

		itemIDs = activityQuery.getUncheckedItemIDs();
		itemNames = activityQuery.getUncheckedItemNames();
		itemStartDates = activityQuery.getUncheckedItemStartDates();
		itemEndDates = activityQuery.getUncheckedItemEndDates();

		//get the number of un-posted activities
		int size=activityQuery.getUncheckedItemsSize();

		Log.i("uncheckedItems",size+"");

		/*
		 * if there are un-posted activities in the device repository,
		 * then merge every information of activity into one string(message),
		 * then upload to Web server.
		 */
		if(size!=0){
			String message = "";
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			DateFormat df1 = new SimpleDateFormat("Z z");
			//merge information of activities
			for(int i = 0 ; i<size;i++){
				Date tempdate;
				try {
					tempdate = df.parse(itemStartDates.get(i));

					if(i==size){
						message +=  itemNames.get(i)+"&&"+itemStartDates.get(i)+"&&"+df1.format(tempdate);
					}else{
						message +=  itemNames.get(i)+"&&"+itemStartDates.get(i)+"&&"+df1.format(tempdate)+"##";
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}

		    }
		    
		    //send un-posted activities with the size, date, and Google account to Web server.
		    try {
		    	
		    	
		    	Date systemdate = Calendar.getInstance().getTime();
		    	String reportDate = df.format(systemdate);
		    	post.setHeader("sysdate",reportDate);
		    	post.setHeader("size",size+"");
	  	        post.setHeader("message", message);
	  	        post.setHeader("UID", accountName);
	  	        post.setEntity(entity);
	   	    	
	  	        int code = new DefaultHttpClient().execute(post).getStatusLine().getStatusCode();
		        Log.i("m",message);
		        ArrayList<String[]> items = new ArrayList<String[]>();
		        for(int i=0;i<size;i++){
		        	String[] tempItmes = {itemIDs.get(i)+"", itemNames.get(i), itemStartDates.get(i),itemEndDates.get(i), "1"};
		        	items.add(tempItmes);
		        }
		        activityQuery.updateUncheckedItems(items);
		        // update un-posted items to posted in device repository.
//		        for(int i=0;i<size;i++){
//		        	activityQuery.updateUncheckedItems(itemIDs.get(i), itemNames.get(i), itemStartDates.get(i),itemEndDates.get(i), 1);
//        		}
            // if any failure of the response
		    } catch (Exception ex) {
	            	Log.e(getClass().getName(), "Unable to upload sensor logs", ex);
		    } 
		   
		    itemIDs.clear();
		    itemNames.clear();
		    itemStartDates.clear();
		    itemEndDates.clear();
	    }	
	}

	/**
	 * A utility method which copy the database to SD card.
	 * @param targetFile database in the device repository.
	 * @param copyFile path to be copied
	 */
	private void copy( String targetFile, String copyFile ){
		try {
			InputStream lm_oInput = new FileInputStream(new File(targetFile));
			byte[] buff = new byte[ 128 ];
			FileOutputStream lm_oOutPut = new FileOutputStream( copyFile );
			while(true){
				int bytesRead = lm_oInput.read( buff );
				if( bytesRead == -1 ) break;
				lm_oOutPut.write( buff, 0, bytesRead );
			}

			lm_oInput.close();
			lm_oOutPut.close();
			lm_oOutPut.flush();
			lm_oOutPut.close();
		}catch( Exception e ){
		}
	}
}
