package cardroid.foi.hr.cardroid;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Created by Hrgar on 1.6.2016..
 */
public class BluetoothFragment extends Fragment {


    private static final int REQUEST_CONNECT_DEVICE = 1;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService bluetoothService = null;
    private static final int REQUEST_ENABLE_BT = 3;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else if (bluetoothService == null) {
            bluetoothService = new BluetoothService(getActivity(), mHandler);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceScan.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
               // String address = serverIntent.getExtras()
                       // .getString(DeviceScan.DEVICE_ADDRESS);
              //  Log.i("Device adress!!!!!, ", address);
                return true;
            }
        }
        return false;

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CONNECT_DEVICE) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {

                String address = data.getExtras().getString(DeviceScan.DEVICE_ADDRESS);
                connectToDevice(address);
                 Log.i("Device adress!!!!!, ", address);

            }
        }
    }

    private void connectToDevice(String deviceAddress){

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        // Attempt to connect to the device
        bluetoothService.connect(device);

    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case MessageHandlingConstants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                         //   setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                           // setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MessageHandlingConstants.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(msg.toString());
                 //   mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MessageHandlingConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                  //  mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case MessageHandlingConstants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                  //  mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                      //  Toast.makeText(activity, "Connected to "
                        //        + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MessageHandlingConstants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(MessageHandlingConstants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };


}
