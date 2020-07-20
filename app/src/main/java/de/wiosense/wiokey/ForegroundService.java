package de.wiosense.wiokey;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import de.wiosense.wiokey.bluetooth.BluetoothDeviceListing;
import de.wiosense.wiokey.bluetooth.BluetoothUtils;
import de.wiosense.wiokey.bluetooth.HidDeviceController;
import de.wiosense.wiokey.bluetooth.HidDeviceProfile;
import de.wiosense.wiokey.utils.DeviceManagementEvent;
import de.wiosense.wiokey.utils.FirebaseManager;
import de.wiosense.wiokey.utils.UiUpdateEvent;

import static android.app.Notification.DEFAULT_SOUND;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_DEVICECONNECTED;

public class ForegroundService extends Service {

    private static final String TAG = "WioKey|ForegroundService";
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    private static final int DISCOVERABLE_DURATION_S = 60;
    private static final int REQ_CONNECT_TIMEOUT_MS = 8000;

    private static final Object mLock = new Object();

    @GuardedBy("mLock")
    private static boolean mMonitor = false;
    private static Timer mMonitorTimer = new Timer(Looper.getMainLooper().getThread().getName());
    private boolean prevConnected = false;

    private HidDeviceProfile hidDeviceProfile;
    private HidDeviceController hidDeviceController;
    private BluetoothDeviceListing bluetoothDeviceListing;

    private HidDeviceController.ProfileListener profileListener = new HidDeviceController.ProfileListener() {
        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            synchronized (mLock) {
                if (mMonitor && device != null
                        && state == BluetoothProfile.STATE_CONNECTED
                        && device.getBondState() == BluetoothDevice.BOND_BONDED
                        && !bluetoothDeviceListing.isHidDevice(device)) {
                    Log.d(TAG, "New HID host connected and paired: " + device);
                    bluetoothDeviceListing.cacheHidDevice(device);
                    bluetoothDeviceListing.cacheHidDefaultDevice(device);
                    FirebaseManager.sendLogEvent(EVENT_DEVICECONNECTED,null);
                    // Make sure to refresh device display (in case is ON)
                    updateHidDefaultHostDevice();
                    updateHidAvailableDevices();
                }
            }

            // Update default HID device connection state as well
            if (bluetoothDeviceListing.isHidDefaultDevice(device)) {
                EventBus.getDefault().post(new UiUpdateEvent(
                        UiUpdateEvent.Type.UPDATE_DEFAULT_HID_DEVICE,
                        device,
                        hidDeviceProfile.getConnectionState(device)
                ));
            }
        }

        @Override
        public void onAppStatusChanged(boolean registered) {
            /**
             * If we are registered, we have an active controller and a default device
             * we proceed in try to connect with the default HID host. Since we do not
             * know if the device is in range and this may fail, we need to handle it.
             *
             * We do so with a simple timer.
             */
            Log.d(TAG, "On status changed " + registered);
            if (registered && hidDeviceController != null) {
                BluetoothDevice defaultDevice = bluetoothDeviceListing.getHidDefaultDevice();
                if (defaultDevice != null) {
                    Log.d(TAG, "Requesting to connect");
                    hidDeviceController.requestConnect(defaultDevice, REQ_CONNECT_TIMEOUT_MS);
                }
            }
            if (!registered && hidDeviceController != null) {
                hidDeviceController.unregister(getApplicationContext(), profileListener);
            }
        }

        @Override
        public void onInterruptData(BluetoothDevice device, int reportId, byte[] data, BluetoothHidDevice inputHost) {
            if (MainActivity.mTransactionManager != null && MainActivity.isOnForeground()) {
                MainActivity.mTransactionManager.handleReport(data, (rawReports) -> {
                    for (byte[] report : rawReports) {
                        inputHost.sendReport(device, reportId, report);
                    }
                });
            } else {
                Log.w(TAG, "Notification sent to user!");
                showRequestNotification();
            }
        }

