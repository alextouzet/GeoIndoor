package com.example.alexandre.geoindoor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class onMessageReceived extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "enter onMessageReceived");
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().get("receiver").equals(getSharedPreferences("id", 0).getString("id", "0")))
                sendNotification(remoteMessage);
        }
    }

    private void sendNotification(RemoteMessage remoteMessage) {

        String dataTitle = remoteMessage.getData().get("title");
        String dataMessage = remoteMessage.getData().get("message");
        String dataAsked = remoteMessage.getData().get("asked");
        String dataReceiver = remoteMessage.getData().get("receiver");
        String dataSender = remoteMessage.getData().get("sender");
        String dataLamp = remoteMessage.getData().get("lamp");
        String dataLatitude = remoteMessage.getData().get("latitude");
        String dataLongitude = remoteMessage.getData().get("longitude");

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("title", dataTitle);
        intent.putExtra("message", dataMessage);
        intent.putExtra("asked", dataAsked);
        intent.putExtra("receiver", dataReceiver);
        intent.putExtra("sender", dataSender);
        intent.putExtra("lamp", dataLamp);
        intent.putExtra("latitude", dataLatitude);
        intent.putExtra("longitude", dataLongitude);

        int requestID = (int) System.currentTimeMillis();

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(dataTitle)
                .setContentText(dataMessage)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}