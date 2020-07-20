/**
 * Based on initial work of HidDataSender.java from WearMouse, which comes with the
 * following copyright notice, licensed under the Apache License, Version 2.0
 *
 * Copyright 2018, Google LLC All Rights Reserved.
 *
 * Modified to model the HID representation of a security key virtual connection
 * based on the CTAP2 HID interrupt driven protocol available at:
 * https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#usb
 */

package de.wiosense.wiokey.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Looper;
import android.util.ArraySet;
import android.util.Log;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import de.wiosense.wiokey.bluetooth.HidDeviceProfile.ServiceStateListener;

/** Central point for enabling the HID SDP record and sending all data. */
public class HidDeviceController {

    private static final String TAG = "HidDeviceController";

    /** Compound interface that listens to both device and service state changes. */
    public interface ProfileListener
            extends HidDeviceApp.DeviceStateListener, ServiceStateListener {}

    static final class InstanceHolder {
        static final HidDeviceController INSTANCE = createInstance();

        private static HidDeviceController createInstance() {
            return new HidDeviceController(new HidDeviceApp(), new HidDeviceProfile());
        }
    }


    private final HidDeviceApp hidDeviceApp;
    private final HidDeviceProfile hidDeviceProfile;

    private final Object lock = new Object();

    @GuardedBy("lock")
    private final Set<ProfileListener> listeners = new ArraySet<>();

    @GuardedBy("lock")
    @Nullable
    private BluetoothDevice connectedDevice;

    @GuardedBy("lock")
    @Nullable
    private BluetoothDevice waitingForDevice;

    @GuardedBy("lock")
    private boolean isAppRegistered;

    private static Timer timer = new Timer(Looper.getMainLooper().getThread().getName());

    private abstract static class ConnectionTimerTask extends TimerTask {
        BluetoothDevice deviceToConnectTo;

        @SuppressLint("RestrictedApi")
        public ConnectionTimerTask(BluetoothDevice device) {
            deviceToConnectTo = Preconditions.checkNotNull(device);
        }

        public abstract void run();
    }

    /**
     * @param hidDeviceApp HID Device App interface.
     * @param hidDeviceProfile Interface to manage paired HID Host devices.
     */
    @SuppressLint("RestrictedApi")
    private HidDeviceController(HidDeviceApp hidDeviceApp, HidDeviceProfile hidDeviceProfile) {
        this.hidDeviceApp = Preconditions.checkNotNull(hidDeviceApp);
        this.hidDeviceProfile = Preconditions.checkNotNull(hidDeviceProfile);
    }

    /**
     * Retrieve the singleton instance of the class.
     *
     * @return Singleton instance.
     */
    public static HidDeviceController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Ensure that the HID Device SDP record is registered and start listening for the profile proxy
     * and HID Host connection state changes.
     *
     * @param context Context that is required to listen for battery charge.
     * @param listener Callback that will receive the profile events.
     * @return Interface for managing the paired HID Host devices.
     */
    @SuppressLint("RestrictedApi")
    @MainThread
    public HidDeviceProfile register(Context context, ProfileListener listener) {
        synchronized (lock) {
            if (!listeners.add(listener)) {
                // This user is already registered
                return hidDeviceProfile;
            }
            if (listeners.size() > 1) {
                // There are already some users
                return hidDeviceProfile;
            }

            context = Preconditions.checkNotNull(context).getApplicationContext();
            hidDeviceProfile.registerServiceListener(context, profileListener);
            hidDeviceApp.registerDeviceListener(profileListener);
        }
        return hidDeviceProfile;
    }

    /**
     * Stop listening for the profile events. When the last listener is unregistered, the SD record
     * for HID Device will also be unregistered.
     *
     * @param context Context that is required to listen for battery charge.
     * @param listener Callback to unregisterDeviceListener.
     */
    @SuppressLint("RestrictedApi")
    @MainThread
    public void unregister(Context context, ProfileListener listener) {
        synchronized (lock) {
            if (!listeners.remove(listener)) {
                // This user was removed before
                return;
            }
            if (!listeners.isEmpty()) {
                // Some users are still left
                return;
            }

            context = Preconditions.checkNotNull(context).getApplicationContext();
            hidDeviceApp.unregisterDeviceListener();

            for (BluetoothDevice device : hidDeviceProfile.getConnectedDevices()) {
                hidDeviceProfile.disconnect(device);
            }

            hidDeviceApp.setDevice(null);
            hidDeviceApp.unregisterApp();

            hidDeviceProfile.unregisterServiceListener();

            connectedDevice = null;
            waitingForDevice = null;
        }
    }

    /**
     * Check if there is any active connection present.
     *
     * @return {@code true} if HID Host is connected, {@code false} otherwise.
     */
    public boolean isConnected() {
        return (connectedDevice != null);
    }

