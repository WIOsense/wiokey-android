package de.wiosense.wiokey;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt.CryptoObject;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.wiosense.webauthn.Authenticator;
import de.wiosense.webauthn.TransactionManager;
import de.wiosense.webauthn.fido.hid.Framing;
import de.wiosense.webauthn.util.WioBiometricPrompt;
import de.wiosense.webauthn.util.WioBiometricPrompt.PromptCallback;

import de.wiosense.webauthn.util.WioRequestDialog;
import de.wiosense.wiokey.ui.WioNotificationManager;
import de.wiosense.wiokey.ui.fragments.HomeFragment;
import de.wiosense.wiokey.utils.DeviceManagementEvent;
import de.wiosense.wiokey.utils.FirebaseManager;

import static de.wiosense.wiokey.ui.WioNotificationManager.CHANNEL_HIGH;
import static de.wiosense.wiokey.ui.WioNotificationManager.CHANNEL_LOW;
import static de.wiosense.wiokey.ui.WioNotificationManager.CHANNEL_MAX;
import static de.wiosense.wiokey.ui.WioNotificationManager.CHANNEL_MEDIUM;
import static de.wiosense.wiokey.ui.WioNotificationManager.KEY_ACTION;
import static de.wiosense.wiokey.ui.WioNotificationManager.KEY_URL;
import static de.wiosense.wiokey.ui.WioNotificationManager.VALUE_SUBSCRIBE;
import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_ACCOUNTLOGIN;
import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_ACCOUNTREGISTERED;
import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_U2F_AUTHENTICATION;
import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_U2F_REGISTRATION;
import static de.wiosense.wiokey.utils.FirebaseManager.sendLogEvent;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "WioKey|MainActivity";

    BluetoothAdapter btAdapter;
    public FirebaseManager mFirebaseManager;
    BottomNavigationView navView;

    private static long lastVerified;
    private static boolean onForeground;
    public static boolean hasCredentials;
    public static boolean autoConnectFlag;
    public static boolean bluetoothOff = false;
    public static boolean bluetoothUnavailable = false;

    public static final String PREFERENCES = "WioPreferences";
    public static final String FIRST_START = "firstStart";
    public static final String PREVIOUS_VERSION = "previousVersion";
    public static final String HAS_HW_STORAGE = "hasHwStorage";
    public static final String HAS_CREDENTIALS = "hasCredentials";
    public static final String ASK_CREDENTIALS = "askForCredentials";

    private static final long VERIFY_REPROMPT_MINS = 2;

    private static final Handler foregroundHandler = new Handler();
    private static final int IN_BACKGROUND_DELAY_MS = 300;
    private boolean skipOneTimeBiometricCheck = false;

    private static class AuthListener
            implements Framing.WebAuthnListener, Framing.U2fAuthnListener {
        @Override
        public void onCompleteMakeCredential() {
            sendLogEvent(EVENT_ACCOUNTREGISTERED, null);
        }

        @Override
        public void onCompleteGetAssertion() {
            sendLogEvent(EVENT_ACCOUNTLOGIN, null);
        }

        @Override
        public void onRegistrationResponse() {
            sendLogEvent(EVENT_U2F_REGISTRATION, null);
        }

        @Override
        public void onAuthenticationResponse() {
            sendLogEvent(EVENT_U2F_AUTHENTICATION, null);
        }
    }

    private static AuthListener authenticatorListener = new AuthListener();

    @Nullable
    public static TransactionManager mTransactionManager;
    public static Authenticator mAuthenticator;

    private final PromptCallback biometricTimeCb = new PromptCallback(false) {
        @Override
        public void onResult(boolean result, CryptoObject cryptoObject) {
            if (result) {
                lastVerified = Calendar.getInstance().getTimeInMillis();
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        showWarningDialog(true);
                        bluetoothOff = true;
                        stopService();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        showWarningDialog(false);
                        bluetoothOff = false;
                        startService();
                        break;
                }
            }
        }
    };

    public static boolean isOnForeground() {
        return onForeground;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_credential, R.id.navigation_about)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        /*
         * Notifications
         */
        WioNotificationManager.createNotificationChannel(this,CHANNEL_MAX, NotificationManager.IMPORTANCE_HIGH);
        WioNotificationManager.createNotificationChannel(this,CHANNEL_HIGH, NotificationManager.IMPORTANCE_DEFAULT);
        WioNotificationManager.createNotificationChannel(this,CHANNEL_MEDIUM, NotificationManager.IMPORTANCE_LOW);
        WioNotificationManager.createNotificationChannel(this,CHANNEL_LOW, NotificationManager.IMPORTANCE_MIN);

        /*
         * Firebase
         */
        mFirebaseManager = new FirebaseManager(this);

        /*
         * Bluetooth
         */
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver,filter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null){
            bluetoothUnavailable = true;
        } else {
            if(!btAdapter.isEnabled()){
                bluetoothOff = true;
            }
        }

        if (!bluetoothOff) {
            startService();
        }

        /*
         * Preferences
         */
        SharedPreferences appPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = appPreferences.edit();

        boolean firstStart = appPreferences.getBoolean(FIRST_START, true);
        final int previousVersion = appPreferences.getInt(PREVIOUS_VERSION, 0);
        final int currVersion = BuildConfig.VERSION_CODE;

        if (previousVersion != currVersion) {
            // Fresh install / re-install
            editor.putInt(PREVIOUS_VERSION, currVersion).commit();
            firstStart = true;
        }

        KeyguardManager keyguardManager = (KeyguardManager)this.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            hasCredentials = keyguardManager.isDeviceSecure();
        } else {
            hasCredentials = false;
        }

        if (firstStart) {
            showIntro();
            // We finish here
            // Tutorial will redirect to splash screen which handles credential check and
            // Splash screen returns to MainActivity on success, otherwise blocks access
            finish();
        }

        /*
         * Biometric prompt
         */
        lastVerified = 0; // Always verify on create
        WioBiometricPrompt.clearCallbacks(); // Make sure there are no other callbacks
        WioBiometricPrompt.registerCallback(biometricTimeCb); // Update last verified time

        /*
         * Device Management
         */
        autoConnectFlag = true;

        //Intent check
        handleNotificationIntents(getIntent());
    }

    @Override
    protected void onStart(){
        super.onStart();

        if (mTransactionManager != null) {
            mTransactionManager.updateActivity(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        foregroundHandler.removeCallbacksAndMessages(null);
        onForeground = true;

        if (mTransactionManager == null) {
            try {
                boolean hasStrongbox = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE);
                if (!hasStrongbox) {
                    Log.e(TAG, "There is no Strongbox, defaulting to HSM/TEE");
                }
                mAuthenticator = new Authenticator(this, hasStrongbox);
            } catch (Exception e) {
                //TODO: Remember to handle this
            }

            mTransactionManager = new TransactionManager(this, mAuthenticator);
            mTransactionManager.registerListener((Framing.WebAuthnListener) authenticatorListener);
            mTransactionManager.registerListener((Framing.U2fAuthnListener) authenticatorListener);
        }
        biometricCheck();

    }

    @Override
    protected void onNewIntent(Intent intent){
        Log.d(TAG,"On New Intent with extras: "+intent.toString());
        super.onNewIntent(intent);
        handleNotificationIntents(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        foregroundHandler.postDelayed(() -> {
            onForeground = false;
        }, IN_BACKGROUND_DELAY_MS);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if(mBroadcastReceiver!=null){
            unregisterReceiver(mBroadcastReceiver);
        }
        EventBus.getDefault().post(new DeviceManagementEvent(
                DeviceManagementEvent.Type.DISCONNECT_DEVICE
        ));
        stopService();
    }

    @Override
    public void onBackPressed(){
        if(navView.getSelectedItemId() == R.id.navigation_home){
            super.onBackPressed();
            finishAndRemoveTask();
        } else{
            navView.setSelectedItemId(R.id.navigation_home);
        }
    }

    public void startService(){
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(){
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    public void turnBluetoothOn(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
    }

    private void showWarningDialog(boolean visible){
        Fragment navHostFragment = getSupportFragmentManager().getPrimaryNavigationFragment();
        if (navHostFragment != null) {
            Fragment fragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            try {
                ((HomeFragment) fragment).onClickWarning(visible);
            } catch (ClassCastException e) {
                if (visible) {
                    WioRequestDialog bluetoothEnableDialog = WioRequestDialog.create(
                            getString(R.string.bluetoothoff_dialog_title),
                            getString(R.string.bluetoothoff_dialog_message),
                            new WioRequestDialog.PromptCallback() {
                                @Override
                                public void onResult(boolean result) {
                                    if (result) {
                                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                        startActivityForResult(enableBtIntent, 1);
                                    } else {
                                        Log.w(TAG, "User denied Bluetooth re-activation");
                                    }
                                }
                            });
                    bluetoothEnableDialog.show(Objects.requireNonNull(fragment.getActivity()));
                }
            }
        }
    }

    private void showIntro() {
        Intent intent = new Intent(MainActivity.this, IntroActivity.class);
        intent.putExtra(MainActivity.HAS_CREDENTIALS, hasCredentials);
        intent.putExtra(MainActivity.ASK_CREDENTIALS, true);

        startActivity(intent);
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void biometricCheck() {
        if (skipOneTimeBiometricCheck) {
            // Skip this time the check -> done in the Splash lock screen
            skipOneTimeBiometricCheck = false;
        } else {
            new Handler().post(biometricRunnable);
        }
    }

    private final Runnable biometricRunnable = new Runnable() {
        @Override
        public void run() {
            long now = Calendar.getInstance().getTimeInMillis();
            if ((now - lastVerified)/60000 < VERIFY_REPROMPT_MINS) {
                // Nothing to do - we still trust previous authentication
                return;
            }

            if (!hasCredentials) {
                // Go to splash activity and let it handle the lack of credentials
                startActivity(new Intent(MainActivity.this, SplashActivity.class));
                return;
            }

            WioBiometricPrompt.registerCallback(
                new WioBiometricPrompt.PromptCallback(true) {
                @Override
                public void onResult(boolean result, CryptoObject cryptoObject) {
                    // Go to splash activity if the user was not verified
                    if (!result) {
                        startActivity(new Intent(MainActivity.this,
                                      SplashActivity.class));
                        finish();
                    }
                }
            });

            new WioBiometricPrompt(MainActivity.this,
                    getString(R.string.biometrics_promptTitle),
                    getString(R.string.biometrics_promptSubtitle),
                    true);
        }
    };

    private void handleNotificationIntents(Intent intent){
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            try {
                if(intent.hasExtra(KEY_URL)){
                    String value = intent.getExtras().getString(KEY_URL);
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(value)));
                } else if(intent.hasExtra(KEY_ACTION)){
                    if(intent.getExtras().get(KEY_ACTION).equals(VALUE_SUBSCRIBE)){
                        //new SubscribeDialog(this,mDatabase,false);
                        Log.w(TAG,"Email subscription currently not implemented");
                    }
                }
            } catch (NullPointerException e){
                Log.w(TAG,"No data provided from Firebase notification");
            }
            if (intent.hasExtra(SplashActivity.EXTRA_AUTH_RESULT)) {
                skipOneTimeBiometricCheck = bundle.getBoolean(SplashActivity.EXTRA_AUTH_RESULT);
            }
        }
    }
}
