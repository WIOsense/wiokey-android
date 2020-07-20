/**
 * Based on initial work of Constants.java from WearMouse, which comes with the
 * following copyright notice, licensed under the Apache License, Version 2.0
 *
 * Copyright 2018, Google LLC All Rights Reserved.
 *
 * Modified to model the HID representation of a security key virtual connection
 * based on the CTAP2 HID interrupt driven protocol available at:
 * https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#usb
 */

package de.wiosense.wiokey.bluetooth;

import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;

public class Constants {

    // Android Bluetooth HID Profile seems to have a 64 capped MTU for L2CAP
    // SCO  out of which first byte is dedicated to the HIDP protocol header
    // and the second byte is used by the report Id encapsulation leaving
    // 62 bytes available for the HID payload (frame size)

    private static final byte[] HID_REPORT_DESC = {
            (byte)0x06, (byte)0xD0, (byte)0xF1,             // Usage Page (FIDO_USAGE_PAGE, 2 bytes)
            (byte)0x09, (byte)0x01,                             // Usage (FIDO_USAGE_U2FHID)
            (byte)0xA1, (byte)0x01,                             // Collection (Application)

            (byte)0x09, (byte)0x20,                             // Usage (FIDO_USAGE_DATA_IN)
            (byte)0x15, (byte)0x00,                             // Logical Minimum (0)
            (byte)0x26, (byte)0xFF, (byte)0x00,                 // Logical Maximum (255, 2 bytes)
            (byte)0x75, (byte)0x08,                             // Report Size (8)
            (byte)0x95, (byte)de.wiosense.webauthn.fido.hid.Constants.HID_REPORT_SIZE,      // Report Count (variable)
            (byte)0x81, (byte)0x02,                             // Input (Data, Absolute, Variable)

            (byte)0x09, (byte)0x21,                             // Usage (FIDO_USAGE_DATA_OUT)
            (byte)0x15, (byte)0x00,                             // Logical Minimum (0)
            (byte)0x26, (byte)0xFF, (byte)0x00,                 // Logical Maximum (255, 2 bytes)
            (byte)0x75, (byte)0x08,                             // Report Size (8)
            (byte)0x95, (byte)de.wiosense.webauthn.fido.hid.Constants.HID_REPORT_SIZE,                  // Report Count (variable)
            (byte)0x91, (byte)0x02,                             // Output (Data, Absolute, Variable)

            (byte)0xC0                                      // End Collection
    };

    private static final String SDP_NAME = "WioKey";
    private static final String SDP_DESCRIPTION = "FIDO2/U2F Android OS Security Key";
    private static final String SDP_PROVIDER = "WIOsense GmbH &amp; Co. KG";
    private static final int QOS_TOKEN_RATE = 1000; // pedantic choice to be under the peak BW
    private static final int QOS_TOKEN_BUCKET_SIZE = de.wiosense.webauthn.fido.hid.Constants.HID_REPORT_SIZE + 1;
    private static final int QOS_PEAK_BANDWIDTH = 2000; // 2Mbps is maximum reliable over Bluetooth
    private static final int QOS_LATENCY = 5000;

    public final static BluetoothHidDeviceAppSdpSettings SDP_RECORD =
            new BluetoothHidDeviceAppSdpSettings(
                    Constants.SDP_NAME,
                    Constants.SDP_DESCRIPTION,
                    Constants.SDP_PROVIDER,
                    BluetoothHidDevice.SUBCLASS1_COMBO,
                    Constants.HID_REPORT_DESC);

    public final static BluetoothHidDeviceAppQosSettings QOS_OUT =
            new BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    Constants.QOS_TOKEN_RATE,
                    Constants.QOS_TOKEN_BUCKET_SIZE,
                    Constants.QOS_PEAK_BANDWIDTH,
                    Constants.QOS_LATENCY,
                    BluetoothHidDeviceAppQosSettings.MAX);
}
