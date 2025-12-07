package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AddMedicineActivity extends Activity {

    private LinearLayout selectedTimesLayout;
    private List<String> selectedTimes = new ArrayList<>();
    private DatabaseHelper databaseHelper;
    private long medicationId = -1;
    private int selectedBoxNumber = -1;
    private String selectedBoxName = "";
    private EditText etMedName;
    private RadioGroup radioGroupFrequency;
    private EditText etInstructions;
    private Switch switchRefill;
    private BluetoothAdapter bluetoothAdapter;
    private LinearLayout llCustomSchedule;
    private EditText etCustomDays;
    private EditText etCustomTimes;

    // Box colors
    private static final int[] BOX_COLORS = {
            Color.parseColor("#FF6B6B"), // Box 1 - Red
            Color.parseColor("#4ECDC4"), // Box 2 - Teal
            Color.parseColor("#FFD166")  // Box 3 - Yellow
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        // Initialize database
        databaseHelper = new DatabaseHelper(this);

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get medication ID if editing existing
        Intent intent = getIntent();
        if (intent.hasExtra("medication_id")) {
            medicationId = intent.getLongExtra("medication_id", -1);
        }

        // Initialize views
        etMedName = findViewById(R.id.etMedName);
        radioGroupFrequency = findViewById(R.id.radioGroupFrequency);
        etInstructions = findViewById(R.id.etInstructions);
        switchRefill = findViewById(R.id.switchRefill);
        selectedTimesLayout = findViewById(R.id.selectedTimesLayout);
        llCustomSchedule = findViewById(R.id.llCustomSchedule);
        etCustomDays = findViewById(R.id.etCustomDays);
        etCustomTimes = findViewById(R.id.etCustomTimes);

        // Find FINISH button by ID or create if it doesn't exist
        ImageView btnFinish = findViewById(R.id.btnFinish);
        if (btnFinish == null) {
            // Add Finish button to top bar
            LinearLayout topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                btnFinish = new ImageView(this);
                btnFinish.setId(View.generateViewId());
                btnFinish.setImageResource(android.R.drawable.ic_menu_save);
                btnFinish.setContentDescription("Finish");
                btnFinish.setPadding(8, 8, 8, 8);
                btnFinish.setOnClickListener(v -> saveMedication());

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                topBar.addView(btnFinish, params);
            }
        } else {
            btnFinish.setOnClickListener(v -> saveMedication());
        }

        // Check for Bluetooth connection first
        checkBluetoothAndBoxes();

        // Setup frequency radio group listener
        radioGroupFrequency.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAsNeeded) {
                llCustomSchedule.setVisibility(View.VISIBLE);
            } else {
                llCustomSchedule.setVisibility(View.GONE);
            }
        });

        // Setup time selection
        findViewById(R.id.btnAddTime).setOnClickListener(v -> showTimePicker());

        // Add predefined times
        addPredefinedTimes();

        // BACK button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Load existing medication if editing
        if (medicationId != -1) {
            loadExistingMedication();
        }
    }

    private void checkBluetoothAndBoxes() {
        // Fixed: variable name should be lowercase bluetoothAdapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            showBluetoothAlert();
            return;
        }

        // Get paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<String> availableBoxes = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            if (deviceName != null && (deviceName.contains("HC-05") || deviceName.contains("HC-06"))) {
                // Extract box number from device name if possible
                String boxName = deviceName;
                if (deviceName.contains("Box")) {
                    // Fixed: regex pattern string needs quotes
                    Pattern pattern = Pattern.compile("Box\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(deviceName);
                    if (matcher.find()) {
                        boxName = "Box " + matcher.group(1); // Fixed: add space
                    }
                }
                availableBoxes.add(boxName);
            }
        }

        if (availableBoxes.isEmpty()) {
            showNoBoxesAlert();
        } else {
            showBoxSelectionDialog(availableBoxes);
        }
    }

    private void showBluetoothAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Bluetooth Required")
                .setMessage("Please enable Bluetooth to connect to MedBox devices.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showNoBoxesAlert() {
        new AlertDialog.Builder(this)
                .setTitle("No Boxes Found")
                .setMessage("No MedBox devices found. Please ensure:\n1. MedBox is powered on\n2. Bluetooth is paired in device settings\n3. Device name contains 'HC-05' or 'HC-06'")
                .setPositiveButton("Retry", (dialog, which) -> checkBluetoothAndBoxes())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showBoxSelectionDialog(List<String> availableBoxes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Medicine Box");
        builder.setMessage("Choose which box to store this medication in:");

        String[] boxesArray = availableBoxes.toArray(new String[0]);
        builder.setItems(boxesArray, (dialog, which) -> {
            String selectedBox = boxesArray[which];
            selectedBoxName = selectedBox;

            // Extract box number from name
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(selectedBox);
            if (matcher.find()) {
                selectedBoxNumber = Integer.parseInt(matcher.group());
            } else {
                // Default to box 1 if no number found
                selectedBoxNumber = 1;
            }

            // Update medicine name with box info
            etMedName.setText("Box " + selectedBoxNumber + " Medicine");

            // Check if box already has medication
            if (databaseHelper.hasMedicationInBox(selectedBoxNumber)) {
                showBoxAlreadyHasMedicineDialog();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void showBoxAlreadyHasMedicineDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Box Already Has Medicine")
                .setMessage("This box already contains medication. Do you want to replace it or add as additional medication?")
                .setPositiveButton("Replace", (dialog, which) -> {
                    // User will replace, continue
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void addPredefinedTimes() {
        String[] predefinedTimes = {"7:00 am", "8:00 am", "9:00 am", "12:00 pm", "8:00 pm"};
        for (String time : predefinedTimes) {
            if (!selectedTimes.contains(time)) {
                selectedTimes.add(time);
                addTimeChip(time);
            }
        }
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String amPm = hourOfDay >= 12 ? "pm" : "am";
            int hour = hourOfDay % 12;
            if (hour == 0) hour = 12;
            String time = String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, amPm);

            if (!selectedTimes.contains(time)) {
                selectedTimes.add(time);
                addTimeChip(time);
            }
        }, 12, 0, false).show();
    }

    private void addTimeChip(String time) {
        TextView chip = new TextView(this);
        chip.setText(time);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(16);
        chip.setPadding(36, 24, 36, 24);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(50);

        // Use box color for chips
        if (selectedBoxNumber > 0 && selectedBoxNumber <= BOX_COLORS.length) {
            shape.setColor(BOX_COLORS[selectedBoxNumber - 1]);
        } else {
            shape.setColor(Color.parseColor("#8D6E63"));
        }

        chip.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(12, 12, 12, 12);
        chip.setLayoutParams(params);

        // Add remove functionality
        chip.setOnClickListener(v -> {
            selectedTimes.remove(time);
            selectedTimesLayout.removeView(chip);
        });

        selectedTimesLayout.addView(chip);
    }

    private void loadExistingMedication() {
        // In a real implementation, load from database
        // For now, we'll just mark that we're editing
        Toast.makeText(this, "Editing existing medication", Toast.LENGTH_SHORT).show();
    }

    private void saveMedication() {
        // Validate inputs
        String medicineName = etMedName.getText().toString().trim();
        if (medicineName.isEmpty()) {
            showError("Please enter medicine name");
            return;
        }

        if (selectedBoxNumber == -1) {
            showError("Please select a box first");
            return;
        }

        if (selectedTimes.isEmpty()) {
            showError("Please select at least one notification time");
            return;
        }

        // Get frequency
        String frequency = "";
        int checkedId = radioGroupFrequency.getCheckedRadioButtonId();
        if (checkedId == R.id.rbOnce) {
            frequency = "Once a day";
        } else if (checkedId == R.id.rbTwice) {
            frequency = "Twice a day";
        } else if (checkedId == R.id.rbThree) {
            frequency = "Three times a day";
        } else if (checkedId == R.id.rbFour) {
            frequency = "Four times a day";
        } else if (checkedId == R.id.rbAsNeeded) {
            frequency = "As needed";
        }

        // Get custom schedule for "As needed"
        String days = "";
        String times = "";
        if (checkedId == R.id.rbAsNeeded) {
            days = etCustomDays.getText().toString().trim();
            times = etCustomTimes.getText().toString().trim();
            if (days.isEmpty() || times.isEmpty()) {
                showError("Please fill in days and times for 'As needed' schedule");
                return;
            }
        }

        // Get notification time (use first selected time)
        String notificationTime = selectedTimes.get(0);

        // Get instructions
        String instructions = etInstructions.getText().toString().trim();

        // Get refill reminder
        boolean notifyRefill = switchRefill.isChecked();

        // Create medication object
        Medication medication = new Medication();
        if (medicationId != -1) {
            medication.setId(medicationId);
        }
        medication.setBoxNumber(selectedBoxNumber);
        medication.setBoxName(selectedBoxName);
        medication.setMedicineName(medicineName);
        medication.setFrequency(frequency);
        medication.setDays(days);
        medication.setTimes(times);
        medication.setNotificationTime(notificationTime);
        medication.setInstructions(instructions);
        medication.setActive(true);

        // Save to database
        long id = databaseHelper.addMedication(medication);

        if (id != -1) {
            Toast.makeText(this, "Medication saved successfully!", Toast.LENGTH_LONG).show();

            // Also update box name in database
            if (!selectedBoxName.isEmpty()) {
                databaseHelper.updateBoxName(selectedBoxNumber, selectedBoxName);
            }

            // Return to home
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Failed to save medication", Toast.LENGTH_LONG).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) { // Bluetooth enable request
            if (resultCode == RESULT_OK) {
                checkBluetoothAndBoxes();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}
