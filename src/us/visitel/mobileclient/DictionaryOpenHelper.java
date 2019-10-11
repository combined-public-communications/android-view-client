package us.visitel.mobileclient;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class DictionaryOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DICTIONARY_TABLE_NAME = "dictionary";
    private static final String DATABASE_NAME = "visitel";
    private static final String KEY_WORD = "keyword";
    private static final String KEY_DEFINITION = "keyvalue";

    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + DICTIONARY_TABLE_NAME + " (" +
                    KEY_WORD + " TEXT, " +
                    KEY_DEFINITION + " TEXT);";

    DictionaryOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

    public String get(String id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(DICTIONARY_TABLE_NAME,
                new String[]{KEY_DEFINITION}, KEY_WORD + "=?",
                new String[]{id}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String value = c.getString(0);
            c.close();
            return value;
        }
        c.close();
        return null;
    }

    public void insert(String id, String name) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO " + DICTIONARY_TABLE_NAME +
                "('" + KEY_WORD + "', '" + KEY_DEFINITION + "') values ('"
                + id + "', '"
                + name + "')");
    }

    /**
     * Wipe out the DB
     */
    public void delete(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DICTIONARY_TABLE_NAME, KEY_WORD + "=?", new String[]{id});
    }
}