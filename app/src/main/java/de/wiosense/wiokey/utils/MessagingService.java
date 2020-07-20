package de.wiosense.wiokey.utils;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import de.wiosense.wiokey.ui.WioNotificationManager;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "WioKey|MessagingService";

    @Override
    public void onMessageReceived(RemoteMessage message){
        super.onMessageReceived(message);

        if(message.getNotification() != null){
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();
            String channel = message.getNotification().getChannelId();
            String key = null;
            String value = null;
            Log.d(TAG,"Data supplied size: "+message.getData().entrySet().size());
            if(message.getData().entrySet().iterator().hasNext()) {
                key = message.getData().entrySet().iterator().next().getKey();
                value = message.getData().entrySet().iterator().next().getValue();
            } else{
                Log.d(TAG,"No extra data provided for notification");
            }
            String sound = message.getNotification().getSound();
            Log.d(TAG,"Received notification with priority: "+channel+", key value: "+value+" and sound "+sound);
            WioNotificationManager.displayNotification(getApplicationContext(),title,body,channel,key,value,sound);
        }

    }

}
