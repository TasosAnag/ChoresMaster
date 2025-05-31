package com.example.choresmaster;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "choresMaster.db";
    private static final int DATABASE_VERSION = 2;

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
                "imageUri TEXT, " +
                "cost INTEGER DEFAULT 0, " +
                "slot TEXT)");

        db.execSQL("CREATE TABLE Inventory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER, " +
                "itemId INTEGER, " +
                "FOREIGN KEY(userId) REFERENCES User(id), " +
                "FOREIGN KEY(itemId) REFERENCES Items(id))");

        db.execSQL("CREATE TABLE Equipped (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER, " +
                "itemId INTEGER, " +
                "slot TEXT, " +
                "FOREIGN KEY(userId) REFERENCES User(id), " +
                "FOREIGN KEY(itemId) REFERENCES Items(id))");

        insertInitialItems(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Equipped");
        db.execSQL("DROP TABLE IF EXISTS Inventory");
        db.execSQL("DROP TABLE IF EXISTS Items");
        db.execSQL("DROP TABLE IF EXISTS Chores");
        db.execSQL("DROP TABLE IF EXISTS User");
        onCreate(db);
    }

    public void insertUser(String name, String gender) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("gender", gender);
        values.put("level", 1);
        values.put("xp", 0);
        values.put("coins", 0);
        db.insert("User", null, values);
    }

    public long addChore(String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", description);
        values.put("isCompleted", 0);
        return db.insert("Chores", null, values);
    }

    public void completeChore(int choreId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues choreValues = new ContentValues();
        choreValues.put("isCompleted", 1);
        db.update("Chores", choreValues, "id=?", new String[]{String.valueOf(choreId)});

        Cursor cursor = db.rawQuery("SELECT xp, coins, level FROM User WHERE id=1", null);
        if (cursor.moveToFirst()) {
            int currentXP = cursor.getInt(0);
            int coins = cursor.getInt(1);
            int currentLevel = cursor.getInt(2);

            currentXP += 25;
            coins += 10;
            int newLevel = 1 + (currentXP / 100);

            ContentValues userValues = new ContentValues();
            userValues.put("xp", currentXP);
            userValues.put("coins", coins);
            userValues.put("level", newLevel);

            db.update("User", userValues, "id=?", new String[]{"1"});
        }
        cursor.close();
    }

    public Cursor getUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM User LIMIT 1", null);
    }

    public Cursor getAllChores() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Chores", null);
    }

    public Cursor getItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Items", null);
    }

    public Cursor getInventory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Inventory WHERE userId=1", null);
    }

    public boolean ownsItem(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM Inventory WHERE userId=1 AND itemId=?",
                new String[]{String.valueOf(itemId)}
        );
        boolean owned = cursor.moveToFirst();
        cursor.close();
        return owned;
    }

    public boolean buyItem(int itemId, int cost) {
        if (ownsItem(itemId)) return false; // Prevent buying duplicate

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT coins FROM User WHERE id=1", null);
        if (cursor.moveToFirst()) {
            int coins = cursor.getInt(0);
            if (coins >= cost) {
                ContentValues values = new ContentValues();
                values.put("coins", coins - cost);
                db.update("User", values, "id=?", new String[]{"1"});

                ContentValues invValues = new ContentValues();
                invValues.put("userId", 1);
                invValues.put("itemId", itemId);
                db.insert("Inventory", null, invValues);

                cursor.close();
                return true;
            }
        }
        cursor.close();
        return false;
    }

    public boolean isItemEquipped(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM Equipped WHERE userId=1 AND itemId=?",
                new String[]{String.valueOf(itemId)}
        );
        boolean equipped = cursor.moveToFirst();
        cursor.close();
        return equipped;
    }

    public boolean equipItem(int itemId) {
        if (!ownsItem(itemId)) return false; // Must own item to equip

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT slot FROM Items WHERE id=?", new String[]{String.valueOf(itemId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        String slot = cursor.getString(0);
        cursor.close();

        db.beginTransaction();
        try {
            db.delete("Equipped", "userId=? AND slot=?", new String[]{"1", slot});

            ContentValues values = new ContentValues();
            values.put("userId", 1);
            values.put("itemId", itemId);
            values.put("slot", slot);
            db.insert("Equipped", null, values);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return true;
    }

    public Cursor getEquippedItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT Equipped.slot, Items.imageUri FROM Equipped " +
                        "JOIN Items ON Equipped.itemId = Items.id " +
                        "WHERE Equipped.userId = 1", null);
    }

    private void insertInitialItems(SQLiteDatabase db) {
        insertItem(db, "Sword", "sword1", 100, "weapon");
        insertItem(db, "Helmet", "helmet3", 75, "headgear");
        insertItem(db, "Potion", "potion1", 30, "consumable");
        insertItem(db, "Sword 3", "sword3", 150, "weapon");
        insertItem(db, "Potion 2", "potion2", 50, "consumable");
        insertItem(db, "Potion 3", "potion3", 70, "consumable");
        insertItem(db, "Pet", "pet1", 200, "pet");
    }

    private void insertItem(SQLiteDatabase db, String name, String imageName, int cost, String slot) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("imageUri", imageName);
        values.put("cost", cost);
        values.put("slot", slot);
        db.insert("Items", null, values);
    }
}
