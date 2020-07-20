package de.wiosense.wiokey.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UiUpdateEvent {

    public enum Type {
        UPDATE_HID_DEVICES_REQ,
        UPDATE_HID_DEVICES,
        UPDATE_DEFAULT_HID_DEVICE_REQ,
        UPDATE_DEFAULT_HID_DEVICE,
        UPDATE_CONN_ANIMATION,
        UPDATE_TOAST,
        UPDATE_SCREEN_KEEP_ALIVE_ON,
        UPDATE_SCREEN_KEEP_ALIVE_OFF,
    }

    public UiUpdateEvent.Type mType;
    public Map<BluetoothDevice, Integer> mDevices;
    public String mToast;

    public UiUpdateEvent(UiUpdateEvent.Type type){
        this.mType = type;
        this.mDevices = null;
        this.mToast = null;
    }

    public UiUpdateEvent(UiUpdateEvent.Type type,
                         List<BluetoothDevice> devices) {
        this(type);
        this.mDevices = new HashMap<>();
        if (devices != null) {
            for (BluetoothDevice device : devices) {
                this.mDevices.put(device, BluetoothProfile.STATE_DISCONNECTED);
            }
        } else {
            this.mDevices.put(null, BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    public UiUpdateEvent(UiUpdateEvent.Type type,
                         BluetoothDevice defaultDevice,
                         int connectedState) {
        this(type);
        this.mDevices = new HashMap<>();
        this.mDevices.put(defaultDevice, connectedState);
    }

    public UiUpdateEvent(UiUpdateEvent.Type type, String toastText) {
        this(type);
        if (toastText != null) {
            mToast = toastText;
        }
    }
}
