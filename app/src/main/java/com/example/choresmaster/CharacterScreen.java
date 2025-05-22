package com.example.choresmaster;


import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class CharacterScreen extends AppCompatActivity {

    private ImageView imageViewCharacter;
    private TextView textViewNameLevel;
    private ProgressBar progressBarXP;
    private LinearLayout choresContainer;

    private DBHelper dbHelper;
    private String userName;
    private int userId, level, xp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_screen);

        imageViewCharacter = findViewById(R.id.imageViewCharacter);
        textViewNameLevel = findViewById(R.id.textViewNameLevel);
        progressBarXP = findViewById(R.id.progressBarXP);
        choresContainer = findViewById(R.id.choresContainer);

        dbHelper = new DBHelper(this);
        loadUser();
        loadChores();
    }

    private void loadUser() {
        Cursor cursor = dbHelper.getUser();
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            userName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
            level = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
            xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"));

            textViewNameLevel.setText(userName + " - Level " + level);
            progressBarXP.setProgress(xp % 100); // Every 100 XP = level up

            if (gender.equalsIgnoreCase("Male")) {
                imageViewCharacter.setImageResource(R.drawable.male_character);
            } else {
                imageViewCharacter.setImageResource(R.drawable.female_character);
            }
        }
        cursor.close();
    }

    private void loadChores() {
        Cursor chores = dbHelper.getAllChores();
        while (chores.moveToNext()) {
            int choreId = chores.getInt(chores.getColumnIndexOrThrow("id"));
            String title = chores.getString(chores.getColumnIndexOrThrow("title"));

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(title);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    dbHelper.completeChore(choreId); // awards XP & coins
                    Toast.makeText(this, "+25 XP +10 Coins", Toast.LENGTH_SHORT).show();
                    refreshXP();
                    checkBox.setEnabled(false);
                }
            });
            choresContainer.addView(checkBox);
        }
        chores.close();
    }

    private void refreshXP() {
        Cursor cursor = dbHelper.getUser();
        if (cursor.moveToFirst()) {
            xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"));
            level = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
            progressBarXP.setProgress(xp % 100);
            textViewNameLevel.setText(userName + " - Level " + level);
        }
        cursor.close();
    }
}
