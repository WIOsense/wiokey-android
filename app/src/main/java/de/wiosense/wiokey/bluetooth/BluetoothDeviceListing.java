package de.wiosense.wiokey.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

public class BluetoothDeviceListing {

    private static final String TAG = "BluetoothDeviceListing";
    private static final String HID_PREFERENCES = "hidbtdev";
    private static final String HID_PREFERRED_HOST = "DEFAULT_HID_HOST";
    private static final String HID_RANDOM_HOST = "RANDOM_HID_HOST";

    private BluetoothAdapter bluetoothAdapter;
    private HidDeviceProfile hidDeviceProfile;
    private SharedPreferences hidPreferences;

    private final class BluetoothDeviceStringModel {
        String devName;
        String devAddress;
        String dev;

        @SuppressLint("RestrictedApi")
        BluetoothDeviceStringModel(BluetoothDevice device) {
            Preconditions.checkNotNull(device);
            devName = device.getName();
            devAddress = device.getAddress();
            String devMajorDeviceClass = Integer.toString(device.getBluetoothClass().getMajorDeviceClass());
            String devDeviceClass = Integer.toString(device.getBluetoothClass().getDeviceClass());
            dev = devName + devAddress + devMajorDeviceClass + devDeviceClass;
        }

        private String bytesToHexString(@NonNull byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }

        String getHash() {
            String devHash;
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(dev.getBytes());
                devHash = bytesToHexString(messageDigest.digest());
            } catch (NoSuchAlgorithmException e) {
                Log.d(TAG, "Failed to use SHA-256 for hashing - using literal representation");
                devHash = dev;
            }

            return devHash;
        }
    }

    @SuppressLint("RestrictedApi")
    public BluetoothDeviceListing(Context context, final HidDeviceProfile hidDeviceProfile) {
        hidPreferences = Preconditions.checkNotNull(context).
                getSharedPreferences(HID_PREFERENCES, Context.MODE_PRIVATE);
        bluetoothAdapter = Preconditions.checkNotNull(BluetoothAdapter.getDefaultAdapter());
        this.hidDeviceProfile = Preconditions.checkNotNull(hidDeviceProfile);
    }

    @SuppressLint("RestrictedApi")
    public List<BluetoothDevice> getAvailableDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> availableDevices = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            if (hidDeviceProfile.isProfileSupported(device)) {
                BluetoothClass devClass = device.getBluetoothClass();
                Log.d(TAG, "Bluetooth device: " + device + " is of class: " + devClass.getMajorDeviceClass());
                Log.d(TAG, "Device Hash:" + device.hashCode());
                if (devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
                    availableDevices.add(device);
                }
            }
        }
        return availableDevices;
    }

    public List<BluetoothDevice> getHidAvailableDevices() {
        List<BluetoothDevice> availableDevices = getAvailableDevices();
        List<BluetoothDevice> removeDevices = new ArrayList<BluetoothDevice>();

        for (BluetoothDevice device: availableDevices) {
            BluetoothDeviceStringModel deviceModel = new BluetoothDeviceStringModel(device);
            if (!hidPreferences.contains(deviceModel.getHash())) {
                Log.d(TAG, device + " is not paired as an HID host");
                removeDevices.add(device);
            }
        }

        availableDevices.removeAll(removeDevices);

        return availableDevices;
    }

    public boolean cacheHidDefaultDevice(BluetoothDevice device) {
        if (device != null && hidDeviceProfile.isProfileSupported(device)
                && device.getBondState() == BluetoothDevice.BOND_BONDED) {
            return saveHidPreference(device, HID_PREFERRED_HOST);
        }
        return false;
    }

    public boolean cacheHidDevice(BluetoothDevice device) {
        if (device != null && hidDeviceProfile.isProfileSupported(device)
                && device.getBondState() == BluetoothDevice.BOND_BONDED) {
            return saveHidPreference(device, HID_RANDOM_HOST);
        }
        return false;
    }

    public boolean isHidDevice(BluetoothDevice device) {
        boolean retcode = false;
        if (device != null) {
            BluetoothDeviceStringModel deviceModel = new BluetoothDeviceStringModel(device);
            String deviceHash = deviceModel.getHash();
            retcode = hidPreferences.contains(deviceHash);
        }
        return retcode;
    }

    public boolean isHidDefaultDevice(BluetoothDevice device) {
        String hidDefaultHost = hidPreferences.getString(HID_PREFERRED_HOST, null);
        if (device != null) {
            BluetoothDeviceStringModel deviceModel = new BluetoothDeviceStringModel(device);
            String deviceHash = deviceModel.getHash();
            return (deviceHash.equals(hidDefaultHost));
        }
        if (hidDefaultHost == null) {
            // No current default
            return true;
        }
        return false;
    }

    @Nullable
    public BluetoothDevice getHidDefaultDevice() {
        List<BluetoothDevice> pairedDevices = getAvailableDevices();
        if (pairedDevices.size() > 0) {
            String preferredDevice = hidPreferences.getString(HID_PREFERRED_HOST, null);
            if (preferredDevice == null) {
                return null;
            }

            // At this point we had some previous preference so try to see if it is still paired
            for (BluetoothDevice device: pairedDevices) {
                BluetoothDeviceStringModel deviceModel = new BluetoothDeviceStringModel(device);
                if (deviceModel.getHash().equals(preferredDevice)) {
                    Log.d(TAG, "Found bounded preferred device: " + device);
                    return device;
                }
            }

            // At this point we end up with no currently bonded devices matching the preferred
            // one, so simply clear the preference to reflect the reality
            Log.d(TAG, "Failed to find previous preference " + preferredDevice + " among bonded devices");
            if (clearHidPreference(null, HID_PREFERRED_HOST)) {
                Log.d(TAG, "Preference cleared");
            } else {
                Log.d(TAG, "Preference kept");
            }

        }
        return null;
    }

    public boolean clearHidDevice(BluetoothDevice device) {
        boolean retcode = true;
        if (device != null) {
            if (isHidDefaultDevice(device)) {
                retcode = clearHidPreference(device, HID_PREFERRED_HOST);
            }
            if (isHidDevice(device)) {
                retcode &= clearHidPreference(device, HID_RANDOM_HOST);
            }
        }
        return retcode;
    }

    private boolean saveHidPreference(BluetoothDevice device, String type) {
        BluetoothDeviceStringModel deviceModel = new BluetoothDeviceStringModel(device);
        if (type.equals(HID_PREFERRED_HOST)) {
            return hidPreferences.edit()
                    .putString(HID_PREFERRED_HOST, deviceModel.getHash())
                    .commit();
        }
        if (type.equals(HID_RANDOM_HOST)) {
            return hidPreferences.edit()
                    .putString(deviceModel.getHash(), HID_RANDOM_HOST)
                    .commit();
        }
        // Unknown type of preference
        return false;
    }

    private boolean clearHidPreference(BluetoothDevice device, String type) {
        if (type.equals(HID_PREFERRED_HOST)) {
            return hidPreferences.edit()
                    .remove(HID_PREFERRED_HOST)
                    .commit();
        }
        if (type.equals(HID_RANDOM_HOST)) {
            BluetoothDeviceStringModel deviceModel = new BluetoothDeviceStringModel(device);
            String deviceHash = deviceModel.getHash();
            if (hidPreferences.contains(deviceHash)) {
                return hidPreferences.edit()
                        .remove(deviceHash)
                        .commit();
            }
        }
        return false;
    }
}
