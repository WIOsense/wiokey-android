package de.wiosense.wiokey.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import static android.content.Context.MODE_PRIVATE;
import static de.wiosense.wiokey.MainActivity.PREFERENCES;
import static de.wiosense.wiokey.ui.WioNotificationManager.PUSHNOTIFICATIONSTOPIC;

public class FirebaseManager {

    private static final String TAG = "WioKey|FirebaseManager";
    private Context ctx;

    private static FirebaseAnalytics mFirebaseAnalytics;
    //public FirebaseDatabase mDatabase;

    /*
     * SharedPreferences
     */
    public static final String SUBSCRIBE_EMAIL = "subscribeEmail";
    public static final String EMAIL_PUSHED = "emailRegistered";
    public static final String FIREBASE_DBID = "firebaseDbid";
    public static final String FIREBASE_OPTOUT_EVENTS = "optoutEvents";
    public static final String FIREBASE_OPTOUT_CRASHES = "optoutCrashes";
    public static final String FIREBASE_OPTOUT_NOTIFICATIONS = "optoutNotifications";

    /*
     * Firebase
     */
    public static final String EVENT_DEVICEREGISTERED = "device_registered";
    public static final String EVENT_DEVICECONNECTED = "device_connected";
    public static final String EVENT_ACCOUNTREGISTERED = "account_registered";
    public static final String EVENT_ACCOUNTLOGIN = "account_login";
    public static final String EVENT_U2F_REGISTRATION = "u2f_account_registration";
    public static final String EVENT_U2F_AUTHENTICATION = "u2f_account_authentication";
    public static final String EVENT_ACCOUNTDELETED = "deleted_account";
    public static final String EVENT_NUMBEROFACCOUNTS = "number_of_accounts";
    public static final String EVENT_APPSHARED = "app_shared";
    public static final String EVENT_WEBSITEOPENED = "website_opened";
    public static final String EVENT_ONPOPUP = "popup_opened";
    public static final String DB_FIREBASEID = "firebaseid";
    public static final String DB_EMAIL = "email";

    public FirebaseManager(Context ctx){

        this.ctx = ctx;

        SharedPreferences appPreferences = ctx.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        boolean optout_events = appPreferences.getBoolean(FIREBASE_OPTOUT_EVENTS,false);
        boolean optout_crashes = appPreferences.getBoolean(FIREBASE_OPTOUT_CRASHES,false);
        boolean optout_notifications = appPreferences.getBoolean(FIREBASE_OPTOUT_NOTIFICATIONS,false);

        FirebaseApp.initializeApp(ctx);
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if(!task.isSuccessful()){
                    Log.e(TAG,"Firebase Get InstanceId failed");
                    return;
                }
                String token = task.getResult().getToken();
                Log.d(TAG,"Firebase InstanceId: "+token);
            }
        });

        //mDatabase = FirebaseDatabase.getInstance();
        //mDatabase.setPersistenceEnabled(true);
        subscribeAnalytics(!optout_events);
        subscribeCrash(!optout_crashes);
        subscribeNotifications(!optout_notifications);
    }

    public static void sendLogEvent(String event, String value){
        if(mFirebaseAnalytics!=null){
            Log.d(TAG,"Sending Firebase Log - Event: "+event+" Value: "+value);
            Bundle params = new Bundle();
            if(value!=null){
                params.putString(FirebaseAnalytics.Param.VALUE, value);
            }
            mFirebaseAnalytics.logEvent(event, params);
        } else {
            Log.d(TAG,"Log not sent. User has opted out from receiving analytic events");
        }
    }

    public static void sendLogEvent(String event, int value){
        if(mFirebaseAnalytics!=null){
            Log.d(TAG,"Sending Firebase Log - Event: "+event+" Value: "+value);
            Bundle params = new Bundle();
            params.putInt(FirebaseAnalytics.Param.VALUE, value);
            mFirebaseAnalytics.logEvent(event, params);
        } else {
            Log.d(TAG,"Log not sent. User has opted out from receiving analytic events");
        }
    }

    public void subscribeAnalytics(boolean enable){
        Log.d(TAG,"User subscribe to analytics: "+enable);
        if(enable){
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx);
            mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
        } else{
            FirebaseAnalytics.getInstance(ctx).setAnalyticsCollectionEnabled(false);
            mFirebaseAnalytics = null;
        }
    }

    public void subscribeNotifications(boolean enable){
        Log.d(TAG,"User subscribe to notifications channel: "+enable);
        if(enable){
            FirebaseMessaging.getInstance().subscribeToTopic(PUSHNOTIFICATIONSTOPIC);
        } else{
            FirebaseMessaging.getInstance().unsubscribeFromTopic(PUSHNOTIFICATIONSTOPIC);
        }
    }

    public void subscribeCrash(boolean enable){
        Log.d(TAG,"User subscribe to crashlytics: "+enable);
        if(enable){
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        } else{
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
        }
    }

}
