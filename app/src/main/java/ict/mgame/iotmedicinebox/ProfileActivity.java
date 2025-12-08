package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREFS_NAME = "MedTrackerPrefs";
    private static final String TAG = "ProfileActivity";

    // UI Components
    private ImageButton btnBack, btnSave, btnEditPhoto;
    private CircleImageView ivProfilePicture;
    private EditText etFullName, etPhoneNumber, etAllergies, etMedicalConditions;
    private EditText etEmergencyName, etEmergencyRelationship, etEmergencyPhone;
    private LinearLayout layoutDateOfBirth;
    private TextView tvDateOfBirth;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private Spinner spinnerBloodType;
    private SwitchCompat switchMedicationReminders, switchRefillAlerts, switchMissedDoseAlerts;
    private Button btnSaveProfile;
    private BottomNavigationView bottomNav;

    private Calendar selectedDate;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "ProfileActivity created");

        initializeViews();
        setupBloodTypeSpinner();
        setupDatePicker();
        setupListeners();
        loadSavedData();
        setupBottomNavigation();
    }

    private void initializeViews() {
        try {
            // Header buttons
            btnBack = findViewById(R.id.btnBack);
            btnSave = findViewById(R.id.btnSave);

            // Profile picture
            ivProfilePicture = findViewById(R.id.ivProfilePicture);
            btnEditPhoto = findViewById(R.id.btnEditPhoto);

            // Personal information
            etFullName = findViewById(R.id.etFullName);
            layoutDateOfBirth = findViewById(R.id.layoutDateOfBirth);
            tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
            rgGender = findViewById(R.id.rgGender);
            rbMale = findViewById(R.id.rbMale);
            rbFemale = findViewById(R.id.rbFemale);
            rbOther = findViewById(R.id.rbOther);
            etPhoneNumber = findViewById(R.id.etPhoneNumber);

            // Medical information
            spinnerBloodType = findViewById(R.id.spinnerBloodType);
            etAllergies = findViewById(R.id.etAllergies);
            etMedicalConditions = findViewById(R.id.etMedicalConditions);

            // Emergency contact
            etEmergencyName = findViewById(R.id.etEmergencyName);
            etEmergencyRelationship = findViewById(R.id.etEmergencyRelationship);
            etEmergencyPhone = findViewById(R.id.etEmergencyPhone);

            // Notification preferences
            switchMedicationReminders = findViewById(R.id.switchMedicationReminders);
            switchRefillAlerts = findViewById(R.id.switchRefillAlerts);
            switchMissedDoseAlerts = findViewById(R.id.switchMissedDoseAlerts);

            // Save button
            btnSaveProfile = findViewById(R.id.btnSaveProfile);

            // Bottom navigation - try multiple methods
            bottomNav = findViewById(R.id.bottomNavigationView);

            if (bottomNav == null) {
                View bottomNavView = findViewById(R.id.bottomNavInclude);
                if (bottomNavView != null) {
                    bottomNav = bottomNavView.findViewById(R.id.bottomNavigationView);
                }
            }

            if (bottomNav == null) {
                Log.d(TAG, "Starting recursive search for bottom nav");
                View rootView = findViewById(android.R.id.content).getRootView();
                bottomNav = findBottomNavRecursive(rootView);
            }

            Log.d(TAG, "Bottom nav found: " + (bottomNav != null));

            selectedDate = Calendar.getInstance();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
        }
    }

    // Recursively find BottomNavigationView
    private BottomNavigationView findBottomNavRecursive(View view) {
        if (view instanceof BottomNavigationView) {
            return (BottomNavigationView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                BottomNavigationView result = findBottomNavRecursive(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void setupBloodTypeSpinner() {
        String[] bloodTypes = {"Select blood type", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                bloodTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(adapter);
    }

    private void setupDatePicker() {
        layoutDateOfBirth.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    tvDateOfBirth.setText(sdf.format(selectedDate.getTime()));
                    tvDateOfBirth.setTextColor(getResources().getColor(android.R.color.black));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Set minimum date to 120 years ago
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -120);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Ask to save before leaving
                showExitConfirmationDialog();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> saveProfile());
        }

        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> openImagePicker());
        }

        if (ivProfilePicture != null) {
            ivProfilePicture.setOnClickListener(v -> openImagePicker());
        }
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save your changes before leaving?")
                .setPositiveButton("Save", (dialog, which) -> {
                    saveProfile();
                    navigateToHome();
                })
                .setNegativeButton("Discard", (dialog, which) -> navigateToHome())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openImagePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Photo");
        builder.setItems(new String[]{"Take Photo", "Choose from Gallery", "Remove Photo"},
                (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Take photo
                            Toast.makeText(this, "Camera feature coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            // Choose from gallery
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, PICK_IMAGE_REQUEST);
                            break;
                        case 2:
                            // Remove photo
                            ivProfilePicture.setImageResource(R.drawable.ic_default_profile);
                            selectedImageUri = null;
                            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ivProfilePicture.setImageBitmap(bitmap);
                Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfile() {
        // Validate required fields
        String fullName = etFullName.getText().toString().trim();
        String dateOfBirth = tvDateOfBirth.getText().toString();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateOfBirth.equals("Select date")) {
            Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show();
            layoutDateOfBirth.requestFocus();
            return;
        }

        // Get selected gender
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedGenderId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedGenderId);
            gender = selectedRadioButton.getText().toString();
        }

        // Save to SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Personal information
        editor.putString("fullName", fullName);
        editor.putString("dateOfBirth", dateOfBirth);
        editor.putString("gender", gender);
        editor.putString("phoneNumber", etPhoneNumber.getText().toString().trim());

        // Medical information
        editor.putInt("bloodTypePosition", spinnerBloodType.getSelectedItemPosition());
        editor.putString("bloodType", spinnerBloodType.getSelectedItem().toString());
        editor.putString("allergies", etAllergies.getText().toString().trim());
        editor.putString("medicalConditions", etMedicalConditions.getText().toString().trim());

        // Emergency contact
        editor.putString("emergencyName", etEmergencyName.getText().toString().trim());
        editor.putString("emergencyRelationship", etEmergencyRelationship.getText().toString().trim());
        editor.putString("emergencyPhone", etEmergencyPhone.getText().toString().trim());

        // Notification preferences
        editor.putBoolean("medicationReminders", switchMedicationReminders.isChecked());
        editor.putBoolean("refillAlerts", switchRefillAlerts.isChecked());
        editor.putBoolean("missedDoseAlerts", switchMissedDoseAlerts.isChecked());

        // Profile picture URI
        if (selectedImageUri != null) {
            editor.putString("profileImageUri", selectedImageUri.toString());
        }

        // Mark profile as completed
        editor.putBoolean("profileCompleted", true);
        editor.putLong("profileLastUpdated", System.currentTimeMillis());

        boolean saved = editor.commit();

        if (saved) {
            Toast.makeText(this, "✓ Profile saved successfully", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Profile saved successfully");
        } else {
            Toast.makeText(this, "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to save profile");
        }
    }

    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Personal information
        String savedName = prefs.getString("fullName", "");
        if (!savedName.isEmpty()) {
            etFullName.setText(savedName);
        }

        String savedDate = prefs.getString("dateOfBirth", "");
        if (!savedDate.isEmpty()) {
            tvDateOfBirth.setText(savedDate);
            tvDateOfBirth.setTextColor(getResources().getColor(android.R.color.black));
        }

        String gender = prefs.getString("gender", "");
        switch (gender) {
            case "Male":
                rbMale.setChecked(true);
                break;
            case "Female":
                rbFemale.setChecked(true);
                break;
            case "Other":
                rbOther.setChecked(true);
                break;
        }

        etPhoneNumber.setText(prefs.getString("phoneNumber", ""));

        // Medical information
        spinnerBloodType.setSelection(prefs.getInt("bloodTypePosition", 0));
        etAllergies.setText(prefs.getString("allergies", ""));
        etMedicalConditions.setText(prefs.getString("medicalConditions", ""));

        // Emergency contact
        etEmergencyName.setText(prefs.getString("emergencyName", ""));
        etEmergencyRelationship.setText(prefs.getString("emergencyRelationship", ""));
        etEmergencyPhone.setText(prefs.getString("emergencyPhone", ""));

        // Notification preferences
        switchMedicationReminders.setChecked(prefs.getBoolean("medicationReminders", true));
        switchRefillAlerts.setChecked(prefs.getBoolean("refillAlerts", true));
        switchMissedDoseAlerts.setChecked(prefs.getBoolean("missedDoseAlerts", true));

        // Profile picture
        String imageUriString = prefs.getString("profileImageUri", "");
        if (!imageUriString.isEmpty()) {
            selectedImageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ivProfilePicture.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                // If loading fails, use default image
                ivProfilePicture.setImageResource(R.drawable.ic_default_profile);
            }
        }

        Log.d(TAG, "Loaded saved profile data");
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) {
            Log.e(TAG, "Bottom navigation not found!");
            return;
        }

        Log.d(TAG, "Setting up bottom navigation");

        // Set profile as selected
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            Log.d(TAG, "Bottom nav clicked: " + item.getTitle());

            if (itemId == R.id.nav_home) {
                navigateToActivity(HomeActivity.class);
                return true;

            } else if (itemId == R.id.nav_calendar) {
                navigateToActivity(TrackerActivity.class);
                return true;

            } else if (itemId == R.id.nav_add) {
                navigateToActivity(AddMedicineActivity.class);
                return true;

            } else if (itemId == R.id.nav_medbox) {
                navigateToActivity(MedBoxActivity.class);
                return true;

            } else if (itemId == R.id.nav_chat) {
                navigateToActivity(ChatActivity.class);
                return true;

            } else if (itemId == R.id.nav_profile) {
                // Already on profile page
                return true;
            }

            return false;
        });

        Log.d(TAG, "Bottom navigation setup complete");
    }

    private void navigateToActivity(Class<?> activityClass) {
        try {
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Cannot open activity: " + activityClass.getSimpleName(), e);
            Toast.makeText(this, "Cannot open page", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensure profile is selected when resuming
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        showExitConfirmationDialog();
    }
}