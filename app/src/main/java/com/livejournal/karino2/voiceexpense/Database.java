package com.livejournal.karino2.voiceexpense;

import java.util.Date;
import java.util.Hashtable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class Database {
    private static final String DATABASE_NAME = "voice_expense.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ENTRY_TABLE_NAME = "entry";
    private static final String CATEGORY_TABLE_NAME = "category";
    private static final String BOOK_TABLE_NAME = "book";
    private static final String TAG = "VoiceExpenseDatabase";
	
	
    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + BOOK_TABLE_NAME + " ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "NAME TEXT"
                    + ");");
            db.execSQL("CREATE TABLE " + CATEGORY_TABLE_NAME + " ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "NAME TEXT"
                    + ");");
            db.execSQL("CREATE TABLE " + ENTRY_TABLE_NAME + " ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "DATE INTEGER,"
                    + "CATEGORY INTEGER,"
                    + "MEMO TEXT,"
                    + "PRICE INTEGER,"
                    + "BOOK INTEGER,"
					+ "UPDATEDATE INTEGER"
                    + ");");
            
    		ContentValues values = new ContentValues();
    		values.put("NAME", "Foods"); // add one entry for less special case.
            db.insert(CATEGORY_TABLE_NAME, null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            recreate(db);
        }

		public void recreate(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS " + BOOK_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + CATEGORY_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + ENTRY_TABLE_NAME);
            onCreate(db);
		}		
    }

    DatabaseHelper dbHelper;
    SQLiteDatabase database;
	public void open(Context context) {
    	dbHelper = new DatabaseHelper(context);
    	database = dbHelper.getWritableDatabase();
    }
	
	public void close() {
		dbHelper.close();
	}
	
	public void recreate() {
		dbHelper.recreate(database);
	}
	
	public int totalPrice(long bookId, Date from, Date until)
	{
		return totalPriceCommonGeneric("BOOK = ? AND DATE >= ? AND DATE < ?",
				new String[]{String.valueOf(bookId),
				String.valueOf(from.getTime()),
				String.valueOf(until.getTime())});
	}
	
	public Date earliest(long bookId)
	{
		Date ret = null;
		Cursor cursor = database.query(ENTRY_TABLE_NAME, new String[]{"MIN(DATE)"}, "BOOK = ?", new String[]{ String.valueOf(bookId)}, null, null, null);
		if(cursor.moveToFirst())
		{
			ret = new Date(cursor.getLong(0));
		}
		cursor.close();
		return ret;
	}

	int totalPriceCommonGeneric(String selection, String[] selectionArg) {
		int ret = 0;
		Cursor cursor = database.query(ENTRY_TABLE_NAME,
				new String[]{"SUM(PRICE)"}, selection, selectionArg, null, null, null, null);
		if(cursor.moveToFirst())
		{
			ret = cursor.getInt(0);
		}
		cursor.close();
		return ret;
	}

	public Cursor fetchAllEntry(long bookId) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		// use table name directly for simplicity.
		builder.setTables("entry LEFT OUTER JOIN category ON (entry.CATEGORY = category._id) ");
		
		return builder.query(database, new String[] {
		"entry._id AS _id", "DATE", "NAME", "MEMO", "PRICE"
		}, "book = ?", new String[] { String.valueOf(bookId) }, null, null, "DATE DESC, _id DESC");
	}
	
	public Cursor fetchBooksCursor() {
		return fetchNameTableCursor(BOOK_TABLE_NAME);
	}
	
	public Cursor fetchCategoriesCursor() {
		return fetchNameTableCursor(CATEGORY_TABLE_NAME);
	}
	
	public Hashtable<Long, String> fetchBooks() {
		return fetchNameTableMap(BOOK_TABLE_NAME);
	}
	
		
	public Hashtable<Long, String> fetchCategories() {
		return fetchNameTableMap(CATEGORY_TABLE_NAME);
	}
	
	public long newCategory(String name)
	{
		return insertNewName(name, CATEGORY_TABLE_NAME);
	}
	
	public long newBook(String name)
	{
		return insertNewName(name, BOOK_TABLE_NAME);
	}

	long insertNewName(String name, String tableName) {
		ContentValues values = new ContentValues();
		values.put("NAME", name);
		return database.insert(tableName, null, values);
	}
	
	
	public void insert(Entry entry) {
		ContentValues values = valuesFrom(entry);		
		database.insert(ENTRY_TABLE_NAME, null, values);
	}

	public void update(Entry entry) {
		ContentValues values = valuesFrom(entry);		
		database.update(ENTRY_TABLE_NAME, values, "_id = ?", new String[] { String.valueOf(entry.getId()) } );
	}
	
	ContentValues valuesFrom(Entry entry) {
		ContentValues values = new ContentValues();
		copyTo(entry, values);
		return values;
	}
	

	void copyTo(Entry entry, ContentValues values) {
		values.put("DATE", entry.getDate().getTime());
		values.put("CATEGORY", entry.getCategoryId());
		values.put("MEMO", entry.getMemo());
		values.put("PRICE", entry.getPrice());
		values.put("BOOK", entry.getBookId());
	}
	

	Hashtable<Long, String> fetchNameTableMap(String tableName) {
		Cursor cursor = fetchNameTableCursor(tableName);
		Hashtable<Long, String> map = new Hashtable<Long, String>();
		while(!cursor.isLast())
		{
			cursor.moveToNext();
			map.put(cursor.getLong(0), cursor.getString(1));
		}
		cursor.close();
		return map;
	}

	Cursor fetchNameTableCursor(String tableName) {
		Cursor cursor = database.query(true, tableName,
				new String[]{"_id", "NAME"}, null, null, null, null, null, null);
		return cursor;
	}

	public void deleteBook(long id) {
		database.delete(ENTRY_TABLE_NAME, "BOOK = ?", new String[]{ String.valueOf(id) });
		database.delete(BOOK_TABLE_NAME, "_id = ?", new String[] { String.valueOf(id)} );		
	}

	public Entry fetchEntry(long bookId, long entryId) {
		Cursor cursor = database.query(ENTRY_TABLE_NAME, new String[]{"_id",  "DATE", "CATEGORY", "MEMO", "PRICE" },
				"_id = ?", new String[] { String.valueOf(entryId) }, null, null, null);
		cursor.moveToFirst();
		Entry ent = new Entry(cursor.getLong(0),
				new Date(cursor.getLong(1)),
				cursor.getLong(2),
				cursor.getString(3),
				cursor.getInt(4),
				bookId);
		cursor.close();
		return ent;
	}

	public void deleteEntry(long entryId) {
		database.delete(ENTRY_TABLE_NAME, "_id = ?", new String[]{ String.valueOf(entryId) });
	}
	
	public static Date beforeDate(Date target, int minusDay) {
		long mil = target.getTime();
		return new Date(mil - ((long)minusDay)*24*60*60*1000);
	}

	public static int betweenDate(Date from, Date to) {
		// ex. from 11/13 to 11/14, expectation would be 2 because caller collect the entry of 11/13 and 11/14.
		return (int)((to.getTime()-from.getTime())/(24*60*60*1000))+1;
	}


}
