package activity.classifier.repository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * A utility class which extends superclass {@link Queries} 
 * for handling queries to save or load activity.
 * 
 *	<p>
 *	Changes made by Umran: <br>
 *	Changed all methods dealing with database open/close methods
 *	to <code>synchronized</code> in order to ensure thread safety when called 
 *	from multiple threads.
 * 
 * @author Justin Lee
 *
 */
public class ActivityQueries extends Queries{

	private DbAdapter dbAdapter;

	/**
	 * ArrayList data type to store un-posted items.
	 */
	private ArrayList<String> itemNames = new ArrayList<String>();
	private ArrayList<String> itemStartDates = new ArrayList<String>();
	private ArrayList<String> itemEndDates = new ArrayList<String>();
	private ArrayList<Integer> itemIDs = new ArrayList<Integer>();

	private ArrayList<String> todayItemNames = new ArrayList<String>();
	private ArrayList<String> todayItemStartDates = new ArrayList<String>();
	private ArrayList<String> todayItemEndDates = new ArrayList<String>();
	private ArrayList<Integer> todayItemIDs = new ArrayList<Integer>();

	private ArrayList<String> fourHourItemNames = new ArrayList<String>();
	private ArrayList<String> fourHourItemStartDates = new ArrayList<String>();
	private ArrayList<String> fourHourItemEndDates = new ArrayList<String>();
	private ArrayList<Integer> fourHourItemIDs = new ArrayList<Integer>();

	private ArrayList<String> hourItemNames = new ArrayList<String>();
	private ArrayList<String> hourItemStartDates = new ArrayList<String>();
	private ArrayList<String> hourItemEndDates = new ArrayList<String>();
	private ArrayList<Integer> hourItemIDs = new ArrayList<Integer>();

	/**
	 * the number of un-posted items.
	 */
	private int size;

	private int todaySize;
	private int fourHourSize;
	private int hourSize;

	/**
	 * @see Queries
	 * @param context context from Activity or Service classes 
	 */
	public ActivityQueries(Context context) {
		super(context);
		dbAdapter = super.dbAdapter;
	}
//	public synchronized void getLastItemFromActivityTable(int rowId){
//		dbAdapter.open();
//		
//	}
	
	/**
	 * Insert activity into the database.
	 *  
	 * @see DbAdapter
	 * @param activity name of activity 
	 * @param time date when the activity is happened
	 * @param isChecked state whether posted or not (1 or 0)
	 */
	public synchronized void insertActivities(String activity, String time, int isChecked){
		dbAdapter.open();
		dbAdapter.insertToActivityTable(activity, time, isChecked);
		dbAdapter.close();
	}

	/**
	 * Get un-posted items from the database.
	 * 
	 * @see DbAdapter
	 * @param isChecked state whether posted or not (1 or 0)
	 */
	public synchronized void getUncheckedItemsFromActivityTable(int isChecked){

		ArrayList<String> uncheckedItems = new ArrayList<String>();

		dbAdapter.open();
		//get un-posted items and assign them to #uncheckedItems.
		Cursor result = dbAdapter.fetchUnCheckedItemsFromActivityTable(isChecked);
		int i=0;
		while(result.moveToNext()) {

			uncheckedItems.add(Integer.parseInt(result.getString(0))+","+result.getString(1)+","+result.getString(2)+","+result.getString(3));
			//			Log.i("uncheckedItems",uncheckedItems.get(i));
			i++;
		}
		result.close();
		dbAdapter.close();

		/*
		 * since the results from the query have three columns of data (Activity name, Date, state)
		 * it needs to separate to three part as well in order to get them separately.
		 */
		seperateItems(uncheckedItems);
		setUncheckedItemsSize(uncheckedItems.size());
	}