        @Override
        public void onServiceStateChanged(BluetoothProfile proxy) {
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        setHid();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"Starting Foreground - " + intent);
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title_foregroundservice))
                .setContentText(getString((R.string.notification_body_foregroundservice)))
                .setSmallIcon(R.drawable.logo_small)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(hidDeviceController!=null){
            hidDeviceController.unregister(getApplicationContext(),
                    profileListener);
        }
        // Terminate the event bus for this foreground service
        EventBus.getDefault().post(UiUpdateEvent.Type.UPDATE_CONN_ANIMATION);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setHid(){
        hidDeviceController = HidDeviceController.getInstance();
        hidDeviceProfile = hidDeviceController.register(getApplicationContext(),
                profileListener);
        bluetoothDeviceListing = new BluetoothDeviceListing(getApplicationContext(), hidDeviceProfile);

        BluetoothDevice defaultHidDevice = bluetoothDeviceListing.getHidDefaultDevice();
        try {
            EventBus.getDefault().post(new UiUpdateEvent(
                    UiUpdateEvent.Type.UPDATE_CONN_ANIMATION,
                    defaultHidDevice,
                    hidDeviceProfile.getConnectionState(defaultHidDevice)
            ));
        } catch (NullPointerException e) {
            EventBus.getDefault().post(new UiUpdateEvent(
                    UiUpdateEvent.Type.UPDATE_CONN_ANIMATION,
                    null,
                    BluetoothProfile.STATE_DISCONNECTED)
            );
        }
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "WioKey Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        serviceChannel.setShowBadge(true);
        serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void showRequestNotification(){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, intent, FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_small)
                .setContentTitle(getString(R.string.notification_title_actionrequired))
                .setContentText(getString(R.string.notification_body_actionrequired))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2,builder.build());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUiUpdateEvent(UiUpdateEvent event) {
        switch (event.mType) {
            case UPDATE_HID_DEVICES_REQ:
                Log.d(TAG,"UI -> " + event.mType);
                updateHidAvailableDevices();
                break;
            case UPDATE_DEFAULT_HID_DEVICE_REQ:
                Log.d(TAG,"UI -> " + event.mType);
                updateHidDefaultHostDevice();
                break;
            default:
                Log.w(TAG,"Unknown UI updated event " + event.mType);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceManagementEvent(DeviceManagementEvent event) {
        switch (event.mType) {
            case SELECT_DEFAULT_DEVICE:
                Log.d(TAG,"DevMgmt -> " + event.mType + " Device: " + event.mDevice);
                onReceiveSelectDevice(event.mDevice);
                break;
            case REGISTER_NEW_DEVICE:
                Log.d(TAG,"DevMgmt -> " + event.mType);
                onReceiveRegisterNewDevice();
                break;
            case MANUAL_CONNECT_DEVICE:
                Log.d(TAG, "DevMgmt -> " + event.mType);
                onReceiveManualConnectDevice();
                break;
            case DISCONNECT_DEVICE:
                Log.d(TAG, "DevMgmt -> " + event.mType);
                onReceiveDisconnectDevice();
                break;
            case REMOVE_DEVICE:
                Log.d(TAG, "DevMgmt -> " + event.mType);
                onReceiveRemoveDevice(event.mDevice);
                break;
            default:
                Log.w(TAG,"Unknown DevMgmt event " + event.mType);
        }
    }

    private void updateHidDefaultHostDevice(){
        BluetoothDevice defaultDevice = bluetoothDeviceListing.getHidDefaultDevice();
        EventBus.getDefault().post(new UiUpdateEvent(
                UiUpdateEvent.Type.UPDATE_DEFAULT_HID_DEVICE,
                defaultDevice,
                hidDeviceProfile.getConnectionState(defaultDevice)
        ));
    }

    private void updateHidAvailableDevices() {
        if (bluetoothDeviceListing != null) {
            List<BluetoothDevice> deviceList = bluetoothDeviceListing.getHidAvailableDevices();
            EventBus.getDefault().post(new UiUpdateEvent(
                    UiUpdateEvent.Type.UPDATE_HID_DEVICES,
                    deviceList
            ));
        }
    }

    private void onReceiveSelectDevice(BluetoothDevice device){
        if (bluetoothDeviceListing.cacheHidDefaultDevice(device)) {
            Log.d(TAG, "Updated default preference: " + device);
            Toast.makeText(this,
                    getString(R.string.toast_deviceselected,device.getName()),
                    Toast.LENGTH_LONG)
                    .show();
            updateHidDefaultHostDevice();
            onReceiveManualConnectDevice();
        } else {
            Toast.makeText(this,
                    getString(R.string.toast_deviceselected_fail),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void onReceiveRegisterNewDevice(){
        prevConnected = false;

        synchronized (mLock) {
            // First check if we are currently connected to the default HID host disconnect immediately
            if (!mMonitor && hidDeviceController.isConnected()) {
                hidDeviceController.requestConnect(null);
                prevConnected = true;
            }

            // Protect this against future calls until the current one completes
            if (mMonitor) {
                Toast.makeText(this,
                        getString(R.string.toast_deviceregister_repeatedcall),
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                mMonitor = true;
                EventBus.getDefault().post(new UiUpdateEvent(
                        UiUpdateEvent.Type.UPDATE_SCREEN_KEEP_ALIVE_ON
                ));

                // Schedule new timer
                mMonitorTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (mLock) {
                            mMonitor = false;
                        }
                        Log.d(TAG, "Discoverable timer expired");

                        /* Restore connectable but non-discoverable state to ensure privacy */
                        if (BluetoothUtils.setScanMode(BluetoothAdapter.getDefaultAdapter(),
                                BluetoothAdapter.SCAN_MODE_CONNECTABLE,
                                0)) {
                            Log.d(TAG,getString(R.string.toast_deviceregister_disabled));

                            // Notify user via a toast using event bus and home fragment
                            EventBus.getDefault().post(new UiUpdateEvent(
                                    UiUpdateEvent.Type.UPDATE_TOAST,
                                    getString(R.string.toast_deviceregister_disabled)
                            ));

                            // Disable screen keepalive lock
                            EventBus.getDefault().post(new UiUpdateEvent(
                                    UiUpdateEvent.Type.UPDATE_SCREEN_KEEP_ALIVE_OFF
                            ));
                        }

                        /* If we are not connected at this point, but we were before reconnect */
                        if (!hidDeviceController.isConnected() && prevConnected) {
                            hidDeviceController.requestConnect(bluetoothDeviceListing.getHidDefaultDevice(),
                                    REQ_CONNECT_TIMEOUT_MS);
                        } else {
                            // Otherwise simply refresh UI state based on current HID
                            BluetoothDevice device = bluetoothDeviceListing.getHidDefaultDevice();
                            EventBus.getDefault().post(new UiUpdateEvent(
                                    UiUpdateEvent.Type.UPDATE_CONN_ANIMATION,
                                    device,
                                    hidDeviceProfile.getConnectionState(device)
                            ));
                        }
                    }
                }, DISCOVERABLE_DURATION_S * 1000);

                // Here just make the device discoverable for the next minute or so to allow user to pair to PC
                BluetoothUtils.setScanMode(BluetoothAdapter.getDefaultAdapter(),
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                        DISCOVERABLE_DURATION_S);

                // Notify user via a toast using event bus and home fragment
                EventBus.getDefault().post(new UiUpdateEvent(
                        UiUpdateEvent.Type.UPDATE_TOAST,
                        getString(R.string.toast_deviceregister_enabled,DISCOVERABLE_DURATION_S)));
            }
            
            // Rerender a delayed animation showing that we are waiting for a request to pair
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    EventBus.getDefault().post(new UiUpdateEvent(
                            UiUpdateEvent.Type.UPDATE_CONN_ANIMATION,
                            null,
                            BluetoothProfile.STATE_CONNECTING
                    ));
                }
            }, 500);
        }
    }

    private void onReceiveManualConnectDevice() {
        hidDeviceController.requestConnect(bluetoothDeviceListing.getHidDefaultDevice(),
                REQ_CONNECT_TIMEOUT_MS);
        EventBus.getDefault().post(new UiUpdateEvent(
                UiUpdateEvent.Type.UPDATE_TOAST,
                getString(R.string.toast_devicemanual_connect,
                        Objects.requireNonNull(bluetoothDeviceListing.getHidDefaultDevice()).getName())
        ));
    }

    private void onReceiveDisconnectDevice() {
        hidDeviceController.requestConnect(null);
        EventBus.getDefault().post(new UiUpdateEvent(
                UiUpdateEvent.Type.UPDATE_TOAST,
                getString(R.string.toast_device_disconnected)
        ));
    }

    private void onReceiveRemoveDevice(BluetoothDevice device) {
        // 0. Check if device is null -> do nothing if so
        // 1. If connected to this device -> disconnect
        // 2.0 If device was default host -> clear clearHidPreference(BluetoothDevice device, String HID_PREFERRED_HOST)
        // 2.1 Remove device from HID devices list
        // 3. Call the removeBond(device) method from Bluetooth
        // 4. Post event to update UI on the Event Bus for UPDATE_HID_DEVICES
        // 5. end

        if (device == null) {
            Log.e(TAG, "Cannot remove null device");
            return;
        }

        if (hidDeviceProfile != null && hidDeviceController.isConnected() &&
                hidDeviceProfile.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
            hidDeviceController.requestConnect(null);
        }

        if (bluetoothDeviceListing != null && bluetoothDeviceListing.clearHidDevice(device)) {
            Log.d(TAG, "Cleared HID device");
            EventBus.getDefault().post(new UiUpdateEvent(
                    UiUpdateEvent.Type.UPDATE_TOAST,
                    getString(R.string.toast_devicecleared)
            ));
        } else {
            Log.e(TAG, "Failed to clear HID device from cache");
        }

        if (BluetoothUtils.removeBond(device)) {
            EventBus.getDefault().post(new UiUpdateEvent(
                    UiUpdateEvent.Type.UPDATE_TOAST,
                    getString(R.string.toast_deviceunpaired,device.getAddress())
            ));
        } else {
            EventBus.getDefault().post(new UiUpdateEvent(
                    UiUpdateEvent.Type.UPDATE_TOAST,
                    getString(R.string.toast_deviceunpaired_failed, device.getAddress())
            ));
        }

        // Post delayed UI event update to allow sync with sharedPreferences
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateHidDefaultHostDevice();
                updateHidAvailableDevices();
            }
        }, 100);
    }
}