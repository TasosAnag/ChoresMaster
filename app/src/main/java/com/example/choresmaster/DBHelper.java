package com.example.choresmaster;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "choresMaster.db";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE User (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "gender TEXT, " +
                "level INTEGER DEFAULT 1, " +
                "xp INTEGER DEFAULT 0, " +
                "coins INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE Chores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "description TEXT, " +
                "isCompleted INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE Items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "imageUri TEXT)");

        db.execSQL("CREATE TABLE Inventory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER, " +
                "itemId INTEGER, " +
                "FOREIGN KEY(userId) REFERENCES User(id), " +
                "FOREIGN KEY(itemId) REFERENCES Items(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Inventory");
        db.execSQL("DROP TABLE IF EXISTS Items");
        db.execSQL("DROP TABLE IF EXISTS Chores");
        db.execSQL("DROP TABLE IF EXISTS User");
        onCreate(db);
    }

    // Add a new user
    public void insertUser(String name, String gender) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("gender", gender);
        values.put("level", 1);
        values.put("xp", 0);
        values.put("coins", 0);
        db.insert("user", null, values);
    }


    // Add a chore
    public long addChore(String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("description", description);
            values.put("isCompleted", 0); // Explicitly set default
            long id = db.insert("Chores", null, values);
            db.close();
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Mark a chore as completed and award XP/Coins
    public void completeChore(int choreId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Mark as completed
        ContentValues values = new ContentValues();
        values.put("isCompleted", 1);
        db.update("Chores", values, "id=?", new String[]{String.valueOf(choreId)});

        // Update XP and coins
        Cursor cursor = db.rawQuery("SELECT xp, coins, level FROM User WHERE id=1", null);
        if (cursor.moveToFirst()) {
            int currentXP = cursor.getInt(0);
            int coins = cursor.getInt(1);
            int currentLevel = cursor.getInt(2);

            // Add XP and coins
            currentXP += 25;
            coins += 10;

            // Calculate new level based on total XP (100 XP per level)
            int newLevel = 1 + (currentXP / 100);

            ContentValues userValues = new ContentValues();
            userValues.put("xp", currentXP); // Store total XP, not remainder
            userValues.put("coins", coins);
            userValues.put("level", newLevel);

            db.update("User", userValues, "id=?", new String[]{"1"});
        }
        cursor.close();
    }


    public Cursor getUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM user LIMIT 1", null);
    }

    public Cursor getAllChores() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Chores", null);
    }

    public Cursor getInventory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Inventory", null);
    }

    public Cursor getItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Items", null);
    }


}
