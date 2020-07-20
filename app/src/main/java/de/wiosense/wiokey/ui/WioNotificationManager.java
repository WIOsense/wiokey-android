package de.wiosense.wiokey.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import de.wiosense.wiokey.MainActivity;
import de.wiosense.wiokey.R;

import static android.app.Notification.DEFAULT_ALL;

public class WioNotificationManager {

    // Notification Params
    public static final String CHANNEL_MAX = "CHANNEL_MAX";
    public static final String CHANNEL_HIGH = "CHANNEL_HIGH";
    public static final String CHANNEL_MEDIUM = "CHANNEL_MEDIUM";
    public static final String CHANNEL_LOW = "CHANNEL_LOW";
    public static final String KEY_URL = "URL";
    public static final String KEY_ACTION = "ACTION";
    public static final String VALUE_SUBSCRIBE = "SUBSCRIBE";
    public static final String PUSHNOTIFICATIONSTOPIC = "NOTIFICATIONS_ENABLED";

    public static void displayNotification(Context ctx, String title, String body, String channel, String key, String value, String sound){

        PendingIntent pendingIntent= null;
        String channelId=channel;

        if(key!=null) {
            if (key.equals(KEY_URL)) {
                Intent notifyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
                notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                pendingIntent = PendingIntent.getActivity(ctx, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else if (key.equals(KEY_ACTION)) {
                Intent notifyIntent = new Intent(ctx, MainActivity.class);
                notifyIntent.putExtra(KEY_ACTION, value);
                pendingIntent = PendingIntent.getActivity(ctx, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }

        if(channel==null){
            channelId=CHANNEL_HIGH;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx,channelId)
                .setSmallIcon(R.drawable.logo_small)
                .setContentTitle(title)
                .setContentText(body)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if(sound != null && sound.equals("default")){
            builder.setDefaults(DEFAULT_ALL);
        }else{
            builder.setDefaults(0);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.notify(3,builder.build());
    }

    public static void createNotificationChannel(Context ctx, String channelid, int priority) {
        NotificationChannel serviceChannel = new NotificationChannel(
                channelid,
                "WioKey Remote Messaging Channel",
                priority
        );
        serviceChannel.setShowBadge(true);
        serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = ctx.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