	public synchronized void getItemsFromActivityTable(String itemName){

		ArrayList<String> items = new ArrayList<String>();

		dbAdapter.open();
		//get un-posted items and assign them to #uncheckedItems.
		Cursor result = dbAdapter.fetchItemsFromActivityTable(itemName);
		int i=0;
		while(result.moveToNext()) {

			items.add(Integer.parseInt(result.getString(0))+","+result.getString(1)+","+result.getString(2)+","+result.getString(3));
			//			Log.i("uncheckedItems",items.get(i));
			i++;
		}
		result.close();
		dbAdapter.close();

		/*
		 * since the results from the query have three columns of data (Activity name, Date, state)
		 * it needs to separate to three part as well in order to get them separately.
		 */
		seperateItems(items);
		setUncheckedItemsSize(items.size());
	}
	public synchronized ArrayList<String[]> getTodayItemsFromActivityTable(){

		ArrayList<String[]> items = new ArrayList<String[]>();
		Date date = new Date();
		int day = date.getDate();
		int month = date.getMonth()+1;
		Calendar calendarToday = Calendar.getInstance();
		calendarToday.add(Calendar.DAY_OF_MONTH, -1);
		Date todayTime = calendarToday.getTime();
		Calendar calendarFourHour = Calendar.getInstance();
		calendarFourHour.add(Calendar.HOUR, -4);
		Date fourHourTime = calendarFourHour.getTime();
		Calendar calendarHour = Calendar.getInstance();
		calendarHour.add(Calendar.HOUR, -1);
		Date hourTime = calendarHour.getTime();

		setOneDayBefore(todayTime);
		setFourHourBefore(fourHourTime);
		setHourBefore(hourTime);


		dbAdapter.open();
		items = dbAdapter.fetchTodayItemsFromActivityTable(todayTime,date);
		//		Log.i("DB",items.size()+"");
		dbAdapter.close();
		ArrayList<ArrayList<String[]>> activityGroup = new ArrayList<ArrayList<String[]>>();
		activityGroup=getActivityGroup(items);

		return items;

	}
	private Date todayTime;
	private Date fourHourTime;
	private Date hourTime;

	public Date getOneDayBefore(){
		return todayTime;
	}
	public void setOneDayBefore(Date todayTime){
		this.todayTime = todayTime;
	}
	public Date getFourHourBefore(){
		return fourHourTime;
	}
	public void setFourHourBefore(Date fourHourTime){
		this.fourHourTime = fourHourTime;
	}
	public Date getHourBefore(){
		return hourTime;
	}
	public void setHourBefore(Date hourTime){
		this.hourTime = hourTime;
	}

	public ArrayList<ArrayList<String[]>> getActivityGroup(ArrayList<String[]> items){
		ArrayList<ArrayList<String[]>> activityGroup = new ArrayList<ArrayList<String[]>>();
		ArrayList<String[]> CHARGING = new ArrayList<String[]>();
		ArrayList<String[]> UNCARRIED = new ArrayList<String[]>();
		ArrayList<String[]> WALKING = new ArrayList<String[]>();
		ArrayList<String[]> TRAVELLING = new ArrayList<String[]>();
		ArrayList<String[]> PADDLING = new ArrayList<String[]>();
		ArrayList<String[]> ACTIVE = new ArrayList<String[]>();
		ArrayList<String[]> UNKNOWN = new ArrayList<String[]>();

		for(int i=0;i<items.size();i++){
			if(items.get(i)[1].equals("Charging")){
				CHARGING.add(items.get(i));
			}else if(items.get(i)[1].equals("Uncarried")){
				UNCARRIED.add(items.get(i));
			}else if(items.get(i)[1].equals("Walking")){
				WALKING.add(items.get(i));
			}else if(items.get(i)[1].equals("Travelling")){
				TRAVELLING.add(items.get(i));
			}else if(items.get(i)[1].equals("Paddling")){
				PADDLING.add(items.get(i));
			}else if(items.get(i)[1].equals("Active")){
				ACTIVE.add(items.get(i));
			}else if(items.get(i)[1].equals("Unknown")){
				UNKNOWN.add(items.get(i));
			}else{

			}
		}

		activityGroup.add(CHARGING);
		activityGroup.add(UNCARRIED);
		activityGroup.add(WALKING);
		activityGroup.add(TRAVELLING);
		activityGroup.add(PADDLING);
		activityGroup.add(ACTIVE);
		activityGroup.add(UNKNOWN);

		return activityGroup;
	}

