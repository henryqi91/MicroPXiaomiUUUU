package com.example.henryqi_code.micropxiaomiuuuu;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;

public class MainActivity extends AppCompatActivity {

    //UI Section
    private SeekBar speedBar, intensityBar;
    private TextView speedBarValue, intensityBarValue,bleBtnText;
    private Button bleBtn;
    private int blScanStage;

    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mBtDevice;
    private BluetoothGatt mGatt;

    private int REQ_ENABLE_BT = 1;
//    private Handler mHandler;
//    private static  final long SCAN_PERIOD = 10000; // 10 sec
//    private BluetoothLeScanner mLEScanner;
//    private ScanSettings settings;
//    private List<ScanFilter> filters;
//    private BluetoothGatt mGatt;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //acquire the BtAdapter
        final BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = btManager.getAdapter();
        /*** UI section: ***/
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
        //intensity Bar
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
//        //BLE Button Text
        blScanStage = 0;
        bleBtn.setText(btnTextUpdate(blScanStage));// "PRESS FOR PAIRING"

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume(){
        super.onResume();
        //step 1: enable Bt if not:
        if( mBtAdapter == null || !mBtAdapter.isEnabled() ){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
        }else {
            //step 2: scanning
            bleBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    blScanStage = 1;
                    bleBtn.setText(btnTextUpdate(blScanStage)); // "SEARCHING FOR DEVICE..."
                    //step 2: find the BLE Device(nucleo ADDR:03:80:E1:00:34:12 )
                    scanLeDevice(true);
                }
            });
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        if(mBtAdapter != null && mBtAdapter.isEnabled()){
            scanLeDevice(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * UI-initialization
     */
    private void init() {
        speedBar = (SeekBar) findViewById(R.id.speedBar);
        speedBarValue = (TextView) findViewById(R.id.speedCurrent);

        intensityBar = (SeekBar) findViewById(R.id.intensityBar);
        intensityBarValue = (TextView) findViewById(R.id.intensityCurr);

        bleBtn = (Button) findViewById(R.id.bleBtn);
        bleBtnText = (TextView)findViewById(R.id.bleBtnText);
    }

    /**
     * @Method: provide a message to display
     * @param blScanStage
     * @return String: message to display
     */
    private String btnTextUpdate(int blScanStage){
        String output = "";
        switch(blScanStage){
            case 0 :
                output = "PRESS FOR PAIRING";
                break;

            case 1:
                output = "SEARCHING FOR DEVICE...";
                break;

            case 2:
                output = "DEVICE FOUND! PAIRING...";
                break;

            case 3:
                output = "CONNECTED!";
                break;
        }
        return output;
    }

    //scanning method
    private void scanLeDevice(final boolean enable){
        if(enable){
            mBtAdapter.startLeScan(mLeScanCallback);
        }else{
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    }
    //Device Scan call back
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback(){
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            blScanStage = 2;
                            bleBtn.setText(btnTextUpdate(blScanStage));//"found! pairing..."
                            connToGatt(device);
                        }
                    });
                }
            };
    /** Gatt methods: **/
    //connect to Gatt of the Device:
    public void connToGatt(BluetoothDevice device){
        if(mGatt == null){
            mGatt = device.connectGatt(this,false,gattCallback);

            blScanStage = 3;
            bleBtn.setText(btnTextUpdate(blScanStage)); // connected!

            bleBtnText.setText(device.toString()); // print MAC address of the device
           // scanLeDevice(false); // stop on finding the device.
        }
    }
    //Gatt call back method
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch(newState){
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    break;
            }
        }
    };

    /*
    Google System-Defined APIs
     */
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.henryqi_code.micropxiaomiuuuu/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }
    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.henryqi_code.micropxiaomiuuuu/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
