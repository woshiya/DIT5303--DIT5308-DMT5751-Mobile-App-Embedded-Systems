package ict.mgame.iotmedicinebox;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler handler;
    private BluetoothListener listener;

    private String targetDeviceName = "HC-05"; // Default device name
    private String targetDeviceAddress = "";

    // Connection states
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    private int connectionState = STATE_NONE;

    public interface BluetoothListener {
        void onBluetoothDataReceived(String data);
        void onBluetoothStatusChanged(String status);
        void onBluetoothError(String error);
        void onAvailableDevicesFound(List<BluetoothDevice> devices); // 新增：发现设备回调
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler();
    }

    public void setListener(BluetoothListener listener) {
        this.listener = listener;
    }

    public void setTargetDeviceName(String deviceName) {
        this.targetDeviceName = deviceName;
    }

    public void setTargetDeviceAddress(String address) {
        this.targetDeviceAddress = address;
    }

    public boolean connect() {
        if (bluetoothAdapter == null) {
            notifyError("Bluetooth not supported");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            notifyError("Bluetooth is disabled");
            return false;
        }

        // Find target device
        BluetoothDevice targetDevice = findTargetDevice();
        if (targetDevice == null) {
            notifyError("Device not found: " + targetDeviceName);
            return false;
        }

        // Cancel any existing connection
        if (connectionState == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectionState == STATE_CONNECTED) {
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }
        }

        // Start connection thread
        setState(STATE_CONNECTING);
        connectThread = new ConnectThread(targetDevice);
        connectThread.start();
        return true;
    }

    // 新增：连接特定设备
    public boolean connectToDevice(BluetoothDevice device) {
        if (device == null) {
            notifyError("Device is null");
            return false;
        }

        setTargetDeviceName(device.getName());
        setTargetDeviceAddress(device.getAddress());
        return connect();
    }

    // 新增：搜索所有可能的MedBox设备
    public List<BluetoothDevice> findAvailableMedBoxDevices() {
        List<BluetoothDevice> availableDevices = new ArrayList<>();

        if (bluetoothAdapter == null) {
            return availableDevices;
        }

        if (!bluetoothAdapter.isEnabled()) {
            return availableDevices;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null) {
            return availableDevices;
        }

        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            if (deviceName == null) continue;

            String deviceNameLower = deviceName.toLowerCase();

            // 更宽松的设备匹配逻辑
            boolean isMedBoxDevice =
                    deviceNameLower.contains("hc-05") ||
                            deviceNameLower.contains("hc-06") ||
                            deviceNameLower.contains("medbox") ||
                            deviceNameLower.contains("box") ||
                            (device.getAddress() != null && device.getAddress().startsWith("00:")) || // HC模块通常以00开头
                            deviceNameLower.matches(".*bluetooth.*serial.*") ||
                            deviceNameLower.contains("bt05") ||
                            deviceNameLower.contains("bt06") ||
                            deviceNameLower.contains("arduino") ||
                            deviceNameLower.contains("servo") ||
                            deviceNameLower.contains("bluetooth module");

            if (isMedBoxDevice) {
                availableDevices.add(device);
                Log.d(TAG, "Found potential MedBox device: " + deviceName + " [" + device.getAddress() + "]");
            }
        }

        // 如果没有找到特定设备，返回所有已配对设备
        if (availableDevices.isEmpty()) {
            availableDevices.addAll(pairedDevices);
            Log.d(TAG, "No specific MedBox devices found, returning all paired devices");
        }

        // 通知监听器
        if (listener != null && !availableDevices.isEmpty()) {
            listener.onAvailableDevicesFound(availableDevices);
        }

        return availableDevices;
    }

    private BluetoothDevice findTargetDevice() {
        if (bluetoothAdapter == null) return null;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();

                // 优先使用地址匹配（最准确）
                if (!targetDeviceAddress.isEmpty() && device.getAddress() != null &&
                        device.getAddress().equalsIgnoreCase(targetDeviceAddress)) {
                    Log.d(TAG, "Target device found by address: " + deviceName + " [" + device.getAddress() + "]");
                    return device;
                }

                // 然后使用名称匹配
                if (deviceName != null) {
                    // 宽松的名称匹配
                    if (deviceName.toLowerCase().contains(targetDeviceName.toLowerCase()) ||
                            targetDeviceName.toLowerCase().contains(deviceName.toLowerCase())) {
                        Log.d(TAG, "Target device found by name: " + deviceName);
                        return device;
                    }

                    // 检查是否是可能的MedBox设备
                    String deviceNameLower = deviceName.toLowerCase();
                    if ((targetDeviceName.isEmpty() || targetDeviceName.equals("HC-05")) &&
                            (deviceNameLower.contains("hc-05") ||
                                    deviceNameLower.contains("hc-06") ||
                                    deviceNameLower.contains("medbox") ||
                                    deviceNameLower.contains("arduino"))) {
                        Log.d(TAG, "Found MedBox device: " + deviceName);
                        return device;
                    }
                }
            }
        }

        // 如果没找到，尝试第一个HC-05/HC-06设备
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName != null) {
                    String deviceNameLower = deviceName.toLowerCase();
                    if (deviceNameLower.contains("hc-05") || deviceNameLower.contains("hc-06")) {
                        Log.d(TAG, "Using first HC-05/HC-06 device: " + deviceName);
                        return device;
                    }
                }
            }
        }

        return null;
    }

    public void sendCommand(String command) {
        if (connectionState != STATE_CONNECTED || connectedThread == null) {
            notifyError("Not connected to MedBox");
            return;
        }

        connectedThread.write(command);
        Log.d(TAG, "Command sent: " + command);
    }

    public void disconnect() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
        notifyStatus("Disconnected");
    }

    public boolean isConnected() {
        return connectionState == STATE_CONNECTED;
    }

    private synchronized void setState(int state) {
        connectionState = state;
    }

    private void connectionFailed() {
        setState(STATE_NONE);
        notifyError("Connection failed");
    }

    private void connectionLost() {
        setState(STATE_NONE);
        notifyStatus("Connection lost");
    }

    private void connected(BluetoothSocket socket) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(STATE_CONNECTED);
        notifyStatus("Connected to MedBox: " + targetDeviceName);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmpSocket = null;

            try {
                tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket creation failed", e);
                notifyError("Socket creation failed: " + e.getMessage());
            }
            socket = tmpSocket;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Socket close failed", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            connected(socket);
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket close failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temporary sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (connectionState == STATE_CONNECTED) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String receivedData = new String(buffer, 0, bytes).trim();

                        handler.post(() -> {
                            if (listener != null && !receivedData.isEmpty()) {
                                listener.onBluetoothDataReceived(receivedData);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(String command) {
            try {
                outputStream.write((command + "\n").getBytes());
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Send failed", e);
                notifyError("Send failed: " + e.getMessage());
            }
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket close failed", e);
            }
        }
    }

    private void notifyStatus(String status) {
        handler.post(() -> {
            if (listener != null) {
                listener.onBluetoothStatusChanged(status);
            }
        });
    }

    private void notifyError(String error) {
        handler.post(() -> {
            if (listener != null) {
                listener.onBluetoothError(error);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
