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

    // Connection states
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    private int connectionState = STATE_NONE;

    public interface BluetoothListener {
        void onBluetoothDataReceived(String data);
        void onBluetoothStatusChanged(String status);
        void onBluetoothError(String error);
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

    private BluetoothDevice findTargetDevice() {
        if (bluetoothAdapter == null) return null;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName != null && deviceName.contains(targetDeviceName)) {
                    Log.d(TAG, "Target device found: " + deviceName);
                    return device;
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
