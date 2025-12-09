package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AddMedicineActivity extends Activity implements BluetoothService.BluetoothListener {

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
    private boolean isEditingExisting = false;
    private Button btnSaveBottom;

    // Bluetooth service
    private BluetoothService bluetoothService;
    private boolean isBound = false;
    private boolean isConnected = false;

    // Permission request codes
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1002;
    private static final int REQUEST_BLUETOOTH_CONNECT = 1003;

    // Box colors
    private static final int[] BOX_COLORS = {
            Color.parseColor("#FF6B6B"), // Box 1 - Red
            Color.parseColor("#4ECDC4"), // Box 2 - Teal
            Color.parseColor("#FFD166")  // Box 3 - Yellow
    };

    // Service connection
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            bluetoothService.setListener(AddMedicineActivity.this);
            isBound = true;

            Log.d("AddMedicineActivity", "Bluetooth service connected");

            // 自动搜索可用的MedBox设备
            if (checkBluetoothPermissions()) {
                scanForDevices();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            bluetoothService = null;
            Log.d("AddMedicineActivity", "Bluetooth service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        // Initialize database
        databaseHelper = new DatabaseHelper(this);

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Initialize views
        etMedName = findViewById(R.id.etMedName);
        radioGroupFrequency = findViewById(R.id.radioGroupFrequency);
        etInstructions = findViewById(R.id.etInstructions);
        switchRefill = findViewById(R.id.switchRefill);
        selectedTimesLayout = findViewById(R.id.selectedTimesLayout);
        llCustomSchedule = findViewById(R.id.llCustomSchedule);
        etCustomDays = findViewById(R.id.etCustomDays);
        etCustomTimes = findViewById(R.id.etCustomTimes);
        btnSaveBottom = findViewById(R.id.btnSaveBottom);

        // 设置底部保存按钮
        if (btnSaveBottom != null) {
            btnSaveBottom.setOnClickListener(v -> saveMedication());

            // 根据是编辑还是添加设置按钮文本
            if (isEditingExisting) {
                btnSaveBottom.setText("UPDATE MEDICATION");
            } else {
                btnSaveBottom.setText("SAVE MEDICATION");
            }
        }

        // 隐藏顶部保存按钮（如果存在）
        Button btnSaveTop = findViewById(R.id.btnSaveTop);
        if (btnSaveTop != null) {
            btnSaveTop.setVisibility(View.GONE);
        }

        // Get medication ID if editing existing
        Intent intent = getIntent();
        if (intent.hasExtra("medication_id")) {
            medicationId = intent.getLongExtra("medication_id", -1);
            isEditingExisting = true;

            // 如果是编辑现有药物，获取盒子信息
            if (intent.hasExtra("box_number")) {
                selectedBoxNumber = intent.getIntExtra("box_number", -1);
                if (selectedBoxNumber != -1) {
                    selectedBoxName = "Box " + selectedBoxNumber;

                    // 为编辑的药物预先设置提示
                    if (etMedName.getText().toString().trim().isEmpty()) {
                        etMedName.setHint("e.g. Medicine for " + selectedBoxName);
                    }
                }
            }
        }

        // Bind Bluetooth service
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

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

        // 如果是编辑现有药物，直接加载信息
        if (isEditingExisting) {
            loadExistingMedication();
        } else {
            // 对于新添加药物，延迟显示盒子选择对话框
            new Handler().postDelayed(() -> {
                if (checkBluetoothPermissions()) {
                    showDeviceSelectionDialog();
                }
            }, 500);
        }
    }

    // 新增：扫描设备方法
    private void scanForDevices() {
        if (bluetoothService != null) {
            List<BluetoothDevice> devices = bluetoothService.findAvailableMedBoxDevices();
            if (devices.isEmpty()) {
                Log.d("AddMedicineActivity", "No MedBox devices found.");
                // 如果没有找到设备，显示默认盒子选择
                showDefaultBoxSelection();
            }
        }
    }

    // 新增：显示默认盒子选择
    private void showDefaultBoxSelection() {
        List<String> defaultBoxes = new ArrayList<>();
        defaultBoxes.add("Box 1");
        defaultBoxes.add("Box 2");
        defaultBoxes.add("Box 3");

        showBoxSelectionDialog(defaultBoxes, false);
    }

    // 新增：显示设备选择对话框（类似MedBoxActivity）
    private void showDeviceSelectionDialog() {
        if (!checkBluetoothPermissions()) {
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            showToast("Please enable Bluetooth first");
            // 蓝牙未开启，显示默认盒子选择
            showDefaultBoxSelection();
            return;
        }

        // 使用Service的方法搜索设备
        if (bluetoothService != null) {
            List<BluetoothDevice> availableDevices = bluetoothService.findAvailableMedBoxDevices();

            if (availableDevices.isEmpty()) {
                showNoDevicesDialog();
                return;
            }

            // 创建设备列表
            List<String> deviceNames = new ArrayList<>();
            for (BluetoothDevice device : availableDevices) {
                String name = getDeviceNameSafely(device);
                String address = device.getAddress();
                deviceNames.add(name + " [" + address.substring(0, 8) + "...]");
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select MedBox Device");
            builder.setMessage("Choose which device to use for this medication:");

            String[] devicesArray = deviceNames.toArray(new String[0]);
            builder.setItems(devicesArray, (dialog, which) -> {
                BluetoothDevice selectedDevice = availableDevices.get(which);

                // 安全地获取设备名称
                String deviceName = getDeviceNameSafely(selectedDevice);

                showToast("Using " + deviceName + " for this medication");

                // 从设备名提取盒子信息
                if (deviceName != null && !deviceName.equals("Unknown Device")) {
                    // 尝试从设备名提取盒子号
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(deviceName);
                    if (matcher.find()) {
                        selectedBoxNumber = Integer.parseInt(matcher.group());
                        selectedBoxName = "Box " + selectedBoxNumber;
                    } else {
                        // 如果没有数字，使用默认盒子1
                        selectedBoxNumber = 1;
                        selectedBoxName = "Box 1";
                    }
                } else {
                    // 未知设备，使用默认盒子1
                    selectedBoxNumber = 1;
                    selectedBoxName = "Box 1";
                }

                // 更新提示
                if (etMedName.getText().toString().trim().isEmpty()) {
                    etMedName.setHint("e.g. Medicine for " + selectedBoxName);
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.setNeutralButton("Use Default Boxes", (dialog, which) -> {
                showDefaultBoxSelection();
            });

            builder.show();
        }
    }

    // 安全获取设备名称的方法
    private String getDeviceNameSafely(BluetoothDevice device) {
        String deviceName = "Unknown Device";
        try {
            // 在 Android 12+ 上需要 BLUETOOTH_CONNECT 权限
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                deviceName = device.getName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = "Unknown Device";
                }
            }
        } catch (SecurityException e) {
            Log.e("AddMedicineActivity", "Permission denied for Bluetooth device name", e);
            deviceName = "Device [" + device.getAddress().substring(0, 8) + "...]";
        }
        return deviceName;
    }

    // 新增：无设备对话框
    private void showNoDevicesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Devices Found");
        builder.setMessage("No MedBox devices found. Please ensure:\n\n" +
                "✓ MedBox is powered ON\n" +
                "✓ Bluetooth is enabled on both devices\n" +
                "✓ Device is paired in Android Settings\n\n" +
                "You can use default boxes or pair a device.");

        builder.setPositiveButton("Open Bluetooth Settings", (dialog, which) -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            // 稍后重试
            new Handler().postDelayed(() -> {
                showDeviceSelectionDialog();
            }, 2000);
        });

        builder.setNegativeButton("Use Default Boxes", (dialog, which) -> {
            showDefaultBoxSelection();
        });

        builder.setNeutralButton("Retry", (dialog, which) -> {
            showDeviceSelectionDialog();
        });

        builder.show();
    }

    // 修改后的盒子选择对话框
    private void showBoxSelectionDialog(List<String> availableBoxes, boolean isBluetoothDevice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Medicine Box");

        if (isBluetoothDevice) {
            builder.setMessage("Choose which MedBox device to use:");
        } else {
            builder.setMessage("Choose which virtual box to use:");
        }

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

            // Update medicine name hint with box info
            if (etMedName.getText().toString().trim().isEmpty()) {
                etMedName.setHint("e.g. Medicine for " + selectedBoxName);
            }

            // Check if box already has medication (只在添加新药物时检查)
            if (!isEditingExisting && databaseHelper.hasMedicationInBox(selectedBoxNumber)) {
                showBoxAlreadyHasMedicineDialog();
            }
        });

        builder.setCancelable(false);

        // 如果是从主页进入编辑，不显示对话框
        if (isEditingExisting) {
            // 不显示选择对话框，直接使用已选择的盒子
            if (selectedBoxNumber != -1) {
                if (etMedName.getText().toString().trim().isEmpty()) {
                    etMedName.setHint("e.g. Medicine for " + selectedBoxName);
                }
            }
        } else {
            builder.show();
        }
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
        } else {
            // For older versions, need location permission for Bluetooth scanning
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showDeviceSelectionDialog();
            } else {
                // 如果没有权限，显示默认盒子选择
                showDefaultBoxSelection();
            }
        }
    }

    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void showBoxAlreadyHasMedicineDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Box Already Has Medicine")
                .setMessage("This box already contains medication. Do you want to replace it or add as additional medication?")
                .setPositiveButton("Replace", (dialog, which) -> {
                    // User will replace, continue
                })
                .setNegativeButton("Choose Different Box", (dialog, which) -> {
                    // 重新显示选择对话框
                    showDeviceSelectionDialog();
                })
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
        if (medicationId == -1) return;

        Medication medication = databaseHelper.getMedicationById(medicationId);
        if (medication != null) {
            // 填充现有数据
            etMedName.setText(medication.getMedicineName());
            selectedBoxNumber = medication.getBoxNumber();
            selectedBoxName = medication.getBoxName();

            // 设置频率
            String frequency = medication.getFrequency();
            if (frequency != null) {
                if (frequency.equals("Once a day")) {
                    radioGroupFrequency.check(R.id.rbOnce);
                } else if (frequency.equals("Twice a day")) {
                    radioGroupFrequency.check(R.id.rbTwice);
                } else if (frequency.equals("Three times a day")) {
                    radioGroupFrequency.check(R.id.rbThree);
                } else if (frequency.equals("Four times a day")) {
                    radioGroupFrequency.check(R.id.rbFour);
                } else if (frequency.equals("As needed")) {
                    radioGroupFrequency.check(R.id.rbAsNeeded);
                    llCustomSchedule.setVisibility(View.VISIBLE);
                    etCustomDays.setText(medication.getDays());
                    etCustomTimes.setText(medication.getTimes());
                }
            }

            // 设置时间
            if (medication.getNotificationTime() != null) {
                selectedTimes.clear();
                selectedTimes.add(medication.getNotificationTime());
                selectedTimesLayout.removeAllViews();
                addTimeChip(medication.getNotificationTime());
            }

            // 设置说明
            if (medication.getInstructions() != null) {
                etInstructions.setText(medication.getInstructions());
            }
        }
    }

    private void saveMedication() {
        // Validate inputs
        String medicineName = etMedName.getText().toString().trim();
        if (medicineName.isEmpty()) {
            showError("Please enter medicine name");
            return;
        }

        if (selectedBoxNumber == -1) {
            // 如果没有选择盒子，显示选择对话框
            showDeviceSelectionDialog();
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
        long id;
        if (medicationId != -1) {
            // 更新现有药物
            databaseHelper.updateMedication(medication);
            id = medicationId;
        } else {
            // 添加新药物
            id = databaseHelper.addMedication(medication);
        }

        if (id != -1) {
            // 设置提醒闹钟
            scheduleReminderAlarm(id, medicineName, notificationTime, instructions, selectedBoxNumber);

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

    // 修改：设置提醒闹钟 - 使用 setAlarmClock 确保可靠触发
    private void scheduleReminderAlarm(long medicationId, String medicineName, String notificationTime, String instructions, int boxNumber) {
        try {
            // 解析时间字符串，例如 "7:00 am"
            String[] parts = notificationTime.split(" ");
            String timePart = parts[0]; // "7:00"
            String amPm = parts[1]; // "am"

            String[] timeComponents = timePart.split(":");
            int hour = Integer.parseInt(timeComponents[0]);
            int minute = Integer.parseInt(timeComponents[1]);

            // 转换为24小时制
            if (amPm.equalsIgnoreCase("pm") && hour != 12) {
                hour += 12;
            } else if (amPm.equalsIgnoreCase("am") && hour == 12) {
                hour = 0;
            }

            // 创建Calendar实例
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // 如果设置的时间已经过去，设置为明天
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // 创建Intent
            Intent reminderIntent = new Intent(this, ReminderActivity.class);
            reminderIntent.putExtra("medication_id", medicationId);
            reminderIntent.putExtra("medicine_name", medicineName);
            reminderIntent.putExtra("reminder_time", notificationTime);
            reminderIntent.putExtra("instructions", instructions);
            reminderIntent.putExtra("box_number", boxNumber);
            reminderIntent.putExtra("dosage", "1 tablet");

            // 添加必要的标志
            reminderIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // 创建PendingIntent
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    (int) medicationId, // 使用medicationId作为requestCode确保唯一性
                    reminderIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            // 设置闹钟 - 使用 setAlarmClock() 确保在锁屏和深度睡眠下也能工作
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                // 使用 setAlarmClock() 显示在锁屏上
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    android.app.AlarmManager.AlarmClockInfo alarmClockInfo =
                            new android.app.AlarmManager.AlarmClockInfo(
                                    calendar.getTimeInMillis(),
                                    pendingIntent
                            );
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                    Log.d("AddMedicineActivity", "AlarmClock set for: " + notificationTime + " (" + calendar.getTime() + ")");
                }
                // 对于 Android 4.4+ 使用 setExactAndAllowWhileIdle
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d("AddMedicineActivity", "setExactAndAllowWhileIdle set for: " + notificationTime + " (" + calendar.getTime() + ")");
                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d("AddMedicineActivity", "setExact set for: " + notificationTime + " (" + calendar.getTime() + ")");
                }
                else {
                    alarmManager.set(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d("AddMedicineActivity", "set set for: " + notificationTime + " (" + calendar.getTime() + ")");
                }

                // 显示调试信息
                debugAlarmStatus(alarmManager, pendingIntent);
            }

        } catch (Exception e) {
            Log.e("AddMedicineActivity", "Error setting alarm: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Failed to set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 新增：调试闹钟状态
    private void debugAlarmStatus(android.app.AlarmManager alarmManager, android.app.PendingIntent pendingIntent) {
        Log.d("AddMedicineActivity", "PendingIntent created: " + (pendingIntent != null ? "YES" : "NO"));

        // 检查下一个闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.app.AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();
            if (nextAlarm != null) {
                Log.d("AddMedicineActivity", "Next alarm scheduled for: " + new Date(nextAlarm.getTriggerTime()));
            } else {
                Log.d("AddMedicineActivity", "No next alarm found in system");
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // 给蓝牙一些时间启动
                new Handler().postDelayed(() -> {
                    showDeviceSelectionDialog();
                }, 1000);
            } else {
                // 用户拒绝开启蓝牙，使用默认盒子选择
                showDefaultBoxSelection();
            }
        }
    }

    // BluetoothService.BluetoothListener implementations
    @Override
    public void onBluetoothDataReceived(String data) {
        Log.d("AddMedicineActivity", "Bluetooth data received: " + data);
        runOnUiThread(() -> {
            // Process received data here
            if (data.contains("ACK") || data.contains("OK")) {
                showToast("Device communication successful");
            }
        });
    }

    @Override
    public void onBluetoothStatusChanged(String status) {
        Log.d("AddMedicineActivity", "Bluetooth status: " + status);
        runOnUiThread(() -> {
            if (status.contains("Connected")) {
                isConnected = true;
                showToast("MedBox connected successfully");
            } else if (status.contains("Disconnected") || status.contains("Connection lost")) {
                isConnected = false;
                showToast("MedBox disconnected");
            }
        });
    }

    @Override
    public void onBluetoothError(String error) {
        Log.e("AddMedicineActivity", "Bluetooth error: " + error);
        runOnUiThread(() -> {
            isConnected = false;
            showToast("Error: " + error);
        });
    }

    // 新增：可用设备发现回调
    @Override
    public void onAvailableDevicesFound(List<BluetoothDevice> devices) {
        Log.d("AddMedicineActivity", "Found " + devices.size() + " available devices");
        runOnUiThread(() -> {
            if (!isEditingExisting && devices.size() == 1) {
                // 只有一个设备，自动选择
                BluetoothDevice device = devices.get(0);
                String deviceName = getDeviceNameSafely(device);

                if (deviceName != null && !deviceName.equals("Unknown Device")) {
                    // 尝试从设备名提取盒子号
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(deviceName);
                    if (matcher.find()) {
                        selectedBoxNumber = Integer.parseInt(matcher.group());
                        selectedBoxName = "Box " + selectedBoxNumber;

                        // 更新提示
                        if (etMedName.getText().toString().trim().isEmpty()) {
                            etMedName.setHint("e.g. Medicine for " + selectedBoxName);
                        }
                        showToast("Automatically selected " + selectedBoxName);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind service
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }

        // Close database
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