    /**
     * Initiate connection sequence for the specified HID Host. If another device is already
     * connected, it will be disconnected first. If the parameter is {@code null}, then the service
     * will only disconnect from the current device.
     *
     * @param device New HID Host to connect to or {@code null} to disconnect.
     */
    @MainThread
    public void requestConnect(BluetoothDevice device) {
        synchronized (lock) {
            waitingForDevice = device;
            if (!isAppRegistered) {
                // Request will be fulfilled as soon the as app becomes registered.
                return;
            }

            connectedDevice = null;
            updateDeviceList();

            if (device != null && device.equals(connectedDevice)) {
                for (ProfileListener listener : listeners) {
                    if (listener != null) {
                        listener.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTED);
                    }
                }
            }
        }
    }

    @MainThread
    public void requestConnect(BluetoothDevice device, int timeout) {
        if (device == null) {
            // Perform disconnect without timeout
            requestConnect(null);
            return;
        }

        // Schedule connection check with given timeout
        timer.schedule(new ConnectionTimerTask(device) {
            @Override
            public void run() {
                if (hidDeviceProfile.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Device " + device + " is connected");
                    return;
                }
                // If we are not in connected state then just disconnect
                Log.d(TAG, "Device " +  device + " not yet connected. Next failure will abort!");
                requestConnect(null);
            }
        }, timeout);
        requestConnect(device);
    }

    private final ProfileListener profileListener =
            new ProfileListener() {
                @Override
                @MainThread
                public void onServiceStateChanged(BluetoothProfile proxy) {
                    synchronized (lock) {
                        if (proxy == null) {
                            if (isAppRegistered) {
                                // Service has disconnected before we could unregister the app.
                                // Notify listeners, update the UI and internal state.
                                onAppStatusChanged(false);
                            }
                        } else {
                            hidDeviceApp.registerApp(proxy);
                        }
                        updateDeviceList();
                        for (ProfileListener listener : listeners) {
                            if (listener != null) {
                                listener.onServiceStateChanged(proxy);
                            }
                        }
                    }
                }

                @Override
                @MainThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    synchronized (lock) {
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            // A new connection was established. If we weren't expecting that, it
                            // must be an incoming one. In that case, we shouldn't try to disconnect
                            // from it.
                            waitingForDevice = device;
                        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            // If we are disconnected from a device we are waiting to connect to, we
                            // ran into a timeout and should no longer try to connect.
                            if (device == waitingForDevice) {
                                waitingForDevice = null;
                            }
                        }
                        updateDeviceList();
                        for (ProfileListener listener : listeners) {
                            if (listener != null) {
                                listener.onConnectionStateChanged(device, state);
                            }
                        }
                    }
                }

                @Override
                @MainThread
                public void onAppStatusChanged(boolean registered) {
                    synchronized (lock) {
                        if (isAppRegistered == registered) {
                            // We are already in the correct state.
                            return;
                        }
                        isAppRegistered = registered;

                        for (ProfileListener listener : listeners) {
                            if (listener != null) {
                                listener.onAppStatusChanged(registered);
                            }
                        }
                        if (registered && waitingForDevice != null) {
                            // Fulfill the postponed request to connect.
                            requestConnect(waitingForDevice);
                        }
                    }
                }

                @Override
                @MainThread
                public void onInterruptData(BluetoothDevice device,
                                            int reportId, byte[] data,
                                            BluetoothHidDevice inputHost) {
                    synchronized (lock) {
                        if (data == null) {
                            // No data to process - nothing to do
                            return;
                        }

                        for (ProfileListener listener : listeners) {
                            if (listener != null) {
                                listener.onInterruptData(device, reportId, data, inputHost);
                            }
                        }
                    }
                }
            };

    @MainThread
    private void updateDeviceList() {
        synchronized (lock) {
            BluetoothDevice connected = null;

            // If we are connected to some device, but want to connect to another (or disconnect
            // completely), then we should disconnect all other devices first.
            for (BluetoothDevice device : hidDeviceProfile.getConnectedDevices()) {
                if (device.equals(waitingForDevice) || device.equals(connectedDevice)) {
                    connected = device;
                } else {
                    hidDeviceProfile.disconnect(device);
                }
            }

            // If there is nothing going on, and we want to connect, then do it.
            if (hidDeviceProfile
                    .getDevicesMatchingConnectionStates(
                            new int[]{
                                    BluetoothProfile.STATE_CONNECTED,
                                    BluetoothProfile.STATE_CONNECTING,
                                    BluetoothProfile.STATE_DISCONNECTING
                            })
                    .isEmpty()
                    && waitingForDevice != null) {
                hidDeviceProfile.connect(waitingForDevice);
            }

            if (connectedDevice == null && connected != null) {
                connectedDevice = connected;
                waitingForDevice = null;
            } else if (connectedDevice != null && connected == null) {
                connectedDevice = null;
            }
            hidDeviceApp.setDevice(connectedDevice);
        }
    }
}
