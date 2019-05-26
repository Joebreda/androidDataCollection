package com.example.deployment;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class stressCPUService extends Service {
    public static boolean isRecursionEnable;
    int curNumThreads;
    boolean stress;
    private PowerManager mpowerManager;
    private PowerManager.WakeLock partialWakeLock;
    final static int myID = 1345;


    @Override
    public void onCreate(){
        curNumThreads = 0;
        isRecursionEnable = true;
        stress = true;
        Log.i("HELLO", "CPU stress service on create");
        mpowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        partialWakeLock = mpowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpustress:partialWakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        partialWakeLock.acquire();
        /*
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            // min 26
            String NOTIFICATION_CHANNEL_ID = "com.example.deployment";
            String channelName = "Temperature data gathering";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    //.setSmallIcon(R.drawable.icon_1)
                    .setContentTitle("CPU is being stressed")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(myID, notification);
        } else {
            // For min 25
            // notification to allow app to run in foreground and increase data collection
            Intent notificationIntent = new Intent(this, TemperatureEstimateService.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);
            Notification notification =
                    new Notification.Builder(this)
                            .setContentTitle("CPU stressor")
                            .setContentText("Stressing CPU")
                            //.setSmallIcon(R.drawable.icon)
                            .setContentIntent(pendingIntent)
                            //.setTicker(getText(R.string.ticker_text))
                            .build();
            startForeground(myID, notification);
        }
        */
        Log.i("HELLO", "CPU stress service on start");


        //TODO this is where we are generating the load.
        //launchStressApp();
        //Log.e("stressApp", "Stress app launched");
        generateLoadThreads();
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        if(partialWakeLock.isHeld()){
            partialWakeLock.release();
        }
        Log.i("HELLO", "CPU stress service on destroy");
        stress = false;
        curNumThreads = 0;
        //isRecursionEnable = true;
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }


    public void launchStressApp() {
        Intent launchStress = getPackageManager().getLaunchIntentForPackage("xyz.bluephoenix.stresscpu");
        startActivity(launchStress);
    }



    void generateLoadThreads() {
        if (!isRecursionEnable)
            // Handle not to start multiple parallel threads
            return;
        // on exception on thread make it true again
        Thread currThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> randoms = new ArrayList<>();
                for(int i = 0; i < 500; i++){
                    byte[] array = new byte[7]; // length is bounded by 7
                    new Random().nextBytes(array);
                    String generatedString = new String(array, Charset.forName("UTF-8"));
                    randoms.add(generatedString);
                }

                if(curNumThreads < 4){
                    generateLoadThreads();
                    curNumThreads++;
                } else {
                    isRecursionEnable = false;
                }
                Log.i("HELLO", "generating load for thread number " + curNumThreads);
                while(stress) {
                    Collections.sort(randoms);
                    Collections.shuffle(randoms);
                }
            }
        });

        currThread.setPriority(Thread.MAX_PRIORITY);
        currThread.start();
    }
}
