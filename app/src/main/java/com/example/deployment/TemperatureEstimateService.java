package com.example.deployment;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HardwarePropertiesManager;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.topjohnwu.superuser.io.SuRandomAccessFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_BATTERY;
import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU;
import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_SKIN;
import static android.os.HardwarePropertiesManager.TEMPERATURE_CURRENT;

public class TemperatureEstimateService extends Service {


    // either update or second
    private String collectionStyle = "second";
    private boolean rooted;


    // for the systematic thread generation for black-box model data collection
    boolean stress = true;
    int numThreads;
    Timer timer = new Timer ();
    boolean timerBeenScheduled = false;

    final static int myID = 8989;
    private boolean isRunning = false;
    private DatabaseHelper database;
    private Context context;
    // data collection options
    private TemperatureEstimateServiceHandler TemperatureEstimateServiceHandler;
    private BatteryInfoReceiver batteryInfoReceiver;




    //////////////// declare managers globally so they can be used in all methods //////////////////

    // Specifically for sensors
    private SensorManager mSensorManager;
    private Sensor proximitySensor;
    private Sensor lightSensor;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private SensorDataStorage currentSensorData;
    // hardware manager
    private HardwarePropertiesManager mHardwareManger;
    // power manager
    private PowerManager mpowerManager;
    private PowerManager.WakeLock partialWakeLock;
    // for tracking frequency of updates received
    private double recieveCounter = 0;
    private String firstTimestamp = "";
    private Date firstDateTime = null;

    /* Date and time formatting related things. */
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    DecimalFormat decimalFormat2 = new DecimalFormat("0.0000");
    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");



    class ShuffleSortThread extends Thread {
        ShuffleSortThread() {
            // Create a new, second thread
            super("Shuffle Sort Thread");
            start(); // Start the thread
        }

        // This is the entry point for the second thread.
        public void run() {
            /*
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
            */
            int counter = 0;
            while(stress){
                counter++;
            }
        }
    }


    TimerTask automatedGenerateThreads = new TimerTask() {
        @Override
        public void run() {
            //TODO figure out how to set CPU frequency manually
            //TODO add number of threads
            if(numThreads == 0){
                Log.i("GENERATING THREADS", numThreads + " threads running");
            }
            else if(numThreads <= 4){
                Log.i("GENERATING THREADS", numThreads + "threads running");
                stress = true;
                new ShuffleSortThread(); // create a new thread
            }
            else if(numThreads == 5){
                Log.i("COOLING OFF", "all numbers of threads have been run for the full duration.");
                stress = false;
            }
            else {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                timer.cancel();
            }
            numThreads++;
        }
    };

    TimerTask stopLoad = new TimerTask() {
        @Override
        public void run() {
            Log.e("CPU STRESS FROM SERVICE", "TURNING OFF CPU STRESS AND SETTING NUMTHREADS TO 0");
            stress = false;
            numThreads = 0;
        }
    };

    public void generateFullLoadFor(int seconds){
        stress = true;
        numThreads = 5;
        int NUM_THREADS = 4;
        for(int i = 0; i < NUM_THREADS; ++i) {
            new ShuffleSortThread(); // create a new thread
        }
        if(!timerBeenScheduled){
            timer.schedule(stopLoad, seconds*1000); // turn screen off after 1 hour. 3600+1000
            timerBeenScheduled = true;
        }
    }

    public void generateThreadsOneAtATime(int secondsInitialDelay, int secondsDelayBetween){
        numThreads = 0;
        timer.schedule(automatedGenerateThreads, secondsInitialDelay, secondsDelayBetween*1000); // repeats task every 30 minutes
    }


