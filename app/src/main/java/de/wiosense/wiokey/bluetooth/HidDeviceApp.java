/**
 * Based on initial work of HidDeviceApp.java from WearMouse, which comes with the
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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.BinderThread;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

/** Helper class that holds all data about the HID Device's SDP record and wraps data sending. */
public class HidDeviceApp {

    private static final String TAG = "HidDeviceApp";

    /** Used to call back when a device connection state has changed. */
    public interface DeviceStateListener {
        /**
         * Callback that receives the new device connection state.
         *
         * @param device Device that was connected or disconnected.
         * @param state New connection state, see {@link BluetoothProfile#EXTRA_STATE}.
         */
        @MainThread
        void onConnectionStateChanged(BluetoothDevice device, int state);

        /** Callback that receives the app unregister event. */
        @MainThread
        void onAppStatusChanged(boolean registered);

        /** Callback that handles the interrupt requests of the current device
         *
         *
         */
        void onInterruptData(BluetoothDevice device, int reportId,
                             byte[] data, BluetoothHidDevice inputHost);
    }

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    @Nullable private BluetoothDevice device;
    @Nullable private DeviceStateListener deviceStateListener;

    /** Callback to receive the HID Device's SDP record state. */
    private final BluetoothHidDevice.Callback callback =
            new BluetoothHidDevice.Callback() {
                @Override
                @BinderThread
                public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
                    super.onAppStatusChanged(pluggedDevice, registered);
                    HidDeviceApp.this.registered = registered;
                    HidDeviceApp.this.onAppStatusChanged(registered);
                }

                @Override
                @BinderThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    super.onConnectionStateChanged(device, state);
                    HidDeviceApp.this.onConnectionStateChanged(device, state);
                }

                @Override
                @BinderThread
                public void onGetReport(BluetoothDevice device , byte type, byte id, int bufferSize) {
                    super.onGetReport(device, type, id, bufferSize);

                    if (inputHost != null) {
                        if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE) {
                            byte[] data = {0x00, 0x05, 0x02, 0x06, 0x01, 0x05, 0x06, 0x00};
                            inputHost.replyReport(device, type, id, data);
                        } else {
                            inputHost.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                        }
                    }
                }


                @Override
                @BinderThread
                public void onSetReport(BluetoothDevice device , byte type, byte id, byte[] data) {
                    super.onSetReport(device, type, id, data);

                    if (inputHost != null) {
                        inputHost.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
                    }
                }

                @Override
                @BinderThread
                public void onInterruptData(BluetoothDevice device , byte reportId, byte[] data) {
                    super.onInterruptData(device, reportId, data);

                    if (inputHost!= null && device != null) {
                        HidDeviceApp.this.onInterruptData(device, reportId, data, inputHost);
                    }
                }
            };

    @Nullable private BluetoothHidDevice inputHost;
    private boolean registered;

    /**
     * Register the HID Device's SDP record.
     *
     * @param inputHost Interface for managing the paired HID Host devices and sending the data.
     */
    @SuppressLint("RestrictedApi")
    @MainThread
    void registerApp(BluetoothProfile inputHost) {
        this.inputHost = Preconditions.checkNotNull((BluetoothHidDevice) inputHost);
        this.inputHost.registerApp(
                Constants.SDP_RECORD, null, Constants.QOS_OUT, Runnable::run, callback);
    }

    /** Unregister the HID Device's SDP record. */
    @MainThread
    void unregisterApp() {
        if (inputHost != null && registered) {
            inputHost.unregisterApp();
        }
        inputHost = null;
    }

    /**
     * Start listening for device connection state changes.
     *
     * @param listener Callback that will receive the new device connection state.
     */
    @SuppressLint("RestrictedApi")
    @MainThread
    void registerDeviceListener(DeviceStateListener listener) {
        deviceStateListener = Preconditions.checkNotNull(listener);
    }

    /** Stop listening for device connection state changes. */
    @MainThread
    void unregisterDeviceListener() {
        deviceStateListener = null;
    }

    /**
     * Notify that we have a new HID Host to send the data to.
     *
     * @param device New device or {@code null} if we should stop sending any data.
     */
    @MainThread
    public void setDevice(@Nullable BluetoothDevice device) {
        this.device = device;
    }

    @BinderThread
    private void onConnectionStateChanged(BluetoothDevice device, int state) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onConnectionStateChanged(device, state);
            }
        });
    }

    @BinderThread
    private void onAppStatusChanged(boolean registered) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onAppStatusChanged(registered);
            }
        });
    }

    @BinderThread
    private void onInterruptData(BluetoothDevice device,
                                 byte reportId,
                                 byte[] data,
                                 BluetoothHidDevice inputHost) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onInterruptData(device, reportId, data, inputHost);
            }
        });
    }
}
