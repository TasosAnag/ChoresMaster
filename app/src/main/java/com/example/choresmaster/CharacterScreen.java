package com.example.choresmaster;

import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CharacterScreen extends AppCompatActivity {

    private ImageView imageViewCharacter;
    private TextView textViewNameLevel;
    private ProgressBar progressBarXP;
    private LinearLayout choresContainer;
    private Button buttonAddChore;
    private DBHelper dbHelper;
    private String userName;
    private int userId, level, xp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_screen);

        // Initialize views
        imageViewCharacter = findViewById(R.id.imageViewCharacter);
        textViewNameLevel = findViewById(R.id.textViewNameLevel);
        progressBarXP = findViewById(R.id.progressBarXP);
        choresContainer = findViewById(R.id.choresContainer);
        buttonAddChore = findViewById(R.id.buttonAddChore);
        dbHelper = new DBHelper(this);

        loadUser();
        loadChores();

        buttonAddChore.setOnClickListener(v -> showAddChoreDialog());
    }

    private void loadUser() {
        try (Cursor cursor = dbHelper.getUser()) {
            if (cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                userName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                level = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
                xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"));

                textViewNameLevel.setText(userName + " - Level " + level);
                progressBarXP.setProgress(xp % 100);

                int characterResource = gender.equalsIgnoreCase("Male")
                        ? R.drawable.male_character
                        : R.drawable.female_character;
                imageViewCharacter.setImageResource(characterResource);
            }
        }
    }

    private void loadChores() {
        choresContainer.removeAllViews();
        try (Cursor chores = dbHelper.getAllChores()) {
            while (chores.moveToNext()) {
                int choreId = chores.getInt(chores.getColumnIndexOrThrow("id"));
                String description = chores.getString(chores.getColumnIndexOrThrow("description"));
                int isCompleted = chores.getInt(chores.getColumnIndexOrThrow("isCompleted"));

                addChoreToUI(choreId, description, isCompleted == 1);
            }
        }
    }

    private void addChoreToUI(int choreId, String description, boolean isCompleted) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(description);
        checkBox.setChecked(isCompleted);
        checkBox.setEnabled(!isCompleted);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                new Thread(() -> {
                    dbHelper.completeChore(choreId);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "+25 XP +10 Coins", Toast.LENGTH_SHORT).show();
                        refreshXP();
                        checkBox.setEnabled(false);
                    });
                }).start();
            }
        });

        choresContainer.addView(checkBox);
    }

    private void refreshXP() {
        try (Cursor cursor = dbHelper.getUser()) {
            if (cursor.moveToFirst()) {
                xp = cursor.getInt(cursor.getColumnIndexOrThrow("xp"));
                // Calculate level based on XP (assuming 100 XP per level)
                level = xp / 100 + 1;
                progressBarXP.setProgress(xp % 100);
                textViewNameLevel.setText(userName + " - Level " + level);

                // Update the level in database if it's changed
                if (level != cursor.getInt(cursor.getColumnIndexOrThrow("level"))) {
                    new Thread(() -> dbHelper.completeChore(level)).start();
                }
            }
        }
    }

    private void showAddChoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Chore");

        final EditText input = new EditText(this);
        input.setHint("Enter chore description");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String choreDescription = input.getText().toString().trim();
            if (!choreDescription.isEmpty()) {
                addNewChore(choreDescription);
            } else {
                Toast.makeText(this, "Description cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addNewChore(String description) {
        new Thread(() -> {
            long result = dbHelper.addChore(description);
            runOnUiThread(() -> {
                if (result != -1) {
                    addChoreToUI((int) result, description, false);
                    Toast.makeText(this, "Chore added!", Toast.LENGTH_SHORT).show();

                    // Auto-scroll to show new chore
                    ScrollView scrollView = (ScrollView) choresContainer.getParent();
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                } else {
                    Toast.makeText(this, "Failed to add chore", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}