    @Override
    public void onCreate(){
        // initiate thread to monitor software and hardware sensor values
        HandlerThread handlerThread = new HandlerThread("TemperatureEstimateThread", Process.THREAD_PRIORITY_BACKGROUND);
        // TODO UNSURE IF THIS WILL ACTUALLY JUST FORGROUND THE PROCESS SO WE NEED NOT WAKELOCK.
        //HandlerThread handlerThread = new HandlerThread("TemperatureEstimateThread", Process.THREAD_PRIORITY_FOREGROUND);
        handlerThread.start();

        // Conditional for data collection style
        if(collectionStyle.equals("second")){
            Log.i("collection Style", "Collecting data set to once per second");
            Looper looper = handlerThread.getLooper();
            TemperatureEstimateServiceHandler = new TemperatureEstimateServiceHandler(looper);
        } else if(collectionStyle.equals("update")){
            Log.i("collection Style", "Collecting data set to on update of battery temperature");
            batteryInfoReceiver = new BatteryInfoReceiver();
            // register the battery info receiver
            this.registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } else {
            Log.i("collection Style", "PLEASE SELECT A COLLECTION STYLE");
        }

        isRunning = true;
        // database stuff
        context = getApplicationContext();
        this.database = new DatabaseHelper(context);
        // initiate power manager and wakelock
        mpowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        partialWakeLock = mpowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DataCollection:partialWakeLock");

        // initiate sensor managers
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // local class to store values of sensors upon update
        currentSensorData = new SensorDataStorage();
        // register sensors
        mSensorManager.registerListener(SensorListener, proximitySensor, 2 * 1000 * 1000);
        mSensorManager.registerListener(SensorListener, lightSensor, 2 * 1000 * 1000);
        mSensorManager.registerListener(SensorListener, accelerometer, 2 * 1000 * 1000);
        mSensorManager.registerListener(SensorListener, gyroscope, 2 * 1000 * 1000);
        // only worked on rooted devices!
        mHardwareManger = (HardwarePropertiesManager) getSystemService(Context.HARDWARE_PROPERTIES_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //String yes = intent.getStringExtra("isRootedButton");
        rooted = intent.getExtras().getBoolean("isRootedButton");
        boolean generatingFullLoad = intent.getExtras().getBoolean("generatingFullLoad");
        int seconds = intent.getExtras().getInt("seconds");

        //int seconds = intent.getIntExtra("seconds", 0);
        Log.e("extras passed", "root: " + rooted + ", generating high load: " + generatingFullLoad + ", for " + seconds + " seconds.");
        // trigger wakelock on start of data collection
        if(rooted){
            partialWakeLock.acquire();
        }
        if(generatingFullLoad){
            generateFullLoadFor(seconds);
        } else {
            numThreads = 0;
        }
        //generateThreadsOneAtATime(0, 15);

        // make message display on start
        if(collectionStyle.equals("second")){
            Message message = TemperatureEstimateServiceHandler.obtainMessage();
            message.arg1 = startId;
            TemperatureEstimateServiceHandler.sendMessage(message);
        }

        if(rooted){
            Toast.makeText(this, "starting data logging for ROOTED device", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "starting data logging for NOT ROOTED device", Toast.LENGTH_SHORT).show();
        }

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
                    .setContentTitle("App is running in background")
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
                            .setContentTitle("Temperature Estimator")
                            .setContentText("Estimating Temperature in Room")
                            //.setSmallIcon(R.drawable.icon)
                            .setContentIntent(pendingIntent)
                            //.setTicker(getText(R.string.ticker_text))
                            .build();
            startForeground(myID, notification);
        }
        // If service is killed while starting, it restarts
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        // clean up variables which are infinitely loop data collection and stress generation as well as timer.
        isRunning = false;
        stress = false;
        timer.cancel();
        if(collectionStyle.equals("update")){
            this.unregisterReceiver(batteryInfoReceiver);
            // calculate the duration of the test in seconds
            Date currentTime = Calendar.getInstance().getTime();
            double totalDuration = (currentTime.getTime() - firstDateTime.getTime())/1000;
            double frequency = recieveCounter / totalDuration;
            Log.i("total duration was ", totalDuration + " seconds");
            Log.i("Frequency of Update", recieveCounter + " updates per second for " + totalDuration + " seconds!");
            Log.i("frequency of update", "frequency of update was: " + frequency);
        }
        Toast.makeText(this, "Data logging stopped.", Toast.LENGTH_SHORT).show();
        // export database to location accessible by email button
        database.exportDB();
        database.clearDB();
        // without this line, it is literally impossible to change the database structure without reinstalling the all
        // and the app needs to be a device admin and therefore can never be uninstalled...
        context.deleteDatabase(database.DATABASE_NAME);
        // release wakelock before closing application
        if(rooted){
            partialWakeLock.release();
        }
    }

