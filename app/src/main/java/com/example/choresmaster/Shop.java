package com.example.choresmaster;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Shop extends AppCompatActivity {

    private DBHelper dbHelper;
    private LinearLayout shopItemsLayout;
    private TextView coinTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        dbHelper = new DBHelper(this);
        shopItemsLayout = findViewById(R.id.shopItemsLayout);
        coinTextView = findViewById(R.id.coinTextView);

        updateUserCoins();
        loadShopItems();  // Load shop items on start
    }

    private void updateUserCoins() {
        Cursor userCursor = dbHelper.getUser();
        if (userCursor.moveToFirst()) {
            int coins = userCursor.getInt(userCursor.getColumnIndexOrThrow("coins"));
            coinTextView.setText("Coins: " + coins);
        }
        userCursor.close();
    }

    private void loadShopItems() {
        shopItemsLayout.removeAllViews();

        Cursor cursor = dbHelper.getItems();
        while (cursor.moveToNext()) {
            String itemName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"));
            int cost = cursor.getInt(cursor.getColumnIndexOrThrow("cost"));
            int itemId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));

            int imageResId = getResources().getIdentifier(imageUri, "drawable", getPackageName());
            if (imageResId == 0) {
                imageResId = android.R.color.darker_gray; // fallback image
            }

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(10, 10, 10, 10);

            ImageView itemImage = new ImageView(this);
            itemImage.setImageResource(imageResId);
            itemImage.setLayoutParams(new LinearLayout.LayoutParams(150, 150));
            itemLayout.addView(itemImage);

            TextView itemText = new TextView(this);
            itemText.setText(itemName + "\nCost: " + cost + " coins");
            itemText.setPadding(20, 0, 20, 0);
            itemLayout.addView(itemText);

            Button actionButton = new Button(this);

            if (dbHelper.ownsItem(itemId)) {
                actionButton.setText("Equip");
                actionButton.setOnClickListener(v -> {
                    boolean equipped = dbHelper.equipItem(itemId);
                    if (equipped) {
                        Toast.makeText(this, itemName + " equipped!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);  // Notify CharacterScreen to refresh on return
                        loadShopItems(); // Refresh shop UI (Equip/Buy buttons)
                    } else {
                        Toast.makeText(this, "Failed to equip item", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                actionButton.setText("Buy");
                actionButton.setOnClickListener(v -> {
                    boolean success = dbHelper.buyItem(itemId, cost);
                    if (success) {
                        Toast.makeText(this, "Item bought!", Toast.LENGTH_SHORT).show();
                        updateUserCoins(); // Refresh coin display
                        loadShopItems(); // Refresh shop UI (now can Equip)
                    } else {
                        Toast.makeText(this, "Not enough coins or already owned!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            itemLayout.addView(actionButton);

            shopItemsLayout.addView
                    (itemLayout);
        }
        cursor.close();
    }
}