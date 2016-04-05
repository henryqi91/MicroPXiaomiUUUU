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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Delayed;

public class MainActivity extends AppCompatActivity {

    //UI Section
    private SeekBar speedBar, intensityBar;
    private TextView speedBarValue, intensityBarValue,bleBtnText,testText,rollBox
            ,pitchBox,tempBox;
    private Button bleBtn,closeConnBtn;
    private int blScanStage;

    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mBtDevice;
    private BluetoothGatt mGatt;
    private BluetoothLeService mBtLeService;

    private int REQ_ENABLE_BT = 1;
    private boolean isClosed = false; // flag to make sure pairing only starts via button press
    //UUID for 2 Services:
    public static String UUID_ORIENTATION_SERVICE = "42821a40-e477-11e2-82d0-0002a5d5c51b";
    public static String UUID_SAMPLE_SERVICE = "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    //UUID for 4 characteristics:
    public static String UUID_TEST_BUTTON = "e23e78a0-cf4a-11e1-8ffc-0002a5d5c51b"; //Free Fall
    public static String UUID_TEMP_MEASUREMENT = "a32e5520-e477-11e2-a9e3-0002a5d5c51b";//
    public static String UUID_PITCH_MEASUREMENT = "cd20c480-e48b-11e2-840b0002a5d5c51b";
    public static String UUID_ROLL_MEASUREMENT = "01c50b60-e48c-11e2-a073-0002a5d5c51b";

    //Controlling the comm of the BLE:
    private final static String TAG = MainActivity.class.getSimpleName();
    private String mDeviceName,mDeviceAddr;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;


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
            //step 2: scanning on click
            bleBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    //check if Bt is enabled, prompt the user if not
                    if( mBtAdapter == null || !mBtAdapter.isEnabled() ){
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE );
                        startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
                    }
                    isClosed = false;
                    blScanStage = 1;
                    bleBtn.setText(btnTextUpdate(blScanStage)); // "SEARCHING FOR DEVICE..."
                    //step 2: find the BLE Device(nucleo ADDR:03:80:E1:00:34:08 )
                    scanLeDevice(true);
                }
            });
            closeConnBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    close();
                    blScanStage = 0;
                    bleBtn.setText(btnTextUpdate(blScanStage));
                    bleBtnText.setText("");
                    testText.setText("");
                    scanLeDevice(false);
                }
            });
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

        closeConnBtn = (Button)findViewById(R.id.closeConnBtn);

        testText = (TextView)findViewById(R.id.userBtnTest);
        testText.setText("waiting..");

        rollBox = (TextView)findViewById(R.id.rollBox);
        pitchBox = (TextView)findViewById(R.id.pitchBox);
        tempBox = (TextView)findViewById(R.id.tempBox);
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
                output = "FOUND!WAITING...";
                break;

            case 3:
                output = "CONNECTED!";
                break;
        }
        return output;
    }

    /** Scanning Stage **/
    private void scanLeDevice(final boolean enable){
        if(enable && !isClosed){
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
                            bleBtn.setText(btnTextUpdate(blScanStage));//"found!Waiting.."
                            if (device.toString().equals("03:80:E1:00:34:08"))
                                connToGatt(device);
                        }
                    });
                }
            };
    /** Gatt methods: **/
    //connect to Gatt of the Device:
    public void connToGatt(BluetoothDevice device){
        if(mGatt == null && !isClosed){
            mGatt = device.connectGatt(this,false,gattCallback);

            blScanStage = 3;
            bleBtn.setText(btnTextUpdate(blScanStage)); // connected!
            bleBtnText.setText(device.toString()); // print MAC address of the device

            scanLeDevice(false); // stop scanning on finding the device.
        }
    }

    //Gatt call back method
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        testText.setText("Watiing for values...");
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
//            if(status == BluetoothGatt.GATT_SUCCESS){
//            testText.setText("Watiing for values...");
                List<BluetoothGattService> gattServices = gatt.getServices();
                updateRollPitchTemp(gattServices);
