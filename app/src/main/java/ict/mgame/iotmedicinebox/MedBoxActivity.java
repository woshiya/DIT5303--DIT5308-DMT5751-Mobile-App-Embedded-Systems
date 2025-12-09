package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MedBoxActivity extends Activity implements BluetoothService.BluetoothListener {
    private static final String TAG = "MedBoxActivity";

    // Permission request codes
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1002;

    private BluetoothService bluetoothService;
    private boolean isBound = false;
    private boolean isConnected = false;
    private TextView connectionStatus;
    private BluetoothAdapter bluetoothAdapter;

    private final Handler mainHandler = new Handler();

    // Service connection
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            bluetoothService.setListener(MedBoxActivity.this);
            isBound = true;

            Log.d(TAG, "Bluetooth service connected");

            // 自动搜索可用的MedBox设备
            if (checkBluetoothPermissions()) {
                scanForDevices();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            bluetoothService = null;
            Log.d(TAG, "Bluetooth service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medbox);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setupUI();
    }

    private void setupUI() {
        // Setup connection status TextView
        connectionStatus = findTextViewByText("Device Connected");

        // Bind Bluetooth service
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Setup back button click listener
        setupBackButton();

        // Setup MedBox Logo click listener
        setupMedBoxLogo();

        // Setup card click listeners
        setupCardClickListeners();

        updateConnectionStatus();
    }

    private void setupBackButton() {
        // Find back button by traversal
        ViewGroup topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            for (int i = 0; i < topBar.getChildCount(); i++) {
                View child = topBar.getChildAt(i);
                if (child instanceof android.widget.ImageView) {
                    // Assume the first ImageView is the back button
                    child.setOnClickListener(v -> finish());
                    break;
                }
            }
        }
    }

    private void setupMedBoxLogo() {
        // Find MedBox Logo RelativeLayout
        ViewGroup mainContent = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (mainContent != null) {
            // Find the second LinearLayout (main content area)
            ViewGroup contentLayout = null;
            for (int i = 0; i < mainContent.getChildCount(); i++) {
                View child = mainContent.getChildAt(i);
                if (child instanceof LinearLayout && child.getId() != R.id.topBar) {
                    contentLayout = (ViewGroup) child;
                    break;
                }
            }

            if (contentLayout != null) {
                // Find RelativeLayout (MedBox Logo) in content area
                for (int i = 0; i < contentLayout.getChildCount(); i++) {
                    View child = contentLayout.getChildAt(i);
                    if (child instanceof RelativeLayout) {
                        child.setClickable(true);
                        child.setOnClickListener(v -> {
                            if (checkBluetoothPermissions()) {
                                showDeviceSelectionDialog();
                            }
                        });
                        break;
                    }
                }
            }
        }
    }

    private void setupCardClickListeners() {
        // Find GridLayout
        GridLayout gridLayout = findGridLayout();
        if (gridLayout != null) {
            // Setup card click listeners in order
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                final int position = i;
                View card = gridLayout.getChildAt(i);
                card.setClickable(true);

                switch (position) {
                    case 0: // Light Up
                        card.setOnClickListener(v -> {
                            if (isConnected) {
                                showLedSelectionDialog();
                            } else {
                                showToast("Please connect to MedBox first");
                            }
                        });
                        break;
                    case 1: // Open Box
                        card.setOnClickListener(v -> {
                            if (isConnected) {
                                showBoxSelectionDialog();
                            } else {
                                showToast("Please connect to MedBox first");
                            }
                        });
                        break;
                    case 2: // Find Device
                        card.setOnClickListener(v -> {
                            showToast("Searching for nearby Bluetooth devices...");
                            showDeviceSelectionDialog();
                        });
                        break;
                    case 3: // Disconnect
                        card.setOnClickListener(v -> disconnectBluetooth());
                        break;
                }
            }
        }
    }

    private void connectBluetooth() {
        if (bluetoothAdapter == null) {
            showToast("Device does not support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // Request to enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            return;
        }

        if (bluetoothService != null && bluetoothService.connect()) {
            Log.d(TAG, "Bluetooth connection initiated");
            showToast("Connecting to MedBox...");
        } else {
            Log.e(TAG, "Failed to initiate Bluetooth connection");
            showToast("Unable to connect to Bluetooth service");
        }
    }

    // 新增：扫描设备方法
    private void scanForDevices() {
        if (bluetoothService != null) {
            List<BluetoothDevice> devices = bluetoothService.findAvailableMedBoxDevices();
            if (devices.isEmpty()) {
                showToast("No MedBox devices found. Please pair a device first.");
            }
        }
    }

    private void disconnectBluetooth() {
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
        isConnected = false;
        updateConnectionStatus();
        Log.d(TAG, "Bluetooth disconnected by user");
        showToast("Disconnected from MedBox");
    }

    private void showDeviceSelectionDialog() {
        if (!checkBluetoothPermissions()) {
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            showToast("Please enable Bluetooth first");
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
                String name = device.getName();
                String address = device.getAddress();
                if (name == null || name.isEmpty()) {
                    deviceNames.add("Unknown Device [" + address + "]");
                } else {
                    deviceNames.add(name + " [" + address.substring(0, 8) + "...]");
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select MedBox Device");
            builder.setMessage("Choose which device to connect to:");

            String[] devicesArray = deviceNames.toArray(new String[0]);
            builder.setItems(devicesArray, (dialog, which) -> {
                BluetoothDevice selectedDevice = availableDevices.get(which);
                // 连接到选中的设备
                if (bluetoothService != null) {
                    bluetoothService.connectToDevice(selectedDevice);
                    showToast("Connecting to " + selectedDevice.getName() + "...");
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.setNeutralButton("Manual Connect", (dialog, which) -> {
                showManualConnectionDialog();
            });

            builder.show();
        }
    }

    // 新增：手动连接对话框
    private void showManualConnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manual Connection");
        builder.setMessage("If your device is not listed, please:\n\n" +
                "1. Go to Android Settings > Bluetooth\n" +
                "2. Make sure MedBox is powered ON\n" +
                "3. Pair with the device (usually named HC-05 or HC-06)\n" +
                "4. Return here and tap 'Retry'");

        builder.setPositiveButton("Open Bluetooth Settings", (dialog, which) -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });

        builder.setNegativeButton("Retry", (dialog, which) -> {
            showDeviceSelectionDialog();
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    // 新增：无设备对话框
    private void showNoDevicesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Devices Found");
        builder.setMessage("No MedBox devices found. Please ensure:\n\n" +
                "✓ MedBox is powered ON\n" +
                "✓ Bluetooth is enabled on both devices\n" +
                "✓ Device is paired in Android Settings\n\n" +
                "Common device names: HC-05, HC-06, MedBox, Arduino");

        builder.setPositiveButton("Open Bluetooth Settings", (dialog, which) -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });

        builder.setNegativeButton("Try Demo Mode", (dialog, which) -> {
            isConnected = true;
            updateConnectionStatus();
            showToast("Demo mode activated. You can now test controls.");
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void showLedSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Box to Light Up");

        String[] boxes = {"Box 1", "Box 2", "Box 3"};
        builder.setItems(boxes, (dialog, which) -> {
            switch (which) {
                case 0:
                    turnOnLed(1);
                    break;
                case 1:
                    turnOnLed(2);
                    break;
                case 2:
                    turnOnLed(3);
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showBoxSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Box to Open");

        String[] boxes = {"Box 1", "Box 2", "Box 3"};
        builder.setItems(boxes, (dialog, which) -> {
            switch (which) {
                case 0:
                    openBox(1);
                    break;
                case 1:
                    openBox(2);
                    break;
                case 2:
                    openBox(3);
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void turnOnLed(int boxNumber) {
        if (!isConnected || bluetoothService == null) {
            showToast("Please connect to MedBox first");
            return;
        }

        String command = "Box" + boxNumber + "_LED_ON";
        bluetoothService.sendCommand(command);
        Log.d(TAG, "LED ON command sent: " + command);
        showToast("Box " + boxNumber + " LED turned on - Reminding to take medicine");

        // Auto turn off after 3 seconds
        mainHandler.postDelayed(() -> {
            if (isConnected && bluetoothService != null) {
                String offCommand = "Box" + boxNumber + "_LED_OFF";
                bluetoothService.sendCommand(offCommand);
                Log.d(TAG, "LED auto-off: " + offCommand);
            }
        }, 3000);
    }

    private void openBox(int boxNumber) {
        if (!isConnected || bluetoothService == null) {
            showToast("Please connect to MedBox first");
            return;
        }

        String openCommand = "Box" + boxNumber + "_OPEN";
        bluetoothService.sendCommand(openCommand);
        Log.d(TAG, "Box OPEN command sent: " + openCommand);
        showToast("Box " + boxNumber + " opened");

        // Auto close after 5 seconds
        mainHandler.postDelayed(() -> {
            if (isConnected && bluetoothService != null) {
                String closeCommand = "Box" + boxNumber + "_CLOSE";
                bluetoothService.sendCommand(closeCommand);
                Log.d(TAG, "Box auto-close: " + closeCommand);
            }
        }, 5000);
    }

    private void updateConnectionStatus() {
        runOnUiThread(() -> {
            if (connectionStatus != null) {
                if (isConnected) {
                    connectionStatus.setText("Device Connected");
                    connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    connectionStatus.setText("Device Disconnected");
                    connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
        });
    }

    // Permission check
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
                scanForDevices();
            } else {
                showToast("Bluetooth permissions denied. Cannot connect.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // 给蓝牙一些时间启动
                mainHandler.postDelayed(() -> {
                    scanForDevices();
                }, 1000);
            } else {
                showToast("Bluetooth not enabled. Cannot connect.");
            }
        }
    }

    // BluetoothService.BluetoothListener implementations
    @Override
    public void onBluetoothDataReceived(String data) {
        Log.d(TAG, "Bluetooth data received: " + data);
        runOnUiThread(() -> {
            // Process received data here
            if (data.contains("ACK") || data.contains("OK")) {
                showToast("Command executed successfully");
            }
        });
    }

    @Override
    public void onBluetoothStatusChanged(String status) {
        Log.d(TAG, "Bluetooth status: " + status);
        runOnUiThread(() -> {
            if (status.contains("Connected")) {
                isConnected = true;
                showToast("MedBox connected successfully");
            } else if (status.contains("Disconnected") || status.contains("Connection lost")) {
                isConnected = false;
                showToast("MedBox disconnected");
            }
            updateConnectionStatus();
        });
    }

    @Override
    public void onBluetoothError(String error) {
        Log.e(TAG, "Bluetooth error: " + error);
        runOnUiThread(() -> {
            isConnected = false;
            updateConnectionStatus();
            showToast("Error: " + error);
        });
    }

    // 新增：可用设备发现回调
    @Override
    public void onAvailableDevicesFound(List<BluetoothDevice> devices) {
        Log.d(TAG, "Found " + devices.size() + " available devices");
        // 这里可以自动连接到第一个设备，或者显示通知
        if (devices.size() == 1 && !isConnected) {
            // 只有一个设备，自动连接
            BluetoothDevice device = devices.get(0);
            if (bluetoothService != null) {
                showToast("Connecting to " + device.getName() + "...");
                bluetoothService.connectToDevice(device);
            }
        }
    }

    // Helper methods
    private GridLayout findGridLayout() {
        ViewGroup mainContent = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (mainContent != null) {
            // Find the second LinearLayout (main content area)
            ViewGroup contentLayout = null;
            for (int i = 0; i < mainContent.getChildCount(); i++) {
                View child = mainContent.getChildAt(i);
                if (child instanceof LinearLayout && child.getId() != R.id.topBar) {
                    contentLayout = (ViewGroup) child;
                    break;
                }
            }

            if (contentLayout != null) {
                // Find GridLayout in content area
                for (int i = 0; i < contentLayout.getChildCount(); i++) {
                    View child = contentLayout.getChildAt(i);
                    if (child instanceof GridLayout) {
                        return (GridLayout) child;
                    }
                }
            }
        }
        return null;
    }

    private TextView findTextViewByText(String targetText) {
        ViewGroup mainContent = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (mainContent != null) {
            // Find the second LinearLayout (main content area)
            ViewGroup contentLayout = null;
            for (int i = 0; i < mainContent.getChildCount(); i++) {
                View child = mainContent.getChildAt(i);
                if (child instanceof LinearLayout && child.getId() != R.id.topBar) {
                    contentLayout = (ViewGroup) child;
                    break;
                }
            }

            if (contentLayout != null) {
                // Recursively find TextView in content area
                return findTextViewByTextRecursive(contentLayout, targetText);
            }
        }
        return null;
    }

    private TextView findTextViewByTextRecursive(ViewGroup parent, String targetText) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (targetText.equals(textView.getText().toString())) {
                    return textView;
                }
            } else if (child instanceof ViewGroup) {
                TextView found = findTextViewByTextRecursive((ViewGroup) child, targetText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}
