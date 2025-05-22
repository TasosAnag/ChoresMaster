package com.example.choresmaster;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;




public class MainActivity extends AppCompatActivity {

    private ImageView imageMale, imageFemale;
    private EditText editTextName;
    private String selectedGender = null; // "Male" or "Female"
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageMale = findViewById(R.id.imageMale);
        imageFemale = findViewById(R.id.imageFemale);
        editTextName = findViewById(R.id.editTextName);
        Button btnStart = findViewById(R.id.btnStart);
        dbHelper = new DBHelper(this);

        imageMale.setOnClickListener(v -> {
            selectedGender = "Male";
            imageMale.setAlpha(1f);
            imageFemale.setAlpha(0.5f);
        });

        imageFemale.setOnClickListener(v -> {
            selectedGender = "Female";
            imageFemale.setAlpha(1f);
            imageMale.setAlpha(0.5f);
        });

        btnStart.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();

            if (name.isEmpty() || selectedGender == null) {
                Toast.makeText(this, "Please enter name and select a character", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.addUser(name, selectedGender);

            Intent intent = new Intent(MainActivity.this, CharacterScreen.class);
            startActivity(intent);
            finish();
        });
    }
}