//            }
        }
    };

    public void updateRollPitchTemp( List<BluetoothGattService> gattServices) {
        //if(gattServices == null) return;
        int rV,pV,tV;
        String uuid = "";
//        testText.setText("Watiing for values...");
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
//                final int charaProp = gattCharacteristic.getProperties();
//                if( (charaProp | gattCharacteristic.PROPERTY_READ) > 0){
//                    if(mNotifyCharacteristic != null){
//                        mGatt.setCharacteristicNotification(mNotifyCharacteristic,false);
//                        mNotifyCharacteristic = null;
//                    }
                mBtLeService.readCharacteristic(gattCharacteristic);
//                }
//                if( (charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0){
//                    mNotifyCharacteristic = gattCharacteristic;
//                    mGatt.setCharacteristicNotification(gattCharacteristic, true);
//                }
                uuid = gattCharacteristic.getUuid().toString();
                //
                if(uuid.equals(UUID_ROLL_MEASUREMENT)){
                    rV = gattCharacteristic.getIntValue(0x12,1);
                    rollValueUpdate(rV);
                }
                else if(uuid.equals(UUID_PITCH_MEASUREMENT)){
                    pV = gattCharacteristic.getIntValue(0x12,1);
                    pitchValueUpdate(pV);
                }
                else if(uuid.equals(UUID_TEMP_MEASUREMENT)){
                    tV = gattCharacteristic.getIntValue(0x12,1);
                    tempValueUpdate(tV);
                }
                else{
                    rollValueUpdate(0);
                    pitchValueUpdate(0);
                    tempValueUpdate(0);
                }
            }
        }
    }
//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
//                mConnected = true;
//                updateConnectionState
//            }
//
//        }
//    }
//
//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mBtLeService = ((BluetoothLeService.LocalBinder) service).getService();
//            if(!mBtLeService.initialize()){
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
//            mBtLeService.connect(mDeviceAddr);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            mBtLeService = null;
//        }
//    };


//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver(){
//        @Override
//        public void onReceive(Context context, Intent intent){
//            final String action = intent.getAction();
//            if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
//                mConnected = true;
//            }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
//                mConnected = false;
//            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                //get all services and
//                getGattServices(mBtLeService.getSupportedGattServices());
//            }
//        }
//    };

//    private void getGattServices(List<BluetoothGattService> gattServices){
//        if(gattServices == null) return;
//        String uuid = null;
//        String unknownServiceString = "Unknown Service";
//        String unknownCharaString = "Unknown Characteristic";
//        ArrayList<HashMap<String,String>> gattServiceData =
//                new ArrayList<HashMap<String, String>>();
//        ArrayList< ArrayList< HashMap<String,String> > > gattCharacteristicData =
//                new ArrayList< ArrayList< HashMap<String,String> > >();
//        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
//
//        //Loop through aviilable GATT services:
////        for(BluetoothGattService gattService : gattServices){
//            HashMap<String,String> currentServiceData = new HashMap<String,String>();
//            uuid = gattService.getUuid().toString();
//            currentServiceData.put(
//                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
//            currentServiceData.put(LIST_UUID, uuid);
//            gattServiceData.add(currentServiceData);
//        // end of the GATT services Loop
//
//            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
//                    new ArrayList<HashMap<String,String>>();
//            List<BluetoothGattCharacteristic> gattCharacteristics =
//                    gattService.getCharacteristics();
//            ArrayList<BluetoothGattCharacteristic> charas =
//                    new ArrayList<BluetoothGattCharacteristic>();
//
//            //Loop through Service's characteristics and put them in
//            for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
//                charas.add(gattCharacteristic);
//                HashMap<String,String> currentCharaData = new HashMap<String,String>();
//                uuid = gattCharacteristic.getUuid().toString();
//                //**testing roll and pitch first
//                if(uuid.equals(ROLL_MEASUREMENT) || uuid.equals(PITCH_MEASUREMENT)) {
//                    currentCharaData.put(
//                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
//                    currentCharaData.put(LIST_UUID, uuid);
//                    gattCharacteristicGroupData.add((currentCharaData));
//                }
//            }
//            mGattCharacteristics.add(charas);
//            gattCharacteristicData.add(gattCharacteristicGroupData);
//        }
//    }

//    private static IntentFilter makeGattUpdateIntentFilter(){
//        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        return intentFilter;
//    }

    public void rollValueUpdate(int rollValue){
        //rollValue /= 100;
        String rollText = "" + rollValue;
        rollBox.setText("100");
    }
    public void pitchValueUpdate(int pitchValue){
//        pitchValue /= 100;
        String pitchText = "" + pitchValue;
        pitchBox.setText("100");
    }
    public void tempValueUpdate(int tempValue){
//        tempValue /= 100;
        String tempText = "" + tempValue;
        tempBox.setText("100");
    }

    public void close(){
        if(mGatt == null){
            return;
        }
        isClosed = true;
        mGatt.close();
        //mBtAdapter.disable();
        mGatt = null;
    }

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