	/**
	 * 
	 * @param rowId table row ID
	 * @return activity name related to the row ID
	 */
	public synchronized String getItemNameFromActivityTable(long rowId){
		dbAdapter.open();
		String result = dbAdapter.fetchLastItemNames(rowId);
		dbAdapter.close();
		return result;
	}

	/**
	 * 
	 * @param rowId table row ID
	 * @return end date related to the row ID
	 */
	public synchronized String getItemEndDateFromActivityTable(long rowId){
		dbAdapter.open();
		String result = dbAdapter.fetchLastItemEndDate(rowId);
		dbAdapter.close();
		return result;
	}

	/**
	 * 
	 * @param rowId table row ID
	 * @return end date related to the row ID
	 */
	public synchronized String getItemStartDateFromActivityTable(long rowId){
		dbAdapter.open();
		String result = dbAdapter.fetchLastItemStartDate(rowId);
		dbAdapter.close();
		return result;
	}

	/**
	 * 
	 * @param ItemIDs row ID
	 * @param ItemEndDates activity end time
	 */
	public synchronized void updateNewItems(long ItemIDs, String ItemEndDates){
		dbAdapter.open();
		dbAdapter.updateNewItemstoActivityTable(ItemIDs, ItemEndDates);
		dbAdapter.close();
	}

	/**
	 * 
	 * @return the size of the activity table (the number of the rows)
	 */
	public synchronized int getSizeOfTable(){
		dbAdapter.open();
		int count=dbAdapter.fetchSizeOfRow();
		dbAdapter.close();
		return count;
	}

	/**
	 * Update un-posted items 
	 * @param itemIDs row Id
	 * @param itemNames activity name
	 * @param itemDates activity date
	 * @param isChecked sent item state
	 */
	public synchronized void updateUncheckedItems(long ItemIDs, String ItemNames, String ItemStartDates, String ItemEndDates, int isChecked){
		dbAdapter.open();
		dbAdapter.updateItemsToActivityTable(ItemIDs, ItemNames, ItemStartDates, ItemEndDates, isChecked);
		dbAdapter.close();
	}
	
	/**
	 * Update un-posted items 
	 * @param itemIDs row Id
	 * @param itemNames activity name
	 * @param itemDates activity date
	 * @param isChecked sent item state
	 */
	public synchronized void updateUncheckedItems(ArrayList<String[]> items){
		dbAdapter.open();
		for(int i=0;i<items.size();i++){
			dbAdapter.updateItemsToActivityTable(Long.parseLong(items.get(i)[0]), items.get(i)[1], items.get(i)[2], items.get(i)[3], Integer.parseInt(items.get(i)[4]));
		}
		dbAdapter.close();
	}

	private void setUncheckedItemsSize(int size){
		this.size = size;
	}

	private void setTodayItemsSize(int size){
		this.todaySize = size;
	}

	private void setFourHourItemsSize(int size){
		this.fourHourSize = size;
	}

	private void setHourItemsSize(int size){
		this.hourSize = size;
	}