    SensorEventListener SensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // update current sensor data object with current sensor values
            if(sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT){
                currentSensorData.updateLight(sensorEvent.values[0]);
            } else if(sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY){
                currentSensorData.updateProxy(sensorEvent.values[0]);
            } else if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                currentSensorData.updateAccel(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            } else if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                currentSensorData.updateGyro(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //Log.i("onSensorAccuracyChange", "theres been an accuracy change");
        }
    };

    private class SensorDataStorage {
        // hardware sensors
        double prox_val;

        double acc_val_x;
        double acc_val_y;
        double acc_val_z;

        double light_val;

        double gyro_val_x;
        double gyro_val_y;
        double gyro_val_z;


        private SensorDataStorage() {
            // hardware sensors
            prox_val = 0;
            acc_val_x = 0;
            acc_val_y = 0;
            acc_val_z = 0;
            light_val = 0;
            gyro_val_x = 0;
            gyro_val_y = 0;
            gyro_val_z = 0;
        }

        private void updateProxy(double val){ prox_val = val; }
        private void updateAccel(double x, double y, double z){
            acc_val_x = x;
            acc_val_y = y;
            acc_val_z = z;
        }
        private void updateLight(double val){
            light_val = val;
        }
        private void updateGyro(double x, double y, double z){
            gyro_val_x = x;
            gyro_val_y = y;
            gyro_val_z = z;
        }

        private double getProx_val(){
            return prox_val;
        }

        private double getAcc_x(){ return acc_val_x; }
        private double getAcc_y(){ return acc_val_y; }
        private double getAcc_z(){ return acc_val_z; }

        private double getLight_val(){
            return light_val;
        }

        private double getGyro_x(){
            return gyro_val_x;
        }
        private double getGyro_y(){
            return gyro_val_y;
        }
        private double getGyro_z(){
            return gyro_val_z;
        }
    }


    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    /* Calculates and returns battery temperature. */
    public double getBatteryTemperature(){
        // this method works for non-rooted phones and I just chose to associate it with the update record style
        if(collectionStyle.equals("update") || !rooted){
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            assert intent != null;
            double celsius = ((double)intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) / 10;
            return Double.valueOf(decimalFormat.format(((celsius * 9) / 5) + 32));
        // this method only works on rooted devices and I chose to do this for on second because it allows for more granular data
        } else if(collectionStyle.equals("second") && rooted){
            float[] batteryTemp = mHardwareManger.getDeviceTemperatures(DEVICE_TEMPERATURE_BATTERY, TEMPERATURE_CURRENT);
            return CtoF(batteryTemp[0]);
        } else {
            return -1;
        }
    }

    /* Returns battery level. */
    private double getBatteryLevel(){
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert intent != null;
        return (double)intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
    }

    /* Returns battery voltage. */
    private double getBatteryVoltage(){
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert intent != null;
        double batteryVoltage = (double)intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);
        batteryVoltage = Double.valueOf(decimalFormat2.format(batteryVoltage / 1000));     /* Convert from millivolts to volts. */
        return batteryVoltage;
    }

    /* Returns instantaneous battery current. */
    private double getBatteryCurrent(){
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        assert batteryManager != null;
        double batteryCurrent = (double)batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        batteryCurrent = Double.valueOf(decimalFormat2.format(batteryCurrent*Math.pow(10, -6)));   /* Convert from microamps to amps. */
        return batteryCurrent;
    }

    /* Returns available memory as a percent value. */
    private double getAvailMemory(){
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert activityManager != null;
        activityManager.getMemoryInfo(memoryInfo);
        double availPercent;
        availPercent = Double.valueOf(decimalFormat2.format(memoryInfo.availMem / (double)memoryInfo.totalMem * 100.0));
        return availPercent;
    }

    /* Get network usage for WiFi. */
    private long getWiFiUsage(long previousUsage){
        long startTime = 0;
        long endTime = System.currentTimeMillis();
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getSystemService(NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket;
        try{
            assert networkStatsManager != null;
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", startTime, endTime);
        }catch (RemoteException e){
            return -1;
        }
        long currentUsage = bucket.getRxPackets() + bucket.getTxPackets();
        return currentUsage - previousUsage;
    }

    /* Functions related to getting network usage for mobile data. */
    private long getDataUsage(long previousUsage){
        long startTime = 0;
        long endTime = System.currentTimeMillis();
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getSystemService(NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket;
        try{
            assert networkStatsManager != null;
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, getSubscriberId(ConnectivityManager.TYPE_MOBILE), startTime, endTime);
        }catch (RemoteException e){
            return -1;
        }
        long currentUsage = bucket.getRxPackets() + bucket.getTxPackets();
        return currentUsage - previousUsage;
    }

    @SuppressLint("MissingPermission")
    private String getSubscriberId(int networkType){
        if (ConnectivityManager.TYPE_MOBILE == networkType){
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            assert telephonyManager != null;
            return telephonyManager.getSubscriberId();
        }
        return "";
    }

    /* Calculates and returns the CPU usage. */
    //TODO find a way to do this on Android 8+ without root access???
    private double getCPULoad() {
        // base case for when the SDK is greater than android 7 and is not rooted.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !rooted) {
            return -1000.00;
        }
        try {
            //TODO This option generates around 30% CPU load alone... Therefore it cannot be used.
            //SuRandomAccessFile reader = SuRandomAccessFile.open("/proc/stat", "r");
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rooted){
            //    reader = new RandomAccessFile("/proc/stat", "r");
            //} else {
                //SuRandomAccessFile reader = SuRandomAccessFile.open(“/proc/stat”, “r”);
            //}

            String load = reader.readLine();

            String[] toks = load.split(" +");  // Split on one or more spaces

            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(400);
            } catch (Exception e) {
                e.printStackTrace();
            }

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");

            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            double raw_load = (double)(Math.abs(cpu2 - cpu1)) / Math.abs((cpu2 + idle2) - (cpu1 + idle1));
            if (Double.isNaN(raw_load)){
                raw_load = 0.0;
            }
            raw_load = Double.valueOf(decimalFormat2.format(raw_load*100));
            if (raw_load > 100){
                raw_load = 100.0;
            }
            return raw_load;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // REQUIRES ROOT
    private Double getCPUFreq(String cpuNumber){
        // base case for non-rooted phones
        if(!rooted){
            return -1000.00;
        }
        String cpuFreq = "";
        try{
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu" + cpuNumber +"/cpufreq/scaling_cur_freq", "r");
            cpuFreq = reader.readLine();
            reader.close();
        } catch (IOException e) {
            cpuFreq = "0";
            e.printStackTrace();
        }
        double freq = (double)Long.parseLong(cpuFreq);
        return freq;
    }

    private int getScreenOn(){
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                return 1;
            }
        }
        return 0;
    }

    private int getIsCharging(){
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        if(plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS){
            return 1;
        }
        return 0;
    }
    //REQUIRES ROOT
    private double getAverageCPUtemp() {
        // Base case for non-rooted phones
        if(!rooted){
            return -1000;
        }
        // allows user to get descriptions of how long jobs have been running on each CPU.
        // CpuUsageInfo[] usageInfo = mHardwareManger.getCpuUsages();
        // gets specific heat values
        /*
        FOR GOOGLE PIXEL 1
        * battery temp has only 1 value
        * CPU temp has 4 values, likely 1 for each core
        * skin temp has 1 value
        * fan speed is NOT supported
        */
        float[] batteryTemp = mHardwareManger.getDeviceTemperatures(DEVICE_TEMPERATURE_BATTERY, TEMPERATURE_CURRENT);
        float[] CPUTemp = mHardwareManger.getDeviceTemperatures(DEVICE_TEMPERATURE_CPU, TEMPERATURE_CURRENT);
        float[] skinTemp = mHardwareManger.getDeviceTemperatures(DEVICE_TEMPERATURE_SKIN, TEMPERATURE_CURRENT);
        // gets fan speeds
        float[] fanSpeeds = mHardwareManger.getFanSpeeds();
        /*
        Log.i("HardwareProperties", "Battery Temp is: " + CtoF(batteryTemp[0])
                + " CPU temps are: " + CtoF(CPUTemp[0]) + ", " + CtoF(CPUTemp[1]) + ", " + CtoF(CPUTemp[2]) + ", " + CtoF(CPUTemp[3]) +
                " Skin temp is: " + CtoF(skinTemp[0]));
        */
        double averageCPUTemp = (double)((CtoF(CPUTemp[0]) + CtoF(CPUTemp[1]) + CtoF(CPUTemp[2]) + CtoF(CPUTemp[3]))/4);
        return averageCPUTemp;
    }

    private float CtoF(float temp){
        double fahrenheit =  Double.valueOf(decimalFormat.format(((temp * 9) / 5) + 32));
        return (float)fahrenheit;
    }

    // REQUIRES ROOT
    private float[] getLoadAvg() {
        // base case for non-rooted phones
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || !rooted){
            float[] loadAvgArray = new float[3];
            loadAvgArray[0] = -1000;
            loadAvgArray[1] = -1000;
            loadAvgArray[2] = -1000;
            return loadAvgArray;
        }
        RandomAccessFile reader = null;

        try {
            reader = new RandomAccessFile("/proc/loadavg", "r");
            final String load = reader.readLine();
            String[] myString = load.split(" ");
            float[] cpuUsageAsFloat = new float[myString.length];
            //return only first 3 values (1,5, and 15 mins load avg)
            for (int i = 0; i < 3; i++) {
                myString[i] = myString[i].trim();
                cpuUsageAsFloat[i] = Float.parseFloat(myString[i]);
            }
            return cpuUsageAsFloat;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //return empty array if failed to get loadavg
        float[] loadAvgArray = new float[3];
        return loadAvgArray;
    }

    private int getNumThreads(int numThreads){
        // this is to compensate for the fact that numThreads is also a counter for the thread generation task
        if(numThreads >= 1 && numThreads <= 5){
            return numThreads - 1;
        }
        return 0;

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// METHODS FOR COLLECTING DATA /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    // Handler to collect data once per second rather than on update of battery temperature
    private final class TemperatureEstimateServiceHandler extends Handler{
        TemperatureEstimateServiceHandler(Looper looper){
            super(looper);
        }
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message message){
            synchronized (this){
                while(isRunning){
                    // private function which logs data to console and stores in database
                    logData();
                }
            }
            // Stops the service for the start Id
            stopSelfResult(message.arg1);
        }
    }

    private class BatteryInfoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int celsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            double temp =  ((double)celsius) / 10;
            double fahrenheit =  Double.valueOf(decimalFormat.format(((temp * 9) / 5) + 32));
            // track frequency of battery sensor update
            recieveCounter = recieveCounter + 1;
            Date currentTime = Calendar.getInstance().getTime();
            String currentTimeString = dateFormat.format(currentTime);
            if(firstTimestamp.equals("")){
                firstTimestamp = currentTimeString;
                firstDateTime = currentTime;
            }
            Log.i("onBatteryTempReceive", "update number " + recieveCounter + " at: " + currentTimeString + " " + fahrenheit + " degrees F!");

            // method to log data to console and database
            logData();
        }
    }

    private void logData(){
        try {
            Date currentTime = Calendar.getInstance().getTime();
            String currentTimeString = dateFormat.format(currentTime);
            double battery_temp = getBatteryTemperature();
            double battery_level = getBatteryLevel();
            double battery_voltage = getBatteryVoltage();
            double battery_current = getBatteryCurrent();
            double available_memory = getAvailMemory();
            long wifiUsagePrevious = getWiFiUsage(0);
            long dataUsagePrevious = getDataUsage(0);
            double cpu_load = getCPULoad();
            Thread.sleep(500);
            long wifiUsageCurrent = getWiFiUsage(wifiUsagePrevious);
            long dataUsageCurrent = getDataUsage(dataUsagePrevious);
            // HARDWARE SENSOR MEASUREMENTS
            double proximity_data = currentSensorData.getProx_val();

            double accelerometer_data_x = currentSensorData.getAcc_x();
            double accelerometer_data_y = currentSensorData.getAcc_y();
            double accelerometer_data_z = currentSensorData.getAcc_z();

            double light_data = currentSensorData.getLight_val();

            double gyroscope_data_x = currentSensorData.getGyro_x();
            double gyroscope_data_y = currentSensorData.getGyro_y();
            double gyroscope_data_z = currentSensorData.getGyro_z();
            // extra state info
            double cpu_temp = getAverageCPUtemp();
            int screenOn = getScreenOn();
            int isCharging = getIsCharging();
            double CPU0Freq = getCPUFreq("0");
            double CPU1Freq = getCPUFreq("1");
            double CPU2Freq = getCPUFreq("2");
            double CPU3Freq = getCPUFreq("3");

            int loggedNumberOfThreads = getNumThreads(numThreads);

            float[] loadAvg = getLoadAvg();
            double oneMinLoadAvg = (double)loadAvg[0];
            double fiveMinLoadAvg = (double)loadAvg[1];
            double fifteenMinLoadAvg = (double)loadAvg[2];

            double prediction = 0;

            Log.i(currentTimeString, "Battery Temp: " + battery_temp +
                    " Battery Level " + battery_level +
                    " Battery Volt " + battery_voltage +
                    " Battery Current " + battery_current +
                    " CPU Load " + cpu_load +
                    " 1 min load Avg: " + oneMinLoadAvg +
                    " 5 min load Avg: " + fiveMinLoadAvg +
                    " 15 min load Avg: " + fifteenMinLoadAvg +
                    " CPU 0 Frequency: " + CPU0Freq +
                    " CPU 1 Frequency: " + CPU1Freq +
                    " CPU 2 Frequency: " + CPU2Freq +
                    " CPU 3 Frequency: " + CPU3Freq +
                    " number of load threads: " + loggedNumberOfThreads +
                    " CPU Temp: " + cpu_temp +
                    " Available Mem " + available_memory +
                    " Wifi Usage "  + wifiUsageCurrent +
                    " Data Usage " + dataUsageCurrent +
                    " proxy: " + proximity_data +
                    " light: " + light_data +
                    " acc_x: " + accelerometer_data_x +
                    " acc_y: " + accelerometer_data_y +
                    " acc_z: " + accelerometer_data_z +
                    " gyro_x: " + gyroscope_data_x +
                    " gyro_y: " + gyroscope_data_y +
                    " gyro_z: " + gyroscope_data_z +
                    " screen on: " + screenOn +
                    " is charging: " + isCharging

            );
            //TODO add time since charging and time since screen on...
            //TODO whenever you change this make sure to start and stop logging, then clear cache and app data a few times
            database.insertEntry(currentTimeString,
                    battery_temp,
                    battery_level,
                    battery_voltage,
                    battery_current,
                    cpu_load,
                    oneMinLoadAvg,
                    fiveMinLoadAvg,
                    fifteenMinLoadAvg,
                    CPU0Freq,
                    CPU1Freq,
                    CPU2Freq,
                    CPU3Freq,
                    loggedNumberOfThreads,
                    cpu_temp,
                    available_memory,
                    wifiUsageCurrent,
                    dataUsageCurrent,
                    proximity_data,
                    light_data,
                    accelerometer_data_x,
                    accelerometer_data_y,
                    accelerometer_data_z,
                    gyroscope_data_x,
                    gyroscope_data_y,
                    gyroscope_data_z,
                    screenOn,
                    isCharging,
                    prediction);
        } catch(Exception e){
            Log.i("CATCH", e.getMessage());
        }
    }

}
