package com.example.henryqi_code.micropxiaomiuuuu;

import android.os.Bundle;
import android.app.Activity;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private SeekBar speedBar,intensityBar;
    private TextView speedBarValue,intensityBarValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize the requried variables
        init();
        //speedBar
        speedBar.setProgress(-10);
        speedBarValue.setText("Current value: " + speedBar.getProgress());
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = -10;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue - 10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                speedBarValue.setText("Current value: " + progress);
            }
        });

        // intialize the required materials
        intensityBarValue.setText("Current value: " + intensityBar.getProgress());
        intensityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                intensityBarValue.setText("Current value: " + progress);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    private void init(){
        speedBar = (SeekBar)findViewById(R.id.speedBar);
        speedBarValue = (TextView)findViewById(R.id.speedCurrent);
        intensityBar = (SeekBar)findViewById(R.id.intensityBar);
        intensityBarValue = (TextView)findViewById(R.id.intensityCurr);
    }

}
