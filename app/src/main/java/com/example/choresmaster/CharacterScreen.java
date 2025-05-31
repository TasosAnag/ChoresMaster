package com.example.choresmaster;

import android.content.Intent;
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

    private static final int REQUEST_CODE_SHOP = 1;

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

        imageViewCharacter = findViewById(R.id.imageViewCharacter);
        textViewNameLevel = findViewById(R.id.textViewNameLevel);
        progressBarXP = findViewById(R.id.progressBarXP);
        choresContainer = findViewById(R.id.choresContainer);
        buttonAddChore = findViewById(R.id.buttonAddChore);
        dbHelper = new DBHelper(this);

        loadUser();
        loadChores();
        loadEquippedItems();  // Load equipped items on start

        buttonAddChore.setOnClickListener(v -> showAddChoreDialog());

        Button buttonOpenShop = findViewById(R.id.buttonOpenShop);
        buttonOpenShop.setOnClickListener(v -> {
            Intent intent = new Intent(CharacterScreen.this, Shop.class);
            startActivityForResult(intent, REQUEST_CODE_SHOP);  // Start shop for result
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SHOP && resultCode == RESULT_OK) {
            loadEquippedItems();  // Refresh equipped items immediately after returning from shop
        }
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
                int currentLevelInDb = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
                level = xp / 100 + 1;
                progressBarXP.setProgress(xp % 100);
                textViewNameLevel.setText(userName + " - Level " + level);

                if (level != currentLevelInDb) {
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

                    ScrollView scrollView = (ScrollView) choresContainer.getParent();
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                } else {
                    Toast.makeText(this, "Failed to add chore", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Load equipped items into their respective slots
    private void loadEquippedItems() {
        ImageView slotWeapon = findViewById(R.id.itemSlot1);
        ImageView slotHeadgear = findViewById(R.id.itemSlot2);
        ImageView slotConsumable = findViewById(R.id.itemSlot3);
        ImageView slotPet = findViewById(R.id.itemSlot4);

        // Clear slots first
        slotWeapon.setImageResource(android.R.color.transparent);
        slotHeadgear.setImageResource(android.R.color.transparent);
        slotConsumable.setImageResource(android.R.color.transparent);
        slotPet.setImageResource(android.R.color.transparent);

        try (Cursor cursor = dbHelper.getEquippedItems()) {
            while (cursor.moveToNext()) {
                String slot = cursor.getString(cursor.getColumnIndexOrThrow("slot"));
                String imageName = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"));

                int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
                if (imageResId == 0) {
                    imageResId = android.R.color.darker_gray;  // fallback image
                }

                switch (slot) {
                    case "weapon":
                        slotWeapon.setImageResource(imageResId);
                        break;
                    case "headgear":
                        slotHeadgear.setImageResource(imageResId);
                        break;
                    case "consumable":
                        slotConsumable.setImageResource(imageResId);
                        break;
                    case "pet":
                        slotPet.setImageResource(imageResId);
                        break;
                }
            }
        }
    }
}
