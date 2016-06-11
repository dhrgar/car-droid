package cardroid.foi.hr.cardroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Hrgar on 31.5.2016..
 */
public class DeviceScan extends Activity {


    public static String DEVICE_ADDRESS = "device_address";

    private ListView lv = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        lv = (ListView) findViewById(R.id.device_list);



        ArrayList deviceStrs = new ArrayList();
        final ArrayList devices = new ArrayList();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }

        // show list

        //final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // selected item
           String deviceAddress = devices.get(position).toString();

                Intent intent = new Intent();
                intent.putExtra(DEVICE_ADDRESS, deviceAddress);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
          /*
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String deviceAddress = devices.get(position).toString();
                // TODO save deviceAddress
                Intent intent = new Intent();
                intent.putExtra(DEVICE_ADDRESS, deviceAddress);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
*/
      //  alertDialog.setTitle("Choose Bluetooth device");
      //  alertDialog.show();*/
    }
}
