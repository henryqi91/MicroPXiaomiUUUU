package com.example.henryqi_code.micropxiaomiuuuu;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by henryqi_code on 04/04/2016.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBtManager;
    private BluetoothAdapter mBtAdapter;
    private String mBtDeviceAddr;
    private BluetoothGatt mBtGatt;
    private int mConnState = STATE_CONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "EXTRA_DATA";

    public final static UUID UUID_ROLL_MEASUREMENT = UUID.fromString(SampleGattAttributes.ROLL_MEASUREMENT);
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation.
         *                 {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of
         *                 {@link BluetoothProfile#STATE_DISCONNECTED} or
         *                 {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnState = STATE_CONNECTED;
                broadcastUpdate(intentAction);

                Log.i(TAG, "Connected to GATT server");
                Log.i(TAG, "Attempting to start service discovery:" + mBtGatt.discoverServices());
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server");
                broadcastUpdate(intentAction);
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else{
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated
         *                       remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

//        /**
//         * Callback indicating the result of a characteristic write operation.
//         * <p>
//         * <p>If this callback is invoked while a reliable write transaction is
//         * in progress, the value of the characteristic represents the value
//         * reported by the remote device. An application should compare this
//         * value to the desired value to be written. If the values don't match,
//         * the application must abort the reliable write transaction.
//         *
//         * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
//         * @param characteristic Characteristic that was written to the associated
//         *                       remote device.
//         * @param status         The result of the write operation
//         *                       {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
//         */
////        @Override
////        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//////            super.onCharacteristicWrite(gatt, characteristic, status);
////        }
    };

    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);
        if(UUID_ROLL_MEASUREMENT.equals(characteristic.getUuid())){
            int flag = characteristic.getProperties();
            int format = -1;
            if((flag & 0x01) != 0){
                format = BluetoothGattCharacteristic.FORMAT_UINT16; //*** subject to change
                Log.d(TAG, "Heart rate format UINT16.");
            }else{
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG,"Heart rate format UINT8.");
            }
            final int rollValue = characteristic.getIntValue(format,1);
            Log.d(TAG,String.format("Received Roll value: %d", rollValue));
            intent.putExtra(EXTRA_DATA, String.valueOf(rollValue));
        }else{
            final byte[] data = characteristic.getValue();
            if(data != null && data.length > 0){
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar:data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
            }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService(){
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    public boolean onUnbind(Intent intent){
        close();
        return super.onUnbind(intent);
    }

    private IBinder mBinder = new LocalBinder();

    public boolean initialize(){
        if(mBtManager == null){
            mBtManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBtManager == null){
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }

        mBtAdapter = mBtManager.getAdapter();
        if(mBtAdapter == null){
            Log.e(TAG, "Unable to obtain a BluetoothAdapter");
            return false;
        }

        return true;
    }

    public boolean connect(final String address){
        if(mBtAdapter == null || address == null){
            Log.w(TAG, "BluetoothAdapter not initialzied or unspecified address.");
            return false;
        }

        if( (mBtDeviceAddr != null) && (address.equals(mBtDeviceAddr)) && (mBtGatt != null) ) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if(mBtGatt.connect()){
                mConnState = STATE_CONNECTING;
                return true;
            }else{
                return false;
            }
        }

        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        if(device == null){
            Log.w(TAG, "Device not found. Unable to Connect");
            return false;
        }

        mBtGatt = device.connectGatt(this,false,mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBtDeviceAddr = address;
        mConnState = STATE_CONNECTING;
        return true;
    }

    public void disconnect(){
        if(mBtAdapter == null || mBtGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBtGatt.disconnect();
    }

    public void close(){
        if(mBtGatt == null){
            return;
        }
        mBtGatt.close();
        mBtGatt = null;
    }

    public void readCharacteristic (BluetoothGattCharacteristic characteristic){
        if(mBtAdapter == null || mBtGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBtGatt.readCharacteristic(characteristic);
    }

//    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled){
//        if(mBtAdapter == null || mBtGatt == null){
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBtGatt.setCharacteristicNotification(characteristic, enabled);
//        //ROll-specific
//        if(UUID_ROLL_MEASUREMENT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.C))
//        }
//    }

    public List<BluetoothGattService> getSupportedGattServices(){
        if(mBtGatt == null) return null;

        return mBtGatt.getServices();
    }

}
