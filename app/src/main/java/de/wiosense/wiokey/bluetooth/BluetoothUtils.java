/**
 * Based on initial work of BluetoothUtils.java from WearMouse, which comes with the
 * following copyright notice, licensed under the Apache License, Version 2.0
 *
 * Copyright 2018, Google LLC All Rights Reserved.
 */

package de.wiosense.wiokey.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Method;

import androidx.annotation.Nullable;

// TODO: Hide this class in this package - as the methods are "priviledged"

/** Helper class that exposes some hidden methods from the Android framework. */
public class BluetoothUtils {
    private static final String TAG = "BluetoothUtils";

    private static final Method methodCancelBondProcess = lookupCancelBondProcess();
    private static final Method methodRemoveBond = lookupRemoveBond();
    private static final Method methodSetScanMode = lookupSetScanMode();

    /** Cancel an in-progress bonding request started with {@link #createBond}. */
    public static boolean cancelBondProcess(BluetoothDevice device) {
        if (methodCancelBondProcess != null) {
            try {
                return (Boolean) methodCancelBondProcess.invoke(device);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking cancelBondProcess", e);
            }
        }
        return false;
    }

    /**
     * Remove bond (pairing) with the remote device.
     *
     * <p>Delete the link key associated with the remote device, and immediately terminate
     * connections to that device that require authentication and encryption.
     */
    public static boolean removeBond(BluetoothDevice device) {
        if (methodRemoveBond != null) {
            try {
                return (Boolean) methodRemoveBond.invoke(device);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking removeBond", e);
            }
        }
        return false;
    }

    /**
     * Set the Bluetooth scan mode of the local Bluetooth adapter.
     *
     * <p>The Bluetooth scan mode determines if the local adapter is connectable and/or discoverable
     * from remote Bluetooth devices.
     *
     * <p>For privacy reasons, discoverable mode is automatically turned off after <code>duration
     * </code> seconds. For example, 120 seconds should be enough for a remote device to initiate
     * and complete its discovery process.
     */
    public static boolean setScanMode(BluetoothAdapter adapter, int mode, int duration) {
        if (methodSetScanMode != null) {
            try {
                return (Boolean) methodSetScanMode.invoke(adapter, mode, duration);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking setScanMode", e);
            }
        }
        return false;
    }

    /**
     * Queries whether or not the HID device profile is enabled for the local Bluetooth adapter.
     *
     * <p>The Bluetooth HID device profile is necessary to emulate a virtual HID connection
     * with a host and to implement the wireless CTAPHID protocol over Bluetooth link.
     *
     * @return {@code true} if profile is supported by the queried adapter or {@code false} if the
     * profile is not supported by the adapter or the adapter instance is null.
     */
    @Nullable
    private static Method lookupCancelBondProcess() {
        try {
            return BluetoothDevice.class.getMethod("cancelBondProcess");
        } catch (Exception e) {
            Log.e(TAG, "Error looking up cancelBondProcess", e);
        }
        return null;
    }

    @Nullable
    private static Method lookupRemoveBond() {
        try {
            return BluetoothDevice.class.getMethod("removeBond");
        } catch (Exception e) {
            Log.e(TAG, "Error looking up removeBond", e);
        }
        return null;
    }

    @Nullable
    private static Method lookupSetScanMode() {
        try {
            return BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
        } catch (Exception e) {
            Log.e(TAG, "Error looking up setScanMode", e);
        }
        return null;
    }
}