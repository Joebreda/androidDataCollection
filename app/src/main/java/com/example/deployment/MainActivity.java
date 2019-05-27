package com.example.deployment;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HardwarePropertiesManager;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    boolean stress = true;
    Timer timer = new Timer ();
    int curNumThreads = 0;
    public static boolean isRecursionEnable = true;
    int stage = 0;

    // used for passing to logger service so multiple buttons can use the same code.
    boolean rooted;
    // used such that timer is only scheduled once per run of the app.
    //TODO this means that if a user tries to collect data using scheduled job it will only work the first time
    boolean timerBeenScheduled = false;
    TextView loggingIndicator;



    ///////////////////////////////// Automated tests //////////////////////////////////////////////


    class ShuffleSortThread extends Thread {
        ShuffleSortThread() {
            // Create a new, second thread
            super("Shuffle Sort Thread");
            start(); // Start the thread
        }

        // This is the entry point for the second thread.
        public void run() {
            ArrayList<String> randoms = new ArrayList<>();
            for(int i = 0; i < 1000; i++){
                byte[] array = new byte[7]; // length is bounded by 7
                new Random().nextBytes(array);
                String generatedString = new String(array, Charset.forName("UTF-8"));
                randoms.add(generatedString);
            }

            while(stress) {
                Collections.sort(randoms);
                Collections.shuffle(randoms);
            }

        }
    }

    void generateLoadThreads() {
        if (!isRecursionEnable)
            // Handle not to start multiple parallel threads
            return;
        // isRecursionEnable = false; when u want to stop
        // on exception on thread make it true again
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> randoms = new ArrayList<>();
                for(int i = 0; i < 1000; i++){
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


                while(stress) {
                    Collections.sort(randoms);
                    Collections.shuffle(randoms);
                }


            }
        }).start();
    }

    TimerTask automatedTest1 = new TimerTask() {
        @Override
        public void run() {
            Intent CPUintent = new Intent(MainActivity.this, stressCPUService.class);
            // reach steady state for 30 minutes
            if(stage == 0){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                //stress = false;
            }
            // Just CPU for 30 minutes
            else if (stage == 1){
                Log.i("stage " + stage, "Just CPU throttled at 100 for 30 minutes");
                stress = true; // used to turn on and off threads
                startService(CPUintent);
                //generateLoadThreads();
            }
            // cool off for 30 minutes
            else if (stage == 2){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                stress= false;
                stopService(CPUintent);

            }
            // just screen for 30 minutes
            else if (stage == 3){
                Log.i("stage " + stage, "Just screen on for 30 minutes");
                stress = false;
                // must be used whenever updating UI from thread other than main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
            }
            // cool off for 30 minutes
            else if (stage == 4){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                // turn off stress threads
                stress = false;
                // clear flags that keep screen on
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });

            }
            // screen + CPU for 30 minutes
            else if (stage == 5){
                Log.i("stage " + stage, "CPU throttled at 100 and screen on for 30 minutes");
                stress = true;
                // must be used whenever updating UI from thread other than main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
                // throttle CPU
                startService(CPUintent);
                //generateLoadThreads();
            }
            // cool off again for 30 minutes
            else if (stage == 6){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                stopService(CPUintent);
                // turn off stress threads
                stress = false;
                // clear flags that keep screen on
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
            }
            else {
                Log.i("stage " + stage, "done!");
                timer.cancel();
            }
            // increase stage counter once per run
            stage++;
        }
    };

    TimerTask CPUstressTask = new TimerTask () {
        @Override
        public void run () {
            Log.i("CPU stress task", "stressing CPU at 100%");
            stress = true;
            int NUM_THREADS = 4;
            for(int i = 0; i < NUM_THREADS; ++i) {
                new ShuffleSortThread(); // create a new thread
            }
        }
    };


    TimerTask automatedTest2 = new TimerTask() {
        @Override
        public void run() {
            Intent CPUintent = new Intent(MainActivity.this, stressCPUService.class);
            if(stage == 0){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                //stress = false;
            }
            else if (stage == 1){
                Log.i("stage " + stage, "Just screen on for 30 minutes");
                stress = false;
                // must be used whenever updating UI from thread other than main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
            }
            else if (stage == 2){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                // turn off stress threads
                stress = false;
                // clear flags that keep screen on
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
            }
            else if (stage == 3){
                Log.i("stage " + stage, "CPU throttled at 100 and screen on for 30 minutes");
                stress = true;
                // must be used whenever updating UI from thread other than main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
                // throttle CPU
                startService(CPUintent);
            }
            // cool off for 30 minutes
            else if (stage == 4){
                Log.i("stage " + stage, "doing nothing for 30 minutes");
                // turn off stress threads
                stress = false;
                // clear flags that keep screen on
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    }
                });
                stopService(CPUintent);
            }
            else {
                Log.i("stage " + stage, "done!");
                timer.cancel();
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            }
            // increase stage counter once per run
            stage++;
        }
    };


    TimerTask turnScreenOff = new TimerTask () {
        @Override
        public void run () {
            Log.i("screen off", "woo");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                }
            });

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = this.getPackageManager().queryIntentActivities( mainIntent, 0);
        for(int i = 0; i < pkgAppsList.size(); i++){
            Log.i("packages", pkgAppsList.get(i).toString());
        }

        setContentView(R.layout.activity_main);
        /* Obtains necessary permissions. */
        String[] permissions = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        String[] permissionNames = {"READ_PHONE_STATE", "WRITE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE"};
        int permissionsCode = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!permissionsGranted(permissionNames)){
                ActivityCompat.requestPermissions(this, permissions, permissionsCode);
            }
        }

        /* Obtains further permissions needed for NetworkStatsManager. */
        if (!usageAccessGranted()){
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }

        /* Implements functionality for the data logging buttons. */
        View.OnClickListener listenerLoggerButtons = new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this, TemperatureEstimateService.class);
                switch (view.getId()){
                    case R.id.startLogger:
                        rooted = true;
                        Log.i("JOE", "Starting Logger...");
                        intent.putExtra("isRootedButton", rooted);
                        startService(intent);
                        loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                        loggingIndicator.setText("Currently Logging Rooted...");
                        // TODO automated testing after starting logging
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                getWindow().addFlags(
                                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                            }
                        });
                        if(!timerBeenScheduled){
                            timer.schedule(turnScreenOff, 3600*1000); // turn screen off after 1 hour.
                            timerBeenScheduled = true;
                        }
                        break;
                    case R.id.stopLogger:
                        Log.i("JOE", "Stopping Logger...");
                        stopService(intent);
                        loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                        loggingIndicator.setText("Not logging...");
                        break;
                }
            }
        };
        findViewById(R.id.startLogger).setOnClickListener(listenerLoggerButtons);
        findViewById(R.id.stopLogger).setOnClickListener(listenerLoggerButtons);


        /* Implements functionality for the data logging buttons. */
        View.OnClickListener cpuStressButton = new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this, TemperatureEstimateService.class);
                switch (view.getId()){
                    case R.id.startUnrootedLogging:
                        rooted = false;
                        Log.i("JOE", "Starting Logger as NOT rooted user...");
                        intent.putExtra("isRootedButton", rooted);
                        startService(intent);
                        loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                        loggingIndicator.setText("Currently Logging unrooted...");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                getWindow().addFlags(
                                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                            }
                        });
                        if(!timerBeenScheduled){
                            timer.schedule(turnScreenOff, 3600*1000); // turn screen off after 1 hour.
                            timerBeenScheduled = true;
                        }
                        break;
                    case R.id.stopUnrootedLogging:
                        Log.i("JOE", "Stopping Logger as NOT rooted user......");
                        stopService(intent);
                        loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                        loggingIndicator.setText("Not logging...");
                        break;
                }
            }

        };
        findViewById(R.id.startUnrootedLogging).setOnClickListener(cpuStressButton);
        findViewById(R.id.stopUnrootedLogging).setOnClickListener(cpuStressButton);




        View.OnClickListener listenerEmail = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"jbreda@umass.edu"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "data from: " + Calendar.getInstance().getTime());
                File root = Environment.getExternalStorageDirectory();
                String pathToAttachedFile = DatabaseHelper.DATABASE_NAME;
                File file = new File(root, pathToAttachedFile);
                if (!file.exists()){
                    return;
                }

                //Uri uri = Uri.fromFile(file);
                Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "com.example.deployment.fileprovider", file);


                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                Log.i("email button", "2");
                startActivity(Intent.createChooser(emailIntent, "Pick an email provider."));
                Log.i("email button", "3");
            }
        };
        findViewById(R.id.emailDB).setOnClickListener(listenerEmail);

    }

    /* Check to see if usage access permission has been granted by user. */
    private boolean usageAccessGranted(){
        try{
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
                assert appOpsManager != null;
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            }
            return (mode == AppOpsManager.MODE_ALLOWED);
        }catch (PackageManager.NameNotFoundException e){
            return false;
        }
    }

    /* Check to see if other permissions have been granted. */
    private boolean permissionsGranted(String[] permissionNames){
        boolean permissionsGranted = true;
        for (String permission: permissionNames){
            permissionsGranted = permissionsGranted && (ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED);
        }
        return permissionsGranted;
    }
}
