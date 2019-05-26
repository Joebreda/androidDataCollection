package com.example.deployment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "BatteryTemperatures.db";
    // TODO remove columns if necessary
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DatabaseContract.DatabaseEntry.TABLE_NAME + " (" +
                    DatabaseContract.DatabaseEntry._ID + " INTEGER PRIMARY KEY," +
                    DatabaseContract.DatabaseEntry.COLUMN_TIME + " TEXT," +
                    DatabaseContract.DatabaseEntry.COLUMN_TEMPERATURE + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_LEVEL + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_VOLTAGE + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CURRENT + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CPU + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_ONE_MIN_LOAD + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_FIVE_MIN_LOAD + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_FIFTEEN_MIN_LOAD + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ0 + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ1 + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ2 + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ3 + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_NUM_THREADS + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_CPU_TEMP + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_MEMORY + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_WIFI_USAGE + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_DATA_USAGE + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_PROXIMITY_DATA + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_LIGHT_DATA + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_ACCELEROMETER_X + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_ACCELEROMETER_Y + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_ACCELEROMETER_Z + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_GYROSCOPE_DATA_X + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_GYROSCOPE_DATA_Y + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_GYROSCOPE_DATA_Z + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_SCREEN_ON + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_IS_CHARGING + " REAL," +
                    DatabaseContract.DatabaseEntry.COLUMN_PREDICTION + " REAL)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DatabaseContract.DatabaseEntry.TABLE_NAME;

    DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.d("Database operations", "Database created!");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // For now the upgrade process just deletes the old database and creates
        // a new one
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
        Log.d("Database operations", "Database upgraded!");
    }
    // TODO MAKE SURE THIS WORKS AS WELL (INPUT PARAMETERS AS WELL AS VALUES.PUT

    long insertEntry(String time,
                     double temperature,
                     double level,
                     double voltage,
                     double current,
                     double cpu_load,
                     double oneMinLoadAvg,
                     double fiveMinLoadAvg,
                     double fifteenMinLoadAvg,
                     double cpu_freq0,
                     double cpu_freq1,
                     double cpu_freq2,
                     double cpu_freq3,
                     int num_threads,
                     double cpu_temp,
                     double memory,
                     long wifi_usage,
                     long data_usage,
                     double proximity,
                     double light,
                     double acceleration_x,
                     double acceleration_y,
                     double acceleration_z,
                     double gyroscope_x,
                     double gyroscope_y,
                     double gyroscope_z,
                     int screenOn,
                     int isCharging,
                     double prediction){
        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.DatabaseEntry.COLUMN_TIME, time);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_TEMPERATURE, temperature);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_LEVEL, level);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_VOLTAGE, voltage);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CURRENT, current);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CPU, cpu_load);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_ONE_MIN_LOAD, oneMinLoadAvg);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_FIVE_MIN_LOAD, fiveMinLoadAvg);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_FIFTEEN_MIN_LOAD, fifteenMinLoadAvg);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ0, cpu_freq0);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ1, cpu_freq1);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ2, cpu_freq2);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CPU_FREQ3, cpu_freq3);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_NUM_THREADS, num_threads);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_CPU_TEMP, cpu_temp);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_MEMORY, memory);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_WIFI_USAGE, wifi_usage);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_DATA_USAGE, data_usage);
        // Sensor Data
        values.put(DatabaseContract.DatabaseEntry.COLUMN_PROXIMITY_DATA, proximity);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_LIGHT_DATA, light);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_ACCELEROMETER_X, acceleration_x);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_ACCELEROMETER_Y, acceleration_y);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_ACCELEROMETER_Z, acceleration_z);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_LIGHT_DATA, light);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_GYROSCOPE_DATA_X, gyroscope_x);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_GYROSCOPE_DATA_Y, gyroscope_y);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_GYROSCOPE_DATA_Z, gyroscope_z);
        // abstract state info
        values.put(DatabaseContract.DatabaseEntry.COLUMN_SCREEN_ON, screenOn);
        values.put(DatabaseContract.DatabaseEntry.COLUMN_IS_CHARGING, isCharging);
        // prediction
        values.put(DatabaseContract.DatabaseEntry.COLUMN_PREDICTION, prediction);

        long newRowId = database.insert(DatabaseContract.DatabaseEntry.TABLE_NAME, null, values);

        //Log.d("Database operations", "Row inserted!");
        return newRowId;
    }

    public Cursor getEntry(long id){
        SQLiteDatabase database = this.getReadableDatabase();
        Cursor res = database.rawQuery( " SELECT * FROM " + DatabaseContract.DatabaseEntry.TABLE_NAME + " WHERE " +
                DatabaseContract.DatabaseEntry._ID + "=?", new String[]{Long.toString(id)});
        Log.d("Database operations", "Row returned!");
        return res;
    }

    void clearDB(){
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(DatabaseContract.DatabaseEntry.TABLE_NAME, null, null);
        database.close();
    }

    void exportDB(){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;
        FileChannel destination;
        String currentDBPath = "/data/" + "com.example.deployment" + "/databases/" + DATABASE_NAME;
        //String currentDBPath = "com.examples.deployment" + "/databases/" + DATABASE_NAME;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, DATABASE_NAME);
        try{
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        } catch(IOException e){
            e.printStackTrace();
        }

    }
}