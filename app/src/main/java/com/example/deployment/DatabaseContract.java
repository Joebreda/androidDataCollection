package com.example.deployment;

import android.provider.BaseColumns;

final class DatabaseContract {
    // Prevent someone from accidentally instantiating the contract class; make
    // constructor private.
    private DatabaseContract(){
    }

    /* Inner class that defines the table contents */
    static class DatabaseEntry implements BaseColumns{
        static final String TABLE_NAME = "temperature_database";
        static final String COLUMN_TIME = "time";
        // battery data
        static final String COLUMN_TEMPERATURE = "battery_temperature";
        static final String COLUMN_LEVEL = "battery_level";
        static final String COLUMN_VOLTAGE = "battery_voltage";
        static final String COLUMN_CURRENT = "battery_current";
        // CPU data
        static final String COLUMN_CPU = "cpu_usage";
        static final String COLUMN_ONE_MIN_LOAD = "one_min_load";
        static final String COLUMN_FIVE_MIN_LOAD = "five_min_load";
        static final String COLUMN_FIFTEEN_MIN_LOAD = "fifteen_min_load";
        static final String COLUMN_CPU_FREQ0 = "cpu_0_freq";
        static final String COLUMN_CPU_FREQ1 = "cpu_1_freq";
        static final String COLUMN_CPU_FREQ2 = "cpu_2_freq";
        static final String COLUMN_CPU_FREQ3 = "cpu_3_freq";
        static final String COLUMN_NUM_THREADS = "num_threads";
        static final String COLUMN_CPU_TEMP = "cpu_temp";
        // software data
        static final String COLUMN_MEMORY = "available_memory";
        static final String COLUMN_WIFI_USAGE = "wifi_usage";
        static final String COLUMN_DATA_USAGE = "data_usage";
        // sensor data
        static final String COLUMN_PROXIMITY_DATA = "proximity";
        static final String COLUMN_ACCELEROMETER_X = "accelerometer_x";
        static final String COLUMN_ACCELEROMETER_Y = "accelerometer_y";
        static final String COLUMN_ACCELEROMETER_Z = "accelerometer_z";
        static final String COLUMN_LIGHT_DATA = "light";
        static final String COLUMN_GYROSCOPE_DATA_X = "gyroscope_x";
        static final String COLUMN_GYROSCOPE_DATA_Y = "gyroscope_y";
        static final String COLUMN_GYROSCOPE_DATA_Z = "gyroscope_z";
        // abstract state data
        static final String COLUMN_SCREEN_ON = "screen_on";
        static final String COLUMN_IS_CHARGING = "is_charging";
        // prediction
        static final String COLUMN_PREDICTION = "prediction";
    }
}