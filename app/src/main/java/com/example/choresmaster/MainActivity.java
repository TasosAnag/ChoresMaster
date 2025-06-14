package com.example.choresmaster;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This activity handles the initial launch of the app
 * and determines which activity to start.
 */
public class MainActivity extends AppCompatActivity {

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);

        Cursor cursor = dbHelper.getUser();
        Intent intent;

        if (cursor.moveToFirst()) {
            // User exists, go to Character Screen
            intent = new Intent(this, CharacterScreen.class);
        } else {
            // No user, go to Character Selection
            intent = new Intent(this, CharacterSelectionActivity.class);
        }

        cursor.close();
        startActivity(intent);
        finish(); // Close this activity
    }
}
