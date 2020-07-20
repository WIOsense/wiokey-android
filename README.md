# WioKey - Authenticator App (Beta)

[<img src="app/src/main/res/mipmap-xhdpi/ic_launcher.png"/>](https://www.wiokey.de) [<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" width=280px />](https://play.google.com/store/apps/details?id=de.wiosense.wiokey)

Get an additional layer of security with WioKey and access your favorite web services using only your phone’s biometrics! We offer support for both second factor authentication and passwordless logins leveraging the U2F and FIDO2 standards.

You can learn more about WioKey by visiting our [our website](https://wiokey.de)! For the hasty, have a glimpse of the app in action over the [tutorials](https://wiokey.de/tutorials).

**Note: It's important for you to always set a backup second factor authentication method for all your accounts that are registered with WioKey (in case you might loose your phone / delete the app)! We recommend the usage of recovery codes!**

## First time setup:
We use Bluetooth as transport for the communications between your phone and computer, so you'll need a working Bluetooth module on your PC for the app to work. Here is a step by step guide for the pairing process with a Windows computer.

[![First time setup video](https://img.youtube.com/vi/EDray7H3wd8/maxresdefault.jpg)](https://www.youtube.com/watch?v=EDray7H3wd8)

### Known compatibility issues
Due to our usage of [Bluetooth HID device emulation](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice) and [BiometricPrompt](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt) capabilities introduced in Android API level 28, you will need to have an Android 9.0+ phone in order to use the application.

### Windows 10 Host
 * Supported by all major browsers (Chrome, Firefox, Edge)
 * Interacting directly with Windows Hello
 * Capable of being used as security key for desktop login with AAD / Hybrid AD tenants

### macOS Host
 * Currently supported just by Chrome

### Linux Host
 * Currently supported just by Chrome as long as the udev rules are setup such that the HID profile is accessible by non-root users. This should be the case by default with distros with `systemd --version` greater than `systemd 244`. If that is not the case simply add the right udev rules:
 ```
    KERNEL=="hidraw*", SUBSYSTEM=="hidraw", MODE="0664", GROUP="plugdev"
 ```
 as
 ```bash
    sudo nano /etc/udev/rules.d/99-hidraw-permissions.rules
 ```

### Mobile Devices
Not all manufacturers offer support for the Android Bluetooth HID device profile (e.g. **OnePlus**, **Nokia**), so if the app does not behave as expected, please make sure that such APIs are [available](https://play.google.com/store/apps/details?id=com.rkaneapplabs.bluetooth_hid.bluetoothproxy) to start with.

## Demo usage:
We have prepared step by step tutorials on how to use the app with the following websites:

 * [Google](https://youtu.be/9WqH7CQ1MF0) (U2F)
 * [Microsoft accout](https://youtu.be/haslyaDC2HU) (Passwordless access)
 * [Facebook](https://youtu.be/lVLLq0J7trk) (U2F)
 * [WioKey demo website](https://youtu.be/hQFFPhdOt70) (Passwordless access)

You can get a more comprehensive list of compatible services [here](https://www.wiokey.de/#compatibility)!

## WIOsense Roaming Authenticator
This app is using the open source [WIOsense Roaming Authenticator Android](https://github.com/WIOsense/rauth-android) library as the base FIDO2 authenticator. Check out the code to get more information on the usage of the submodule.

**NOTE**: This repository is meant to serve as an usage example of the [roaming authenticator libary](https://github.com/WIOsense/rauth-android) for Android devices and also provide an outlook of the WioKey application code. The code will try as much as possible to follow the Google Play Store versioning and apply the critical updates for a secure and bug-free user experience, but is really meant for developers to play around, experiment, provide bug/security reports. If you just want the WioKey app please [download the latest update](https://play.google.com/store/apps/details?id=de.wiosense.wiokey) from Google Play Store.

## License
The application is open source is released under the terms of [BSD3 license](LICENSE). Some parts of the code were modified from other open source projects (see [INFO](INFO)) or leverage the Android Open Source Project (see [NOTICE](NOTICE)), being marked with their original license terms and copyright information.

**NOTE**: The open source version of the application differs from the Google Play version by using development dummy batch certificates and Firebase analytics key IDs.

## Privacy
WioKey is built based on a privacy-first approach and limits the any data collection and analytics to just fully anonymous crash reports which can be at any time turned off from the `About Fragment`. For more information about privacy make sure to check out the in-app privacy policy.

## Security
If you identify any security problems or vulnerabilities within WioKey itself, please contact vulnerability@wiosense.de with a security disclosure and report of the identified issues.

We practice a fortnight (2 weeks) security disclosure policy, time in which we will try to address the problems and provide a fix. Therefore we kindly ask you to delay any planned public vulnerability disclosure either until 2 weeks have passed or a fix has been issued to allow for this process to take place.

Contributions are of course welcome!

As WioKey is an app running on a fully fledged mobile OS it certainly trades off some hardware hardened security guarantees towards convenience. However, it tries to leverage modern mobile OS security to its fullest. To this point the app uses the Android Keystore and requires the existance of at least one screen lock mechanism (e.g. PIN, pattern, Fingerprint, Face Unlock) to securely store your generated credentials private keys. These are stored by default in hardware-backed components, either in a Trusted Execution Environment (TEE) or a dedicated hardware Secure Element (SE) which Android OS asserts to harden and prevent the extraction of the key materials. The usage of the keys is by default protected by biometric or screen lock authentication as means of user verification. You can check the location of your existing credentials at any time from the `Credentials Fragment` by clicking on the Chip icon in the top left corner.

**NOTE**: Currently the app is capable on running on rooted phones but this is just meant to not limit user choice. This is NOT a security vulnerability but a design choice, considering that such uses and security vulnerabilities of rooted phones are fully understood by their users. In future, the production application or features therof are not guaranteed to be available on rooted phones.
