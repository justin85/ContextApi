package activity.classifier.db;

import android.database.sqlite.SQLiteDatabase;

interface DbTableAdapter {
	
	//	used by the DbAdapter when creating the table
	//		or dropping the table is needed
	void createTable(SQLiteDatabase database);
	void dropTable(SQLiteDatabase database);

	boolean init(SQLiteDatabase database);
	void done();
	
}
