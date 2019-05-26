package com.example.deployment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class DisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        /*
        //TODO: setContentView(R.layout.activity_display_temperature);
        final TextView textView = findViewById(R.id.textView_predTemperature);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        double temperature = intent.getDoubleExtra(TemperaturePredictorService.EXTRA_PREDICTION, 0);
                        textView.setText("Predicted Temperature: " + temperature + " \u2109");
                    }
                }, new IntentFilter(TemperaturePredictorService.ACTION_PREDICTION_BROADCAST)
        );
        */
    }
}