	private void seperateItems(ArrayList<String> uncheckedItems){
		if(!itemIDs.isEmpty()){
			itemIDs.clear();
			itemNames.clear();
			itemStartDates.clear();
			itemEndDates.clear();
		}
		for(int i = 0; i < uncheckedItems.size(); i++){
			String[] line = uncheckedItems.get(i).split(",");
			itemIDs.add(Integer.parseInt(line[0]));
			itemNames.add(line[1]);
			itemStartDates.add(line[2]);
			itemEndDates.add(line[3]);
		}
	}
	private void seperateItems(ArrayList<ArrayList<String>> uncheckedItems,int size){
		if(!todayItemIDs.isEmpty()){
			todayItemIDs.clear();
			todayItemNames.clear();
			todayItemStartDates.clear();
			todayItemEndDates.clear();
		}
		for(int j = 0; j < uncheckedItems.get(0).size(); j++){
			String[] line = uncheckedItems.get(0).get(j).split(",");
			todayItemIDs.add(Integer.parseInt(line[0]));
			todayItemNames.add(line[1]);
			todayItemStartDates.add(line[2]);
			todayItemEndDates.add(line[3]);
		}

		if(!fourHourItemIDs.isEmpty()){
			fourHourItemIDs.clear();
			fourHourItemNames.clear();
			fourHourItemStartDates.clear();
			fourHourItemEndDates.clear();
		}
		for(int j = 0; j < uncheckedItems.get(1).size(); j++){
			String[] line = uncheckedItems.get(1).get(j).split(",");
			fourHourItemIDs.add(Integer.parseInt(line[0]));
			fourHourItemNames.add(line[1]);
			fourHourItemStartDates.add(line[2]);
			fourHourItemEndDates.add(line[3]);
		}

		if(!hourItemIDs.isEmpty()){
			hourItemIDs.clear();
			hourItemNames.clear();
			hourItemStartDates.clear();
			hourItemEndDates.clear();
		}
		for(int j = 0; j < uncheckedItems.get(2).size(); j++){
			String[] line = uncheckedItems.get(2).get(j).split(",");
			hourItemIDs.add(Integer.parseInt(line[0]));
			hourItemNames.add(line[1]);
			hourItemStartDates.add(line[2]);
			hourItemEndDates.add(line[3]);
		}
	}

	/**
	 * Get un-posted row IDs in the database
	 * @return Integer type ArrayList of row IDs in the database  
	 */
	public ArrayList<Integer> getUncheckedItemIDs(){
		return itemIDs;
	}

	/**
	 * Get un-posted activity names in the database
	 * @return String type ArrayList of activity names
	 */
	public ArrayList<String> getUncheckedItemNames(){
		return itemNames;
	}

	/**
	 * Get un-posted activity start date
	 * @return String type ArrayList of activity dates
	 */
	public ArrayList<String> getUncheckedItemStartDates(){
		return itemStartDates;
	}

	/**
	 * Get un-posted activity end date
	 * @return String type ArrayList of activity dates
	 */
	public ArrayList<String> getUncheckedItemEndDates(){
		return itemEndDates;
	}

	/**
	 * Get the array size of un-posted items
	 * @return the array size of un-posted items
	 */
	public int getUncheckedItemsSize(){
		return size;
	}


	public ArrayList<Integer> getTodayItemIDs(){
		return todayItemIDs;
	}

	public ArrayList<String> getTodayItemNames(){
		return todayItemNames;
	}

	public ArrayList<String> getTodayItemStartDates(){
		return todayItemStartDates;
	}

	public ArrayList<String> getTodayItemEndDates(){
		return todayItemEndDates;
	}

	public int getTodayItemsSize(){
		return todaySize;
	}


	public ArrayList<Integer> getFourHourItemIDs(){
		return fourHourItemIDs;
	}

	public ArrayList<String> getFourHourItemNames(){
		return fourHourItemNames;
	}

	public ArrayList<String> getFourHourItemStartDates(){
		return fourHourItemStartDates;
	}

	public ArrayList<String> getFourHourItemEndDates(){
		return fourHourItemEndDates;
	}

	public int getFourHourItemsSize(){
		return fourHourSize;
	}


	public ArrayList<Integer> getHourItemIDs(){
		return hourItemIDs;
	}

	public ArrayList<String> getHourItemNames(){
		return hourItemNames;
	}

	public ArrayList<String> getHourItemStartDates(){
		return hourItemStartDates;
	}

	public ArrayList<String> getHourItemEndDates(){
		return hourItemEndDates;
	}

	public int getHourItemsSize(){
		return hourSize;
	}

}
