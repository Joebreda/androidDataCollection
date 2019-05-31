package com.example.deployment;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class stressCPUService extends Service {
    int numThreads = 0;
    Thread[] threads = new Thread[4];

    class ShuffleSortThread extends Thread {
        ShuffleSortThread() {
            // Create a new, second thread
            super("Shuffle Sort Thread");
            start(); // Start the thread
        }
        // This is the entry point for the second thread.
        public void run() {
            int counter = 0;
            while(!this.isInterrupted()){
                counter++;
            }
        }
    }

    @Override
    public void onCreate(){
        Log.i("HELLO", "CPU stress service on create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.i("HELLO", "CPU stress service on start");
        //TODO this is where we are generating the load.
        int NUM_THREADS = 4;
        for(int i = 0; i < NUM_THREADS; ++i) {
            // must keep track of the threads so they can be interupted
            threads[i] = new ShuffleSortThread(); // create a new thread
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.e("CPU STRESS FROM SERVICE", "TURNING OFF CPU STRESS AND SETTING NUMTHREADS TO 0");
        // sets number of running threads to be logged
        numThreads = 0;
        // stops all running threads thus reducing the CPU frequencies of each core
        threads[0].interrupt();
        threads[1].interrupt();
        threads[2].interrupt();
        threads[3].interrupt();
        Log.i("HELLO", "CPU stress service on destroy");
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
}
