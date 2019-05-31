package com.example.deployment;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    //Timer timer = new Timer ();
    boolean stress = true;
    Timer timer;
    // used for passing to logger service so multiple buttons can use the same code.
    boolean rooted;
    // used such that timer is only scheduled once per run of the app.
    TextView loggingIndicator;
    EditText inputField;
    Switch rootedSwitch;
    Switch CPUSwitch;
    Switch screenSwitch;
    //Intent stressIntent;

    /////////////////////////////// Methods used to turn screen off ////////////////////////////////

    public void releaseScreenLock(){
        Log.i("screen", "turning off");
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

    //////////////////////////////// Methods for stressing CPU /////////////////////////////////////

    private class RSA {
        private int bitlen;
        /* renamed from: d */
        private BigInteger f6d;
        /* renamed from: e */
        private BigInteger f7e;
        /* renamed from: n */
        private BigInteger f8n;

        private RSA(BigInteger newn, BigInteger newe) {
            this.bitlen = 1024;
            this.f8n = newn;
            this.f7e = newe;
        }

        private RSA(int bits) {
            this.bitlen = 1024;
            this.bitlen = bits;
            SecureRandom r = new SecureRandom();
            BigInteger p = new BigInteger(this.bitlen / 2, 100, r);
            BigInteger q = new BigInteger(this.bitlen / 2, 100, r);
            this.f8n = p.multiply(q);
            BigInteger m = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
            this.f7e = new BigInteger("3");
            while (m.gcd(this.f7e).intValue() > 1) {
                this.f7e = this.f7e.add(new BigInteger("2"));
            }
            this.f6d = this.f7e.modInverse(m);
        }

        private synchronized BigInteger encrypt(BigInteger message) {
            return message.modPow(this.f7e, this.f8n);
        }

        private synchronized BigInteger decrypt(BigInteger message) {
            return message.modPow(this.f6d, this.f8n);
        }

        private synchronized void generateKeys() {
            SecureRandom r = new SecureRandom();
            BigInteger p = new BigInteger(this.bitlen / 2, 100, r);
            BigInteger q = new BigInteger(this.bitlen / 2, 100, r);
            this.f8n = p.multiply(q);
            BigInteger m = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
            this.f7e = new BigInteger("3");
            while (m.gcd(this.f7e).intValue() > 1) {
                this.f7e = this.f7e.add(new BigInteger("2"));
            }
            this.f6d = this.f7e.modInverse(m);
        }
    }

    class RSALoadThread extends Thread {
        RSALoadThread() {

        }
        public void run() {
            while(stress){
                MainActivity.this.callRSA();
            }
        }
    }

    private void callRSA() {
        RSA rsa = new RSA(2048);
        rsa.decrypt(rsa.encrypt(BigInteger.probablePrime(2048, new Random())));
    }

    public void generateRSALoadThreads() {
        stress = true;
        for(int i = 0; i < Runtime.getRuntime().availableProcessors(); i++){
            new RSALoadThread().start();
        }

    }

    /////////////////////// wrappers to call automated screen and CPU switches /////////////////////

    public void idleExperiment(Intent intent, int seconds){
        // screen remains off and no load is generated
        Log.i("EXPERIMENT IN EXECUTION", "IDLE");
        intent.putExtra("generatingFullLoad", false);
        intent.putExtra("seconds", seconds);
    }


    public void justScreenExperiment(Intent intent, int seconds){
        Log.i("EXPERIMENT IN EXECUTION", "JUST SCREEN");
        intent.putExtra("generatingFullLoad", false);
        intent.putExtra("seconds", seconds);
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

    public void ScreenAndCPUExperiment(Intent intent, int seconds){
        Log.i("EXPERIMENT IN EXECUTION", "CPU AND SCREEN");
        // NOTE it is better that we generate the load in the service as mainActivity performs to much to consistently keep load at 100

        // when generating stress in this way, we get 100% utilization
        intent.putExtra("generatingFullLoad", true);
        intent.putExtra("seconds", seconds);
        // when generating stress in this way we get around 90% utilization
        /*
        if(generateLoadInSeparateService){
            startService(stressIntent);
        }
        */
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO in the case where we would like to use a separate service for generating load.
        //stressIntent = new Intent(MainActivity.this, stressCPUService.class);

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

        // switch for setting rooted or not rooted
        rootedSwitch = (Switch) findViewById(R.id.rootSwitch);
        screenSwitch = (Switch) findViewById(R.id.screenSwitch);
        CPUSwitch = (Switch) findViewById(R.id.CPUSwitch);
        inputField = (EditText) findViewById(R.id.inputField);

        /* Implements functionality for the data logging buttons. */
        View.OnClickListener listenerLoggerButtons = new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this, TemperatureEstimateService.class);
                switch (view.getId()){
                    case R.id.startLogger:
                        String input = inputField.getText().toString();
                        int durationOfExperiment = Integer.parseInt(input);
                        // reset timer
                        timer = new Timer();

                        // switch which dictates if service should log as if on rooted device or not
                        boolean rootSwitchState = rootedSwitch.isChecked();
                        if(rootSwitchState){
                            rooted = true;
                            Log.e("switchCheck", "Logging rooted data because of switch");
                            loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                            loggingIndicator.setText("Currently Logging Rooted...");
                        } else {
                            rooted = false;
                            Log.e("switchCheck", "Logging NOT rooted data because of switch");
                            loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                            loggingIndicator.setText("Currently Logging unrooted...");
                        }
                        intent.putExtra("isRootedButton", rooted);

                        //TODO PLEASE DO NOT FORGET TO SET THIS VALUE TO WHATEVER DURATION YOUD LIKE THE SCREEN TO BE ON FOR.
                        timer.schedule(
                                new TimerTask(){
                                    public void run(){
                                        releaseScreenLock();
                                    }
                                }, durationOfExperiment*1000);

                        // Switches for running idle, justScreen, or screen+stressCPU
                        boolean screenSwitchState = screenSwitch.isChecked();
                        boolean CPUSwitchState = CPUSwitch.isChecked();
                        if (CPUSwitchState && screenSwitchState){
                            // makes changes to intent therefore must be called before startService
                            ScreenAndCPUExperiment(intent, durationOfExperiment);
                        }
                        else if(screenSwitchState){
                            justScreenExperiment(intent, durationOfExperiment);
                        } else {
                            idleExperiment(intent, durationOfExperiment);
                        }
                        Log.i("JOE", "Starting Logger...");
                        startService(intent);
                        break;
                    case R.id.stopLogger:
                        Log.i("JOE", "Stopping Logger...");
                        timer.cancel();
                        stress = false;
                        releaseScreenLock();
                        stopService(intent);
                        loggingIndicator = (TextView)findViewById(R.id.loggingIndicator);
                        loggingIndicator.setText("Not logging...");
                        break;
                }
            }
        };
        findViewById(R.id.startLogger).setOnClickListener(listenerLoggerButtons);
        findViewById(R.id.stopLogger).setOnClickListener(listenerLoggerButtons);



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
