package de.wiosense.wiokey.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

public class DeviceManagementEvent {

    public enum Type {
        SELECT_DEFAULT_DEVICE,
        REGISTER_NEW_DEVICE,
        MANUAL_CONNECT_DEVICE,
        DISCONNECT_DEVICE,
        REMOVE_DEVICE
    }

    public Type mType;
    public BluetoothDevice mDevice;
    public int mConnectionState;

    public DeviceManagementEvent(DeviceManagementEvent.Type type){
        this.mType = type;
        this.mDevice = null;
        this.mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    }

    public DeviceManagementEvent(DeviceManagementEvent.Type type, BluetoothDevice device){
        this.mType = type;
        this.mDevice = device;
        this.mConnectionState = BluetoothProfile.STATE_CONNECTING;
    }

    public DeviceManagementEvent(DeviceManagementEvent.Type type, BluetoothDevice device, int connectionState){
        this.mType = type;
        this.mDevice = device;
        this.mConnectionState = connectionState;
    }
}
